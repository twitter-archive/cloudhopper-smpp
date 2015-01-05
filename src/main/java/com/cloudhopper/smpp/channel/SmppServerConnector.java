package com.cloudhopper.smpp.channel;

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


import com.cloudhopper.smpp.impl.DefaultSmppServer;
import com.cloudhopper.smpp.impl.UnboundSmppSession;
import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.ssl.SslContextFactory;
import javax.net.ssl.SSLEngine;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler for server SMPP sessions.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelPipelineCoverage("all")
public class SmppServerConnector extends SimpleChannelUpstreamHandler {
    private static final Logger logger = LoggerFactory.getLogger(SmppServerConnector.class);

    // reference to every channel connected via this server channel
    private ChannelGroup channels;
    private DefaultSmppServer server;

    public SmppServerConnector(ChannelGroup channels, DefaultSmppServer server) {
        this.channels = channels;
        this.server = server;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // the channel we are going to handle
        Channel channel = e.getChannel();

        // always add it to our channel group
        channels.add(channel);
        this.server.getCounters().incrementChannelConnectsAndGet();

        // create a default "unbound" thread name for the thread processing the channel
        // this will create a name of "RemoteIPAddress.RemotePort"
        String channelName = ChannelUtil.createChannelName(channel);
        String threadName = server.getConfiguration().getName() + ".UnboundSession." + channelName;

        // rename the current thread for logging, then rename it back
        String currentThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(server.getConfiguration().getName());
        logger.info("New channel from [{}]", channelName);
        Thread.currentThread().setName(currentThreadName);

	// add SSL handler
        if (server.getConfiguration().isUseSsl()) {
	    SslConfiguration sslConfig = server.getConfiguration().getSslConfiguration();
	    if (sslConfig == null) throw new IllegalStateException("sslConfiguration must be set");
	    SslContextFactory factory = new SslContextFactory(sslConfig);
	    SSLEngine sslEngine = factory.newSslEngine();
	    sslEngine.setUseClientMode(false);
	    channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_SSL_NAME, new SslHandler(sslEngine));
	}

        // add a new instance of a thread renamer
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_THREAD_RENAMER_NAME, new SmppSessionThreadRenamer(threadName));
        
        // add a new instance of a decoder (that takes care of handling frames)
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_PDU_DECODER_NAME, new SmppSessionPduDecoder(server.getTranscoder()));

        // create a new wrapper around an "unbound" session to pass the pdu up the chain
        UnboundSmppSession session = new UnboundSmppSession(channelName, channel, server);
        channel.getPipeline().addLast(SmppChannelConstants.PIPELINE_SESSION_WRAPPER_NAME, new SmppSessionWrapper(session));
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // called every time a channel disconnects
        channels.remove(e.getChannel());
        this.server.getCounters().incrementChannelDisconnectsAndGet();
    }

}
