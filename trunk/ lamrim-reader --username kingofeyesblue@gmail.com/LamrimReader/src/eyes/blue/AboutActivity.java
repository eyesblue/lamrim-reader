package eyes.blue;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.widget.TextView;

public class AboutActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		TextView aboutTextView=(TextView) findViewById(R.id.aboutTextView);
		aboutTextView.setText(Html.fromHtml(getString(R.string.aboutUs)));
	}
}
