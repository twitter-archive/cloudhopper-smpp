package com.cloudhopper.smpp.demo.persist;

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
