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

import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * Interface representing an SmppClient.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppClient {

    /**
     * Binds a client to a remote SMPP endpoint by opening the socket, sending
     * a bind request, and waiting for a bind response.
     * @param config The client session configuration
     * @param sessionHandler The session handler
     * @return If the bind is successful will be the new session
     * @throws SmppTimeoutException Thrown if either the underlying TCP/IP connection
     * cannot connect within the "connectTimeout" or we can connect but don't
     * receive a response back to the bind request within the "bindTimeout".
     * @throws SmppChannelException Thrown if there is an error with the underlying
     * TCP/IP connection such as a bad host name or the remote server's port
     * is not accepting connections.
     * @throws SmppBindException Thrown only in the case where the "bind" request
     * was successfully sent to the remote system and we actually got back
     * a "bind" response that rejected the bind attempt.
     * @throws UnrecoverablePduException Thrown in the case where we were able
     * to connect and send our "bind" request, but we got back data that
     * was not failed parsing into a PDU.
     * @throws InterruptedException Thrown if the calling thread is interrupted
     * while we are attempting the bind.
     */
    public SmppSession bind(SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, SmppBindException, UnrecoverablePduException, InterruptedException;

    /**
     * Destroy a client by ensuring that all session sockets are closed and all
     * resources are cleaned up.  This method should the <b>last</b> method called
     * before discarding or losing a reference to a client.  Since this method
     * cleans up all resources, make sure that any data you need to access is 
     * accessed <b>before</b> calling this method.  After calling this method
     * it is not guaranteed that <b>any</b> other method will correctly work.
     */
    public void destroy();

}
