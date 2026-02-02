package com.example.almuadhin.noor.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.almuadhin.noor.api.ChatApiClient
import com.example.almuadhin.noor.api.ChatStreamingClient
import com.example.almuadhin.noor.api.LlmRouter
import com.example.almuadhin.noor.api.ModelsApiClient
import com.example.almuadhin.noor.api.StreamEvent
import com.example.almuadhin.noor.config.ConfigManager
import com.example.almuadhin.noor.data.ApiProvider
import com.example.almuadhin.noor.data.Message
import com.example.almuadhin.noor.data.Conversation
import com.example.almuadhin.noor.data.MessageFile
import com.example.almuadhin.noor.utils.ImageProcessor
import com.example.almuadhin.noor.utils.MessagePreparer
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.UUID

class NoorViewModel : ViewModel() {

    private val TAG = "NoorViewModel"

    // UI State
    var conversations by mutableStateOf<List<Conversation>>(emptyList())
        private set

    var currentConversation by mutableStateOf<Conversation?>(null)
        private set

    var messages by mutableStateOf<List<Message>>(emptyList())
        private set

    var selectedModelId by mutableStateOf("omni")
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    // Available models
    var availableModels by mutableStateOf<List<ModelsApiClient.FetchedModel>>(emptyList())
        private set

    var isLoadingModels by mutableStateOf(false)
        private set

    // Files for multimodal
    var pendingFiles by mutableStateOf<List<MessageFile>>(emptyList())
        private set

    var isUploadingFile by mutableStateOf(false)
        private set

    // Streaming job
    private var streamingJob: Job? = null

    init {
        selectedModelId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
        fetchModels()
    }

    override fun onCleared() {
        super.onCleared()
        streamingJob?.cancel()
    }

    fun fetchModels() {
        viewModelScope.launch {
            isLoadingModels = true
            try {
                val models = ModelsApiClient.getAllModels()
                availableModels = models
                selectedModelId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
            } catch (e: Exception) {
                error = "فشل في جلب النماذج: ${e.message}"
            } finally {
                isLoadingModels = false
            }
        }
    }

    private fun isOmniRouter(): Boolean {
        val aliasId = ConfigManager.get(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, "omni")
        return selectedModelId == aliasId || selectedModelId.isBlank()
    }

    fun selectConversation(conversation: Conversation) {
        currentConversation = conversation
        messages = conversation.messages
    }

    fun newChat() {
        currentConversation = null
        messages = emptyList()
        error = null
        pendingFiles = emptyList()
    }

    fun deleteConversation(conversation: Conversation) {
        conversations = conversations.filter { it.id != conversation.id }
        if (currentConversation?.id == conversation.id) {
            newChat()
        }
    }

    fun selectModel(modelId: String) {
        Log.d(TAG, "Selecting model: $modelId")
        selectedModelId = modelId
        ConfigManager.set(ConfigManager.Keys.PUBLIC_LLM_ROUTER_ALIAS_ID, modelId)
    }

