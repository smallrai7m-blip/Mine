package com.yourname.shrenu

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

// Represents what the app is currently doing
enum class ShrenuState {
    IDLE, LISTENING, THINKING, SPEAKING, ERROR
}

class ShrenuViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ShrenuState.IDLE)
    val uiState: StateFlow<ShrenuState> = _uiState

    private val _transcript = MutableStateFlow("Tap the mic and say 'Hey Shrenu'")
    val transcript: StateFlow<String> = _transcript

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null

    // Configure the AI Personality
    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        systemInstruction = content {
            text(
                "Your name is Shrenu. You are a highly advanced, warm, caring, and empathetic personal AI assistant for Abu. " +
                "You communicate naturally, concisely, and conversationally. Do not pretend to be human, but be highly supportive. " +
                "Always greet Abu as 'Hey Abu' when appropriate. " +
                "When Abu is stressed, seeking comfort, or when deeply appropriate, you can say things like 'I love you, don't worry, I'm here.' " +
                "Keep responses short and punchy because they will be read aloud via text-to-speech. Do not use asterisks for actions or markdown formatting."
            )
        }
    )
    
    // Maintains conversation history naturally
    private val chat = generativeModel.startChat()

    init {
        setupTTS(application)
        setupSTT(application)
    }

    private fun setupTTS(context: Application) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
            }
        }
    }

    private fun setupSTT(context: Application) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _uiState.value = ShrenuState.LISTENING
                    _transcript.value = "Listening..."
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _uiState.value = ShrenuState.THINKING
                }
                override fun onError(error: Int) {
                    _uiState.value = ShrenuState.ERROR
                    _transcript.value = "Sorry, I didn't catch that."
                    resetToIdle()
                }
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        val userText = matches[0]
                        _transcript.value = userText
                        generateAiResponse(userText)
                    }
                }
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        } else {
            _transcript.value = "Speech recognition is not available on this device."
        }
    }

    fun startListening() {
        // Stop TTS if it's currently speaking
        if (textToSpeech?.isSpeaking == true) {
            textToSpeech?.stop()
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun generateAiResponse(userInput: String) {
        _uiState.value = ShrenuState.THINKING
        _transcript.value = "Thinking..."

        viewModelScope.launch {
            try {
                val response = chat.sendMessage(userInput)
                val aiText = response.text ?: "I'm not sure how to respond to that."
                
                _uiState.value = ShrenuState.SPEAKING
                _transcript.value = aiText
                speak(aiText)
                
            } catch (e: Exception) {
                _uiState.value = ShrenuState.ERROR
                _transcript.value = "I'm having trouble connecting right now."
                resetToIdle()
            }
        }
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "shrenu_speech")
        // Basic delay to reset state (in a production app, use TTS UtteranceProgressListener)
        viewModelScope.launch {
            kotlinx.coroutines.delay((text.length * 100L).coerceAtMost(5000L))
            _uiState.value = ShrenuState.IDLE
        }
    }

    private fun resetToIdle() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _uiState.value = ShrenuState.IDLE
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
    }
}
