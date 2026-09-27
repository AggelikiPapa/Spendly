package com.spendly.data.local.database

import android.content.Context
import androidx.room.Room

object SpendlyDatabaseProvider {
    @Volatile
    private var instance: SpendlyDatabase? = null

    fun get(context: Context): SpendlyDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            SpendlyDatabase::class.java,
            "spendly.db",
        ).addCallback(DefaultCategorySeeder).build().also { instance = it }
    }
}
