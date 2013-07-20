package eyes.blue;

import java.io.BufferedReader;


import android.net.Uri;
import android.os.SystemClock;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.GradientDrawable.Orientation;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.NinePatchDrawable;
import android.graphics.drawable.PaintDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
//import android.widget.MediaController;
import android.widget.Toast;
//import android.widget.MediaController.MediaPlayerControl;
/*
 * The class maintain the MediaPlayer, MediaPlayController and subtitle. There are many stage of MediaPlayer while play media, all stage maintain in the class, call functions of this function Instead of the functions of MediaPlayer,
 * Then you will get the better controller of MediaPlayer.
 * */
public class MediaPlayerController {
	final public static int MP_IDLE = 0; // after create()
	final public static int MP_INITED = 1; // after setDataSource()
	final public static int MP_PREPARING = 2; // prepare()
	final public static int MP_PREPARED = 3;
	final public static int MP_PLAYING = 4;
	final public static int MP_PAUSE = 5;
	int mpState = 0;
	
	LamrimReaderActivity activity=null;
	String logTag=null;
	MediaPlayer mediaPlayer=new MediaPlayer();
	MediaController mediaController=null;
	SubtitleTimer subtitleTimer=null;
	private PowerManager powerManager=null;
	private PowerManager.WakeLock wakeLock = null;
	MediaPlayerControllerListener changedListener=null;
	private SubtitleElement[] subtitle = null;
	Object playingIndexKey=new Object();
//	FileInputStream fis = null;
	int playingIndex=-2;
//	long monInterval=100;
	Toast toast = null;
	
	boolean isPlayRegion = false;
	int regionStartMs = -1;
	int regionEndMs = -1;

	/*
	 * Give The constructor the Activity and changedListener for build object. You can change the LamrimReaderActivity to your activity and modify the code of UI control to meet your logic. 
	 * */
	public MediaPlayerController(LamrimReaderActivity activity,MediaPlayerControllerListener changedListener){
		this.activity=activity;
		logTag=activity.getResources().getString(R.string.app_name);
		powerManager=(PowerManager) activity.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
		this.changedListener=changedListener;
		
		toast = Toast.makeText(activity, "", Toast.LENGTH_SHORT);
//		if(mediaPlayer==null)mediaPlayer=new MediaPlayer();
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				Log.d(logTag, "Error happen while play media");
					// 发生错误时也解除资源与MediaPlayer的赋值*
					// mp.release();
					// tv.setText("播放发生异常!");
				if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
				return false;
			}
		});
	}
	
