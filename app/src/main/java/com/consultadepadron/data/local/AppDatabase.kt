package com.consultadepadron.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos 100% interna del dispositivo: `padron_interno.db` vive en el
 * almacenamiento privado de la app y funciona sin conexión.
 */
@Database(
    entities = [TrabajadorEntity::class, PeriodoEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trabajadorDao(): TrabajadorDao

    companion object {
        const val NOMBRE_BASE = "padron_interno.db"

        fun crear(context: Context): AppDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                NOMBRE_BASE
            )
                // WAL: las consultas del buscador siguen respondiendo durante la importación.
                .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
                .build()
    }
}