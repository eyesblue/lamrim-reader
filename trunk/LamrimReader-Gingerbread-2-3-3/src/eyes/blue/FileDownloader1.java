package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.widget.ProgressBar;

public class FileDownloader1 {
	Context context=null;
	DownloadListener listener=null;
	static ArrayList<RemoteSource> remoteResources=new ArrayList<RemoteSource>();
	Downloader downloader=null;
	
	public FileDownloader1(Context context,DownloadListener listener){
		this.context=context;
		this.listener=listener;
		if(remoteResources.size() == 0){
			GoogleRemoteSource grs=new GoogleRemoteSource(context);
			remoteResources.add(grs);
			Log.d(getClass().getName(),"Add remote resource site: " +grs.getName()+", there are "+remoteResources.size()+" site in list.");
		}
	}
	
	public void start(int index){
		// json content: mediaIndex,type,outputPath,url
		JSONArray ja=new JSONArray();
		JSONObject jSubtitle=new JSONObject();
		JSONObject jMedia=new JSONObject();
		RemoteSource rs=remoteResources.get(0);
		
		if(FileSysManager.isFileValid(index, context.getResources().getInteger(R.integer.SUBTITLE_TYPE)) &&
				FileSysManager.isFileValid(index, context.getResources().getInteger(R.integer.MEDIA_TYPE))){
			 listener.prepareFinish(index);
			 return;
		}
			 
		
		try {
			jSubtitle.put("mediaIndex", index);
			jSubtitle.put("type", context.getResources().getInteger(R.integer.SUBTITLE_TYPE));
			jSubtitle.put("outputPath",FileSysManager.getLocalSubtitleFile(index));
			jSubtitle.put("url", rs.getSubtitleFileAddress(index));
			
			jMedia.put("mediaIndex", index);
			jMedia.put("type", context.getResources().getInteger(R.integer.MEDIA_TYPE));
			jMedia.put("outputPath", FileSysManager.getLocalMediaFile(index));
			jMedia.put("url", rs.getMediaFileAddress(index));
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		downloader=new Downloader();
		downloader.execute(jSubtitle,jMedia);
	}
	
	public void finish(){
		if(downloader !=null && downloader.getStatus()!=AsyncTask.Status.FINISHED)
			downloader.cancel(false);
	}
	
	public boolean isRunning(){
		if(downloader == null)return false;
		return (downloader.getStatus()==Status.RUNNING);
	}
	
	public class Downloader extends AsyncTask<JSONObject, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(JSONObject... urls) {
			for(JSONObject j:urls){
				if(!download(j))return false;
			}
			
			int index=-1;
			try {
				index=urls[0].getInt("mediaIndex");
			} catch (JSONException e){
				e.printStackTrace();
			}
			listener.prepareFinish(index);
			downloader=null;
			return true;
		}
		
		private boolean download(JSONObject jobj){
			// json content: mediaIndex,type,outputPath,url
			Log.d(getClass().getName(),Thread.currentThread().getName()+": Download Thread started");
			String url=null, outputPath=null;
			int mediaIndex=-1,type=-1;
			
			listener.downloadPreExec(jobj);
			try {
				url = jobj.getString("url");
				type=jobj.getInt("type");
				outputPath=jobj.getString("outputPath");
				mediaIndex=jobj.getInt("mediaIndex");
			} catch (JSONException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			
//			File outputFile=new File(outputPath);
			File tmpFile=new File(outputPath+"dltmp");
	        long startTime=System.currentTimeMillis();
	        int readLen=-1;
	        int counter=0;
	        int bufLen=context.getResources().getInteger(R.integer.downloadBufferSize);
	        Checksum checksum = new CRC32();
	        FileOutputStream fos=null;
	        
	        HttpClient httpclient = new DefaultHttpClient();
			HttpGet httpget = new HttpGet(url);
			HttpResponse response=null;
			int respCode=-1;
	    	try {
				response = httpclient.execute(httpget);
				respCode=response.getStatusLine().getStatusCode();
//					For debug
				if(respCode!=200)System.out.println("CheckRemoteThread: Return code not equal 200! check return "+respCode);
	    	}catch (ClientProtocolException e) {
	    		e.printStackTrace();
	    		return false;
	    	}catch (IOException e) {
	    		e.printStackTrace();
//	    		notifyWaiter();
	    		return false;
	    	}
	    	HttpEntity httpEntity=response.getEntity();
	    	InputStream is=null;
			try {
				is = httpEntity.getContent();
			} catch (IllegalStateException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
	    	long contentLength=httpEntity.getContentLength();
	    	
	        listener.setMaxProgress(contentLength);
	        try {
				fos=new FileOutputStream(outputPath);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}

	        
	        try {
	        	byte[] buf=new byte[bufLen];
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
	        	while((readLen=is.read(buf))!=-1){
	        		counter+=readLen;
	        		fos.write(buf,0,readLen);
	        		checksum.update(buf,0,readLen);
	        		listener.setDownloadProgress(counter);
	        	}
	        is.close();
	        fos.flush();
	        fos.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
	        	listener.downloadFail(jobj);
	        	return false;
	        }

	        	
	        if(type==context.getResources().getInteger(R.integer.MEDIA_TYPE)){
	        	long sum = checksum.getValue();
	        	boolean isCorrect=(FileSysManager.mp3FileCRC32[mediaIndex]==sum);
	        	int spend=(int) (System.currentTimeMillis()-startTime);
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": File index: "+mediaIndex+" Read length: "+counter+", CRC32 check: "+((isCorrect)?" Correct!":" Error!"+" ("+sum+"/"+FileSysManager.mp3FileCRC32[mediaIndex])+"), spend time: "+spend+"ms");
	        	if(!isCorrect){
	        		listener.downloadFail(jobj);
	        		return false;
	        	}
	        }
	        // rename the protected file name to correct file name
	        tmpFile.renameTo(new File(outputPath));
	        Log.d(getClass().getName(),Thread.currentThread().getName()+": Download finish, notify downloadFinishListener");
	        listener.downloadFinish(jobj);
	        return true;
		}

	}
}
