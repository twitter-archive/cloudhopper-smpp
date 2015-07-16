/*
 * Copyright 2015 Cloudhopper by Twitter.
 *
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
 */
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
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;

/**
 *
 * @author pgoergler
 */
public interface SmppClientSession extends SmppSession {

    public BaseBindResp bind(BaseBind request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppBindException, SmppTimeoutException, SmppChannelException, InterruptedException;
    
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

}
