package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DeviceAliasDao {
    @Query("SELECT * FROM device_aliases ORDER BY updatedAt DESC")
    fun getAllAliases(): Flow<List<DeviceAliasEntity>>

    @Query("SELECT * FROM device_aliases WHERE deviceAddress = :address LIMIT 1")
    fun getAlias(address: String): Flow<DeviceAliasEntity?>

    @Query("SELECT * FROM device_aliases WHERE deviceAddress = :address LIMIT 1")
    suspend fun getAliasDirect(address: String): DeviceAliasEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: DeviceAliasEntity)

    @Update
    suspend fun updateAlias(alias: DeviceAliasEntity)

    @Query("DELETE FROM device_aliases WHERE deviceAddress = :address")
    suspend fun deleteAlias(address: String)

    @Query("DELETE FROM device_aliases")
    suspend fun clearAll()
}