// =============== Function implements of MediaPlayercontroller =================
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void pause() {
		if(subtitleTimer!=null)subtitleTimer.cancel(true);
		subtitleTimer=null;
		synchronized(mediaPlayer){
			mpState=MP_PAUSE;
			mediaPlayer.pause();
		}
		
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
	}
	
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer.
	 * */
	public void seekTo(int pos) {
		Log.d(logTag,"SeekTo function been call.");
		if(mpState<MP_PREPARED)return;
		
		if(subtitle==null){
			mediaPlayer.seekTo(pos);
			return;
		}
	
		int index=subtitleBSearch(subtitle, pos);
		if(index<0){
			changedListener.startMoment();
			mediaPlayer.seekTo(pos);
			return;
		}
		else if(index>subtitle.length-1){
			mediaPlayer.seekTo(pos);
			return;
		}

		playingIndex=index;
		mediaPlayer.seekTo(subtitle[playingIndex].startTimeMs);
		changedListener.onSeek(subtitle[playingIndex]);
		changedListener.onSubtitleChanged(subtitle[playingIndex]);
	}
	
	
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer. create new subtitleTimer for subtitle changed event.
	 * */
	public void start() {
		// It will play mp3 in Android 4.1 while screen blank. this line solve the problem
		// Not tested.
		if(!powerManager.isScreenOn())return;
		
		if(!wakeLock.isHeld()){Log.d(logTag,"Play media and Lock screen.");wakeLock.acquire();}
		if(subtitleTimer!=null){
			subtitleTimer.cancel(true);
			subtitleTimer=null;
		}
		if(subtitle!=null){
			Log.d(getClass().getName(),"The subtitle exist, prepare subtitle timer.");
			subtitleTimer = new SubtitleTimer();
			subtitleTimer.execute(subtitle);
		}
		
		if(isPlayRegion && regionStartMs != -1 && regionEndMs != -1){
			mediaPlayer.seekTo(regionStartMs);
			changedListener.startRegionPlay();
		}
		
		synchronized(mediaPlayer){
			mediaPlayer.start();
			mpState=MP_PLAYING;
		}
	}
	
	/*
	 * The reader get content from local drive, always return 0;
	 * */
	public int getBufferPercentage() {return 0;}
	/*
	 * Same as function of MediaPlayer but not throw IllegalStateException(return 0).
	 * */
	public int getCurrentPosition() {
		try{
		return mediaPlayer.getCurrentPosition();
		}catch(java.lang.IllegalStateException ise){
			return 0;
		}
	}
	/*
	 * Same as function of MediaPlayer but not throw IllegalStateException(return 0).
	 * */
	public int getDuration() {
		try{
			return mediaPlayer.getDuration();
		}catch(java.lang.IllegalStateException ise){
			return 0;
		}
	}

	/*
	 * Return is the MediaPlayer playing, but not throw IllegalStateException(return false).
	 * */
	public boolean isPlaying() {
		try{
			synchronized(mediaPlayer){
				return mediaPlayer.isPlaying();
			}
		} catch (IllegalStateException e) {
			return false;
		}
	}
	/*
	 * Always return true.
	 * */
	public boolean canPause() {return true;}
	
	/*
	 * Always return true.
	 * */
	public boolean canSeekBackward() {return true;}
	/*
	 * Always return true.
	 * */
	public boolean canSeekForward() {return true;}
