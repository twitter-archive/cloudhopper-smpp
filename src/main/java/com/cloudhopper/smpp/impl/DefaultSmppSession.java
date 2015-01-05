package com.cloudhopper.smpp.impl;

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

import com.cloudhopper.commons.util.PeriodFormatterUtil;
import com.cloudhopper.smpp.jmx.DefaultSmppSessionMXBean;
import com.cloudhopper.commons.util.windowing.DuplicateKeyException;
import com.cloudhopper.commons.util.windowing.OfferTimeoutException;
import com.cloudhopper.commons.util.windowing.Window;
import com.cloudhopper.commons.util.windowing.WindowFuture;
import com.cloudhopper.commons.util.windowing.WindowListener;
import com.cloudhopper.smpp.SmppBindType;
import com.cloudhopper.smpp.SmppConstants;
import com.cloudhopper.smpp.SmppServerSession;
import com.cloudhopper.smpp.type.SmppChannelException;
import com.cloudhopper.smpp.SmppSessionConfiguration;
import com.cloudhopper.smpp.SmppSessionCounters;
import com.cloudhopper.smpp.SmppSessionHandler;
import com.cloudhopper.smpp.SmppSessionListener;
import com.cloudhopper.smpp.type.SmppTimeoutException;
import com.cloudhopper.smpp.pdu.BaseBind;
import com.cloudhopper.smpp.pdu.BaseBindResp;
import com.cloudhopper.smpp.pdu.EnquireLink;
import com.cloudhopper.smpp.pdu.EnquireLinkResp;
import com.cloudhopper.smpp.pdu.Pdu;
import com.cloudhopper.smpp.pdu.PduRequest;
import com.cloudhopper.smpp.pdu.PduResponse;
import com.cloudhopper.smpp.pdu.SubmitSm;
import com.cloudhopper.smpp.pdu.SubmitSmResp;
import com.cloudhopper.smpp.pdu.Unbind;
import com.cloudhopper.smpp.tlv.Tlv;
import com.cloudhopper.smpp.tlv.TlvConvertException;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoder;
import com.cloudhopper.smpp.transcoder.DefaultPduTranscoderContext;
import com.cloudhopper.smpp.transcoder.PduTranscoder;
import com.cloudhopper.smpp.type.RecoverablePduException;
import com.cloudhopper.smpp.type.SmppBindException;
import com.cloudhopper.smpp.type.UnrecoverablePduException;
import com.cloudhopper.smpp.util.SequenceNumber;
import com.cloudhopper.smpp.util.SmppSessionUtil;
import com.cloudhopper.smpp.util.SmppUtil;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.ObjectName;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of either an ESME or SMSC SMPP session.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppSession implements SmppServerSession, SmppSessionChannelListener, WindowListener<Integer,PduRequest,PduResponse>, DefaultSmppSessionMXBean {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSmppSession.class);

    // are we an "esme" or "smsc" session type?
    private final Type localType;
    // current state of this session
    private final AtomicInteger state;
    // the timestamp when we became "bound"
    private final AtomicLong boundTime;
    private final SmppSessionConfiguration configuration;
    private final Channel channel;
    private SmppSessionHandler sessionHandler;
    private final SequenceNumber sequenceNumber;
    private final PduTranscoder transcoder;
    private final Window<Integer,PduRequest,PduResponse> sendWindow;
    private byte interfaceVersion;
    // only for server sessions
    private DefaultSmppServer server;
    // the session id assigned by the server to this particular instance
    private Long serverSessionId;
    // pre-prepared BindResponse to send back once we're flagged as ready
    private BaseBindResp preparedBindResponse;
    private ScheduledExecutorService monitorExecutor;
    private DefaultSmppSessionCounters counters;

    /**
     * Creates an SmppSession for a server-based session.
     */
    public DefaultSmppSession(Type localType, SmppSessionConfiguration configuration, Channel channel, DefaultSmppServer server, Long serverSessionId, BaseBindResp preparedBindResponse, byte interfaceVersion, ScheduledExecutorService monitorExecutor) {
        this(localType, configuration, channel, (SmppSessionHandler)null, monitorExecutor);
        // default state for a server session is that it's binding
        this.state.set(STATE_BINDING);
        this.server = server;
        this.serverSessionId = serverSessionId;
        this.preparedBindResponse = preparedBindResponse;
        this.interfaceVersion = interfaceVersion;
    }

    /**
     * Creates an SmppSession for a client-based session. It is <b>NOT</b> 
     * recommended that this constructor is called directly.  The recommended
     * way to construct a session is either via a DefaultSmppClient or
     * DefaultSmppServer.  This constructor will cause monitoring to be disabled.
     * @param localType The type of local endpoint (ESME vs. SMSC)
     * @param configuration The session configuration
     * @param channel The channel associated with this session. The channel
     *      needs to already be opened.
     * @param sessionHandler The handler for session events
     */
    public DefaultSmppSession(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppSessionHandler sessionHandler) {
        this(localType, configuration, channel, sessionHandler, null);
    }
    
    
    /**
     * Creates an SmppSession for a client-based session. It is <b>NOT</b> 
     * recommended that this constructor is called directly.  The recommended
     * way to construct a session is either via a DefaultSmppClient or
     * DefaultSmppServer. 
     * @param localType The type of local endpoint (ESME vs. SMSC)
     * @param configuration The session configuration
     * @param channel The channel associated with this session. The channel
     *      needs to already be opened.
     * @param sessionHandler The handler for session events
     * @param executor The executor that window monitoring and potentially
     *      statistics will be periodically executed under.  If null, monitoring
     *      will be disabled.
     */
    public DefaultSmppSession(Type localType, SmppSessionConfiguration configuration, Channel channel, SmppSessionHandler sessionHandler, ScheduledExecutorService monitorExecutor) {
        this.localType = localType;
        this.state = new AtomicInteger(STATE_OPEN);
        this.configuration = configuration;
        this.channel = channel;
        this.boundTime = new AtomicLong(0);
        this.sessionHandler = (sessionHandler == null ? new DefaultSmppSessionHandler(logger) : sessionHandler);
        this.sequenceNumber = new SequenceNumber();
        // always "wrap" the custom pdu transcoder context with a default one
        this.transcoder = new DefaultPduTranscoder(new DefaultPduTranscoderContext(this.sessionHandler));
        this.monitorExecutor = monitorExecutor;
        
        // different ways to construct the window if monitoring is enabled
        if (monitorExecutor != null && configuration.getWindowMonitorInterval() > 0) {
            // enable send window monitoring, verify if the monitoringInterval has been set
            this.sendWindow = new Window<Integer,PduRequest,PduResponse>(configuration.getWindowSize(), monitorExecutor, configuration.getWindowMonitorInterval(), this, configuration.getName() + ".Monitor");
        } else {
            this.sendWindow = new Window<Integer,PduRequest,PduResponse>(configuration.getWindowSize());
        }
        
        // these server-only items are null
        this.server = null;
        this.serverSessionId = null;
        this.preparedBindResponse = null;
        if (configuration.isCountersEnabled()) {
            this.counters = new DefaultSmppSessionCounters();
        }
    }
    
    public void registerMBean(String objectName) {
        // register the this queue manager as an mbean
        try {
            ObjectName name = new ObjectName(objectName);
            ManagementFactory.getPlatformMBeanServer().registerMBean(this, name);
        } catch (Exception e) {
            // log the error, but don't throw an exception for this datasource
            logger.error("Unable to register DefaultSmppSessionMXBean [{}]", objectName, e);
        }
    }
    
    public void unregisterMBean(String objectName) {
        // register the this queue manager as an mbean
        try {
            ObjectName name = new ObjectName(objectName);
            ManagementFactory.getPlatformMBeanServer().unregisterMBean(name);
        } catch (Exception e) {
            // log the error, but don't throw an exception for this datasource
            logger.error("Unable to unregister DefaultSmppServerMXBean [{}]", objectName, e);
        }
    }
        
    @Override
    public SmppBindType getBindType() {
        return this.configuration.getType();
    }

    @Override
    public Type getLocalType() {
        return this.localType;
    }

    @Override
    public Type getRemoteType() {
        if (this.localType == Type.CLIENT) {
            return Type.SERVER;
        } else {
            return Type.CLIENT;
        }
    }

    protected void setBound() {
        this.state.set(STATE_BOUND);
        this.boundTime.set(System.currentTimeMillis());
    }

    @Override
    public long getBoundTime() {
        return this.boundTime.get();
    }

    @Override
    public String getStateName() {
        int s = this.state.get();
        if (s >= 0 || s < STATES.length) {
            return STATES[s];
        } else {
            return "UNKNOWN (" + s + ")";
        }
    }

    protected void setInterfaceVersion(byte value) {
        this.interfaceVersion = value;
    }

    @Override
    public byte getInterfaceVersion() {
        return this.interfaceVersion;
    }

    @Override
    public boolean areOptionalParametersSupported() {
        return (this.interfaceVersion >= SmppConstants.VERSION_3_4);
    }

    @Override
    public boolean isOpen() {
        return (this.state.get() == STATE_OPEN);
    }

    @Override
    public boolean isBinding() {
        return (this.state.get() == STATE_BINDING);
    }

    @Override
    public boolean isBound() {
        return (this.state.get() == STATE_BOUND);
    }

    @Override
    public boolean isUnbinding() {
        return (this.state.get() == STATE_UNBINDING);
    }

    @Override
    public boolean isClosed() {
        return (this.state.get() == STATE_CLOSED);
    }

    @Override
    public SmppSessionConfiguration getConfiguration() {
        return this.configuration;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public SequenceNumber getSequenceNumber() {
        return this.sequenceNumber;
    }

    protected PduTranscoder getTranscoder() {
        return this.transcoder;
    }
    
    @Override
    public Window<Integer,PduRequest,PduResponse> getRequestWindow() {
        return getSendWindow();
    }

    @Override
    public Window<Integer,PduRequest,PduResponse> getSendWindow() {
        return this.sendWindow;
    }
    
    @Override
    public boolean hasCounters() {
        return (this.counters != null);
    }
    
    @Override
    public SmppSessionCounters getCounters() {
        return this.counters;
    }

    @Override
    public void serverReady(SmppSessionHandler sessionHandler) {
        // properly setup the session handler (to handle notifications)
        this.sessionHandler = sessionHandler;
        // send the prepared bind response
        try {
            this.sendResponsePdu(this.preparedBindResponse);
        } catch (Exception e) {
            logger.error("{}", e);
        }
        // flag the channel is ready to read
        this.channel.setReadable(true).awaitUninterruptibly();
        this.setBound();
    }

    protected BaseBindResp bind(BaseBind request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppBindException, SmppTimeoutException, SmppChannelException, InterruptedException {
        assertValidRequest(request);
        boolean bound = false;
        try {
            this.state.set(STATE_BINDING);

            PduResponse response = sendRequestAndGetResponse(request, timeoutInMillis);
            SmppSessionUtil.assertExpectedResponse(request, response);
            BaseBindResp bindResponse = (BaseBindResp)response;

            // check if the bind succeeded
            if (bindResponse == null || bindResponse.getCommandStatus() != SmppConstants.STATUS_OK) {
                // bind failed for a specific reason
                throw new SmppBindException(bindResponse);
            }

            // if we make it all the way here, we're good and bound
            bound = true;

            //
            // negotiate version in use based on response back from server
            //
            Tlv scInterfaceVersion = bindResponse.getOptionalParameter(SmppConstants.TAG_SC_INTERFACE_VERSION);

            if (scInterfaceVersion == null) {
                // this means version 3.3 is in use
                this.interfaceVersion = SmppConstants.VERSION_3_3;
            } else {
                try {
                    byte tempInterfaceVersion = scInterfaceVersion.getValueAsByte();
                    if (tempInterfaceVersion >= SmppConstants.VERSION_3_4) {
                        this.interfaceVersion = SmppConstants.VERSION_3_4;
                    } else {
                        this.interfaceVersion = SmppConstants.VERSION_3_3;
                    }
                } catch (TlvConvertException e) {
                    logger.warn("Unable to convert sc_interface_version to a byte value: {}", e.getMessage());
                    this.interfaceVersion = SmppConstants.VERSION_3_3;
                }
            }

            return bindResponse;
        } finally {
            if (bound) {
                // this session is now successfully bound & ready for processing
                setBound();
            } else {
                // the bind failed, we need to clean up resources
                try { this.close(); } catch (Exception e) { }
            }
        }
    }

    @Override
    public void unbind(long timeoutInMillis) {
        // is this channel still open?
        if (this.channel.isConnected()) {
            this.state.set(STATE_UNBINDING);

            // try a "graceful" unbind by sending an "unbind" request
            try {
                sendRequestAndGetResponse(new Unbind(), timeoutInMillis);
            } catch (Exception e) {
                // not sure if an exception while attempting to unbind matters...
                // we are going to just print out a warning
                logger.warn("Did not cleanly receive an unbind response to our unbind request, safe to ignore: " + e.getMessage());
            }
        } else {
            logger.info("Session channel is already closed, not going to unbind");
        }

        // always delegate the unbind to finish up with a "close"
        close(timeoutInMillis);
    }

    @Override
    public void close() {
        close(5000);
    }

    @Override
    public void close(long timeoutInMillis) {
        if (channel.isConnected()) {
            // temporarily set to "unbinding" for now
            this.state.set(STATE_UNBINDING);
            // make sure the channel is always closed
            if (channel.close().awaitUninterruptibly(timeoutInMillis)) {
                logger.info("Successfully closed");
            } else {
                logger.warn("Unable to cleanly close channel");
            }
        }
        this.state.set(STATE_CLOSED);
    }
    
    @Override
    public void destroy() {
        close();
        this.sendWindow.destroy();
        if (this.counters != null) {
            this.counters.reset();
        }
        // make sure to lose the reference to to the session handler - many
        // users of this class will probably pass themselves as the reference
        // and this may help to prevent a circular reference
        this.sessionHandler = null;
    }

    @Override
    public EnquireLinkResp enquireLink(EnquireLink request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        assertValidRequest(request);
        PduResponse response = sendRequestAndGetResponse(request, timeoutInMillis);
        SmppSessionUtil.assertExpectedResponse(request, response);
        return (EnquireLinkResp)response;
    }

    @Override
    public SubmitSmResp submit(SubmitSm request, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        assertValidRequest(request);
        PduResponse response = sendRequestAndGetResponse(request, timeoutInMillis);
        SmppSessionUtil.assertExpectedResponse(request, response);
        return (SubmitSmResp)response;
    }
    
    protected void assertValidRequest(PduRequest request) throws NullPointerException, RecoverablePduException, UnrecoverablePduException {
        if (request == null) {
            throw new NullPointerException("PDU request cannot be null");
        }
    }

    /**
     * Sends a PDU request and gets a PDU response that matches its sequence #.
     * NOTE: This PDU response may not be the actual response the caller was
     * expecting, it needs to verify it afterwards.
     */
    protected PduResponse sendRequestAndGetResponse(PduRequest requestPdu, long timeoutInMillis) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        WindowFuture<Integer,PduRequest,PduResponse> future = sendRequestPdu(requestPdu, timeoutInMillis, true);
        boolean completedWithinTimeout = future.await();
        
        if (!completedWithinTimeout) {
            // since this is a "synchronous" request and it timed out, we don't
            // want it eating up valuable window space - cancel it before returning exception
            future.cancel();
            throw new SmppTimeoutException("Unable to get response within [" + timeoutInMillis + " ms]");
        }
        
        // 3 possible scenarios once completed: success, failure, or cancellation
        if (future.isSuccess()) {
            return future.getResponse();
        } else if (future.getCause() != null) {
            Throwable cause = future.getCause();
            if (cause instanceof ClosedChannelException) {
                throw new SmppChannelException("Channel was closed after sending request, but before receiving response", cause);
            } else {
                throw new UnrecoverablePduException(cause.getMessage(), cause);
            }
        } else if (future.isCancelled()) {
            throw new RecoverablePduException("Request was cancelled");
        } else {
            throw new UnrecoverablePduException("Unable to sendRequestAndGetResponse successfully (future was in strange state)");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public WindowFuture<Integer,PduRequest,PduResponse> sendRequestPdu(PduRequest pdu, long timeoutMillis, boolean synchronous) throws RecoverablePduException, UnrecoverablePduException, SmppTimeoutException, SmppChannelException, InterruptedException {
        // assign the next PDU sequence # if its not yet assigned
        if (!pdu.hasSequenceNumberAssigned()) {
            pdu.setSequenceNumber(this.sequenceNumber.next());
        }

        // encode the pdu into a buffer
        ChannelBuffer buffer = transcoder.encode(pdu);

        WindowFuture<Integer,PduRequest,PduResponse> future = null;
        try {
            future = sendWindow.offer(pdu.getSequenceNumber(), pdu, timeoutMillis, configuration.getRequestExpiryTimeout(), synchronous);
        } catch (DuplicateKeyException e) {
            throw new UnrecoverablePduException(e.getMessage(), e);
        } catch (OfferTimeoutException e) {
            throw new SmppTimeoutException(e.getMessage(), e);
        }
        
        if(this.sessionHandler instanceof SmppSessionListener) {
            if(!((SmppSessionListener)this.sessionHandler).firePduDispatch(pdu)) {
                logger.info("dispatched request PDU discarded: {}", pdu);
                future.cancel(); //@todo probably throwing exception here is better solution?
                return future;
            }
        }

        // we need to log the PDU after encoding since some things only happen
        // during the encoding process such as looking up the result message
        if (configuration.getLoggingOptions().isLogPduEnabled()) {
            if (synchronous) {
                logger.info("sync send PDU: {}", pdu);
            } else {
                logger.info("async send PDU: {}", pdu);
            }
        }

        // write the pdu out & wait timeout amount of time
	ChannelFuture channelFuture = this.channel.write(buffer).await();

        // check if the write was a success
        if (!channelFuture.isSuccess()) {
            // the write failed, make sure to throw an exception
            throw new SmppChannelException(channelFuture.getCause().getMessage(), channelFuture.getCause());
        }
        
        this.countSendRequestPdu(pdu);

        return future;
    }

    /**
     * Asynchronously sends a PDU and does not wait for a response PDU.
     * This method will wait for the PDU to be written to the underlying channel.
     * @param pdu The PDU to send (can be either a response or request)
     * @throws RecoverablePduEncodingException
     * @throws UnrecoverablePduEncodingException
     * @throws SmppChannelException
     * @throws InterruptedException
     */
    @Override
    public void sendResponsePdu(PduResponse pdu) throws RecoverablePduException, UnrecoverablePduException, SmppChannelException, InterruptedException {
        // assign the next PDU sequence # if its not yet assigned
        if (!pdu.hasSequenceNumberAssigned()) {
            pdu.setSequenceNumber(this.sequenceNumber.next());
        }
        
        if(this.sessionHandler instanceof SmppSessionListener) {
            if(!((SmppSessionListener)this.sessionHandler).firePduDispatch(pdu)) {
                logger.info("dispatched response PDU discarded: {}", pdu);
                return;
            }
        }

        // encode the pdu into a buffer
        ChannelBuffer buffer = transcoder.encode(pdu);

        // we need to log the PDU after encoding since some things only happen
        // during the encoding process such as looking up the result message
        if (configuration.getLoggingOptions().isLogPduEnabled()) {
            logger.info("send PDU: {}", pdu);
        }

        // write the pdu out & wait timeout amount of time
        ChannelFuture channelFuture = this.channel.write(buffer).await();

        // check if the write was a success
        if (!channelFuture.isSuccess()) {
            // the write failed, make sure to throw an exception
            throw new SmppChannelException(channelFuture.getCause().getMessage(), channelFuture.getCause());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void firePduReceived(Pdu pdu) {
        if (configuration.getLoggingOptions().isLogPduEnabled()) {
            logger.info("received PDU: {}", pdu);
        }

        if(this.sessionHandler instanceof SmppSessionListener) {
            if(!((SmppSessionListener)this.sessionHandler).firePduReceived(pdu)){
                logger.info("recieved PDU discarded: {}", pdu);
                return;
            }
        }

        if (pdu instanceof PduRequest) {
            // process this request and allow the handler to return a result
            PduRequest requestPdu = (PduRequest)pdu;
            
            this.countReceiveRequestPdu(requestPdu);
            
            long startTime = System.currentTimeMillis();
            PduResponse responsePdu = this.sessionHandler.firePduRequestReceived(requestPdu);
            
            // if the handler returned a non-null object, then we need to send it back on the channel
            if (responsePdu != null) {
                try {
                    long responseTime = System.currentTimeMillis() - startTime;
                    this.countSendResponsePdu(responsePdu, responseTime, responseTime);
                    
                    this.sendResponsePdu(responsePdu);
                } catch (Exception e) {
                    logger.error("Unable to cleanly return response PDU: {}", e);
                }
            }
        } else {
            // this is a response -- we need to check if its "expected" or "unexpected"
            PduResponse responsePdu = (PduResponse)pdu;
            int receivedPduSeqNum = pdu.getSequenceNumber();
            
            try {
                // see if a correlating request exists in the window
                WindowFuture<Integer,PduRequest,PduResponse> future = this.sendWindow.complete(receivedPduSeqNum, responsePdu);
                if (future != null) {
                    logger.trace("Found a future in the window for seqNum [{}]", receivedPduSeqNum);
                    this.countReceiveResponsePdu(responsePdu, future.getOfferToAcceptTime(), future.getAcceptToDoneTime(), (future.getAcceptToDoneTime() / future.getWindowSize()));
                    
                    // if this isn't null, we found a match to a request
                    int callerStateHint = future.getCallerStateHint();
                    //logger.trace("IsCallerWaiting? " + future.isCallerWaiting() + " callerStateHint=" + callerStateHint);
                    if (callerStateHint == WindowFuture.CALLER_WAITING) {
                        logger.trace("Caller waiting for request: {}", future.getRequest()); 
                        // if a caller is waiting, nothing extra needs done as calling thread will handle the response
                        return;
                    } else if (callerStateHint == WindowFuture.CALLER_NOT_WAITING) {
                        logger.trace("Caller not waiting for request: {}", future.getRequest()); 
                        // this was an "expected" response - wrap it into an async response
                        this.sessionHandler.fireExpectedPduResponseReceived(new DefaultPduAsyncResponse(future));
                        return;
                    } else {
                        logger.trace("Caller timed out waiting for request: {}", future.getRequest());
                        // we send the request, but caller gave up on it awhile ago
                        this.sessionHandler.fireUnexpectedPduResponseReceived(responsePdu);
                    }
                } else {
                    this.countReceiveResponsePdu(responsePdu, 0, 0, 0);
                    
                    // original request either expired OR was completely unexpected
                    this.sessionHandler.fireUnexpectedPduResponseReceived(responsePdu);
                }
            } catch (InterruptedException e) {
                logger.warn("Interrupted while attempting to process response PDU and match it to a request via requesWindow: ", e);
                // do nothing, continue processing
            }
        }
    }

    @Override
    public void fireExceptionThrown(Throwable t) {
        if (t instanceof UnrecoverablePduException) {
            this.sessionHandler.fireUnrecoverablePduException((UnrecoverablePduException)t);
        } else if (t instanceof RecoverablePduException) {
            this.sessionHandler.fireRecoverablePduException((RecoverablePduException)t);
        } else {
            // during testing under high load -- java.io.IOException: Connection reset by peer
            // let's check to see if this session was requested to be closed
            if (isUnbinding() || isClosed()) {
                logger.debug("Unbind/close was requested, ignoring exception thrown: {}", t);
            } else {
                this.sessionHandler.fireUnknownThrowable(t);
            }
        }
    }

    @Override
    public void fireChannelClosed() {
        // if this is a server session, we need to notify the server first
        // NOTE: its important this happens first
        if (this.server != null) {
            this.server.destroySession(serverSessionId, this);
        }
        
        // most of the time when a channel is closed, we don't necessarily want
        // to do anything special -- however when a caller is waiting for a response
        // to a request and we know the channel closed, we should check for those
        // specific requests and make sure to cancel them
        if (this.sendWindow.getSize() > 0) {
            logger.trace("Channel closed and sendWindow has [{}] outstanding requests, some may need cancelled immediately", this.sendWindow.getSize());
            Map<Integer,WindowFuture<Integer,PduRequest,PduResponse>> requests = this.sendWindow.createSortedSnapshot();
            Throwable cause = new ClosedChannelException();
            for (WindowFuture<Integer,PduRequest,PduResponse> future : requests.values()) {
                // is the caller waiting?
                if (future.isCallerWaiting()) {
                    logger.debug("Caller waiting on request [{}], cancelling it with a channel closed exception", future.getKey());
                    try {
                        future.fail(cause);
                    } catch (Exception e) { }
                }
            }
        }

        // we need to check if this "unexpected" or "expected" based on whether
        // this session's unbind() or close() methods triggered a close request
        if (isUnbinding() || isClosed()) {
            // do nothing -- ignore it
            logger.debug("Unbind/close was requested, ignoring channelClosed event");
        } else {
            this.sessionHandler.fireChannelUnexpectedlyClosed();
        }
    }

    @Override
    public void expired(WindowFuture<Integer, PduRequest, PduResponse> future) {
        this.countSendRequestPduExpired(future.getRequest());
        this.sessionHandler.firePduRequestExpired(future.getRequest());
    }

    private void countSendRequestPdu(PduRequest pdu) {
        if (this.counters == null) {
            return;     // noop
        }
        
        if (pdu.isRequest()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM:
                    this.counters.getTxSubmitSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM:
                    this.counters.getTxDeliverSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_DATA_SM:
                    this.counters.getTxDataSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK:
                    this.counters.getTxEnquireLink().incrementRequestAndGet();
                    break;
            }
        }
    }
    
    private void countSendResponsePdu(PduResponse pdu, long responseTime, long estimatedProcessingTime) {
        if (this.counters == null) {
            return;     // noop
        }
        
        if (pdu.isResponse()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
                    this.counters.getRxSubmitSM().incrementResponseAndGet();
                    this.counters.getRxSubmitSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getRxSubmitSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getRxSubmitSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM_RESP:
                    this.counters.getRxDeliverSM().incrementResponseAndGet();
                    this.counters.getRxDeliverSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getRxDeliverSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getRxDeliverSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DATA_SM_RESP:
                    this.counters.getRxDataSM().incrementResponseAndGet();
                    this.counters.getRxDataSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getRxDataSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getRxDataSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
                    this.counters.getRxEnquireLink().incrementResponseAndGet();
                    this.counters.getRxEnquireLink().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getRxEnquireLink().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getRxEnquireLink().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
            }
        }
    }
    
    private void countSendRequestPduExpired(PduRequest pdu) {
        if (this.counters == null) {
            return;     // noop
        }
        
        if (pdu.isRequest()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM:
                    this.counters.getTxSubmitSM().incrementRequestExpiredAndGet();
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM:
                    this.counters.getTxDeliverSM().incrementRequestExpiredAndGet();
                    break;
                case SmppConstants.CMD_ID_DATA_SM:
                    this.counters.getTxDataSM().incrementRequestExpiredAndGet();
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK:
                    this.counters.getTxEnquireLink().incrementRequestExpiredAndGet();
                    break;
            }
        }
    }
    
    private void countReceiveRequestPdu(PduRequest pdu) {
        if (this.counters == null) {
            return;     // noop
        }
        
        if (pdu.isRequest()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM:
                    this.counters.getRxSubmitSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM:
                    this.counters.getRxDeliverSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_DATA_SM:
                    this.counters.getRxDataSM().incrementRequestAndGet();
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK:
                    this.counters.getRxEnquireLink().incrementRequestAndGet();
                    break;
            }
        }
    }
    
    private void countReceiveResponsePdu(PduResponse pdu, long waitTime, long responseTime, long estimatedProcessingTime) {
        if (this.counters == null) {
            return;     // noop
        }
        
        if (pdu.isResponse()) {
            switch (pdu.getCommandId()) {
                case SmppConstants.CMD_ID_SUBMIT_SM_RESP:
                    this.counters.getTxSubmitSM().incrementResponseAndGet();
                    this.counters.getTxSubmitSM().addRequestWaitTimeAndGet(waitTime);
                    this.counters.getTxSubmitSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getTxSubmitSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getTxSubmitSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DELIVER_SM_RESP:
                    this.counters.getTxDeliverSM().incrementResponseAndGet();
                    this.counters.getTxDeliverSM().addRequestWaitTimeAndGet(waitTime);
                    this.counters.getTxDeliverSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getTxDeliverSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getTxDeliverSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_DATA_SM_RESP:
                    this.counters.getTxDataSM().incrementResponseAndGet();
                    this.counters.getTxDataSM().addRequestWaitTimeAndGet(waitTime);
                    this.counters.getTxDataSM().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getTxDataSM().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getTxDataSM().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
                case SmppConstants.CMD_ID_ENQUIRE_LINK_RESP:
                    this.counters.getTxEnquireLink().incrementResponseAndGet();
                    this.counters.getTxEnquireLink().addRequestWaitTimeAndGet(waitTime);
                    this.counters.getTxEnquireLink().addRequestResponseTimeAndGet(responseTime);
                    this.counters.getTxEnquireLink().addRequestEstimatedProcessingTimeAndGet(estimatedProcessingTime);
                    this.counters.getTxEnquireLink().getResponseCommandStatusCounter().incrementAndGet(pdu.getCommandStatus());
                    break;
            }
        }
    }
    
    // mainly for JMX management

    @Override
    public void resetCounters() {
        if (hasCounters()) {
            this.counters.reset();
        }
    }
    
    @Override
    public String getBindTypeName() {
        return this.getBindType().toString();
    }

    @Override
    public String getBoundDuration() {
        return PeriodFormatterUtil.toLinuxUptimeStyleString(System.currentTimeMillis() - getBoundTime());
    }

    @Override
    public String getInterfaceVersionName() {
        return SmppUtil.toInterfaceVersionString(interfaceVersion);
    }

    @Override
    public String getLocalTypeName() {
        return this.getLocalType().toString();
    }

    @Override
    public String getRemoteTypeName() {
        return this.getRemoteType().toString();
    }

    @Override
    public int getNextSequenceNumber() {
        return this.sequenceNumber.peek();
    }

    @Override
    public String getLocalAddressAndPort() {
        if (this.channel != null) {
            InetSocketAddress addr = (InetSocketAddress)this.channel.getLocalAddress();
            return addr.getAddress().getHostAddress() + ":" + addr.getPort();
        } else {
            return null;
        }
    }

    @Override
    public String getRemoteAddressAndPort() {
        if (this.channel != null) {
            InetSocketAddress addr = (InetSocketAddress)this.channel.getRemoteAddress();
            return addr.getAddress().getHostAddress() + ":" + addr.getPort();
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return this.configuration.getName();
    }

    @Override
    public String getPassword() {
        return this.configuration.getPassword();
    }

    @Override
    public long getRequestExpiryTimeout() {
        return this.configuration.getRequestExpiryTimeout();
    }

    @Override
    public String getSystemId() {
        return this.configuration.getSystemId();
    }

    @Override
    public String getSystemType() {
        return this.configuration.getSystemType();
    }

    @Override
    public boolean isWindowMonitorEnabled() {
        return (this.monitorExecutor != null && this.configuration.getWindowMonitorInterval() > 0);
    }
    
    @Override
    public long getWindowMonitorInterval() {
        return this.configuration.getWindowMonitorInterval();
    }
    
    @Override
    public int getMaxWindowSize() {
        return this.sendWindow.getMaxSize();
    }

    @Override
    public int getWindowSize() {
        return this.sendWindow.getSize();
    }

    @Override
    public long getWindowWaitTimeout() {
        return this.configuration.getWindowWaitTimeout();
    }
    
    @Override
    public String[] dumpWindow() {
        Map<Integer,WindowFuture<Integer,PduRequest,PduResponse>> sortedSnapshot = this.sendWindow.createSortedSnapshot();
        String[] dump = new String[sortedSnapshot.size()];
        int i = 0;
        for (WindowFuture<Integer,PduRequest,PduResponse> future : sortedSnapshot.values()) {
            dump[i] = future.getRequest().toString();
            i++;
        }
        return dump;
    }

    @Override
    public String getRxDataSMCounter() {
        return hasCounters() ? this.counters.getRxDataSM().toString() : null;
    }

    @Override
    public String getRxDeliverSMCounter() {
        return hasCounters() ? this.counters.getRxDeliverSM().toString() : null;
    }

    @Override
    public String getRxEnquireLinkCounter() {
        return hasCounters() ? this.counters.getRxEnquireLink().toString() : null;
    }

    @Override
    public String getRxSubmitSMCounter() {
        return hasCounters() ? this.counters.getRxSubmitSM().toString() : null;
    }

    @Override
    public String getTxDataSMCounter() {
        return hasCounters() ? this.counters.getTxDataSM().toString() : null;
    }

    @Override
    public String getTxDeliverSMCounter() {
        return hasCounters() ? this.counters.getTxDeliverSM().toString() : null;
    }

    @Override
    public String getTxEnquireLinkCounter() {
        return hasCounters() ? this.counters.getTxEnquireLink().toString() : null;
    }

    @Override
    public String getTxSubmitSMCounter() {
        return hasCounters() ? this.counters.getTxSubmitSM().toString() : null;
    }
    
    @Override
    public void enableLogBytes() {
        this.configuration.getLoggingOptions().setLogBytes(true);
    }
    
    @Override
    public void disableLogBytes() {
        this.configuration.getLoggingOptions().setLogBytes(false);
    }
    
    @Override
    public void enableLogPdu() {
        this.configuration.getLoggingOptions().setLogPdu(true);
    }
    
    @Override
    public void disableLogPdu() {
        this.configuration.getLoggingOptions().setLogPdu(false);
    }
}
