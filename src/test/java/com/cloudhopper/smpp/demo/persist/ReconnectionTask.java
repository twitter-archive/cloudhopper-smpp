package com.cloudhopper.smpp.demo.persist;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2014 Cloudhopper by Twitter
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
		} else if (client.isConnected()) {
			log.info("client {} is connected, skipping reconnect", client);
		} else {
			log.info("skipping duplicate reconnect for client {}", client);
		}
	}

	/** when multiple tasks are executed/scheduled, execute just the first one */
	private boolean optimisticLock() {
		return client.getConnectionFailedTimes().equals(connectionFailedTimes);
	}

}
