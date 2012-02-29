package eyes.blue;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class TheoryPageView extends TextView {
	boolean onCmd=false;
	float partSize=0;
	
	public TheoryPageView(Context context) {
		super(context);
	}

	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

	public void setTextSize(float size){
		super.setTextSize(size);
		Log.d("LamrimLeader","TheoryPageView.setTextSize() Set font size to "+size);
	}
	

	@Override
    protected void onDraw(Canvas canvas)
    {
		ArrayList<Integer> cmdList=new ArrayList<Integer>();
        partSize=(float) (getTextSize()*0.7);
        float wordLen=getPaint().measureText("中");
        float orgTextSize=getTextSize();
        
        Rect bounds=new Rect();
        int pointSize=(int) (getTextSize()/7);
        
/*        for(int j=0;j<10;j++){
        	int baseLine=this.getLineBounds(j, bounds)+pointSize+2;
        	for(int i=0;i<35;i++){
        		canvas.drawCircle(bounds.left+(i*wordLen), baseLine, pointSize, paint);
        }}
*/
        // Draw Text
        String text = (String) super.getText();
        int lineCounter=0;
//        int wordCounter=0;
        int baseLine=0;
//        String words="";
        int start=0,end=0;
        int y=getLineBounds(lineCounter, bounds);
        int x=bounds.left;
        String cmd=null;
        for(int i=0;i<text.length();i++){
        	char c=text.charAt(i);
        	if(onCmd){
        		if(c!='>'){end++;continue;}
        		Log.d("LamrimReader","Find a command stop");
            	end=i;
            	onCmd=false;
            	cmd=text.substring(start, end);
            	Log.d("LamrimReader","Command: "+cmd+", start: "+start+", end: "+end);
            	char cmd1=0;
            	if((end-start)==1)cmd1=text.charAt(start);
            	else cmd1=text.substring(start, end).charAt(0);

            	
           		switch(cmd1){
           			case '/':
           				switch(cmd.charAt(1)){
           				case 'b':Log.d("LamrimReader","release bold command");getPaint().setFakeBoldText(false);break;
           				case 'n':Log.d("LamrimReader","release num command");getPaint().setColor(Color.BLACK);break;
           				case 's':Log.d("LamrimReader","release small command");getPaint().setTextSize(orgTextSize);break;
           				};break;
           			case 'b':Log.d("LamrimReader","set bold command");getPaint().setFakeBoldText(true);break;
           			case 'n':Log.d("LamrimReader","set num command");getPaint().setColor(Color.BLUE);break;
           			case 's':Log.d("LamrimReader","set small command");getPaint().setTextSize((float) (getTextSize()*0.93));break;
           		}
           		start=i+1;
           		end=i+1;
        	}
        	if(c=='.'){
        		if(text.charAt(start)!='.'){canvas.drawText(text, start, end, x, y, getPaint());
//        		wordCounter+=end-start;
        		x+=getPaint().measureText("中")*(end-start);
        		}
//        		Log.d("LamrimReader","Get point, Before:"+words);
        		canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		start=i+1;
        		end=i+1;
        		continue;
        	}
        	else if(c=='\n'){
//        		Log.d("LamrimReader","Get new line, draw text from "+start+" to "+end+",on ("+x+","+y+") text length="+text.length());
        		canvas.drawText(text, start, end, x, y, getPaint());
//        		x+=wordLen*words.length();
        		start=i+1;
        		end=i+1;
//        		wordCounter=0;
        		y=getLineBounds(++lineCounter, bounds);
        		x=bounds.left;
        		continue;
        	}
        	else if(c=='<'){
        		Log.d("LamrimReader","Find a command start");
        		if(end-start>0){
        			canvas.drawText(text, start, end-1, x, y, getPaint());
        			x+=getPaint().measureText("中")*(end-start-1);
        		}
        		
        		start=i+1;
        		end=i+1;
        		onCmd=true;
        	}
        	
        	else{
        		end++;
        	}
/* Print with copy data to new object "words"
        	char c=text.charAt(i);
        	if(c=='.'){
        		canvas.drawText(words, 0, words.length(), x, y, getPaint());
        		wordCounter+=words.length();
        		x+=wordLen*words.length();
        		
        		Log.d("LamrimReader","Get point, Before:"+words);
        		canvas.drawCircle(x, y+pointSize+2, pointSize, paint);
        		words="";
        		continue;
        	}
        	else if(c=='\n'){
        		Log.d("LamrimReader","Get new line.");
        		canvas.drawText(words, 0, words.length(), x, y, getPaint());
//        		x+=wordLen*words.length();
        		words="";
        		lineCounter++;
        		wordCounter=0;
        		y=getLineBounds(lineCounter, bounds);
        		x=bounds.left;
        		continue;
        	}
        	else{
        		words+=c;
        	}
*/        	
        	
//        	int baseLine=this.getLineBounds(lineCounter, bounds);
//        	canvas.translate(bounds.left, baseLine);
//        	canvas.drawText(c, 0, 0, getPaint());
        	
        }
//        canvas.drawLine(bounds.left, baseLine + 1, bounds.right, baseLine + 1, paint);
    }
}
