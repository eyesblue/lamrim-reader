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

import eyes.blue.MediaControllerView.MediaPlayerControl;
import eyes.blue.modified.RegionableSeekBar;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.ProgressBar;
//import android.widget.MediaController;
import android.widget.Toast;
//import android.widget.MediaController.MediaPlayerControl;
/*
 * The class maintain the MediaPlayer, MediaPlayController and subtitle. There are many stage of MediaPlayer while play media, all stage maintain in the class, call functions of this function Instead of the functions of MediaPlayer,
 * Then you will get the better controller of MediaPlayer.
 * */
public class MediaPlayerController implements MediaControllerView.MediaPlayerControl{
	final public static int MP_IDLE = 0; // after create()
	final public static int MP_INITING = 1;
	final public static int MP_INITED = 2; // after setDataSource()
	final public static int MP_PREPARING = 3; // prepare()
	final public static int MP_PREPARED = 4;
	final public static int MP_PLAYING = 5;
	final public static int MP_PAUSE = 6;
	final public static int MP_COMPLETE = 7;
	int mpState = 0;
	
	SharedPreferences runtime = null;
	AudioManager audioManager=null;
	Activity activity=null;
	RegionableSeekBar seekBar=null;
	String logTag=null;
	FileSysManager fsm=null;
	MediaPlayer mediaPlayer=new MediaPlayer();
	MediaControllerView mediaController=null;
	SubtitleTimer subtitleTimer=null;
//	private PowerManager powerManager=null;
	private PowerManager.WakeLock wakeLock = null;
	MediaPlayerControllerListener changedListener=null;
	ComponentName remoteControlReceiver=null;
	private SubtitleElement[] subtitle = null;
	Object playingIndexKey=new Object();
	Object mediaPlayerKey=new Object();
//	FileInputStream fis = null;
	int playingIndex=-2, playPosition=0;
	Integer loadingMedia=-1;
//	long monInterval=100;
	Toast toast = null;
	int regionStartMs = -1;
	int regionEndMs = -1;
	ViewGroup anchorView=null;
	
//	VideoControllerView controller;
	public MediaPlayerController(LamrimReaderActivity activity, View anchorView, FileSysManager fsm, final MediaPlayerControllerListener changedListener){
		this(activity, anchorView);
		setChangeListener(changedListener);
		this.fsm=fsm;
	}
	public void setChangeListener(MediaPlayerControllerListener changedListener) {
		this.changedListener=changedListener;
	}
	/*
	 * Give The constructor the Activity and changedListener for build object. You can change the LamrimReaderActivity to your activity and modify the code of UI control to meet your logic. 
	 * */
	public MediaPlayerController(LamrimReaderActivity activity, View anchorView){
		this.activity=activity;
		this.anchorView=(ViewGroup) anchorView;
		logTag=activity.getResources().getString(R.string.app_name);
		PowerManager powerManager=(PowerManager) activity.getSystemService(Context.POWER_SERVICE);
		//wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
		wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "mpController@LamrimReader");
		
//		toast = Toast.makeText(activity, "", Toast.LENGTH_SHORT);
//		if(mediaPlayer==null)mediaPlayer=new MediaPlayer();
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener(){
			@Override
			public void onCompletion(MediaPlayer mp) {
				Log.d(logTag,"Media player play completion! release WakeLock.");
				if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
				synchronized(playingIndexKey){
					Log.d(logTag,"Set mpState to MP_COMPLETE.");
					mpState=MP_COMPLETE;
					if(subtitleTimer!=null){
						subtitleTimer.cancel(false);
						subtitleTimer=null;
					/*
					 * While user drag the seek bar indicator over end of media control view, there are 2 situation we don't want:
					 * 1. The MediaPlayer will stop play and reset the MediaPlay.currentPosition() to 0, then play media from 0ms in next play.
					 * 2. While many seek event fired shortly, The SeekBar don't flash UI to last seek point, it cause the indicator look like jump back to random position, this is the bug of SeekBar. 
					 * */
						int subIndex=subtitle[subtitle.length-1].startTimeMs;
						if(isRegionPlay())subIndex=regionEndMs;
						seekTo(subIndex);
					}
					Log.d(getClass().getName(),"Call changedListener.onComplatePlay()");
					changedListener.onComplatePlay();
				}
			}});
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
			@Override
			public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
				Log.d(logTag, "Error happen while play media");
					// 发生错误时也解除资源与MediaPlayer的赋值*
					// mp.release();
					// tv.setText("播放发生异常!");
				changedListener.onPlayerError();
				if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
				return false;
			}
		});

		// Fix the player bug of Android 4.4
//		mediaPlayer.setWakeMode(activity, PowerManager.FULL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE);
		
		mediaPlayer.setScreenOnWhilePlaying(true);
		
		mediaController = new MediaControllerView(activity);
		mediaController.setMediaPlayer(this);
		mediaController.setAnchorView(this.anchorView);
		mediaController.setEnabled(true);
		seekBar=mediaController.getSeekBar();
		// Use for static broadcast receiver - RemoteControlReceiver
		mpController=this;
		
	}

/*	public void setAnchorView(View view){
		mediaController.setAnchorView(view);
	}
	*/
