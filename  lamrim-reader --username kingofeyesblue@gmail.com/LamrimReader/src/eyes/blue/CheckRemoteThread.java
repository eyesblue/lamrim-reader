package eyes.blue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

public class CheckRemoteThread extends StopableThread{
    Object lockKey=null;
    String sitePath=null;
    HttpEntity httpEntity=null;
    int respCode=-1;
    long contentLength=-1;
    int index=-1;
    long startTime=-1;
    long waitTime=-1;
    
    public CheckRemoteThread(String sitePath,Object lockKey,int index,String threadName,long waitTime){
    	setName(threadName);
            this.lockKey=lockKey;
            this.sitePath=sitePath;
            this.index=index;
            this.waitTime=waitTime;
    }
    
    public void run(){
    	startTime=System.currentTimeMillis();
/*            URL conn=null;
            try {
                            conn=new URL(sitePath);
                    } catch (MalformedURLException e) {
                            // TODO Auto-generated catch block
                            Log.d("Lamrim Reader","Program error: The remote URL format is unvalid.");
                            e.printStackTrace();
                    }
            if(stopThread)return;
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
*/
    	HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(sitePath);
		HttpResponse response=null;
    	try {
			response = httpclient.execute(httpget);
			respCode=response.getStatusLine().getStatusCode();
//				For debug
			if(respCode!=200)System.out.println("CheckRemoteThread: Return code not equal 200! check "+sitePath+" return "+respCode);
    	}catch (ClientProtocolException e) {
    		e.printStackTrace();
    		notifyWaiter();
    		return;
    	}catch (IOException e) {
    		e.printStackTrace();
    		notifyWaiter();
    		return;
    	}
    	httpEntity=response.getEntity();
    	contentLength=httpEntity.getContentLength();
    	// After waitTime, the mainly download thread has give up the download, don't wake up download thread, or cause next error notify for next download.
    	
    	long endTime=System.currentTimeMillis();
    	long spendTime=endTime-startTime;
    	Log.d("LamrimReader","Check spend time: File exist, ready for download, start="+startTime+", endTime="+endTime+", SpendTime="+spendTime);
    	if(spendTime<waitTime)notifyWaiter();
    	System.out.println("Thread stoped.");
    }
    private void notifyWaiter(){synchronized(lockKey){lockKey.notify();Log.d("CheckRemoteThread","Check thread notify "+sitePath+" at "+System.currentTimeMillis());}}
    
    public int getResponseCode(){return respCode;}
    public InputStream getInputStream() throws IllegalStateException, IOException{return httpEntity.getContent();}
    public int getFromSite(){return index;}
    public long getContentLength(){return contentLength;}
    public void release(){
    	if(httpEntity!=null)
		try {
			httpEntity.getContent().close();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}}
}
