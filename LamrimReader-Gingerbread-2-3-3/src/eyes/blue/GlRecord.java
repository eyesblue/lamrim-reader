package eyes.blue;

import android.util.Log;

public class GlRecord {
	String dateStart;
	String dateEnd;
	String speechPositionStart;
	String speechPositionEnd;
	String totalTime;
	String theoryLineStart;
	String theoryLineEnd;
	String subtitleLineStart;
	String subtitleLineEnd;
	String desc;
	
	public String toString(){
		return dateStart+", "+dateEnd+", "+speechPositionStart+", "+speechPositionEnd+", "+totalTime+", "+theoryLineStart+", "+theoryLineEnd+", "+subtitleLineStart+", "+subtitleLineEnd+", "+desc;
	}
	
	public String[] toStringArray(){
		String[] sa={dateStart,dateEnd,speechPositionStart,speechPositionEnd,totalTime,theoryLineStart,theoryLineEnd,subtitleLineStart,subtitleLineEnd,desc};
		return sa;
	}
	
	public static int[] getSpeechStrToInt(String str){
		if(str==null || str.length()==0)return null;
		String[] split=str.split(":");
		if(split.length!=3)return null;
		
		int res[]=new int[3];
		res[0]=SpeechData.getNameToId(split[0]);
		for(int i=1;i<res.length;i++)
			res[i]=(int) (Float.parseFloat(split[i]));
		
		Log.d("Global Lamrim record","Parse string ["+str+"] to time ["+res[1]+":"+res[2]+"]");
		return res;
	}
	
	public static int[] getTheoryStrToInt(String str){
		String[] split=str.split("-");
		int page=-1, line=-1;
		
		split[0]=split[0].replace("P", "").replace("p", "");
		page=Integer.parseInt(split[0])-1;
		
		split[1]=split[1].toUpperCase();
		if(split[1].startsWith("LL")){
			line=Integer.parseInt(split[1].replace("LL", ""));
			int contentLineCount=TheoryData.content[page].split("\n").length;
			System.out.println("Content lines of page "+page+" is "+contentLineCount);
			line=contentLineCount-line;
		}else{
			line=Integer.parseInt(split[1].replace("L", ""))-1;
		}
		
		int[] res={page,line};
		return res;
	}
	
}
