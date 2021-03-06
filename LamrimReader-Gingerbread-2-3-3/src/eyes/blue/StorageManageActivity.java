package eyes.blue;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

public class StorageManageActivity extends Activity {
	TextView extFreePercent, intFreePercent, extAppUsagePercent, intAppUsagePercent, intFree, extFree, extAppUseage, intAppUseage, labelChoicePath;
	Button btnMoveAllToExt, btnMoveAllToInt, btnDelExtFiles, btnDelIntFiles, btnOk;
	ImageButton btnChoicePath;
	RadioGroup radioMgnType =null;
	EditText fieldPathInput = null;
	boolean isUseThirdDir=false;
	
	private PowerManager.WakeLock wakeLock = null;
	SharedPreferences runtime = null;
	Toast toast = null;
	
	long intFreeB, extFreeB, intTotal, extTotal, intUsed, extUsed;
	String userSpecDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.storage_manage);
		
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
		new FileSysManager(this);
		toast = Toast.makeText(this, "", Toast.LENGTH_LONG);
		//if(!wakeLock.isHeld()){wakeLock.acquire();}
		
		extFreePercent = (TextView)findViewById(R.id.extFreePercent);
		intFreePercent = (TextView)findViewById(R.id.intFreePercent);
		extAppUsagePercent = (TextView)findViewById(R.id.extAppUsagePercent);
		intAppUsagePercent = (TextView)findViewById(R.id.intAppUsagePercent);
		intFree = (TextView)findViewById(R.id.intFree);
		extFree = (TextView)findViewById(R.id.extFree);
		extAppUseage = (TextView)findViewById(R.id.extAppUseage);
		intAppUseage = (TextView)findViewById(R.id.intAppUseage);
		labelChoicePath = (TextView)findViewById(R.id.labelChoicePath);
		btnMoveAllToExt = (Button) findViewById(R.id.btnMoveAllToExt);
		btnMoveAllToInt = (Button) findViewById(R.id.btnMoveAllToInt);
		btnDelExtFiles = (Button) findViewById(R.id.btnDelExtFiles);
		btnDelIntFiles = (Button) findViewById(R.id.btnDelIntFiles);
		btnChoicePath = (ImageButton) findViewById(R.id.btnChoicePath);
		btnOk = (Button) findViewById(R.id.btnOk);
		radioMgnType = (RadioGroup) findViewById(R.id.radioMgnType);
		fieldPathInput = (EditText) findViewById(R.id.fieldPathInput);

		// The ImageButton can't disable from xml.
		btnChoicePath.setClickable(false);
		btnChoicePath.setEnabled(false);
		
		isUseThirdDir=runtime.getBoolean(getString(R.string.isUseThirdDir),false);
		if(isUseThirdDir){
			radioMgnType.check(R.id.radioUserMgnStorage);
			fieldPathInput.setEnabled(true);
			btnChoicePath.setClickable(true);
			btnChoicePath.setEnabled(true);
			labelChoicePath.setEnabled(true);
		}
		
		String thirdDir=runtime.getString(getString(R.string.userSpecifySpeechDir),null);
		if(thirdDir==null || thirdDir.length()==0)thirdDir=FileSysManager.getSysDefMediaDir();
		fieldPathInput.setText(thirdDir,null);
		
		btnMoveAllToExt.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle("移動檔案");
				builder.setMessage("您確定要移動檔案嗎？");
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FileSysManager.moveAllFilesTo(FileSysManager.INTERNAL,FileSysManager.EXTERNAL,new CopyListener(){
							@Override
							public void copyFinish(){
								refreshUsage();
							}
						});
					}});
				builder.create().show();
			}});
		btnMoveAllToInt.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle("移動檔案");
				builder.setMessage("您確定要移動檔案嗎？");
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FileSysManager.moveAllFilesTo(FileSysManager.EXTERNAL,FileSysManager.INTERNAL,new CopyListener(){
							@Override
							public void copyFinish(){
								refreshUsage();
							}
							@Override
							public void copyFail(final File from, final File to){
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										toast.setText("Move fail while copy "+from.getAbsolutePath()+" to "+to.getAbsolutePath());
										toast.show();
								}});
								
							}
						});
					}});
				builder.create().show();
			}});
		btnDelExtFiles.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle(String.format(getString(R.string.dlgDelWarnTitle),"檔案"));
				builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg),"檔案"));
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
						pd.setTitle("刪除檔案");
						pd.setMessage("刪除中，請稍候...");
						pd.show();
						
						Thread t=new Thread(new Runnable(){
							@Override
							public void run() {
								FileSysManager.deleteAllSpeechFiles(FileSysManager.EXTERNAL);
								FileSysManager.deleteAllSubtitleFiles(FileSysManager.EXTERNAL);
								pd.dismiss();
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										refreshUsage();
										btnDelExtFiles.setEnabled(false);
								}});
							}});
						t.start();
					}});
				builder.create().show();
			}});
		btnDelIntFiles.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle(String.format(getString(R.string.dlgDelWarnTitle),"檔案"));
				builder.setMessage(String.format(getString(R.string.dlgDelWarnMsg),"檔案"));
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
						pd.setTitle("刪除檔案");
						pd.setMessage("刪除中，請稍候...");
						pd.show();
						
						Thread t=new Thread(new Runnable(){
							@Override
							public void run() {
								FileSysManager.deleteAllSpeechFiles(FileSysManager.INTERNAL);
								FileSysManager.deleteAllSubtitleFiles(FileSysManager.INTERNAL);
								pd.dismiss();
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										refreshUsage();
										btnDelIntFiles.setEnabled(false);
								}});
								
							}});
						t.start();
					}});
				builder.create().show();
			}});
		btnChoicePath.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				Intent intent = new Intent(getBaseContext(), FileDialogActivity.class);
                intent.putExtra(FileDialogActivity.START_PATH, "/sdcard");
                
                //can user select directories or not
                intent.putExtra(FileDialogActivity.CAN_SELECT_DIR, true);
                
                //alternatively you can set file filter
                //intent.putExtra(FileDialog.FORMAT_FILTER, new String[] { "png" });
                
                startActivityForResult(intent, 0);
			}});
		
		btnOk.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				SharedPreferences.Editor editor = runtime.edit();
				if(!isUseThirdDir){
					Log.d(getClass().getName(),"is user specify the third dir? "+isUseThirdDir);
					editor.putBoolean(getString(R.string.isUseThirdDir), false);
					editor.commit();
					finish();
					return;
				}
				
				if(fieldPathInput.getText().toString().length()==0){
					fieldPathInput.setText(FileSysManager.getSysDefMediaDir());
					AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
					builder.setTitle("目錄錯誤");
					builder.setMessage("路徑不可為空！請重新選擇。");
					builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							dialog.dismiss();
						}});
					builder.create().show();
					return;
				}
					// Check is the path is FILE
				File f=new File(fieldPathInput.getText().toString());
				if(f.isFile()){
					AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
					builder.setTitle("目錄錯誤");
					builder.setMessage("您所指定的儲存位置為檔案！請重新選擇。");
					builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							dialog.dismiss();
						}});
					builder.create().show();
					return;
				}
				
				// Write file test
				if(!f.exists()){
					if(!f.mkdirs()){
						AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
						builder.setTitle("權限錯誤");
						builder.setMessage("您所指定的儲存目錄無法建立！請重新選擇。");
						builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								dialog.dismiss();
							}});
						builder.create().show();
						return;
					}
				}
				else{
					f=new File(f.getAbsolutePath()+File.separator+"WRITE_TEXT.txt");
					Log.d(getClass().getName(),"Check is write in user specification location: "+f.getAbsolutePath());
					try {
						f.createNewFile();
						f.delete();
					} catch (IOException e) {
						AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
						builder.setTitle("權限錯誤");
						builder.setMessage("您所指定的儲存位置無法寫入！請重新選擇。");
						builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								dialog.dismiss();
							}});
						builder.create().show();
						return;
					}
				}
				editor.putBoolean(getString(R.string.isUseThirdDir), true);
				editor.putString(getString(R.string.userSpecifySpeechDir), fieldPathInput.getText().toString());
				editor.commit();

				finish();
			}});
		
		radioMgnType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			public void onCheckedChanged(RadioGroup group, int checkedId){

				switch(checkedId)
				{
				case R.id.radioAutoMgnStorage:
					isUseThirdDir=false;
					fieldPathInput.setEnabled(false);
					btnChoicePath.setEnabled(false);
					btnChoicePath.setClickable(false);
					labelChoicePath.setEnabled(false);
					break;
				case R.id.radioUserMgnStorage:
					isUseThirdDir=true;
					fieldPathInput.setEnabled(true);
					btnChoicePath.setEnabled(true);
					btnChoicePath.setClickable(true);
					labelChoicePath.setEnabled(true);
					break;
				}
			}});
	}
	
	@Override
	protected void onStart() {
		super.onStart();
		GaLogger.activityStart(this);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		GaLogger.activityStop(this);
	}
	
	private void refreshUsage(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				intFreeB=FileSysManager.getFreeMemory(FileSysManager.INTERNAL);
				extFreeB=FileSysManager.getFreeMemory(FileSysManager.EXTERNAL);
				intTotal=FileSysManager.getTotalMemory(FileSysManager.INTERNAL);
				extTotal=FileSysManager.getTotalMemory(FileSysManager.EXTERNAL);
				intUsed=FileSysManager.getAppUsed(FileSysManager.INTERNAL);
				extUsed=FileSysManager.getAppUsed(FileSysManager.EXTERNAL);
				userSpecDir=runtime.getString(getString(R.string.userSpecifySpeechDir), null);
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						extFreePercent.setText(Math.round(((double)extFreeB/extTotal)*100)+"%");
						intFreePercent.setText(Math.round(((double)intFreeB/intTotal)*100)+"%");
						extAppUsagePercent.setText(Math.round(((double)extUsed/extTotal)*100)+"%");
						intAppUsagePercent.setText(Math.round(((double)intUsed/intTotal)*100)+"%");
						extFree.setText(numToKMG(extFreeB)+"B");
						intFree.setText(numToKMG(intFreeB)+"B");
						extAppUseage.setText(numToKMG(extUsed)+"B");
						intAppUseage.setText(numToKMG(intUsed)+"B");
						
						if(intFreeB>extUsed&&extUsed>0)btnMoveAllToInt.setEnabled(true);
						else btnMoveAllToInt.setEnabled(false);
						if(extFreeB>intUsed&&intUsed>0)btnMoveAllToExt.setEnabled(true);
						else btnMoveAllToExt.setEnabled(false);
						if(intUsed>0)btnDelIntFiles.setEnabled(true);
						else btnDelIntFiles.setEnabled(false);
						if(extUsed>0)btnDelExtFiles.setEnabled(true);
						else btnDelExtFiles.setEnabled(false);
						
						if(userSpecDir!=null)fieldPathInput.setText(userSpecDir);
				}});
				
			}}).start();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		refreshUsage();
	}
	
	@Override
	public void onBackPressed() {
		super.onBackPressed();
		finish();
	}
	
	@Override
	public void finish() {
		super.finish();
		GaLogger.sendEvent("storage_status", "ext_storage", "free_percent", Math.round(((double)extFreeB/extTotal)*100));
		GaLogger.sendEvent("storage_status", "ext_storage", "usage_percent", Math.round(Math.round(((double)extUsed/extTotal)*100)));
		GaLogger.sendEvent("storage_status", "ext_storage", "free_byte", extFreeB);
		GaLogger.sendEvent("storage_status", "ext_storage", "used_byte", extUsed);
		
		GaLogger.sendEvent("storage_status", "int_storage", "free_percent", Math.round(((double)intFreeB/intTotal)*100));
		GaLogger.sendEvent("storage_status", "int_storage", "usage_percent", Math.round(Math.round(((double)intUsed/intTotal)*100)));
		GaLogger.sendEvent("storage_status", "int_storage", "free_byte", intFreeB);
		GaLogger.sendEvent("storage_status", "int_storage", "used_byte", intUsed);
		
		boolean isUserSpecifyDir=runtime.getBoolean(getString(R.string.isUseThirdDir),false);
		GaLogger.sendEvent("storage_status", "user_specify_dir", "boolean", ((isUserSpecifyDir)?1:0));
		if(isUserSpecifyDir)
			GaLogger.sendEvent("storage_status", "user_specify_dir", runtime.getString(getString(R.string.userSpecifySpeechDir), null), null);
			
		else GaLogger.sendEvent("storage_status", "user_specify_dir", FileSysManager.getSysDefMediaDir(), null);
	}
	
	private String numToKMG(long num){
		Log.d(getClass().getName(),"Cac: "+num);
		String[] unit={"","K","M","G","T"};
		String s=""+num;
		int len=s.length();
		
		int sign=(int) (len/3);
		if(sign*3==len)sign--;
		
		int index=sign*3;
		String result=s.substring(0, s.length()-index)+'.'+s.charAt(index)+unit[sign];
		Log.d(getClass().getName(),"Num= "+s+", Length: "+s.length()+", result="+result);
		return result;
	}
	
	
	public synchronized void onActivityResult(final int requestCode,
            int resultCode, final Intent data) {

            if (resultCode == Activity.RESULT_OK) {

                    if (requestCode == 0) {
                            System.out.println("Saving...");
                    } else if (requestCode == 1) {
                            System.out.println("Loading...");
                    }
                    
                    String filePath = data.getStringExtra(FileDialogActivity.RESULT_PATH);
                    fieldPathInput.setText(filePath);
            } else if (resultCode == Activity.RESULT_CANCELED) {
                    System.out.println("file not selected");
            }

    }
	
	private AlertDialog.Builder getConfirmDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
	        	if(wakeLock.isHeld())wakeLock.release();
	            dialog.cancel();
	        }
	    });
		return builder;
	}
	
	

}
