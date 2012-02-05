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
	
	public InputStream downloadFile(URL url) throws IOException, InterruptedException{
		InputStream is=url.openConnection().getInputStream();
		tempFile=new File(context.getCacheDir(),"downloadingMedia_" + ".dat");
		DownloadThread dt=new DownloadThread(is,tempFile,4000,buffer.length);
		dt.start();
		
		synchronized(dt){
			dt.wait(8000);
		}
		return new FileInputStream(tempFile);
	}
	
	public void play(final String path) throws IOException, InterruptedException{
		decoder=new Decoder();
		
//		progBar.setString("Decode the media file ...");
		
		Thread playThread=new Thread(){
			Bitstream bitstream;
			int frameCount=0;
			int msPerFrame=-1;
			int waveFreq=-1;
			int waveCountPerFrame=-1;
			boolean initial=true;
			public void run(){
				URL url=null;
				InputStream is=null;
				try {
					url = new URL(path);
				} catch (MalformedURLException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				try {
					is=url.openConnection().getInputStream();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		audioTrack=new AudioTrack(AudioManager.STREAM_MUSIC, 11025, AudioFormat.CHANNEL_CONFIGURATION_MONO,AudioFormat.ENCODING_PCM_16BIT, 4000, AudioTrack.MODE_STATIC);
		int iMinBufferSize=AudioTrack.getMinBufferSize(11025, AudioFormat.CHANNEL_CONFIGURATION_MONO, AudioFormat.ENCODING_PCM_16BIT);
		audioTrack.play();
		
		
		Log.d("LamrimReader","Min buffer size of AudioTracker: "+iMinBufferSize);
		try {
			//				bitstream = new Bitstream(downloadFile(url));
			bitstream = new Bitstream(is);

			Header h;
			int wlen=0;

				while((h = bitstream.readFrame())!=null){
			// sample buffer set when decoder constructed
					frameCount++;
					SampleBuffer output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
					if(initial){
						msPerFrame=(int) (h.ms_per_frame());
						waveFreq=output.getSampleFrequency();
						waveCountPerFrame=output.getBufferLength();
						initial=false;
					}
					
//					wlen=audioTrack.write(output.getBuffer(), 0, output.getBufferLength());
//					Log.d("LamrimReader","Write "+wlen+" of buffer length"+output.getBufferLength());
					
//					System.out.println("SampleBuffer length: "+ output.getBufferLength()+" Channels: " +decoder.getOutputChannels()+" ,Freq: "+decoder.getOutputFrequency()+" ,sample freq:"+output.getSampleFrequency()+"Total ms: "+h.total_ms(output.getBufferLength())+" MS per frame: "+h.	ms_per_frame() );
					int offset=0;
					int writelen=0;
					while(offset < buffer.length)
					{
					writelen = audioTrack.write(buffer, offset, iMinBufferSize);
//					  if(writelen  > 0)
//						  audioTrack.play();
					  offset += writelen ;  
					  if(writelen < iMinBufferSize)
					     break;
					}
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
					Log.d("LamrimReader", "Saved file length: "+counter);
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
}

