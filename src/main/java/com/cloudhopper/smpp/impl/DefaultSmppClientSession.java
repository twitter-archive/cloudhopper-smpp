package com.cloudhopper.smpp.impl;

import com.cloudhopper.smpp.SmppClientSession;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SmppSessionUtil;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.netty.channel.Channel;

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
/**
 * Default implementation of and SMSC SMPP session.
 *
 */
public class DefaultSmppClientSession extends AbstractSmppSession implements SmppClientSession {

    /**
     * Creates an SmppSession for a client-based session. It is <b>NOT</b>
     * recommended that this constructor is called directly. The recommended way
     * to construct a session is either via a DefaultSmppClient or
     * DefaultSmppServer. This constructor will cause monitoring to be disabled.
     *
     * @param localType The type of local endpoint (ESME vs. SMSC)
     * @param configuration The session configuration
     * @param channel The channel associated with this session. The channel
     * needs to already be opened.
     * @param sessionHandler The handler for session events
     */
    public DefaultSmppClientSession(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppSessionHandler sessionHandler) {
        super(localType, configuration, channel, sessionHandler, null);
    }

    /**
     * Creates an SmppSession for a client-based session. It is <b>NOT</b>
     * recommended that this constructor is called directly. The recommended way
     * to construct a session is either via a DefaultSmppClient or
     * DefaultSmppServer.
     *
     * @param localType The type of local endpoint (ESME vs. SMSC)
     * @param configuration The session configuration
     * @param channel The channel associated with this session. The channel
     * needs to already be opened.
     * @param sessionHandler The handler for session events
     * @param monitorExecutor The executor that window monitoring and
     * potentially statistics will be periodically executed under. If null,
     * monitoring will be disabled.
     */
    public DefaultSmppClientSession(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppSessionHandler sessionHandler, ScheduledExecutorService monitorExecutor) {
        super(localType, configuration, channel, sessionHandler, monitorExecutor);
    }

    @Override
    public BaseBindResp bind(BaseBind request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppBindException, SmppTimeoutException, SmppChannelException, InterruptedException {
        assertValidRequest(request);
        boolean bound = false;
        try {
            this.state.set(STATE_BINDING);

            PduResponse response = sendRequestAndGetResponse(request, timeoutInMillis);
            SmppSessionUtil.assertExpectedResponse(request, response);
            BaseBindResp bindResponse = (BaseBindResp) response;

            // check if the bind succeeded
            if (bindResponse == null || bindResponse.getCommandStatus() != SmppConstants.STATUS_OK) {
                // bind failed for a specific reason
                throw new SmppBindException(bindResponse);
            }

            // if we make it all the way here, we're good and bound
            bound = true;

            //
            // negotiate version in use based on response back from server
            //
            Tlv scInterfaceVersion = bindResponse.getOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION);

            if (scInterfaceVersion == null) {
                // this means version 3.3 is in use
                this.interfaceVersion = SmppConstants.VERSION_3_3;
            } else {
                try {
                    byte tempInterfaceVersion = scInterfaceVersion.getValueAsByte();
                    if (tempInterfaceVersion >= SmppConstants.VERSION_3_4) {
                        this.interfaceVersion = SmppConstants.VERSION_3_4;
                    } else {
                        this.interfaceVersion = SmppConstants.VERSION_3_3;
                    }
                } catch (TlvConvertException e) {
                    logger.warn("Unable to convert sc_interface_version to a byte value: {}", e.getMessage());
                    this.interfaceVersion = SmppConstants.VERSION_3_3;
                }
            }

            return bindResponse;
        } finally {
            if (bound) {
                // this session is now successfully bound & ready for processing
                setBound();
            } else {
                // the bind failed, we need to clean up resources
                try {
                    this.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    public SubmitSmResp submit(SubmitSm request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        assertValidRequest(request);
        PduResponse response = sendRequestAndGetResponse(request, timeoutInMillis);
        SmppSessionUtil.assertExpectedResponse(request, response);
        return (SubmitSmResp) response;
    }

}
