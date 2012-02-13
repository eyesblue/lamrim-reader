package eyes.blue;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.util.Log;

public class CheckRemoteThread extends StopableThread{
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
    }
    public void stopThread(){stopThread=true;}
    public boolean isReached(){return reached;}
    public InputStream getInputStream(){return is;}
    public int getFromSite(){return index;}
}
