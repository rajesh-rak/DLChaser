package org.iiitb.dlpscanner.vo;

import java.util.Collection;

public class NodeInfo {

	private String predictionFile;
	private long fileLineNo;
	private int nhDepth = 0;
	private int nhDensity = 0;
	private Collection <String> classNames;
	
	public String getPredictionFile() {
		return predictionFile;
	}
	public void setPredictionFile(String predictionFile) {
		this.predictionFile = predictionFile;
	}
	public long getFileLineNo() {
		return fileLineNo;
	}
	public void setFileLineNo(long fileLineNo) {
		this.fileLineNo = fileLineNo;
	}
	public int getNhDepth() {
		return nhDepth;
	}
	public void setNhDepth(int nhDepth) {
		this.nhDepth = nhDepth;
	}
	public int getNhDensity() {
		if (classNames!= null) {
			nhDensity = classNames.size();
		}
		return nhDensity;
	}
	public Collection<String> getClassNames() {
		return classNames;
	}
	public void setClassNames(Collection<String> classNames) {
		this.classNames = classNames;
	}
}
