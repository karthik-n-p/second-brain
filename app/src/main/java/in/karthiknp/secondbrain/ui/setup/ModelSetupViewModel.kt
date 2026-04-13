package `in`.karthiknp.secondbrain.ui.setup

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import `in`.karthiknp.secondbrain.ai.LlamaEngine
import `in`.karthiknp.secondbrain.ai.ModelDownloader
import `in`.karthiknp.secondbrain.ai.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Model Setup screen.
 * Manages model download lifecycle and device compatibility checks.
 */
class ModelSetupViewModel(application: Application) : AndroidViewModel(application) {

    private val llmEngine = LlamaEngine.getInstance()
    private val modelDownloader = ModelDownloader()

    // ─── State ───────────────────────────────────────────────────────

    val downloadState: StateFlow<ModelDownloader.DownloadState> = modelDownloader.downloadState
    val modelState: StateFlow<LlamaEngine.ModelState> = llmEngine.modelState

    private val _selectedModel = MutableStateFlow(ModelInfo.DEFAULT)
    val selectedModel: StateFlow<ModelInfo> = _selectedModel.asStateFlow()

    val isModelDownloaded: Boolean get() = llmEngine.isModelDownloaded(getApplication())
    val hasEnoughMemory: Boolean get() = llmEngine.hasEnoughMemory(getApplication())

    // ─── Actions ─────────────────────────────────────────────────────

    /**
     * Start downloading the selected model.
     */
    fun startDownload() {
        viewModelScope.launch {
            val file = modelDownloader.downloadModel(
                context = getApplication(),
                modelInfo = _selectedModel.value
            )
            if (file != null) {
                // Model downloaded successfully — initialize the engine
                llmEngine.initialize(getApplication())
            }
        }
    }

    /**
     * Cancel an in-progress download.
     */
    fun cancelDownload() {
        modelDownloader.cancel()
    }

    /**
     * Import a model from a user-selected file (via SAF file picker).
     */
    fun importModel(uri: Uri) {
        viewModelScope.launch {
            val file = modelDownloader.importModel(
                context = getApplication(),
                sourceUri = uri
            )
            if (file != null) {
                llmEngine.initialize(getApplication())
            }
        }
    }

    /**
     * Reset download state to allow retrying.
     */
    fun resetDownload() {
        modelDownloader.reset()
    }

    /**
     * Re-attempt loading the engine (e.g., after user frees RAM).
     */
    fun retryInitialize() {
        viewModelScope.launch {
            llmEngine.initialize(getApplication())
        }
    }
}
