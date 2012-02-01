package eyes.blue;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;

public class OptCtrlPanel extends Activity {
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.test);
	}
	
	public void onBackPressed(){
//		super.onBackPressed();
		System.out.println("Into back pressed!!");
		Intent intent=new Intent();
//		intent.putExtra("BookFontSize", ((EditText)findViewById(R.id.bookFontSizeEdit)).getText());
//		intent.putExtra("SubtitleFontSize", ((EditText)findViewById(R.id.subtitleFontSizeEdit)).getText());
//		intent.putExtra("SubtitleLineCount", ((EditText)findViewById(R.id.subtitleDisplayLineCount)).getText());

		setResult(RESULT_OK,intent);
		finish();
	}
}
