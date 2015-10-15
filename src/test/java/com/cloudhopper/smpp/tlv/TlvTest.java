package com.cloudhopper.smpp.tlv;

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
import com.cloudhopper.commons.util.HexUtil;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class TlvTest {
    private static final Logger logger = LoggerFactory.getLogger(TlvTest.class);

    @Test
    public void nullValue() throws Exception {
        Tlv tlv0 = new Tlv((short)0x0001, null);
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertEquals(0, tlv0.getUnsignedLength());
        Assert.assertEquals(0, tlv0.getLength());
        Assert.assertArrayEquals(null, tlv0.getValue());
        Assert.assertEquals("tlv: 0x0001 0x0000 []", tlv0.toString());
    }

    @Test
    public void emptyValue() throws Exception {
        Tlv tlv0 = new Tlv((short)0xFFFF, new byte[0]);
        Assert.assertEquals((short)0xFFFF, tlv0.getTag());
        Assert.assertEquals(0, tlv0.getUnsignedLength());
        Assert.assertEquals(0, tlv0.getLength());
        Assert.assertArrayEquals(new byte[0], tlv0.getValue());
        Assert.assertEquals("tlv: 0xFFFF 0x0000 []", tlv0.toString());
    }

    @Test
    public void stringValue() throws Exception {
        Tlv tlv0 = new Tlv((short)0x0001, new byte[] { 0x41 });
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertEquals(1, tlv0.getUnsignedLength());
        Assert.assertEquals(1, tlv0.getLength());
        Assert.assertArrayEquals(new byte[] { 0x41 }, tlv0.getValue());
        Assert.assertEquals("tlv: 0x0001 0x0001 [41]", tlv0.toString());
        Assert.assertEquals("A", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x00 });
        Assert.assertEquals("A", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x00, 0x00 });
        Assert.assertEquals("A", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x00 });
        Assert.assertEquals("", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { });
        Assert.assertEquals("", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, null);
        Assert.assertEquals(null, tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x42, 0x43, 0x44 });
        Assert.assertEquals("ABCD", tlv0.getValueAsString());

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x42, 0x43, 0x44, 0x00 });
        Assert.assertEquals("ABCD", tlv0.getValueAsString());

        // try UCS2 encoding
        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x42 });
        Assert.assertEquals("\u4142", tlv0.getValueAsString("UTF-16BE"));

        tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x42, 0x00 });
        Assert.assertEquals("\u4142", tlv0.getValueAsString("UTF-16BE"));

        // try unknown encoding
        try {
            tlv0 = new Tlv((short)0x0001, new byte[] { 0x41, 0x42, 0x00 });
            tlv0.getValueAsString("BUNK");
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
            //logger.debug("{}", e);
        }
    }

    @Test
    public void longByteArrayValue() throws Exception {
        // let's create a 50K byte long TLV
        byte[] longValue = new byte[50000];
        Tlv tlv0 = new Tlv((short)0x0001, longValue);
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertEquals(50000, tlv0.getUnsignedLength());       // unsigned length of TLV
        Assert.assertEquals("C350", HexUtil.toHexString(tlv0.getLength()));
        Assert.assertEquals((short)0xC350, tlv0.getLength());
        Assert.assertEquals("tlv: 0x0001 0xC350", tlv0.toString().substring(0, 18));
        // first byte is NULL, so it should return an empty string
        Assert.assertEquals("", tlv0.getValueAsString());
    }

    @Test
    public void getValueAsByte() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { 0x01 });
        Assert.assertEquals((short)0xF001, tlv0.getTag());
        Assert.assertEquals(1, tlv0.getUnsignedLength());
        Assert.assertEquals(1, tlv0.getLength());
        Assert.assertArrayEquals(new byte[] { 0x01 }, tlv0.getValue());
        Assert.assertEquals("tlv: 0xF001 0x0001 [01]", tlv0.toString());
        Assert.assertEquals(0x01, tlv0.getValueAsByte());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02 });
        try {
            tlv1.getValueAsByte();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsByte();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsUnsignedByte() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { (byte)0xF0 });
        Assert.assertEquals((short)240, tlv0.getValueAsUnsignedByte());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02 });
        try {
            tlv1.getValueAsUnsignedByte();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsUnsignedByte();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsShort() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { 0x01, 0x02 });
        Assert.assertEquals(0x0102, tlv0.getValueAsShort());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02, 0x03 });
        try {
            tlv1.getValueAsShort();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsShort();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsUnsignedShort() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { (byte)0xF0, 0x02 });
        Assert.assertEquals(61442, tlv0.getValueAsUnsignedShort());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02, 0x03 });
        try {
            tlv1.getValueAsUnsignedShort();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsUnsignedShort();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsInt() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { 0x01, 0x02, 0x03, 0x04 });
        Assert.assertEquals(0x01020304, tlv0.getValueAsInt());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
        try {
            tlv1.getValueAsInt();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsInt();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsUnsignedInt() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { (byte)0xF0, 0x02, 0x03, 0x04 });
        Assert.assertEquals(4026663684L, tlv0.getValueAsUnsignedInt());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05 });
        try {
            tlv1.getValueAsUnsignedInt();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsUnsignedInt();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void getValueAsLong() throws Exception {
        Tlv tlv0 = new Tlv((short)0xF001, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x01, 0x02, 0x03, 0x04 });
        Assert.assertEquals(0x0102030401020304L, tlv0.getValueAsLong());


        Tlv tlv1 = new Tlv((short)0x0002, new byte[] { 0x01, 0x02, 0x03, 0x04, 0x05, 0x01, 0x02, 0x03, 0x04 });
        try {
            tlv1.getValueAsLong();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }

        Tlv tlv2 = new Tlv((short)0x0003, null);
        try {
            tlv2.getValueAsLong();
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }

    @Test
    public void testEquals() throws Exception{
        Tlv tlv0 = new Tlv((short) 0x88, "equals".getBytes());
        // Trivial case
        Assert.assertTrue(tlv0.equals(tlv0));
        Tlv tlv0Mirror = new Tlv((short) 0x88, "equals".getBytes());
        // Reflexivity
        Assert.assertEquals(tlv0, tlv0);
        // Symmetry
        Assert.assertTrue(tlv0.equals(tlv0Mirror));
        Assert.assertTrue(tlv0Mirror.equals(tlv0));
        
        // Transitivity
        Tlv tlv0Transitive = new Tlv((short) 0x88, "equals".getBytes());
        Assert.assertTrue(tlv0.equals(tlv0Transitive));
        Assert.assertTrue(tlv0Mirror.equals(tlv0Transitive));

        // Non nullity
        Assert.assertFalse(tlv0.equals(null));

        // Some more non-equality tests
        Tlv differentTag = new Tlv((short) 0x77, "equals".getBytes());
        Tlv differentVal = new Tlv((short) 0x88, "nonequals".getBytes());
        Tlv differentAll = new Tlv((short) 0x77, "nonequals".getBytes());

        Assert.assertNotEquals(tlv0, differentTag);
        Assert.assertNotEquals(tlv0, differentVal);
        Assert.assertNotEquals(tlv0, differentAll);
    }

    @Test
    public void testHashCode() throws Exception{
        Tlv tlv0 = new Tlv((short) 0x88, "equals".getBytes());
        Tlv tlv0Mirror = new Tlv((short) 0x88, "equals".getBytes());
        Assert.assertEquals(tlv0.hashCode(), tlv0Mirror.hashCode());

        Tlv differentTag = new Tlv((short) 0x77, "equals".getBytes());
        Tlv differentVal = new Tlv((short) 0x88, "nonequals".getBytes());
        Tlv differentAll = new Tlv((short) 0x77, "nonequals".getBytes());

        Assert.assertNotEquals(tlv0.hashCode(), differentTag.hashCode());
        Assert.assertNotEquals(tlv0.hashCode(), differentVal.hashCode());
        Assert.assertNotEquals(tlv0.hashCode(), differentAll.hashCode());
    }

}
