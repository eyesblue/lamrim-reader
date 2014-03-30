package eyes.blue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;

import yuku.ambilwarna.AmbilWarnaDialog;
import yuku.ambilwarna.AmbilWarnaDialog.OnAmbilWarnaListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.support.v4.app.FragmentManager;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.SubMenu;

import android.view.MenuInflater;

import com.actionbarsherlock.view.MenuItem;

import android.view.Display;
import android.view.DragEvent;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.Scroller;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import eyes.blue.SpeechMenuActivity.SpeechListAdapter;
import eyes.blue.modified.MyListView;
import eyes.blue.modified.MyHorizontalScrollView;
import eyes.blue.modified.OnDoubleTapEventListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;

/**
* 更新: $$Date: 2013-12-29 12:01:44 +0800 (Sun, 29 Dec 2013) $$
* 作者: $$Author: kingofeyesblue@gmail.com $$
* 版本: $$Revision: 111 $$
* ID  ：$$Id: LamrimReaderActivity.java 111 2013-12-29 04:01:44Z kingofeyesblue@gmail.com $$
*/
public class LamrimReaderActivity extends SherlockFragmentActivity{
	/** Called when the activity is first created. */
	private static final long serialVersionUID = 4L;
	final static String logTag = "LamrimReader";
	final static String funcInto = "Function Into";
	final static String funcLeave = "Function Leave";

	final static int THEORY_MENU = 0;
	final static int SPEECH_MENU = 1;

	final static int SPEECH_MENU_RESULT = 0;
	final static int THEORY_MENU_RESULT = 1;
	final static int OPT_MENU_RESULT = 2;
	final static int SELECT_FG_PIC_RESULT=3;
	final static int SUBTITLE_MODE=1;
	final static int READING_MODE=2;
	
	
	final int FG=1, FG_SCALE_TYPE=2, BG_COLOR=3, BG_PIC=4, SUB_FG=5, SUB_BG=6, SUB_ALPHA=7;
	
	int renderMode=SUBTITLE_MODE;
	static int mediaIndex = -1;
	MediaPlayerController mpController;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	ImageView bookView = null;
	TextView subtitleView = null;
	SharedPreferences runtime = null;
//	ArrayList<HashMap<String, String>> bookList = null;
//	TheoryListAdapter adapter = null;
	
	MenuItem speechMenu, setTextSize, saveRegion, playRegionRec, prjWeb, exitApp;
	MenuItem swModeBtn, setImg, imgScaleType, setImgBg, setSubFgColor, setSubBgColor, setSubAlpha;

	FileSysManager fileSysManager = null;
//	FileDownloader fileDownloader = null;
	RelativeLayout rootLayout = null;
	
	Typeface educFont = null;
	View toastLayout = null;
	TextView toastTextView = null;
	Toast toast = null;
	ImageView toastSubtitleIcon;
	ImageView toastInfoIcon;
	MenuItem rootMenuItem = null, optMenuItem = null;
	int regionPlayIndex = -1;
//	ArrayList<RegionRecord> regionRecord = null;
	
	// the 3 object is paste on the popupwindow object, it not initial at startup.
	SimpleAdapter regionRecordAdapter = null;
	ArrayList<HashMap<String,String>> regionFakeList = null;
	ListView regionListView = null;
	
	HashMap<String,String> fakeSample = new HashMap();
	PackageInfo pkgInfo = null;
	
	View actionBarControlPanel = null;
	ImageView bookIcon=null;
//	EditText jumpPage = null;
	SeekBar volumeController = null;
	
	int[][] readingModeSEindex=null;
	String readingModeAllSubtitle=null;
	static Point screenDim=new Point();
	Button modeSwBtn=null;
	boolean isShowModeSwBtn=true;
	
	private TaskFragment mTaskFragment;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams. FLAG_FULLSCREEN ,
				WindowManager.LayoutParams. FLAG_FULLSCREEN);
		setContentView(R.layout.main);
		
//		getSupportActionBar();
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);

		Log.d(funcInto, "******* Into LamrimReader.onCreate *******");
		if (savedInstanceState != null){
			Log.d(logTag, "The savedInstanceState is not null!");
		}
		Log.d(getClass().getName(), "mediaIndex=" + mediaIndex);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,logTag);
		educFont=Typeface.createFromAsset(this.getAssets(), "EUDC.TTF");
		try {
			pkgInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
		} catch (NameNotFoundException e3) {
			e3.printStackTrace();
		}
		
		try {
			getWindowManager().getDefaultDisplay().getSize(screenDim);
	    } catch (java.lang.NoSuchMethodError ignore) { // Older device
	    	screenDim.x = getWindowManager().getDefaultDisplay().getWidth();
	    	screenDim.y = getWindowManager().getDefaultDisplay().getHeight();
	    }
		// The value will get portrait but not landscape value sometimes, exchange it if happen.
		if(screenDim.x < screenDim.y)screenDim.set(screenDim.y, screenDim.x);
		
		LayoutInflater factory = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
		actionBarControlPanel = factory.inflate(R.layout.action_bar_control_panel, null);
		
		modeSwBtn=(Button)findViewById(R.id.modeSwBtn);
		modeSwBtn.setOnTouchListener(new View.OnTouchListener(){
			boolean pressed=false;
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				
//				Log.d(logTag, "Action Code= "+event.getAction());
				if(event.getAction()==MotionEvent.ACTION_UP || event.getAction()==MotionEvent.ACTION_CANCEL){
					Log.d(logTag, "Leave event received");
					if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
						modeSwBtn.setBackground(getResources().getDrawable(R.drawable.mode_sw_button));
					else
						modeSwBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mode_sw_button));
					pressed=false;
					return true;
				}
				
//				Log.d(logTag, "Into onScroll, y="+event.getRawY());
				if(!pressed){
					Log.d(logTag, "Set pressed color");
					if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
						modeSwBtn.setBackground(getResources().getDrawable(R.drawable.mode_sw_press_button));
					else
						modeSwBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mode_sw_press_button));
					
					pressed=true;
				}
				
				int height=(int) (screenDim.y - event.getRawY());
				float upBoundDp=(float)getResources().getInteger(R.integer.subtitleScrollTouchBtnHeightPercentDp)/100*screenDim.y;
				int minHeight=(int) subtitleView.getLineHeight();
				int maxHeight=(int) (rootLayout.getHeight()-upBoundDp);
				//int maxHeight=(int) (rootLayout.getHeight()-getResources().getDisplayMetrics().density*getResources().getInteger(R.integer.subtitleScrollTouchUpperBoundDp));
				
		//		synchronized (mpController){
				// set Subtitle mode
				if(height<=minHeight){
					height=minHeight;
					renderMode=SUBTITLE_MODE;
					subtitleView.setGravity(Gravity.CENTER);
					subtitleView.setMovementMethod(null);
					if(mpController.getMediaPlayerState()==MediaPlayerController.MP_PLAYING && mpController.getSubtitle()!=null){
						if(mpController.getCurrentPosition()==-1)return true;
						setSubtitleViewText(mpController.getSubtitle(mpController.getCurrentPosition()).text);
					}
					else
						setSubtitleViewText(getString(R.string.dlgHintMpController));
				}
				// set reading mode
				else{
					// It is first time into reading mode, set the all text to subtitleView, but not set text every time.
					if(renderMode==SUBTITLE_MODE){
						if(mpController==null || !mpController.isSubtitleReady() || readingModeAllSubtitle==null){
							showNarmalToastMsg("尚無字幕，無法切換到閱讀模式！");
							return true;
						}
						subtitleView.setGravity(Gravity.LEFT);
							setSubtitleViewText(readingModeAllSubtitle);
//							subtitleView.setScroller(new Scroller(LamrimReaderActivity.this)); 
							subtitleView.setScrollBarStyle(TextView.SCROLLBARS_INSIDE_OVERLAY);
							//subtitleView.setMovementMethod(new ScrollingMovementMethod());
							subtitleView.setMovementMethod(ScrollingMovementMethod.getInstance());
							renderMode=READING_MODE;
					}
				}
		//		}
				
