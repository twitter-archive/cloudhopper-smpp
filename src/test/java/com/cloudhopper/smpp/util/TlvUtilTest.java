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
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class TlvUtilTest {
    private static final Logger logger = LoggerFactory.getLogger(TlvUtilTest.class);

    @Test
    public void createNullTerminatedStringTlv() throws Exception {
        Tlv tlv0 = null;

        // null string should just be 0x00
        tlv0 = TlvUtil.createNullTerminatedStringTlv((short)0x0001, null, "ISO-8859-1");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("00"), tlv0.getValue());

        tlv0 = TlvUtil.createNullTerminatedStringTlv((short)0x0001, "", "ISO-8859-1");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("00"), tlv0.getValue());

        tlv0 = TlvUtil.createNullTerminatedStringTlv((short)0x0001, "a");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("6100"), tlv0.getValue());

        tlv0 = TlvUtil.createNullTerminatedStringTlv((short)0x0001, "c1net");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("63316e657400"), tlv0.getValue());
    }

    @Test
    public void createFixedLengthStringTlv() throws Exception {
        Tlv tlv0 = null;

        tlv0 = TlvUtil.createFixedLengthStringTlv((short)0x0001, null, 2, "ISO-8859-1");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("0000"), tlv0.getValue());

        tlv0 = TlvUtil.createFixedLengthStringTlv((short)0x0001, "", 2);
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("0000"), tlv0.getValue());

        tlv0 = TlvUtil.createFixedLengthStringTlv((short)0x0001, "1", 2, "ISO-8859-1");
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("3100"), tlv0.getValue());

        tlv0 = TlvUtil.createFixedLengthStringTlv((short)0x0001, "12", 2);
        Assert.assertEquals((short)0x0001, tlv0.getTag());
        Assert.assertArrayEquals(HexUtil.toByteArray("3132"), tlv0.getValue());

        try {
            tlv0 = TlvUtil.createFixedLengthStringTlv((short)0x0001, "12", 1, "ISO-8859-1");
            Assert.fail();
        } catch (TlvConvertException e) {
            // correct behavior
        }
    }
}
