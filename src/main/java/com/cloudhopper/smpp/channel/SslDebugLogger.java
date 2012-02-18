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

package com.cloudhopper.smpp.channel;

import org.jboss.netty.buffer.ChannelBuffer;
import static org.jboss.netty.buffer.ChannelBuffers.hexDump;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler responsible for logging the bytes sent/received on an 
 * SmppSession.  The internal "options" object is tied directly to the SmppSession
 * so that changes can be made on-the-fly during runtime.
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelPipelineCoverage("one")
public class SslDebugLogger extends SimpleChannelHandler {
    private final Logger logger = LoggerFactory.getLogger(SslDebugLogger.class);

    /**
     * Logs the specified event to the {@link InternalLogger} returned by
     * {@link #getLogger()}. If hex dump has been enabled for this handler,
     * the hex dump of the {@link ChannelBuffer} in a {@link MessageEvent} will
     * be logged together.
     */
    public void log(String direction, ChannelEvent e) {
        String msg = e.toString();

        // Append hex dump if necessary.
        if (e instanceof MessageEvent) {
            MessageEvent me = (MessageEvent) e;
            if (me.getMessage() instanceof ChannelBuffer) {
                ChannelBuffer buf = (ChannelBuffer) me.getMessage();
                msg = msg + " - (HEXDUMP: " + hexDump(buf) + ')';
            }
        }

        // Log the message (and exception if available.)
        if (e instanceof ExceptionEvent) {
            logger.info(direction + ": " + msg, ((ExceptionEvent) e).getCause());
        } else {
            logger.info(direction + ": " + msg);
        }
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
            throws Exception {
        log("upstream", e);
        super.handleUpstream(ctx, e);
    }

    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e)
            throws Exception {
        log("downstream", e);
        super.handleDownstream(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        log("channelClosed", e);
    }
    
    @Override
    public void channelDisconnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        log("channelDisconnected", e);
    }
}
