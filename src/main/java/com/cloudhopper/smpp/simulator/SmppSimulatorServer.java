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
import io.netty.channel.*;
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
    
    private final SmppSimulatorServerHandler serverHandler;

//    public SmppSimulatorServer() {
//        this(Executors.newCachedThreadPool());
//    }

    public SmppSimulatorServer() {
        // used for tracking any child channels (sessions)
        this.sessionChannels = new DefaultChannelGroup("simulatorServer", GlobalEventExecutor.INSTANCE);
        // we'll put the "boss" worker for a server in its own pool
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        // a factory for creating channels (connections)
        // tie the server bootstrap to this server socket channel factory
        this.serverBootstrap = new ServerBootstrap();
        this.serverBootstrap.channel(NioServerSocketChannel.class);
        this.serverBootstrap.group(bossGroup, workerGroup);

        // the handler to use when new child connections are accepted
        this.serverHandler = new SmppSimulatorServerHandler(this.sessionChannels);
        // set up the event pipeline factory for new connections
        this.serverBootstrap.childHandler(serverHandler);
	this.serverBootstrap.handler(new ChannelInboundHandlerAdapter() {
		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		    serverHandler.channelRead(ctx, msg);
		}
	    });
    }
    
    public void start(int port) {
        logger.info("Simulator server starting on port " + port + "...");
        ChannelFuture f = this.serverBootstrap.bind(new InetSocketAddress(port)).syncUninterruptibly();
        serverChannel = f.channel();
        logger.info("Simulator server started");
    }

    public void stop() {
        try {
            logger.info("Closing all server session channels...");
            this.sessionChannels.close().sync();
            logger.info("Closing server channel...");
            this.serverChannel.close().sync();
            this.serverBootstrap = null;
        } catch (InterruptedException e) {
            logger.warn("Thread interrupted closing server channel.", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            try {
                // Wait until all threads are terminated.
                bossGroup.terminationFuture().sync();
                workerGroup.terminationFuture().sync();
            } catch (InterruptedException e) {
                logger.warn("Thread interrupted closing executors.", e);
            }
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
