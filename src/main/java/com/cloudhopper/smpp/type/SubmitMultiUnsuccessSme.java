package com.cloudhopper.smpp.type;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2014 Cloudhopper by Twitter
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

import com.cloudhopper.commons.util.StringUtil;
import com.cloudhopper.smpp.util.ChannelBufferUtil;
import com.cloudhopper.smpp.util.PduUtil;
import org.jboss.netty.buffer.ChannelBuffer;

public class SubmitMultiUnsuccessSme {

    private Address address;
    private int errorStatusCode;

    public SubmitMultiUnsuccessSme() {
    }

    public SubmitMultiUnsuccessSme(final Address address, final int errorStatusCode) {
        this.address = address;
        this.errorStatusCode = errorStatusCode;
    }

    public Address getAddress() {
        return address;
    }

    public void setAddress(final Address address) {
        this.address = address;
    }

    public int getErrorStatusCode() {
        return errorStatusCode;
    }

    public void setErrorStatusCode(final int errorStatusCode) {
        this.errorStatusCode = errorStatusCode;
    }

    public void readBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        address = ChannelBufferUtil.readAddress(buffer);
        errorStatusCode = buffer.readInt();
    }

    public int calculateByteSizeOfBody() {
        return PduUtil.calculateByteSizeOfAddress(address) + 4; //errorStatusCode
    }

    public void writeBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        ChannelBufferUtil.writeAddress(buffer, address);
        buffer.writeInt(errorStatusCode);
    }

    public void appendBodyToString(StringBuilder buffer) {
        buffer.append("(address [").append(StringUtil.toStringWithNullAsEmpty(address));
        buffer.append("] error_status_code [").append(errorStatusCode).append("])");
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("(address [").append(StringUtil.toStringWithNullAsEmpty(address));
        buffer.append("] error_status_code [").append(errorStatusCode).append("])");
        return buffer.toString();
    }
}
