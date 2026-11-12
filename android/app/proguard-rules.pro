# Universal Reader — R8 rules.
#
# JNI symbol names are derived from the Java class and method names, so both
# must survive shrinking and obfuscation.
-keep class com.universalreader.app.core.NativeBridge { *; }
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# The platform MediaSession/notification entry points are referenced from the
# manifest and from framework callbacks.
-keep class com.universalreader.app.tts.TtsPlaybackService { *; }
-keep class com.universalreader.app.MainActivity { *; }

# Readability.js is an asset, never shrunk. Keep the Kotlin JSON helper names
# used by unit tests and readable stack traces.
-keepattributes SourceFile,LineNumberTable
