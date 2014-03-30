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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.Locale;

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

//import net.simonvt.calendarview.CalendarView;





import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.csvreader.CsvReader;
import com.disegnator.robotocalendar.RobotoCalendarView;
import com.disegnator.robotocalendar.RobotoCalendarView.RobotoCalendarListener;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

public class CalendarActivity extends SherlockActivity {
	Hashtable<String,GlRecord> glSchedule=new Hashtable<String,GlRecord>();
	ProgressDialog downloadPDialog = null;
	SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy/MM/dd");
	Date glRangeStart=null, glRangeEnd=null;
	
	
	// For Calendar view.
	private RobotoCalendarView robotoCalendarView;
    private Calendar currentCalendar;
    private int currentMonthIndex;
    
    GlRecord selectedGlr=null;
	boolean dialogShowing=false;
	String selectedDay=null;
	
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_calendar);
		getSupportActionBar();
		
		initialCalendarView();
		downloadPDialog=new ProgressDialog(CalendarActivity.this);
		downloadPDialog.setTitle(getString(R.string.dlgTitleDownloading));
		downloadPDialog.setMessage( String.format(getString(R.string.dlgDescDownloading),"", getString(R.string.title_activity_calendar)));
	}

	
	
	@Override
	protected void onStart() {
		super.onStart();
		GaLogger.activityStart(this);
		GaLogger.sendEvent("activity", "CalendarActivity", "into_onStart", null);
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				File schedule=getLocalScheduleFile();
				if(schedule!=null){
					if(reloadSchedule(schedule))
						if(glRangeEnd.getTime()>System.currentTimeMillis())
							return;
				}

				dialogShowing=true;
				if(!downloadSchedule())return;
				schedule=getLocalScheduleFile();
				if(schedule==null)return;
				reloadSchedule(schedule);
			}}).start();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		GaLogger.activityStop(this);
	}
	
	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(getString(R.string.downloadSchedule))
            .setIcon(R.drawable.update)
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        return true;
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if(dialogShowing)return true;
		dialogShowing=true;
		new Thread(new Runnable(){
			@Override
			public void run() {
				if(!downloadSchedule())return;
				
				File schedule=getLocalScheduleFile();
				if(schedule == null)return;
				reloadSchedule(schedule);
			}}).start();
		return true;
	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED || selectedGlr==null || !isFileExist(selectedGlr)) 
			finish();

		if(intent == null){
			GaLogger.sendException("", new Exception("SpeechMenuActivity return data to CalendarActivity Failure(Failure delivering result ResultInfo)."), true);
			return;
		}
		
		setResult(Activity.RESULT_OK, getResultIntent(selectedGlr));
		finish();
	}
	
	private void initialCalendarView(){
		robotoCalendarView = (RobotoCalendarView) findViewById(R.id.robotoCalendarPicker);
		

        // Initialize the RobotoCalendarPicker with the current index and date
        currentMonthIndex = 0;
        currentCalendar = Calendar.getInstance(Locale.getDefault());
        robotoCalendarView.initializeCalendar(currentCalendar);

        // Mark current day
        robotoCalendarView.markDayAsCurrentDay(currentCalendar.getTime());
 
        robotoCalendarView.setRobotoCalendarListener(new RobotoCalendarListener(){

			@Override
			public void onDateSelected(Date date) {
				if(dialogShowing)return;
				dialogShowing=true;
				
				robotoCalendarView.markDayAsSelectedDay(date);
				String key=dateFormater.format(date);
				selectedDay=key;
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
							dialog.dismiss();
							dialogShowing=false;
							finish();
						}
						else{
							final Intent speechMenu = new Intent(CalendarActivity.this,	SpeechMenuActivity.class);
							int[] speechStart=GlRecord.getSpeechStrToInt(glr.speechPositionStart);// {speechIndex,min,sec}
							int[] speechEnd=GlRecord.getSpeechStrToInt(glr.speechPositionEnd);// {speechIndex,min,sec}
							dialog.dismiss();
							dialogShowing=false;
							speechMenu.putExtra("index", speechStart[0]+","+speechEnd[0]);
							startActivityForResult(speechMenu, 0);
						}
			        }
			    });
			    builder.setNegativeButton(getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
			        public void onClick(DialogInterface dialog, int id) {
			            dialog.cancel();
			            dialogShowing=false;
			        }
			    });
				builder.create().show();
			}

			@Override
			public void onRightButtonClick() {
				currentMonthIndex++;
		        updateCalendar();
		        if(currentMonthIndex == 0)robotoCalendarView.markDayAsCurrentDay(currentCalendar.getTime());
		        markScheduleDays();
			}

			@Override
			public void onLeftButtonClick() {
				currentMonthIndex--;
		        updateCalendar();
		        if(currentMonthIndex == 0)robotoCalendarView.markDayAsCurrentDay(currentCalendar.getTime());
		        markScheduleDays();
			}
			
			private void updateCalendar() {
		        currentCalendar = Calendar.getInstance(Locale.getDefault());
		        currentCalendar.add(Calendar.MONTH, currentMonthIndex);
		        robotoCalendarView.initializeCalendar(currentCalendar);
		    }
        });
	}
	
	/*
	 * Call this for update UI, it seems fire UI update too quick in short time,
	 * If I just place [robotoCalendarView.markDayWithStyle(style[index], month.getTime());] in runOnUiThread()
	 * the UI refresh fragmentation.
	 * */
	private void markScheduleDays() {
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				markScheduleDaysInCalendar();
			}});
	}
	
	private void markScheduleDaysInCalendar() {
		Log.d(getClass().getName(),"Into Mark schedule function");
		final Calendar month = (Calendar) currentCalendar.clone();
		int daysOfMonth=month.getActualMaximum(Calendar.DAY_OF_MONTH);
		Log.d(getClass().getName(),"Get Month: "+month.get(Calendar.YEAR)+"/"+month.get(Calendar.MONTH)+", there are "+daysOfMonth);
        	
		//final int style[]={RobotoCalendarView.BLUE_CIRCLE, RobotoCalendarView.GREEN_CIRCLE, RobotoCalendarView.RED_CIRCLE};
		final int style[]={RobotoCalendarView.BLUE_LINE, RobotoCalendarView.GREEN_LINE, RobotoCalendarView.RED_LINE};
		int styleIndex=0;
		GlRecord lastGlr=null;
		for(int i=1;i<= daysOfMonth;i++){
			month.set(Calendar.DAY_OF_MONTH, i);
			String key=dateFormater.format(month.getTime());
			Log.d(getClass().getName(),"Get Data with key: "+key);
			GlRecord glr=glSchedule.get(key);
			if(glr == null)continue;
        		
			if(lastGlr != null && glr != lastGlr)
				if(++styleIndex == style.length)styleIndex = 0;
        		
			Log.d(getClass().getName(),"Mark "+month.get(Calendar.YEAR)+"/"+month.get(Calendar.MONTH)+"/"+i+" as style"+styleIndex);
			final int index=styleIndex;
			robotoCalendarView.markDayWithStyle(style[index], month.getTime());
			lastGlr=glr;
        	}
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
		
	private File getLocalScheduleFile() {
		String scheFileName=getString(R.string.globalLamrimScheduleFile);
		String format=getString(R.string.globalLamrimScheduleFileFormat);
		File file=null;
		file=new File(getFilesDir()+File.separator+scheFileName+"."+format);
		Log.d(getClass().getName(),"Schedule file: "+file.getAbsolutePath());
		if(file.exists())return file;
		return null;
	}
	
	private boolean reloadSchedule(File file) {
		CsvReader csvr=null;
		
		// Load the date range of file.
		try {
			csvr=new CsvReader(file.getAbsolutePath(),',', Charset.forName("UTF-8"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileNotFound));
			GaLogger.sendException("File: "+file.getAbsolutePath(), e,true);
			return false;
		}
		
		try {
			if(!csvr.readRecord()){
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr));
				GaLogger.sendException("Error happen while csv reader read record csvr.readRecord()", new Exception("Error happen while read record of csv reader."), true);
				return false;
			}
			int count=csvr.getColumnCount();
			if(count<2){
				Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileRangeFmtErr));
				GaLogger.sendException("Error: the date start and date end colume of global lamrim schedule file is not 2 colume", new Exception("Global Lamrim schedule file format error."), true);
				return false;
			}
			//DateFormat df = DateFormat.getDateInstance();
			//DateFormat df = new SimpleDateFormat("EE-MM-dd-yyyy");
			
			try {
				Date arg1=dateFormater.parse(csvr.get(0));
				Date arg2=dateFormater.parse(csvr.get(1));
				//Date arg1=df.parse(csvr.get(0));
				//Date arg2=df.parse(csvr.get(1));
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
				GaLogger.sendException("Error happen while parse data region of Global Lamrim schedule file: data1="+csvr.get(0)+", data2="+csvr.get(1), e, true);
				return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr));
			GaLogger.sendException("IOException happen while read date region of global lamrim schedule file.", e, true);
			return false;
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
				if(!addGlRecord(glr))return false;
			}
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileDecodeErr));
			GaLogger.sendException("IOException happen while read data of global lamrim schedule file.", e, true);
			return false;
		}
		markScheduleDays();
		Log.d(getClass().getName(),"Total records: "+glSchedule.size());
		return true;
	}
	
	private boolean addGlRecord(GlRecord glr){
		//DateFormat df = DateFormat.getDateInstance();
		Date startDate=null, endDate=null;
		String key=null;
		int length=0;

		try {
			startDate = dateFormater.parse(glr.dateStart);
			endDate = dateFormater.parse(glr.dateEnd);
			length=(int) ((endDate.getTime()-startDate.getTime())/86400000)+1;
		} catch (ParseException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.localGlobalLamrimScheduleFileReadErr)+"\""+glr+"\"");
			GaLogger.sendException("Date format parse error: startDate="+startDate+", endDate="+endDate, e, true);
			return false;
		}
		
		for(int i=0;i<length;i++){
			startDate.setTime(startDate.getTime()+(i*86400000));
			key=dateFormater.format(startDate);
			glSchedule.put(key, glr);
		}
		Log.d(getClass().getName(),"Add record: key="+key+", data="+glr);
		return true;
	}

	
	private boolean downloadSchedule() {
		GoogleRemoteSource grs=new GoogleRemoteSource(getApplicationContext());
		String url=grs.getGlobalLamrimSchedule();
		String scheFileName=getString(R.string.globalLamrimScheduleFile);
		String tmpFileSub=getString(R.string.downloadTmpPostfix);
		String format=getString(R.string.globalLamrimScheduleFileFormat);
		File tmpFile=new File(getFilesDir()+File.separator+scheFileName+tmpFileSub);
		File scheFile=new File(getFilesDir()+File.separator+scheFileName+"."+format);
		
		runOnUiThread(new Runnable(){@Override	public void run(){	downloadPDialog.show();	}});
		
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
				return false;
	        }
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return false;
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
			return false;
		} catch (IOException e2) {
			httpclient.getConnectionManager().shutdown();
			e2.printStackTrace();
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return false;
		}

		final long contentLength=httpEntity.getContentLength();
		Log.d(getClass().getName(),"Content length: "+contentLength);
		FileOutputStream fos=null;
		int counter=0;
		try {
			Log.d(getClass().getName(),"Create download temp file: "+tmpFile);
			fos=new FileOutputStream(tmpFile);
		} catch (FileNotFoundException e) {e.printStackTrace();return false;}
		
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
			try {   is.close();     } catch (IOException e2) {e2.printStackTrace();return false;}
			try {   fos.close();    } catch (IOException e2) {e2.printStackTrace();return false;}
			tmpFile.delete();
			e.printStackTrace();
			Log.d(getClass().getName(),Thread.currentThread().getName()+": IOException happen while download media.");
			Util.showNarmalToastMsg(CalendarActivity.this, getString(R.string.dlgDescDownloadFail));
			downloadPDialog.dismiss();
			return false;
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
		dialogShowing=false;
		return true;
	}
	
/*	private void showDownloadProgDialog(){
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				downloadPDialog = ProgressDialog.show(CalendarActivity.this, getString(R.string.dlgTitleDownloading), String.format(getString(R.string.dlgDescDownloading),"", getString(R.string.title_activity_calendar)), true);
		}});
	}
*/	
	private Intent getResultIntent(GlRecord glr){
		Intent data=new Intent();
		data.putExtra("selectedDay", selectedDay);
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