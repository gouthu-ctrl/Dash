// Supabase Edge Function: generate-plan
// Generates AI trip plans using Google Gemini 1.5 Flash (via REST API) without OpenAI SDK

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

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

    // Google API Key from Supabase Secrets
    const googleApiKey = Deno.env.get("GOOGLE_API_KEY");
    if (!googleApiKey) {
      console.error("Missing GOOGLE_API_KEY secret.");
      // Fallback for demo if secret missing (DON'T DO THIS IN PROD)
      // return new Response(...)
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

    // 2. Call Google Gemini 1.5 Flash API (REST)
    // https://ai.google.dev/tutorials/rest_quickstart
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${googleApiKey}`;

    const requestBody = {
      contents: [{
        parts: [{
          text: `${systemPrompt}\n\nUser Request: ${prompt}`
        }]
      }],
      generationConfig: {
        response_mime_type: "application/json", // Force JSON mode if supported or just rely on prompt
        temperature: 0.7,
        max_output_tokens: 8192,
      }
    };

    const geminiRes = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(requestBody)
    });

    const geminiData = await geminiRes.json();

    // Extract text from Gemini response
    // Response structure: { candidates: [ { content: { parts: [ { text: "..." } ] } } ] }
    let rawContent = geminiData.candidates?.[0]?.content?.parts?.[0]?.text;

    if (!rawContent) {
      console.error("Gemini Error or Empty Response:", geminiData);
      return new Response(
        JSON.stringify({ success: false, error: "AI Generation failed" }),
        { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 500 }
      );
    }

    // Clean up potential markdown formatting (```json ... ```)
    rawContent = rawContent.replace(/^```json\s*/, "").replace(/\s*```$/, "");

    // Parse the JSON
    let items: GeneratedItem[] = [];
    try {
      items = JSON.parse(rawContent);
    } catch (e) {
      console.warn("JSON Parse Error:", e, rawContent);
      // Return raw content as summary if parsing fails
      return new Response(
        JSON.stringify({
          success: true,
          items: [],
          summary: rawContent
        } as PlanResponse),
        { headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 3. Save to Trip (Optional)
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
        console.error("Insert error:", insertError);
      }
    }

    return new Response(
      JSON.stringify({ success: true, items } as PlanResponse),
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
