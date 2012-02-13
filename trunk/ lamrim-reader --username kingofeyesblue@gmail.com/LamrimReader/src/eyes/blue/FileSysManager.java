package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;

public class FileSysManager {
        public static String[] fileName=null;
        public static int[] fileSize=null;
        static int targetFileIndex=-1;
        static String logTag=null;
        static int NO_CACHE=0;
        static int EXTERNAL_MEMORY=1;
        static int INTERNAL_MEMORY=2;
        
        static SharedPreferences options=null;
        static StatFs[] statFs=null;
        static Context context=null;
        static String[] remoteSite=null;
        static DownloadFailListener downloadFailListener=null;
        static DiskSpaceFullListener diskFullListener=null;
        static DownloadFinishListener downloadFinishListener=null;
        static DownloadProgressListener downloadProgressListener=null;
        static FileOperationFailListener fileOperationFailListener =null;
        static int downloadFromSite=-1;
        static int bufLen=16384;
        
        public FileSysManager(Context context){
//              statFs[0]=new StatFs(Environment.getRootDirectory().getAbsolutePath());
//              statFs[1]=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                this.remoteSite=context.getResources().getStringArray(R.array.remoteSite);
                this.logTag=context.getString(R.string.app_name);
                FileSysManager.fileName=context.getResources().getStringArray(R.array.fileName);
                FileSysManager.fileSize=context.getResources().getIntArray(R.array.fileSize);
                FileSysManager.context=context;
                options = context.getSharedPreferences(context.getString(R.string.optionFile), 0);
                checkFileStructure();
        }
        
        
        public void setFileOperationFailListener(FileOperationFailListener listener){
        	this.fileOperationFailListener=listener;
        } 
        
        public static void downloadFileFromRemote(int fileIndex){
                Log.d(logTag,"Download file from remote "+fileName[fileIndex]);
                
                InputStream is=null;
                targetFileIndex=fileIndex;
                Log.d(logTag,"Create "+remoteSite.length+" thread for check remote file.");
                CheckRemoteThread[] crt=new CheckRemoteThread[remoteSite.length];
                for(int i=0;i<crt.length;i++){
                        crt[i]=new CheckRemoteThread(remoteSite[i]+fileName[targetFileIndex],remoteSite,i);
                        crt[i].start();
                }
                
                
                synchronized(remoteSite){
                        try {
                        	Log.d(logTag,"Wait for any check thread weak up main thread");
                        	remoteSite.wait(8000);
                        } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                        	e.printStackTrace();
                        }
                        for(CheckRemoteThread t:crt){
                        	if(!t.isReached())t.stopThread();
                        	is=t.getInputStream();
                                downloadFromSite=t.getFromSite();
                        }
                }
                
                if(is==null){
                        Log.d(logTag,"There is no valid remote site for download this file");
                        downloadFailListener.downloadFail(targetFileIndex);
                }

                Log.d(logTag,"Create thread for download file");

                DownloadThread dt=new DownloadThread(context,is,targetFileIndex,bufLen,downloadFinishListener,downloadProgressListener, new DownloadFailListener(){
                        public void downloadFail(int index){
                                Log.d(logTag,"Download "+fileName[index]+" fail, Try to download from other site");
                                if(remoteSite.length<=1){
                                        Log.d(logTag,"There is no remote site for download file, recovery site list and return fail to Activate");
                                        remoteSite=context.getResources().getStringArray(R.array.remoteSite);
                                        downloadFailListener.downloadFail(index);
                                }
                                
                                ArrayList<String> al=new ArrayList<String>();
                                
                                Log.d(logTag,"Remove the fail site from remote site list, and restart download from other site.");
                                for(int i=0;i<remoteSite.length;i++){
                                        if(i==downloadFromSite)continue;
                                        al.add(remoteSite[i]);
                                }
                                remoteSite= al.toArray(new String[0]);
                                downloadFileFromRemote(targetFileIndex);
                        }
                });
                dt.start();
        }
        
        /*
         * the file structure is follow
         * [PackageDir]\[AppName](LamrimReader)\{Audio,Book,Subtitle}
         * */
        public static void checkFileStructure(){
        	boolean extWritable=(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        	File appRoot=null;
        	
        	if(extWritable)appRoot=context.getExternalFilesDir(context.getString(R.string.app_name));
        	else appRoot=context.getFileStreamPath(context.getString(R.string.app_name));
        	
        	File subDir=new File(appRoot+File.separator+context.getString(R.string.audioDirName));
        	if(!subDir.exists())subDir.mkdirs();
        	subDir=new File(appRoot+File.separator+context.getString(R.string.subtitleDirName));
        	if(!subDir.exists())subDir.mkdirs();
        	subDir=new File(appRoot+File.separator+context.getString(R.string.theoryDirName));
        	if(!subDir.exists())subDir.mkdirs();
        }
        
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * */
        public static File getLocalMediaFile(int i){
        	if(isExtMemWritable())return new File(context.getExternalFilesDir(File.separator+context.getString(R.string.app_name)).getAbsoluteFile()+File.separator+context.getString(R.string.audioDirName)+File.separator+fileName[i]);
        	else return new File(context.getFileStreamPath(context.getString(R.string.app_name)).getAbsoluteFile()+File.separator+context.getString(R.string.audioDirName)+File.separator+fileName[i]);
        }
        
        public static boolean isFileValid(int i){
        	Log.d(logTag,"Check the existed file");
        	File file=getLocalMediaFile(i);
        	if(!file.exists()){
        		Log.d(logTag,"The file"+file.getAbsolutePath()+" is not exist");
        		return false;
        	}
        	
        	int size=context.getResources().getIntArray(R.array.fileSize)[i];
        	
        	if(file.length()!=size){
        		Log.d(logTag,"The size of file is not corrent, should be "+size+", but "+file.length());
        		return false;
        	}
        	
        	// !!!!!!!!!!!! There should check the file more condition.
        	return true;
        }
        
        public int getRecommandStoreLocate(){
                int extFree=FreeMemory(EXTERNAL_MEMORY);
                int intFree=FreeMemory(INTERNAL_MEMORY);
                
                if(extFree>2000)return EXTERNAL_MEMORY;
                if(intFree>2000)return INTERNAL_MEMORY;
                return NO_CACHE;
        }
        

        
        
        public static boolean isExtMemWritable(){
        	return (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()));
        }
        
        
        public void setDownloadFailListener(DownloadFailListener dfl){
            this.downloadFailListener=dfl;
        }
        
        public void setDiskSpaceFullListener(DiskSpaceFullListener dsfl){
            this.diskFullListener=dsfl;
        }
    
        public void setDownloadProgressListener(DownloadProgressListener downloadProgressListener){
            this.downloadProgressListener=downloadProgressListener; 
        }
    
        public void setDownloadFinishListener(DownloadFinishListener downloadFinishListener){
            this.downloadFinishListener=downloadFinishListener;
        }
        public int TotalMemory(int locate)
        {
        	return (statFs[locate].getBlockCount() * statFs[locate].getBlockSize()) >>> 20;
        }

        public int FreeMemory(int locate)
        {
        	return (statFs[locate].getAvailableBlocks() * statFs[locate].getBlockSize()) >>> 20;
        }

    class DiskSpaceFullListener{
        public void diskSpaceFull(){}
    }

}
