package com.cloudhopper.smpp.impl;

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
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.netty.channel.Channel;

/**
 * Default implementation of an ESME SMPP session.
 *
 */
public class DefaultSmppServerSession extends AbstractSmppSession implements SmppServerSession {

    protected DefaultSmppServer server;
    // the session id assigned by the server to this particular instance
    protected Long serverSessionId;
    // pre-prepared BindResponse to send back once we're flagged as ready
    protected BaseBindResp preparedBindResponse;

    /**
     * Creates an SmppSession for a server-based session.
     *
     * @param localType The type of local endpoint (ESME vs. SMSC)
     * @param configuration The session configuration
     * @param channel The channel associated with this session. The channel
     * needs to already be opened.
     * @param server
     * @param serverSessionId
     * @param preparedBindResponse
     * @param interfaceVersion
     * @param monitorExecutor The executor that window monitoring and
     * potentially statistics will be periodically executed under. If null,
     * monitoring will be disabled.
     */
    public DefaultSmppServerSession(Type localType, SmppSessionConfiguration configuration, Channel channel, DefaultSmppServer server, Long serverSessionId, BaseBindResp preparedBindResponse, byte interfaceVersion, ScheduledExecutorService monitorExecutor) {
        super(localType, configuration, channel, (SmppSessionHandler) null, monitorExecutor);
        // default state for a server session is that it's binding
        this.state.set(STATE_BINDING);
        this.server = server;
        this.serverSessionId = serverSessionId;
        this.preparedBindResponse = preparedBindResponse;
        this.interfaceVersion = interfaceVersion;
    }

    @Override
    public void serverReady(SmppSessionHandler sessionHandler) {
        // properly setup the session handler (to handle notifications)
        this.sessionHandler = sessionHandler;
        // send the prepared bind response
        try {
            this.sendResponsePdu(this.preparedBindResponse);
        } catch (Exception e) {
            logger.error("{}", e);
        }
        // flag the channel is ready to read
        this.channel.setReadable(true).awaitUninterruptibly();
        this.setBound();
    }

    @Override
    public void fireChannelClosed() {
        // if this is a server session, we need to notify the server first
        // NOTE: its important this happens first
        if (this.server != null) {
            this.server.destroySession(serverSessionId, this);
        }
        super.fireChannelClosed();
    }

}
