package eyes.blue;

import java.io.BufferedReader;

import android.net.Uri;
import android.os.SystemClock;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.os.PowerManager;
import android.util.Log;
import android.view.View;
import android.widget.MediaController;
import android.widget.MediaController.MediaPlayerControl;
/*
 * The class maintain the MediaPlayer, MediaPlayController and subtitle. There are many stage of MediaPlayer while play media, all stage maintain in the class, call functions of this function Instead of the functions of MediaPlayer,
 * Then you will get the better controller of MediaPlayer.
 * */
public class MediaPlayerController implements MediaPlayerControl {
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
//	long monInterval=100;
	
	/*
	 * Give The constructor the Activity and changedListener for build object. You can change the LamrimReaderActivity to your activity and modify the code of UI control to meet your logic. 
	 * */
	public MediaPlayerController(LamrimReaderActivity activity,MediaPlayerControllerListener changedListener){
		this.activity=activity;
		logTag=activity.getResources().getString(R.string.app_name);
		powerManager=(PowerManager) activity.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, logTag);
		this.changedListener=changedListener;
		
//		if(mediaPlayer==null)mediaPlayer=new MediaPlayer();
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
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
	}
	
// =============== Function implements of MediaPlayercontroller =================
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer and release the subtitleTimer.
	 * */
	@Override
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
	@Override
	public void seekTo(int pos) {
		if(mpState>=MP_PREPARED){
			mediaPlayer.seekTo(pos);
		}
	}
	
	/*
	 * Same as function of MediaPlayer and maintain the state of MediaPlayer. create new subtitleTimer for subtitle changed event.
	 * */
	@Override
	public void start() {
		if(!wakeLock.isHeld()){Log.d(logTag,"Play media and Lock screen.");wakeLock.acquire();}
		if(subtitleTimer!=null){
			subtitleTimer.cancel(true);
			subtitleTimer=null;
		}
		if(subtitle!=null){
			Log.d(getClass().getName(),"The subtitle is not null, prepare subtitle timer.");
			subtitleTimer = new SubtitleTimer();
			subtitleTimer.execute(subtitle);
		}
			
		synchronized(mediaPlayer){
			mediaPlayer.start();
			mpState=MP_PLAYING;
		}
	}
	
	/*
	 * The reader get content from local drive, always return 0;
	 * */
	@Override
	public int getBufferPercentage() {return 0;}
	/*
	 * Same as function of MediaPlayer but not throw IllegalStateException(return 0).
	 * */
	@Override
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
	@Override
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
	@Override
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
	@Override
	public boolean canPause() {return true;}
	
	/*
	 * Always return true.
	 * */
	@Override
	public boolean canSeekBackward() {return true;}
	/*
	 * Always return true.
	 * */
	@Override
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
	}
	/*
	 * Return is the subtitle ready.
	 * */
	public boolean isSubtitleReady(){
		if(subtitle==null)return false;
		return true;
	}
	/*
	 * Set data source of MediaPlayer, and parse the file of subtitle if exist.
	 * */
	public void setDataSource(Context context,int index) throws IllegalArgumentException, SecurityException, IllegalStateException, IOException{
		final File subtitleFile=FileSysManager.getLocalSubtitleFile(index);
		
		if(subtitleFile.exists()){
			Log.d(getClass().getName(),"The subtitle file exist, prepare the subtitle elements.");
			new Thread(new Runnable(){
				@Override
				public void run() {
					subtitle = loadSubtitle(subtitleFile);
					if(subtitle.length==0)subtitle=null;
				}}).run();
		}else {
			Log.d(getClass().getName(),"The subtitle file not exist, set the subtitle null.");
			subtitle=null;
		}
		
		synchronized(mediaPlayer){
			mediaPlayer.setDataSource(context, Uri.fromFile(FileSysManager.getLocalMediaFile(index)));
			mpState=MP_INITED;
		}
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
		if (activity.isFinishing()) {
			Log.d(logTag,"The activity not prepare yet, skip show media controller.");
			return;
		}
		if (mediaController != null && mediaController.isShowing()) {
			Log.d(logTag,"The controller has showing, skip show media controller.");
			return;
		}

		mediaController = new MediaController(activity);
		mediaController.setMediaPlayer(this);
		mediaController.setAnchorView(rootView);
		mediaController.setEnabled(true);

		activity.runOnUiThread(new Runnable() {
			public void run() {
				mediaController.show();
			}
		});
	}
	
	final protected OnPreparedListener onPreparedListener = new OnPreparedListener() {
		public void onPrepared(MediaPlayer mp) {
			Log.d(getClass().getName(), "**** Into onPreparedListener of MediaPlayer ****");
			synchronized(mediaPlayer){
				mpState = MP_PREPARED;
			}
			
			if (!wakeLock.isHeld())
				wakeLock.acquire();
			
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
				if (getCurrentPosition() > 0)
					try {
						start();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					}
				// mpController.start();
				break;
			// lost audio focus long time, release resource here.
			case AudioManager.AUDIOFOCUS_LOSS:
				Log.d("onAudioFocusChange",
						"Loss of audio focus of unknown duration.");
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
			int playingIndex=-1;
			int monInterval=activity.getResources().getInteger(R.integer.subtitleMonInterval);
			while(true){
				if(isCancelled())return null;	// playArrayIndex not last one
				try{
//					Log.d(getClass().getName(),"Subtitle index miss, search the index.");
					int playPoint=mediaPlayer.getCurrentPosition();
					int playArrayIndex=subtitleBSearch(se, playPoint);
					if(playingIndex==playArrayIndex)continue;
					changedListener.onSubtitleChanged(subtitle[playArrayIndex]);
					
					// The last of subtitle has reached.
					if(playArrayIndex==se.length-1)return null;
					
					Thread.sleep(monInterval);

					
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
					if (stemp
							.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")) {
						// System.out.println("Get time string: "+stemp);
						int startTimeMs;
						String ts = stemp.substring(0, 2);
						// System.out.println("Hour: "+ts);
						startTimeMs = Integer.parseInt(ts) * 3600000;
						ts = stemp.substring(3, 5);
						// System.out.println("Min: "+ts);
						startTimeMs += Integer.parseInt(ts) * 60000;
						ts = stemp.substring(6, 8);
						// System.out.println("Sec: "+ts);
						startTimeMs += Integer.parseInt(ts) * 1000;
						ts = stemp.substring(9, 12);
						// System.out.println("Sub: "+ts);
						startTimeMs += Integer.parseInt(ts);
						// System.out.println("Set time: "+startTimeMs);
						se.startTimeMs = startTimeMs;
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
