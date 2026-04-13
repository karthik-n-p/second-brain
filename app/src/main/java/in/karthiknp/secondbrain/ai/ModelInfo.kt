package `in`.karthiknp.secondbrain.ai

/**
 * Describes an available LLM model that can be downloaded and used for inference.
 *
 * @param id Unique identifier for this model variant
 * @param displayName Human-readable name shown in UI
 * @param fileName The GGUF file name stored on disk
 * @param downloadUrl Direct download URL (no auth required)
 * @param fileSizeBytes Expected file size for validation
 * @param minRamMb Minimum device RAM in MB required to load this model
 * @param description Short description for the user
 */
data class ModelInfo(
    val id: String,
    val displayName: String,
    val fileName: String,
    val downloadUrl: String,
    val fileSizeBytes: Long,
    val minRamMb: Int,
    val description: String
) {
    companion object {
        /**
         * Primary recommended model: Qwen2.5 1.5B Instruct (Q4_K_M quantization)
         * - Apache 2.0 license (no gating, no login required)
         * - ~1 GB download
         * - Excellent instruction-following at 1.5B scale
         * - Direct download from HuggingFace
         */
        val QWEN_2_5_1_5B = ModelInfo(
            id = "qwen2.5-1.5b-q4km",
            displayName = "Qwen 2.5 · 1.5B",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            fileSizeBytes = 1_050_000_000L, // ~1 GB
            minRamMb = 1500,
            description = "Compact & fast. Great for tasks, ideas, and quick responses."
        )

        /** All available models for the user to choose from */
        val AVAILABLE_MODELS = listOf(QWEN_2_5_1_5B)

        /** The default model to use */
        val DEFAULT = QWEN_2_5_1_5B
    }
}
