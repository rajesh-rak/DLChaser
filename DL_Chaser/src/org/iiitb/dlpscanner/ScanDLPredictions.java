package org.iiitb.dlpscanner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.iiitb.dlpscanner.conf.Conf;
import org.iiitb.dlpscanner.vo.NodeInfo;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
/**
 * 
 */

/**
 * @author rajesh.rkumar
 *
 */
public class ScanDLPredictions {

	private static String confFolderPath;
	private static String folder;
	private static String filesWC;
	private static String exclusionsStr;
	private static String cycles;
	private static int nhDepth;
	private static int nhDensity;
	private static String cyclesTxt;
	private static String threadTxt;
	private static boolean dryRun;
	private static boolean excludeInnerClass;
	private static String outFolder;
	private static Collection <String> cyclesList = new ArrayList<String>();
	private static Collection <String> exclusionsList = new ArrayList<String>();
	private static long fileLineNo = 0;
	private static boolean verbose = false;
	private static long selectedNodeCount = 0;
	private static Collection<String> logLinesHeader = new ArrayList<String>();
	private static Collection<String> logLines = new ArrayList<String>();
	private static long runStartTime;
	private static long runEndTime;
	private static int totalFilesProcessed = 0;
	private static long totalNodesProcessed = 0;
	private static long totalNodesSelected = 0;

	/**
	 * The entire behavior of this program is controlled by dlpscanner.properties file,
	 * before running this program make sure the dlpscanner.properties is configured
	 * correctly
	 * 
	 * The main method can be called by passing the folder containing 
	 * dlpscanner.properties as parameter, if dlpscanner.properties is not
	 * available in the classpath.
	 * 
	 * If the dlpscanner.properties is available in classpath the program
	 * will pick that even if the folder path is specified
	 * 
	 * @param args folder containing dlpscanner.properties file
	 */
	public static void main(String[] args) {
		if (args.length > 0)
			confFolderPath = args[0];
		if (!Conf.init(confFolderPath)) {
			System.out.println("Error loading the dlpscanner.properties file"
					+ "\n\nTry running it by passing the correct location of"
					+ "\nthe folder path to the file as parameter"
					+ "\n\n ...program will terminate!");
		} else {
			execute();
		}	
	}
	
	private static void execute() {
		init();
		if (verbose) System.out.println("Folder: " + folder);
		if (verbose)System.out.println("filesWC: " + filesWC);
		if (verbose)System.out.println("exclusionsStr: " + exclusionsStr);
		if (verbose)System.out.println("cycles: " + cycles);
		if (verbose)System.out.println("nhZone: " + nhDepth);
		if (verbose)System.out.println("nhZone: " + cyclesTxt);
		if (verbose)System.out.println("dryRun: " + dryRun);
		if (verbose)System.out.println("cyclesList" + cyclesList.toString());
		if (verbose)System.out.println("exclusionsList" + exclusionsList.toString());
		try {
			prepare();
			runStartTime = 	System.currentTimeMillis();
			scanFiles();
			runEndTime = 	System.currentTimeMillis();
			finalizeLogs();
			IOUtils.writeLines(logLinesHeader, "\n", System.out);
		} catch (Exception e) {
			System.out.println("ERROR OCCURED: ");
			e.printStackTrace();
		}
	}
	
	private static void init () {
		folder = Conf.getValue("PREDICTION_RESULTS_FOLDER");
		filesWC = Conf.getValue("FILES");
		exclusionsStr = Conf.getValue("PACKAGE_EXCLUSIONS");
		cycles = Conf.getValue("CYCLES");
		nhDepth = Integer.parseInt(Conf.getValue("NEIGHBOURHOOD_DEPTH"));
		nhDensity = Integer.parseInt(Conf.getValue("NEIGHBOURHOOD_DENSITY"));
		outFolder = Conf.getValue("OUT_FOLDER");
		dryRun = Conf.getValue("DRY_RUN").equals("ON");
		excludeInnerClass = Conf.getValue("EXCLUDE_INNER_CLASS").equals("ON");
		outFolder = Conf.getValue("OUT_FOLDER");
		cyclesTxt = Conf.getValue("CYCLES_TEXT");
		threadTxt = Conf.getValue("THREAD_TEXT");
		verbose = Conf.getValue("VERBOSE").equals("ON");
		StringTokenizer st;
		st = new StringTokenizer(cycles,",");
		while (st.hasMoreTokens()) {
			cyclesList.add(cyclesTxt + st.nextToken());
		}
		st = new StringTokenizer(exclusionsStr,",");
		while (st.hasMoreTokens()) {
			exclusionsList.add(st.nextToken());
		}
		
		//Log Key params
		logLinesHeader.add("Execution Summary");
		logLinesHeader.add("-----------------");
		logLinesHeader.add("Source Folder:\t" + folder + "/" + filesWC);		
		logLinesHeader.add("Output Folder:\t" + outFolder);		
		logLinesHeader.add("Neighbourhood Depth:\t" + nhDepth);		
		logLinesHeader.add("Neighbourhood Density:\t" + nhDensity);		
	}
	
