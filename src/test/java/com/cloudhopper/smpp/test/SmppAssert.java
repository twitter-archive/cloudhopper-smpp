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

import com.cloudhopper.smpp.pdu.SubmitMulti;
import com.cloudhopper.smpp.pdu.SubmitMultiResp;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.SubmitMultiDestinationAddress;
import com.cloudhopper.smpp.type.SubmitMultiUnsuccessSme;
import org.junit.Assert;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author innar.made@ishisystems.com
 */
public class SmppAssert {

    private SmppAssert() {}

    public static void assertSubmitMulti(SubmitMulti expected, SubmitMulti actual) {
        assertEquals("command_length", expected.getCommandLength(), actual.getCommandLength());
        assertEquals("command_id", expected.getCommandId(), actual.getCommandId());
        assertEquals("command_status", expected.getCommandStatus(), actual.getCommandStatus());
        assertEquals("sequence_number", expected.getSequenceNumber(), actual.getSequenceNumber());
        assertEquals("service_type", expected.getServiceType(), actual.getServiceType());
        assertAddress(expected.getSourceAddress(), actual.getSourceAddress());
        assertSubmitMultiDestinationAddressList(
                expected.getSubmitMultiDestinationAddressList(), actual.getSubmitMultiDestinationAddressList());
        assertEquals("esm_class", expected.getEsmClass(), actual.getEsmClass());
        assertEquals("protocol_id", expected.getProtocolId(), actual.getProtocolId());
        assertEquals("priority", expected.getPriority(), actual.getPriority());
        assertEquals("schedule", expected.getScheduleDeliveryTime(), actual.getScheduleDeliveryTime());
        assertEquals("validity", expected.getValidityPeriod(), actual.getValidityPeriod());
        assertEquals("registered_delivery", expected.getRegisteredDelivery(), actual.getRegisteredDelivery());
        assertEquals("replace_if_present", expected.getReplaceIfPresent(), actual.getReplaceIfPresent());
        assertEquals("data_coding", expected.getDataCoding(), actual.getDataCoding());
        assertEquals("default_msg_id", expected.getDefaultMsgId(), actual.getDefaultMsgId());
        Assert.assertArrayEquals("short_msg", expected.getShortMessage(), actual.getShortMessage());
    }

    public static void assertSubmitMultiResp(SubmitMultiResp expected, SubmitMultiResp actual) {
        assertNullity(expected, actual);
        if (expected == null) {
            return;
        }
        assertEquals("command_length", expected.getCommandLength(), actual.getCommandLength());
        assertEquals("command_id", expected.getCommandId(), actual.getCommandId());
        assertEquals("command_status", expected.getCommandStatus(), actual.getCommandStatus());
        assertEquals("sequence_number", expected.getSequenceNumber(), actual.getSequenceNumber());
        assertEquals("message_id", expected.getMessageId(), actual.getMessageId());
        final int numberOfUnsuccessSmes = expected.getNumberOfUnsuccessSmes();
        assertEquals("no_unsuccess", numberOfUnsuccessSmes, actual.getNumberOfUnsuccessSmes());
        if (numberOfUnsuccessSmes > 0) {
            for (int i = 0; i < numberOfUnsuccessSmes; i++) {
                assertSubmitMultiUnsuccessSme(expected.getUnsuccessSmes().get(i), actual.getUnsuccessSmes().get(i));
            }
        } else {
            assertNullity(expected.getUnsuccessSmes(), actual.getUnsuccessSmes());
        }
    }

    public static void assertAddress(Address expected, Address actual) {
        assertNullity(expected, actual);
        if (expected == null) {
            return;
        }
        assertEquals("ton", expected.getTon(), actual.getTon());
        assertEquals("npi", expected.getNpi(), actual.getNpi());
        assertEquals("address", expected.getAddress(), actual.getAddress());
    }

    public static void assertSubmitMultiDestinationAddressList(List<SubmitMultiDestinationAddress> expected,
                                                               List<SubmitMultiDestinationAddress> actual) {
        assertNullity(expected, actual);
        if (expected == null) {
            return;
        }
        assertEquals("number_of_dests", expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            SubmitMultiDestinationAddress e = expected.get(i);
            SubmitMultiDestinationAddress a = actual.get(i);
            assertSubmitMultiDestinationAddress(e, a);
        }
    }

    public static void assertSubmitMultiDestinationAddress(SubmitMultiDestinationAddress expected,
                                                           SubmitMultiDestinationAddress actual) {
        assertNullity(expected, actual);
        if (expected == null) {
            return;
        }
        assertEquals("dest_flag", expected.getDestinationFlag(), actual.getDestinationFlag());
        if (expected.isAddress()) {
            assertAddress(expected.getAddress(), actual.getAddress());
        } else if (expected.isDistributionListName()) {
            assertEquals(expected.getDistributionListName(), actual.getDistributionListName());
        } else {
            Assert.fail("Invalid dest_flag value " + expected.getDestinationFlag());
        }
    }

    public static void assertSubmitMultiUnsuccessSme(SubmitMultiUnsuccessSme expected, SubmitMultiUnsuccessSme actual) {
        assertNullity(expected, actual);
        if (expected == null) {
            return;
        }
        assertAddress(expected.getAddress(), actual.getAddress());
        assertEquals("error_status_code", expected.getErrorStatusCode(), actual.getErrorStatusCode());
    }

    private static void assertNullity(Object expected, Object actual) {
        if (expected != null) {
            assertNotNull("Actual object must not be null", actual);
        } else {
            assertNull("Actual object must be null", actual);
        }
    }
}
