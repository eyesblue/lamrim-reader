package eyes.blue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

public class TestStreamingPlayer {
	Context context=null;
	String fileName[]=null;
	int fileSize[]=null;
	int targetFile=0;
	DecodeThread player=null;
	String logTag=null;
	SharedPreferences options;
	
	public TestStreamingPlayer(Context context,int index){
		this.fileName=context.getResources().getStringArray(R.array.fileName);
		this.fileSize=context.getResources().getIntArray(R.array.fileSize);
		this.context=context;
		this.targetFile=index;
		logTag=context.getString(R.string.app_name);
		options = context.getSharedPreferences(context.getString(R.string.optionFile), 0);
	}
	
	 public void play() throws IOException, InterruptedException{
		URL url=new URL("https://sites.google.com/a/eyes-blue.com/lamrimreader/appresources/"+fileName[targetFile]);
		InputStream is=url.openStream();
		DownloadThread dt=new DownloadThread(context,is,targetFile);
		dt.start();
	}
	 


	 class DownloadThread extends StopableThread{
			Context context=null;
			InputStream is=null;
			int targetFileIndex=-1;
//			DownloadFinishListener downloadFinishListener=null;
			
//			public DownloadThread(InputStream is,String fileName,int fileSize,PipedOutputStream pos,DownloadFinishListener downloadFinishListener){
			public DownloadThread(Context context,InputStream is,int index){
				this.context=context;
				this.is=is;
				this.targetFileIndex=index;
			}
			
			public DownloadThread(Context context,int index){
				this.context=context;
				this.targetFileIndex=index;
			}
			
