package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import eyes.blue.DownloadService.DownloaderBinder;
import eyes.blue.PlayerService.PlayerBinder;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources.NotFoundException;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StatFs;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

public class LamrimReaderActivity extends Activity{
	/** Called when the activity is first created. */
	final static String logTag = "LamrimReader";
	final static String funcInto = "Function Into";
	final static String funcLeave = "Function Leave";
	LogRepoter log;

	final static int NORMAL_MODE = 0;
	final static int READING_MODE = 1;
	final static int RECODE_SECTION_MODE = 2;
	final static int TV_MODE = 3;
	
	final static int THEORY_MENU=0;
	final static int SPEECH_MENU=1;
	
	final static int SPEECH_MENU_RESULT=0;
	final static int THEORY_MENU_RESULT=1;
	final static int OPT_MENU_RESULT=2;

	// final static int DIALOG_DOWNLOAD_FAIL=0;
	static SubtitleElement[] subtitle = null;
//	int newMediaTarget=-1;
//	int newMediaPlayPosition=-1;
	int currentPlayedSubtitleIndex = -1;
	int playingMediaIndex = -1;
	int playerStartPosition = -1;
	int subtitleWordMax = 0;
	int theoryIndexShift = 0;
	int theoryIndexTop = 0;
	// int subtitleMonInterval = -1;
	// AlertDialog.Builder dialogBuilder =null;
	// Intent optCtrlPanel=null;
	Intent playIntent=null;
	PlayerService playerService=null;
	PlayerBinder playerBinder=null;
	ServiceConnection mConnection=null;
	Intent downloadIntent=null;
	DownloadService downloaderService=null;
	DownloaderBinder downloadBinder=null;
	ServiceConnection dlConn=null;
	
	Intent aboutIntent;
//	protected MediaPlayer mediaPlayer = null;
//	final MediaPlayerOnPreparedListener onPreparedListener=new MediaPlayerOnPreparedListener();
//	final DownloadEventListener downloadListener=new DownloadEventListener();
	PowerManager powerManager=null;
	PowerManager.WakeLock wakeLock = null;
//    PowerManager.WakeLock releaseScreenLock=null;
	ListView bookView=null;
	TextView subtitleView=null;
	static MediaController mediaController=null;
	ProgressBar progressBar=null;
	SharedPreferences runtime=null;
//	int appMode = 0;
	
//	MediaPlayer mediaPlayer=null;
	Timer playBarTimer = new Timer();
	ArrayList<HashMap<String, String>> bookList = null;
	TheoryListAdapter adapter=null;
	Toast toast = null;
	
	FileSysManager fileSysManager = null;
	LinearLayout rootLayout=null;
	static boolean enablePlayer=true;
//	static boolean prepareFromDownloadFinish=false;
	static boolean mediaChanged=false;
	static boolean downlaodServiceBinded=false;
	ArrayList<HashMap<String,Integer>> downloadQ=new ArrayList<HashMap<String,Integer>>();
	// Used for when request play something before player service connect.
	int playRequest=-1;
//	int downloadingSubtitle=-1;
//	int downloadingMedia=-1;
	// String remoteSite[]=null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// try{
		super.onCreate(savedInstanceState);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
		if(runtime==null)runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);

		// fontSizeArraylength=getResources().getIntArray(R.array.fontSizeArray).length;
		setContentView(R.layout.main);

		Log.d(funcInto, "******* Into LamrimReader.onCreate *******");
		if(savedInstanceState!=null)Log.d(logTag, "The savedInstanceState is not null!");
		
		try {
			LogRepoter.setRecever(this,"http://10.0.200.156:8080/cylog/api/interface", 20120406002001L, "LamrimReader");
			LogRepoter.reportMachineType();
			LogRepoter.log("Initialing");
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	    
		if(powerManager==null){
			powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
	    	wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
	    }

		// remoteSite=getResources().getStringArray(R.array.remoteSite);
		
		if (fileSysManager == null){
			Log.d(logTag,"fileSysManager is NULL");
			fileSysManager = new FileSysManager(this);
		}

		if(mediaController==null){
//			mediaController=new MediaController(getApplicationContext());
//			mediaController=new MediaController(getParent());
			mediaController=new MediaController(LamrimReaderActivity.this);
			mediaController.setMediaPlayer(mediaPlayerControl);
			mediaController.setAnchorView(findViewById(R.id.rootLayout));
	     	mediaController.setEnabled(true);
		}
		
		// bookView.setMovementMethod(new ScrollingMovementMethod());
		// bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);

		if(toast==null)toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);

		if(subtitleView==null){
			subtitleView = (TextView) findViewById(R.id.subtitleView);
			subtitleView.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
					Log.d(logTag,v+" been clicked, Show media plyaer control panel.");
					showMediaPlayerController();
				}});
		}
		
		if(bookView==null){
			bookView = (ListView) findViewById(R.id.bookPageGrid);
//			adapter=getAllBookContent();
//			bookView.setAdapter(adapter);
		}
		if(progressBar==null){
			progressBar=(ProgressBar)findViewById(R.id.progressBar);
		}
		
		if(rootLayout==null){
			rootLayout=(LinearLayout) findViewById(R.id.rootLayout);
			rootLayout.setOnClickListener(new OnClickListener(){
				@Override
				public void onClick(View v) {
//					String s=(v.equals(subtitleView))?" is ":" is not ";
					Log.d(logTag,v+" been clicked, Show media plyaer control panel.");
					showMediaPlayerController();
				}
			});
		}
				
		// subtitleMonInterval=getResources().getInteger(R.integer.subtitleMonInterval);
/*		playBar = (SeekBar) findViewById(R.id.playBar);
		playBar.setEnabled(false);
		playBar.setClickable(false);
		playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

			@Override
			public void onProgressChanged(SeekBar playBar, int progress,
					boolean fromUser) {
				// For avoid the critical with subtitle player, we cancel the
				// subtitle player and reset the currentPlayedSubtitleIndex,
				// restart subtitle player while all ready.

				if (!fromUser)
					return;
				if (mediaPlayer != null && mediaPlayer.isPlaying())
					mediaPlayer.seekTo(progress);
				if (subtitle == null)
					return;

				final String[] hms = new String[3];
				int time = mediaPlayer.getCurrentPosition();
				hms[0] = "" + time / 3600000;
				if (hms[0].length() == 1)
					hms[0] = '0' + hms[0];
				time = time % 3600000;
				hms[1] = "" + time / 60000;
				time = time % 60000;
				hms[2] = "" + time / 1000;
				int ms = time % 1000;

				for (int i = 0; i < 3; i++)
					if (hms[i].length() == 1)
						hms[i] = '0' + hms[i];
				final String timeStr = hms[0] + ':' + hms[1] + ':' + hms[2]
						+ '.' + ms;

				toast.setText(timeStr);
				toast.show();

				playBar.setProgress(progress);
				synchronized (playBarTimer) {
					currentPlayedSubtitleIndex = subtitleBSearch(subtitle,progress);
					final char[] text = subtitle[currentPlayedSubtitleIndex].text.toCharArray();
					setSubtitleViewText(text);
				}
			}

*/		
		
        // wait for service create.
