package eyes.blue;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.londatiga.android.ActionItem;
import net.londatiga.android.QuickAction;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class SpeechMenuActivity extends Activity {
	ImageButton btnDownloadAll, btnMaintain,  btnManageStorage;
	boolean speechFlags[], subtitleFlags[]=null;
	String[] descs, subjects;
	ArrayList<HashMap<String,Boolean>> fakeList = new ArrayList<HashMap<String,Boolean>>();
	SimpleAdapter adapter=null;
	ListView speechList=null;
	Intent playWindow=null;
	private PowerManager.WakeLock wakeLock = null;
	Toast toast = null;
	SharedPreferences runtime = null;
	// The handle for close the dialog.
	AlertDialog itemManageDialog = null;
	int manageItemIndex=-1;
	FileDownloader downloader = null;
	boolean fireLockKey = false;
	final int PLAY=0,UPDATE=1,	DELETE=2, CANCEL=3;
//	boolean fireLock=false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
	super.onCreate(savedInstanceState);
	setContentView(R.layout.speech_menu);

	
	speechList=(ListView) findViewById(R.id.list);
	btnDownloadAll=(ImageButton) findViewById(R.id.btnDownloadAll);
	btnMaintain=(ImageButton) findViewById(R.id.btnMaintain);
	btnManageStorage=(ImageButton) findViewById(R.id.btnManageStorage);
	playWindow=new Intent(SpeechMenuActivity.this,LamrimReaderActivity.class);
	 
	PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
	wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
	//wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
	runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
	fireLocker.start();
	//if(!wakeLock.isHeld()){wakeLock.acquire();}
	//if(wakeLock.isHeld())wakeLock.release();
	new FileSysManager(this);
	downloader=new FileDownloader(SpeechMenuActivity.this);
	
	toast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
	
	final QuickAction mQuickAction 	= new QuickAction(this);
	mQuickAction.addActionItem(new ActionItem(PLAY, getString(R.string.dlgManageSrcPlay), getResources().getDrawable(R.drawable.play)));
	mQuickAction.addActionItem(new ActionItem(UPDATE, getString(R.string.dlgManageSrcUpdate), getResources().getDrawable(R.drawable.update)));
	mQuickAction.addActionItem(new ActionItem(DELETE, getString(R.string.dlgManageSrcDel), getResources().getDrawable(R.drawable.delete)));
	mQuickAction.addActionItem(new ActionItem(CANCEL, getString(R.string.dlgCancel), getResources().getDrawable(R.drawable.return_sign)));
	
	mQuickAction.setOnActionItemClickListener(new QuickAction.OnActionItemClickListener() {
		@Override
		public void onItemClick(QuickAction quickAction, int pos, int actionId) {
			AlertDialog.Builder builder;
			switch(actionId){
			case PLAY:
				resultAndPlay(manageItemIndex);
				break;
			case UPDATE:
				DialogInterface.OnClickListener updateListener=new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(SpeechMenuActivity.this);
						File f=FileSysManager.getLocalMediaFile(manageItemIndex);
			        	if(f!=null && !FileSysManager.isFromUserSpecifyDir(f))f.delete();
			        	f=FileSysManager.getLocalSubtitleFile(manageItemIndex);
			        	if(f!=null)f.delete();
			        	resultAndPlay(manageItemIndex);
					}};
	        	
				BaseDialogs.showDelWarnDialog(SpeechMenuActivity.this, "檔案", null, updateListener, null, null);
				break;
			case DELETE:
				DialogInterface.OnClickListener deleteListener=new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(SpeechMenuActivity.this);
						File f=FileSysManager.getLocalMediaFile(manageItemIndex);
						if(f!=null && !FileSysManager.isFromUserSpecifyDir(f))f.delete();
			        	f=FileSysManager.getLocalSubtitleFile(manageItemIndex);
			        	if(f!=null)f.delete();
			        	updateUi(manageItemIndex);
					}};

				BaseDialogs.showDelWarnDialog(SpeechMenuActivity.this, "檔案", null, deleteListener, null, null);
	        	break;
			case CANCEL:
				// Do nothing.
				break;
			};
		}
	});
	
	mQuickAction.setOnDismissListener(new QuickAction.OnDismissListener() {
		@Override
		public void onDismiss() {
			//Toast.makeText(getApplicationContext(), "Ups..dismissed", Toast.LENGTH_SHORT).show();
		}
	});
	
	String infos[]=getResources().getStringArray(R.array.desc);
	descs=new String[infos.length];
	subjects=new String[infos.length];
	for(int i=0;i<infos.length;i++){
//		Log.d(getClass().getName(),"Desc: "+infos[i]);
		String[] sep=infos[i].split("-");
		descs[i]=sep[0];
		if(sep.length>1)subjects[i]=sep[1];
	}

	speechFlags=new boolean[SpeechData.name.length];
	subtitleFlags=new boolean[SpeechData.name.length];
	
	
	// Initial fakeList
	HashMap<String,Boolean> fakeItem = new HashMap<String,Boolean>();
	fakeItem.put("title", false);
	fakeItem.put("desc", false);
	for(int i=0;i<SpeechData.name.length;i++)
		fakeList.add( fakeItem );


	adapter = new SpeechListAdapter(this, fakeList,
			 R.layout.speech_row, new String[] { "page", "desc" },
				new int[] { R.id.pageContentView, R.id.pageNumView });

	speechList.setOnItemClickListener(new AdapterView.OnItemClickListener(){
		@Override
		public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
			if(fireLock())return;
			resultAndPlay(position);
	}});

	speechList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View v,int position, long id) {
			// If there is no speech file, nor subtitle file, don't show the manage dialog.
			if(!speechFlags[position]&&!subtitleFlags[position])
				return false;
			manageItemIndex=position;
			mQuickAction.show(v);
			//itemManageDialog=getItemManageDialog(position);
			//itemManageDialog.show();
//			if(!wakeLock.isHeld()){wakeLock.acquire();}
			return true;
		}
		 
	});
	
	speechList.setAdapter( adapter );
	 //啟用按鍵過濾功能
	speechList.setTextFilterEnabled(true);

	
	
	btnDownloadAll.setOnClickListener(new View.OnClickListener (){
		@Override
		public void onClick(View arg0) {
			if(fireLock())return;
			downloadAllSrc();
		}});
	btnMaintain.setOnClickListener(new View.OnClickListener (){
		@Override
		public void onClick(View arg0) {
			if(fireLock())return;
			maintain();
		}});
	
	btnManageStorage.setOnClickListener(new View.OnClickListener (){
		@Override
		public void onClick(View v) {
			if(fireLock())return;
			final Intent storageManage = new Intent(SpeechMenuActivity.this, StorageManageActivity.class);
			startActivity(storageManage);
		}});

	 }
	
