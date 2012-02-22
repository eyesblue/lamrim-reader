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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
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
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class LamrimReaderActivity extends Activity implements OnAudioFocusChangeListener, OnBufferingUpdateListener, OnPreparedListener {
    /** Called when the activity is first created. */
    static String logTag="LamrimReader";
        
    final static int NORMAL_MODE=0;
    final static int READING_MODE=1;
    final static int RECODE_SECTION_MODE=2;
    final static int TV_MODE=3;
    
//    final static int DIALOG_DOWNLOAD_FAIL=0;
    static SubtitleElement[] subtitle = null;
        int currentPlayedSubtitleIndex = -1;
        int subtitleWordMax = 0;
//      int subtitleMonInterval = -1;
//    AlertDialog.Builder dialogBuilder =null;
//    Intent optCtrlPanel=null;
    private MediaPlayer mediaPlayer=null;
        
    ListView bookView;
        TextView subtitleView;
        SeekBar playBar;
        SharedPreferences options;
        SharedPreferences runtime;
        int appMode=0;
        int playingMediaIndex=-1;
        int playerStartPosition=-1;
        
        Timer playBarTimer=new Timer();
        ArrayList<HashMap<String,String>> bookList =null;
//      String bookFontSizeKey=null;
//      String subtitleFontSizeKey=null;
//      String subtitleLineCountKey=null;

//      int defBookFontSize=R.integer.defBookFontSize;
//      int fontSizeArraylength=0;
        FileSysManager fileSysManager=null;
//      String remoteSite[]=null;
        
    @Override
    public void onCreate(Bundle savedInstanceState) {

//      try{
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

//        fontSizeArraylength=getResources().getIntArray(R.array.fontSizeArray).length;
                
        setContentView(R.layout.main);
        
        Log.d(logTag,"Into LamrimReader.onCreate");
        
        options = getSharedPreferences(getString(R.string.optionFile), 0);
        runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
        appMode=getResources().getInteger(R.integer.defAppMode);
        appMode=options.getInt(getString(R.string.appModeKey), appMode);
//        remoteSite=getResources().getStringArray(R.array.remoteSite);

        if(fileSysManager==null)fileSysManager=new FileSysManager(this);
        FileSysManager.checkFileStructure();

        
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(getString(R.string.subtitleFontSizeKey), subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(getString(R.string.subtitleLineCount), subtitleLineCount);


        // Contruct the content of book with the bookViewList
        getAllBookContent();
//        bookView.setMovementMethod(new ScrollingMovementMethod());
//        bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);

        // For Demo
//        String[] bookContent=getResources().getStringArray(R.array.book);
//        String bookPages=bookContent[63]+"\n"+bookContent[64]+"\n"+bookContent[65]+"\n"+bookContent[66]+"\n"+bookContent[67]+"\n"+bookContent[68]+"\n"+bookContent[69]+"\n"+bookContent[70]+"\n"+bookContent[71]+"\n"+bookContent[72];
//        bookView.setText(bookPages);
        
        Log.d(logTag,"Leave onCreate");
        
        subtitleView=(TextView)findViewById(R.id.subtitleView);
        subtitleView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[subtitleFontSize]);
        subtitleView.setLines(subtitleLineCount);

//        subtitleMonInterval=getResources().getInteger(R.integer.subtitleMonInterval);
        
        
        playBar=(SeekBar)findViewById(R.id.playBar);
        playBar.setEnabled(false);
        playBar.setClickable(false);
        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

        @Override
        public void onProgressChanged(SeekBar playBar, int progress, boolean fromUser) {
        // For avoid the critical with subtitle player, we cancel the subtitle player and reset the currentPlayedSubtitleIndex, restart subtitle player while all ready.
                
                if(!fromUser)return;
                if(mediaPlayer!=null&&mediaPlayer.isPlaying())mediaPlayer.seekTo(progress);
                        
                if(subtitle==null)return;
                synchronized(playBarTimer){
                        currentPlayedSubtitleIndex=subtitleBSearch(subtitle,progress);
                        final char[] text=subtitle[currentPlayedSubtitleIndex].text.toCharArray();
                                setSubtitleViewText(text);
                }
        }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {}});
