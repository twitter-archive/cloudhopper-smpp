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
import com.cloudhopper.smpp.type.UnsucessfulSME;
import com.cloudhopper.smpp.util.ByteBufUtil;
import com.cloudhopper.smpp.util.PduUtil;

/**
 * @author Amit Bhayani
 * 
 */
public class SubmitMultiResp extends BaseSmResp {

	private int numberOfUnsucessfulDest;
	private List<UnsucessfulSME> unsucessfulSmes = new ArrayList<UnsucessfulSME>();

	/**
	 * @param commandId
	 * @param name
	 */
	public SubmitMultiResp() {
		super(SmppConstants.CMD_ID_SUBMIT_MULTI_RESP, "submit_multi_resp");
	}

	public void addUnsucessfulSME(UnsucessfulSME unsucessfulSME)
			throws SmppInvalidArgumentException {
		this.numberOfUnsucessfulDest++;
		this.unsucessfulSmes.add(unsucessfulSME);
	}

	public int getNumberOfUnsucessfulDest() {
		return numberOfUnsucessfulDest;
	}

	public List<UnsucessfulSME> getUnsucessfulSmes() {
		return unsucessfulSmes;
	}

	@Override
	public void readBody(ByteBuf buffer)
			throws UnrecoverablePduException, RecoverablePduException {
		super.readBody(buffer);

		this.numberOfUnsucessfulDest = buffer.readUnsignedByte();

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) {
			Address address = ByteBufUtil.readAddress(buffer);
			int errorStatusCode = buffer.readInt();

			this.unsucessfulSmes.add(new UnsucessfulSME(errorStatusCode,
					address));

		}
	}

	@Override
	public int calculateByteSizeOfBody() {
		int bodyLength = 0;
		bodyLength = super.calculateByteSizeOfBody();

		bodyLength += 1; // no_unsuccess

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) {
			UnsucessfulSME unsucessfulSME = this.unsucessfulSmes.get(count);
			bodyLength += PduUtil.calculateByteSizeOfAddress(unsucessfulSME
					.getAddress());
			bodyLength += 4; // error_status_code
		}

		return bodyLength;
	}

	@Override
	public void writeBody(ByteBuf buffer)
			throws UnrecoverablePduException, RecoverablePduException {
		super.writeBody(buffer);

		buffer.writeByte(this.numberOfUnsucessfulDest);

		for (int count = 0; count < this.numberOfUnsucessfulDest; count++) {
			UnsucessfulSME unsucessfulSME = this.unsucessfulSmes.get(count);
			ByteBufUtil.writeAddress(buffer, unsucessfulSME.getAddress());
			buffer.writeInt(unsucessfulSME.getErrorStatusCode());
		}
	}

}
