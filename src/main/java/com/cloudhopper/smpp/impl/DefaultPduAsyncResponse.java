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

package com.cloudhopper.smpp.impl;

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.commons.util.windowing.ResponseFuture;
import com.cloudhopper.smpp.PduAsyncResponse;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;

/**
 * Default implementation of an SmppAsyncResponse.
 * 
 * @author joelauer
 */
public class DefaultPduAsyncResponse implements PduAsyncResponse {
    // we internally "wrap" a PDU response future
    private final ResponseFuture<Integer,PduRequest,PduResponse> future;

    public DefaultPduAsyncResponse(ResponseFuture<Integer,PduRequest,PduResponse> future) {
        this.future = future;
    }

    public PduRequest getRequest() {
        return future.getRequest();
    }

    public PduResponse getResponse() {
        return future.getResponse();
    }

    public long getProcessingTime() {
        return future.getProcessingTime();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(100);
        buf.append("smpp_async_resp: seqNum [0x");
        buf.append(HexUtil.toHexString(this.future.getKey()));
        buf.append("] processingTime [");
        buf.append(this.future.getProcessingTime());
        buf.append(" ms] reqType [");
        buf.append(getRequest().getName());
        buf.append("] respType [");
        buf.append(getResponse().getName());
        buf.append("]");
        return buf.toString();
    }
}
