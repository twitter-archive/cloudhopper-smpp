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
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));
		clients.add(createClient(smppClientMessageService));

		while (true) {
			System.in.read();
			break;
		}
		for (OutboundClient client1 : clients) {
			client1.shutdown();
		}
	}

	private static OutboundClient createClient(DummySmppClientMessageService smppClientMessageService) {
		OutboundClient client = new OutboundClient();
		client.initialize(getSmppSessionConfiguration(), smppClientMessageService);
		client.executeReconnect();
		return client;
	}

	private static SmppSessionConfiguration getSmppSessionConfiguration() {
		// same configuration for each client runner
		SmppSessionConfiguration config = new SmppSessionConfiguration();
		config.setWindowSize(5);
		config.setName("Tester.Session.0");
		config.setType(SmppBindType.TRANSCEIVER);
		config.setHost("127.0.0.1");
		config.setPort(5019);
		config.setConnectTimeout(10000);
		config.setSystemId("systemId");
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
