package eyes.blue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;

public class GoogleRemoteSource extends RemoteSource{
	Context context=null;
	final static String baseURL="https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/";
	final static String mediaSubName="MP3";
	final static String subtitleSubName="SRT";
	final static String theorySubName="TXT";
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
	public String getMediaFileAddress(int i){return baseURL+audioDirName+"/"+context.getResources().getStringArray(R.array.fileName)[i]+"."+mediaSubName;}
	@Override
	public String getSubtitleFileAddress(int i){return baseURL+subtitleDirName+"/"+context.getResources().getStringArray(R.array.fileName)[i]+"."+subtitleSubName;}
	@Override
	public String getTheoryFileAddress(int i){return baseURL+theoryDirName+"/"+context.getResources().getStringArray(R.array.fileName)[i]+"."+theorySubName;}
	@Override
	public String getName(){return "Google";}
}
