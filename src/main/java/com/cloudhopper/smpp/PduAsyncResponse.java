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

import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

/**
 * A container to hold an asynchronous response and include information tracked
 * internally by an SmppSession.  For example, an instance of this class will
 * contain the original request, the response, and the processing time.
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface PduAsyncResponse {

    /**
     * Gets the original request associated with the response.
     * @return The original request
     */
    public PduRequest getRequest();

    /**
     * Gets the response from the remote endpoint.
     * @return The response
     */
    public PduResponse getResponse();

    /**
     * Gets the amount of time required to accept the request into the session's
     * send window (for a free slot to open up).
     * @return The amount of time (in ms) to accept the request into the send window
     */
    public long getWindowWaitTime();
    
    /**
     * Gets the amount of time required for the remote endpoint to acknowledge
     * the request with a response.  This value is based on the time the request
     * went out on the wire till a response was received on the wire.  Does not
     * include any time required waiting for a slot in the window to become
     * available.
     * @return The amount of time (in ms) to receive a response from remote endpoint
     */
    public long getResponseTime();
    
}