// End of onCreate
	
	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences.Editor editor = runtime.edit();

		editor.putInt("speechMenuPage",speechList.getFirstVisiblePosition());
		View v=speechList.getChildAt(0);  
        editor.putInt("speechMenuPageShift",(v==null)?0:v.getTop());
        editor.putInt("lastViewItem",speechList.getLastVisiblePosition());
        editor.commit();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		final int speechMenuPage=runtime.getInt("speechMenuPage", 0);
//		final int speechMenuPageShift=runtime.getInt("speechMenuPageShift", 0);
		int lastPage=runtime.getInt("lastViewItem", -1);
		
		if(lastPage==-1){
			lastPage=speechMenuPage+10;
			if(lastPage>=SpeechData.name.length)
				lastPage=SpeechData.name.length-1;
		}
		refreshFlags(speechMenuPage,++lastPage,true);
		refreshFlags(0,speechFlags.length,false);
	}
		
	private void resultAndPlay(int position){
		Log.d(getString(R.string.app_name),"Speech menu "+position+"th item clicked.");
		
		//fakeList=null;
		playWindow.putExtra("index", position);
		setResult(RESULT_OK,new Intent().putExtras(playWindow));
		if(wakeLock.isHeld())wakeLock.release();
		finish();
	}
	
	private void updateUi(final int i){
		File speech=FileSysManager.getLocalMediaFile(i);
		File subtitle =FileSysManager.getLocalSubtitleFile(i);
		
		speechFlags[i]=(speech!=null && speech.exists());
		subtitleFlags[i]=(subtitle!=null && subtitle.exists());
		refreshListView();
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				speechList.setSelection(i);
		}});
	}
	
	private void refreshFlags(final int start,final int end,final boolean isRefreshView){
		Log.d(getClass().getName(), "Refresh flags: start="+start+", end="+end);
		Thread t=new Thread(new Runnable(){
			@Override
			public void run() {
				for(int i=start;i<end;i++){
					File speech=FileSysManager.getLocalMediaFile(i);
					File subtitle=FileSysManager.getLocalSubtitleFile(i);
					boolean me=(speech!=null && speech.exists());
					boolean se=(subtitle!=null && subtitle.exists());
//					Log.d(getClass().getName(), "Set flags of "+SpeechData.getNameId(i)+": is speech exist: "+me+", is subtitle exist: "+se);
					synchronized(speechFlags){
						speechFlags[i]=me;
					}
					
					synchronized(subtitleFlags){
						subtitleFlags[i]=se;
					}
				}
				if(isRefreshView)refreshListView();
			}
		});
		 t.start();
	}
	
	private void refreshListView(){
		final int speechListPage=runtime.getInt("speechMenuPage", 0);
		final int speechListPageShift=runtime.getInt("speechMenuPageShift", 0);
		adapter = new SpeechListAdapter(SpeechMenuActivity.this, fakeList,
				 R.layout.speech_row, new String[] { "page", "desc" },
					new int[] { R.id.pageContentView, R.id.pageNumView });
		
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				speechList.setAdapter( adapter );
				adapter.notifyDataSetChanged();
				speechList.setSelectionFromTop(speechListPage, speechListPageShift);
			}});
	}
	
	
	private void downloadAllSrc(){
		downloader.setDownloadListener(new DownloadListener(){
			@Override
			public void allPrepareFinish(int... i){
				if(wakeLock.isHeld())wakeLock.release();
			}
			@Override
			public void prepareFinish(int i, int type){
				updateUi(i);
			}
			@Override
			public void prepareFail(final int i, int type){
				runOnUiThread(new Runnable() {
					public void run() {
						toast.setText("下載失敗！請確認檔案空間足夠，或您的網路連線是否正常。");
						toast.show();
				}});
				updateUi(i);
			}
			
			@Override
			public void userCancel(int i, int type){
				Log.d(getClass().getName(),"User cancel the download!");
				if(wakeLock.isHeld())wakeLock.release();
				return;
			}
		});	
		if(!wakeLock.isHeld()){wakeLock.acquire();}
		final int[] ia=new int[SpeechData.name.length];
		for(int i=0;i<ia.length;i++)
			ia[i]=i;
		
		downloader.start(ia);
	}
	
	private void maintain(){
		final ProgressDialog pd= new ProgressDialog(SpeechMenuActivity.this);
		pd.setCancelable(false);
		pd.setTitle(getString(R.string.dlgTitleMaintaining));
		pd.setMessage(getString(R.string.dlgMsgMaintaining));
		pd.show();
		if(!wakeLock.isHeld()){wakeLock.acquire();}
		
		AsyncTask<Void, Void, Void> runner=new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				FileSysManager.maintainStorages();
				pd.dismiss();
				if(wakeLock.isHeld())wakeLock.release();
				return null;
			}
		};
		
		runner.execute();
	}
	
	private boolean fireLock(){
		if(fireLockKey){
			Log.d(getClass().getName(),"Fire locked");
			return true;
		}
		fireLockKey=true;
		synchronized(fireLocker){
			fireLocker.notify();
		}
		Log.d(getClass().getName(),"Fire released, lock it!");
		return false;
	}
	
	Thread fireLocker=new Thread(){
		@Override
		public void run(){
			while(true){
					try {
						synchronized(this){
							wait();
						}
					} catch (InterruptedException e1) {	e1.printStackTrace();}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {e.printStackTrace();}
					Log.d(getClass().getName(),"Fire locker release the lock.");
					fireLockKey=false;

			}
		}
	};
