# AI Collective Nexus v1.9 — Phone-local five-bot Android AI

**No computer, USB cable, paid AI subscription, API key or account registration is required for the local AI engine.** Android APK displays five animated bots and communicates with a Python + llama.cpp AI server running inside Termux **on the same phone**, via 127.0.0.1. This is genuine local model inference once the model has been downloaded and the server started. It is not yet an embedded Android inference engine: Termux must remain running.

## 1. Install the latest APK
Open [GitHub Actions](https://github.com/1989johnharrison-oss/ai-collective-nexus./actions), select the latest **green** build, download artifact `AI-Collective-Nexus-APK`, extract the ZIP and install `app-debug.apk` over the previous APK. The app should show **NEXUS v1.9 • PHONE-LOCAL BOT COLLECTIVE**.

## 2. Install phone-local AI (on your Samsung)
Install **Termux from F-Droid or the official Termux GitHub releases**, not an outdated Play Store build. Open Termux and run:

```sh
pkg update -y
pkg install -y git
git clone https://github.com/1989johnharrison-oss/ai-collective-nexus..git ~/nexus-installer
bash ~/nexus-installer/phone/setup-phone.sh
bash ~/ai-collective-nexus/start-phone.sh
```

The installer downloads a ~1 GB Q4 quantised Qwen2.5 1.5B instruction model and first attempts the prebuilt Termux llama-cpp package, and compiles llama.cpp locally using only two build jobs if the package is unavailable. **This is an initial experimental configuration, not yet verified on your particular Samsung.** Allow several GB free space, keep the phone charged, and expect compilation/model downloads to take time. If the model URL changes, set `NEXUS_MODEL_URL` to a direct GGUF download URL before running setup. If the phone heats up, stop the server and allow it to cool. Never disable Android's thermal protections.

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


## Keyless-first design decisions

The local AI model is downloaded from a public GGUF model repository, not from a paid inference provider. The five agents reuse one model on the same device rather than loading five copies into RAM. The app's local API uses loopback only. **No credentials should be entered into Nexus or committed to GitHub.** Internet access is required for initial software/model downloads, but inference works offline after installation. Termux is a temporary integration step until an embedded Android native inference runtime is built and verified.

## v1.9: persistent bot memory

The Android screen now includes **Save knowledge** and **View saved knowledge** controls. Notes are stored in the local SQLite database in Termux, and relevant saved notes are retrieved into later specialist prompts. This is explicit user-approved memory, **not autonomous learning**. The **Check local AI model** button tests whether llama.cpp's local inference server is reachable. Backend HTTP tests include saving and retrieving notes and validation of malformed entries.

The APK is still a frontend for a same-phone Termux process. No API keys or external paid services are required, but installation and a working model server are still prerequisites. Source commits are not proof that a GitHub Actions build or device test has passed.

## v2.0: optional direct cloud AI on Android

The Android APK now includes **Local AI** (default, no API key, same-phone Termux backend) and **OpenRouter Cloud AI** (optional, internet required). Cloud mode directly calls the OpenRouter OpenAI-compatible chat completions endpoint from Android and runs Planner, Researcher, Engineer, Critic, Synthesizer sequentially. It does **not** require Termux for cloud tasks. The chosen model is `openrouter/free`, which routes to a currently available free model. Free-tier rate limits apply; five agents make up to five separate calls per task. No subscription or credit card is required to get started, according to OpenRouter's current developer documentation, but an OpenRouter account and API key **are required**. You must create the key yourself at https://openrouter.ai/keys and enter it **inside the APK**, never in chat or GitHub. The app does not persist the key to preferences, files or database, though the running process temporarily holds it in memory. Do not use cloud mode for sensitive personal information: prompts are transmitted to OpenRouter and its model providers. Cloud task history and local memory are **not yet integrated** with this direct-cloud path. A production build should improve key handling and cloud request resilience.

The existing local mode still requires no key and remains the default. There is no way for an independent app to use your ChatGPT subscription as an OpenAI API credential. Latest source is not proof that the APK build has passed.

## v2.1: Ollama Cloud and adaptive cloud collective

Nexus now supports **Ollama Cloud** directly from Android at `https://ollama.com/v1/chat/completions` with your existing Ollama API key. Ollama's documented cloud model `gemma4:31b` is the initial default; availability and account limits depend on Ollama. **No Termux or computer is needed for cloud tasks.** Enter your key only in the password field inside the APK; the app does not save it to disk or commit it to GitHub. Your prompts leave the phone in cloud mode. OpenRouter remains available as an alternative; offline/local mode still requires no key.

**Efficient coordination** is the default: short tasks use a single request with five role headings; longer prompts use three stages (plan/research, engineering, critique/synthesis). **Full review** runs all five specialist calls separately. Fewer calls reduce latency and quota consumption, but combined stages are not independent model instances and are not equivalent to five separate independent reviews. The current adaptive decision uses prompt length, not a learned difficulty classifier. Ollama may enforce free-tier rate or usage limits. Cloud mode currently lacks persisted task history and shared memory. APK and device testing still pending.
