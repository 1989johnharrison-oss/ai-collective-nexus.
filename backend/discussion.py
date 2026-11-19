"""Small persistent discussion board; model text is untrusted and bounded."""
import re

KINDS=("goal","finding","proposal","concern","revision","decision")
MAX_ITEMS=24
def initialize(connection):
 connection.execute("CREATE TABLE IF NOT EXISTS discussion (id INTEGER PRIMARY KEY AUTOINCREMENT, task_id TEXT NOT NULL, agent TEXT NOT NULL, kind TEXT NOT NULL, content TEXT NOT NULL)")
 connection.execute("CREATE INDEX IF NOT EXISTS idx_discussion_task ON discussion(task_id,id)")
def post(connection,task_id,agent,kind,content):
 if kind not in KINDS:raise ValueError("Invalid discussion kind")
 content=str(content).strip()[:600]
 if not content:return
 connection.execute("INSERT INTO discussion(task_id,agent,kind,content) VALUES(?,?,?,?)",(task_id,agent,kind,content))
 connection.execute("DELETE FROM discussion WHERE task_id=? AND id NOT IN (SELECT id FROM discussion WHERE task_id=? ORDER BY id DESC LIMIT ?)",(task_id,task_id,MAX_ITEMS))
def entries(connection,task_id):
 return [dict(zip(("agent","kind","content"),row)) for row in connection.execute("SELECT agent,kind,content FROM discussion WHERE task_id=? ORDER BY id",(task_id,))]
def context(items):
 return "\\n".join(item["agent"]+" ["+item["kind"]+"]: "+item["content"] for item in items)[-2800:]
def summary(text):
 # Strip fenced code before selecting the first meaningful sentence.
 clean=re.sub(r"```.*?```"," [code omitted] ",text,flags=re.S)
 clean=re.sub(r"\\s+"," ",clean).strip()
 return clean[:450]
