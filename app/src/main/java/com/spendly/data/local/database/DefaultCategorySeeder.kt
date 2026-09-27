package com.spendly.data.local.database

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.spendly.domain.model.DefaultCategories

internal object DefaultCategorySeeder : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        DefaultCategories.names.forEach { name ->
            db.execSQL(
                "INSERT OR IGNORE INTO categories (name, isSystem, isActive) VALUES (?, 1, 1)",
                arrayOf(name),
            )
        }
    }
}
