package com.cloudhopper.smpp.demo.persist;

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
		clients.add(createClient(smppClientMessageService,++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));
		clients.add(createClient(smppClientMessageService, ++i));

		while (true) {
			System.in.read();
			break;
		}
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
		// same configuration for each client runner
		SmppSessionConfiguration config = new SmppSessionConfiguration();
		config.setWindowSize(5);
		config.setName("Tester.Session."+i);
		config.setType(SmppBindType.TRANSCEIVER);
		config.setHost("127.0.0.1");
		config.setPort(5019);
		config.setConnectTimeout(10000);
		config.setSystemId("systemId"+i);
		config.setPassword("password");
		config.getLoggingOptions().setLogBytes(false);
		// to enable monitoring (request expiration)

		// values for easier bug reproducing?
		config.setRequestExpiryTimeout(15000);
		config.setWindowMonitorInterval(7000);
		config.setWindowWaitTimeout(30000);

		config.setCountersEnabled(true);
		return config;
	}

}
