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
import com.cloudhopper.smpp.util.SmppUtil;
import org.junit.*;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppUtilTest {

    @Test
    public void isUserDataHeaderIndicatorEnabled() {
        Assert.assertEquals(false, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x01));
        Assert.assertEquals(false, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x90));
        Assert.assertEquals(false, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x80));
        Assert.assertEquals(true, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x40));
        Assert.assertEquals(true, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0x41));
        Assert.assertEquals(true, SmppUtil.isUserDataHeaderIndicatorEnabled((byte)0xC0));
    }

    @Test
    public void isReplyPathEnabled() {
        Assert.assertEquals(false, SmppUtil.isReplyPathEnabled((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isReplyPathEnabled((byte)0x01));
        Assert.assertEquals(true, SmppUtil.isReplyPathEnabled((byte)0x90));
        Assert.assertEquals(true, SmppUtil.isReplyPathEnabled((byte)0xC0));
        Assert.assertEquals(true, SmppUtil.isReplyPathEnabled((byte)0x80));
        Assert.assertEquals(false, SmppUtil.isReplyPathEnabled((byte)0x40));
        Assert.assertEquals(true, SmppUtil.isReplyPathEnabled((byte)0x81));
    }

    @Test
    public void isMessageTypeSmscDeliveryReceipt() {
        Assert.assertEquals(false, SmppUtil.isMessageTypeSmscDeliveryReceipt((byte)0x00));
        Assert.assertEquals(true, SmppUtil.isMessageTypeSmscDeliveryReceipt((byte)0x04));
        // set both intermediate AND dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeSmscDeliveryReceipt((byte)0x24));
        // intermediate set, but esme receipt
        Assert.assertEquals(false, SmppUtil.isMessageTypeSmscDeliveryReceipt((byte)0x28));
        // udh set & intermediate & dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeSmscDeliveryReceipt((byte)0x64));
    }

    @Test
    public void isMessageTypeIntermediateDeliveryReceipt() {
        Assert.assertEquals(false, SmppUtil.isMessageTypeIntermediateDeliveryReceipt((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isMessageTypeIntermediateDeliveryReceipt((byte)0x04));
        // set both intermediate AND dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeIntermediateDeliveryReceipt((byte)0x24));
        // intermediate set, but esme receipt
        Assert.assertEquals(true, SmppUtil.isMessageTypeIntermediateDeliveryReceipt((byte)0x28));
        // udh set & intermediate & dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeIntermediateDeliveryReceipt((byte)0x64));
    }

    @Test
    public void isMessageTypeEsmeDeliveryReceipt() {
        Assert.assertEquals(false, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x04));
        Assert.assertEquals(true, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x08));
        // set both intermediate AND dlr
        Assert.assertEquals(false, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x24));
        // intermediate set, but esme receipt
        Assert.assertEquals(true, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x28));
        // udh set & intermediate & dlr
        Assert.assertEquals(false, SmppUtil.isMessageTypeEsmeDeliveryReceipt((byte)0x64));
    }

    @Test
    public void isMessageTypeAnyDeliveryReceipt() {
        Assert.assertEquals(false, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x00));
        Assert.assertEquals(true, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x04));
        Assert.assertEquals(true, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x08));
        // set both intermediate AND dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x24));
        // intermediate set, but esme receipt
        Assert.assertEquals(true, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x28));
        // udh set & intermediate & dlr
        Assert.assertEquals(true, SmppUtil.isMessageTypeAnyDeliveryReceipt((byte)0x64));
    }


    @Test
    public void isSmscDeliveryReceiptRequested() {
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptRequested((byte)0x00));
        Assert.assertEquals(true, SmppUtil.isSmscDeliveryReceiptRequested((byte)0x01));
        Assert.assertEquals(true, SmppUtil.isSmscDeliveryReceiptRequested((byte)0x21));
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptRequested((byte)0x20));
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptRequested((byte)0x02));
    }

    @Test
    public void isSmscDeliveryReceiptOnFailureRequested() {
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptOnFailureRequested((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptOnFailureRequested((byte)0x01));
        Assert.assertEquals(true, SmppUtil.isSmscDeliveryReceiptOnFailureRequested((byte)0x22));
        Assert.assertEquals(false, SmppUtil.isSmscDeliveryReceiptOnFailureRequested((byte)0x20));
        Assert.assertEquals(true, SmppUtil.isSmscDeliveryReceiptOnFailureRequested((byte)0x02));
    }

    @Test
    public void isIntermediateReceiptRequested() {
        Assert.assertEquals(false, SmppUtil.isIntermediateReceiptRequested((byte)0x00));
        Assert.assertEquals(false, SmppUtil.isIntermediateReceiptRequested((byte)0x01));
        Assert.assertEquals(true, SmppUtil.isIntermediateReceiptRequested((byte)0x12));
        // this is actually bit 4 not bit 5 (SMPP 3.4 specs originally had both bits mentioned)
        Assert.assertEquals(true, SmppUtil.isIntermediateReceiptRequested((byte)0x10));
        Assert.assertEquals(false, SmppUtil.isIntermediateReceiptRequested((byte)0x02));
    }

    @Test
    public void toInterfaceVersionString() {
        Assert.assertEquals("3.4", SmppUtil.toInterfaceVersionString((byte)0x34));
        Assert.assertEquals("0.3", SmppUtil.toInterfaceVersionString((byte)0x03));
    }

}
