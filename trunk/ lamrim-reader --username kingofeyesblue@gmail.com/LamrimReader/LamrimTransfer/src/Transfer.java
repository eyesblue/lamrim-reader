import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

/*
 * The format transfer for LamrimReader. change the variable "baseFileName" to point the directory of source directory,
 * and the variable "outputFileBase" for output directory. the package format of output only RESOURCE type of android,
 * I'll add the JSON format soon.
 * There should no new line(\n) in end of line for android, but that unfriendly for debug, you can control the output
 * format with variable "debug".  
 * */

public class Transfer {
	static boolean disableShow=false;
	static String pagesText=null;
	static int FORMAT_RESC=0;
	static int OUT_FORMAT=FORMAT_RESC;
	static boolean debug=false;
	static HashMap<String,String[]> hm=new HashMap<String,String[]>();
	
	public static void main(String args[]) throws Exception{
//		gson=new Gson();
		ArrayList<String> pages=new ArrayList<String>();
		String baseFileName="E:\\Documents\\Dropbox\\LamrimTheory\\UTF-8Page\\ptd-";
		String outputFileBase="E:\\Documents\\Dropbox\\LamrimTheory\\BookSec\\";
		String outputAll=new String(outputFileBase+"all.txt");
		FileWriter fwa=new FileWriter(outputFileBase+"all.txt");
		fwa.write("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n<string-array name=\"book\">\n");
		String stemp="";
		File f=null;
		
//		checkBlock(new File("E:\\Documents\\Dropbox\\LamrimTheory\\UTF-8Page\\ptd-01.htm"));

		for(int i=1;i<25;i++){
			lineCount=0;
			stemp=""+i;
			if(stemp.length()==1)stemp="0"+stemp;
			f=new File(baseFileName+stemp+".htm");
			System.out.println("Check: "+f.getAbsolutePath());
			checkBlock(f,pages);
			
			String output="";
			if(OUT_FORMAT==FORMAT_RESC){
				System.out.println("There are "+pages.size()+" pages in the chapter");
				output="<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n<string-array name=\"book\">\n";
				while(pages.size()>0){
					String s=pages.remove(0);
					output+="<item>\n<![CDATA["+s+"]]>\n</item>\n";
					fwa.write("<item>\n<![CDATA["+s+"]]>\n</item>\n");
				}
			}
			output+="</string-array>\n</resources>\n";
			System.out.println("There are "+pages.size()+" element in pages.");
			FileWriter fwc=new FileWriter(outputFileBase+"Chapter"+stemp+".txt");
			
			fwc.write(output);
			fwc.flush();
			fwc.close();
//			System.out.println("Pages: "+output);
		}
		fwa.write("</string-array>\n</resources>\n");
		fwa.flush();
		fwa.close();
		//pointsGson=gson.toJson(points.toArray(new String[0]));
	}
	
	static String cmdPrefix="";
	static String cmdPostfix="";
	public static void fontSizeCmd(){
//		System.out.println("Disable show!");
		cmdPrefix+="<s>";
		cmdPostfix="</s>"+cmdPostfix;
	}
	public static void textNumCmd(){
		cmdPrefix+="<n>";
		cmdPostfix="</n>"+cmdPostfix;
		disableShow=true;
	}
	public static void textBoldCmd(){
		cmdPrefix+="<b>";
		cmdPostfix="</b>"+cmdPostfix;
	}
	public static void unSetFontSizeCmd(){
		if(!cmdPrefix.endsWith("<s>"))System.out.println("Warring: There is a </s> tag hasn't <s> tag exist.");
		if(cmdPrefix.length()>3)cmdPrefix=cmdPrefix.substring(0, cmdPrefix.length()-3);
		else cmdPrefix="";
		if(cmdPostfix.length()>4)cmdPostfix=cmdPostfix.substring(4);
		else cmdPostfix="";
	}
	public static void unSetTextNumCmd(){
//		System.out.println("Disable show clear");
		if(!cmdPrefix.endsWith("<n>"))System.out.println("Warring: There is a </n> tag hasn't <n> tag exist.");
		if(cmdPrefix.length()>3)cmdPrefix=cmdPrefix.substring(0, cmdPrefix.length()-3);
		else cmdPrefix="";
		if(cmdPostfix.length()>4)cmdPostfix=cmdPostfix.substring(4);
		else cmdPostfix="";
		disableShow=false;
	}
	public static void unSetTextBoldCmd(){
		if(!cmdPrefix.endsWith("<b>"))System.out.println("Warring: There is a </b> tag hasn't <b> tag exist.");
		if(cmdPrefix.length()>3)cmdPrefix=cmdPrefix.substring(0, cmdPrefix.length()-3);
		else cmdPrefix="";
		if(cmdPostfix.length()>4)cmdPostfix=cmdPostfix.substring(4);
		else cmdPostfix="";
	}
	
	private static String getPrefixTag(){
		return cmdPrefix;
	}
	
	private static String getPostfixTag(){
		return cmdPostfix;
	}
	
	
	public static void checkBlock(File f,ArrayList<String> pages) throws Exception{
		StringBuffer sb=new StringBuffer();
		BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(f)));
		
