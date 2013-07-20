package eyes.blue;

import java.io.File;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class BaseDialogs {
/* 更新: $Date$
* 作者: $Author$
* 版本: $Revision$
* ID  ：$Id$
*/
	public static void showDelWarnDialog(Context context, String target, String positiveBtnString, DialogInterface.OnClickListener positiveListener, String negativeBtnString, DialogInterface.OnClickListener negativeListener){
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(String.format(context.getString(R.string.dlgDelWarnTitle),target));
		builder.setMessage(String.format(context.getString(R.string.dlgDelWarnMsg),target));
		
		if(positiveBtnString == null)
			positiveBtnString=context.getString(R.string.dlgOk);
		
		builder.setPositiveButton(positiveBtnString, positiveListener);
		
		if(negativeListener==null)
			builder.setNegativeButton(context.getString(R.string.dlgCancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					dialog.cancel();
				}
			});
		else
			builder.setNegativeButton(negativeBtnString, negativeListener);
		
		builder.create().show();
	}
	
	//public static void showEditRegionDialog(final Activity activity,final int mediaIndex, final int startTimeMs, final int endTimeMs,final String titleStr,final SimpleAdapter adapter, final int recIndex){
	public static void showEditRegionDialog(final Activity activity,final int mediaIndex, final int startTimeMs, final int endTimeMs,final String titleStr, final String info, final int recIndex, final Runnable PositiveListener){
		LayoutInflater factory = (LayoutInflater)activity.getSystemService(activity.LAYOUT_INFLATER_SERVICE);
	    final View v = factory.inflate(R.layout.save_region_dialog, null);
	    final EditText regionTitle=(EditText) v.findViewById(R.id.regionTitle);
	    final TextView startTime=(TextView) v.findViewById(R.id.startTime);
	    final TextView endTime=(TextView) v.findViewById(R.id.endTime);
	    final String startHMS=Util.getMsToHMS(startTimeMs);
		final String endHMS=Util.getMsToHMS(endTimeMs);
		
		activity.runOnUiThread(new Runnable(){
			@Override
			public void run() {
				if(recIndex!=-1 && titleStr != null)
					regionTitle.setText(titleStr);
				startTime.setText(startHMS);
				endTime.setText(endHMS);
			}});
	    
	    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
	    builder.setTitle("請輸入此記錄名稱");
	    builder.setPositiveButton(activity.getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				String title=regionTitle.getText().toString().trim();
				if(title.length()==0){
					activity.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Toast.makeText(activity, activity.getString(R.string.inputTitleHint), Toast.LENGTH_LONG).show();
						}});
					return;
				}

				if(recIndex==-1)
					RegionRecord.addRegionRecord(activity, 0, regionTitle.getText().toString(), mediaIndex, startTimeMs, endTimeMs, info);
				else
					RegionRecord.updateRecord(activity, 0, regionTitle.getText().toString(), mediaIndex, startTimeMs, endTimeMs, recIndex);

				PositiveListener.run();
				dialog.dismiss();
			}});
	    builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}});
	    
	    AlertDialog setTextSizeDialog=builder.create();
	    setTextSizeDialog.setView(v);
	    setTextSizeDialog.setCanceledOnTouchOutside(false);
	    setTextSizeDialog.show();
	}
	
	public static void showToast(Context context,String msg){
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
		
	}
}
