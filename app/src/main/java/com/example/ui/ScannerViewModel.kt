package com.example.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GeminiClient
import com.example.data.ScanRecord
import com.example.data.ScanRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanUiState {
    object Idle : ScanUiState
    object Processing : ScanUiState
    data class Success(val record: ScanRecord) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ScanRepository
    val allRecords: StateFlow<List<ScanRecord>>
    val favoriteRecords: StateFlow<List<ScanRecord>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ScanRepository(database.scanRecordDao)
        allRecords = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
        favoriteRecords = repository.favoriteRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    // UI state for active scan session
    var activeState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
        private set

    // Active record for the detail screen
    var activeRecord = MutableStateFlow<ScanRecord?>(null)

    // Notification states
    private val _notification = MutableSharedFlow<String>()
    val notification: SharedFlow<String> = _notification

    /**
     * Parse raw scan content and insert to local DB, and update UI state.
     */
    fun processScanResult(rawContent: String) {
        if (rawContent.isBlank()) {
            activeState.value = ScanUiState.Error("Empy QR code parsed.")
            return
        }

        activeState.value = ScanUiState.Processing
        triggerVibration()

        viewModelScope.launch {
            val contentType = detectContentType(rawContent)
            val title = extractTitlePlaceholder(rawContent, contentType)
            
            val newRecord = ScanRecord(
                rawContent = rawContent,
                contentType = contentType,
                timestamp = System.currentTimeMillis(),
                isFavorite = false,
                title = title
            )

            try {
                val generatedId = withContext(Dispatchers.IO) {
                    repository.insertRecord(newRecord)
                }
                
                // Keep record with DB ID
                val savedRecord = newRecord.copy(id = generatedId)
                activeRecord.value = savedRecord
                activeState.value = ScanUiState.Success(savedRecord)
                _notification.emit("Successfully decoded QR Code!")

                // Auto summarize with AI in background
                queryAISummaryForRecord(savedRecord)

            } catch (e: Exception) {
                activeState.value = ScanUiState.Error("Database Save Failed: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Query Gemini AI summary for a selected record and update DB.
     */
    fun queryAISummaryForRecord(record: ScanRecord) {
        viewModelScope.launch {
            _notification.emit("Analyzing scan with AI Lens...")
            val aiResponse = withContext(Dispatchers.IO) {
                GeminiClient.queryGeminiAI(record.rawContent, record.contentType)
            }
            
            val updatedRecord = record.copy(aiSummary = aiResponse)
            if (activeRecord.value?.id == record.id) {
                activeRecord.value = updatedRecord
            }
            
            withContext(Dispatchers.IO) {
                repository.updateRecord(updatedRecord)
            }
            _notification.emit("AI Analysis Completed!")
        }
    }

    /**
     * Bookmark or favorite a record.
     */
    fun toggleFavorite(record: ScanRecord) {
        viewModelScope.launch {
            val updated = record.copy(isFavorite = !record.isFavorite)
            if (activeRecord.value?.id == record.id) {
                activeRecord.value = updated
            }
            withContext(Dispatchers.IO) {
                repository.updateRecord(updated)
            }
            val word = if (updated.isFavorite) "Bookmarked" else "Removed from Bookmarks"
            _notification.emit(word)
        }
    }

    /**
     * Delete a single record.
     */
    fun deleteRecord(record: ScanRecord) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.deleteRecordById(record.id)
            }
            if (activeRecord.value?.id == record.id) {
                activeRecord.value = null
            }
            _notification.emit("Scan deleted successfully.")
        }
    }

    /**
     * Clear absolute scan history.
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                repository.clearAllRecords()
            }
            activeRecord.value = null
            _notification.emit("Cleared Scan History.")
        }
    }

    /**
     * Closes the active sheet and returns to scanner.
     */
    fun resetScannerState() {
        activeState.value = ScanUiState.Idle
    }

    /**
     * Haptic vibration feedback for scanning.
     */
    private fun triggerVibration() {
        val context = getApplication<Application>().applicationContext
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun detectContentType(raw: String): String {
            val trimmed = raw.trim()
            return when {
                trimmed.startsWith("http://", ignoreCase = true) || 
                trimmed.startsWith("https://", ignoreCase = true) || 
                trimmed.startsWith("www.", ignoreCase = true) -> {
                    val lower = trimmed.lowercase()
                    when {
                        lower.endsWith(".png") || lower.endsWith(".jpg") || 
                        lower.endsWith(".jpeg") || lower.endsWith(".webp") || 
                        lower.endsWith(".gif") -> "IMAGE"
                        
                        lower.endsWith(".pdf") || lower.endsWith(".epub") -> "PDF"
                        
                        lower.contains("youtube.com") || lower.contains("youtu.be") || 
                        lower.contains("vimeo.com") || lower.endsWith(".mp4") -> "VIDEO"
                        
                        else -> "URL"
                    }
                }
                trimmed.startsWith("WIFI:", ignoreCase = true) -> "WIFI"
                trimmed.startsWith("BEGIN:VCARD", ignoreCase = true) || 
                trimmed.startsWith("MECARD:", ignoreCase = true) -> "VCARD"
                
                trimmed.startsWith("geo:", ignoreCase = true) || 
                trimmed.contains("google.com/maps", ignoreCase = true) -> "LOCATION"
                
                trimmed.startsWith("upi:", ignoreCase = true) || 
                trimmed.startsWith("bitcoin:", ignoreCase = true) || 
                trimmed.startsWith("ethereum:", ignoreCase = true) || 
                trimmed.contains("paypal.me", ignoreCase = true) -> "PAYMENT"
                
                trimmed.startsWith("mailto:", ignoreCase = true) -> "EMAIL"
                trimmed.startsWith("sms:", ignoreCase = true) || 
                trimmed.startsWith("smsto:", ignoreCase = true) -> "SMS"
                
                trimmed.startsWith("tel:", ignoreCase = true) -> "PHONE"
                
                else -> "TEXT"
            }
        }

        private fun extractTitlePlaceholder(raw: String, type: String): String {
            return when (type) {
                "URL", "IMAGE", "VIDEO", "PDF" -> {
                    try {
                        val temp = raw.replaceFirst(Regex("https?://(www\\.)?"), "")
                        val domain = temp.split("/").firstOrNull() ?: raw
                        "Web Resource: $domain"
                    } catch (e: Exception) {
                        "Web Asset"
                    }
                }
                "WIFI" -> {
                    val ssidPart = raw.split(";").firstOrNull { it.startsWith("WIFI:S:", ignoreCase = true) || it.startsWith("S:", ignoreCase = true) }
                    val ssid = ssidPart?.replace("WIFI:S:", "", ignoreCase = true)?.replace("S:", "", ignoreCase = true) ?: "Unknown Hotspot"
                    "WiFi Hotspot: $ssid"
                }
                "VCARD" -> {
                    val fnPart = raw.lines().firstOrNull { it.startsWith("FN:", ignoreCase = true) }
                    val name = fnPart?.replace("FN:", "", ignoreCase = true) ?: "VCard Contact"
                    "Contact Profile: $name"
                }
                "LOCATION" -> "Geographic Landmark"
                "PAYMENT" -> "Financial QR Destination"
                "EMAIL" -> "Email Address"
                "SMS" -> "SMS Destination"
                "PHONE" -> "Phone Line Contact"
                else -> {
                    if (raw.length > 25) {
                        raw.take(22) + "..."
                    } else {
                        raw
                    }
                }
            }
        }
    }
}
