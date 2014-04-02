package com.cloudhopper.smpp.demo.persist;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReconnectionDaemon {

	public static final int MAX_THREADS = 50;
	public static final ReconnectionDaemon RECONNECTION_DAEMON = new ReconnectionDaemon("1,5,15");
	private static final Logger log = LoggerFactory.getLogger(ReconnectionDaemon.class);
	private final ScheduledThreadPoolExecutor executorService;
	private final String[] reconnectionPeriods;

	public ReconnectionDaemon(String reconnectionPeriods) {
		this.reconnectionPeriods = reconnectionPeriods.split(",");

		// #schedule does not create more threads than corePoolSize
		executorService = new ScheduledThreadPoolExecutor(MAX_THREADS, new ThreadFactory() {

			private AtomicInteger sequence = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("ReconnectionDaemon-" + sequence.getAndIncrement());
				return t;
			}
		});
		executorService.setKeepAliveTime(120, TimeUnit.SECONDS);
		executorService.allowCoreThreadTimeOut(true);

	}

	public static ReconnectionDaemon getInstance() {
		return RECONNECTION_DAEMON;
	}

	public void executeReconnect(ReconnectionTask reconnectionTask) {
		executorService.execute(reconnectionTask);
	}

	public void scheduleReconnectByFailureNumber(OutboundClient outboundClient, Integer integer,
			ReconnectionTask reconnectionTask) {
		String reconnectionPeriod = getReconnectionPeriod(integer);
		log.info("Scheduling reconnect for {} in {} seconds", outboundClient, reconnectionPeriod);
		executorService.schedule(reconnectionTask, Long.parseLong(reconnectionPeriod),
				TimeUnit.SECONDS);

	}

	protected String getReconnectionPeriod(Integer integer) {
		String reconnectionPeriod;
		if (reconnectionPeriods.length > integer) {
			reconnectionPeriod = reconnectionPeriods[integer];
		} else {
			reconnectionPeriod = reconnectionPeriods[reconnectionPeriods.length - 1];
		}
		return reconnectionPeriod;
	}

	@PreDestroy
	public void shutdown() {
		executorService.shutdown();
	}

}
