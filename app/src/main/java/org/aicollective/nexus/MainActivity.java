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
 private boolean active=true;
 @Override public void onCreate(Bundle savedInstanceState){
  super.onCreate(savedInstanceState);
  ScrollView scroll=new ScrollView(this);
  root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);
  scroll.addView(root);setContentView(scroll);
  title("AI COLLECTIVE NEXUS");
  label("Android controller • 12 GB device profile");
  label("Requires a Python backend connected over USB (adb reverse).");
  button("Check backend connection",()->request("GET","/api/health",null));
  title("AI Agents");
  prompt=new EditText(this);prompt.setHint("Enter an agent task");prompt.setMinLines(3);root.addView(prompt);
  button("Launch task",()->{
   String p=prompt.getText().toString().trim();
   if(p.isEmpty()){output.setText("Enter a task");return;}
   try{JSONObject data=new JSONObject();data.put("prompt",p);request("POST","/api/tasks",data);}catch(Exception e){output.setText(e.toString());}
  });
  button("Task history",()->request("GET","/api/tasks",null));
  title("Performance");
  button("Eco",()->setMode("eco"));
  button("Balanced",()->setMode("balanced"));
  button("Performance",()->setMode("performance"));
  title("Movie Studio");
  button("Recent video jobs",()->request("GET","/api/video/jobs",null));
  title("Response");
  output=label("Ready");
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
    handler.post(()->{if(active)output.setText(result);});
   }catch(Exception e){handler.post(()->{if(active)output.setText("Backend unavailable. Run adb reverse tcp:8765 tcp:8765 and start the Python backend.\n"+e);});}
   finally{if(conn!=null)conn.disconnect();}
  });
 }
 @Override protected void onDestroy(){active=false;executor.shutdownNow();super.onDestroy();}
}
