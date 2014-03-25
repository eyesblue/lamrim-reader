package eyes.blue.modified;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eyes.blue.GaLogger;
import eyes.blue.R;
import eyes.blue.SpeechData;
import eyes.blue.TheoryData;
import eyes.blue.TheoryPageView;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MyListView extends ListView {
	Context context;
	SharedPreferences runtime = null;
	Typeface educFont = null;
	TheoryListAdapter adapter = null;
	ArrayList<HashMap<String, String>> bookList = null;
	ScaleGestureDetector scaleGestureDetector=null;
	OnDoubleTapEventListener doubleTapEventListener=null;
//	View.OnTouchListener onTouchListener=null;
	int mFadeColor=0;
	int highlineLine[][];//[PageIndex][startLine][endLine]
	
	public MyListView(Context context) {
		super(context);this.context=context;
		init();
	}
	
	public MyListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context=context;
		init();
	}
	
	private void init(){
		runtime = context.getSharedPreferences(context.getString(R.string.runtimeStateFile), 0);
		educFont=Typeface.createFromAsset(context.getAssets(), "EUDC.TTF");
		bookList = new ArrayList<HashMap<String, String>>();
        int pIndex = 0;

        for (String value : TheoryData.content) {
                HashMap<String, String> item = new HashMap<String, String>();
                item.put("page", value);
                item.put("desc", "第 " + (++pIndex) + " 頁");
                bookList.add(item);
        }
        
        adapter = new TheoryListAdapter(context, bookList,	R.layout.theory_page_view, new String[] { "page", "desc" },	new int[] { R.id.pageContentView, R.id.pageNumView });
		setAdapter(adapter);
    	
    	setScaleGestureDetector(new ScaleGestureDetector(context,new SimpleOnScaleGestureListener() {
    		@Override
    		public boolean onScaleBegin(ScaleGestureDetector detector) {
    			Log.d(getClass().getName(),"Begin scale called factor: "+detector.getScaleFactor());
    			GaLogger.sendEvent("ui_action", "bookview_event", "change_text_size_start", null);
    			return true;
    		}
    		@Override
    		public boolean onScale(ScaleGestureDetector detector) {
    			float size=adapter.getTextSize()*detector.getScaleFactor();
//    				Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
    				adapter.setTextSize(size);
    				adapter.notifyDataSetChanged();
//    				Log.d(getClass().getName(),"set size after setting: "+adapter.getTextSize());
    				return true;
    			}
    		@Override
    		public void onScaleEnd(ScaleGestureDetector detector){
    			SharedPreferences.Editor editor = runtime.edit();
    			editor.putInt(context.getString(R.string.bookFontSizeKey), (int) adapter.getTextSize());
    			editor.commit();
    			GaLogger.sendEvent("ui_action", "bookview_event", "change_text_size_end", null);
    		}
    		}));
    	
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		
		boolean res=false;
		if(scaleGestureDetector==null)return false;
//		if(onTouchListener!=null)onTouchListener.
		if(event.getPointerCount()==2){
			// The scale gesture detector always return true.
			try{// Here will throw IllegalArgumentException sometimes.
				res=scaleGestureDetector.onTouchEvent(event);
			}catch(Exception e){
				e.printStackTrace();
				GaLogger.sendException(e, true);
				return false;
			}
//			Log.d(getClass().getName(),"Scale return "+res);
			return res;
		}
		
		try{// Here will throw IllegalArgumentException sometimes.
			res=super.onTouchEvent(event) | gestureListener.onTouchEvent(event) ;
		}catch(Exception e){
			e.printStackTrace();
			GaLogger.sendException(e, true);
			return false;
		}
//		Log.d(getClass().getName(),"TheoryPageView onTouchEvent return "+res);
		return res;
	}
//	public void setOnTouchListener(View.OnTouchListener onTouchListener){this.onTouchListener=onTouchListener;}
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
//			Log.d(getClass().getName(),"Into onScroll, distance("+distanceX+", "+distanceY+"), scroll point=("+getScrollX()+", "+getScrollY()+")");
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
			Log.d(getClass().getName(),"Scroll to ("+scrollX+", "+getScrollY()+")");
