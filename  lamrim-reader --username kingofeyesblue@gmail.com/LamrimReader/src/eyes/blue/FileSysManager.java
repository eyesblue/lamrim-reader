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
        int targetFileIndex=-1;
        String logTag=null;
        static int NO_CACHE=0;
        static int EXTERNAL_MEMORY=1;
        static int INTERNAL_MEMORY=2;
        
        SharedPreferences options=null;
        static StatFs[] statFs=null;
        static Context context=null;
        public static int defLocate =0;
        String[] remoteSite=null;
        DownloadFailListener downloadFailListener=null;
        DiskSpaceFullListener diskFullListener=null;
        DownloadFinishListener downloadFinishListener=null;
        DownloadProgressListener downloadProgressListener=null;
        FileOperationFailListener fileOperationFailListener =null;
        int downloadFromSite=-1;
        int bufLen=16384;
        
        public FileSysManager(Context context,int defLocate,String[] remoteSite){
                FileSysManager.defLocate=defLocate;
//              statFs[0]=new StatFs(Environment.getRootDirectory().getAbsolutePath());
//              statFs[1]=new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
                this.remoteSite=remoteSite;
                this.logTag=context.getString(R.string.app_name);
                FileSysManager.fileName=context.getResources().getStringArray(R.array.fileName);
                FileSysManager.fileSize=context.getResources().getIntArray(R.array.fileSize);
                this.context=context;
                options = context.getSharedPreferences(context.getString(R.string.optionFile), 0);
        }
        
        public void setFileOperationFailListener(FileOperationFailListener listener){
        	this.fileOperationFailListener=listener;
        } 
        
        public void downloadFileFromRemote(int fileIndex){
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
                DownloadThread dt=new DownloadThread(is,fileName[targetFileIndex],bufLen,downloadFinishListener, new DownloadFailListener(){
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
        	
        	File subDir=new File(appRoot+context.getString(R.string.audioDirName));
        	if(!subDir.exists())subDir.mkdirs();
        	subDir=new File(appRoot+context.getString(R.string.subtitleDirName));
        	if(!subDir.exists())subDir.mkdirs();
        	subDir=new File(appRoot+context.getString(R.string.theoryDirName));
        	if(!subDir.exists())subDir.mkdirs();
        }
        
        /*
         * The location of media file is [PackageDir]\[AppName](LamrimReader)\Audio
         * */
        public static File getLocalMediaFile(int i){
        	if(isExtMemWritable())return new File(context.getExternalFilesDir(File.separator+context.getString(R.string.app_name)).getAbsoluteFile()+File.separator+context.getString(R.string.audioDirName));
        	else return new File(context.getFileStreamPath(context.getString(R.string.app_name)).getAbsoluteFile()+File.separator+context.getString(R.string.audioDirName));
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
    
    class CheckRemoteThread extends Thread{
        boolean start=true;
        Object lockKey=null;
        String sitePath=null;
        boolean reached=false;
        InputStream is=null;
        int index=-1;
        
        public CheckRemoteThread(String sitePath,Object lockKey,int index){
                this.lockKey=lockKey;
                this.sitePath=sitePath;
                this.index=index;
        }
        
        public void run(){
                URL conn=null;
                try {
                                conn=new URL(sitePath);
                        } catch (MalformedURLException e) {
                                // TODO Auto-generated catch block
                                Log.d("Lamrim Reader","Program error: The remote URL format is unvalid.");
                                e.printStackTrace();
                        }
                if(!start)return;
                try {
                                is=conn.openStream();
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                        }
                reached=true;
                synchronized(lockKey){
                        lockKey.notify();
                }
        }
        public void stopThread(){start=false;}
        public boolean isReached(){return reached;}
        public InputStream getInputStream(){return is;}
        public int getFromSite(){return index;}
    }
    
    class DownloadThread extends Thread{
        InputStream is=null;
        int bufLen=-1;
        DownloadFinishListener downloadFinishListener=null;
        DownloadFailListener downloadFailListener=null;
        String fileName=null;
        
        public DownloadThread(InputStream is,String name,int bufLen,DownloadFinishListener downloadFinishListener,DownloadFailListener downloadFailListener){
                this.is=is;
                this.fileName=name;
                this.bufLen=bufLen;
                this.downloadFinishListener=downloadFinishListener;
                this.downloadFailListener=downloadFailListener;
        }
        public void run(){
                Log.d(logTag,"Download Thread started");
                byte[] buf=new byte[bufLen];
                
                FileOutputStream fos=null;
                if(context==null)Log.d(logTag,"The context is NULL");
                try {
                        if(defLocate==INTERNAL_MEMORY)
                                fos=context.openFileOutput(fileName, Context.MODE_PRIVATE);
                        if(defLocate==EXTERNAL_MEMORY)
                                fos=new FileOutputStream(context.getString(R.string.appExtMemDirName)+fileName);
                        } catch (FileNotFoundException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                        }
                
                try {
                        int percent=fileSize[targetFileIndex]/100;
                        int percentArray[]=new int[100];
                        int percentIndex=0;
                        int readLen=-1;
                        int counter=0;
                        Log.d(logTag,"One percent is "+percent);
                        for(int i=0;i<100;i++){
                                percentArray[i]=(i+1)*percent;
                                Log.d(logTag,"Set array["+i+"] = "+percentArray[i]);
                        }
                        
                        Log.d(logTag,"Start read stream from remote site");
                                while((readLen=is.read(buf))!=-1){
                                        counter+=readLen;
                                        fos.write(buf,0,readLen);
                                        Log.d(logTag,"Read length: "+counter+", index="+percentIndex);
                                        if(percentIndex<=99&&counter>=percentArray[percentIndex]){
                                                downloadProgressListener.setDownloadProgress(++percentIndex);
                                                Log.d(logTag,"Add one percent");
                                        }
                                }
                                is.close();
                                fos.flush();
                        fos.close();
                        } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                                Log.d(logTag,"IOException happen while download media.");
                                downloadFailListener.downloadFail(targetFileIndex);
                        }
                Log.d(logTag,"Download finish, notify downloadFinishListener");
                downloadFinishListener.downloadFinish(targetFileIndex);
        }
    }

    class DiskSpaceFullListener{
        public void diskSpaceFull(){}
    }

}
