package eyes.blue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;



/**
 * MediaPlayer does not yet support streaming from external URLs so this class provides a pseudo-streaming function
 * by downloading the content incrementally & playing as soon as we get enough audio in our temporary storage.
 */
public class StreamingMp3Player {
	Decoder decoder=null;
	AudioTrack audioTrack=null;
	File mp3File;
	File tempFile;
	URL url;
	Context context;
	byte[] buffer;
	
	public StreamingMp3Player(Context context,URL url){
		this.context=context;
		this.url=url;
		buffer=new byte[100*1024];
	}
	
	public InputStream downloadFile() throws IOException, InterruptedException{
		InputStream is=url.openConnection().getInputStream();
		tempFile=new File(context.getCacheDir(),"downloadingMedia_" + ".dat");
		DownloadThread dt=new DownloadThread(is,tempFile,4000,buffer.length);
		dt.start();
		
		synchronized(dt){
			dt.wait(8000);
		}
		return new FileInputStream(tempFile);
	}
	
	public void play() throws IOException, InterruptedException{
		decoder=new Decoder();
		
//		progBar.setString("Decode the media file ...");
		
		Thread playThread=new Thread(){
			Bitstream bitstream;
			
			public void run(){
				InputStream is=null;
				try {
					//is=downloadFile();
					is=url.openConnection().getInputStream();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				} 
				
				audioTrack=new AudioTrack(AudioManager.STREAM_MUSIC, 11025, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT, 4000, AudioTrack.MODE_STATIC);
				int iMinBufferSize=AudioTrack.getMinBufferSize(11025, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		
				Log.d("LamrimReader","Min buffer size of AudioTracker: "+iMinBufferSize);
				try {
			//				bitstream = new Bitstream(downloadFile(url));
					bitstream = new Bitstream(is);

					Header h;
					String stemp="";

					while((h = bitstream.readFrame())!=null){
			// sample buffer set when decoder constructed
						SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
						for(int i=0;i<output.getBufferLength();i++)
							stemp+=output.getBuffer()[i]+", ";
						int len=audioTrack.write(output.getBuffer(), 0, output.getBufferLength());
						Log.d("LamrimReader","Write "+len+" of Data"+stemp);
						stemp="";
						audioTrack.play();
						bitstream.closeFrame();
					}
		}
		 catch (BitstreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		} catch (DecoderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}}};
		playThread.start();
	}

	class DownloadThread extends Thread{
		boolean start=true;
		byte buffer[]=null;
		InputStream is=null;
		File output=null;
		int notifyLen=-1;
		
		public DownloadThread(InputStream is,File file,int bufferLen,int notifyLen){
			this.is=is;
			this.buffer=new byte[bufferLen];
			this.output=file;
			this.notifyLen=notifyLen;
		}
		
		public void run(){
			FileOutputStream fos;
			
			try {
				fos = new FileOutputStream(output);
				int counter=0;
				int itemp=-1;
				boolean notified=false;
				
				while((itemp=is.read(buffer))!=-1){
//					Log.d("LamrimReader", "Content: "+new String(buffer));
					counter+=itemp;
					fos.write(buffer);
//					Log.d("LamrimReader", "Saved file length: "+counter);
					if(!start){
						// Do something that been interrupt!
						break;
					}
					if(!notified&&counter>=notifyLen){
						Log.d("LamrimReader", "Notify the initial length has reached: "+notifyLen);
						synchronized(this){
							notify();
						}
						notified=true;
						}
					}
				Log.d("LamrimReader", "Leave the download thread");
				is.close();
				fos.flush();
				fos.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}

		
		public void stopThread(){
			start=false;
		}
	}
	
	class BufferObject{
		private byte[] buffer=null;
		private int prodIndex=0;
		private int conIndex=0;
		int validLength=0;
		
		public int getValidLength(){return validLength;}
		public void writeLength(int length){validLength+=length;}
		public void cleanLength(int length){validLength-=length;}
		
		public int getReadBondIndex(){
			if(validLength==0)return -1;
			return (conIndex<prodIndex)? prodIndex:buffer.length-1;

		}
		public int getWriteBondIndex(){
			if(validLength==0)return -1;
			return (prodIndex>conIndex)?buffer.length-1:conIndex;
		}
	}
	
	class Producer extends StopableThread{
		int downloadID=-1;
		URL url=null;
		BufferObject bo=null;
		DownloadFailListener dfl=null;
		
		public Producer(URL url,BufferObject bo,int downloadID ){
			this.url=url;
			this.bo=bo;
			this.downloadID=downloadID;
		}
		
		public void setDownloadFailListener(DownloadFailListener dfl){
			this.dfl=dfl;
		}
		
		@Override
		public void run(){
			
			InputStream is=null;
			try {
				is=url.openStream();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(context.getString(R.string.app_name),this.getName()+" Thread: Connecting to "+url.toString()+" fail.");
				dfl.downloadMediaFail(downloadID);
				e.printStackTrace();
			}
			
			int bond=-1;
			int readLen=-1;
			int counter=0;
			try {
				while(true){
					synchronized(bo){
						// while producer index equal consumer index, it mean the data has been clear by consumer.
						while(true){
							if(super.stopThread)break;
							if(bo.prodIndex==bo.conIndex-1){bo.notify();bo.wait();}
							if(bo.prodIndex==bo.buffer.length-1)bo.prodIndex=0;
							if(bo.conIndex<=bo.prodIndex)bond=bo.buffer.length-1-bo.prodIndex;
							else bond=bo.conIndex-bo.prodIndex-1;
							if((readLen=is.read(bo.buffer,bo.prodIndex+1,bond))==-1)break;
							bo.prodIndex+=readLen;
							counter+=readLen;
						}
					}
					is.close();
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.e(context.getString(R.string.app_name),this.getName()+" Thread: I/O Exception happen while Download "+url.toString()+" at "+counter+" byte.");
				dfl.downloadMediaFail(downloadID);
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	class Cumsumer extends StopableThread{
		int downloadID=-1;
		URL url=null;
		BufferObject bo=null;
		DownloadFailListener dfl=null;
		
		public Cumsumer(URL url,BufferObject bo,int downloadID ){
			this.url=url;
			this.bo=bo;
			this.downloadID=downloadID;
		}
		
		public void setDownloadFailListener(DownloadFailListener dfl){
			this.dfl=dfl;
		}
		
		@Override
		public void run(){
			int bond=-1;
			int readLen=-1;
			int counter=0;
			
			while(true){
				synchronized(bo){
					if(bo.conIndex==bo.prodIndex){
						bo.notify();
						try {
							// Empty
							bo.wait();
						} catch (InterruptedException e) {e.printStackTrace();}
					}
					// Buffer bond reached.
					if(bo.conIndex==bo.buffer.length-1)bo.conIndex=0;
					
					if(bo.conIndex<bo.prodIndex)bond=bo.prodIndex-bo.conIndex;
					else bond=bo.buffer.length-1;
					// while producer index equal consumer index, it mean the data has been clear by consumer.
					while(true){
						if(super.stopThread)break;
						
						}
					}
				}
			
		}
	}
}

