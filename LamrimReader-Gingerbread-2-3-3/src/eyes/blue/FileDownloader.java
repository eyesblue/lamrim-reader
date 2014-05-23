package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.analytics.tracking.android.MapBuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ProgressBar;
import android.widget.TextView;

/*
 * Download data from remote. while the downloader active, The UI of activity control by the class, you can use the ring progress of main thread of activity and skip after download.
 * */
public class FileDownloader {
        Activity activity=null;
        DownloadListener listener=null;
        SharedPreferences runtime = null;
        static ArrayList<RemoteSource> remoteResources=new ArrayList<RemoteSource>();
        Downloader downloader = null;
        ProgressDialog dlPrgsDialog;
        ProgressDialog mkDlTaskDialog;
        Object dlProgsKey=new Object(), mkDlTaskKey = new Object();
        AsyncTask<Void, Void, Void> checkTask;
        AlertDialog netAccessWarnDialog = null;
        private PowerManager.WakeLock wakeLock = null;
        JSONObject[] downloadTasks = null;
        /*
         * Give the activity and download listener.
         * */
        public FileDownloader(Activity activity){
                this.activity=activity;
                PowerManager powerManager=(PowerManager) activity.getSystemService(Context.POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, getClass().getName());
                //wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getName());
               
                //if(!wakeLock.isHeld()){wakeLock.acquire();}
                runtime = activity.getSharedPreferences(activity.getString(R.string.runtimeStateFile), 0);
                if(remoteResources.size() == 0){
                        GoogleRemoteSource grs=new GoogleRemoteSource(activity);
                        remoteResources.add(grs);
                        Log.d(getClass().getName(),"Add remote resource site: " +grs.getName()+", there are "+remoteResources.size()+" site in list.");
                }
                
                dlPrgsDialog = getDlprgsDialog();
                mkDlTaskDialog= new ProgressDialog(activity);
        }
       
        public FileDownloader(Activity activity,DownloadListener listener){
                this(activity);
                this.listener=listener;
        }

