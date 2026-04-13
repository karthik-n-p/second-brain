package `in`.karthiknp.secondbrain.ai

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * Robust HTTP model downloader with:
 * - Progress tracking via StateFlow
 * - Resumable downloads (HTTP Range headers)
 * - Temporary file strategy (prevents corrupt partial loads)
 * - Retry mechanism with exponential backoff
 * - File size validation
 * - Cancellation support
 */
class ModelDownloader {

    companion object {
        private const val TAG = "ModelDownloader"
        private const val BUFFER_SIZE = 8 * 1024 // 8 KB buffer
        private const val MAX_RETRIES = 3
    }

    // ─── Download State ──────────────────────────────────────────────

    sealed class DownloadState {
        data object Idle : DownloadState()
        data class Downloading(
            val bytesDownloaded: Long,
            val totalBytes: Long,
            val speedBytesPerSec: Long = 0
        ) : DownloadState() {
            val progressPercent: Int
                get() = if (totalBytes > 0) ((bytesDownloaded * 100) / totalBytes).toInt() else 0
            val downloadedMb: Long get() = bytesDownloaded / (1024 * 1024)
            val totalMb: Long get() = totalBytes / (1024 * 1024)
        }
        data object Verifying : DownloadState()
        data class Completed(val file: File) : DownloadState()
        data class Failed(val message: String) : DownloadState()
        data object Cancelled : DownloadState()
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    @Volatile
    private var isCancelled = false

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    // ─── Public API ──────────────────────────────────────────────────

    /**
     * Download a model file to the app's internal storage.
     * The file is first written to a .tmp file, then renamed on success.
     */
    suspend fun downloadModel(
        context: Context,
        modelInfo: ModelInfo
    ): File? = withContext(Dispatchers.IO) {
        isCancelled = false

        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, modelInfo.fileName)
        val tmpFile = File(modelsDir, "${modelInfo.fileName}.tmp")

        // If model already exists and is valid, return it
        if (targetFile.exists() && targetFile.length() > 100L * 1024 * 1024) {
            _downloadState.value = DownloadState.Completed(targetFile)
            return@withContext targetFile
        }

        var lastException: Exception? = null

        for (attempt in 1..MAX_RETRIES) {
            if (isCancelled) {
                _downloadState.value = DownloadState.Cancelled
                tmpFile.delete()
                return@withContext null
            }

            try {
                val result = performDownload(modelInfo.downloadUrl, tmpFile)
                if (result) {
                    // Validate file size
                    _downloadState.value = DownloadState.Verifying
                    if (tmpFile.length() < 100L * 1024 * 1024) {
                        tmpFile.delete()
                        throw RuntimeException(
                            "Downloaded file too small (${tmpFile.length() / 1024} KB). " +
                            "Expected a ~1 GB model file."
                        )
                    }

                    // Rename tmp to final
                    if (targetFile.exists()) targetFile.delete()
                    if (tmpFile.renameTo(targetFile)) {
                        Log.i(TAG, "Model download complete: ${targetFile.absolutePath}")
                        _downloadState.value = DownloadState.Completed(targetFile)
                        return@withContext targetFile
                    } else {
                        throw RuntimeException("Failed to rename temp file to final destination")
                    }
                }
            } catch (e: Exception) {
                lastException = e
                Log.e(TAG, "Download attempt $attempt failed", e)

                if (attempt < MAX_RETRIES && !isCancelled) {
                    // Exponential backoff: 2s, 4s, 8s
                    val delayMs = (1L shl attempt) * 1000L
                    Log.i(TAG, "Retrying in ${delayMs / 1000}s...")
                    kotlinx.coroutines.delay(delayMs)
                }
            }
        }

        tmpFile.delete()
        _downloadState.value = DownloadState.Failed(
            lastException?.message ?: "Download failed after $MAX_RETRIES attempts"
        )
        return@withContext null
    }

    /**
     * Cancel an in-progress download.
     */
    fun cancel() {
        isCancelled = true
    }

    /**
     * Reset the download state back to idle.
     */
    fun reset() {
        _downloadState.value = DownloadState.Idle
        isCancelled = false
    }

    // ─── Internal ────────────────────────────────────────────────────

