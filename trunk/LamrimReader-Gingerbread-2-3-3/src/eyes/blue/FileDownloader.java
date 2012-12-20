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

import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

/*
 * Download data from remote. while the downloader active, The UI of activity control by the class, you can use the ring progress of main thread of activity and skip after download. 
 * */
public class FileDownloader {
	LamrimReaderActivity activity=null;
	DownloadListener listener=null;
	static ArrayList<RemoteSource> remoteResources=new ArrayList<RemoteSource>();
	Downloader downloader=null;
	ProgressBar progressBar=null;
	TextView subtitleView = null;
	
	/*
	 * Give the activity and download listener.
	 * */
	public FileDownloader(LamrimReaderActivity activity,DownloadListener listener){
		this.activity=activity;
		this.listener=listener;
		if(remoteResources.size() == 0){
			GoogleRemoteSource grs=new GoogleRemoteSource(activity);
			remoteResources.add(grs);
			Log.d(getClass().getName(),"Add remote resource site: " +grs.getName()+", there are "+remoteResources.size()+" site in list.");
		}
		progressBar=(ProgressBar) activity.findViewById(R.id.progressBar);
		subtitleView=(TextView) activity.findViewById(R.id.subtitleView);
	}
	
	/*
	 * Start download source of media, it download the subtitle and media file. call DownloadListener.prepareFinish after all download finish.
	 * */
	public void start(int index){
		// json content: mediaIndex,type,outputPath,url
		JSONArray ja=new JSONArray();
		JSONObject jSubtitle=new JSONObject();
		JSONObject jMedia=new JSONObject();
		RemoteSource rs=remoteResources.get(0);
		try {
			jSubtitle.put("mediaIndex", index);
			jSubtitle.put("type", activity.getResources().getInteger(R.integer.SUBTITLE_TYPE));
			jSubtitle.put("outputPath",FileSysManager.getLocalSubtitleFile(index));
			jSubtitle.put("url", rs.getSubtitleFileAddress(index));
			
			jMedia.put("mediaIndex", index);
			jMedia.put("type", activity.getResources().getInteger(R.integer.MEDIA_TYPE));
			jMedia.put("outputPath", FileSysManager.getLocalMediaFile(index));
			jMedia.put("url", rs.getMediaFileAddress(index));
		} catch (JSONException e) {e.printStackTrace();}
		boolean isSubtitleExist=FileSysManager.isFileValid(index, activity.getResources().getInteger(R.integer.SUBTITLE_TYPE));
		boolean isSpeechExist=FileSysManager.isFileValid(index, activity.getResources().getInteger(R.integer.MEDIA_TYPE));

		// Subtitle and speech has ready, no need to download.
		if(isSubtitleExist && isSpeechExist){
			 listener.prepareFinish(index);
			 return;
		}
		
		downloader=new Downloader();
		
		// Subtitle not ready, but speech ready, download subtitle only.
		if(!isSubtitleExist && isSpeechExist){
			downloader.execute(jSubtitle);
			return;
		}	 

		downloader.execute(jSubtitle,jMedia);
	}
	
	/*
	 * Stop and release threads create by the class.
	 * */
	public void finish(){
		if(downloader !=null && downloader.getStatus()!=AsyncTask.Status.FINISHED)
			downloader.cancel(false);
	}
	
	/*
	 * Return is the AsyncTask running.
	 * */
	public boolean isRunning(){
		if(downloader == null)return false;
		return (downloader.getStatus()==Status.RUNNING);
	}
	
	public class Downloader extends AsyncTask<JSONObject, Integer, Boolean> {

		@Override
		protected Boolean doInBackground(JSONObject... urls) {
			// json content: mediaIndex,type,outputPath,url
			String url=null, outputPath=null;
			int mediaIndex=-1,type=-1;
			
			for(JSONObject j:urls){
				
				try {
					url = j.getString("url");
					type=j.getInt("type");
					outputPath=j.getString("outputPath");
					mediaIndex=j.getInt("mediaIndex");
				} catch (JSONException e2) {
					e2.printStackTrace();
				}
				
				if (type == activity.getResources().getInteger(R.integer.MEDIA_TYPE)) {
					activity.setSubtitleViewText("音檔下載中，請稍候");
				} else if (type == activity.getResources().getInteger(R.integer.SUBTITLE_TYPE)) {
					activity.setSubtitleViewText("字幕下載中，請稍候");
				}
				
				// We allow download fail of subtitle, but not speech.
				if(!download(url, outputPath,mediaIndex,type) && type==activity.getResources().getInteger(R.integer.MEDIA_TYPE)){
					((ProgressBar)activity.findViewById(R.id.progressBar)).setVisibility(View.GONE);
					listener.prepareFail(mediaIndex);
					return false;
				}
			}
			
			activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					((ProgressBar)activity.findViewById(R.id.progressBar)).setVisibility(View.GONE);
				}});
			listener.prepareFinish(mediaIndex);
//			downloader=null;
			return true;
		}
		
		private boolean download(String url, String outputPath, int mediaIndex,int type){
			((ProgressBar)activity.findViewById(R.id.progressBar)).setProgress(0);
			Log.d(getClass().getName(),"Download file from "+url);
			File tmpFile=new File(outputPath+"dltmp");
	        long startTime=System.currentTimeMillis();
	        int readLen=-1;
	        int counter=0;
	        int bufLen=activity.getResources().getInteger(R.integer.downloadBufferSize);
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
	    	
	    	((ProgressBar)activity.findViewById(R.id.progressBar)).setMax((int)contentLength);
	    	
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
	        		
	        		final int passInt=counter;
	        		activity.runOnUiThread(new Runnable() {
	    				public void run() {
	    					((ProgressBar)activity.findViewById(R.id.progressBar)).setVisibility(View.VISIBLE);
	    					((ProgressBar)activity.findViewById(R.id.progressBar)).setProgress(passInt);
	    				}
	    			});
	        	}
	        is.close();
	        fos.flush();
	        fos.close();
	        } catch (IOException e) {
	        	e.printStackTrace();
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
	        	return false;
	        }

	        	
	        if(type==activity.getResources().getInteger(R.integer.MEDIA_TYPE)){
	        	long sum = checksum.getValue();
	        	boolean isCorrect=(FileSysManager.mp3FileCRC32[mediaIndex]==sum);
	        	int spend=(int) (System.currentTimeMillis()-startTime);
	        	Log.d(getClass().getName(),Thread.currentThread().getName()+": File index: "+mediaIndex+" Read length: "+counter+", CRC32 check: "+((isCorrect)?" Correct!":" Error!"+" ("+sum+"/"+FileSysManager.mp3FileCRC32[mediaIndex])+"), spend time: "+spend+"ms");
	        	if(!isCorrect){
	        		return false;
	        	}
	        }
	        // rename the protected file name to correct file name
	        tmpFile.renameTo(new File(outputPath));
	        Log.d(getClass().getName(),Thread.currentThread().getName()+": Download finish, notify downloadFinishListener");
	        return true;
		}

	}
}
