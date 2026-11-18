# AI Collective Nexus — Android v1.6 + five-bot Python backend

This repository builds an Android controller with five animated robots: **Planner, Researcher, Engineer, Critic and Synthesizer**. It includes a local Python agent backend that calls **Ollama** on your computer, saves tasks and agent events to SQLite, and can be accessed over a USB cable. The Android app displays battery, available RAM and free storage.

## Download the updated APK

Open [GitHub Actions](https://github.com/1989johnharrison-oss/ai-collective-nexus./actions/workflows/android-apk.yml), open the **latest successful** run, and download the **AI-Collective-Nexus-APK** artifact. Extract it to get `app-debug.apk`. **Install the newest APK over the old one**; the previous app does not automatically update when GitHub code changes. Confirm that the top of the screen says **NEXUS v1.6 • LIVE BOT COLLECTIVE** and displays five animated robots.

## Make the Python connection work

A computer (Windows, macOS or Linux) is needed for this version. Install Python 3.10+, Android Platform Tools (adb), and [Ollama](https://ollama.com). On the computer, run:

```bash
ollama pull gemma3:1b
ollama serve
```

Ollama may already be running in the background; if so, don't start a second server. In another terminal, from this repository run:

```bash
python backend/server.py
```

Enable Developer options and **USB debugging** on your Samsung, connect it to the computer via USB, approve the debugging prompt, then run:

```bash
adb devices
adb reverse tcp:8765 tcp:8765
```

Open Nexus on your phone and tap **Check backend connection**. It should say **Backend: CONNECTED**. Now enter an agent task and tap **Launch task**. Use **Refresh live bot activity** to view the task and its specialist events. Ollama must have the chosen model downloaded; set `NEXUS_MODEL` to a different locally installed model if desired.

## What this build can and cannot do

- **Works:** Android app, five animated avatars, connection status, Ollama-powered five-role sequential collaboration, task history, SQLite persistence, battery/storage/RAM information.
- **Not yet:** standalone on-device AI inference, phone-only Python server, autonomous internet research, fully generative AI movies. The included `backend/server.py` is a compact real Ollama agent backend; it intentionally returns HTTP 501 for movie rendering.
- **Movie studio:** The Android scene editor is present. For storyboard MP4 rendering, use the full Python v1.5 backend from the earlier prototype with FFmpeg and Pillow; this smaller backend does not contain its renderer.
- **Security:** Python binds to `127.0.0.1` only. Do **not** expose its unauthenticated API to the public internet or forward router ports. USB `adb reverse` avoids opening a LAN server.

## Troubleshooting

If the old white screen remains, you have the previous APK installed: download the latest successful workflow artifact and install it. If the build fails, open the failed step in GitHub Actions. If the app says disconnected, check `adb devices`, re-run `adb reverse`, and verify `http://127.0.0.1:8765/api/health` on the computer. If agents fail with Ollama errors, run `ollama list` and ensure the model is installed.

This is an open-source prototype. No personal credentials should be committed to this public repository.
