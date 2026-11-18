package org.aicollective.nexus;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** OpenRouter free model gateway. Key is supplied per session, never stored or logged. */
final class CloudAgent {
 static final String ENDPOINT="https://openrouter.ai/api/v1/chat/completions";
 static final String MODEL="openrouter/free";
 static String complete(String key,String prompt) throws Exception {
  if(key==null||key.trim().isEmpty())throw new IOException("OpenRouter key is required for cloud mode");
  JSONObject payload=new JSONObject();
  payload.put("model",MODEL);
  payload.put("stream",false);
  payload.put("max_tokens",600);
  JSONArray messages=new JSONArray();
  messages.put(new JSONObject().put("role","user").put("content",prompt));
  payload.put("messages",messages);
  HttpURLConnection conn=(HttpURLConnection)new URL(ENDPOINT).openConnection();
  try {
   conn.setRequestMethod("POST");
   conn.setConnectTimeout(10000);
   conn.setReadTimeout(90000);
   conn.setDoOutput(true);
   conn.setRequestProperty("Content-Type","application/json");
   conn.setRequestProperty("Authorization","Bearer "+key.trim());
   try(OutputStream out=conn.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
   int status=conn.getResponseCode();
   InputStream stream=status>=400?conn.getErrorStream():conn.getInputStream();
   if(stream==null)throw new IOException("Cloud service returned HTTP "+status);
   ByteArrayOutputStream buffer=new ByteArrayOutputStream();
   try(InputStream in=stream){
    byte[] block=new byte[4096];int count;
    while((count=in.read(block))!=-1){buffer.write(block,0,count);if(buffer.size()>1000000)throw new IOException("Response too large");}
   }
   JSONObject response=new JSONObject(buffer.toString("UTF-8"));
   if(status>=400)throw new IOException("Cloud service HTTP "+status+": "+(response.optJSONObject("error")==null?"Request failed":response.optJSONObject("error").optString("message","Request failed")));
   return response.getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content","");
  }finally{conn.disconnect();}
 }
}
