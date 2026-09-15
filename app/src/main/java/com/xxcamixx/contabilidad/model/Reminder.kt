package com.xxcamixx.contabilidad.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double = 0.0,
    val targetDateInMillis: Long,
    val isStore: Boolean = false,
    val country: String = "Colombia",
    val originMode: String = "PERSONAL"
)