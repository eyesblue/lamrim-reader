package eyes.blue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URLEncoder;
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
import afzkl.development.colorpickerview.dialog.ColorPickerDialog;
import afzkl.development.colorpickerview.view.ColorPickerView;
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
import android.graphics.drawable.TransitionDrawable;
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
import com.actionbarsherlock.app.SherlockFragmentActivity;
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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
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
import com.winsontan520.wversionmanager.library.WVersionManager;

import eyes.blue.SpeechMenuActivity.SpeechListAdapter;
import eyes.blue.modified.MyListView;
import eyes.blue.modified.MyHorizontalScrollView;
import eyes.blue.modified.OnDoubleTapEventListener;
import android.view.ScaleGestureDetector.SimpleOnScaleGestureListener;
/**
 * 更新: $$Date: 2013-12-29 12:01:44 +0800 (Sun, 29 Dec 2013) $$ 作者: $$Author:
 * kingofeyesblue@gmail.com $$ 版本: $$Revision: 111 $$ ID ：$$Id:
 * LamrimReaderActivity.java 111 2013-12-29 04:01:44Z kingofeyesblue@gmail.com
 * $$
 */
public class LamrimReaderActivity extends SherlockFragmentActivity {
	/** Called when the activity is first created. */
	private static final long serialVersionUID = 4L;
	final static String logTag = "LamrimReader";
	final static String funcInto = "Function Into";
	final static String funcLeave = "Function Leave";

	final static int SPEECH_MENU_RESULT = 0;
	final static int THEORY_MENU_RESULT = 1;
	final static int SPEECH_MENU_RESULT_REGION = 2;
	final static int GLOBAL_LAMRIM_RESULT = 3;
	final static int SELECT_FG_PIC_RESULT = 4;
	final static int SUBTITLE_MODE = 1;
	final static int READING_MODE = 2;
	
	

	int subtitleViewRenderMode = SUBTITLE_MODE;
	static int mediaIndex = -1;
	MediaPlayerController mpController;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	MyListView bookView = null;
	ImageView renderView = null;
	TextView subtitleView = null;
	SharedPreferences runtime = null;

	MenuItem rootMenuItem, speechMenu, globalLamrim, playRegionRec,swRenderMode, prjWeb, exitApp;

	FileSysManager fileSysManager = null;
	// FileDownloader fileDownloader = null;
	RelativeLayout rootLayout = null;

	Typeface educFont = null;
	View toastLayout = null;
	TextView toastTextView = null;
	ImageView toastSubtitleIcon;
	ImageView toastInfoIcon;
	// ArrayList<RegionRecord> regionRecord = null;

	// the 3 object is paste on the popupwindow object, it not initial at
	// startup.
	SimpleAdapter regionRecordAdapter = null;
	ArrayList<HashMap<String, String>> regionFakeList = null;
	ListView regionListView = null;

	HashMap<String, String> fakeSample = new HashMap();
	PackageInfo pkgInfo = null;

	View actionBarControlPanel = null;
	ImageView bookIcon = null;
	EditText jumpPage = null;
	SeekBar volumeController = null;
	ImageButton textSize = null;
	ImageButton search = null;
	
	int[][] readingModeSEindex = null;
	String readingModeAllSubtitle = null;
	static Point screenDim = new Point();
	Button modeSwBtn = null;
	GlRecord glRecord=null;
	int[] bookViewMountPoint={0,0};
	
	int theoryHighlightRegion[]=new int[4];//{startPage, startLine, endPage, endLine}
	int[][] GLamrimSect=new int[2][3];
	int GLamrimSectIndex=-1;
	String actionBarTitle="";
	String regionStartInfo, regionEndInfo;
	
	PrevNextListener prevNextListener = null;
	final int[] regionSet={-1,-1,-1,-1};
	
	final ImageView.ScaleType scaleType[]={ImageView.ScaleType.CENTER_CROP, ImageView.ScaleType.CENTER_INSIDE, ImageView.ScaleType.FIT_XY, ImageView.ScaleType.FIT_START, ImageView.ScaleType.FIT_CENTER, ImageView.ScaleType.FIT_END, ImageView.ScaleType.CENTER,  ImageView.ScaleType.MATRIX};
	WVersionManager versionManager=null;
	
//	boolean repeatPlay=false;
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
///		requestWindowFeature(Window.FEATURE_NO_TITLE);
//		requestWindowFeature(com.actionbarsherlock.view.Window.FEATURE_ACTION_BAR_OVERLAY);

