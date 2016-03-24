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

import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.commons.util.StringUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.type.Address;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppInvalidArgumentException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.ChannelBufferUtil;
import com.cloudhopper.smpp.util.PduUtil;

import org.jboss.netty.buffer.ChannelBuffer;

public class ReplaceSm extends PduRequest<ReplaceSmResp> {

    private String messageId;
    private Address sourceAddress;
    private String scheduleDeliveryTime;
    private String validityPeriod;
    private byte registeredDelivery;
    private byte defaultMsgId;
    private byte[] shortMessage; 
    
    public ReplaceSm() {
        super(SmppConstants.CMD_ID_REPLACE_SM, "replace_sm");
    }

    @Override
    public ReplaceSmResp createResponse() {
        ReplaceSmResp resp = new ReplaceSmResp();
        resp.setSequenceNumber(this.getSequenceNumber());
        return resp;
    }

    @Override
    public Class<ReplaceSmResp> getResponseClass() {
        return ReplaceSmResp.class;
    }
    
    
    public String getMessageId(){
        return messageId;
    }
    
    public void setMessageId(String messageId){
        this.messageId = messageId;
    }
    
    public int getShortMessageLength() {
        return (this.shortMessage == null ? 0 : this.shortMessage.length);
    }

    public byte[] getShortMessage() {
        return this.shortMessage;
    }

    public void setShortMessage(byte[] value) throws SmppInvalidArgumentException {
        if (value != null && value.length > 255) {
            throw new SmppInvalidArgumentException("A short message in a PDU can only be a max of 255 bytes [actual=" + value.length + "]; use optional parameter message_payload as an alternative");
        }
        this.shortMessage = value;
    }

    public byte getDefaultMsgId() {
        return this.defaultMsgId;
    }

    public void setDefaultMsgId(byte value) {
        this.defaultMsgId = value;
    }

    public byte getRegisteredDelivery() {
        return this.registeredDelivery;
    }

    public void setRegisteredDelivery(byte value) {
        this.registeredDelivery = value;
    }

    public String getValidityPeriod() {
        return this.validityPeriod;
    }

    public void setValidityPeriod(String value) {
        this.validityPeriod = value;
    }

    public String getScheduleDeliveryTime() {
        return this.scheduleDeliveryTime;
    }

    public void setScheduleDeliveryTime(String value) {
        this.scheduleDeliveryTime = value;
    }

    public Address getSourceAddress() {
        return this.sourceAddress;
    }

    public void setSourceAddress(Address value) {
        this.sourceAddress = value;
    }

    @Override
    public void readBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        this.messageId = ChannelBufferUtil.readNullTerminatedString(buffer); 
        this.sourceAddress = ChannelBufferUtil.readAddress(buffer);
        this.scheduleDeliveryTime = ChannelBufferUtil.readNullTerminatedString(buffer);
        this.validityPeriod = ChannelBufferUtil.readNullTerminatedString(buffer);
        this.registeredDelivery = buffer.readByte();
        this.defaultMsgId = buffer.readByte();
        // this is always an unsigned version of the short message length
        short shortMessageLength = buffer.readUnsignedByte();
        this.shortMessage = new byte[shortMessageLength];
        buffer.readBytes(this.shortMessage);
    }

    @Override
    public int calculateByteSizeOfBody() {
        int bodyLength = 0;
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.messageId);
        bodyLength += PduUtil.calculateByteSizeOfAddress(this.sourceAddress);
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.scheduleDeliveryTime);
        bodyLength += PduUtil.calculateByteSizeOfNullTerminatedString(this.validityPeriod);
        bodyLength += 3;    // regDelivery, defaultMsgId, messageLength bytes
        bodyLength += getShortMessageLength();
        return bodyLength;
    }

    @Override
    public void writeBody(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        ChannelBufferUtil.writeNullTerminatedString(buffer, this.messageId);
        ChannelBufferUtil.writeAddress(buffer, this.sourceAddress);
        ChannelBufferUtil.writeNullTerminatedString(buffer, this.scheduleDeliveryTime);
        ChannelBufferUtil.writeNullTerminatedString(buffer, this.validityPeriod);
        buffer.writeByte(this.registeredDelivery);
        buffer.writeByte(this.defaultMsgId);
        buffer.writeByte((byte)getShortMessageLength());
        if (this.shortMessage != null) {
            buffer.writeBytes(this.shortMessage);
        }
    }

    @Override
    public void appendBodyToString(StringBuilder buffer) {
        buffer.append("( messageId [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.messageId));
        buffer.append("] sourceAddr [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.sourceAddress));
        buffer.append("] scheduleDeliveryTime [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.scheduleDeliveryTime));
        buffer.append("] validityPeriod [");
        buffer.append(StringUtil.toStringWithNullAsEmpty(this.validityPeriod));
        buffer.append("] regDlvry [0x");
        buffer.append(HexUtil.toHexString(this.registeredDelivery));
        buffer.append("] message [");
        HexUtil.appendHexString(buffer, this.shortMessage);
        buffer.append("])");
    }

}