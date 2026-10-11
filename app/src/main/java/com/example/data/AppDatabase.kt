package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [UserProgress::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        const val DB_NAME = "azkar_db"
        private const val LEGACY_DB_NAME = "azkar_database"

        /**
         * هجرة فارغة: date هو PrimaryKey فلا حاجة لفهرس إضافي.
         * إنشاء فهرس غير مُعرَّف في الـ Entity كان يرمي IllegalStateException بعد الترقية.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // no-op — يرفع رقم الإصدار دون تغيير المخطط
            }
        }

        /** المرحلة 4: تتبع إتمام أذكار النوم. الصفوف القديمة تأخذ القيمة false. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_progress ADD COLUMN completedSleep INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val app = context.applicationContext
                    migrateLegacyDatabaseIfNeeded(app)
                    Room.databaseBuilder(app, AppDatabase::class.java, DB_NAME)
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }

        private fun migrateLegacyDatabaseIfNeeded(context: Context) {
            try {
                val legacy = context.getDatabasePath(LEGACY_DB_NAME)
                val current = context.getDatabasePath(DB_NAME)
                if (legacy.exists() && !current.exists()) {
                    legacy.copyTo(current, overwrite = false)
                    File(legacy.path + "-wal").takeIf { it.exists() }?.copyTo(File(current.path + "-wal"), false)
                    File(legacy.path + "-shm").takeIf { it.exists() }?.copyTo(File(current.path + "-shm"), false)
                    context.deleteDatabase(LEGACY_DB_NAME)
                    Log.i("AppDatabase", "Migrated legacy $LEGACY_DB_NAME → $DB_NAME")
                } else if (legacy.exists() && current.exists()) {
                    context.deleteDatabase(LEGACY_DB_NAME)
                    Log.i("AppDatabase", "Removed leftover legacy $LEGACY_DB_NAME")
                }
            } catch (e: Exception) {
                Log.w("AppDatabase", "Legacy DB migration skipped", e)
            }
        }
    }
}
