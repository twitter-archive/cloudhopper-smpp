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

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.transcoder.PduTranscoderContext;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static io.netty.channel.ChannelHandler.Sharable;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@Sharable
public class SmppSimulatorServerHandler extends SimpleChannelInboundHandler<Pdu> {
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
    protected void channelRead0(ChannelHandlerContext ctx, Pdu msg) throws Exception {
        logger.info("Read message {} from channel {}", msg.toString(), ctx.channel());
        //if (msg instanceof Pdu) {
        //    Pdu pdu = (Pdu)msg;
        //    this.listener.firePduReceived(pdu);
        //}
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        logger.info("childChannelRegistered {}", ctx.channel());

        // modify its pipeline
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);

        // create a new "smsc" session instance (which is just a handler)
        SmppSimulatorSessionHandler session = new SmppSimulatorSessionHandler(ctx.channel(), transcoder);

        // add this channel's new processing pipeline
        ctx.channel().pipeline().addLast(SmppSimulatorServer.PIPELINE_SESSION_NAME, session);

        session.setPduProcessor(defaultPduProcessor);

        // store this in our internal queue
        this.sessionChannels.add(ctx.channel());
        this.sessionQueue.add(session);
    }

    /**
     * Invoked when a child {@link io.netty.channel.Channel} was closed.
     * (e.g. the accepted connection was closed)
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.info("childChannelClosed {}", ctx.channel());
        super.channelInactive(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info("childChannelOpened {}", ctx.channel());
        super.channelActive(ctx);
    }

    /**
     * Invoked when an exception was raised by an I/O thread or an upstream handler.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("Exception triggered in upstream ChannelHandler: {}", cause);
    }
}