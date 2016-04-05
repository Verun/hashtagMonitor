/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

/**
 * Notifies clients that a tweet with hashtags they're interested in has arrived.
 * @author Verun
 *
 */
public interface ClientNotifier {

	/**
	 * Notify clients.
	 * @param pTweet
	 */
	void notifyClients(ProcessedTweet pTweet);
}
