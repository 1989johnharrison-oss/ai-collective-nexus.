# AI Collective Nexus v1.8 — Phone-local five-bot Android AI

**No computer or USB cable is required for this setup.** Android APK displays five animated bots and communicates with a Python + llama.cpp AI server running inside Termux **on the same phone**, via 127.0.0.1. This is genuine local model inference once the model has been downloaded and the server started. It is not yet an embedded Android inference engine: Termux must remain running.

## 1. Install the latest APK
Open [GitHub Actions](https://github.com/1989johnharrison-oss/ai-collective-nexus./actions), select the latest **green** build, download artifact `AI-Collective-Nexus-APK`, extract the ZIP and install `app-debug.apk` over the previous APK. The app should show **NEXUS v1.8 • PHONE-LOCAL BOT COLLECTIVE**.

## 2. Install phone-local AI (on your Samsung)
Install **Termux from F-Droid or the official Termux GitHub releases**, not an outdated Play Store build. Open Termux and run:

```sh
pkg update -y
pkg install -y git
git clone https://github.com/1989johnharrison-oss/ai-collective-nexus..git ~/nexus-installer
bash ~/nexus-installer/phone/setup-phone.sh
bash ~/ai-collective-nexus/start-phone.sh
```

The installer downloads a ~1 GB Q4 quantised Qwen2.5 1.5B instruction model and compiles llama.cpp locally using only two build jobs. **This is an initial experimental configuration, not yet verified on your particular Samsung.** Allow several GB free space, keep the phone charged, and expect compilation/model downloads to take time. If the model URL changes, set `NEXUS_MODEL_URL` to a direct GGUF download URL before running setup. If the phone heats up, stop the server and allow it to cool. Never disable Android's thermal protections.

## 3. Start the five bots
Keep Termux open or running in the background while using the Nexus APK. Tap **Check backend connection**; the app should show **Backend: CONNECTED**. Type a task and tap **Launch task**. Planner, Researcher, Engineer, Critic and Synthesizer run in sequence using the locally loaded model, and the animated bots update individually. Tasks are saved in SQLite.

After reboot, start Termux and run:

```sh
bash ~/ai-collective-nexus/start-phone.sh
```

The local Python API listens on `127.0.0.1:8765`; llama.cpp listens on `127.0.0.1:8080`. No router configuration, cloud AI subscription or USB debugging is needed.

## Actual capabilities and limits
- **Implemented in source:** five animated robot avatars, sequential specialist prompts, task/event history, live status polling, local SQLite database, Android device metrics, configurable Ollama or llama.cpp inference, and phone-only Termux setup scripts.
- **Not implemented:** integrated native Android model loading without Termux, independent concurrent agents, automatic online research, generative video, working movie rendering in the compact Python backend, or persistent self-learning across tasks. Movie Studio UI currently calls an endpoint returning 501.
- Eco/Balanced/Performance adjust **maximum output tokens**, not actual device clock speed.
- Local 1.5B models can be slow and make mistakes; 12 GB RAM doesn't guarantee a model will run fast or reliably.
- **Security:** Both servers bind to loopback only. Do not expose the unauthenticated API over the internet.
- **Build status:** GitHub Actions runs backend tests and builds an APK. Source changes are not installed until you download the new successful artifact.

## Troubleshooting
If you still see the old screen, install the latest successful APK artifact. If the backend is disconnected, ensure Termux is still running and check `curl http://127.0.0.1:8765/api/health` inside Termux. If llama.cpp fails, review the Termux error output and check that `~/ai-collective-nexus/models/qwen2.5-1.5b-instruct-q4_k_m.gguf` exists. If Android kills Termux in the background, keep it foreground while testing and review Samsung battery restrictions for Termux.

