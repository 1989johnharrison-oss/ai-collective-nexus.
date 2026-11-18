#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
ROOT="$HOME/ai-collective-nexus"
MODEL="$ROOT/models/qwen2.5-1.5b-instruct-q4_k_m.gguf"
SERVER="$(command -v llama-server || true)"
if [ -z "$SERVER" ]; then SERVER="$ROOT/llama.cpp/build/bin/llama-server"; fi
[ -x "$SERVER" ] || { echo "Run setup-phone.sh first"; exit 1; }
[ -s "$MODEL" ] || { echo "Model missing: $MODEL"; exit 1; }
export NEXUS_ENGINE=llamacpp
export NEXUS_MODEL=local
export LLAMA_URL=http://127.0.0.1:8080/v1/chat/completions
echo "Starting local llama.cpp inference on Android. Keep Termux running."
"$SERVER" -m "$MODEL" --host 127.0.0.1 --port 8080 -c 2048 -t 4 -ngl 0 &
LLAMA_PID=$!
cleanup(){ kill "$LLAMA_PID" 2>/dev/null || true; }
trap cleanup EXIT INT TERM
sleep 4
python "$ROOT/source/backend/server.py"
