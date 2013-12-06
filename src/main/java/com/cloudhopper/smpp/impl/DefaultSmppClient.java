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
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import com.cloudhopper.smpp.type.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.net.InetSocketAddress;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Default implementation to "bootstrap" client SMPP sessions (create & bind).
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppClient implements SmppClient {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppClient.class);

    private final ChannelGroup channels;
    private final SmppClientConnector clientConnector;
    private Bootstrap clientBootstrap;
    private final NioEventLoopGroup workerGroup;
    private final ScheduledExecutorService monitorExecutor;

    /**
     * Creates a new default SmppClient. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * The maximum number of IO worker threads across any client sessions
     * created with this SmppClient will be Runtime.getRuntime().availableProcessors().
     * An Executors.newCachedDaemonThreadPool will be used for IO worker threads.
     */
    public DefaultSmppClient() {
        this(new NioEventLoopGroup());
    }

    /**
     * Creates a new default SmppClient. Window monitoring and automatic
     * expiration of requests will be disabled with no monitorExecutors.
     * The maximum number of IO worker threads across any client sessions
     * created with this SmppClient will be Runtime.getRuntime().availableProcessors().
     * @param workerGroup The {@link EventLoopGroup} which is used to handle all the events
     *     for the to-be-creates {@link Channel}. The max threads will never grow more
     *     than expectedSessions if NIO sockets are used.
     */
    public DefaultSmppClient(NioEventLoopGroup workerGroup) {
        this(workerGroup, null);
    }
    
    /**
     * Creates a new default SmppClient.
     * @param workerGroup The max number of concurrent sessions expected
     *      to be active at any time.  This number controls the max number of worker
     *      threads that the underlying Netty library will use.  If processing
     *      occurs in a sessionHandler (a blocking op), be <b>VERY</b> careful
     *      setting this to the correct number of concurrent sessions you expect.
     * @param monitorExecutor The scheduled executor that all sessions will share
     *      to monitor themselves and expire requests.  If null monitoring will
     *      be disabled.
     */
    public DefaultSmppClient(NioEventLoopGroup workerGroup, ScheduledExecutorService monitorExecutor) {
        this.channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        this.workerGroup = workerGroup;
        this.clientBootstrap = new Bootstrap();
        this.clientBootstrap.group(this.workerGroup);
        this.clientBootstrap.channel(NioSocketChannel.class);
        // we use the same default pipeline for all new channels - no need for a factory
        this.clientConnector = new SmppClientConnector(this.channels);

        this.clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(SmppChannelConstants.PIPELINE_CLIENT_CONNECTOR_NAME, clientConnector);
            }
        });

        this.monitorExecutor = monitorExecutor;
    }
    
    public int getConnectionSize() {
        return this.channels.size();
    }

    @Override
    public void destroy() {
        // close all channels still open within this session "bootstrap"
        this.channels.close().awaitUninterruptibly();
        // @todo clean up all external resources
        //this.workerGroup.shutdownGracefully();//??? close all channels in event loop?
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
                channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
            } catch (Exception e) {
                throw new SmppChannelConnectException("Unable to create SSL session]: " + e.getMessage(), e);
            }
        }

        // add the thread renamer portion to the pipeline
        if (config.getName() != null) {
            channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, new SmppSessionThreadRenamer(config.getName()));
        } else {
            logger.warn("Session configuration did not have a name set - skipping threadRenamer in pipeline");
        }

        // create the logging handler (for bytes sent/received on wire)
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(), config.getLoggingOptions());
        channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

        // add a new instance of a decoder (that takes care of handling frames)
        channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME, new SmppSessionPduDecoder(session.getTranscoder()));

        // create a new wrapper around a session to pass the pdu up the chain
        channel.pipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));

        return session;
    }

    protected Channel createConnectedChannel(String host, int port, long connectTimeoutMillis) throws SmppTimeoutException, SmppChannelException, InterruptedException {
        // a socket address used to "bind" to the remote system
        InetSocketAddress socketAddr = new InetSocketAddress(host, port);

        // attempt to connect to the remote system
        ChannelFuture connectFuture = this.clientBootstrap.connect(socketAddr);
        
        // wait until the connection is made successfully
        boolean timeout = !connectFuture.await(connectTimeoutMillis);

        if (timeout) {
            throw new SmppChannelConnectTimeoutException("Unable to connect to host [" + host + "] and port [" + port + "] within " + connectTimeoutMillis + " ms");
        }

        if (!connectFuture.isSuccess()) {
            throw new SmppChannelConnectException("Unable to connect to host [" + host + "] and port [" + port + "]: " + connectFuture.cause().getMessage(), connectFuture.cause());
        }

        // if we get here, then we were able to connect and get a channel
        return connectFuture.channel();
    }

}
