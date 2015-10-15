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

import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.channel.ChannelUtil;
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppProcessingException;
import java.util.TimerTask;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles new channels connected via the SmppServer that are not yet properly
 * "bound" (authenticated).  An unbound session handles some of the logic to
 * authenticate a channel, then do final setups of an SmppSession, and handoff
 * a prepared session to a server handler.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class UnboundSmppSession implements SmppSessionChannelListener {
    private static final Logger logger = LoggerFactory.getLogger(UnboundSmppSession.class);

    // the channel that's not "bound" yet as an SMPP session
    private final String channelName;
    private final Channel channel;
    private final BindTimeoutTask bindTimeoutTask;
    private final DefaultSmppServer server;

    public UnboundSmppSession(String channelName, Channel channel, DefaultSmppServer server) {
        this.channelName = channelName;
        this.channel = channel;
        this.server = server;
        // schedule the timer to close the channel after X milliseconds
        this.bindTimeoutTask = new BindTimeoutTask();
        this.server.getBindTimer().schedule(bindTimeoutTask, this.server.getConfiguration().getBindTimeout());
    }

    // called when a PDU is received and decoded on the channel
    @Override
    public void firePduReceived(Pdu pdu) {
        // always log the PDU received on an unbound session
        logger.info("received PDU: {}", pdu);

        // only bind and enquire_link requests are permitted
        if (pdu instanceof BaseBind) {
            // delegate any bind request to the server handler
            // variables we track for a successful bind request
            BaseBind bindRequest = (BaseBind)pdu;

            // create a default session configuration based on this bind request
            SmppSessionConfiguration sessionConfiguration = createSessionConfiguration(bindRequest);

            // assign a new identifier for this session
            Long sessionId = server.nextSessionId();

            try {
                // delegate the bind request upstream to server handler
                this.server.bindRequested(sessionId, sessionConfiguration, bindRequest);
            } catch (SmppProcessingException e) {
                logger.warn("Bind request rejected or failed for connection [{}] with error [{}]", channelName, e.getMessage());
                // create a failed bind response and send back to connection
                BaseBindResp bindResponse = server.createBindResponse(bindRequest, e.getErrorCode());
                this.sendResponsePdu(bindResponse);
                // cancel the timer task & close connection
                closeChannelAndCancelTimer();
                return;
            }

            // if we got there then 98% "bound" -- we just need to create the
            // new session and tie everything together -- cancel the bind timer
            this.bindTimeoutTask.cancel();

            // prepare an "OK" bind response that the session will send back once flagged as 'serverReady'
            BaseBindResp preparedBindResponse = server.createBindResponse(bindRequest, SmppConstants.STATUS_OK);

            try {
                // create a new a new session and tie the bind response to it
                server.createSession(sessionId, channel, sessionConfiguration, preparedBindResponse);
            } catch (SmppProcessingException e) {
                logger.warn("Bind request was approved, but createSession failed for connection [{}] with error [{}]", channelName, e.getMessage());
                // create a failed bind response and send back to connection
                BaseBindResp bindResponse = server.createBindResponse(bindRequest, e.getErrorCode());
                this.sendResponsePdu(bindResponse);
                // cancel the timer task & close connection
                closeChannelAndCancelTimer();
                return;
            }
        } else if (pdu instanceof EnquireLink) {
            EnquireLinkResp response = ((EnquireLink) pdu).createResponse();
            logger.info("Responding to enquire_link with response [{}]", response);
            this.sendResponsePdu(response);
            return;
        } else {
            logger.warn("Only bind or enquire_link requests are permitted on new connections, closing connection [{}]", channelName);

            // FIXME: we could create a response with an error and THEN close the connection...

            // cancel the timer task & close connection
            closeChannelAndCancelTimer();
            return;
        }

    }

    public void closeChannelAndCancelTimer() {
        // if the channel is being closed, we should always make sure the timer
        // bind task is always cancelled as well
        this.bindTimeoutTask.cancel();
        // close the channel
        this.channel.close();
    }

    @Override
    public void fireExceptionThrown(Throwable t) {
        logger.warn("Exception thrown, closing connection [{}]: {}", channelName, t);
        closeChannelAndCancelTimer();
    }

    @Override
    public void fireChannelClosed() {
        logger.info("Connection closed with [{}]", channelName);
        closeChannelAndCancelTimer();
    }

    protected SmppSessionConfiguration createSessionConfiguration(BaseBind bindRequest) {
        SmppSessionConfiguration sessionConfiguration = new SmppSessionConfiguration();
        sessionConfiguration.setName("SmppServerSession." + bindRequest.getSystemId() + "." + bindRequest.getSystemType());
        sessionConfiguration.setSystemId(bindRequest.getSystemId());
        sessionConfiguration.setPassword(bindRequest.getPassword());
        sessionConfiguration.setSystemType(bindRequest.getSystemType());
        sessionConfiguration.setBindTimeout(server.getConfiguration().getBindTimeout());
        sessionConfiguration.setAddressRange(bindRequest.getAddressRange());
        sessionConfiguration.setHost(ChannelUtil.getChannelRemoteHost(channel));
        sessionConfiguration.setPort(ChannelUtil.getChannelRemotePort(channel));
        sessionConfiguration.setInterfaceVersion(bindRequest.getInterfaceVersion());

        LoggingOptions loggingOptions = new LoggingOptions();
        loggingOptions.setLogPdu(true);
        sessionConfiguration.setLoggingOptions(loggingOptions);

        // handle all 3 types...
        if (bindRequest instanceof BindTransceiver) {
            sessionConfiguration.setType(SmppBindType.TRANSCEIVER);
        } else if (bindRequest instanceof BindReceiver) {
            sessionConfiguration.setType(SmppBindType.RECEIVER);
        } else if (bindRequest instanceof BindTransmitter) {
            sessionConfiguration.setType(SmppBindType.TRANSMITTER);
        }
        
        // new default options set from server config
        sessionConfiguration.setWindowSize(server.getConfiguration().getDefaultWindowSize());
        sessionConfiguration.setWindowWaitTimeout(server.getConfiguration().getDefaultWindowWaitTimeout());
        sessionConfiguration.setWindowMonitorInterval(server.getConfiguration().getDefaultWindowMonitorInterval());
        sessionConfiguration.setRequestExpiryTimeout(server.getConfiguration().getDefaultRequestExpiryTimeout());
        sessionConfiguration.setCountersEnabled(server.getConfiguration().isDefaultSessionCountersEnabled());

        return sessionConfiguration;
    }

    public void sendResponsePdu(PduResponse pdu) {
        try {
            // encode the pdu into a buffer
            ChannelBuffer buffer = server.getTranscoder().encode(pdu);

            // always log the PDU
            logger.info("send PDU: {}", pdu);

            // write the pdu out & wait till its written
            ChannelFuture channelFuture = this.channel.write(buffer).await();

            // check if the write was a success
            if (!channelFuture.isSuccess()) {
                // the write failed, make sure to throw an exception
                throw new SmppChannelException(channelFuture.getCause().getMessage(), channelFuture.getCause());
            }
        } catch (Exception e) {
            logger.error("Fatal exception thrown while attempting to send response PDU: {}", e);
        }
    }

    /**
     * Simple task that closes a channel if its not bound within a certain time.
     */
    private final class BindTimeoutTask extends TimerTask {
        @Override
        public void run() {
            logger.warn("Channel not bound within [{}] ms, closing connection [{}]", server.getConfiguration().getBindTimeout(), channelName);
            channel.close();
            this.cancel();
            server.getCounters().incrementBindTimeoutsAndGet();
        }
    }
}
