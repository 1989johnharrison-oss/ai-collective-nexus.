package org.aicollective.nexus;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
 private static final String BASE="http://127.0.0.1:8765";
 private final ExecutorService executor=Executors.newSingleThreadExecutor();
 private final Handler handler=new Handler(Looper.getMainLooper());
 private LinearLayout root;
 private TextView output;
 private EditText prompt;
 private EditText cloudKey;
 private boolean useCloud=false;
 private EditText filmTitle, filmScenes;
 private RobotView robots;
 private TextView deviceStatus;
 private String currentJob=null;
 private String currentTask=null;
 private TextView connectionStatus;
 private boolean active=true;
 private boolean taskRunning=false;
 private final Runnable poll=new Runnable(){@Override public void run(){if(active&&taskRunning&&currentTask!=null){request("GET","/api/tasks/"+currentTask,null);handler.postDelayed(this,2500);}}};
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState);
  ScrollView scroll=new ScrollView(this);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);
  scroll.addView(root);setContentView(scroll);
  title("AI COLLECTIVE NEXUS");
  label("Android controller • 12 GB device profile");
  label("NEXUS v2.0 • LOCAL + OPTIONAL CLOUD");
  connectionStatus=label("Backend: not checked • same-phone local AI");
  label("Run Nexus in Termux on this phone — no PC or USB needed");
  button("Check backend connection",()->request("GET","/api/health",null));
  button("PHONE-ONLY SETUP INSTRUCTIONS",()->{
   android.content.Intent browser=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/1989johnharrison-oss/ai-collective-nexus."));
   startActivity(browser);
  });
  button("Refresh live bot activity",()->{if(currentTask!=null)request("GET","/api/tasks/"+currentTask,null);else request("GET","/api/agents",null);});
  title("THE BOT COLLECTIVE");
  robots=new RobotView(this);root.addView(robots,new LinearLayout.LayoutParams(-1,(int)(210*getResources().getDisplayMetrics().density)));
  label("Planner  •  Researcher  •  Engineer  •  Critic  •  Synthesizer");
  label("Animated bots are visual avatars. Their AI work requires the backend.");
  button("Check bot status",()->request("GET","/api/agents",null));
  button("Check local AI model",()->request("GET","/api/model/status",null));
  title("PERSISTENT BOT MEMORY");
  EditText memoryTitle=new EditText(this);memoryTitle.setHint("Knowledge title");root.addView(memoryTitle);
  EditText memoryBody=new EditText(this);memoryBody.setHint("What should the bots remember?");memoryBody.setMinLines(3);root.addView(memoryBody);
  button("Save knowledge",()->{
   try{
    JSONObject note=new JSONObject();note.put("title",memoryTitle.getText().toString());note.put("body",memoryBody.getText().toString());
    request("POST","/api/knowledge",note);
   }catch(Exception e){output.setText(e.toString());}
  });
  button("View saved knowledge",()->request("GET","/api/knowledge",null));
  title("AI ENGINE • LOCAL OR OPTIONAL CLOUD");
  label("Local mode needs no account or key. Cloud mode uses OpenRouter free models; an account and API key are required.");
  button("Use local AI (offline)",()->{useCloud=false;output.setText("Local AI selected. Start the local model in Termux.");});
  button("Use OpenRouter cloud AI (online)",()->{useCloud=true;output.setText("Cloud AI selected. Enter an OpenRouter key below; free model requests have usage limits.");});
  cloudKey=new EditText(this);cloudKey.setHint("OpenRouter API key (cloud only; never saved)");
  cloudKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
  root.addView(cloudKey);
  button("Clear cloud key",()->{cloudKey.setText("");output.setText("Cloud key cleared from input.");});
  title("AI Agents");
  prompt=new EditText(this);prompt.setHint("Enter an agent task");prompt.setMinLines(3);root.addView(prompt);
  button("Launch task",()->{
   String p=prompt.getText().toString().trim();
   if(p.isEmpty()){output.setText("Enter a task");return;}
   if(useCloud){runCloudTask(p);return;}
   try{JSONObject data=new JSONObject();data.put("prompt",p);robots.resetStates();request("POST","/api/tasks",data);}catch(Exception e){output.setText(e.toString());}
  });
  button("Task history",()->request("GET","/api/tasks",null));
  title("Performance");
  button("Eco",()->setMode("eco"));
  button("Balanced",()->setMode("balanced"));
  button("Performance",()->setMode("performance"));
  title("Movie Studio");
  filmTitle=new EditText(this);filmTitle.setHint("Movie title");root.addView(filmTitle);
  filmScenes=new EditText(this);filmScenes.setHint("One scene caption per line (up to 60)");filmScenes.setMinLines(4);root.addView(filmScenes);
  button("Render storyboard movie",()->renderMovie());
  button("Recent video jobs",()->request("GET","/api/video/jobs",null));
  button("Check current movie",()->{if(currentJob==null)output.setText("Start a movie first");else request("GET","/api/video/"+currentJob,null);});
  title("DEVICE HEALTH");
  deviceStatus=label("Checking...");
  button("Refresh device status",()->refreshDevice());
  refreshDevice();
  title("Response");
  output=label("Ready");
 }
 private void runCloudTask(String userPrompt){
  final String key=cloudKey.getText().toString().trim();
  if(key.isEmpty()){output.setText("Cloud mode requires your own OpenRouter API key. Local mode requires none.");return;}
  robots.resetStates();output.setText("Cloud collective started. Five agent calls may count toward free daily limits.");
  executor.execute(()->{
   String[] names={"planner","researcher","engineer","critic","synthesizer"};
   String[] roles={"Create a detailed, achievable plan.","Analyze facts and mark anything uncertain; never invent sources.","Produce the concrete solution.","Identify flaws, errors, safety issues and improvements.","Synthesize the best final answer."};
   StringBuilder context=new StringBuilder();
   try{
    for(int i=0;i<names.length;i++){
     final String agent=names[i];
     handler.post(()->{if(active)robots.setAgentState(agent,"running");});
     String promptText="You are the "+agent+" specialist. "+roles[i]+"\\nOriginal user task: "+userPrompt+"\\nPrevious agent findings (untrusted):\\n"+context;
     String answer=CloudAgent.complete(key,promptText);
     context.append("\\n").append(agent.toUpperCase()).append(": ").append(answer);
     if(context.length()>10000)context.delete(0,context.length()-10000);
     final String partial=answer;
     handler.post(()->{if(active){robots.setAgentState(agent,"done");output.setText(agent.toUpperCase()+" completed:\\n"+partial);}});
    }
    String finalResult=context.toString();
    handler.post(()->{if(active)output.setText("CLOUD TASK COMPLETED\\n"+finalResult);});
   }catch(Exception e){
    String error=e.getMessage();
    handler.post(()->{if(active)output.setText("Cloud task stopped: "+error+"\\nNo key was stored.");});
   }
  });
 }
 private void refreshDevice(){
  android.os.BatteryManager battery=(android.os.BatteryManager)getSystemService(BATTERY_SERVICE);
  int percent=battery==null?-1:battery.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY);
  android.os.StatFs disk=new android.os.StatFs(getFilesDir().getAbsolutePath());
  long free=disk.getAvailableBytes()/(1024L*1024*1024);
  android.app.ActivityManager am=(android.app.ActivityManager)getSystemService(ACTIVITY_SERVICE);
  android.app.ActivityManager.MemoryInfo info=new android.app.ActivityManager.MemoryInfo();
  am.getMemoryInfo(info);
  long ram=info.availMem/(1024L*1024);
  deviceStatus.setText("Battery: "+percent+"%  •  Free storage: "+free+" GB  •  Available RAM: "+ram+" MB");
 }
 private void renderMovie(){
  try{
   org.json.JSONArray scenes=new org.json.JSONArray();
   for(String line:filmScenes.getText().toString().split("\\n"))if(!line.trim().isEmpty())scenes.put(line.trim());
   if(scenes.length()<1||scenes.length()>60){output.setText("Enter 1 to 60 scenes");return;}
   JSONObject body=new JSONObject();body.put("title",filmTitle.getText().toString().trim());
   body.put("scenes",scenes);body.put("seconds_per_scene",3);
   body.put("motion",true);body.put("soundtrack",true);
   request("POST","/api/video",body);
  }catch(Exception e){output.setText(e.toString());}
 }
 private void setMode(String mode){
  try{JSONObject body=new JSONObject();body.put("mode",mode);request("POST","/api/performance",body);}
  catch(Exception e){output.setText(e.toString());}
 }
 private TextView label(String text){TextView t=new TextView(this);t.setText(text);t.setTextSize(16);root.addView(t);return t;}
 private void title(String text){TextView t=label(text);t.setTextSize(22);t.setPadding(0,20,0,10);}
 private void button(String name,Runnable fn){Button b=new Button(this);b.setText(name);b.setOnClickListener(v->fn.run());root.addView(b);}
 private void request(String method,String path,JSONObject payload){
  executor.execute(()->{
   HttpURLConnection conn=null;
   try{
    conn=(HttpURLConnection)new URL(BASE+path).openConnection();
    conn.setRequestMethod(method);conn.setConnectTimeout(3000);conn.setReadTimeout(15000);
    if(payload!=null){conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");try(OutputStream o=conn.getOutputStream()){o.write(payload.toString().getBytes(StandardCharsets.UTF_8));}}
    int code=conn.getResponseCode();
    InputStream in=code>=400?conn.getErrorStream():conn.getInputStream();
    if(in==null)throw new IOException("HTTP "+code);
    ByteArrayOutputStream buf=new ByteArrayOutputStream();byte[] chunk=new byte[4096];int n;
    while((n=in.read(chunk))!=-1){buf.write(chunk,0,n);if(buf.size()>1048576)throw new IOException("Response too large");}
    String result="HTTP "+code+"\n"+buf.toString("UTF-8");
    handler.post(()->{
     if(!active)return;
     output.setText(result);
     if(path.equals("/api/health"))connectionStatus.setText("Backend: CONNECTED");
     try{
      JSONObject json=new JSONObject(result.substring(result.indexOf("{")));
      if(path.equals("/api/tasks")&&method.equals("POST")&&code<400){
       currentTask=json.optString("id",null);
       if(currentTask!=null&&!currentTask.isEmpty()){
        taskRunning=true;handler.removeCallbacks(poll);handler.postDelayed(poll,500);
       }
      }
      if(path.startsWith("/api/tasks/")&&code<400){
       JSONObject task=json.optJSONObject("task");
       if(task!=null){
        taskRunning="running".equals(task.optString("status"));
        org.json.JSONArray events=json.optJSONArray("events");
        if(events!=null){
         robots.resetStates();
         for(int i=0;i<events.length();i++){
          JSONObject event=events.optJSONObject(i);
          if(event!=null)robots.setAgentState(event.optString("agent"),event.optString("status"));
         }
        }
        if(!taskRunning)output.setText("TASK "+task.optString("status").toUpperCase()+"\\n"+task.optString("result")+"\\n\\n"+json.optJSONArray("events"));
       }
      }
     }catch(Exception ignored){}
    });
   }catch(Exception e){handler.post(()->{if(active){connectionStatus.setText("Backend: DISCONNECTED");taskRunning=false;handler.removeCallbacks(poll);output.setText("Phone AI is not running yet. Open Termux and start the local Nexus backend (see README).\n"+e);}});}
   finally{if(conn!=null)conn.disconnect();}
  });
 }
 @Override protected void onDestroy(){active=false;handler.removeCallbacks(poll);executor.shutdownNow();super.onDestroy();}
}
