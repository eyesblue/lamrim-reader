package eyes.blue;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import android.content.Context;
import android.util.Log;

public class DownloadThread extends Thread{
	Context context=null;
    InputStream is=null;
    int bufLen=-1;
    DownloadFinishListener downloadFinishListener=null;
    DownloadFailListener downloadFailListener=null;
    DownloadProgressListener downloadProgressListener=null;
    String fileList[]=null;
    int[] fileSize=null;
    int fileIndex;
    String logTag=null;
    long crc32=-1;
//    String fileName=null;
    
    public DownloadThread(Context context,InputStream is,int fileIndex,long crc32,int bufLen,DownloadFinishListener downloadFinishListener,DownloadProgressListener downloadProgressListener,DownloadFailListener downloadFailListener){
    	this.context=context;
            this.is=is;
            this.bufLen=bufLen;
            this.downloadFinishListener=downloadFinishListener;
            this.downloadFailListener=downloadFailListener;
            this.downloadProgressListener=downloadProgressListener;
            fileList=context.getResources().getStringArray(R.array.fileName);
            fileSize=context.getResources().getIntArray(R.array.mediaFileSize);
            this.fileIndex=fileIndex;
            logTag=context.getResources().getString(R.string.app_name);
            this.crc32=crc32;
    }
    
    public void run(){
            Log.d(logTag,"Download Thread started");
            long startTime=System.currentTimeMillis();
            int percent=fileSize[fileIndex]/100;
            int percentArray[]=new int[100];
            int percentIndex=0;
            int readLen=-1;
            int counter=0;
            Checksum checksum = new CRC32();
            
            FileOutputStream fos=null;
            if(context==null)Log.d(logTag,"The context is NULL");

            try {
            	fos=new FileOutputStream( FileSysManager.getLocalMediaFile(fileIndex));
            } catch (FileNotFoundException e1) {
            	Log.e(logTag,"File not found Exception happen while open the "+FileSysManager.getLocalMediaFile(fileIndex));
            	e1.printStackTrace();
            }

            Log.d(logTag,"One percent is "+percent);
            for(int i=0;i<100;i++){
                    percentArray[i]=(i+1)*percent;
                    Log.d(logTag,"Set array["+i+"] = "+percentArray[i]);
            }
            
            try {
            	byte[] buf=new byte[bufLen];
            	Log.d(logTag,"Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
            	while((readLen=is.read(buf))!=-1){
            		counter+=readLen;
            		fos.write(buf,0,readLen);
            		checksum.update(buf,0,readLen);
            		if(percentIndex<=99&&counter>=percentArray[percentIndex]){
            			downloadProgressListener.setDownloadProgress(++percentIndex);
            			Log.d(logTag,"Add one percent");
            		}
            	}

            is.close();
            fos.flush();
            fos.close();
            } catch (IOException e) {
            	e.printStackTrace();
            	Log.d(logTag,"IOException happen while download media.");
            	downloadFailListener.downloadMediaFail(fileIndex);
            }
            Log.d(logTag,"Download finish, notify downloadFinishListener");
            long sum = checksum.getValue();
            boolean isCorrect=(crc32==sum);
            int spend=(int) (System.currentTimeMillis()-startTime);
            Log.d(logTag,this.getName()+":File index: "+fileIndex+" Read length: "+counter+", CRC32 check: "+((isCorrect)?" Correct!":" Error!"+" ("+sum+"/"+crc32)+"), spend time: "+spend+"ms");
            if(!isCorrect)downloadFailListener.downloadMediaFail(fileIndex);
            else downloadFinishListener.downloadMediaFinish(fileIndex);
    }
}
