import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:speech_recognition/speech_recognition.dart';

void main() {
  runApp(const MyApp());
}

const languages = [
  Language('Português', 'pt_BR'),
  Language('Francais', 'fr_FR'),
  Language('English', 'en_US'),
  Language('Pусский', 'ru_RU'),
  Language('Italiano', 'it_IT'),
  Language('Español', 'es_ES'),
];

class Language {
  final String name;
  final String code;

  const Language(this.name, this.code);
}

class MyApp extends StatefulWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  SpeechRecognition? _speech;

  bool _speechRecognitionAvailable = false;
  bool _isListening = false;

  String transcription = '';

  //String _currentLocale = 'en_US';
  Language selectedLang = languages.first;

  @override
  void initState() {
    super.initState();

    Permission.microphone.request().then((PermissionStatus permissionStatus) => permissionStatus.isGranted ? activateSpeechRecognizer() : null);
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  void activateSpeechRecognizer() {
    _speech = SpeechRecognition();
    _speech?.setAvailabilityHandler(onSpeechAvailability);
    _speech?.setCurrentLocaleHandler(onCurrentLocale);
    _speech?.setRecognitionStartedHandler(onRecognitionStarted);
    _speech?.setRecognitionResultHandler(onRecognitionResult);
    _speech?.setSpeechEndCallbackHandler(onSpeechEnd);

    _speech?.setErrorHandler(errorHandler);
    _speech?.activate().then((res) => setState(() => _speechRecognitionAvailable = res));
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Speech Recognition'),
          actions: [
            PopupMenuButton<Language>(
              onSelected: _selectLangHandler,
              itemBuilder: (BuildContext context) => _buildLanguagesWidgets,
            )
          ],
        ),
        body: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Expanded(child: Container(padding: const EdgeInsets.all(8.0), color: Colors.grey.shade200, child: Text(transcription))),
                  _buildButton(
                    onPressed: _speechRecognitionAvailable && !_isListening ? () => start() : null,
                    label: _isListening ? 'Listening...' : 'Listen (${selectedLang.code})',
                  ),
                  _buildButton(
                    onPressed: _isListening ? () => cancel() : null,
                    label: 'Cancel',
                  ),
                  _buildButton(
                    onPressed: _isListening ? () => stop() : null,
                    label: 'Stop',
                  ),
                ],
              ),
            )),
      ),
    );
  }

  List<CheckedPopupMenuItem<Language>> get _buildLanguagesWidgets => languages
      .map((l) => CheckedPopupMenuItem<Language>(
            value: l,
            checked: selectedLang == l,
            child: Text(l.name),
          ))
      .toList();

  void _selectLangHandler(Language lang) {
    setState(() => selectedLang = lang);
  }

  Widget _buildButton({required String label, VoidCallback? onPressed}) => Padding(
      padding: const EdgeInsets.all(12.0),
      child: TextButton(
        style: ButtonStyle(backgroundColor: MaterialStateColor.resolveWith((states) => Colors.cyan.shade600)),
        onPressed: onPressed,
        child: Text(
          label,
          style: const TextStyle(color: Colors.white),
        ),
      ));

  void start() => _speech?.listen(locale: selectedLang.code);

  void cancel() => _speech?.cancel().then((result) => setState(() => _isListening = result));

  void stop() => _speech?.stop().then((result) {
        setState(() => _isListening = result);
      });

  void onSpeechAvailability(bool result) => setState(() => _speechRecognitionAvailable = result);

  void onCurrentLocale(String locale) {
    setState(() => selectedLang = languages.firstWhere((l) => l.code == locale));
  }

  void onRecognitionStarted() => setState(() => _isListening = true);

  void onRecognitionResult(String text) => setState(() => transcription = text);

  void onRecognitionComplete() => setState(() => _isListening = false);

  void errorHandler() => activateSpeechRecognizer();

  void onSpeechEnd() {
    setState(() {
      _isListening = false;
    });
    print("Speech endede");
  }
}
