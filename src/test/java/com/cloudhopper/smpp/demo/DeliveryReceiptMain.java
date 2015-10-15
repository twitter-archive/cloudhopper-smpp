package com.cloudhopper.smpp.demo;

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

import com.cloudhopper.smpp.util.DeliveryReceipt;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DeliveryReceiptMain {
    private static final Logger logger = LoggerFactory.getLogger(DeliveryReceiptMain.class);

    static public void main(String[] args) throws Exception {
        //DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("id:4 sub:001 dlvrd:001 submit date:1006020051 done date:1006020051 stat:DELIVRD err:000 Text:Hello", DateTimeZone.UTC, true);

        //DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("sub:001 id:4 dlvrd:001 done date:1006020051 stat:DELIVRD submit date:1006020051 err:000 Text:Hello", DateTimeZone.UTC, true);

        //DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("sub:001 id:4 dlvrd:001 done date:1006020051 stat:DELIVRD submit date:1006020051 err:000 text:", DateTimeZone.UTC, true);

        DeliveryReceipt dlr = DeliveryReceipt.parseShortMessage("id:2E179B310EDE971B2760C72B7F026E1B submit date:20110314181534 done date:20110314181741 stat:DELIVRD err:0", DateTimeZone.UTC, false);

        logger.debug("{}", dlr);
    }

}