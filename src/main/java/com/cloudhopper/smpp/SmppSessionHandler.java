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

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.transcoder.PduTranscoderContext;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * Handles events received on an SmppSession.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppSessionHandler extends PduTranscoderContext {

    /**
     * Called when the underlying channel of a session has been closed and it
     * wasn't at the request of our side.  This will either indicate the remote
     * system closed the socket OR the connection dropped in-between.  If the
     * session's actual "close" method was called, this won't be triggered.
     */
    public void fireChannelUnexpectedlyClosed();

    /**
     * Called when a request PDU such as a "DeliverSM" has been received on a
     * session.  This method provides a simply way to return a response PDU.
     * If a non-null response PDU is returned, this pdu will be sent
     * back on the session's channel.  If the response PDU is null, then no
     * response will be sent back and its up to the implementation to send
     * back the response instead.
     * @param pduRequest The request PDU received on this session
     * @return The response PDU to send back OR null if no response should be
     *      returned.
     */
    public PduResponse firePduRequestReceived(PduRequest pduRequest);

    /**
     * Called when a request PDU has not received an associated response within
     * the expiry time.  Usually, this means the request should be retried.
     * @param pduRequest The request PDU received on this session
     */
    public void firePduRequestExpired(PduRequest pduRequest);

    /**
     * Called when a response PDU is received for a previously sent request PDU.
     * Only "expected" responses are passed to this method. An "expected" response
     * is a response that matches a previously sent request.  Both the original
     * request and the response along with other info is passed to this method.
     * <BR>
     * NOTE: If another thread is "waiting" for a response, that thread will
     * receive it vs. this method.  This method will only receive expected
     * responses that were either sent "asynchronously" or received after the
     * originating thread timed out while waiting for a response.
     * @param pduAsyncResponse The "expected" response PDU received on this session
     */
    public void fireExpectedPduResponseReceived(PduAsyncResponse pduAsyncResponse);

    /**
     * Called when a response PDU is received for a request this session never sent.
     * Only "unexpected" responses are passed to this method. An "unexpected" response
     * is a response that does NOT match a previously sent request.  That can
     * either happen because it really is an invalid response OR another thread
     * that originated the request "cancelled" it.  Cancelling is VERY uncommon
     * so an invalid response is more likely.
     * @param pduResponse The "unexpected" response PDU received on this session
     */
    public void fireUnexpectedPduResponseReceived(PduResponse pduResponse);

    /**
     * Called when an "unrecoverable" exception has been thrown downstream in
     * the session's pipeline.  The best example is a PDU that has an impossible
     * sequence number.  The recommended action is almost always to close the
     * session and attempt to rebind at a later time.
     * @param e The exception
     */
    public void fireUnrecoverablePduException(UnrecoverablePduException e);

    /**
     * Called when a "recoverable" exception has been thrown downstream in
     * the session's pipeline.  The best example is a PDU that may have been
     * missing some fields such as NULL byte.  A "recoverable" exception always
     * includes a "PartialPdu" which always contains enough information to
     * create a "NACK" back.  That's the recommended behavior of implementations --
     * to trigger a GenericNack for PduRequests.
     * @param e The exception
     */
    public void fireRecoverablePduException(RecoverablePduException e);

    /**
     * Called when any exception/throwable has been thrown downstream in
     * the session's pipeline that wasn't of the types: UnrecoverablePduException
     * or RecoverablePduException.
     * @param e The exception
     */
    public void fireUnknownThrowable(Throwable t);

}
