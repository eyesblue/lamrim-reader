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
	final static int GLOBAL_LAMRIM_RESULT = 2;
	final static int SUBTITLE_MODE = 1;
	final static int READING_MODE = 2;

	int subtitleViewRenderMode = SUBTITLE_MODE;
	static int mediaIndex = -1;
	MediaPlayerController mpController;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	MyListView bookView = null;
	TextView subtitleView = null;
	SharedPreferences runtime = null;

	MenuItem rootMenuItem, speechMenu, globalLamrim, setTextSize,
			playRegionRec, prjWeb, exitApp;

	FileSysManager fileSysManager = null;
	// FileDownloader fileDownloader = null;
	RelativeLayout rootLayout = null;

	Typeface educFont = null;
	View toastLayout = null;
	TextView toastTextView = null;
	ImageView toastSubtitleIcon;
	ImageView toastInfoIcon;
	int regionPlayIndex = -1;
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

	int[][] readingModeSEindex = null;
	String readingModeAllSubtitle = null;
	static Point screenDim = new Point();
	Button modeSwBtn = null;
	GlRecord glRecord=null;
	int[] bookViewMountPoint={0,0};
	
	int GLamrimHighlightRegion[]=new int[4];//{startPage, startLine, endPage, endLine}
	int[][] GLamrimSect=new int[2][3];
	int GLamrimSectIndex=-1;
	String GLamrimSelectedDay="";
	
	PrevNextListener prevNextListener = null;
	GLamrimModePrevNextListener glModePrevNextListener = new GLamrimModePrevNextListener();
	NormalModePrevNextListener normalModePrevNextListener = new NormalModePrevNextListener();
