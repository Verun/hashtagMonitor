/**
 * 
 */
package com.integral.codingTest.hashtagMonitor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

/**
 * @author Verun
 *
 */
public class HashtagMonitorImpl implements HashtagMonitor {

	private final ConcurrentMap<String, ConcurrentMap<String, Boolean>> hashtagToClientIdMap = new ConcurrentHashMap<String, ConcurrentMap<String, Boolean>>();
	private final ConcurrentMap<String, Set<String>> clientIdToHashtag = new ConcurrentHashMap<String, Set<String>>();
	
	private final TwitterSubscriber twitterSubscriber;
	private final Logger logger;
	private final int numTweetProcessors;
	private final ClientNotifier clientNotifier;
	private final ExecutorService execService;
	
	/**
	 * Constructor - dependencies expected to be injected via a container such as IoC.
	 * @param twitterSubscriber
	 * @param logger
	 * @param clientNotifier
	 * @param numTweetProcessors
	 */
	public HashtagMonitorImpl(TwitterSubscriber twitterSubscriber, Logger logger, 
			ClientNotifier clientNotifier,
			int numTweetProcessors) {
		Preconditions.checkNotNull(twitterSubscriber);
		Preconditions.checkNotNull(logger);
		Preconditions.checkArgument(numTweetProcessors > 0);
		Preconditions.checkNotNull(clientNotifier);
		this.twitterSubscriber = twitterSubscriber;
		this.logger = logger;
		this.numTweetProcessors = numTweetProcessors;
		this.clientNotifier = clientNotifier;
		this.execService = Executors.newFixedThreadPool(numTweetProcessors);
	}
	
	/**
	 * Starts the monitor.
	 */
	public void start() {
		final BlockingQueue<String> inTweets = new ArrayBlockingQueue<String>(100);//TODO drive this capacity from configuration
		//submit the tweet processors
		for (int i = 0; i < numTweetProcessors; i++) {
			TweetProcessor tProcessor = new TweetProcessor(inTweets);
			execService.submit(tProcessor);
		}
		
		//start the tweet subscription
		this.twitterSubscriber.start(new TwitterConsumer() {
			public void consume(String tweet) {
				try {
					inTweets.put(tweet);
				} catch (InterruptedException e) {
					logger.logWarn("Interrupted exception in twitter consumer!");
				}
			}
		});
	}
	
	public void stop() {
		//stop the tweet subscription
		this.twitterSubscriber.stop();
		//shut down the tweet processors
		this.execService.shutdownNow();
	}
	
	public void register(List<String> hashtag, String clientId) {
		Preconditions.checkNotNull(clientId);
		Preconditions.checkNotNull(hashtag);
		Preconditions.checkArgument(hashtag.size() > 0);
		//put in the client -> hashtag map
		Set<String> set = new HashSet<String>(hashtag);
		clientIdToHashtag.putIfAbsent(clientId, set);
		
		//put in the hashtag -> client id map
		for (String hTag : hashtag) {
			ConcurrentMap<String, Boolean> cMap = hashtagToClientIdMap.putIfAbsent(hTag, new ConcurrentHashMap<String, Boolean>());
			cMap.putIfAbsent(clientId, true);
		}
		
		logger.logInfo(String.format("%s-%s-%s", "REGISTERED",clientId,Arrays.toString(hashtag.toArray(new String[0]))));
	}

	public void unregister(String clientId) {
		Preconditions.checkNotNull(clientId);
		
		//update the client -> hashtag map
		Set<String> current = clientIdToHashtag.remove(clientId);
		
		//update the hashtag -> clientId map
		for (String hTag : current) {
			ConcurrentMap<String, Boolean> cMap = hashtagToClientIdMap.get(clientId);
			if (cMap != null) {
				cMap.remove(clientId);
			}
		}

		logger.logInfo(String.format("%s-%s", "UNREGISTERED",clientId));
		
	}

	public void modify(List<String> updatedHashtags, String clientId) {
		unregister(clientId);
		register(updatedHashtags, clientId);	
	}

	/**
	 * Processes a tweet and determines which clients should be 
	 * notified.
	 * @author Verun
	 *
	 */
	class TweetProcessor implements Runnable {

		private final BlockingQueue<String> inTweets;
		
		TweetProcessor(BlockingQueue<String> inTweets) {
			this.inTweets = inTweets;
		}
		
		public void run() {
			
			while(true) {
				try {
					String tweet = inTweets.take();
					//brute forcing this for now
					//get the hashtags we are currently interested in
					Set<String> hashtagsOfInterest = hashtagToClientIdMap.keySet();
					
					//see which hashtags are present in the tweet
					Set<String> hashtagsFound = new HashSet<String>();
					for (String h : hashtagsOfInterest) {
						if (tweet.contains(h)) hashtagsFound.add(h);
					}
					
					//figure out which clients need to be notified
					Set<String> clientsToNotify = new HashSet<String>();
					for (String h : hashtagsFound) {
						ConcurrentMap<String, Boolean> clients = hashtagToClientIdMap.get(h);
						if (clients.size() > 0)
							clientsToNotify.addAll(clients.keySet());
					}
					
					//forward the info to the client notifier by placing it in the blocking queue
					ProcessedTweet pTweet = new ProcessedTweet(tweet, Lists.newArrayList(clientsToNotify));
					clientNotifier.notifyClients(pTweet);
				} catch (InterruptedException e) {
					logger.logWarn("TweetProcessor encountered interrupted exception!");
				}
			}
		}
		
	}
}
