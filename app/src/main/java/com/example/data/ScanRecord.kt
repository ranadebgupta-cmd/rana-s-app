package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "scan_records")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawContent: String,
    val contentType: String, // e.g., "URL", "TEXT", "WIFI", "VCARD", "EMAIL", "SMS", "LOCATION", "PAYMENT", "IMAGE", "VIDEO"
    val timestamp: Long,
    val isFavorite: Boolean = false,
    val aiSummary: String? = null,
    val title: String? = null,
    val faviconUrl: String? = null
) : Serializable