//		ArrayList<int[][]> points = new ArrayList<int[][]>();
		
		int wordCount=0;
		
		int level=0;
		char c=0;
		boolean tagStart=false;
		String stemp="";
		
		int itemp=-1;
		
		while((itemp= br.read())!=-1){
			c=(char)itemp;
			sb.append(c);
			if(checkTag(c,br,sb))continue;
			if(disableShow){continue;}
			if(!wordFilter(c))continue;
			
				if(c=='\n'){
					if(wordCount!=0)lineCount++;
					if(checkPage(sb,pages))sb=new StringBuffer();
					// Check the last word is the "</s></b>"
					br.mark(512000);
					stemp=br.readLine();
					if(stemp!=null)
					if(stemp.lastIndexOf("</s></b>")==-1){
						sb.append("　　");
						wordCount=2;
					}
					else wordCount=0;
					
					br.reset();
					
				}
				else if(c=='.')continue;
/*				else if(wordCount==35){
					
					
//					System.out.println(stemp);
//					stemp=""+c;
					sb.append('\n');
//					sb.insert(sb.length()-1, '\n');
					wordCount=1;
					lineCount++;
					checkPage();
					
				}
*/				else{
					wordCount++;
					if(wordCount==35){
						br.mark(10);
						if((itemp=br.read())==-1){br.reset();continue;}
						br.reset();
						char ctemp=(char)itemp;
						if(ctemp=='.'){
							sb.append((char)br.read());
						}
						if(ctemp!='\n')sb.append('\n');
						wordCount=0;
						lineCount++;
						if(checkPage(sb,pages))sb=new StringBuffer();
					}
				}
		}
		//System.out.println(sb.toString());
		prepareOutput(sb,pages);
	}
	
	private static boolean checkTag(char c, BufferedReader br,StringBuffer sb) throws IOException{
		String command="";
		
		if(c!='<')return false;
		
			while((c=(char) br.read())!=-1){
				sb.append(c);
				if(c!='>')
					command+=c;
				else{
//					System.out.println("Command: "+command);
					if(command.startsWith("/")){
						if(command.startsWith("/n")){unSetTextNumCmd();command="";}
						if(command.startsWith("/b")){unSetTextBoldCmd();command="";}
						if(command.startsWith("/s")){unSetFontSizeCmd();command="";}
					}
					else if(command.startsWith("n")){
//						String s=command.substring(command.indexOf("\"")+2,command.lastIndexOf("\""));
//						fontSizeCmd(new Integer(Integer.parseInt(s)));
						textNumCmd();
						command="";
					}
					else if(command.startsWith("s")){
//						String s=command.substring(command.indexOf("\"")+2,command.lastIndexOf("\""));
//						fontSizeCmd(new Integer(Integer.parseInt(s,16)));
						fontSizeCmd();
						command="";
					}
					else if(command.startsWith("b")){
//						String s=command.substring(command.indexOf("\""),command.lastIndexOf("\""));
						textBoldCmd();
						command="";
					}
					break;
				}
			}
			return true;
	}
	
	static int lineCount=0;

	private static boolean checkPage(StringBuffer sb,ArrayList<String> pages) throws IOException{
		
		
		if(lineCount==13){
//			System.out.println("Line count="+lineCount);
			lineCount=0;
//			char c=sb.charAt(sb.length()-1);
//			sb.deleteCharAt(sb.length()-1);
//			sb.append('\n');
			
			prepareOutput(sb,pages);
//			System.out.println(sb.toString());
			return true;
//			sb.append(c);
		}
		return false;
	}
	private static boolean wordFilter(char w){
		final char sample[]={'，','。','、','：','《','》','\r'};
		for(char c:sample){
			
			if(w==c){
//				System.out.println("Match word: "+w);
				return false;
			}
		}
		
		return true;
	}

	static String lastPageTag="";
	private static void prepareOutput(StringBuffer sb,ArrayList<String> pages) throws IOException{
		BufferedReader br=new BufferedReader(new StringReader(sb.toString()));
		StringBuffer line=new StringBuffer();
//		ArrayList<String> pop=new ArrayList<String>();
		String stemp=null;
		int[][] ia;
		int itemp=-1;
		char c=0;
		int lineCounter=0;
		String pageContent=""+lastPageTag;
		lastPageTag="";
		
		while((stemp=br.readLine())!=null){
			
//			if(OUT_TEXT==HTML_TEXT)pageContent+=getLineContent(stemp);
			pageContent+=getLineContent(stemp);
			stemp=stemp.replaceAll("<b>", "");
			stemp=stemp.replaceAll("</b>", "");
			stemp=stemp.replaceAll("<s>", "");
			stemp=stemp.replaceAll("</s>", "");
			stemp=stemp.replaceAll("<n>", "");
			stemp=stemp.replaceAll("</n>", "");
//			System.out.println("Process Line: "+stemp);
//			if(OUT_TEXT==PURE_TEXT)pageContent+=getPoints(stemp,pop,lineCounter++);
		}
		
		// remove last \n or \\n\n
		if(pageContent.endsWith("\\n\n"))
			pageContent=pageContent.substring(0, pageContent.length()-3);
		else if(pageContent.endsWith("\\n"))
			pageContent=pageContent.substring(0, pageContent.length()-2);
		
		stemp=getPostfixTag();
		System.out.println("Last postfix tag: "+stemp);
		if(stemp.length()>0){
			
			pageContent+=stemp;
			lastPageTag=getPrefixTag();
		}

		pages.add(pageContent);
		System.out.println("Add to page content: ("+pages.size()+"), "+pageContent);
	}
	
	private static String getLineContent(String line){
		String ret="";
		
		for(int i=0;i<line.length();i++){
			char c=line.charAt(i);
			if(wordFilter(c))
				ret+=c;
		}
		if(debug)return ret+"\\n\n";
		else return ret+"\\n";
	}
}
