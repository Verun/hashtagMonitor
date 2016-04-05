/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

/**
 * Subscribe to receive tweets.
 * @author Verun
 *
 */
public interface TwitterSubscriber {

	/**
	 * Subscribe to receive tweets using the given consumer.
	 * @param twitterConsumer
	 */
	void start(TwitterConsumer twitterConsumer);
	
	void stop();
}
