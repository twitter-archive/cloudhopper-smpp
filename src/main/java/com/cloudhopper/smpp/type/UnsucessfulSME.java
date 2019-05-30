/**
 * 
 */
package com.cloudhopper.smpp.type;

import com.cloudhopper.smpp.util.ByteBufUtil;

import io.netty.buffer.ByteBuf;

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


/**
 * @author Amit Bhayani
 * 
 */
public class UnsucessfulSME {

	private int errorStatusCode;
	private Address address;

	/**
	 * 
	 */
	public UnsucessfulSME() {
	}

	public UnsucessfulSME(int errorStatusCode, Address address) {
		super();
		this.errorStatusCode = errorStatusCode;
		this.address = address;
	}

	public int getErrorStatusCode() {
		return errorStatusCode;
	}

	public void setErrorStatusCode(int errorStatusCode) {
		this.errorStatusCode = errorStatusCode;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

	public void read(ByteBuf buffer) throws UnrecoverablePduException,
			RecoverablePduException {
		this.address = ByteBufUtil.readAddress(buffer);
		this.errorStatusCode = buffer.readInt();
	}

	public void write(ByteBuf buffer) throws UnrecoverablePduException,
			RecoverablePduException {
	    ByteBufUtil.writeAddress(buffer, this.address);
		buffer.writeInt(this.errorStatusCode);
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder(44);
		buffer.append(this.address.toString());
		buffer.append(" errorStatusCode [");
		buffer.append(this.errorStatusCode);
		buffer.append("]");
		return buffer.toString();
	}

}
