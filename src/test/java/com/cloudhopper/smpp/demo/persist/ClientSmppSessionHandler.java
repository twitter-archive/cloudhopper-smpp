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


import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.pdu.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;

public class ClientSmppSessionHandler extends DefaultSmppSessionHandler {

	private static final Logger logger = LoggerFactory.getLogger(ClientSmppSessionHandler.class);

	private OutboundClient client;
	private SmppClientMessageService smppClientMessageService;

	public ClientSmppSessionHandler(OutboundClient client,
									SmppClientMessageService smppClientMessageService) {
		super(logger);
		this.client = client;
		this.smppClientMessageService = smppClientMessageService;
	}

	@Override
	public void firePduRequestExpired(PduRequest pduRequest) {
		logger.warn("PDU request expired: {}", pduRequest);
	}

	@Override
	public PduResponse firePduRequestReceived(PduRequest request) {
		PduResponse response = null;
		try {
			if (request instanceof DeliverSm) {
				logger.info("request {}", request);
				response = smppClientMessageService.received(client, (DeliverSm) request);
				logger.info("response {}", response);
			} else {
				response = request.createResponse();
			}
		} catch (Throwable e) {
			LoggingUtil.log(logger, e);
			response = request.createResponse();
			response.setResultMessage(e.getMessage());
			response.setCommandStatus(SmppConstants.STATUS_UNKNOWNERR);
		}

		return response;
	}

	/**
	 * TODO not sure if we really need to call reconnect here
	 */
	@Override
	public void fireUnknownThrowable(Throwable t) {
		if (t instanceof ClosedChannelException) {
			logger.warn("Unknown throwable received, but it was a ClosedChannelException, executing reconnect" + " "
					+ LoggingUtil.toString(client.getConfiguration()));
			client.scheduleReconnect();
		} else if (t instanceof IOException) {
			logger.warn(t + " " + LoggingUtil.toString(client.getConfiguration()));
			//#fireChannelUnexpectedlyClosed will be called from a different place
		} else {
			logger.warn(String.valueOf(t) + " " + LoggingUtil.toString(client.getConfiguration()), t);
		}
	}

	@Override
	public void fireChannelUnexpectedlyClosed() {
		client.scheduleReconnect();
	}

}
