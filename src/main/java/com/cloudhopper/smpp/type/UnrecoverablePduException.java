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

package com.cloudhopper.smpp.type;

/**
 * Thrown when an unrecoverable PDU decoding error occurs. A good example is
 * a PDU length that is so large its signed negative (31st bit is 1).  This is
 * an impossible error and likely means something is drastically wrong with
 * the sequence of bytes.
 *
 * Another example is an invalid sequence number in an invalid range.  If
 * an invalid sequenceNumber is used, the recommended action is to close the
 * session.
 * 
 * The recommended action for an unrecoverable error is to close the SMPP 
 * connection.
 *
 * @author joelauer
 */
public class UnrecoverablePduException extends Exception {
    static final long serialVersionUID = 1L;
    
    public UnrecoverablePduException(String msg) {
        super(msg);
    }

    public UnrecoverablePduException(String msg, Throwable t) {
        super(msg, t);
    }
}