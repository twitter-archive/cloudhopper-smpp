package com.cloudhopper.smpp.impl;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2012 Cloudhopper by Twitter
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

import com.cloudhopper.smpp.*;
import com.cloudhopper.smpp.channel.*;
import com.cloudhopper.smpp.jmx.DefaultSmppServerMXBean;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.oio.OioServerSocketChannel;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;
    private Channel serverChannel; 
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
        this(configuration, serverHandler, null,
                configuration.isNonBlockingSocketsEnabled() ? new NioEventLoopGroup() : new OioEventLoopGroup(),
                configuration.isNonBlockingSocketsEnabled() ? new NioEventLoopGroup() : new OioEventLoopGroup());
    }

    /**
     * Creates a new default SmppServer.
     * @param configuration The server configuration to create this server with
     * @param serverHandler The handler implementation for handling bind requests
     *      and creating/destroying sessions.
     * @param monitorExecutor The scheduled executor that all sessions will share
     *      to monitor themselves and expire requests. If null monitoring will
     *      be disabled.
     * @param bossGroup Specify the EventLoopGroup to accept new connections and
     *      handle accepted connections. The {@link EventLoopGroup} is used to handle
     *      all the events and IO for {@link SocketChannel}.
     * @param workerGroup The {@link EventLoopGroup} is used to handle all the events
     *      and IO for {@link Channel}.
     */
    public DefaultSmppServer(final SmppServerConfiguration configuration, SmppServerHandler serverHandler,
                             ScheduledExecutorService monitorExecutor, EventLoopGroup bossGroup,
                             EventLoopGroup workerGroup) {
        this.configuration = configuration;
        // the same group we'll put every server channel

        //NEW
        //TODO: How do we control the thread pools and executors?
        //      How do we set the max # of threads in the worker pool?
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.serverHandler = serverHandler;

        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap();

        // a factory for creating channels (connections)
        if (configuration.isNonBlockingSocketsEnabled()) {
            this.serverBootstrap.channel(NioServerSocketChannel.class);
        } else {
            this.serverBootstrap.channel(OioServerSocketChannel.class);
        }

        this.bossGroup = bossGroup;
        this.workerGroup = workerGroup;
        this.serverBootstrap.group(this.bossGroup, this.workerGroup);

        // set options for the server socket that are useful
        this.serverBootstrap.option(ChannelOption.SO_REUSEADDR, configuration.isReuseAddress());
        
        // we use the same default pipeline for all new channels - no need for a factory
        this.serverConnector = new SmppServerConnector(channels, this);

        this.serverBootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(SmppChannelConstants.PIPELINE_SERVER_CONNECTOR_NAME, serverConnector);
            }
        });

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
        //TODO is isRegistered the same as isBound
        // return (this.serverChannel != null && this.serverChannel.isBound());
        return (this.serverChannel != null && this.serverChannel.isRegistered());
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
            ChannelFuture f = this.serverBootstrap.bind(new InetSocketAddress(configuration.getPort()));

            // wait until the connection is made successfully
            boolean timeout = !f.await(configuration.getBindTimeout());

            if (timeout)
                throw new SmppChannelException("Can't bind to port " + configuration.getPort()
                        + " after " + configuration.getBindTimeout() + " milliseconds");

            if (!f.isSuccess())
                throw new SmppChannelException("Can't bind to port " + configuration.getPort()
                        + " future cause: " + f.cause());

            logger.info("{} started on SMPP port [{}]", configuration.getName(), configuration.getPort());
            serverChannel = f.channel();
        } catch (ChannelException e) {
            throw new SmppChannelException(e.getMessage(), e);
        } catch (InterruptedException e) {
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
	    try {
		/// this.serverChannel.close().awaitUninterruptibly();
		this.serverChannel.close().sync(); 
		this.serverChannel = null;
	    } catch (InterruptedException e) {
		logger.warn("Thread interrupted closing server channel.", e);
	    }
        }
        logger.info("{} stopped on SMPP port [{}]", configuration.getName(), configuration.getPort());
    }

    @Override
    public void destroy() {
        this.bindTimer.cancel();
        stop();

        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        try {
            // Wait until all threads are terminated.
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted closing executors.", e);
        }

        this.serverBootstrap = null;

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

        //TODO how do we do this in netty4?
        // channel.setReadable(false).awaitUninterruptibly();
        channel.config().setAutoRead(false);

        // auto negotiate the interface version in use based on the requested interface version
        byte interfaceVersion = this.autoNegotiateInterfaceVersion(config.getInterfaceVersion());

        // create a new server session associated with this server
        DefaultSmppSession session = new DefaultSmppSession(SmppSession.Type.SERVER, config, channel, this, sessionId, preparedBindResponse, interfaceVersion, monitorExecutor);

        // replace name of thread used for renaming
        SmppSessionThreadRenamer threadRenamer = (SmppSessionThreadRenamer)channel.pipeline().get(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME);
        threadRenamer.setThreadName(config.getName());

        // add a logging handler after the thread renamer
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(), config.getLoggingOptions());
        channel.pipeline().addAfter(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

        // add a writeTimeout handler after the logger
        if (config.getWriteTimeout() > 0) {
            WriteTimeoutHandler writeTimeoutHandler = new WriteTimeoutHandler(config.getWriteTimeout(), TimeUnit.MILLISECONDS);
            channel.pipeline().addAfter(SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, SmppChannelConstants.PIPELINE_SESSION_WRITE_TIMEOUT_NAME, writeTimeoutHandler);
        }

        // decoder in pipeline is ok (keep it)

        // create a new wrapper around a session to pass the pdu up the chain
        channel.pipeline().remove(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME);
        channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));
        
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
