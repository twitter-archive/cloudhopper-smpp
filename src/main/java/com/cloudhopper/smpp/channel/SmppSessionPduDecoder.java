package com.cloudhopper.smpp.channel;

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

import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel handler responsible for decoding a ByteBuf into a PDU.  A
 * decoded PDU is then passed up the pipeline for further processing.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSessionPduDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(SmppSessionPduDecoder.class);

    private final PduTranscoder transcoder;

    public SmppSessionPduDecoder(PduTranscoder transcoder) {
        this.transcoder = transcoder;
    }

    // @Override
    // protected Object decode(ChannelHandlerContext ctx, Channel channel, ByteBuf buffer) throws Exception {
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buffer, List<Object> out) throws Exception {
        // try to decode the frame into a PDU
        // NOTE: this already returns null if there isn't enough data yet
        // return transcoder.decode(buffer);
	Pdu pdu = transcoder.decode(buffer);
	logger.debug("Decoded PDU: {}", pdu);
        out.add(pdu);
    }
    
}
