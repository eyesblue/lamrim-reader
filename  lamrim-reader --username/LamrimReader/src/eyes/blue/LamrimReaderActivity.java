package eyes.blue;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.PowerManager;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class LamrimReaderActivity extends Activity implements OnAudioFocusChangeListener {
    /** Called when the activity is first created. */
	static String logTag="LamrimReader";
	
    final static int NORMAL_MODE=0;
    final static int READING_MODE=1;
    final static int NOSUBTITLE_MODE=2;
    final static int TV_MODE=3;
    
	Intent optCtrlPanel=null;
	private MediaPlayer mMediaPlayer=null;
	static SubtitleElement[] subtitle;
//	TextView bookView;
	TextView subtitleView;
	SeekBar playBar;
	SharedPreferences options;
	SharedPreferences runtime;
	int appMode=0;
	int currentPlayedSubtitleIndex=-1;
	int subtitleWordMax=0;
	int screenWidth=0;
	Timer playBarTimer=null;
	
	String appModeKey=null;
	String isDisplayBookKey=null;
	String isDisplaySubtitleKey=null;
	String isPlayAudioKey=null;
	String bookFontSizeKey=null;
	String subtitleFontSizeKey=null;
	String subtitleLineCountKey=null;
	int defBookFontSize=R.integer.defBookFontSize;
	int fontSizeArraylength=0;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        appModeKey=getString(R.string.appModeKey);
        isDisplayBookKey=getString(R.string.isDisplayBook);
		isDisplaySubtitleKey=getString(R.string.isDisplaySubtitle);
		isPlayAudioKey=getString(R.string.isPlayAudio);
		bookFontSizeKey=getString(R.string.bookFontSizeKey);
		subtitleFontSizeKey=getString(R.string.subtitleFontSizeKey);
		subtitleLineCountKey=getString(R.string.subtitleLineCount);
		fontSizeArraylength=getResources().getIntArray(R.array.fontSizeArray).length;
		
		setContentView(R.layout.main);
		
        options = getSharedPreferences(getString(R.string.optionFile), 0);
        runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
        appMode=getResources().getInteger(R.integer.defAppMode);
        appMode=options.getInt(appModeKey, appMode);
        
        
        
        
        int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
        optCtrlPanel = new Intent(LamrimReaderActivity.this,OptCtrlPanel.class);
//        bookView=(TextView)findViewById(R.id.bookView);
//        bookView.setMovementMethod(new ScrollingMovementMethod());
//        bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
        
        
        subtitleView=(TextView)findViewById(R.id.subtitleView);
        subtitleView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
        subtitleView.setLines(subtitleLineCount);
        
        // Determine how many words can show per line.
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth=dm.widthPixels;
        
        playBar=(SeekBar)findViewById(R.id.playBar);
        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

		@Override
		public void onProgressChanged(SeekBar playBar, int progress, boolean fromUser) {
				// TODO Auto-generated method stub
			if(!fromUser)return;
			currentPlayedSubtitleIndex=subtitleBSearch(subtitle,progress);
			final SubtitleElement se=subtitle[currentPlayedSubtitleIndex];
			runOnUiThread(new Runnable() {
					public void run() {
						subtitleView.setText(se.text.toCharArray(), 0, se.text.length());
					}});
			mMediaPlayer.seekTo(progress);
		}

			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}});
        
        
        subtitle=loadSubtitle(new File("/sdcard/001A.srt"));
        
        
        Log.d(logTag,"Width of screen: "+dm.widthPixels+", Width of Word: "+subtitleView.getPaint().measureText("中")+", There are "+subtitleWordMax+" can show in one line.");
        playAudio();
    }
    
    private void switchMode(int mode){
    	final LinearLayout.LayoutParams mainLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT,3);
    	final LinearLayout.LayoutParams bottomLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,0);
    	
    	switch(mode){
    		case NORMAL_MODE:
    			Log.d(logTag,"Switch to Normal mode.");
    			((LinearLayout)findViewById(R.id.rootLayout)).setGravity(Gravity.CENTER);
//    			bookView.setLayoutParams(mainLayout);
//    			bookView.setVisibility(View.VISIBLE);

    			subtitleView.setLayoutParams(bottomLayout);
    			subtitleView.setGravity(Gravity.CENTER|Gravity.BOTTOM);
    			subtitleView.setVisibility(View.VISIBLE);
    			((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
    			appMode=mode;
    			break;
    		case READING_MODE:
    			Log.d(logTag,"Switch to Reading mode.");
//    			bookView.setLayoutParams(mainLayout);
//    			bookView.setVisibility(View.VISIBLE);
    			subtitleView.setVisibility(View.GONE);
    			((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.GONE);
    			releasePlayer();
    			appMode=mode;
    			break;
    		case NOSUBTITLE_MODE:
    			Log.d(logTag,"Switch to NoSubtitle mode.");
//    			bookView.setLayoutParams(mainLayout);
//   			bookView.setVisibility(View.VISIBLE);
//    			subtitleView.setLayoutParams(bottomLayout);
//    			subtitleView.setGravity(Gravity.CENTER|Gravity.BOTTOM);
    			subtitleView.setVisibility(View.GONE);
    			((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
    			appMode=mode;
    			break;
    		case TV_MODE:
    			Log.d(logTag,"Switch to TV mode.");
//    			bookView.setLayoutParams(mainLayout);
//    			bookView.setVisibility(View.GONE);
    			subtitleView.setLayoutParams(mainLayout);
    			subtitleView.setGravity(Gravity.CENTER);
    			subtitleView.setVisibility(View.VISIBLE);
    			((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
    			appMode=mode;
    			break;
    	}
    	
    	
    }
    
    private int getSubtitleWordCountMax(TextView view){
    	int count=(int)((float)screenWidth/view.getPaint().measureText("中"));
    	Log.d(logTag,"Width of screen: "+screenWidth+", Width of Word: "+view.getPaint().measureText("中")+", There are "+count+" can show in one line.");
    	return  count;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0,NORMAL_MODE,0,R.string.normalModeDesc);
    	menu.add(0,READING_MODE,1,R.string.readingModeDesc);
    	menu.add(0,NOSUBTITLE_MODE,2,R.string.noSubtitleModeDesc);
    	menu.add(0,TV_MODE,3,R.string.tvModeDesc);
    	menu.add(0,4,4,R.string.showCtrlPanel);
    	menu.add(0,5,5,R.string.showAbout);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	int mode=item.getItemId();
    	Log.d(logTag,"User select menu item: "+mode);
    	if(appMode==mode)return true;
    	switch (mode) {
		case NORMAL_MODE:switchMode(NORMAL_MODE);return true;
		case READING_MODE:switchMode(READING_MODE);return true;
		case NOSUBTITLE_MODE:switchMode(NOSUBTITLE_MODE);return true;
		case TV_MODE:switchMode(TV_MODE);return true;
		case 4:startActivityForResult(optCtrlPanel, 0);return true;
		case 5: return true;
		}
		return false;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	super.onActivityResult(requestCode, resultCode, intent);
    	boolean isDisplayBook=intent.getBooleanExtra(isDisplayBookKey, true);
    	if(!isDisplayBook){
//    		bookView.setVisibility(View.GONE);
    		subtitleView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.FILL_PARENT));
    		subtitleView.setGravity(Gravity.CENTER);
    	}
    	boolean isDisplaySubtitle=intent.getBooleanExtra(isDisplaySubtitleKey, true);
    	if(!isDisplaySubtitle){
    		subtitleView.setVisibility(View.GONE);
    	}
    	boolean isPlayAudio=intent.getBooleanExtra(isPlayAudioKey, true);
    	Log.d(logTag,"Set bookview: "+isDisplayBook+", subtitleView: "+isDisplaySubtitle+", Play audio: "+isPlayAudio);
    	
    	final int bookFontSize=intent.getIntExtra(bookFontSizeKey, R.integer.defBookFontSize);
    	final int subtitleFontSize=intent.getIntExtra(subtitleFontSizeKey, R.integer.defSubtitleFontSize);
    	final int subtitleLineCount=intent.getIntExtra(subtitleLineCountKey, R.integer.defSubtitleLineCount);

    	Log.d(logTag,"Book font size key="+bookFontSizeKey+", subtitle font size key="+subtitleFontSizeKey);
    	Log.d(logTag,"Get the book font size: "+bookFontSize+", subtitleFontSize: "+subtitleFontSize);
/*
    	if(isDisplayBook!=uiIsDisplayBook){
       	 intent.putExtra(isDisplayBookKey, uiIsDisplayBook);
    	}
    	else if(isDisplaySubtitle!=uiIsDisplaySubtitle){
      	 	intent.putExtra(isDisplaySubtitleKey, uiIsDisplaySubtitle);
    	}
    	else if(isPlayAudio!=uiIsPlayAudio){
      	 	intent.putExtra(isPlayAudioKey, uiIsPlayAudio);
    	}
    	else if(bookFontSize!=uiBookFontSize){
    		intent.putExtra(bookFontSizeKey, uiBookFontSize);
    	}
    	else if(subtitleFontSize!=uiSubtitleFontSize){
    		intent.putExtra(subtitleFontSizeKey, uiSubtitleFontSize);
    	}
    	else if(subtitleLineCount!=uiSubtitleLineCount){
    		intent.putExtra(subtitleLineCountKey, uiSubtitleLineCount);
    	}
 */      
    	runOnUiThread(new Thread() {
    		@Override
			public void run() {
//				if(bookView.getTextSize()!=bookFontSize){
//					Log.d(logTag,"Set book font size to "+getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
//					bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
//				}
				if(subtitleView.getTextSize()!=subtitleFontSize){
					Log.d(logTag,"Set subtitle font size to "+getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
					subtitleView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
				}
				if(subtitleView.getLineCount()!=subtitleLineCount)subtitleView.setLines(subtitleLineCount);
			}
		});
    	
    }
    
    private static int subtitleBSearch(SubtitleElement[] a,int key){
		int mid=a.length/2;
		int low=0;
		int hi=a.length;
		while (low <= hi){
			mid = (low + hi) >>> 1;
//    			         final int d = Collections.compare(a[mid], key, c);
    		int d=0;
    		if(mid==0){System.out.println("Shift to the index 0, return 0");return 0;}
    		if(mid==a.length-1){System.out.println("Shift to the last element, return "+(a.length-1));return a.length-1;}
    		if(a[mid].startTimeMs > key && key <= a[mid+1].startTimeMs){d=1;System.out.println("MID="+mid+", Compare "+a[mid].startTimeMs+" > "+key +" > "+a[mid+1].startTimeMs+", set -1, shift to smaller");}
    		else if(a[mid].startTimeMs<= key && key < a[mid+1].startTimeMs){d=0;System.out.println("This should find it! MID="+mid+", "+a[mid].startTimeMs+" < "+key +" > "+a[mid+1].startTimeMs+", set 0, this should be.");}
    		else {d=-1;System.out.println("MID="+mid+", Compare "+a[mid].startTimeMs+" < "+key +" < "+a[mid+1].startTimeMs+", set -1, shift to bigger");}
    		if (d == 0)
    			return mid;
    		else if (d > 0)
    			hi = mid - 1;
    		else
    			// This gets the insertion point right on the last loop
    			low = ++mid;
		}
		String msg="Binary search state error, shouldn't go to the unknow stage. this may cause by a not sorted subtitle: MID="+mid+", Compare "+a[mid].startTimeMs+" <> "+key +" <> "+a[mid+1].startTimeMs+ " into unknow state.";
		Log.e(logTag, msg, new Exception(msg));
		return -1;
    }
    
    private void playAudio() {
        try {
        	AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,    AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {    // could not get audio focus.}
            	AlertDialog.Builder builder = new AlertDialog.Builder(this);
            	builder.setMessage("The audio device has been catched by another application(it may catched by another media player, browser or any apps which play sound in background), please close the app for release audio device.").setCancelable(false).setPositiveButton("Ok, I'll take it!",null);
            	builder.create();
            }
            
        	File f=new File("/sdcard/001A.MP3");
       		Log.d("Lamrim Reader:", "Open the "+f.getAbsolutePath()+" for test.");
        	
            mMediaPlayer = MediaPlayer.create(this, Uri.fromFile(f));
            mMediaPlayer.setWakeMode(getApplicationContext(), PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
            mMediaPlayer.setOnPreparedListener(new OnPreparedListener()
            
            {
              @Override
              public void onPrepared(final MediaPlayer mp) 
              {
            	  int timerDelaytime=100;
            	  SeekBar seekBar=(SeekBar)findViewById(R.id.playBar);
            	  seekBar=(SeekBar) findViewById(R.id.playBar);
            	  seekBar.setMax(mp.getDuration());
            	  //seekBar.setMax(300000);
            	  playBarTimer=new Timer();
            	  playBarTimer.schedule(new TimerTask(){
                      @Override
                      public void run() {
//                    	  int i=subtitleSearch(subtitle,mp.getCurrentPosition());
//                    	  int i=subtitleBSearch(subtitle,mp.getCurrentPosition());
//                   	  final SubtitleElement se =subtitle[i];
                    	  if(mp==null)return;
                    	  playBar.setProgress(mp.getCurrentPosition());
                    	  
                    	  if(currentPlayedSubtitleIndex<subtitle.length-1 && mp.getCurrentPosition()>=subtitle[currentPlayedSubtitleIndex+1].startTimeMs){
                    			  final SubtitleElement se =subtitle[++currentPlayedSubtitleIndex];
                    			  runOnUiThread(new Runnable() {
          							public void run() {
          								subtitleView.setText(subtitle[currentPlayedSubtitleIndex].text.toCharArray(), 0, se.text.length());
          							}});
                    	  }
                      }
            	  }, 0, timerDelaytime);
              }
          });

            mMediaPlayer.start();

//            tx.setText("Playing audio...");

        } catch (Exception e) {
//            Log.e(TAG, "error: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO Auto-generated method stub
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }
    
    

    public static SubtitleElement[] loadSubtitle(File file){
    	ArrayList<SubtitleElement> subtitleList=new ArrayList<SubtitleElement>();
    	try {
			System.out.println("Open "+file.getAbsolutePath()+" for read subtitle.");
			BufferedReader br=new BufferedReader(new FileReader(file));
			String stemp;
			int lineCounter=0;
			int step=0; // 0: Find the serial number, 1: Get the serial number, 2: Get the time description, 3: Get Subtitle
			int serial=0;
			SubtitleElement se = null;
			
			while((stemp=br.readLine())!=null){
				lineCounter++;
				
				// This may find the serial number
				if(step==0){
					if(stemp.matches("[0-9]+")){
//						System.out.println("Find a subtitle start: "+stemp);
						se=new SubtitleElement();
						serial=Integer.parseInt(stemp);
						step=1;
					}
				}
				
				// This may find the time description
				else if(step==1){
					if(stemp.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")){
//						System.out.println("Get time string: "+stemp);
						int startTimeMs;
						String ts=stemp.substring(0,2);
//						System.out.println("Hour: "+ts);
						startTimeMs=Integer.parseInt(ts)*3600000;
						ts=stemp.substring(3,5);
//						System.out.println("Min: "+ts);
						startTimeMs+=Integer.parseInt(ts)*60000;
						ts=stemp.substring(6,8);
//						System.out.println("Sec: "+ts);
						startTimeMs+=Integer.parseInt(ts)*1000;
						ts=stemp.substring(9,12);
//						System.out.println("Sub: "+ts);
						startTimeMs+=Integer.parseInt(ts);
//						System.out.println("Set time: "+startTimeMs);
						se.startTimeMs=startTimeMs;
						step=2;
					}
					else {
//						System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
						step=0;
					}
				}
				else if(step==2){
					se.text=stemp;
					step=0;
					subtitleList.add(se);
					System.out.println("get Subtitle: "+stemp);
					if(stemp.length()==0)System.err.println("Load Subtitle: Warring: Get a Subtitle with no content at line "+lineCounter);
				}
			}
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (SubtitleElement[])subtitleList.toArray(new SubtitleElement[0]);
	}
    
    @Override
    public void onBackPressed(){
    	releasePlayer();
    	SharedPreferences.Editor editor =options.edit();
    	editor.putInt(appModeKey, appMode);
    	editor.commit();
    	finish();
    }

    protected void onStart(){
    	super.onStart();
    	switchMode(appMode);
    }
    /*    
    protected void onRestart(){}

    protected void onResume(){}

    protected void onPause(){}

    protected void onStop(){}

    protected void onDestroy(){}
*/


    private void releasePlayer(){
    	if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    	if(playBarTimer!=null){
    		playBarTimer.cancel();
    		playBarTimer=null;
    	}
    }
	@Override
	public void onAudioFocusChange(int focusChange) {
		// TODO Auto-generated method stub
		switch(focusChange){
		// Gaint the audio device
		case AudioManager.AUDIOFOCUS_GAIN:
			
			break;
		// lost audio focus long time, release resource here.
		case AudioManager.AUDIOFOCUS_LOSS:
			releasePlayer();
			break;
		// temporarily lost audio focus, but should receive it back shortly. You must stop all audio playback, but you can keep your resources because you will probably get focus back shortly
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:break;
		// You have temporarily lost audio focus, but you are allowed to continue to play audio quietly (at a low volume) instead of killing audio completely.
		case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:break;
		}
	}
        
}