package eyes.blue;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.PowerManager;
import android.os.StatFs;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

public class LamrimReaderActivity extends Activity implements OnAudioFocusChangeListener, OnBufferingUpdateListener, OnPreparedListener {
    /** Called when the activity is first created. */
        static String logTag="LamrimReader";
        
    final static int NORMAL_MODE=0;
    final static int READING_MODE=1;
    final static int NOSUBTITLE_MODE=2;
    final static int TV_MODE=3;
    
    final static int DIALOG_DOWNLOAD_FAIL=0;
    
    AlertDialog.Builder dialogBuilder =null;
    Intent optCtrlPanel=null;
    private MediaPlayer mediaPlayer=null;
	static SubtitleElement[] subtitle=null;
	TextView bookView;
	TextView subtitleView;
	SeekBar playBar;
	SharedPreferences options;
	SharedPreferences runtime;
	int appMode=0;
	int currentPlayedSubtitleIndex=-1;
	int subtitleWordMax=0;
	int screenWidth=0;
	final Timer playBarTimer=new Timer();
        
	String appModeKey=null;
	String bookFontSizeKey=null;
	String subtitleFontSizeKey=null;
	String subtitleLineCountKey=null;
	int subtitleMonInterval=-1;
	int defBookFontSize=R.integer.defBookFontSize;
	int fontSizeArraylength=0;
	FileSysManager fileSysManager=null;
	String remoteSite[]=null;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        appModeKey=getString(R.string.appModeKey);
        bookFontSizeKey=getString(R.string.bookFontSizeKey);
        subtitleFontSizeKey=getString(R.string.subtitleFontSizeKey);
        subtitleLineCountKey=getString(R.string.subtitleLineCount);
        fontSizeArraylength=getResources().getIntArray(R.array.fontSizeArray).length;
                
        setContentView(R.layout.main);
        
        
        options = getSharedPreferences(getString(R.string.optionFile), 0);
        runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
        appMode=getResources().getInteger(R.integer.defAppMode);
        appMode=options.getInt(appModeKey, appMode);
        remoteSite=getResources().getStringArray(R.array.remoteSite);
        
        
        if(fileSysManager==null)fileSysManager=new FileSysManager(this);
        
        int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
        if(optCtrlPanel==null)optCtrlPanel = new Intent(LamrimReaderActivity.this,OptCtrlPanel.class);
        bookView=(TextView)findViewById(R.id.bookView);
        bookView.setMovementMethod(new ScrollingMovementMethod());
        bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
        // For Demo
        String[] bookContent=getResources().getStringArray(R.array.book);
        String bookPages=bookContent[63]+"\n"+bookContent[64]+"\n"+bookContent[65]+"\n"+bookContent[66]+"\n"+bookContent[67]+"\n"+bookContent[68]+"\n"+bookContent[69]+"\n"+bookContent[70]+"\n"+bookContent[71]+"\n"+bookContent[72];
        bookView.setText(bookPages);
        
        
        subtitleView=(TextView)findViewById(R.id.subtitleView);
        subtitleView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
        subtitleView.setLines(subtitleLineCount);
        subtitleMonInterval=getResources().getInteger(R.integer.subtitleMonInterval);
        
        // Determine how many words can show per line.
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth=dm.widthPixels;
        
        
        
        FileSysManager.checkFileStructure();
        Intent intent=getIntent();
        final int searchingMode=intent.getIntExtra(this.getResources().getString(R.string.searchingType), -1);
        final int mediaPosition=intent.getIntExtra("index", -1);
        // !!!!!! The activity is not start by another intent, it should show menu back. !!!!!!!!!
        if(searchingMode==-1 || mediaPosition == -1){}
        