// =============== Function implements of MediaPlayercontroller =================
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void pause() {
		if(subtitleTimer!=null)subtitleTimer.cancel(false);
		subtitleTimer=null;
		
		synchronized(mediaPlayerKey){
			if(mediaPlayer==null || mpState < MP_PREPARED)return;
			mpState=MP_PAUSE;
			mediaPlayer.pause();
		}
		
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
		changedListener.onPause();
	}
	
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer.
	 * */
	public synchronized void seekTo(int pos) {
		Log.d(logTag,Thread.currentThread().getName()+" SeekTo function: seek to position: "+pos+", duration="+mediaPlayer.getDuration());
		if(mediaPlayer==null)return;
		if(mpState<MP_PREPARED)return;
		
		if(subtitle==null){
			synchronized(mediaPlayerKey){
//				Log.d(logTag,"real set position to "+pos);
				mediaPlayer.seekTo(pos);
			}
			return;
		}
		
		// Check is the seek position over the start or end region.
		int index=Util.subtitleBSearch(subtitle, pos);
		if(index<0)index=0;

		if(regionStartMs!=-1 && subtitle[index].startTimeMs<regionStartMs){
			pos=regionStartMs;
			index=Util.subtitleBSearch(subtitle, regionStartMs);
			if(index==-1)index=regionStartMs;
		}
		else if(regionEndMs !=-1 && subtitle[index].endTimeMs>regionEndMs){
			pos=regionEndMs;
			index=Util.subtitleBSearch(subtitle, regionEndMs);
			if(index==-1)index=regionEndMs;
		}

		synchronized(mediaPlayerKey){
//			Log.d(logTag,Thread.currentThread().getName()+" Perform mediaPlayer.seekTo(pos) to "+pos);
			mediaPlayer.seekTo(pos);
		}
		
		synchronized(playingIndexKey){
			playingIndex=index;
			changedListener.onSeek(playingIndex, subtitle[playingIndex]);
			changedListener.onSubtitleChanged(playingIndex, subtitle[playingIndex]);
//			Log.d(logTag,"real set position to "+subtitle[playingIndex].startTimeMs);
		}			
	}
	
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer. create new subtitleTimer for subtitle changed event.
	 * */
	public void start() {
		// It will play mp3 in Android 4.1 while screen blank. this line solve the problem
		// Not tested.
//		if(!powerManager.isScreenOn())return;
		if(mediaPlayer==null)return;
		
		changedListener.onStartPlay();

		// Avoid some problem.
		if(mpState>=MP_PREPARED)
		synchronized(mediaPlayerKey){
			try{
				mediaPlayer.start();
				mpState=MP_PLAYING;
				
				if(subtitleTimer!=null){
					subtitleTimer.cancel(false);
					subtitleTimer=null;
				}
				
				if(subtitle!=null){
					Log.d(getClass().getName(),"The subtitle exist, prepare subtitle timer.");
					subtitleTimer = new SubtitleTimer();
					subtitleTimer.start();
				}
				if(playingIndex!=-1 && subtitle != null)
					mediaPlayer.seekTo(subtitle[playingIndex].startTimeMs);
				/*
				 * While user drag the seek bar over end of media control view, the control view hide and reset the
				 * MediaPlayer.currentPosition() to 0, it seems control by MediaPlayer, reset it to regionStartMs here. 
				 * */
				if(isRegionPlay() && mediaPlayer.getCurrentPosition() < regionStartMs){
					Log.d(getClass().getName(),"Reset the play start position to region start MS.");
					mediaPlayer.seekTo(regionStartMs);
//					changedListener.startRegionPlay();
				}
				if(!wakeLock.isHeld()){Log.d(logTag,"Play media and Lock screen.");wakeLock.acquire();}
			}catch(Exception e){
				// Stop the subtitle timer if start failure.
				if(subtitleTimer!=null){
					subtitleTimer.cancel(false);
					subtitleTimer=null;
				}
				
				changedListener.onPlayerError();
				e.printStackTrace();
				GaLogger.sendException("mpState="+mpState, e, true);
			}
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

		synchronized(mediaPlayerKey){
			try{
				return mediaPlayer.getCurrentPosition();
			}catch(Exception e){
				changedListener.onPlayerError();
				e.printStackTrace();
				GaLogger.sendException("mpState="+mpState, e, true);
			return 0;
			}
		}
	}
	/*
	 * Same as function of MediaPlayer but not throw IllegalStateException(return 0).
	 * */
	public int getDuration() {
		synchronized(mediaPlayerKey){
			try{
				// avoid initial problem.
				if(mediaPlayer == null)return 0;
				return mediaPlayer.getDuration();
			}catch(Exception e){
				changedListener.onPlayerError();
				e.printStackTrace();
				GaLogger.sendException("mpState="+mpState, e, true);
				return 0;
			}
		}
	}

	/*
	 * Return is the MediaPlayer playing, but not throw IllegalStateException(return false).
	 * */
	public boolean isPlaying() {
		try{
			synchronized(mediaPlayerKey){
				if(mediaPlayer==null)return false;
				return mediaPlayer.isPlaying();
			}
		} catch (Exception e) {
			e.printStackTrace();
			GaLogger.sendException(e, false);
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
	
	@Override
	public boolean isFullScreen() {
		Log.d(getClass().getName(),"isFullScreen been called.");
		return false;
	}

	@Override
	public void toggleFullScreen() {
		Log.d(getClass().getName(),"toggleFullScreen been called.");
	}
	
	public void	setOnRegionClick(View.OnClickListener listener){
		mediaController.setOnRegionListener(listener);
	}
	
	public void setOnReportClick(View.OnClickListener listener){
		mediaController.setOnReportListener(listener);
    }
	
// =================================================================
	
	// sometimes large memory objects may get lost.
	public boolean isPlayerReady(){
		Log.d(logTag,"mediaplayer="+mediaPlayer+", state="+MP_PREPARED);
		return (mediaPlayer!=null && mpState>=MP_PREPARED);
	}
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void reset(){
		// If already reset, skip it avoid the state exception(reset after reset).
		synchronized(mediaPlayerKey){
			if(mpState == MP_IDLE){
				Log.d(getClass().getName(),"Get reset after reset command, skip this reset command.");
				return;
			}
		}
		
		if(subtitleTimer!=null){
			subtitleTimer.cancel(false);
			subtitleTimer=null;
		}
		
		synchronized(mediaPlayerKey){
			Log.d("","============ Reset MediaPlayer ===============");
			try{
				mediaPlayer.reset();
				mpState=MP_IDLE;
				desetPlayRegion();
			}catch(Exception e){
				e.printStackTrace();
				changedListener.onPlayerError();
				GaLogger.sendException("mpState="+mpState, e, true);
			}
		}
		synchronized(playingIndexKey){
			playingIndex=0;
		}
		
/*		SeekBar sb=(SeekBar) mediaController.findViewById(R.id.mediacontroller_progress);
		if(sb!=null){
			sb.setProgress(0);
			updateSeekBar();
		}
*/
		// Can't update seekbar on this stage, because there is no information of duration, seekbar can't be create. put in onPrepared.
		//updateSeekBar();
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
	}
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	public void release(){
		if(subtitleTimer!=null)subtitleTimer.cancel(false);
		subtitleTimer=null;
			synchronized(mediaPlayerKey){
				if(mediaPlayer==null)return;
				Log.d("","============ Release MediaPlayer ===============");
				mediaPlayer.reset();
				mediaPlayer.release();
				mediaPlayer=null;
				mpState=MP_IDLE;
			}
		if(audioManager != null && remoteControlReceiver != null)audioManager.unregisterMediaButtonEventReceiver(remoteControlReceiver);
		if(audioManager != null && audioFocusChangeListener != null)audioManager.abandonAudioFocus(audioFocusChangeListener);
		
		if(wakeLock.isHeld()){Log.d(logTag,"Player paused, release wakeLock.");wakeLock.release();}
	}
	
	public void finish(){
		release();
		
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
		if(mediaPlayer==null)mediaPlayer=new MediaPlayer();
		final File subtitleFile=fsm.getLocalSubtitleFile(index);
		File speechFile=fsm.getLocalMediaFile(index);
		
		if(speechFile==null || !speechFile.exists()){
			Log.d(getClass().getName(),"setDataSource: The speech file not exist, skip!!!");
			Util.showErrorPopupWindow(activity, anchorView, speechFile+"音檔不存在！取消播放！");
			return;
		}

		// indicate that the media loading now.
		synchronized(loadingMedia){
			loadingMedia=index;
		}
		
		if( subtitleFile==null || !subtitleFile.exists()){
			Log.d(getClass().getName(),"setDataSource: The speech or subtitle file not exist, skip!!!");
			Util.showInfoPopupWindow(activity, anchorView, "字幕檔案不存在，取消字幕功能。");
			subtitle=null;
		}
		else{
			Log.d(getClass().getName(),"The subtitle file exist, prepare the subtitle elements.");
			subtitle = Util.loadSubtitle(subtitleFile);
			if(subtitle.length==0)subtitle=null;
		}
		
		Log.d(getClass().getName(),"MediaPlayer: Set data source to index: "+index);
		//Uri speechFileUri=Uri.fromFile(speechFile);
		
		synchronized(mediaPlayerKey){
			try{
				Log.d(logTag,"Set media player data source in stage: "+mpState+", file: "+ Uri.fromFile(speechFile));
				if(mpState != MP_IDLE)reset();
				mpState=MP_INITING;
				//mediaPlayer.setDataSource(context, speechFileUri);
				FileInputStream fis = new FileInputStream(speechFile);
				mediaPlayer.setDataSource(fis.getFD());
				mpState=MP_INITED;
				mediaPlayer.prepare();
			}catch(IOException ioe){
				Util.showErrorPopupWindow(activity, anchorView, "無法正常讀取音檔，請檢查音檔是否損毀或儲存空間已滿: "+speechFile);
				ioe.printStackTrace();
			}catch(Exception e){
				changedListener.onPlayerError();
				e.printStackTrace();
				GaLogger.sendException("mpState="+mpState+", mediaPlayer="+mediaPlayer+", context="+context+", speechFile="+speechFile+", is speech file exist="+((speechFile!=null)?speechFile.exists():"speechFile is null.")+", Uri="+speechFile.getAbsolutePath(), e, true);
				return;
			}
		}
	}
	

	public synchronized int getLoadingMediaIndex(){
		synchronized(loadingMedia){
			return loadingMedia;
		}
	}
	
	/*
	 * Return the stage of MediaPlayer to avoid error.
	 * */
	public synchronized int getMediaPlayerState(){
		synchronized(mediaPlayerKey){
			return mpState;
		}
	}
	
	/*
	 * The system will release MediaPlayer while resource low, the function create MediaPlayer object and seek to playPosition.
	 * */
	private void reloadMediaPlayer(){
		if(mediaPlayer==null){}
	}
	
	/*
	 * Show the floating player bar on activity, given the rootView of Activity.
	 * */
	synchronized public void showControllerView(Activity activity) {
	//synchronized public void showMediaPlayerController(Activity rootView) {
		if (activity.isFinishing()) {
			Log.d(logTag,"The activity not prepare yet, skip show media controller.");
			return;
		}
		if (mediaController == null) {
			Log.d(logTag,"The media player is null, skip show controller.");
			return;
		}
		
		if(mpState<MP_PREPARED){
			Log.d(logTag,"The media player loading source skip show controller.");
			return;
		}
		
/*		if(isPlayRegion=true &&	regionStartMs!=-1 && regionEndMs!=-1){
			ImageButton ibp= (ImageButton)mediaController.findViewById(R.id.prev);
			ibp.setImageResource(R.drawable.ic_media_rew);
			ImageButton ibn= (ImageButton)mediaController.findViewById(R.id.next);
			ibn.setImageResource(R.drawable.ic_media_ff);
			updateSeekBar();
		}*/
		
		Util.getRootView(activity).post(new Runnable() {
			public void run() {
//				mediaController.setAnchorView(anchorView);
//				updateSeekBar();
				mediaController.show();
//				if(regionStartMs!=-1 && regionEndMs!=-1)updateSeekBar();
			}
		});
	}
	
	public void hideMediaPlayerController(){
//		if(mediaController.isShowing())
			mediaController.hide();
			
	}
	
/*	public void refreshSeekBar(){
		Util.getRootView(activity).post(new Runnable() {
			public void run() {
				Log.d(getClass().getName(),"==========================Refeesh control panel.======================");
				mediaController.show(500);
				updateSeekBar();
//				mediaController.hide();
			}
		});
	}
*/	
	// ================================ Functions for region play ================================
	
	public void rewToLastSubtitle(){
		if(subtitle==null){
			Log.d(getClass().getName(),"Rew Click: subtitle is null");
			int pos = mediaPlayer.getCurrentPosition();
            pos -= 5000; // milliseconds
            if(pos<0)pos=0;
            mediaPlayer.seekTo(pos);
            reflashProgressView();
			return;
		}
		synchronized(playingIndexKey){
			Log.d(getClass().getName(),"Rew Click: playingIndex = "+playingIndex);
			int currentIndex=playingIndex-1;
			if(currentIndex>=subtitle.length)
				currentIndex=subtitle.length-1;
			else if(currentIndex<0)currentIndex=0;
			int i=subtitle[currentIndex].startTimeMs;
			seekTo(i);
		}
		reflashProgressView();
	}
	
	public void fwToNextSubtitle(){
		if(subtitle==null){
			int pos = mediaPlayer.getCurrentPosition();
            pos += 15000; // milliseconds
            if(pos>mediaPlayer.getDuration())// If next step over media duration, do nothing.
            	return;
            mediaPlayer.seekTo(pos);
            reflashProgressView();
			return;
		}
		synchronized(playingIndexKey){
			Log.d(getClass().getName(),"Fw Click: playingIndex = "+playingIndex);
			int currentIndex=playingIndex+1;
			if(currentIndex>=subtitle.length)
				currentIndex=subtitle.length-1;
			else if(currentIndex<0)currentIndex=0;
			int i=subtitle[currentIndex].startTimeMs;
			seekTo(i);
		}
		reflashProgressView();
	}
	
	public void reflashProgressView(){
		mediaController.setProgress();
	}
/*	
	public static Bitmap getNinepatch(int id,int x, int y, Context context){
		//id is a resource id for a valid ninepatch

		Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
		byte[] chunk = bitmap.getNinePatchChunk();
		//NinePatchDrawable np_drawable = new NinePatchDrawable(bitmap, chunk, new Rect(), null);
		NinePatchDrawable np_drawable = new NinePatchDrawable(context.getResources(),bitmap, chunk, new Rect(), null);
		np_drawable.setBounds(0, 0,x, y);

		Bitmap output_bitmap = Bitmap.createBitmap(x, y, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(output_bitmap);
		np_drawable.draw(canvas);

		return output_bitmap;
	}

	/*
	 * The function can't been call while mpState < MP_PREPARED, because the MediaPlayer.getDuration() will throw IllegalStateException.
	 * 
	boolean firstTimeCallUpdateSeekBar = true;
	private void updateSeekBar(){
		SeekBar sb=(SeekBar) mediaController.findViewById(R.id.mediacontroller_progress);
		LayerDrawable layer = (LayerDrawable) sb.getProgressDrawable();
		Drawable drawableFg = (Drawable)layer.findDrawableByLayerId(android.R.id.progress);
		Drawable drawableBg = (Drawable)layer.findDrawableByLayerId(android.R.id.background);
		
		Log.d(logTag,"There are "+layer.getNumberOfLayers()+" layer in SeekBar object, forground: "+drawableFg+", background: "+drawableBg);
		
		Rect fgBound=drawableFg.copyBounds();
		Rect bgBound=drawableBg.copyBounds();
		
		// The view never draw, skip draw.
		if(fgBound.height()==0){
			Log.d(logTag,"The seekbar not layouted, skip");
			return ;
		}
		
		if(mpState<MP_PREPARED){
			Log.d(logTag,"updateSeekBar: The player not set data yet, skip.");
			return;
		}
		
		Log.d(logTag,"forgound: bound.right="+fgBound.right+", bound.left="+fgBound.left+", bound.top="+fgBound.top+", bound.botton="+fgBound.bottom+", IntrinsicWidth="+drawableFg.getIntrinsicWidth()+",IntrinsicHeight= "+drawableFg.getIntrinsicHeight()+", rect.height="+fgBound.height()+", rect.width="+fgBound.width());
		Log.d(logTag,"backgound: bound.right="+bgBound.right+", drawableFg.getIntrinsicWidth="+drawableBg.getIntrinsicWidth());
		// Release the segment select.
		if(regionStartMs==-1 && regionEndMs==-1 ){
			Log.d(logTag,"Release the region select mode.");
			Bitmap seekBarFgBmap = getNinepatch(R.drawable.scrubber_primary_holo, fgBound.width(), fgBound.height(), activity);
			BitmapDrawable fgDrawable = new BitmapDrawable(activity.getResources(), seekBarFgBmap);
			ClipDrawable progress = new ClipDrawable(fgDrawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
			progress.setBounds(fgBound);
			layer.setDrawableByLayerId(android.R.id.progress, progress);
			
			Bitmap seekBarBgBmap = getNinepatch(R.drawable.scrubber_track_holo_dark, bgBound.width(), bgBound.height(), activity);
			BitmapDrawable bgDrawable = new BitmapDrawable(activity.getResources(), seekBarBgBmap);
			InsetDrawable background=  new InsetDrawable(bgDrawable,0);
			background.setBounds(bgBound);
			layer.setDrawableByLayerId(android.R.id.background, background);
			
			sb.postInvalidate();
			int value=sb.getProgress();
			sb.setProgress(0);
			sb.setProgress(value);
			return;
		}
		
		Log.d(logTag,"Debug: drawableFg: "+drawableFg+", copyBounds(): "+ drawableFg.copyBounds()+", getIntrinsicWidth: "+drawableFg.getIntrinsicWidth());
		
		//int seekBarStartPosition=Math.round ((regionStartMs==-1)?fgBound.left:(float)regionStartMs/mediaPlayer.getDuration()*fgBound.width());
		int seekBarStartPosition=Math.round ((regionStartMs==-1)?fgBound.left:(float)regionStartMs/mediaPlayer.getDuration()*fgBound.width());
		// Add one pixel avoid while enableEnd = enableStart, there will throw exception while copy pixel.
		int seekBarEndPosition=Math.round (((regionEndMs==-1)?bgBound.right:(float)regionEndMs/mediaPlayer.getDuration()*bgBound.width())+1);
		Log.d(logTag,"Set start pixel and end pixel: regionStartMs="+regionStartMs+", seekBarStartPosition="+seekBarStartPosition+", regionEndMs="+regionEndMs+", seekBarEndPosition="+seekBarEndPosition);
		
		
		Log.d(logTag,"Create forground rec: width="+(seekBarEndPosition-seekBarStartPosition)+", height="+fgBound.bottom);
		//fgBmap=getNinepatch(R.drawable.scrubber_primary_holo, seekBarEndPosition-seekBarStartPosition,fgBound.bottom, activity);
		Bitmap fgBmap=getNinepatch(R.drawable.scrubber_primary_segment_mode, bgBound.width(), bgBound.height(), activity);
		Log.d(logTag,"Create background rec: width="+bgBound.right+", height="+bgBound.bottom);
		Bitmap bgBmap=getNinepatch(R.drawable.scrubber_track_holo_dark, bgBound.width(), bgBound.height(), activity);
		
		//fgBmap.setDensity()
//		Canvas fgCanvas=new Canvas(fgBmap);
		Canvas bgCanvas=new Canvas(bgBmap);

		
		Log.d(logTag,"Copy forground rect to background: x1="+seekBarStartPosition+", y1="+ bgBound.top+", x2="+ seekBarEndPosition+", y2="+ bgBound.bottom);
		//Rect src = new Rect(0, 0, fgBmap.getWidth(), fgBmap.getHeight());
//		int len=seekBarEndPosition-seekBarStartPosition;
//		int h=fgBound.height();
//		int[] colors=new int[len*h];
		
//		fgBmap.getPixels(colors, 0, len, seekBarStartPosition, 0, len, h);
//		      getPixels (int[] pixels, int offset, int stride, int x, int y, int width, int height)
//		bgBmap.setPixels(colors, 0, len, seekBarStartPosition, 0, len, h);
		Rect src = new Rect(seekBarStartPosition, 0, seekBarEndPosition, fgBound.bottom);
		Rect dst = new Rect(seekBarStartPosition, 0, seekBarEndPosition, fgBound.bottom);

		//bgCanvas.drawBitmap(fgBmap, src, dst, new Paint());
//		bgCanvas.setDensity(Bitmap.DENSITY_NONE);
//		fgBmap.setDensity(Bitmap.DENSITY_NONE);
		bgCanvas.drawBitmap(fgBmap, dst, dst, null);
		
		Drawable drawable = new BitmapDrawable(activity.getResources(), bgBmap);
		ClipDrawable progress = new ClipDrawable(drawable, Gravity.AXIS_PULL_BEFORE, ClipDrawable.HORIZONTAL);
		InsetDrawable background=  new InsetDrawable(drawable,0);
		
		progress.setBounds(fgBound);
		background.setBounds(bgBound);
		
		LayerDrawable mylayer = new LayerDrawable(new Drawable[]{background,progress});
		mylayer.setId(0, android.R.id.background);
		mylayer.setId(1, android.R.id.progress);
		sb.setProgressDrawable(mylayer);

		//progress.setBounds(fgBound);
		//background.setBounds(bgBound);
		//layer.setDrawableByLayerId(android.R.id.background, background);
		//layer.setDrawableByLayerId(android.R.id.progress, progress);
		sb.postInvalidate();
		int value=sb.getProgress();
		sb.setProgress(0);
		sb.setProgress(value);
		
		// Avoid the seekbar bug.
		if(firstTimeCallUpdateSeekBar){
			firstTimeCallUpdateSeekBar=false;
			updateSeekBar();
		}
	}
*/	
	public void setPrevNextListeners(OnClickListener prev, OnClickListener next){
		mediaController.setPrevNextListeners(prev, next);
	}
	
	public void setPlayRegionStartMs(int startMs){
		// Clear region start time.
		if(startMs<0){
			regionStartMs=startMs;
			return;
		}
		// If subtitle not exist, then set the time directly.
		if(subtitle==null){
			synchronized(playingIndexKey){
				regionStartMs=startMs;
			}
			return;
		}
		// If subtitle exist, set the time to the start time of the subtitle.
		int subIndex=timeMsToSubtitleIndex(startMs);
		synchronized(playingIndexKey){
			regionStartMs=subtitle[subIndex].startTimeMs;
		}
		
		seekBar.setRegionStart(regionStartMs, mediaPlayer.getDuration());
		seekBar.postInvalidate();
	}
	public void setPlayRegionEndMs(int endMs){
		// Clear region start time.
		if(endMs<0){
			regionEndMs=endMs;
			return;
		}
		// If subtitle not exist, then set the time directly.
		if(subtitle==null){
			synchronized(playingIndexKey){
				regionEndMs=endMs;
			}
			return;
		}
		// If subtitle exist, set the time to the end time of the subtitle.
		int subIndex=timeMsToSubtitleIndex(endMs);
		synchronized(playingIndexKey){
			regionEndMs=subtitle[subIndex].endTimeMs;
		}

		seekBar.setRegionEnd(regionEndMs, mediaPlayer.getDuration());
		seekBar.postInvalidate();
	}
	
	public void setPlayRegion(int startTimeMs,int endTimeMs){
		setPlayRegionEndMs(endTimeMs);
		setPlayRegionStartMs(startTimeMs);

//		seekBar.setRegionMode(startTimeMs, endTimeMs, mediaPlayer.getDuration());
		Log.d(logTag," Set play region: isPlayRegion="+isRegionPlay()+", start="+regionStartMs+", end="+regionEndMs);
	}
	
	public void desetPlayRegion(){
		Log.d(logTag,"Deset play region");
		regionStartMs=-1;
		regionEndMs=-1;
		seekBar.disableRegionMode();
		seekBar.postInvalidate();
	}
	
//	public boolean isPlayRegion(){return (mpState==MP_PLAYING && canPlayRegion());}
	public boolean isRegionPlay(){
//		Log.d(getClass().getName(),Thread.currentThread().getName()+": Region start="+regionStartMs+", end="+regionEndMs);
		return (regionStartMs>=0 && regionEndMs >=0);
	}
	
	public int getRegionStartPosition(){
		return regionStartMs;
	}
	public int getRegionEndPosition(){
		return regionEndMs;
	}
	public SubtitleElement getSubtitle(int time){
		int index=Util.subtitleBSearch(subtitle, time);
		if(index<0)index=0;
		return subtitle[index];
	}
	public int getSubtitleIndex(int time){
		return Util.subtitleBSearch(subtitle, time);
	}
	
	public ViewGroup getControllerView(){
		return mediaController.getControllerView();
	}
	
	private int timeMsToSubtitleIndex(int timeMs){
		int subIndex = Util.subtitleBSearch(subtitle, timeMs);
		if(subIndex<0)subIndex=0;
		return subIndex;
	}

	// ===========================================================================================
	
/*	private void showToastMsg(final String s){
		activity.runOnUiThread(new Runnable() {
			public void run() {
				toast.setText(s);
				toast.show();
			}
		});
	}
*/	
	final protected OnPreparedListener onPreparedListener = new OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			Log.d(getClass().getName(), "**** Into onPreparedListener of MediaPlayer ****");
			synchronized(mediaPlayerKey){
				mpState = MP_PREPARED;
			}
			
			audioManager = (AudioManager) activity.getSystemService(Context.AUDIO_SERVICE);
			int result = audioManager.requestAudioFocus(audioFocusChangeListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
			
			// could not get audio focus.
			if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
///				activity.setSubtitleViewText(activity.getResources().getString(R.string.soundInUseError));
				changedListener.getAudioFocusFail();
				return;
			}

			remoteControlReceiver=new ComponentName(activity,RemoteControlReceiver.class.getName());
			audioManager.registerMediaButtonEventReceiver(remoteControlReceiver);
			
			Log.d(logTag, "Prepare data");
			changedListener.onMediaPrepared();
			
			loadingMedia=-1;
			Log.d(getClass().getName(),"**** Leave onPreparedListener of MediaPlayer ****");
		}
	};
	
	
	final protected OnAudioFocusChangeListener audioFocusChangeListener = new OnAudioFocusChangeListener() {
		int lastState=-1;
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
				Log.d(getClass().getName(),"Got audio focus.");
				if(lastState == MP_PLAYING)
					start();
				lastState=-1;
				break;
			// lost audio focus long time, release resource here.
			case AudioManager.AUDIOFOCUS_LOSS:
				Log.d("onAudioFocusChange",	"Loss of audio focus of unknown duration.");
				try {
					pause();
					//release();
				} catch (IllegalStateException e) {
					GaLogger.sendException("AudioFocusChangeListener.AUDIOFOCUS_LOSS", e, false);
					e.printStackTrace();
				}
				// mpController.stopSubtitleTimer();
				break;
			// temporarily lost audio focus, but should receive it back shortly.
			// You must stop all audio playback, but you can keep your resources
			// because you will probably get focus back shortly
			case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
				try {
					lastState=getMediaPlayerState();
					pause();
				} catch (IllegalStateException e) {
					GaLogger.sendException("AudioFocusChangeListener.AUDIOFOCUS_LOSS_TRANSIENT", e, false);
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
					lastState=getMediaPlayerState();
					pause();
				} catch (IllegalStateException e) {
					e.printStackTrace();
					GaLogger.sendException("AudioFocusChangeListener.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK", e, false);
				}
				// mpController.stopSubtitleTimer();
				break;
			}
		}
	};
	
	static MediaPlayerController mpController=null;
	static public class RemoteControlReceiver extends BroadcastReceiver {
		static int key_actionDown=-1;
	    @Override
	    public void onReceive(Context context, Intent intent) {
	    	if(mpController==null)return;
	    	// If the key is not group of ACTION_MEDIA_BUTTON skip.
	    	if (!Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) return;
	    	
	    	Log.d(getClass().getName(),"Get a Receive Brocast key event: The state of MediaPlayer is "+mpController.getMediaPlayerState());
	    	KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	        KeyEvent Xevent = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
	        
	        if(event == null || Xevent == null){
	        	GaLogger.sendException("MediaPlayerRemoteController get key event but event is null.", new NullPointerException(), true);
	        	return;
	        }
	        
	        // If the event is key down, record the code to key_actionDown then skip.
		    if(Xevent.getAction() == KeyEvent.ACTION_DOWN){
		    	Log.d(getClass().getName(),"Receive Brocast: get "+event.getKeyCode()+" key down.");
		    	key_actionDown=event.getKeyCode();
		    	return;
		    }
		    
		    // If there is a key up event but there is no the key down before, skip.
		    if(Xevent.getAction() == KeyEvent.ACTION_UP && event.getKeyCode() != key_actionDown)return;
		    Log.d(getClass().getName(),"Receive Brocast: get "+event.getKeyCode()+" key up.");
	            if (event.getKeyCode() == KeyEvent.KEYCODE_MEDIA_PLAY) {
	                Log.d(getClass().getName(),"Receive Brocast: get MEDIA_PLAY key.");
	                mpController.start();
	                return;
	            }
	            else if (KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE == event.getKeyCode() || event.getKeyCode() == KeyEvent.KEYCODE_HEADSETHOOK) {
	                Log.d(getClass().getName(),"Receive Brocast: get MEDIA_PLAY_PAUSE key.");
	            	if(mpController.getMediaPlayerState()==MP_PLAYING){
	            		mpController.pause();
	            		return;
	            	}else if(mpController.getMediaPlayerState()==MP_PREPARED || mpController.getMediaPlayerState()==MP_PAUSE){
	            		mpController.start();
	            		return;
	                }
	            	Log.d(getClass().getName(),"The MediaPlayer not stay in can PLAY/PAUSE state.");
	            }
	            else if(KeyEvent.KEYCODE_MEDIA_PREVIOUS == event.getKeyCode()) {
	            	Log.d(getClass().getName(),"Receive Brocast: get MEDIA_PREVIOUS key.");
            		mpController.fwToNextSubtitle();
            		mpController.reflashProgressView();
	            	return;
	            }
	            else if(KeyEvent.KEYCODE_MEDIA_NEXT == event.getKeyCode()) {
	            	Log.d(getClass().getName(),"Receive Brocast: get MEDIA_NEXT key.");
            		mpController.rewToLastSubtitle();
            		mpController.reflashProgressView();
	            	return;
	            }
	            Log.d(getClass().getName(),"Unknow MEDIA_KEY: "+event.getKeyCode());
	    }
	}
	
