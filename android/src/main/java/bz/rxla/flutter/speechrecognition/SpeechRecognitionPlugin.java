package bz.rxla.flutter.speechrecognition;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.content.res.Configuration;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

import java.util.ArrayList;
import java.util.Locale;

/**
 * SpeechRecognitionPlugin
 */
public class SpeechRecognitionPlugin implements MethodCallHandler, RecognitionListener {

    private static final String LOG_TAG = "SpeechRecognitionPlugin";

    private SpeechRecognizer speech;
    private MethodChannel speechChannel;
    private String transcription = "";
    private Intent recognizerIntent;
    private Activity activity;
    private Boolean logger = false;

    /**
     * Plugin registration.
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), "speech_recognition");
        channel.setMethodCallHandler(new SpeechRecognitionPlugin(registrar.activity(), channel));
    }

    private SpeechRecognitionPlugin(Activity activity, MethodChannel channel) {
        this.speechChannel = channel;
        this.speechChannel.setMethodCallHandler(this);
        this.activity = activity;

        speech = SpeechRecognizer.createSpeechRecognizer(activity.getApplicationContext());
        speech.setRecognitionListener(this);

        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        switch (call.method) {
            case "speech.logger":
                logger = true;
                break;
            case "speech.activate":
                // FIXME => Dummy activation verification : we assume that speech recognition permission
                // is declared in the manifest and accepted during installation ( AndroidSDK 21- )
                Configuration config = activity.getResources().getConfiguration();
                Locale locale;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    locale = config.getLocales().get(0);
                } else {
                    locale = config.locale;
                }

                handleLog("Current Locale : " + locale.toString());
                speechChannel.invokeMethod("speech.onCurrentLocale", locale.toString());
                result.success(true);
                break;
            case "speech.listen":
                recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, getLocale(call.arguments.toString()));
                speech.startListening(recognizerIntent);
                result.success(true);
                break;
            case "speech.cancel":
                speech.cancel();
                result.success(false);
                break;
            case "speech.stop":
                speech.stopListening();
                result.success(true);
                break;
            case "speech.destroy":
                speech.cancel();
                speech.destroy();
                result.success(true);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    private Locale getLocale(String code) {
        String[] localeParts = code.split("_");
        return new Locale(localeParts[0], localeParts[1]);
    }

    public void handleLog(String log) {
        if (logger) {
            Log.d(LOG_TAG, log);
        }
    }

    @Override
    public void onReadyForSpeech(Bundle params) {
        handleLog("onReadyForSpeech");
        speechChannel.invokeMethod("speech.onSpeechAvailability", true);
    }

    @Override
    public void onBeginningOfSpeech() {
        handleLog("onRecognitionStarted");
        transcription = "";
        speechChannel.invokeMethod("speech.onRecognitionStarted", null);
    }

    @Override
    public void onRmsChanged(float rmsdB) {
        handleLog("onRmsChanged : " + rmsdB);
    }

    @Override
    public void onBufferReceived(byte[] buffer) {
        handleLog("onBufferReceived");
    }

    @Override
    public void onEndOfSpeech() {
        handleLog("onEndOfSpeech");
        speechChannel.invokeMethod("speech.endOfSpeech", transcription);
    }

    @Override
    public void onError(int error) {
        handleLog("onError : " + error);
        speechChannel.invokeMethod("speech.onSpeechAvailability", false);
        speechChannel.invokeMethod("speech.onError", error);
    }

    @Override
    public void onPartialResults(Bundle partialResults) {
        handleLog("onPartialResults...");
        ArrayList<String> matches = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            transcription = matches.get(0);
        }
        sendTranscription(false);
    }

    @Override
    public void onEvent(int eventType, Bundle params) {
        handleLog("onEvent : " + eventType);
    }

    @Override
    public void onResults(Bundle results) {
        handleLog("onResults...");
        ArrayList<String> matches = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null) {
            transcription = matches.get(0);
            handleLog("onResults -> " + transcription);
            sendTranscription(true);
        }
        sendTranscription(false);
    }

    private void sendTranscription(boolean isFinal) {
        speechChannel.invokeMethod(isFinal ? "speech.onRecognitionComplete" : "speech.onSpeech", transcription);
    }
}
