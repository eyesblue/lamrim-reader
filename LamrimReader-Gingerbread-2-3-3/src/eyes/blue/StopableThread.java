package eyes.blue;

public class StopableThread extends Thread {
	boolean stopThread=false;
	public void stopThread(){
		stopThread=false;
	}
}