        public void setDownloadListener(DownloadListener listener){
                this.listener=listener;
        }
       
       
        /*
         * Start download source of media, it download the subtitle and media file. call DownloadListener.prepareFinish after all download finish.
         * */
        public void start(final int... index){
                final ArrayList<JSONObject> al =new ArrayList<JSONObject>();
                Log.d(getClass().getName(),"mkDlTaskDialog="+((mkDlTaskDialog==null)?"Null":"not null"));
                synchronized(mkDlTaskKey){
                        if(mkDlTaskDialog!=null){
                                mkDlTaskDialog.dismiss();
//                                mkDlTaskDialog=null;
                        }
                }
                       
                
               
                //mkDlTaskProgDialog;
                //AlertDialog netAccessDialog = null;
               
                checkTask=new AsyncTask<Void, Void, Void>() {
       
                        @Override
                        protected void onCancelled(){
                                if(wakeLock.isHeld())wakeLock.release();
                        }

                        @Override
                        protected Void doInBackground(Void... params) {
                                for(int i:index){
                                        Log.d(getClass().getName(),"Check sources of index "+i);
                                        if(!FileSysManager.isFileValid(i, activity.getResources().getInteger(R.integer.SUBTITLE_TYPE)))
                                        	al.add(getSubtitleDesc(i));
                                        if(!FileSysManager.isFileValid(i, activity.getResources().getInteger(R.integer.MEDIA_TYPE)))
                                        	al.add(getMediaDesc(i));
                                        synchronized(mkDlTaskKey){
                                                if(mkDlTaskDialog!=null && mkDlTaskDialog.isShowing())mkDlTaskDialog.setProgress(i+1);
                                                if(this.isCancelled()){
                                                        mkDlTaskDialog.dismiss();
//                                                      mkDlTaskDialog=null;
                                                        return null;
                                                }
                                        }
                                }
                               
                                if(al.size()==0){
                                        synchronized(mkDlTaskKey){
                                                if(mkDlTaskDialog!=null && mkDlTaskDialog.isShowing()){
                                                        mkDlTaskDialog.dismiss();
//                                                        mkDlTaskDialog=null;
                                                }
                                        }
                                        if(wakeLock.isHeld())wakeLock.release();
                                        Log.d(getClass().getName(),"Call All prepare finish at prepare download stage.");
                                        listener.allPrepareFinish(index);
                                        return null;
                                }
                               
                                downloadTasks=al.toArray(new JSONObject[0]);
                                //if(mkDlTaskDialog.isShowing())
                                synchronized(mkDlTaskKey){
                                        mkDlTaskDialog.dismiss();
//                                        mkDlTaskDialog=null;
                                }
                                Log.d(getClass().getName(),"Call checkNetAccessPermission()");
                                checkNetAccessPermission();
                               
                                return null;
                        }

                };

                // Do not show the prepare download dialog while short term, or may happen the thread finish before dialog show, then the dialog will not been close.
                if(index.length>1){
                        mkDlTaskDialog.setTitle("準備下載");
                        mkDlTaskDialog.setMessage("檔案驗證中，請稍候 ...");
                        mkDlTaskDialog.setCanceledOnTouchOutside(false);
                        mkDlTaskDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                        mkDlTaskDialog.setMax(index.length);
                        mkDlTaskDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                        Log.d(getClass().getName(),"User cancel dialog mkDlTaskDialog.");
                                        checkTask.cancel(false);
                                        if(wakeLock.isHeld())wakeLock.release();
                                        synchronized(mkDlTaskKey){
                                                if(mkDlTaskDialog==null)return;
                                                mkDlTaskDialog.dismiss();
                                                listener.userCancel();
//                                                mkDlTaskDialog=null;
                                        }
                                }
                        });
                        if(!wakeLock.isHeld()){wakeLock.acquire();}
                        Handler handler=new Handler();  
                        handler.post(new Runnable(){
                                @Override
                                public void run() {
                                	try{
                                        if(!activity.isFinishing() && !mkDlTaskDialog.isShowing())mkDlTaskDialog.show();
                                	}catch(Exception e){
                                		e.printStackTrace();
                                        GaLogger.sendEvent("exception", "FileDownloader", "ShowDownloadTaskDialog", null);
                                	}
                        }});
                }
                Log.d(getClass().getName(),"Creat a source check task and start.");
                checkTask.execute();
        }
       
        public void checkNetAccessPermission(){
                boolean isShowNetAccessWarn=runtime.getBoolean(activity.getString(R.string.isShowNetAccessWarn), true);
                boolean isAllowAccessNet=runtime.getBoolean(activity.getString(R.string.isAllowNetAccess), false);
                Log.d(this.getClass().getName(),"ShowNetAccessWarn: "+isShowNetAccessWarn+", isAllowNetAccess: "+isAllowAccessNet);
                if(isShowNetAccessWarn ||(!isShowNetAccessWarn && !isAllowAccessNet)){
                        activity.runOnUiThread(new Runnable() {
                                public void run() {
                                        if(netAccessWarnDialog==null || !netAccessWarnDialog.isShowing()){
                                                if(!wakeLock.isHeld()){wakeLock.acquire();}
                                                netAccessWarnDialog=getNetAccessDialog();
                                                netAccessWarnDialog.setCanceledOnTouchOutside(false);
                                                new Handler().post(new Runnable(){
                                                        @Override
                                                        public void run() {
                                                                netAccessWarnDialog.show();
                                                        }});
                                        }
                                }
                        });
                        return ;
                }
                startDownloadThread();
        }
       
       private void startDownloadThread(final int... index){
        	downloader = new Downloader();
        	activity.runOnUiThread(new Runnable() {
        		public void run() {
        			dismissDlProgress();
        			
        			dlPrgsDialog.setCanceledOnTouchOutside(false);
        			dlPrgsDialog.setTitle("下載檔案");
        			dlPrgsDialog.setMessage("下載中，請稍候 ...");
        			dlPrgsDialog.setMax(index.length);
        			if(!dlPrgsDialog.isShowing() && activity!=null && !activity.isFinishing() && !activity.isRestricted())dlPrgsDialog.show();
        			if(!wakeLock.isHeld()){wakeLock.acquire();}
        			downloader.execute(downloadTasks);
        		}});
               
                return ;
        }
       
        private JSONObject getSubtitleDesc(int index){
                RemoteSource rs=remoteResources.get(0);
                JSONObject jObj=new JSONObject();
                // json content: mediaIndex,type,outputPath,url
                try {
                        jObj.put("mediaIndex", index);
                        jObj.put("type", activity.getResources().getInteger(R.integer.SUBTITLE_TYPE));
                        jObj.put("outputPath",FileSysManager.getLocalSubtitleFileSavePath(index));
                        jObj.put("url", rs.getSubtitleFileAddress(index));
                } catch (JSONException e) {e.printStackTrace();}
                return jObj;
        }
       
        private JSONObject getMediaDesc(int index){
                RemoteSource rs=remoteResources.get(0);
                JSONObject jObj=new JSONObject();
                // json content: mediaIndex,type,outputPath,url
                try {
                        jObj.put("mediaIndex", index);
                        jObj.put("type", activity.getResources().getInteger(R.integer.MEDIA_TYPE));
                        jObj.put("outputPath",FileSysManager.getLocalMediaFileSavePath(index));
                        jObj.put("url", rs.getMediaFileAddress(index));
                } catch (JSONException e) {e.printStackTrace();}
                return jObj;
        }
       
        private AlertDialog getNetAccessDialog(){
                final SharedPreferences.Editor editor = runtime.edit();
               
                LayoutInflater adbInflater = LayoutInflater.from(activity);
                View eulaLayout = adbInflater.inflate(R.layout.net_access_warn_dialog, null);
                final CheckBox dontShowAgain= (CheckBox) eulaLayout.findViewById(R.id.skip);
       
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setView(eulaLayout);
                builder.setTitle(activity.getString(R.string.dlgNetAccessTitle));
                builder.setMessage(activity.getString(R.string.dlgNetAccessMsg));
                builder.setPositiveButton(activity.getString(R.string.dlgAllow), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                        Log.d(getClass().getName(),"Check box check status: "+dontShowAgain.isChecked());
                       
                        editor.putBoolean(activity.getString(R.string.isShowNetAccessWarn), !dontShowAgain.isChecked());
                    editor.putBoolean(activity.getString(R.string.isAllowNetAccess), true);
                    editor.commit();
                    dialog.cancel();
                    new Thread(new Runnable(){
                                @Override
                                public void run() {
                                        startDownloadThread();
                                }}).start();
                }
            });
            builder.setNegativeButton(activity.getString(R.string.dlgDisallow), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                        Log.d(getClass().getName(),"Check box check status: "+dontShowAgain.isChecked());
                        editor.putBoolean(activity.getString(R.string.isShowNetAccessWarn), !dontShowAgain.isChecked());
                    editor.putBoolean(activity.getString(R.string.isAllowNetAccess), false);
                        editor.commit();
                        listener.userCancel();
                        if(wakeLock.isHeld())wakeLock.release();
                    dialog.cancel();
                }
            });

            return builder.create();
        }
       
       

       
        public class Downloader extends AsyncTask<JSONObject, Integer, Boolean> {
                JSONObject executing=null;
                boolean cancelled = false;

                public void cancelTask(){
                    if(wakeLock.isHeld())wakeLock.release();
                    Log.d(getClass().getName(),"onCancelled: User cancel the download procedule, set flag to cancelled");
                    cancelled = true;
                    listener.userCancel();
                }
                // From API call
                @Override
                protected void onCancelled(){
                	cancelTask();
                }
                // From API call               
                @Override
                protected void onCancelled(Boolean result) {
                	cancelTask();
                }
                
                @Override
                protected Boolean doInBackground(JSONObject... urls) {
                        // json content: mediaIndex,type,outputPath,url
                        String url=null, outputPath=null;
                        int mediaIndex=-1,type=-1;
                       
                        for(int i=0;i<urls.length;i++){
                                JSONObject j=urls[i];
                                try {
                                        url = j.getString("url");
                                        type=j.getInt("type");
                                        outputPath=j.getString("outputPath");
                                        mediaIndex=j.getInt("mediaIndex");
                                } catch (JSONException e2) {e2.printStackTrace();}
                               
                                if(cancelled){Log.d(getClass().getName(),"User canceled, download procedure skip!");return false;}
                               
//                              double d=(double)(i)/urls.length*dlPrgsDialog.getMax();
//                              dlPrgsDialog.setSecondaryProgress((int) d);
                               
                                // We not allow download fail either speech nor subtitle, but not speech.
                                executing=j;
                                if(!download(url, outputPath,mediaIndex,type)){
                                	if(cancelled){
                                        Log.d(getClass().getName(),"User canceled, return false");
                                        return false;
                                	}
                                    setProgressMsg(activity.getString(R.string.dlgTitleDownloadFail),activity.getString(R.string.dlgDescDownloadFail));
                                    Log.d(getClass().getName(),"User canceled, call prepareFail");
                                    listener.prepareFail(mediaIndex,type);
                                }
                                listener.prepareFinish(mediaIndex,type);
                        }
                       
                       
                        dismissDlProgress();
                        if(wakeLock.isHeld())wakeLock.release();
                        Log.d(getClass().getName(),"Call All prepare finish at downloaded stage.");
                        listener.allPrepareFinish(mediaIndex);
//                      downloader=null;
                        return true;
                }
               
               
               
                private boolean download(String url, String outputPath, int mediaIndex,int type){
                        setDlProgress(0);
                        Log.d(getClass().getName(),"Download file from "+url);
                        File tmpFile=new File(outputPath+activity.getString(R.string.downloadTmpPostfix));
                long startTime=System.currentTimeMillis(), respWaitStartTime;
               
                int readLen=-1, counter=0, bufLen=activity.getResources().getInteger(R.integer.downloadBufferSize);
                Checksum checksum = new CRC32();
                FileOutputStream fos=null;
               
                //HttpClient httpclient = getNewHttpClient();
                HttpClient httpclient = new DefaultHttpClient();
                        HttpGet httpget = new HttpGet(url);
                        HttpResponse response=null;
                        int respCode=-1;
                        if(cancelled){Log.d(getClass().getName(),"User canceled, download procedure skip!");return false;}
                       
                        setProgressMsg(activity.getString(R.string.dlgTitleConnecting),String.format(activity.getString(R.string.dlgDescConnecting), SpeechData.getNameId(mediaIndex),(type == activity.getResources().getInteger(R.integer.MEDIA_TYPE))?"音檔":"字幕"));
                       
                try {
                        respWaitStartTime=System.currentTimeMillis();
                                response = httpclient.execute(httpget);
                                respCode=response.getStatusLine().getStatusCode();

                                // For debug
                                if(respCode!=HttpStatus.SC_OK){
                                        httpclient.getConnectionManager().shutdown();
                                        System.out.println("CheckRemoteThread: Return code not equal 200! check return "+respCode);
                                }
                }catch (ClientProtocolException e) {
                        httpclient.getConnectionManager().shutdown();
                        e.printStackTrace();
                        return false;
                }catch (IOException e) {
                        httpclient.getConnectionManager().shutdown();
                        e.printStackTrace();
                        return false;
                }
               
                if(cancelled){
                        httpclient.getConnectionManager().shutdown();
                        Log.d(getClass().getName(),"User canceled, download procedure skip!");
                        return false;
                }
                GaLogger.send(MapBuilder
                              .createTiming("download",    // Timing category (required)
                                          System.currentTimeMillis()-respWaitStartTime,       // Timing interval in milliseconds (required)
                                    "wait resp time",  // Timing name
                                    null)           // Timing label
                      .build());
               
                HttpEntity httpEntity=response.getEntity();
                InputStream is=null;
                        try {
                                is = httpEntity.getContent();
                        } catch (IllegalStateException e2) {
                                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                                httpclient.getConnectionManager().shutdown();
                                tmpFile.delete();
                                e2.printStackTrace();
                                return false;
                        } catch (IOException e2) {
                                httpclient.getConnectionManager().shutdown();
                                tmpFile.delete();
                                e2.printStackTrace();
                                return false;
                        }
                       
                        if(cancelled){
                                Log.d(getClass().getName(),"User canceled, download procedure skip!");
                                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                                httpclient.getConnectionManager().shutdown();
                                tmpFile.delete();
                                return false;
                        }
                       
                        final long contentLength=httpEntity.getContentLength();
               
                        setDlProgressMax((int)contentLength);
                       
               
                try {
                                fos=new FileOutputStream(tmpFile);
                        } catch (FileNotFoundException e1) {
                                Log.d(getClass().getName(),"File Not Found Exception happen while create output temp file ["+tmpFile.getName()+"] !");
                                httpclient.getConnectionManager().shutdown();
                                try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                                tmpFile.delete();
                                e1.printStackTrace();
                                return false;
                        }

                if(cancelled){
                        httpclient.getConnectionManager().shutdown();
                        try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                        try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
                        tmpFile.delete();
                        Log.d(getClass().getName(),"User canceled, download procedure skip!");
                        return false;
                }
               
                setProgressMsg(activity.getString(R.string.dlgTitleDownloading),String.format(activity.getString(R.string.dlgDescDownloading), SpeechData.getNameId(mediaIndex),(type == activity.getResources().getInteger(R.integer.MEDIA_TYPE))?"音檔":"字幕"));
                setDlProgressMax((int) contentLength);
               
                try {
                        byte[] buf=new byte[bufLen];
                        Log.d(getClass().getName(),Thread.currentThread().getName()+": Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
                        while((readLen=is.read(buf))!=-1){
                               
                                counter+=readLen;
                                fos.write(buf,0,readLen);
                                checksum.update(buf,0,readLen);
                                setDlProgress(counter);

                                if(cancelled){
                                        httpclient.getConnectionManager().shutdown();
                                        try {   is.close();     } catch (IOException e) {e.printStackTrace();}
                                try {   fos.close();    } catch (IOException e) {e.printStackTrace();}
                                tmpFile.delete();
                                Log.d(getClass().getName(),"User canceled, download procedure skip!");
                                return false;
                        }
                        }
                is.close();
                fos.flush();
                fos.close();
                } catch (IOException e) {
                        httpclient.getConnectionManager().shutdown();
                        try {   is.close();     } catch (IOException e2) {e2.printStackTrace();}
                        try {   fos.close();    } catch (IOException e2) {e2.printStackTrace();}
                        tmpFile.delete();
                        e.printStackTrace();
                        Log.d(getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
                        return false;
                }

                if(counter!=contentLength || cancelled){
                        httpclient.getConnectionManager().shutdown();
                        tmpFile.delete();
                        return false;
                }
               
                // Check is options mediaCheckSum indicate we should check crc at this stage.
                if(type==activity.getResources().getInteger(R.integer.MEDIA_TYPE) && activity.getString(R.string.mediaCheckSumWhileDownload).equalsIgnoreCase("true")){
                        long sum = checksum.getValue();
                        boolean isCorrect=(SpeechData.crc[mediaIndex]==sum);
                        int spend=(int) (System.currentTimeMillis()-startTime);
                       
                        Log.d(getClass().getName(),Thread.currentThread().getName()+": File index: "+mediaIndex+" Read length: "+counter+", CRC32 check: "+((isCorrect)?" Correct!":" Error!"+" ("+sum+"/"+SpeechData.crc[mediaIndex])+"), spend time: "+spend+"ms");
                        if(!isCorrect){
                                httpclient.getConnectionManager().shutdown();
                                tmpFile.delete();
                                return false;
                        }
                        GaLogger.send(MapBuilder
                                      .createTiming("download",    // Timing category (required)
                                                  (long)spend,       // Timing interval in milliseconds (required)
                                                  "download time",  // Timing name
                                                  null)           // Timing label
                                                  .build());
                }
                // rename the protected file name to correct file name
                tmpFile.renameTo(new File(outputPath));
                httpclient.getConnectionManager().shutdown();
                Log.d(getClass().getName(),Thread.currentThread().getName()+": Download finish, return true.");
                return true;
                }
               
/*                public HttpClient getNewHttpClient() {
                    try {
                        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                        trustStore.load(null, null);

                        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
                        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                        HttpParams params = new BasicHttpParams();
                        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
                        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

                        SchemeRegistry registry = new SchemeRegistry();
                        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
                        registry.register(new Scheme("https", sf, 443));

                        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

                        return new DefaultHttpClient(ccm, params);
                    } catch (Exception e) {
                        return new DefaultHttpClient();
                    }
                }
*/
        }
       
       
        private ProgressDialog getDlprgsDialog(){
                ProgressDialog pd= new ProgressDialog(activity);
                pd.setCancelable(false);
                pd.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(downloader!=null && isRunning()){
                                Log.d(getClass().getName(),"The download procedure been cancel.");
                                activity.runOnUiThread(new Runnable() {
                                        public void run() {
                                                downloader.cancel(true);
                                                downloader.cancelTask();
                                                if(wakeLock.isHeld())wakeLock.release();
                                        }
                                });
                        }
                        dismissDlProgress();
                    }
                });
                pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                return pd;
        }
       
        /*
         * Stop and release threads create by the class.
         * */
