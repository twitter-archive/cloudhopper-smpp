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

/**
 * A container to hold an asynchronous response and include information tracked
 * internally by an SmppSession.  For example, an instance of this class will
 * contain the original request, the response, and a few timestamps.
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
     * Gets the size of the window after this request was added.
     * @return The size of the window after this request was added.
     */
    public int getWindowSize();

    /**
     * Gets the amount of time required to accept the request into the session
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
     * <br><br>
     * NOTE: If the window size is > 1, this value can be somewhat misleading.
     * The remote endpoint would process X number of requests ahead of this one
     * that went out ahead of it in the window.  This does represent the total
     * response time, but doesn't mean the remote endpoint is this slow at processing
     * one request.  In cases of high load where the window is always full, the
     * windowWaitTime actually represents how fast the remote endpoint is processing
     * requests.
     * @return The amount of time (in ms) to receive a response from remote endpoint
     */
    public long getResponseTime();
    
    /**
     * Gets an estimate of the processing time required by the remote endpoint
     * to process this request.  The value is calculated with the following 
     * formula: "response time" divided by the "window size" at the time of the
     * request.
     * @return The amount of estimated time (in ms) to receive a response from
     *      the remote endpoint just for this request (as opposed to potentially
     *      this request and all requests ahead of it in the window).
     */
    public long getEstimatedProcessingTime();
    
}