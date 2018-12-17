package com.dddog.util;

import java.util.logging.Level;
import java.util.logging.Logger;

public class DDLog {

	private static final boolean DEBUG = true;
	private static Logger mLogger = Logger.getLogger(DDLog.class.getSimpleName());
	
	public static void i(String msg) {
		mLogger.setLevel(Level.FINEST);
		mLogger.info(msg);
	}
	
	public static void w(String msg) {
		mLogger.setLevel(Level.WARNING);
		mLogger.info(msg);
	}
	
	public static void e(String msg) {
		mLogger.setLevel(Level.SEVERE);
		mLogger.info(msg);
	}
}