//				Log.d(logTag, "Set height to: "+height);
				if(height>maxHeight)height=maxHeight;
				subtitleView.setHeight(height);

				return true;
			}});
		boolean isShowModeSwBtn=runtime.getBoolean(getString(R.string.isShowModeSwBtnKey),true);
		if(isShowModeSwBtn)modeSwBtn.setVisibility(View.VISIBLE);
		else modeSwBtn.setVisibility(View.GONE);
		
		fakeSample.put(null,null);
		RegionRecord.init(this);
		regionFakeList = new ArrayList<HashMap<String,String>>();
		for( int i=0;i<RegionRecord.records.size() ; ++i) 
        		regionFakeList.add(fakeSample);
		
		regionRecordAdapter=new RegionRecordAdapter(
				this,
				regionFakeList,
                android.R.layout.simple_list_item_2,
                new String[] { "title", "desc" },
                new int[] { android.R.id.text1, android.R.id.text2}
		);

		

		toast = new Toast(getApplicationContext());


		subtitleView = (TextView) findViewById(R.id.subtitleView);
		Log.d(logTag,"subtitleView="+subtitleView+", EDUC="+educFont);
		subtitleView.setTypeface(educFont);
		int color=runtime.getInt(getString(R.string.subtitleFgColorKey),subtitleView.getTextColors().getDefaultColor());
		subtitleView.setTextColor(color);
		color=runtime.getInt(getString(R.string.subtitleBgColorKey),getResources().getColor(R.color.subtitleBGcolor));
		subtitleView.setBackgroundColor(color);
		int alpha=runtime.getInt(getString(R.string.subtitleAlphaKey),100);
		subtitleView.getBackground().setAlpha(alpha);
		
		final GestureDetector subtitleViewGestureListener=new GestureDetector(getApplicationContext(),new SimpleOnGestureListener(){
			@Override
			public boolean onDown(MotionEvent e) {return true;}
			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				Log.d(logTag, "SubtitleView been clicked, Show media plyaer control panel.");
				if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED)
					mpController.showMediaPlayerController();
				GaLogger.sendEvent("ui_action", "subtitle_event", "single_tap", null);
				return true;
			}
			@Override
			public boolean onDoubleTapEvent(MotionEvent e) {
				// If it stay in subtitle mode, do nothing.
				if(renderMode==SUBTITLE_MODE)return false;
				if(mpController.getMediaPlayerState()==MediaPlayerController.MP_PLAYING && mpController.getSubtitle()!=null){
					int index=mpController.getSubtitleIndex(mpController.getCurrentPosition());
					if(index==-1)return true;
					//subtitleView.bringPointIntoView(readingModeSEindex[index][0]);
					try{
						int line = subtitleView.getLayout ().getLineForOffset (readingModeSEindex[index][0]);
						subtitleView.scrollTo(subtitleView.getScrollX(),subtitleView.getLineBounds(line, null)-subtitleView.getLineHeight());
					}catch(Exception et){
						et.printStackTrace();
						GaLogger.sendException("readingModeSEindex under contruct and read.", et, true);
					}
				}
				return true;
			}
			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if(renderMode==READING_MODE){
				int y=(int) (subtitleView.getScrollY()+distanceY);

				// Unknown problem, there will return null on some machine.
				Layout layout=subtitleView.getLayout();
				Log.d(logTag,"Layout is "+((layout==null)?"null":"not null"));
				if(layout==null)return true;
				// ======================================================
				int bottom=subtitleView.getLineBounds(
						subtitleView.getLayout().getLineForOffset(
								subtitleView.getText().length()
								),null)-
								subtitleView.getMeasuredHeight()
								+subtitleView.getLineHeight();
				Log.d(logTag,"Org Y="+y+"layout.height="+subtitleView.getLayoutParams().height+", subtitle.height="+subtitleView.getHeight()+", measureHeight="+subtitleView.getMeasuredHeight());
				if(y<0)y=0;
				if(y>bottom)y=bottom;
//				if(subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight()-y<0)y=subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight();
				subtitleView.scrollTo(subtitleView.getScrollX(), y);
				Log.d(logTag,"Scroll subtitle view to "+subtitleView.getScrollX()+", "+y);
				}
				return true;
			}
		});
		
		final ScaleGestureDetector stScaleGestureDetector =new ScaleGestureDetector(this.getApplicationContext(),new SimpleOnScaleGestureListener() {
			@Override
			public boolean onScaleBegin(ScaleGestureDetector detector) {
				Log.d(getClass().getName(),"Begin scale called factor: "+detector.getScaleFactor());
				GaLogger.sendEvent("ui_action", "subtitle_event", "scale_start", null);
				return true;
			}
			@Override
			public boolean onScale(ScaleGestureDetector detector) {
				float size=subtitleView.getTextSize()*detector.getScaleFactor();
//				Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
   				subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
//				Log.d(getClass().getName(),"Realy size after setting: "+adapter.getTextSize());
   				if(renderMode==SUBTITLE_MODE)
   					subtitleView.setHeight(subtitleView.getLineHeight());
   				
   				return true;
   			}
			@Override
			public void onScaleEnd(ScaleGestureDetector detector){
				SharedPreferences.Editor editor = runtime.edit();
				editor.putInt(getString(R.string.subtitleFontSizeKey), (int) subtitleView.getTextSize());
				editor.commit();
				GaLogger.sendEvent("ui_action", "subtitle_event", "scale_end", null);
			}
		});
		
		subtitleView.setOnTouchListener(new View.OnTouchListener(){
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				try{
					if(event.getPointerCount()==2){
						return stScaleGestureDetector.onTouchEvent(event);
					}
					boolean res= subtitleViewGestureListener.onTouchEvent(event);
					return res;
//				Log.d(logTag, "Subtitle OnTouchListener return "+res);
				}catch(Exception e){
                    e.printStackTrace();
                    GaLogger.sendEvent("exception", "SubtitleView", "ScaleGestureDetector", null);
                    return true;
				}
			}
		});
		

        bookView = (ImageView) findViewById(R.id.mainImage);
        String imgPath=runtime.getString(getString(R.string.mainImgFgPathKey), null);
        if(imgPath!=null)bookView.setImageURI(Uri.fromFile(new File(imgPath)));
        bookView.setScaleType(scaleType[runtime.getInt(getString(R.string.mainImgScaleKey), 0)]);
        color=runtime.getInt(getString(R.string.mainImgBgColorKey),0);
        bookView.setBackgroundColor(color);
        bookView.setOnLongClickListener(new View.OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				openOptionsMenu();
			    return true;
			}});
		// Implement 3 finger command, while 3 finger scroll/drag over 1/10 of width/height of screen then fire.
		bookView.setOnTouchListener(new View.OnTouchListener(){
			boolean cmdStart=false, hasFired=false;
			float xStart=-1, yStart=-1;
			
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				Log.d(getClass().getName(),"Into bookView.OnTouchListener");
				int fingerCount=event.getPointerCount();
				Log.d(getClass().getName(),"There are "+fingerCount+" fingers");
				
				if(fingerCount!=3){
					cmdStart=false;
					hasFired=false;
					return false;
				}
			
				if(!cmdStart){
					cmdStart=true;
					hasFired=false;
					xStart=event.getX();
					yStart=event.getY();
				}
				
				int xThreshold = screenDim.x/10, yThreshold = screenDim.y / 10;
				float xShift = Math.abs(xStart-event.getX()), yShift = Math.abs(yStart-event.getY());
				if(xShift<xThreshold && yShift <yThreshold)return true;
				if(hasFired)return true;
				
				// Left or Right scroll
				if(xShift>yShift){
					if(event.getX()<xStart)fireLeft();
					else fireRight();
				}
				// Upper or down scroll
				else{
					if(event.getY()<yStart)fireUp();
					else fireDown();
				}
				hasFired=true;
				return true;
			}
			
			private void fireLeft(){Log.d(getClass().getName(),"-- 3 Finger command fire: Left.");};
			private void fireRight(){Log.d(getClass().getName(),"-- 3 Finger command fire: Right.");};
			private void fireUp(){Log.d(getClass().getName(),"-- 3 Finger command fire: Up.");};
			private void fireDown(){Log.d(getClass().getName(),"-- 3 Finger command fire: Down.");};
		});
		
		rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		rootLayout.setLongClickable(false);

		fileSysManager = new FileSysManager(this);
		FileSysManager.checkFileStructure();
		
		//String appSubtitle=getString(R.string.app_name) +" V"+pkgInfo.versionName+"."+pkgInfo.versionCode;
		String appSubtitle=getString(R.string.app_name) +" V"+pkgInfo.versionName;

		if(mpController!=null)Log.d(logTag,"The media player controller is not null in onCreate!!!!!");
		if(mpController==null)
		//mpController = new MediaPlayerController(LamrimReaderActivity.this, LamrimReaderActivity.this.findViewById(android.R.id.content), new MediaPlayerControllerListener() {
			mpController = new MediaPlayerController(LamrimReaderActivity.this, LamrimReaderActivity.this.findViewById(R.id.rootLayout), new MediaPlayerControllerListener() {
			@Override
			public void onSubtitleChanged(final int index, final SubtitleElement subtitle) {
//				Log.d(getClass().getName(), "Set subtitle: "+ subtitle.text);
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
		//				synchronized (mpController){
						switch(renderMode){
						case SUBTITLE_MODE:
							subtitleView.setText(subtitle.text);
							int lineCount=subtitleView.getLineCount();// There will return 0 sometimes.
							if(lineCount<1)lineCount=1;
							subtitleView.setHeight(subtitleView.getLineHeight()*lineCount);
							break;
						case READING_MODE:
							//SpannableString str=new SpannableString (subtitleView.getText());
							SpannableString str=new SpannableString (readingModeAllSubtitle);
							//Spannable WordtoSpan = (Spannable) subtitleView.getText();
							try{
								str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)), readingModeSEindex[index][0], readingModeSEindex[index][1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
								subtitleView.setText(str);
							}catch(Exception e){
								e.printStackTrace();
								GaLogger.sendException("mediaIndex="+mediaIndex+", subtitleIndex="+index+", totalLen="+str.length(), e, true);
							}
							break;
						};
		//			}
				}
				});
			}
			@Override
			public void onPlayerError(){
				setSubtitleViewText(getString(R.string.app_name));
				GaLogger.sendEvent("error", "player_error", "error_happen", null);
			}
			@Override
			public void onSeek(final int index, final SubtitleElement subtitle){
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
				//		synchronized (mpController){
							switch(renderMode){
							case SUBTITLE_MODE:
								showSubtitleToast(subtitle.text+" - ("+getMsToHMS(subtitle.startTimeMs,"\"","'",false)+" - "+getMsToHMS(subtitle.endTimeMs,"\"","'",false)+')');
								break;
							case READING_MODE:
								SpannableString str=new SpannableString (readingModeAllSubtitle);
								try{
									str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)), readingModeSEindex[index][0], readingModeSEindex[index][1], Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
									subtitleView.setText(str);
								}catch(Exception e){
									e.printStackTrace();
									GaLogger.sendException("mediaIndex="+mediaIndex+", subtitleIndex="+index+", totalLen="+str.length(), e, true);
								}
								break;
						};
					
				//}
						}});
			}
