/**
 * Copyright (C) 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.cloudhopper.smpp;

import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 * Interface representing an SmppClient.
 * 
 * @author joelauer
 */
public interface SmppClient {

    /**
     *
     * @param config
     * @param sessionHandler
     * @return
     * @throws SmppTimeoutException Thrown if either the underlying TCP/IP connection
     * cannot connect within the "connectTimeout" or we can connect, but don't
     * receive a response back to the bind request within the "bindTimeout".
     * @throws SmppChannelException Thrown if there is an error with the underlying
     * TCP/IP connection such as a bad host name or the remote server's port
     * is not accepting connections.
     * @throws SmppBindException Thrown only in the case where the "bind" request
     * was successfully sent to the remote system and we actually got back
     * a "bind" response that rejected the bind attempt.
     * @throws UnrecoverablePduException Thrown in the case where we were able
     * to connect and send our "bind" request, but we got back data that
     * was not parseable to a valid PDU.
     * @throws InterruptedException Thrown if the calling thread is interrupted
     * while we are attempting the bind.
     */
    SmppSession bind(SmppSessionConfiguration config, SmppSessionHandler sessionHandler) throws SmppTimeoutException, SmppChannelException, SmppBindException, UnrecoverablePduException, InterruptedException;

    void shutdown();

}