// =================================================================

	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void reset(){
		if(subtitleTimer!=null)subtitleTimer.cancel(true);
		subtitleTimer=null;
		synchronized(mediaPlayer){
			Log.d("","============ Reset MediaPlayer ===============");
			mediaPlayer.reset();
			mpState=MP_IDLE;
		}
//		isPlayRegion=false;
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
	}
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void release(){
		if(subtitleTimer!=null)subtitleTimer.cancel(true);
		subtitleTimer=null;
		synchronized(mediaPlayer){
			Log.d("","============ Release MediaPlayer ===============");
			mediaPlayer.release();
			mpState=MP_IDLE;
		}
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
	}
	/*
	 * Return is the subtitle ready.
	 * */
	public boolean isSubtitleReady(){
		return (subtitle!=null);
	}
	/*
	 * Set data source of MediaPlayer, and parse the file of subtitle if exist.
	 * */
	public void setDataSource(Context context,int index) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
		final File subtitleFile=FileSysManager.getLocalSubtitleFile(index);
		File speechFile=FileSysManager.getLocalMediaFile(index);
		
		if(speechFile==null || !speechFile.exists()){
			Log.d(getClass().getName(),"setDataSource: The speech file not exist, skip!!!");
			return;
		}
		

		synchronized(mediaPlayer){
			mediaPlayer.setDataSource(context, Uri.fromFile(speechFile));
			//mediaPlayer.setDataSource(fis.getFD());
			mpState=MP_INITED;
		}
		
		
		if( subtitleFile==null ||  !subtitleFile.exists()){
			Log.d(getClass().getName(),"setDataSource: The speech or subtitle file not exist, skip!!!");
			subtitle=null;
			return;
		}
			
		Log.d(getClass().getName(),"The subtitle file exist, prepare the subtitle elements.");
		new Thread(new Runnable(){
			@Override
			public void run() {
				subtitle = loadSubtitle(subtitleFile);
				if(subtitle.length==0)subtitle=null;
		}}).run();

		
		
	}
	
	/*
	 * Set prepare the media of MediaPlayer, call the MediaPlayerControllerListener.onMediaPrepared when ready. remember the subtitle prepare at setDataSource stage.
	 * */
	public void prepareMedia() throws IllegalStateException, IOException{
		mediaPlayer.prepare();
	}
	
	/*
	 * Return the stage of MediaPlayer to avoid error.
	 * */
	public int getMediaPlayerState(){
		return mpState;
	}
	
	/*
	 * Show the floating player bar on activity, given the rootView of Activity.
	 * */
	synchronized public void showMediaPlayerController(View rootView) {
	//synchronized public void showMediaPlayerController(Activity rootView) {
		if (activity.isFinishing()) {
			Log.d(logTag,"The activity not prepare yet, skip show media controller.");
			return;
		}
		if (mediaController != null && mediaController.isShowing()) {
			Log.d(logTag,"The controller has showing, skip show media controller.");
			return;
		}

		if(mediaController == null){
		mediaController = new MediaController(activity);
		mediaController.setMediaPlayer(this);
		mediaController.setSubtitle(subtitle);
		
		mediaController.setPrevNextListeners(
				// Next button hit.
				new View.OnClickListener(){
					@Override
					public void onClick(View v) {
						onNextClick();
					}}
				,
				// Prev button hit.
				new View.OnClickListener(){

					@Override
					public void onClick(View v) {
						onPreviousClick();
					}}
		);
		mediaController.setAnchorView(rootView);
		mediaController.setEnabled(true);
		}
		activity.runOnUiThread(new Runnable() {
			public void run() {
				mediaController.show();
			}
		});
	}
	
	// ================================ Functions for region play ================================
	
	public void rewToLastSubtitle(){
		synchronized(playingIndexKey){
			int currentIndex=playingIndex-1;
			if(currentIndex<0){seekTo(subtitle[0].startTimeMs);}
			else seekTo(subtitle[currentIndex].startTimeMs);
		}
	}
	
	public void fwToNextSubtitle(){
		synchronized(playingIndexKey){
			int currentIndex=playingIndex+1;
			if(currentIndex>=subtitle.length){seekTo(subtitle[subtitle.length-1].startTimeMs);}
			else seekTo(subtitle[currentIndex].startTimeMs);
		}
	}
	
	private void onPreviousClick(){
		if(subtitle==null)return;
		
		// delay the hide time of controller.
		activity.runOnUiThread(new Runnable() {
			public void run() {
				mediaController.show();
			}
		});
		
		ImageButton ib= (ImageButton)mediaController.findViewById(R.id.prev);
		
		// Set or deSet
		if(regionStartMs!=-1){
			regionStartMs=-1;
			isPlayRegion = false;
			
			ib.setImageResource(R.drawable.ic_media_rew_d);
			changedListener.startRegionDeset(subtitle[playingIndex].startTimeMs);
			updateSeekBar();
			return;
		}
		if(regionEndMs!=-1 && subtitle[playingIndex].startTimeMs>regionEndMs){
			BaseDialogs.showToast(activity, "標記錯誤，開始標記大於結束標記");
			Log.d(logTag,"User operation error: the region start > region end!");
			return;
		}
		
		synchronized(playingIndexKey){
			regionStartMs=subtitle[playingIndex].startTimeMs;
		}
		

		if(regionStartMs != -1 && regionEndMs != -1)isPlayRegion = true;
		ib.setImageResource(R.drawable.ic_media_rew);
		changedListener.startRegionSeted(regionStartMs);

		updateSeekBar();
	}
	
	/*
	 * 
	 * 
	 * */
	private void onNextClick(){
		if(subtitle==null)return;
		
		// delay the hide time of controller.
		activity.runOnUiThread(new Runnable() {
			public void run() {
				mediaController.show();
			}
		});
		
		ImageButton ib= (ImageButton)mediaController.findViewById(R.id.next);
		
		// Set or deSet
		if(regionEndMs!=-1){
			regionEndMs=-1;
			isPlayRegion = false;
			
			ib.setImageResource(R.drawable.ic_media_ff_d);
			changedListener.endRegionDeset(subtitle[playingIndex].endTimeMs);
			updateSeekBar();
			return;
		}
		if(regionStartMs!=-1 && subtitle[playingIndex].endTimeMs<regionStartMs){
			BaseDialogs.showToast(activity, "標記錯誤，結束標記小於開始標記");
			Log.d(logTag,"User operation error: the region end  < region start !");
			return;
		}
		
		synchronized(playingIndexKey){
			regionEndMs=subtitle[playingIndex].endTimeMs;
		}
		
		if(regionStartMs != -1 && regionEndMs != -1)isPlayRegion = true;
		ib.setImageResource(R.drawable.ic_media_ff);
		changedListener.endRegionSeted(regionEndMs);
		updateSeekBar();
	}
	
	public static Bitmap getNinepatch(int id,int x, int y, Context context){
		//id is a resource id for a valid ninepatch

		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
		byte[] chunk = bitmap.getNinePatchChunk();
		NinePatchDrawable np_drawable = new NinePatchDrawable(context.getResources(),bitmap, chunk, new Rect(), null);
		np_drawable.setBounds(0, 0,x, y);

		Bitmap output_bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output_bitmap);
		np_drawable.draw(canvas);

		return output_bitmap;
	}

	Bitmap seekBarFgBmap = null, seekBarBgBmap = null;
	private void updateSeekBar(){
		Bitmap fgBmap=null, bgBmap=null;
		Canvas bgCanvas=null;
		
		final SeekBar sb=(SeekBar) mediaController.findViewById(R.id.mediacontroller_progress);
		final LayerDrawable layer = (LayerDrawable) sb.getProgressDrawable();
		Drawable drawableFg = (Drawable)layer.findDrawableByLayerId(android.R.id.progress);
		Drawable drawableBg = (Drawable)layer.findDrawableByLayerId(android.R.id.background);
		
		Log.d(logTag,"There are "+layer.getNumberOfLayers()+" layer in SeekBar object, forground: "+drawableFg+", background: "+drawableBg);
		
		final Rect fgBound=drawableFg.copyBounds();
		final Rect bgBound=drawableBg.copyBounds();
		
		// initial the background and foreground
		if(seekBarFgBmap == null){
			Log.d(logTag,"Initial the forground width="+fgBound.right+", height="+fgBound.bottom);
			seekBarFgBmap = getNinepatch(R.drawable.scrubber_primary_holo, fgBound.right, fgBound.bottom, activity);
			Log.d(logTag,"Initial the background width="+bgBound.right+", height="+bgBound.bottom);
			seekBarBgBmap = getNinepatch(R.drawable.scrubber_track_holo_light, bgBound.right, bgBound.bottom, activity);
		}
		
		// Release the segment select.
		if(regionStartMs==-1 && regionEndMs==-1 ){
			Log.d(logTag,"Release the region select mode.");
			BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), seekBarFgBmap);
			Drawable progress = new ClipDrawable(drawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
			progress.setBounds(0, 0, fgBound.right, fgBound.bottom);
			layer.setDrawableByLayerId(android.R.id.progress, progress);
			
			drawable = new BitmapDrawable(activity.getResources(), seekBarBgBmap);
			InsetDrawable background=  new InsetDrawable(drawable,5,5,5,5);
			background.setBounds(0, 0, bgBound.right, bgBound.bottom);
			layer.setDrawableByLayerId(android.R.id.background, background);
			
			sb.postInvalidate();

			int value=sb.getProgress();
			sb.setProgress(0);
			sb.setProgress(value);
			return;
		}

		// Add one pixel avoid while enableEnd = enableStart, there will throw exception while copy pixel.
		Log.d(logTag,"Debug: drawableFg: "+drawableFg+", copyBounds(): "+ drawableFg.copyBounds()+", getIntrinsicWidth: "+drawableFg.getIntrinsicWidth());
		float enableStart=((regionStartMs==-1)?0:(float)regionStartMs/mediaPlayer.getDuration()*fgBound.right);
		float enableEnd=((regionEndMs==-1)?fgBound.right:(float)regionEndMs/mediaPlayer.getDuration()*fgBound.right)+1;
		
		Log.d(logTag,"Create forground rec: width="+Math.round(enableEnd-enableStart)+", height="+fgBound.bottom);
		fgBmap=getNinepatch(R.drawable.scrubber_primary_holo, Math.round(enableEnd-enableStart),fgBound.bottom, activity);
		bgBmap=getNinepatch(R.drawable.scrubber_track_holo_light, bgBound.right,bgBound.bottom, activity);
		