//			@Override
//			public void startMoment(){setSubtitleViewText("");}
			@Override
			public void onMediaPrepared() {
				Log.d(getClass().getName(),"MediaPlayer prepared, show controller.");
				GaLogger.sendEvent("play_action", "player_event", SpeechData.name[mediaIndex]+"_prepared", null);
				
				if(mpController.isSubtitleReady()){
					setSubtitleViewText(getString(R.string.dlgHintMpController));
					SubtitleElement[] se=mpController.getSubtitle();
					readingModeSEindex=new int[se.length][2];
					readingModeAllSubtitle=new String();
					int wordCounter=0;
					for(int i=0;i<se.length;i++){
						readingModeSEindex[i][0]=wordCounter;
						wordCounter+=se[i].text.length();
						readingModeSEindex[i][1]=wordCounter;
						String str=se[i].text.replaceAll(",", "，");
						str=str.replaceAll("\\.", "。");
						str=str.replaceAll("!", "！");
						str=str.replaceAll(";", "；");
						str=str.replaceAll("\\?", "？");
						str=str.replaceAll(":", "：");
						readingModeAllSubtitle+=str;
					}
				}
				else setSubtitleViewText(getString(R.string.dlgHintMpControllerNoSubtitle));
			
				// If this time fire by user select a new speech, no need to seekTo(sometime), just play from 0, and set the newPlay flag to false.
				int seekPosition=runtime.getInt("playPosition",0);
				Log.d(logTag,"Seek to last play positon "+seekPosition);
				mpController.seekTo(seekPosition);

//				getSupportActionBar().setTitle(SpeechData.getNameId(mediaIndex));
				Log.d(logTag,"Check media static before show controller: media player state: "+mpController.getMediaPlayerState()+", normal should equal or bigger then "+MediaPlayerController.MP_PREPARED);
				if(regionPlayIndex!=-1){
					Log.d(logTag,"This play event is region play, set play region.");					
					mpController.setPlayRegion(RegionRecord.records.get(regionPlayIndex).startTimeMs, RegionRecord.records.get(regionPlayIndex).endTimeMs);
					mpController.start();
					regionPlayIndex=-1;
				}
				else{
					Log.d(logTag,"The play event is fire by user select a new speech.");
					mpController.showMediaPlayerController();
				}
			}
			
			@Override
			public void onCompleteReload(){
				setSubtitleViewText(getString(R.string.playCompleteReloading));
			}
			
			@Override
			public void startRegionSeted(int position){
//				showNarmalToastMsg("區段開始位置設定完成: "+getMsToHMS(position,"\"","'",false));
			}
			@Override
			public void startRegionDeset(int position){
//				showNarmalToastMsg("區段開始位置已清除: ");
			}
