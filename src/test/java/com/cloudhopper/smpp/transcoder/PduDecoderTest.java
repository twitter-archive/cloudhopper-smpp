package com.cloudhopper.smpp.transcoder;

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
import com.cloudhopper.smpp.pdu.*;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.type.UnknownCommandIdException;
import com.cloudhopper.smpp.type.TerminatingNullByteNotFoundException;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.tlv.Tlv;
import org.junit.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class PduDecoderTest {
    private static final Logger logger = LoggerFactory.getLogger(PduDecoderTest.class);
    
    private PduTranscoderContext context;
    private PduTranscoder transcoder;

    public PduDecoderTest() {
        this.context = new DefaultPduTranscoderContext();
        this.transcoder = new DefaultPduTranscoder(this.context);
    }

    @Test
    public void decodeLessThan4Bytes() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000");

        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
        Assert.assertNull(pdu0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(3, buffer.readableBytes());
    }

    @Test
    public void decodeOnly4Bytes() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010");

        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
        Assert.assertNull(pdu0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(4, buffer.readableBytes());
    }

    @Test
    public void decodeLessThan16Bytes() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001000000015000000000a342e");

        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
        Assert.assertNull(pdu0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(15, buffer.readableBytes());
    }

    @Test
    public void decodeUnsupportedRequestCommandId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001000000110000000000a342ee7");

        try {
            EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
            Assert.fail();
        } catch (UnknownCommandIdException e) {
            // correct behavior
        }

        // an unsupported command id is recoverable and all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeUnsupportedResponseCommandId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001080000110000000000a342ee7");

        try {
            EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
            Assert.fail();
        } catch (UnknownCommandIdException e) {
            // correct behavior
        }

        // an unsupported command id is recoverable and all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBadPduButSkipAllDataInBuffer() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001100000110000000000a342ee70F0000001000000015000000000a342ee7");

        try {
            EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
            Assert.fail();
        } catch (UnknownCommandIdException e) {
            // correct behavior (this should fail)
        }

        // at this point, the first PDU command id wasn't valid, but we still
        // read the entire PDU and the next PDU was able to be parsed okay
        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(0x00000015, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192039, pdu0.getSequenceNumber());

        // an unsupported command id is recoverable and all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSignedIntLength() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("F000001100000110000000000a342ee7");
        try {
            EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
            Assert.fail();
        } catch (UnrecoverablePduException e) {
            // correct behavior (this should fail)
        }
    }

    @Test
    public void decodeMaxSequenceNumber() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001000000015000000007fffffff");

        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(2147483647, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeTooBigSequenceNumber() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010000000150000000080000000");

        try {
            EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
            // this should work now that we support 32 bits for a sequence number
        } catch (UnrecoverablePduException e) {
            Assert.fail();
        }

        // despite having too large a sequence number, all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeZeroSequenceNumber() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010000000150000000000000000");

        // this should work now...
        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
    }

    @Test
    public void decodeSequenceNumberOfOne() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010000000150000000000000001");
        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
        Assert.assertEquals(1, pdu0.getSequenceNumber());
        // despite having too large a sequence number, all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSequenceNumberMaxValue() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001000000015000000007fffffff");
        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);
        Assert.assertEquals(0x7fffffff, pdu0.getSequenceNumber());
        // despite having too large a sequence number, all the bytes should have been read
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeEnquireLink() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001000000015000000000a342ee7");

        EnquireLink pdu0 = (EnquireLink)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192039, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeEnquireLinkResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001080000015000000000a342eed");

        EnquireLinkResp pdu0 = (EnquireLinkResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192045, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeTwoEnquireLinkRespButWithReadRequiredInBetween() throws Exception {
        // f1 missing on end at first
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001080000015000000000a342eed0000001080000015000000000a342e");

        EnquireLinkResp pdu0 = (EnquireLinkResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192045, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());

        Assert.assertEquals(15, buffer.readableBytes());

        // second enquireLink response (but missing 1 byte)
        pdu0 = (EnquireLinkResp)transcoder.decode(buffer);
        Assert.assertNull(pdu0);

        Assert.assertEquals(15, buffer.readableBytes());

        // add 1 more byte (should finish the byte array off)
        ChannelBuffer buffer0 = BufferHelper.createBuffer("f1");
        ChannelBuffer buffer1 = ChannelBuffers.wrappedBuffer(buffer, buffer0);    // merge both buffers...
        buffer = buffer1;

        pdu0 = (EnquireLinkResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ENQUIRE_LINK_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192049, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSubmitSmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001c80000004000000000a342ee1393432353834333135393400");

        SubmitSmResp pdu0 = (SubmitSmResp)transcoder.decode(buffer);

        Assert.assertEquals(28, pdu0.getCommandLength());
        Assert.assertEquals(0x80000004, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192033, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        // messageId 94258431594
        Assert.assertEquals("94258431594", pdu0.getMessageId());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSubmitSmRespWithNoMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001080000004000000000a342ee1");

        SubmitSmResp pdu0 = (SubmitSmResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(0x80000004, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192033, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(null, pdu0.getMessageId());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSubmitSmRespWithEmptyMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001180000004000000000a342ee100");

        SubmitSmResp pdu0 = (SubmitSmResp)transcoder.decode(buffer);

        Assert.assertEquals(17, pdu0.getCommandLength());
        Assert.assertEquals(0x80000004, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192033, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("", pdu0.getMessageId());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeSubmitSmRespWithNoNullByteForMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001180000004000000000a342ee139");

        SubmitSmResp pdu0 = null;
        try {
            pdu0 = (SubmitSmResp)transcoder.decode(buffer);
            Assert.fail();
        } catch (TerminatingNullByteNotFoundException e) {
            // correct behavior (should still be able to get partial PDU
            pdu0 = (SubmitSmResp)e.getPartialPdu();
        }

        Assert.assertEquals(17, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_SUBMIT_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(171192033, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(null, pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001c800000050000000000116ac7393432353834333135393400");

        DeliverSmResp pdu0 = (DeliverSmResp)transcoder.decode(buffer);

        Assert.assertEquals(28, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("94258431594", pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmRespWithEmptyMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000011800000050000000000116ac700");

        DeliverSmResp pdu0 = (DeliverSmResp)transcoder.decode(buffer);

        Assert.assertEquals(17, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("", pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }
    
    @Test
    public void decodeDeliverSmRespWithNoMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800000050000000000116ac7");

        DeliverSmResp pdu0 = (DeliverSmResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(null, pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDataSmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001c800001030000000000116ac7393432353834333135393400");

        DataSmResp pdu0 = (DataSmResp)transcoder.decode(buffer);

        Assert.assertEquals(28, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DATA_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("94258431594", pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDataSmRespWithEmptyMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000011800001030000000000116ac700");

        DataSmResp pdu0 = (DataSmResp)transcoder.decode(buffer);

        Assert.assertEquals(17, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DATA_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("", pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDataSmRespWithNoMessageId() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800001030000000000116ac7");

        DataSmResp pdu0 = (DataSmResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DATA_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141447, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(null, pdu0.getMessageId());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindTransceiverResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001f800000090000000000039951536d73632053696d756c61746f7200");

        BindTransceiverResp pdu0 = (BindTransceiverResp)transcoder.decode(buffer);

        Assert.assertEquals(31, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235857, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("Smsc Simulator", pdu0.getSystemId());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindTransceiverRespFailedButWithSystemId() throws Exception {
        // this specific PDU actually failed with legacy smpp library
        ChannelBuffer buffer = BufferHelper.createBuffer("00000015800000090000000e00004db3534d534300");

        BindTransceiverResp pdu0 = (BindTransceiverResp)transcoder.decode(buffer);

        Assert.assertEquals(21, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP, pdu0.getCommandId());
        Assert.assertEquals(0x0000000e, pdu0.getCommandStatus());
        Assert.assertEquals(19891, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("SMSC", pdu0.getSystemId());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }
    
    @Test
    public void decodeBindTransceiverRespWithOptionalParams() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001d800000090000000000039943536d7363204757000210000134");

        BindTransceiverResp pdu0 = (BindTransceiverResp)transcoder.decode(buffer);

        Assert.assertEquals(29, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235843, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("Smsc GW", pdu0.getSystemId());
        Assert.assertEquals(1, pdu0.getOptionalParameters().size());
        Assert.assertEquals(true, pdu0.hasOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION));
        Assert.assertEquals(SmppConstants.TAG_SC_INTERFACE_VERSION, pdu0.getOptionalParameters().get(0).getTag());
        Assert.assertEquals(1, pdu0.getOptionalParameters().get(0).getLength());
        Assert.assertArrayEquals(new byte[] {(byte)0x34}, pdu0.getOptionalParameters().get(0).getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindTransmitterRespWithOptionalParams() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001d80000002000000000003995f54574954544552000210000134");

        BindTransmitterResp pdu0 = (BindTransmitterResp)transcoder.decode(buffer);

        Assert.assertEquals(29, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSMITTER_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235871, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("TWITTER", pdu0.getSystemId());
        Assert.assertEquals(1, pdu0.getOptionalParameters().size());
        Assert.assertEquals(true, pdu0.hasOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION));
        Assert.assertEquals(SmppConstants.TAG_SC_INTERFACE_VERSION, pdu0.getOptionalParameters().get(0).getTag());
        Assert.assertEquals(1, pdu0.getOptionalParameters().get(0).getLength());
        Assert.assertArrayEquals(new byte[] {(byte)0x34}, pdu0.getOptionalParameters().get(0).getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindReceiverRespWithOptionalParams() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001d80000001000000000003996274776974746572000210000134");

        BindReceiverResp pdu0 = (BindReceiverResp)transcoder.decode(buffer);

        Assert.assertEquals(29, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_RECEIVER_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235874, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals("twitter", pdu0.getSystemId());
        Assert.assertEquals(1, pdu0.getOptionalParameters().size());
        Assert.assertEquals(true, pdu0.hasOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION));
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION);
        Assert.assertEquals(SmppConstants.TAG_SC_INTERFACE_VERSION, tlv0.getTag());
        Assert.assertEquals(1, tlv0.getLength());
        Assert.assertArrayEquals(new byte[] { (byte)0x34 }, tlv0.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindTransceiver() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034010200");

        BindTransceiver pdu0 = (BindTransceiver)transcoder.decode(buffer);

        Assert.assertEquals(35, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSCEIVER, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235857, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("ALL_TW", pdu0.getSystemId());
        Assert.assertEquals("ALL_TW", pdu0.getPassword());
        Assert.assertEquals("", pdu0.getSystemType());
        Assert.assertEquals((byte)0x34, pdu0.getInterfaceVersion());
        Assert.assertEquals((byte)0x01, pdu0.getAddressRange().getTon());
        Assert.assertEquals((byte)0x02, pdu0.getAddressRange().getNpi());
        Assert.assertEquals("", pdu0.getAddressRange().getAddress());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindTransmitter() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000002500000002000000000003995f747769747465720074776974746572000034000000");

        BindTransmitter pdu0 = (BindTransmitter)transcoder.decode(buffer);

        Assert.assertEquals(37, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_TRANSMITTER, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235871, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("twitter", pdu0.getSystemId());
        Assert.assertEquals("twitter", pdu0.getPassword());
        Assert.assertEquals("", pdu0.getSystemType());
        Assert.assertEquals((byte)0x34, pdu0.getInterfaceVersion());
        Assert.assertEquals((byte)0x00, pdu0.getAddressRange().getTon());
        Assert.assertEquals((byte)0x00, pdu0.getAddressRange().getNpi());
        Assert.assertEquals("", pdu0.getAddressRange().getAddress());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeBindReceiver() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000025000000010000000000039961747769747465720074776974746572000034000000");

        BindReceiver pdu0 = (BindReceiver)transcoder.decode(buffer);

        Assert.assertEquals(37, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_BIND_RECEIVER, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(235873, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("twitter", pdu0.getSystemId());
        Assert.assertEquals("twitter", pdu0.getPassword());
        Assert.assertEquals("", pdu0.getSystemType());
        Assert.assertEquals((byte)0x34, pdu0.getInterfaceVersion());
        Assert.assertEquals((byte)0x00, pdu0.getAddressRange().getTon());
        Assert.assertEquals((byte)0x00, pdu0.getAddressRange().getNpi());
        Assert.assertEquals("", pdu0.getAddressRange().getAddress());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSm() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000400000000500000000000000030002013837363534333231000409343034303400000000000000000000084024232125262F3A000E0001010006000101");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(64, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(3, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x02, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("87654321", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x09, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(8, pdu0.getShortMessage().length);
        Assert.assertArrayEquals(HexUtil.toByteArray("4024232125262f3a"), pdu0.getShortMessage());

        Assert.assertEquals(2, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmAsDeliveryReceipt() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000BA00000005000000000000000200010134343935313336313932300001013430343034000400000000000000006E69643A30303539313133393738207375623A30303120646C7672643A303031207375626D697420646174653A3130303231303137333020646F6E6520646174653A3130303231303137333120737461743A44454C49565244206572723A30303020746578743A4024232125262F3A000E0001010006000101001E000833383630316661000427000102");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(186, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(2, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(110, pdu0.getShortMessage().length);
        Assert.assertArrayEquals(HexUtil.toByteArray("69643a30303539313133393738207375623a30303120646c7672643a303031207375626d697420646174653a3130303231303137333020646f6e6520646174653a3130303231303137333120737461743a44454c49565244206572723a30303020746578743a4024232125262f3a"), pdu0.getShortMessage());

        Assert.assertEquals(4, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID);
        Assert.assertEquals("38601fa", tlv2.getValueAsString());
        Tlv tlv3 = pdu0.getOptionalParameter(SmppConstants.TAG_MSG_STATE);
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, tlv3.getValueAsByte());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    /**
    @Test
    public void decodeDeliverSmNetforsDeliveryReceiptTriggeredErrorInLegacySmppLibrary() throws Exception {
        // interesting -- the optional parameter in this TLV is actually incorrect
        // it has a length of 0x41, but only 5 bytes were actually included
        // notified vendor this was a mistake, but perhaps we should try to make
        // this implementation work somehow?
        ChannelBuffer buffer = BufferHelper.createBuffer("000000a40000000500000000000000050001013233343830333337363831353100050056414e534f000400000000040000006369643a30303030303033303035207375623a30303120646c7672643a303031207375626d697420646174653a3130303131383134333120646f6e6520646174653a3130303131383134333120737461743a44454c49565244206572723a3030302074650427000102001e00413330303500");

        DeliverSm pdu0 = (DeliverSm)PduDecoder.decode(buffer);

        Assert.assertEquals(164, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(5, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("2348033768151", pdu0.getSourceAddress());
        Assert.assertEquals(0x05, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x00, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("VANSO", pdu0.getDestinationAddress());
        Assert.assertEquals(0x04, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x04, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(99, pdu0.getShortMessage().length);
        Assert.assertArrayEquals(StringUtil.fromHexString("69643a30303030303033303035207375623a30303120646c7672643a303031207375626d697420646174653a3130303131383134333120646f6e6520646174653a3130303131383134333120737461743a44454c49565244206572723a303030207465"), pdu0.getShortMessage());

        Assert.assertEquals(4, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_RECEIPTED_MSG_ID);
        Assert.assertEquals("38601fa", tlv2.getValueAsString());
        Tlv tlv3 = pdu0.getOptionalParameter(SmppConstants.TAG_MSG_STATE);
        Assert.assertEquals(SmppConstants.STATE_DELIVERED, tlv3.getValueAsByte());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }
     */

    @Test
    public void decodeSubmitSm() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000039000000040000000000004FE80001013430343034000101343439353133363139323000000000000001000000084024232125262F3A");

        SubmitSm pdu0 = (SubmitSm)transcoder.decode(buffer);

        Assert.assertEquals(57, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_SUBMIT_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x01, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(8, pdu0.getShortMessage().length);
        Assert.assertArrayEquals(HexUtil.toByteArray("4024232125262f3a"), pdu0.getShortMessage());

        Assert.assertEquals(null, pdu0.getOptionalParameters());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeUnbind() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010000000060000000000000001");

        Unbind pdu0 = (Unbind)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_UNBIND, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeUnbindResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800000060000000000000001");

        UnbindResp pdu0 = (UnbindResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_UNBIND_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeGenericNak() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800000000000000100082a77");

        GenericNack pdu0 = (GenericNack)transcoder.decode(buffer);
        logger.debug("{}", pdu0);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_GENERIC_NACK, pdu0.getCommandId());
        Assert.assertEquals(SmppConstants.STATUS_INVMSGLEN, pdu0.getCommandStatus());
        Assert.assertEquals(535159, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmWithOptionalMessagePayload() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000640000000500000000000547EB0002013434393531333631393200040934303430340000000000000000000000000E000101000600010104240026404D616964656E6D616E363634207761732069742073617070793F2026526F6D616E7469633F");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(100, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(346091, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x02, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("4495136192", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x09, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
        Assert.assertArrayEquals(HexUtil.toByteArray(""), pdu0.getShortMessage());

        Assert.assertEquals(3, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        Assert.assertArrayEquals(HexUtil.toByteArray("404d616964656e6d616e363634207761732069742073617070793f2026526f6d616e7469633f"), tlv2.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmWith2BytePayload() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000040000000050000000000657E4E0002013434393531333631393200040934303430340000000000000000000000000E0001010006000101042400024F6B");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(0x40, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(6651470, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x02, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("4495136192", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x09, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
        Assert.assertArrayEquals(HexUtil.toByteArray(""), pdu0.getShortMessage());

        Assert.assertEquals(3, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        Assert.assertArrayEquals(HexUtil.toByteArray("4f6b"), tlv2.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmWith1BytePayload() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000003F000000050000000000657E4E0002013434393531333631393200040934303430340000000000000000000000000E0001010006000101042400014F");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(0x3F, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(6651470, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x02, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("4495136192", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x09, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
        Assert.assertArrayEquals(HexUtil.toByteArray(""), pdu0.getShortMessage());

        Assert.assertEquals(3, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        Assert.assertArrayEquals(HexUtil.toByteArray("4f"), tlv2.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmWithNoPayload() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000003E000000050000000000657E4E0002013434393531333631393200040934303430340000000000000000000000000E000101000600010104240000");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(0x3E, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(6651470, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x02, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("4495136192", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x09, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
        Assert.assertArrayEquals(HexUtil.toByteArray(""), pdu0.getShortMessage());

        Assert.assertEquals(3, pdu0.getOptionalParameters().size());
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_SOURCE_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv0.getValueAsByte());
        Tlv tlv1 = pdu0.getOptionalParameter(SmppConstants.TAG_DEST_NETWORK_TYPE);
        Assert.assertEquals(0x01, tlv1.getValueAsByte());
        Tlv tlv2 = pdu0.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        Assert.assertArrayEquals(HexUtil.toByteArray(""), tlv2.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliverSmWithDeliveryReceiptThatFailedFromEndToEnd() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000A2000000050000000000116AD500010134343935313336313932303537000501475442616E6B000400000000010000006E69643A3934323531343330393233207375623A30303120646C7672643A303031207375626D697420646174653A3039313130343031323420646F6E6520646174653A3039313130343031323420737461743A41434345505444206572723A31303720746578743A20323646313032");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(162, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1141461, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("4495136192057", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x05, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("GTBank", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x01, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(110, pdu0.getShortMessageLength());
        Assert.assertArrayEquals(HexUtil.toByteArray("69643a3934323531343330393233207375623a30303120646c7672643a303031207375626d697420646174653a3039313130343031323420646f6e6520646174653a3039313130343031323420737461743a41434345505444206572723a31303720746578743a20323646313032"), pdu0.getShortMessage());

        Assert.assertEquals(0, pdu0.getOptionalParameterCount());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeDeliveryReceipt0() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000EB00000005000000000000000100010134343935313336313932300000013430343034000400000000000003009069643A3132366538356136656465616331613032303230303939333132343739353634207375623A30303120646C7672643A303031207375626D697420646174653A3130303231393136333020646F6E6520646174653A3130303231393136333020737461743A44454C49565244206572723A30303020546578743A48656C6C6F2020202020202020202020202020200427000102001E0021313236653835613665646561633161303230323030393933313234373935363400");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(235, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(1, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x04, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x03, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(144, pdu0.getShortMessageLength());
        //Assert.assertArrayEquals(HexUtil.toByteArray("69643a3934323531343330393233207375623a30303120646c7672643a303031207375626d697420646174653a3039313130343031323420646f6e6520646174653a3039313130343031323420737461743a41434345505444206572723a31303720746578743a20323646313032"), pdu0.getShortMessage());

        Assert.assertEquals(2, pdu0.getOptionalParameterCount());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeLargeSequenceNumber() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000400000000500000000A2859F22313030310001013434393531333631393230000001343034303430343034303430343034300000000000000000080000");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        Assert.assertEquals(64, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(-1568301278, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("1001", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("4040404040404040", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x08, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
    }
    
    @Test
    public void decodeWAUMalformedPacket() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000400000000500000000A2859F22313030310001013434393531333631393230000001343034303430343034303430343034300000000000000000080000");

        DeliverSm pdu0 = (DeliverSm)transcoder.decode(buffer);

        /**
        Assert.assertEquals(64, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DELIVER_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(-1568301278, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("1001", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("4040404040404040", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x08, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
         */
    }

    @Test
    public void decodeSubmitSmWith255ByteShortMessage() throws Exception {
        String text255 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum in orci magna. Etiam auctor ultrices lacus vel suscipit. Maecenas eget faucibus purus. Etiam aliquet mollis fermentum. Proin vel augue arcu. Praesent venenatis tristique ante turpis duis.";
        byte[] text255Bytes = text255.getBytes("ISO-8859-1");

        ChannelBuffer buffer = BufferHelper.createBuffer("00000130000000040000000000004FE80001013430343034000101343439353133363139323000000000000001000000FF" + HexUtil.toHexString(text255Bytes));

        SubmitSm pdu0 = (SubmitSm)transcoder.decode(buffer);

        Assert.assertEquals(304, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_SUBMIT_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals("", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("", pdu0.getValidityPeriod());
        Assert.assertEquals(0x01, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(255, pdu0.getShortMessage().length);
        Assert.assertArrayEquals(text255Bytes, pdu0.getShortMessage());

        Assert.assertEquals(null, pdu0.getOptionalParameters());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }
    
    @Ignore @Test
    public void decodeDeliverSMWithCorrectTotalByteLengthButInvalidShortMessageLength() throws Exception {
        // short_message is only 8 bytes, but short_message_length claims it should be 16
        // the problem is that the pdu_length in the header IS set to the correct length
        // this normally causes an IndexOutOfBoundsException on the ByteBuffer read, but
        // this should probably be caught and have a specific exception thrown instead!
        // MX nextel parsing exception in real world
        ChannelBuffer buffer = BufferHelper.createBuffer("00000039000000050000000000000001000101393939393139393134353933000000363436340000000000000000080010c1e1e9edf3faf1fc");

        DeliverSm pdu = (DeliverSm)transcoder.decode(buffer);

        // 999919914593
    }
    
    @Test
    public void decodeDeliverSMWithNotPerSpecSequenceNumberButNeedsToBeValid() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000390000000500000000806A179B000101393939393139393134353933000000363436340000000000000000080008c1e1e9edf3faf1fc");

        DeliverSm pdu = (DeliverSm)transcoder.decode(buffer);
        
        // confirm the sequence number was parsed ok
        Assert.assertEquals(pdu.getSequenceNumber(), 0x806A179B);

        // make sure the pdu is correct on a reply
        DeliverSmResp pduResponse = pdu.createResponse();
        
        ChannelBuffer respbuf = transcoder.encode(pduResponse);
        String actualHex = BufferHelper.createHexString(respbuf).toUpperCase();
        String expectedHex = "000000118000000500000000806A179B00";
        
        Assert.assertEquals(expectedHex, actualHex);
    }
    
    @Test
    public void decodeDataSM() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("000000300000010300000000000000000001013535353237313030303000000139363935000001000424000454657374");

        DataSm pdu0 = (DataSm)transcoder.decode(buffer);
        
        Assert.assertEquals(0x30, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_DATA_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(0, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("5552710000", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("9695", pdu0.getDestAddress().getAddress());
        Assert.assertEquals(0x00, pdu0.getEsmClass());
        Assert.assertEquals(0x01, pdu0.getRegisteredDelivery());
        Assert.assertEquals(0x00, pdu0.getDataCoding());
        
        // these are NOT actually part of data_sm's, but included for compatability
        // with submit_sm and delivery_sm
        Assert.assertEquals(0x00, pdu0.getProtocolId());
        Assert.assertEquals(0x00, pdu0.getPriority());
        Assert.assertEquals(null, pdu0.getScheduleDeliveryTime());
        Assert.assertEquals(null, pdu0.getValidityPeriod());
        Assert.assertEquals(0x00, pdu0.getReplaceIfPresent());
        Assert.assertEquals(0x00, pdu0.getDefaultMsgId());
        Assert.assertEquals(0, pdu0.getShortMessageLength());
        Assert.assertNull(pdu0.getShortMessage());
        
        
        Tlv tlv0 = pdu0.getOptionalParameter(SmppConstants.TAG_MESSAGE_PAYLOAD);
        Assert.assertArrayEquals("Test".getBytes("ISO-8859-1"), tlv0.getValue());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }


    @Test
    public void decodeCancelSm() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000002D000000080000000000004FE80031323334350001013430343034000101343439353133363139323000");

        CancelSm pdu0 = (CancelSm)transcoder.decode(buffer);

        Assert.assertEquals(45, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_CANCEL_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals("", pdu0.getServiceType());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getDestAddress().getNpi());
        Assert.assertEquals("44951361920", pdu0.getDestAddress().getAddress());
        Assert.assertEquals("12345", pdu0.getMessageId());

        Assert.assertEquals(null, pdu0.getOptionalParameters());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }


    @Test
    public void decodeQuerySm() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("0000001E000000030000000000004FE83132333435000101343034303400");

        QuerySm pdu0 = (QuerySm)transcoder.decode(buffer);

        Assert.assertEquals(30, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_QUERY_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("40404", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals("12345", pdu0.getMessageId());

        Assert.assertEquals(null, pdu0.getOptionalParameters());

        // interesting -- this example has optional parameters it happened to skip...
        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeCancelSmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800000080000000000004FE8");

        CancelSmResp pdu0 = (CancelSmResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_CANCEL_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isResponse());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeQuerySmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000019800000030000000000004FE8313233343500000600");

        QuerySmResp pdu0 = (QuerySmResp)transcoder.decode(buffer);

        Assert.assertEquals(25, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_QUERY_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals("12345", pdu0.getMessageId());
        Assert.assertEquals("", pdu0.getFinalDate());
        Assert.assertEquals((byte)0x06, pdu0.getMessageState());
        Assert.assertEquals((byte)0x00, pdu0.getErrorCode());
        Assert.assertEquals(true, pdu0.isResponse());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeReplaceSm() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000050000000070000000000004FE86D73672D313233343500010135353532373130303030003135303230333034303530363730382B00303130323033303430353036303030520001020474657874");

        ReplaceSm pdu0 = (ReplaceSm)transcoder.decode(buffer);

        Assert.assertEquals(80, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_REPLACE_SM, pdu0.getCommandId());
        Assert.assertEquals(0, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals(true, pdu0.isRequest());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getTon());
        Assert.assertEquals(0x01, pdu0.getSourceAddress().getNpi());
        Assert.assertEquals("5552710000", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals("150203040506708+", pdu0.getScheduleDeliveryTime());
        Assert.assertEquals("010203040506000R", pdu0.getValidityPeriod());
        Assert.assertEquals((byte)0x01, pdu0.getRegisteredDelivery());
        Assert.assertEquals((byte)0x02, pdu0.getDefaultMsgId());
        Assert.assertEquals("text", new String(pdu0.getShortMessage(), "ISO-8859-1"));

        Assert.assertEquals(null, pdu0.getOptionalParameters());

        Assert.assertEquals(0, buffer.readableBytes());
    }

    @Test
    public void decodeReplaceSmResp() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000010800000070000000200004FE8");

        ReplaceSmResp pdu0 = (ReplaceSmResp)transcoder.decode(buffer);

        Assert.assertEquals(16, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_REPLACE_SM_RESP, pdu0.getCommandId());
        Assert.assertEquals(true, pdu0.isResponse());
        Assert.assertEquals(2, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());

        Assert.assertEquals(0, buffer.readableBytes());
    }
    
    @Test
    public void decodeAlertNotification() throws Exception {
        ChannelBuffer buffer = BufferHelper.createBuffer("00000025000001020000000200004FE8010135353532373130303030000101343034303400");

        AlertNotification pdu0 = (AlertNotification)transcoder.decode(buffer);

        Assert.assertEquals(37, pdu0.getCommandLength());
        Assert.assertEquals(SmppConstants.CMD_ID_ALERT_NOTIFICATION, pdu0.getCommandId());
        Assert.assertEquals(false, pdu0.isResponse());
        Assert.assertEquals(2, pdu0.getCommandStatus());
        Assert.assertEquals(20456, pdu0.getSequenceNumber());
        Assert.assertEquals("5552710000", pdu0.getSourceAddress().getAddress());
        Assert.assertEquals("40404", pdu0.getEsmeAddress().getAddress());

        Assert.assertEquals(0, buffer.readableBytes());
    }
}