	private static void finalizeLogs() throws IOException {
		if (!dryRun) {
			//Prepare files
			File execLogFile = new File(outFolder + "/exec_log.txt");
			OutputStream opStream  = FileUtils.openOutputStream(execLogFile);
			opStream.close();
			logLinesHeader.add("Total Files Processed:\t" + totalFilesProcessed);
			logLinesHeader.add("Total Nodes Processed:\t" + totalNodesProcessed);
			logLinesHeader.add("Total Nodes Selected:\t" + totalNodesSelected);
			logLinesHeader.add("Total Processing Time:\t" + (runEndTime- runStartTime) + "ms");
			logLinesHeader.add("-------------------------------------");
			FileUtils.writeLines(execLogFile, logLinesHeader);
			FileUtils.writeLines(execLogFile, logLines, true);
		}	
	}

	private static void prepare() {
		DateFormat df = new SimpleDateFormat("yyyy_MM_dd_HHmmss");
		Date dt = new Date();
		outFolder = outFolder + "/" + "RUN_" + df.format(dt);
		System.out.println("O/P DIR NAME: " + outFolder);
	}

	private static void scanFiles() throws Exception {
		File[] files = getFilesList();
		if (files == null) return;
		totalFilesProcessed = files.length;
		for (int i = 0; i < files.length; i++) {
			File ipFile = files[i];
			long startTime = System.currentTimeMillis();
			fileLineNo = 0;
			//Logs
			logLines.add("File:\t\t\t" + ipFile.getName());
			System.out.println("File:\t\t\t" + ipFile.getName());
			//Processing File
			processFile(ipFile);
			long endTime = System.currentTimeMillis();
			long processedTime = endTime - startTime;
			//Logs
			logLines.add("Processed in: \t\t" + processedTime + "ms");
			System.out.println("Processed in: \t\t" + processedTime + "ms");
			System.out.println("------" );
			logLines.add("------" );
		}
	}
	
	private static void processFile(File ipFile) throws Exception {
		String ipFileName = ipFile.getName();
		BufferedReader fReader = new BufferedReader(new FileReader(ipFile)); 
		String fileLine;
		int nodeCount = 0;
		int filteredNodeCount = 0;
		String foundNodeName = "";
		NodeInfo nodeInfo = null;
		long foundLineNo = 0;
		while ((fileLine = fReader.readLine())!=null) {
			fileLineNo++;
			for (String str: cyclesList) {
				if (fileLine.contains("<" + str)) {
					nodeCount++;
					foundLineNo = fileLineNo;
					foundNodeName = str;
					//Process Found Node
					if (verbose)System.out.println("Node Found @ Line No: " + fileLineNo);
					nodeInfo = processNode(fReader, foundNodeName, fileLine);
					nodeInfo.setPredictionFile(ipFileName);
					nodeInfo.setFileLineNo(foundLineNo);
					//Check for Filtering and Write Files
					if (nodeInfo.getNhDensity() <= nhDensity
							&& nodeInfo.getNhDepth() <= nhDepth) {
						if (containsSubStr(nodeInfo.getClassNames(), exclusionsList)) {
							if (verbose)System.out.println("Node Excluded: " 
									+ nodeInfo.getPredictionFile()
									+ " Line No: "
									+ nodeInfo.getFileLineNo());
						} else {
							filteredNodeCount++;
							writeNodeToFile(nodeInfo);
						}	
					}
					break;
				}
			}
		}
		fReader.close();
		totalNodesProcessed = totalNodesProcessed + nodeCount;
		totalNodesSelected = totalNodesSelected + filteredNodeCount;
		logLines.add("Nodes Processed:\t" + nodeCount);
		logLines.add("Nodes Selected:\t\t" + filteredNodeCount);
		System.out.println("Nodes Processed:\t" + nodeCount 
						+ "\n" + "Nodes Selected:\t\t" + filteredNodeCount);
		
	}
	
