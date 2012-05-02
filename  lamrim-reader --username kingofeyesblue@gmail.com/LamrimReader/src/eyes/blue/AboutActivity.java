package eyes.blue;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.widget.TextView;

public class AboutActivity extends Activity {
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.about);
		TextView aboutTextTitle=(TextView) findViewById(R.id.aboutTextTitle);
		aboutTextTitle.setText(R.string.app_name);
		TextView aboutTextContent=(TextView) findViewById(R.id.aboutTextContent);
		aboutTextContent.setMovementMethod(new ScrollingMovementMethod());
		aboutTextContent.setText(Html.fromHtml(getString(R.string.aboutDesc)));
	}
}