//			@Override
//			public void endRegionSeted(int position){showNarmalToastMsg("區段結束位置設定完成: "+getMsToHMS(position,"\"","'",false));}
//			@Override
//			public void endRegionDeset(int position){showNarmalToastMsg("區段結束位置已清除: ");}
			@Override
			public void startRegionPlay(){
				showNarmalToastMsg("開始區段播放");
			}
			@Override
			public void stopRegionPlay(){
				showNarmalToastMsg("停止區段播放");
			}
			@Override
			public void startMoment() {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void endRegionSeted(int position) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void endRegionDeset(int position) {
				// TODO Auto-generated method stub
				
			}
			@Override
			public void getAudioFocusFail() {
				// TODO Auto-generated method stub
				//setSubtitleViewText(getResources().getString(R.string.soundInUseError));
				setSubtitleViewText(getResources().getString(R.string.soundInUseError));
			}
		});
		
/*		FragmentManager fm = getSupportFragmentManager();
	    mTaskFragment = (TaskFragment) fm.findFragmentByTag("PlayerTask");

	    // If the Fragment is non-null, then it is currently being
	    // retained across a configuration change.
	    if (mTaskFragment == null) {
	      mTaskFragment = new TaskFragment();
	      fm.beginTransaction().add(mTaskFragment, "PlayerTask").commit();
	    }
*/		Log.d(funcLeave, "******* onCreate *******");

		// LogRepoter.log("Leave OnCreate");
	}
	
	
	@Override
	protected void onStart() {
		super.onStart();
		Log.d(funcInto, "**** onStart() ****");
		
		Log.d(logTag,"Into onResume");
		float modeSwBtnHeight=(float)getResources().getInteger(R.integer.subtitleScrollTouchBtnHeightPercentDp)/100*screenDim.y;
		float modeSwBtnWidth=(float)getResources().getInteger(R.integer.subtitleScrollTouchBtnWidthPercentDp)/100*screenDim.x;
		modeSwBtn.getLayoutParams().width=(int) modeSwBtnWidth;
		modeSwBtn.getLayoutParams().height=(int) modeSwBtnHeight;
		
		GaLogger.activityStart(this);
		GaLogger.sendEvent("activity", "LamrimReaderActivity", "into_onStart", null);
		
		// Dump default settings to DB
		int isInit=runtime.getInt("mediaIndex", -1);
		if(isInit==-1){
			Log.d(logTag,"This is first time launch LamrimReader, initial default settings.");
			subtitleView.setTextSize(getResources().getInteger(R.integer.defFontSize));
//			int currentIndex=mpController.getCurrentPosition();
			SharedPreferences.Editor editor = runtime.edit();
			editor.putInt("mediaIndex", isInit);
//			editor.putInt("playerStatus", mpController.getMediaPlayerState());
			editor.putInt("playPosition", 0);
			editor.commit();
			
			Log.d(funcLeave,"**** saveRuntime ****");
		}
		
//		int bookPage = runtime.getInt("bookPage", 0);
//		jumpPage.setText(bookPage);
		Log.d(funcLeave, "**** onStart() ****");
	}

	public static Point getScreenDim(){
		return screenDim;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		/*
		 * While in the sleep mode, the life cycle into onPause, when user active the application the life cycle become 
		 * onResume -> onPause -> onDestroy -> onCreate -> onStart -> onResume, the media player still exist after the application recreate.
		 * the prepare method call twice both in the two onResume, the second prepare will throw illegalStageExcteption, and will cause error
		 * sometime, If the stage into PREPARING, it mean it preparing the media source at first onResume, then do nothing. 
		 * */
/*		try {
			if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARING){
				Log.d(logTag,"onResume: The state of MediaPlayer is PAUSE, start play.");
//				mpController.setAnchorView(LamrimReaderActivity.this.findViewById(android.R.id.content));
//				mpController.showMediaPlayerController();
				return;
			}
		} catch (IllegalStateException e) {	e.printStackTrace();}
*/		
		mediaIndex=runtime.getInt("mediaIndex", -1);
		if(mediaIndex==-1)return;
		
		if(!mpController.isPlayerReady())startPlay(mediaIndex);
		Log.d(logTag,"Leave onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveRuntime();
		try {
			if(mpController.getMediaPlayerState()==MediaPlayerController.MP_PLAYING)
				mpController.pause();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		if (wakeLock.isHeld())wakeLock.release();
	}

	@Override
	protected void onStop() {
		super.onStop();
		toast.cancel();
		GaLogger.activityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(funcInto, "**** onDestroy ****");
//		fileDownloader.finish();
		mpController.finish();
		toast.cancel();
		Log.d(funcLeave, "**** onDestroy ****");
	}
	
	protected void saveRuntime(){
		Log.d(funcInto,"**** saveRuntime ****");
		SharedPreferences.Editor editor = runtime.edit();
		Log.d(logTag,"Save mediaIndex="+mediaIndex);
		
		

		Log.d(logTag,"MediaPlayer status="+mpController.getMediaPlayerState());
		editor.putInt("mediaIndex", mediaIndex);
//		editor.putInt("playerStatus", mpController.getMediaPlayerState());
		if(mpController.getMediaPlayerState()>MediaPlayerController.MP_PREPARING){
			int playPosition=mpController.getCurrentPosition();
			editor.putInt("playPosition", playPosition);
		}
        Log.d(logTag,"Save content: mediaIndex="+mediaIndex+", playPosition(write)="+", playPosition(read)="+runtime.getInt("playPosition", -1));
		editor.commit(); Log.d(funcLeave,"**** saveRuntime ****");
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(LamrimReaderActivity.this);
	    builder.setTitle(getString(R.string.dlgExitTitle));
	    builder.setMessage(getString(R.string.dlgExitMsg));
	    builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	dialog.cancel();
	        	saveRuntime();
	    		finish();
	        }
	    });
	    builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	            dialog.cancel();
	        }
	    });
		builder.create().show();
		Log.d(funcInto, "**** onBackPressed ****");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        //getSupportMenuInflater().inflate(R.menu.main, menu);
        //return super.onCreateOptionsMenu(menu);
		SubMenu rootMenu = menu.addSubMenu("操作選單");
		speechMenu=rootMenu.add(getString(R.string.menuStrSelectSpeech));
		speechMenu.setIcon(R.drawable.speech);
		setTextSize=rootMenu.add(getString(R.string.menuStrTextSize));
		setTextSize.setIcon(R.drawable.font_size);
		saveRegion=rootMenu.add(getString(R.string.menuStrSavePlayRegion));
		saveRegion.setIcon(R.drawable.save);
		playRegionRec=rootMenu.add(getString(R.string.menuStrPlayRegionRec));
		playRegionRec.setIcon(R.drawable.region);
		
		prjWeb=rootMenu.add(getString(R.string.menuStrOpenProjectWeb));
		prjWeb.setIcon(R.drawable.project_web);
		
		exitApp=rootMenu.add(getString(R.string.exitApp));
		exitApp.setIcon(R.drawable.exit_app);
		
        rootMenuItem = rootMenu.getItem();
        //rootMenuItem.setIcon(R.drawable.menu_down_48x48);
        rootMenuItem.setIcon(R.drawable.menu_down);
        rootMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        
        
        SubMenu optMenu = menu.addSubMenu("設定選單");
        swModeBtn=optMenu.add("顯示/隱藏模式切換鍵");
        setImg=optMenu.add("選擇圖片");
        imgScaleType=optMenu.add("圖片擴展方式");
        setImgBg=optMenu.add("圖片背景");
        setSubFgColor=optMenu.add("字幕顏色");
        setSubBgColor=optMenu.add("字幕背景");
        setSubAlpha=optMenu.add("字幕透明度");
        
        //LayoutInflater factory = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
	    //final View v = factory.inflate(R.layout.action_bar_control_panel, null);

