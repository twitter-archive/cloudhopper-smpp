/**
 * 
 */
package com.cloudhopper.smpp.pdu;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2013 Cloudhopper by Twitter
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

import io.netty.buffer.ByteBuf;

import java.util.ArrayList;
import java.util.List;

import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.ByteBufUtil;
import com.cloudhopper.smpp.util.PduUtil;

/**
 * @author Amit Bhayani
 * 
 */
public class SubmitMulti extends BaseSm<SubmitMultiResp> {

	private int numberOfDest;

	private List<Address> destAddresses = new ArrayList<Address>();
	private List<String> destDistributionList = new ArrayList<String>();

	/**
	 * @param commandId
	 * @param name
	 */
	public SubmitMulti() {
		super(SmppConstants.CMD_ID_SUBMIT_MULTI, "submit_multi");
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cloudhopper.smpp.pdu.PduRequest#createResponse()
	 */
	@Override
	public SubmitMultiResp createResponse() {
		SubmitMultiResp resp = new SubmitMultiResp();
		resp.setSequenceNumber(this.getSequenceNumber());
		return resp;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.cloudhopper.smpp.pdu.PduRequest#getResponseClass()
	 */
	@Override
	public Class<SubmitMultiResp> getResponseClass() {
		return SubmitMultiResp.class;
	}

	@Override
	public Address getDestAddress() {
		return null;
	}

	@Override
	public void setDestAddress(Address value) {

	}

	public void addDestAddresses(Address address)
			throws SmppInvalidArgumentException {
		this.numberOfDest++;
		this.destAddresses.add(address);
	}

	public void addDestDestributionListName(String name) {
		this.numberOfDest++;
		this.destDistributionList.add(name);
	}

	public List<Address> getDestAddresses() {
		return this.destAddresses;
	}

	public List<String> getDestDestributionListName() {
		return this.destDistributionList;
	}
	
	public int getNumberOfDest(){
		return this.numberOfDest;
	}

    @Override
    public void readBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException {
        this.serviceType = ByteBufUtil.readNullTerminatedString(buffer);
        this.sourceAddress = ByteBufUtil.readAddress(buffer);

        this.numberOfDest = buffer.readUnsignedByte();

        for (int count = 0; count < this.numberOfDest; count++) {
            byte flag = buffer.readByte();
            if (flag == SmppConstants.SME_ADDRESS) {
                this.destAddresses.add(ByteBufUtil.readAddress(buffer));
            } else if (flag == SmppConstants.DISTRIBUTION_LIST_NAME) {
                this.destDistributionList.add(ByteBufUtil.readNullTerminatedString(buffer));
            }
        }

        this.esmClass = buffer.readByte();
        this.protocolId = buffer.readByte();
        this.priority = buffer.readByte();
        this.scheduleDeliveryTime = ByteBufUtil.readNullTerminatedString(buffer);
        this.validityPeriod = ByteBufUtil.readNullTerminatedString(buffer);
        this.registeredDelivery = buffer.readByte();
        this.replaceIfPresent = buffer.readByte();
        this.dataCoding = buffer.readByte();
        this.defaultMsgId = buffer.readByte();
        // this is always an unsigned version of the short message length
        short shortMessageLength = buffer.readUnsignedByte();
        this.shortMessage = new byte[shortMessageLength];
        buffer.readBytes(this.shortMessage);
    }

	@Override
    public void writeBody(ByteBuf buffer) throws UnrecoverablePduException, RecoverablePduException {
        ByteBufUtil.writeNullTerminatedString(buffer, this.serviceType);
        ByteBufUtil.writeAddress(buffer, this.sourceAddress);

        buffer.writeByte(this.numberOfDest);

        for (Address adress : this.destAddresses) {
            buffer.writeByte(SmppConstants.SME_ADDRESS);
            ByteBufUtil.writeAddress(buffer, adress);
        }

        for (String s : this.destDistributionList) {
            buffer.writeByte(SmppConstants.DISTRIBUTION_LIST_NAME);
            ByteBufUtil.writeNullTerminatedString(buffer, s);

        }

        buffer.writeByte(this.esmClass);
        buffer.writeByte(this.protocolId);
        buffer.writeByte(this.priority);
        ByteBufUtil.writeNullTerminatedString(buffer, this.scheduleDeliveryTime);
        ByteBufUtil.writeNullTerminatedString(buffer, this.validityPeriod);
        buffer.writeByte(this.registeredDelivery);
        buffer.writeByte(this.replaceIfPresent);
        buffer.writeByte(this.dataCoding);
        buffer.writeByte(this.defaultMsgId);
        buffer.writeByte(getShortMessageLength());
        if (this.shortMessage != null) {
            buffer.writeBytes(this.shortMessage);
        }
    }

    @Override
    public int calculateByteSizeOfBody() {
        int bodyLength = 0;
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.serviceType);
        bodyLength += PduUtil.calculateByteSizeOfAddress(this.sourceAddress);
        
        bodyLength +=1; //number_of_dests
        
        for(Address adress : this.destAddresses){
        	bodyLength += 1;//Flag
        	bodyLength += PduUtil.calculateByteSizeOfAddress(adress);
        }
        
		for(String s : this.destDistributionList){
			bodyLength += 1;//Flag
			bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(s);
		}
		
        bodyLength += 3;    // esmClass, priority, protocolId
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.scheduleDeliveryTime);
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.validityPeriod);
        bodyLength += 5;    // regDelivery, replace, dataCoding, defaultMsgId, messageLength bytes
        bodyLength += getShortMessageLength();
        return bodyLength;
    }
}
