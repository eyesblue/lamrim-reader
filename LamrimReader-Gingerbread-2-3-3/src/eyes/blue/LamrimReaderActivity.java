package eyes.blue;

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
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
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
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/*
 * $Id$
 * */
public class LamrimReaderActivity extends Activity {
	/** Called when the activity is first created. */
	private static final long serialVersionUID = 3L;
	final static String logTag = "LamrimReader";
	final static String funcInto = "Function Into";
	final static String funcLeave = "Function Leave";

	final static int THEORY_MENU = 0;
	final static int SPEECH_MENU = 1;

	final static int SPEECH_MENU_RESULT = 0;
	final static int THEORY_MENU_RESULT = 1;
	final static int OPT_MENU_RESULT = 2;

	
	static int mediaIndex = -1;
	MediaPlayerController mpController;
	private PowerManager powerManager = null;
	private PowerManager.WakeLock wakeLock = null;
	ListView bookView = null;
	TextView subtitleView = null;
	SharedPreferences runtime = null;

	ArrayList<HashMap<String, String>> fakeList = null;
	TheoryListAdapter adapter = null;
	
	

	FileSysManager fileSysManager = null;
	FileDownloader fileDownloader = null;
	LinearLayout rootLayout = null;
	
	Typeface educFont = null;
	View toastLayout = null;
	TextView toastTextView = null;
	Toast toast = null;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		// try{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
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

		mpController = new MediaPlayerController(LamrimReaderActivity.this, new MediaPlayerControllerListener() {
			@Override
			public void onSubtitleChanged(SubtitleElement subtitle) {
//				Log.d(getClass().getName(), "Set subtitle: "+ subtitle.text);
				setSubtitleViewText(subtitle.text);
			}
			@Override
			public void onSeek(SubtitleElement subtitle){
				showSubtitleToast(subtitle.text+" - ("+getMsToHMS(subtitle.startTimeMs)+')');
			}
			@Override
			public void startMoment(){setSubtitleViewText("");}
			@Override
			public void onMediaPrepared() {
				Log.d(getClass().getName(),"MediaPlayer prepared, show controller.");
				if(mpController.isSubtitleReady())setSubtitleViewText(getString(R.string.dlgHintMpController));
				else setSubtitleViewText(getString(R.string.dlgHintMpControllerNoSubtitle));
				
				// If this time fire by user select a new speech, no need to seekTo(sometime), just play from 0, and set the newPlay flag to false.
				int seekPosition=runtime.getInt("playPosition", -1);
				if(seekPosition!=0)mpController.seekTo(seekPosition);
				setTitle(getString(R.string.app_name) +" V"+serialVersionUID+" - "+ SpeechData.getNameId(mediaIndex));
				Log.d(logTag,"Check media static before show controller: media player state: "+mpController.getMediaPlayerState()+", normal should equal or bigger then "+MediaPlayerController.MP_PREPARED);
				mpController.showMediaPlayerController(LamrimReaderActivity.this.findViewById(android.R.id.content));
			}
			@Override
			public void startRegionSeted(SubtitleElement subtitle){
				showNarmalToastMsg("區段開始位置設定完成: "+getMsToHMS(subtitle.startTimeMs));
			}
			@Override
			public void startRegionDeset(SubtitleElement subtitle){
				showNarmalToastMsg("區段開始位置已清除: ");
			}
			@Override
			public void endRegionSeted(SubtitleElement subtitle){showNarmalToastMsg("區段結束位置設定完成: "+getMsToHMS(subtitle.endTimeMs));}
			@Override
			public void endRegionDeset(SubtitleElement subtitle){showNarmalToastMsg("區段結束位置已清除: ");}
			@Override
			public void startRegionPlay(SubtitleElement startSubtitle,SubtitleElement endSubtitle){
				showNarmalToastMsg("開始區段播放: "+getMsToHMS(startSubtitle.startTimeMs)+" - "+ getMsToHMS(endSubtitle.endTimeMs));
			}
			@Override
			public void stopRegionPlay(SubtitleElement startSubtitle,SubtitleElement endSubtitle){
				showNarmalToastMsg("停止區段播放: "+getMsToHMS(startSubtitle.startTimeMs)+" - "+ getMsToHMS(endSubtitle.endTimeMs));
			}
		});

		
		LayoutInflater inflater = getLayoutInflater();
		toastLayout = inflater.inflate(R.layout.toast_text_view, (ViewGroup) findViewById(R.id.toastLayout));
		toastTextView = (TextView) toastLayout.findViewById(R.id.text);
		toastTextView.setTypeface(educFont);
		toast = new Toast(getApplicationContext());
//		toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
//		toast.setDuration(Toast.LENGTH_LONG);
//		toast.setView(layout);
//		toast.show();
		
/*		toastTextView = new TextView(LamrimReaderActivity.this);
		toastTextView.setTypeface(educFont);
		toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);*/

