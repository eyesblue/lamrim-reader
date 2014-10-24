package eyes.blue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StatFs;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

public class StorageManageActivity extends Activity {
	FileSysManager fsm=null;
	TextView extSpeechPathInfo, extSubtitlePathInfo, intSpeechPathInfo, intSubtitlePathInfo, extFreePercent, intFreePercent, extAppUsagePercent, intAppUsagePercent, intFree, extFree, extAppUseage, intAppUseage, labelChoicePath;
	Button btnMoveAllToExt, btnMoveAllToInt, btnMoveToUserSpy, btnDelExtFiles, btnDelIntFiles, btnOk;
	ImageButton btnChoicePath;
	RadioGroup radioMgnType =null;
	EditText filePathInput;
	boolean isUseThirdDir=false;
	
//	private PowerManager.WakeLock wakeLock = null;
	SharedPreferences runtime = null;
	
	long intFreeB, extFreeB, intTotal, extTotal, intUsed, extUsed;
	String userSpecDir;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.storage_manage);
		Log.d(getClass().getName(),"Into onCreate");
		this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
//		PowerManager powerManager=(PowerManager) getSystemService(Context.POWER_SERVICE);
//		wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
		runtime = getSharedPreferences(getString(R.string.runtimeStateFile), 0);
		fsm=new FileSysManager(this);
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
		btnMoveToUserSpy = (Button) findViewById(R.id.moveToUserSpyDirBtn);
		btnOk = (Button) findViewById(R.id.btnOk);
		radioMgnType = (RadioGroup) findViewById(R.id.radioMgnType);
		filePathInput = (EditText) findViewById(R.id.fieldPathInput);
		extSpeechPathInfo = (TextView) findViewById(R.id.extSpeechPathInfo);
		extSubtitlePathInfo = (TextView) findViewById(R.id.extSubtitlePathInfo);
		intSpeechPathInfo = (TextView) findViewById(R.id.intSpeechPathInfo);
		intSubtitlePathInfo = (TextView) findViewById(R.id.intSubtitlePathInfo);

		// The ImageButton can't disable from xml.
		btnChoicePath.setClickable(false);
		btnChoicePath.setEnabled(false);
		btnMoveToUserSpy.setEnabled(false);
		
		isUseThirdDir=runtime.getBoolean(getString(R.string.isUseThirdDir),false);
		if(isUseThirdDir){
			radioMgnType.check(R.id.radioUserMgnStorage);
			filePathInput.setEnabled(true);
			btnChoicePath.setClickable(true);
			btnChoicePath.setEnabled(true);
			labelChoicePath.setEnabled(true);
			btnMoveToUserSpy.setEnabled(true);
		}
		
		String thirdDir=runtime.getString(getString(R.string.userSpecifySpeechDir),null);
		if(thirdDir==null || thirdDir.length()==0)thirdDir=fsm.getSysDefMediaDir();
		filePathInput.setText(thirdDir,null);
		
		btnMoveAllToExt.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder=getConfirmDialog();
				builder.setTitle("移動檔案");
				builder.setMessage("您確定要移動檔案嗎？");
				builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
					@Override
					public void onClick(DialogInterface dialog, int which) {
						fsm.moveAllFilesTo(FileSysManager.INTERNAL,FileSysManager.EXTERNAL,new CopyListener(){
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
						fsm.moveAllFilesTo(FileSysManager.EXTERNAL,FileSysManager.INTERNAL,new CopyListener(){
							@Override
							public void copyFinish(){
								refreshUsage();
							}
							@Override
							public void copyFail(final File from, final File to){
								runOnUiThread(new Runnable(){
									@Override
									public void run() {
										Util.showErrorPopupWindow(StorageManageActivity.this, findViewById(R.id.smRootView), "搬移檔案時發生錯誤: 來源 "+from.getAbsolutePath()+", 目的地:  "+to.getAbsolutePath());
								}});
								
							}
						});
					}});
				builder.create().show();
			}});
		
		btnMoveToUserSpy.setOnClickListener(new View.OnClickListener (){
			@Override
			public void onClick(View arg0) {
				btnMoveToUserSpy.setEnabled(false);

				GaLogger.sendEvent("statistics", "MOVE_FILE_TO_SPECIFY_FOLDER", "CLICK", 1);
				
				Log.d(getClass().getName(),"thread started");
				String path=filePathInput.getText().toString();
				if(path==null||path.length() == 0){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定路徑錯誤，無法移動檔案。");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				File filePath=new File(path);
				if(filePath.isFile()){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定目錄所指定的位置為已存在的檔案，請重新選擇！");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				Log.d(getClass().getName(),"Create folder: "+path);
				filePath.mkdir();
				if(!filePath.exists() || !filePath.isDirectory() || !filePath.canWrite()){
					Util.showErrorPopupWindow(StorageManageActivity.this, "使用者指定目錄錯誤或無寫入權限，無法移動檔案。");
					btnMoveToUserSpy.setEnabled(true);
					return;
				}
				
				// Check the path is not external/internal default storage path.
				ArrayList<String> srcList=new ArrayList<String>();
				srcList.add(fsm.getSrcRootPath(FileSysManager.INTERNAL)+File.separator+getString(R.string.audioDirName));
				String ext=fsm.getSrcRootPath(FileSysManager.EXTERNAL);
				if(ext!=null)srcList.add(ext+File.separator+getString(R.string.audioDirName));
		    	
				Log.d(getClass().getName(),	"There are "+srcList.size()+" src folder for move file.");
				Intent intent = new Intent(StorageManageActivity.this,	MoveFileService.class);
				intent.putStringArrayListExtra("srcDirs", srcList);
				intent.putExtra("destDir",path);
				Log.d(getClass().getName(),	"Start move file service.");
				
				// While user press the move button that mean the path is specified.
				SharedPreferences.Editor editor = runtime.edit();
				editor.putBoolean(getString(R.string.isUseThirdDir), true);
				editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
				editor.commit();
				
				Util.showInfoPopupWindow(StorageManageActivity.this, "背景移動中，請檢視通知列以瞭解進度，移動過程中請勿執行其他操作。");
				startService(intent);
				GaLogger.sendEvent("ui_action", "botton_pressed", "reloadLastState_MoveFileToUserSpecify", null);
				
				refreshUsage();
				btnMoveToUserSpy.setEnabled(true);
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
								fsm.deleteAllSpeechFiles(FileSysManager.EXTERNAL);
								fsm.deleteAllSubtitleFiles(FileSysManager.EXTERNAL);
								if(pd.isShowing())pd.dismiss();
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
								fsm.deleteAllSpeechFiles(FileSysManager.INTERNAL);
								fsm.deleteAllSubtitleFiles(FileSysManager.INTERNAL);
								if(pd.isShowing())pd.dismiss();
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
				intent.putExtra(FileDialogActivity.TITLE, "請選擇存放目錄");
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
				
				if(filePathInput.getText().toString().length()==0){
					filePathInput.setText(fsm.getSysDefMediaDir());
					AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
					builder.setTitle("目錄錯誤");
					builder.setMessage("路徑不可為空！請重新選擇。");
					builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							try{
								dialog.dismiss();
							}catch(Exception e){e.printStackTrace();}
						}});
					builder.create().show();
					return;
				}
					// Check is the path is FILE
				File f=new File(filePathInput.getText().toString());
				if(f.isFile()){
					AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
					builder.setTitle("目錄錯誤");
					builder.setMessage("您所指定的儲存位置為檔案！請重新選擇。");
					builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
						@Override
						public void onClick(DialogInterface dialog,	int which) {
							try{
								dialog.dismiss();
							}catch(Exception e){e.printStackTrace();}
						}});
					builder.create().show();
					return;
				}
				
				// Write file test
				if(!f.exists()){
					f.mkdir();
					if(!f.exists() || !f.canWrite()){
						AlertDialog.Builder builder = new AlertDialog.Builder(StorageManageActivity.this);
						builder.setTitle("權限錯誤");
						builder.setMessage("您所指定的儲存目錄無法建立或無寫入權限！請重新選擇。");
						builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener (){
							@Override
							public void onClick(DialogInterface dialog,	int which) {
								try{
									dialog.dismiss();
								}catch(Exception e){e.printStackTrace();}
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
								try{
									dialog.dismiss();
								}catch(Exception e){e.printStackTrace();}
							}});
						builder.create().show();
						return;
					}
				}
				
				editor.putBoolean(getString(R.string.isUseThirdDir), true);
				editor.putString(getString(R.string.userSpecifySpeechDir), filePathInput.getText().toString());
				editor.commit();

				finish();
			}});
		
		radioMgnType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			public void onCheckedChanged(RadioGroup group, int checkedId){

				switch(checkedId)
				{
				case R.id.radioAutoMgnStorage:
					isUseThirdDir=false;
					filePathInput.setEnabled(false);
					btnChoicePath.setEnabled(false);
					btnChoicePath.setClickable(false);
					labelChoicePath.setEnabled(false);
					btnMoveToUserSpy.setEnabled(false);
					break;
				case R.id.radioUserMgnStorage:
					isUseThirdDir=true;
					filePathInput.setEnabled(true);
					btnChoicePath.setEnabled(true);
					btnChoicePath.setClickable(true);
					labelChoicePath.setEnabled(true);
					btnMoveToUserSpy.setEnabled(true);
					break;
				}
			}});
		
		String extSpeechDir=fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
		String extSubtitleDir=fsm.getLocateDir(FileSysManager.EXTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));
		String intSpeechDir=fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.MEDIA_TYPE));
		String intSubtitleDir=fsm.getLocateDir(FileSysManager.INTERNAL, getResources().getInteger(R.integer.SUBTITLE_TYPE));
		
		extSpeechPathInfo.setText(((extSpeechDir != null)?extSpeechDir:getString(R.string.noExtSpace)));
		extSubtitlePathInfo.setText(((extSubtitleDir != null)?extSubtitleDir:getString(R.string.noExtSpace)));
		intSpeechPathInfo.setText(intSpeechDir);
		intSubtitlePathInfo.setText(intSubtitleDir);
		
		Log.d(getClass().getName(),"Leave onCreate");
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
				intFreeB=fsm.getFreeMemory(FileSysManager.INTERNAL);
				extFreeB=fsm.getFreeMemory(FileSysManager.EXTERNAL);
				intTotal=fsm.getTotalMemory(FileSysManager.INTERNAL);
				extTotal=fsm.getTotalMemory(FileSysManager.EXTERNAL);
				intUsed=fsm.getAppUsed(FileSysManager.INTERNAL);
				extUsed=fsm.getAppUsed(FileSysManager.EXTERNAL);
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
						
						if(userSpecDir!=null)filePathInput.setText(userSpecDir);
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
			
		else GaLogger.sendEvent("storage_status", "user_specify_dir", fsm.getSysDefMediaDir(), null);
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
	
	
	public synchronized void onActivityResult(final int requestCode, int resultCode, final Intent data) {
		if (resultCode != Activity.RESULT_OK || requestCode != 0 || data == null) return;

		final String filePath = data.getStringExtra(FileDialogActivity.RESULT_PATH);

		// Avoid EditText bug,  the EditText will not change to the new value without the thread.
		new Thread(new Runnable(){
			@Override
			public void run() {
				try {
					Thread.sleep(500);
					Log.d(getClass().getName(),"Set path the EditText: "+filePath);
					runOnUiThread(new Runnable(){
						@Override
						public void run() {
							filePathInput.setText(filePath);
						}});
					
				} catch (InterruptedException e) {e.printStackTrace();}

/*				
				String oldPath=runtime.getString(getString(R.string.userSpecifySpeechDir),FileSysManager.getSysDefMediaDir());
				// Path not change.
				if(filePath.equals(oldPath))return;
				File[] intFiles=FileSysManager.getMediaFileList(FileSysManager.INTERNAL);
				File[] extFiles=FileSysManager.getMediaFileList(FileSysManager.EXTERNAL);
				if((intFiles.length+extFiles.length) == 0) return;
				
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						showAskMoveToSpecifyDialog(filePath);
					}});
*/			}}).start();
		
    }
	
	private void showAskMoveToSpecifyDialog(final String path) {
		final AlertDialog.Builder builder=getConfirmDialog();
		builder.setTitle("移動檔案");
		builder.setMessage("您要將所有的音檔移動到您所指定的位置嗎？");
		builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				final ProgressDialog pd= new ProgressDialog(StorageManageActivity.this);
				pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        	pd.setCancelable(false);
	        	pd.setTitle("檔案搬移");
	        	pd.setMessage("搬移中，請稍候...");
				Thread t=new Thread(new Runnable(){
					@Override
					public void run() {
						File destFile=new File(path);
						if(!fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd) || !fsm.moveAllMediaFileToUserSpecifyDir(destFile, pd))
							Util.showErrorPopupWindow(StorageManageActivity.this, findViewById(R.id.smRootView), "檔案搬移失敗，請確認目的地空間是否足夠。");
						if(pd.isShowing())pd.dismiss();
						refreshUsage();
					}});
				t.start();
				
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						pd.show();
					}});
				
			}});
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(final DialogInterface dialog, int which) {
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						dialog.cancel();
					}});
			}});
		
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				builder.create().show();
			}});
	}

	private AlertDialog.Builder getConfirmDialog(){
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
	        public void onClick(DialogInterface dialog, int id) {
//	        	if(wakeLock.isHeld())wakeLock.release();
	            dialog.cancel();
	        }
	    });
		return builder;
	}
	
	

}
