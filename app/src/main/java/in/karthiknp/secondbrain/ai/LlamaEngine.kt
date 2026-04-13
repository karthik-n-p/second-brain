package `in`.karthiknp.secondbrain.ai

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * On-device LLM engine powered by llama.cpp via Llamatik.
 *
 * Uses Qwen2.5-1.5B (Q4_K_M quantized GGUF) running 100% on-device.
 * No internet required after model download.
 */
class LlamaEngine private constructor() {

    companion object {
        private const val TAG = "LlamaEngine"
        private const val MIN_MODEL_SIZE_BYTES = 100L * 1024 * 1024 // 100 MB

        @Volatile
        private var instance: LlamaEngine? = null

        fun getInstance(): LlamaEngine {
            return instance ?: synchronized(this) {
                instance ?: LlamaEngine().also { instance = it }
            }
        }
    }

    // ─── State Management ────────────────────────────────────────────

    sealed class ModelState {
        data object NotLoaded : ModelState()
        data object Loading : ModelState()
        data object Ready : ModelState()
        data class Error(val message: String) : ModelState()
        data object ModelNotFound : ModelState()
    }

    private val _modelState = MutableStateFlow<ModelState>(ModelState.NotLoaded)
    val modelState: StateFlow<ModelState> = _modelState.asStateFlow()

    val isReady: Boolean get() = _modelState.value is ModelState.Ready

    // ─── Model Management ────────────────────────────────────────────

    fun getModelDirectory(context: Context): File {
        return File(context.filesDir, "models")
    }

    /**
     * Find the first valid GGUF model file in the models directory.
     */
    fun getModelFile(context: Context): File? {
        val dir = getModelDirectory(context)
        if (!dir.exists()) return null
        return dir.listFiles()?.firstOrNull {
            it.name.endsWith(".gguf") && it.length() > MIN_MODEL_SIZE_BYTES
        }
    }

    fun isModelDownloaded(context: Context): Boolean {
        return getModelFile(context) != null
    }

    /**
     * Check if the device has enough free RAM to load the model.
     */
    fun hasEnoughMemory(context: Context, requiredMb: Int = ModelInfo.DEFAULT.minRamMb): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        val availableMb = memInfo.availMem / (1024 * 1024)
        return availableMb >= requiredMb
    }

    /**
     * Initialize the LLM engine. This is heavy and MUST run on a background thread.
     */
    suspend fun initialize(context: Context) {
        if (_modelState.value is ModelState.Ready) return

        _modelState.value = ModelState.Loading

        val modelFile = getModelFile(context)
        if (modelFile == null) {
            _modelState.value = ModelState.ModelNotFound
            Log.w(TAG, "No GGUF model found in: ${getModelDirectory(context).absolutePath}")
            return
        }

        // Memory check
        if (!hasEnoughMemory(context)) {
            _modelState.value = ModelState.Error(
                "Not enough free RAM to load the AI model. Close some apps and try again."
            )
            Log.e(TAG, "Insufficient memory to load model")
            return
        }

        try {
            withContext(Dispatchers.IO) {
                Log.i(TAG, "Loading model from: ${modelFile.absolutePath} (${modelFile.length() / 1024 / 1024} MB)")

                // Load model using Llamatik API
                val loaded = LlamaBridge.initGenerateModel(modelFile.absolutePath)
                if (!loaded) {
                    throw RuntimeException("LlamaBridge.initGenerateModel returned false")
                }
            }
            _modelState.value = ModelState.Ready
            Log.i(TAG, "LLM Engine initialized successfully with ${modelFile.name}")
        } catch (e: Exception) {
            _modelState.value = ModelState.Error(e.message ?: "Failed to load model")
            Log.e(TAG, "Failed to initialize LLM", e)
        }
    }

    /**
     * Release the LLM engine resources.
     */
    fun release() {
        try {
            if (_modelState.value is ModelState.Ready) {
                LlamaBridge.shutdown()
            }
            _modelState.value = ModelState.NotLoaded
            Log.i(TAG, "LLM Engine released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing LLM", e)
        }
    }

    // ─── Inference ───────────────────────────────────────────────────

    /**
     * Generate a response for the given prompt.
     * Returns null if the model isn't ready.
     */
    suspend fun generateResponse(prompt: String): String? {
        if (!isReady) return null

        return try {
            withContext(Dispatchers.IO) {
                // Use Llamatik's generate text wrapper
                LlamaBridge.generate(prompt)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inference failed", e)
            null
        }
    }
}
