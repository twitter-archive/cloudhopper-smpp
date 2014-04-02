package com.cloudhopper.smpp.demo.persist;

import com.cloudhopper.smpp.*;

public abstract class Client {

	protected volatile SmppSession smppSession;

	protected Client(SmppSession smppSession) {
		this.smppSession = smppSession;
	}

	public SmppSessionConfiguration getConfiguration() {
		return smppSession.getConfiguration();
	}

	public boolean isConnected() {
		if (smppSession != null) {
			return smppSession.isBound();
		}
		return false;
	}

	public SmppSession getSession() {
		return smppSession;
	}
}