		setContentView(R.layout.main);
		getSupportActionBar();

		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);

		Log.d(funcInto, "******* Into LamrimReader.onCreate *******");

		// Check new version
		versionManager = new WVersionManager(LamrimReaderActivity.this);
		versionManager.setTitle("新版本已發佈");
		versionManager.setUpdateNowLabel("立即更新");
		versionManager.setRemindMeLaterLabel("稍後通知我");
		versionManager.setIgnoreThisVersionLabel("忽略此版本");
		versionManager.setReminderTimer(10);
	    versionManager.setVersionContentUrl(getString(R.string.versionCheckUrl)); // your update content url, see the response format below
	    versionManager.checkVersion();
	    
		if (savedInstanceState != null) Log.d(logTag, "The savedInstanceState is not null!");
		Log.d(getClass().getName(), "mediaIndex=" + mediaIndex);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
		educFont = Typeface.createFromAsset(this.getAssets(), "EUDC.TTF");
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
		// The value will get portrait but not landscape value sometimes,
		// exchange it if happen.
		if (screenDim.x < screenDim.y)
			screenDim.set(screenDim.y, screenDim.x);

		LayoutInflater factory = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
		actionBarControlPanel = factory.inflate(R.layout.action_bar_control_panel, null);
		modeSwBtn = (Button) findViewById(R.id.modeSwBtn);
		modeSwBtn.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					modeSwBtn.setPressed(false);
					return true;
				}

				modeSwBtn.setPressed(true);
				int height = (int) (screenDim.y - event.getRawY());
				float upBoundDp = (float) getResources().getInteger(R.integer.subtitleScrollTouchBtnHeightPercentDp) / 100 * screenDim.y;
				int minHeight = (int) subtitleView.getLineHeight();
				int maxHeight = (int) (rootLayout.getHeight() - upBoundDp);
				// int maxHeight=(int)
				// (rootLayout.getHeight()-getResources().getDisplayMetrics().density*getResources().getInteger(R.integer.subtitleScrollTouchUpperBoundDp));

				// synchronized (mpController){
				// set Subtitle mode
				if (height <= minHeight) {
					height = minHeight;
					setSubtitleViewMode(SUBTITLE_MODE);
					if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING && mpController.getSubtitle() != null) {
						if (mpController.getCurrentPosition() == -1)
							return true;
						setSubtitleViewText(mpController.getSubtitle(mpController.getCurrentPosition()).text);
					} else
						setSubtitleViewText(getString(R.string.dlgHintShowMpController));
				}
				// set reading mode
				else {
					// It is first time into reading mode, set the all text to
					// subtitleView, but not set text every time.
					if (subtitleViewRenderMode == SUBTITLE_MODE) {
						if (mpController == null || !mpController.isSubtitleReady()	|| readingModeAllSubtitle == null) 
						{
							Util.showErrorPopupWindow(LamrimReaderActivity.this, findViewById(R.id.rootLayout), getString(R.string.dlgHintLoadMediaBeforeSwitchToReadingMode));
							return true;
						}
						setSubtitleViewMode(READING_MODE);
					}
				}
				// Log.d(logTag, "Set height to: "+height);
				if (height > maxHeight)
					height = maxHeight;
				subtitleView.setHeight(height);
				
				return true;
			}
		});

		bookIcon = (ImageView) actionBarControlPanel.findViewById(R.id.bookIcon);
		/*
		 * bookIcon.setOnClickListener(new View.OnClickListener(){
		 * 
		 * @Override public void onClick(View v) { if(mediaIndex<0 ||
		 * mediaIndex>=SpeechData.name.length)return; final int
		 * pageNum=SpeechData.refPage[mediaIndex]-1; if(pageNum==-1)return;
		 * //bookView.setItemChecked(pageNum, true); setTheoryArea(pageNum, 0);
		 * Log.d(logTag,"Jump to theory page index "+pageNum); //
		 * adapter.notifyDataSetChanged(); }});
		 */
		jumpPage = (EditText) actionBarControlPanel.findViewById(R.id.jumpPage);
		jumpPage.setGravity(Gravity.CENTER);
		jumpPage.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				Log.d(logTag, "User input jump page: "
						+ jumpPage.getText().toString());
				InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(jumpPage.getWindowToken(), 0);

				int num;
				if (jumpPage.getText().toString() == null)
					return false;
				String input = jumpPage.getText().toString().trim();
				if (input.length() == 0 || !input.matches("[0-9]+")) {
					new Handler().postDelayed(new Runnable() {
						@Override
						public void run() {
							bookView.setSelectionFromTop( bookView.getFirstVisiblePosition(), 0);}}, 200);
					return false;
				}
				num = Integer.parseInt(jumpPage.getText().toString());
				if (num > bookView.getCount())
					num = bookView.getCount();
				else if (num < 1)
					num = 1;

				final int pageNum = num - 1;

				new Handler().postDelayed(new Runnable() {

					@Override
					public void run() {
						bookView.setSelectionFromTop(pageNum, 0);
						GaLogger.sendEvent("ui_action", "EditText_edited", "jump_page_" + pageNum, null);
					}
				}, 200);
				// bookView.setItemChecked(num-1, true);
				// bookView.setSelection(pageNum);
				Log.d(logTag, "Jump to theory page index " + (num - 1));
				// adapter.notifyDataSetChanged();
				return false;
			}
		});

		final AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
		volumeController = (SeekBar) actionBarControlPanel.findViewById(R.id.volumeController);
		volumeController.setMax(maxVolume);
		volumeController.setProgress(curVolume);
		volumeController.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
					@Override
					public void onStopTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onStartTrackingTouch(SeekBar arg0) {
					}

					@Override
					public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,	arg1, 0);
						GaLogger.sendEvent("ui_action", "SeekBar_scored", "volume_control_arg1", null);
					}
				});
		textSize=(ImageButton) actionBarControlPanel.findViewById(R.id.textSize);
		textSize.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				showSetTextSizeDialog();
			}});
		
		search=(ImageButton) actionBarControlPanel.findViewById(R.id.search);
		search.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				search.setEnabled(false);
				showSearchDialog();
				search.setEnabled(true);
			}});
		fakeSample.put(null, null);
		RegionRecord.init(this);
		regionFakeList = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < RegionRecord.records.size(); ++i)
			regionFakeList.add(fakeSample);

		regionRecordAdapter = new RegionRecordAdapter(this, regionFakeList,
				android.R.layout.simple_list_item_2, new String[] { "title","desc" },
				new int[] { android.R.id.text1, android.R.id.text2 });

		if (mpController != null)
			Log.d(logTag, "The media player controller is not null in onCreate!!!!!");
		if (mpController == null)
			// mpController = new
			// MediaPlayerController(LamrimReaderActivity.this,
			// LamrimReaderActivity.this.findViewById(android.R.id.content), new
			// MediaPlayerControllerListener() {
			mpController = new MediaPlayerController(LamrimReaderActivity.this,
					LamrimReaderActivity.this.findViewById(R.id.mediaControllerMountPoint),
					new MediaPlayerControllerListener() {
						@Override
						public void onSubtitleChanged(final int index,
								final SubtitleElement subtitle) {
							// Log.d(getClass().getName(), "Set subtitle: "+
							// subtitle.text);
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// synchronized (mpController){
									switch (subtitleViewRenderMode) {
									case SUBTITLE_MODE:
										subtitleView.setText(subtitle.text);
										int lineCount = subtitleView.getLineCount();// There will return 0 sometimes.
										if (lineCount < 1)
											lineCount = 1;
										subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);
										break;
									case READING_MODE:
										// SpannableString str=new
										// SpannableString
										// (subtitleView.getText());
										SpannableString str = new SpannableString(readingModeAllSubtitle);
										// Spannable WordtoSpan = (Spannable)
										// subtitleView.getText();
										try {
											// *************** Bug here **************
											// Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
											str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)),
													readingModeSEindex[index][0],
													readingModeSEindex[index][1],
													Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
											subtitleView.setText(str);
										} catch (Exception e) {
											e.printStackTrace();
											GaLogger.sendException("mediaIndex="+ mediaIndex
															+ ", subtitleIndex="+ index
															+ ", totalLen="	+ str.length(), e,true);
										}
										break;
									}
									;
								}
							});
						}

						@Override
						public void onPlayerError() {
							setSubtitleViewText(getString(R.string.app_name));
							GaLogger.sendEvent("error", "player_error", "error_happen", null);
						}

						@Override
						public void onSeek(final int index,	final SubtitleElement subtitle) {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									// synchronized (mpController){
									switch (subtitleViewRenderMode) {
									case SUBTITLE_MODE:
										//Util.showSubtitleToast(LamrimReaderActivity.this, subtitle.text+ " - (" + Util.getMsToHMS(subtitle.startTimeMs, "\"", "'", false) + " - "	+ Util.getMsToHMS(subtitle.endTimeMs, "\"", "'", false) + ')');
										Util.showSubtitlePopupWindow(LamrimReaderActivity.this, rootLayout, subtitle.text+ " - (" + Util.getMsToHMS(subtitle.startTimeMs, "\"", "'", false) + " - "	+ Util.getMsToHMS(subtitle.endTimeMs, "\"", "'", false) + ')');
										break;
									case READING_MODE:
										SpannableString str = new SpannableString(readingModeAllSubtitle);
										try {
											// *************** Bug here **************
											// Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
											str.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.subtitleRedingModeHilightColor)),
													readingModeSEindex[index][0],readingModeSEindex[index][1],Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
											subtitleView.setText(str);
										} catch (Exception e) {
											e.printStackTrace();
											GaLogger.sendException("mediaIndex="+ mediaIndex + ", subtitleIndex="+ index + ", totalLen=" + str.length(), e, true);
										}
										break;
									};
								}
							});
						}

						// @Override
						// public void startMoment(){setSubtitleViewText("");}
						@Override
						public void onMediaPrepared() {
							Log.d(getClass().getName(),	"MediaPlayer prepared, show controller.");
							GaLogger.sendEvent("play_action", "player_event", SpeechData.name[mediaIndex] + "_prepared", null);

							if (mpController.isSubtitleReady()) {
								setSubtitleViewText(getString(R.string.dlgHintShowMpController));
								SubtitleElement[] se = mpController.getSubtitle();
								readingModeSEindex = new int[se.length][2];
								readingModeAllSubtitle = new String();
								int wordCounter = 0;
								for (int i = 0; i < se.length; i++) {
									readingModeSEindex[i][0] = wordCounter;
									wordCounter += se[i].text.length();
									readingModeSEindex[i][1] = wordCounter;
									String str = se[i].text
											.replaceAll(",", "，");
									str = str.replaceAll("\\.", "。");
									str = str.replaceAll("!", "！");
									str = str.replaceAll(";", "；");
									str = str.replaceAll("\\?", "？");
									str = str.replaceAll(":", "：");
									readingModeAllSubtitle += str;
								}
							}
							else {
								setSubtitleViewText(getString(R.string.dlgHintMpControllerNoSubtitle));
							}

							ActionBar actionBar=getSupportActionBar();
							if(actionBar != null)actionBar.setTitle(actionBarTitle);
							if(GLamrimSectIndex!=-1){
								new Handler().postDelayed(new Runnable(){
									@Override
									public void run() {
										bookView.setHighlightLine(theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3]);
									}}, 1000);
								
								mpController.refreshSeekBar();
								Log.d(logTag, "GlobalLamrim mode: play index "+GLamrimSect[GLamrimSectIndex][0]+", Sec: "+GLamrimSect[GLamrimSectIndex][1]+":"+GLamrimSect[GLamrimSectIndex][2]);
								Log.d(getClass().getName(),"Mark theory: start page="+theoryHighlightRegion[0]+" start line="+theoryHighlightRegion[1]+", offset="+bookViewMountPoint[1]);
								
								int regionStart=GLamrimSect[GLamrimSectIndex][1];
								int regionEnd=GLamrimSect[GLamrimSectIndex][2];
								if(regionEnd==-1)regionEnd=mpController.getDuration()-1000;
								mpController.seekTo(GLamrimSect[GLamrimSectIndex][1]);
								if(GLamrimSect[1][0]==-1){
									setMediaControllerView(regionStart, regionEnd, false, false, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
								}
								else if(GLamrimSectIndex == 0){
									setMediaControllerView(regionStart, regionEnd, false, true, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
								}else{
									setMediaControllerView(regionStart, regionEnd, true, false, glModePrevNextListener.getPrevPageListener(), glModePrevNextListener.getNextPageListener());
								}
								
								mpController.start();
								
								//
								showMediaController();
/*							}else if (regionPlayIndex != -1) {
								Log.d(logTag, "Region Mode: This play event is region play, set play region.");
								bookView.setHighlightLine(theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3]);
								if(actionBar != null)actionBar.setTitle(getString(R.string.menuStrPlayRegionRecShortName)+": "+RegionRecord.records.get(regionPlayIndex).title);
								setMediaControllerView(RegionRecord.records.get(regionPlayIndex).startTimeMs,RegionRecord.records.get(regionPlayIndex).endTimeMs, true, true, normalModePrevNextListener.getPrevListener(), true, true, normalModePrevNextListener.getNextListener());
								mpController.seekTo(RegionRecord.records.get(regionPlayIndex).startTimeMs);
								mpController.start();
								regionPlayIndex = -1;*/
							} else {
								Log.d(logTag, "Normal mode: The play event is fire by user select a new speech.");
								
								// The title of actionBar will miss while restart the App.
								actionBarTitle=SpeechData.getNameId(mediaIndex);
								if(actionBar != null)actionBar.setTitle(actionBarTitle);
								
								int seekPosition = runtime.getInt("playPosition", 0);
								Log.d(logTag, "Seek to last play positon " + seekPosition);
								mpController.setPrevNextListeners(normalModePrevNextListener.getPrevPageListener(), normalModePrevNextListener.getNextPageListener());
								mpController.seekTo(seekPosition);
								showMediaController();
							}
						}

						@Override
						public void getAudioFocusFail() {
							setSubtitleViewText(getResources().getString(R.string.soundInUseError));
						}

						@Override
						public void onStartPlay() {
							Log.d(getClass().getName(),"Hide Title bar.");
//							hideTitle();
						}
						@Override
						public void onPause() {
							Log.d(getClass().getName(),"Show Title bar.");
//							showTitle();
							
							//getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
							//getSupportActionBar().show();
							
							if (wakeLock.isHeld())
								wakeLock.release();
						}
						@Override
						public void onComplatePlay() {
							Log.d(getClass().getName(),"Show Title bar.");
//							showTitle();
							if (wakeLock.isHeld())
								wakeLock.release();
						}
					});

		mpController.setOnRegionClick(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				showOnRegionOptionDialog(mediaIndex, mpController.getCurrentPosition());
			}
		});
		
		subtitleView = (TextView) findViewById(R.id.subtitleView);
		subtitleView.setTypeface(educFont);
		subtitleView.setBackgroundColor(getResources().getColor(R.color.defSubtitleBGcolor));

		// subtitleView = new TextView(LamrimReaderActivity.this);
		/*
		 * subtitleView.setOnClickListener(new OnClickListener() {
		 * 
		 * @Override public void onClick(View v) { Log.d(logTag, v +
		 * " been clicked, Show media plyaer control panel."); if
		 * (mpController.getMediaPlayerState() >=
		 * MediaPlayerController.MP_PREPARED)
		 * mpController.showMediaPlayerController(); } });
		 */
		final GestureDetector subtitleViewGestureListener = new GestureDetector(
				getApplicationContext(), new SimpleOnGestureListener() {
					@Override
					public boolean onDown(MotionEvent e) {
						return true;
					}

					@Override
					public boolean onSingleTapConfirmed(MotionEvent e) {
						Log.d(logTag, "SubtitleView been clicked, Show media plyaer control panel.");
						if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED){
							mpController.showControllerView(LamrimReaderActivity.this);
							showMediaController();
///							showTitle();
						}
						GaLogger.sendEvent("ui_action", "subtitle_event", "single_tap", null);
						return true;
					}

					@Override
					public boolean onDoubleTapEvent(MotionEvent e) {
						// If it stay in subtitle mode, do nothing.
						if (subtitleViewRenderMode == SUBTITLE_MODE)
							return false;
						if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING && mpController.getSubtitle() != null) {
							int index = mpController.getSubtitleIndex(mpController.getCurrentPosition());
							if (index == -1)
								return true;
							// subtitleView.bringPointIntoView(readingModeSEindex[index][0]);
							try {
								// *************** Bug here **************
								// Here will happen error while readingModeSEindex array under construct, but access fire by user at above line.
								int line = subtitleView.getLayout().getLineForOffset(readingModeSEindex[index][0]);
								subtitleView.scrollTo(subtitleView.getScrollX(), subtitleView.getLineBounds(line, null)	- subtitleView.getLineHeight());
							} catch (Exception et) {
								et.printStackTrace();
								GaLogger.sendException("readingModeSEindex under contruct and read.", et, true);
							}
						}
						return true;
					}

					@Override
					public boolean onScroll(MotionEvent e1, MotionEvent e2,
							float distanceX, float distanceY) {
						if (subtitleViewRenderMode == READING_MODE) {
							int y = (int) (subtitleView.getScrollY() + distanceY);

							// Unknown problem, there will return null on some
							// machine.
							Layout layout = subtitleView.getLayout();
							Log.d(logTag, "Layout is "
									+ ((layout == null) ? "null" : "not null"));
							if (layout == null)
								return true;
							// ======================================================
							int bottom = subtitleView.getLineBounds(
									subtitleView.getLayout().getLineForOffset(
											subtitleView.getText().length()),
									null)
									- subtitleView.getMeasuredHeight()
									+ subtitleView.getLineHeight();
							Log.d(logTag,
									"Org Y="
											+ y
											+ "layout.height="
											+ subtitleView.getLayoutParams().height
											+ ", subtitle.height="
											+ subtitleView.getHeight()
											+ ", measureHeight="
											+ subtitleView.getMeasuredHeight());
							if (y < 0)
								y = 0;
							if (y > bottom)
								y = bottom;
							// if(subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight()-y<0)y=subtitleView.getLayoutParams().height-subtitleView.getMeasuredHeight();
							subtitleView.scrollTo(subtitleView.getScrollX(), y);
							Log.d(logTag, "Scroll subtitle view to "
									+ subtitleView.getScrollX() + ", " + y);
						}
						return true;
					}
				});

		final ScaleGestureDetector stScaleGestureDetector = new ScaleGestureDetector(this.getApplicationContext(), new SimpleOnScaleGestureListener() {
			int textSizeMax=getResources().getInteger(R.integer.textMaxSize);
    		int textSizeMin=getResources().getInteger(R.integer.textMinSize);
					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						Log.d(getClass().getName(),	"Begin scale called factor: " + detector.getScaleFactor());
						GaLogger.sendEvent("ui_action", "subtitle_event", "scale_start", null);
						return true;
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						float size = subtitleView.getTextSize()	* detector.getScaleFactor();
						if(size<textSizeMin || size > textSizeMax)return true;
						// Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
						subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
						// Log.d(getClass().getName(),"Realy size after setting: "+adapter.getTextSize());
						if (subtitleViewRenderMode == SUBTITLE_MODE)
							subtitleView.setHeight(subtitleView.getLineHeight());

						return true;
					}

					@Override
					public void onScaleEnd(ScaleGestureDetector detector) {
						SharedPreferences.Editor editor = runtime.edit();
						editor.putInt(getString(R.string.subtitleFontSizeKey), (int) subtitleView.getTextSize());
						editor.commit();
						GaLogger.sendEvent("ui_action", "subtitle_event", "scale_end", null);
					}
				});

		subtitleView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				try {
					if (event.getPointerCount() == 2) {
						return stScaleGestureDetector.onTouchEvent(event);
					}
					boolean res = subtitleViewGestureListener.onTouchEvent(event);
					return res;
					// Log.d(logTag, "Subtitle OnTouchListener return "+res);
				} catch (Exception e) {
					e.printStackTrace();
					GaLogger.sendEvent("exception", "SubtitleView",
							"ScaleGestureDetector", null);
					return true;
				}

			}

		});

		bookView = (MyListView) findViewById(R.id.bookPageGrid);
		bookView.setFadeColor(getResources().getColor(R.color.defSubtitleBGcolor));
		int defTheoryTextSize = getResources().getInteger(R.integer.defFontSize);
		final int theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey), defTheoryTextSize);
		bookView.setTextSize(theoryTextSize);
		int bookPage = runtime.getInt("bookPage", 0);
		int bookPageShift = runtime.getInt("bookPageShift", 0);
		bookView.setSelectionFromTop(bookPage, bookPageShift);

		bookView.setOnScrollListener(new AbsListView.OnScrollListener() {
			@Override
			public void onScroll(AbsListView view, final int firstVisibleItem,
					int visibleItemCount, int totalItemCount) {
				if (view == null)
					return;
				// if(bookList == null)return;
				String input = jumpPage.getText().toString().trim();
				if (input.length() == 0 || !input.matches("[0-9]+"))
					return;
				int num = Integer.parseInt(jumpPage.getText().toString());
				if (num < 0 || num > bookView.getCount())
					return;

				int showNum = Integer.parseInt(jumpPage.getText().toString());
				if (showNum == firstVisibleItem + 1)
					return;

				Handler handler = new Handler() {};
				handler.post(new Runnable() {
					@Override
					public void run() {
						jumpPage.setText(String.valueOf(firstVisibleItem + 1));
					}
				});
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
			}
		});

		bookView.setOnDoubleTapEventListener(new OnDoubleTapEventListener() {
			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if(bookViewMountPoint[0]==-1)return true;
				bookView.setSelectionFromTop(bookViewMountPoint[0], bookViewMountPoint[1]);
				Log.d(getClass().getName(), "Jump to theory page index " + bookViewMountPoint[0]+" shift "+bookViewMountPoint[1]);
				GaLogger.sendEvent("ui_action", "bookview_event", "jump_to_audio_start", null);
				return true;
			}
		});

		bookView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					Log.d(getClass().getName(), "Hide media player controller.");
					mpController.hideMediaPlayerController();
				}
				return false;
			}
		});

		renderView = (ImageView) findViewById(R.id.renderView);
		File renderImage=null;
		String imgPath=runtime.getString(getString(R.string.renderImgFgPathKey), null);
		if(imgPath!=null)
			renderImage=new File(imgPath);
		
		if(renderImage != null && renderImage.exists())
			renderView.setImageURI(Uri.fromFile(renderImage));
		else renderView.setImageResource(R.drawable.master);
		
        renderView.setScaleType(scaleType[runtime.getInt(getString(R.string.renderImgScaleKey), 0)]);
        int color=runtime.getInt(getString(R.string.renderImgBgColorKey),0);
        renderView.setBackgroundColor(color);
        renderView.setOnLongClickListener(new View.OnLongClickListener(){
			@Override
			public boolean onLongClick(View v) {
				Log.d(getClass().getName(),"Into onLongClickListener of render image.");
				showRenderModeFirstLevelMenu();
				return true;
			}});
        renderView.setOnClickListener(new View.OnClickListener(){

			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"Into onLongClickListener of render image.");
				if(mpController!=null)mpController.hideMediaPlayerController();
			}});
 /*       renderView.setOnTouchListener(new View.OnTouchListener(){
            boolean cmdStart=false, hasFired=false;
            float xStart=-1, yStart=-1;
           
            @Override
            public boolean onTouch(View view, MotionEvent event) {
                    Log.d(getClass().getName(),"Into bookView.OnTouchListener");
                    return true;
            }});
        */
		// bookView.setScrollingCacheEnabled( false );
		rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
