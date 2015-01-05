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

// third party imports
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.type.Address;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class PduUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(PduUtilTest.class);

    @Test
    public void isRequestCommandId() throws Exception {
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_TRANSMITTER));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_RECEIVER));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_TRANSCEIVER));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_CANCEL_SM));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_DATA_SM));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_ENQUIRE_LINK));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_SUBMIT_SM));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_DELIVER_SM));
        Assert.assertEquals(true, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_UNBIND));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_GENERIC_NACK));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_TRANSMITTER_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_RECEIVER_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_CANCEL_SM_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_DATA_SM_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_SUBMIT_SM_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_DELIVER_SM_RESP));
        Assert.assertEquals(false, PduUtil.isRequestCommandId(SmppConstants.CMD_ID_UNBIND_RESP));
    }

    @Test
    public void isResponseCommandId() throws Exception {
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_TRANSMITTER));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_RECEIVER));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_TRANSCEIVER));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_CANCEL_SM));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_DATA_SM));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_ENQUIRE_LINK));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_SUBMIT_SM));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_DELIVER_SM));
        Assert.assertEquals(false, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_UNBIND));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_GENERIC_NACK));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_TRANSMITTER_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_RECEIVER_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_CANCEL_SM_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_DATA_SM_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_SUBMIT_SM_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_DELIVER_SM_RESP));
        Assert.assertEquals(true, PduUtil.isResponseCommandId(SmppConstants.CMD_ID_UNBIND_RESP));
    }

    @Test
    public void calculateByteSizeOfAddress() throws Exception {
        Assert.assertEquals(3, PduUtil.calculateByteSizeOfAddress(null));
        Assert.assertEquals(3, PduUtil.calculateByteSizeOfAddress(new Address()));
        Assert.assertEquals(4, PduUtil.calculateByteSizeOfAddress(new Address((byte)0x01, (byte)0x01, "A")));
    }

    @Test
    public void calculateByteSizeOfNullTerminatedString() throws Exception {
        Assert.assertEquals(1, PduUtil.calculateByteSizeOfNullTerminatedString(null));
        Assert.assertEquals(1, PduUtil.calculateByteSizeOfNullTerminatedString(""));
        Assert.assertEquals(2, PduUtil.calculateByteSizeOfNullTerminatedString("A"));
    }
}