			public void run(){
				if(is==null){
					Log.e(logTag,"At the debug phase, you shouldn't initial the DownloadThread with downloadThread(Context context,int index), please initial with DownloadThread(Context context,InputStream is,int index)");
					return;
				}
				FileSysManager.checkFileStructure();
				FileOutputStream fos=null;
				try {
					fos=new FileOutputStream(FileSysManager.getLocalMediaFile(targetFileIndex));
				} catch (FileNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				
				Log.d(logTag, "Save file to "+FileSysManager.getLocalMediaFile(targetFileIndex));
				// While open file fail, do something here.
				// if(fos==null) ...

				byte[] inetBuffer=new byte[context.getResources().getInteger(R.integer.downloadBufferSize)];
				byte[] decoderBuffer=new byte[context.getResources().getIntArray(R.array.fileSize)[targetFileIndex]];
				PipedInputStream pis=new PipedInputStream();
				PipedOutputStream pos=new PipedOutputStream();
				try {
					pis.connect(pos);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					Log.d(logTag,"IOException happen while initial phase form connect two piped stream");
					e1.printStackTrace();
				}
					
				player=new DecodeThread(pis);
				player.start();
				
				
				Log.d("LamrimReader","Create "+inetBuffer.length+" for download cache, fos="+fos);
				try {
					int counter=0;
					int itemp=-1;
					while((itemp=is.read(inetBuffer))!=-1){
//						Log.d("LamrimReader", "Content: "+new String(buffer));
							
						fos.write(inetBuffer);
						pos.write(inetBuffer);
						counter+=itemp;
//						pos.flush();
						if(stopThread){
							// Do something that been interrupt!
							break;
						}
					}
					
					player.stopThread();
					Log.d("LamrimReader", "Leave the download thread");
					is.close();
					fos.flush();
					fos.close();
				}catch (IOException e) {
				// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
	 }
	 
	 class PipedByteArrayInputStream extends ByteArrayInputStream{
		 public PipedByteArrayInputStream(byte[] buffer){
			 super(buffer);
			 super.count=0;
		 }
		 public PipedByteArrayInputStream(byte[] buffer, int offset, int length){
			 super(buffer,offset,length);
		 
		 super.count = 0;
		 }
		
		public synchronized void append(byte[] buf, int offset, int length){
			System.arraycopy(super.buf, super.mark+super.pos, buf, 0, length);
			super.count+=length;
		}
	 }
	 
	 
	 class DecodeThread extends StopableThread{
		 InputStream is=null;
		 int bufferSec=15;
		 public DecodeThread(InputStream is){
			 this.is=is;
		 }
      
		 public void run(){
			 Bitstream bitstream = new Bitstream(is);
			 Decoder decoder=new Decoder();
			 Header h=null;
			 SampleBuffer output=null;
			 
			 try {
				 synchronized(is){
				 	if(is.available()<1)
					is.wait();
				 }
				} catch (InterruptedException e3) {
					// TODO Auto-generated catch block
					e3.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
			 try {
				if(is.available()<1)
					try {
						is.wait();
					} catch (InterruptedException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
			}
			try {
				h = bitstream.readFrame();
				if(h==null)Log.d(logTag,"The header is null");
				output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
			} catch (BitstreamException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 
			 int freq=output.getSampleFrequency();
			 int channelMode=h.mode();
			 switch(channelMode){
			 case Header.SINGLE_CHANNEL:channelMode=AudioFormat.CHANNEL_CONFIGURATION_MONO;break;
			 case Header.DUAL_CHANNEL:channelMode=AudioFormat.CHANNEL_CONFIGURATION_STEREO;break;
			 }
			 int minWriteSize=AudioTrack.getMinBufferSize(freq, channelMode, AudioFormat.ENCODING_PCM_16BIT)*2;
			 int bufSize=(bufferSec)*freq;
			 bufSize=bufSize%minWriteSize;
			 ArrayList<short[]> fullList=new ArrayList<short[]>();
			 ArrayList<short[]> nullList=new ArrayList<short[]>();
			 
			 short[][] b=new short[bufSize][minWriteSize];
			 for(short ba[]:b)nullList.add(ba);
/*			 for(int i=0;i<bufSize;i++){
				 nullList.add(new short[minWriteSize]);
			 }
*/			 
			 int bufFree=nullList.get(0).length-output.getBufferLength();
			 System.arraycopy(output.getBuffer(), 0, nullList.get(0), 0, output.getBufferLength());
			 int itemp=0;
			 int frameCounter=1;
			 
			 AudioTrack audioTrack=null;
			 try{
				 audioTrack=new AudioTrack(AudioManager.STREAM_MUSIC, freq, channelMode,AudioFormat.ENCODING_PCM_16BIT, minWriteSize, AudioTrack.MODE_STREAM);
			 }catch(IllegalArgumentException iae){iae.printStackTrace();}

			 FeedThread ft=new FeedThread(audioTrack,nullList,fullList);
//			 ft.setPriority(Thread.MAX_PRIORITY);
			 ft.start();

			 try {
				 short[] buf=null;
				 synchronized(nullList){
						buf=nullList.remove(0);
						Log.d(logTag,"Get a free buf from nullList, remaining "+nullList.size()+" buffer in nullList.");
				}
				while(!stopThread&&(h = bitstream.readFrame())!=null){
					output = (SampleBuffer)decoder.decodeFrame(h, bitstream);
					
					frameCounter++;
//					if(frameCounter==3)return;
					Log.d(logTag,"Read "+frameCounter+"th frame.");

					while(nullList.size()==0)Thread.sleep(1000);
/*					if(nullList.size()==0)
						try {
							Log.d(logTag,"No free List for keep frame buffer, wait for notify.");
							synchronized(ft){ft.wait();}
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
*/					
					

					itemp=output.getBufferLength();
					if(bufFree>=itemp){
						Log.d(logTag,"The remaining space of buffer begger then audio frame, write it directory.");
						Log.d(logTag,"Copy data to buffer offset="+(buf.length-bufFree)+", length="+itemp+", buffer free="+(bufFree-itemp));
						 System.arraycopy(output.getBuffer(), 0, buf, buf.length-bufFree, itemp);
						 bufFree-=itemp;
		//				 synchronized(fullList){Log.d(logTag,"Add the buffer to fullList and weakup the FeedThread");fullList.add(buf);fullList.notify();}
						 
						 if(bufFree==0){
							 fullList.add(buf);
//							 while(nullList.size()==0){Log.d(logTag,"No free buffer in nullList, going sleep and wait for FeedThread weakup.");synchronized(ft){ft.wait();}}
							 while(nullList.size()==0)Thread.sleep(1000);
//							 synchronized(nullList){buf=nullList.remove(0);Log.d(logTag,"Get a new buffer for next decode from nullList, remaining "+nullList.size()+" buffers.");}
							 buf=nullList.remove(0);
							 bufFree=minWriteSize;
						 }
					}
					 else{ // itemp > bufFree
						 Log.d(logTag,"The space of the buffer is not enough for keep data, remaining "+bufFree+".");
						 Log.d(logTag,"Write wave to buffer from bufferIndex="+(buf.length-bufFree)+", Length="+bufFree+", remaining data="+(itemp-bufFree));
						 System.arraycopy(output.getBuffer(), 0, buf, buf.length-bufFree, bufFree);
						 itemp-=bufFree;
//						 synchronized(ft){Log.d(logTag,"Write the fill buffer to fullList, and notify FeedThread.");fullList.add(buf);ft.notify();}
						 fullList.add(buf);
//						 while(nullList.size()==0){Log.d(logTag,"No free buffer in nullList, going sleep and wait for FeedThread weakup.");synchronized(ft){ft.wait();}}
						 while(nullList.size()==0)Thread.sleep(1000);
						 //synchronized(nullList){buf=nullList.remove(0);Log.d(logTag,"Get a new buffer for next decode from nullList, remaining "+nullList.size()+" buffers.");}
						 buf=nullList.remove(0);
						 Log.d(logTag,"Write remaining data to new buffer, length: "+itemp);
						 System.arraycopy(output.getBuffer(), 0, buf, 0, itemp);
						 
						 bufFree=minWriteSize-itemp;
					 }
//					System.arraycopy(output.getBuffer(), 0, buffer, writeIndex, bond);
					 bitstream.closeFrame();
				}
				ft.stopThread();
				ft=null;
				b=null;
				fullList=null;
				nullList=null;
				bitstream=null;
				decoder=null;
				audioTrack=null;
				h=null;
				output=null;
				
				release();
			} catch (BitstreamException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			} catch (DecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
          }
		 
		 public void release(){
			 is=null;
		 }
   }



	 
	 class FeedThread extends StopableThread{
		 ArrayList<short[]> nullList=null,fullList=null;
		 AudioTrack track=null;

		 
		 public FeedThread(AudioTrack track,ArrayList<short[]> nullList,ArrayList<short[]> fullList){
			 this.nullList=nullList;
			 this.fullList=fullList;
			 this.track=track;
		 }
		 
		 public void run(){
			 while(!super.stopThread){
				 short[] buf=null;
				 
				 
//					 if(fullList.size()==0)try {synchronized(this){	this.wait();} } catch (InterruptedException e) {	e.printStackTrace();	}
				 while(fullList.size()==0)
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					 buf=fullList.remove(0);
					 Log.d(logTag,"Get a buffer from fullList, remain "+fullList.size());
				 

/*				 String test="";
				 for(short s:buf)
					 test+=" "+s;
						 Log.d(logTag,"Content: "+test);
						 test="";
*/						 
				 int writeStat=track.write(buf, 0, buf.length);
//				 Log.d(logTag,"Write "+writeStat+" data to track.");
//				 synchronized(nullList){nullList.add(buf);/*Log.d(logTag,"add the buffer to null list, now have "+nullList.size());*/}
				 nullList.add(buf);
//				 if(nullList.size()==1)synchronized(this){Log.d(logTag,"Notify the decode thread weak up.");this.notify();}

				 if(writeStat!=buf.length)Log.d(logTag,"Warring: the size of success write to track not equal minSizeWrite "+buf.length);
				 Log.d(logTag,"Play sound");
				 track.play();
			 } 
		 }
		 
		 public void release(){
			 nullList=null;
			 fullList=null;
			 track=null;
		 }
	 }

}
