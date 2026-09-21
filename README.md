# MAX

MAX is a Kotlin Android voice assistant with Hindi/Hinglish and English speech paths, online/offline brain routing, persistent Room memory, calls, notification reading, accessibility control, explicit camera capture, and reminders.

## Build

1. Install JDK 17 and Android SDK 35.
2. Copy `local.properties.example` to `local.properties` and set `llm_api_key`, `llm_api_url`, and `llm_model`. The key is read only during the Gradle build and is not committed.
3. Download `vosk-model-small-hi-0.22` and `vosk-model-small-en-us-0.15` from the Vosk model site. Put the complete extracted folders in `app/src/main/assets/models/`.
4. Download `qwen2.5-1.5b-instruct-q4_k_m.gguf`; copy it to the app private files directory as `qwen2.5-1.5b-instruct-q4_k_m.gguf`. A llama.cpp build exposing `llama_jni` is needed for real offline generation.
5. Run `./gradlew assembleDebug`; the GitHub workflow performs the same build with JDK 17.

## Important Android limits

Android does not expose a universal programmatic accept-any-call API on every OEM; MAX uses the requested TelecomManager calls where the device grants the declared role and permission. WhatsApp auto-reply only works when its notification exposes a RemoteInput action. Piper and llama.cpp require separately built native JNI libraries; MAX falls back to Android TextToSpeech and reports when the offline engine is unavailable. Photo capture uses Camera2 and video opens the visible system camera UI, both only after an explicit voice command and with a foreground notification.

## File guide

- `MainActivity.kt`: Hindi first-launch permission screen.
- `wakeword/`: foreground microphone service and Vosk wake loop.
- `stt/`: dual Hindi/English Vosk recognizer.
- `tts/`: Piper JNI bridge with Android TTS fallback.
- `brain/`: connectivity, online API, llama JNI bridge, and router.
- `memory/`: Room database for important facts and recent context.
- `calls/`: call screening announcement and call actions.
- `messages/`: SMS/WhatsApp notification speech and notification reply action.
- `control/`: accessibility gestures, screen text, torch, settings, and app launching.
- `camera/`: explicit-command camera foreground service.
- `reminders/`: AlarmManager and notification receiver.

## हर फ़ाइल का सरल परिचय

- `settings.gradle.kts`: यह Gradle को app module और repositories बताती है।
- `build.gradle.kts`: यह Android और Kotlin plugin versions को pin करती है।
- `app/build.gradle.kts`: यह app की SDK, dependencies और local API key fields तय करती है।
- `AndroidManifest.xml`: यह services और सभी Android permissions register करता है।
- `MainActivity.kt`: यह Hindi में पहली बार permissions और system settings खोलती है।
- `WakeWordService.kt`: यह persistent foreground microphone service है जो Hey Max सुनती है।
- `VoskRecognizer.kt`: यह Hindi और English Vosk results में बेहतर transcript चुनता है।
- `AssetInstaller.kt`: यह packaged Vosk model folders को private storage में copy करता है।
- `PiperTts.kt`: यह Piper female voice चलाता है और failure पर Android TTS उपयोग करता है।
- `Brain.kt`: यह internet देखकर online API या local llama.cpp engine चुनता है।
- `MemoryStore.kt`: यह Room में महत्वपूर्ण बातें और recent context रखता है।
- `MaxCallScreeningService.kt`: यह incoming caller का number voice में बताता है।
- `CallActions.kt`: यह granted Telecom permission के साथ call answer या reject करता है।
- `MessageNotificationService.kt`: यह SMS और WhatsApp notifications पढ़ता है और reply action भेजता है।
- `SmsReply.kt`: यह केवल explicit command के बाद SMS भेजने का API देता है।
- `MaxAccessibilityService.kt`: यह screen text, tap, scroll, back और home actions देता है।
- `PhoneControl.kt`: यह torch, settings और installed app launch नियंत्रित करता है।
- `CameraCaptureService.kt`: यह explicit photo या video command पर visible camera foreground flow चलाता है।
- `ReminderScheduler.kt`: यह AlarmManager से reminder notification schedule करता है।
- `CommandParser.kt`: यह Hindi और English voice phrases को MAX commands में बदलता है।
- `accessibility_service_config.xml`: यह accessibility capabilities Android को बताती है।
- `styles.xml`: यह MAX का Material theme तय करती है।
- `ic_stat_max.xml`: यह foreground notifications का सफेद MAX icon है।
- `build.yml`: यह GitHub Actions में JDK 17 से debug APK बनाती है।
- `local.properties.example`: यह local LLM configuration का सुरक्षित example है।