        if(searchingMode==getResources().getInteger(R.integer.PLAY_FROM_MEDIA)){
        	Log.d(logTag,"Play from media "+mediaPosition);
        	new Thread(){
            	public void run(){
                	playAudio(mediaPosition);
                	}
                }.start();
        }
        else if(searchingMode==getResources().getInteger(R.integer.PLAY_FROM_THEORY)){
        	Log.d(logTag,"Play from theory "+mediaPosition);
        }
        
        
        //// for DEMO
        if(mediaPosition==0)
        	subtitle=loadSubtitle(new File("/sdcard/001A.srt"));
        if(mediaPosition==74)
        	subtitle=loadSubtitle(new File("/sdcard/038A.srt"));
        else subtitleView.setText("無字幕");
        
        
        Log.d(logTag,"Width of screen: "+dm.widthPixels+", Width of Word: "+subtitleView.getPaint().measureText("中")+", There are "+subtitleWordMax+" can show in one line.");
        
        playBar=(SeekBar)findViewById(R.id.playBar);
        playBar.setEnabled(false);
        playBar.setClickable(false);
        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

        	@Override
        	public void onProgressChanged(SeekBar playBar, int progress, boolean fromUser) {
        		// For avoid the critical with subtitle player, we cancel the subtitle player and reset the currentPlayedSubtitleIndex, restart subtitle player while all ready.
        		

        		if(!fromUser)return;
//				for demo
        		if(mediaPosition==74||mediaPosition==0){
        		synchronized(playBarTimer){
        			currentPlayedSubtitleIndex=subtitleBSearch(subtitle,progress);
        			final char[] text=subtitle[currentPlayedSubtitleIndex].text.toCharArray();
        			runOnUiThread(new Runnable() {
        				public void run() {
        					subtitleView.setText(text, 0, text.length);
        				}});
        		}
        		}
        		if(mediaPlayer!=null&&mediaPlayer.isPlaying())mediaPlayer.seekTo(progress);
        		startSubtitlePlayer();
        	}