*/			scrollTo((int)scrollX,(int)getScrollY());
			
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
	public void setTextSize(float size){adapter.setTextSize(size);}
	public float getTextSize(){return adapter.getTextSize();}
	public void refresh(){adapter.notifyDataSetChanged();}
	public void setHighlightLine(int startPage, int startLine,int endPage, int endLine){
		int pageCount=endPage-startPage+1;
		highlineLine=new int[pageCount][3];
		if(pageCount==1){
			highlineLine[0][0]=startPage;
			highlineLine[0][1]=startLine;
			highlineLine[0][2]=endLine;
		}
		else{
			highlineLine[0][0]=startPage;
			highlineLine[0][1]=startLine;
			highlineLine[0][2]=-1;
			for(int i=1;i<pageCount-1;i++){
				highlineLine[i][0]=startPage+i;
				highlineLine[i][1]=0;
				highlineLine[i][2]=-1;
			}
			highlineLine[pageCount-1][0]=endPage;
			highlineLine[pageCount-1][1]=0;
			highlineLine[pageCount-1][2]=endLine;
		}
		
		// debug
		System.out.println("Content of highlineLine:");
		for(int i=0;i<pageCount;i++){
			System.out.println("["+highlineLine[i][0]+"] ["+highlineLine[i][1]+"] ["+highlineLine[i][2]+"]");
		}
		
		refresh();
	}
	public void clearHighlightLine(){
		highlineLine=null;
		((Activity)context).runOnUiThread(new Runnable(){
			@Override
			public void run() {
				refresh();
			}});
		
	}
	
	public float setViewToPosition(int page,int line){
/*		int firstView=getFirstVisiblePosition()+1;
		Log.d(getClass().getName(),"First view index: "+firstView);
		View v=getChildAt(firstView);
		TextView tpView=(TextView)v.findViewById(R.id.pageContentView);

		int padding=tpView.getPaddingTop();
*/		
		Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG);
		paint.setStyle(Paint.Style.FILL);
		paint.setTextAlign(Paint.Align.CENTER);
		paint.setTextSize(adapter.getTextSize());
		Rect bounds = new Rect();
		paint.getTextBounds("a", 0, 1, bounds);
		
		float textSize=bounds.height();
//		float shift=textSize*line/context.getResources().getDisplayMetrics().densityDpi*160f;
		float shift=-textSize*line*2.4f;
		Log.d(getClass().getName(),"Move view to page "+page+" shift "+shift);
		setSelectionFromTop(page, (int) shift);
		refresh();
		return shift;
	}
	
	public void search(String str){
		
	}
	
	class TheoryListAdapter extends SimpleAdapter {
		float textSize = 0;

		public TheoryListAdapter(Context context,List<? extends Map<String, ?>> data, int resource,	String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				Log.d(getClass().getName(), "row=null, construct it.");
				LayoutInflater inflater = ((Activity)context).getLayoutInflater();
				row = inflater.inflate(R.layout.theory_page_view, parent, false);
			}

			// / Log.d(logTag, "row=" + row+", ConvertView="+convertView);
			TheoryPageView bContent = (TheoryPageView) row.findViewById(R.id.pageContentView);
			bContent.setHorizontallyScrolling(true);
			// bContent.drawPoints(new int[0][0]);
			bContent.setTypeface(educFont);
			if (bContent.getTextSize() != textSize)
				bContent.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			bContent.setText(bookList.get(position).get("page"));
			if(highlineLine!=null && position>=highlineLine[0][0] && position<=highlineLine[highlineLine.length-1][0]){
				int index=position-highlineLine[0][0];
				bContent.setHighlightLine(highlineLine[index][1], highlineLine[index][2]);
			}
			// bContent.setText(Html.fromHtml("<font color=\"#FF0000\">No subtitle</font>"));
			TextView pNum = (TextView) row.findViewById(R.id.pageNumView);
			if (pNum.getTextSize() != textSize)
				pNum.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
			pNum.setText(bookList.get(position).get("desc"));
			return row;
		}

		public void setTextSize(float size) {
			textSize = size;
		}
		public float getTextSize(){
			return textSize;
		}
	}
}