//        try {synchronized(mConnection){	mConnection.wait();}} catch (InterruptedException e) {e.printStackTrace();}
		Log.d(funcLeave, "******* onCreate *******");
		LogRepoter.log("Leave OnCreate");
	}
	
	private void initApp(){
		Log.d(funcInto,"**** initApp ****");
		playingMediaIndex=runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingIndex),playingMediaIndex);
		if(playingMediaIndex==-1){
			Log.d(logTag,"This is the first time launch LamrimReader, show select menu hint");
			setSubtitleViewText(getResources().getString(R.string.selectIndexFromMenuDesc));
			return;
		}

		Log.d(logTag,"Load history");
		loadRuntime();
//		bookView.setSelectionFromTop(theoryIndexShift, theoryIndexTop);
		Log.d(logTag,"Check is target media changed: newMediaTarget="+playingMediaIndex+", playingMediaIndex="+playingMediaIndex);
		if(enablePlayer){
//		if(mediaChanged){
			Log.d(logTag,"The media target has changed, Prepare media index "+playingMediaIndex);
//			playingMediaIndex=newMediaTarget;
//			playerStartPosition=newMediaPlayPosition;
//			saveRuntime();
			switchNormalMode();
//			Log.d(logTag,"Check is mediaPlayer is null, if not, show controller panel: "+(playerService.getMediaPlayer()==null));
//			if(playerService!=null && playerService.getMediaPlayer()!=null)showMediaPlayerController();
//		}
		}
		else{
			switchReadingMode();
		}
		Log.d(funcLeave,"**** initApp ****");
	}
	
	private void connectPlayer(){
		/** Defines callbacks for service binding, passed to bindService() */
		mConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName className,IBinder service) {
	            // We've bound to LocalService, cast the IBinder and get LocalService instance
				Log.d(logTag,"!!!! Player Service Connected !!!!");
				Log.d(logTag,"Check is all service has connected");
				
				playerBinder = (PlayerBinder) service;
				playerService = playerBinder.getService();
				// Load last status of media player
				setTheoryArea();
				
				// initial app after all services connected.
				if(downloaderService == null) return;
				else initApp();
			}
	        @Override
	        public void onServiceDisconnected(ComponentName arg0) {
	        	Log.d(logTag,"Into onServiceDisconnect.");
	        }};
	        
	        playIntent=new Intent(this, PlayerService.class);
	    try{
	    	if(playerBinder==null)
	    		getApplicationContext().bindService(playIntent, mConnection, Context.BIND_AUTO_CREATE);
		}catch(Exception e){e.printStackTrace();}
	}
	
	private void setTheoryArea(){
		if(playerService == null) return;
		
		bookList=playerService.getBookContent();
		adapter=new TheoryListAdapter(this, bookList, R.layout.theory_page_view, new String[] { "page", "desc" },	new int[] { R.id.pageContentView, R.id.pageNumView });
		bookView.setAdapter(adapter);
		adapter.notifyDataSetChanged();
		
	}
	
	private void setMediaPlayer(final int index){
		Log.d(funcInto,"**** setMediaPlayer ****");
		if(playerService == null )Log.d(logTag,"The playerService is null, skip create mediaplaye");
//		if(playerService.getMediaPlayer() == null)Log.d(logTag,"The mediaplayer is null, skip create mediaplayer");
		if(enablePlayer && playerService != null){
			Log.d(funcInto,"Create player.");
			playerService.createPlayer(index, playerStartPosition, onPreparedListener);
			subtitle=playerService.getSubtitle(index);
		}
		Log.d(funcLeave,"**** setMediaPlayer ****");
	}

	private void setUIContent(){
		Log.d(funcInto,"**** setUIContent ****");
		Log.d(logTag, "Into setUIContent for set UI content: playerService="+playerService+", onPreparedListener="+onPreparedListener);

		if(bookList == null)setTheoryArea();
		
		//Check is it initial state.
		if(playingMediaIndex<0)return;
			
		// The file not ready, wait for download.
//		preparePlay(playingMediaIndex,true);
		
//		if(enablePlayer)setMediaPlayer();
		Log.d(funcLeave,"**** setUIContent ****");
	}


	
	

	private void connectDownloader(){
		dlConn = new ServiceConnection() {
	        @Override
	        public void onServiceConnected(ComponentName className,IBinder service) {
	            // We've bound to LocalService, cast the IBinder and get LocalService instance
	        	Log.d(logTag,"!!!! Download Service conected !!!!");
	        	downloadBinder = (DownloaderBinder) service;
	            downloaderService = downloadBinder.getService();
	            downloaderService.setDownloadListener(downloadListener);
	            
	            //initial app after all services connected.
	            if(playerService == null) return;
				else initApp();
	            // Check is there has download request exist before connected.
/*	            if(downloadQ.size()>0){
	            	Log.d(logTag,"There are requests in download queue, add to service.");
	            	for(int i=0;i<downloadQ.size();i++){
	            		HashMap<String,Integer> hm=downloadQ.remove(0);
	            		downloaderService.addDownload((Integer)hm.get("index"),(Integer)hm.get("type"));
	            	}
	            	downloaderService.startDownload();	
	            }
*/	        }
	        
	        @Override
	        public void onServiceDisconnected(ComponentName arg0) {
	        	Log.d(logTag,"Into onServiceDisconnect.");
	        }};
	        
			downloadIntent=new Intent(this, DownloadService.class);
		try{
			if(downloaderService==null){
				Log.d(logTag,"Connecting to download Service.");
				getApplicationContext().bindService(downloadIntent, dlConn, Context.BIND_AUTO_CREATE);
				
			}
		}catch(Exception e){e.printStackTrace();}
	}
/*	private TheoryListAdapter getAllBookContent(){
		
		String[] bookPage = getResources().getStringArray(R.array.book);
		bookList = new ArrayList<HashMap<String, String>>();
		int pIndex = 0;

		for (String value : bookPage) {
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("page", value);
			item.put("desc", "第 " + (++pIndex) + " 頁");
			bookList.add(item);
		}
		return new TheoryListAdapter(this, bookList, R.layout.theory_page_view, new String[] { "page", "desc" },	new int[] { R.id.pageContentView, R.id.pageNumView }) ;
	}
*/
	class TheoryListAdapter extends SimpleAdapter{
		float textSize=0;
		public TheoryListAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
//			super.getView(position, convertView, parent);

			View row = convertView;
			if (row == null) {
				Log.d(logTag, "row=null, construct it.");
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.theory_page_view, parent,false);
			}

///			Log.d(logTag, "row=" + row+", ConvertView="+convertView);
			TheoryPageView bContent = (TheoryPageView) row.findViewById(R.id.pageContentView);
				//bContent.drawPoints(new int[0][0]);
			
			if(bookList==null)Log.d(logTag,"The bookList is null!!!");
			if(bContent.getTextSize()!=textSize)bContent.setTextSize(textSize);
			bContent.setText(bookList.get(position).get("page"));
//				bContent.setText(Html.fromHtml("<font color=\"#FF0000\">No subtitle</font>"));
			TextView pNum = (TextView)row.findViewById(R.id.pageNumView);
			if(pNum.getTextSize()!=textSize)pNum.setTextSize(textSize);
			pNum.setText(bookList.get(position).get("desc"));
				//pNum.setText("text");
