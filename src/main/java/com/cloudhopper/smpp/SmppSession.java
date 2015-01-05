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

import com.cloudhopper.commons.util.windowing.Window;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * Defines a common interface for either a Client (ESME) or Server (SMSC) SMPP
 * session.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppSession {

    /**
     * The type of SMPP session.  Will be either Server (SMSC) or Client (ESME).
     * Each side of the session has to be either one of these two.  It determines
     * which types of requests are possible to send to the other side.
     */
    public enum Type {
        SERVER,
        CLIENT
    }

    /** Session is in an initial state (not connected, but created) */
    static public final int STATE_INITIAL = 0;
    /** Session is connected and a bind is pending */
    static public final int STATE_OPEN = 1;
    /** Session is in the process of binding.  If local is ESME, we sent the bind request.
     If the local is SMSC, then the ESME initiated a bind request, but we have't responded yet. */
    static public final int STATE_BINDING = 2;
    /** Session is bound (ready to process requests) */
    static public final int STATE_BOUND = 3;
    /** Session is in the process of un-binding. This may have been initiated by us or them. */
    static public final int STATE_UNBINDING = 4;
    /** Session is unbound and closed (destroyed) */
    static public final int STATE_CLOSED = 5;
    /** Descriptions of each state */
    static public final String[] STATES = {
        "INITIAL", "OPEN", "BINDING", "BOUND", "UNBINDING", "CLOSED"
    };

    /**
     * Gets the type of bind for this session such as "transceiver", "receiver",
     * or "transmitter".
     * @return The type of bind for this session
     */
    public SmppBindType getBindType();

    /**
     * Gets the session type of the local system.  If the local type is ESME,
     * then we are connected to an SMSC.  We are permitted to send submit_sm or
     * data_sm requests.
     * 
     * @return The session type of the local system
     */
    public Type getLocalType();

    /**
     * Gets the session type of the remote system.  If the remote type is SMSC,
     * then we are the ESME.  We are permitted to send submit_sm or data_sm requests.
     *
     * @return The session type of the remote system
     */
    public Type getRemoteType();

    /**
     * Gets the configuration associated with this session.
     * @return The session configuration
     */
    public SmppSessionConfiguration getConfiguration();

    /**
     * Gets the name of the current state of the session.
     * @return The current state of the session by name such as "CLOSED"
     */
    public String getStateName();

    /**
     * Gets the interface version currently in use between local and remote
     * endpoints.  This interface version is negotiated during the bind process
     * to mainly ensure that optional parameters are supported.
     * @return The interface version currently in use between local and remote
     *      endpoints.
     */
    public byte getInterfaceVersion();

    /**
     * Returns whether optional parameters are supported with the remote
     * endpoint.  If the interface version currently in use is >= 3.4, then
     * this method returns true, otherwise will return false.
     * @return True if optional parameters are supported, otherwise false.
     */
    public boolean areOptionalParametersSupported();


    /**
     * Checks if the session is currently in the "OPEN" state.  The "OPEN" state
     * means the session is connected and a bind is pending.
     * @return True if session is currently in the "OPEN" state, otherwise false.
     */
    public boolean isOpen();

    /**
     * Checks if the session is currently in the "BINDING" state.  The "BINDING" state
     * means the session is in the process of binding.  If local is ESME, we sent
     * the bind request, but have not yet received the bind response.  If the local
     * is SMSC, then the ESME initiated a bind request, but we have't responded yet.
     * @return True if session is currently in the "BINDING" state, otherwise false.
     */
    public boolean isBinding();

    /**
     * Checks if the session is currently in the "BOUND" state.  The "BOUND" state
     * means the session is bound and ready to process requests.
     * @return True if session is currently in the "BOUND" state, otherwise false.
     */
    public boolean isBound();

    /**
     * Checks if the session is currently in the "UNBINDING" state.  The "UNBINDING" state
     * means the session is in the process of unbinding. This may have been initiated
     * by us or them.
     * @return True if session is currently in the "UNBINDING" state, otherwise false.
     */
    public boolean isUnbinding();

    /**
     * Checks if the session is currently in the "CLOSED" state.  The "CLOSED" state
     * means the session is unbound and closed (destroyed).
     * @return True if session is currently in the "CLOSED" state, otherwise false.
     */
    public boolean isClosed();

    /**
     * Returns the System.currentTimeMillis() value of when this session reached
     * the "BOUND" state.
     * @return The System.currentTimeMillis() value when the session was bound.
     */
    public long getBoundTime();

    /**
     * @deprecated
     * @see #getSendWindow()
     */
    public Window<Integer,PduRequest,PduResponse> getRequestWindow();
    
    /**
     * Gets the underlying request "window" for this session.  A "window" represents
     * a request sent to the remote endpoint, but has not received a response
     * yet.  Accessing this property is useful if unacknowledged requests need
     * to be cleared out (most likely for a retry at a later time).
     * @return The request "window"
     */
    public Window<Integer,PduRequest,PduResponse> getSendWindow();
    
    /**
     * Returns true if and only if this session has counters enabled.
     * @return True if the session has counters
     */
    public boolean hasCounters();
    
    /**
     * Gets the counters this session is tracking.
     * @return The session counters or null if counters are disabled.
     */
    public SmppSessionCounters getCounters();

    /**
     * Immediately close the session by closing the underlying socket/channel.
     * This method will not attempt to "unbind" first, rather just immediately
     * close the channel.  Once closed, this session is not usable.  It is
     * always recommended an proper unbind is attempted first, rather than just
     * closing the socket.
     * @see #unbind(long) 
     */
    public void close();

    /**
     * Attempts to "unbind" the session, waiting up to a specified period of
     * milliseconds for an unbind response from the remote endpoint.  Regardless of whether
     * a proper unbind response was received, the socket/channel is closed.
     * @param timeoutMillis The number of milliseconds to wait until an unbind
     *      response is received from the SMSC.
     * @see #close() 
     */
    public void unbind(long timeoutMillis);
    
    /**
     * Destroy a session by ensuring the socket is closed and all
     * resources are cleaned up.  This method should the <b>last</b> method called
     * before discarding or losing a reference to a session.  Since this method
     * cleans up all resources, make sure that any data you need to access is 
     * accessed <b>before</b> calling this method.  After calling this method
     * it is not guaranteed that <b>any</b> other method will correctly work.
     */
    public void destroy();

    /**
     * Synchronously sends an "enquire_link" request to the remote endpoint and
     * waits for up to a specified number of milliseconds for a response. The
     * timeout value includes both waiting for a "window" slot, the time it
     * takes to transmit the actual bytes on the socket, and for the remote
     * endpoint to send a response back.
     * @param request The request to send to the remote endpoint
     * @param timeoutMillis The number of milliseconds to wait until a valid
     *      response is received.
     * @return A valid response to the request
     * @throws RecoverablePduException Thrown when a recoverable PDU error occurs.
     *      A recoverable PDU error includes the partially decoded PDU in order
     *      to generate a negative acknowledgement (NACK) response.
     * @throws UnrecoverablePduException Thrown when an unrecoverable PDU error
     *      occurs. This indicates a seriours error occurred and usually indicates
     *      the session should be immediately terminated.
     * @throws SmppTimeoutException A timeout occurred while waiting for a response
     *      from the remote endpoint.  A timeout can either occur with an unresponse
     *      remote endpoint or the bytes were not written in time.
     * @throws SmppChannelException Thrown when the underlying socket/channel was
     *      unable to write the request.
     * @throws InterruptedException The calling thread was interrupted while waiting
     *      to acquire a lock or write/read the bytes from the socket/channel.
     */
    public EnquireLinkResp enquireLink(EnquireLink request, long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException;

    /**
     * Synchronously sends a "submit" request to the remote endpoint and
     * waits for up to a specified number of milliseconds for a response. The
     * timeout value includes both waiting for a "window" slot, the time it
     * takes to transmit the actual bytes on the socket, and for the remote
     * endpoint to send a response back.
     * @param request The request to send to the remote endpoint
     * @param timeoutMillis The number of milliseconds to wait until a valid
     *      response is received.
     * @return A valid response to the request
     * @throws RecoverablePduException Thrown when a recoverable PDU error occurs.
     *      A recoverable PDU error includes the partially decoded PDU in order
     *      to generate a negative acknowledgement (NACK) response.
     * @throws UnrecoverablePduException Thrown when an unrecoverable PDU error
     *      occurs. This indicates a seriours error occurred and usually indicates
     *      the session should be immediately terminated.
     * @throws SmppTimeoutException A timeout occurred while waiting for a response
     *      from the remote endpoint.  A timeout can either occur with an unresponse
     *      remote endpoint or the bytes were not written in time.
     * @throws SmppChannelException Thrown when the underlying socket/channel was
     *      unable to write the request.
     * @throws InterruptedException The calling thread was interrupted while waiting
     *      to acquire a lock or write/read the bytes from the socket/channel.
     */
    public SubmitSmResp submit(SubmitSm request, long timeoutMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException;

    /**
     * Main underlying method for sending a request PDU to the remote endpoint.
     * If no sequence number was assigned to the PDU, this method will assign one.
     * The PDU will be converted into a sequence of bytes by the underlying transcoder.
     * Also, adds the request to the underlying request "window" by either taking or
     * waiting for an open slot.  If "synchronous", the request "window" will
     * be flagged so that the eventual response will be routed back to the
     * "request future" returned from this method vs. routed back to the session
     * handler and "fireUnexpectedPduResponseReceived" would be triggered.
     * If not "synchronous", then the eventual response will be passed to
     * the "fireExpectedPduResponseReceived" method on the session handler.
     * Please note that its possible th response PDU really isn't
     * the correct PDU we were waiting for, so the caller should verify it.
     * For example it is possible that a "Generic_Nack" could be returned by
     * the remote endpoint in response to a PDU.
     * @param requestPdu The request PDU to send
     * @param timeoutMillis If synchronous is true, this represents the time to
     *      wait for a slot to open in the underlying window AND the time to wait
     *      for a response back from the remote endpoint. If synchronous is false,
     *      this only represents the time to wait for a slot to open in the
     *      underlying window.
     * @param synchronous True if the calling thread plans on waiting for a
     *      response on the returned future.  False if the calling thread plans
     *      on discarding the returned future and expects the response PDU to
     *      be passed to the "fireExpectedPduResponseReceived" method on the session handler.
     * @return A future object that can be synchronized against when a response
     *      is returned.
     * @throws RecoverablePduException Thrown when a recoverable PDU error occurs.
     *      A recoverable PDU error includes the partially decoded PDU in order
     *      to generate a negative acknowledgment (NACK) response.
     * @throws UnrecoverablePduException Thrown when an unrecoverable PDU error
     *      occurs. This indicates a serious error occurred and usually indicates
     *      the session should be immediately terminated.
     * @throws SmppTimeoutException A timeout occurred while waiting for a response
     *      from the remote endpoint.  A timeout can either occur with an unresponsive
     *      remote endpoint or the bytes were not written in time.
     * @throws SmppChannelException Thrown when the underlying socket/channel was
     *      unable to write the request.
     * @throws InterruptedException The calling thread was interrupted while waiting
     *      to acquire a lock or write/read the bytes from the socket/channel.
     */
    public WindowFuture<Integer,PduRequest,PduResponse> sendRequestPdu(PduRequest request, long timeoutMillis, boolean synchronous) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException;

    /**
     * Main underlying method for sending a response PDU to the remote endpoint.
     * The PDU will be converted into a sequence of bytes by the underlying transcoder.
     * Writes the bytes out to the socket/channel.
     * @param response The response PDU to send
     * @throws RecoverablePduException Thrown when a recoverable PDU error occurs.
     *      A recoverable PDU error includes the partially decoded PDU in order
     *      to generate a negative acknowledgment (NACK) response.
     * @throws UnrecoverablePduException Thrown when an unrecoverable PDU error
     *      occurs. This indicates a serious error occurred and usually indicates
     *      the session should be immediately terminated.
     * @throws SmppChannelException Thrown when the underlying socket/channel was
     *      unable to write the request.
     * @throws InterruptedException The calling thread was interrupted while waiting
     *      to acquire a lock or write/read the bytes from the socket/channel.
     */
    public void sendResponsePdu(PduResponse response) throws RecoverablePduException, UnrecoverablePduException, SmppChannelException, InterruptedException;
}