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
/*        
        Paint paint = new Paint();

        paint.setColor(android.graphics.Color.BLUE);



        
        
        float wordLen=getPaint().measureText("中");

        Rect bounds=new Rect();
        int pointSize=(int) (getTextSize()/7);
        
        for(int j=0;j<10;j++){
        	int baseLine=this.getLineBounds(j, bounds)+pointSize+2;
        	
        for(int i=0;i<35;i++){
        	canvas.drawCircle(bounds.left+(i*wordLen), baseLine, pointSize, paint);
        }}
*/
//        canvas.drawLine(bounds.left, baseLine + 1, bounds.right, baseLine + 1, paint);
    }
}
