import importlib.util
import json
import tempfile
import threading
import unittest
import urllib.error
import urllib.request
from unittest.mock import patch
from pathlib import Path

spec=importlib.util.spec_from_file_location("nexus_server",Path(__file__).parent/"server.py")
server=importlib.util.module_from_spec(spec)
spec.loader.exec_module(server)

class BackendTests(unittest.TestCase):
 @classmethod
 def setUpClass(cls):
  cls.tmp=tempfile.TemporaryDirectory()
  server.DB=Path(cls.tmp.name)/"test.sqlite3"
  cls.http=server.ThreadingHTTPServer(("127.0.0.1",0),server.API)
  cls.thread=threading.Thread(target=cls.http.serve_forever,daemon=True)
  cls.thread.start()
  cls.url="http://127.0.0.1:"+str(cls.http.server_port)
 @classmethod
 def tearDownClass(cls):
  cls.http.shutdown();cls.http.server_close();cls.thread.join();cls.tmp.cleanup()
 def call(self,path,body=None):
  data=None if body is None else json.dumps(body).encode()
  req=urllib.request.Request(self.url+path,data=data,headers={"Content-Type":"application/json"})
  try:
   with urllib.request.urlopen(req,timeout=3) as response:return response.status,json.load(response)
  except urllib.error.HTTPError as e:return e.code,json.load(e)
 def test_health(self):
  code,obj=self.call("/api/health")
  self.assertEqual(code,200);self.assertEqual(obj["status"],"ok")
 def test_five_agents(self):
  code,obj=self.call("/api/agents")
  self.assertEqual(code,200);self.assertEqual(len(obj["agents"]),5)
 def test_performance_is_effective(self):
  for mode,limit in [("eco",256),("balanced",512),("performance",1024)]:
   self.assertEqual(self.call("/api/performance",{"mode":mode})[0],200)
   self.assertEqual(self.call("/api/performance")[1]["max_output_tokens"],limit)
  self.assertEqual(server.LIMITS[server.MODE],1024)
 def test_invalid_mode(self):
  self.assertEqual(self.call("/api/performance",{"mode":"turbo"})[0],400)
 def test_missing_task(self):
  self.assertEqual(self.call("/api/tasks/not-a-real-task")[0],404)
 def test_reject_nonobject(self):
  self.assertEqual(self.call("/api/tasks",["bad"])[0],400)
 def test_reject_empty_task(self):
  self.assertEqual(self.call("/api/tasks",{"prompt":""})[0],400)
 def test_llamacpp_request_shape(self):
  class FakeResponse:
   def __enter__(self):return self
   def __exit__(self,*args):return False
  def fake_urlopen(req,timeout):
   payload=json.loads(req.data)
   self.assertEqual(payload["messages"][0]["role"],"user")
   self.assertEqual(payload["messages"][0]["content"],"Hello")
   self.assertGreater(payload["max_tokens"],0)
   return FakeResponse()
  old_engine=server.ENGINE
  server.ENGINE="llamacpp"
  try:
   with patch.object(server.urllib.request,"urlopen",side_effect=fake_urlopen):
    with patch.object(server.json,"load",return_value={"choices":[{"message":{"content":"Hi"}}]}):
     self.assertEqual(server.infer("Hello"),"Hi")
  finally:server.ENGINE=old_engine
 def test_movie_not_falsely_advertised(self):
  self.assertEqual(self.call("/api/video",{"scenes":["x"]})[0],501)

if __name__=="__main__":unittest.main()