//		rootLayout.setLongClickable(false);

		fileSysManager = new FileSysManager(this);
		FileSysManager.checkFileStructure();

		// String appSubtitle=getString(R.string.app_name)
		// +" V"+pkgInfo.versionName+"."+pkgInfo.versionCode;
		String appSubtitle = getString(R.string.app_name) + " V" + pkgInfo.versionName;
		ActionBar actionBar=getSupportActionBar();
		// Disable show App icon.
		actionBar.setDisplayShowHomeEnabled(false);
		if(actionBar != null)actionBar.setSubtitle(appSubtitle);
		
		/*
		 * FragmentManager fm = getSupportFragmentManager(); mTaskFragment =
		 * (TaskFragment) fm.findFragmentByTag("PlayerTask");
		 * 
		 * // If the Fragment is non-null, then it is currently being //
		 * retained across a configuration change. if (mTaskFragment == null) {
		 * mTaskFragment = new TaskFragment();
		 * fm.beginTransaction().add(mTaskFragment, "PlayerTask").commit(); }
		 * Log.d(funcLeave, "******* onCreate *******");
		 */
	}
	
	private void swapRegionSet(){
		if(regionSet[0]<regionSet[2])return;
		if(regionSet[1]<regionSet[3])return;
		
		int swap=regionSet[0];
		regionSet[0]=regionSet[2];
		regionSet[2]=swap;
		
		swap=regionSet[1];
		regionSet[1]=regionSet[3];
		regionSet[3]=swap;
	}

	private void shareSegment(RegionRecord record){
		shareSegment(record.title, record.mediaStart, record.startTimeMs, record.mediaEnd, record.endTimeMs, record.theoryPageStart, record.theoryStartLine,record.theoryPageEnd,record.theoryEndLine);
	}
	private void shareSegment(String title, int speechStartIndex, int speechStartMs, int speechEndIndex, int speechEndMs, int theoryPageStart, int theoryStartLine, int theoryPageEnd, int theoryEndLine){
		String lamrimCmdUri=getString(R.string.lamrimCmdUri)+"play?";
		String queryStr="mode=region";
		String speechStart=GlRecord.getSpeechIndexToStr(speechStartIndex)+":"+Util.getMsToHMS(speechStartMs,":","",true);
		String speechEnd=GlRecord.getSpeechIndexToStr(speechEndIndex)+":"+Util.getMsToHMS(speechEndMs,":","",true);
		String theoryStart=(theoryPageStart+1)+":"+(theoryStartLine+1);
		String theoryEnd=(theoryPageEnd+1)+":"+(theoryEndLine+1);
		
		queryStr+="&speechStart="+speechStart+"&speechEnd="+speechEnd+"&theoryStart="+theoryStart+"&theoryEnd="+theoryEnd;
		if(title!=null)
			try {
				queryStr+="&title="+URLEncoder.encode(title,"utf8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		shareSegment(lamrimCmdUri+queryStr);
	}
	private void shareSegment(String msg){
		Intent sendIntent = new Intent();
    	sendIntent.setAction(Intent.ACTION_SEND);
    	sendIntent.putExtra(Intent.EXTRA_TEXT, msg);
    	sendIntent.setType("text/plain");
    	startActivity(Intent.createChooser(sendIntent, "區段分享"));
	}
	
	
	public void showOnRegionOptionDialog(final int mediaIndex, final int mediaPosition){
		final AlertDialog setRegionOptDialog=new AlertDialog.Builder(LamrimReaderActivity.this).create();
		
		LayoutInflater factory = (LayoutInflater)getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
	    final View v = factory.inflate(R.layout.region_option_dialog, null);
	    TextView startDesc=(TextView) v.findViewById(R.id.leftBoundDesc);
	    TextView endDesc=(TextView) v.findViewById(R.id.rightBoundDesc);
	    final LinearLayout leftBound=(LinearLayout) v.findViewById(R.id.setLeftBound);
	    final LinearLayout rightBound=(LinearLayout) v.findViewById(R.id.setRightBound);
	    final LinearLayout saveOpt=(LinearLayout) v.findViewById(R.id.saveOpt);
	    final LinearLayout shareOpt=(LinearLayout) v.findViewById(R.id.shareOpt);
	    final ImageView save=(ImageView) v.findViewById(R.id.save);
	    final ImageView share=(ImageView) v.findViewById(R.id.share);
	    
	    if(regionSet[0]!=-1){
	    	startDesc.setText(SpeechData.getNameId(regionSet[0])+":"+Util.getMsToHMS(regionSet[1],":","",true));
	    }
	    if(regionSet[2]!=-1){
	    	endDesc.setText(SpeechData.getNameId(regionSet[2])+":"+Util.getMsToHMS(regionSet[3],":","",true));
	    }
	    if(regionSet[0]!=-1 && regionSet[2]!=-1){
	    	saveOpt.setEnabled(true);
		    shareOpt.setEnabled(true);
	    	save.setEnabled(true);
	    	share.setEnabled(true);
	    }
	    else{
	    	saveOpt.setEnabled(false);
		    shareOpt.setEnabled(false);
	    	save.setEnabled(false);
	    	share.setEnabled(false);
	    }
	    
	    leftBound.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mpController == null || mpController.getSubtitle()==null)return;
				regionSet[0]=mediaIndex;
				regionSet[1]=mpController.getSubtitle(mediaPosition).startTimeMs;
				regionStartInfo=mpController.getSubtitle(mediaPosition).text;
				setRegionOptDialog.dismiss();
			}});
	    rightBound.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(mpController == null || mpController.getSubtitle()==null)return;
				regionSet[2]=mediaIndex;
				regionSet[3]=mpController.getSubtitle(mediaPosition).endTimeMs;
				regionEndInfo=mpController.getSubtitle(mediaPosition).text;
				setRegionOptDialog.dismiss();
			}});
	    saveOpt.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				if(Math.abs(regionSet[0]-regionSet[2])>1){
					BaseDialogs.showErrorDialog(LamrimReaderActivity.this, "只能標記相鄰的音檔");
					return;
				}
		
				swapRegionSet();
				
				BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this, regionSet[0] , regionSet[1], regionSet[2], regionSet[3], theoryHighlightRegion[0], theoryHighlightRegion[1], theoryHighlightRegion[2], theoryHighlightRegion[3], regionStartInfo+" ~ "+regionEndInfo, -1, new Runnable(){
					@Override public void run() {
						runOnUiThread(new Runnable(){
							@Override
							public void run() {
								regionFakeList.add(fakeSample);
                                if (regionRecordAdapter != null)
                                        Log.d(logTag, "Warring: the regionRecordAdapter = null !!!");
                                else
                                        regionRecordAdapter.notifyDataSetChanged();
							}});
						}
					}
				);
				setRegionOptDialog.dismiss();
			}});
	    
	    shareOpt.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View view) {
				if(Math.abs(regionSet[0]-regionSet[2])>1){
					BaseDialogs.showErrorDialog(LamrimReaderActivity.this, "只能標記相鄰的音檔");
					return;
				}
				
				swapRegionSet();
				LayoutInflater factory = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			    final View v = factory.inflate(R.layout.save_region_dialog_for_share, null);
			    
			    final TextView startTime=(TextView) v.findViewById(R.id.startTime);
			    final TextView endTime=(TextView) v.findViewById(R.id.endTime);
			    final String startHMS=SpeechData.getSubtitleName(regionSet[0])+"  "+Util.getMsToHMS(regionSet[1], ":", "", true);
				final String endHMS=SpeechData.getSubtitleName(regionSet[2])+"  "+Util.getMsToHMS(regionSet[3], ":", "", true);
				
				if(theoryHighlightRegion[0] !=0 && theoryHighlightRegion[1] !=0 && theoryHighlightRegion[2] !=0 && theoryHighlightRegion[3] !=0){
					((EditText)v.findViewById(R.id.startPage)).setText(""+(theoryHighlightRegion[0]+1));
					((EditText)v.findViewById(R.id.startLine)).setText(""+(theoryHighlightRegion[1]+1));
					((EditText)v.findViewById(R.id.endPage)).setText(""+(theoryHighlightRegion[2]+1));
					((EditText)v.findViewById(R.id.endLine)).setText(""+(theoryHighlightRegion[3]+1));
				}
				
			    runOnUiThread(new Runnable(){
					@Override
					public void run() {
						startTime.setText(startHMS);
						endTime.setText(endHMS);
				}});
			    
			    final AlertDialog.Builder builder = new AlertDialog.Builder(LamrimReaderActivity.this);
			    builder.setView(v);
			    builder.setTitle("分享區段");
			    builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						String inPageStart=((TextView)v.findViewById(R.id.startPage)).getText().toString();
						String inPageEnd=((TextView)v.findViewById(R.id.endPage)).getText().toString();
						String inLineStart=((TextView)v.findViewById(R.id.startLine)).getText().toString();
						String inLineEnd=((TextView)v.findViewById(R.id.endLine)).getText().toString();
					    
						// Check Theory page, start line and end line.
						int theoryPageStart=-1, theoryPageEnd=-1, inStartLine=-1, inEndLine=-1;
						try{
							theoryPageStart=Integer.parseInt(inPageStart.trim())-1;
							theoryPageEnd=Integer.parseInt(inPageEnd.trim())-1;
							inStartLine=Integer.parseInt(inLineStart.trim())-1;
							inEndLine=Integer.parseInt(inLineEnd.trim())-1;
						}catch(NumberFormatException nfe){
							BaseDialogs.showErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgNumberFormatError));
							dialog.dismiss();
							return;
						}

						if(theoryPageStart< 0 || theoryPageEnd< 0 || inStartLine<0 || inEndLine <0){
							BaseDialogs.showErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgPageNumOverPageCount));
							dialog.dismiss();
							return;
						}
						
						if(theoryPageStart>=TheoryData.content.length ||  theoryPageEnd >= TheoryData.content.length){
							BaseDialogs.showErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgPageNumOverPageCount));
							dialog.dismiss();
							return;
						}
						
						// Check if the same page, but end line greater then start line
						if(theoryPageEnd < theoryPageStart || (theoryPageEnd == theoryPageStart && inEndLine < inStartLine)){
							Log.d(getClass().getName(),"User input the same page, but line number end > start.");
							BaseDialogs.showErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgEndLineGreaterThenStart));
							dialog.dismiss();
							return;
						}
						
						// Check if the line count over the count of page.
						if(inStartLine<0 || inEndLine >= TheoryData.content[theoryPageEnd].length()){
							BaseDialogs.showErrorDialog(LamrimReaderActivity.this, getString(R.string.dlgLineNumOverPageCount));
							dialog.dismiss();
							return;
						}
						dialog.dismiss();
						Log.d(getClass().getName(),"Share region: speechStartIndex="+regionSet[0]+", speechTimeMs="+regionSet[1]+", speechEndIndex="+regionSet[2]+", speechTimeMs="+regionSet[3]+", TheoryStart="+theoryPageStart+":"+inStartLine+", theoryEnd="+theoryPageEnd+":"+inEndLine);
						shareSegment(null, regionSet[0], regionSet[1], regionSet[2], regionSet[3], theoryPageStart, inStartLine, theoryPageEnd, inEndLine);
					}});
				
			    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();
					}});
			    builder.show();
			}});
	    setRegionOptDialog.setView(v);
	    setRegionOptDialog.show();
	}
	

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(getClass().getName(), "**** onStart() ****");

		float modeSwBtnHeight = (float) getResources().getInteger(
				R.integer.subtitleScrollTouchBtnHeightPercentDp)
				/ 100 * screenDim.y;
		float modeSwBtnWidth = (float) getResources().getInteger(
				R.integer.subtitleScrollTouchBtnWidthPercentDp)
				/ 100 * screenDim.x;
		modeSwBtn.getLayoutParams().width = (int) modeSwBtnWidth;
		modeSwBtn.getLayoutParams().height = (int) modeSwBtnHeight;

		GaLogger.activityStart(this);
		GaLogger.sendEvent("activity", "LamrimReaderActivity", "into_onStart", null);

		int defTitleTextSize = getResources().getInteger(R.integer.defFontSize);
		final int subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), defTitleTextSize);
		subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subtitleTextSize);
		// int bookPage = runtime.getInt("bookPage", 0);
		// jumpPage.setText(bookPage);

		// Show change log if need.
		ChangeLog cl = new ChangeLog(this);
		if (cl.firstRun())
			cl.getLogDialog().show();

		
		
		Log.d(getClass().getName(), "**** Leave onStart() ****");
	}

	public static Point getScreenDim() {
		return screenDim;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(getClass().getName(), "**** Into onResume() ****");
		/*
		 * While in the sleep mode, the life cycle into onPause, when user
		 * active the application the life cycle become onResume -> onPause ->
		 * onDestroy -> onCreate -> onStart -> onResume, the media player still
		 * exist after the application recreate. the prepare method call twice
		 * both in the two onResume, the second prepare will throw
		 * illegalStageExcteption, and will cause error sometime, If the stage
		 * into PREPARING, it mean it preparing the media source at first
		 * onResume, then do nothing.
		 */
		/*
		 * try { if (mpController.getMediaPlayerState() >=
		 * MediaPlayerController.MP_PREPARING){
		 * Log.d(logTag,"onResume: The state of MediaPlayer is PAUSE, start play."
		 * ); //
		 * mpController.setAnchorView(LamrimReaderActivity.this.findViewById
		 * (android.R.id.content)); // mpController.showMediaPlayerController();
		 * return; } } catch (IllegalStateException e) { e.printStackTrace();}
		 */

		Intent cmdIntent=this.getIntent();
		Log.d(getClass().getName(), "Action: "+getIntent().getAction());
		if(cmdIntent != null && cmdIntent.getAction().equals(Intent.ACTION_VIEW)){
			GLamrimSectIndex=0;
			getIntent().setAction(Intent.ACTION_MAIN);
			String title=cmdIntent.getStringExtra("title");
			if(title!=null)actionBarTitle=getString(R.string.menuStrPlayRegionRecShortName)+": "+title;
			else actionBarTitle=getString(R.string.menuStrPlayRegionRecShortName)+": 未知標題";

			startRegionPlay(cmdIntent.getIntExtra("mediaStart", 0),
					cmdIntent.getIntExtra("startTimeMs",0), 
					cmdIntent.getIntExtra("mediaEnd",0), 
					cmdIntent.getIntExtra("endTimeMs",0), 
					cmdIntent.getIntExtra("theoryStartPage",0), 
					cmdIntent.getIntExtra("theoryStartLine",0), 
					cmdIntent.getIntExtra("theoryEndPage",0), 
					cmdIntent.getIntExtra("theoryEndLine",0));
			return;
		}
		
		// Check is mediaPlayer loaded.
		mediaIndex = runtime.getInt("mediaIndex", -1);
		Log.d(getClass().getName(), "Media index = "+mediaIndex);
		if ( !mpController.isPlayerReady() && mediaIndex != -1)
			startPlay(mediaIndex);

		Log.d(getClass().getName(), "**** Leave onResume() ****");
	}

	@Override
	protected void onPause() {
		super.onPause();
		saveRuntime();
		try {
			if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING)
				mpController.pause();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	@Override
	protected void onStop() {
		super.onStop();
		GaLogger.activityStop(this);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(funcInto, "**** onDestroy ****");
		// fileDownloader.finish();
		mpController.finish();
		Log.d(funcLeave, "**** onDestroy ****");
	}

	protected void saveRuntime() {
		Log.d(funcInto, "**** saveRuntime ****");
		SharedPreferences.Editor editor = runtime.edit();
		Log.d(logTag, "Save mediaIndex=" + mediaIndex);
		int bookPosition = bookView.getFirstVisiblePosition();
		View v = bookView.getChildAt(0);
		int bookShift = (v == null) ? 0 : v.getTop();

		Log.d(logTag, "MediaPlayer status=" + mpController.getMediaPlayerState());
		editor.putInt("mediaIndex", mediaIndex);
		// editor.putInt("playerStatus", mpController.getMediaPlayerState());
		if (mpController.getMediaPlayerState() > MediaPlayerController.MP_PREPARING) {
			int playPosition = mpController.getCurrentPosition();
			editor.putInt("playPosition", playPosition);
		}
		editor.putInt("bookPage", bookPosition);
		editor.putInt("bookPageShift", bookShift);
		Log.d(logTag, "Save content: mediaIndex=" + mediaIndex
						+ ", playPosition(write)=" + ", playPosition(read)="
						+ runtime.getInt("playPosition", -1) + ", book index="
						+ bookPosition + ", book shift=" + bookShift);
		editor.commit();
		Log.d(funcLeave, "**** saveRuntime ****");
	}

	@Override
	public void onBackPressed() {
		AlertDialog.Builder builder = new AlertDialog.Builder(
				LamrimReaderActivity.this);
		builder.setTitle(getString(R.string.dlgExitTitle));
		builder.setMessage(getString(R.string.dlgExitMsg));
		builder.setPositiveButton(getString(R.string.dlgOk),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
						saveRuntime();
						finish();
					}
				});
		builder.setNegativeButton(getString(R.string.dlgCancel),
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						dialog.cancel();
					}
				});
		builder.create().show();
		Log.d(funcInto, "**** onBackPressed ****");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// getSupportMenuInflater().inflate(R.menu.main, menu);
		// return super.onCreateOptionsMenu(menu);
		SubMenu rootMenu = menu.addSubMenu("");
		speechMenu = rootMenu.add(getString(R.string.menuStrSelectSpeech));
		speechMenu.setIcon(R.drawable.speech);
		globalLamrim = rootMenu.add(getString(R.string.globalLamrim));
		globalLamrim.setIcon(R.drawable.global_lamrim);
		playRegionRec = rootMenu.add(getString(R.string.menuStrPlayRegionRec));
		playRegionRec.setIcon(R.drawable.region);
		swRenderMode = rootMenu.add(getString(R.string.menuStrRenderMode));
		swRenderMode.setIcon(R.drawable.render_mode);
		prjWeb = rootMenu.add(getString(R.string.menuStrOpenProjectWeb));
		prjWeb.setIcon(R.drawable.project_web);
		exitApp = rootMenu.add(getString(R.string.exitApp));
		exitApp.setIcon(R.drawable.exit_app);

		rootMenuItem = rootMenu.getItem();
		// rootMenuItem.setIcon(R.drawable.menu_down_48x48);
		rootMenuItem.setIcon(R.drawable.menu_down);
		rootMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

		getSupportActionBar().setCustomView(actionBarControlPanel);
		getSupportActionBar().setDisplayShowCustomEnabled(true);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(funcInto, "****OptionsItemselected, select item=" + item.getItemId()
						+ ", String=" + item.getTitle() + ", Order=" + item.getOrder() + " ****");
		String gid = (String) item.getTitle();
		GaLogger.sendEvent("ui_action", "menu_event",
				((gid.length() == 0) ? "root_menu" : gid) + "_pressed", null);

		if (item.equals(rootMenuItem)) {
			Log.d(logTag, "Create menu: can save region? " + mpController.isRegionPlay());
			if (RegionRecord.records.size() > 0) {
				playRegionRec.setEnabled(true);
				playRegionRec.setIcon(R.drawable.region);
			} else {
				playRegionRec.setEnabled(false);
				playRegionRec.setIcon(R.drawable.region_d);
			}
		}

		if (item.getTitle().equals(getString(R.string.menuStrSelectSpeech))) {
			startSpeechMenuActivity();
		} else if (item.getTitle().equals(getString(R.string.globalLamrim))) {
			startGlobalLamrimCalendarActivity();
		}else if (item.getTitle().equals(getString(R.string.menuStrRenderMode))) {
			switchMainView();
		}  else if (item.getTitle().equals(getString(R.string.menuStrPlayRegionRec))) {
			showRecordListPopupMenu();
		} else if (item.getTitle().equals(getString(R.string.menuStrOpenProjectWeb))) {
			startProjectWebUrl();
		} else if (item.getTitle().equals(getString(R.string.exitApp))) {
			onBackPressed();
			Log.d(funcLeave, "**** onOptionsItemSelected ****");
		}
		/*
		 * switch (item.getItemId()) { case 1: final Intent speechMenu = new
		 * Intent(LamrimReaderActivity.this, SpeechMenuActivity.class); if
		 * (wakeLock.isHeld())wakeLock.release();
		 * startActivityForResult(speechMenu, SPEECH_MENU_RESULT); break; case
		 * 2: final Intent optCtrlPanel = new Intent(LamrimReaderActivity.this,
		 * OptCtrlPanel.class); if (wakeLock.isHeld())wakeLock.release();
		 * startActivityForResult(optCtrlPanel, OPT_MENU_RESULT); break; case 3:
		 * final Intent aboutPanel = new Intent(LamrimReaderActivity.this,
		 * AboutActivity.class); if (wakeLock.isHeld())wakeLock.release();
		 * this.startActivity(aboutPanel); break; }
		 */
		Log.d(funcLeave, "**** Into Options selected, select item=" + item.getItemId()+ " ****");
		return true;
	}
	
	private void startSpeechMenuActivity(){
		final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
		if (wakeLock.isHeld())wakeLock.release();
		startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
	}
	
	private void startGlobalLamrimCalendarActivity(){
		if (wakeLock.isHeld())wakeLock.release();
		final Intent calendarMenu = new Intent(LamrimReaderActivity.this,	CalendarActivity.class);
		startActivityForResult(calendarMenu, GLOBAL_LAMRIM_RESULT);
	}
	
	private void startProjectWebUrl(){
		if (wakeLock.isHeld())wakeLock.release();
		Uri uri = Uri.parse(getString(R.string.projectWebUrl));
		Intent it = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(it);
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Log.d(funcInto, "**** Into onActivityResult: Get result from: "	+ requestCode + " ****");

		if (resultCode == RESULT_CANCELED) {
			Log.d(logTag, "User skip, do nothing.");
			return;
		}
		
		SharedPreferences.Editor editor = runtime.edit();
		switch (requestCode) {
		case SPEECH_MENU_RESULT:
			Log.d(getClass().getName(),"Return from SPEECH_MENU_RESULT");
			if(intent == null)return;

			final int selected = intent.getIntExtra("index", -1);
			Log.d(logTag, "OnResult: the user select index=" + selected);
			if(selected == -1)return;

			mpController.setPlayRegion(-1, -1);
			mpController.reset();
			editor.putInt("mediaIndex", selected);
			editor.putInt("playPosition", 0);
			editor.commit();
			mediaIndex=selected;
			GLamrimSectIndex = -1;
			
			final int pageNum = SpeechData.refPage[selected] - 1;
			Log.d(getClass().getName(),"The speech reference theory page "+pageNum);
			if (pageNum >= 0){
				bookViewMountPoint[0]=pageNum;
				bookViewMountPoint[1]=0;
			}
			actionBarTitle=SpeechData.getNameId(selected);
			Log.d(logTag, "Call reset player in onActivityResult.");
			
			GaLogger.sendEvent("activity", "SpeechMenu_result", "select_index_"	+ selected, null);
			// After onActivityResult, the life-cycle will return to onStart,
			// do start downloader in OnResume.
			break;
		case SPEECH_MENU_RESULT_REGION:
			Log.d(getClass().getName(),"Return from SPEECH_MENU_RESULT_REGION");
			mpController.reset();

			editor.putInt("mediaIndex", GLamrimSect[0][0]);
			editor.putInt("playPosition", GLamrimSect[0][1]);
			editor.commit();
			mediaIndex = GLamrimSect[0][0];
			GLamrimSectIndex=0;
			
			// Action bar title has set in popup window.
			//actionBarTitle=getString(R.string.menuStrPlayRegionRecShortName)+": "+rec.title;
			Log.d(getClass().getName(),"Mark theory: start page="+theoryHighlightRegion[0]+" start line="+theoryHighlightRegion[1]+", offset="+bookViewMountPoint[1]);

			//startPlay(mediaIndex);
			break;
		case GLOBAL_LAMRIM_RESULT:
			if(intent == null)return;
			
			if(glRecord==null)glRecord=new GlRecord();
			glRecord.dateStart=intent.getStringExtra("dateStart");
			glRecord.dateEnd=intent.getStringExtra("dateEnd");
			glRecord.speechPositionStart=intent.getStringExtra("speechPositionStart");
			glRecord.speechPositionEnd=intent.getStringExtra("speechPositionEnd");
			glRecord.totalTime=intent.getStringExtra("totalTime");
			glRecord.theoryLineStart=intent.getStringExtra("theoryLineStart");
			glRecord.theoryLineEnd=intent.getStringExtra("theoryLineEnd");
			glRecord.subtitleLineStart=intent.getStringExtra("subtitleLineStart");
			glRecord.subtitleLineEnd=intent.getStringExtra("subtitleLineEnd");
			glRecord.desc=intent.getStringExtra("desc");
			actionBarTitle=intent.getStringExtra("selectedDay");
			Log.d(getClass().getName(),"Get data: "+glRecord);
			
			String sec[]=actionBarTitle.split("/");
			actionBarTitle=getString(R.string.globalLamrimShortName)+": "+sec[1]+"/"+sec[2];
			String regionInfo[]=glRecord.desc.split("……");
			regionStartInfo=regionInfo[0].trim();
			regionEndInfo=regionInfo[1].trim();
			setRegionSec(glRecord.speechPositionStart, glRecord.speechPositionEnd, glRecord.theoryLineStart, glRecord.theoryLineEnd);
			GLamrimSectIndex=0;
			mediaIndex = GLamrimSect[0][0];
			
			Log.d(getClass().getName(), "Set mediaIndex="+mediaIndex+", play position="+GLamrimSect[0][0]);
			editor.putInt("mediaIndex", GLamrimSect[0][0]);
			editor.putInt("playPosition", 0);
			editor.commit();
			
			mpController.reset();
			break;
			
		case SELECT_FG_PIC_RESULT:
			if(intent == null)return;
			final String filePath = intent.getStringExtra(FileDialogActivity.RESULT_PATH);
			File file=new File(filePath);
			
			if(!file.exists()){
				Util.showErrorPopupWindow(LamrimReaderActivity.this, findViewById(R.id.rootLayout), "您所選擇的檔案不存在或無法讀取。");
				return;
			}
			
			renderView.setImageURI(Uri.fromFile(file));
			editor.putString("renderImgFgPathKey", filePath);
			editor.commit();
			break;
		}

		Log.d(funcLeave, "Leave onActivityResult");
	}
	
	

	public boolean startPlay(final int mediaIndex) {
		File f = FileSysManager.getLocalMediaFile(mediaIndex);
		if (f == null || !f.exists()) {
			return false;
		}

		AsyncTask<Void, Void, Void> runner = new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					// Reset subtitle to SUBTITLE_MODE
					bookView.clearHighlightLine();
					setSubtitleViewMode(SUBTITLE_MODE);
					setSubtitleViewText(getString(R.string.dlgDescPrepareSpeech));
					mpController.setDataSource(getApplicationContext(),	mediaIndex);
				} catch (IllegalArgumentException e) {
					setSubtitleViewText(getString(R.string.errIAEwhileSetPlayerSrc));
					GaLogger.sendEvent("error", "player_error",	"IllegalArgumentException", null);
					e.printStackTrace();
				} catch (SecurityException e) {
					setSubtitleViewText(getString(R.string.errSEwhileSetPlayerSrc));
					GaLogger.sendEvent("error", "player_error",	"SecurityException", null);
					e.printStackTrace();
				} catch (IllegalStateException e) {
					setSubtitleViewText(getString(R.string.errISEwhileSetPlayerSrc));
					GaLogger.sendEvent("error", "player_error",	"IllegalStateException", null);
					e.printStackTrace();
				} catch (IOException e) {
					setSubtitleViewText(getString(R.string.errIOEwhileSetPlayerSrc));
					GaLogger.sendEvent("error", "player_error", "IOException",null);
					e.printStackTrace();
				}
				// }
				return null;
			}
		};
		runner.execute();

		return true;
	}

	private void setSubtitleViewMode(final int mode) {
		runOnUiThread(new Runnable() {
			public void run() {
				if(mode == SUBTITLE_MODE){
					subtitleViewRenderMode = SUBTITLE_MODE;
					subtitleView.setMovementMethod(null);
					subtitleView.setHeight(subtitleView.getLineHeight());
					subtitleView.setGravity(Gravity.CENTER);
				}
				else if(mode == READING_MODE){
					subtitleViewRenderMode = READING_MODE;
					subtitleView.setMovementMethod(ScrollingMovementMethod.getInstance());
					subtitleView.setScrollBarStyle(TextView.SCROLLBARS_INSIDE_OVERLAY);
					subtitleView.setGravity(Gravity.LEFT);
					setSubtitleViewText(readingModeAllSubtitle);
				}
			}
		});
	}
	
	public void setSubtitleViewText(final CharSequence s) {
		runOnUiThread(new Runnable() {
			public void run() {
				subtitleView.setText(s);
				int lineCount = subtitleView.getLineCount();// There will return
															// 0 sometimes.
				if (lineCount < 1)
					lineCount = 1;
				subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);
			}
		});
	}

	private void showSetTextSizeDialog() {
		LayoutInflater factory = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View v = factory.inflate(R.layout.set_text_size_dialog_view, null);
		final SeekBar theorySb = (SeekBar) v.findViewById(R.id.theorySizeBar);
		final SeekBar subtitleSb = (SeekBar) v.findViewById(R.id.subtitleSizeBar);
		final int orgTheorySize = runtime.getInt(getString(R.string.bookFontSizeKey),
				getResources().getInteger(R.integer.defFontSize)) - getResources().getInteger(R.integer.textMinSize);
		final int orgSubtitleSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), getResources().getInteger(R.integer.defFontSize))
				- getResources().getInteger(R.integer.textMinSize);
		final int textMaxSize = getResources().getInteger(R.integer.textMaxSize) - getResources().getInteger(R.integer.textMinSize);

		Log.d(logTag, "Set theory size Max=" + (textMaxSize) + ", orgSize="
				+ orgTheorySize + ", subtitle size Max=" + textMaxSize
				+ ", orgSize=" + orgSubtitleSize);
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				theorySb.setMax(textMaxSize);
				subtitleSb.setMax(textMaxSize);
				theorySb.setProgress(orgTheorySize);
				subtitleSb.setProgress(orgSubtitleSize);
			}
		});

		OnSeekBarChangeListener sbListener = new OnSeekBarChangeListener() {
			@Override
			public void onProgressChanged(final SeekBar seekBar, final int progress, boolean fromUser) {
				if (!fromUser)
					return;
				final int minSize = getResources().getInteger(R.integer.textMinSize);

				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						Log.d(logTag,
								"Seek bar get progress: "+ progress	+ ", min size: "
										+ getResources().getInteger(R.integer.textMinSize)
										+ ", add:"+ (progress + getResources().getInteger(R.integer.textMinSize)));
						if (seekBar.equals(theorySb)) {
							bookView.setTextSize(progress + minSize);
							bookView.refresh();
						}
						// theorySample.setTextSize;
						else{
							if(subtitleViewRenderMode == SUBTITLE_MODE){
								int lineCount = subtitleView.getLineCount();// There will return 0 sometimes.
								if (lineCount < 1)
									lineCount = 1;
								subtitleView.setHeight(subtitleView.getLineHeight() * lineCount);
							}
							subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, progress + minSize);
						}
						seekBar.setProgress(progress);
					}
				});
			}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				SharedPreferences.Editor editor = runtime.edit();
				editor.putInt(getString(R.string.bookFontSizeKey), (int) bookView.getTextSize());
				editor.commit();
				GaLogger.sendEvent("ui_action", "bookview_event", "change_text_size_end", null);
			}
		};
		theorySb.setOnSeekBarChangeListener(sbListener);
		subtitleSb.setOnSeekBarChangeListener(sbListener);

		// dialog.show();

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog setTextSizeDialog = builder.create();
		setTextSizeDialog.setView(v);
		WindowManager.LayoutParams lp = setTextSizeDialog.getWindow().getAttributes();
		lp.alpha = 0.7f;
		setTextSizeDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				SharedPreferences.Editor editor = runtime.edit();
				Log.d(logTag, "Write theory size: "	+ (int) theorySb.getProgress()
								+ ", subtitle size: " + subtitleSb.getProgress() + " to runtime.");
				editor.putInt(getString(R.string.bookFontSizeKey), theorySb.getProgress()
								+ getResources().getInteger(R.integer.textMinSize));
				editor.putInt(getString(R.string.subtitleFontSizeKey), subtitleSb.getProgress()
								+ getResources().getInteger(R.integer.textMinSize));
				editor.commit();

				Log.d(logTag,"Check size after write to db: theory size: "
								+ runtime.getInt(getString(R.string.bookFontSizeKey), 0) + ", subtitle size: "
								+ runtime.getInt(getString(R.string.subtitleFontSizeKey), 0));
				// updateTextSize();
				dialog.dismiss();
			}
		});
		setTextSizeDialog.setCanceledOnTouchOutside(true);
		setTextSizeDialog.show();
	}


	private void showSearchDialog(){
		LayoutInflater factory = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View searchView = factory.inflate(R.layout.search_view, null);
		final ImageButton searchBtn= (ImageButton) searchView.findViewById(R.id.searchBtn);
		final EditText searchInput=(EditText) searchView.findViewById(R.id.searchInput);
		searchBtn.setOnClickListener(new SearchListener(searchInput, searchBtn));
		
        
		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		AlertDialog searchDialog = builder.create();
		searchDialog.setView(searchView);
		searchDialog.setCanceledOnTouchOutside(true);
		WindowManager.LayoutParams lp=searchDialog.getWindow().getAttributes();
        lp.alpha=0.8f;
        searchDialog.getWindow().setAttributes(lp);
        searchDialog.setCanceledOnTouchOutside(true);
        searchDialog.show();
		searchDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				searchBtn.setEnabled(true);
			}});
	}
	
	class SearchListener implements View.OnClickListener{
		int index[]={-1,-1,-1};
		String lastSearch="";
		EditText searchInput=null;
		ImageButton searchBtn=null;
		
		public SearchListener(EditText searchInput, ImageButton searchBtn){
			this.searchInput=searchInput;
			this.searchBtn=searchBtn;
		}
		
		@Override
		public void onClick(View v) {
			searchBtn.setEnabled(false);
			if(searchInput.length() == 0){
				Log.d(getClass().getName(),"User input length = 0, skip search");
				searchBtn.setEnabled(true);
				return;
			}
			final String str=searchInput.getText().toString();
			if(!str.equals(lastSearch)){index[0]=0;index[1]=0;index[2]=0;}
			try{
				lastSearch=str;
				int result[] = bookView.search(index[0], index[1], index[2], str);

				if(result==null){
					Log.d(getClass().getName(),"Not found.");
					searchBtn.setEnabled(true);
					return;
				}
				else{
					index=result;
					bookView.setViewToPosition(index[0],index[1]);
					bookView.setHighlightWord(index[0], index[1], index[2],str.length());
					index[2]++;

					Log.d(getClass().getName(),"Change start word from "+index[2]);
				}
			}catch(Exception e){
				e.printStackTrace();
				GaLogger.sendException("Error happen while SEARCH "+str, e, true);
			}
			searchBtn.setEnabled(true);
		}
	};