/*	private class SubtitleTimer extends AsyncTask<SubtitleElement, Void, Void> {
		protected Void doInBackground(SubtitleElement... se) {
			String logTag="SubtitleTimer";
			int playPoint=-1, playArrayIndex=-1;
			int monInterval=activity.getResources().getInteger(R.integer.subtitleMonInterval);
			while(true){
				if(isCancelled()){
					Log.d(logTag,"Exit nomaly.");
					return null;	// playArrayIndex not last one
				}
				try{
//					Log.d(getClass().getName(),"Subtitle index miss, search the index.");
					synchronized(playingIndexKey){
						/*
						 * While user drag the seek bar indicator over end of media control view, the MediaPlayer will complete play,
						 * if there isn't check mpState, that will cause the indicator of SeekBar jump back to random position.
						 * 
						if(mpState == MP_COMPLETE){
							Log.d(logTag,"SubtitleTimer: the mpState is MP_COMPLETE, terminate subtitleTimer");
							return null;
						}
//						Log.d(logTag,"SubtitleTimer: the mpState is not MP_COMPLETE.");
						playPoint=mediaPlayer.getCurrentPosition();
                        playArrayIndex=Util.subtitleBSearch(se, playPoint);
                       
                        //Log.d(logTag,"check play status: isPlayRegion="+isPlayRegion+", region start="+regionStartMs+", region end="+regionEndMs+", play point="+playPoint);
                        // Play region function has set, and over the region, stop play.
                        //if(isRegionPlay() && playPoint > regionEndMs){
                        if(regionEndMs > 0 && playPoint > regionEndMs){
                                Log.d(logTag,"Stop Play: play point="+playPoint+", regionEndMs="+regionEndMs);
                                pause();
                                changedListener.onComplatePlay();
                                return null;
                        }
                       
                        if(playingIndex!=playArrayIndex){
                                playingIndex=playArrayIndex;
                                if(playArrayIndex!=-1)changedListener.onSubtitleChanged(playArrayIndex, subtitle[playArrayIndex]);
                        }
//                        Log.d(logTag,"SubtitleTimer: release playingIndexKey.");
					}
					// The last of subtitle has reached.
					//if(playArrayIndex==se.length-1)return null;
					
					if(isCancelled()){
						Log.d(logTag,"Exit nomaly.");
						return null;
					}
					
					Thread.sleep(monInterval);
					
					}catch(IllegalStateException e) {
						e.printStackTrace();
						GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="+mpState+", playPoint="+playPoint+", playArrayIndex="+playArrayIndex+", regionEndMs="+regionEndMs, e, true);
						return null;
					}catch (InterruptedException e) {
//						e.printStackTrace();
						GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="+mpState+", playPoint="+playPoint+", playArrayIndex="+playArrayIndex+", regionEndMs="+regionEndMs, e, true);
						return null;
					}catch (Exception e) {
//						e.printStackTrace();
						GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="+mpState+", playPoint="+playPoint+", playArrayIndex="+playArrayIndex+", regionEndMs="+regionEndMs, e, true);
						return null;
					}
				}
		}
	}
*/	

	private class SubtitleTimer extends Thread{
		boolean isCanceled=false;
		public void cancel(){
			isCanceled=true;
		}
		public void cancel(boolean dontCare){
			isCanceled=true;
		}
		
		private boolean isCancelled(){
			return isCanceled;
		}
		
		@Override
		public void run(){
			String logTag = "SubtitleTimer";
			int playPoint = -1, playArrayIndex = -1;
			int monInterval = activity.getResources().getInteger(
					R.integer.subtitleMonInterval);
			while (true) {
				if (isCancelled()) {
					Log.d(logTag, "Exit nomaly.");
					return; // playArrayIndex not last one
				}
				try {
					// Log.d(getClass().getName(),"Subtitle index miss, search the index.");
					synchronized (playingIndexKey) {
						/*
						 * While user drag the seek bar indicator over end of
						 * media control view, the MediaPlayer will complete
						 * play, if there isn't check mpState, that will cause
						 * the indicator of SeekBar jump back to random
						 * position.
						 */
						if (mpState == MP_COMPLETE) {
							Log.d(logTag,
									"SubtitleTimer: the mpState is MP_COMPLETE, terminate subtitleTimer");
							return;
						}
						// Log.d(logTag,"SubtitleTimer: the mpState is not MP_COMPLETE.");
						playPoint = mediaPlayer.getCurrentPosition();
						playArrayIndex = Util.subtitleBSearch(subtitle, playPoint);

						// Log.d(logTag,"check play status: isPlayRegion="+isPlayRegion+", region start="+regionStartMs+", region end="+regionEndMs+", play point="+playPoint);
						// Play region function has set, and over the region,
						// stop play.
						// if(isRegionPlay() && playPoint > regionEndMs){
						if (regionEndMs > 0 && playPoint > regionEndMs) {
							Log.d(logTag, "Stop Play: play point=" + playPoint
									+ ", regionEndMs=" + regionEndMs);
							pause();
							changedListener.onComplatePlay();
							return;
						}

						//Log.d(logTag,"Current position: "+Util.getMsToHMS(playPoint)+", subtitle: "+subtitle[playArrayIndex].text);
						if (playingIndex != playArrayIndex) {
							playingIndex = playArrayIndex;
							if (playArrayIndex != -1)
								changedListener.onSubtitleChanged(
										playArrayIndex,
										subtitle[playArrayIndex]);
						}
						// Log.d(logTag,"SubtitleTimer: release playingIndexKey.");
					}
					// The last of subtitle has reached.
					// if(playArrayIndex==se.length-1)return null;

					if (isCancelled()) {
						Log.d(logTag, "Exit nomaly.");
						return;
					}

					Thread.sleep(monInterval);

				} catch (IllegalStateException e) {
					e.printStackTrace();
					GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="
							+ mpState + ", playPoint=" + playPoint
							+ ", playArrayIndex=" + playArrayIndex
							+ ", regionEndMs=" + regionEndMs, e, true);
					return;
				} catch (InterruptedException e) {
					// e.printStackTrace();
					GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="
							+ mpState + ", playPoint=" + playPoint
							+ ", playArrayIndex=" + playArrayIndex
							+ ", regionEndMs=" + regionEndMs, e, true);
					return;
				} catch (Exception e) {
					// e.printStackTrace();
					GaLogger.sendException("SubtitleTimer_EXCEPTION: mpState="
							+ mpState + ", playPoint=" + playPoint
							+ ", playArrayIndex=" + playArrayIndex
							+ ", regionEndMs=" + regionEndMs, e, true);
					return;
				}
			}
		}
	}
	public SubtitleElement[] getSubtitle(){
		return subtitle;
	}
}
