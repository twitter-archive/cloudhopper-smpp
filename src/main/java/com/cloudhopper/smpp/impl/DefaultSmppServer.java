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

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServer;
import com.cloudhopper.smpp.SmppServerConfiguration;
import com.cloudhopper.smpp.SmppServerHandler;
import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.channel.SmppChannelConstants;
import com.cloudhopper.smpp.channel.SmppServerConnector;
import com.cloudhopper.smpp.channel.SmppSessionLogger;
import com.cloudhopper.smpp.channel.SmppSessionThreadRenamer;
import com.cloudhopper.smpp.channel.SmppSessionWrapper;
import com.cloudhopper.smpp.jmx.DefaultSmppServerMXBean;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.util.DaemonExecutors;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ObjectName;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of an SmppServer that supports SMPP version 3.3 and 3.4.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppServer implements SmppServer, DefaultSmppServerMXBean {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppServer.class);

    private final ChannelGroup channels;
    private final SmppServerConnector serverConnector;
    private final SmppServerConfiguration configuration;
    private final SmppServerHandler serverHandler;
    private final PduTranscoder transcoder;
    private ExecutorService bossThreadPool;
    private ChannelFactory channelFactory;
    private ServerBootstrap serverBootstrap;
    private Channel serverChannel; 
    // shared instance of a timer for session writeTimeout timing
    private final org.jboss.netty.util.Timer writeTimeoutTimer;
    // shared instance of a timer background thread to close unbound channels
    private final Timer bindTimer;
   // shared instance of a session id generator (an atomic long)
    private final AtomicLong sessionIdSequence;
    // shared instance for monitor executors
    private final ScheduledExecutorService monitorExecutor;
    private DefaultSmppServerCounters counters;
    
    /**
     * Creates a new default SmppServer. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * A "cachedDaemonThreadPool" will be used for IO worker threads.
     * @param configuration The server configuration to create this server with
     * @param serverHandler The handler implementation for handling bind requests
     *      and creating/destroying sessions.
     */
    public DefaultSmppServer(SmppServerConfiguration configuration, SmppServerHandler serverHandler) {
        this(configuration, serverHandler, DaemonExecutors.newCachedDaemonThreadPool());
    }
    
    /**
     * Creates a new default SmppServer. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * @param configuration The server configuration to create this server with
     * @param serverHandler The handler implementation for handling bind requests
     *      and creating/destroying sessions.
     * @param executor The executor that IO workers will be executed with. An
     *      Executors.newCachedDaemonThreadPool() is recommended. The max threads
     *      will never grow more than configuration.getMaxConnections() if NIO
     *      sockets are used.
     */
    public DefaultSmppServer(final SmppServerConfiguration configuration, SmppServerHandler serverHandler, ExecutorService executor) {
        this(configuration, serverHandler, executor, null);
    }

    /**
     * Creates a new default SmppServer.
     * @param configuration The server configuration to create this server with
     * @param serverHandler The handler implementation for handling bind requests
     *      and creating/destroying sessions.
     * @param executor The executor that IO workers will be executed with. An
     *      Executors.newCachedDaemonThreadPool() is recommended. The max threads
     *      will never grow more than configuration.getMaxConnections() if NIO
     *      sockets are used.
     * @param monitorExecutor The scheduled executor that all sessions will share
     *      to monitor themselves and expire requests. If null monitoring will
     *      be disabled.
     */
    public DefaultSmppServer(final SmppServerConfiguration configuration, SmppServerHandler serverHandler, ExecutorService executor, ScheduledExecutorService monitorExecutor) {
        this.configuration = configuration;
        // the same group we'll put every server channel
        this.channels = new DefaultChannelGroup();
        this.serverHandler = serverHandler;
        // we'll put the "boss" worker for a server in its own pool
        this.bossThreadPool = Executors.newCachedThreadPool();
        
        // a factory for creating channels (connections)
        if (configuration.isNonBlockingSocketsEnabled()) {
            this.channelFactory = new NioServerSocketChannelFactory(this.bossThreadPool, executor, configuration.getMaxConnectionSize());
        } else {
            this.channelFactory = new OioServerSocketChannelFactory(this.bossThreadPool, executor);
        }
        
        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap(this.channelFactory);
        
        // set options for the server socket that are useful
        this.serverBootstrap.setOption("reuseAddress", configuration.isReuseAddress());
        
        // we use the same default pipeline for all new channels - no need for a factory
        this.serverConnector = new SmppServerConnector(channels, this);
        this.serverBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_SERVER_CONNECTOR_NAME, this.serverConnector);
	// a shared instance of a timer for session writeTimeout timing
	this.writeTimeoutTimer = new org.jboss.netty.util.HashedWheelTimer();
        // a shared timer used to make sure new channels are bound within X milliseconds
        this.bindTimer = new Timer(configuration.getName() + "-BindTimer0", true);
        // NOTE: this would permit us to customize the "transcoding" context for a server if needed
        this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext());
        this.sessionIdSequence = new AtomicLong(0);        
        this.monitorExecutor = monitorExecutor;
        this.counters = new DefaultSmppServerCounters();
        if (configuration.isJmxEnabled()) {
            registerMBean();
        }
    }
    
    private void registerMBean() {
        if (configuration == null) {
            return;
        }
        if (configuration.isJmxEnabled()) {
            // register the this queue manager as an mbean
            try {
                ObjectName name = new ObjectName(configuration.getJmxDomain() + ":name=" + configuration.getName());
                ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
            } catch (Exception e) {
                // log the error, but don't throw an exception for this datasource
                logger.error("Unable to register DefaultSmppServerMXBean [{}]", configuration.getName(), e);
            }
        }
    }
    
    private void unregisterMBean() {
        if (configuration == null) {
            return;
        }
        if (configuration.isJmxEnabled()) {
            // register the this queue manager as an mbean
            try {
                ObjectName name = new ObjectName(configuration.getJmxDomain() + ":name=" + configuration.getName());
                ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
            } catch (Exception e) {
                // log the error, but don't throw an exception for this datasource
                logger.error("Unable to unregister DefaultSmppServerMXBean [{}]", configuration.getName(), e);
            }
        }
    }

    public PduTranscoder getTranscoder() {
        return this.transcoder;
    }

    @Override
    public ChannelGroup getChannels() {
        return this.channels;
    }

    public SmppServerConfiguration getConfiguration() {
        return this.configuration;
    }
    
    @Override
    public DefaultSmppServerCounters getCounters() {
        return this.counters;
    }

    public Timer getBindTimer() {
        return this.bindTimer;
    }
    
    @Override
    public boolean isStarted() {
        return (this.serverChannel != null && this.serverChannel.isBound());
    }

    @Override
    public boolean isStopped() {
        return (this.serverChannel == null);
    }

    @Override
    public boolean isDestroyed() {
        return (this.serverBootstrap == null);
    }
    
    @Override
    public void start() throws SmppChannelException {
        if (isDestroyed()) {
            throw new SmppChannelException("Unable to start: server is destroyed");
        }
        try {
            serverChannel = this.serverBootstrap.bind(new InetSocketAddress(configuration.getHost(), configuration.getPort()));
            logger.info("{} started at {}:{}", configuration.getName(), configuration.getHost(), configuration.getPort());
        } catch (ChannelException e) {
            throw new SmppChannelException(e.getMessage(), e);
        }
    }

    @Override
    public void stop() {
        if (this.channels.size() > 0) {
            logger.info("{} currently has [{}] open child channel(s) that will be closed as part of stop()", configuration.getName(), this.channels.size());
        }
        // close all channels still open within this session "bootstrap"
        this.channels.close().awaitUninterruptibly();
        // clean up all external resources
        if (this.serverChannel != null) {
            this.serverChannel.close().awaitUninterruptibly();
            this.serverChannel = null;
        }
        logger.info("{} stopped at {}:{}", configuration.getName(), configuration.getHost(), configuration.getPort());
    }
    
    @Override
    public void destroy() {
        this.bindTimer.cancel();
        stop();
        this.serverBootstrap.releaseExternalResources();
        this.serverBootstrap = null;
	this.writeTimeoutTimer.stop();
        unregisterMBean();
        logger.info("{} destroyed on SMPP port [{}]", configuration.getName(), configuration.getPort());
    }

    protected Long nextSessionId() {
        return this.sessionIdSequence.getAndIncrement();
    }

    protected byte autoNegotiateInterfaceVersion(byte requestedInterfaceVersion) {
        if (!this.configuration.isAutoNegotiateInterfaceVersion()) {
            return requestedInterfaceVersion;
        } else {
            if (requestedInterfaceVersion >= SmppConstants.VERSION_3_4) {
                return SmppConstants.VERSION_3_4;
            } else {
                // downgrade to 3.3
                return SmppConstants.VERSION_3_3;
            }
        }
    }

    protected BaseBindResp createBindResponse(BaseBind bindRequest, int statusCode) {
        BaseBindResp bindResponse = (BaseBindResp)bindRequest.createResponse();
        bindResponse.setCommandStatus(statusCode);
        bindResponse.setSystemId(configuration.getSystemId());

        // if the server supports an SMPP server version >= 3.4 AND the bind request
        // included an interface version >= 3.4, include an optional parameter with configured sc_interface_version TLV
        if (configuration.getInterfaceVersion() >= SmppConstants.VERSION_3_4 && bindRequest.getInterfaceVersion() >= SmppConstants.VERSION_3_4) {
            Tlv scInterfaceVersion = new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { configuration.getInterfaceVersion() });
            bindResponse.addOptionalParameter(scInterfaceVersion);
        }

        return bindResponse;
    }    

    protected void bindRequested(Long sessionId, SmppSessionConfiguration config, BaseBind bindRequest) throws SmppProcessingException {
        counters.incrementBindRequestedAndGet();
        // delegate request upstream to server handler
        this.serverHandler.sessionBindRequested(sessionId, config, bindRequest);
    }


    protected void createSession(Long sessionId, Channel channel, SmppSessionConfiguration config, BaseBindResp preparedBindResponse) throws SmppProcessingException {
        // NOTE: exactly one PDU (bind request) was read from the channel, we
        // now need to delegate permitting this bind request by calling a method
        // further upstream.  Only after the server-side is completely ready to
        // start processing requests from this session, do we want to actually
        // return the bind response and start reading further requests -- we'll
        // initially block reading from the channel first -- this will be turned
        // back on via the "serverReady()" method call on the session object

        // make sure the channel is not being read/processed (until we flag we're ready later on)
        channel.setReadable(false).awaitUninterruptibly();

        // auto negotiate the interface version in use based on the requested interface version
        byte interfaceVersion = this.autoNegotiateInterfaceVersion(config.getInterfaceVersion());

        // create a new server session associated with this server
        DefaultSmppSession session = new DefaultSmppSession(SmppSession.Type.SERVER, config, channel, this, sessionId, preparedBindResponse, interfaceVersion, monitorExecutor);

        // replace name of thread used for renaming
        SmppSessionThreadRenamer threadRenamer = (SmppSessionThreadRenamer)channel.getPipeline().get(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME);
        threadRenamer.setThreadName(config.getName());

        // add a logging handler after the thread renamer
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(), config.getLoggingOptions());
        channel.getPipeline().addAfter(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

	// add a writeTimeout handler after the logger
	if (config.getWriteTimeout() > 0) {
	    WriteTimeoutHandler writeTimeoutHandler = new WriteTimeoutHandler(writeTimeoutTimer, config.getWriteTimeout(), TimeUnit.MILLISECONDS);
	    channel.getPipeline().addAfter(SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, SmppChannelConstants.PIPELINE_SESSION_WRITE_TIMEOUT_NAME, writeTimeoutHandler);
	}

        // decoder in pipeline is ok (keep it)

        // create a new wrapper around a session to pass the pdu up the chain
        channel.getPipeline().remove(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME);
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));
        
        // check if the # of channels exceeds maxConnections
        if (this.channels.size() > this.configuration.getMaxConnectionSize()) {
            logger.warn("The current connection size [{}] exceeds the configured max connection size [{}]", this.channels.size(), this.configuration.getMaxConnectionSize());
        }
        
        // session created, now pass it upstream
        counters.incrementSessionCreatedAndGet();
        incrementSessionSizeCounters(session);
        this.serverHandler.sessionCreated(sessionId, session, preparedBindResponse);
        
        // register this session as an mbean
        if (configuration.isJmxEnabled()) {
            session.registerMBean(configuration.getJmxDomain() + ":type=" + configuration.getName() + "Sessions,name=" + sessionId);
        }
    }


    protected void destroySession(Long sessionId, DefaultSmppSession session) {
        // session destroyed, now pass it upstream
        counters.incrementSessionDestroyedAndGet();
        decrementSessionSizeCounters(session);
        serverHandler.sessionDestroyed(sessionId, session);
        
        // unregister this session as an mbean
        if (configuration.isJmxEnabled()) {
            session.unregisterMBean(configuration.getJmxDomain() + ":type=" + configuration.getName() + "Sessions,name=" + sessionId);
        }
    }
    
    private void incrementSessionSizeCounters(DefaultSmppSession session) {
        this.counters.incrementSessionSizeAndGet();
        switch (session.getBindType()) {
            case TRANSCEIVER:
                this.counters.incrementTransceiverSessionSizeAndGet();
                break;
            case RECEIVER:
                this.counters.incrementTransmitterSessionSizeAndGet();
                break;
            case TRANSMITTER:
                this.counters.incrementReceiverSessionSizeAndGet();
                break;
        }
    }
    
    private void decrementSessionSizeCounters(DefaultSmppSession session) {
        this.counters.decrementSessionSizeAndGet();
        switch (session.getBindType()) {
            case TRANSCEIVER:
                this.counters.decrementTransceiverSessionSizeAndGet();
                break;
            case RECEIVER:
                this.counters.decrementTransmitterSessionSizeAndGet();
                break;
            case TRANSMITTER:
                this.counters.decrementReceiverSessionSizeAndGet();
                break;
        }
    }

    // mainly for exposing via JMX
    
    @Override
    public void resetCounters() {
        this.counters.reset();
    }
    
    @Override
    public int getSessionSize() {
        return this.counters.getSessionSize();
    }
    
    @Override
    public int getTransceiverSessionSize() {
        return this.counters.getTransceiverSessionSize();
    }
    
    @Override
    public int getTransmitterSessionSize() {
        return this.counters.getTransmitterSessionSize();
    }
    
    @Override
    public int getReceiverSessionSize() {
        return this.counters.getReceiverSessionSize();
    }
    
    @Override
    public int getMaxConnectionSize() {
        return this.configuration.getMaxConnectionSize();
    }

    @Override
    public int getConnectionSize() {
        return this.channels.size();
    }

    @Override
    public long getBindTimeout() {
        return this.configuration.getBindTimeout();
    }

    @Override
    public boolean isNonBlockingSocketsEnabled() {
        return this.configuration.isNonBlockingSocketsEnabled();
    }
    
    @Override
    public boolean isReuseAddress() {
        return this.configuration.isReuseAddress();
    }

    @Override
    public int getChannelConnects() {
        return this.getCounters().getChannelConnects();
    }

    @Override
    public int getChannelDisconnects() {
        return this.getCounters().getChannelDisconnects();
    }

    @Override
    public int getBindTimeouts() {
        return this.getCounters().getBindTimeouts();
    }

    @Override
    public int getBindRequested() {
        return this.getCounters().getBindRequested();
    }

    @Override
    public int getSessionCreated() {
        return this.getCounters().getSessionCreated();
    }

    @Override
    public int getSessionDestroyed() {
        return this.getCounters().getSessionDestroyed();
    }
}
