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

import com.cloudhopper.smpp.type.SmppChannelException;

/**
 * Defines the interface for managing an SmppServer.
 * 
 * @author joelauer
 */
public interface DefaultSmppServerMXBean {
    
    public boolean isStarted();
    
    public boolean isStopped();
    
    public boolean isDestroyed();
    
    public void start() throws SmppChannelException;
    
    public void stop();
    
    public void destroy();
    
    // access to various configuration, stats, and counters useful for monitoring
    
    public void resetCounters();
    
    public int getSessionSize();
    
    public int getTransceiverSessionSize();
    
    public int getTransmitterSessionSize();
    
    public int getReceiverSessionSize();
    
    public int getMaxConnectionSize();
    
    public int getConnectionSize();
    
    public long getBindTimeout();
    
    public boolean isNonBlockingSocketsEnabled();
    
    public boolean isReuseAddress();
    
    public int getChannelConnects();
    
    public int getChannelDisconnects();
    
    public int getBindTimeouts();
    
    public int getBindRequested();
    
    public int getSessionCreated();
    
    public int getSessionDestroyed();
    
}
