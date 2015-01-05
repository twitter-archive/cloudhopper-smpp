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

import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.type.SmppProcessingException;

/**
 * Handles events received on an SmppServer.  Handles events for processing
 * new bind requests, associating them with sessions, and eventually destroying
 * the sessions.  The life cycle of a session is as follows:
 *
 *   1) New socket/channel connected
 *   2) Assigned a sessionId and calls sessionBindRequested
 *   3) If sessionBindRequested is ok, creates a new session, and calls sessionCreated
 *   4) When socket/channel is closed, calls sessionDestroyed
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppServerHandler {

    /**
     * Triggered when a new client connection has been created with this server
     * and an SMPP bind request has been received.  The bind request can either
     * be a transceiver, receiver, or transmitter.  If the bind request should
     * be denied, then an exception should be thrown.  The session configuration
     * contains all the information copied directly from the bind request. Any
     * changes required on the session should be made in this method. For example,
     * the name of the session should be changed so that logging statements
     * use the correct name.
     * @param sessionId The unique numeric identifier assigned to the bind request.
     *      Will be the same value between sessionBindRequested, sessionCreated,
     *      and sessionDestroyed method calls.
     * @param sessionConfiguration The session configuration object that will
     *      be associated with this session.  Initially prepared to match the
     *      values contained in the bind request.
     * @param bindRequest The bind request received from the client.
     * @throws SmppProcessingException Thrown if the bind fails or should be
     *      rejected.  If thrown, a bind response with the SMPP status code
     *      contained in this exception will be generated and returned back to
     *      the client.
     */
    public void sessionBindRequested(Long sessionId, SmppSessionConfiguration sessionConfiguration, final BaseBind bindRequest) throws SmppProcessingException;

    /**
     * Triggered when a session is in-between a bound state and a processing
     * state.  After a new client connection as been successfully passed
     * sessionBindRequested(), a session is created.  This session must still
     * have the "serverReady()" method called to actually trigger the final
     * part of a server session.
     * @param sessionId The unique numeric identifier assigned to the bind request.
     *      Will be the same value between sessionBindRequested, sessionCreated,
     *      and sessionDestroyed method calls.
     * @param session The server session associated with the bind request and
     *      underlying channel.
     * @param preparedBindResponse The prepared bind response that will
     *      eventually be returned to the client when "serverReady" is finally
     *      called on the session.
     * @throws SmppProcessingException Thrown if the bind fails or should be
     *      rejected.  If thrown, a bind response with the SMPP status code
     *      contained in this exception will be generated and returned back to
     *      the client.
     */
    public void sessionCreated(Long sessionId, SmppServerSession session, BaseBindResp preparedBindResponse) throws SmppProcessingException;

    /**
     * Triggered when a session is unbound and closed with the client.
     * @param sessionId The unique numeric identifier assigned to the bind request.
     *      Will be the same value between sessionBindRequested, sessionCreated,
     *      and sessionDestroyed method calls.
     * @param session The server session associated with the bind request and
     *      underlying channel.
     */
    public void sessionDestroyed(Long sessionId, SmppServerSession session);

}