        	@Override
        	public void onStartTrackingTouch(SeekBar seekBar) {}
        	@Override
        	public void onStopTrackingTouch(SeekBar seekBar) {}});
    }
    
    private void switchMode(int mode){
        final LinearLayout.LayoutParams mainLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT,3);
        final LinearLayout.LayoutParams bottomLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,0);
        
        switch(mode){
                case NORMAL_MODE:
                        Log.d(logTag,"Switch to Normal mode.");
                        ((LinearLayout)findViewById(R.id.rootLayout)).setGravity(Gravity.CENTER);
//                      bookView.setLayoutParams(mainLayout);
                        findViewById(R.id.horizontalScrollView1).setVisibility(View.VISIBLE);

                        subtitleView.setLayoutParams(bottomLayout);
                        subtitleView.setGravity(Gravity.CENTER|Gravity.BOTTOM);
                        subtitleView.setVisibility(View.VISIBLE);
                        ((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
                        appMode=mode;
                        break;
                case READING_MODE:
                        Log.d(logTag,"Switch to Reading mode.");
//                      bookView.setLayoutParams(mainLayout);
//                      bookView.setVisibility(View.VISIBLE);
                        subtitleView.setVisibility(View.GONE);
                        ((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.GONE);
                        releasePlayer();
                        appMode=mode;
                        break;
                case NOSUBTITLE_MODE:
                        Log.d(logTag,"Switch to NoSubtitle mode.");
//                      bookView.setLayoutParams(mainLayout);
//                      bookView.setVisibility(View.VISIBLE);
//                      subtitleView.setLayoutParams(bottomLayout);
//                      subtitleView.setGravity(Gravity.CENTER|Gravity.BOTTOM);
                        subtitleView.setVisibility(View.GONE);
                        ((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
                        appMode=mode;
                        break;
                case TV_MODE:
                        Log.d(logTag,"Switch to TV mode.");
//                      bookView.setLayoutParams(mainLayout);
                        findViewById(R.id.horizontalScrollView1).setVisibility(View.GONE);
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
                                if(bookView.getTextSize()!=bookFontSize){
                                        Log.d(logTag,"Set book font size to "+getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
                                        bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
                                }
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
//                               final int d = Collections.compare(a[mid], key, c);
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
    
/*    private void playAudio(int index){
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,    AudioManager.AUDIOFOCUS_GAIN);
        if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {    // could not get audio focus.}
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The audio device has been catched by another application(it may catched by another media player, browser or any apps which play sound in background), please close the app for release audio device.").setCancelable(false).setPositiveButton("Ok, I'll take it!",null);
                builder.create();
        }
        
              URL url=null;
        String site="https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/100A.MP3";
        try {
                        url = new URL(site);
                        StreamingMediaPlayer smp=new StreamingMediaPlayer(this,subtitleView,playBar);
                smp.startStreaming(site, 3602, 30*60);
                } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
  */      
        
 /*       
        URL url=null;
                try {
                        url = new URL("https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/100A.MP3");
                        StreamingMp3Player smp=new StreamingMp3Player(this,url);
                smp.play("https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/100A.MP3");
                } catch (MalformedURLException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }

    }
*/    
    /*
     * There has a critical region of subtitle player and user change the search bar.
     * While start subtitle player should call the function for avoid the problem.
     * */
    private synchronized void  startSubtitlePlayer(){
        playBarTimer.schedule(
         new TimerTask(){
        @Override
        public void run() {
// 			for DEMO
          if(subtitle==null)return;
          int mediaPosition=-1;
          try{
                  mediaPosition=mediaPlayer.getCurrentPosition();
          }catch(NullPointerException npe){
                  Log.e(logTag,"The media player has gone. it may happen on normal switch file or release stage.");
                  npe.printStackTrace();
          }
          playBar.setProgress(mediaPosition);
          synchronized(playBarTimer){
          if(currentPlayedSubtitleIndex<subtitle.length-1 && mediaPosition>=subtitle[currentPlayedSubtitleIndex+1].startTimeMs){
        	  final char[] text=subtitle[++currentPlayedSubtitleIndex].text.toCharArray();
        	  runOnUiThread(new Runnable() {
        		  public void run() {
        			  subtitleView.setText(text, 0, text.length);
        		  }});
          }}
        }
          }, 0, subtitleMonInterval);
    }
    
    /*
     * Play with TestStreamPlayer
     * */
/*    private void playAudio(final int index){
    	TestStreamingPlayer tsp=new TestStreamingPlayer(this,index);
    	try {
			tsp.play();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
*/    
    
    private void playAudio(final int index) {
        Log.d(logTag,"Play "+this.getResources().getStringArray(R.array.fileName)[index]);
        runOnUiThread(new Runnable() {
            public void run() {
            	playBar.setEnabled(false);
            	playBar.setClickable(false);
            }
        });
        
                AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int result = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC,    AudioManager.AUDIOFOCUS_GAIN);
            if (result != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {    // could not get audio focus.}
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("The audio device has been catched by another application(it may catched by another media player, browser or any apps which play sound in background), please close the app for release audio device.").setCancelable(false).setPositiveButton("Ok, I'll take it!",null);
                builder.create();
            }
            
            fileSysManager.setDownloadFailListener(new DownloadFailListener(){
                public void downloadFail(int index){
                	Looper.prepare();
                    onCreateDialog(DIALOG_DOWNLOAD_FAIL).show();
                }
            });
            fileSysManager.setDownloadProgressListener(new DownloadProgressListener(){
                public void setDownloadProgress(int percent){
                        if(percent==100){
                                playBar.setSecondaryProgress(playBar.getMax());
                                return;
                        }
                        int value=playBar.getMax()/100*percent;
                        playBar.setSecondaryProgress(value);
                }
            });
            
            fileSysManager.setDownloadFinishListener(new DownloadFinishListener(){
                public void downloadFinish(int fileIndex){
                        Log.d(logTag,FileSysManager.fileName[fileIndex]+" Download finish, call LamrimReader.playAudio("+fileIndex+") again");
                        playAudio(fileIndex);
///						for DEMO
                   //// for DEMO
                        if(index==0||index==74)
                        runOnUiThread(new Runnable() {
                            public void run() {
                            	subtitleView.setText("無字幕");
                        }});
                }
            });
  //            File f=new File("/sdcard/001A.MP3");
  //                    Log.d("Lamrim Reader:", "Open the "+f.getAbsolutePath()+" for test.");
  //            mMediaPlayer = MediaPlayer.create(this, Uri.fromFile(f));
            
            
            
            if(!FileSysManager.isFileValid(index)){
//	Here should show dialog to user that will be start download file from internet. 
            	Log.d(logTag,"The file which alreadly in phone is not valid");
            	subtitleView.setText(getResources().getString(R.string.downloadingDesc));
            	FileSysManager.downloadFileFromRemote(index);
            	return;
            }
            /*            
            FileInputStream fis1=null;
            try{
                fis1=openFileInput(getResources().getStringArray(R.array.fileName)[index]);
            }catch(FileNotFoundException fnfe){
                fnfe.printStackTrace();
            }
            if(fis1==null)Log.d(logTag,"file not exist");

            File file=new File("/sdcard/001A.MP3");
            Log.d(logTag,"Try to file the file from local file system");
            FileInputStream fis=getLocalFile(index);
            if(fis==null){
                Log.d(logTag,"There is no such media in local file system, try to download.");
                fileSysManager.downloadFileFromRemote(index);
                subtitleView.setText(getString(R.string.downloadingDesc));
                return;
            }
*/            
            Log.d(logTag,"Found it!");
//            URLConnection con = url.openConnection();
//            con.connect();
//            con.getContent(); //This is needed or setDataSource will throw IOException
//            
            File file=FileSysManager.getLocalMediaFile(index);
            
            try{
            	FileInputStream fis=new FileInputStream(file);
                Log.d(logTag,"Play media: "+file.getAbsolutePath());
//				For mediaPlayer.getDuration(), while start mediaPlayer with prepareAsync(), the time is not precision enough, we start with create() function for this.                 
                if(mediaPlayer==null)mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));
/*                if(mediaPlayer!=null)mediaPlayer.reset();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(fis.getFD());
                mediaPlayer.prepareAsync();
*/
            }catch(IllegalStateException ise){ise.printStackTrace();}
//            catch(IOException ioe){ioe.printStackTrace();}
            catch(IllegalArgumentException iae){iae.printStackTrace();}
            catch (FileNotFoundException e) {e.printStackTrace();}
            catch (IOException e) {e.printStackTrace();}
                

//            mMediaPlayer.setDataSource("http://lamrimreader.eyes-blue.com/appresources/100A.MP3");
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnBufferingUpdateListener(this);
//                      mediaPlayer.setOnPreparedListener(this);
            
            
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
            mediaPlayer.setOnPreparedListener(new OnPreparedListener()
            
            {
              @Override
              public void onPrepared(final MediaPlayer mp) 
              {
                  Log.d(logTag,"Into OnPrepared function");
                  playBar.setMax(mp.getDuration());
                  mediaPlayer.start();
                  playBar.setEnabled(true);
                  playBar.setClickable(true);
//			For DEMO
                  if(index==0||index==74)
                  startSubtitlePlayer();
                  
              }
          });

    
            mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener()  
            {  
              @Override 
              //覆盖错误处理事件
              public boolean onError(MediaPlayer arg0, int arg1, int arg2)  
              {
                  Log.d(logTag,"Error happen while play media");
                // TODO Auto-generated method stub  
                try 
                {  
                  //发生错误时也解除资源与MediaPlayer的赋值*
   //               mp.release();  
   //               tv.setText("播放发生异常!");  
                }  
                catch (Exception e)  
                {  
                        e.printStackTrace();   
                }   
                return false;   
              }   
            });   
            Log.e(logTag, "Prepare data");
            try{
//            mediaPlayer.prepare();
            Log.e(logTag, "Start play");
//            mediaPlayer.start();
        }catch(IllegalStateException ise){ise.printStackTrace();}
//        catch(IOException ioe){ioe.printStackTrace();}
        catch(IllegalArgumentException iae){iae.printStackTrace();}
            
//            tx.setText("Playing audio...");

        
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // TODO Auto-generated method stub
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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
//                                              System.out.println("Find a subtitle start: "+stemp);
                                                se=new SubtitleElement();
                                                serial=Integer.parseInt(stemp);
                                                step=1;
                                        }
                                }
                                
                                // This may find the time description
                                else if(step==1){
                                        if(stemp.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")){
//                                              System.out.println("Get time string: "+stemp);
                                                int startTimeMs;
                                                String ts=stemp.substring(0,2);
//                                              System.out.println("Hour: "+ts);
                                                startTimeMs=Integer.parseInt(ts)*3600000;
                                                ts=stemp.substring(3,5);
//                                              System.out.println("Min: "+ts);
                                                startTimeMs+=Integer.parseInt(ts)*60000;
                                                ts=stemp.substring(6,8);
//                                              System.out.println("Sec: "+ts);
                                                startTimeMs+=Integer.parseInt(ts)*1000;
                                                ts=stemp.substring(9,12);
//                                              System.out.println("Sub: "+ts);
                                                startTimeMs+=Integer.parseInt(ts);
//                                              System.out.println("Set time: "+startTimeMs);
                                                se.startTimeMs=startTimeMs;
                                                step=2;
                                        }
                                        else {
//                                              System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
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
    @Override
    public void onConfigurationChanged(Configuration newConfig)
    {
        super.onConfigurationChanged(newConfig);
     if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
     {
//land
     }
     else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
     {
//port
     }
    }
    protected Dialog onCreateDialog(int id){
        if(dialogBuilder==null)dialogBuilder = new AlertDialog.Builder(this);
        AlertDialog dialog=null;
                        
        switch(id) {
        case DIALOG_DOWNLOAD_FAIL:
                dialogBuilder.setMessage(getString(R.string.downloadFail));
                dialog = dialogBuilder.create();
                break;

        default:
            dialog = null;
        }
        return dialog;
    }
    
    private void releasePlayer(){
        Log.d(logTag,"Stop subtitle player.");
        if(playBarTimer!=null){
                playBarTimer.cancel();
                playBarTimer.purge();
        }
        Log.d(logTag,"Stop media player.");
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
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

        
        public void onPrepared(MediaPlayer arg0) {
                int videoWidth = mediaPlayer.getVideoWidth();
                int videoHeight = mediaPlayer.getVideoHeight();
                if (videoHeight != 0 && videoWidth != 0) {
                        arg0.start();
                }
                Log.e("mediaPlayer", "onPrepared");
        }
        
        @Override
        public void onBufferingUpdate(MediaPlayer arg0, int bufferingProgress) {
                playBar.setSecondaryProgress(bufferingProgress);
                int currentProgress=playBar.getMax()*mediaPlayer.getCurrentPosition()/mediaPlayer.getDuration();
                Log.e(currentProgress+"% play", bufferingProgress + "% buffer");
 
        }
        

        class SubtitlePlayTask extends TimerTask{
                boolean start=true;
                public void stopThread(){start=false;}
        @Override
        public void run() {
//              if()
          int mediaPosition=-1;
          try{
                  mediaPosition=mediaPlayer.getCurrentPosition();
          }catch(NullPointerException npe){
                  Log.e(logTag,"The media player has gone. it may happen on normal switch file or release stage.");
                  npe.printStackTrace();
          }
          playBar.setProgress(mediaPosition);
          if(currentPlayedSubtitleIndex<subtitle.length-1 && mediaPosition>=subtitle[currentPlayedSubtitleIndex+1].startTimeMs){
                          final SubtitleElement se =subtitle[++currentPlayedSubtitleIndex];
                          runOnUiThread(new Runnable() {
                                                public void run() {
//                                                      try{
                                                                        subtitleView.setText(subtitle[currentPlayedSubtitleIndex].text.toCharArray(), 0, se.text.length());
/*                                                                      }catch(IndexOutOfBoundsException ioobe){
                                                                Log.e(logTag,"Error happen while display subtitle: "+new String(subtitle[currentPlayedSubtitleIndex].text.toCharArray()));
                                                                ioobe.printStackTrace();
                                                        }
*/                                                              }});
          }
        }
          }
}
