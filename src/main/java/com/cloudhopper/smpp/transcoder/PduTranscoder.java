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

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.type.RecoverablePduException;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Interface for encoding/decoding PDUs to/from ChannelBuffers.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface PduTranscoder {

    /**
     * Encodes a PDU into a new ChannelBuffer.
     * @param pdu The PDU to convert into a buffer
     * @return The new ChannelBuffer ready to send on a Channel
     * @throws UnrecoverablePduEncodingException Thrown if there is an unrecoverable
     *      error while encoding the buffer.  Recommended action is to rebind
     *      the session.
     * @throws RecoverablePduEncodingException Thrown if there is recoverable
     *      error while encoding the buffer. A good example is an optional parameter
     *      that is invalid or a terminating null byte wasn't found.
     */
    public ChannelBuffer encode(Pdu pdu) throws UnrecoverablePduException, RecoverablePduException;

    /**
     * Decodes a ChannelBuffer into a new PDU.
     * @param buffer The buffer to read data from
     * @return The new PDU created from the data
     * @throws UnrecoverablePduEncodingException Thrown if there is an unrecoverable
     *      error while decoding the buffer.  Recommended action is to rebind
     *      the session.
     * @throws RecoverablePduEncodingException Thrown if there is recoverable
     *      error while decoding the buffer. A good example is an optional parameter
     *      that is invalid or a terminating null byte wasn't found.
     */
    public Pdu decode(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException;
    
}