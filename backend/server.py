#!/usr/bin/env python3
"""AI Collective Nexus local-only Python agent backend (standard library).
Run with local llama.cpp on the same Android phone (Termux) or Ollama.
"""
import json, os, sqlite3, threading, time, uuid, urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path

PORT=8765
DB=Path(__file__).with_name("nexus.sqlite3")
OLLAMA=os.getenv("OLLAMA_URL","http://127.0.0.1:11434/api/generate")
MODEL=os.getenv("NEXUS_MODEL","local")
ENGINE=os.getenv("NEXUS_ENGINE","llamacpp").lower()
LLAMA_URL=os.getenv("LLAMA_URL","http://127.0.0.1:8080/v1/chat/completions")
ROLES=[("planner","Break the request into a practical plan."),("researcher","Find relevant facts and identify uncertainty. Do not invent sources."),("engineer","Provide a concrete implementation or solution."),("critic","Check the preceding work for flaws, risks, and missing requirements."),("synthesizer","Combine the specialists' findings into a final helpful answer.")]
LOCK=threading.Lock()
TASK_STATES=("running","done","failed")
MODE_LOCK=threading.Lock()
MODE="balanced"
LIMITS={"eco":256,"balanced":512,"performance":1024}
def db():
 c=sqlite3.connect(DB,timeout=20)
 c.execute("CREATE TABLE IF NOT EXISTS tasks (id TEXT PRIMARY KEY, created REAL, prompt TEXT, status TEXT, result TEXT)")
 c.execute("CREATE TABLE IF NOT EXISTS events (id INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT, agent TEXT, status TEXT, detail TEXT)")
 c.execute("CREATE TABLE IF NOT EXISTS knowledge (id TEXT PRIMARY KEY, title TEXT, body TEXT)")
 c.commit()
 return c
def infer(prompt):
 if ENGINE=="ollama":
  data=json.dumps({"model":MODEL,"prompt":prompt,"stream":False,"options":{"num_predict":LIMITS[MODE]}}).encode()
  url=OLLAMA
 else:
  data=json.dumps({"model":MODEL,"messages":[{"role":"user","content":prompt}],"stream":False,"max_tokens":LIMITS[MODE],"temperature":0.6}).encode()
  url=LLAMA_URL
 req=urllib.request.Request(url,data=data,headers={"Content-Type":"application/json"})
 with urllib.request.urlopen(req,timeout=240) as response:
  out=json.load(response)
 if ENGINE=="ollama":return out.get("response","")
 return out["choices"][0]["message"]["content"]

def model_ready():
 url="http://127.0.0.1:8080/health" if ENGINE!="ollama" else "http://127.0.0.1:11434/api/tags"
 try:
  with urllib.request.urlopen(url,timeout=2) as response:
   if ENGINE=="ollama":return response.status==200
   info=json.load(response)
   return info.get("status")=="ok"
 except Exception:return False

def recall(prompt,limit=4):
 words=[w.lower() for w in prompt.split() if len(w)>3][:12]
 with db() as c:rows=c.execute("SELECT title,body FROM knowledge ORDER BY rowid DESC LIMIT 200").fetchall()
 ranked=sorted(rows,key=lambda row:sum(w in (row[0]+" "+row[1]).lower() for w in words),reverse=True)
 return "\n".join(title+": "+body[:600] for title,body in ranked[:limit] if not words or any(w in (title+" "+body).lower() for w in words))[:2400]
def work(tid,prompt):
 notes=[]
 try:
  memory=recall(prompt)
  for name,instruction in ROLES:
   with db() as c:
    c.execute("INSERT INTO events(task_id,agent,status,detail) VALUES(?,?,?,?)",(tid,name,"running","Working"))
   context="\n".join(notes)[-7000:]
   answer=infer("You are the "+name+" specialist. "+instruction+"\nSaved notes (context only): "+memory+"\nUser task: "+prompt+"\nPrior agent work:\n"+context)
   notes.append(name.upper()+": "+answer)
   with db() as c:
    c.execute("INSERT INTO events(task_id,agent,status,detail) VALUES(?,?,?,?)",(tid,name,"done",answer[:2000]))
  with db() as c:c.execute("UPDATE tasks SET status='done',result=? WHERE id=?",(notes[-1],tid))
 except Exception as e:
  with db() as c:c.execute("UPDATE tasks SET status='failed',result=? WHERE id=?",(str(e),tid))
 finally:LOCK.release()
