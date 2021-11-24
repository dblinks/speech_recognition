package bz.rxla.flutter.speech_recognition

import androidx.annotation.NonNull

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.app.Activity
import android.content.Context

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener

import android.speech.SpeechRecognizer
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.PluginRegistry
import android.speech.RecognizerIntent
import io.flutter.Log
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import java.util.*

/** SpeechRecognitionPlugin */
class SpeechRecognitionPlugin : FlutterPlugin, MethodCallHandler, RecognitionListener,
    ActivityAware {

    private val LOG_TAG = "SpeechRecognitionPlugin"


    private var transcription = ""
    private val logger = false

    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var speech: SpeechRecognizer
    private lateinit var recognizerIntent: Intent

    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var channel: MethodChannel

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        this.context = flutterPluginBinding.applicationContext;

        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "speech_recognition")
        channel.setMethodCallHandler(this)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "speech.activate" -> {
                val config = activity.resources.configuration
                val locale: Locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    config.locales[0]
                } else {
                    config.locale
                }
                channel.invokeMethod("speech.onCurrentLocale", locale.toString())
                result.success(true)
            }
            "speech.listen" -> {
                recognizerIntent.putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE,
                    getLocale(call.arguments.toString())
                );
                speech.startListening(recognizerIntent)
                result.success(true)
            }
            "speech.cancel" -> {
                speech.cancel()
                result.success(false)
            }
            "speech.stop" -> {
                speech.stopListening()
                result.success(true)
            }
            "speech.destroy" -> {
                speech.cancel()
                speech.destroy()
                result.success(true)
            }
            else -> Log.d(LOG_TAG, "Method ${call.method} not implemented")
        }
    }

    private fun initSpeech() {
        speech = SpeechRecognizer.createSpeechRecognizer(activity.applicationContext)
        speech.setRecognitionListener(this)

        recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        recognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onReadyForSpeech(p0: Bundle?) {
        channel.invokeMethod("speech.onSpeechAvailability", true);
    }

    override fun onBeginningOfSpeech() {
        transcription = ""
        channel.invokeMethod("speech.onRecognitionStarted", null)
    }

    override fun onRmsChanged(p0: Float) {}

    override fun onBufferReceived(p0: ByteArray?) {}

    override fun onEndOfSpeech() {
        channel.invokeMethod("speech.endOfSpeech", transcription);
    }

    override fun onError(error: Int) {
        channel.invokeMethod("speech.onSpeechAvailability", false)
        channel.invokeMethod("speech.onError", error)
    }

    override fun onResults(results: Bundle?) {
        val matches: ArrayList<String>? =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

        if (matches != null) {
            transcription = matches[0]
            sendTranscription(true)
        }

        sendTranscription(false)
    }

    override fun onPartialResults(results: Bundle?) {
        val matches: ArrayList<String>? =
            results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)


        if (matches != null) {
            transcription = matches[0]
            sendTranscription(true)
        }
    }

    override fun onEvent(p0: Int, p1: Bundle?) {
        TODO("Not yet implemented")
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity;

        initSpeech()
    }

    override fun onDetachedFromActivityForConfigChanges() {}

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {}

    override fun onDetachedFromActivity() {}

    private fun getLocale(code: String): Locale {
        val localeParts = code.split("_").toTypedArray()
        return Locale(localeParts[0], localeParts[1])
    }

    private fun sendTranscription(isFinal: Boolean) {
        channel.invokeMethod(
            if (isFinal) "speech.onRecognitionComplete" else "speech.onSpeech",
            transcription
        )
    }
}
