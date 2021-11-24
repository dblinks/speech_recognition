import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef AvailabilityHandler = void Function(bool result);
typedef StringResultHandler = void Function(String text);

class SpeechRecognition {
  static const MethodChannel _channel = MethodChannel('speech_recognition');

  static final SpeechRecognition _speech = SpeechRecognition._internal();

  SpeechRecognition._internal() {
    _channel.setMethodCallHandler(_platformCallHandler);
  }

  factory SpeechRecognition() => _speech;

  AvailabilityHandler? availabilityHandler;

  StringResultHandler? currentLocaleHandler;
  StringResultHandler? recognitionResultHandler;

  VoidCallback? recognitionStartedHandler;

  VoidCallback? speechEndCallbackHandler;

  StringResultHandler? recognitionCompleteHandler;

  VoidCallback? errorHandler;

  /// ask for speech  recognizer permission
  Future activate() => _channel.invokeMethod("speech.activate");

  /// start listening
  Future listen({String? locale}) => _channel.invokeMethod("speech.listen", locale);

  /// cancel speech
  Future cancel() => _channel.invokeMethod("speech.cancel");

  /// stop listening
  Future stop() => _channel.invokeMethod("speech.stop");

  Future _platformCallHandler(MethodCall call) async {
    switch (call.method) {
      case "speech.onSpeechAvailability":
        availabilityHandler!(call.arguments);
        break;
      case "speech.onCurrentLocale":
        currentLocaleHandler!(call.arguments);
        break;
      case "speech.onSpeech":
        recognitionResultHandler!(call.arguments);
        break;
      case "speech.endOfSpeech":
        speechEndCallbackHandler!();
        break;
      case "speech.onRecognitionStarted":
        recognitionStartedHandler!();
        break;
      case "speech.onRecognitionComplete":
        recognitionCompleteHandler!(call.arguments);
        break;
      case "speech.onError":
        errorHandler!();
        break;
      default:
        debugPrint('Unknowm method ${call.method} ');
    }
  }

  // define a method to handle availability / permission result
  void setAvailabilityHandler(AvailabilityHandler handler) => availabilityHandler = handler;

  // define a method to handle recognition result
  void setRecognitionResultHandler(StringResultHandler handler) => recognitionResultHandler = handler;

  // define a method to handle native call
  void setRecognitionStartedHandler(VoidCallback handler) => recognitionStartedHandler = handler;

  // define a method to handle native call
  void setRecognitionCompleteHandler(StringResultHandler handler) => recognitionCompleteHandler = handler;

  void setSpeechEndCallbackHandler(VoidCallback handler) => speechEndCallbackHandler = handler;

  void setCurrentLocaleHandler(StringResultHandler handler) => currentLocaleHandler = handler;

  void setErrorHandler(VoidCallback handler) => errorHandler = handler;
}
