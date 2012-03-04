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
	final static boolean debug=false;

	
	public TheoryPageView(Context context) {
		super(context);
	}

	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

	public void setTextSize(float size){
		super.setTextSize(size);
		if(debug)Log.d("LamrimLeader","TheoryPageView.setTextSize() Set font size to "+size);
	}
	

	@Override
    protected void onDraw(Canvas canvas)
    {
        float orgTextSize=getTextSize();
        Rect bounds=new Rect();
        int pointSize=(int) (getTextSize()/7);
        String text = (String) super.getText();
        int lineCounter=0;
        int start=0,end=0;
        int y=getLineBounds(lineCounter, bounds);
        int x=bounds.left;

        for(int i=0;i<text.length();i++){
        	char c=text.charAt(i);
        	if(onCmd){
        		if(c!='>'){end++;continue;}
        		if(debug)Log.d("LamrimReader","Find a command stop");
            	onCmd=false;
            	
           		switch(text.charAt(start)){
           			case '/':
           				switch(text.charAt(start+1)){
           				case 'b':if(debug)Log.d("LamrimReader","release bold command");getPaint().setFakeBoldText(false);break;
           				case 'n':if(debug)Log.d("LamrimReader","release num command");getPaint().setColor(Color.BLACK);break;
           				case 's':if(debug)Log.d("LamrimReader","release small command");getPaint().setTextSize(orgTextSize);break;
           				};break;
           			case 'b':if(debug)Log.d("LamrimReader","set bold command");getPaint().setFakeBoldText(true);break;
           			case 'n':if(debug)Log.d("LamrimReader","set num command");getPaint().setColor(Color.BLUE);break;
           			case 's':if(debug)Log.d("LamrimReader","set small command");getPaint().setTextSize((float) (getTextSize()*0.9));break;
           		}
           		start=i+1;
           		end=start;
        	}
        	else if(c=='.'){
        		if(text.charAt(start)!='.'){
        			canvas.drawText(text, start, end, x, y, getPaint());
        			x+=getPaint().measureText("中")*(end-start);
        		}
        		if(debug)Log.d("LamrimReader","Print "+text.substring(start, end)+", start: "+start+", end: "+end+", ("+(end-start)+")");
//        		Log.d("LamrimReader","Get point, Before:"+words);
        		canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		start=i+1;
        		end=start;
        		continue;
        	}
        	else if(c=='\n'){
//        		Log.d("LamrimReader","Get new line, draw text from "+start+" to "+end+",on ("+x+","+y+") text length="+text.length());
        		canvas.drawText(text, start, end, x, y, getPaint());
//        		x+=wordLen*words.length();
        		start=i+1;
        		end=start;
//        		wordCounter=0;
        		y=getLineBounds(++lineCounter, bounds);
        		x=bounds.left;
        		continue;
        	}
        	else if(c=='<'){
        		if(debug)Log.d("LamrimReader","Find a command start");
        		if(end-start>0){
        			canvas.drawText(text, start, end, x, y, getPaint());
        			x+=getPaint().measureText("中")*(end-start);
        		}
        		
        		start=i+1;
        		end=start;
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