		subtitleView = (TextView) findViewById(R.id.subtitleView);
		subtitleView.setTypeface(educFont);
		
//		subtitleView = new TextView(LamrimReaderActivity.this);
		subtitleView.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				Log.d(logTag, v	+ " been clicked, Show media plyaer control panel.");
				if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED)
					mpController.showMediaPlayerController(findViewById(R.id.rootLayout));
			}
		});
		fileDownloader = new FileDownloader(LamrimReaderActivity.this,downloadListener);
		bookView = (ListView) findViewById(R.id.bookPageGrid);
		setTheoryArea();
//		bookView.setScrollingCacheEnabled( false );
		rootLayout = (LinearLayout) findViewById(R.id.rootLayout);
		rootLayout.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// String s=(v.equals(subtitleView))?" is ":" is not ";
				Log.d(logTag, v	+ " been clicked, Show media plyaer control panel.");
				if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED)
					mpController.showMediaPlayerController(findViewById(R.id.rootLayout));
			}
		});

		fileSysManager = new FileSysManager(this);
		FileSysManager.checkFileStructure();
		Log.d(funcLeave, "******* onCreate *******");
		// LogRepoter.log("Leave OnCreate");
	}

	@Override
	protected void onStart() {
		super.onStart();
		Log.d(funcInto, "**** onStart() ****");
		
		// Dump default settings to DB
		int isInit=runtime.getInt("mediaIndex", -1);
		if(isInit==-1){
			SharedPreferences.Editor editor = runtime.edit();
			editor.putInt("mediaIndex", isInit);
//			editor.putInt("playerStatus", mpController.getMediaPlayerState());
			editor.putInt("playPosition", mpController.getCurrentPosition());
			editor.commit();
			
			Log.d(funcLeave,"**** saveRuntime ****");
		}
		Log.d(funcLeave, "**** onStart() ****");
	}

	@Override
	protected void onResume() {
		super.onResume();
		Log.d(logTag,"Into onResume");
		
		try {
			if (mpController.getMediaPlayerState() >= MediaPlayerController.MP_PREPARED){
				Log.d(logTag,"onResume: The state of MediaPlayer is PAUSE, start play.");
				mpController.showMediaPlayerController(LamrimReaderActivity.this.findViewById(android.R.id.content));
				return;
			}
		} catch (IllegalStateException e) {	e.printStackTrace();}
		
		mediaIndex=runtime.getInt("mediaIndex", -1);
		if(mediaIndex!=-1)fileDownloader.start(mediaIndex);

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
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(funcInto, "**** onDestroy ****");
		fileDownloader.finish();
		mpController.release();
		
		Log.d(funcLeave, "**** onDestroy ****");
	}
	
	protected void saveRuntime(){
		Log.d(funcInto,"**** saveRuntime ****");
		SharedPreferences.Editor editor = runtime.edit();
		Log.d(logTag,"Save mediaIndex="+mediaIndex);
		editor.putInt("mediaIndex", mediaIndex);
//		editor.putInt("playerStatus", mpController.getMediaPlayerState());
		editor.putInt("playPosition", mpController.getCurrentPosition());
		editor.putInt("bookPage",bookView.getFirstVisiblePosition());
		View v=bookView.getChildAt(0);  
        editor.putInt("bookPageShift",(v==null)?0:v.getTop());
        Log.d(logTag,"Save content: mediaIndex="+mediaIndex+", playPosition="+runtime.getInt("playPosition", -1)+"");
		editor.commit(); Log.d(funcLeave,"**** saveRuntime ****");
	}

	@Override
	public void onBackPressed() {
		Log.d(funcInto, "**** onBackPressed ****");

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
		Log.d(funcLeave, "**** onBackPressed ****");
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        //參數1:群組id, 參數2:itemId, 參數3:item順序, 參數4:item名稱
        menu.add(0, 0, 0, getString(R.string.menuStrSelectSpeech));
        menu.add(0, 1, 1, getString(R.string.menuStrOption));
        menu.add(0, 2, 2, getString(R.string.menuStrAbout));
        return super.onCreateOptionsMenu(menu);
    }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(funcInto,	"****OptionsItemselected, select item=" + item.getItemId()	+ " ****");
		
		switch (item.getItemId()) {
		case SPEECH_MENU_RESULT:
			final Intent speechMenu = new Intent(LamrimReaderActivity.this,	SpeechMenuActivity.class);
			if (wakeLock.isHeld())wakeLock.release();
			startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
			break;
		case THEORY_MENU_RESULT:
			final Intent optCtrlPanel = new Intent(LamrimReaderActivity.this, OptCtrlPanel.class);
			if (wakeLock.isHeld())wakeLock.release();
			startActivityForResult(optCtrlPanel, OPT_MENU_RESULT);
			break;
		case OPT_MENU_RESULT:
			final Intent aboutPanel = new Intent(LamrimReaderActivity.this,	AboutActivity.class);
			if (wakeLock.isHeld())wakeLock.release();
			this.startActivity(aboutPanel);
			break;
		}

		Log.d(funcLeave, "**** Into Options selected, select item=" + item.getItemId()	+ " ****");
		return true;
	}

	private void onOptResultData(int resultCode, Intent intent) {
		Log.d(funcInto, "onOptResultData");
		final int bookFontSize = intent.getIntExtra(getString(R.string.bookFontSizeKey),getResources().getInteger(R.integer.defBookFontSize));
		final int subtitleFontSize = intent.getIntExtra(getString(R.string.subtitleFontSizeKey), getResources().getInteger(R.integer.defSubtitleFontSize));

		SharedPreferences.Editor editor = runtime.edit();
		editor.putInt(getString(R.string.bookFontSizeKey), bookFontSize);
		editor.putInt(getString(R.string.subtitleFontSizeKey), subtitleFontSize);
		editor.commit();

		updateTextSize();
		Log.d(logTag, "Get the book font size: " + bookFontSize	+ ", subtitleFontSize: " + subtitleFontSize);
		Log.d(funcLeave, "Leave onOptResultData");
	}

	protected void onActivityResult(int requestCode, int resultCode,Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);

		Log.d(funcInto, "**** Into onActivityResult: Get result from: "	+ requestCode + " ****");
		switch (requestCode) {
		case SPEECH_MENU_RESULT:
			if (resultCode == RESULT_CANCELED){
				Log.d(logTag, "User skip, do nothing.");
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
			SharedPreferences.Editor editor = runtime.edit();
			editor.putInt("mediaIndex", selected);
			editor.putInt("playPosition", 0);
			editor.commit();
			mpController.reset();
			
			// After onActivityResult, the life-cycle will return to onStart, do start downloader in OnResume.
			break;
		case THEORY_MENU_RESULT:
			break;
		case OPT_MENU_RESULT:
			onOptResultData(resultCode, intent);
			break;
		}

		Log.d(funcLeave, "Leave onActivityResult");
	}

	private void setTheoryArea() {
		int defTitleTextSize = getResources().getInteger(R.integer.defSubtitleFontSize);
		final int subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), defTitleTextSize);
		int defTheoryTextSize = getResources().getInteger(R.integer.defBookFontSize);
		final int theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey),defTheoryTextSize);
		final int bookPage=runtime.getInt("bookPage", 0);
		final int bookPageShift=runtime.getInt("bookPageShift", 0);
		String[] bookArray=getResources().getStringArray(R.array.book);
		
		fakeList = new ArrayList<HashMap<String, String>>();
		for (int i=0;i<bookArray.length;i++) {
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("page", bookArray[i]);
			item.put("desc", "第 "+(i+1)+" 頁");
			fakeList.add(item);
		}
		
		adapter = new TheoryListAdapter(this, fakeList,
				R.layout.theory_page_view, new String[] { "page", "desc" },
				new int[] { R.id.pageContentView, R.id.pageNumView });
		bookView.setAdapter(adapter);
		
		Log.d(logTag,"Update theory font size: "+theoryTextSize+", subtitle font size: "+subtitleTextSize);
		
		runOnUiThread(new Runnable() {
			public void run() {
				adapter.setTextSize(theoryTextSize);
				bookView.setSelectionFromTop(bookPage, bookPageShift);
				adapter.notifyDataSetChanged();
				subtitleView.setTextSize(subtitleTextSize);
			}
		});
	}

	private void setUIContent() {
		Log.d(funcInto, "**** setUIContent ****");
		if (fakeList == null)
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
				row = inflater.inflate(R.layout.theory_page_view, parent, false);
			}

			// / Log.d(logTag, "row=" + row+", ConvertView="+convertView);
			TheoryPageView bContent = (TheoryPageView) row.findViewById(R.id.pageContentView);
			// bContent.drawPoints(new int[0][0]);
			bContent.setTypeface(educFont);
			if (bContent.getTextSize() != textSize)
				bContent.setTextSize(textSize);
			bContent.setText(fakeList.get(position).get("page"));
			// bContent.setText(Html.fromHtml("<font color=\"#FF0000\">No subtitle</font>"));
			TextView pNum = (TextView) row.findViewById(R.id.pageNumView);
			if (pNum.getTextSize() != textSize)
				pNum.setTextSize(textSize);
			pNum.setText(fakeList.get(position).get("desc"));
			return row;
		}

		public void setTextSize(float size) {
			textSize = size;
		}
	}

	private void updateTextSize() {
		int defTitleTextSize = getResources().getInteger(R.integer.defSubtitleFontSize);
		final int subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), defTitleTextSize);
		int defTheoryTextSize = getResources().getInteger(R.integer.defBookFontSize);
		final int theoryTextSize = runtime.getInt(getString(R.string.bookFontSizeKey),defTheoryTextSize);
		
		Log.d(logTag,"Update theory font size: "+theoryTextSize+", subtitle font size: "+subtitleTextSize);
		runOnUiThread(new Runnable() {
			public void run() {
				if (adapter != null)
					adapter.setTextSize(theoryTextSize);
				bookView.destroyDrawingCache();
				if (adapter != null)
					adapter.notifyDataSetChanged();
				
				subtitleView.setTextSize(subtitleTextSize);
			}
		});

	}


	/*
	 * Set the message on the subtitle view, there should check the subtitleView
	 * is not playing, or hide the message.
	 */
	public void setSubtitleViewText(final String s) {
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
				ImageView img=(ImageView) toastLayout.findViewById(R.id.imageView);
				img.setImageResource(R.drawable.ic_launcher);
				toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setView(toastLayout);
				toastTextView.setText(s);
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
				ImageView img=(ImageView) toastLayout.findViewById(R.id.imageView);
				img.setImageResource(R.drawable.info_icon);
				toast.setView(toastLayout);
				toast.setDuration(Toast.LENGTH_LONG);
				toastTextView.setText(s);
//				toast=toast.makeText(LamrimReaderActivity.this, s, Toast.LENGTH_SHORT);
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


	final protected DownloadListener downloadListener = new DownloadListener() {
		boolean isSpeechReady=true;
		boolean isSubtitleReady=true;
		
		@Override
		public void allPrepareFinish(int... index) {
			Log.d(funcInto, "**** The target index (" + index + ") has prepared ****");
			Log.d(logTag, "Create player.");

			if(!isSpeechReady)return;
				
			try {
				mpController.reset();
			} catch (IllegalStateException iae) {
				setSubtitleViewText(getString(R.string.errWhileReleasePlayer));
				iae.printStackTrace();
				return;
			}

			Log.d(getClass().getName(), "Get the local file of index " + index);
			
			synchronized (mpController) {
				try {
					setSubtitleViewText(getString(R.string.dlgDescPrepareSpeech));
					mpController.setDataSource(getApplicationContext(),index[0]);
				} catch (IllegalArgumentException e) {
					setSubtitleViewText(getString(R.string.errIAEwhileSetPlayerSrc));
					e.printStackTrace();
					return;
				} catch (SecurityException e) {
					setSubtitleViewText(getString(R.string.errSEwhileSetPlayerSrc));
					e.printStackTrace();
					return;
				} catch (IllegalStateException e) {
					setSubtitleViewText(getString(R.string.errISEwhileSetPlayerSrc));
					e.printStackTrace();
					return;
				} catch (IOException e) {
					setSubtitleViewText(getString(R.string.errIOEwhileSetPlayerSrc));
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
					setSubtitleViewText(getString(R.string.errISEwhilePrepPlayer));
					e.printStackTrace();
				} catch (IOException e) {
					setSubtitleViewText(getString(R.string.errIOEwhilePrepPlayer));
					e.printStackTrace();
				}
			}

			Log.d(getClass().getName(),"**** Prepare files success ****");
		}

		@Override
		public void prepareFail(int fileIndex, int type) {
			//if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
			if(type==getResources().getInteger(R.integer.MEDIA_TYPE))isSpeechReady=false;
			if(type==getResources().getInteger(R.integer.SUBTITLE_TYPE))isSubtitleReady=false;
			setSubtitleViewText(getString(R.string.dlgDescDownloadFail));
			Log.d(getClass().getName(),"**** Prepare files fail ****");
		}
	};

	private String getMsToHMS(int ms){
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
		return mst+'分'+ss+"."+sub+'秒';
	}
}
