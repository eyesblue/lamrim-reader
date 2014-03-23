package eyes.blue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;

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
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import net.simonvt.calendarview.CalendarView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.csvreader.CsvReader;

import eyes.blue.FileDownloader.MySSLSocketFactory;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CalendarActivity extends SherlockFragmentActivity {
	Hashtable<String,GlRecord> glSchedule=new Hashtable<String,GlRecord>();
	ProgressDialog downloadPDialog = null;
	SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd");
	Date glRangeStart=null, glRangeEnd=null;
	Toast toast=null;
	
	GlRecord selectedGlr=null;
			
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calendar);
		getSupportActionBar();
		
		initialCalendarView();
		toast = new Toast(getApplicationContext());
	}

	long userSelectDay=-1;
	private void initialCalendarView(){
		final CalendarView cv=(CalendarView) findViewById(R.id.calendarView);
		cv.setShownWeekCount(4);
		cv.setShowWeekNumber(false);
		cv.setFocusedMonthDateColor(Color.WHITE);
		cv.setUnfocusedMonthDateColor(Color.rgb(100, 100, 100));
		userSelectDay=cv.getDate();
	    cv.setOnDateChangeListener(new CalendarView.OnDateChangeListener(){
			@Override
			public void onSelectedDayChange(CalendarView view, int year, int month, int dayOfMonth) {
				if(cv.getDate()==userSelectDay)return;
				userSelectDay=cv.getDate();
				String mStr=""+(month+1);
				String dStr=""+dayOfMonth;
				if(mStr.length()==1)mStr="0"+mStr;
				if(dStr.length()==1)dStr="0"+dStr;
				String key=year+"/"+mStr+"/"+dStr;
				final GlRecord glr=glSchedule.get(key);
				if(glr==null){
					Log.d(getClass().getName(),"No record for: "+key);
					return;
				}
				
				selectedGlr=glr;
				
				String msg="日期: "+glr.dateStart+" ~ "+glr.dateEnd+"\n";
				msg+="音檔: "+glr.speechPositionStart+" ~ "+glr.speechPositionEnd+"\n";
				msg+="長度: "+glr.totalTime+"\n";
				msg+="廣論: "+glr.theoryLineStart+" ~ "+glr.theoryLineEnd+"\n";
				msg+="手抄: "+glr.subtitleLineStart+" ~ "+glr.subtitleLineEnd+"\n";
				msg+="內容: "+glr.desc;
				
				AlertDialog.Builder builder = new AlertDialog.Builder(CalendarActivity.this);
			    builder.setTitle(key);
			    builder.setMessage(msg);
			    builder.setPositiveButton(getString(R.string.dlgOk), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			        	
						
						if (isFileExist(glr)){
							setResult(Activity.RESULT_OK, getResultIntent(glr));
							finish();
						}
						else{
							final Intent speechMenu = new Intent(CalendarActivity.this,	SpeechMenuActivity.class);
							int[] speechStart=GlRecord.getSpeechStrToInt(glr.speechPositionStart);// {speechIndex,min,sec}
							int[] speechEnd=GlRecord.getSpeechStrToInt(glr.speechPositionEnd);// {speechIndex,min,sec}
							speechMenu.putExtra("index", speechStart[0]+","+speechEnd[0]);
							startActivityForResult(speechMenu, 0);
						}
			        }
			    });
			    builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			            dialog.cancel();
			        }
			    });
				builder.create().show();
		}});
	    
	    ImageButton loadHistory=(ImageButton) cv.findViewById(R.id.loadHistory);
	    ImageButton downloadSche=(ImageButton) cv.findViewById(R.id.downloadSche);
	    
	    downloadSche.setOnClickListener(new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				new Thread(new Runnable(){
					@Override
					public void run() {
						downloadSchedule();
						reloadSchedule(getLocalScheduleFile());
					}}).start();
			}});
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		new Thread(new Runnable(){
			@Override
			public void run() {
/*				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
*/				File schedule=getLocalScheduleFile();
				if(schedule==null){
					downloadSchedule();
				}
				else{
					reloadSchedule(schedule);
				}
			}}).start();
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED || selectedGlr==null || !isFileExist(selectedGlr)) 
			finish();

		setResult(Activity.RESULT_OK, getResultIntent(selectedGlr));
		finish();
	}
	
	private boolean isFileExist(GlRecord glr){
		int[] speechStart=GlRecord.getSpeechStrToInt(glr.speechPositionStart);// {speechIndex,min,sec}
		int[] speechEnd=GlRecord.getSpeechStrToInt(glr.speechPositionEnd);// {speechIndex,min,sec}
		
    	File mediaStart = FileSysManager.getLocalMediaFile(speechStart[0]);
		File subtitleStart = FileSysManager.getLocalSubtitleFile(speechStart[0]);
		File mediaEnd = FileSysManager.getLocalMediaFile(speechEnd[0]);
		File subtitleEnd = FileSysManager.getLocalSubtitleFile(speechEnd[0]);
		return (mediaStart.exists() && subtitleStart.exists() && mediaEnd.exists() && subtitleEnd.exists());
	}
		
	private Intent getResultIntent(GlRecord glr){
		Intent data=new Intent();
    	data.putExtra("dateStart", glr.dateStart);
    	data.putExtra("dateEnd", glr.dateEnd);
    	data.putExtra("speechPositionStart", glr.speechPositionStart);
    	data.putExtra("speechPositionEnd", glr.speechPositionEnd);
    	data.putExtra("totalTime", glr.totalTime);
    	data.putExtra("theoryLineStart", glr.theoryLineStart);
    	data.putExtra("theoryLineEnd", glr.theoryLineEnd);
    	data.putExtra("subtitleLineStart", glr.subtitleLineStart);
    	data.putExtra("subtitleLineEnd", glr.subtitleLineEnd);
    	data.putExtra("desc", glr.desc);
    	return data;
	}
	
	private void reloadSchedule(File file) {
		CsvReader csvr=null;
		
		// Load the date range of file.
		try {
			csvr=new CsvReader(file.getAbsolutePath(),',', Charset.forName("UTF-8"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileNotFound));
			return;
		}
		
		try {
			if(!csvr.readRecord()){
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr));
				return;
			}
			int count=csvr.getColumnCount();
			if(count<2){
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileRangeFmtErr));
				return;
			}
			DateFormat df = DateFormat.getDateInstance();
			try {
				Date arg1=df.parse(csvr.get(0));
				Date arg2=df.parse(csvr.get(1));
				if(arg1.getTime()<arg2.getTime()){
					glRangeStart=arg1;
					glRangeEnd=arg2;
				}else{
					glRangeStart=arg2;
					glRangeEnd=arg1;
				}
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileDateFmtErr));
				return;
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr));
			return;
		}
		
		Log.d(getClass().getName(),"GlobalLamrim range start: "+glRangeStart);
		Log.d(getClass().getName(),"GlobalLamrim range end: "+glRangeEnd);
		
		try {
			while(csvr.readRecord()){
				int count=csvr.getColumnCount();
				GlRecord glr=new GlRecord();

				glr.dateStart=csvr.get(0);
				glr.dateEnd=csvr.get(1);
				glr.speechPositionStart=csvr.get(2);
				glr.speechPositionEnd=csvr.get(3);
				glr.totalTime=csvr.get(4);
				glr.theoryLineStart=csvr.get(5);
				glr.theoryLineEnd=csvr.get(6);
				glr.subtitleLineStart=csvr.get(7);
				glr.subtitleLineEnd=csvr.get(8);
				glr.desc=csvr.get(9);
				addGlRecord(glr);
			}
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileDecodeErr));
			return;
		}
		Log.d(getClass().getName(),"Total records: "+glSchedule.size());
	}
	
	private void addGlRecord(GlRecord glr){
		DateFormat df = DateFormat.getDateInstance();
		Date startDate=null, endDate=null;
		String key=null;
		int length=0;

		try {
			startDate = df.parse(glr.dateStart);
			endDate = df.parse(glr.dateEnd);
			length=(int) ((endDate.getTime()-startDate.getTime())/86400000)+1;
		} catch (ParseException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr)+"\""+glr+"\"");
			return;
		}
		
		for(int i=0;i<length;i++){
			startDate.setTime(startDate.getTime()+(i*86400000));
			key=dateFormater.format(startDate);
			glSchedule.put(key, glr);
		}
		Log.d(getClass().getName(),"Add record: key="+key+", data="+glr);
	}

	private void downloadSchedule() {
		GoogleRemoteSource grs=new GoogleRemoteSource(getApplicationContext());
		String url=grs.getGlobalLamrimSchedule();
		String scheFileName=getString(R.string.globalLamrimScheduleFile);
		String tmpFileSub=getString(R.string.downloadTmpPostfix);
		String format=getString(R.string.globalLamrimScheduleFileFormat);
		File tmpFile=new File(getFilesDir()+File.separator+scheFileName+tmpFileSub);
		File scheFile=new File(getFilesDir()+File.separator+scheFileName+"."+format);
		
		showDownloadProgDialog();
		
		Log.d(getClass().getName(),"Download "+url);
		HttpClient httpclient = getNewHttpClient();
		HttpGet httpget = new HttpGet(url);
		HttpResponse response=null;
		int respCode=-1;

		try {
			response = httpclient.execute(httpget);
			respCode=response.getStatusLine().getStatusCode();
			if(respCode!=HttpStatus.SC_OK){
				httpclient.getConnectionManager().shutdown();
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
				downloadPDialog.dismiss();
				return;
	        }
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return;
		}
        
		

		Log.d(getClass().getName(),"Connect success, Downloading file.");
		HttpEntity httpEntity=response.getEntity();
		InputStream is=null;
		try {
			is = httpEntity.getContent();
		} catch (IllegalStateException e2) {
			try {   is.close();     } catch (IOException e) {e.printStackTrace();}
			httpclient.getConnectionManager().shutdown();
			e2.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return;
		} catch (IOException e2) {
			httpclient.getConnectionManager().shutdown();
			e2.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return;
		}

		final long contentLength=httpEntity.getContentLength();
		Log.d(getClass().getName(),"Content length: "+contentLength);
		FileOutputStream fos=null;
		int counter=0;
		try {
			Log.d(getClass().getName(),"Create download temp file: "+tmpFile);
			fos=new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e) {e.printStackTrace();}
		
		try {
			byte[] buf=new byte[getResources().getInteger(R.integer.downloadBufferSize)];
			int readLen=0;
			Log.d(getClass().getName(),Thread.currentThread().getName()+": Start read stream from remote site, is="+((is==null)?"NULL":"exist")+", buf="+((buf==null)?"NULL":"exist"));
			while((readLen=is.read(buf))!=-1){
				counter+=readLen;
				fos.write(buf,0,readLen);
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
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return;
		}
		
/*		if(counter!=contentLength){
			httpclient.getConnectionManager().shutdown();
			tmpFile.delete();
			showNarmalToastMsg(getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return;
		}
*/		
		// rename the protected file name to correct file name
		if(scheFile.exists())scheFile.delete();
        tmpFile.renameTo(scheFile);
        httpclient.getConnectionManager().shutdown();
        Log.d(getClass().getName(),Thread.currentThread().getName()+": Download finish.");
        downloadPDialog.dismiss();
	}
	
	private void showDownloadProgDialog(){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				downloadPDialog = ProgressDialog.show(CalendarActivity.this, getString(R.string.dlgTitleDownloading), String.format(getString(R.string.dlgDescDownloading),"", getString(R.string.title_activity_calendar)), true);
		}});
	}

	private File getLocalScheduleFile() {
		String scheFileName=getString(R.string.globalLamrimScheduleFile);
		String format=getString(R.string.globalLamrimScheduleFileFormat);
		File file=null;
		file=new File(getFilesDir()+File.separator+scheFileName+"."+format);
		Log.d(getClass().getName(),"Schedule file: "+file.getAbsolutePath());
		if(file.exists())return file;
		return null;
	}

/*	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.calendar, menu);
		return true;
	}
*/
	
	private HttpClient getNewHttpClient() {
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
}