//			Log.d(logTag, "Leave getView()");

			return row;
		}
			
		public void setTextSize(float size){
			textSize=size;
		}
	}

	private void updateTextSizeFromRuntimRecord(){
		int subtitleTextSize = getResources().getInteger(R.integer.defSubtitleFontSize);
		subtitleTextSize = runtime.getInt(getString(R.string.subtitleFontSizeKey), subtitleTextSize);
		int theoryTextSize=getResources().getInteger(R.integer.defBookFontSize);
		theoryTextSize=runtime.getInt(getString(R.string.bookFontSizeKey), theoryTextSize);
			
		updateTextSize(theoryTextSize,subtitleTextSize);
	}
	
	private void updateTextSize(final int theoryTextSize,final int subtitleTextSize){
		runOnUiThread(new Runnable() {
			public void run() {
				if(adapter != null)adapter.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[theoryTextSize]);
				bookView.destroyDrawingCache();
				if(adapter != null)adapter.notifyDataSetChanged();
			}
		});
		setSubtitleViewTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleTextSize]);
	}
	
	private void setSubtitleViewTextSize(final float size){
		runOnUiThread(new Runnable() {
			public void run() {
				subtitleView.setTextSize(size);
			}
		});
	}
	
	public int getSubtitleFontSize(){
		int subtitleFontSize = getResources().getInteger(R.integer.defSubtitleFontSize);
		return runtime.getInt(getString(R.string.subtitleFontSizeKey), subtitleFontSize);
	}
	/*
	 * Set the message on the subtitle view, there should check the subtitleView
	 * is not playing, or hide the message.
	 */
	private void setSubtitleViewText(String s) {
		setSubtitleViewText(s.toCharArray());
	}

	private void setSubtitleViewText(final char[] b) {
		runOnUiThread(new Runnable() {	public void run() {	subtitleView.setText(b, 0, b.length);	}});
	}
	
	private TranslateAnimation getShowControllerAnimation(){
		TranslateAnimation showController = new TranslateAnimation(
				Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF,1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f);
		showController.setDuration(100);
		return showController;
	}
	
	private TranslateAnimation getHideControllerAnimation(){
		TranslateAnimation hideController = new TranslateAnimation(
			Animation.RELATIVE_TO_SELF,0.0f,
			Animation.RELATIVE_TO_SELF,0.0f,
			Animation.RELATIVE_TO_SELF, 0.0f,
			Animation.RELATIVE_TO_SELF, 1.0f); 
		hideController.setDuration(100);
		return hideController;
	}
	
	synchronized private void showMediaPlayerController(){
		if(isFinishing()){
			Log.d(logTag,"The activity not prepare yet, skip show media controller.");
			return;
		}
		if(playerService==null){
			Log.d(logTag,"The Player Service not connect, skip show media controller.");
			return;
		}
		if(playerService.getMediaPlayer()==null){
			Log.d(logTag,"The Media Player not create yet, skip show media controller.");
			return;
		}
		if(mediaController.isShowing()){
			Log.d(logTag,"The controller has showing,, skip show media controller.");
			return;
		}
		
		Handler handler = new Handler();
		handler.post(new Runnable() {
			public void run() {
				mediaController=new MediaController(LamrimReaderActivity.this);
    			mediaController.setMediaPlayer(mediaPlayerControl);
    			mediaController.setAnchorView(findViewById(R.id.rootLayout));
    			mediaController.setEnabled(true);
    			mediaController.show();
            }
          });
/*		runOnUiThread(new Runnable() {
			public void run() {
				Log.d(logTag,"Show media controller.");
				mediaController.setAnimation(getShowControllerAnimation());
				try{
					mediaController.show();
				}catch(BadTokenException bte){
					bte.printStackTrace();
				}catch(IllegalStateException ise){
					ise.printStackTrace();
				}
			}
		});
		*/
	}
	
/*	private void switchMode(int mode) {
		final LinearLayout.LayoutParams mainLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.FILL_PARENT, 3);
		final LinearLayout.LayoutParams bottomLayout = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT, 0);

		switch (mode) {
		case NORMAL_MODE:
			Log.d(logTag, "Switch to Normal mode.");
			((LinearLayout) findViewById(R.id.rootLayout)).setGravity(Gravity.CENTER);
			Log.d(logTag, "Set book index: top="+theoryIndexTop+", shift="+theoryIndexShift);
			bookView.post(new Runnable() {	public void run() {

//			adapter.notifyDataSetChanged();
//			bookView.requestFocusFromTouch();
//			bookView.clearFocus();
			bookView.setSelectionFromTop(theoryIndexShift, theoryIndexTop);
//			bookView.setSelection(theoryIndexTop);
				
			}});
			subtitleView.setLayoutParams(bottomLayout);
			//subtitleView.setGravity(Gravity.CENTER | Gravity.BOTTOM);
			subtitleView.setGravity(Gravity.CENTER);
			subtitleView.setVisibility(View.VISIBLE);
			Log.d(logTag,"Call prepare mediaplayer from switchMode()");
			preparePlayer(playingMediaIndex);
			showMediaPlayerController();
			appMode = mode;
			break;
		case READING_MODE:
			Log.d(logTag, "Switch to Reading mode.");
			Log.d(logTag, "Set book index: top="+theoryIndexTop+", shift="+theoryIndexShift);
			bookView.post(new Runnable() {	public void run() {	
//				adapter.notifyDataSetChanged();

//				bookView.requestFocusFromTouch();
//				bookView.clearFocus();
				bookView.setSelectionFromTop(theoryIndexShift, theoryIndexTop);
//				bookView.setSelection(theoryIndexTop);
			}});
			subtitleView.setVisibility(View.GONE);
			releasePlayer();
			appMode = mode;
			break;
		case RECODE_SECTION_MODE:
			Log.d(logTag, "Switch to NoSubtitle mode.");
			// bookView.setLayoutParams(mainLayout);
			// bookView.setVisibility(View.VISIBLE);
			// subtitleView.setLayoutParams(bottomLayout);
			// subtitleView.setGravity(Gravity.CENTER|Gravity.BOTTOM);
			subtitleView.setVisibility(View.GONE);
			appMode = mode;
			break;
		case TV_MODE:
			Log.d(logTag, "Switch to TV mode.");
			// bookView.setLayoutParams(mainLayout);
			findViewById(R.id.bookPageGrid).setVisibility(View.GONE);
			subtitleView.setLayoutParams(mainLayout);
			subtitleView.setGravity(Gravity.CENTER);
			subtitleView.setVisibility(View.VISIBLE);
			appMode = mode;
			break;
		}
	}
*/	
		public void saveRuntime() {
			Log.d(funcInto,"**** saveRuntime ****");
		// Save status of the mode now in used.
		SharedPreferences.Editor editor = runtime.edit();
		
		int index = bookView.getFirstVisiblePosition();
		View v = bookView.getChildAt(0);
		int top = (v == null) ? 0 : v.getTop();
		int position=-1;

		
		editor.putBoolean(getString(R.string.enablePlayerKey), enablePlayer);
		editor.putInt(getResources().getString(R.string.runtimeNormalTheoryTop),top);
		editor.putInt(getResources().getString(R.string.runtimeNormalTheoryShift),index);		

		
		if(enablePlayer){
		if(playerService!=null && playerService.getMediaPlayer()!=null)
			position=playerService.getMediaPlayer().getCurrentPosition();
		else {
			Log.d(logTag,"Error: The media player is null, the media player shouldn't release before saveRuntime()");
			position=0;
		}
		editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex),playingMediaIndex);
		editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition),position);
		Log.d(logTag, "Save runtime status for Normal mode: Theory index: "+top+", Theory shift: "+index+", Player index: "+playingMediaIndex+", playerPosition="+ position);
		}
		
		
		/*editor.putInt(getString(R.string.appModeKey), appMode);
				switch (appMode) {
		 * 
		case NORMAL_MODE:
			// getTheoryIndex
			editor.putInt(getResources().getString(R.string.newMediaTarget),newMediaTarget);
			editor.putInt(getResources().getString(R.string.newMediaPlayPosition),newMediaPlayPosition);

			editor.putInt(getResources().getString(R.string.runtimeNormalTheoryTop),top);
			editor.putInt(getResources().getString(R.string.runtimeNormalTheoryShift),index);
			
			if(mediaPlayer!=null)
				position=mediaPlayer.getCurrentPosition();
			else {
				Log.d(logTag,"Error: The media player is null, the media player shouldn't release before saveRuntime()");
				position=0;
			}
			editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex),playingMediaIndex);
			editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition),position);
			Log.d(logTag, "Save runtime status for Normal mode: Theory index: "+top+", Theory shift: "+index+", Player index: "+playingMediaIndex+", playerPosition="+ position);

			break;
		case READING_MODE:
			// Get the theory index and save.
			editor.putInt(getResources().getString(R.string.runtimeReadingTheoryTop),top);
			editor.putInt(getResources().getString(R.string.runtimeReadingTheoryShift),index);
			Log.d(logTag, "Save runtime status for Reading mode: Theory index: "+top+", Theory shift: "+index);
			break;
		// case NOSUBTITLE_MODE:switchMode(NOSUBTITLE_MODE);break;
		case TV_MODE:
			if (mediaPlayer != null) {
				// editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex),
				// playingMediaIndex);
				// editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition),
				// mediaPlayer.getCurrentPosition());
			}
			break;
			*/
		editor.commit();
		Log.d(funcLeave,"**** saveRuntime ****");
	}
	
	public void loadRuntime() {
		Log.d(funcInto,"**** LoadRuntime ****");
		
		enablePlayer=runtime.getBoolean(getResources().getString(R.string.enablePlayerKey), true);
		theoryIndexTop=runtime.getInt(getResources().getString(R.string.runtimeNormalTheoryTop), 0);
		theoryIndexShift=runtime.getInt(getResources().getString(R.string.runtimeNormalTheoryShift), 0);
		if(enablePlayer){
			playerStartPosition = runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingPosition), 0);
			playingMediaIndex=runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingIndex),-1);
			Log.d(logTag, "Load back the runtime: enablePlayer="+enablePlayer+"Theory index: "+theoryIndexTop+", Theory shift: "+theoryIndexShift+", Player index: "+playingMediaIndex+", playerPosition="+ playerStartPosition);
		}

