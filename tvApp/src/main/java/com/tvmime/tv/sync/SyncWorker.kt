package com.tvmime.tv.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvmime.db.AppDatabase
import com.tvmime.repository.XtreamRepository
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Napier.i("Background SyncWorker started")
        try {
            val database = AppDatabase.getDatabase(applicationContext)
            val repository = XtreamRepository(database)
            
            val result = repository.syncActivePortals()
            if (result.isSuccess) {
                Napier.i("Background SyncWorker completed successfully")
                Result.success()
            } else {
                Napier.e("Background SyncWorker failed", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            Napier.e("Background SyncWorker crashed", e)
            Result.retry()
        }
    }
}
