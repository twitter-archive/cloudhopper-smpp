package com.cloudhopper.smpp;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2014 Cloudhopper by Twitter
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

import com.cloudhopper.smpp.pdu.Pdu;

/**
 * Low-level PDU traffic listener. 
 * Could be used for PDU filtering.
 * Use with caution.
 *
 * @author t3hk0d3
 */
public interface SmppSessionListener extends SmppSessionHandler {
    
    /**
     * Called when ANY PDU received from connection.
     * This method could be used to sniff all incoming SMPP packet traffic,
     * and also permit/deny it from futher processing.
     * Could be used for advanced packet logging, counters and filtering.
     * @param pdu 
     * @return boolean allow PDU processing. If false PDU would be discarded.
     */
    public boolean firePduReceived(Pdu pdu);
    
    /**
     * Called when ANY PDU received from connection.
     * This method could be used to sniff all outgoing SMPP packet traffic,
     * and also permit/deny it from sending.
     * Could be used for advanced packet logging, counters and filtering.
     * @param pdu 
     * @return boolean allow PDU sending. If false PDU would be discarded.
     */
    public boolean firePduDispatch(Pdu pdu);
    
}
