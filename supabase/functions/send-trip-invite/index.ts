import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const RESEND_API_KEY = Deno.env.get('RESEND_API_KEY')

serve(async (req) => {
  const { tripTitle, destination, startDate, endDate, invitedBy, emails } = await req.json()

  // Formal friendly email template with HTML
  const emailHtml = `
    <div style="font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; max-width: 600px; margin: auto; border: 1px solid #eee; padding: 20px; border-radius: 10px;">
      <div style="text-align: center; margin-bottom: 20px;">
        <h1 style="color: #007AFF; margin: 0;">Dash</h1>
        <p style="color: #666; font-style: italic;">Your personal travel companion</p>
      </div>

      <p>Hello!</p>

      <p><strong>${invitedBy}</strong> has invited you to join a new trip on <strong>Dash</strong>!</p>

      <div style="background-color: #f9f9f9; padding: 15px; border-radius: 8px; margin: 20px 0;">
        <h2 style="margin-top: 0; color: #333;">${tripTitle}</h2>
        <p><strong>Destination:</strong> ${destination}</p>
        <p><strong>Dates:</strong> ${startDate} to ${endDate}</p>
      </div>

      <p>Click the button below to view the trip and start collaborating on your itinerary.</p>

      <div style="text-align: center; margin-top: 30px;">
        <a href="https://dash-travel.app/join" style="background-color: #007AFF; color: white; padding: 12px 24px; text-decoration: none; border-radius: 5px; font-weight: bold;">View Trip in Dash</a>
      </div>

      <hr style="border: 0; border-top: 1px solid #eee; margin: 30px 0;">

      <p style="font-size: 12px; color: #999; text-align: center;">
        You're receiving this because ${invitedBy} added your email to their trip on Dash Trip Planner.
      </p>
    </div>
  `

  // This example uses Resend. You can adapt it for SendGrid, Mailgun, etc.
  const res = await fetch('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${RESEND_API_KEY}`,
    },
    body: JSON.stringify({
      from: 'Dash Travel <onboarding@resend.dev>',
      to: emails,
      subject: `Invitation to join trip: ${tripTitle}`,
      html: emailHtml,
    }),
  })

  const data = await res.json()

  return new Response(JSON.stringify(data), {
    headers: { 'Content-Type': 'application/json' },
    status: 200,
  })
})
