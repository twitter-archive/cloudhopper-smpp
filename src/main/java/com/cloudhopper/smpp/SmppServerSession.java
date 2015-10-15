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

/**
 * An extended interface for a Server (SMSC) SMPP session.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppServerSession extends SmppSession {

    /**
     * Indicates that the local endpoint (server) is ready to start processing
     * requests for the session.  Completes the bind process by the remote
     * endpoint by setting up the handler for the session, sending back the
     * prepared bind response and setting the socket/channel as readable.
     *
     * Please note that when the SmppServer accepts a new socket connection
     * from a client, it reads the bind request, but does not automatically
     * send back a bind response.  The bind response can only be sent back
     * by calling this method to indicate that the server is now fully ready
     * to process requests from the client.

     * @param sessionHandler The handler for the session to use for processing
     */
    public void serverReady(SmppSessionHandler sessionHandler);

}
