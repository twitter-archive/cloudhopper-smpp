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

import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.type.UnknownCommandIdException;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.commons.util.HexUtil;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.pdu.AlertNotification;
import com.cloudhopper.smpp.pdu.BindReceiver;
import com.cloudhopper.smpp.pdu.BindReceiverResp;
import com.cloudhopper.smpp.pdu.BindTransceiver;
import com.cloudhopper.smpp.pdu.BindTransceiverResp;
import com.cloudhopper.smpp.pdu.BindTransmitter;
import com.cloudhopper.smpp.pdu.BindTransmitterResp;
import com.cloudhopper.smpp.pdu.CancelSm;
import com.cloudhopper.smpp.pdu.CancelSmResp;
import com.cloudhopper.smpp.pdu.DataSm;
import com.cloudhopper.smpp.pdu.DataSmResp;
import com.cloudhopper.smpp.pdu.DeliverSm;
import com.cloudhopper.smpp.pdu.DeliverSmResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.GenericNack;
import com.cloudhopper.smpp.pdu.PartialPdu;
import com.cloudhopper.smpp.pdu.PartialPduResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.QuerySm;
import com.cloudhopper.smpp.pdu.QuerySmResp;
import com.cloudhopper.smpp.pdu.ReplaceSm;
import com.cloudhopper.smpp.pdu.ReplaceSmResp;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.pdu.UnbindResp;
import com.cloudhopper.smpp.type.NotEnoughDataInBufferException;
import com.cloudhopper.smpp.util.PduUtil;
import com.cloudhopper.smpp.util.SequenceNumber;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultPduTranscoder implements PduTranscoder {

    private final PduTranscoderContext context;

    public DefaultPduTranscoder(PduTranscoderContext context) {
        this.context = context;
    }

    @Override
    public ChannelBuffer encode(Pdu pdu) throws UnrecoverablePduException, RecoverablePduException {
        // see if we can map the command status into a message
        if (pdu instanceof PduResponse) {
            PduResponse response = (PduResponse)pdu;
            if (response.getResultMessage() == null) {
                response.setResultMessage(context.lookupResultMessage(pdu.getCommandStatus()));
            }
        }

        // if the pdu length hasn't been assigned yet, calculate it now
        // NOTE: it may be safest to recalculate it, but we won't since the SmppSession
        // should really be the only interface creating PDUs
        if (!pdu.hasCommandLengthCalculated()) {
            pdu.calculateAndSetCommandLength();
        }

        // create the buffer and add the header
        ChannelBuffer buffer = new BigEndianHeapChannelBuffer(pdu.getCommandLength());

        buffer.writeInt(pdu.getCommandLength());
        buffer.writeInt(pdu.getCommandId());
        buffer.writeInt(pdu.getCommandStatus());
        buffer.writeInt(pdu.getSequenceNumber());

        // add mandatory body (a noop if no body exists)
        pdu.writeBody(buffer);

        // add optional parameters (a noop if none exist)
        pdu.writeOptionalParameters(buffer, context);

        // NOTE: at this point, the entire buffer written MUST match the command length
        // from earlier -- if it doesn't match, the our encoding process went awry
        if (buffer.readableBytes() != pdu.getCommandLength()) {
            throw new NotEnoughDataInBufferException("During PDU encoding the expected commandLength did not match the actual encoded (a serious error with our own encoding process)", pdu.getCommandLength(), buffer.readableBytes());
        }

        return buffer;
    }
    
    @Override
    public Pdu decode(ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        // wait until the length prefix is available
        if (buffer.readableBytes() < SmppConstants.PDU_INT_LENGTH) {
            return null;
        }

        // parse the command length (first 4 bytes)
        int commandLength = buffer.getInt(buffer.readerIndex());
        //logger.trace("PDU commandLength [" + commandLength + "]");

        // valid command length is >= 16 bytes
        if (commandLength < SmppConstants.PDU_HEADER_LENGTH) {
            throw new UnrecoverablePduException("Invalid PDU length [0x" + HexUtil.toHexString(commandLength) + "] parsed");
        }

        // wait until the whole pdu is available (entire pdu)
        if (buffer.readableBytes() < commandLength) {
            return null;
        }

        // at this point, we have the entire PDU and length already in the buffer
        // we'll create a new "view" of this PDU and read the data from the actual buffer
        // NOTE: this should be super fast since the underlying byte array doesn't get copied
        ChannelBuffer buffer0 = buffer.readSlice(commandLength);

        return doDecode(commandLength, buffer0);
    }

    protected Pdu doDecode(int commandLength, ChannelBuffer buffer) throws UnrecoverablePduException, RecoverablePduException {
        // skip the length field because we already parsed it
        buffer.skipBytes(SmppConstants.PDU_INT_LENGTH);

        // read the remaining portion of the PDU header
        int commandId = buffer.readInt();
        int commandStatus = buffer.readInt();
        int sequenceNumber = buffer.readInt();

        // this is a major issue if the sequence number is invalid
        SequenceNumber.assertValid(sequenceNumber);

        Pdu pdu = null;

        // any command id with its 31st bit set to true is a response
        if (PduUtil.isRequestCommandId(commandId)) {
            if (commandId == SmppConstants.CMD_ID_ENQUIRE_LINK) {
                pdu = new EnquireLink();
            } else if (commandId == SmppConstants.CMD_ID_DELIVER_SM) {
                pdu = new DeliverSm();
            } else if (commandId == SmppConstants.CMD_ID_SUBMIT_SM) {
                pdu = new SubmitSm();
            } else if (commandId == SmppConstants.CMD_ID_DATA_SM) {
                pdu = new DataSm();
            } else if (commandId == SmppConstants.CMD_ID_CANCEL_SM) {
                pdu = new CancelSm();
            } else if (commandId == SmppConstants.CMD_ID_QUERY_SM) {
                pdu = new QuerySm();
            } else if (commandId == SmppConstants.CMD_ID_REPLACE_SM) {
                pdu = new ReplaceSm();
            } else if (commandId == SmppConstants.CMD_ID_BIND_TRANSCEIVER) {
                pdu = new BindTransceiver();
            } else if (commandId == SmppConstants.CMD_ID_BIND_TRANSMITTER) {
                pdu = new BindTransmitter();
            } else if (commandId == SmppConstants.CMD_ID_BIND_RECEIVER) {
                pdu = new BindReceiver();
            } else if (commandId == SmppConstants.CMD_ID_UNBIND) {
                pdu = new Unbind();
            } else if (commandId == SmppConstants.CMD_ID_ALERT_NOTIFICATION) {
                pdu = new AlertNotification();
            } else {
                pdu = new PartialPdu(commandId);
            }
        } else {
            if (commandId == SmppConstants.CMD_ID_SUBMIT_SM_RESP) {
                pdu = new SubmitSmResp();
            } else if (commandId == SmppConstants.CMD_ID_DELIVER_SM_RESP) {
                pdu = new DeliverSmResp();
            } else if (commandId == SmppConstants.CMD_ID_DATA_SM_RESP) {
                pdu = new DataSmResp();
            } else if (commandId == SmppConstants.CMD_ID_CANCEL_SM_RESP) {
                pdu = new CancelSmResp();
            } else if (commandId == SmppConstants.CMD_ID_QUERY_SM_RESP) {
                pdu = new QuerySmResp();
            } else if (commandId == SmppConstants.CMD_ID_REPLACE_SM_RESP) {
                pdu = new ReplaceSmResp();
            } else if (commandId == SmppConstants.CMD_ID_ENQUIRE_LINK_RESP) {
                pdu = new EnquireLinkResp();
            } else if (commandId == SmppConstants.CMD_ID_BIND_TRANSCEIVER_RESP) {
                pdu = new BindTransceiverResp();
            } else if (commandId == SmppConstants.CMD_ID_BIND_RECEIVER_RESP) {
                pdu = new BindReceiverResp();
            } else if (commandId == SmppConstants.CMD_ID_BIND_TRANSMITTER_RESP) {
                pdu = new BindTransmitterResp();
            } else if (commandId == SmppConstants.CMD_ID_UNBIND_RESP) {
                pdu = new UnbindResp();
            } else if (commandId == SmppConstants.CMD_ID_GENERIC_NACK) {
                pdu = new GenericNack();
            } else {
                pdu = new PartialPduResp(commandId);
            }
        }

        // set pdu header values
        pdu.setCommandLength(commandLength);
        pdu.setCommandStatus(commandStatus);
        pdu.setSequenceNumber(sequenceNumber);

        // check if we need to throw an exception
        if (pdu instanceof PartialPdu) {
            throw new UnknownCommandIdException(pdu, "Unsupported or unknown PDU request commandId [0x" + HexUtil.toHexString(commandId) + "]");
        } else if (pdu instanceof PartialPduResp) {
            throw new UnknownCommandIdException(pdu, "Unsupported or unknown PDU response commandId [0x" + HexUtil.toHexString(commandId) + "]");
        }

        // see if we can map the command status into a message
        if (pdu instanceof PduResponse) {
            PduResponse response = (PduResponse)pdu;
            response.setResultMessage(context.lookupResultMessage(commandStatus));
        }

        try {
            // parse pdu body parameters (may throw exception)
            pdu.readBody(buffer);
            // parse pdu optional parameters (may throw exception)
            pdu.readOptionalParameters(buffer, context);
        } catch (RecoverablePduException e) {
            // check if we should add the partial pdu to the exception
            if (e.getPartialPdu() == null) {
                e.setPartialPdu(pdu);
            }
            // rethrow it
            throw e;
        }

        return pdu;
    }
}