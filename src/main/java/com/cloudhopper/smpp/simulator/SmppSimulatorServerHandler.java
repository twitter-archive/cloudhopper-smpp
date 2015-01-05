package com.cloudhopper.smpp.simulator;

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

import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.transcoder.PduTranscoderContext;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelPipelineCoverage("one")
public class SmppSimulatorServerHandler extends SimpleChannelHandler {
    private static final Logger logger = LoggerFactory.getLogger(SmppSimulatorServerHandler.class);

    private final ChannelGroup sessionChannels;
    private final BlockingQueue<SmppSimulatorSessionHandler> sessionQueue;

    private SmppSimulatorPduProcessor defaultPduProcessor;

    public SmppSimulatorServerHandler(ChannelGroup sessionChannels) {
        this.sessionChannels = sessionChannels;
        this.sessionQueue = new LinkedBlockingQueue<SmppSimulatorSessionHandler>();
    }

    public SmppSimulatorPduProcessor getDefaultPduProcessor() {
        return this.defaultPduProcessor;
    }

    public void setDefaultPduProcessor(SmppSimulatorPduProcessor pduProcessor) {
        this.defaultPduProcessor = pduProcessor;
    }

    public BlockingQueue<SmppSimulatorSessionHandler> getSessionQueue() {
        return this.sessionQueue;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
    	logger.info(e.toString());
        /**
        if (e.getMessage() instanceof Pdu) {
            Pdu pdu = (Pdu)e.getMessage();
            this.listener.firePduReceived(pdu);
    	}
         */
    }

    @Override
    public void childChannelOpen(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
        logger.info("childChannelOpen: {}", e);

        // modify its pipeline
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);

        // create a new "smsc" session instance (which is just a handler)
        SmppSimulatorSessionHandler session = new SmppSimulatorSessionHandler(e.getChildChannel(), transcoder);

        // add this channel's new processing pipeline
        e.getChildChannel().getPipeline().addLast(SmppSimulatorServer.PIPELINE_SESSION_NAME, session);

        session.setPduProcessor(defaultPduProcessor);

        // store this in our internal queue
        this.sessionChannels.add(e.getChildChannel());
        this.sessionQueue.add(session);
    }

    /**
     * Invoked when a child {@link Channel} was closed.
     * (e.g. the accepted connection was closed)
     */
    @Override
    public void childChannelClosed(ChannelHandlerContext ctx, ChildChannelStateEvent e) throws Exception {
        logger.info("childChannelClosed: {}", e);
        ctx.sendUpstream(e);
    }

    /**
     * Invoked when an exception was raised by an I/O thread or an upstream handler.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.warn("Exception triggered in upstream ChannelHandler: {}", e.getCause());
        //this.listener.fireExceptionThrown(e.getCause());
    }

    /**
     * Invoked when a Channel was disconnected from its remote peer.
     */
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
        //ctx.sendUpstream(e);
    }

    /**
     * Invoked when a Channel was unbound from the current local address.
     */
    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
    }

    /**
     * Invoked when a Channel was closed and all its related resources were released.
     */
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
        //this.listener.fireChannelClosed();
    }

    /**
     * Invoked when a Channel was disconnected from its remote peer.
     */
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
        //ctx.sendUpstream(e);
    }

    /**
     * Invoked when a Channel was unbound from the current local address.
     */
    @Override
    public void channelUnbound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
    }

    /**
     * Invoked when a Channel was closed and all its related resources were released.
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logger.info(e.toString());
        //this.listener.fireChannelClosed();
    }
}