package eyes.blue.modified;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class MyLinearLayout extends LinearLayout {
	MyLinearLayoutController touchEventListener=null;
//	GestureDetector gestureListener=null;
	public MyLinearLayout(Context context) {super(context);}
	public MyLinearLayout(Context context, AttributeSet attrs) {super(context, attrs);}

//	public void setGestureListener(GestureDetector gd){gestureListener=gd;}
	public void setOnInterceptTouchEvent(MyLinearLayoutController touchEventListener){this.touchEventListener=touchEventListener;};
	
	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev){
		boolean res= touchEventListener.onInterceptTouchEvent(ev);
		return res;
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent ev){
		Log.d(getClass().getName(),"Into onTouchEvent of MyLinearLayout.");
		boolean res= touchEventListener.onTouchEvent(ev);
		Log.d(getClass().getName(),"Return: "+res);
		return true;
	}

}
