package com.cloudhopper.smpp;

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

import com.cloudhopper.smpp.type.SmppChannelException;
import org.jboss.netty.channel.group.ChannelGroup;

/**
 * Interface representing an SmppServer.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppServer {
    
    /**
     * Returns true if the SMPP server is started.
     * @return True if started, otherwise false.
     */
    public boolean isStarted();
    
    /**
     * Returns true if the SMPP server is stopped.
     * @return True if stopped, otherwise false.
     */
    public boolean isStopped();
    
    /**
     * Returns true if the SMPP server is destroyed.
     * @return True if destroyed, otherwise false.
     */
    public boolean isDestroyed();

    /**
     * Starts the SMPP server. Binds all server socket connectors to configured
     * ports.
     */
    public void start() throws SmppChannelException;

    /**
     * Stops the SMPP server. Closes all child sockets and then closes all server
     * socket connectors by unbinding them from ports. Once stopped, the server
     * can be started again.  If a server will no longer be used, please follow
     * a call to stop by calling {@see #shutdown()}
     */
    public void stop();
    
    /**
     * Destroys the SMPP server. Ensures the server is first stopped, then
     * releases all resources, and unregisters from JMX (if it was enabled).
     */
    public void destroy();

    public ChannelGroup getChannels();
    
    public SmppServerCounters getCounters();

}
