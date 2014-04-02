package eyes.blue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import eyes.blue.LamrimReaderActivity.RegionRecordAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class Util {
	static Toast toast = null;
	static ArrayList<HashMap<String,String>> regionFakeList = new ArrayList<HashMap<String,String>>();
	static HashMap<String,String> fakeSample = new HashMap();

	public static void showSaveRegionDialog(final Activity activity, MediaPlayerController mpController, Runnable callBack){
		int regionStartMs=mpController.getRegionStartPosition();
		int regionEndMs=mpController.getRegionEndPosition();
	    final SubtitleElement startSubtitle=mpController.getSubtitle(regionStartMs);
	    final SubtitleElement endSubtitle=mpController.getSubtitle(regionEndMs-1);
	    String info=startSubtitle.text+" ~ "+endSubtitle.text;

	    Log.d("Util","Check size of region list before: "+RegionRecord.records.size());
/*		Runnable callBack=new Runnable(){
			@Override
			public void run() {
				activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					regionFakeList.add(fakeSample);
					if(regionRecordAdapter!=null)Log.d(logTag,"Warring: the regionRecordAdapter = null !!!");
					else regionRecordAdapter.notifyDataSetChanged();
					Log.d(logTag,"Check size of region list after: "+RegionRecord.records.size());
				}});
			}};
*/		
			BaseDialogs.showEditRegionDialog(activity, mpController.playingIndex , regionStartMs, regionEndMs, null, info, -1, callBack);
			GaLogger.sendEvent("ui_action", "show_dialog", "save_region", null);
	}
	
	public static void showSubtitleToast(final Activity activity,final String s){
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if(toast!=null)toast.cancel();
				toast = new Toast(activity.getApplicationContext());

				LayoutInflater inflater = activity.getLayoutInflater();
				View toastLayout = inflater.inflate(R.layout.toast_text_view, (ViewGroup) activity.findViewById(R.id.toastLayout));
				TextView toastTextView = (TextView) toastLayout.findViewById(R.id.text);
				Typeface educFont=Typeface.createFromAsset(activity.getAssets(), "EUDC.TTF");
				toastTextView.setTypeface(educFont);
				toastTextView.setText(s);
				
				toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
				toast.setDuration(Toast.LENGTH_LONG);
				toast.setView(toastLayout);
				toast.show();
			}
		});
	}
	
	public static void showNarmalToastMsg(final Activity activity, final String s){
		activity.runOnUiThread(new Runnable() {
			public void run() {
				if(toast!=null)toast.cancel();
                toast = new Toast(activity.getApplicationContext());

                LayoutInflater inflater = activity.getLayoutInflater();
                View toastLayout = inflater.inflate(R.layout.toast_text_view, (ViewGroup) activity.findViewById(R.id.toastLayout));
                TextView toastTextView = (TextView) toastLayout.findViewById(R.id.text);
                Typeface educFont=Typeface.createFromAsset(activity.getAssets(), "EUDC.TTF");
                toastTextView.setTypeface(educFont);
                toastTextView.setText(s);
               
                toast.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(toastLayout);
                toast.show();
			}
		});
	}
	
	public static void cancelToast(){
		if(toast==null)return;
		toast.cancel();
	}
	
	/**
	   * Enables/Disables all child views in a view group.
	   * 
	   * @param viewGroup the view group
	   * @param enabled <code>true</code> to enable, <code>false</code> to disable
	   * the views.
	   */
	  public static void enableDisableViewGroup(ViewGroup viewGroup, boolean enabled) {
	    int childCount = viewGroup.getChildCount();
	    for (int i = 0; i < childCount; i++) {
	      View view = viewGroup.getChildAt(i);
	      view.setEnabled(enabled);
	      if (view instanceof ViewGroup) {
	        enableDisableViewGroup((ViewGroup) view, enabled);
	      }
	    }
	  }
	
	public static String getMsToHMS(int ms){
		return getMsToHMS(ms,"'","\"",true);
	}
	
	public static String getMsToHMS(int ms,String minuteSign,String secSign,boolean hasDecimal){
		String sub=""+(ms%1000);
		if(sub.length()==1)sub="00"+sub;
		else if(sub.length()==2)sub="0"+sub;
	
		int second=ms/1000;
		int ht=second/3600;
		second=second%3600;
		int mt=second/60;
		second=second%60;
	
		String hs=""+ht;
		if(hs.length()==1)hs="0"+hs;
		String mst=""+mt;
		if(mst.length()==1)mst="0"+mst;
		String ss=""+second;
		if(ss.length()==1)ss="0"+ss;
	
//	System.out.println("getMSToHMS: input="+ms+"ms, ht="+ht+", mt="+mt+", sec="+second+", HMS="+hs+":"+ms+":"+ss+"."+sub);
		return mst+minuteSign+ss+((hasDecimal)?"."+sub:"")+secSign;
	}
	
	public static double getDisplaySizeInInch(Activity activity){
		DisplayMetrics dm = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
		double x = Math.pow(dm.widthPixels/dm.xdpi,2);
		double y = Math.pow(dm.heightPixels/dm.ydpi,2);
		double screenInches = Math.sqrt(x+y);
		return screenInches;
	}
	
	public static boolean unZip( String zipname , String extractTo)
	{       
	     InputStream is;
	     ZipInputStream zis;
	     try 
	     {
	         is = new FileInputStream(extractTo + zipname);
	         zis = new ZipInputStream(new BufferedInputStream(is));          
	         ZipEntry ze;

	         while ((ze = zis.getNextEntry()) != null) 
	         {
	             ByteArrayOutputStream baos = new ByteArrayOutputStream();
	             byte[] buffer = new byte[1024];
	             int count;

	             // zapis do souboru
	             String filename = ze.getName();
	             FileOutputStream fout = new FileOutputStream(extractTo + filename);

	             // cteni zipu a zapis
	             while ((count = zis.read(buffer)) != -1) 
	             {
	                 baos.write(buffer, 0, count);
	                 byte[] bytes = baos.toByteArray();
	                 fout.write(bytes);             
	                 baos.reset();
	             }
	             fout.flush();
	             fout.close();               
	             zis.closeEntry();
	         }
	         zis.close();
	     } 
	     catch(IOException e)
	     {
	         e.printStackTrace();
	         return false;
	     }

	    return true;
	}

	public static boolean isFileCorrect(String fileName,long crc32) throws Exception{return isFileCorrect(new File(fileName),crc32);}
	public static boolean isFileCorrect(File file,long crc32) throws Exception {
		long startTime=System.currentTimeMillis();
		Checksum checksum = new CRC32();
		InputStream fis =  new FileInputStream(file);
		byte[] buffer = new byte[16384];
		int readLen=-1;

		while((readLen = fis.read(buffer))!=-1)
			checksum.update(buffer,0,readLen);
		fis.close();
	       
		long sum = checksum.getValue();
		boolean isCorrect=(crc32==sum);
		int spend=(int) (System.currentTimeMillis()-startTime);
		Log.d("Util","CRC Check result: "+((isCorrect)?"Correct!":"Incorrect!")+", ( Sum="+sum+", record="+crc32+"), length: "+file.length()+", spend time: "+spend+"ms, File path: "+file.getAbsolutePath());
		return isCorrect;
	}
	
	public static SubtitleElement[] loadSubtitle(File file) {
		ArrayList<SubtitleElement> subtitleList = new ArrayList<SubtitleElement>();
		try {
			System.out.println("Open " + file.getAbsolutePath()
					+ " for read subtitle.");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String stemp;
			int lineCounter = 0;
			int step = 0; // 0: Find the serial number, 1: Get the serial
							// number, 2: Get the time description, 3: Get
							// Subtitle
			int serial = 0;
			SubtitleElement se = null;

			while ((stemp = br.readLine()) != null) {
				lineCounter++;

				// This may find the serial number
				if (step == 0) {
					if (stemp.matches("[0-9]+")) {
						// System.out.println("Find a subtitle start: "+stemp);
						se = new SubtitleElement();
						serial = Integer.parseInt(stemp);
						step = 1;
					}
				}

				// This may find the time description
				else if (step == 1) {
					if (stemp
							.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")) {
						// System.out.println("Get time string: "+stemp);
						int startTimeMs;
						String ts = stemp.substring(0, 2);
						// System.out.println("Hour: "+ts);
						startTimeMs = Integer.parseInt(ts) * 3600000;
						ts = stemp.substring(3, 5);
						// System.out.println("Min: "+ts);
						startTimeMs += Integer.parseInt(ts) * 60000;
						ts = stemp.substring(6, 8);
						// System.out.println("Sec: "+ts);
						startTimeMs += Integer.parseInt(ts) * 1000;
						ts = stemp.substring(9, 12);
						// System.out.println("Sub: "+ts);
						startTimeMs += Integer.parseInt(ts);
						// System.out.println("Set time: "+startTimeMs);
						se.startTimeMs = startTimeMs;
						step = 2;
					} else {
						// System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
						step = 0;
					}
				} else if (step == 2) {
					se.text = stemp;
					step = 0;
					subtitleList.add(se);
					System.out.println("get Subtitle: " + stemp);
					if (stemp.length() == 0)
						System.err
								.println("Load Subtitle: Warring: Get a Subtitle with no content at line "
										+ lineCounter);
				}
			}
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (SubtitleElement[]) subtitleList.toArray(new SubtitleElement[0]);
	}
}
