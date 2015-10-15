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


import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelUpstreamHandler;

/**
 * Channel handler responsible for renaming the current thread, passing the
 * event upstream, then renaming the thread back after its done processing. This
 * handler should be the first one in the pipeline to make sure all handlers
 * after it have the correct thread name for proper logging.
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
@ChannelPipelineCoverage("one")
public class SmppSessionThreadRenamer implements ChannelUpstreamHandler {

    private String threadName;

    public SmppSessionThreadRenamer(String threadName) {
        this.threadName = threadName;
    }

    public String getThreadName() {
        return this.threadName;
    }

    public void setThreadName(String value) {
        this.threadName = value;
    }

    @Override
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        // always rename the current thread and then rename it back
        String currentThreadName = Thread.currentThread().getName();
        Thread.currentThread().setName(threadName);
        ctx.sendUpstream(e);
        Thread.currentThread().setName(currentThreadName);
    }
}
