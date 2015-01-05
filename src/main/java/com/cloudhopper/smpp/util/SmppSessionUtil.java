package com.cloudhopper.smpp.util;

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

import com.cloudhopper.smpp.SmppSession;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.type.GenericNackException;
import com.cloudhopper.smpp.type.UnexpectedPduResponseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSessionUtil {
    private static final Logger logger = LoggerFactory.getLogger(SmppSessionUtil.class);

    static public void close(SmppSession session) {
        if (session != null) {
            try {
                session.close();
            } catch (Throwable t) {
                logger.warn("Unable to cleanly close session: {}", t);
            }
        }
    }

    /**
     * Asserts that a PDU response matches a PDU request.  A good example is that
     * a PDU response may be a "Generic_Nack" to our request.
     * @param request The PDU request
     * @param response The PDU response
     * @throws GenericNackException Thrown if the response was a "Generic_Nack"
     * @throws UnexpectedPduResponseException Thrown if the response type did
     *      not match the request type.  For example, if the request was a
     *      "submit_sm", but the response was "data_sm_resp".
     */
    static public void assertExpectedResponse(PduRequest request, PduResponse response) throws GenericNackException, UnexpectedPduResponseException {
        if (request.getResponseClass().isInstance(response)) {
            return;
        } else if (response instanceof GenericNack) {
            throw new GenericNackException((GenericNack)response);
        } else {
            throw new UnexpectedPduResponseException(response);
        }
    }
}