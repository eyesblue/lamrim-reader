package eyes.blue;

import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

public class TheoryPageView extends TextView {

	public TheoryPageView(Context context) {
		super(context);
		this.setBackgroundColor(Color.WHITE);
		this.setTextColor(Color.BLACK);
		setEllipsize(null);
		// TODO Auto-generated constructor stub
	}
	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.setBackgroundColor(Color.WHITE);
		this.setTextColor(Color.BLACK);
		setEllipsize(null);
    }
	
	@Override
    protected void onDraw(Canvas canvas)
    {
		ArrayList<int[]> al=new ArrayList<int[]>();
        
        Paint paint = new Paint();
        //  将边框设为黑色
        paint.setColor(android.graphics.Color.YELLOW);
//        paint.setShadowLayer(5, this.getWidth()/2, this.getHeight()/2, android.graphics.Color.YELLOW);
//        Drawable da=this.getBackground();
//        this.setPaintFlags(getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);

        //  画TextView的4个边
        String text=(String) this.getText();
        int start=-1;
        int line=0;int index=0;
        String newStr="";
        for(char c: text.toCharArray()){
        	index++;
        	if(c=='\n'){line++;index=0;}
        	else if(c=='.'){
        		al.add(new int[]{line,index});
        		Log.d("LamrimReader","Find point at: "+line+", "+index);
        		continue;
        	}
        	newStr+=c;
        }
        setText(newStr);
        
        canvas.drawCircle(this.getWidth()/2, this.getHeight()/2, 30, paint);
        super.onDraw(canvas);
//        canvas.drawLine(0, 0, this.getWidth() - 1, 0, paint);
//        canvas.drawLine(0, 0, 0, this.getHeight() - 1, paint);
//        canvas.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1, paint);
//        canvas.drawLine(0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1, paint);
    }
}
