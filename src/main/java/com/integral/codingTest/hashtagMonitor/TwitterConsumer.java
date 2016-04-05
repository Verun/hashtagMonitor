package com.integral.codingTest.hashtagMonitor;

/**
 * Consumes tweets.
 * @author Verun
 *
 */
public interface TwitterConsumer {
	
	/**
	 * Consume the tweet.
	 * @param tweet
	 */
	void consume(String tweet);
}