/*	private AlertDialog.Builder getConfirmDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if(wakeLock.isHeld())wakeLock.release();
	            dialog.cancel();
	        }
	    });
		return builder;
	}
*/	// These buttons of the dialog are short term process, no wake luck need, if destroy by blank screen, no effect for our logic. 
	
	
	class SpeechListAdapter extends SimpleAdapter {
		float textSize = 0;

		public SpeechListAdapter(Context context,
				List<? extends Map<String, ?>> data, int resource,
				String[] from, int[] to) {
			super(context, data, resource, from, to);
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			if (row == null) {
//				Log.d(getClass().getName(), "row=null, construct it.");
				LayoutInflater inflater = getLayoutInflater();
				row = inflater.inflate(R.layout.speech_row, parent, false);
			}

//			Log.d(getClass().getName(), "Set "+SpeechData.getNameId(position)+": is speech exist: "+speechFlags[position]+", is subtitle exist: "+subtitleFlags[position]);
			TextView title = (TextView) row.findViewById(R.id.title);
			TextView subject = (TextView) row.findViewById(R.id.subject);
			ImageView mediaSign = (ImageView) row.findViewById(R.id.mediaSign);
			ImageView subtitleSign = (ImageView) row.findViewById(R.id.subtitleSign);
			TextView speechDesc = (TextView) row.findViewById(R.id.speechDesc);
			
			if(speechFlags[position])
				mediaSign.setImageResource(R.drawable.speech);
//				mediaSign.setBackgroundColor(0xFFFFFFDF);
//			mediaSign.setBackgroundColor(Color.BLACK);				
			else mediaSign.setImageResource(R.drawable.speech_d);
			if(subtitleFlags[position])
				//subtitleSign.setBackgroundColor(0xFFFFFFDF);
				subtitleSign.setImageResource(R.drawable.subtitle);
			//subtitleSign.setBackgroundColor(Color.BLACK);
			else subtitleSign.setImageResource(R.drawable.subtitle_d); 
			if(speechFlags[position]&&subtitleFlags[position]){
				title.setTextColor(Color.BLACK);
				subject.setTextColor(Color.BLACK);
//				speechDesc.setTextColor(Color.BLACK);
				row.setBackgroundColor(0xFFFFFFDF);
			}
			else {
				title.setTextColor(Color.WHITE);
				subject.setTextColor(Color.WHITE);
//				speechDesc.setTextColor(Color.GRAY);
				row.setBackgroundColor(Color.BLACK);
			}
			
			title.setText(SpeechData.getNameId(position));
			subject.setText(subjects[position]);
			speechDesc.setText(descs[position]);
			return row;
		}
	}
}
