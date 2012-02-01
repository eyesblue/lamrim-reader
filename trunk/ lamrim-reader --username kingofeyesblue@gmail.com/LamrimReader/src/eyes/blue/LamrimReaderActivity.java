package eyes.blue;


import java.io.BufferedReader;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import eyes.blue.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class LamrimReaderActivity extends Activity {
    /** Called when the activity is first created. */
static //	private static final String MEDIA = "media";
	String logTag="LamrimReader";
	Intent optCtrlPanel=null;
	private MediaPlayer mMediaPlayer;
	static SubtitleElement[] subtitle;
	TextView bookView;
	TextView subtitleView;
	SeekBar playBar;
	SharedPreferences options;
	SharedPreferences runtime;
	public final static String optionsFile="options";
	public final static String runtimeStatusFile="runtime";
	public final static String bookFontSizeKey="FontSize";
	public final static String subtitleFontSizeKey=bookFontSizeKey;
	public final static String subtitleLineCountKey="SubtitleLineCount";
	public final static int defBookFontSize=18;
	public final static int defSubtitleFontSize=24;
	public final static int defSubtitleLineCount=2;
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
//        setContentView(R.layout.test);
        options = getSharedPreferences(optionsFile, 0);
        runtime = getSharedPreferences(runtimeStatusFile, 0);
        int bookFontSize=options.getInt(bookFontSizeKey, defBookFontSize);
        int subtitleFontSize=options.getInt(subtitleFontSizeKey, defSubtitleFontSize);
        int subtitleLineCount=options.getInt(subtitleLineCountKey, defSubtitleLineCount);
        
        optCtrlPanel = new Intent(LamrimReaderActivity.this,
				OptCtrlPanel.class);
        bookView=(TextView)findViewById(R.id.bookView);
        bookView.setMovementMethod(new ScrollingMovementMethod());
        bookView.setTextSize(bookFontSize);
        
        subtitleView=(TextView)findViewById(R.id.subtitleView);
        subtitleView.setTextSize(subtitleFontSize);
        subtitleView.setLines(subtitleLineCount);
        
        playBar=(SeekBar)findViewById(R.id.playBar);
        playBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

			@Override
			public void onProgressChanged(SeekBar playBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				if(!fromUser)return;
				
				playBar.setProgress(progress);
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
        playAudio();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0,0,0,R.string.readingMode);
    	menu.add(0,1,1,R.string.listenMode);
    	menu.add(0,2,2,R.string.showCtrlPanel);
    	menu.add(0,3,3,R.string.showAbout);
    	return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch (item.getItemId()) {
		
		case 0:
			return true;
		case 1:
			return true;
		case 2:
			startActivityForResult(optCtrlPanel, 0);
			
			return true;
		case 3:
			return true;
		}
		return false;
    }
    
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	final int bookFontSize=Integer.parseInt(data.getCharSequenceExtra("BookFontSize").toString());
    	final int subtitleFontSize=Integer.parseInt(data.getCharSequenceExtra("SubtitleFontSize").toString());
    	final int subtitleLineCount=Integer.parseInt(data.getCharSequenceExtra("SubtitleLineCount").toString());

    	runOnUiThread(new Thread() {
    		@Override
			public void run() {
				if(bookView.getTextSize()!=bookFontSize)bookView.setTextSize(bookFontSize);
				if(subtitleView.getTextSize()!=subtitleFontSize)subtitleView.setTextSize(subtitleFontSize);
				if(subtitleView.getLineCount()!=subtitleLineCount)subtitleView.setLines(subtitleLineCount);
			}
		});
    	
    }
    
    private static int subtitleSearch(SubtitleElement[] se,int ms){
    	for(int i=0;i<se.length-1;i++){
    		Log.d("Subtitle Runner","Compare play time "+ms+" with subtitle["+i+"]="+se[i].startTimeMs+", subtitles length: "+se.length);
    		if( ms < se[i+1].startTimeMs && ms > se[i].startTimeMs )return i;
    	}
    	return 0;
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
		Log.e(logTag, "Binary search state error, shouldn't go to the unknow stage. this may cause by a not sorted subtitle.", new Exception("Binary search state error, shouldn't go to the unknow stage. this may cause by a not sorted subtitle."));
		return -1;
    }
    private void playAudio() {
        try {

                    /**
                     * TODO: Upload a audio file to res/raw folder and provide
                     * its resid in MediaPlayer.create() method.
                     */
        	File f=new File("/sdcard/001A.MP3");
       		Log.d("Lamrim Reader:", "Open the "+f.getAbsolutePath()+" for test.");
        	
            mMediaPlayer = MediaPlayer.create(this, Uri.fromFile(f));
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
            	  final Timer playBarTimer=new Timer();
            	  playBarTimer.schedule(new TimerTask(){
                      @Override
                      public void run() {
//                    	  int i=subtitleSearch(subtitle,mp.getCurrentPosition());
                    	  int i=subtitleBSearch(subtitle,mp.getCurrentPosition());
                    	  final SubtitleElement se =subtitle[i];
                    	  playBar.setProgress(mp.getCurrentPosition());
                    	  runOnUiThread(new Runnable() {
    							public void run() {
    								subtitleView.setText(se.text.toCharArray(), 0, se.text.length());
    							}});
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
						System.out.println("Find a subtitle start: "+stemp);
						se=new SubtitleElement();
						serial=Integer.parseInt(stemp);
						step=1;
					}
				}
				// This may find the time description
				else if(step==1){
					if(stemp.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")){
						System.out.println("Get time string: "+stemp);
						int startTimeMs;
						String ts=stemp.substring(0,2);
						System.out.println("Hour: "+ts);
						startTimeMs=Integer.parseInt(ts)*3600000;
						ts=stemp.substring(3,5);
						System.out.println("Min: "+ts);
						startTimeMs+=Integer.parseInt(ts)*60000;
						ts=stemp.substring(6,8);
						System.out.println("Sec: "+ts);
						startTimeMs+=Integer.parseInt(ts)*1000;
						ts=stemp.substring(9,12);
						System.out.println("Sub: "+ts);
						startTimeMs+=Integer.parseInt(ts);
						
						System.out.println("Set time: "+startTimeMs);
						se.startTimeMs=startTimeMs;
						step=2;
					}
					else {
						System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
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
/*
    protected void onStart(){
    	super.onStart(savedInstanceState);
    }
    
    protected void onRestart(){}

    protected void onResume(){}

    protected void onPause(){}

    protected void onStop(){}

    protected void onDestroy(){}
*/
        
}