/*	private void showSaveRegionDialog() {
		int regionStartMs = mpController.getRegionStartPosition();
		int regionEndMs = mpController.getRegionEndPosition();
		final SubtitleElement startSubtitle = mpController.getSubtitle(regionStartMs);
		final SubtitleElement endSubtitle = mpController.getSubtitle(regionEndMs - 1);
		String info = startSubtitle.text + " ~ " + endSubtitle.text;

		Log.d(logTag, "Check size of region list before: " + RegionRecord.records.size());
		Runnable callBack = new Runnable() {
			@Override
			public void run() {
				runOnUiThread(new Runnable() {
					@Override
					public void run() {
						regionFakeList.add(fakeSample);
						if (regionRecordAdapter != null)
							Log.d(logTag, "Warring: the regionRecordAdapter = null !!!");
						else
							regionRecordAdapter.notifyDataSetChanged();
						Log.d(logTag, "Check size of region list after: " + RegionRecord.records.size());
					}
				});
			}
		};

		BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this, mediaIndex,
				regionStartMs, regionEndMs, null, info, -1, callBack);
		GaLogger.sendEvent("ui_action", "show_dialog", "save_region", null);
	}*/

	private void showRecordListPopupMenu() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View popupView = inflater.inflate(R.layout.popup_record_list,	null);
		Rect rectgle = new Rect();
		Window window = getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		int StatusBarHeight = rectgle.top;
		int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT).getTop();
		int titleBarHeight = contentViewTop - StatusBarHeight;
		int screenHeight = getWindowManager().getDefaultDisplay().getHeight();
		int subtitleViewHeight = ((TextView) findViewById(R.id.subtitleView)).getHeight();
		// int listViewHeight=screenHeight-titleBarHeight-subtitleViewHeight;
		int listViewHeight = screenHeight - contentViewTop;

		Log.i(logTag, "StatusBar Height= " + StatusBarHeight
				+ " , TitleBar Height = " + titleBarHeight);
		final PopupWindow popupWindow = new PopupWindow(
		// findViewById(R.layout.popup_record_list),
				popupView,
				// LayoutParams.WRAP_CONTENT, listViewHeight);
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
//		popupWindow.setContentView(popupView);

		regionListView = (ListView) popupView.findViewById(R.id.recordListView);
		regionListView.setAdapter(regionRecordAdapter);
//		popupWindow.setWidth(popupView.getWidth());
		Log.d(getClass().getName(),"There are "+regionRecordAdapter.getCount()+" items in regionList view.");
		regionListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, final int position, long id) {
				Log.d(logTag, "Region record menu: item " + RegionRecord.records.get(position).title + " clicked.");
				RegionRecord rec=RegionRecord.records.get(position);
				String param="";
				int start=rec.mediaStart;
				int end = rec.mediaEnd;
				File media = FileSysManager.getLocalMediaFile(start);
				File subtitle = FileSysManager.getLocalSubtitleFile(start);
				if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){param=""+start;}
				if(end != start){
					media = FileSysManager.getLocalMediaFile(end);
					subtitle = FileSysManager.getLocalSubtitleFile(end);
					if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){
						if(param.length()==0)param=""+end;
						else param+=","+end;
					}
				}
				
				final String files=param;
				if(param.length()!=0){
					final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
					speechMenu.putExtra("index", files);
					if (wakeLock.isHeld())
						wakeLock.release();
					startActivityForResult(speechMenu, SPEECH_MENU_RESULT_REGION);
					popupWindow.dismiss();
				}
				startRegionPlay(rec.mediaStart, rec.startTimeMs ,rec.mediaEnd, rec.endTimeMs, rec.theoryPageStart, rec.theoryStartLine, rec.theoryPageEnd, rec.theoryEndLine);
				actionBarTitle=getString(R.string.menuStrPlayRegionRecShortName)+": "+rec.title;
				popupWindow.dismiss();
				GaLogger.sendEvent("dialog_action", "select_record", "play_saved_region", position);
				
			}
		});

		popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
			@Override
			public void onDismiss() {
				SharedPreferences.Editor editor = runtime.edit();
				String pageKey = getString(R.string.regionRecordListViewPage);
				String pageShiftKey = getString(R.string.regionRecordListViewPageShift);
				int pageCount = regionListView.getFirstVisiblePosition();
				View v = regionListView.getChildAt(0);
				int shift = (v == null) ? 0 : v.getTop();

				editor.putInt(pageKey, pageCount);
				editor.putInt(pageShiftKey, shift);
				editor.commit();
				GaLogger.sendEvent("dialog_action", "select_record", "cancel_select", null);
			}
		});
		popupWindow.setFocusable(true);
		popupWindow.setBackgroundDrawable(new BitmapDrawable());
		// popupWindow.setWidth((int)
		// (getWindowManager().getDefaultDisplay().getWidth()*0.4));

		// popupWindow.setContentView(findViewById(R.id.rootLayout));
		// AnimationUtils.loadAnimation(getApplicationContext(),R.anim.bounce);
		popupWindow.setAnimationStyle(R.style.AnimationPopup);
		popupWindow.update();
		popupWindow.showAtLocation(findViewById(R.id.rootLayout), Gravity.LEFT| Gravity.TOP, 0, contentViewTop);
		GaLogger.sendEvent("ui_action", "show_dialog", "record_list_popup_menu_count", regionRecordAdapter.getCount());
		// popupWindow.showAsDropDown(findViewById(R.id.subtitleView),0, 0);
		// popupWindow.showAsDropDown(findViewById(R.id.subtitleView));
	}

	private void startRegionPlay(final int mediaStart,final int startTimeMs,final int mediaEnd,final int endTimeMs,final int theoryStartPage,final int theoryStartLine,final int theoryEndPage,final int theoryEndLine){
		
		setRegionSec( mediaStart, startTimeMs, mediaEnd, endTimeMs, theoryStartPage, theoryStartLine, theoryEndPage, theoryEndLine);
		
		String param="";
		int start=mediaStart;
		int end = mediaEnd;
		File media = FileSysManager.getLocalMediaFile(start);
		File subtitle = FileSysManager.getLocalSubtitleFile(start);
		if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){param=""+start;}
		if(end != start){
			media = FileSysManager.getLocalMediaFile(end);
			subtitle = FileSysManager.getLocalSubtitleFile(end);
			if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){
				if(param.length()==0)param=""+end;
				else param+=","+end;
			}
		}
		
		Log.d(getClass().getName(),"Send download param "+param+" to speechMenuActivity");
		final String files=param;
		if(param.length()!=0){
			final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
			speechMenu.putExtra("index", files);
			if (wakeLock.isHeld())
				wakeLock.release();
			startActivityForResult(speechMenu, SPEECH_MENU_RESULT_REGION);
			
			// The procedure will not return to onStart or onResume, start
			// play media from here.

			// Check file exist again, if no download, return.
			boolean isDownloaded=true;
			media = FileSysManager.getLocalMediaFile(start);
			subtitle = FileSysManager.getLocalSubtitleFile(start);
			if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){isDownloaded=false;}
			if(end != start){
				media = FileSysManager.getLocalMediaFile(end);
				subtitle = FileSysManager.getLocalSubtitleFile(end);
				if (media == null || subtitle == null || !media.exists() || !subtitle.exists()){isDownloaded=false;}
			}
			if(!isDownloaded)return;
