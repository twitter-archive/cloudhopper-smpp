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

import com.cloudhopper.smpp.SmppServerCounters;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of a SmppSessionCounters interface.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppServerCounters implements SmppServerCounters {
    
    private AtomicInteger channelConnects;
    private AtomicInteger channelDisconnects;
    private AtomicInteger bindTimeouts;
    private AtomicInteger bindRequested;
    private AtomicInteger sessionCreated;
    private AtomicInteger sessionDestroyed;
    private AtomicInteger sessionSize;
    private AtomicInteger transceiverSessionSize;
    private AtomicInteger receiverSessionSize;
    private AtomicInteger transmitterSessionSize;
    
    public DefaultSmppServerCounters() {
        this.channelConnects = new AtomicInteger(0);
        this.channelDisconnects = new AtomicInteger(0);
        this.bindTimeouts = new AtomicInteger(0);
        this.bindRequested = new AtomicInteger(0);
        this.sessionCreated = new AtomicInteger(0);
        this.sessionDestroyed = new AtomicInteger(0);
        this.sessionSize = new AtomicInteger(0);
        this.transceiverSessionSize = new AtomicInteger(0);
        this.receiverSessionSize = new AtomicInteger(0);
        this.transmitterSessionSize = new AtomicInteger(0);
    }
    
    @Override
    public void reset() {
        this.channelConnects.set(0);
        this.channelDisconnects.set(0);
        this.bindTimeouts.set(0);
        this.bindRequested.set(0);
        this.sessionCreated.set(0);
        this.sessionDestroyed.set(0);
    }
    
    @Override
    public void clear() {
        this.reset();
        this.sessionSize.set(0);
        this.transceiverSessionSize.set(0);
        this.receiverSessionSize.set(0);
        this.transmitterSessionSize.set(0);
    }

    @Override
    public int getChannelConnects() {
        return this.channelConnects.get();
    }
    
    public int incrementChannelConnectsAndGet() {
        return this.channelConnects.incrementAndGet();
    }

    @Override
    public int getChannelDisconnects() {
        return this.channelDisconnects.get();
    }
    
    public int incrementChannelDisconnectsAndGet() {
        return this.channelDisconnects.incrementAndGet();
    }

    @Override
    public int getBindTimeouts() {
        return this.bindTimeouts.get();
    }
    
    public int incrementBindTimeoutsAndGet() {
        return this.bindTimeouts.incrementAndGet();
    }

    @Override
    public int getBindRequested() {
        return this.bindRequested.get();
    }
    
    public int incrementBindRequestedAndGet() {
        return this.bindRequested.incrementAndGet();
    }

    @Override
    public int getSessionCreated() {
        return this.sessionCreated.get();
    }
    
    public int incrementSessionCreatedAndGet() {
        return this.sessionCreated.incrementAndGet();
    }

    @Override
    public int getSessionDestroyed() {
        return this.sessionDestroyed.get();
    }
    
    public int incrementSessionDestroyedAndGet() {
        return this.sessionDestroyed.incrementAndGet();
    }

    @Override
    public int getSessionSize() {
        return sessionSize.get();
    }
    
    public int incrementSessionSizeAndGet() {
        return this.sessionSize.incrementAndGet();
    }
    
    public int decrementSessionSizeAndGet() {
        return this.sessionSize.decrementAndGet();
    }
    
    @Override
    public int getReceiverSessionSize() {
        return receiverSessionSize.get();
    }
    
    public int incrementReceiverSessionSizeAndGet() {
        return this.receiverSessionSize.incrementAndGet();
    }
    
    public int decrementReceiverSessionSizeAndGet() {
        return this.receiverSessionSize.decrementAndGet();
    }

    @Override
    public int getTransceiverSessionSize() {
        return transceiverSessionSize.get();
    }
    
    public int incrementTransceiverSessionSizeAndGet() {
        return this.transceiverSessionSize.incrementAndGet();
    }
    
    public int decrementTransceiverSessionSizeAndGet() {
        return this.transceiverSessionSize.decrementAndGet();
    }

    @Override
    public int getTransmitterSessionSize() {
        return transmitterSessionSize.get();
    }
    
    public int incrementTransmitterSessionSizeAndGet() {
        return this.transmitterSessionSize.incrementAndGet();
    }
    
    public int decrementTransmitterSessionSizeAndGet() {
        return this.transmitterSessionSize.decrementAndGet();
    }
    
    @Override
    public String toString() {
        StringBuilder to = new StringBuilder();
        to.append("[channelConnects=");
        to.append(getChannelConnects());
        to.append(" channelDisconnects=");
        to.append(getChannelDisconnects());
        to.append(" bindTimeouts=");
        to.append(getBindTimeouts());
        to.append(" bindRequested=");
        to.append(getBindRequested());
        to.append(" sessionCreated=");
        to.append(getSessionCreated());
        to.append(" sessionDestroyed=");
        to.append(getSessionDestroyed());
        to.append(" session [size=");
        to.append(getSessionSize());
        to.append(" tr=");
        to.append(getTransceiverSessionSize());
        to.append(" tx=");
        to.append(getTransmitterSessionSize());
        to.append(" rx=");
        to.append(getReceiverSessionSize());
        to.append("]]");
        return to.toString();
    }
}
