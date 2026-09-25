package com.example

import android.app.Application
import com.example.data.DeviceAliasRepository
import com.example.data.RadarDatabase

class RadarApplication : Application() {

    lateinit var database: RadarDatabase
        private set

    lateinit var aliasRepository: DeviceAliasRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = RadarDatabase.getInstance(this)
        aliasRepository = DeviceAliasRepository(database.deviceAliasDao())
    }
}
