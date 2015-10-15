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

import java.net.InetSocketAddress;
import org.jboss.netty.channel.Channel;

/**
 * Utility methods for working with Netty Channels.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class ChannelUtil {

    /**
     * Create a name for the channel based on the remote host's IP and port.
     */
    static public String createChannelName(Channel channel) {
        // check if anything is null
        if (channel == null || channel.getRemoteAddress() == null) {
            return "ChannelWasNull";
        }
        // create a channel name
        if (channel.getRemoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress)channel.getRemoteAddress();
            // just get the raw IP address
            String remoteHostAddr = addr.getAddress().getHostAddress();
            int remoteHostPort = addr.getPort();
            return remoteHostAddr + ":" + remoteHostPort;
        } else {
            return channel.getRemoteAddress().toString();
        }        
    }

    static public String getChannelRemoteHost(Channel channel) {
        if (channel == null || channel.getRemoteAddress() == null) {
            return null;
        }
        // create a channel name
        if (channel.getRemoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress)channel.getRemoteAddress();
            // just get the raw IP address
            return addr.getAddress().getHostAddress();
        }
        return null;
    }

    static public int getChannelRemotePort(Channel channel) {
        if (channel == null || channel.getRemoteAddress() == null) {
            return 0;
        }
        // create a channel name
        if (channel.getRemoteAddress() instanceof InetSocketAddress) {
            InetSocketAddress addr = (InetSocketAddress)channel.getRemoteAddress();
            // just get the raw IP address
            return addr.getPort();
        }
        return 0;
    }

}