//        getSupportActionBar().setCustomView(actionBarControlPanel);
//        getSupportActionBar().setDisplayShowCustomEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(funcInto,	"****OptionsItemselected, select item=" + item.getItemId()	+ ", String="+item.getTitle()+", Order="+item.getOrder()+" ****");
		String gid=(String) item.getTitle();
		GaLogger.sendEvent("ui_action", "menu_event", ((gid.length()==0)?"root_menu":gid)+"_pressed", null);
		
		if(item.equals(rootMenuItem)){
		Log.d(logTag,"Create menu: can save region? "+mpController.canPlayRegion());
			if(mpController.canPlayRegion()){
				saveRegion.setEnabled(true);
				saveRegion.setIcon(R.drawable.save);
			}
			else{
				saveRegion.setEnabled(false);
				saveRegion.setIcon(R.drawable.save_d);
			}
			if(RegionRecord.records.size()>0){
				playRegionRec.setEnabled(true);
				playRegionRec.setIcon(R.drawable.region);
				}
			else{
				playRegionRec.setEnabled(false);
				playRegionRec.setIcon(R.drawable.region_d);
			}
		}
		
		if(item.getTitle().equals(getString(R.string.menuStrSelectSpeech))){
			final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
			if (wakeLock.isHeld())wakeLock.release();
			startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
		}
		else if(item.getTitle().equals(getString(R.string.menuStrTextSize))){
			showSetTextSizeDialog();
		}else if(item.getTitle().equals(getString(R.string.menuStrSavePlayRegion))){
			showSaveRegionDialog();
		}else if(item.getTitle().equals(getString(R.string.menuStrPlayRegionRec))){
			showRecordListPopupMenu();
		}else if(item.getTitle().equals(getString(R.string.menuStrOpenProjectWeb))){
			Uri uri = Uri.parse(getString(R.string.projectWebUrl));
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(it);
		}else if(item.getTitle().equals(getString(R.string.exitApp))){
			onBackPressed();
			Log.d(funcLeave, "**** onBackPressed ****");
		}

		
		// ================ For option menu =====================
		else if(item.getTitle().equals("顯示/隱藏模式切換鍵")){
			if(isShowModeSwBtn){
				modeSwBtn.setVisibility(View.GONE);
				isShowModeSwBtn=false;
				
				SharedPreferences.Editor editor = runtime.edit();
	        	editor.putBoolean(getString(R.string.isShowModeSwBtnKey), isShowModeSwBtn);
				editor.commit();
				
			}
			else {
				modeSwBtn.setVisibility(View.VISIBLE);
				isShowModeSwBtn=true;
				
				SharedPreferences.Editor editor = runtime.edit();
	        	editor.putBoolean(getString(R.string.isShowModeSwBtnKey), isShowModeSwBtn);
				editor.commit();
			}
		}
		else if(item.getTitle().equals("選擇圖片")){
			Intent fgIntent = new Intent(getBaseContext(), FileDialogActivity.class);
			fgIntent.putExtra(FileDialogActivity.START_PATH, "/sdcard");
            //can user select directories or not
			fgIntent.putExtra(FileDialogActivity.CAN_SELECT_DIR, false);
            //alternatively you can set file filter
			fgIntent.putExtra(FileDialogActivity.FORMAT_FILTER, new String[] { "jpg", "gif", "png", "bmp", "webp" });
            startActivityForResult(fgIntent, SELECT_FG_PIC_RESULT);
		}
		else if(item.getTitle().equals("圖片擴展方式")){showScaleTypeDialog();}
		else if(item.getTitle().equals("圖片背景")){
			int defColor=runtime.getInt(getString(R.string.mainImgBgColorKey), R.color.subtitleBGcolor);
			AmbilWarnaDialog dialog = new AmbilWarnaDialog(LamrimReaderActivity.this, defColor, new OnAmbilWarnaListener() {
		        @Override
		        public void onOk(AmbilWarnaDialog dialog, int color) {
		        	bookView.setBackgroundColor(color);
		        	SharedPreferences.Editor editor = runtime.edit();
		        	editor.putInt(getString(R.string.mainImgBgColorKey), color);
					editor.commit();
		        }

		        @Override
		        public void onCancel(AmbilWarnaDialog dialog) {}
			});
			
			dialog.show();
		}
		else if(item.getTitle().equals("字幕顏色")){
			int defFgColor=runtime.getInt(getString(R.string.subtitleFgColorKey), R.color.subtitleFGcolor);
			AmbilWarnaDialog fgDialog = new AmbilWarnaDialog(LamrimReaderActivity.this, defFgColor, new OnAmbilWarnaListener() {
		        @Override
		        public void onOk(AmbilWarnaDialog dialog, int color) {
		        	subtitleView.setTextColor(color);
		        	SharedPreferences.Editor editor = runtime.edit();
		        	editor.putInt(getString(R.string.subtitleFgColorKey), color);
					editor.commit();
		        }
		                
		        @Override
		        public void onCancel(AmbilWarnaDialog dialog) {}
			});
			
			fgDialog.show();
		}
		else if(item.getTitle().equals("字幕背景")){
			int defBgColor=runtime.getInt(getString(R.string.subtitleBgColorKey), R.color.subtitleBGcolor);
			AmbilWarnaDialog bgDialog = new AmbilWarnaDialog(LamrimReaderActivity.this, defBgColor, new OnAmbilWarnaListener() {
		        @Override
		        public void onOk(AmbilWarnaDialog dialog, int color) {
		        	subtitleView.setBackgroundColor(color);
		        	SharedPreferences.Editor editor = runtime.edit();
		        	editor.putInt(getString(R.string.subtitleBgColorKey), color);
					editor.commit();
		        }
		                
		        @Override
		        public void onCancel(AmbilWarnaDialog dialog) {}
			});
			
			bgDialog.show();
		}
		else if(item.getTitle().equals("字幕透明度")){
			final EditText input=new EditText(this);
			int alpha=runtime.getInt(getString(R.string.subtitleAlphaKey),100);
			input.setText(alpha);
			input.setSingleLine();
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			new AlertDialog.Builder(this).setTitle("請輸入透明度(0~255)").setIcon(
			android.R.drawable.ic_dialog_info).setView(input
			).setPositiveButton("确定", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					try{
						int alpha=Integer.parseInt(input.getText().toString());
						if(alpha<0 || alpha>255)return;
						subtitleView.getBackground().setAlpha(alpha);

			        	SharedPreferences.Editor editor = runtime.edit();
			        	editor.putInt(getString(R.string.subtitleAlphaKey), alpha);
						editor.commit();
					}catch(Exception e){}
				}})
			.setNegativeButton("取消", null).show();
		}
		
		Log.d(funcLeave, "**** Into Options selected, select item=" + item.getItemId()	+ " ****");
		return true;
	}

	protected void onActivityResult(int requestCode, int resultCode,Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		Log.d(funcInto, "**** Into onActivityResult: Get result from: "	+ requestCode + " ****");
		SharedPreferences.Editor editor = runtime.edit();
		switch (requestCode) {
		case SPEECH_MENU_RESULT:
			if (resultCode == RESULT_CANCELED){
				Log.d(logTag, "User skip, do nothing.");
				GaLogger.sendEvent("activity", "SpeechMenu_result_LamrimReader", "user_cancel", null);
				return;
			}
			final int selected = intent.getIntExtra("index", -1);
/*			// The code under test, after user selected a media then pause media player and into the idle mode, it do nothing if user select the same one.
			if( selected == mediaIndex && mpController.getMediaPlayerState()!=MediaPlayerController.MP_IDLE){
				Log.d(logTag, "The selected same as playing, do nothing.");
				showToastMsg("目前所播放的檔案已是"+SpeechData.getNameId(selected));
				return;// selected same as playing
			}
*/
			
			Log.d(logTag, "OnResult: the user select index="+selected);
			// Flag for runtime
			
			editor.putInt("mediaIndex", selected);
			editor.putInt("playPosition", 0);
			editor.commit();
			regionPlayIndex=-1;
			renderMode=SUBTITLE_MODE;
			subtitleView.setGravity(Gravity.CENTER);
			subtitleView.setHeight(subtitleView.getLineHeight());
//			isRegionPlay=false;
//			mpController.desetPlayRegion();
			Log.d(logTag, "Call reset player in onActivityResult.");
			mpController.reset();
			GaLogger.sendEvent("activity", "SpeechMenu_result", "select_index_"+selected, null);
			// After onActivityResult, the life-cycle will return to onStart, do start downloader in OnResume.
			break;
		case SELECT_FG_PIC_RESULT:
			if (resultCode == Activity.RESULT_OK) {

/*                if (requestCode == 0) {
                        System.out.println("Saving...");
                } else if (requestCode == 1) {
                        System.out.println("Loading...");
                }
*/                
                String filePath = intent.getStringExtra(FileDialogActivity.RESULT_PATH);
                bookView.setImageURI(Uri.fromFile(new File(filePath)));
                bookView.setScaleType(scaleType[runtime.getInt(getString(R.string.mainImgScaleKey),0)]);

				Log.d(logTag,"Set forground picture src: "+filePath);
				editor.putString(getString(R.string.mainImgFgPathKey), filePath);
				editor.commit();
				
			}
			break;
		}
		
		
		Log.d(funcLeave, "Leave onActivityResult");
	}

	public boolean startPlay(final int mediaIndex){
		File f=FileSysManager.getLocalMediaFile(mediaIndex);
		if(f==null || !f.exists()){
			return false;
		}
		
		AsyncTask<Void, Void, Void> runner=new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
		//		synchronized (mpController) {
					try {
						setSubtitleViewText(getString(R.string.dlgDescPrepareSpeech));
						mpController.setDataSource(getApplicationContext(), mediaIndex);
						mpController.prepareMedia();
					} catch (IllegalArgumentException e) {
						setSubtitleViewText(getString(R.string.errIAEwhileSetPlayerSrc));
						GaLogger.sendEvent("error", "player_error", "IllegalArgumentException", null);
						e.printStackTrace();
					} catch (SecurityException e) {
						setSubtitleViewText(getString(R.string.errSEwhileSetPlayerSrc));
						GaLogger.sendEvent("error", "player_error", "SecurityException", null);
						e.printStackTrace();
					} catch (IllegalStateException e) {
						setSubtitleViewText(getString(R.string.errISEwhileSetPlayerSrc));
						GaLogger.sendEvent("error", "player_error", "IllegalStateException", null);
						e.printStackTrace();
					} catch (IOException e) {
						setSubtitleViewText(getString(R.string.errIOEwhileSetPlayerSrc));
						GaLogger.sendEvent("error", "player_error", "IOException", null);
						e.printStackTrace();
					}
		//		}
				return null;
			}
		};
		runner.execute();
		
		
		return true;
	}


	/*
	 * Set the message on the subtitle view, there should check the subtitleView
	 * is not playing, or hide the message.
	 */
	public void setSubtitleViewText(final CharSequence  s) {
		runOnUiThread(new Runnable() {
			public void run() {
				subtitleView.setTextKeepState(s);
			}
		});
	}

	public void showSubtitleToast(final String s){
		runOnUiThread(new Runnable() {
			public void run() {
				toast.cancel();
				toast = new Toast(getApplicationContext());
				//LayoutInflater factory = (LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE);
			    //final View v = factory.inflate(R.layout.toastLayout, null);
				
				LayoutInflater inflater = getLayoutInflater();
				toastLayout = inflater.inflate(R.layout.toast_text_view, (ViewGroup) findViewById(R.id.toastLayout));
				toastTextView = (TextView) toastLayout.findViewById(R.id.text);
				toastTextView.setTypeface(educFont);
				toastTextView.setText(s);
				
				
//				ImageView img=(ImageView) toastLayout.findViewById(R.id.imageView);
//				img.setImageResource(R.drawable.ic_launcher);
				toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setView(toastLayout);
//				toast=toast.makeText(LamrimReaderActivity.this, s, Toast.LENGTH_SHORT);
				toast.show();
				//toast.setText(s);
				//toast.show();
			}
		});
	}
	
	public void showNarmalToastMsg(final String s){
		runOnUiThread(new Runnable() {
			public void run() {
				toast.cancel();
                toast = new Toast(getApplicationContext());

                LayoutInflater inflater = getLayoutInflater();
                toastLayout = inflater.inflate(R.layout.toast_text_view, (ViewGroup) findViewById(R.id.toastLayout));
                toastTextView = (TextView) toastLayout.findViewById(R.id.text);
                toastTextView.setTypeface(educFont);
                toastTextView.setText(s);
               
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(toastLayout);
//              toast=toast.makeText(LamrimReaderActivity.this, s, Toast.LENGTH_SHORT);
                toast.show();
                //toast.setText(s);
                //toast.show();

			}
		});
	}

	private TranslateAnimation getShowControllerAnimation() {
		TranslateAnimation showController = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		showController.setDuration(100);
		return showController;
	}

	private TranslateAnimation getHideControllerAnimation() {
		TranslateAnimation hideController = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 1.0f);
		hideController.setDuration(100);
		return hideController;
	}

	private int getSubtitleWordCountMax(TextView view) {
		// Determine how many words can show per line.
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int count = (int) ((float) screenWidth / view.getPaint().measureText(
				"中"));
		Log.d(logTag, "Width of screen: " + screenWidth + ", Width of Word: "
				+ view.getPaint().measureText("中") + ", There are " + count
				+ " can show in one line.");
		return count;
	}


	private void showSetTextSizeDialog(){
		final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
		LayoutInflater factory = (LayoutInflater)this.getSystemService(LAYOUT_INFLATER_SERVICE);
	    final View v = factory.inflate(R.layout.set_text_size_dialog_view, null);
	    final SeekBar volumeController=(SeekBar) v.findViewById(R.id.theorySizeBar);
	    final int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		final int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		volumeController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {}
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {}
			@Override
			public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
				audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
				GaLogger.sendEvent("ui_action", "SeekBar_scored", "volume_control_arg1", null);
			}
		});
		
	    final SeekBar subtitleSb=(SeekBar) v.findViewById(R.id.subtitleSizeBar);
	    final int orgSubtitleSize=runtime.getInt(getString(R.string.subtitleFontSizeKey), getResources().getInteger(R.integer.defFontSize))-getResources().getInteger(R.integer.textMinSize);
	    final int textMaxSize=getResources().getInteger(R.integer.textMaxSize)-getResources().getInteger(R.integer.textMinSize);

	    Log.d(logTag,"Set theory size Max="+(textMaxSize)+", orgSize="+orgSubtitleSize+", subtitle size Max="+textMaxSize+", orgSize="+orgSubtitleSize);
	    runOnUiThread(new Runnable(){
			@Override
			public void run() {
				volumeController.setMax(maxVolume);
				volumeController.setProgress(curVolume);
			    subtitleSb.setMax(textMaxSize);
			    subtitleSb.setProgress(orgSubtitleSize);
			}});

	    subtitleSb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {
				if(!fromUser)return;
				final int minSize = getResources().getInteger(R.integer.textMinSize);
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						Log.d(logTag,"Seek bar get progress: "+progress+", min size: "+getResources().getInteger(R.integer.textMinSize)+", add:"+(progress+getResources().getInteger(R.integer.textMinSize)));
						subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, progress+minSize);
						seekBar.setProgress(progress);
					}});
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}
	    });
	    
