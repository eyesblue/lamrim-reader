package eyes.blue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileSysManager {
//        public static String[] fileName=null;
//        public static int[] fileSize=null;
	static int targetFileIndex=-1;
	static String logTag=null;
	static int NO_CACHE=0;
	public final static int INTERNAL=0;
	public final static int EXTERNAL=1;
	public final static String[] locateDesc={"internal","external"};
	final static int MEDIA_FILE=0;
	final static int SUBTITLE_FILE=1;
	final static int THEORY_FILE=2;
        
	static SharedPreferences runtime = null;
	static StatFs[] statFs=null;
	static Activity context=null;
//        static String[] remoteSite=null;
	static ArrayList<RemoteSource> remoteResources=new ArrayList<RemoteSource>();
	static DiskSpaceFullListener diskFullListener=null;

	DownloadListener downloadListener=null;
	static int downloadFromSite=-1;
	static GoogleRemoteSource grs=null;
	static String srcRoot[] = new String[2];

//        static int bufLen=16384;
        
	public FileSysManager(Activity context){
		runtime = context.getSharedPreferences(context.getString(R.string.runtimeStateFile), 0);
		
		statFs=new StatFs[2];
		statFs[INTERNAL]=new StatFs(Environment.getRootDirectory().getAbsolutePath());
		statFs[EXTERNAL]=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		srcRoot[INTERNAL]=context.getFileStreamPath(context.getString(R.string.app_name)).getAbsolutePath();
		srcRoot[EXTERNAL]=context.getExternalFilesDir(File.separator+context.getString(R.string.app_name)).getAbsolutePath();
              
		FileSysManager.logTag=getClass().getName();
		FileSysManager.context=context;
                grs=new GoogleRemoteSource(context);
        }
        
        public void setDownloadListener(DownloadListener listener){
        	this.downloadListener=listener;
        }
        
        
        /*
         * the file structure is follow
         * [PackageDir]\[AppName](LamrimReader)\{Audio,Book,Subtitle}
         * */
        public static void checkFileStructure(){
        	boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        	File appRoot=null;
        	
        	// Make file structure of external storage.
        	if(extWritable){
        		appRoot=new File(srcRoot[EXTERNAL]);
        		if(appRoot.isFile()){
            		appRoot.delete();
            		appRoot.mkdirs();
            	}
        		
        		String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName),context.getString(R.string.theoryDirName)};
            	for(String s:dirs){
            		File subDir=new File(srcRoot[EXTERNAL]+File.separator+s);
            		if(subDir.isFile())subDir.delete();
            		if(!subDir.exists())subDir.mkdirs();
            	}
        	}
        	
        	// Make file structure of internal storage
        	appRoot=new File(srcRoot[INTERNAL]);
        	
        	// The app root should not be a file, that will be cause app error
        	if(appRoot.isFile()){
        		appRoot.delete();
        		appRoot.mkdirs();
        	}
        	
        	String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName),context.getString(R.string.theoryDirName)};
        	for(String s:dirs){
        		File subDir=new File(srcRoot[INTERNAL]+File.separator+s);
        		if(subDir.isFile())subDir.delete();
        		if(!subDir.exists())subDir.mkdirs();
        	}
        }
        
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * If the file exist, return the exist file no matter internal or external,
         * if file not exist, return the allocate place of the file, allocate in external first, if not, return internal file.
         * */
        public static File getLocalMediaFile(int i){
        	// Check the directory by user specify.
        	boolean useThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        	String userSpecDir=runtime.getString(context.getString(R.string.userSpecifySpeechDir),null);
  
        	File specFile=null, extF=null, intF=null;
        	if(useThirdDir && userSpecDir!=null){
        		specFile=new File(userSpecDir+File.separator+SpeechData.name[i]);
        		// Test is exist and readable.
        		if(specFile.exists()){
        			Log.d(logTag,Thread.currentThread().getName()+": the media file exist in user specification location: "+specFile.getAbsolutePath());
        			return specFile;
        		}
        	}
        	
        	if(isExtMemWritable()){
        		extF=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        		Log.d(logTag,"Check exist: "+extF.getAbsolutePath());
        		if(extF.exists())return extF;
        	}
        	intF=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
    		Log.d(logTag,"Check exist: "+intF.getAbsolutePath());
    		if(intF.exists())return intF;

    		
    		// Test is user specify locate writable.
    		if(useThirdDir && (new File(userSpecDir)).exists()){
    			return specFile;
    		}
    		// Check is there enough space for save the file
    		int reserv=context.getResources().getIntArray(R.array.mediaFileSize)[i];
			reserv+=reserv*context.getResources().getInteger(R.integer.reservSpacePercent);
    		if(isExtMemWritable()){
    			Log.d(logTag,"File not exist return user external place");
    			if(getFreeMemory(EXTERNAL)>reserv)
    				return extF;
    		}
    		Log.d(logTag,"File not exist return user internal place");
    		if(getFreeMemory(INTERNAL)>reserv)
    			return intF;
    		
    		return null;
        }
      
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * If the file exist, return the exist file no matter internal or external,
         * if file not exist, return the allocate place of the file, allocate in external first, if not, return internal file.
         * */
        public static File getLocalSubtitleFile(int i){
        	File extF=null, intF=null;
        	if(isExtMemWritable()){
        		extF= new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		if(extF.exists())return extF;
        		intF=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		if(intF.exists())return intF;
        	}
        	
        	int reserv=context.getResources().getInteger(R.integer.subtitleReservSizeK);
        	if(getFreeMemory(EXTERNAL)>reserv)
        		return extF;
        	if(getFreeMemory(INTERNAL)>reserv)
        		return intF;
        	
        	return null;
        }
        
        public static void deleteSpeechFiles(int locate){
        	Log.d("FileSysManager","Delete all speech file in "+locateDesc[locate]);
        	String dir=context.getString(R.string.audioDirName);

        	File srcDir=new File(srcRoot[locate]+File.separator+dir);
        	for(File f:srcDir.listFiles())
        		f.delete();

        }
        
        public static void deleteSubtitleFiles(int locate){
        	Log.d("FileSysManager","Delete all subtitle file in "+locateDesc[locate]);
        	String dir=context.getString(R.string.subtitleDirName);
       		File srcDir=new File(srcRoot[locate]+File.separator+dir);
       		for(File f:srcDir.listFiles())
       			f.delete();
        }
        
        /*
         * Not test yet.
         * Move all files of INTERNAL or EXTERNAL to EXTERNAL or INTERNAL
         * */
        public static void moveAllFilesTo(int from,int to,final CopyListener listener){
        	final ProgressDialog pd= new ProgressDialog(context);
        	pd.setCancelable(false);
        	pd.setTitle("檔案搬移");
        	
        	final AsyncTask<Integer, Void, Void> executer=new  AsyncTask<Integer, Void, Void>(){
				@Override
				protected Void doInBackground(Integer... params) {
					int from=params[0];
					int to=params[1];
					final String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName)};
					
                	for(int j=0;j<dirs.length;j++){
                		String s=dirs[j];
                		File srcDir=new File(srcRoot[from]+File.separator+s);
                		String distDir=srcRoot[to]+File.separator+s;
                		final File[] files=srcDir.listFiles();
                		File srcFile = null;
                		final int progress=j;
                		
                		Log.d(getClass().getName(),"There are "+files.length+" files wait for move.");
                		context.runOnUiThread(new Runnable(){
                			@Override
                			public void run(){
                				pd.setMax(files.length);
                        		pd.setSecondaryProgress((int) (((double)progress/dirs.length)*files.length));
                			}
                		});
                		
                		for(int i=0;i<files.length;i++){
                			srcFile=files[i];
                			File distFile=new File(distDir+File.separator+srcFile.getName());
                			if(distFile.exists()){
                				if(srcFile.length()==distFile.length()){
                					srcFile.delete();
                					pd.setProgress(i+1);
                					if(this.isCancelled())return null;
                					continue;
                				}
                			}

                			/* Copy To */
                			File distTemp=new File(distFile.getAbsolutePath()+context.getString(R.string.downloadTmpPostfix));
                			FileInputStream fis = null;
                			FileOutputStream fos = null;
                			Log.d(getClass().getName(),"Copy "+srcFile.getAbsolutePath()+" to "+distFile.getAbsolutePath());
        					try {
        						fis = new FileInputStream(srcFile);
        						fos =new FileOutputStream(distTemp);
        					} catch (FileNotFoundException e) {
        						listener.copyFail(srcFile,distFile);
        						e.printStackTrace();
        						return null;
        					}
                			
                			byte[] buf=new byte[context.getResources().getInteger(R.integer.downloadBufferSize)];
                			int readLen=0;
                			long totalLen=0;
                			
                			try {
                				while((readLen=fis.read(buf))!=-1){
                					fos.write(buf, 0, readLen);
                					totalLen+=readLen;
                				
                					if(this.isCancelled()){
                						fis.close();
                						fos.close();
                						distTemp.delete();
                						return null;
                					}
                				}
                				
                				fis.close();
                    			fos.flush();
                    			fos.close();
                    			
                    			Log.d(getClass().getName(),"Total read: "+totalLen+", File length: "+distTemp.length());
                				if(distTemp.length()==totalLen){
                					distFile.delete();
                    				distTemp.renameTo(distFile);
                    				Log.d(getClass().getName(),"Rename: "+distTemp.getAbsolutePath()+" to "+distFile.length());
                    				
                    				
                    				boolean isSuccess=srcFile.delete();
                    				Log.d(getClass().getName(),"Delete: "+srcFile.getAbsolutePath()+isSuccess);
                    			}
                				else{
                					Log.d(getClass().getName(),"Copy fail: from: "+srcFile.getAbsolutePath()+", to: "+distFile.getAbsolutePath()+", temp file: "+distTemp.getAbsolutePath());
                					distTemp.delete();
                					listener.copyFail(srcFile,distFile);
                				}
                				
                				pd.setProgress(i+1);
                    			
                			} catch (IOException e) {
                				listener.copyFail(srcFile,distFile);
        						e.printStackTrace();
        						return null;
        					}
                		}
                	}
                	context.runOnUiThread(new Runnable(){
            			@Override
            			public void run(){
            				listener.copyFinish();
            				pd.dismiss();
            			}
            		});
					return null;
				}
				
				@Override
        		protected void onCancelled(){
					listener.userCancel();
        		}
			};
        	
			executer.execute(from,to);
			
			
    		
    		pd.setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
    		    @Override
    		    public void onClick(DialogInterface dialog, int which) {
    		    	executer.cancel(false);
    		    	dialog.dismiss();
    		    }
    		});
    		pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
    		pd.show();
        }
        
        public static void maintainStorages(){
        	
        	File userSpecDir=null;
        	boolean isUseThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        	if(isUseThirdDir){
        		userSpecDir=new File(runtime.getString(context.getString(R.string.userSpecifySpeechDir), null));
        		if(!userSpecDir.exists())userSpecDir=null;
        	}
        	// we must make sure the userSpecDir exist, because of remove SD card, the dir will not exist.

        	
        	// Check duplication files.
        	for(int i=0;i<SpeechData.name.length;i++){
        		File meu=null;
        		
        		if(userSpecDir!=null)meu=new File(userSpecDir.getAbsolutePath()+File.separator+SpeechData.name[i]);
        		
        		File mei=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        		File mex=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName)+File.separator+SpeechData.name[i]);
        		File sbi=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		File sbx=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName)+File.separator+SpeechData.getSubtitleName(i)+"."+context.getString(R.string.defSubtitleType));
        		
        		if(meu!=null && meu.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Media file exist in USER SPECIFY DIR, delete external and internal.");
        			mex.delete();
        			mei.delete();
        		}
        		else if(mex.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Media file exist in EXTERNAL DIR, delete internal.");
        			mei.delete();
        		}
        		
        		if(sbx.exists()){
        			Log.d(logTag,SpeechData.getNameId(i)+" Subtitle file exist in EXTERNAL DIR, delete internal.");
        			sbi.delete();
        		}
        	}
        	
        	File miDir=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.audioDirName));
        	File meDir=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.audioDirName));
        	File siDir=new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.subtitleDirName));
        	File seDir=new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.subtitleDirName));
        	FilenameFilter filter= new FilenameFilter (){
				@Override
				public boolean accept(File dir, String filename) {
					if(filename.endsWith(context.getResources().getString(R.string.downloadTmpPostfix)))
						return true;
					return false;
				}};
				
			for(File f:miDir.listFiles(filter))f.delete();
			for(File f:meDir.listFiles(filter))f.delete();
			for(File f:siDir.listFiles(filter))f.delete();
			for(File f:seDir.listFiles(filter))f.delete();
			if(userSpecDir!=null)for(File f:userSpecDir.listFiles(filter))f.delete();
        }
        
        // NOT test yet
        public static File getLocalTheoryFile(int i){
        	if(isExtMemWritable())return new File(srcRoot[EXTERNAL]+File.separator+context.getString(R.string.theoryDirName)+File.separator+SpeechData.getTheoryName(i)+"."+context.getString(R.string.defTheoryType));
        	else return new File(srcRoot[INTERNAL]+File.separator+context.getString(R.string.theoryDirName)+File.separator+SpeechData.getTheoryName(i)+"."+context.getString(R.string.defTheoryType));
        }

        public static boolean isFileValid(int i,int resType){
        	Log.d(logTag,Thread.currentThread().getName()+":Check the existed file");
        	File file=null;
        	

        	if(resType==context.getResources().getInteger(R.integer.MEDIA_TYPE)){
        		file=getLocalMediaFile(i);
        		if(file==null||!file.exists())return false;
        		
        		// First check is the file from user specified dir, then no size or crc check,
        		// return true if file exist.
        		//boolean isUseThirdDir=runtime.getBoolean(context.getString(R.string.isUseThirdDir), false);
        		String userSpecDir=runtime.getString(context.getString(R.string.userSpecifySpeechDir), null);
        		if(userSpecDir != null)
        		if(file.getAbsolutePath().startsWith(userSpecDir)){
        			Log.d(logTag,"The file is from user specified folder, no size or crc check.");
        			if(userSpecDir!=null && file.exists()){
        				return true;
        			}
        			else {
        				Log.d(logTag,"The user specified file is not exist.");
        				return false;
        			}
        		}
        		
        		
        		
        		int size=SpeechData.length[i];
        		if(file.length()!=size){
            		Log.d(logTag,"The size of file is not correct, should be "+size+", but "+file.length());
            		return false;
            	}
            	try {
            		
    				return Util.isFileCorrect(file, SpeechData.crc[i]);
    			} catch (Exception e) {
    				e.printStackTrace();
    				return false;
    			}
        	}
        	else if(resType==context.getResources().getInteger(R.integer.SUBTITLE_TYPE)){
        		file=getLocalSubtitleFile(i);
        		if(file==null||file.exists())return true;
            	Log.d(logTag,"The subtitle file "+file.getAbsolutePath()+" is not exist");
            	return false;
        	}
        	else if(resType==context.getResources().getInteger(R.integer.THEORY_TYPE)){
        		file=getLocalTheoryFile(i);
        		if(file.exists())return true;
            	Log.d(logTag,"The theory file "+file.getAbsolutePath()+" is not exist");
            	return false;
        	}
        	
        	Log.e(logTag,"FileSysManager.isFileValid(): Logical error: the resource type shouldn't "+resType);
        	return false;
        }
        
        public static boolean isExtMemWritable(){
        	return (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        }
        
        public static long getTotalMemory(int locate)
        {
        	return (statFs[locate].getBlockCount() * statFs[locate].getBlockSize());
        }

        public static long getFreeMemory(int locate)
        {
        	return (statFs[locate].getAvailableBlocks() * statFs[locate].getBlockSize());
        }
        
        public static int getGlobalUsage(int locate){
        	double result=statFs[locate].getAvailableBlocks();
        	result/=statFs[locate].getBlockCount();
        	return (int) (result*100);
        }
        
        public static long getAppUsed(int locate){
        	long total=0;
        	String[] dirs={context.getString(R.string.audioDirName),context.getString(R.string.subtitleDirName)};
        	
        	for(String s: dirs){
        		File[] fs=new File(srcRoot[locate]+File.separator+s).listFiles();
        		for(File f:fs)
        			total+=f.length();
        	}
        	return total;
        }
        
        public void setDiskSpaceFullListener(DiskSpaceFullListener dsfl){
            this.diskFullListener=dsfl;
        }
    

        
    class DiskSpaceFullListener{
        public void diskSpaceFull(){}
    }
}
