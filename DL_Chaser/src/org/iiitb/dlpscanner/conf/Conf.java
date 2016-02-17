package org.iiitb.dlpscanner.conf;


import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;

public class Conf {
	
	private static String DLP_SCANNER_CONF = "dlpscanner";
	private static ResourceBundle rb = null;
		
	private static void initBundle() {
		if (rb == null) {
			rb = ResourceBundle.getBundle(DLP_SCANNER_CONF);
		}
	}
	
	public static boolean init(String folderPath) {
		boolean result = false;
		if (folderPath != null) {
			File file = new File (folderPath);
			try {
				URL[] fileUrls = {file.toURI().toURL()};
				ClassLoader clsLoader = new URLClassLoader(fileUrls);
				rb = ResourceBundle.getBundle(DLP_SCANNER_CONF,Locale.getDefault(),clsLoader);
				result = true;
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return result;
			}
		} else {
			//In case the folder is not provided for the resource bundle file
			//attempt load it from classpath
			initBundle();
			result = true;
		}
		return result;
	}
	
	public static String getValue(String propName){
		return rb.getString(propName);
	}

}
