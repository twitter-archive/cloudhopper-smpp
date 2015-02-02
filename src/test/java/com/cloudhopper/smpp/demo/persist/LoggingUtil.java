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

import com.cloudhopper.smpp.SmppSessionConfiguration;

public class LoggingUtil {

	public static String toString(SmppSessionConfiguration config) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(config.getSystemId());
		sb.append(":");
		sb.append(config.getPassword());

		sb.append(";");

		sb.append(config.getHost());
		sb.append(":");
		sb.append(config.getPort());

		sb.append(";");
		sb.append(config.getType());
		sb.append("]");
		return sb.toString();
	}

	public static String toString2(SmppSessionConfiguration config) {
		StringBuilder sb = new StringBuilder();
		sb.append("[");

		sb.append(config.getName());

		sb.append(";");

		sb.append(config.getSystemId());

		sb.append(";");

		sb.append(config.getHost());
		sb.append(":");
		sb.append(config.getPort());

		sb.append(";");
		sb.append(config.getType());
		sb.append("]");
		return sb.toString();
	}

	public static void log(Logger log, Throwable e) {
		log.warn(String.valueOf(e.getMessage()), e);
	}
}
