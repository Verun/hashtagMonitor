/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

import java.util.List;

/**
 * Clients can register to receive tweets for specific hashtags
 * with this monitor.
 * @author Verun
 *
 */
public interface HashtagMonitor {

	/**
	 * Register the clientId to receive tweets pertaining to these hashtags.
	 * @param hashtag
	 * @param clientId
	 */
	void register(List<String> hashtag, String clientId);
	
	/**
	 * Unregister the given clientId.
	 * @param clientId
	 */
	void unregister(String clientId);
	
	/**
	 * Update the hashtags that the given client is interested in.
	 * @param updatedHashtags
	 * @param clientId
	 */
	void modify(List<String> updatedHashtags, String clientId);
	
}
