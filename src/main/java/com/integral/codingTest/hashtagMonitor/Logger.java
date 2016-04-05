/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

/**
 * Logger. Putting this in an interface for ease of testing.
 * @author Verun
 *
 */
public interface Logger {
	
	void logTrace(String msg);
	
	void logWarn(String msg);
	
	void logDebug(String msg);
	
	void logInfo(String msg);
	
	void logError(String msg);
}
