/**
 * Copyright (C) 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.cloudhopper.smpp.impl;

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
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.SmppProcessingException;
import com.cloudhopper.smpp.util.DaemonExecutors;
import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of an SmppServer that supports SMPP version 3.3 and 3.4.
 * 
 * @author joelauer
 */
public class DefaultSmppServer implements SmppServer {
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

    // shared instance of a timer background thread to close unbound channels
    private final Timer bindTimer;
    // shared instance of a session id generator (an atomic long)
    private final AtomicLong sessionIdSequence;
    
    public DefaultSmppServer(SmppServerConfiguration configuration, SmppServerHandler serverHandler) {
        this(configuration, DaemonExecutors.newCachedDaemonThreadPool(), serverHandler);
    }

    public DefaultSmppServer(final SmppServerConfiguration configuration, ExecutorService executors, SmppServerHandler serverHandler) {
        this.configuration = configuration;
        // the same group we'll put every server channel
        this.channels = new DefaultChannelGroup();
        this.serverHandler = serverHandler;
        // we'll put the "boss" worker for a server in its own pool
        this.bossThreadPool = Executors.newCachedThreadPool();
        // a factory for creating channels (connections)
        this.channelFactory = new NioServerSocketChannelFactory(this.bossThreadPool, executors);
        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap(this.channelFactory);
        // we use the same default pipeline for all new channels - no need for a factory
        this.serverConnector = new SmppServerConnector(channels, this);
        this.serverBootstrap.getPipeline().addLast(SmppChannelConstants.PIPELINE_SERVER_CONNECTOR_NAME, this.serverConnector);
        // a shared timer used to make sure new channels are bound within X milliseconds
        this.bindTimer = new Timer(configuration.getName() + "-BindTimer0", true);
        // NOTE: this would permit us to customize the "transcoding" context for a server if needed
        this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext());
        this.sessionIdSequence = new AtomicLong(0);
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

    public Timer getBindTimer() {
        return this.bindTimer;
    }
    
    @Override
    public void start() {
        serverChannel = this.serverBootstrap.bind(new InetSocketAddress(configuration.getPort()));
    }

    @Override
    public void stop() {
        // close all channels still open within this session "bootstrap"
        this.channels.close().awaitUninterruptibly();
        // clean up all external resources
        if (this.serverChannel != null) {
            this.serverChannel.close().awaitUninterruptibly();
        }
        this.serverBootstrap.releaseExternalResources();
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
        DefaultSmppSession session = new DefaultSmppSession(SmppSession.Type.SERVER, config, channel, this, sessionId, preparedBindResponse, interfaceVersion);

        // replace name of thread used for renaming
        SmppSessionThreadRenamer threadRenamer = (SmppSessionThreadRenamer)channel.getPipeline().get(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME);
        threadRenamer.setThreadName(config.getName());

        // add a logging handler after the thread renamer
        SmppSessionLogger loggingHandler = new SmppSessionLogger(DefaultSmppSession.class.getCanonicalName(), config.getLoggingOptions());
        channel.getPipeline().addAfter(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, SmppChannelConstants.PIPELINE_SESSION_LOGGER_NAME, loggingHandler);

        // decoder in pipeline is ok (keep it)

        // create a new wrapper around a session to pass the pdu up the chain
        channel.getPipeline().remove(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME);
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));

        // session created, now pass it upstream
        this.serverHandler.sessionCreated(sessionId, session, preparedBindResponse);
    }


    protected void destroySession(Long sessionId, DefaultSmppSession session) {
        // session destroyed, now pass it upstream
        serverHandler.sessionDestroyed(sessionId, session);
    }
}