/*		switch (mode) {
		case NORMAL_MODE:
			// getTheoryIndex
			Log.d(logTag, "Load runtime status of Normal_Mode");
			
			theoryIndexTop=runtime.getInt(getResources().getString(R.string.runtimeNormalTheoryTop), 0);
			theoryIndexShift=runtime.getInt(getResources().getString(R.string.runtimeNormalTheoryShift), 0);
			playerStartPosition = runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingPosition), playerStartPosition);
			playingMediaIndex=runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingIndex),playingMediaIndex);
			Log.d(logTag, "Load back the runtime for NORMAL_MODE: Theory index: "+theoryIndexTop+", Theory shift: "+theoryIndexShift+", Player index: "+playingMediaIndex+", playerPosition="+ playerStartPosition);
			break;
		case READING_MODE:
			// Get the theory index and save.
			theoryIndexTop=runtime.getInt(getResources().getString(R.string.runtimeReadingTheoryTop), 0);
			theoryIndexShift=runtime.getInt(getResources().getString(R.string.runtimeReadingTheoryShift), 0);
			Log.d(logTag, "Load back the runtime for READING_MODE: Theory index: "+theoryIndexTop+", Theory shift: "+theoryIndexShift);
			break;
		case TV_MODE:
			break;
		}
*/		Log.d(funcLeave,"**** LoadRuntime. ****");
		}

	private int getSubtitleWordCountMax(TextView view) {
		// Determine how many words can show per line.
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int screenWidth = dm.widthPixels;
		int count = (int) ((float) screenWidth / view.getPaint().measureText("中"));
		Log.d(logTag, "Width of screen: " + screenWidth + ", Width of Word: "+ view.getPaint().measureText("中") + ", There are " + count+ " can show in one line.");
		return count;
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) { 
		Log.d(funcInto,"**** onCreateOptionMenu ****.");
		if(enablePlayer){
			Log.d(logTag,"Player is enable, show reading_mode menu.");
			menu.findItem(R.id.mode).setTitle(R.string.readingModeDesc);
//			inflater.inflate(R.menu.main_reading_mode, menu);
		}
		else {
			Log.d(logTag,"Player is disable, show normal_mode menu.");
			menu.findItem(R.id.mode).setTitle(R.string.normalModeDesc);
			//inflater.inflate(R.menu.main_normal_mode, menu);
		}
		return true;
	}

	private void switchNormalMode(){
		subtitleView=(TextView) findViewById(R.id.subtitleView);
		subtitleView.setVisibility(View.VISIBLE);
		if(playingMediaIndex==-1){
			Log.d(logTag,"**** The user not select a chapter yet! skip play! ****");
			setSubtitleViewText(getResources().getString(R.string.selectIndexFromMenuDesc));
			return;
		}
		setTitle(getString(R.string.app_name)+" - "+getResources().getStringArray(R.array.fileName)[playingMediaIndex]);
		Log.d(logTag,"Prepare media index "+playingMediaIndex);
		downloaderService.prepareFile(playingMediaIndex);
	}
	private void switchReadingMode(){
		subtitleView=(TextView) findViewById(R.id.subtitleView);
		subtitleView.setVisibility(View.GONE);
		playerService.releasePlayer();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(funcInto,"****OptionsItemselected, select item="+item.getItemId()+" ****");
		int selected=item.getItemId();
		
		switch (selected) {
			case R.id.fromTheoryMenu:
				saveRuntime();
				break;
			case R.id.fromSpeechMenu:
				saveRuntime();
				final Intent speechMenu = new Intent(LamrimReaderActivity.this,SpeechMenuActivity.class);
				startActivityForResult(speechMenu, SPEECH_MENU_RESULT);
				break;
			case R.id.mode:
				// Save play progress
				if(enablePlayer)saveRuntime();
				Log.d(logTag,"User select the Change Mode: title="+item.getTitle()+"is equal?"+item.getTitle().equals(getString(R.string.normalModeDesc)));
				Log.d(logTag,"Change enable mode, now="+enablePlayer);
				enablePlayer=(!enablePlayer);
				Log.d(logTag,"User select the Change Mode:: set enablePlayer="+enablePlayer);

				if(enablePlayer){
					Log.d(logTag,"Switch to play mode");
					loadRuntime();
					switchNormalMode();// There will happen error while show play controller on switch activity
//					saveRuntime();
				}
				else{
					Log.d(logTag,"Switch to reading mode");
					switchReadingMode();
//					saveRuntime();
				}
				break;
			case R.id.showCtrlPanel:
				saveRuntime();
				final Intent optCtrlPanel = new Intent(LamrimReaderActivity.this,OptCtrlPanel.class);
				startActivityForResult(optCtrlPanel, OPT_MENU_RESULT);
				break;
			case R.id.showAbout:
				saveRuntime();
				final Intent aboutPanel=new Intent(LamrimReaderActivity.this,AboutActivity.class);
				this.startActivity(aboutPanel);
				break;
//			default:
//				return super.onContextItemSelected(item);
	  }
		Log.d(funcLeave,"**** Into Options selected, select item="+item.getItemId()+" ****");
		return true;
	}
	
	private void onOptResultData(int resultCode, Intent intent){
		Log.d(funcInto, "onOptResultData");
		final int bookFontSize = intent.getIntExtra(getString(R.string.bookFontSizeKey), getResources().getInteger(R.integer.defBookFontSize));
		final int subtitleFontSize = intent.getIntExtra(getString(R.string.subtitleFontSizeKey),getResources().getInteger(R.integer.defSubtitleFontSize));
		
		SharedPreferences.Editor editor = runtime.edit();
		editor.putInt(getString(R.string.bookFontSizeKey), bookFontSize);
		editor.putInt(getString(R.string.subtitleFontSizeKey), subtitleFontSize);
		editor.commit();
		
		Log.d(logTag, "Get the book font size: " + bookFontSize	+ ", subtitleFontSize: " + subtitleFontSize);
		updateTextSizeFromRuntimRecord();
		
		
/*		runOnUiThread(new Thread() {
			@Override
			public void run() {
				Log.d(logTag,"Set book font size from "+((TheoryPageView) bookView.findViewById(R.id.pageContentView)).getTextSize()+" to "+getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
				  if(((TheoryPageView) bookView.findViewById(R.id.pageContentView)).getTextSize()!=bookFontSize){
					  Log.d(logTag,"Set book font size to "+getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
					  adapter.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
					  bookView.destroyDrawingCache();
					  adapter.notifyDataSetChanged();
				  }
				 if (subtitleView.getTextSize() != subtitleFontSize) {
					Log.d(logTag,"Set subtitle font size to "+ getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
					subtitleView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
				}
				if (subtitleView.getLineCount() != subtitleLineCount)
					subtitleView.setLines(subtitleLineCount);
			}
		});
*/		Log.d(funcLeave, "Leave onOptResultData");
	}

	private void onSpeechMenuResultData(int resultCode, Intent intent){
		Log.d(funcInto, "onSpeechMenuResultData");
		
		int selected=intent.getIntExtra("index", 0);
/*		if(selected==playingMediaIndex && playerService.getMediaPlayer() != null){
			Log.d(logTag, "The index of user selected is the same with playing one.="+selected);
			return;
		}
*/		
		
		mediaChanged=true;
		if(playBarTimer!=null){
			playBarTimer.cancel();
			playBarTimer=null;
		}
//		releasePlayer();  // playerService has check is the request index equal playing index
		playingMediaIndex = selected;
		
//		downloaderService.clearQueue();
//		downloaderService.
		saveRuntime();
		
		Log.d(logTag, "Get result from speech menu, select index="+selected);
		
/*		switch(lastAppMode){
		case NORMAL_MODE:
			int selected=intent.getIntExtra("index", 0);
			if(selected==playingMediaIndex&&mediaPlayer!=null&&mediaPlayer.isPlaying()){
				Log.d(logTag, "The index of user selected is the same with playing one.="+selected);
				return;
			}
			
			newMediaTarget = selected;
			newMediaPlayPosition = 0;
			saveRuntime();
			releasePlayer();
			Log.d(logTag, "Get result from speech menu, select index="+selected);
//			preparePlayer(selected);
			break;
		// Search the page of theory from speech.
		case READING_MODE:
			
			break;
		}
*/		Log.d(funcLeave, "Leave onSpeechMenuResultData");

	}
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		
		Log.d(funcInto, "**** Into onActivityResult: Get result from: "+requestCode+" ****");
		switch(requestCode){
		case SPEECH_MENU_RESULT:
			if(resultCode==RESULT_CANCELED)return;
			onSpeechMenuResultData(resultCode,intent);
			break;
		case THEORY_MENU_RESULT:break;
		case OPT_MENU_RESULT:onOptResultData(resultCode, intent);break;
		}
		
		Log.d(funcLeave, "Leave onActivityResult");
	}

	private static int subtitleBSearch(SubtitleElement[] a, int key) {
		int mid = a.length / 2;
		int low = 0;
		int hi = a.length;
		while (low <= hi) {
			mid = (low + hi) >>> 1;
			// final int d = Collections.compare(a[mid], key, c);
			int d = 0;
			if (mid == 0) {
				System.out.println("Shift to the index 0, return 0");
				return 0;
			}
			if (mid == a.length - 1) {
				System.out.println("Shift to the last element, return "
						+ (a.length - 1));
				return a.length - 1;
			}
			if (a[mid].startTimeMs > key && key <= a[mid + 1].startTimeMs) {
				d = 1;
				System.out.println("MID=" + mid + ", Compare " + a[mid].startTimeMs + " > " + key + " > " + a[mid + 1].startTimeMs + ", set -1, shift to smaller");
			} else if (a[mid].startTimeMs <= key
					&& key < a[mid + 1].startTimeMs) {
				d = 0;
				System.out.println("This should find it! MID=" + mid + ", "
						+ a[mid].startTimeMs + " < " + key + " > "
						+ a[mid + 1].startTimeMs + ", set 0, this should be.");
			} else {
				d = -1;
				System.out.println("MID=" + mid + ", Compare "
						+ a[mid].startTimeMs + " < " + key + " < "
						+ a[mid + 1].startTimeMs + ", set -1, shift to bigger");
			}
			if (d == 0)
				return mid;
			else if (d > 0)
				hi = mid - 1;
			else
				// This gets the insertion point right on the last loop
				low = ++mid;
		}
		String msg = "Binary search state error, shouldn't go to the unknow stage. this may cause by a not sorted subtitle: MID="
				+ mid+ ", Compare "+ a[mid].startTimeMs	+ " <> "+ key+ " <> " + a[mid + 1].startTimeMs + " into unknow state.";
		Log.e(logTag, msg, new Exception(msg));
		return -1;
	}
	
	private void startSubtitlePlayer(int index) {
		Log.d(funcInto,"**** startSubtitlePlayer ****");
		// Load the subtitle file, if not exist, show "no subtitle" on subtitle
		// view.
		// Before load subtitle, we should check is the subtitle has downloaded
		// and installed.
/*		Log.d(logTag,"Load subtitle index "+index);
		File subtitleFile = FileSysManager.getLocalSubtitleFile(index);
		if (!subtitleFile.exists() || (subtitle = Util.loadSubtitle(subtitleFile))==null) {
			subtitle = null;
			setSubtitleViewText(getResources().getString(R.string.noSubtitleDesc));
			return;
		}
*/
		
		if(subtitle==null){
			Log.e(logTag,"The subtitle has not initial yet!!!");
			return;
		}
	
		currentPlayedSubtitleIndex = subtitleBSearch(subtitle,playerStartPosition);
		final char[] text = subtitle[currentPlayedSubtitleIndex].text.toCharArray();
		setSubtitleViewText(text);
	
		if (playBarTimer == null)
			playBarTimer = new Timer();
		playBarTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				int mediaPosition = -1;
				try {
					if(playerService != null && playerService.getMediaPlayer() != null) mediaPosition = playerService.getMediaPlayer().getCurrentPosition();
				} catch (NullPointerException npe) {
					Log.e(logTag,"The media player has gone. it may happen on normal switch file or release stage.");
					npe.printStackTrace();
				}
//				playBar.setProgress(mediaPosition);
				synchronized (mediaController) {
					if (currentPlayedSubtitleIndex < subtitle.length - 1 && mediaPosition >= subtitle[currentPlayedSubtitleIndex + 1].startTimeMs) {
						final char[] text = subtitle[++currentPlayedSubtitleIndex].text.toCharArray();
						runOnUiThread(new Runnable() {
							public void run() {
								subtitleView.setText(text, 0, text.length);
							}
						});
					}
				}
			}
		}, 0, getResources().getInteger(R.integer.subtitleMonInterval));
		Log.d(funcLeave,"**** startSubtitlePlayer ****");
	}


