package com.cloudhopper.smpp.jmx;

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

import java.util.Map;

/**
 * Defines the interface for an SmppSession managed bean.
 * 
 * @author joelauer
 */
public interface DefaultSmppSessionMXBean {

    public boolean isBinding();

    public boolean isBound();

    public boolean isClosed();

    public boolean isOpen();

    public boolean isUnbinding();
    
    public String getStateName();
    
    public String getBindTypeName();

    public String getBoundDuration();
    
    public String getInterfaceVersionName();
    
    public String getLocalTypeName();

    public String getRemoteTypeName();

    public int getNextSequenceNumber();
    
    public String getLocalAddressAndPort();
    
    public String getRemoteAddressAndPort();

    public void close();

    public void close(long timeoutInMillis);

    public void destroy();

    public void unbind(long timeoutInMillis);
    
    // most from configuration of a session
    public String getName();

    public String getPassword();

    public long getRequestExpiryTimeout();

    public String getSystemId();

    public String getSystemType();

    public boolean isWindowMonitorEnabled();
    
    public long getWindowMonitorInterval();

    public int getMaxWindowSize();

    public long getWindowWaitTimeout();
    
    // pulled from state objects and counters
    public int getWindowSize();
    
    public String[] dumpWindow();
    
    public void resetCounters();
    
    public String getRxDataSMCounter();

    public String getRxDeliverSMCounter();

    public String getRxEnquireLinkCounter();

    public String getRxSubmitSMCounter();

    public String getTxDataSMCounter();

    public String getTxDeliverSMCounter();

    public String getTxEnquireLinkCounter();

    public String getTxSubmitSMCounter();
    
    public void enableLogBytes();
    
    public void disableLogBytes();
    
    public void enableLogPdu();
    
    public void disableLogPdu();
}
