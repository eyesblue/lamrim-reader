package eyes.blue;

import java.util.Map;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;

import android.app.Activity;

public class GaLogger {
	static EasyTracker easyTracker=null;
	
	public static void activityStart(Activity actvity){easyTracker=EasyTracker.getInstance(actvity);}
	public static void activityStop(Activity actvity){EasyTracker.getInstance(actvity).activityStop(actvity);}
	
	public static void sendEvent(String category, String action, String label, Long value){
		easyTracker.send(MapBuilder
			      .createEvent(category,     // Event category (required)
			    		  action,  // Event action (required)
			    		  label,   // Event label
			    		  value)            // Event value
			      .build()
			  );
	}
	
	public static void sendEvent(String category, String action, String label, int value){
		easyTracker.send(MapBuilder
			      .createEvent(category,     // Event category (required)
			    		  action,  // Event action (required)
			    		  label,   // Event label
			    		  (long)value)            // Event value
			      .build()
			  );
	}
	
	public static void send(Map<String, String> builder){easyTracker.send(builder);}
}