/*      }catch(Exception e){
                LogRepoter.log(e.toString());
        }
*/    }
    
    private void getAllBookContent(){
        bookView=(ListView)findViewById(R.id.bookPageGrid);
        String[] bookPage=getResources().getStringArray(R.array.book);

        bookList=new ArrayList<HashMap<String,String>>();
        ArrayList<int[]> pointList=new ArrayList<int[]>();
        int pIndex=0;
        
                for(String value:bookPage){
                        HashMap<String,String> item = new HashMap<String,String>();
/*                        
                        int line=0;int index=0;
                        String newStr="";
                        for(char c: value.toCharArray()){
                        	index++;
                        	if(c=='\n'){line++;index=0;}
                        	else if(c=='.'){
                        		pointList.add(new int[]{line,index});
                        		//Log.d("LamrimReader","Find point at: "+line+", "+index);
                        		continue;
                        	}
                        	if(c=='\r')Log.d(logTag, "Find \\r exist");
                        	newStr+=c;
                        }

                        item.put("page", newStr);
*/
                        item.put("page", value);
                        item.put("desc", "第 "+(++pIndex)+" 頁");
                        bookList.add( item );
                }
                SimpleAdapter adapter = new SimpleAdapter(this, bookList, R.layout.theory_page_view, new String[] { "page","desc" },
                                new int[] { R.id.pageContentView, R.id.pageNumView } );
                bookView.setAdapter( adapter );

                // Setup the book page content
//              int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
//              bookFontSize=options.getInt(getString(R.string.bookFontSizeKey), bookFontSize);
//              TextView tv=(TextView) findViewById(R.id.speechTitle);
//              tv.setMovementMethod(new ScrollingMovementMethod());
//              tv.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
    }
    
    /*
     * Set the message on the subtitle view, there should check the subtitleView is not playing, or hide the message.
     * */
    private void setSubtitleViewText(String s){setSubtitleViewText(s.toCharArray());}
    private void setSubtitleViewText(final char[] b){
        runOnUiThread(new Runnable() {
        	public void run() {
        		subtitleView.setText(b, 0, b.length);
        	}});
    }
    
    private void switchMode(int mode){
        final LinearLayout.LayoutParams mainLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.FILL_PARENT,3);
        final LinearLayout.LayoutParams bottomLayout=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,0);
        
        switch(mode){
                case NORMAL_MODE:
                	Log.d(logTag,"Switch to Normal mode.");
                	((LinearLayout)findViewById(R.id.rootLayout)).setGravity(Gravity.CENTER);
//                      bookView.setLayoutParams(mainLayout);
                	findViewById(R.id.bookPageGrid).setVisibility(View.VISIBLE);

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
                case RECODE_SECTION_MODE:
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
                        findViewById(R.id.bookPageGrid).setVisibility(View.GONE);
                        subtitleView.setLayoutParams(mainLayout);
                        subtitleView.setGravity(Gravity.CENTER);
                        subtitleView.setVisibility(View.VISIBLE);

                        ((LinearLayout)findViewById(R.id.audioPanel)).setVisibility(View.VISIBLE);
                        appMode=mode;
                        break;
        }
    }
    
    private int getSubtitleWordCountMax(TextView view){
        // Determine how many words can show per line.
        DisplayMetrics dm = new DisplayMetrics();
        this.getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenWidth=dm.widthPixels;
        int count=(int)((float)screenWidth/view.getPaint().measureText("中"));
        Log.d(logTag,"Width of screen: "+screenWidth+", Width of Word: "+view.getPaint().measureText("中")+", There are "+count+" can show in one line.");
        return  count;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,NORMAL_MODE,0,R.string.normalModeDesc);
        menu.add(0,READING_MODE,1,R.string.readingModeDesc);
        menu.add(0,RECODE_SECTION_MODE,2,R.string.noSubtitleModeDesc);
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
                case NORMAL_MODE:saveRuntime();switchMode(NORMAL_MODE);loadRuntime(NORMAL_MODE);return true;
                case READING_MODE:saveRuntime();switchMode(READING_MODE);loadRuntime(READING_MODE);return true;
                case RECODE_SECTION_MODE:saveRuntime();switchMode(RECODE_SECTION_MODE);loadRuntime(RECODE_SECTION_MODE);return true;
                case TV_MODE:saveRuntime();switchMode(TV_MODE);loadRuntime(TV_MODE);return true;
                case 4:
                        final Intent optCtrlPanel=new Intent(LamrimReaderActivity.this,OptCtrlPanel.class);
                        startActivityForResult(optCtrlPanel, 0);
                        return true;
                case 5: return true;
        }
                return false;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        
        final int bookFontSize=intent.getIntExtra(getString(R.string.bookFontSizeKey), R.integer.defBookFontSize);
        final int subtitleFontSize=intent.getIntExtra(getString(R.string.subtitleFontSizeKey), R.integer.defSubtitleFontSize);
        final int subtitleLineCount=intent.getIntExtra(getString(R.string.subtitleLineCount), R.integer.defSubtitleLineCount);

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
/*                                if(bookView.getTextSize()!=bookFontSize){
                                        Log.d(logTag,"Set book font size to "+getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
                                        bookView.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
                                }
*/                                if(subtitleView.getTextSize()!=subtitleFontSize){
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
    private void startSubtitlePlayer(int index){
        // Load the subtitle file, if not exist, show "no subtitle" on subtitle view.
        // Before load subtitle, we should check is the subtitle has downloaded and installed.
        File subtitleFile=FileSysManager.getLocalSubtitleFile(index);
        if(subtitleFile.exists()){
                subtitle=Util.loadSubtitle(subtitleFile);
                runOnUiThread(new Runnable() {
                public void run() {
                        playBar.setEnabled(true);
                        playBar.setClickable(true);
                }
            });
        }
        
        // There is no subtitle for the speech media, just show "no title" on subtitle view and return. 
        else{
                subtitle=null;
                setSubtitleViewText(getResources().getString(R.string.noSubtitleDesc));
                return;
        }
        
        if(playBarTimer==null)playBarTimer=new Timer();
        playBarTimer.schedule(
         new TimerTask(){
        @Override
        public void run() {
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
          }, 0, getResources().getInteger(R.integer.subtitleMonInterval));
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
                public void downloadMediaFail(int index){
                        setSubtitleViewText(getResources().getString(R.string.downloadFail));
//                      Looper.prepare();
//                    onCreateDialog(DIALOG_DOWNLOAD_FAIL).show();
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
                public void downloadMediaFinish(int fileIndex){
                        Log.d(logTag,getResources().getStringArray(R.array.fileName)[fileIndex]+" Download finish, call LamrimReader.playAudio("+fileIndex+") again");
                        playAudio(fileIndex);
                        setSubtitleViewText("無字幕");
                }
            });
            
            
            
            
            
            
            if(!FileSysManager.isFileValid(index)){
//      Here should show dialog to user that will be start download file from internet. 
                Log.d(logTag,"The file which alreadly in phone is not valid");
                setSubtitleViewText(getResources().getString(R.string.downloadingDesc));
                FileSysManager.downloadFileFromRemote(index);
                Log.d(logTag,Thread.currentThread().getName()+": return to onStart()");
                return;
            }
            Log.d(logTag,"Found it!");
            
            
            
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
            
//            URLConnection con = url.openConnection();
//            con.connect();
//            con.getContent(); //This is needed or setDataSource will throw IOException
//            
            File file=FileSysManager.getLocalMediaFile(index);
            
            try{
//              FileInputStream fis=new FileInputStream(file);
                Log.d(logTag,"Play media: "+file.getAbsolutePath());
//                              For mediaPlayer.getDuration(), while start mediaPlayer with prepareAsync(), the time is not precision enough, we start with create() function for this.                 
                if(mediaPlayer==null)mediaPlayer = MediaPlayer.create(this, Uri.fromFile(file));
/*                if(mediaPlayer!=null)mediaPlayer.reset();
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setDataSource(fis.getFD());
                mediaPlayer.prepareAsync();
*/
            }catch(IllegalStateException ise){ise.printStackTrace();}
//            catch(IOException ioe){ioe.printStackTrace();}
            catch(IllegalArgumentException iae){iae.printStackTrace();}
//            catch (FileNotFoundException e) {e.printStackTrace();}
//            catch (IOException e) {e.printStackTrace();}
                

//            mMediaPlayer.setDataSource("http://lamrimreader.eyes-blue.com/appresources/100A.MP3");
//            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mediaPlayer.setOnBufferingUpdateListener(this);
//                      mediaPlayer.setOnPreparedListener(this);
            
            
            mediaPlayer.setWakeMode(getApplicationContext(), PowerManager.SCREEN_BRIGHT_WAKE_LOCK);


            /*            While mediaplayer start with create(), there is no need the onPrepared() function.
            mediaPlayer.setOnPreparedListener(new OnPreparedListener(){
 
                
                

                        
              @Override
              public void onPrepared(final MediaPlayer mp) 
              {
                  Log.d(logTag,"Into OnPrepared function");
                  playBar.setMax(mp.getDuration());
                  mediaPlayer.start();
                  playBar.setEnabled(true);
                  playBar.setClickable(true);
                  startSubtitlePlayer();
                  
              }

          });
*/
    
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

            
            // The Duration list has error, must correct before use it.
            //playBar.setMax(getResources().getIntArray(R.array.duration)[index]);
            
            Log.d(logTag,"Play start positon = "+playerStartPosition);
            if(playerStartPosition>0){
                Log.d(logTag,"Media player seek to last play point "+playerStartPosition);
                mediaPlayer.seekTo(playerStartPosition);
            }
            playBar.setMax(mediaPlayer.getDuration());
            startSubtitlePlayer(index);
            playingMediaIndex=index;
            mediaPlayer.start();
            runOnUiThread(new Runnable() {
                public void run() {
                        playBar.setEnabled(true);
                        playBar.setClickable(true);
                }
            });
    }

    
    public void saveRuntime(){
        // Save status of the mode now in used.
        SharedPreferences.Editor editor =runtime.edit();
        
        switch(appMode){
        case NORMAL_MODE:
                
//              getTheoryIndex
                boolean mediaPlayerState=false;
//              if(mediaPlayer!=null&&mediaPlayer.isPlaying())mediaPlayerState=true;
                if(mediaPlayer!=null)mediaPlayerState=true;
                editor.putBoolean(getResources().getString(R.string.runtimeNormalPlayerState), mediaPlayerState);
                if(mediaPlayerState){
                        editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex), playingMediaIndex);
                        editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition), mediaPlayer.getCurrentPosition());
                        Log.d(logTag,"Save runtime status: playStartPosition="+mediaPlayer.getCurrentPosition());
                }
                editor.commit();
                break;
        case READING_MODE:
                // Get the theory index and save.
                break;
