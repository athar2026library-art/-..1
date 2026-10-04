package com.example.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [UserProgress::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    companion object {
        /** الاسم الموحّد في الإنتاج. */
        const val DB_NAME = "azkar_db"
        /** اسم قديم محتمل من حزم سابقة — يُرحَّل لمرة واحدة إن وُجد. */
        private const val LEGACY_DB_NAME = "azkar_database"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_user_progress_date ON user_progress(date)"
                )
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
                        .addMigrations(MIGRATION_1_2)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }

        /**
         * إن وُجد ملف قاعدة قديم باسم azkar_database ولم تُنشأ azkar_db بعد،
         * ننسخ الملف مرة واحدة ثم نحذف القديم حتى لا يُفقد تقدم المستخدم.
         */
        private fun migrateLegacyDatabaseIfNeeded(context: Context) {
            try {
                val legacy = context.getDatabasePath(LEGACY_DB_NAME)
                val current = context.getDatabasePath(DB_NAME)
                if (legacy.exists() && !current.exists()) {
                    legacy.copyTo(current, overwrite = false)
                    // ملفات WAL/SHM إن وُجدت
                    File(legacy.path + "-wal").takeIf { it.exists() }?.copyTo(File(current.path + "-wal"), false)
                    File(legacy.path + "-shm").takeIf { it.exists() }?.copyTo(File(current.path + "-shm"), false)
                    context.deleteDatabase(LEGACY_DB_NAME)
                    Log.i("AppDatabase", "Migrated legacy $LEGACY_DB_NAME → $DB_NAME")
                } else if (legacy.exists() && current.exists()) {
                    // كلاهما موجود: لا نستبدل الحالي؛ نحذف القديم فقط بعد التأكد أنه أصغر/أقدم
                    context.deleteDatabase(LEGACY_DB_NAME)
                    Log.i("AppDatabase", "Removed leftover legacy $LEGACY_DB_NAME")
                }
            } catch (e: Exception) {
                Log.w("AppDatabase", "Legacy DB migration skipped", e)
            }
        }
    }
}
