package com.cloudhopper.smpp.channel;

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

import com.cloudhopper.smpp.impl.SmppSessionChannelListener;
import com.cloudhopper.smpp.pdu.Pdu;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelHandler.Sharable
public class SmppSessionWrapper extends SimpleChannelInboundHandler<Pdu> {
    private static final Logger logger = LoggerFactory.getLogger(SmppSessionWrapper.class);

    private SmppSessionChannelListener listener;

    public SmppSessionWrapper(SmppSessionChannelListener listener) {
        this.listener = listener;
    }
    
    // @Override
    // public void messageReceived(ChannelHandlerContext ctx, Pdu pdu) throws Exception {
    @Override
    public void channelRead0(ChannelHandlerContext ctx, Pdu pdu) throws Exception {
	this.listener.firePduReceived(pdu);
    }
    
    /**
     * Invoked when an exception was raised by an I/O thread or an upstream handler.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        //logger.warn("Exception triggered in upstream ChannelHandler: {}", e.getCause());
        this.listener.fireExceptionThrown(e);
    }

    /**
     * Invoked when a Channel was closed and all its related resources were released.
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //logger.info(e.toString());
        this.listener.fireChannelClosed();
    }
}