//			return;
		}

		//mpController.desetPlayRegion();
		mpController.reset();
		SharedPreferences.Editor editor = runtime.edit();
		editor.putInt("mediaIndex", mediaStart);
		editor.putInt("playPosition", startTimeMs);
		editor.commit();
		mediaIndex = mediaStart;
		GLamrimSectIndex=0;
		Log.d(getClass().getName(),"Mark theory: start page="+theoryHighlightRegion[0]+" start line="+theoryHighlightRegion[1]+", offset="+bookViewMountPoint[1]);

		startPlay(mediaIndex);
	}
	private void setRegionSec(String speechPositionStart, String speechPositionEnd, String theoryLineStart, String theoryLineEnd){
		final int[] theoryStart=GlRecord.getTheoryStrToInt(glRecord.theoryLineStart);// {page,line}
		int[] theoryEnd=GlRecord.getTheoryStrToInt(glRecord.theoryLineEnd);// {page,line}
		int[] speechStart=GlRecord.getSpeechStrToInt(glRecord.speechPositionStart);// {speechIndex, TimeMs}
		int[] speechEnd=GlRecord.getSpeechStrToInt(glRecord.speechPositionEnd);// {speechIndex, TimeMs}
		
		Log.d(getClass().getName(),"Parse result: Theory: P"+theoryStart[0]+"L"+ theoryStart[1]+" ~ P"+theoryEnd[0]+"L"+theoryEnd[1]);
		Log.d(getClass().getName(),"Parse result: Speech: "+speechStart[0]+":"+ Util.getMsToHMS(speechStart[1])+" ~ "+speechEnd[0]+":"+ Util.getMsToHMS(speechEnd[1]));

		setRegionSec(speechStart[0], speechStart[1], speechEnd[0], speechEnd[1], theoryStart[0],  theoryStart[1], theoryEnd[0], theoryEnd[1]);
	}
	
	private void setRegionSec(int speechStartIndex, int speechStartMs, int speechEndIndex, int speechEndMs, final int theoryStartPage, final int theoryStartLine, int theoryEndPage, int thtoryEndLine){
		Log.d(getClass().getName(),"Set region[0]: startIndex="+speechStartIndex+", startMs="+ speechStartMs+", speechEndIndex="+speechEndIndex+", endMs="+speechEndMs);
		if(speechStartIndex == speechEndIndex){
			Log.d(getClass().getName(),"Set region[0]: startIndex="+speechStartIndex+", startMs="+ speechStartMs+", endMs="+speechEndMs+"; region[1]: -1, -1, -1");
			GLamrimSect[0][0]=speechStartIndex;
			GLamrimSect[0][1]=speechStartMs;
			GLamrimSect[0][2]=speechEndMs;
			GLamrimSect[1][0]=-1;
			GLamrimSect[1][0]=-1;
			GLamrimSect[1][0]=-1;
		}
		else{// difference media.
			Log.d(getClass().getName(),"Set region[0]: startIndex="+speechStartIndex+", startMs="+ speechStartMs+", endMs=-1; region[1]: endIndex="+speechEndIndex+", startMs=0, endMs="+speechEndMs);
			GLamrimSect[0][0]=speechStartIndex;
			GLamrimSect[0][1]=speechStartMs;
			GLamrimSect[0][2]=-1;
			GLamrimSect[1][0]=speechEndIndex;
			GLamrimSect[1][1]=0;
			GLamrimSect[1][2]=speechEndMs;
		}
		// Copy section data to regionSet
		regionSet[0]=speechStartIndex;
		regionSet[1]=speechStartMs;
		regionSet[2]=speechEndIndex;
		regionSet[3]=speechEndMs;
		
		// Set theory high light
		theoryHighlightRegion[0]=theoryStartPage;
		theoryHighlightRegion[1]=theoryStartLine;
		theoryHighlightRegion[2]=theoryEndPage;
		theoryHighlightRegion[3]=thtoryEndLine;

		// Set theory mount point.
		bookViewMountPoint[0]=theoryStartPage;
		bookViewMountPoint[1]=(int) bookView.setViewToPosition(theoryStartPage, theoryStartLine);
	}

	
	
	// ======================= For Render mode ======================================
	private void showRenderModeFirstLevelMenu(){
		String[] menuStr = {"離開公播模式","一般選單","設定選單"};
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("功能選單").setItems(menuStr, new DialogInterface.OnClickListener(){

                @Override
                public void onClick(DialogInterface dialog, int which) {
                        if(which == 0)switchMainView();
                        else if(which == 1)showRenderModeNormalMenu();
                        else shwoRenderModeOptMenu();
                }});
		/*builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		*/
		builderSingle.show();
	}
	
	private void showRenderModeNormalMenu(){
		String[] menuStr={getString(R.string.menuStrSelectSpeech), getString(R.string.globalLamrim),getString(R.string.menuStrPlayRegionRec),"離開公播模式", getString(R.string.menuStrOpenProjectWeb), getString(R.string.exitApp)};
		
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("一般選單").setItems(menuStr, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0)startSpeechMenuActivity();
				else if(which == 1)startGlobalLamrimCalendarActivity();
				else if(which == 2){
					if (RegionRecord.records.size() == 0) {
						Util.showInfoPopupWindow(LamrimReaderActivity.this, findViewById(R.id.rootLayout), "沒有區段記錄，請先記錄區段。");
						return;
					}
					showRecordListPopupMenu();
				}
				else if(which == 3)switchMainView();
				else if(which == 4)startProjectWebUrl();
				else if(which == 5)onBackPressed();
				else Log.d(getClass().getName(),"There is a non exist menu option been selected.");
			}});
		/*builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		*/
		
		AlertDialog dialog=builderSingle.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}
	
	private void shwoRenderModeOptMenu(){
		String[] menuStr={"顯示/隱藏模式切換鍵","選擇圖片", "圖片擴展方式", "圖片背景顏色", "字幕顏色", "字幕背景顏色", "字幕背景透明度","回到原始設定"};
		AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
		builderSingle.setIcon(R.drawable.ic_launcher);
		builderSingle.setTitle("功能選單").setItems(menuStr, new DialogInterface.OnClickListener(){

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(which == 0)showHideSubtitleSwBtn();
				else if(which == 1)selectRenderImage();
				else if(which == 2)showScaleTypeDialog();
				else if(which == 3)showRenderImageBgColorDlg();
				else if(which == 4)showSubFgColorDlg();
				else if(which == 5)showSubBgColorDlg();
				else if(which == 6)showSubBgAlphaDlg();
				else if(which == 7)setRenderModeOptsToDefault();
			}});