//      case NOSUBTITLE_MODE:switchMode(NOSUBTITLE_MODE);break;
        case TV_MODE:
                if(mediaPlayer!=null){
//                      editor.putInt(getResources().getString(R.string.runtimeNormalPlayingIndex), playingMediaIndex);
//                      editor.putInt(getResources().getString(R.string.runtimeNormalPlayingPosition), mediaPlayer.getCurrentPosition());
                }
                editor.commit();
                break;
        }
    }
    
    public void loadRuntime(int mode){
        switch(mode){
        case NORMAL_MODE:
//              getTheoryIndex
                Log.d(logTag,"Load runtime status of Normal_Mode");
                if(mediaPlayer!=null)releasePlayer();
                playerStartPosition=runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingPosition), playerStartPosition);
                Log.d(logTag,"Load back the playerStartPosition="+playerStartPosition);
                boolean mediaPlayerState=runtime.getBoolean(getResources().getString(R.string.runtimeNormalPlayerState), false);
                if(mediaPlayerState)playAudio(runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingIndex), playingMediaIndex));
                break;
        case READING_MODE:
                // Get the theory index and save.
                break;
//      case NOSUBTITLE_MODE:switchMode(NOSUBTITLE_MODE);break;
        case TV_MODE:
                if(mediaPlayer!=null){
                        playerStartPosition=runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingPosition), playerStartPosition);
                mediaPlayerState=runtime.getBoolean(getResources().getString(R.string.runtimeNormalPlayerState), false);
                if(mediaPlayerState)playAudio(runtime.getInt(getResources().getString(R.string.runtimeNormalPlayingIndex), playingMediaIndex));
                }
                break;
        }
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
    
    


    
    @Override
    public void onBackPressed(){
        releasePlayer();
        SharedPreferences.Editor editor =options.edit();
        editor.putInt(getString(R.string.subtitleFontSizeKey), appMode);
        editor.commit();
        finish();
    }

    protected void onStart(){
        super.onStart();
        
        Intent intent=getIntent();
        final int searchingMode=intent.getIntExtra(this.getResources().getString(R.string.searchingType), -1);
        final int mediaPosition=intent.getIntExtra("index", -1);
        // !!!!!! The activity is not start by another intent, it should show menu back. !!!!!!!!!
        if(searchingMode==-1 || mediaPosition == -1){}
        
        if(searchingMode==getResources().getInteger(R.integer.PLAY_FROM_MEDIA)){
                Log.d(logTag,"Play from media "+mediaPosition);
                new Thread("Play thread"){
                public void run(){
                        playAudio(mediaPosition);
                        }
                }.start();
        }
        else if(searchingMode==getResources().getInteger(R.integer.PLAY_FROM_THEORY)){
                Log.d(logTag,"Play from theory "+mediaPosition);
        }
        
        switchMode(appMode);
        
                // Setup the book page content, this can't put in onCreate, will cause null point.
//              int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
//              bookFontSize=options.getInt(getString(R.string.bookFontSizeKey), bookFontSize);
//              TextView tv=(TextView) findViewById(R.id.speechTitle);
//              tv.setMovementMethod(new ScrollingMovementMethod());
//              tv.setTextSize(getResources().getIntArray(R.array.fontSizeArray)[bookFontSize]);
        
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
   //land
     if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){}
   //port
     else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT){}
    }
    

/*    The function cause Thread hang on, it may not a correct version.
    
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
*/    
    private void releasePlayer(){
        Log.d(logTag,"Stop subtitle player.");
        if(playBarTimer!=null){
                playBarTimer.cancel();
                playBarTimer.purge();
                playBarTimer=null;
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
}
