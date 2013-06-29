package eyes.blue;

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
/*	 try {
		LogRepoter.setRecever("http://10.0.200.156:8080/cylog/api/interface","Eyes Blue","Log");
	} catch (MalformedURLException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	 LogRepoter.reportMachineType();
*/	 
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
	 String[] speechDesc=getResources().getStringArray(R.array.mediaDesc);
//	 String[] speechDesc=getResources().getStringArray(R.array.speechDesc);

	 for(int i=0;i<speechDesc.length;i++){
		 HashMap<String,String> item = new HashMap<String,String>();
		 item.put("title", speechTitle[i]);
		 item.put("desc", speechDesc[i]);
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
		
		list=null;
		playWindow.putExtra(this.getResources().getString(R.string.searchingType), this.getResources().getInteger(R.integer.PLAY_FROM_MEDIA));
		playWindow.putExtra("index", position);
		setResult(RESULT_OK,new Intent().putExtras(playWindow));
		finish();
	}
}
