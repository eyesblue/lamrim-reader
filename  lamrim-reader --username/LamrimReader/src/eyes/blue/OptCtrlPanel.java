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
	String isDisplayBookKey=null;
	String isDisplaySubtitleKey=null;
	String isPlayAudioKey=null;
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
		
		isDisplayBookKey=getString(R.string.isDisplayBook);
		isDisplaySubtitleKey=getString(R.string.isDisplaySubtitle);
		isPlayAudioKey=getString(R.string.isPlayAudio);
		bookFontSizeKey=getString(R.string.bookFontSizeKey);
		subtitleFontSizeKey=getString(R.string.subtitleFontSizeKey);
		subtitleLineCountKey=getString(R.string.subtitleLineCount);
//		isDisplayBookBox=((CheckBox)findViewById(R.id.isDisplayBook));
//		isDisplaySubtitleBox=((CheckBox)findViewById(R.id.isDisplaySubtitle));
//		isPlayAudioBox=((CheckBox)findViewById(R.id.isPlayAudio));
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
		boolean isDisplayBook=options.getBoolean(isDisplayBookKey, true);
		boolean isDisplaySubtitle=options.getBoolean(isDisplaySubtitleKey, true);
		boolean isPlayAudio=options.getBoolean(isPlayAudioKey, true);
		int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
        // Set options
        isDisplayBookBox.setChecked(isDisplayBook);
        isDisplaySubtitleBox.setChecked(isDisplaySubtitle);
        isPlayAudioBox.setChecked(isPlayAudio);
        bookFontSizeBar.setProgress(bookFontSize);
        subtitleFontSizeBar.setProgress(subtitleFontSize);
        subtitleLineCountText.setText(""+subtitleLineCount);
    }
	
	public void onBackPressed(){
//		super.onBackPressed();
		System.out.println("Into back pressed!!");
		
		
		boolean isDisplayBook=options.getBoolean(isDisplayBookKey, true);
		boolean isDisplaySubtitle=options.getBoolean(isDisplaySubtitleKey, true);
		boolean isPlayAudio=options.getBoolean(isPlayAudioKey, true);
		int bookFontSize=getResources().getInteger(R.integer.defBookFontSize);
        bookFontSize=options.getInt(bookFontSizeKey, bookFontSize);
        int subtitleFontSize=getResources().getInteger(R.integer.defSubtitleFontSize);
        subtitleFontSize=options.getInt(subtitleFontSizeKey, subtitleFontSize);
        int subtitleLineCount=getResources().getInteger(R.integer.defSubtitleLineCount);
        subtitleLineCount=options.getInt(subtitleLineCountKey, subtitleLineCount);
        
		boolean uiIsDisplayBook=isDisplayBookBox.isChecked();
        boolean uiIsDisplaySubtitle=isDisplaySubtitleBox.isChecked();
        boolean uiIsPlayAudio=isPlayAudioBox.isChecked();
        int uiBookFontSize=bookFontSizeBar.getProgress();
        int uiSubtitleFontSize=subtitleFontSizeBar.getProgress();
        Log.d(logTag,"Get book font size: "+bookFontSize+", subtitle font size: "+uiSubtitleFontSize);
        int uiSubtitleLineCount=Integer.parseInt(subtitleLineCountText.getText().toString());
        
        
        SharedPreferences.Editor editor =options.edit();
        if(isDisplayBook!=uiIsDisplayBook)
        	editor.putBoolean(isDisplayBookKey, uiIsDisplayBook);
        if(isDisplaySubtitle!=uiIsDisplaySubtitle)
        	editor.putBoolean(isDisplaySubtitleKey, uiIsDisplayBook);
        if(isPlayAudio!=uiIsPlayAudio)
        	editor.putBoolean(isPlayAudioKey, uiIsPlayAudio);
        if(bookFontSize!=uiBookFontSize)
        	editor.putInt(bookFontSizeKey, uiBookFontSize);
        if(subtitleFontSize!=uiSubtitleFontSize)
        	editor.putInt(subtitleFontSizeKey, uiSubtitleFontSize);
        if(subtitleLineCount!=uiSubtitleLineCount)
        	editor.putInt(subtitleLineCountKey, uiSubtitleLineCount);
        
        editor.commit();
        
        Bundle b=new Bundle();
        b.putBoolean(isDisplayBookKey, uiIsDisplayBook);
        b.putBoolean(isDisplaySubtitleKey, uiIsDisplaySubtitle);
        b.putBoolean(isPlayAudioKey, uiIsPlayAudio);
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
