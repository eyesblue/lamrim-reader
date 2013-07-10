package eyes.blue;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;

import android.content.Context;

public class GoogleRemoteSource extends RemoteSource{
	Context context=null;
	final static String baseURL="https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/";
//	final static String mediaSubName="MP3";
//	final static String subtitleSubName="SRT";
//	final static String theorySubName="TXT";
	String audioDirName=null;
	String subtitleDirName=null;
	String theoryDirName=null;
	
	public GoogleRemoteSource(Context context) {
		this.context=context;
		this.audioDirName=context.getResources().getString(R.string.audioDirName).toLowerCase();
		this.subtitleDirName=context.getResources().getString(R.string.subtitleDirName).toLowerCase();
		this.theoryDirName=context.getResources().getString(R.string.theoryDirName).toLowerCase();
	}
	
	@Override
	public String getMediaFileAddress(int i){
		String url=null;
		try {
			url = baseURL+audioDirName+"/"+URLEncoder.encode(SpeechData.name[i],"UTF-8");
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		return url;
	}
	@Override
	public String getSubtitleFileAddress(int i){
		//return baseURL+subtitleDirName+"/"+SpeechData.getNameId(i)+"."+subtitleSubName;
		String url=null;
		try {
			url = baseURL+subtitleDirName+"/"+URLEncoder.encode(SpeechData.getSubtitleName(i),"UTF-8")+"."+context.getResources().getString(R.string.defSubtitleType);
		} catch (UnsupportedEncodingException e) {e.printStackTrace();}
		return url;
	}
	@Override
	public String getTheoryFileAddress(int i){return baseURL+theoryDirName+"/"+SpeechData.getNameId(i)+"."+context.getResources().getString(R.string.defTheoryType);}
	@Override
	public String getName(){return "Google";}
}
