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
	
	public void startRegionSeted(SubtitleElement subtitle){}
	public void startRegionDeset(SubtitleElement subtitle){}
	public void endRegionSeted(SubtitleElement subtitle){}
	public void endRegionDeset(SubtitleElement subtitle){}
	public void startRegionPlay(SubtitleElement startSubtitle,SubtitleElement endSubtitle){}
	public void stopRegionPlay(SubtitleElement startSubtitle,SubtitleElement endSubtitle){}
}
