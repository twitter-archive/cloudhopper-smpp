package com.cloudhopper.smpp.demo.persist;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectionTask implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(ReconnectionTask.class);

	private final OutboundClient client;
	private Integer connectionFailedTimes;

	protected ReconnectionTask(OutboundClient client, Integer connectionFailedTimes) {
		this.client = client;
		this.connectionFailedTimes = connectionFailedTimes;
	}

	@Override
	public synchronized void run() {
		// guard against multiple reconnects, downside is that you must be sure to kill session before calling this,
		// sometimes it stain bound state even after SMSC is killed
		if (!client.isConnected() && optimisticLock()) {
			client.connect();
		} else {
			log.info("client {} is connected, skipping reconnect", client);
		}
	}

	/** when multiple tasks are executed/scheduled, execute just the first one */
	private boolean optimisticLock() {
		return client.getConnectionFailedTimes().equals(connectionFailedTimes);
	}

}