/*	private void addDownloadRequest(int index,int type){
		Log.d(funcInto,"**** addDownloadRequest ****");
		if(downloaderService!=null){
			Log.d(logTag,"Download service connected, add request to download service.");
			downloaderService.addDownload(index, type);
			Log.d(logTag,"Call service download file.");
			downloaderService.startDownload();
		}
		else{
			// Service connection has disconnect, add the request to downloadQ and reconnect.
			Log.d(logTag,"Download service not connect, add request to download queue.");
			HashMap<String,Integer> hm=new HashMap<String,Integer>();
			hm.put("index", index);
			hm.put("type", getResources().getInteger(R.integer.SUBTITLE_TYPE));
			downloadQ.add(hm);
		}
		Log.d(funcLeave,"**** Leave addDownloadRequest ****");
	}
*/
/*	private synchronized boolean isFilesReadly(final int index) {
		if(index<0){
			Log.d(logTag,"LamrimReader.playAudio: Logical error, the request index is "+index);
		}
		Log.d(logTag,"Prepare media "+ getResources().getStringArray(R.array.fileName)[index] + " for play.");
		// Lock App for download thread until download finish and media player prepared.
		
		
/*		if(!FileSysManager.downloadSubtitleFromGoogle(index)){
			setSubtitleViewText("尚無字幕或下載失敗");
			subtitle=null;
		}else{
			setSubtitleViewText("字幕更新完成");
			Log.d(logTag,"Load subtitle index "+index);
			File subtitleFile = FileSysManager.getLocalSubtitleFile(index);
			if (subtitleFile.exists())
				subtitle = playerService.getSubtitle(index);
		}
*/
/*		boolean waitForDownload=false;
		if (!FileSysManager.isFileValid(index,getResources().getInteger(R.integer.SUBTITLE_TYPE))){
			addDownloadRequest(index, getResources().getInteger(R.integer.SUBTITLE_TYPE));
			//downloaderService.addDownload(index, getResources().getInteger(R.integer.SUBTITLE_TYPE));
			waitForDownload=true;
		}
		if (!FileSysManager.isFileValid(index,getResources().getInteger(R.integer.MEDIA_TYPE))) {
			// Here should show dialog to user that will be start download file.
			// from internet.
			Log.d(logTag, "The file is not exist or which alreadly in phone is not valid.");
//			setSubtitleViewText(getResources().getString(R.string.downloadingDesc));
			addDownloadRequest(index, getResources().getInteger(R.integer.MEDIA_TYPE));
			waitForDownload=true;
//			FileSysManager.downloadFileFromRemote(index,FileSysManager.MEDIA_FILE);
			Log.d(logTag, Thread.currentThread().getName()+ ": Go to terminate");
		}
		
		if(waitForDownload)return false;
		Log.d(logTag, "All data are valid!");

		/*
		 * FileInputStream fis1=null; try{
		 * fis1=openFileInput(getResources().getStringArray
		 * (R.array.fileName)[index]); }catch(FileNotFoundException fnfe){
		 * fnfe.printStackTrace(); }
		 * if(fis1==null)Log.d(logTag,"file not exist");
		 * 
		 * File file=new File("/sdcard/001A.MP3");
		 * Log.d(logTag,"Try to file the file from local file system");
		 * FileInputStream fis=getLocalFile(index); if(fis==null){ Log.d(logTag,
		 * "There is no such media in local file system, try to download.");
		 * fileSysManager.downloadFileFromRemote(index);
		 * subtitleView.setText(getString(R.string.downloadingDesc)); return; }
		 */

		// URLConnection con = url.openConnection();
		// con.connect();
		// con.getContent(); //This is needed or setDataSource will throw
		// IOException
		//
