package com.consultadepadron

import android.app.Application
import com.consultadepadron.data.PadronLocalDataSource
import com.consultadepadron.data.PadronRepository
import com.consultadepadron.data.local.AppDatabase

/**
 * Contenedor de dependencias mínimo (service locator).
 * Todo se resuelve de forma perezosa: la base de datos no se abre hasta el primer uso.
 */
class ConsultaPadronApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.crear(this) }

    val repository: PadronRepository by lazy {
        PadronRepository(PadronLocalDataSource(database.trabajadorDao()))
    }
}