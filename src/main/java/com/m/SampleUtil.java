package com.m;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SampleUtil {
	private static final Log LOGGER = LogFactory.getLog(SampleUtil.class);
	
	public static void call2(String untrustedValue,ServletOutputStream out, HttpServletResponse response,HttpSession session) throws Exception{
		// 深度 2
		//--[defect16]Log Forging
		String token = untrustedValue;
		LOGGER.info(token);
	}
}
