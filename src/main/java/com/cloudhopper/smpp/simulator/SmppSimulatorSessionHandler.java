package com.cloudhopper.smpp.simulator;

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
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.jboss.netty.buffer.ChannelBuffers.*;

/**
 * Basically turns the event-driven handler into a "queued" handler by queuing
 * received PDUs and exceptions thrown.  A calling thread can then "poll" for
 * these events.  Also, the responses can be scheduled and queued ahead of time.
 * The next event that matches it will automatically write it back.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSimulatorSessionHandler extends FrameDecoder {
    private static final Logger logger = LoggerFactory.getLogger(SmppSimulatorSessionHandler.class);

    private final Channel channel;
    private final PduTranscoder transcoder;
    private final BlockingQueue<Pdu> pduQueue;
    private final BlockingQueue<Throwable> exceptionQueue;
    private final BlockingDeque<Pdu> writePduQueue;

    private SmppSimulatorPduProcessor pduProcessor;

    public SmppSimulatorSessionHandler(Channel channel, PduTranscoder transcoder) {
        this.channel = channel;
        this.transcoder = transcoder;
        this.pduQueue = new LinkedBlockingQueue<Pdu>();
        this.exceptionQueue = new LinkedBlockingQueue<Throwable>();
        this.writePduQueue = new LinkedBlockingDeque<Pdu>();
    }

    public SmppSimulatorPduProcessor getPduProcessor() {
        return this.pduProcessor;
    }

    public void setPduProcessor(SmppSimulatorPduProcessor pduProcessor) {
        this.pduProcessor = pduProcessor;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public PduTranscoder getTranscoder() {
        return this.transcoder;
    }

    public BlockingQueue<Pdu> getPduQueue() {
        return this.pduQueue;
    }

    public void addPduToWriteOnNextPduReceived(Pdu pdu) {
        this.writePduQueue.add(pdu);
    }

    public Pdu pollNextPdu(long timeoutInMillis) throws InterruptedException {
        return this.pduQueue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    public BlockingQueue<Throwable> getThrowableQueue() {
        return this.exceptionQueue;
    }

    public Throwable pollNextThrowable(long timeoutInMillis) throws InterruptedException {
        return this.exceptionQueue.poll(timeoutInMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.info("Received new throwable on channel 0x" + HexUtil.toHexString(channel.getId()) );
        this.exceptionQueue.add(e.getCause());
    }

    public void sendPdu(Pdu pdu) throws Exception {
        logger.info("Sending on channel 0x" + HexUtil.toHexString(channel.getId()) + " PDU: {}", pdu);
        ChannelBuffer writeBuffer = this.transcoder.encode(pdu);
        channel.write(writeBuffer).await();
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        // ignore requests with zero bytes
        if (buffer.readableBytes() <= 0) {
            return null;
        }

        // decode the buffer into a pdu
        Pdu pdu = transcoder.decode(buffer);

        // if the pdu was null, we don't have enough data yet
        if (pdu == null) {
            logger.info("Received data on channel 0x" + HexUtil.toHexString(channel.getId()) + ", but not enough to parse PDU fully yet");
            logger.info("Bytes in buffer: [{}]", hexDump(buffer));
            return null;
        }

        logger.info("Decoded buffer on channel 0x" + HexUtil.toHexString(channel.getId()) + " into PDU: {}", pdu);

        // if we have a pdu procesor registered, let's see if it handles it
        boolean processed = false;
        if (this.pduProcessor != null) {
            processed = this.pduProcessor.process(this, channel, pdu);
        }

        if (processed) {
            logger.info("This PDU was processed by the registered PduProcessor");
        } else {
            this.pduQueue.add(pdu);
        }

        // is there a PDU someone wants us to write in response?
        if (this.writePduQueue.size() > 0) {
            Pdu pduToWrite = this.writePduQueue.remove();
            logger.info("Automatically writing back on channel 0x" + HexUtil.toHexString(channel.getId()) + " the PDU: {}", pduToWrite);
            ChannelBuffer writeBuffer = this.transcoder.encode(pduToWrite);
            channel.write(writeBuffer);
        }

        return pdu;
    }
    
}