/*      private void finish(){
                synchronized(mkDlTaskKey){
                        if(mkDlTaskDialog!=null){
                                mkDlTaskDialog.dismiss();
                                mkDlTaskDialog=null;
                        }
                }
                if(downloader !=null && downloader.getStatus()!=AsyncTask.Status.FINISHED)
                        downloader.cancel(false);
                if(checkTask  !=null && checkTask.getStatus()!=AsyncTask.Status.FINISHED)
                        checkTask.cancel(true);
                dismissDlProgress();
        }
*/      
        /*
         * Return is the AsyncTask running.
         * */
        public boolean isRunning(){
                if(downloader == null)return false;
                return (downloader.getStatus()==Status.RUNNING);
        }
       
        private void setProgressMsg(final String title,final String msg){
                activity.runOnUiThread(new Runnable() {
                        public void run() {
                                synchronized(dlProgsKey){
                                	try{
                                        if(dlPrgsDialog==null && !dlPrgsDialog.isShowing())return;
                                        if(title!=null)dlPrgsDialog.setTitle(title);
                                        if(msg!=null)dlPrgsDialog.setMessage(msg);
                                	}catch(Exception e){
                                        e.printStackTrace();
                                        GaLogger.sendEvent("exception", "progress_dialog", "dismiss_after_dismissed", null);
                                }
                                }
                        }});
        }
       
        private void setDlProgress(final int progress){
                activity.runOnUiThread(new Runnable() {
                        public void run() {
                                synchronized(dlProgsKey){
                                	try{
                                        if(dlPrgsDialog!=null && dlPrgsDialog.isShowing())
                                                dlPrgsDialog.setProgress(progress);
                                	}catch(Exception e){
                                        e.printStackTrace();
                                        GaLogger.sendEvent("exception", "progress_dialog", "dismiss_after_dismissed", null);
                                }
                                }
                        }
                });
        }
       
        private void setDlProgressMax(final int max){
                activity.runOnUiThread(new Runnable() {
                        public void run() {
                                synchronized(dlProgsKey){
                                	try{
                                        if(dlPrgsDialog!=null && dlPrgsDialog.isShowing())
                                        dlPrgsDialog.setMax(max);
                                	}catch(Exception e){
                                        e.printStackTrace();
                                        GaLogger.sendEvent("exception", "progress_dialog", "dismiss_after_dismissed", null);
                                }
                                }
                        }
                });
        }
       
        private void dismissDlProgress(){
                activity.runOnUiThread(new Runnable() {
                        public void run() {
                                synchronized(dlProgsKey){
                                        if(dlPrgsDialog!=null && dlPrgsDialog.isShowing()){
                                                // Here ever happen dismiss after dismissed.
                                                try{
                                                        dlPrgsDialog.dismiss();
//                                                        dlPrgsDialog=null;
                                                }catch(Exception e){
                                                        e.printStackTrace();
                                                        GaLogger.sendEvent("exception", "progress_dialog", "dismiss_after_dismissed", null);
                                                }
                                }
                                if(wakeLock.isHeld())wakeLock.release();
                                }
                        }});
        }
       
/*
        public class MySSLSocketFactory extends SSLSocketFactory {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
                super(truststore);

                TrustManager tm = new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    }

                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                };

                sslContext.init(null, new TrustManager[] { tm }, null);
            }

            @Override
            public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
                return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
            }

            @Override
            public Socket createSocket() throws IOException {
                return sslContext.getSocketFactory().createSocket();
            }
        }
        */
}
