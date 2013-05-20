package eyes.blue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;


public class LamrimReaderActivity extends Activity {
	/** Called when the activity is first created. */
	final static String logTag = "LamrimReader";
	final static String funcInto = "Function Into";
	final static String funcLeave = "Function Leave";

	final static int THEORY_MENU = 0;
	final static int SPEECH_MENU = 1;

	final static int SPEECH_MENU_RESULT = 0;
	final static int THEORY_MENU_RESULT = 1;
	final static int OPT_MENU_RESULT = 2;

	
	static int mediaIndex = -1;
	int seekPosition=0;
	// int playerStartPosition = -1;
//	MediaPlayer mediaPlayer = null;
	MediaPlayerController mpController;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;

/* The onRestoreInstanceState function not allow multi-thread executing,
 * While load last status after UI been destroy, I reload last mediaIndex
 * and playPoint, play Immediately while prepared, at this point, the UI
 * not construct yet, it will error here. the showPlayControllerWhilePrepared
 * instruction should I play media after prepared for avoid the error.
 */
	boolean showPlayControllerWhilePrepared=true;
	
	// PowerManager.WakeLock releaseScreenLock=null;
	ListView bookView = null;
	TextView subtitleView = null;
//	MediaController mediaController = null;
	ProgressBar progressBar = null;
	SharedPreferences runtime = null;

	ArrayList<HashMap<String, String>> bookList = null;
	TheoryListAdapter adapter = null;
	Toast toast = null;

	FileSysManager fileSysManager = null;
	FileDownloader fileDownloader = null;
	LinearLayout rootLayout = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
//		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		if (runtime == null)
			runtime = getSharedPreferences(
					getString(R.string.runtimeStateFile), 0);

		// fontSizeArraylength=getResources().getIntArray(R.array.fontSizeArray).length;
		setContentView(R.layout.main);

		Log.d(funcInto, "******* Into LamrimReader.onCreate *******");
		if (savedInstanceState != null){
			Log.d(logTag, "The savedInstanceState is not null!");
		}
		Log.d(getClass().getName(), "mediaIndex=" + mediaIndex);
		powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,logTag);
		fileSysManager = new FileSysManager(this);

			mpController = new MediaPlayerController(LamrimReaderActivity.this,			
			new MediaPlayerControllerListener() {
				@Override
				public void onSubtitleChanged(SubtitleElement subtitle) {
//					Log.d(getClass().getName(), "Set subtitle: "+ subtitle.text);
					setSubtitleViewText(subtitle.text);
				}
				@Override
				public void onMediaPrepared() {
					Log.d(getClass().getName(),"MediaPlayer prepared, show controller.");
					if(mpController.isSubtitleReady())setSubtitleViewText(getString(R.string.mpControllerHint));
					else setSubtitleViewText(getString(R.string.mpControllerNoSubtitleHint));
					mpController.seekTo(seekPosition);
					seekPosition=0;
					mpController.start();
					setTitle(getString(R.string.app_name) +" - "+ getResources().getStringArray(R.array.fileName)[mediaIndex]);
					if(showPlayControllerWhilePrepared)mpController.showMediaPlayerController(LamrimReaderActivity.this.findViewById(android.R.id.content));
					showPlayControllerWhilePrepared=true;
				}
			});

