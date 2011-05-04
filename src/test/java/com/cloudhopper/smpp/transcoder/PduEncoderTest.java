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

package com.cloudhopper.smpp.transcoder;

// third party imports
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.BindReceiver;
import com.cloudhopper.smpp.pdu.BindReceiverResp;
import com.cloudhopper.smpp.pdu.BindTransceiver;
import com.cloudhopper.smpp.pdu.BindTransceiverResp;
import com.cloudhopper.smpp.pdu.BindTransmitter;
import com.cloudhopper.smpp.pdu.BindTransmitterResp;
import com.cloudhopper.smpp.pdu.BufferHelper;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.pdu.UnbindResp;
import com.cloudhopper.smpp.tlv.Tlv;
import org.junit.*;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class PduEncoderTest {
    private static final Logger logger = LoggerFactory.getLogger(PduEncoderTest.class);

    private PduTranscoderContext context;
    private PduTranscoder transcoder;

    public PduEncoderTest() {
        this.context = new DefaultPduTranscoderContext();
        this.transcoder = new DefaultPduTranscoder(this.context);
    }

    @Test
    public void encodeEnquireLink() throws Exception {
        EnquireLink pdu0 = new EnquireLink();
        pdu0.setCommandStatus(0);
        pdu0.setSequenceNumber(171192039);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001000000015000000000a342ee7"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeEnquireLinkResp() throws Exception {
        EnquireLinkResp pdu0 = new EnquireLinkResp();
        pdu0.setCommandStatus(0);
        pdu0.setSequenceNumber(171192045);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001080000015000000000a342eed"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeSubmitSmResp() throws Exception {
        SubmitSmResp pdu0 = new SubmitSmResp();
        pdu0.setSequenceNumber(171192033);
        pdu0.setMessageId("94258431594");

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001c80000004000000000a342ee1393432353834333135393400"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeSubmitSmRespWithNoMessageId() throws Exception {
        SubmitSmResp pdu0 = new SubmitSmResp();
        pdu0.setSequenceNumber(171192033);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001180000004000000000a342ee100"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeDeliverSmResp() throws Exception {
        DeliverSmResp pdu0 = new DeliverSmResp();
        pdu0.setSequenceNumber(1141447);
        pdu0.setMessageId("94258431594");

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001c800000050000000000116ac7393432353834333135393400"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeDataSmResp() throws Exception {
        DataSmResp pdu0 = new DataSmResp();
        pdu0.setSequenceNumber(1141447);
        pdu0.setMessageId("94258431594");

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001c800001030000000000116ac7393432353834333135393400"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransceiverResp() throws Exception {
        BindTransceiverResp pdu0 = new BindTransceiverResp();
        pdu0.setSequenceNumber(235857);
        pdu0.setSystemId("Smsc Simulator");

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001f800000090000000000039951536d73632053696d756c61746f7200"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransceiverRespFailedButWithSystemId() throws Exception {
        BindTransceiverResp pdu0 = new BindTransceiverResp();
        pdu0.setSequenceNumber(19891);
        pdu0.setCommandStatus(0x0000000e);
        pdu0.setSystemId("SMSC");

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("00000015800000090000000e00004db3534d534300"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransceiverRespWithOptionalParams() throws Exception {
        BindTransceiverResp pdu0 = new BindTransceiverResp();
        pdu0.setSequenceNumber(235843);
        pdu0.setCommandStatus(0);
        pdu0.setSystemId("Smsc GW");
        pdu0.setOptionalParameter(new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { (byte)0x34 }));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001d800000090000000000039943536d7363204757000210000134"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransmitterRespWithOptionalParams() throws Exception {
        BindTransmitterResp pdu0 = new BindTransmitterResp();
        pdu0.setSequenceNumber(235871);
        pdu0.setCommandStatus(0);
        pdu0.setSystemId("TWITTER");
        pdu0.setOptionalParameter(new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { (byte)0x34 }));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001d80000002000000000003995f54574954544552000210000134"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindReceiverRespWithOptionalParams() throws Exception {
        BindReceiverResp pdu0 = new BindReceiverResp();
        pdu0.setSequenceNumber(235874);
        pdu0.setCommandStatus(0);
        pdu0.setSystemId("twitter");
        pdu0.setOptionalParameter(new Tlv(SmppConstants.TAG_SC_INTERFACE_VERSION, new byte[] { (byte)0x34 }));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("0000001d80000001000000000003996274776974746572000210000134"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransceiverWithNullAddressRange() throws Exception {
        BindTransceiver pdu0 = new BindTransceiver();
        pdu0.setSequenceNumber(235857);
        pdu0.setSystemId("ALL_TW");
        pdu0.setPassword("ALL_TW");
        //pdu0.setSystemType(""); // don't set, shouldn't change the value
        pdu0.setInterfaceVersion((byte)0x34);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034000000"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransceiverWithAddressRange() throws Exception {
        BindTransceiver pdu0 = new BindTransceiver();
        pdu0.setSequenceNumber(235857);
        pdu0.setSystemId("ALL_TW");
        pdu0.setPassword("ALL_TW");
        //pdu0.setSystemType(""); // don't set, shouldn't change the value
        pdu0.setInterfaceVersion((byte)0x34);
        pdu0.setAddressRange(new Address());
        pdu0.getAddressRange().setTon((byte)0x01);
        pdu0.getAddressRange().setNpi((byte)0x02);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("00000023000000090000000000039951414c4c5f545700414c4c5f5457000034010200"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeBindTransmitter() throws Exception {
        BindTransmitter pdu0 = new BindTransmitter();
        pdu0.setSequenceNumber(235871);
        pdu0.setSystemId("twitter");
        pdu0.setPassword("twitter");
        //pdu0.setSystemType(""); // don't set, shouldn't change the value
        pdu0.setInterfaceVersion((byte)0x34);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("0000002500000002000000000003995f747769747465720074776974746572000034000000"), BufferHelper.createByteArray(buffer));
    }
    
    @Test
    public void encodeBindReceiver() throws Exception {
        BindReceiver pdu0 = new BindReceiver();
        pdu0.setSequenceNumber(235873);
        pdu0.setSystemId("twitter");
        pdu0.setPassword("twitter");
        //pdu0.setSystemType(""); // don't set, shouldn't change the value
        pdu0.setInterfaceVersion((byte)0x34);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("00000025000000010000000000039961747769747465720074776974746572000034000000"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeDeliverSm() throws Exception {
        DeliverSm pdu0 = new DeliverSm();

        pdu0.setSequenceNumber(3);
        pdu0.setSourceAddress(new Address((byte)0x02, (byte)0x01, "87654321"));
        pdu0.setDestAddress(new Address((byte)0x04, (byte)0x09, "40404"));
        pdu0.setEsmClass((byte)0x00);
        pdu0.setProtocolId((byte)0x00);
        pdu0.setPriority((byte)0x00);
        pdu0.setScheduleDeliveryTime(null);
        pdu0.setValidityPeriod(null);
        pdu0.setRegisteredDelivery((byte)0x00);
        pdu0.setReplaceIfPresent((byte)0x00);
        pdu0.setDataCoding((byte)0x00);
        pdu0.setDefaultMsgId((byte)0x00);
        pdu0.setShortMessage(HexUtil.toByteArray("4024232125262f3a"));

        // order is important
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_SOURCE_NETWORK_TYPE, new byte[] { (byte)0x01 }));
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_TYPE, new byte[] { (byte)0x01 }));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("000000400000000500000000000000030002013837363534333231000409343034303400000000000000000000084024232125262F3A000E0001010006000101"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeDeliverSmAsDeliveryReceipt() throws Exception {
        DeliverSm pdu0 = new DeliverSm();

        pdu0.setSequenceNumber(2);
        pdu0.setSourceAddress(new Address((byte)0x01, (byte)0x01, "44951361920"));
        pdu0.setDestAddress(new Address((byte)0x01, (byte)0x01, "40404"));
        pdu0.setEsmClass((byte)0x04);
        pdu0.setProtocolId((byte)0x00);
        pdu0.setPriority((byte)0x00);
        pdu0.setScheduleDeliveryTime(null);
        pdu0.setValidityPeriod(null);
        pdu0.setRegisteredDelivery((byte)0x00);
        pdu0.setReplaceIfPresent((byte)0x00);
        pdu0.setDataCoding((byte)0x00);
        pdu0.setDefaultMsgId((byte)0x00);
        pdu0.setShortMessage(HexUtil.toByteArray("69643a30303539313133393738207375623a30303120646c7672643a303031207375626d697420646174653a3130303231303137333020646f6e6520646174653a3130303231303137333120737461743a44454c49565244206572723a30303020746578743a4024232125262f3a"));

        // order is important
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_SOURCE_NETWORK_TYPE, new byte[] { (byte)0x01 }));
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_DEST_NETWORK_TYPE, new byte[] { (byte)0x01 }));
        // NOTE: VERY IMPORTANT -- THIS IS A C-STRING!
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_RECEIPTED_MSG_ID, "38601fa\u0000".getBytes()));
        pdu0.addOptionalParameter(new Tlv(SmppConstants.TAG_MSG_STATE, new byte[] { SmppConstants.STATE_DELIVERED }));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("000000BA00000005000000000000000200010134343935313336313932300001013430343034000400000000000000006E69643A30303539313133393738207375623A30303120646C7672643A303031207375626D697420646174653A3130303231303137333020646F6E6520646174653A3130303231303137333120737461743A44454C49565244206572723A30303020746578743A4024232125262F3A000E0001010006000101001E000833383630316661000427000102"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeSubmitSm() throws Exception {
        SubmitSm pdu0 = new SubmitSm();

        pdu0.setSequenceNumber(20456);
        pdu0.setSourceAddress(new Address((byte)0x01, (byte)0x01, "40404"));
        pdu0.setDestAddress(new Address((byte)0x01, (byte)0x01, "44951361920"));
        pdu0.setEsmClass((byte)0x00);
        pdu0.setProtocolId((byte)0x00);
        pdu0.setPriority((byte)0x00);
        pdu0.setScheduleDeliveryTime(null);
        pdu0.setValidityPeriod(null);
        pdu0.setRegisteredDelivery((byte)0x01);
        pdu0.setReplaceIfPresent((byte)0x00);
        pdu0.setDataCoding((byte)0x00);
        pdu0.setDefaultMsgId((byte)0x00);
        pdu0.setShortMessage(HexUtil.toByteArray("4024232125262f3a"));

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("00000039000000040000000000004FE80001013430343034000101343439353133363139323000000000000001000000084024232125262F3A"), BufferHelper.createByteArray(buffer));
    }
    
    @Test
    public void encodeDataSmWithEmptyMessage() throws Exception {
        DataSm pdu0 = new DataSm();

        pdu0.setSequenceNumber(3);
        pdu0.setSourceAddress(new Address((byte)0x02, (byte)0x01, "87654321"));
        pdu0.setDestAddress(new Address((byte)0x04, (byte)0x09, "40404"));
        pdu0.setEsmClass((byte)0x00);
        pdu0.setProtocolId((byte)0x00);
        pdu0.setPriority((byte)0x00);
        pdu0.setScheduleDeliveryTime(null);
        pdu0.setValidityPeriod(null);
        pdu0.setRegisteredDelivery((byte)0x00);
        pdu0.setReplaceIfPresent((byte)0x00);
        pdu0.setDataCoding((byte)0x00);
        pdu0.setDefaultMsgId((byte)0x00);
        pdu0.setShortMessage(null);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        //logger.debug("{}", HexUtil.toHexString(BufferHelper.createByteArray(buffer)));
        Assert.assertArrayEquals(HexUtil.toByteArray("0000002E000001030000000000000003000201383736353433323100040934303430340000000000000000000000"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeUnbind() throws Exception {
        Unbind pdu0 = new Unbind();
        pdu0.setCommandStatus(0);
        pdu0.setSequenceNumber(1);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("00000010000000060000000000000001"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeUnbindResp() throws Exception {
        UnbindResp pdu0 = new UnbindResp();
        pdu0.setCommandStatus(0);
        pdu0.setSequenceNumber(1);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("00000010800000060000000000000001"), BufferHelper.createByteArray(buffer));
    }

    @Test
    public void encodeGenericNack() throws Exception {
        GenericNack pdu0 = new GenericNack();
        pdu0.setCommandStatus(SmppConstants.STATUS_INVMSGLEN);
        pdu0.setSequenceNumber(535159);

        ChannelBuffer buffer = transcoder.encode(pdu0);
        Assert.assertArrayEquals(HexUtil.toByteArray("00000010800000000000000100082a77"), BufferHelper.createByteArray(buffer));
    }

}
