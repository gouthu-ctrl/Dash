// Supabase Edge Function: generate-plan
// Generates AI trip plans using Groq (Mixtral 8x7b) via OpenAI-compatible API
// Implements caching via 'ai_suggestions_cache' table

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";
import { crypto } from "https://deno.land/std@0.168.0/crypto/mod.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface PlanRequest {
  prompt: string;       // e.g. "3 days in Kyoto, food + temples, budget $800"
  tripId?: string;      // Optional: existing trip to add items to
  preferences?: {       // Optional: user preferences from profile
    pace?: string;      // "relaxed", "medium", "packed"
    interests?: string[];
    dietary?: string[];
  };
}

interface GeneratedItem {
  type: string;         // "activity", "eat", "stay"
  title: string;
  description: string;
  start_time?: string;
  end_time?: string;
  location_name?: string;
  estimated_cost?: number;
  currency?: string;
}

interface PlanResponse {
  success: boolean;
  items?: GeneratedItem[];
  summary?: string;
  error?: string;
  debug_info?: string; // To indicate cache hit/miss
}

// Helper: Generate SHA-256 hash for cache key
async function generateCacheKey(text: string): Promise<string> {
  const msgUint8 = new TextEncoder().encode(text.toLowerCase().trim());
  const hashBuffer = await crypto.subtle.digest("SHA-256", msgUint8);
  const hashArray = Array.from(new Uint8Array(hashBuffer));
  const hashHex = hashArray.map(b => b.toString(16).padStart(2, "0")).join("");
  return hashHex;
}

serve(async (req: Request) => {
  // Handle CORS preflight
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: req.headers.get("Authorization")! } } }
    );

    const { prompt, tripId, preferences } = (await req.json()) as PlanRequest;

    if (!prompt) {
      return new Response(
        JSON.stringify({ success: false, error: "Prompt is required" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
      );
    }

    // --- CACHING LOGIC START ---
    const cacheKey = await generateCacheKey(prompt + JSON.stringify(preferences || {}));

    // Check Cache
    const { data: cachedData, error: cacheError } = await supabaseClient
      .from('ai_suggestions_cache')
      .select('response_payload')
      .eq('user_query_hash', cacheKey)
      .gt('expires_at', new Date().toISOString()) // Ensure not expired
      .single();

    if (cachedData && cachedData.response_payload) {
      console.log(`Cache Hit for key: ${cacheKey}`);
      return new Response(
        JSON.stringify({
          ...cachedData.response_payload,
          debug_info: "CACHE_HIT"
        }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    console.log(`Cache Miss for key: ${cacheKey}. Generating fresh plan...`);
    // --- CACHING LOGIC END ---

    // Groq API Key from Supabase Secrets
    const groqApiKey = Deno.env.get("GROQ_API_KEY");
    if (!groqApiKey) {
      console.error("Missing GROQ_API_KEY secret.");
      throw new Error("Missing GROQ_API_KEY");
    }

    // 1. Construct System Prompt
    const systemPrompt = `You are a helpful travel assistant that creates detailed day-by-day itineraries.
When given a travel request, output a JSON array of itinerary items.
Each item should have: type (activity/eat/stay), title, description, location_name, estimated_cost (number), currency.
Optionally include start_time and end_time in ISO format if specific times make sense.
Be realistic about costs and travel times.
${preferences?.pace ? `User prefers a ${preferences.pace} pace.` : ""}
${preferences?.interests?.length ? `User interests: ${preferences.interests.join(", ")}` : ""}
${preferences?.dietary?.length ? `Dietary preferences: ${preferences.dietary.join(", ")}` : ""}
Output ONLY valid JSON, no markdown, no explanation.`;

    // 2. Call Groq API (OpenAI Compatible)
    const groqUrl = "https://api.groq.com/openai/v1/chat/completions";
    // Using Llama 3.3 70B for high quality + speed (Mixtral 8x7b is decommissioned)
    const modelId = "llama-3.3-70b-versatile";

    const requestBody = {
      model: modelId,
      messages: [
        { role: "system", content: systemPrompt },
        { role: "user", content: `User Request: ${prompt}` }
      ],
      response_format: { type: "json_object" },
      temperature: 0.7,
      max_tokens: 4096
    };

    const groqRes = await fetch(groqUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${groqApiKey}`
      },
      body: JSON.stringify(requestBody)
    });

    if (!groqRes.ok) {
      const errText = await groqRes.text();
      console.error("Groq API Error:", errText);
      throw new Error(`Groq API Failed: ${errText}`);
    }

    const groqData = await groqRes.json();

    // Extract text from Groq response
    let rawContent = groqData.choices?.[0]?.message?.content;

    if (!rawContent) {
      console.error("Groq Error or Empty Response:", groqData);
      return new Response(
        JSON.stringify({ success: false, error: "AI Generation failed" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    // Parse the JSON (Groq usually follows instructions well with json_object mode, but sometimes wraps in keyed object)
    let items: GeneratedItem[] = [];
    try {
      const parsed = JSON.parse(rawContent);
      // Handle if Groq returns { "items": [...] } or just [...]
      if (Array.isArray(parsed)) {
        items = parsed;
      } else if (parsed.items && Array.isArray(parsed.items)) {
        items = parsed.items;
      } else if (parsed.itinerary && Array.isArray(parsed.itinerary)) {
        items = parsed.itinerary;
      } else {
        console.warn("Unexpected JSON structure:", parsed);
        // Attempt to find array in values
        const possibleArray = Object.values(parsed).find(v => Array.isArray(v));
        if (possibleArray) items = possibleArray as GeneratedItem[];
      }
    } catch (e) {
      console.warn("JSON Parse Error:", e, rawContent);
      return new Response(
        JSON.stringify({
          success: true,
          items: [],
          summary: rawContent
        } as PlanResponse),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const responsePayload: PlanResponse = { success: true, items, debug_info: "CACHE_MISS" };

    // 3. Save to Cache (Async, don't block response)
    const { error: insertCacheError } = await supabaseClient
      .from('ai_suggestions_cache')
      .insert({
        user_query_hash: cacheKey,
        location_normalized: prompt.match(/Context: I am visiting (.*?)\n/)?.[1]?.slice(0, 50)
          || prompt.match(/User Request: (.*)/)?.[1]?.slice(0, 50)
          || prompt.slice(0, 50),
        prompt_type: 'trip_plan',
        response_payload: responsePayload,
        // Expiry handled by default constraint (180 days) or explicitly set here
        expires_at: new Date(Date.now() + 180 * 24 * 60 * 60 * 1000).toISOString()
      });

    if (insertCacheError) {
      console.error("Cache insert error (non-fatal):", insertCacheError);
    }

    // 4. Save to Trip (Optional)
    if (tripId && items.length > 0) {
      const insertItems = items.map((item, index) => ({
        trip_id: tripId,
        type: item.type?.toLowerCase() || "activity",
        status: "proposed",
        title: item.title,
        description: item.description,
        start_time: item.start_time,
        end_time: item.end_time,
        location_name: item.location_name,
        estimated_cost: item.estimated_cost || 0,
        currency: item.currency || "USD",
        sorting_index: index * 100,
      }));

      const { error: insertError } = await supabaseClient
        .from("itinerary_items")
        .insert(insertItems);

      if (insertError) {
        console.error("Trip Item Insert error:", insertError);
      }
    }

    return new Response(
      JSON.stringify(responsePayload),
      { headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );

  } catch (err) {
    console.error("generate-plan error:", err);
    return new Response(
      JSON.stringify({ success: false, error: String(err) }),
      { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
    );
  }
});
