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
/* 更新: $Date: 2013-11-24 22:00:51 +0800 (Sun, 24 Nov 2013) $
* 作者: $Author: kingofeyesblue@gmail.com $
* 版本: $Revision: 83 $
* ID  ：$Id: BaseDialogs.java 83 2013-11-24 14:00:51Z kingofeyesblue@gmail.com $
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
	public static void showEditRegionDialog(final Activity activity,final int mediaIndex, final int startTimeMs, final int endTimeMs,final String titleStr, final String info, final int recIndex, final Runnable positiveListener){
		LayoutInflater factory = (LayoutInflater)activity.getSystemService(activity.LAYOUT_INFLATER_SERVICE);
	    final View v = factory.inflate(R.layout.save_region_dialog, null);
	    final EditText regionTitle=(EditText) v.findViewById(R.id.regionTitle);
	    final EditText regionTheoryPageNum=(EditText) v.findViewById(R.id.theoryPageNum);
	    final EditText regionTheoryStartLine=(EditText) v.findViewById(R.id.startLine);
	    final EditText regionTheoryStartEnd=(EditText) v.findViewById(R.id.endLine);
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
	    builder.setTitle("儲存區段");
	    builder.setPositiveButton(activity.getString(R.string.dlgOk), new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Check name can't be empty.
				String title=regionTitle.getText().toString().trim();
				if(title.length()==0){
					activity.runOnUiThread(new Runnable(){
						@Override
						public void run() {
							Toast.makeText(activity, activity.getString(R.string.inputTitleHint), Toast.LENGTH_LONG).show();
						}});
					return;
				}

				// Check Theory page, start line and end line.
				int theoryPageNum, inStartLine, inEndLine;
				try{
					theoryPageNum=Integer.parseInt(regionTheoryPageNum.getText().toString().trim());
					inStartLine=Integer.parseInt(regionTheoryStartLine.getText().toString().trim());
					inEndLine=Integer.parseInt(regionTheoryStartEnd.getText().toString().trim());
				}catch(NumberFormatException nfe){
					Util.showNarmalToastMsg(activity, activity.getString(R.string.dlgNumberFormatError));
					return;
				}
				
				if(recIndex==-1)
					RegionRecord.addRegionRecord(activity, 0, regionTitle.getText().toString(), mediaIndex, startTimeMs, endTimeMs, theoryPageNum, inStartLine, inEndLine, info);
				else
					RegionRecord.updateRecord(activity, 0, regionTitle.getText().toString(), mediaIndex, startTimeMs, endTimeMs, theoryPageNum, inStartLine, inEndLine, recIndex);

				if(positiveListener!=null)positiveListener.run();
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