//	    dialog.show();
	    
	    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
	    AlertDialog setTextSizeDialog=builder.create();
	    setTextSizeDialog.setView(v);
	    WindowManager.LayoutParams lp = setTextSizeDialog.getWindow().getAttributes();
	    lp.alpha=0.7f;
	    setTextSizeDialog.setOnDismissListener(new DialogInterface.OnDismissListener (){
			@Override
			public void onDismiss(DialogInterface dialog) {
				SharedPreferences.Editor editor = runtime.edit();
				editor.putInt(getString(R.string.subtitleFontSizeKey), subtitleSb.getProgress()+getResources().getInteger(R.integer.textMinSize));
				editor.commit();
				
				Log.d(logTag,"Check size after write to db: theory size: "+runtime.getInt(getString(R.string.bookFontSizeKey), 0) + ", subtitle size: "+runtime.getInt(getString(R.string.subtitleFontSizeKey),0));
//				updateTextSize();
				dialog.dismiss();
			}});
	    setTextSizeDialog.setCanceledOnTouchOutside(true);
	    setTextSizeDialog.show();
	}
	
	private void showSaveRegionDialog(){
		int regionStartMs=mpController.getRegionStartPosition();
		int regionEndMs=mpController.getRegionEndPosition();
	    final SubtitleElement startSubtitle=mpController.getSubtitle(regionStartMs);
	    final SubtitleElement endSubtitle=mpController.getSubtitle(regionEndMs-1);
	    String info=startSubtitle.text+" ~ "+endSubtitle.text;

	    Log.d(logTag,"Check size of region list before: "+RegionRecord.records.size());
		Runnable callBack=new Runnable(){
			@Override
			public void run() {
				runOnUiThread(new Runnable(){
				@Override
				public void run() {
					regionFakeList.add(fakeSample);
					if(regionRecordAdapter!=null)Log.d(logTag,"Warring: the regionRecordAdapter = null !!!");
					else regionRecordAdapter.notifyDataSetChanged();
					Log.d(logTag,"Check size of region list after: "+RegionRecord.records.size());
				}});
			}};
		
			BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this,mediaIndex , regionStartMs, regionEndMs, null, info, -1, callBack);
			GaLogger.sendEvent("ui_action", "show_dialog", "save_region", null);
	}
	
	private void showRecordListPopupMenu(){
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View popupView = inflater.inflate(R.layout.popup_record_list, null);
		
		Rect rectgle= new Rect();
		Window window= getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		int StatusBarHeight= rectgle.top;
		int contentViewTop= window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
		int titleBarHeight= contentViewTop - StatusBarHeight;
		int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		int subtitleViewHeight=((TextView)findViewById(R.id.subtitleView)).getHeight();
		//int listViewHeight=screenHeight-titleBarHeight-subtitleViewHeight;
		int listViewHeight=screenHeight-contentViewTop;
		
		Log.i(logTag, "StatusBar Height= " + StatusBarHeight + " , TitleBar Height = " + titleBarHeight);
		final PopupWindow popupWindow = new PopupWindow(
				//findViewById(R.layout.popup_record_list),
				popupView,
				//LayoutParams.WRAP_CONTENT, listViewHeight);  
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        popupWindow.setContentView(popupView);  
        
		regionListView=(ListView) popupView.findViewById(R.id.recordListView);
        regionListView.setAdapter(regionRecordAdapter); 
        regionListView.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, final int position, long id) {
				Log.d(logTag,"Region record menu: item "+RegionRecord.records.get(position).title+" clicked.");
				File media=FileSysManager.getLocalMediaFile(RegionRecord.records.get(position).mediaIndex);
				File subtitle=FileSysManager.getLocalSubtitleFile(RegionRecord.records.get(position).mediaIndex);
				if(media == null || subtitle ==null || !media.exists() || !subtitle.exists()){
					String msg=String.format(getString(R.string.dlgResNeedDownloadFirst), SpeechData.getNameId(RegionRecord.records.get(position).mediaIndex));
					AlertDialog.Builder dialog = new AlertDialog.Builder(LamrimReaderActivity.this);
					dialog.setTitle(msg); 
					dialog.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {  
					    public void onClick(DialogInterface dialog, int which) {
					    	Bundle bundle=new Bundle();
					    	bundle.putInt("index", RegionRecord.records.get(position).mediaIndex);
					    	final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
					    	speechMenu.putExtras(bundle);
					    	if (wakeLock.isHeld())wakeLock.release();
					    	startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
					    }  
					}); 
					dialog.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {  
					    public void onClick(DialogInterface dialog, int which) {
					    	dialog.dismiss();
					    	if (wakeLock.isHeld())wakeLock.release();
					    }  
					});
					if(!wakeLock.isHeld()){wakeLock.acquire();}
					dialog.show();
					return;
				}
				
				
				
				mpController.desetPlayRegion();
				mpController.reset();
				SharedPreferences.Editor editor = runtime.edit();
				editor.putInt("mediaIndex", RegionRecord.records.get(position).mediaIndex);
				editor.putInt("playPosition", RegionRecord.records.get(position).startTimeMs);
				editor.commit();
				popupWindow.dismiss();
				mediaIndex=RegionRecord.records.get(position).mediaIndex;
				regionPlayIndex=position;
				startPlay(mediaIndex);
				renderMode=SUBTITLE_MODE;
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						subtitleView.setGravity(Gravity.CENTER);
						subtitleView.setHeight(subtitleView.getLineHeight());
					}});
				GaLogger.sendEvent("dialog_action", "select_record", "play_saved_region", position);
				// The procedure will not return to onStart or onResume, start play media from here.
			}});

		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener (){
			@Override
			public void onDismiss() {
				SharedPreferences.Editor editor = runtime.edit();
				String pageKey=getString(R.string.regionRecordListViewPage);
				String pageShiftKey=getString(R.string.regionRecordListViewPageShift);
				int pageCount=regionListView.getFirstVisiblePosition();
				View v=regionListView.getChildAt(0);  
		        int shift=(v==null)?0:v.getTop();
		        
				editor.putInt(pageKey, pageCount);
				editor.putInt(pageShiftKey, shift);
				editor.commit();
				GaLogger.sendEvent("dialog_action", "select_record", "cancel_select", null);
		}});
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new BitmapDrawable());
//        popupWindow.setWidth((int) (getWindowManager().getDefaultDisplay().getWidth()*0.4));
        

