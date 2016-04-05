/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

import java.util.List;

/**
 * A tweet along with the client that should be notified of it.
 * @author Verun
 *
 */
public class ProcessedTweet {
	
	private final String tweet;
	private final List<String> clientsToNotify;
	
	ProcessedTweet(String tweet, List<String> clientsToNotify) {
		this.tweet = tweet;
		this.clientsToNotify = clientsToNotify;
	}

	String getTweet() {
		return tweet;
	}

	List<String> getClientsToNotify() {
		return clientsToNotify;
	}
	
	
}
