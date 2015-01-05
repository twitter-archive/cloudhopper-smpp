package com.cloudhopper.smpp.simulator;

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

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import org.jboss.netty.channel.Channel;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSimulatorBindProcessor implements SmppSimulatorPduProcessor {

    private String systemId;
    private String password;
    
    public SmppSimulatorBindProcessor(String systemId, String password) {
        this.systemId = systemId;
        this.password = password;
    }

    @Override
    public boolean process(SmppSimulatorSessionHandler session, Channel channel, Pdu pdu) throws Exception {
        // anything other than a bind is super bad!
        if (!(pdu instanceof BaseBind)) {
            if (pdu instanceof PduRequest) {
                session.addPduToWriteOnNextPduReceived(((PduRequest)pdu).createGenericNack(SmppConstants.STATUS_INVBNDSTS));
                return true;
            } else {
                //logger.error("PDU response received, but not bound");
                channel.close();
                return true;
            }
        }

        BaseBind bind = (BaseBind)pdu;
        BaseBindResp bindResp = (BaseBindResp)bind.createResponse();

        if (!bind.getSystemId().equals(systemId)) {
            bindResp.setCommandStatus(SmppConstants.STATUS_INVSYSID);
        } else if (!bind.getPassword().equals(password)) {
            bindResp.setCommandStatus(SmppConstants.STATUS_INVPASWD);
        }

        session.addPduToWriteOnNextPduReceived(bindResp);
        return true;
    }

}
