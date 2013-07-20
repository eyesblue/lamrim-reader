package eyes.blue;

public class MediaPlayerControllerListener {
	/*
	 * Show subtitle in subtitle bar.
	 * */
	public void onSubtitleChanged(SubtitleElement subtitle){}
	
	/*
	 * Called while user seek.
	 * */
	public void onSeek(SubtitleElement subtitle){}
	
	/*
	 * Call while start play but first subtitle not coming.
	 * */
	public void startMoment(){}
	
	/*
	 * Call on media prepared.
	 * */
	public void onMediaPrepared(){}
	
	public void startRegionSeted(int position){}
	public void startRegionDeset(int position){}
	public void endRegionSeted(int position){}
	public void endRegionDeset(int position){}
	public void startRegionPlay(){}
	public void stopRegionPlay(){}
}