/*		builderSingle.setNegativeButton("cancel", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});*/
		AlertDialog dialog=builderSingle.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
	}
	
	private void setRenderModeOptsToDefault(){
		renderView.setImageResource(R.drawable.master);
		renderView.setScaleType(scaleType[0]);
		renderView.setBackgroundColor(0);
		subtitleView.setTextColor(getResources().getColor(R.color.defSubtitleFGcolor));
		subtitleView.setBackgroundColor(getResources().getColor(R.color.defSubtitleBGcolor));
		modeSwBtn.setVisibility(View.VISIBLE);
		
		SharedPreferences.Editor editor = runtime.edit();
		editor.remove(getString(R.string.isShowModeSwBtnKey));
		editor.remove(getString(R.string.renderImgFgPathKey));
		editor.remove(getString(R.string.renderImgScaleKey));
		editor.remove(getString(R.string.renderImgBgColorKey));
		editor.remove(getString(R.string.subtitleFgColorKey));
		editor.remove(getString(R.string.subtitleBgColorKey));
		editor.remove(getString(R.string.subtitleAlphaKey));
		editor.commit();
	}
	
	final String scaleStr[]={"等比擴展填滿置中", "等比擴展全圖顯示置中", "不按比例完全擴展", "等比縮放置上", "等比縮放置中", "等比縮放置底", "不縮放置中",  "向量"};
    private void showScaleTypeDialog(){
            AlertDialog.Builder builderSingle = new AlertDialog.Builder(LamrimReaderActivity.this);
        builderSingle.setIcon(R.drawable.ic_launcher);
        builderSingle.setTitle("選擇擴展方式")
        .setItems(scaleStr, new DialogInterface.OnClickListener(){

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                            // TODO Auto-generated method stub
                            renderView.setScaleType(scaleType[which]);
                            SharedPreferences.Editor editor = runtime.edit();
                            Log.d(logTag,"Set image scale type: "+scaleStr[which]);
                            editor.putInt(getString(R.string.renderImgScaleKey), which);
                            editor.commit();
                    }});
