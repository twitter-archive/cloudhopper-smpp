package com.cloudhopper.smpp.impl;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2015 Cloudhopper by Twitter
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

import com.cloudhopper.smpp.SmppClient;
import com.cloudhopper.smpp.util.DaemonExecutors;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.channel.SmppClientConnector;
import com.cloudhopper.smpp.channel.SmppSessionPduDecoder;
import com.cloudhopper.smpp.channel.SmppSessionLogger;
import com.cloudhopper.smpp.channel.SmppSessionWrapper;
import com.cloudhopper.smpp.channel.SmppSessionThreadRenamer;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.BindReceiver;
import com.cloudhopper.smpp.pdu.BindTransceiver;
import com.cloudhopper.smpp.pdu.BindTransmitter;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelConnectException;
import com.cloudhopper.smpp.type.SmppChannelConnectTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLEngine;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation to "bootstrap" client SMPP sessions (create & bind).
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppClient implements SmppClient {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppClient.class);

    private ChannelGroup channels;
    private SmppClientConnector clientConnector;
    private ExecutorService executors;
    private ClientSocketChannelFactory channelFactory;
    private ClientBootstrap clientBootstrap;
    private ScheduledExecutorService monitorExecutor;
    // shared instance of a timer for writeTimeout timing
    private final org.jboss.netty.util.Timer writeTimeoutTimer;

    /**
     * Creates a new default SmppClient. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * The maximum number of IO worker threads across any client sessions
     * created with this SmppClient will be Runtime.getRuntime().availableProcessors().
     * An Executors.newCachedDaemonThreadPool will be used for IO worker threads.
     */
    public DefaultSmppClient() {
        this(DaemonExecutors.newCachedDaemonThreadPool());
    }

    /**
     * Creates a new default SmppClient. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * The maximum number of IO worker threads across any client sessions
     * created with this SmppClient will be Runtime.getRuntime().availableProcessors().
     * @param executor The executor that IO workers will be executed with. An
     *      Executors.newCachedDaemonThreadPool() is recommended. The max threads
     *      will never grow more than expectedSessions if NIO sockets are used.
     */
    public DefaultSmppClient(ExecutorService executors) {
        this(executors, Runtime.getRuntime().availableProcessors());
    }

    /**
     * Creates a new default SmppClient. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * @param executor The executor that IO workers will be executed with. An
     *      Executors.newCachedDaemonThreadPool() is recommended. The max threads
     *      will never grow more than expectedSessions if NIO sockets are used.
     * @param expectedSessions The max number of concurrent sessions expected
     *      to be active at any time.  This number controls the max number of worker
     *      threads that the underlying Netty library will use.  If processing
     *      occurs in a sessionHandler (a blocking op), be <b>VERY</b> careful
     *      setting this to the correct number of concurrent sessions you expect.
     */
    public DefaultSmppClient(ExecutorService executors, int expectedSessions) {
        this(executors, expectedSessions, null);
    }
    
    /**
     * Creates a new default SmppClient.
     * @param executor The executor that IO workers will be executed with. An
     *      Executors.newCachedDaemonThreadPool() is recommended. The max threads
     *      will never grow more than expectedSessions if NIO sockets are used.
     * @param expectedSessions The max number of concurrent sessions expected
     *      to be active at any time.  This number controls the max number of worker
     *      threads that the underlying Netty library will use.  If processing
     *      occurs in a sessionHandler (a blocking op), be <b>VERY</b> careful
     *      setting this to the correct number of concurrent sessions you expect.
     * @param monitorExecutor The scheduled executor that all sessions will share
     *      to monitor themselves and expire requests.  If null monitoring will
     *      be disabled.
     */
    public DefaultSmppClient(ExecutorService executors, int expectedSessions, ScheduledExecutorService monitorExecutor) {
        this.channels = new DefaultChannelGroup();
        this.executors = executors;
        this.channelFactory = new NioClientSocketChannelFactory(this.executors, this.executors, expectedSessions);
        this.clientBootstrap = new ClientBootstrap(channelFactory);
        // we use the same default pipeline for all new channels - no need for a factory
        this.clientConnector = new SmppClientConnector(this.channels);
        this.clientBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_CLIENT_CONNECTOR_NAME, this.clientConnector);
        this.monitorExecutor = monitorExecutor;
	// a shared instance of a timer for session writeTimeout timing
	this.writeTimeoutTimer = new org.jboss.netty.util.HashedWheelTimer();
    }
    
    public int getConnectionSize() {
        return this.channels.size();
    }

    @Override
    public void destroy() {
        // close all channels still open within this session "bootstrap"
        this.channels.close().awaitUninterruptibly();
        // clean up all external resources
        this.clientBootstrap.releaseExternalResources();
	// stop the writeTimeout timer 
	this.writeTimeoutTimer.stop();
    }

    protected BaseBind createBindRequest(SmppSessionConfiguration config) throws UnrecoverablePduException {
        BaseBind bind = null;
        if (config.getType() == SmppBindType.TRANSCEIVER) {
            bind = new BindTransceiver();
        } else if (config.getType() == SmppBindType.RECEIVER) {
            bind = new BindReceiver();
        } else if (config.getType() == SmppBindType.TRANSMITTER) {
            bind = new BindTransmitter();
        } else {
            throw new UnrecoverablePduException("Unable to convert SmppSessionConfiguration into a BaseBind request");
        }
        bind.setSystemId(config.getSystemId());
        bind.setPassword(config.getPassword());
        bind.setSystemType(config.getSystemType());
        bind.setInterfaceVersion(config.getInterfaceVersion());
        bind.setAddressRange(config.getAddressRange());
        return bind;
    }


    public SmppSession bind(SmppSessionConfiguration config) throws SmppTimeoutException, SmppChannelException, SmppBindException, UnrecoverablePduException, InterruptedException {
        return bind(config, null);
    }

    @Override
    public SmppSession bind(SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, SmppBindException, UnrecoverablePduException, InterruptedException {
        DefaultSmppSession session = null;
        try {
            // connect to the remote system and create the session
            session = doOpen(config, sessionHandler);

            // try to bind to the remote system (may throw an exception)
            doBind(session, config, sessionHandler);
        } finally {
            // close the session if we weren't able to bind correctly
            if (session != null && !session.isBound()) {
                // make sure that the resources are always cleaned up
                try { session.close(); } catch (Exception e) { }
            }
        }
        return session;
    }

    protected void doBind(DefaultSmppSession session, SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, SmppBindException, UnrecoverablePduException, InterruptedException {
        // create the bind request we'll use (may throw an exception)
        BaseBind bindRequest = createBindRequest(config);
        BaseBindResp bindResp = null;

        try {
            // attempt to bind to the SMSC
            // session implementation handles error checking, version negotiation, and can be discarded
            bindResp = session.bind(bindRequest, config.getBindTimeout());
        } catch (RecoverablePduException e) {
            // if a bind fails, there really is no recovery...
            throw new UnrecoverablePduException(e.getMessage(), e);
        }
    }

    protected DefaultSmppSession doOpen(SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, InterruptedException {
        // create and connect a channel to the remote host
        Channel channel = createConnectedChannel(config.getHost(), config.getPort(), config.getConnectTimeout());
        // tie this new opened channel with a new session
        return createSession(channel, config, sessionHandler);
    }

    protected DefaultSmppSession createSession(Channel channel, SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, InterruptedException {
        DefaultSmppSession session = new DefaultSmppSession(SmppSession.Type.CLIENT, config, channel, sessionHandler, monitorExecutor);

	// add SSL handler 
        if (config.isUseSsl()) {
	    SslConfiguration sslConfig = config.getSslConfiguration();
	    if (sslConfig == null) throw new IllegalStateException("sslConfiguration must be set");
	    try {
		SslContextFactory factory = new SslContextFactory(sslConfig);
		SSLEngine sslEngine = factory.newSslEngine();
		sslEngine.setUseClientMode(true);
		channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
	    } catch (Exception e) {
		throw new SmppChannelConnectException("Unable to create SSL session]: " + e.getMessage(), e);
	    }
	}

        // add the thread renamer portion to the pipeline
        if (config.getName() != null) {
            channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, new SmppSessionThreadRenamer(config.getName()));
        } else {
            logger.warn("Session configuration did not have a name set - skipping threadRenamer in pipeline");
        }

        // create the logging handler (for bytes sent/received on wire)
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(), config.getLoggingOptions());
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

	// add a writeTimeout handler after the logger
	if (config.getWriteTimeout() > 0) {
	    WriteTimeoutHandler writeTimeoutHandler = new WriteTimeoutHandler(writeTimeoutTimer, config.getWriteTimeout(), TimeUnit.MILLISECONDS);
	    channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRITE_TIMEOUT_NAME, writeTimeoutHandler);
	}

        // add a new instance of a decoder (that takes care of handling frames)
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME, new SmppSessionPduDecoder(session.getTranscoder()));

        // create a new wrapper around a session to pass the pdu up the chain
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));

        return session;
    }

    protected Channel createConnectedChannel(String host, int port, long connectTimeoutMillis) throws SmppTimeoutException, SmppChannelException, InterruptedException {
        // a socket address used to "bind" to the remote system
        InetSocketAddress socketAddr = new InetSocketAddress(host, port);

	// set the timeout
	this.clientBootstrap.setOption("connectTimeoutMillis", connectTimeoutMillis);

        // attempt to connect to the remote system
        ChannelFuture connectFuture = this.clientBootstrap.connect(socketAddr);
        
        // wait until the connection is made successfully
	// boolean timeout = !connectFuture.await(connectTimeoutMillis);
	// BAD: using .await(timeout)
	//      see http://netty.io/3.9/api/org/jboss/netty/channel/ChannelFuture.html
	connectFuture.awaitUninterruptibly();
	//assert connectFuture.isDone();

	if (connectFuture.isCancelled()) {
	    throw new InterruptedException("connectFuture cancelled by user");
	} else if (!connectFuture.isSuccess()) {
	    if (connectFuture.getCause() instanceof org.jboss.netty.channel.ConnectTimeoutException) {
		throw new SmppChannelConnectTimeoutException("Unable to connect to host [" + host + "] and port [" + port + "] within " + connectTimeoutMillis + " ms", connectFuture.getCause());
	    } else {
		throw new SmppChannelConnectException("Unable to connect to host [" + host + "] and port [" + port + "]: " + connectFuture.getCause().getMessage(), connectFuture.getCause());
	    }
	}

        // if we get here, then we were able to connect and get a channel
        return connectFuture.getChannel();
    }

}
