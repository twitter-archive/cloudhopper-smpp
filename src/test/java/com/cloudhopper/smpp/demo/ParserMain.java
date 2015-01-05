package com.cloudhopper.smpp.demo;

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

import com.cloudhopper.smpp.pdu.BufferHelper;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.transcoder.PduTranscoderContext;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class ParserMain {
    private static final Logger logger = LoggerFactory.getLogger(ParserMain.class);

    static public void main(String[] args) throws Exception {
        PduTranscoderContext context = new DefaultPduTranscoderContext();
        PduTranscoder transcoder = new DefaultPduTranscoder(context);
        ChannelBuffer buffer = BufferHelper.createBuffer("000000420000000400000000000000030001003633393238383032000101343439353133363139323035004000000000000000000774657374323232020B00020D05");
        Pdu pdu = transcoder.decode(buffer);
        logger.debug("{}", pdu);
    }

}
