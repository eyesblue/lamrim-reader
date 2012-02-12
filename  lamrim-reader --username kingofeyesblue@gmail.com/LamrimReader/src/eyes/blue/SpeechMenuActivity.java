package eyes.blue;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.LauncherActivity;
import android.app.ListActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class SpeechMenuActivity extends ListActivity {
	ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String,String>>();
	SimpleAdapter adapter;
	ListView speechMenu;
	
	@Override
	 public void onCreate(Bundle savedInstanceState) {
	 super.onCreate(savedInstanceState);
//	 setContentView(R.layout.speech_menu);
	 //注意：不能使用main中的layout，用了會出現錯誤
	 //setContentView(R.layout.main);
	 
	 //setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, R.array.fileName));
	 speechMenu = (ListView) findViewById(R.layout.speech_menu);
	 String[] speechTitle=getResources().getStringArray(R.array.fileName);
//	 String[] speechDesc=getResources().getStringArray(R.array.speechDesc);
	 for(int i=0; i<speechTitle.length; i++){
		 HashMap<String,String> item = new HashMap<String,String>();
//		 item.put( "food", null);
//		 item.put( "place",null );
		 item.put(speechTitle[i], "2nd argument");
		 list.add( item );
		 }
	 
	 //新增SimpleAdapter
	 //adapter = new SimpleAdapter(this, list, android.R.layout.simple_list_item_2, new String[] { speechTitle[0],speechTitle[1] },
	//		 new int[] { R.id.textView1, R.id.textView2 } );
	 setListAdapter(new ArrayAdapter<String>(this,  R.layout.speech_menu));
	// setListAdapter( adapter );
	 
	 //啟用按鍵過濾功能
	 getListView().setTextFilterEnabled(true);
	 }
}
