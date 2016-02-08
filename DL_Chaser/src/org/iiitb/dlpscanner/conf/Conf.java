package org.iiitb.dlpscanner.conf;


import java.util.ResourceBundle;

public class Conf {
	
	private static String DLP_SCANNER_CONF = "dlpscanner";
	private static ResourceBundle rb = null;
		
	private static void initBundle() {
		if (rb == null) {
			rb = ResourceBundle.getBundle(DLP_SCANNER_CONF);
		}
	}
	
	public static String getValue(String propName){
		initBundle();
		return rb.getString(propName);
	}

}
