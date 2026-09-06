package com.streamvault.app.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.io.File

object BackupFileBridge {
    const val MIME_TYPE_JSON = "application/json"

    data class BackupFileCandidate(
        val lastModifiedMs: Long,
        val fileName: String,
        val filePath: String,
        val sizeBytes: Long
    )

    fun rememberManagedExport(context: Context, uri: Uri?) {}
    fun listPickerFreeBackups(context: Context): List<BackupFileCandidate> = emptyList()
    fun createExportFile(context: Context): File = File("")
    fun createExportFile(dir: File): File = File("")
    fun providerUriForFile(context: Context, file: File?): Uri = Uri.EMPTY
    fun buildShareIntent(uri: Uri): Intent = Intent()
    fun createPickerFreeExportUri(context: Context): Uri = Uri.EMPTY
    fun finishPickerFreeExport(context: Context, uri: Uri, success: Boolean): Boolean = true
    fun listBackupFiles(dir: File): List<File> = emptyList()
    fun candidateForFile(file: File): BackupFileCandidate = BackupFileCandidate(0, "", "", 0)
    fun listManagedBackups(context: Context): List<BackupFileCandidate> = emptyList()
    fun deleteManagedBackup(context: Context, candidate: BackupFileCandidate): Boolean = true
}
