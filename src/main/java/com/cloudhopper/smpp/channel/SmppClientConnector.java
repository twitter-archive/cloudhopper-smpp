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

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The default handler used just during a "connect" for a Channel when attempting
 * to bind into an SMSC.  The reason is that some handler must be added to create
 * a channel and then replaced before the actual "bind" is attempted.  This
 * handler basically does nothing.  Until a "bind" request is received by the SMSC,
 * nothing should actually be received so its safe to have a handler that does
 * nothing.
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelPipelineCoverage("all")
public class SmppClientConnector extends SimpleChannelUpstreamHandler {
    private static final Logger logger = LoggerFactory.getLogger(SmppClientConnector.class);

    private ChannelGroup channels;

    public SmppClientConnector(ChannelGroup channels) {
        this.channels = channels;
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // called every time a new channel connects
        channels.add(e.getChannel());
    }

    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // called every time a channel disconnects
        channels.remove(e.getChannel());
    }

    /**
     * Invoked when an exception was raised by an I/O thread or an upstream handler.
     * NOTE: Not implementing this causes annoying log statements to STDERR
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        // the client smpp implementation relies on this to catch errors upstream
        // however, during a connect sequence, we don't have any upstream handlers
        // yet and the framework logged the exceptions to STDERR causing issues
        // on the console.  So, we'll implement a default handling of it here
        // where we just pass it further upstream and basically discard it
        ctx.sendUpstream(e);
    }
}