package com.integral.codingTest.hashtagMonitor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.google.common.collect.Lists;

import junit.framework.Assert;


public class HashtagMonitorImplDemoTest {

	private static final String[] HASHTAGS = new String[] {"#iHeartAwards","#BestFanArmy","#Harmonizers",
			"#Directioners","#5SOSFam","#android","#gameinsight"};
	private static final Random rn = new Random(12345678L);
	
	@Test
	public void HundredClientsTest() {
		final int NUM_CLIENTS = 1;
		//100 client ids
		List<String> clientIds = Lists.newArrayList();
		for (int i = 0; i < NUM_CLIENTS; i++) clientIds.add(Integer.toString(i + 1));
		
		Map<String, List<String>> hashTagToClientIds = new HashMap<String, List<String>>();
		Map<String, Set<String>> cidToHashTag = new HashMap<String, Set<String>>();
		for (String hTag : HASHTAGS) {
			List<String> cids = pickK(1, clientIds);//each hash tag has 10 clients interested in it
			Assert.assertEquals(1, cids.size());
			hashTagToClientIds.put(hTag, cids);
			for (String cid : cids) {
				if (!cidToHashTag.containsKey(cid))
					cidToHashTag.put(cid, new HashSet<String>());
				cidToHashTag.get(cid).add(hTag);
			}
		}
		
		TwitterSubscriberImpl tsi = new TwitterSubscriberImpl(hashTagToClientIds, clientIds);
		HashtagMonitorImpl toTest = new HashtagMonitorImpl(tsi, 
				new Lgr(), new ClientNotifierImpl(), NUM_CLIENTS);
		
		for (String cid : cidToHashTag.keySet()) {
			toTest.register(Lists.newArrayList(cidToHashTag.get(cid)), cid);
		}
		
		toTest.start();
		
		while (tsi.isDone() == false);
		
		System.out.println("Done!");
	}
	
	private static List<String> pickK(int k, List<String> items) {
		List<String> toReturn = Lists.newArrayList();
		int m = items.size();
		int i = 0;
		while (k > 0) {
			String str = items.get(i++);
			int n = rn.nextInt(m);
			if (n < k) {
				toReturn.add(str);
				k--;
			}
			m--;
		}
		return toReturn;
	}
	
	private static class Lgr implements Logger {

		public void logTrace(String msg) {
			System.out.println("TRACE:" + msg);
		}

		public void logWarn(String msg) {
			System.out.println("WARN:" + msg);
		}

		public void logDebug(String msg) {
			System.out.println("DEBUG:" + msg);
		}

		public void logInfo(String msg) {
			System.out.println("INFO:" + msg);
		}

		public void logError(String msg) {
			System.out.println("ERROR:" + msg);
		}
		
	}
	
	private static class TwitterSubscriberImpl implements TwitterSubscriber {

		private final Map<String, List<String>> hTagToClientIds;
		private volatile boolean isDone;
		private final Map<String, Integer> clientIdToExpectedTweetCount;
		
		TwitterSubscriberImpl(Map<String, List<String>> hTagToClientIds, List<String> clientIds) {
			this.hTagToClientIds = hTagToClientIds;
			this.clientIdToExpectedTweetCount = new HashMap<String, Integer>();
			for (String cid : clientIds) {
				this.clientIdToExpectedTweetCount.put(cid, 0);
			}
		}
		
		public void start(final TwitterConsumer twitterConsumer) {
			final TwitterSubscriberImpl impl = this;
			
			new Thread(new Runnable() {

				public void run() {
					List<String> hTags = Lists.newArrayList(HASHTAGS);
					for (int i = 0; i < 1000; i++) { //generate a million tweets
						int embedHashtag = rn.nextInt(10); //each tweet has a 10% chance of getting some valid hashtags
						Set<String> cidsNotified = new HashSet<String>();
						if (embedHashtag < 1) {
							List<String> hTagsToEmbed = pickK(3,hTags); //pick 3 hash tags
							assert(hTagsToEmbed.size() == 3);
							StringBuilder sbr = new StringBuilder();
							for (String hTag : hTagsToEmbed) {
								sbr.append(hTag);
								cidsNotified.addAll(hTagToClientIds.get(hTag));
							}
							twitterConsumer.consume(sbr.toString());

							for (String cid : cidsNotified) {
								clientIdToExpectedTweetCount.put(cid, clientIdToExpectedTweetCount.get(cid) + 1);
							}
							
						} else
							twitterConsumer.consume("XXXXXXXXX_________YYYYYYYYY");
					}
					
					impl.setIsDone();
					
				}
				
			}).start();

			
		}

		public void stop() {
			// TODO Auto-generated method stub
			
		}
		
		public void setIsDone() {
			isDone = true;
		}
		
		public boolean isDone() {
			return isDone;
		}
		
		public Map<String, Integer> getClientIdToExpectedTweetCount() {
			return clientIdToExpectedTweetCount;
		}
	}
	
	private static class ClientNotifierImpl implements ClientNotifier {

		private final Map<String, Integer> clientIdToTweetCount = new HashMap<String, Integer>();
		
		public void notifyClients(ProcessedTweet pTweet) {
			for (String cid : pTweet.getClientsToNotify()) {
				if (clientIdToTweetCount.containsKey(cid))
					clientIdToTweetCount.put(cid, clientIdToTweetCount.get(cid) + 1);
				else
					clientIdToTweetCount.put(cid, 1);
			}
			
		}
		
		public int getTweetCountForClient(String cid) {
			return clientIdToTweetCount.get(cid);
		}
		
	}
	
}