/*		File file = FileSysManager.getLocalMediaFile(index);

		playerService.createPlayer(playingMediaIndex, this.playerStartPosition, onPreparedListener);
		try {
			// FileInputStream fis=new FileInputStream(file);
			Log.d(logTag, "Play media: " + file.getAbsolutePath());
			// For mediaPlayer.getDuration(), while start mediaPlayer with
			// prepareAsync(), the time is not precision enough, we start with
			// create() function for this.
///			if (playerService.getMediaPlayer() == null){
//				playerService.getMediaPlayer() = MediaPlayer.create(getApplicationContext(), Uri.fromFile(file));
//				playerService.getMediaPlayer().setOnPreparedListener(onPreparedListener);
//			}
//			playerService.createPlayer(playingMediaIndex, playerStartPosition);
//			playerService.setOnPrepareListener(onPreparedListener);
		} catch (IllegalStateException ise) {
			ise.printStackTrace();
		}
		// catch(IOException ioe){ioe.printStackTrace();}
		catch (IllegalArgumentException iae) {
			iae.printStackTrace();
		}
*/		// catch (FileNotFoundException e) {e.printStackTrace();}
		// catch (IOException e) {e.printStackTrace();}

		// mMediaPlayer.setDataSource("http://lamrimreader.eyes-blue.com/appresources/100A.MP3");
		// mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

		// mediaPlayer.setOnPreparedListener(this);

