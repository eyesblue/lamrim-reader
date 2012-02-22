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
	int[][] points=null;

	public TheoryPageView(Context context) {
		super(context);
		this.setTextColor(Color.BLACK);
		setEllipsize(null);
		// TODO Auto-generated constructor stub
	}
	public TheoryPageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
		this.setTextColor(Color.BLACK);
		setEllipsize(null);
    }
	
	public void setPointPosition(int[][] points){
		this.points=points;
	}
	
	@Override
    protected void onDraw(Canvas canvas)
    {
		super.onDraw(canvas);
        
        Paint paint = new Paint();
        //  将边框设为黑色
        paint.setColor(android.graphics.Color.BLUE);
//        paint.setShadowLayer(5, this.getWidth()/2, this.getHeight()/2, android.graphics.Color.YELLOW);
//        Drawable da=this.getBackground();
//        this.setPaintFlags(getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);


        
        
        float wordLen=getPaint().measureText("中");

        Rect bounds=new Rect();
        int baseLine=this.getLineBounds(0, bounds);
        
        for(int i=0;i<35;i++){
        	canvas.drawCircle(bounds.left+(i*wordLen), baseLine, 5, paint);
        }
//        canvas.drawLine(bounds.left, baseLine + 1, bounds.right, baseLine + 1, paint);
//        canvas.drawCircle(bounds.right, bounds.bottom, 30, paint);
        
//        canvas.drawLine(0, 0, this.getWidth() - 1, 0, paint);
//        canvas.drawLine(0, 0, 0, this.getHeight() - 1, paint);
//        canvas.drawLine(this.getWidth() - 1, 0, this.getWidth() - 1, this.getHeight() - 1, paint);
//        canvas.drawLine(0, this.getHeight() - 1, this.getWidth() - 1, this.getHeight() - 1, paint);
    }
}
