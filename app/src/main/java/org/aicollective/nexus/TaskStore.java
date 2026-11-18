package org.aicollective.nexus;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONArray;
import org.json.JSONObject;

/** Private on-device history. No API keys are stored. */
final class TaskStore extends SQLiteOpenHelper {
 TaskStore(Context context){super(context,"nexus_tasks.db",null,1);}
 @Override public void onCreate(SQLiteDatabase db){
  db.execSQL("CREATE TABLE cloud_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, created INTEGER NOT NULL, provider TEXT NOT NULL, prompt TEXT NOT NULL, result TEXT NOT NULL)");
 }
 @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){}
 synchronized void save(String provider,String prompt,String result){
  android.content.ContentValues values=new android.content.ContentValues();
  values.put("created",System.currentTimeMillis());values.put("provider",provider);
  values.put("prompt",prompt);values.put("result",result);
  getWritableDatabase().insertOrThrow("cloud_tasks",null,values);
 }
 synchronized JSONArray recent(){
  JSONArray tasks=new JSONArray();
  try(Cursor cursor=getReadableDatabase().rawQuery("SELECT id,created,provider,prompt,result FROM cloud_tasks ORDER BY id DESC LIMIT 30",null)){
   while(cursor.moveToNext()){
    JSONObject item=new JSONObject();
    item.put("id","cloud-"+cursor.getLong(0));item.put("created",cursor.getLong(1)/1000.0);
    item.put("provider",cursor.getString(2));item.put("prompt",cursor.getString(3));
    item.put("result",cursor.getString(4));item.put("status","done");item.put("local_record",true);
    tasks.put(item);
   }
  }catch(Exception e){throw new IllegalStateException("Cannot read task history",e);}
  return tasks;
 }
}