	private static void writeNodeToFile(NodeInfo nodeInfo) throws IOException {
		//File Writing
		selectedNodeCount++;
		String classListFileName = outFolder + "/" + "Test" + selectedNodeCount + "/" + "classlist.txt";
		String nodeInfoFileName = outFolder + "/" + "Test" + selectedNodeCount + "/" + "NodeInfo.txt";
		Collection<String> nodeInfoStr = new ArrayList<String>();
		nodeInfoStr.add("File:\t\t" + nodeInfo.getPredictionFile());
		nodeInfoStr.add("Line No:\t" + nodeInfo.getFileLineNo());
		nodeInfoStr.add("NH Depth:\t" + nodeInfo.getNhDepth());
		nodeInfoStr.add("NH Density:\t" + nodeInfo.getNhDensity());
		nodeInfoStr.add("Classes\t: " + nodeInfo.getClassNames());
		if (!dryRun) {
			//Prepare files
			File classListFile = new File(classListFileName);
			File nodeInfoFile = new File(nodeInfoFileName);
			OutputStream opStream  = FileUtils.openOutputStream(classListFile);
			opStream.close();
			opStream  = FileUtils.openOutputStream(nodeInfoFile);
			opStream.close();
			System.out.println("File Created:\t\t" + classListFile.getAbsolutePath());
			//Prepare File Content
			//Write to Files
			FileUtils.writeLines(classListFile, nodeInfo.getClassNames());
			FileUtils.writeLines(nodeInfoFile, nodeInfoStr);
		} else {
			//write to console
			IOUtils.writeLines(nodeInfoStr, "\n", System.out);
			IOUtils.writeLines(nodeInfo.getClassNames(), "\n", System.out);
		}
	}
	
	private static NodeInfo processNode(BufferedReader fReader, String foundNodeName, String aFileLine) throws IOException {
		if (verbose)System.out.println("Processing Node: " + foundNodeName);
		String nodeTerminator = "</" + foundNodeName;
		String thNodeTxt = "<" + threadTxt;
		NodeInfo cNodeInfo = new NodeInfo();
		NodeInfo tNodeInfo = null;
		String fileLine = aFileLine;
		Collection<String> cClassList;
		Collection<String> tClassList;
		cNodeInfo.setClassNames(new HashSet<String>());
		
		do {
			
			if (fileLine.contains(thNodeTxt)) {
				tNodeInfo = processThreadNode(fReader, fileLine);
				//Add processed ThreadNode to the Cycles Node Info
				tClassList = tNodeInfo.getClassNames();
				cClassList = cNodeInfo.getClassNames();
				cClassList.addAll(tClassList);
				if (tNodeInfo.getNhDepth() > cNodeInfo.getNhDepth()) {
					cNodeInfo.setNhDepth(tNodeInfo.getNhDepth());
				}
			}
		} while (!(fileLine = fReader.readLine()).trim().contains(nodeTerminator) 
				&& !fileLine.contains("<" + cyclesTxt ) && fileLineNo++ != 0);
		
		return cNodeInfo;
	}
	
	private static NodeInfo processThreadNode(BufferedReader fReader, String aFileLine) throws IOException {
		if (verbose)System.out.println("Processing Thread Node");
		String thNodeTerminator = "</" + threadTxt;
		NodeInfo tNodeInfo = new NodeInfo();
		HashSet<String> classNames = new HashSet<String>();
		String fileLine = aFileLine;
	    Pattern pattern1 = Pattern.compile("[\\w+\\.\\$]+:");
		String matchedTxt;
		int callDepth = 0;
		do {
		    //Checking for Pattern-1
			Matcher mat = pattern1.matcher(fileLine);
		    while (mat.find()){
		      callDepth++;	
		      matchedTxt = mat.group();
		      matchedTxt = matchedTxt.substring(0, matchedTxt.lastIndexOf("."));
		      if (verbose) System.out.println("Matched Class: " + matchedTxt);
		      if (excludeInnerClass ) {
		    	  if (!matchedTxt.contains("$")) {
		    		  classNames.add(matchedTxt);
		    	  }
		      } else {
		    	  classNames.add(matchedTxt);
		      }
		    }
		} while (!(fileLine = fReader.readLine()).trim().contains(thNodeTerminator)
				&& !fileLine.contains("<" + threadTxt ) && fileLineNo++ != 0);
		tNodeInfo.setClassNames(classNames);
		tNodeInfo.setNhDepth(callDepth);
		return tNodeInfo;
	}

	private static File[] getFilesList() {
		File scanFolder = new File(folder);
		System.out.println("Folder Present: " + scanFolder.exists() + " - " + scanFolder.getAbsolutePath());
		if (scanFolder.exists()) {
			FileFilter fileFilter = new WildcardFileFilter(filesWC);
	     	return scanFolder.listFiles(fileFilter);
		} else {
			System.out.println("ERROR: Scan Folder Folder NOT Present"); 
			return null;
		}	
	}
	
	private static boolean containsSubStr(Collection<String> srcCollection, Collection<String> subStrCollection) {
		boolean result = false;
		Iterator<String> srcItr = srcCollection.iterator();
		while (srcItr.hasNext()) {
			if (result) break;
			String srcValue = srcItr.next();
			Iterator<String> subStrItr = subStrCollection.iterator();
			while (subStrItr.hasNext()) {
				result = srcValue.contains(subStrItr.next());
				if (result) break;
			}
		}
		return result;
	}
	
}
