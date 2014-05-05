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

import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.impl.DefaultSmppClient;
import com.cloudhopper.smpp.impl.DefaultSmppSessionHandler;
import com.cloudhopper.smpp.type.SmppChannelConnectException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OutboundClient extends Client {

	private ReconnectionDaemon reconnectionDaemon = ReconnectionDaemon.getInstance();

	private Logger logger = LoggerFactory.getLogger(OutboundClient.class);

	private final ScheduledThreadPoolExecutor monitorExecutor;
	private final ThreadPoolExecutor executor;
	private DefaultSmppClient clientBootstrap;
	private DefaultSmppSessionHandler sessionHandler;

	private SmppSessionConfiguration config;

	private ScheduledExecutorService enquireLinkExecutor;
	private ScheduledFuture<?> enquireLinkTask;
	private Integer enquireLinkPeriod = 1000;
	private Integer enquireLinkTimeout = 10000;
	private boolean shutdown = false;

	private volatile Integer connectionFailedTimes = 0;

	public OutboundClient() {
		this.enquireLinkExecutor = Executors.newScheduledThreadPool(1, new ThreadFactory() {

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				String name = config.getName();
				t.setName("EnquireLink-" + name);
				return t;
			}
		});
		// for monitoring thread use, it's preferable to create your own instance
		// of an executor with Executors.newCachedThreadPool() and cast it to ThreadPoolExecutor
		// this permits exposing thinks like executor.getActiveCount() via JMX possible
		// no point renaming the threads in a factory since underlying Netty
		// framework does not easily allow you to customize your thread names
		executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

		// to enable automatic expiration of requests, a second scheduled executor
		// is required which is what a monitor task will be executed with - this
		// is probably a thread pool that can be shared with between all client bootstraps
		monitorExecutor = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, new ThreadFactory() {

			private AtomicInteger sequence = new AtomicInteger(0);

			@Override
			public Thread newThread(Runnable r) {
				Thread t = new Thread(r);
				t.setName("SmppClientSessionWindowMonitorPool-" + sequence.getAndIncrement());
				return t;
			}
		});

		// a single instance of a client bootstrap can technically be shared
		// between any sessions that are created (a session can go to any different
		// number of SMSCs) - each session created under
		// a client bootstrap will use the executor and monitorExecutor set
		// in its constructor - just be *very* careful with the "expectedSessions"
		// value to make sure it matches the actual number of total concurrent
		// open sessions you plan on handling - the underlying netty library
		// used for NIO sockets essentially uses this value as the max number of
		// threads it will ever use, despite the "max pool size", etc. set on
		// the executor passed in here
		clientBootstrap = new DefaultSmppClient(executor, 1, monitorExecutor);
	}

	public void initialize(SmppSessionConfiguration config, SmppClientMessageService smppClientMessageService) {
		this.config = config;
		logger = LoggerFactory.getLogger(OutboundClient.class.getCanonicalName() + config.getName());
		//
		// setup configuration for a client session
		//
		sessionHandler = new ClientSmppSessionHandler(this, smppClientMessageService);
	}

	protected synchronized void reconnect(Integer connectionFailedTimes) {
		if (shutdown) {
			logger.warn("skipping reconnect for client {} due to shutdown", this);
			return;
		}
		if (!getConnectionFailedTimes().equals(connectionFailedTimes)) {
			logger.info("skipping reconnect for client {} due to optimistic lock", this, connectionFailedTimes,
					getConnectionFailedTimes());
			return;
		}
		++this.connectionFailedTimes;

		try {
			logger.info("connecting {}", this);

			disconnect();

			smppSession = clientBootstrap.bind(config, sessionHandler);

			this.connectionFailedTimes = 0;

			runEnquireLinkTask();

			logger.info("connected {}", this);
		} catch (SmppChannelConnectException e) {
			logger.warn("Unable to connect: " + e.getMessage() + " " + LoggingUtil.toString(getConfiguration()));
			logger.debug("", e);
			scheduleReconnect();
		} catch (SmppTimeoutException e) {
			logger.warn("Unable to connect: " + e.getMessage() + " " + LoggingUtil.toString(getConfiguration()));
			logger.debug("", e);
			scheduleReconnect();
		} catch (Exception e) {
			logger.error("Unable to connect: " + LoggingUtil.toString(getConfiguration()), e);
			scheduleReconnect();
		}
	}

	public void scheduleReconnect() {
		reconnectionDaemon.scheduleReconnect(this, connectionFailedTimes, createReconnectionTask());
	}

	private ReconnectionTask createReconnectionTask() {
		return new ReconnectionTask(this, connectionFailedTimes);
	}


	private void runEnquireLinkTask() {
		enquireLinkTask = this.enquireLinkExecutor.scheduleWithFixedDelay(
				new EnquireLinkTask(this, enquireLinkTimeout),
				enquireLinkPeriod, enquireLinkPeriod, TimeUnit.MILLISECONDS);
	}

	public synchronized void shutdown() {
		logger.info("Shutting down client {}", this);

		shutdown = true;
		disconnect();

		// this is required to not causing server to hang from non-daemon threads
		// this also makes sure all open Channels are closed to I *think*
		clientBootstrap.destroy();
		executor.shutdownNow();
		enquireLinkExecutor.shutdownNow();
		monitorExecutor.shutdownNow();
	}

	private void disconnect() {
		stopEnquireLinkTask();

		destroySession();
	}

	private void stopEnquireLinkTask() {
		if (enquireLinkTask != null) {
			this.enquireLinkTask.cancel(true);
		}
	}

	private void destroySession() {
		try {
			if (smppSession != null) {
				logger.debug("Cleaning up session... (final counters)");
				logCounters();

				smppSession.destroy();
				smppSession = null;
				// alternatively, could call close(), get outstanding requests from
				// the sendWindow (if we wanted to retry them later), then call shutdown()
			}
		} catch (Exception e) {
			logger.warn("Destroy session error", e);
		}
	}

	private void logCounters() {
		if (smppSession.hasCounters()) {
			logger.debug("tx-enquireLink: {}", smppSession.getCounters().getTxEnquireLink());
			logger.debug("tx-submitSM: {}", smppSession.getCounters().getTxSubmitSM());
			logger.debug("tx-deliverSM: {}", smppSession.getCounters().getTxDeliverSM());
			logger.debug("tx-dataSM: {}", smppSession.getCounters().getTxDataSM());
			logger.debug("rx-enquireLink: {}", smppSession.getCounters().getRxEnquireLink());
			logger.debug("rx-submitSM: {}", smppSession.getCounters().getRxSubmitSM());
			logger.debug("rx-deliverSM: {}", smppSession.getCounters().getRxDeliverSM());
			logger.debug("rx-dataSM: {}", smppSession.getCounters().getRxDataSM());
		}
	}

	public Integer getConnectionFailedTimes() {
		return connectionFailedTimes;
	}

	@Override
	public SmppSessionConfiguration getConfiguration() {
		return config;
	}

	@Override
	public String toString() {
		return LoggingUtil.toString2(config);
	}

}
