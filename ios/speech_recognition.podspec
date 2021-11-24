#
# To learn more about a Podspec see http://guides.cocoapods.org/syntax/podspec.html.
# Run `pod lib lint speech_recognition.podspec` to validate before publishing.
#
Pod::Spec.new do |s|
  s.name             = 'speech_recognition'
  s.version          = '0.3.0'
  s.summary          = 'A flutter plugin to use the speech recognition on iOS and Android'
  s.description      = <<-DESC
A flutter plugin to use the speech recognition on iOS and Android
                       DESC
  s.homepage         = 'https://github.com/rxlabz/speech_recognition'
  s.license          = { :file => '../LICENSE' }
  s.author           = { 'RX Labz' => 'rxlabz@gmail.com' }
  s.source           = { :path => '.' }
  s.source_files = 'Classes/**/*'
  s.dependency 'Flutter'
  s.platform = :ios, '8.0'

  # Flutter.framework does not contain a i386 slice.
  s.pod_target_xcconfig = { 'DEFINES_MODULE' => 'YES', 'EXCLUDED_ARCHS[sdk=iphonesimulator*]' => 'i386' }
  s.swift_version = '5.0'
end
