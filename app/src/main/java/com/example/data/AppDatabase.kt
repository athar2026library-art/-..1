package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [UserProgress::class], version = 2, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        const val DB_NAME = "azkar_db"

        /**
         * v1 -> v2: الفهرس على date يجب أن يكون UNIQUE ليطابق تعريف الكيان
         * (date أصلاً PRIMARY KEY؛ UNIQUE INDEX آمن).
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_user_progress_date ON user_progress(date)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_progress_completedSabah ON user_progress(completedSabah)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_user_progress_completedMasaa ON user_progress(completedMasaa)"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
