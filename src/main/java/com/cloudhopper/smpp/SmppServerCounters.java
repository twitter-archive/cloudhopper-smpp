package com.cloudhopper.smpp;

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

/**
 * Interface defining the counters that will be tracked for an SMPP server.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppServerCounters {
    
    /**
     * Clears all counters (including session size(s)).
     */
    public void clear();
    
    /**
     * Resets counters that don't track any state (e.g. session size(s)).
     */
    public void reset();
    
    public int getChannelConnects();
    
    public int getChannelDisconnects();
    
    public int getBindTimeouts();
    
    public int getBindRequested();
    
    public int getSessionCreated();
    
    public int getSessionDestroyed();
    
    public int getReceiverSessionSize();

    public int getSessionSize();

    public int getTransceiverSessionSize();

    public int getTransmitterSessionSize();
    
}