//	boolean repeatPlay=false;
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
///		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.main);

		getSupportActionBar();
		
		// setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);

		Log.d(funcInto, "******* Into LamrimReader.onCreate *******");
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
			boolean pressed = false;

			@Override
			public boolean onTouch(View v, MotionEvent event) {

				// Log.d(logTag, "Action Code= "+event.getAction());
				if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
					Log.d(logTag, "Leave event received");
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
						modeSwBtn.setBackground(getResources().getDrawable(R.drawable.mode_sw_button));
					else
						modeSwBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mode_sw_button));
					pressed = false;
					return true;
				}

				// Log.d(logTag, "Into onScroll, y="+event.getRawY());
				if (!pressed) {
					Log.d(logTag, "Set pressed color");
					if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
						modeSwBtn.setBackground(getResources().getDrawable(R.drawable.mode_sw_press_button));
					else
						modeSwBtn.setBackgroundDrawable(getResources().getDrawable(R.drawable.mode_sw_press_button));
					pressed = true;
				}

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
							Util.showNarmalToastMsg(LamrimReaderActivity.this, getString(R.string.dlgHintLoadMediaBeforeSwitchToReadingMode));
							return true;
						}
						
						setSubtitleViewMode(READING_MODE);
					}
				}
				// }

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
							bookView.setSelectionFromTop(
									bookView.getFirstVisiblePosition(), 0);
						}
					}, 200);
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
					public void onProgressChanged(SeekBar arg0, int arg1,
							boolean arg2) {
						audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
								arg1, 0);
						GaLogger.sendEvent("ui_action", "SeekBar_scored",
								"volume_control_arg1", null);
					}
				});
		textSize=(ImageButton) actionBarControlPanel.findViewById(R.id.textSize);
		textSize.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				showSetTextSizeDialog();
			}});
		
		fakeSample.put(null, null);
		RegionRecord.init(this);
		regionFakeList = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < RegionRecord.records.size(); ++i)
			regionFakeList.add(fakeSample);

		regionRecordAdapter = new RegionRecordAdapter(this, regionFakeList,
				android.R.layout.simple_list_item_2, new String[] { "title",
						"desc" }, new int[] { android.R.id.text1,
						android.R.id.text2 });

		if (mpController != null)
			Log.d(logTag,
					"The media player controller is not null in onCreate!!!!!");
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
										Util.showSubtitleToast(LamrimReaderActivity.this,
												subtitle.text+ " - ("
														+ Util.getMsToHMS(subtitle.startTimeMs, "\"", "'", false)
														+ " - "	+ Util.getMsToHMS(subtitle.endTimeMs, "\"", "'", false) + ')');
										break;
									case READING_MODE:
										SpannableString str = new SpannableString(readingModeAllSubtitle);
										try {
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
							if(GLamrimSectIndex!=-1){
								Log.d(logTag, "GlobalLamrim mode: play index "+GLamrimSect[GLamrimSectIndex][0]+", Sec: "+GLamrimSect[GLamrimSectIndex][1]+":"+GLamrimSect[GLamrimSectIndex][2]);
								if(actionBar != null)actionBar.setTitle(getString(R.string.globalLamrimShortName)+": "+GLamrimSelectedDay);
								bookView.setHighlightLine(GLamrimHighlightRegion[0], GLamrimHighlightRegion[1], GLamrimHighlightRegion[2], GLamrimHighlightRegion[3]);
								int regionStart=GLamrimSect[GLamrimSectIndex][1];
								int regionEnd=GLamrimSect[GLamrimSectIndex][2];
								if(regionEnd==-1)regionEnd=mpController.getDuration()-1000;
								mpController.seekTo(GLamrimSect[GLamrimSectIndex][1]);
								if(GLamrimSect[1][0]==-1){
									setMediaControllerView(regionStart, regionEnd, false, false, null, false, false, null);
								}
								else if(GLamrimSectIndex == 0){
									setMediaControllerView(regionStart, regionEnd, false, false, glModePrevNextListener.getPrevListener(), true, true, glModePrevNextListener.getNextListener());
								}else{
									setMediaControllerView(regionStart, regionEnd, true, true, glModePrevNextListener.getPrevListener(), false, false, glModePrevNextListener.getNextListener());
								}
								mpController.start();
								mpController.showControllerView(LamrimReaderActivity.this);
							}
							else if (regionPlayIndex != -1) {
								Log.d(logTag, "Region Mode: This play event is region play, set play region.");
								if(actionBar != null)actionBar.setTitle(getString(R.string.menuStrPlayRegionRecShortName)+": "+RegionRecord.records.get(regionPlayIndex).title);
								setMediaControllerView(RegionRecord.records.get(regionPlayIndex).startTimeMs,RegionRecord.records.get(regionPlayIndex).endTimeMs, true, true, normalModePrevNextListener.getPrevListener(), true, true, normalModePrevNextListener.getNextListener());
								mpController.seekTo(RegionRecord.records.get(regionPlayIndex).startTimeMs);
								mpController.start();
								regionPlayIndex = -1;
							} else {
								Log.d(logTag, "Normal mode: The play event is fire by user select a new speech.");
								if(actionBar != null)actionBar.setTitle(SpeechData.getNameId(mediaIndex));
								setMediaControllerView(-1, -1, false, true, normalModePrevNextListener.getPrevListener(), false, true, normalModePrevNextListener.getNextListener());
								int seekPosition = runtime.getInt("playPosition", 0);
								Log.d(logTag, "Seek to last play positon " + seekPosition);
								mpController.seekTo(seekPosition);
								mpController.showControllerView(LamrimReaderActivity.this);
							}
							mpController.setControllerViewClickable(true);
						}

						@Override
						public void onSaveRegion() {
							showSaveRegionDialog();
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
///							showTitle();
						}
						@Override
						public void onComplatePlay() {
							Log.d(getClass().getName(),"Show Title bar.");
///							showTitle();
						}
					});

		/*
		 * LayoutInflater inflater = getLayoutInflater(); toastLayout =
		 * inflater.inflate(R.layout.toast_text_view, (ViewGroup)
		 * findViewById(R.id.toastLayout)); toastTextView = (TextView)
		 * toastLayout.findViewById(R.id.text);
		 * toastTextView.setTypeface(educFont);
		 * toast = new Toast(getApplicationContext());
		 *
		 * toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
		 * toast.setDuration(Toast.LENGTH_LONG); toast.setView(layout);
		 * toast.show();
		 */
		/*
		 * toastTextView = new TextView(LamrimReaderActivity.this);
		 * toastTextView.setTypeface(educFont); toast = Toast.makeText(this, "",
		 * Toast.LENGTH_SHORT);
		 */

		subtitleView = (TextView) findViewById(R.id.subtitleView);
		subtitleView.setTypeface(educFont);
		subtitleView.setBackgroundColor(getResources().getColor(R.color.subtitleBGcolor));

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
						if (mpController.getMediaPlayerState() == MediaPlayerController.MP_PLAYING
								&& mpController.getSubtitle() != null) {
							int index = mpController
									.getSubtitleIndex(mpController
											.getCurrentPosition());
							if (index == -1)
								return true;
							// subtitleView.bringPointIntoView(readingModeSEindex[index][0]);
							try {
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

		final ScaleGestureDetector stScaleGestureDetector = new ScaleGestureDetector(
				this.getApplicationContext(),
				new SimpleOnScaleGestureListener() {
					@Override
					public boolean onScaleBegin(ScaleGestureDetector detector) {
						Log.d(getClass().getName(),
								"Begin scale called factor: "
										+ detector.getScaleFactor());
						GaLogger.sendEvent("ui_action", "subtitle_event",
								"scale_start", null);
						return true;
					}

					@Override
					public boolean onScale(ScaleGestureDetector detector) {
						float size = subtitleView.getTextSize()
								* detector.getScaleFactor();
						// Log.d(getClass().getName(),"Get scale rate: "+detector.getScaleFactor()+", current Size: "+adapter.getTextSize()+", setSize: "+adapter.getTextSize()*detector.getScaleFactor());
						subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
								size);
						// Log.d(getClass().getName(),"Realy size after setting: "+adapter.getTextSize());
						if (subtitleViewRenderMode == SUBTITLE_MODE)
							subtitleView.setHeight(subtitleView.getLineHeight());

						return true;
					}

					@Override
					public void onScaleEnd(ScaleGestureDetector detector) {
						SharedPreferences.Editor editor = runtime.edit();
						editor.putInt(getString(R.string.subtitleFontSizeKey),
								(int) subtitleView.getTextSize());
						editor.commit();
						GaLogger.sendEvent("ui_action", "subtitle_event",
								"scale_end", null);
					}
				});

		subtitleView.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				try {
					if (event.getPointerCount() == 2) {
						return stScaleGestureDetector.onTouchEvent(event);
					}
					boolean res = subtitleViewGestureListener
							.onTouchEvent(event);
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

		// fileDownloader = new
		// FileDownloader(LamrimReaderActivity.this,downloadListener);

		bookView = (MyListView) findViewById(R.id.bookPageGrid);
		bookView.setFadeColor(getResources().getColor(R.color.subtitleBGcolor));
		int defTheoryTextSize = getResources()
				.getInteger(R.integer.defFontSize);
		final int theoryTextSize = runtime.getInt(
				getString(R.string.bookFontSizeKey), defTheoryTextSize);
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

				Handler handler = new Handler() {
				};
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

		// bookView.setScrollingCacheEnabled( false );
		rootLayout = (RelativeLayout) findViewById(R.id.rootLayout);
		rootLayout.setLongClickable(false);

		fileSysManager = new FileSysManager(this);
		FileSysManager.checkFileStructure();

		// String appSubtitle=getString(R.string.app_name)
		// +" V"+pkgInfo.versionName+"."+pkgInfo.versionCode;
		String appSubtitle = getString(R.string.app_name) + " V" + pkgInfo.versionName;
		ActionBar actionBar=getSupportActionBar();
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

	private void setTheoryArea(final int pageIndex, final int pageShift) {
		int defTitleTextSize = getResources().getInteger(R.integer.defFontSize);
		final int subtitleTextSize = runtime.getInt(
				getString(R.string.subtitleFontSizeKey), defTitleTextSize);
		int defTheoryTextSize = getResources()
				.getInteger(R.integer.defFontSize);
		final int theoryTextSize = runtime.getInt(
				getString(R.string.bookFontSizeKey), defTheoryTextSize);

		Log.d(logTag, "Update theory font size: " + theoryTextSize
				+ ", subtitle font size: " + subtitleTextSize);

		runOnUiThread(new Runnable() {
			public void run() {
				bookView.setTextSize(theoryTextSize);
				bookView.setSelectionFromTop(pageIndex, pageShift);
				bookView.refresh();
				subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, subtitleTextSize);
				jumpPage.setText(Integer.toString(pageIndex));
			}
		});
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

		// Dump default settings to DB
		int isInit = runtime.getInt("mediaIndex", -1);
		if (isInit == -1) {
			Log.d(logTag, "This is first time launch LamrimReader, initial default settings.");
			subtitleView.setTextSize(getResources().getInteger(R.integer.defFontSize));
			// int currentIndex=mpController.getCurrentPosition();
			SharedPreferences.Editor editor = runtime.edit();
			editor.putInt("mediaIndex", isInit);
			// editor.putInt("playerStatus",
			// mpController.getMediaPlayerState());
			editor.putInt("playPosition", 0);
			editor.commit();

		}

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

		mediaIndex = runtime.getInt("mediaIndex", -1);
		Log.d(getClass().getName(), "Media index = "+mediaIndex);
		if (mediaIndex == -1)
			return;

		if (!mpController.isPlayerReady())
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
//		setTextSize = rootMenu.add(getString(R.string.menuStrTextSize));
//		setTextSize.setIcon(R.drawable.font_size);
		playRegionRec = rootMenu.add(getString(R.string.menuStrPlayRegionRec));
		playRegionRec.setIcon(R.drawable.region);
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
		Log.d(funcInto,
				"****OptionsItemselected, select item=" + item.getItemId()
						+ ", String=" + item.getTitle() + ", Order="
						+ item.getOrder() + " ****");
		String gid = (String) item.getTitle();
		GaLogger.sendEvent("ui_action", "menu_event",
				((gid.length() == 0) ? "root_menu" : gid) + "_pressed", null);

		if (item.equals(rootMenuItem)) {
			Log.d(logTag,
					"Create menu: can save region? "
							+ mpController.isRegionPlay());
			if (RegionRecord.records.size() > 0) {
				playRegionRec.setEnabled(true);
				playRegionRec.setIcon(R.drawable.region);
			} else {
				playRegionRec.setEnabled(false);
				playRegionRec.setIcon(R.drawable.region_d);
			}
		}

		if (item.getTitle().equals(getString(R.string.menuStrSelectSpeech))) {
			final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
			if (wakeLock.isHeld())wakeLock.release();
			startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
		} else if (item.getTitle().equals(getString(R.string.globalLamrim))) {
			final Intent calendarMenu = new Intent(LamrimReaderActivity.this,	CalendarActivity.class);
			if (wakeLock.isHeld())wakeLock.release();
			startActivityForResult(calendarMenu, GLOBAL_LAMRIM_RESULT);
//		}else if (item.getTitle().equals(getString(R.string.menuStrTextSize))) {
//			showSetTextSizeDialog();
		} else if (item.getTitle().equals(
				getString(R.string.menuStrSavePlayRegion))) {
			showSaveRegionDialog();
		} else if (item.getTitle().equals(
				getString(R.string.menuStrPlayRegionRec))) {
			showRecordListPopupMenu();
		} else if (item.getTitle().equals(
				getString(R.string.menuStrOpenProjectWeb))) {
			Uri uri = Uri.parse(getString(R.string.projectWebUrl));
			Intent it = new Intent(Intent.ACTION_VIEW, uri);
			startActivity(it);
		} else if (item.getTitle().equals(getString(R.string.exitApp))) {
			onBackPressed();
			Log.d(funcLeave, "**** onBackPressed ****");
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

	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		Log.d(funcInto, "**** Into onActivityResult: Get result from: "	+ requestCode + " ****");

		SharedPreferences.Editor editor = runtime.edit();
		switch (requestCode) {
		case SPEECH_MENU_RESULT:
			if (resultCode == RESULT_CANCELED) {
				Log.d(logTag, "User skip, do nothing.");
				return;
			}
			
			if(intent == null){
				GaLogger.sendException("SpeechMenuActivity return data to LamrimRaderActivity Failure(Failure delivering result ResultInfo).", null, true);
				return;
			}
			
			final int selected = intent.getIntExtra("index", -1);
			Log.d(logTag, "OnResult: the user select index=" + selected);
			if(selected == -1)return;
			// Flag for runtime
			
			
			editor.putInt("mediaIndex", selected);
			editor.putInt("playPosition", 0);
			editor.commit();
			regionPlayIndex = -1;
			GLamrimSectIndex = -1;
			
			final int pageNum = SpeechData.refPage[selected] - 1;
			if (pageNum != -1){
				bookViewMountPoint[0]=pageNum;
				bookViewMountPoint[1]=0;
			}

			Log.d(logTag, "Call reset player in onActivityResult.");
			mpController.reset();
			GaLogger.sendEvent("activity", "SpeechMenu_result", "select_index_"	+ selected, null);
			// After onActivityResult, the life-cycle will return to onStart,
			// do start downloader in OnResume.
			break;
		case GLOBAL_LAMRIM_RESULT:
			if (resultCode == RESULT_CANCELED)return;
			
			if(intent == null){
				GaLogger.sendException("CalendarActivity return data to LamrimRaderActivity Failure(Failure delivering result ResultInfo).", null, true);
				return;
			}
			
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
			GLamrimSelectedDay=intent.getStringExtra("selectedDay");
			Log.d(getClass().getName(),"Get data: "+glRecord);
			
			String sec[]=GLamrimSelectedDay.split("/");
			GLamrimSelectedDay=sec[1]+"/"+sec[2];
			
			final int[] theoryStart=GlRecord.getTheoryStrToInt(glRecord.theoryLineStart);// {page,line}
			int[] theoryEnd=GlRecord.getTheoryStrToInt(glRecord.theoryLineEnd);// {page,line}
			int[] speechStart=GlRecord.getSpeechStrToInt(glRecord.speechPositionStart);// {speechIndex,min,sec}
			int[] speechEnd=GlRecord.getSpeechStrToInt(glRecord.speechPositionEnd);// {speechIndex,min,sec}
			
			Log.d(getClass().getName(),"Parse result: Theory: P"+theoryStart[0]+"L"+ theoryStart[1]+" ~ P"+theoryEnd[0]+"L"+theoryEnd[1]);
			Log.d(getClass().getName(),"Parse result: Speech: "+speechStart[0]+":"+ speechStart[1]+":"+speechStart[2]+" ~ "+speechEnd[0]+":"+speechEnd[1]+":"+speechEnd[2]);
			
			GLamrimHighlightRegion[0]=theoryStart[0];
			GLamrimHighlightRegion[1]=theoryStart[1];
			GLamrimHighlightRegion[2]=theoryEnd[0];
			GLamrimHighlightRegion[3]=theoryEnd[1];
			bookViewMountPoint[1]=(int) bookView.setViewToPosition(theoryStart[0], theoryStart[1]);
			bookViewMountPoint[0]=theoryStart[0];

			if(speechStart[0] == speechEnd[0]){// The same media.
				GLamrimSect[0][0]=speechStart[0];
				GLamrimSect[0][1]=(speechStart[1]*60000)+(speechStart[2]*1000);
				GLamrimSect[0][2]=(speechEnd[1]*60000)+(speechEnd[2]*1000);
				GLamrimSect[1][0]=-1;
				GLamrimSect[1][0]=-1;
				GLamrimSect[1][0]=-1;
				Log.d(getClass().getName(),"Region time="+GLamrimSect[0][1]+" ~ "+GLamrimSect[0][2]);
			}
			else{// difference media.
				GLamrimSect[0][0]=speechStart[0];
				GLamrimSect[0][1]=(speechStart[1]*60000)+(speechStart[2]*1000);
				GLamrimSect[0][2]=-1;
				GLamrimSect[1][0]=speechEnd[0];
				GLamrimSect[1][1]=0;
				GLamrimSect[1][2]=(speechEnd[1]*60000)+(speechEnd[2]*1000);
				Log.d(getClass().getName(),"Region time="+GLamrimSect[0][1]+" ~ "+GLamrimSect[1][2]);
			}
			GLamrimSectIndex=0;
			
			Log.d(getClass().getName(), "Set mediaIndex="+mediaIndex+", play position=0");
			editor.putInt("mediaIndex", GLamrimSect[0][0]);
			editor.putInt("playPosition", 0);
			editor.commit();
			
			mpController.reset();
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
			public void onProgressChanged(final SeekBar seekBar,
					final int progress, boolean fromUser) {
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
						Log.d(logTag, "Write theory size: "
										+ (int) theorySb.getProgress()
										+ ", subtitle size: "
										+ subtitleSb.getProgress()
										+ " to runtime.");
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

	private void showSaveRegionDialog() {
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
	}

	private void showRecordListPopupMenu() {
		LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		final View popupView = inflater.inflate(R.layout.popup_record_list,	null);
		Rect rectgle = new Rect();
		Window window = getWindow();
		window.getDecorView().getWindowVisibleDisplayFrame(rectgle);
		int StatusBarHeight = rectgle.top;
		int contentViewTop = window.findViewById(Window.ID_ANDROID_CONTENT)
				.getTop();
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
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		popupWindow.setContentView(popupView);

		regionListView = (ListView) popupView.findViewById(R.id.recordListView);
		regionListView.setAdapter(regionRecordAdapter);
		regionListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> arg0, View v, final int position, long id) {
				Log.d(logTag, "Region record menu: item " + RegionRecord.records.get(position).title + " clicked.");
				File media = FileSysManager.getLocalMediaFile(RegionRecord.records.get(position).mediaIndex);
				File subtitle = FileSysManager.getLocalSubtitleFile(RegionRecord.records.get(position).mediaIndex);
				if (media == null || subtitle == null || !media.exists() || !subtitle.exists()) {
					String msg = String.format(getString(R.string.dlgResNeedDownloadFirst),	SpeechData.getNameId(RegionRecord.records.get(position).mediaIndex));
					AlertDialog.Builder dialog = new AlertDialog.Builder(LamrimReaderActivity.this);
					dialog.setTitle(msg);
					dialog.setPositiveButton(getString(R.string.dlgOk),	new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
									speechMenu.putExtra("index", ""+RegionRecord.records.get(position).mediaIndex);
									if (wakeLock.isHeld())
										wakeLock.release();
									startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
								}
							});
					dialog.setNegativeButton(getString(R.string.dlgCancel),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,	int which) {
									dialog.dismiss();
									if (wakeLock.isHeld())
										wakeLock.release();
								}
							});
					if (!wakeLock.isHeld()) {
						wakeLock.acquire();
					}
					dialog.show();
					return;
				}

				mpController.desetPlayRegion();
				mpController.reset();
				SharedPreferences.Editor editor = runtime.edit();
				editor.putInt("mediaIndex",	RegionRecord.records.get(position).mediaIndex);
				editor.putInt("playPosition", RegionRecord.records.get(position).startTimeMs);
				editor.commit();
				popupWindow.dismiss();
				mediaIndex = RegionRecord.records.get(position).mediaIndex;
				regionPlayIndex = position;
				GLamrimSectIndex=-1;
				bookViewMountPoint[0]=-1;
				bookViewMountPoint[1]=-1;
				startPlay(mediaIndex);
				GaLogger.sendEvent("dialog_action", "select_record", "play_saved_region", position);
				// The procedure will not return to onStart or onResume, start
				// play media from here.
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

	private void hideTitle(){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);  
			    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
			    getSupportActionBar().hide();
			}});
	}

	private void showTitle(){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);  
				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
				getSupportActionBar().show();
			}});
	}

	private void highlightView(View v){
		Animation animation = (Animation) AnimationUtils.loadAnimation(this, R.anim.blank);
		v.startAnimation(animation);
	}
	class RegionRecordAdapter extends SimpleAdapter {

		public RegionRecordAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				Log.d(getClass().getName(), "row=null, construct it.");
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.popup_record_list_row, parent,
						false);
			}

			RegionRecord record = RegionRecord.getRegionRecord(LamrimReaderActivity.this, position);
			Log.d(getClass().getName(), "Set: " + record.title);
			TextView title = (TextView) row.findViewById(R.id.regionRowTitle);
			TextView timeReg = (TextView) row.findViewById(R.id.timeRegion);

			TextView info = (TextView) row.findViewById(R.id.info);

			ImageButton editButton = (ImageButton) row.findViewById(R.id.editButton);
			ImageButton delButton = (ImageButton) row.findViewById(R.id.deleteButton);

			title.setText(record.title);
			timeReg.setText(SpeechData.getNameId(record.mediaIndex) + "    "
					+ Util.getMsToHMS(record.startTimeMs, "\"", "'", false)+ " ~ "
					+ Util.getMsToHMS(record.endTimeMs, "\"", "'", false));
			info.setText(record.info);
			Log.d(logTag, "Info: " + record.info);

			editButton.setFocusable(false);
			delButton.setFocusable(false);

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
					BaseDialogs.showEditRegionDialog(LamrimReaderActivity.this,
							mediaIndex, rr.startTimeMs, rr.endTimeMs, rr.title,
							null, position, callBack);
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
	
	private void setMediaControllerView(int regStartMs, int regEndMs, boolean prevBtnEnable,boolean prevBtnVisiable, OnClickListener prev, boolean nextBtnEnable, boolean nextBtnVisiable,OnClickListener next){
		mpController.setPrevNextListeners(next, prev);
		mpController.setPrevButtonIconEnable(prevBtnEnable);
		mpController.setNextButtonIconEnable(nextBtnEnable);
		mpController.setPlayRegionStartMs(regStartMs);
		mpController.setPlayRegionEndMs(regEndMs);
		View nextBtn=mpController.getControllerView().findViewById(R.id.next);
		View prevBtn=mpController.getControllerView().findViewById(R.id.prev);
		if(prevBtnVisiable)prevBtn.setVisibility(View.VISIBLE);	else prevBtn.setVisibility(View.GONE);
		if(nextBtnVisiable)nextBtn.setVisibility(View.VISIBLE); else nextBtn.setVisibility(View.GONE);
	}
	
	public interface PrevNextListener{
		public OnClickListener getNextListener();
		public OnClickListener getPrevListener();
	}
	
	class GLamrimModePrevNextListener implements PrevNextListener{
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
//			mpController.setControllerViewClickable(false);-
			Log.d(getClass().getName(),"Switch to first section of Global Lamrim.");
			mpController.reset();
			startPlay(GLamrimSect[GLamrimSectIndex][0]);
		}
		
		public OnClickListener getPrevListener(){
			return prevListener;
		}
		
		public OnClickListener getNextListener(){
			return nextListener;
		}
	}
	
	class NormalModePrevNextListener implements PrevNextListener{

		OnClickListener prevListener=new OnClickListener(){
		@Override
		public void onClick(View v) {
			mpController.showControllerView(LamrimReaderActivity.this);
			Log.d(getClass().getName(),"Prev button click on Normal mode.");
			// Clear regionStart
			if(mpController.getRegionStartPosition()!=-1){
				mpController.setPlayRegionStartMs(-1);
				mpController.setPrevButtonIconEnable(false);
				return;
			}
			
			mpController.setPlayRegionStartMs(mpController.getCurrentPosition());
			mpController.setPrevButtonIconEnable(true);
			
		}};

		OnClickListener nextListener=new OnClickListener(){
			@Override
			public void onClick(View v) {
				mpController.showControllerView(LamrimReaderActivity.this);
				Log.d(getClass().getName(),"Next button click on Normal mode.");
				// Clear regionStart
				if(mpController.getRegionEndPosition()!=-1){
					mpController.setPlayRegionEndMs(-1);
					mpController.setNextButtonIconEnable(false);
					return;
				}
			
				mpController.setPlayRegionEndMs(mpController.getCurrentPosition());
				mpController.setNextButtonIconEnable(true);
			}
		};
		public OnClickListener getPrevListener(){
			return prevListener;
		}
		
		public OnClickListener getNextListener(){
			return nextListener;
		}
	}

}