//        popupWindow.setContentView(findViewById(R.id.rootLayout));
        //AnimationUtils.loadAnimation(getApplicationContext(),R.anim.bounce);
        popupWindow.setAnimationStyle(R.style.AnimationPopup);
        popupWindow.update();
        popupWindow.showAtLocation(findViewById(R.id.rootLayout), Gravity.LEFT|Gravity.TOP, 0, contentViewTop);
        GaLogger.sendEvent("ui_action", "show_dialog", "record_list_popup_menu_count", regionRecordAdapter.getCount());
        //popupWindow.showAsDropDown(findViewById(R.id.subtitleView),0, 0);
        //popupWindow.showAsDropDown(findViewById(R.id.subtitleView));
	}
	
	final String scaleStr[]={"等比擴展填滿置中", "等比擴展全圖顯示置中", "不按比例完全擴展", "等比縮放置上", "等比縮放置中", "等比縮放置底", "不縮放置中",  "向量"};
	final ImageView.ScaleType scaleType[]={ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_XY, ImageView.ScaleType.FIT_START, ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_END, ImageView.ScaleType.CENTER,  ImageView.ScaleType.MATRIX};
	private void showScaleTypeDialog(){
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
	    builderSingle.setIcon(R.drawable.ic_launcher);
	    builderSingle.setTitle("選擇擴展方式:-")
	    .setItems(scaleStr, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				bookView.setScaleType(scaleType[which]);
				SharedPreferences.Editor editor = runtime.edit();
				Log.d(logTag,"Set image scale type: "+scaleStr[which]);
				editor.putInt(getString(R.string.mainImgScaleKey), which);
				editor.commit();
			}});
	    builderSingle.setNegativeButton("cancel",
	            new DialogInterface.OnClickListener() {

	                @Override
	                public void onClick(DialogInterface dialog, int which) {
	                    dialog.dismiss();
	                }
	            });
	    
	    builderSingle.show();
	}
	
	class RegionRecordAdapter extends SimpleAdapter{

		public RegionRecordAdapter(Context context,	List<? extends Map<String, ?>> data, int resource, String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
        public View getView(final int position, View convertView, ViewGroup parent) {
        	View row = convertView;
        	if (row == null) {
        		Log.d(getClass().getName(), "row=null, construct it.");
        		LayoutInflater inflater = getLayoutInflater();
        		row = inflater.inflate(R.layout.popup_record_list_row, parent, false);
        	}
        	
        	RegionRecord record=RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
        	Log.d(getClass().getName(), "Set: "+record.title);
        	TextView title = (TextView) row.findViewById(R.id.regionRowTitle);
        	TextView timeReg = (TextView) row.findViewById(R.id.timeRegion);
 
        	
        	TextView info = (TextView) row.findViewById(R.id.info);
        	
        	ImageButton editButton = (ImageButton) row.findViewById(R.id.editButton);
        	ImageButton delButton = (ImageButton) row.findViewById(R.id.deleteButton);
        	
        	title.setText(record.title);
      		timeReg.setText(SpeechData.getNameId(record.mediaIndex)+"    "+getMsToHMS(record.startTimeMs,"\"","'",false)+" ~ "+getMsToHMS(record.endTimeMs,"\"","'",false));
      		info.setText(record.info);
      		Log.d(logTag,"Info: "+record.info);
      			
      		editButton.setFocusable(false);
      		delButton.setFocusable(false);
      			
      		editButton.setOnClickListener(new View.OnClickListener(){
      		@Override
      			public void onClick(View v) {
      				RegionRecord rr=RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
      				Runnable callBack=new Runnable(){
      					@Override
      					public void run() {
      						runOnUiThread(new Runnable(){
							@Override
							public void run() {
								regionRecordAdapter.notifyDataSetChanged();
							}});
       					}};
        				BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this, mediaIndex , rr.startTimeMs, rr.endTimeMs, rr.title, null, position, callBack);
        			}});
        			
      		delButton.setOnClickListener(new View.OnClickListener(){
      			@Override
      			public void onClick(View v) {
      				BaseDialogs.showDelWarnDialog(
        				LamrimReaderActivity.this,
        				"記錄",
        				null,
        				new DialogInterface.OnClickListener(){
        					@Override
        						public void onClick(DialogInterface dialog,	int which) {
        							RegionRecord.removeRecord(LamrimReaderActivity.this, position);
        							regionFakeList.remove(position);
        							regionRecordAdapter.notifyDataSetChanged();
        						}},
        					null,
        					null);
      				}});
      		return row;
		}
	};
	
	
	private String getMsToHMS(int ms,String minuteSign,String secSign,boolean hasDecimal){
		String sub=""+(ms%1000);
		if(sub.length()==1)sub="00"+sub;
		else if(sub.length()==2)sub="0"+sub;
	
		int second=ms/1000;
		int ht=second/3600;
		second=second%3600;
		int mt=second/60;
		second=second%60;
	
		String hs=""+ht;
		if(hs.length()==1)hs="0"+hs;
		String mst=""+mt;
		if(mst.length()==1)mst="0"+mst;
		String ss=""+second;
		if(ss.length()==1)ss="0"+ss;
	
//	System.out.println("getMSToHMS: input="+ms+"ms, ht="+ht+", mt="+mt+", sec="+second+", HMS="+hs+":"+ms+":"+ss+"."+sub);
		return mst+minuteSign+ss+((hasDecimal)?"."+sub:"")+secSign;
	}


}