//		mediaPlayer.setWakeMode(getApplicationContext(),PowerManager.SCREEN_BRIGHT_WAKE_LOCK);

		/*
		 * While mediaplayer start with create(), there is no need the
		 * onPrepared() function. mediaPlayer.setOnPreparedListener(new
		 * OnPreparedListener(){
		 * 
		 * 
		 * 
		 * 
		 * 
		 * @Override public void onPrepared(final MediaPlayer mp) {
		 * Log.d(logTag,"Into OnPrepared function");
		 * playBar.setMax(mp.getDuration()); mediaPlayer.start();
		 * playBar.setEnabled(true); playBar.setClickable(true);
		 * startSubtitlePlayer();
		 * 
		 * }
		 * 
		 * });
		 *

		return true;
	}
*/	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		Log.d(funcInto,"**** onDestroy ****");
		// TODO Auto-generated method stub
		if(playerService !=null && playerService.getMediaPlayer() != null)playerService.releasePlayer();
		
		try{
			if(mConnection!=null){
				playerService.stopSelf();
				getApplicationContext().unbindService(mConnection);
			}
			if(dlConn!=null){
				downloaderService.stopSelf();
				getApplicationContext().unbindService(dlConn);
			}
		}catch(IllegalArgumentException iae){iae.printStackTrace();
		}catch(RuntimeException re){re.printStackTrace();}
		Log.d(funcLeave,"**** onDestroy ****");
	}

	@Override
	public void onBackPressed() {
		Log.d(funcInto,"**** onBackPressed ****");
	
		Log.d(logTag,"Check status: playerService="+playerService+", playIntent="+playIntent+", playerBinder="+playerBinder+", playerConn="+mConnection);
		saveRuntime();
		
		if(playerService !=null){playerService.releasePlayer();playerService.stopSelf();}
		if(downloaderService != null)downloaderService.stopSelf();
		if(wakeLock.isHeld()){
			Log.d(logTag, "Release the screen lock");
			wakeLock.release();
		}
		Log.d(logTag,"Check status: playerService="+playerService+", playIntent="+playIntent+", playerBinder="+playerBinder+", playerConn="+mConnection);
		finish();
		Log.d(funcLeave,"**** onBackPressed ****");
	}

	protected void onStart() {
		super.onStart();
		Log.d(funcInto,"**** onStart() ****");
		FileSysManager.checkFileStructure();
		boolean initFromServiceConneted=false;
		
//		if(mediaController==null){
//			mediaController=new MediaController(getApplicationContext());
//			mediaController=new MediaController(getParent());
//			mediaController=new MediaController(this);
//			mediaController.setMediaPlayer(mediaPlayerControl);
//		}
		
		if(playerService == null){
			Log.d(logTag,"The player service not running, start connect to player.");
			connectPlayer();
			initFromServiceConneted=true;
		}
		
		if(downloaderService == null){
			Log.d(logTag,"The download service not running, start connect to downloader.");
			connectDownloader();
			initFromServiceConneted=true;
		}
		
		if(!initFromServiceConneted){
			Log.d(logTag,"All service are connected, start init app.");
			initApp();
		}
//		updateTextSizeFromRuntimRecord();
		Log.d(funcLeave,"**** onStart() ****");
	}
	
/*	protected void onSaveInstanceState (Bundle outState){
		super.onSaveInstanceState(outState);
		
		
	}
*/	/*
	 * protected void onRestart(){}
	 * 
	 * protected void onResume(){}
	 * 
	 * protected void onPause(){}
	 * 
	 * protected void onStop(){}
	 * 
	 * protected void onDestroy(){}
	 */
/*	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// land
		if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
		}
		// port
		else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
		}
	}
*/
	protected void onStop(){
		super.onStop();
/*	try{
		if(playerBinder != null )playerService.unbindService(mConnection);
	}catch(IllegalArgumentException iae){
		iae.printStackTrace();
	}
*/	}
	/*
	 * The function cause Thread hang on, it may not a correct version.
	 * 
	 * protected Dialog onCreateDialog(int id){
	 * if(dialogBuilder==null)dialogBuilder = new AlertDialog.Builder(this);
	 * AlertDialog dialog=null;
	 * 
	 * switch(id) { case DIALOG_DOWNLOAD_FAIL:
	 * dialogBuilder.setMessage(getString(R.string.downloadFail)); dialog =
	 * dialogBuilder.create(); break;
	 * 
	 * default: dialog = null; } return dialog; }
	 */
/*	private void releasePlayer() {
		Log.d(funcInto, "**** releasePlayer ****");
		if (playBarTimer != null) {
			playBarTimer.cancel();
			playBarTimer.purge();
			playBarTimer = null;
		}
		Log.d(logTag, "Stop media player.");
		if(playerService != null)playerService.releasePlayer();
	}
*/
	final protected MediaPlayerControl mediaPlayerControl = new MediaPlayerControl(){
		@Override
		public void pause() {
			if(playerService.getMediaPlayer()==null)return;

			if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
			playerService.getMediaPlayer().pause();
		}
		@Override
		public void seekTo(int pos) {
			if(playerService.getMediaPlayer()==null)return;
				
			playerService.getMediaPlayer().seekTo(pos);
			if(subtitle!=null){
				synchronized (mediaController) {
					currentPlayedSubtitleIndex = subtitleBSearch(subtitle,pos);
					final char[] text = subtitle[currentPlayedSubtitleIndex].text.toCharArray();
					setSubtitleViewText(text);
				}
			}
		}
			
		@Override
		public void start() {
			if(playerService.getMediaPlayer()==null)return;
				
			if(!wakeLock.isHeld()){Log.d(logTag,"Play media and Lock screen.");wakeLock.acquire();}
			if(subtitle!=null)startSubtitlePlayer(playingMediaIndex);
			playerService.getMediaPlayer().start();
		}
		@Override
		public int getBufferPercentage() {return 0;}
		@Override
		public int getCurrentPosition() {if(playerService.getMediaPlayer()==null)return 0;return playerService.getMediaPlayer().getCurrentPosition();}
		@Override
		public int getDuration() {
			if(playerService.getMediaPlayer()==null)return 0;
			return playerService.getMediaPlayer().getDuration();
		}

		@Override
		public boolean isPlaying() {
			if(playerService.getMediaPlayer()==null)return false;
			return playerService.getMediaPlayer().isPlaying();
		}
		@Override
		public boolean canPause() {return true;}
		@Override
		public boolean canSeekBackward() {return true;}
		@Override
		public boolean canSeekForward() {return true;}
	};
	
	
	final protected OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener(){
	@Override
	public void onAudioFocusChange(int focusChange) {
		
		// TODO Auto-generated method stub
		switch (focusChange) {
		// Gaint the audio device
		case AudioManager.AUDIOFOCUS_GAIN:
			if(playerService == null || playerService.getMediaPlayer()!=null && playerService.getMediaPlayer().getCurrentPosition()>0)
				playerService.getMediaPlayer().start();
			break;
		// lost audio focus long time, release resource here.
		case AudioManager.AUDIOFOCUS_LOSS:
			if(playerService != null && playerService.getMediaPlayer() != null)playerService.releasePlayer();
			break;
		// temporarily lost audio focus, but should receive it back shortly. You
		// must stop all audio playback, but you can keep your resources because
		// you will probably get focus back shortly
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
			if(playerService != null && playerService.getMediaPlayer() != null)playerService.getMediaPlayer().pause();
			break;
		// You have temporarily lost audio focus, but you are allowed to
		// continue to play audio quietly (at a low volume) instead of killing
		// audio completely.
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
			if(playerService != null && playerService.getMediaPlayer() != null)playerService.getMediaPlayer().pause();
			break;
		}
	}};