/*        builderSingle.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
*/       
        AlertDialog dialog=builderSingle.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
    }

	private void switchMainView(){
		if(renderView.getVisibility()==View.VISIBLE){
			renderView.setVisibility(View.GONE);
			bookView.setVisibility(View.VISIBLE);
			subtitleView.setTextColor(getResources().getColor(R.color.defSubtitleFGcolor));
			subtitleView.setBackgroundColor(getResources().getColor(R.color.defSubtitleBGcolor));
			modeSwBtn.setVisibility(View.VISIBLE);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);  
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
			getSupportActionBar().show();
	    }
		else{
			showRenderModeWarring();
			renderView.setVisibility(View.VISIBLE);
			bookView.setVisibility(View.GONE);
			int alpha=runtime.getInt(getString(R.string.subtitleAlphaKey), 255) << 24 & 0xFF000000;
			int color=runtime.getInt(getString(R.string.subtitleBgColorKey), getResources().getColor(R.color.defSubtitleBGcolor)) & 0x00FFFFFF;
			Log.d(getClass().getName(),"Load alpha of subitlte: "+alpha);
			int bgColor = alpha | color;
			subtitleView.setTextColor(runtime.getInt(getString(R.string.subtitleFgColorKey), getResources().getColor(R.color.defSubtitleFGcolor)));
			subtitleView.setBackgroundColor(bgColor);
			boolean isShowModeSwBtn=runtime.getBoolean(getString(R.string.isShowModeSwBtnKey), true);
			if(isShowModeSwBtn)modeSwBtn.setVisibility(View.VISIBLE);
			else modeSwBtn.setVisibility(View.GONE);
			getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  
			getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			getSupportActionBar().hide();
		}
	}

	// ================ For option menu =====================
	
	private void showRenderModeWarring(){
		AlertDialog.Builder builderSingle =new AlertDialog.Builder(this).setTitle("警告").setIcon(android.R.drawable.ic_dialog_alert)
				.setMessage("此模式畫面上沒有選單按鍵！\n必須透過\"按住圖片\"才能叫出選單。\n重開廣論App後會回到正常模式。");
		builderSingle.setPositiveButton("我發誓我有讀懂!", null);
		builderSingle.setCancelable(false);
		builderSingle.show();
	}
	
	private void showHideSubtitleSwBtn(){
		SharedPreferences.Editor editor = runtime.edit();
		if(modeSwBtn.getVisibility() == View.VISIBLE){
			modeSwBtn.setVisibility(View.GONE);
            editor.putBoolean(getString(R.string.isShowModeSwBtnKey), false);
            editor.commit();
		}
		else {
			modeSwBtn.setVisibility(View.VISIBLE);
			editor.putBoolean(getString(R.string.isShowModeSwBtnKey), true);
			editor.commit();
		}
	}
		private void selectRenderImage(){
		Intent fgIntent = new Intent(getBaseContext(), FileDialogActivity.class);
		fgIntent.putExtra(FileDialogActivity.TITLE, "請選擇圖片檔案");
		fgIntent.putExtra(FileDialogActivity.START_PATH, "/sdcard");
		//can user select directories or not
		fgIntent.putExtra(FileDialogActivity.CAN_SELECT_DIR, false);
		fgIntent.putExtra(FileDialogActivity.SELECTION_MODE, FileDialogActivity.MODE_OPEN);
		
		//alternatively you can set file filter
		fgIntent.putExtra(FileDialogActivity.FORMAT_FILTER, new String[] { "jpg", "gif", "png", "bmp", "webp" });
		startActivityForResult(fgIntent, SELECT_FG_PIC_RESULT);
    }

	private void showRenderImageBgColorDlg(){
		int defColor=runtime.getInt(getString(R.string.renderImgBgColorKey), R.color.defSubtitleBGcolor);

		final ColorPickerDialog colorDialog=new ColorPickerDialog(LamrimReaderActivity.this, defColor,new ColorPickerView.OnColorChangedListener() {
			@Override
			public void onColorChanged(int color) {
				renderView.setBackgroundColor(color | 0xFF000000);
			}
		});
		 colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					int color=colorDialog.getColor() | 0xFF000000;
					SharedPreferences.Editor editor = runtime.edit();
	                editor.putInt(getString(R.string.renderImgBgColorKey), color);
	                editor.commit();
				}});
        
		WindowManager.LayoutParams lp=colorDialog.getWindow().getAttributes();
        lp.alpha=0.8f;
        colorDialog.getWindow().setAttributes(lp);
        colorDialog.setCanceledOnTouchOutside(true);
        colorDialog.show();
    }
	
	private void showSubFgColorDlg(){
            int defFgColor=runtime.getInt(getString(R.string.subtitleFgColorKey), R.color.defSubtitleFGcolor);
            
            final ColorPickerDialog colorDialog=new ColorPickerDialog(LamrimReaderActivity.this, defFgColor,new ColorPickerView.OnColorChangedListener() {
				@Override
				public void onColorChanged(int color) {
					subtitleView.setTextColor(color|0xFF000000);
				}
			});
            
            colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					int color=subtitleView.getCurrentTextColor();
					SharedPreferences.Editor editor = runtime.edit();
                    editor.putInt(getString(R.string.subtitleFgColorKey), color);
                    editor.commit();
				}});

    		WindowManager.LayoutParams lp=colorDialog.getWindow().getAttributes();
            lp.alpha=0.8f;
            colorDialog.getWindow().setAttributes(lp);
            colorDialog.setCanceledOnTouchOutside(true);
            colorDialog.show();
    }
	
	private void showSubBgColorDlg(){
            int defBgColor=runtime.getInt(getString(R.string.subtitleBgColorKey), R.color.defSubtitleBGcolor);
            
            final ColorPickerDialog colorDialog=new ColorPickerDialog(LamrimReaderActivity.this, defBgColor,new ColorPickerView.OnColorChangedListener() {
				@Override
				public void onColorChanged(int color) {
					int alpha=runtime.getInt(getString(R.string.subtitleAlphaKey),255);
					Log.d(getClass().getName(),"Get alpha: "+alpha);
					int c=Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
					subtitleView.setBackgroundColor(c);
				}
			});
            colorDialog.setOnCancelListener(new DialogInterface.OnCancelListener(){
				@Override
				public void onCancel(DialogInterface dialog) {
					int color=colorDialog.getColor();
					SharedPreferences.Editor editor = runtime.edit();
                    editor.putInt(getString(R.string.subtitleBgColorKey), color);
                    editor.commit();
				}});
            
    		WindowManager.LayoutParams lp=colorDialog.getWindow().getAttributes();
            lp.alpha=0.8f;
            colorDialog.getWindow().setAttributes(lp);
            colorDialog.setCanceledOnTouchOutside(true);
            colorDialog.show();
    }
	
	private void showSubBgAlphaDlg(){
		final SeekBar seekBar=new SeekBar(this);
		seekBar.setMax(255);
		int alpha=runtime.getInt(getString(R.string.subtitleAlphaKey),255);
		seekBar.setProgress(alpha);
		seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener (){
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				int color=runtime.getInt(getString(R.string.subtitleBgColorKey),255);
				subtitleView.setBackgroundColor(Color.argb(progress, Color.red(color), Color.green(color), Color.blue(color)));
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {}
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {}});

		AlertDialog.Builder builderSingle =new AlertDialog.Builder(this).setTitle("請輸選擇透明度").setIcon(android.R.drawable.ic_dialog_info).setView(seekBar);
		builderSingle.setOnCancelListener(new DialogInterface.OnCancelListener(){
			@Override
			public void onCancel(DialogInterface dialog) {
				int alpha=seekBar.getProgress();
				Log.d(getClass().getName(),"Save alpha of subitlte to "+alpha);
				SharedPreferences.Editor editor = runtime.edit();
                editor.putInt(getString(R.string.subtitleAlphaKey), alpha);
                editor.commit();
			}});
		AlertDialog dialog=builderSingle.create();
		dialog.setCanceledOnTouchOutside(true);
		dialog.show();
		
		
		/*.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						try{
							//int alpha=Integer.parseInt(input.getText().toString());
							int alphaValue=seekBar.getProgress();
							subtitleView.getBackground().setAlpha(alphaValue);

							
						}catch(Exception e){}
					}})
					.setNegativeButton("取消", null).show();*/
    }
