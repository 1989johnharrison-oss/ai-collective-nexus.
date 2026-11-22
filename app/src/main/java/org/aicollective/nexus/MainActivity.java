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
 private LinearLayout screenRoot;
 private LinearLayout[] pages=new LinearLayout[5];
 private Button[] navButtons=new Button[5];
 private ScrollView mainScroll;
 private int currentPage=0;
 private static final int BG=0xff0b1020, PANEL=0xff17233b, TEXT=0xffedf4ff, MUTED=0xffa8b8d2, ACCENT=0xff5eead4;
 private TextView output;
 private LinearLayout historyContainer;
 private TaskStore taskStore;
 private String lastTaskResponse="";
 private String availableArtifactUrl=null;
 private Button artifactButton;
 private TextView quickStatus;
 private TextView activitySummary;
 private TextView technicalDetails;
 private boolean detailsExpanded=false;
 private String lastTechnical="No diagnostic response yet.";
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
 private String lastStatusText="";
 private String lastActivityText="";
 private String lastAgentSnapshot="";
 private boolean pollInFlight=false;
 private final Runnable poll=new Runnable(){@Override public void run(){if(active&&taskRunning&&currentTask!=null&&!pollInFlight){pollInFlight=true;request("GET","/api/tasks/"+currentTask,null);}}};
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState);
  taskStore=new TaskStore(this);
  getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
  getWindow().getDecorView().setSystemUiVisibility(0);
  ScrollView scroll=new ScrollView(this);
  mainScroll=scroll;
  scroll.setFillViewport(true);
  scroll.setBackgroundColor(BG);
  screenRoot=new LinearLayout(this);screenRoot.setOrientation(LinearLayout.VERTICAL);screenRoot.setPadding(dp(14),dp(18),dp(14),dp(30));
  root=screenRoot;
  scroll.addView(screenRoot);setContentView(scroll);
  title("AI COLLECTIVE NEXUS");
  label("YOUR FIVE-AGENT AI WORKSPACE");
  label("NEXUS • DEVELOPMENT DASHBOARD");
  addNavigation();
  beginPage(0);
  connectionStatus=label("Backend: not checked • same-phone local AI");
  quickStatus=label("● READY  •  Choose an engine and launch a task");
  quickStatus.setTextColor(ACCENT);quickStatus.setBackground(shape(PANEL,0xff36516c));quickStatus.setPadding(dp(14),dp(12),dp(14),dp(12));
  title("LIVE ACTIVITY");
  activitySummary=label("No active task. Your agents will report their progress here.");
  activitySummary.setTextColor(TEXT);activitySummary.setBackground(shape(PANEL,0xff30435f));activitySummary.setPadding(dp(16),dp(14),dp(16),dp(14));
  button("◉ Show / hide technical details",()->{detailsExpanded=!detailsExpanded;technicalDetails.setVisibility(detailsExpanded?View.VISIBLE:View.GONE);technicalDetails.setText(lastTechnical);});
  technicalDetails=label(lastTechnical);technicalDetails.setTextSize(12);technicalDetails.setTextIsSelectable(true);technicalDetails.setVisibility(View.GONE);
  technicalDetails.setBackground(shape(PANEL,0xff30435f));technicalDetails.setPadding(dp(12),dp(12),dp(12),dp(12));
  label("Cloud mode works without Termux. Local mode requires the phone backend.");
  button("◉ Check local connection",()->request("GET","/api/health",null));
  button("⌁ Phone setup guide",()->{
   android.content.Intent browser=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse("https://github.com/1989johnharrison-oss/ai-collective-nexus."));
   startActivity(browser);
  });
  button("↻ Refresh agent activity",()->{if(currentTask!=null)request("GET","/api/tasks/"+currentTask,null);else request("GET","/api/agents",null);});
  title("THE BOT COLLECTIVE");
  robots=new RobotView(this);root.addView(robots,new LinearLayout.LayoutParams(-1,(int)(210*getResources().getDisplayMetrics().density)));
  label("PLANNER   •   RESEARCHER   •   ENGINEER   •   CRITIC   •   SYNTHESIZER");
  label("Agent colours indicate their roles. Task updates appear in Live Activity above.");
  button("Check bot status",()->request("GET","/api/agents",null));
  button("Check local AI model",()->request("GET","/api/model/status",null));
  beginPage(2);
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
  beginPage(4);
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
  beginPage(1);
  title("AI TASKS");
  prompt=new EditText(this);prompt.setHint("Enter an agent task");prompt.setMinLines(3);field(prompt);
  button("Launch task",()->{
   String p=prompt.getText().toString().trim();
   if(p.isEmpty()){output.setText("Enter a task");return;}
   if(useCloud){runCloudTask(p);return;}
   try{JSONObject data=new JSONObject();data.put("prompt",p);robots.resetStates();request("POST","/api/tasks",data);}catch(Exception e){output.setText(e.toString());}
  });
  button("↻ Load saved task history",()->loadTaskHistory());
  historyContainer=new LinearLayout(this);historyContainer.setOrientation(LinearLayout.VERTICAL);root.addView(historyContainer);
  loadTaskHistory();
  title("Performance");
  button("Eco",()->setMode("eco"));
  button("Balanced",()->setMode("balanced"));
  button("Performance",()->setMode("performance"));
  beginPage(3);
  title("MOVIE STUDIO");
  filmTitle=new EditText(this);filmTitle.setHint("Movie title");field(filmTitle);
  filmScenes=new EditText(this);filmScenes.setHint("One scene caption per line (up to 60)");filmScenes.setMinLines(4);field(filmScenes);
  button("Render storyboard movie",()->renderMovie());
  button("Recent video jobs",()->request("GET","/api/video/jobs",null));
  button("Check current movie",()->{if(currentJob==null)output.setText("Start a movie first");else request("GET","/api/video/"+currentJob,null);});
  beginPage(4);
  title("DEVICE HEALTH");
  deviceStatus=label("Checking...");
  button("Refresh device status",()->refreshDevice());
  refreshDevice();
  beginPage(1);
  title("TASK RESULT");
  artifactButton=new Button(this);artifactButton.setText("▶ Preview generated HTML game");artifactButton.setAllCaps(false);artifactButton.setTextColor(ACCENT);artifactButton.setBackground(shape(PANEL,0xff36516c));artifactButton.setVisibility(View.GONE);
  artifactButton.setOnClickListener(v->{if(availableArtifactUrl!=null){android.content.Intent browser=new android.content.Intent(android.content.Intent.ACTION_VIEW,android.net.Uri.parse(BASE+availableArtifactUrl));startActivity(browser);}});
  root.addView(artifactButton,new LinearLayout.LayoutParams(-1,dp(52)));
  output=label("Your completed task output will appear here.");
  output.setTextColor(TEXT);output.setTextIsSelectable(true);
  output.setBackground(shape(PANEL,0xff30435f));output.setPadding(dp(14),dp(12),dp(14),dp(12));
  showPage(0);
 }
 private void addNavigation(){
  String[] labels={"⌂ Home","◈ Tasks","▤ Memory","▣ Studio","⚙ Settings"};
  LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
  for(int i=0;i<labels.length;i++){
   final int index=i;
   Button b=new Button(this);b.setText(labels[i]);b.setAllCaps(false);b.setTextSize(11);
   b.setTextColor(TEXT);b.setPadding(0,0,0,0);
   LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(48),1);
   lp.setMargins(dp(2),dp(4),dp(2),dp(8));row.addView(b,lp);
   b.setOnClickListener(v->showPage(index));navButtons[i]=b;
  }
  screenRoot.addView(row);
 }
 private void beginPage(int index){
  if(pages[index]==null){
   LinearLayout page=new LinearLayout(this);page.setOrientation(LinearLayout.VERTICAL);
   pages[index]=page;screenRoot.addView(page);
  }
  root=pages[index];
 }
 private void showPage(int index){
  currentPage=index;
  for(int i=0;i<pages.length;i++){
   if(pages[i]!=null)pages[i].setVisibility(i==index?View.VISIBLE:View.GONE);
   if(navButtons[i]!=null){
    navButtons[i].setBackground(shape(i==index?0xff24565a:PANEL,i==index?ACCENT:0xff30435f));
    navButtons[i].setTextColor(i==index?ACCENT:TEXT);
   }
  }
  if(mainScroll!=null)mainScroll.post(()->mainScroll.smoothScrollTo(0,0));
 }
 private void runCloudTask(String userPrompt){
  final String key=cloudKey.getText().toString().trim();
  final String provider=cloudProvider,model=modelInput.getText().toString().trim().isEmpty()?cloudModel:modelInput.getText().toString().trim();
  final boolean efficient=efficientCloud;
  if(key.isEmpty()){output.setText("Enter your "+provider+" API key privately here. Local mode requires no key.");return;}
  robots.resetStates();
  status("Cloud collective started using "+provider+" • "+(efficient?"efficient":"full review")+" mode");
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
    handler.post(()->{if(active){output.setText("CLOUD TASK COMPLETED\\n"+result);activity("✓ CLOUD TASK COMPLETED\\n"+concise(result,650));status("Cloud task completed");}});
    try{taskStore.save(provider,userPrompt,result);handler.post(()->{if(active)loadTaskHistory();});}
    catch(Exception storageError){handler.post(()->{if(active)status("Cloud completed • could not save on device: "+storageError.getMessage());});}
   }catch(Exception e){
    String error=e.getMessage();
    handler.post(()->{if(active)output.setText("Cloud task stopped: "+error+"\\nYour key was not saved.");});
   }
  });
 }
 private void saveCloudHistory(String userPrompt,String result,String provider){
  HttpURLConnection conn=null;
  try{
   JSONObject body=new JSONObject();body.put("prompt",userPrompt);body.put("result",result);body.put("provider",provider);
   conn=(HttpURLConnection)new URL(BASE+"/api/cloud/history").openConnection();
   conn.setRequestMethod("POST");conn.setConnectTimeout(1800);conn.setReadTimeout(2500);
   conn.setDoOutput(true);conn.setRequestProperty("Content-Type","application/json");
   try(OutputStream out=conn.getOutputStream()){out.write(body.toString().getBytes(StandardCharsets.UTF_8));}
   int code=conn.getResponseCode();
   handler.post(()->{if(active&&code>=400)status("Cloud result ready • local history save failed (HTTP "+code+")");});
  }catch(Exception ignored){
   handler.post(()->{if(active)status("Cloud result ready • local backend offline, history not saved");});
  }finally{if(conn!=null)conn.disconnect();}
 }
 private void loadTaskHistory(){
  try{renderHistory(taskStore.recent());status("On-device cloud history loaded");}
  catch(Exception e){status("Cannot read on-device history: "+e.getMessage());}
  request("GET","/api/tasks",null);
 }
 private void renderHistory(org.json.JSONArray tasks){
  if(historyContainer==null)return;
  historyContainer.removeAllViews();
  if(tasks==null||tasks.length()==0){
   TextView empty=new TextView(this);empty.setText("No saved tasks yet. Run a task to build your history.");empty.setTextColor(MUTED);historyContainer.addView(empty);return;
  }
  for(int i=0;i<Math.min(tasks.length(),30);i++){
   JSONObject task=tasks.optJSONObject(i);if(task==null)continue;
   final String id=task.optString("id");
   final String result=task.optString("result");
   final String taskPrompt=task.optString("prompt");
   final String taskState=task.optString("status","unknown");
   LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);
   card.setBackground(shape(PANEL,0xff30435f));card.setPadding(dp(12),dp(10),dp(12),dp(10));
   LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(5),0,dp(9));historyContainer.addView(card,cp);
   TextView heading=new TextView(this);heading.setText(("done".equals(taskState)?"✓ ":"running".equals(taskState)?"◉ ":"! ")+taskState.toUpperCase()+" • "+concise(taskPrompt,110));
   heading.setTextColor(TEXT);heading.setTextSize(14);heading.setTypeface(Typeface.DEFAULT,Typeface.BOLD);card.addView(heading);
   Button open=new Button(this);open.setAllCaps(false);open.setText("View result / agent details");open.setTextColor(ACCENT);open.setBackground(shape(0xff20334b,0xff36516c));
   final boolean onDevice=task.optBoolean("local_record",false);
   open.setOnClickListener(v->{output.setText(result.isEmpty()?"No final result yet.":result);availableArtifactUrl=null;artifactButton.setVisibility(View.GONE);if(!onDevice)request("GET","/api/tasks/"+id,null);});
   card.addView(open,new LinearLayout.LayoutParams(-1,dp(44)));
  }
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
 private void status(String message){
  if(message.equals(lastStatusText))return;
  lastStatusText=message;
  if(quickStatus!=null)quickStatus.setText("● "+message);
 }
 private void activity(String message){if(message.equals(lastActivityText))return;lastActivityText=message;if(activitySummary!=null)activitySummary.setText(message);}
 private void diagnostics(String message){
  if(message.equals(lastTechnical))return;
  lastTechnical=message;
  if(technicalDetails!=null&&detailsExpanded)technicalDetails.setText(message);
 }
 private String concise(String text,int limit){
  if(text==null)return "";
  text=text.replace("\\n"," ").replace("\\r"," ").trim();
  return text.length()>limit?text.substring(0,limit)+"…":text;
 }
 private void presentResponse(String path,int code,String raw){
  diagnostics("HTTP "+code+" • "+path+"\\n"+raw);
  try{
   JSONObject json=new JSONObject(raw);
   if(path.equals("/api/health")){
    boolean ready=json.optBoolean("inference_ready",false);
    connectionStatus.setText("LOCAL ENGINE  •  "+(ready?"CONNECTED + MODEL READY":"BACKEND CONNECTED"));
    status(ready?"Local AI online and ready":"Backend online • checking model readiness");
    activity("◉ LOCAL CONNECTION\\n"+(ready?"Your AI model and Nexus backend are responding.":"The backend responded. The AI model may still be loading."));
   }else if(path.equals("/api/model/status")){
    boolean ready=code<400;
    status(ready?"AI model ready":"AI model not ready");
    activity("◉ LOCAL MODEL\\n"+(ready?"Model endpoint responded successfully.":"Model unavailable: "+concise(raw,180)));
   }else if(path.startsWith("/api/tasks/")){
    JSONObject task=json.optJSONObject("task");
    if(task!=null){
     JSONObject artifact=json.optJSONObject("artifact");
     String preview=artifact==null?"":artifact.optString("preview_url","");
     availableArtifactUrl=preview.startsWith("/api/artifacts/")?preview:null;
     if(artifactButton!=null)artifactButton.setVisibility(availableArtifactUrl==null?View.GONE:View.VISIBLE);
     String state=task.optString("status","unknown");
     org.json.JSONArray events=json.optJSONArray("events");
     StringBuilder view=new StringBuilder();
     view.append("◉ TASK  •  ").append(state.toUpperCase()).append("\\n");
     view.append(concise(task.optString("prompt"),160)).append("\\n\\n");
     String[] agents={"planner","researcher","engineer","critic","synthesizer"};
     for(String agent:agents){
      String agentState="waiting",detail="";
      if(events!=null)for(int i=0;i<events.length();i++){
       JSONObject event=events.optJSONObject(i);
       if(event!=null&&agent.equalsIgnoreCase(event.optString("agent"))){
        agentState=event.optString("status","waiting");
        detail=event.optString("detail","");
       }
      }
      String symbol="done".equals(agentState)?"✓":"running".equals(agentState)?"◉":"○";
      view.append(symbol).append(" ").append(agent.toUpperCase()).append("  •  ").append(agentState);
      if(!detail.isEmpty()&&!detail.equals("Working"))view.append("\\n    ").append(concise(detail,130));
      view.append("\\n");
     }
     if(!"running".equals(state)&&!task.optString("result").isEmpty()){
      view.append("\\nRESULT\\n").append(concise(task.optString("result"),1800));
     }
     activity(view.toString());
     if(!"running".equals(state)&&!task.optString("result").isEmpty()&&!task.optString("result").equals(lastTaskResponse)){
      lastTaskResponse=task.optString("result");output.setText(lastTaskResponse);
     }
     status("Task "+state+" • "+(events==null?0:events.length())+" updates");
    }
   }else if(path.equals("/api/tasks")&&"POST".equals("POST")&&json.has("id")){
    activity("◉ TASK STARTED\\nYour collective is processing the request. Agent updates will appear here.");
    status("Task accepted • agents starting");
   }else if(path.equals("/api/tasks")&&json.has("tasks")){
    org.json.JSONArray combined=taskStore.recent();
    org.json.JSONArray local=json.optJSONArray("tasks");
    if(local!=null)for(int i=0;i<local.length();i++)combined.put(local.optJSONObject(i));
    renderHistory(combined);
    status("Task history loaded");
   }else if(path.equals("/api/agents")){
    activity("◉ AGENT STATUS\\n"+concise(raw,450));
    status("Agent status received");
   }else if(path.equals("/api/knowledge")){
    activity("◉ PERSISTENT MEMORY\\n"+concise(raw,550));
    status("Memory response received");
   }else{
    activity("◉ "+path.toUpperCase()+"\\n"+concise(raw,450));
    status("HTTP "+code+" • "+(code<400?"Request complete":"Request failed"));
   }
  }catch(Exception e){
   status("HTTP "+code+" • response received");
   activity("◉ SERVER RESPONSE\\n"+concise(raw,350));
  }
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
  b.setOnClickListener(v->{status("Tapped: "+name);try{fn.run();}catch(Exception e){status("Error: "+e.getMessage());}});
  LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));lp.setMargins(0,dp(4),0,dp(8));root.addView(b,lp);
 }
 private void request(String method,String path,JSONObject payload){
  if(!path.startsWith("/api/tasks/"))status("Connecting to "+path+" …");
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
    String responseBody=new String(buf.toByteArray(),StandardCharsets.UTF_8);
    String result="HTTP "+code+"\n"+responseBody;
    handler.post(()->{
     if(!active)return;
     if(path.startsWith("/api/tasks/"))pollInFlight=false;
     presentResponse(path,code,responseBody);
     if(path.equals("/api/health")&&code>=400)connectionStatus.setText("Backend: HTTP "+code);
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
         String[] names={"planner","researcher","engineer","critic","synthesizer"};
         String[] states={"standby","standby","standby","standby","standby"};
         for(int i=0;i<events.length();i++){
          JSONObject event=events.optJSONObject(i);
          if(event!=null)for(int j=0;j<names.length;j++)if(names[j].equalsIgnoreCase(event.optString("agent")))states[j]=event.optString("status");
         }
         String snapshot=java.util.Arrays.toString(states);
         if(!snapshot.equals(lastAgentSnapshot)){
          lastAgentSnapshot=snapshot;
          for(int i=0;i<names.length;i++)robots.setAgentState(names[i],states[i]);
         }
        }
        if(!taskRunning){handler.removeCallbacks(poll);}
        else handler.postDelayed(poll,2500);
       }
      }
     }catch(Exception ignored){}
    });
   }catch(Exception e){handler.post(()->{if(active){pollInFlight=false;connectionStatus.setText("Backend: DISCONNECTED");taskRunning=false;handler.removeCallbacks(poll);status("Local connection failed");activity("◉ CONNECTION ERROR\\n"+e.getClass().getSimpleName()+": "+e.getMessage()+"\\nMake sure Termux is still running.");diagnostics(e.toString());}});}
   finally{if(conn!=null)conn.disconnect();}
  });
 }
 @Override protected void onDestroy(){active=false;handler.removeCallbacks(poll);executor.shutdownNow();if(taskStore!=null)taskStore.close();super.onDestroy();}
}