/*		if (mediaController == null) {
			// mediaController=new MediaController(LamrimReaderActivity.this);
			mediaController = new MediaController(this);
			mediaController.setMediaPlayer(mpController);
			mediaController.setAnchorView(findViewById(R.id.rootLayout));
			mediaController.setEnabled(true);
		}
*/
		// bookView.setMovementMethod(new ScrollingMovementMethod());
		// bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
		if(fileDownloader==null)fileDownloader = new FileDownloader(LamrimReaderActivity.this,downloadListener);
		if (toast == null)
			toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

		if (subtitleView == null) {
			subtitleView = (TextView) findViewById(R.id.subtitleView);
			subtitleView.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d(logTag, v
							+ " been clicked, Show media plyaer control panel.");
					if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED)
						mpController.showMediaPlayerController(findViewById(R.id.rootLayout));
				}
			});
		}

		if (bookView == null)
			bookView = (ListView) findViewById(R.id.bookPageGrid);

		if (progressBar == null) {
			progressBar = (ProgressBar) findViewById(R.id.progressBar);
		}

		if (rootLayout == null) {
			rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
			rootLayout.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					// String s=(v.equals(subtitleView))?" is ":" is not ";
					Log.d(logTag, v
							+ " been clicked, Show media plyaer control panel.");
					if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED)
						mpController.showMediaPlayerController(findViewById(R.id.rootLayout));
				}
			});
		}

		setTheoryArea();
		Log.d(funcLeave, "******* onCreate *******");
		// LogRepoter.log("Leave OnCreate");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(funcInto, "**** onStart() ****");
		FileSysManager.checkFileStructure();
		
		// Dump default settings to DB
		int isInit=runtime.getInt("mediaIndex", -1);
		if(isInit==-1){
			SharedPreferences.Editor editor = runtime.edit();
			editor.putInt("mediaIndex", mediaIndex);
			editor.putInt("playerStatus", mpController.getMediaPlayerState());
			editor.putInt("playPosition", mpController.getCurrentPosition());
			editor.commit();
			setSubtitleViewText(getString(R.string.selectIndexFromMenuDesc));
			Log.d(funcLeave,"**** saveRuntime ****");
		}
		
		updateTextSizeFromRuntimRecord();
		final int bookPage=runtime.getInt("bookPage", 0);
		Log.d(logTag,"Restore the theory to page "+bookPage);
		bookView.post(new Runnable() {  public void run() {
			bookView.requestFocusFromTouch();
			bookView.setSelection(bookPage);
			adapter.notifyDataSetChanged();
        }});
		Log.d(funcLeave, "**** onStart() ****");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(logTag,"Into onResume");
		try {
			if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED){
				Log.d(logTag,"onResume: The state of MediaPlayer is PAUSE, start play.");
				mpController.start();
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
		Log.d(logTag,"Leave onResume");
	}

	@Override
	protected void onPause() {
		super.onPause();
		try {
			if(mpController.getMediaPlayerState()==MediaPlayerController.MP_PLAYING)
				mpController.pause();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
//		mediaController.hide();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(funcInto, "**** onDestroy ****");
		fileDownloader.finish();
		mpController.release();
		if (wakeLock.isHeld()) {
			Log.d(logTag, "Release the screen lock");
			wakeLock.release();
		}
		Log.d(funcLeave, "**** onDestroy ****");
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
			Log.d(getClass().getName(),"Into SaveInstanceStatus");
			saveRuntime();		
	}

	protected void saveRuntime(){
		SharedPreferences.Editor editor = runtime.edit();
		editor.putInt("mediaIndex", mediaIndex);
		editor.putInt("playerStatus", mpController.getMediaPlayerState());
		editor.putInt("playPosition", mpController.getCurrentPosition());
		editor.putInt("bookPage",bookView.getFirstVisiblePosition());
		editor.commit(); Log.d(funcLeave,"**** saveRuntime ****");
	}
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		new Thread(new Runnable(){
			@Override
			public void run() {
		Log.d(getClass().getName(),"Into onRestoreInstanceState");
		mediaIndex=runtime.getInt("mediaIndex", -1);
		int state=runtime.getInt("playerStatus", MediaPlayerController.MP_IDLE);
		seekPosition=runtime.getInt("playPosition", 0);
		
		
		Log.d(getClass().getName(),"Last media index="+mediaIndex+", media status="+state);
		Log.d("","MediaPlayer status is "+mpController.getMediaPlayerState());
		
		if(state<MediaPlayerController.MP_PREPARED)return;
		
			try {
				showPlayControllerWhilePrepared=false;
				mpController.setDataSource(LamrimReaderActivity.this, mediaIndex);
				mpController.prepareMedia();
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				Log.e(getClass().getName(),"IllegalArgumentException happen while restore runtime.");
			} catch (SecurityException e) {
				Log.e(getClass().getName(),"SecurityException happen while restore runtime.");
				e.printStackTrace();
			} catch (IllegalStateException e) {
				Log.e(getClass().getName(),"IllegalStateException happen while restore runtime.");
				e.printStackTrace();
			} catch (IOException e) {
				Log.e(getClass().getName(),"IOException happen while restore runtime.");
				e.printStackTrace();
			}
		
			final int bookPage=runtime.getInt("bookPage", 0);
			bookView.post(new Runnable() {  public void run() {
				
	//			bookView.requestFocusFromTouch();
	//			bookView.clearFocus();
//				bookView.setSelectionFromTop(theoryIndexShift, theoryIndexTop);
				bookView.requestFocusFromTouch();
				bookView.setSelection(bookPage);
				adapter.notifyDataSetChanged();
            }});
			Log.d(getClass().getName(),"Leave onRestoreInstanceState");
			}}).run();
	}
	

	@Override
	public void onBackPressed() {
		Log.d(funcInto, "**** onBackPressed ****");

		// Log.d(logTag,"Check status: playerService="+playerService+", playIntent="+playIntent+", playerBinder="+playerBinder+", playerConn="+mConnection);
		// saveRuntime();

		// if(playerService
		// !=null){playerService.releasePlayer();playerService.stopSelf();}

		// Log.d(logTag,"Check status: playerService="+playerService+", playIntent="+playIntent+", playerBinder="+playerBinder+", playerConn="+mConnection);
		saveRuntime();
		finish();
		Log.d(funcLeave, "**** onBackPressed ****");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(funcInto,
				"****OptionsItemselected, select item=" + item.getItemId()
						+ " ****");
		int selected = item.getItemId();

		switch (selected) {
		case R.id.selectChapter:
			final Intent speechMenu = new Intent(LamrimReaderActivity.this,
					SpeechMenuActivity.class);
			startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
			break;
		case R.id.showCtrlPanel:
			final Intent optCtrlPanel = new Intent(LamrimReaderActivity.this,
					OptCtrlPanel.class);
			startActivityForResult(optCtrlPanel, OPT_MENU_RESULT);
			break;
		case R.id.showAbout:
			final Intent aboutPanel = new Intent(LamrimReaderActivity.this,
					AboutActivity.class);
			this.startActivity(aboutPanel);
			break;

		}
		Log.d(funcLeave,
				"**** Into Options selected, select item=" + item.getItemId()
						+ " ****");
		return true;
	}

	private void onOptResultData(int resultCode, Intent intent) {
		Log.d(funcInto, "onOptResultData");
		final int bookFontSize = intent.getIntExtra(
				getString(R.string.bookFontSizeKey),
				getResources().getInteger(R.integer.defBookFontSize));
		final int subtitleFontSize = intent.getIntExtra(
				getString(R.string.subtitleFontSizeKey), getResources()
						.getInteger(R.integer.defSubtitleFontSize));

		SharedPreferences.Editor editor = runtime.edit();
		editor.putInt(getString(R.string.bookFontSizeKey), bookFontSize);
		editor.putInt(getString(R.string.subtitleFontSizeKey), subtitleFontSize);
		editor.commit();

		Log.d(logTag, "Get the book font size: " + bookFontSize
				+ ", subtitleFontSize: " + subtitleFontSize);
		updateTextSizeFromRuntimRecord();
		Log.d(funcLeave, "Leave onOptResultData");
	}

	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		Log.d(funcInto, "**** Into onActivityResult: Get result from: "
				+ requestCode + " ****");
		switch (requestCode) {
		case SPEECH_MENU_RESULT:
			if (resultCode == RESULT_CANCELED){
				Log.d(logTag, "User skip, do nothing.");
				return;
			}
			final int selected = intent.getIntExtra("index", 0);
			// The code under test, after user selected a media then pause media player and into the idle mode, it do nothing if user select the same one.
			if( selected == mediaIndex && mpController.getMediaPlayerState()!=MediaPlayerController.MP_IDLE){
				Log.d(logTag, "The selected same as playing, do nothing.");
				return;// selected same as playing
			}

			Log.d(logTag, "Get result from speech menu, select index=" + selected);
			mediaIndex = selected;
			mpController.reset();
			if(!wakeLock.isHeld()){Log.d(logTag,"Play media and Lock screen.");wakeLock.acquire();}
			fileDownloader.start(selected);
			break;
		case THEORY_MENU_RESULT:
			break;
		case OPT_MENU_RESULT:
			onOptResultData(resultCode, intent);
			break;
		}

		Log.d(funcLeave, "Leave onActivityResult");
	}



	/*
	 * @Override public boolean onPrepareOptionsMenu(Menu menu) {
	 * Log.d(funcInto,"**** onCreateOptionMenu ****.");
	 * Log.d(logTag,"Player is disable, show normal_mode menu.");
	 * menu.findItem(R.id.mode).setTitle(R.string.normalModeDesc); return true;
	 * }
	 */
	private void setTheoryArea() {
		bookList = getBookContent();
		adapter = new TheoryListAdapter(this, bookList,
				R.layout.theory_page_view, new String[] { "page", "desc" },
				new int[] { R.id.pageContentView, R.id.pageNumView });
		bookView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
	}

	private ArrayList<HashMap<String, String>> getBookContent() {
		String[] bookPage = getResources().getStringArray(R.array.book);
		bookList = new ArrayList<HashMap<String, String>>();
		int pIndex = 0;

		for (String value : bookPage) {
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("page", value);
			item.put("desc", "第 " + (++pIndex) + " 頁");
			bookList.add(item);
		}
		return bookList;
	}

	private void setUIContent() {
		Log.d(funcInto, "**** setUIContent ****");
		if (bookList == null)
			setTheoryArea();
		// Check is it initial state.
		if (mediaIndex < 0)
			return;
		Log.d(funcLeave, "**** setUIContent ****");
	}

	class TheoryListAdapter extends SimpleAdapter {
		float textSize = 0;

		public TheoryListAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
				Log.d(logTag, "row=null, construct it.");
				LayoutInflater inflater = getLayoutInflater();
				row = inflater
						.inflate(R.layout.theory_page_view, parent, false);
			}

			// / Log.d(logTag, "row=" + row+", ConvertView="+convertView);
			TheoryPageView bContent = (TheoryPageView) row
					.findViewById(R.id.pageContentView);
			// bContent.drawPoints(new int[0][0]);

			if (bookList == null)
				Log.d(logTag, "The bookList is null!!!");
			if (bContent.getTextSize() != textSize)
				bContent.setTextSize(textSize);
			bContent.setText(bookList.get(position).get("page"));
			// bContent.setText(Html.fromHtml("<font color=\"#FF0000\">No subtitle</font>"));
			TextView pNum = (TextView) row.findViewById(R.id.pageNumView);
			if (pNum.getTextSize() != textSize)
				pNum.setTextSize(textSize);
			pNum.setText(bookList.get(position).get("desc"));
			return row;
		}

		public void setTextSize(float size) {
			textSize = size;
		}
	}

	private void updateTextSizeFromRuntimRecord() {
		int subtitleTextSize = getResources().getInteger(R.integer.defSubtitleFontSize);
		subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), subtitleTextSize);
		int theoryTextSize = getResources().getInteger(R.integer.defBookFontSize);
		theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey),theoryTextSize);
		updateTextSize(theoryTextSize, subtitleTextSize);
	}

	private void updateTextSize(final int theoryTextSize,
			final int subtitleTextSize) {
		runOnUiThread(new Runnable() {
			public void run() {
				if (adapter != null)
					adapter.setTextSize(getResources().getIntArray(
							R.array.fontSizeArray)[theoryTextSize]);
				bookView.destroyDrawingCache();
				if (adapter != null)
					adapter.notifyDataSetChanged();
			}
		});
		setSubtitleViewTextSize(getResources().getIntArray(
				R.array.fontSizeArray)[subtitleTextSize]);
	}

	private void setSubtitleViewTextSize(final float size) {
		runOnUiThread(new Runnable() {
			public void run() {
				subtitleView.setTextSize(size);
			}
		});
	}

	public int getSubtitleFontSize() {
		int subtitleFontSize = getResources().getInteger(
				R.integer.defSubtitleFontSize);
		return runtime.getInt(getString(R.string.subtitleFontSizeKey),
				subtitleFontSize);
	}

	/*
	 * Set the message on the subtitle view, there should check the subtitleView
	 * is not playing, or hide the message.
	 */
	public void setSubtitleViewText(String s) {
		setSubtitleViewText(s.toCharArray());
	}

	public void setSubtitleViewText(final char[] b) {
		runOnUiThread(new Runnable() {
			public void run() {
				subtitleView.setText(b, 0, b.length);
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



	/*
	 * public void saveRuntime() { Log.d(funcInto,"**** saveRuntime ****"); //
	 * Save status of the mode now in used. SharedPreferences.Editor editor =
	 * runtime.edit();
	 * 
	 * int index = bookView.getFirstVisiblePosition(); View v =
	 * bookView.getChildAt(0); int top = (v == null) ? 0 : v.getTop(); int
	 * position=-1;
	 * 
	 * 
	 * editor.putBoolean(getString(R.string.enablePlayerKey), enablePlayer);
	 * editor
	 * .putInt(getResources().getString(R.string.runtimeNormalTheoryTop),top);
	 * editor
	 * .putInt(getResources().getString(R.string.runtimeNormalTheoryShift),
	 * index);
	 * 
	 * if(enablePlayer && mpState>=MP_PREPARED)
	 * position=mediaPlayer.getCurrentPosition();
	 * 
	 * editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex)
	 * ,mediaIndex);
	 * editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition
	 * ),position); Log.d(logTag,
	 * "Save runtime status for Normal mode: Theory index: "
	 * +top+", Theory shift: "
	 * +index+", Player index: "+mediaIndex+", playerPosition="+ position);
	 * 
	 * editor.commit(); Log.d(funcLeave,"**** saveRuntime ****"); }
	 * 
	 * public void loadRuntime() { Log.d(funcInto,"**** LoadRuntime ****");
	 * 
	 * enablePlayer=runtime.getBoolean(getResources().getString(R.string.
	 * enablePlayerKey), true);
	 * theoryIndexTop=runtime.getInt(getResources().getString
	 * (R.string.runtimeNormalTheoryTop), 0);
	 * theoryIndexShift=runtime.getInt(getResources
	 * ().getString(R.string.runtimeNormalTheoryShift), 0); if(enablePlayer){
	 * playerStartPosition =
	 * runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingPosition
	 * ), 0); mediaIndex=runtime.getInt(getResources().getString(R.string.
	 * runtimeNormalPlayingIndex),-1); Log.d(logTag,
	 * "Load back the runtime: enablePlayer="
	 * +enablePlayer+"Theory index: "+theoryIndexTop
	 * +", Theory shift: "+theoryIndexShift
	 * +", Player index: "+mediaIndex+", playerPosition="+ playerStartPosition);
	 * } Log.d(funcLeave,"**** LoadRuntime. ****"); }
	 */
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

	private void switchNormalMode() {
		subtitleView = (TextView) findViewById(R.id.subtitleView);
		subtitleView.setVisibility(View.VISIBLE);
		if (mediaIndex == -1) {
			Log.d(logTag,
					"**** The user not select a chapter yet! skip play! ****");
			setSubtitleViewText(getResources().getString(
					R.string.selectIndexFromMenuDesc));
			return;
		}
		setTitle(getString(R.string.app_name) + " - "
				+ getResources().getStringArray(R.array.fileName)[mediaIndex]);
		Log.d(logTag, "Prepare media index " + mediaIndex);
		// downloaderService.prepareFile(playingMediaIndex);
		fileDownloader.start(mediaIndex);
	}

	private void switchReadingMode() {
		mpController.pause();
		subtitleView = (TextView) findViewById(R.id.subtitleView);
		subtitleView.setVisibility(View.GONE);
	}

	

	// class MediaPlayerOnPreparedListener implements OnPreparedListener{
	

	final protected DownloadListener downloadListener = new DownloadListener() {
		@Override
		public void prepareFinish(int index) {
			Log.d(funcInto, "**** The target index (" + index + ") has prepared ****");
			
			if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
			
			Log.d(logTag, "Reset player.");
			mpController.reset();
			
			Log.d(logTag, "Create player.");
			setSubtitleViewText(getString(R.string.prepareSubtitleDesc));

			try {
				mpController.reset();
			} catch (IllegalStateException iae) {
				setSubtitleViewText("釋放發生撥放器時發生狀態錯誤!");
				iae.printStackTrace();
				return;
			}

			Log.d(getClass().getName(), "Get the local file of index " + index);
			
			synchronized (mpController) {
				try {
					setSubtitleViewText(getString(R.string.prepareSpeechDesc));
					mpController.setDataSource(getApplicationContext(),index);
				} catch (IllegalArgumentException e) {
					setSubtitleViewText("開啟語音檔時發生參數錯誤!");
					e.printStackTrace();
					return;
				} catch (SecurityException e) {
					setSubtitleViewText("開啟語音檔時發生安全性錯誤!");
					e.printStackTrace();
					return;
				} catch (IllegalStateException e) {
					setSubtitleViewText("開啟語音檔時發生撥放器狀態錯誤!");
					e.printStackTrace();
					return;
				} catch (IOException e) {
					setSubtitleViewText("開啟語音檔時發生輸出入錯誤!");
					e.printStackTrace();
					return;
				}
			}

			synchronized (mpController) {
//				mpState = MP_PREPARING;
				Log.d("setMediaPlayer", "**** setMediaPlayer ****");
//				mediaPlayer.setOnPreparedListener(onPreparedListener);
				try {
					mpController.prepareMedia();
				} catch (IllegalStateException e) {
					setSubtitleViewText("準備語音檔時發生撥放器狀態錯誤!");
					e.printStackTrace();
				} catch (IOException e) {
					setSubtitleViewText("準備語音檔時發生輸出入錯誤!");
					e.printStackTrace();
				}
			}
			
			
			Log.d(getClass().getName(),"**** Prepare files success ****");
		}

		@Override
		public void prepareFail(int fileIndex) {
			if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
			setSubtitleViewText(getString(R.string.downloadFail));
			Log.d(getClass().getName(),"**** Prepare files fail ****");
		}
/*		@Override
		public void fileOperationFail(JSONObject jobj) {
			Log.e(getClass().getName(),
					"Download fail cause file operation fail.");
		}
*/	};

}
