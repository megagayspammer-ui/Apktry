package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "device_aliases")
data class DeviceAliasEntity(
    @PrimaryKey
    val deviceAddress: String,
    val customName: String,
    val notes: String = "",
    val tagColorHex: String = "",
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
