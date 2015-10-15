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
import com.cloudhopper.smpp.pdu.BufferHelper;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.transcoder.PduTranscoderContext;
import org.hamcrest.core.StringContains;
import org.jboss.netty.buffer.ChannelBuffer;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DeliveryReceiptTest {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryReceiptTest.class);

    @Test
    public void toShortMessage() {
        DeliveryReceipt dlr = new DeliveryReceipt();
        dlr.setMessageId("0123456789");
        dlr.setSubmitCount(1);
        dlr.setDeliveredCount(1);
        dlr.setSubmitDate(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC));
        dlr.setDoneDate(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC));
        dlr.setState(SmppConstants.STATE_DELIVERED);
        dlr.setErrorCode(12);
        dlr.setText("This is a sample message that I want to have added to the delivery receipt");

        String receipt0 = dlr.toShortMessage();

        //logger.debug(receipt0);
        Assert.assertEquals("id:0123456789 sub:001 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", receipt0);
    }

    @Test
    public void parseShortMessage() throws Exception {
        String receipt0 = "id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes";

        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage(receipt0, DateTimeZone.UTC);

        Assert.assertEquals("0123456789", dlr.getMessageId());
        Assert.assertEquals(2, dlr.getSubmitCount());
        Assert.assertEquals(1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(12, dlr.getErrorCode());
        Assert.assertEquals("This is a sample mes", dlr.getText());


        // example receipt from smsc simulator (checks case sensitivity)
        receipt0 = "id:4 sub:001 dlvrd:001 submit date:1006020051 done date:1006020051 stat:DELIVRD err:000 Text:Hello";
        dlr = DeliveryReceipt.parseShortMessage(receipt0, DateTimeZone.UTC);

        Assert.assertEquals("4", dlr.getMessageId());
        Assert.assertEquals(1, dlr.getSubmitCount());
        Assert.assertEquals(1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2010, 6, 2, 0, 51, 0, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2010, 6, 2, 0, 51, 0, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(0, dlr.getErrorCode());
        Assert.assertEquals("Hello", dlr.getText());
    }

    @Test
    public void parseShortMessageMissingFields() throws Exception {
        try {
            DeliveryReceipt.parseShortMessage("i:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 su:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvr:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvrd:001 submit dat:1005232039 done date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvrd:001 submit date:1005232039 one date:1005242339 stat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 sat:DELIVRD err:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD rr:012 text:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }

        try {
            DeliveryReceipt.parseShortMessage("id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:012 txt:This is a sample mes", DateTimeZone.UTC);
            Assert.fail();
        } catch (DeliveryReceiptException e) {
            // correct behavior
        }
    }

    @Test
    public void parseRealWorldReceipt0() throws Exception {

        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);

        ChannelBuffer buffer0 = BufferHelper.createBuffer("000000BA00000005000000000000000200010134343935313336313932300001013430343034000400000000000000006E69643A30303539313133393738207375623A30303120646C7672643A303031207375626D697420646174653A3130303231303137333020646F6E6520646174653A3130303231303137333120737461743A44454C49565244206572723A30303020746578743A4024232125262F3A000E0001010006000101001E000833383630316661000427000102");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer0);

        // id:0059113978 sub:001 dlvrd:001 submit date:1002101730 done date:1002101731 stat:DELIVRD err:000 text:@$#!%&/:
        byte[] sm0 = pdu0.getShortMessage();

        String message0 = new String(sm0, "ISO-8859-1");

        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage(message0, DateTimeZone.UTC);

        Assert.assertEquals("0059113978", dlr.getMessageId());
        Assert.assertEquals(1, dlr.getSubmitCount());
        Assert.assertEquals(1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2010, 2, 10, 17, 30, 0, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2010, 2, 10, 17, 31, 0, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(0, dlr.getErrorCode());
        Assert.assertEquals("@$#!%&/:", dlr.getText());

        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID);
        String receiptedMessageId = tlv0.getValueAsString();
        Assert.assertEquals("38601fa", receiptedMessageId);

        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_MSG_STATE);
        byte state = tlv1.getValueAsByte();
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, state);

        String receipt0 = dlr.toShortMessage();
        Assert.assertEquals(message0, receipt0);
    }

    @Test
    public void parseReceiptWithTimestampIncludingSeconds() throws Exception {
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);

        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("id:74e02ee1-4e2f-4a6e-a78b-4b247d756a22 sub:001 dlvrd:001 submit date:110206193041 done date:110206193110 stat:DELIVRD err:000 text:", DateTimeZone.UTC);

        Assert.assertEquals("74e02ee1-4e2f-4a6e-a78b-4b247d756a22", dlr.getMessageId());
        Assert.assertEquals(1, dlr.getSubmitCount());
        Assert.assertEquals(1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2011, 2, 6, 19, 30, 41, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2011, 2, 6, 19, 31, 10, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(0, dlr.getErrorCode());
        Assert.assertNull(dlr.getText());
    }

    @Test
    public void parseReceiptWithFieldsOutOfOrder() throws Exception {
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("sub:001 id:74e02ee1-4e2f-4a6e-a78b-4b247d756a22 err:000 dlvrd:001 done date:110206193110 submit date:110206193041 text: stat:DELIVRD", DateTimeZone.UTC);
        Assert.assertEquals("74e02ee1-4e2f-4a6e-a78b-4b247d756a22", dlr.getMessageId());
        Assert.assertEquals(1, dlr.getSubmitCount());
        Assert.assertEquals(1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2011, 2, 6, 19, 30, 41, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2011, 2, 6, 19, 31, 10, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(0, dlr.getErrorCode());
        Assert.assertNull(dlr.getText());
    }

    @Test
    public void toShortMessageWith9DigitLongMessageId() {
        DeliveryReceipt dlr = new DeliveryReceipt();
        dlr.setMessageId(123456789L);
        //dlr.setSubmitCount(1);
        //dlr.setDeliveredCount(1);
        //dlr.setSubmitDate(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setDoneDate(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setState(SmppConstants.STATE_DELIVERED);
        //dlr.setErrorCode(12);
        //dlr.setText("This is a sample message that I want to have added to the delivery receipt");

        String receipt0 = dlr.toShortMessage();

        //logger.debug(receipt0);
        Assert.assertEquals("id:0123456789 sub:000 dlvrd:000 submit date:0000000000 done date:0000000000 stat:BADSTAT err:000 text:", receipt0);
    }

    @Test
    public void toShortMessageWith1DigitLongMessageId() {
        DeliveryReceipt dlr = new DeliveryReceipt();
        dlr.setMessageId(1L);
        //dlr.setSubmitCount(1);
        //dlr.setDeliveredCount(1);
        //dlr.setSubmitDate(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setDoneDate(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setState(SmppConstants.STATE_DELIVERED);
        //dlr.setErrorCode(12);
        //dlr.setText("This is a sample message that I want to have added to the delivery receipt");

        String receipt0 = dlr.toShortMessage();

        //logger.debug(receipt0);
        Assert.assertEquals("id:0000000001 sub:000 dlvrd:000 submit date:0000000000 done date:0000000000 stat:BADSTAT err:000 text:", receipt0);
    }

    @Test
    public void toShortMessageWith10DigitLongMessageId() {
        DeliveryReceipt dlr = new DeliveryReceipt();
        dlr.setMessageId(1234567890L);
        //dlr.setSubmitCount(1);
        //dlr.setDeliveredCount(1);
        //dlr.setSubmitDate(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setDoneDate(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setState(SmppConstants.STATE_DELIVERED);
        //dlr.setErrorCode(12);
        //dlr.setText("This is a sample message that I want to have added to the delivery receipt");

        String receipt0 = dlr.toShortMessage();

        //logger.debug(receipt0);
        Assert.assertEquals("id:1234567890 sub:000 dlvrd:000 submit date:0000000000 done date:0000000000 stat:BADSTAT err:000 text:", receipt0);
    }

    @Test
    public void toShortMessageWith11DigitLongMessageId() {
        DeliveryReceipt dlr = new DeliveryReceipt();
        dlr.setMessageId(12345678901L);
        //dlr.setSubmitCount(1);
        //dlr.setDeliveredCount(1);
        //dlr.setSubmitDate(new DateTime(2010, 5, 23, 20, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setDoneDate(new DateTime(2010, 5, 24, 23, 39, 0, 0, DateTimeZone.UTC));
        //dlr.setState(SmppConstants.STATE_DELIVERED);
        //dlr.setErrorCode(12);
        //dlr.setText("This is a sample message that I want to have added to the delivery receipt");

        String receipt0 = dlr.toShortMessage();

        //logger.debug(receipt0);
        Assert.assertEquals("id:12345678901 sub:000 dlvrd:000 submit date:0000000000 done date:0000000000 stat:BADSTAT err:000 text:", receipt0);
    }


    @Test
    public void toShortMessageWithFullConstructor() throws DeliveryReceiptException {
        DeliveryReceipt dlr = new DeliveryReceipt("12345", 1, 1, new DateTime(0L, DateTimeZone.UTC),
                new DateTime(0L, DateTimeZone.UTC), SmppConstants.STATE_ENROUTE, 0, "text");

        String receipt = dlr.toShortMessage();

        Assert.assertEquals("id:12345 sub:001 dlvrd:001 submit date:7001010000 done date:7001010000 stat:ENROUTE err:000 text:text", receipt);
    }

    @Test
    public void parseShortMessageWith11DigitLongMessageId() throws Exception {
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("id:98765432101 sub:000 dlvrd:000 submit date:1001010000 done date:1001010000 stat:ENROUTE err:000 text:", DateTimeZone.UTC);

        Assert.assertEquals(98765432101L, dlr.getMessageIdAsLong());
    }

    @Test
    public void toMessageIdAsHexString() throws Exception {
        Assert.assertEquals("16fee0e525", DeliveryReceipt.toMessageIdAsHexString(98765432101L));
    }

    @Test
    public void toMessageIdAsLong() throws Exception {
        Assert.assertEquals(98765432101L, DeliveryReceipt.toMessageIdAsLong("16fee0e525"));
    }

    @Test
    public void parseReceiptWithMissingSubAndDlvrdFields() throws Exception {
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("id:2E179B310EDE971B2760C72B7F026E1B submit date:20110314181534 done date:20110314181741 stat:DELIVRD err:0", DateTimeZone.UTC, false);
        Assert.assertEquals("2E179B310EDE971B2760C72B7F026E1B", dlr.getMessageId());
        Assert.assertEquals(-1, dlr.getSubmitCount());
        Assert.assertEquals(-1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2011, 3, 14, 18, 15, 34, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertEquals(new DateTime(2011, 3, 14, 18, 17, 41, 0, DateTimeZone.UTC), dlr.getDoneDate());
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, dlr.getState());
        Assert.assertEquals(0, dlr.getErrorCode());
        Assert.assertNull(dlr.getText());
    }
    
    @Test
    public void parseShortMessageWithSmpp3_4SpecCompliantErrAsStringValue() throws Exception {
        String receipt0 = "id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:21b text:This is a sample mes";
        
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage(receipt0, DateTimeZone.UTC);
        
        Assert.assertEquals("21b", dlr.getRawErrorCode());
        
        // being set if we cannot parse value
        Assert.assertEquals(-1, dlr.getErrorCode());
        
        Assert.assertEquals(receipt0, dlr.toShortMessage());
    }
    
    @Test
    public void parseShortMessageWithSmpp3_4SpecCompliantErrAsIntValue() throws Exception {
        String receipt0 = "id:0123456789 sub:002 dlvrd:001 submit date:1005232039 done date:1005242339 stat:DELIVRD err:010 text:This is a sample mes";
        
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage(receipt0, DateTimeZone.UTC);
        
        Assert.assertEquals("010", dlr.getRawErrorCode());
        
        // being set if we cannot parse value
        Assert.assertEquals(10, dlr.getErrorCode());
        
        Assert.assertEquals(receipt0, dlr.toShortMessage());
    }

    @Test
    public void parseReceiptWithOnlySubmitDate() throws DeliveryReceiptException {
        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("submit date:110206193041", DateTimeZone.UTC, false);
        // uninitialized state is -1 for numeric primitives, null for references
        Assert.assertNull(dlr.getMessageId());
        Assert.assertEquals(-1, dlr.getSubmitCount());
        Assert.assertEquals(-1, dlr.getDeliveredCount());
        Assert.assertEquals(new DateTime(2011, 2, 6, 19, 30, 41, 0, DateTimeZone.UTC), dlr.getSubmitDate());
        Assert.assertNull(dlr.getDoneDate());
        Assert.assertEquals((byte) -1, dlr.getState());
        Assert.assertEquals(-1, dlr.getErrorCode());
        Assert.assertNull(dlr.getText());

        // broken null-check caused date format to be applied to null date,
        // which results in the current time being formatted instead of all-zeroes
        Assert.assertThat(dlr.toShortMessage(), new StringContains(DeliveryReceipt.FIELD_DONE_DATE + "0000000000"));
    }
}