/*	
	private void hideTitle(){
		if(!getSupportActionBar().isShowing())return;
		double inch=Util.getDisplaySizeInInch(LamrimReaderActivity.this);
		Log.d(getClass().getName(),"The screen is "+inch+" inch.");
		// Do not hide title bar over 6 inch screen.
		if(inch > 6)return;
		 
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Log.d(getClass().getName(),"Hide action bar");
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  
			    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			    getSupportActionBar().hide();
			}});
	}

	private void showTitle(){
		Log.d(getClass().getName(),"is action bar showing = "+getSupportActionBar().isShowing());
		Log.d(getClass().getName(),"renderView visiable = "+((renderView.getVisibility()==View.VISIBLE)?"true":"false"));
		if(getSupportActionBar().isShowing() || renderView.getVisibility()==View.VISIBLE)return;
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				Log.d(getClass().getName(),"========================= Show action bar =======================");
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);  
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getSupportActionBar().show();
			}});
	}
*/
	private void showMediaController(){
		mpController.showControllerView(LamrimReaderActivity.this);
/*		if(regionSet[0] != -1 && regionSet[2] != -1)
			((ImageButton)mpController.getControllerView().findViewById(R.id.shareBtn)).setEnabled(true);
		else ((ImageButton)mpController.getControllerView().findViewById(R.id.shareBtn)).setEnabled(false);
		*/
	}
	private void highlightView(View v){
		Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.blank);
		v.startAnimation(animation);
	}
	class RegionRecordAdapter extends SimpleAdapter {
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

			final RegionRecord record = RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
			Log.d(getClass().getName(), "Set: " + record.title);
			TextView title = (TextView) row.findViewById(R.id.regionRowTitle);
			TextView timeReg = (TextView) row.findViewById(R.id.timeRegion);
			TextView theoryIndex = (TextView) row.findViewById(R.id.theoryIndex);
			TextView info = (TextView) row.findViewById(R.id.info);
			
			ImageButton shareButton = (ImageButton) row.findViewById(R.id.shareButton);
			ImageButton editButton = (ImageButton) row.findViewById(R.id.editButton);
			ImageButton delButton = (ImageButton) row.findViewById(R.id.deleteButton);

			title.setText(record.title);
			
			if(record.theoryPageStart!=-1 && record.theoryStartLine != -1 && record.theoryPageEnd != -1 && record.theoryEndLine != -1)
				theoryIndex.setText(String.format(getString(R.string.dlgRecordTheoryIndex), (record.theoryPageStart+1), (record.theoryStartLine+1), (record.theoryPageEnd+1), (record.theoryEndLine+1)));
			
			timeReg.setText(SpeechData.getTheoryName(record.mediaStart) + "  "
					+ Util.getMsToHMS(record.startTimeMs, "\"", "'", false)+ " ~ "
					+ SpeechData.getTheoryName(record.mediaEnd)+ "  "+ Util.getMsToHMS(record.endTimeMs, "\"", "'", false));
			info.setText(record.info);
			Log.d(logTag, "Info: " + record.info);
			
			shareButton.setFocusable(false);
			editButton.setFocusable(false);
			delButton.setFocusable(false);
			shareButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					shareSegment(RegionRecord.getRegionRecord(LamrimReaderActivity.this, position));
				}});
			
			editButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					RegionRecord rr = RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
					Runnable callBack = new Runnable() {
						@Override
						public void run() {
							runOnUiThread(new Runnable() {
								@Override
								public void run() {
									regionRecordAdapter.notifyDataSetChanged();
								}
							});
						}
					};
					BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this,	rr.mediaStart, rr.startTimeMs, rr.mediaEnd, rr.endTimeMs, rr.theoryPageStart, rr.theoryStartLine,rr.theoryPageEnd, rr.theoryEndLine, record.info, position, callBack);
				}
			});

			delButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					BaseDialogs.showDelWarnDialog(LamrimReaderActivity.this, "記錄", null, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							RegionRecord.removeRecord(LamrimReaderActivity.this, position);
							regionFakeList.remove(position);
							regionRecordAdapter.notifyDataSetChanged();
						}
					}, null, null);
				}
			});
			return row;
		}
	};
	
	private void setMediaControllerView(int regStartMs, int regEndMs, boolean prevBtnVisiable, boolean nextBtnVisiable, OnClickListener prevListener, OnClickListener nextListener){
		mpController.setPlayRegion(regStartMs, regEndMs);
		mpController.setPrevNextListeners(prevListener, nextListener);
		((View)mpController.getControllerView().findViewById(R.id.prev)).setVisibility(((prevBtnVisiable)?View.VISIBLE:View.GONE));
		((View)mpController.getControllerView().findViewById(R.id.next)).setVisibility(((nextBtnVisiable)?View.VISIBLE:View.GONE));
	}
	
	public interface PrevNextListener{
		public OnClickListener getPrevPageListener();
		public OnClickListener getNextPageListener();
	}
	
	PrevNextListener glModePrevNextListener = new PrevNextListener(){
		OnClickListener nextListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"Next button click on Global Lamrim mode.");
				GLamrimSectIndex=1;
				startLamrimSection();
		}};

		OnClickListener prevListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"Prev button click on Global Lamrim mode.");
				GLamrimSectIndex=0;
				startLamrimSection();
		}};
		
		private void startLamrimSection(){
			if(GLamrimSect[GLamrimSectIndex][0] == -1) return;
			mpController.hideMediaPlayerController();
			Log.d(getClass().getName(),"Switch to first section of Global Lamrim.");
			mpController.reset();
			startPlay(GLamrimSect[GLamrimSectIndex][0]);
		}
		
		@Override
		public OnClickListener getPrevPageListener() {
			return prevListener;
		}

		@Override
		public OnClickListener getNextPageListener() {
			return nextListener;
		}
	};
	
	
	PrevNextListener normalModePrevNextListener = new PrevNextListener(){
		
		OnClickListener prevListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"Prev button click on Normal mode.");
				if(mediaIndex - 1 < 0)return;

				startLamrimSection(--mediaIndex);
		}};
		
		OnClickListener nextListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d(getClass().getName(),"Next button click on Normal mode.");
				if(mediaIndex + 1 >= SpeechData.name.length)return;
				
				startLamrimSection(++mediaIndex);
		}};

		private void startLamrimSection(int index){
			Log.d(getClass().getName(),"Switch to speech "+SpeechData.getTheoryName(index));
			File media = FileSysManager.getLocalMediaFile(index);
			File subtitle = FileSysManager.getLocalSubtitleFile(index);
			
			// File not exist.
			if (media == null || subtitle == null || !media.exists() || !subtitle.exists()) {
				final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
				speechMenu.putExtra("index", ""+index);
				if (wakeLock.isHeld())
					wakeLock.release();
				startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
				return;
			}
			
			// File exist, play it.
			SharedPreferences.Editor editor = runtime.edit();				
			editor.putInt("mediaIndex", index);
			editor.putInt("playPosition", 0);
			editor.commit();
			GLamrimSectIndex = -1;
			
			final int pageNum = SpeechData.refPage[index] - 1;
			if (pageNum < 0){
				bookViewMountPoint[0]=pageNum;
				bookViewMountPoint[1]=0;
			}

			Log.d(logTag, "Call reset player.");
			actionBarTitle=SpeechData.getNameId(index);
			getSupportActionBar().setTitle(actionBarTitle);
			mpController.reset();
			startPlay(index);
		}
		
		@Override
		public OnClickListener getPrevPageListener() {
			return prevListener;
		}

		@Override
		public OnClickListener getNextPageListener() {
			return nextListener;
		}
	};
	
	
}
