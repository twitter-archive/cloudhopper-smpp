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

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSimulatorServer {
    private static final Logger logger = LoggerFactory.getLogger(SmppSimulatorServer.class);

    public static final String PIPELINE_SESSION_NAME = "session";

    private Channel serverChannel;
    private ChannelGroup sessionChannels;
    private ExecutorService bossThreadPool;
    private NioServerSocketChannelFactory channelFactory;
    private ServerBootstrap serverBootstrap;
    
    private SmppSimulatorServerHandler serverHandler;

    public SmppSimulatorServer() {
        this(Executors.newCachedThreadPool());
    }

    public SmppSimulatorServer(ExecutorService executors) {
        // used for tracking any child channels (sessions)
        this.sessionChannels = new DefaultChannelGroup();
        // we'll put the "boss" worker for a server in its own pool
        this.bossThreadPool = Executors.newCachedThreadPool();
        // a factory for creating channels (connections)
        this.channelFactory = new NioServerSocketChannelFactory(this.bossThreadPool, executors);
        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap(this.channelFactory);
        // the handler to use when new child connections are accepted
        this.serverHandler = new SmppSimulatorServerHandler(this.sessionChannels);
        // set up the event pipeline factory for new connections
        this.serverBootstrap.setParentHandler(serverHandler);
    }
    
    public void start(int port) {
        logger.info("Simulator server starting on port " + port + "...");
        serverChannel = this.serverBootstrap.bind(new InetSocketAddress(port));
        logger.info("Simulator server started");
    }

    public void stop() {
        logger.info("Closing all server session channels...");
        this.sessionChannels.close().awaitUninterruptibly();
        logger.info("Closing server channel...");
        this.serverChannel.close().awaitUninterruptibly();

        logger.info("Releasing server external resources...");
        // NOTE: all this does is "terminate()" the executors
        this.serverBootstrap.releaseExternalResources();

        logger.info("Simulator server stopped");
    }

    public SmppSimulatorServerHandler getHandler() {
        return this.serverHandler;
    }

    public SmppSimulatorSessionHandler pollNextSession(long timeoutInMillis) throws Exception {
        SmppSimulatorSessionHandler session = this.serverHandler.getSessionQueue().poll(timeoutInMillis, TimeUnit.MILLISECONDS);
        if (session == null) {
            throw new Exception("No session created within timeout");
        }
        return session;
    }

}