//	 class MediaPlayerOnPreparedListener implements OnPreparedListener{
	final protected OnPreparedListener onPreparedListener=new  OnPreparedListener(){
	public void onPrepared(MediaPlayer mp) {
		Log.d(funcInto,"**** Into onPreparedListener of MediaPlayer ****");
		if(!wakeLock.isHeld())wakeLock.acquire();
		AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
     	int result = audioManager.requestAudioFocus(audioFocusChangeListener ,AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
     	if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { // could not get audio focus.
     		setSubtitleViewText(getResources().getString(R.string.soundInUseError));
     		return;
     	}
     	
     	if(playerService == null){Log.d(logTag,"The media player service is null");return;}
     	if(playerService.getMediaPlayer() == null){Log.d(logTag,"The mediaPlayer is null");return;}
     	Log.d(logTag, "Media player prepared, bind control panel");
     	
     	if(playerService != null && playerService.getMediaPlayer() !=null )playerService.getMediaPlayer().setOnErrorListener(new MediaPlayer.OnErrorListener() {
     		@Override
     		public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
     			Log.d(logTag, "Error happen while play media");
     			try {
        					// 发生错误时也解除资源与MediaPlayer的赋值*
        					// mp.release();
        					// tv.setText("播放发生异常!");
     			} catch (Exception e) {
     				e.printStackTrace();
     			}
     			return false;
     		}
     	});
     	Log.e(logTag, "Prepare data");

        		// tx.setText("Playing audio...");

        		// The Duration list has error, must correct before use it.
        		// playBar.setMax(getResources().getIntArray(R.array.duration)[index]);

     	Log.d(logTag, "Play start positon = " + playerStartPosition);
     	if (playerStartPosition > 0) {
     		Log.d(logTag, "Media player seek to last play point "+ playerStartPosition);
     		playerService.getMediaPlayer().seekTo(playerStartPosition);
     	}
     	Log.d(logTag, "Set max of playBar: " + playerService.getMediaPlayer().getDuration());
//        		playBar.setMax(mediaPlayer.getDuration());
     	
/*     	int childCount=rootLayout.getChildCount();
     	Log.d(logTag,"There are "+childCount+" child of rootLayout");
     	if(childCount>0)
     		for(int i=0;i<childCount;i++){
     			if(mediaController.equals(rootLayout.getChildAt(i))){
     				Log.d(logTag,"Media controller has bind to the root layout");
     			}
     		}
     		
     	mediaController.setAnchorView(findViewById(R.id.rootLayout));
     	mediaController.setEnabled(true);
*/     	Log.d(logTag, "Call show control panel");
     	showMediaPlayerController();

        if(wakeLock.isHeld()){
        	Log.d(logTag, "Release the screen lock");
        	wakeLock.release();
        	}
        Log.d(funcLeave,"**** Leave onPreparedListener of MediaPlayer ****");
         }
         };
         
		final protected DownloadListener downloadListener=new DownloadListener(){
		 @Override
		public void downloadPreExec(int index,int resType){
			 progressBar.setProgress(0);
			if(resType == getResources().getInteger(R.integer.MEDIA_TYPE)){
				setSubtitleViewText("音檔下載中，請稍候");
			}
			else if(resType == getResources().getInteger(R.integer.SUBTITLE_TYPE)){
				setSubtitleViewText("字幕下載中，請稍候");
			}
			else if(resType == getResources().getInteger(R.integer.THEORY_TYPE)){
				setSubtitleViewText("論文下載中，請稍候");
			}
		 }
		 
		 @Override
		 public void setMaxProgress(long fileSize){
			 Log.d(logTag,"Set max value of progress bar: "+fileSize);
			 progressBar.setMax((int) fileSize);
		 }
		 
		 @Override
		 public void downloadFail(int index,int resType){
			if(resType == getResources().getInteger(R.integer.MEDIA_TYPE)){
				setSubtitleViewText(getResources().getString(R.string.downloadFail));
			}
			else if(resType == getResources().getInteger(R.integer.SUBTITLE_TYPE)){
				setSubtitleViewText("尚無字幕或下載失敗");
				subtitle=null;
			}
			else if(resType == getResources().getInteger(R.integer.THEORY_TYPE)){
			}
		 }

		@Override
		public void downloadFinish(int fileIndex,int resType){
			// Show message to user
			if(resType == getResources().getInteger(R.integer.MEDIA_TYPE)){
				Log.d(logTag,"Media file "+getResources().getStringArray(R.array.fileName)[fileIndex]	+ " Download finish, call LamrimReader.prepare("+ fileIndex + ") again");
				setSubtitleViewText("音檔下載完成");
			}
			else if(resType == getResources().getInteger(R.integer.SUBTITLE_TYPE)){
				Log.d(logTag,"Subtitle file "+getResources().getStringArray(R.array.fileName)[fileIndex]	+ " Download finish, Get subtitle now.");
				setSubtitleViewText("字幕更新完成");
			}
			else if(resType == getResources().getInteger(R.integer.THEORY_TYPE)){}
		 }
		
		 @Override
		public void setDownloadProgress(int value){
//			 Log.d(logTag,"Readed size: "+value);
			 // Show progress bar
			if(progressBar.getVisibility()==View.GONE)runOnUiThread(new Runnable() {
				public void run() {
					progressBar.setVisibility(View.VISIBLE);
				}});
			
//			int value = progressBar.getMax() / 100 * percent;
			progressBar.setProgress(value);
			if (value == progressBar.getMax()){
				runOnUiThread(new Runnable() {
				public void run() {
					progressBar.setProgress(0);
					progressBar.setVisibility(View.GONE);
				}});
				return;
			}
		 }
		 @Override
		 public void prepareFinish(int fileIndex){
			 Log.d(funcInto,"**** The target index ("+fileIndex+") has prepared ****");
			 
			 if(progressBar.getVisibility()==View.VISIBLE)runOnUiThread(new Runnable() {
					public void run() {
						progressBar.setVisibility(View.GONE);
					}});
			 if(!enablePlayer){
				 Log.d(logTag,"Activity stay in NORMAL Mode, skip create player.");
				 return;
			 }
			// if there is no subtitle file ready, set all subtitle object to null;
			if(!FileSysManager.isFileValid(fileIndex, getResources().getInteger(R.integer.SUBTITLE_TYPE))){
				Log.d(logTag,"Verify file again, the file is not valid, skip create player.");
				if(playBarTimer!=null){
					playBarTimer.cancel();
					playBarTimer=null;
				}
				subtitle=null;
			 }
			
			 
			 if(!FileSysManager.isFileValid(fileIndex, getResources().getInteger(R.integer.MEDIA_TYPE))){
				 Log.d(logTag,"Verify file again, the file is not valid, skip create player.");
				 return;
			 }
			 
			 Log.d(logTag,"Create player.");
			 setMediaPlayer(fileIndex);
			 Log.d(logTag,"Reset the subtitle view to default message.");
			 setSubtitleViewText(getString(R.string.app_name));
			 
			 Log.d(funcLeave,"**** Leave prepareFinish notify from downloader service. ****");
		 }
		 @Override
		 public void fileOperationFail(int i,int resType){}
	 };

}
