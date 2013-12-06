package com.cloudhopper.smpp.simulator;

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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSimulatorServer {
    private static final Logger logger = LoggerFactory.getLogger(SmppSimulatorServer.class);

    public static final String PIPELINE_SESSION_NAME = "session";

    private Channel serverChannel;
    private ChannelGroup sessionChannels;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap serverBootstrap;
    
    private SmppSimulatorServerHandler serverHandler;

//    public SmppSimulatorServer() {
//        this(Executors.newCachedThreadPool());
//    }

    public SmppSimulatorServer() {
        // used for tracking any child channels (sessions)
        this.sessionChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
        // we'll put the "boss" worker for a server in its own pool
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();

        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.channel(NioServerSocketChannel.class);
        this.serverBootstrap.group(bossGroup, workerGroup);

        // the handler to use when new child connections are accepted
        this.serverHandler = new SmppSimulatorServerHandler(this.sessionChannels);
        // set up the event pipeline factory for new connections
        this.serverBootstrap.childHandler(serverHandler);
    }
    
    public void start(int port) {
        logger.info("Simulator server starting on port " + port + "...");
        ChannelFuture f = this.serverBootstrap.bind(new InetSocketAddress(port)).syncUninterruptibly();
        serverChannel = f.channel();
        logger.info("Simulator server started");
    }

    public void stop() {
        logger.info("Closing all server session channels...");
        this.sessionChannels.close().awaitUninterruptibly();
        logger.info("Closing server channel...");
        this.serverChannel.close().awaitUninterruptibly();

        logger.info("Releasing server external resources...");
        // NOTE: all this does is "terminate()" the executors

        // Shut down all event loops to terminate all threads.
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();

        // Wait until all threads are terminated.
        try {
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
        } catch (InterruptedException e) {
            //is ok
        }

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
