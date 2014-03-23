package eyes.blue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class TheoryPageView extends TextView {
	boolean onCmd=false;
	final static boolean debug=false;

	// For onTouchListener
	static final int NONE = 0;  
	static final int ZOOM = 1;  
	int mode = NONE;  
	static final int MIN_FONT_SIZE = 20;  
	static final int MAX_FONT_SIZE = 150;  
	float orgDist = 1f;
	float orgFontSize=0;
	float[][] dots=new float[100][3];
	Paint samplePaint=new Paint();
	
	public TheoryPageView(Context context) {
		super(context);
//		this.setOnTouchListener(touchListener);
	}

	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
 //       this.setOnTouchListener(touchListener);
    }
	
	public void highlightWord(int startIndex, int length){
		SpannableStringBuilder text=new SpannableStringBuilder(getText());
		String str=text.toString();
		int invalidStrCount=0;
		for(int i=startIndex;i<startIndex+length;i++){
			if(str.charAt(i)=='\n')
				invalidStrCount++;
		}
		
		int strLen=length+invalidStrCount;
		text.setSpan(new BackgroundColorSpan(0xFFFFFF00), startIndex, strLen, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
		super.setText(text);
	}
	
	
	/*
	 * Set highlight to whole line, from startLine to endLine, assign -1 to endLine that mean to whole end of line, if you want clear highlight call setText(String text).
	 * */
	public void setHighlightLine(int startLine, int endLine){
		Log.d(getClass().getName(),"get setHighlightLine call: startLine="+startLine+", endLine="+endLine);
		SpannableStringBuilder text=new SpannableStringBuilder(getText());
		String str=text.toString();
		String[] lines = str.split("\n");
		if(endLine==-1){
			endLine=lines.length-1;
			Log.d(getClass().getName(),"highlight to end: "+endLine);
		}
		int wordCounter=0;

		for(int i=0;i<lines.length;i++){
			if(i>=startLine && i<=endLine){
				Log.d(getClass().getName(),"highlight: start="+wordCounter+", end="+wordCounter+lines[i].length());
				text.setSpan(new BackgroundColorSpan(0xFFFFFF00), wordCounter, wordCounter+lines[i].length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
			}
			wordCounter+=lines[i].length()+1;
		}
		super.setText(text);
	}

	public void setText(String text){
		int lineCounter=0;
        int start=0,end=0;
        float smallSize=(float)getContext().getResources().getInteger(R.integer.theorySmallTextSizePercent)/100;
        int numColor=getContext().getResources().getColor(R.color.theoryNumTextColor);
        int boldColor=getContext().getResources().getColor(R.color.theoryBoldColor);
        
		SpannableStringBuilder  page = new SpannableStringBuilder ();
        SpannableStringBuilder  line = new SpannableStringBuilder ();
        
        boolean isBold=false, isNum=false, isSmall=false;
        int dotIndex=0;
        
		for(int i=0;i<text.length();i++){
        	char c=text.charAt(i);
        	if(onCmd){
        		if(c!='>'){end++;continue;}
        		if(debug)Log.d("LamrimReader","Find a command stop");
            	onCmd=false;
            	
           		switch(text.charAt(start)){
           			case '/':
           				switch(text.charAt(start+1)){
           					case 'b':if(debug)Log.d("LamrimReader","release bold command");isBold=false;break;
           					case 'n':if(debug)Log.d("LamrimReader","release num command");isNum=false;;break;
           					case 's':if(debug)Log.d("LamrimReader","release small command");isSmall=false;break;
           				};
           				break;
           			case 'b':if(debug)Log.d("LamrimReader","set bold command");isBold=true;break;
           			case 'n':if(debug)Log.d("LamrimReader","set num command");isNum=true;break;
           			case 's':if(debug)Log.d("LamrimReader","set small command");isSmall=true;break;
           		}
           		start=i+1;
           		end=start;
        	}
        	else if(c=='.'){
        		if(text.charAt(start)!='.'){
        			SpannableString str=new SpannableString (text.substring(start, end));
        			if(isBold){
        				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			}
        			if(isNum)str.setSpan(new ForegroundColorSpan(numColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			line.append(str);
        			dots[dotIndex][0]=lineCounter;
        			dots[dotIndex][1]=line.length();
        			dots[dotIndex][2]= (((isSmall)?smallSize:1));
        			//canvas.drawText(text, start, end, x, y, getPaint());
        			//x+=getPaint().measureText("中")*(end-start);
        		}
        		if(debug)Log.d("LamrimReader","Print "+text.substring(start, end)+", start: "+start+", end: "+end+", ("+(end-start)+")");
//        		Log.d("LamrimReader","Get point, Before:"+words);
        		//canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		dots[dotIndex][0]=lineCounter;
    			dots[dotIndex][1]=line.length();
    			//dots[dotIndex][2]=Math.round(((isSmall)?getPaint().measureText("中")*smallSize:getPaint().measureText("中"))*line.length());
    			dots[dotIndex][2]= (((isSmall)?smallSize:1));
    			dotIndex++;
    			
        		start=i+1;
        		end=start;
        		continue;
        	}
        	else if(c=='\n'){
//        		Log.d("LamrimReader","Get new line, draw text from "+start+" to "+end+",on ("+x+","+y+") text length="+text.length());
        		//canvas.drawText(text, start, end, x, y, getPaint());
        		SpannableString str=new SpannableString (text.substring(start, end)+"\n");
        		if(isBold){
    				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			}
        		if(isNum)str.setSpan(new ForegroundColorSpan(numColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			line.append(str);
    			page.append(line);
    			line.clear();
        		start=i+1;
        		end=start;
        		lineCounter++;
        		continue;
        	}
        	else if(c=='<'){
        		if(debug)Log.d("LamrimReader","Find a command start");
        		if(end-start>0){
        			SpannableString str=new SpannableString (text.substring(start, end));
        			if(isBold){
        				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			}
        			if(isNum)str.setSpan(new ForegroundColorSpan(numColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        			line.append(str);
        			//page.append(line);
        			//canvas.drawText(text, start, end, x, y, getPaint());
        			//x+=getPaint().measureText("中")*(end-start);
        		}
        		
        		
        		start=i+1;
        		end=start;
        		onCmd=true;
        	}
        	else if(i==text.length()-1){
        		if(end-start<0)continue;
        		SpannableString str=new SpannableString (text.substring(start, end+1));
    			if(isBold){
    				str.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    				str.setSpan(new ForegroundColorSpan(boldColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			}
    			if(isNum)str.setSpan(new ForegroundColorSpan(numColor), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			if(isSmall)str.setSpan(new RelativeSizeSpan(smallSize), 0, str.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
    			line.append(str);
//    			page.append(line);
        	}
        	else{
        		end++;
        	}
        }
		page.append(line);

		dots[dotIndex][0]=-1;
		dots[dotIndex][1]=-1;
		dots[dotIndex][2]=-1;
		super.setText(page);
    }
	
	
	
	@Override
	public void setTextSize(float size){
		super.setTextSize(size);
//		this.setOnTouchListener(touchListener);
		if(debug)Log.d("LamrimLeader","TheoryPageView.setTextSize() Set font size to "+size);
	}
	
	@Override
    protected void onDraw(Canvas canvas)
    {
		super.onDraw(canvas);
		int pointSize=(int) (getTextSize()/7);
		int yShift=(int) (getTextSize()/5);
		int dotColor=getContext().getResources().getColor(R.color.theoryDotTextColor);
		getPaint().setColor(dotColor);
		
//		StaticLayout tempLayout = new StaticLayout(boldText, paint, 10000, android.text.Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false);
//		int lineCount = tempLayout.getLineCount();
		
		int count=0;
		float orgTextSize=getTextSize();
		
//WG		paint.setTextSize(orgTextSize);
		String[] lineContent=getText().toString().split("\n");
		for(float[] d:dots){
			if(d[0]==-1)break;
			Rect rect=new Rect();
			count++;
			
			int y=getLineBounds((int) d[0], rect);
			float fontSize=orgTextSize*d[2];
			samplePaint.setTextSize(fontSize);
			canvas.drawCircle(rect.left+(samplePaint.measureText(lineContent[(int) d[0]],0,(int) d[1])), y+yShift, pointSize, getPaint());
		}
		
		getPaint().setTextSize(orgTextSize);
/////		super.onDraw(canvas);
    }
/*
	@Override
    protected void onDraw(Canvas canvas)
    {
        float orgTextSize=getTextSize();
        Rect bounds=new Rect();
        int pointSize=(int) (getTextSize()/7);
        String text = (String) super.getText().toString();
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
        		canvas.drawCircle(x, y+pointSize+2, pointSize, getPaint());
        		start=i+1;
        		end=start;
        		continue;
        	}
        	else if(c=='\n'){
        		canvas.drawText(text, start, end, x, y, getPaint());
        		start=i+1;
        		end=start;
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
        	
        	
//        	int baseLine=this.getLineBounds(lineCounter, bounds);
//        	canvas.translate(bounds.left, baseLine);
//        	canvas.drawText(c, 0, 0, getPaint());
        	
        }
//        canvas.drawLine(bounds.left, baseLine + 1, bounds.right, baseLine + 1, paint);
    }
*/
	
	OnTouchListener touchListener=new OnTouchListener(){
		public boolean onTouch(View v, MotionEvent event) {
			float x,y;
//			editText = (EditText) findViewById(R.id.editText1);  
			
			switch (event.getAction() & MotionEvent.ACTION_MASK) {
			case MotionEvent.ACTION_DOWN:
				
				// return false for long click listener
				Log.d(getClass().getName(),"First click event in to onTouchListener, return fals.");
				return false;

			case MotionEvent.ACTION_POINTER_DOWN:
				
				if(event.getActionIndex()==1){
					Log.d(getClass().getName(),"Second click event in to onTouchListener.");
				x = (event.getX(0) - event.getX(1));  
				y = event.getY(0) - event.getY(1);  
				orgDist = (float) Math.sqrt(x * x + y * y);
				if (orgDist > 12f) {
					mode = ZOOM;
					orgFontSize=getTextSize();
				}
				return true;
				}
				
				 
				break;
			case MotionEvent.ACTION_POINTER_UP:  
				mode = NONE;  
				break;  
			case MotionEvent.ACTION_MOVE:  
				if (mode == ZOOM && event.getActionIndex()==1) {
					x = (event.getX(0) - event.getX(1));  
				    y = event.getY(0) - event.getY(1);  
					float newDist = (float) Math.sqrt(x * x + y * y);
					float dp=(newDist - orgDist) * getResources().getDisplayMetrics().density;
					float size = orgFontSize+dp/12;
					Log.d(getClass().getName(),"OrgDist="+orgDist+", Dist= "+dp+"dp, scale="+size);
	 
					if ((size < MAX_FONT_SIZE && size > MIN_FONT_SIZE)) {  
						setTextSize(TypedValue.COMPLEX_UNIT_PX, size);  
					}  
				}  
				break;  
			}  
			return true;  
		  }  	
	};
}
