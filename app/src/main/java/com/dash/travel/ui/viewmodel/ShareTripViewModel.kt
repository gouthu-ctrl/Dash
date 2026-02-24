package com.dash.travel.ui.viewmodel

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dash.travel.data.local.dao.ItineraryDao
import com.dash.travel.data.model.SupabaseTrip
import com.dash.travel.data.repository.TripRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat // Consider java.time for new projects, but SimpleDateFormat is robust for this simple use
import java.util.Date
import java.util.Locale

class ShareTripViewModel(
    private val tripId: String,
    private val tripRepository: TripRepository,
    private val itineraryDao: ItineraryDao
) : ViewModel() {

    private val _isGeneratingPdf = MutableStateFlow(false)
    val isGeneratingPdf = _isGeneratingPdf.asStateFlow()

    private val _trip = MutableStateFlow<SupabaseTrip?>(null)
    val trip = _trip.asStateFlow()
    
    init {
        fetchTripData()
    }

    private fun fetchTripData() {
        viewModelScope.launch {
            try {
                val trip = tripRepository.getTripById(tripId)
                _trip.value = trip
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sharePdf(context: Context) {
        viewModelScope.launch {
            _isGeneratingPdf.value = true
            try {
                val pdfFile = generatePdf(context)
                if (pdfFile != null) {
                    shareFile(context, pdfFile)
                } else {
                    Toast.makeText(context, "Failed to generate PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                _isGeneratingPdf.value = false
            }
        }
    }

    private suspend fun generatePdf(context: Context): File? {
        val trip = _trip.value ?: return null
        
        // Fetch fresh data for PDF
        val items = itineraryDao.getItineraryListForTrip(tripId)
        val members = try { tripRepository.getTripMembersWithProfiles(tripId) } catch (e: Exception) { emptyList() }
        val votes = try { tripRepository.getVotesForTrip(tripId) } catch (e: Exception) { emptyList() }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points (approx)
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Dimensions & Styling
        val margin = 40f
        var yPosition = margin
        val pageWidth = pageInfo.pageWidth.toFloat()

        // --- Header ---
        paint.textSize = 24f
        paint.color = Color.BLACK
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        
        canvas.drawText(trip.title, margin, yPosition + 24, paint)
        yPosition += 40

        paint.textSize = 14f
        paint.typeface = android.graphics.Typeface.DEFAULT
        paint.color = Color.DKGRAY
        
        val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val itemDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault()) // Mon, Oct 12
        
        val startDate = parseIsoDate(trip.startDate)
        val endDate = parseIsoDate(trip.endDate)
        
        val dateString = if (startDate != null && endDate != null) {
             "${displayDateFormat.format(startDate)} - ${displayDateFormat.format(endDate)}"
        } else {
            "Dates: TBD"
        }
        canvas.drawText(dateString, margin, yPosition + 14, paint)
        yPosition += 20
        
        val locationName = trip.destinationData?.name ?: "Unknown Location"
        canvas.drawText("Location: $locationName", margin, yPosition + 14, paint)
        yPosition += 30

        // Team Members
        if (members.isNotEmpty()) {
            val memberNames = members.joinToString(", ") { it.profiles?.fullName ?: it.invitedEmail ?: "Unknown" }
            // Simple text wrapping handling could be added here, but for MVP truncating or single line
            // Checking width
            val teamPrefix = "Team: "
            val teamPaint = Paint(paint)
            val prefixWidth = teamPaint.measureText(teamPrefix)
            canvas.drawText(teamPrefix, margin, yPosition + 14, teamPaint)
            
            // Draw names (simple truncation if too long)
            val availableWidth = pageWidth - margin * 2 - prefixWidth
            val elidedNames = android.text.TextUtils.ellipsize(memberNames, android.text.TextPaint(paint), availableWidth, android.text.TextUtils.TruncateAt.END)
            
            canvas.drawText(elidedNames.toString(), margin + prefixWidth, yPosition + 14, paint)
            yPosition += 40
        } else {
            yPosition += 10
        }


        // --- Itinerary Items ---
        paint.textSize = 18f
        paint.color = Color.BLACK
        paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
        canvas.drawText("Itinerary", margin, yPosition + 18, paint)
        yPosition += 30

        // Draw line separator
        paint.color = Color.LTGRAY
        paint.strokeWidth = 1f
        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, paint)
        yPosition += 20

        // Sorting items by start time
        val sortedItems = items.sortedBy { parseIsoDate(it.startTime)?.time ?: Long.MAX_VALUE }

        paint.textSize = 12f
        paint.color = Color.BLACK
        
        var lastDateStr = ""
        
        for (item in sortedItems) {
            val itemDate = parseIsoDate(item.startTime)
            val dateStr = itemDate?.let { itemDateFormat.format(it) } ?: ""
            
             // Date Header
            if (dateStr != lastDateStr) {
                lastDateStr = dateStr
                yPosition += 10
                paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
                paint.textSize = 14f
                paint.color = Color.BLACK 
                canvas.drawText(dateStr, margin, yPosition + 14, paint)
                yPosition += 20
                
                // Draw separator
                paint.strokeWidth = 0.5f
                paint.color = Color.LTGRAY
                canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, paint)
                yPosition += 10
            }

            // Check for page break (approximate)
            if (yPosition > pageInfo.pageHeight - margin - 60) {
               // Page break logic omitted for MVP
            }

            paint.textSize = 12f
            paint.typeface = android.graphics.Typeface.DEFAULT_BOLD
            paint.color = Color.BLACK
            
            // --- Left Column (Time) ---
            val timeStr = itemDate?.let { timeFormat.format(it) } ?: ""
            canvas.drawText(timeStr, margin, yPosition + 12, paint)
            
            // --- Right Column (Content) ---
            val contentX = margin + 60f
            
            // Title
            canvas.drawText(item.title, contentX, yPosition + 12, paint)
            yPosition += 16

            paint.typeface = android.graphics.Typeface.DEFAULT
            
            // Location
            if (item.locationName?.isNotBlank() == true) {
                paint.color = Color.GRAY
                paint.textSize = 10f
                canvas.drawText(item.locationName, contentX, yPosition + 10, paint)
                yPosition += 14
                paint.color = Color.BLACK
                paint.textSize = 12f
            }
            
            // Flight / Transport Details from JSON
            if (item.providerDetailsJson?.isNotBlank() == true) {
                try {
                    val details = Json.parseToJsonElement(item.providerDetailsJson).jsonObject
                    val detailLines = mutableListOf<String>()
                    
                    details["flight_number"]?.jsonPrimitive?.content?.let { detailLines.add("Flight: $it") }
                    details["train_number"]?.jsonPrimitive?.content?.let { detailLines.add("Train: $it") }
                    details["gate"]?.jsonPrimitive?.content?.let { detailLines.add("Gate: $it") }
                    details["terminal"]?.jsonPrimitive?.content?.let { detailLines.add("Terminal: $it") }
                    details["platform"]?.jsonPrimitive?.content?.let { detailLines.add("Platform: $it") }
                    details["seat"]?.jsonPrimitive?.content?.let { detailLines.add("Seat: $it") }
                    
                    if (detailLines.isNotEmpty()) {
                        paint.color = Color.BLUE
                        paint.textSize = 10f
                        canvas.drawText(detailLines.joinToString(" | "), contentX, yPosition + 10, paint)
                        yPosition += 14
                        paint.color = Color.BLACK
                        paint.textSize = 12f
                    }
                } catch (e: Exception) {
                    // Ignore parsing errors
                }
            }
            
            // Notes/Description
            if (item.description?.isNotBlank() == true) {
                paint.color = Color.DKGRAY
                paint.textSize = 10f
                val noteLines = wrapText(item.description, paint, pageWidth - contentX - margin)
                for (line in noteLines.take(3)) {
                    canvas.drawText(line, contentX, yPosition + 10, paint)
                    yPosition += 12
                }
                paint.color = Color.BLACK
                paint.textSize = 12f
            }
            
            yPosition += 10
        }

        pdfDocument.finishPage(page)

        // Save file
        val file = File(context.cacheDir, "Trip_${tripId}_Itinerary.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            pdfDocument.close()
        }
    }

    private fun shareFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val shareIntent = Intent.createChooser(intent, "Share Itinerary PDF")
        // Warning: Calling startActivity from ViewModel is not ideal pattern usually, 
        // but passing Context into method for this specific action is acceptable for MVP.
        // Better pattern: Expose LiveData/Flow<Event> to UI which handles the intent.
        // For simplicity in this codebase we'll use the context passed in.
        try {
            // We need to start activity from a context. If it's Application context we need FLAG_ACTIVITY_NEW_TASK
            // Ideally this is called from Activity/Fragment context.
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "No app found to share PDF", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseIsoDate(dateString: String?): Date? {
        if (dateString.isNullOrBlank()) return null
        return try {
            // Try strictly ISO 8601 first (with time)
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(dateString)
        } catch (e: Exception) {
            try {
                // Fallback to simple date (yyyy-MM-dd)
                SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateString)
            } catch (e2: Exception) {
                null
            }
        }
    }
    
    /**
     * Helper function to wrap text into multiple lines
     */
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()
        
        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            if (paint.measureText(testLine) <= maxWidth) {
                currentLine = StringBuilder(testLine)
            } else {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
