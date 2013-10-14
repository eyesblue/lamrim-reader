package eyes.blue.modified;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.ListView;

public class MyListView extends ListView {
	Context context;
	ScaleGestureDetector scaleGestureDetector=null;
	OnDoubleTapEventListener doubleTapEventListener=null;
	public MyListView(Context context) {super(context);this.context=context;}
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		boolean res=false;
		if(scaleGestureDetector!=null && event.getPointerCount()==2){
			// The scale gesture detector always return true.
			res=scaleGestureDetector.onTouchEvent(event);
			Log.d(getClass().getName(),"Scale return "+res);
			return res;
		}
		res=super.onTouchEvent(event) | gestureListener.onTouchEvent(event) ;
		Log.d(getClass().getName(),"TheoryPageView onTouchEvent return "+res);
		return res;
	}
	
	public void setScaleGestureDetector(ScaleGestureDetector scaleGestureDetector){this.scaleGestureDetector=scaleGestureDetector;}
	
	GestureDetector gestureListener=new GestureDetector(context ,new android.view.GestureDetector.SimpleOnGestureListener(){
		@Override
		public boolean 	onDown(MotionEvent e){
			if(e.getPointerCount()> 1)return false;
			return true;
		}
		@Override
		public boolean 	onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){return true;}
		@Override
		public boolean 	onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY){
			Log.d(getClass().getName(),"Into onScroll, distance("+distanceX+", "+distanceY+"), scroll point=("+getScrollX()+", "+getScrollY()+")");
			float scrollX=getScrollX()+distanceX;
//			float scrollY=getScrollY()+distanceY;
			float rightBoundY=getHeight()-getMeasuredHeight();
			float rightBoundX=getWidth()-getMeasuredWidth();
//			Log.d(getClass().getName(),"Layout params: ("+getLayout().getWidth()+", "+getLayout().getHeight()+", content size: ("+getWidth()+", "+getHeight()+"), meansure size: "+getMeasuredWidth()+", "+getMeasuredHeight());
			// reached Up/Left bound.
			if(scrollX<=0)scrollX=0;
//			if(scrollY<=0)scrollY=0;
			
				
/*			Log.d(getClass().getName(),"textWidth: "+textWidth+", scrollX: "+getScrollX()+", getMeasuredWidth: "+getMeasuredWidth());
			// Reached Right/bottom bound.
			if(textWidth-getMeasuredWidth()-getScrollX()<=0){
				Log.d(getClass().getName(),"Right bound reached Return false");
				return false;
			}			
*/			Log.d(getClass().getName(),"Scroll to ("+scrollX+", "+getScrollY()+")");
			scrollTo((int)scrollX,(int)getScrollY());
			
			// Left bound has reached, and still scroll to left
			//this.onDoubleTapEvent(e)
			if(scrollX==0 && distanceX < 0)return false;
			return true;
		}
		@Override
		public boolean onDoubleTapEvent(MotionEvent e){
			return doubleTapEventListener.onDoubleTap(e);
		}
	});
	
	int mFadeColor=0;
	@Override
	  public int getSolidColor()
	  {
	    return mFadeColor;
	  }
	  public void setFadeColor( int fadeColor )
	  {
	    mFadeColor = fadeColor;
	    this.invalidate();
	  }
	  public int getFadeColor()
	  {
	    return mFadeColor;
	  }
	  
	public void setOnDoubleTapEventListener(OnDoubleTapEventListener listener){this.doubleTapEventListener=listener;}
}