    fun sendMessage(messageText: String) {
        if (messageText.isBlank() && pendingFiles.isEmpty()) return

        val messageFiles = pendingFiles.toList()

        val userMessage = Message(
            id = System.currentTimeMillis().toString(),
            content = messageText,
            isUser = true,
            timestamp = System.currentTimeMillis(),
            files = messageFiles
        )
        messages = messages + userMessage
        pendingFiles = emptyList()

        if (currentConversation == null) {
            val newId = UUID.randomUUID().toString()
            val newConversation = Conversation(
                id = newId,
                title = messageText.take(30) + if (messageText.length > 30) "..." else "",
                messages = messages,
                timestamp = System.currentTimeMillis(),
                model = selectedModelId
            )
            currentConversation = newConversation
            conversations = listOf(newConversation) + conversations
        }

        val assistantMessageId = System.currentTimeMillis().toString()
        var assistantMessage = Message(
            id = assistantMessageId,
            content = "",
            isUser = false,
            timestamp = System.currentTimeMillis(),
            model = selectedModelId
        )
        messages = messages + assistantMessage

        val providerConfig = ConfigManager.getProviderConfig()
        if (!providerConfig.isValid()) {
            addSimulatedResponse()
            return
        }

        isLoading = true
        error = null

        streamingJob = viewModelScope.launch {
            try {
                val apiMessages = messages.dropLast(1).map { msg ->
                    MessagePreparer.ChatMessage(
                        role = if (msg.isUser) "user" else "assistant",
                        content = msg.content,
                        files = msg.files
                    )
                }

                val hasMultimodalContent = MessagePreparer.hasMultimodalContent(apiMessages)
                val providerIsGoogleDirect = providerConfig.provider == ApiProvider.GOOGLE_AI_STUDIO

                val modelToUse = if (isOmniRouter() && !providerIsGoogleDirect) {
                    val legacyMessages = apiMessages.map {
                        ChatApiClient.ChatMessage(it.role, it.content)
                    }
                    LlmRouter.selectModel(legacyMessages, hasMultimodalContent, false)
                } else {
                    selectedModelId
                }

                Log.i(TAG, "Starting stream with model: $modelToUse")

                ChatStreamingClient.chatCompletionStreamWithFiles(
                    messages = apiMessages,
                    model = modelToUse,
                    isMultimodal = hasMultimodalContent
                ).collect { event ->
                    when (event) {
                        is StreamEvent.Token -> {
                            assistantMessage = assistantMessage.copy(
                                content = assistantMessage.content + event.text
                            )
                            messages = messages.dropLast(1) + assistantMessage
                        }
                        is StreamEvent.Complete -> {
                            isLoading = false
                            updateCurrentConversation()
                        }
                        is StreamEvent.Error -> {
                            Log.e(TAG, "Stream error: ${event.error}")
                            error = event.error
                            isLoading = false
                            val errorMessage = Message(
                                id = assistantMessageId,
                                content = "⚠️ خطأ: ${event.error}",
                                isUser = false,
                                timestamp = System.currentTimeMillis()
                            )
                            messages = messages.dropLast(1) + errorMessage
                            updateCurrentConversation()
                        }
                        is StreamEvent.RouterMetadata -> {}
                        is StreamEvent.Status -> {}
                        is StreamEvent.KeepAlive -> {}
                        is StreamEvent.ToolCall -> {}
                        is StreamEvent.ToolCallsComplete -> {
                            isLoading = false
                            updateCurrentConversation()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error: ${e.message}", e)
                error = e.message
                isLoading = false
                val errorMessage = Message(
                    id = assistantMessageId,
                    content = "⚠️ خطأ: ${e.message}",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
                messages = messages.dropLast(1) + errorMessage
                updateCurrentConversation()
            }
        }
    }

    fun stopGeneration() {
        ChatStreamingClient.cancelCurrentStream()
        streamingJob?.cancel()
        streamingJob = null
        isLoading = false
        updateCurrentConversation()
    }

    fun regenerateLastMessage() {
        val lastUserMessage = messages.lastOrNull { it.isUser }
        if (lastUserMessage != null) {
            val lastMessage = messages.lastOrNull()
            if (lastMessage != null && !lastMessage.isUser) {
                messages = messages.dropLast(1)
            }
            sendMessage(lastUserMessage.content)
        }
    }

    private fun addSimulatedResponse() {
        viewModelScope.launch {
            isLoading = true
            kotlinx.coroutines.delay(1000)

            val aiResponse = Message(
                id = System.currentTimeMillis().toString(),
                content = buildString {
                    appendLine("👋 مرحباً! أنا **نور** - مساعدك الذكي.")
                    appendLine()
                    appendLine("⚠️ **لم يتم تكوين API**")
                    appendLine()
                    appendLine("لتفعيل الردود الحقيقية:")
                    appendLine("1. اذهب إلى **الإعدادات** ⚙️")
                    appendLine("2. اختر **إعدادات نور**")
                    appendLine("3. أدخل **HuggingFace Token** أو **Google AI Studio API Key**")
                    appendLine()
                    appendLine("المزودين المدعومين:")
                    appendLine("- **HuggingFace** (100+ نموذج)")
                    appendLine("- **Google AI Studio** (Gemini)")
                },
                isUser = false,
                timestamp = System.currentTimeMillis()
            )
            messages = messages + aiResponse
            updateCurrentConversation()
            isLoading = false
        }
    }

    private fun updateCurrentConversation() {
        currentConversation = currentConversation?.copy(messages = messages)
        currentConversation?.let { conv ->
            conversations = conversations.map { if (it.id == conv.id) conv else it }
        }
    }

    fun addPendingFile(file: MessageFile) {
        pendingFiles = pendingFiles + file
    }

    fun removePendingFile(file: MessageFile) {
        pendingFiles = pendingFiles.filter { it.name != file.name }
    }

    fun addImageFromUri(context: Context, uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            isUploadingFile = true
            try {
                val processedFile = ImageProcessor.processImageFromUri(
                    context = context,
                    uri = uri,
                    fileName = fileName,
                    mimeType = mimeType
                )

                if (processedFile != null) {
                    pendingFiles = pendingFiles + processedFile
                    Log.i(TAG, "Image added: ${processedFile.name}")
                } else {
                    error = "فشل في معالجة الصورة"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add image: ${e.message}", e)
                error = "فشل في إضافة الصورة: ${e.message}"
            } finally {
                isUploadingFile = false
            }
        }
    }

    fun addTextFileFromUri(context: Context, uri: Uri, fileName: String, mimeType: String) {
        viewModelScope.launch {
            isUploadingFile = true
            try {
                val file = MessageFile.fromUri(context, uri, fileName, mimeType)
                if (file != null) {
                    pendingFiles = pendingFiles + file
                    Log.i(TAG, "File added: ${file.name}")
                } else {
                    error = "فشل في قراءة الملف"
                }
            } catch (e: Exception) {
                error = "فشل في إضافة الملف: ${e.message}"
            } finally {
                isUploadingFile = false
            }
        }
    }
}
