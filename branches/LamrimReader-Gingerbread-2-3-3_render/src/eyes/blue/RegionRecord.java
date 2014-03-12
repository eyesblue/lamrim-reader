package eyes.blue;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class RegionRecord {
/* 更新: $Date: 2013-09-27 19:05:54 +0800 (Fri, 27 Sep 2013) $
* 作者: $Author: kingofeyesblue@gmail.com $
* 版本: $Revision: 75 $
* ID  ：$Id: RegionRecord.java 75 2013-09-27 11:05:54Z kingofeyesblue@gmail.com $
*/
	public int version = -1;
	public int contentSerial = -1;
	public String title=null, createTime=null, info = null;
	public int mediaIndex=-1;
	public int startTimeMs=-1;
	public int endTimeMs=-1;
	static ArrayList<RegionRecord> records = null;
	
	public static void init(Activity activity){records=getAllRecord(activity);}
	public static RegionRecord addRegionRecord(Activity activity, int contentSerial,String title,int mediaIndex,int startTimeMs,int endTimeMs, String info){
		SimpleDateFormat sdf=new SimpleDateFormat();
		
		RegionRecord rr=new RegionRecord();
    	rr.version=1;
    	rr.contentSerial=contentSerial;
    	rr.title=title;
    	rr.mediaIndex=mediaIndex;
    	rr.createTime=sdf.getDateTimeInstance().toString();
    	rr.startTimeMs=startTimeMs;
    	rr.endTimeMs=endTimeMs;
    	rr.info=info;
    	
    	records.add(0,rr);
    	syncToFile(activity); 

    	return rr;
	}
	
	public static RegionRecord getRegionRecord(Activity activity,int i){
		return records.get(i);
	}
	
	public static void removeRecord(Activity activity,int i){
		records.remove(i);
		syncToFile(activity);
	}
	
	public static void updateRecord(Activity activity, int contentSerial,String title,int mediaIndex,int startTimeMs,int endTimeMs, int recordIndex){
		SimpleDateFormat sdf=new SimpleDateFormat();
		RegionRecord rr=records.get(recordIndex);
    	rr.version=1;
    	rr.contentSerial=contentSerial;
    	rr.title=title;
    	rr.mediaIndex=mediaIndex;
    	rr.createTime=sdf.getDateTimeInstance().toString();
    	rr.startTimeMs=startTimeMs;
    	rr.endTimeMs=endTimeMs;
    	
		syncToFile(activity);
	}
	
	public static ArrayList<RegionRecord> getAllRecord(Activity context){
		if(records!=null)return records;
		
		BufferedReader br;
		String line="";
		records=new ArrayList<RegionRecord>();

		try {
			br=new BufferedReader(new InputStreamReader(context.openFileInput(context.getString(R.string.regionRecordColumeName))));
			while((line=br.readLine())!=null){
				records.add(RegionRecord.stringToObj(line));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return records;
		}catch (Exception e) {
			e.printStackTrace();
			showErrToast(context,"讀取檔案時發生錯誤，無法讀取區段記錄！");
		}
		return records;
	}
	
	public static void syncToFile(final Activity activity){
/*		AsyncTask<Void,Void,Void> task = new AsyncTask<Void,Void,Void>(){
			@Override
			protected Void doInBackground(Void... arg0) {
*/				OutputStreamWriter osw;
				String str="";
				
				if(records.size()!=0)
				for(int  i=records.size()-1;i>0;i--)
					str+=objToString(records.get(i))+"\n";

				try {
					osw=new OutputStreamWriter(activity.openFileOutput(activity.getString(R.string.regionRecordColumeName),activity.MODE_WORLD_WRITEABLE));
					osw.write(str);
					osw.flush();
					osw.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					showErrToast(activity,"同步檔案時發生錯誤，找不到檔案！");
//					return null;
				} catch (IOException e) {
					e.printStackTrace();
					showErrToast(activity,"讀取檔案時發生錯誤，無法讀取區段記錄！");
//					return null;
				}
//				return null;
//			}};
//			task.execute();
	}
	
	
	public static RegionRecord stringToObj(String record){
		RegionRecord ro=new RegionRecord();
		try {
			JSONObject jObject = new JSONObject(record);
			ro.version=jObject.getInt("version");
			ro.contentSerial=jObject.getInt("contentSerial");
			ro.title=jObject.getString("title");
			ro.createTime=jObject.getString("createTime");
			ro.mediaIndex=jObject.getInt("mediaIndex");
			ro.startTimeMs=jObject.getInt("startTime");
			ro.endTimeMs=jObject.getInt("endTime");
			ro.info=jObject.getString("info");
		} catch (JSONException e) {	e.printStackTrace();}
		
		return ro;
		
	}
	
	public static String objToString(RegionRecord record){
		JSONObject jObj=new JSONObject();
		try {
		jObj.put("version", record.version);
		jObj.put("contentSerial", record.contentSerial);
		jObj.put("title", record.title);
		jObj.put("createTime", record.createTime);
		jObj.put("mediaIndex", record.mediaIndex);
		jObj.put("startTime", record.startTimeMs);
		jObj.put("endTime", record.endTimeMs);
		jObj.put("info", record.info);
		} catch (JSONException e) {e.printStackTrace();}
		return jObj.toString();
	}
	
	private static void showErrToast(final Activity activity,final String msg){
		activity.runOnUiThread(new Runnable(){

			@Override
			public void run() {
				Toast toast=Toast.makeText(activity, msg, Toast.LENGTH_LONG);
				toast.show();
			}});
		
	}
}
