package eyes.blue;

import java.io.File;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import android.net.http.AndroidHttpClient;
import android.util.Log;

/*
 * The class used to automatically load resource from File, Internet or any media.
 * */
public class ResourceLoader {
	static String logTag="LamrimReader";
	static String[] localFileSys=null;
	static String[] baseUrl=null;
	static AndroidHttpClient[] httpConn=null;
	
	public ResourceLoader(String[] localFileSys,String[] baseUrl){
		ResourceLoader.localFileSys=localFileSys;
		ResourceLoader.baseUrl=baseUrl;
	}
	
	private static File getLocation(String fileName){
		if(localFileSys==null||localFileSys.length==0)return null;
		
		File file=null;
		for(String dir:localFileSys){
			file=new File(dir+File.separator+fileName);
			if(file.exists())return file;
		}
		return null;
	}
	
	public static File checkURL(String fileName){
		if(baseUrl==null||baseUrl.length==0)return null;
		
		AndroidHttpClient c=AndroidHttpClient.newInstance("LamrimReader");
		HttpGet httpGet = new HttpGet (baseUrl[0]+fileName);
		try {
			HttpResponse response = c.execute(httpGet);
			Log.d(logTag,"Check file exist on remote: "+fileName+response.getStatusLine());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public static String[] LoadBookPage(int[] pages){return null;}
	public static SubtitleElement[] LoadSubtitle(int subtitleIndex){return null;}
	public static File loadSpeech(String fileName){
		// Find the file from file system
		File target=getLocation(fileName);
		if(target!=null)return target;
		
		return null;
	}
}
