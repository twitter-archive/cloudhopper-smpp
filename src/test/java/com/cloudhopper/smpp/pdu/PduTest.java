package com.cloudhopper.smpp.pdu;

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
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// my imports

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class PduTest {
    private static final Logger logger = LoggerFactory.getLogger(PduTest.class);
    
    @Test
    public void hasSequenceNumberAssigned() throws Exception {
        Pdu pdu0 = new EnquireLink();

        Assert.assertEquals(false, pdu0.hasSequenceNumberAssigned());

        pdu0.setSequenceNumber(0);

        Assert.assertEquals(true, pdu0.hasSequenceNumberAssigned());

        pdu0.removeSequenceNumber();

        Assert.assertEquals(false, pdu0.hasSequenceNumberAssigned());
        Assert.assertEquals(0, pdu0.getSequenceNumber());
    }

    @Test
    public void hasCommandLengthCalculatedAndSet() throws Exception {
        Pdu pdu0 = new EnquireLink();

        Assert.assertEquals(false, pdu0.hasCommandLengthCalculated());
        Assert.assertEquals(16, pdu0.calculateAndSetCommandLength());
        Assert.assertEquals(true, pdu0.hasCommandLengthCalculated());
        Assert.assertEquals(16, pdu0.getCommandLength());
    }
}
