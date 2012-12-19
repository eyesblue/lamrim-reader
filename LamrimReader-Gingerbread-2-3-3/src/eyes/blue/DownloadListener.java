package eyes.blue;

public class DownloadListener {
	public void downloadPreExec(int index,int resType){}
	public void setMaxProgress(long fileSize){}
	public void downloadFail(int index,int resType){}
	public void downloadFinish(int fileIndex,int resType){}
	public void prepareFinish(int fileIndex){};
	public void setDownloadProgress(int percent){}
	public void fileOperationFail(int i,int resType){}
}
