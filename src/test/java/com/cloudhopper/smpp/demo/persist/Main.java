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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.type.*;

public class Main {

	public static void main(String[] args) throws IOException, RecoverablePduException, InterruptedException,
			SmppChannelException, UnrecoverablePduException, SmppTimeoutException {
		DummySmppClientMessageService smppClientMessageService = new DummySmppClientMessageService();
		List<OutboundClient> clients = new ArrayList<OutboundClient>();
		int i = 0;
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));

		while (true) {
			System.in.read();
			break;
		}
		
		ReconnectionDaemon.getInstance().shutdown();
		for (OutboundClient client1 : clients) {
			client1.shutdown();
		}
	}

	private static OutboundClient createClient(DummySmppClientMessageService smppClientMessageService, int i) {
		OutboundClient client = new OutboundClient();
		client.initialize(getSmppSessionConfiguration(i), smppClientMessageService);
		client.executeReconnect();
		return client;
	}

	private static SmppSessionConfiguration getSmppSessionConfiguration(int i) {
		SmppSessionConfiguration config = new SmppSessionConfiguration();
		config.setWindowSize(5);
		config.setName("Tester.Session." + i);
		config.setType(SmppBindType.TRANSCEIVER);
		config.setHost("127.0.0.1");
		config.setPort(2776);
		config.setConnectTimeout(10000);
		config.setSystemId("systemId" + i);
		config.setPassword("password");
		config.getLoggingOptions().setLogBytes(false);
		// to enable monitoring (request expiration)

		config.setRequestExpiryTimeout(30000);
		config.setWindowMonitorInterval(15000);

		config.setCountersEnabled(true);
		return config;
	}

}
