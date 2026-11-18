#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
# Run this script inside Termux on the SAME Android phone as Nexus.
ROOT="$HOME/ai-collective-nexus"
MODEL_DIR="$ROOT/models"
MODEL_FILE="$MODEL_DIR/qwen2.5-1.5b-instruct-q4_k_m.gguf"
MODEL_URL="${NEXUS_MODEL_URL:-https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q4_K_M.gguf?download=true}"
echo "Installing phone-only dependencies. This can take time and use several GB of storage."
pkg update -y
pkg install -y python git cmake clang make curl
mkdir -p "$MODEL_DIR"
if [ ! -d "$ROOT/source/.git" ]; then
 git clone --depth 1 https://github.com/1989johnharrison-oss/ai-collective-nexus..git "$ROOT/source"
else
 git -C "$ROOT/source" pull --ff-only
fi
if [ ! -d "$ROOT/llama.cpp/.git" ]; then
 git clone --depth 1 https://github.com/ggml-org/llama.cpp.git "$ROOT/llama.cpp"
fi
cmake -S "$ROOT/llama.cpp" -B "$ROOT/llama.cpp/build" -DCMAKE_BUILD_TYPE=Release -DGGML_OPENMP=OFF -DLLAMA_CURL=OFF
cmake --build "$ROOT/llama.cpp/build" --config Release -j 2 --target llama-server
if [ ! -s "$MODEL_FILE" ]; then
 echo "Downloading the local quantised language model (~1 GB)."
 curl -fL --retry 3 --continue-at - "$MODEL_URL" -o "$MODEL_FILE"
fi
if [ ! -s "$MODEL_FILE" ]; then echo "Model download failed";exit 1;fi
echo "Installation complete. Start the backend with: bash $ROOT/start-phone.sh"
