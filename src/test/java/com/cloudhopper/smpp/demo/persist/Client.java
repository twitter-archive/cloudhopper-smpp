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
import com.cloudhopper.smpp.SmppSessionConfiguration;

public abstract class Client {

	protected volatile SmppSession smppSession;

	public SmppSessionConfiguration getConfiguration() {
		return smppSession.getConfiguration();
	}

	public boolean isConnected() {
		SmppSession session = smppSession;
		if (session != null) {
			return session.isBound();
		}
		return false;
	}

	public SmppSession getSession() {
		return smppSession;
	}
}
