"""Restricted, non-executing HTML project workspace for Nexus tasks."""
import html.parser
import json
import re
from pathlib import Path

MAX_BYTES=120000
BLOCK=re.compile(r"```html\s*\n(.*?)```",re.DOTALL|re.IGNORECASE)
ID=re.compile(r"[0-9a-f]{32}\Z")
FORBIDDEN=[
 (re.compile(r"<\s*(iframe|object|embed|applet|base)\b",re.I),"Embedded external content is not supported"),
 (re.compile(r"\b(fetch\s*\(|XMLHttpRequest|WebSocket\s*\(|EventSource\s*\(|navigator\.sendBeacon)"),"Network APIs are not allowed"),
 (re.compile(r"<script\b[^>]*\bsrc\s*=",re.I),"External scripts are not allowed"),
 (re.compile(r"<link\b[^>]*\bhref\s*=",re.I),"External stylesheets are not allowed"),
 (re.compile(r"\b(import\s*\(|import\s+.*?from\s*['\"])"),"Module imports are not supported"),
]
class Inspector(html.parser.HTMLParser):
 def __init__(self):
  super().__init__();self.tags=set();self.scripts=0
 def handle_starttag(self,tag,attrs):
  self.tags.add(tag)
  if tag=="script":self.scripts+=1

def validate(source):
 errors=[];warnings=[]
 if not isinstance(source,str) or not source.strip():return {"valid":False,"errors":["Empty HTML"],"warnings":[],"checks":["non-executing structural checks"]}
 if len(source.encode("utf-8"))>MAX_BYTES:errors.append("File exceeds 120 KB limit")
 if not re.search(r"<!doctype\s+html|<html\b",source,re.I):errors.append("Missing HTML document root")
 if not re.search(r"<body\b",source,re.I):errors.append("Missing body element")
 if not re.search(r"</html\s*>",source,re.I):errors.append("Missing closing html element")
 for pattern,message in FORBIDDEN:
  if pattern.search(source):errors.append(message)
 inspector=Inspector()
 try:inspector.feed(source);inspector.close()
 except ValueError as e:errors.append("HTML parser error: "+str(e))
 if "script" not in inspector.tags:warnings.append("No inline JavaScript found; interactive gameplay not verified")
 warnings.append("JavaScript syntax and gameplay have not been executed or verified")
 return {"valid":not errors,"errors":errors,"warnings":warnings,"checks":["size","HTML structure","disallowed APIs","external dependencies"]}

def extract(outputs):
 for answer in reversed(outputs):
  for match in BLOCK.finditer(answer):
   source=match.group(1).strip()
   if source:return source
 return None

def workspace_path(root,task_id):
 if not ID.fullmatch(task_id):raise ValueError("Invalid task ID")
 return Path(root)/task_id

def save(root,task_id,source,report):
 if not report["valid"]:return None
 directory=workspace_path(root,task_id)
 directory.mkdir(parents=True,exist_ok=True)
 (directory/"index.html").write_text(source,encoding="utf-8")
 (directory/"validation.json").write_text(json.dumps(report,indent=2),encoding="utf-8")
 return {"type":"html","name":"index.html","preview_url":"/api/artifacts/"+task_id,"validated":True,"validation_scope":"static only","report":report}
