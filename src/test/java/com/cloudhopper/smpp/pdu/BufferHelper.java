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

import com.cloudhopper.commons.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class BufferHelper
{

    static public ByteBuf createBuffer(byte[] bytes) throws Exception {
	return Unpooled.wrappedBuffer(bytes);
    }

    static public ByteBuf createBuffer(String hexString) throws Exception {
        return createBuffer(HexUtil.toByteArray(hexString));
    }

    static public byte[] createByteArray(ByteBuf buffer) throws Exception {
        byte[] bytes = new byte[buffer.readableBytes()];
        // temporarily read bytes from the buffer
        buffer.getBytes(buffer.readerIndex(), bytes);
        return bytes;
    }

    static public String createHexString(ByteBuf buffer) throws Exception {
	return HexUtil.toHexString(createByteArray(buffer));
    }

}
