package com.tvmime.tv.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tvmime.db.AppDatabase
import com.tvmime.repository.XtreamRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        android.util.Log.i("SyncWorker", "Background SyncWorker started")
        try {
            val database = AppDatabase.getInstance(applicationContext)
            val repository = XtreamRepository(database)
            
            val result = repository.syncActivePortals()
            if (result.isSuccess) {
                android.util.Log.i("SyncWorker", "Background SyncWorker completed successfully")
                Result.success()
            } else {
                android.util.Log.e("SyncWorker", "Background SyncWorker failed", result.exceptionOrNull())
                Result.retry()
            }
        } catch (e: Exception) {
            android.util.Log.e("SyncWorker", "Background SyncWorker crashed", e)
            Result.retry()
        }
    }
}
