package com.cloudhopper.smpp.test;

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

import com.cloudhopper.commons.gsm.DataCoding;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitMultiResp;
import com.cloudhopper.smpp.type.Address;

/**
 * @author innar.made@ishisystems.com
 */
public class SmppTestDataProvider {

    private SmppTestDataProvider() {}

    public static SubmitMulti getDefaultSubmitMulti() throws Exception {
        SubmitMulti pdu = new SubmitMulti();
        pdu.setSequenceNumber(20456);
        pdu.setServiceType("abc123");
        pdu.setSourceAddress(new Address((byte) 4, (byte) 5, "123456789"));
        pdu.setEsmClass((byte) 1);
        pdu.setProtocolId((byte) 3);
        pdu.setPriority((byte) 2);
        pdu.setScheduleDeliveryTime("140101000000000+");
        pdu.setValidityPeriod("140231235959948-");
        pdu.setRegisteredDelivery((byte) 1);
        pdu.setReplaceIfPresent((byte) 0);
        pdu.setDataCoding(DataCoding.CHAR_ENC_LATIN1);
        pdu.setDefaultMsgId((byte) 55);
        pdu.setShortMessage("Hello World".getBytes("ISO-8859-1"));
        return pdu;
    }

    public static String getDefaultSubmitMultiPduHex(String submitMultiDestinationHex) {
        int length = 88 + (submitMultiDestinationHex.length() / 2);
        return String.format("%s000000210000000000004FE861626331323300" +
                        "040531323334353637383900%s0103023134303130313030303030303030302B00" +
                        "3134303233313233353935393934382D00010003370B48656C6C6F20576F726C64",
                HexUtil.toHexString(length), submitMultiDestinationHex);
    }

    public static SubmitMultiResp getDefaultSubmitMultiResp() {
        SubmitMultiResp pdu = new SubmitMultiResp();
        pdu.setCommandStatus(SmppConstants.STATUS_OK);
        pdu.setSequenceNumber(20456);
        pdu.setMessageId("a1-b2-c3-d4-e5-f6-g7");
        pdu.setUnsuccessSmes(null);
        return pdu;
    }
}
