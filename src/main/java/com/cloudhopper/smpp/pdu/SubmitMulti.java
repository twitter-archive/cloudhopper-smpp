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
import com.cloudhopper.smpp.type.SubmitMultiDestinationAddress;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.ChannelBufferUtil;
import com.cloudhopper.smpp.util.PduUtil;
import org.jboss.netty.buffer.ChannelBuffer;

import java.util.Iterator;
import java.util.List;

/**
 * @author innar.made@ishisystems.com
 */
public class SubmitMulti extends BaseSm<SubmitMultiResp> {

    private List<SubmitMultiDestinationAddress> submitMultiDestinationAddressList;

    public SubmitMulti() {
        super(SmppConstants.CMD_ID_SUBMIT_MULTI, "submit_multi");
    }

    public List<SubmitMultiDestinationAddress> getSubmitMultiDestinationAddressList() {
        return this.submitMultiDestinationAddressList;
    }

    public void setSubmitMultiDestinationAddressList(List<SubmitMultiDestinationAddress> addressList) {
        this.submitMultiDestinationAddressList = addressList;
    }

    public int getNumberOfDestinations() {
        return submitMultiDestinationAddressList == null ? 0 : submitMultiDestinationAddressList.size();
    }

    @Override
    public SubmitMultiResp createResponse() {
        SubmitMultiResp resp = new SubmitMultiResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<SubmitMultiResp> getResponseClass() {
        return SubmitMultiResp.class;
    }

    @Override
    protected void readDestinationAddress(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        submitMultiDestinationAddressList = ChannelBufferUtil.readSubmitMultiAddressList(buffer);
    }

    @Override
    protected void writeDestinationAddress(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        ChannelBufferUtil.writeSubmitMultiAddressList(buffer, submitMultiDestinationAddressList);
    }

    @Override
    protected int calculateDestinationAddressSize() {
        return PduUtil.calculateByteSizeOfSubmitMultiAddressList(submitMultiDestinationAddressList);
    }

    @Override
    protected void appendDestinationAddressToString(StringBuilder buffer) {
        appendSubmitMultiDestinationAddressList(buffer);
    }

    private void appendSubmitMultiDestinationAddressList(StringBuilder buffer) {
        int numberOfDestinations = getNumberOfDestinations();
        buffer.append("] number_of_dests [").append(numberOfDestinations);
        buffer.append("] dest_addresses [");
        if (numberOfDestinations > 0) {
            Iterator<SubmitMultiDestinationAddress> iterator = getSubmitMultiDestinationAddressList().iterator();
            while (iterator.hasNext()) {
                SubmitMultiDestinationAddress address = iterator.next();
                buffer.append("(").append(StringUtil.toStringWithNullAsEmpty(address)).append(")");
                if (iterator.hasNext()) {
                    buffer.append(",");
                }
            }
        }
    }
}
