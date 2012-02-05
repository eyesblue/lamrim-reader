package eyes.blue;

import android.app.Activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class OptCtrlPanel extends Activity {
	final static String logTag="OptCtrlPanel";
	SharedPreferences options;
	String bookFontSizeKey=null;
	String subtitleFontSizeKey=null;
	String subtitleLineCountKey=null;
	CheckBox isDisplayBookBox=null;
	CheckBox isDisplaySubtitleBox=null;
	CheckBox isPlayAudioBox=null;
	SeekBar bookFontSizeBar=null;
	SeekBar subtitleFontSizeBar=null;
	EditText subtitleLineCountText=null;
	int defBookFontSize=R.integer.defBookFontSize;
	int[] fontSizeArray=null;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.options);
		options = getSharedPreferences(getString(R.string.optionFile), 0);
		
		bookFontSizeKey=getString(R.string.bookFontSizeKey);
		subtitleFontSizeKey=getString(R.string.subtitleFontSizeKey);
		subtitleLineCountKey=getString(R.string.subtitleLineCount);
		bookFontSizeBar=((SeekBar)findViewById(R.id.bookFontSize));
		subtitleFontSizeBar=((SeekBar)findViewById(R.id.subtitleFontSize));
		subtitleLineCountText=((EditText)findViewById(R.id.subtitleLineCount));
		fontSizeArray=getResources().getIntArray(R.array.fontSizeArray);
		
		Log.d(logTag,"Book font size key="+bookFontSizeKey+", subtitle font size key="+subtitleFontSizeKey);
		bookFontSizeBar.setMax(fontSizeArray.length-1);
		bookFontSizeBar.setOnSeekBarChangeListener(new FontSizeIndicator((TextView) findViewById(R.id.BookFontSizeDesc)));
		subtitleFontSizeBar.setMax(fontSizeArray.length-1);
		subtitleFontSizeBar.setOnSeekBarChangeListener(new FontSizeIndicator((TextView) findViewById(R.id.subtitleFontSizeDesc)));
	        
	}
	@Override
	protected void onStart(){
		super.onStart();
		
		// Get options
		int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
        // Set options
        bookFontSizeBar.setProgress(bookFontSize);
        subtitleFontSizeBar.setProgress(subtitleFontSize);
        subtitleLineCountText.setText(""+subtitleLineCount);
    }
	
	public void onBackPressed(){
//		super.onBackPressed();
		System.out.println("Into back pressed!!");
		
		int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
        int uiBookFontSize=bookFontSizeBar.getProgress();
        int uiSubtitleFontSize=subtitleFontSizeBar.getProgress();
        Log.d(logTag,"Get book font size: "+bookFontSize+", subtitle font size: "+uiSubtitleFontSize);
        int uiSubtitleLineCount=Integer.parseInt(subtitleLineCountText.getText().toString());
        
        
        SharedPreferences.Editor editor =options.edit();
        if(bookFontSize!=uiBookFontSize)
        	editor.putInt(bookFontSizeKey, uiBookFontSize);
        if(subtitleFontSize!=uiSubtitleFontSize)
        	editor.putInt(subtitleFontSizeKey, uiSubtitleFontSize);
        if(subtitleLineCount!=uiSubtitleLineCount)
        	editor.putInt(subtitleLineCountKey, uiSubtitleLineCount);
        
        editor.commit();
        
        Bundle b=new Bundle();
        b.putInt(bookFontSizeKey, uiBookFontSize);
        b.putInt(subtitleFontSizeKey, uiSubtitleFontSize);
        b.putInt(subtitleLineCountKey, uiSubtitleLineCount);
        
		setResult(RESULT_OK,new Intent().putExtras(b));
		finish();
	}
	
	class FontSizeIndicator implements SeekBar.OnSeekBarChangeListener{
		TextView tv=null;
		public FontSizeIndicator(TextView tv){
			this.tv=tv;
		}

		@Override
		public void onProgressChanged(SeekBar seekBar, final int progress,
				boolean fromUser) {
			if(!fromUser)return;
			
			Log.d(logTag,"Change progress to "+progress);
			runOnUiThread(new Runnable() {
				public void run() {
					tv.setTextSize(fontSizeArray[progress]);
				}});
		}

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {}
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {}
	}
}
