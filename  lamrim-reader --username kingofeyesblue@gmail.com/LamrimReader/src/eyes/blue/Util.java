package eyes.blue;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {

	public static boolean unZip( String zipname , String extractTo)
	{       
	     InputStream is;
	     ZipInputStream zis;
	     try 
	     {
	         is = new FileInputStream(extractTo + zipname);
	         zis = new ZipInputStream(new BufferedInputStream(is));          
	         ZipEntry ze;

	         while ((ze = zis.getNextEntry()) != null) 
	         {
	             ByteArrayOutputStream baos = new ByteArrayOutputStream();
	             byte[] buffer = new byte[1024];
	             int count;

	             // zapis do souboru
	             String filename = ze.getName();
	             FileOutputStream fout = new FileOutputStream(extractTo + filename);

	             // cteni zipu a zapis
	             while ((count = zis.read(buffer)) != -1) 
	             {
	                 baos.write(buffer, 0, count);
	                 byte[] bytes = baos.toByteArray();
	                 fout.write(bytes);             
	                 baos.reset();
	             }
	             fout.flush();
	             fout.close();               
	             zis.closeEntry();
	         }
	         zis.close();
	     } 
	     catch(IOException e)
	     {
	         e.printStackTrace();
	         return false;
	     }

	    return true;
	}

	
	public static byte[] getChecksum(String filename) throws Exception {
	       InputStream fis =  new FileInputStream(filename);

	       byte[] buffer = new byte[1024];
	       MessageDigest complete = MessageDigest.getInstance("MD5");
	       int numRead;

	       do {
	           numRead = fis.read(buffer);
	           if (numRead > 0) {
	               complete.update(buffer, 0, numRead);
	           }
	       } while (numRead != -1);

	       fis.close();
	       return complete.digest();
	   }
	
	public static SubtitleElement[] loadSubtitle(File file) {
		ArrayList<SubtitleElement> subtitleList = new ArrayList<SubtitleElement>();
		try {
			System.out.println("Open " + file.getAbsolutePath()
					+ " for read subtitle.");
			BufferedReader br = new BufferedReader(new FileReader(file));
			String stemp;
			int lineCounter = 0;
			int step = 0; // 0: Find the serial number, 1: Get the serial
							// number, 2: Get the time description, 3: Get
							// Subtitle
			int serial = 0;
			SubtitleElement se = null;

			while ((stemp = br.readLine()) != null) {
				lineCounter++;

				// This may find the serial number
				if (step == 0) {
					if (stemp.matches("[0-9]+")) {
						// System.out.println("Find a subtitle start: "+stemp);
						se = new SubtitleElement();
						serial = Integer.parseInt(stemp);
						step = 1;
					}
				}

				// This may find the time description
				else if (step == 1) {
					if (stemp
							.matches("[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3} +-+> +[0-9]{2}:[0-9]{2}:[0-9]{2},[0-9]{3}")) {
						// System.out.println("Get time string: "+stemp);
						int startTimeMs;
						String ts = stemp.substring(0, 2);
						// System.out.println("Hour: "+ts);
						startTimeMs = Integer.parseInt(ts) * 3600000;
						ts = stemp.substring(3, 5);
						// System.out.println("Min: "+ts);
						startTimeMs += Integer.parseInt(ts) * 60000;
						ts = stemp.substring(6, 8);
						// System.out.println("Sec: "+ts);
						startTimeMs += Integer.parseInt(ts) * 1000;
						ts = stemp.substring(9, 12);
						// System.out.println("Sub: "+ts);
						startTimeMs += Integer.parseInt(ts);
						// System.out.println("Set time: "+startTimeMs);
						se.startTimeMs = startTimeMs;
						step = 2;
					} else {
						// System.err.println("Find a bad format subtitle element at line "+lineCounter+": Serial: "+serial+", Time: "+stemp);
						step = 0;
					}
				} else if (step == 2) {
					se.text = stemp;
					step = 0;
					subtitleList.add(se);
					System.out.println("get Subtitle: " + stemp);
					if (stemp.length() == 0)
						System.err
								.println("Load Subtitle: Warring: Get a Subtitle with no content at line "
										+ lineCounter);
				}
			}

		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return (SubtitleElement[]) subtitleList.toArray(new SubtitleElement[0]);
	}
}