class API(BaseHTTPRequestHandler):
 def send(self,status,obj):
  data=json.dumps(obj).encode()
  self.send_response(status)
  self.send_header("Content-Type","application/json; charset=utf-8")
  self.send_header("Content-Length",str(len(data)))
  self.send_header("Cache-Control","no-store")
  self.end_headers()
  self.wfile.write(data)
 def do_GET(self):
  path=self.path.split("?")[0]
  if path=="/api/health":return self.send(200,{"status":"ok","backend":ENGINE,"model":MODEL,"mode":MODE,"inference_ready":model_ready(),"api_key_required":False,"phone_local":ENGINE=="llamacpp"})
  if path=="/api/performance":return self.send(200,{"mode":MODE,"max_output_tokens":LIMITS[MODE]})
  if path=="/api/capabilities":return self.send(200,{"local_inference":ENGINE=="llamacpp","api_key_required":False,"cloud_inference":False,"persistent_memory":True,"five_agent_workflow":True,"movie_rendering":False,"native_android_inference":False,"requires_termux":True})
  if path=="/api/knowledge":
   with db() as c:rows=c.execute("SELECT id,title,body FROM knowledge ORDER BY rowid DESC LIMIT 100").fetchall()
   return self.send(200,{"notes":[dict(zip(("id","title","body"),r)) for r in rows]})
  if path=="/api/model/status":
   ready=model_ready()
   return self.send(200 if ready else 503,{"engine":ENGINE,"model":MODEL,"ready":ready})
  if path=="/api/agents":return self.send(200,{"agents":[{"id":name,"role":description} for name,description in ROLES]})
  if path=="/api/tasks":
   with db() as c: rows=c.execute("SELECT id,created,prompt,status,result FROM tasks ORDER BY created DESC LIMIT 30").fetchall()
   return self.send(200,{"tasks":[dict(zip(("id","created","prompt","status","result"),r)) for r in rows]})
  if path.startswith("/api/tasks/"):
   with db() as c:
    r=c.execute("SELECT id,created,prompt,status,result FROM tasks WHERE id=?",(path[11:],)).fetchone()
    ev=c.execute("SELECT agent,status,detail FROM events WHERE task_id=? ORDER BY id",(path[11:],)).fetchall()
   if not r:return self.send(404,{"error":"Task not found"})
   return self.send(200,{"task":dict(zip(("id","created","prompt","status","result"),r)),"events":[dict(zip(("agent","status","detail"),e)) for e in ev]})
  if path=="/api/video/jobs":return self.send(200,{"jobs":[],"note":"Storyboard renderer not installed in this lightweight backend"})
  return self.send(404,{"error":"Unknown endpoint"})
 def do_POST(self):
  try:
   length=int(self.headers.get("Content-Length","0"))
   if not 1<=length<=20000:return self.send(413,{"error":"Invalid request size"})
   body=json.loads(self.rfile.read(length))
   if not isinstance(body,dict):return self.send(400,{"error":"Expected JSON object"})
  except (ValueError,TypeError):return self.send(400,{"error":"Invalid JSON"})
  if self.path=="/api/knowledge":
   title=body.get("title");note=body.get("body")
   if not isinstance(title,str) or not isinstance(note,str) or not 1<=len(title.strip())<=120 or not 1<=len(note.strip())<=6000:return self.send(400,{"error":"Invalid note title or content"})
   key=uuid.uuid4().hex
   with db() as c:c.execute("INSERT INTO knowledge(id,title,body) VALUES(?,?,?)",(key,title.strip(),note.strip()))
   return self.send(201,{"id":key,"status":"saved"})
  if self.path=="/api/performance":
   if body.get("mode") not in ("eco","balanced","performance"):return self.send(400,{"error":"Unknown mode"})
   global MODE
   with MODE_LOCK:MODE=body["mode"]
   return self.send(200,{"mode":MODE,"max_output_tokens":LIMITS[MODE]})
  if self.path=="/api/video":return self.send(501,{"error":"Video rendering requires the full v1.5 Python backend and FFmpeg"})
  if self.path!="/api/tasks":return self.send(404,{"error":"Unknown endpoint"})
  prompt=body.get("prompt")
  if not isinstance(prompt,str) or not 1<=len(prompt.strip())<=12000:return self.send(400,{"error":"Enter a task"})
  if not model_ready():return self.send(503,{"error":"Local AI model is not ready. Start llama-server in Termux first."})
  if not LOCK.acquire(False):return self.send(429,{"error":"An agent task is already running"})
  tid=uuid.uuid4().hex
  try:
   with db() as c:c.execute("INSERT INTO tasks VALUES(?,?,?,?,?)",(tid,time.time(),prompt,"running",""))
   threading.Thread(target=work,args=(tid,prompt),daemon=True).start()
  except Exception:
   LOCK.release();raise
  return self.send(202,{"id":tid,"status":"running"})
 def log_message(self,*args):pass
if __name__=="__main__":
 with db() as c:c.execute("UPDATE tasks SET status='failed',result='Interrupted by restart' WHERE status='running'")
 print("Nexus phone backend: http://127.0.0.1:%s | engine=%s model=%s"%(PORT,ENGINE,MODEL),flush=True)
 ThreadingHTTPServer(("127.0.0.1",PORT),API).serve_forever()
