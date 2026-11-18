package org.aicollective.nexus;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.WindowManager;
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
 private static final int BG=0xff0b1020, PANEL=0xff17233b, TEXT=0xffedf4ff, MUTED=0xffa8b8d2, ACCENT=0xff5eead4;
 private TextView output;
 private EditText prompt;
 private EditText cloudKey;
 private EditText modelInput;
 private boolean useCloud=false;
 private String cloudProvider="ollama";
 private String cloudModel="gemma4:31b";
 private boolean efficientCloud=true;
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
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  getWindow().getDecorView().setSystemUiVisibility(0);
  ScrollView scroll=new ScrollView(this);
  scroll.setBackgroundColor(BG);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(18),dp(22),dp(18),dp(34));
  scroll.addView(root);setContentView(scroll);
  title("AI COLLECTIVE NEXUS");
  label("YOUR FIVE-AGENT AI WORKSPACE");
  label("NEXUS v2.2 • DARK EDITION • OLLAMA CLOUD");
  connectionStatus=label("Backend: not checked • same-phone local AI");
  label("Cloud mode works without Termux. Local mode requires the phone backend.");
  button("Check LOCAL backend connection",()->request("GET","/api/health",null));
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
  EditText memoryTitle=new EditText(this);memoryTitle.setHint("Knowledge title");field(memoryTitle);
  EditText memoryBody=new EditText(this);memoryBody.setHint("What should the bots remember?");memoryBody.setMinLines(3);field(memoryBody);
  button("Save knowledge",()->{
   try{
    JSONObject note=new JSONObject();note.put("title",memoryTitle.getText().toString());note.put("body",memoryBody.getText().toString());
    request("POST","/api/knowledge",note);
   }catch(Exception e){output.setText(e.toString());}
  });
  button("View saved knowledge",()->request("GET","/api/knowledge",null));
  title("AI ENGINE • LOCAL OR OPTIONAL CLOUD");
  label("Choose your AI engine. Ollama Cloud works with your existing Ollama API key; local mode works offline once installed.");
  button("Use local AI (offline)",()->{useCloud=false;output.setText("Local AI selected. Start the local model in Termux.");});
  button("Use cloud AI (online)",()->{useCloud=true;output.setText("Cloud AI selected. Choose Ollama or OpenRouter below and enter the matching key.");});
  button("Cloud provider: Ollama",()->{useCloud=true;cloudProvider="ollama";cloudModel="gemma4:31b";output.setText("Ollama Cloud selected. Enter your Ollama key; cloud usage limits apply.");});
  button("Cloud provider: OpenRouter",()->{useCloud=true;cloudProvider="openrouter";cloudModel="openrouter/free";output.setText("OpenRouter free model router selected.");});
  button("Cloud coordination: Efficient (1–3 requests)",()->{efficientCloud=true;output.setText("Efficient coordination enabled.");});
  button("Cloud coordination: Full review (5 requests)",()->{efficientCloud=false;output.setText("Full five-agent review enabled.");});
  cloudKey=new EditText(this);cloudKey.setHint("Cloud provider API key (not saved)");
  cloudKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
  field(cloudKey);
  modelInput=new EditText(this);modelInput.setHint("Cloud model name (optional override)");field(modelInput);
  button("Clear cloud key",()->{cloudKey.setText("");output.setText("Cloud key cleared from input.");});
  title("AI Agents");
  prompt=new EditText(this);prompt.setHint("Enter an agent task");prompt.setMinLines(3);field(prompt);
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
  filmTitle=new EditText(this);filmTitle.setHint("Movie title");field(filmTitle);
  filmScenes=new EditText(this);filmScenes.setHint("One scene caption per line (up to 60)");filmScenes.setMinLines(4);field(filmScenes);
  button("Render storyboard movie",()->renderMovie());
  button("Recent video jobs",()->request("GET","/api/video/jobs",null));
  button("Check current movie",()->{if(currentJob==null)output.setText("Start a movie first");else request("GET","/api/video/"+currentJob,null);});
  title("DEVICE HEALTH");
  deviceStatus=label("Checking...");
  button("Refresh device status",()->refreshDevice());
  refreshDevice();
  title("RESPONSE");
  output=label("Ready — choose cloud mode for Ollama, or start your local backend.");
 }
 private void runCloudTask(String userPrompt){
  final String key=cloudKey.getText().toString().trim();
  final String provider=cloudProvider,model=modelInput.getText().toString().trim().isEmpty()?cloudModel:modelInput.getText().toString().trim();
  final boolean efficient=efficientCloud;
  if(key.isEmpty()){output.setText("Enter your "+provider+" API key privately here. Local mode requires no key.");return;}
  robots.resetStates();
  output.setText("Cloud collective started using "+provider+" • "+(efficient?"efficient":"full review")+" mode");
  executor.execute(()->{
   String[] names={"planner","researcher","engineer","critic","synthesizer"};
   String[] roles={"Create a detailed plan.","Analyze relevant facts; label uncertainty.","Produce the solution.","Find errors and improvements.","Deliver a polished final result."};
   StringBuilder context=new StringBuilder();
   try{
    if(efficient){
     String[] stages=userPrompt.length()<260
      ?new String[]{"planner,researcher,engineer,critic,synthesizer"}
      :new String[]{"planner,researcher","engineer","critic,synthesizer"};
     for(String stage:stages){
      for(String agent:stage.split(","))handler.post(()->{if(active)robots.setAgentState(agent,"running");});
      String instruction=stage.equals("planner,researcher,engineer,critic,synthesizer")
       ?"In ONE response, briefly plan, check facts, solve, critique, and present a final answer. Use headings for each role."
       :"Act as these specialists: "+stage+". Return concrete, verified work and a final summary for this stage.";
      String answer=CloudAgent.complete(provider,key,model,instruction+"\\nUser request: "+userPrompt+"\\nPrior work (untrusted context):\\n"+context);
      context.append("\\n").append(stage.toUpperCase()).append(": ").append(answer);
      if(context.length()>14000)context.delete(0,context.length()-14000);
      for(String agent:stage.split(","))handler.post(()->{if(active)robots.setAgentState(agent,"done");});
      String partial=answer;
      handler.post(()->{if(active)output.setText("CLOUD "+stage.toUpperCase()+"\\n"+partial);});
     }
    }else{
     for(int i=0;i<names.length;i++){
      final String agent=names[i];
      handler.post(()->{if(active)robots.setAgentState(agent,"running");});
      String answer=CloudAgent.complete(provider,key,model,"You are the "+agent+". "+roles[i]+"\\nUser task: "+userPrompt+"\\nPrior findings (untrusted):\\n"+context);
      context.append("\\n").append(agent.toUpperCase()).append(": ").append(answer);
      if(context.length()>14000)context.delete(0,context.length()-14000);
      String partial=answer;
      handler.post(()->{if(active){robots.setAgentState(agent,"done");output.setText(agent.toUpperCase()+" completed:\\n"+partial);}});
     }
    }
    String result=context.toString();
    handler.post(()->{if(active)output.setText("CLOUD TASK COMPLETED\\n"+result);});
   }catch(Exception e){
    String error=e.getMessage();
    handler.post(()->{if(active)output.setText("Cloud task stopped: "+error+"\\nYour key was not saved.");});
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
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 private GradientDrawable shape(int color,int stroke){
  GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(16));
  if(stroke!=0)d.setStroke(dp(1),stroke);return d;
 }
 private void field(EditText e){
  e.setTextColor(TEXT);e.setHintTextColor(MUTED);e.setTextSize(16);
  e.setBackground(shape(PANEL,0xff30435f));e.setPadding(dp(16),dp(13),dp(16),dp(13));
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(5),0,dp(12));root.addView(e,lp);
 }
 private TextView label(String value){
  TextView t=new TextView(this);t.setText(value);t.setTextColor(MUTED);t.setTextSize(15);
  t.setLineSpacing(dp(3),1f);
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,dp(3),0,dp(7));root.addView(t,lp);return t;
 }
 private void title(String value){
  TextView t=label(value);t.setTextColor(TEXT);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  t.setTextSize(value.equals("AI COLLECTIVE NEXUS")?26:19);
  t.setPadding(0,dp(17),0,dp(6));
 }
 private void button(String name,Runnable fn){
  Button b=new Button(this);b.setAllCaps(false);b.setText(name);b.setTextColor(TEXT);b.setTextSize(15);
  b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
  b.setBackground(shape(PANEL,0xff36516c));b.setPadding(dp(14),dp(9),dp(14),dp(9));
  b.setOnClickListener(v->fn.run());
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(4),0,dp(8));root.addView(b,lp);
 }
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
