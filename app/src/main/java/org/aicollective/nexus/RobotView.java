package org.aicollective.nexus;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Five animated robots. Visual animation is local; work status is reported by the backend. */
public final class RobotView extends View {
 private final Paint p=new Paint(3);
 private final String[] names={"PLANNER","RESEARCHER","ENGINEER","CRITIC","SYNTHESIZER"};
 private final int[] colors={0xff68d8ff,0xffa18cff,0xff6ef2b4,0xffffb66e,0xffff7fb1};
 private int selected=-1;
 private boolean working=false;
 public RobotView(Context c){super(c);setMinimumHeight(dp(210));}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+0.5f);}
 public void setSelected(int i){selected=i;invalidate();}
 public void setWorking(boolean value){working=value;invalidate();}
 @Override protected void onDraw(Canvas c){
  super.onDraw(c);
  float w=getWidth(),h=getHeight(),cell=w/5f;
  float t=(SystemClock.uptimeMillis()%2500)/2500f;
  c.drawColor(0xff101a2b);
  for(int i=0;i<5;i++){
   float cx=cell*(i+.5f),cy=h*.39f;
   float bob=(float)Math.sin(t*Math.PI*2+i*.9f)*dp(3);
   int color=colors[i];
   p.setColor(0xff26344a);p.setStyle(Paint.Style.FILL);
   c.drawRoundRect(new RectF(cx-cell*.42f,dp(10),cx+cell*.42f,h-dp(10)),dp(13),dp(13),p);
   p.setColor(color);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(dp(i==selected?2.5f:1.2f));
   c.drawRoundRect(new RectF(cx-cell*.42f,dp(10),cx+cell*.42f,h-dp(10)),dp(13),dp(13),p);
   p.setStyle(Paint.Style.FILL);
   float head=Math.min(cell*.28f,dp(27));
   p.setColor(color);c.drawRoundRect(new RectF(cx-head,cy-head+bob,cx+head,cy+head+bob),dp(9),dp(9),p);
   p.setColor(0xff0b1726);
   c.drawCircle(cx-head*.42f,cy-head*.12f+bob,Math.max(dp(2),head*.14f),p);
   c.drawCircle(cx+head*.42f,cy-head*.12f+bob,Math.max(dp(2),head*.14f),p);
   p.setStrokeWidth(dp(2));c.drawRect(cx-head*.3f,cy+head*.35f+bob,cx+head*.3f,cy+head*.43f+bob,p);
   p.setColor(0xffcbd5e1);p.setTextAlign(Paint.Align.CENTER);
   p.setTextSize(Math.min(dp(10),cell*.13f));p.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
   c.drawText(names[i],cx,h-dp(37),p);
   p.setColor(working?0xff68f0a0:0xff8190a5);
   p.setTextSize(dp(9));p.setTypeface(android.graphics.Typeface.DEFAULT);
   c.drawText(working?"ACTIVE":"STANDBY",cx,h-dp(21),p);
  }
  if(isShown())postInvalidateDelayed(70);
 }
}
