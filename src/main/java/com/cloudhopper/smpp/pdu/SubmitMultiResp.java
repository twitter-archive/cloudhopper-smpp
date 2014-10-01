package com.cloudhopper.smpp.pdu;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2012 Cloudhopper by Twitter
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
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SubmitMultiUnsuccessSme;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.ChannelBufferUtil;
import com.cloudhopper.smpp.util.PduUtil;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Iterator;
import java.util.List;

/**
 * @author innar.made@ishisystems.com
 */
public class SubmitMultiResp extends BaseSmResp {

    private List<SubmitMultiUnsuccessSme> unsuccessSmes;

    public SubmitMultiResp() {
        super(SmppConstants.CMD_ID_SUBMIT_MULTI_RESP, "submit_multi_resp");
    }

    public int getNumberOfUnsuccessSmes() {
        if (unsuccessSmes == null || unsuccessSmes.isEmpty()) {
            return 0;
        }
        return unsuccessSmes.size();
    }

    public List<SubmitMultiUnsuccessSme> getUnsuccessSmes() {
        return unsuccessSmes;
    }

    public void setUnsuccessSmes(List<SubmitMultiUnsuccessSme> unsuccessSmes) {
        this.unsuccessSmes = unsuccessSmes;
    }

    @Override
    public void readBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        super.readBody(buffer);
        unsuccessSmes = ChannelBufferUtil.readSubmitMultiUnsuccessSmeList(buffer);
    }

    @Override
    public int calculateByteSizeOfBody() {
        int bodyLength = super.calculateByteSizeOfBody();
        bodyLength += PduUtil.calculateByteSizeOfSubmitMultiUnsucessSmeList(getUnsuccessSmes());
        return bodyLength;
    }

    @Override
    public void writeBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        ChannelBufferUtil.writeNullTerminatedString(buffer, getMessageId());
        ChannelBufferUtil.writeSubmitMultiUnsuccessSmeList(buffer, getUnsuccessSmes());
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) {
        buffer.append("(messageId [").append(StringUtil.toStringWithNullAsEmpty(getMessageId()));
        int numberOfUnsuccessSmes = getNumberOfUnsuccessSmes();
        buffer.append("] no_unsuccess [").append(numberOfUnsuccessSmes);
        buffer.append("] unsuccess_smes [");
        if (numberOfUnsuccessSmes > 0) {
            Iterator<SubmitMultiUnsuccessSme> iterator = unsuccessSmes.iterator();
            while (iterator.hasNext()) {
                SubmitMultiUnsuccessSme unsuccessSme = iterator.next();
                buffer.append(StringUtil.toStringWithNullAsNull(unsuccessSme));
                if (iterator.hasNext()) {
                    buffer.append(", ");
                }
            }
        } else {
            buffer.append("])");
        }
    }
}