    /**
     * Perform the actual HTTP download with progress tracking.
     * Supports resuming from partial downloads via Range header.
     */
    private fun performDownload(url: String, outputFile: File): Boolean {
        val existingBytes = if (outputFile.exists()) outputFile.length() else 0L

        val requestBuilder = Request.Builder().url(url)

        // Resume support: if we already have partial data, request the remainder
        if (existingBytes > 0) {
            requestBuilder.addHeader("Range", "bytes=$existingBytes-")
            Log.i(TAG, "Resuming download from byte $existingBytes")
        }

        val request = requestBuilder.build()
        val response = httpClient.newCall(request).execute()

        if (!response.isSuccessful && response.code != 206) {
            response.close()
            throw RuntimeException("HTTP error ${response.code}: ${response.message}")
        }

        val responseBody = response.body ?: run {
            response.close()
            throw RuntimeException("Empty response body")
        }

        val contentLength = responseBody.contentLength()
        val totalBytes = if (response.code == 206) {
            // Partial content: total = existing + remaining
            existingBytes + contentLength
        } else {
            contentLength
        }

        val append = response.code == 206
        var bytesDownloaded = if (append) existingBytes else 0L
        var lastSpeedCheck = System.currentTimeMillis()
        var lastSpeedBytes = bytesDownloaded

        FileOutputStream(outputFile, append).use { fos ->
            responseBody.byteStream().use { inputStream ->
                val buffer = ByteArray(BUFFER_SIZE)
                var bytesRead: Int

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    if (isCancelled) {
                        return false
                    }

                    fos.write(buffer, 0, bytesRead)
                    bytesDownloaded += bytesRead

                    // Calculate speed every 500ms
                    val now = System.currentTimeMillis()
                    val elapsed = now - lastSpeedCheck
                    val speed = if (elapsed > 500) {
                        val bytesInPeriod = bytesDownloaded - lastSpeedBytes
                        val speedBps = (bytesInPeriod * 1000) / elapsed
                        lastSpeedCheck = now
                        lastSpeedBytes = bytesDownloaded
                        speedBps
                    } else {
                        (_downloadState.value as? DownloadState.Downloading)?.speedBytesPerSec ?: 0
                    }

                    _downloadState.value = DownloadState.Downloading(
                        bytesDownloaded = bytesDownloaded,
                        totalBytes = totalBytes,
                        speedBytesPerSec = speed
                    )
                }
            }
        }

        response.close()
        return true
    }

    // ─── Import from File ────────────────────────────────────────────

    /**
     * Import a model from a user-selected file (via SAF file picker).
     * Copies the file to internal storage.
     */
    suspend fun importModel(
        context: Context,
        sourceUri: android.net.Uri,
        fileName: String = ModelInfo.DEFAULT.fileName
    ): File? = withContext(Dispatchers.IO) {
        val modelsDir = File(context.filesDir, "models")
        if (!modelsDir.exists()) modelsDir.mkdirs()

        val targetFile = File(modelsDir, fileName)
        val tmpFile = File(modelsDir, "${fileName}.tmp")

        try {
            _downloadState.value = DownloadState.Downloading(0, 0)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tmpFile).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    var bytesRead: Int
                    var totalCopied = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalCopied += bytesRead
                        _downloadState.value = DownloadState.Downloading(totalCopied, 0)
                    }
                }
            }

            _downloadState.value = DownloadState.Verifying

            if (tmpFile.length() < 100L * 1024 * 1024) {
                tmpFile.delete()
                _downloadState.value = DownloadState.Failed(
                    "File too small (${tmpFile.length() / 1024} KB). Please select a valid GGUF model file."
                )
                return@withContext null
            }

            if (targetFile.exists()) targetFile.delete()
            tmpFile.renameTo(targetFile)
            _downloadState.value = DownloadState.Completed(targetFile)
            Log.i(TAG, "Model imported: ${targetFile.absolutePath}")
            return@withContext targetFile
        } catch (e: Exception) {
            tmpFile.delete()
            _downloadState.value = DownloadState.Failed(e.message ?: "Import failed")
            Log.e(TAG, "Model import failed", e)
            return@withContext null
        }
    }
}