//		Canvas fgCanvas=new Canvas(fgBmap);
		bgCanvas=new Canvas(bgBmap);
		
		Rect src = new Rect(0, 0, fgBmap.getWidth(), fgBmap.getHeight());
		Rect dst = new Rect(Math.round(enableStart), 0, Math.round(enableEnd), fgBound.bottom);
		
		bgCanvas.drawBitmap(fgBmap, src, dst, null);

		BitmapDrawable drawable = new BitmapDrawable(activity.getResources(), bgBmap);
		final Drawable progress = new ClipDrawable(drawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
		final InsetDrawable background=  new InsetDrawable(drawable,5,5,5,5);
		
		//LayerDrawable mylayer = new LayerDrawable(new Drawable[]{background,progress});
		//sb.setProgressDrawable(mylayer);
		Handler handler=new Handler();
		handler.post(new Runnable(){

			@Override
			public void run() {
				progress.setBounds(fgBound);
				background.setBounds(bgBound);
				layer.setDrawableByLayerId(android.R.id.background, background);
				layer.setDrawableByLayerId(android.R.id.progress, progress);
				
				sb.postInvalidate();
				int value=sb.getProgress();
				sb.setProgress(0);
				sb.setProgress(value);
			}
		});
		
	}
	
	public void desetPlayRegion(){
		Log.d(logTag,"Deset play region");
			regionStartMs=-1;
			regionEndMs=-1;
//			isPlayRegion = false;
	}
	
	public boolean isPlayRegion(){return isPlayRegion;}
	public boolean canPlayRegion(){return (regionStartMs>=0 && regionEndMs >0);}
	public void setPlayRegion(int startTimeMs,int endTimeMs){
		isPlayRegion=true;
		regionStartMs=startTimeMs;
		regionEndMs=endTimeMs;
		Log.d(logTag," Set play region: isPlayRegion="+isPlayRegion+", start="+regionStartMs+", end="+regionEndMs);
	}
	public int getRegionStartPosition(){
		return regionStartMs;
	}
	public int getRegionEndPosition(){
		return regionEndMs;
	}
	public SubtitleElement getSubtitle(int position){
		return subtitle[subtitleBSearch(subtitle, position)];
	}

	// ===========================================================================================
	
	public void showToastMsg(final String s){
		activity.runOnUiThread(new Runnable() {
			public void run() {
				toast.setText(s);
				toast.show();
			}
		});
	}
	
	final protected OnPreparedListener onPreparedListener = new OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			Log.d(getClass().getName(), "**** Into onPreparedListener of MediaPlayer ****");
			synchronized(mediaPlayer){
				mpState = MP_PREPARED;
			}
			
			AudioManager audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
			int result = audioManager.requestAudioFocus(
					audioFocusChangeListener, AudioManager.STREAM_MUSIC,
					AudioManager.AUDIOFOCUS_GAIN);
			
			// could not get audio focus.
			if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
				activity.setSubtitleViewText(activity.getResources().getString(R.string.soundInUseError));
				return;
			}

			Log.e(logTag, "Prepare data");
			changedListener.onMediaPrepared();
			Log.d(getClass().getName(),"**** Leave onPreparedListener of MediaPlayer ****");
		}
	};
	
	
	final protected OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {
		@Override
		public void onAudioFocusChange(int focusChange) {
			switch (focusChange) {
			// Gaint the audio device
			case AudioManager.AUDIOFOCUS_GAIN:
				/*if (getCurrentPosition() > 0)
					try {
						start();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				*/
				// mpController.start();
				break;
			// lost audio focus long time, release resource here.
			case AudioManager.AUDIOFOCUS_LOSS:
				Log.d("onAudioFocusChange",	"Loss of audio focus of unknown duration.");
				try {
					pause();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				// mpController.stopSubtitleTimer();
				break;
			// temporarily lost audio focus, but should receive it back shortly.
			// You
			// must stop all audio playback, but you can keep your resources
			// because
			// you will probably get focus back shortly
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				try {
					pause();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				// mpController.stopSubtitleTimer();
				break;
			// You have temporarily lost audio focus, but you are allowed to
			// continue to play audio quietly (at a low volume) instead of
			// killing
			// audio completely.
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
				try {
					pause();
				} catch (IllegalStateException e) {
					e.printStackTrace();
				}
				// mpController.stopSubtitleTimer();
				break;
			}
		}
	};
	
	
	private class SubtitleTimer extends AsyncTask<SubtitleElement, Void, Void> {
		protected Void doInBackground(SubtitleElement... se) {
			playingIndex=-2;
			int monInterval=activity.getResources().getInteger(R.integer.subtitleMonInterval);
			while(true){
				if(isCancelled())return null;	// playArrayIndex not last one
				try{
//					Log.d(getClass().getName(),"Subtitle index miss, search the index.");
					synchronized(playingIndexKey){
						int playPoint=mediaPlayer.getCurrentPosition();
						int playArrayIndex=subtitleBSearch(se, playPoint);
						
						Log.d(logTag,"check play status: isPlayRegion="+isPlayRegion+", region start="+regionStartMs+", region end="+regionEndMs+", play point="+playPoint);
						// Play region function has set, and over the region, stop play.
						if(isPlayRegion && regionStartMs != -1 && regionEndMs != -1 && playPoint > regionEndMs){
							Log.d(logTag,"Stop Play");
							pause();
							changedListener.stopRegionPlay();
							return null;
						}
						
						if(playingIndex!=playArrayIndex){
							playingIndex=playArrayIndex;
							if(playArrayIndex==-1){changedListener.startMoment();}
							else changedListener.onSubtitleChanged(subtitle[playArrayIndex]);
						}
					}
					// The last of subtitle has reached.
					//if(playArrayIndex==se.length-1)return null;
					
					Thread.sleep(monInterval);
					if(this.isCancelled())return null;
					
					}catch(IllegalStateException e) {
						e.printStackTrace();
						return null;
					}catch (InterruptedException e) {
						e.printStackTrace();
						return null;
					}
				}
		}
	}
	
	
		private static SubtitleElement[] loadSubtitle(File file) {
		ArrayList<SubtitleElement> subtitleList = new ArrayList<SubtitleElement>();
		try {
			System.out.println("Open " + file.getAbsolutePath()
					+ " for read subtitle.");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String stemp;
			int lineCounter = 0;
			int step = 0; // 0: Find the serial number, 1: Get the serial
							// number, 2: Get the time description, 3: Get
							// Subtitle
			int serial = 0;
			SubtitleElement se = null;

			while ((stemp = br.readLine()) != null) {
				lineCounter++;

				// This may find the serial number
				if (step == 0) {
					if (stemp.matches("[0-9]+")) {
						// System.out.println("Find a subtitle start: "+stemp);
						se = new SubtitleElement();
						serial = Integer.parseInt(stemp);
						step = 1;
					}
				}

				// This may find the time description
				else if (step == 1) {
					if (stemp.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")) {
						String[] region=stemp.split(" +-+> +");
						region[0]=region[0].trim();
						region[1]=region[1].trim();
						// System.out.println("Get time string: "+stemp);
						int timeMs;
						
						String ts = region[0].substring(0, 2);
						// System.out.println("Hour: "+ts);
						timeMs = Integer.parseInt(ts) * 3600000;
						ts = region[0].substring(3, 5);
						// System.out.println("Min: "+ts);
						timeMs += Integer.parseInt(ts) * 60000;
						ts = region[0].substring(6, 8);
						// System.out.println("Sec: "+ts);
						timeMs += Integer.parseInt(ts) * 1000;
						ts = region[0].substring(9, 12);
						// System.out.println("Sub: "+ts);
						timeMs += Integer.parseInt(ts);
						// System.out.println("Set time: "+startTimeMs);
						se.startTimeMs = timeMs;
						
						ts = region[1].substring(0, 2);
						// System.out.println("Hour: "+ts);
						timeMs = Integer.parseInt(ts) * 3600000;
						ts = region[1].substring(3, 5);
						// System.out.println("Min: "+ts);
						timeMs += Integer.parseInt(ts) * 60000;
						ts = region[1].substring(6, 8);
						// System.out.println("Sec: "+ts);
						timeMs += Integer.parseInt(ts) * 1000;
						ts = region[1].substring(9, 12);
						// System.out.println("Sub: "+ts);
						timeMs += Integer.parseInt(ts);
						se.endTimeMs = timeMs;
						step = 2;
					} else {
						// System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
						step = 0;
					}
				} else if (step == 2) {
					se.text = stemp;
					step = 0;
					subtitleList.add(se);
					System.out.println("get Subtitle: " + stemp);
					if (stemp.length() == 0)
						System.err
								.println("Load Subtitle: Warring: Get a Subtitle with no content at line "
										+ lineCounter);
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (SubtitleElement[]) subtitleList.toArray(new SubtitleElement[0]);
	}
	
	/*
	 * While start playing, there may not have subtitle yet, it will return -1, except array index n.
	 * */
	private int subtitleBSearch(SubtitleElement[] a, int key) {
		int mid = a.length / 2;
		int low = 0;
		int hi = a.length;
		while (low <= hi) {
			mid = (low + hi) >>> 1;
			// final int d = Collections.compare(a[mid], key, c);
			int d = 0;
			if (mid == 0) {
//				System.out.println("Shift to the index 0, Find out is -1(no subtitle start yet) or 0 or 1");
				if( key < a[0].startTimeMs ) return -1;
				if( a[1].startTimeMs <=  key) return 1;
				return 0;
			}
			if (mid == a.length - 1) {
//				System.out.println("Shift to the last element, return "	+ (a.length - 1));
				return a.length - 1;
			}
			if (a[mid].startTimeMs > key && key <= a[mid + 1].startTimeMs) {
				d = 1;
//				System.out.println("MID=" + mid + ", Compare " + a[mid].startTimeMs + " > " + key + " > " + a[mid + 1].startTimeMs + ", set -1, shift to smaller");
			} else if (a[mid].startTimeMs <= key
					&& key < a[mid + 1].startTimeMs) {
				d = 0;
//				System.out.println("This should find it! MID=" + mid + ", "						+ a[mid].startTimeMs + " < " + key + " > "						+ a[mid + 1].startTimeMs + ", set 0, this should be.");
			} else {
				d = -1;
//				System.out.println("MID=" + mid + ", Compare "						+ a[mid].startTimeMs + " < " + key + " < "						+ a[mid + 1].startTimeMs + ", set -1, shift to bigger");
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
		Log.e(getClass().getName(), msg, new Exception(msg));
		return -1;
	}
	
}
