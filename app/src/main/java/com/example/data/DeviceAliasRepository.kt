package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class DeviceAliasRepository(private val dao: DeviceAliasDao) {

    val allAliases: Flow<List<DeviceAliasEntity>> = dao.getAllAliases()

    fun getAlias(address: String): Flow<DeviceAliasEntity?> = dao.getAlias(address)

    suspend fun getAliasDirect(address: String): DeviceAliasEntity? = withContext(Dispatchers.IO) {
        dao.getAliasDirect(address)
    }

    suspend fun saveAlias(
        address: String,
        customName: String,
        notes: String = "",
        tagColorHex: String = "",
        isPinned: Boolean = false
    ) = withContext(Dispatchers.IO) {
        val entity = DeviceAliasEntity(
            deviceAddress = address,
            customName = customName.trim(),
            notes = notes.trim(),
            tagColorHex = tagColorHex,
            isPinned = isPinned,
            updatedAt = System.currentTimeMillis()
        )
        dao.insertAlias(entity)
    }

    suspend fun deleteAlias(address: String) = withContext(Dispatchers.IO) {
        dao.deleteAlias(address)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
