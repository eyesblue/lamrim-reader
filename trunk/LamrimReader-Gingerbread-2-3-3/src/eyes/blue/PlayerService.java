package eyes.blue;

import java.io.File;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eyes.blue.LamrimReaderActivity.TheoryListAdapter;
import android.app.Activity;
import android.app.IntentService;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;


public class PlayerService extends IntentService  {
	final static String logTag="eyes.blue.PlayerService";
	static MediaPlayer mediaPlayer=null;
	static int mediaIndex=-1;
	static int mediaPosition=-1;
	private final IBinder mBinder = new PlayerBinder();
	int subtitleIndex=-1;
	static SubtitleElement[] subtitle = null;
	ArrayList<HashMap<String, String>> bookList=null;
	OnPreparedListener onPreparedListener=null;
	public static boolean isRunning=false;

	public PlayerService() {
		super(logTag);
	}
	
	@Override
	public void onCreate() {
		isRunning=true;
		String[] bookPage = getResources().getStringArray(R.array.book);
		bookList = new ArrayList<HashMap<String, String>>();
		int pIndex = 0;

		for (String value : bookPage) {
			HashMap<String, String> item = new HashMap<String, String>();
			item.put("page", value);
			item.put("desc", "第 " + (++pIndex) + " 頁");
			bookList.add(item);
		}
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {}
	
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
	public MediaPlayer createPlayer(int index,int position,OnPreparedListener onPreparedListener){
/*		if(index==mediaIndex && mediaPlayer != null){
			Log.d(logTag,"The request index the same with plaing index: "+index);
			onPreparedListener.onPrepared(mediaPlayer);
			return mediaPlayer;
		}
*/		
		if(mediaPlayer!=null){
			Log.d(logTag,"!!!! The media player is not null, release now !!!!");
			releasePlayer();
		}
		
		mediaIndex=index;
		mediaPosition=position;
		this.onPreparedListener=onPreparedListener;
		
		Log.d(logTag,"Create player index: "+index+", position: "+position);
		File file = FileSysManager.getLocalMediaFile(index);
		mediaPlayer = MediaPlayer.create(getApplicationContext(), Uri.fromFile(file));
		
		mediaPlayer.setOnPreparedListener(onPreparedListener);
		return mediaPlayer;
	}
	

	public MediaPlayer getMediaPlayer(){
		return mediaPlayer;
	}
	
//	public void setOnPrepareListener(MediaPlayerOnPreparedListener listener){
//		onPreparedListener=listener;
//	}
	
	public ArrayList<HashMap<String, String>> getBookContent(){
		Log.d(logTag,"Into playerService.getBookContent: the data of bookList "+bookList.size());
		return bookList;
	}
	
	public SubtitleElement[] getSubtitle(int index){
		if(subtitle!=null && subtitleIndex==index)return subtitle;
		
		Log.d(logTag,"Load subtitle index "+index);
		File subtitleFile = FileSysManager.getLocalSubtitleFile(index);
		if (!subtitleFile.exists())return null;
		
		subtitle = Util.loadSubtitle(subtitleFile);
		subtitleIndex=index;
		return subtitle;
	}
	
	public void releasePlayer(){
		if (mediaPlayer != null){  
            mediaPlayer.reset();  
            mediaPlayer.release();  
            mediaPlayer = null;  
		}
		mediaIndex=-1;
	}
	
	@Override
	public void onDestroy() {
//		releasePlayer();    
		stopSelf();
//		super.onDestroy();
	}
	/**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class PlayerBinder extends Binder {
    	PlayerService getService() {
            // Return this instance of LocalService so clients can call public methods
            return PlayerService.this;
        }
    }
}
