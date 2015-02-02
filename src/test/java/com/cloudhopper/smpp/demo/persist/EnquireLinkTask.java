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

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EnquireLinkTask implements Runnable {

	private static final Logger logger = LoggerFactory.getLogger(EnquireLinkTask.class);
	private OutboundClient client;
	private Integer enquireLinkTimeout;

	public EnquireLinkTask(OutboundClient client, Integer enquireLinkTimeout) {
		this.client = client;
		this.enquireLinkTimeout = enquireLinkTimeout;
	}

	@Override
	public void run() {
		SmppSession smppSession = client.getSession();
		if (smppSession != null && smppSession.isBound()) {
			try {
				logger.debug("sending enquire_link");
				EnquireLinkResp enquireLinkResp = smppSession.enquireLink(new EnquireLink(), enquireLinkTimeout);
				logger.debug("enquire_link_resp: {}", enquireLinkResp);
			} catch (SmppTimeoutException e) {
				logger.warn("Enquire link failed, executing reconnect; " + e);
				logger.debug("", e);
				client.scheduleReconnect();
			} catch (SmppChannelException e) {
				logger.warn("Enquire link failed, executing reconnect; " + e);
				logger.debug("", e);
				client.scheduleReconnect();
			} catch (InterruptedException e) {
				logger.info("Enquire link interrupted, probably killed by reconnecting");
			} catch (Exception e) {
				logger.error("Enquire link failed, executing reconnect", e);
				client.scheduleReconnect();
			}
		} else {
			logger.error("enquire link running while session is not connected");
		}
	}
}
