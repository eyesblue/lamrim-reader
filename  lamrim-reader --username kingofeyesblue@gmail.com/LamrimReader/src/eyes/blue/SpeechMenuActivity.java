package eyes.blue;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.LauncherActivity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SpeechMenuActivity extends ListActivity {
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	SimpleAdapter adapter=null;
	ListView speechMenu=null;
	Intent playWindow=null;
	
	@Override
	 public void onCreate(Bundle savedInstanceState) {
		
	 super.onCreate(savedInstanceState);
	 setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//	 setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);	//將螢幕轉成横式
//	 setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
	 if(playWindow==null)
		 playWindow=new Intent(SpeechMenuActivity.this,LamrimReaderActivity.class);
	 
//	 setContentView(R.layout.speech_menu);
	 //注意：不能使用main中的layout，用了會出現錯誤
	 //setContentView(R.layout.main);
	 
	 //setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, R.array.fileName));
	 speechMenu = (ListView) findViewById(R.layout.speech_menu);
	 String[] speechTitle=getResources().getStringArray(R.array.fileName);
//	 String[] speechDesc=getResources().getStringArray(R.array.speechDesc);

	 for(String value:speechTitle){
		 HashMap<String,String> item = new HashMap<String,String>();
		 item.put( "food", null);
		 item.put( "place",null );
		 item.put("title", value);
		 item.put("desc", "Descript here");
		 list.add( item );
		 }


	 //新增SimpleAdapter
	 adapter = new SimpleAdapter(this, list, R.layout.speech_row, new String[] { "title","desc" },
			 new int[] { R.id.speechTitle, R.id.speechDesc } );
	 //setListAdapter(new ArrayAdapter<String>(this,  R.layout.speech_menu));
	 setListAdapter( adapter );
	 
	 //啟用按鍵過濾功能
	 getListView().setTextFilterEnabled(true);
	 }
	
	public void onListItemClick(ListView l, View v, int position, long id){
		Log.d(getString(R.string.app_name),"Speech menu "+position+"th item clicked.");
		playWindow.putExtra(this.getResources().getString(R.string.searchingType), this.getResources().getInteger(R.integer.PLAY_FROM_MEDIA));
		playWindow.putExtra("index", position);
		this.startActivity(playWindow);
	}
}
