package com.m;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class MsgGrabber extends Thread {
	private final static Log LOGGER = LogFactory.getLog(MsgGrabber.class);
	
	private InputStream is;
	private StringBuffer result = new StringBuffer();
	
	private String encoding = "utf-8";
	
	private boolean run = true;
	
	public MsgGrabber(InputStream is){
		this.is = is;
	}
	
	public MsgGrabber(InputStream is,String encoding){
		this.is = is;
		this.encoding = encoding;
	}
	
	public void run() {
		try {
			InputStreamReader isr = new InputStreamReader(is,encoding);
			BufferedReader br = new BufferedReader(isr);
			String line = null;
			while (((line = br.readLine()) != null) && run){
				result.append(line).append("\n");
			}	
			//LOGGER.debug("MsgGrabber stop");
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	public void stopAction(){
		run = false;
	}
	
	public String getMsg(){
		return result.toString();
	}
}
