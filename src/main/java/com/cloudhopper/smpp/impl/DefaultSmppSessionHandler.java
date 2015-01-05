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

import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.SmppSessionListener;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.nio.channels.ClosedChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation that provides empty implementations of all required
 * methods.  Users are free to subclass this class to only override the methods
 * they require to specialize.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppSessionHandler implements SmppSessionListener {
    private final Logger logger;

    public DefaultSmppSessionHandler() {
        this(LoggerFactory.getLogger(DefaultSmppSessionHandler.class));
    }

    public DefaultSmppSessionHandler(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String lookupResultMessage(int commandStatus) {
        return null;
    }

    @Override
    public String lookupTlvTagName(short tag) {
        return null;
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        logger.info("Default handling is to discard an unexpected channel closed");
    }

    @Override
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        logger.warn("Default handling is to discard unexpected request PDU: {}", pduRequest);
        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        logger.warn("Default handling is to discard expected response PDU: {}", pduAsyncResponse);
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
        logger.warn("Default handling is to discard unexpected response PDU: {}", pduResponse);
    }

    @Override
    public void fireUnrecoverablePduException(UnrecoverablePduException e) {
        logger.warn("Default handling is to discard a unrecoverable exception:", e);
    }

    @Override
    public void fireRecoverablePduException(RecoverablePduException e) {
        logger.warn("Default handling is to discard a recoverable exception:", e);
    }

    @Override
    public void fireUnknownThrowable(Throwable t) {
        if (t instanceof ClosedChannelException) {
            logger.warn("Unknown throwable received, but it was a ClosedChannelException, calling fireChannelUnexpectedlyClosed instead");
            fireChannelUnexpectedlyClosed();
        } else {
            logger.warn("Default handling is to discard an unknown throwable:", t);
        }
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        logger.warn("Default handling is to discard expired request PDU: {}", pduRequest);
    }

    @Override
    public boolean firePduReceived(Pdu pdu) {
        // default handling is to accept pdu for processing up chain
        return true;
    }

    @Override
    public boolean firePduDispatch(Pdu pdu) {
        // default handling is to accept pdu for processing up chain
        return true;
    }
    
}
