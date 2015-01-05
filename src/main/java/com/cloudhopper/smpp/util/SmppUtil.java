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

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppConstants;

/**
 * Utility class for working with SMPP such as encoding/decoding a short
 * message, esm class, or registered delivery flags.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppUtil {

    /**
     * Does the "esm_class" value have a message type set at all?  This basically
     * checks if the "esm_class" could either be SMSC delivery receipt, ESME delivery receipt,
     * manual user acknowledgement, conversation abort, or an intermediate delivery receipt.
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isMessageTypeAnyDeliveryReceipt(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_MT_MASK) > 0);
    }

    /**
     * Does the "esm_class" value have a message type of "manual/user acknowledgment"?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isMessageTypeManualUserAcknowledgement(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_MT_MASK) == SmppConstants.ESM_CLASS_MT_MANUAL_USER_ACK);
    }

    /**
     * Does the "esm_class" value have a message type of "ESME delivery receipt"?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isMessageTypeEsmeDeliveryReceipt(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_MT_MASK) == SmppConstants.ESM_CLASS_MT_ESME_DELIVERY_RECEIPT);
    }

    /**
     * Does the "esm_class" value have a message type of "intermediate delivery receipt"?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isMessageTypeIntermediateDeliveryReceipt(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_INTERMEDIATE_DELIVERY_RECEIPT_FLAG) == SmppConstants.ESM_CLASS_INTERMEDIATE_DELIVERY_RECEIPT_FLAG);
    }

    /**
     * Does the "esm_class" value have a message type of "SMSC delivery receipt"?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isMessageTypeSmscDeliveryReceipt(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_MT_MASK) == SmppConstants.ESM_CLASS_MT_SMSC_DELIVERY_RECEIPT);
    }

    /**
     * Does the "esm_class" value have the "user data header present" bit set?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isUserDataHeaderIndicatorEnabled(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_UDHI_MASK) == SmppConstants.ESM_CLASS_UDHI_MASK);
    }

    /**
     * Does the "esm_class" value have the "reply path" bit set?
     * @param esmClass The "esm_class" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isReplyPathEnabled(byte esmClass) {
        return ((esmClass & SmppConstants.ESM_CLASS_REPLY_PATH_MASK) == SmppConstants.ESM_CLASS_REPLY_PATH_MASK);
    }

    /**
     * Does the "registered_delivery" value have the "SMSC delivery receipt" bit set?
     * This bit represents both a success and failure receipt is requested.
     * @param registeredDelivery The "registered_delivery" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isSmscDeliveryReceiptRequested(byte registeredDelivery) {
        return ((registeredDelivery & SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_MASK) == SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_REQUESTED);
    }

    /**
     * Does the "registered_delivery" value have the "SMSC delivery receipt on failure" bit set?
     * This bit represents only a receipt on failure is requested.
     * @param registeredDelivery The "registered_delivery" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isSmscDeliveryReceiptOnFailureRequested(byte registeredDelivery) {
        return ((registeredDelivery & SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_MASK) == SmppConstants.REGISTERED_DELIVERY_SMSC_RECEIPT_ON_FAILURE);
    }

    /**
     * Does the "registered_delivery" value have the "intermediate receipt" bit set?
     * @param registeredDelivery The "registered_delivery" value to evaluate
     * @return True if the option is set, otherwise false.
     */
    static public boolean isIntermediateReceiptRequested(byte registeredDelivery) {
        return ((registeredDelivery & SmppConstants.REGISTERED_DELIVERY_INTERMEDIATE_NOTIFICATION_MASK) == SmppConstants.REGISTERED_DELIVERY_INTERMEDIATE_NOTIFICATION_REQUESTED);
    }

    /**
     * Converts a byte value such as 0x34 into a version string such as "3.4"
     * @param interfaceVersion
     * @return
     */
    static public String toInterfaceVersionString(byte interfaceVersion) {
        String ver = HexUtil.toHexString(interfaceVersion);
        if (ver == null || ver.length() != 2) {
            return ver;
        } else {
            return ver.substring(0,1) + "." + ver.substring(1);
        }
    }
}
