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
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.TerminatingNullByteNotFoundException;
import com.cloudhopper.smpp.type.NotEnoughDataInBufferException;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.pdu.*;
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
public class ChannelBufferUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(ChannelBufferUtilTest.class);

    @Test
    public void readNullTerminatedString() throws Exception {
        // normal case with a termination zero
        ChannelBuffer buffer0 = BufferHelper.createBuffer("343439353133363139323000");
        String str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("44951361920", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(0, buffer0.readableBytes());

        // another case with an extra byte after NULL byte
        buffer0 = BufferHelper.createBuffer("343439353133363139323000FF");
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("44951361920", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(1, buffer0.readableBytes());

        // another case with an first extra byte
        buffer0 = BufferHelper.createBuffer("39343439353133363139323000");
        buffer0.readByte(); // skip 1 byte
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("44951361920", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(0, buffer0.readableBytes());

        // another case with an first extra byte and last extra byte
        buffer0 = BufferHelper.createBuffer("39343439353133363139323000FF");
        buffer0.readByte(); // skip 1 byte
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("44951361920", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(1, buffer0.readableBytes());

        // another case with an empty string
        buffer0 = BufferHelper.createBuffer("00");
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(0, buffer0.readableBytes());

        // another case with an empty string and last extra byte
        buffer0 = BufferHelper.createBuffer("0039");
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertEquals("", str0);
        // make sure the entire buffer is still there we started with
        Assert.assertEquals(1, buffer0.readableBytes());

        // no bytes left to read in buffer will return null
        buffer0 = BufferHelper.createBuffer("");
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        Assert.assertNull(str0);
        Assert.assertEquals(0, buffer0.readableBytes());

        // no terminating zero
        try {
            buffer0 = BufferHelper.createBuffer("39");
            str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
            Assert.fail();
        } catch (TerminatingNullByteNotFoundException e) {
            // correct behavior
            Assert.assertEquals(1, buffer0.readableBytes());
        }

        // unsupported latin-1 chars?
        buffer0 = BufferHelper.createBuffer("0100");
        str0 = ChannelBufferUtil.readNullTerminatedString(buffer0);
        // correct behavior
        Assert.assertEquals(0, buffer0.readableBytes());
    }

    @Test
    public void writeNullTerminatedString() throws Exception {
        ChannelBuffer buffer0 = ChannelBuffers.dynamicBuffer(10);

        // handle null case
        buffer0.clear();
        ChannelBufferUtil.writeNullTerminatedString(buffer0, null);
        Assert.assertArrayEquals(HexUtil.toByteArray("00"), BufferHelper.createByteArray(buffer0));

        buffer0.clear();
        ChannelBufferUtil.writeNullTerminatedString(buffer0, "");
        Assert.assertArrayEquals(HexUtil.toByteArray("00"), BufferHelper.createByteArray(buffer0));

        buffer0.clear();
        ChannelBufferUtil.writeNullTerminatedString(buffer0, "A");
        Assert.assertArrayEquals(HexUtil.toByteArray("4100"), BufferHelper.createByteArray(buffer0));
    }

    @Test
    public void readTlv() throws Exception {
        Tlv tlv0 = null;
        ChannelBuffer buffer0 = null;

        // a single byte TLV
        buffer0 = BufferHelper.createBuffer("0210000134");
        tlv0 = ChannelBufferUtil.readTlv(buffer0);

        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0x0210, tlv0.getTag());
        Assert.assertEquals((short)0x01, tlv0.getLength());
        Assert.assertArrayEquals(new byte[] {0x34}, tlv0.getValue());

        // a C-string TLV
        buffer0 = BufferHelper.createBuffer("140200056331657400");
        tlv0 = ChannelBufferUtil.readTlv(buffer0);

        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0x1402, tlv0.getTag());
        Assert.assertEquals((short)0x05, tlv0.getLength());
        Assert.assertArrayEquals(HexUtil.toByteArray("6331657400"), tlv0.getValue());

        // a short or just 2 byte TLV
        buffer0 = BufferHelper.createBuffer("02040002ce34");
        tlv0 = ChannelBufferUtil.readTlv(buffer0);

        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0x0204, tlv0.getTag());
        Assert.assertEquals((short)0x02, tlv0.getLength());
        Assert.assertArrayEquals(HexUtil.toByteArray("ce34"), tlv0.getValue());

        // a sample message payload TLV
        buffer0 = BufferHelper.createBuffer("0424002f4f4d4720492077616e7420746f207365652022546865204372617a69657322206c6f6f6b73207369636b21203d5d20");
        tlv0 = ChannelBufferUtil.readTlv(buffer0);
        // OMG I want to see "The Crazies" looks sick! =]
        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0x0424, tlv0.getTag());
        Assert.assertEquals((short)0x2f, tlv0.getLength());
        // convert bytes to actual chars
        Assert.assertEquals("OMG I want to see \"The Crazies\" looks sick! =] ", new String(tlv0.getValue()));

        // multiple TLVs in a row
        buffer0 = BufferHelper.createBuffer("000e000101000600010104240001");
        tlv0 = ChannelBufferUtil.readTlv(buffer0);
        Assert.assertEquals(9, buffer0.readableBytes());
        Assert.assertEquals((short)0x0e, tlv0.getTag());
        Assert.assertEquals((short)0x01, tlv0.getLength());
        Assert.assertArrayEquals(HexUtil.toByteArray("01"), tlv0.getValue());

        tlv0 = ChannelBufferUtil.readTlv(buffer0);
        Assert.assertEquals(4, buffer0.readableBytes());
        Assert.assertEquals((short)0x06, tlv0.getTag());
        Assert.assertEquals((short)0x01, tlv0.getLength());
        Assert.assertArrayEquals(HexUtil.toByteArray("01"), tlv0.getValue());

        try {
            // this should error out since we don't have enough bytes
            tlv0 = ChannelBufferUtil.readTlv(buffer0);
            Assert.fail();
        } catch (NotEnoughDataInBufferException e) {
            // correct behavior
            Assert.assertEquals(0, buffer0.readableBytes());
        }

        // a TLV with an unsigned short length (1 above 15-bit integer)
        StringBuilder buf = new StringBuilder(40000);
        buf.append("FFFF8000");
        for (int i = 0; i < 0x8000; i++) {
            buf.append("01");
        }
        buffer0 = BufferHelper.createBuffer(buf.toString());
        tlv0 = ChannelBufferUtil.readTlv(buffer0);
        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0xffff, tlv0.getTag());
        Assert.assertEquals(32768, tlv0.getUnsignedLength());
        Assert.assertEquals(-32768, tlv0.getLength());  // the "signed" version of the length

        // a TLV with an unsigned short length (1 above 15-bit integer)
        buf = new StringBuilder(70000);
        buf.append("FFFFFFFF");
        for (int i = 0; i < 0xFFFF; i++) {
            buf.append("02");
        }
        buffer0 = BufferHelper.createBuffer(buf.toString());
        tlv0 = ChannelBufferUtil.readTlv(buffer0);
        Assert.assertEquals(0, buffer0.readableBytes());
        Assert.assertEquals((short)0xffff, tlv0.getTag());
        Assert.assertEquals(-1, tlv0.getLength());  // the "signed" version of the length
    }

    @Test
    public void writeTlv() throws Exception {
        Tlv tlv0 = null;
        ChannelBuffer buffer0 = null;

        buffer0 = ChannelBuffers.dynamicBuffer(10);

        // handle null case
        buffer0.clear();
        ChannelBufferUtil.writeTlv(buffer0, tlv0);
        Assert.assertArrayEquals(HexUtil.toByteArray(""), BufferHelper.createByteArray(buffer0));

        buffer0.clear();
        tlv0 = new Tlv((short)0xFFFF, new byte[] { 0x41, 0x42 });
        ChannelBufferUtil.writeTlv(buffer0, tlv0);
        Assert.assertArrayEquals(HexUtil.toByteArray("FFFF00024142"), BufferHelper.createByteArray(buffer0));
    }

    @Test
    public void readAddress() throws Exception {
        Address addr0 = null;
        ChannelBuffer buffer0 = null;

        buffer0 = BufferHelper.createBuffer("021000");
        addr0 = ChannelBufferUtil.readAddress(buffer0);
        Assert.assertEquals(0x02, addr0.getTon());
        Assert.assertEquals(0x10, addr0.getNpi());
        Assert.assertEquals("", addr0.getAddress());
        Assert.assertEquals(0, buffer0.readableBytes());

        // same, but one extra byte shouldn't be read
        buffer0 = BufferHelper.createBuffer("02100000");
        addr0 = ChannelBufferUtil.readAddress(buffer0);
        Assert.assertEquals(0x02, addr0.getTon());
        Assert.assertEquals(0x10, addr0.getNpi());
        Assert.assertEquals("", addr0.getAddress());
        Assert.assertEquals(1, buffer0.readableBytes());

        buffer0 = BufferHelper.createBuffer("02104142434400");
        addr0 = ChannelBufferUtil.readAddress(buffer0);
        Assert.assertEquals(0x02, addr0.getTon());
        Assert.assertEquals(0x10, addr0.getNpi());
        Assert.assertEquals("ABCD", addr0.getAddress());
        Assert.assertEquals(0, buffer0.readableBytes());
    }

    @Test
    public void writeAddress() throws Exception {
        Address addr0 = null;
        ChannelBuffer buffer0 = null;

        buffer0 = ChannelBuffers.dynamicBuffer(10);

        // handle null case
        buffer0.clear();
        ChannelBufferUtil.writeAddress(buffer0, addr0);
        Assert.assertArrayEquals(HexUtil.toByteArray("000000"), BufferHelper.createByteArray(buffer0));

        // handle default address
        buffer0.clear();
        ChannelBufferUtil.writeAddress(buffer0, new Address());
        Assert.assertArrayEquals(HexUtil.toByteArray("000000"), BufferHelper.createByteArray(buffer0));

        // handle some stuff in address
        buffer0.clear();
        ChannelBufferUtil.writeAddress(buffer0, new Address((byte)0x01, (byte)0x02, ""));
        Assert.assertArrayEquals(HexUtil.toByteArray("010200"), BufferHelper.createByteArray(buffer0));

        buffer0.clear();
        ChannelBufferUtil.writeAddress(buffer0, new Address((byte)0x01, (byte)0x02, "ABC"));
        Assert.assertArrayEquals(HexUtil.toByteArray("010241424300"), BufferHelper.createByteArray(buffer0));
    }
}
