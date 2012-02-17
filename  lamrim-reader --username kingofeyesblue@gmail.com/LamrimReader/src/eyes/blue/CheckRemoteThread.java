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
    int index=-1;
    
    public CheckRemoteThread(String sitePath,Object lockKey,int index,String threadName){
    	setName(threadName);
            this.lockKey=lockKey;
            this.sitePath=sitePath;
            this.index=index;
    }
    
    public void run(){
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
			if(respCode!=200)System.out.println("CheckRemoteThread: check "+sitePath+" return "+respCode);
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
    	notifyWaiter();
    	System.out.println("Thread stoped.");
    }
    private void notifyWaiter(){synchronized(lockKey){lockKey.notify();}}
    
    public int getResponseCode(){return respCode;}
    public InputStream getInputStream() throws IllegalStateException, IOException{return httpEntity.getContent();}
    public int getFromSite(){return index;}
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
