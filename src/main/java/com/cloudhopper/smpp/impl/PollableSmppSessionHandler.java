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
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of an SMPP session handler that internally puts every event
 * into a queue.  Callers can then poll for these events rather than asynchronously
 * processing them.  Essentially turns an event driven SmppSession into a polling
 * version.  Mostly used in many of this framework's unit tests.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class PollableSmppSessionHandler implements SmppSessionHandler {

    private final BlockingQueue<PduRequest> receivedPduRequests;
    private final BlockingQueue<PduAsyncResponse> receivedExpectedPduResponses;
    private final BlockingQueue<PduResponse> receivedUnexpectedPduResponses;
    private final BlockingQueue<Throwable> throwables;
    private final AtomicInteger closedCount;

    public PollableSmppSessionHandler() {
        this.receivedPduRequests = new LinkedBlockingQueue<PduRequest>();
        this.receivedExpectedPduResponses = new LinkedBlockingQueue<PduAsyncResponse>();
        this.receivedUnexpectedPduResponses = new LinkedBlockingQueue<PduResponse>();
        this.throwables = new LinkedBlockingQueue<Throwable>();
        this.closedCount = new AtomicInteger();
    }

    public BlockingQueue<PduRequest> getReceivedPduRequests() {
        return this.receivedPduRequests;
    }

    public BlockingQueue<PduAsyncResponse> getReceivedExpectedPduResponses() {
        return this.receivedExpectedPduResponses;
    }

    public BlockingQueue<PduResponse> getReceivedUnexpectedPduResponses() {
        return this.receivedUnexpectedPduResponses;
    }

    public BlockingQueue<Throwable> getThrowables() {
        return this.throwables;
    }

    public int getClosedCount() {
        return this.closedCount.get();
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
    public PduResponse firePduRequestReceived(PduRequest pduRequest) {
        this.receivedPduRequests.add(pduRequest);
        return null;
    }

    @Override
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse) {
        this.receivedExpectedPduResponses.add(pduAsyncResponse);
    }

    @Override
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse) {
        this.receivedUnexpectedPduResponses.add(pduResponse);
    }

    @Override
    public void fireUnrecoverablePduException(UnrecoverablePduException e) {
        this.throwables.add(e);
    }

    @Override
    public void fireRecoverablePduException(RecoverablePduException e) {
        this.throwables.add(e);
    }

    @Override
    public void fireUnknownThrowable(Throwable t) {
        this.throwables.add(t);
    }

    @Override
    public void fireChannelUnexpectedlyClosed() {
        this.closedCount.incrementAndGet();
    }

    @Override
    public void firePduRequestExpired(PduRequest pduRequest) {
        // do nothing
    }
    
}
