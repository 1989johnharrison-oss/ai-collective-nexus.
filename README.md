# AI Collective Nexus — Android controller

Native Android Java controller for a loopback Python multi-agent backend. **This is not yet standalone on-device AI inference or generative video.**

The APK is built by GitHub Actions and published as an artifact named `AI-Collective-Nexus-APK` containing `app-debug.apk`.

To use backend features, run the Python Nexus backend on a computer, enable USB debugging and run `adb reverse tcp:8765 tcp:8765`. The app talks only to `http://127.0.0.1:8765`.

This public repository contains no passwords or personal data.
