# Piper voice assets

Place the Piper medium or high quality Hindi and English model pairs here before building:

- `hi-IN-medium.onnx` and `hi-IN-medium.onnx.json`
- `en-US-medium.onnx` and `en-US-medium.onnx.json`

Higher quality models can use the same names with the matching Piper model files. The native `piper_jni` bridge receives the selected model path, config path, and the configured speaking rate. Keep these files local; they are not downloaded at runtime.