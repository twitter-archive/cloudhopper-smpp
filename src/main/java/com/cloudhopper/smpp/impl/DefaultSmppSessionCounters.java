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

import com.cloudhopper.smpp.SmppSessionCounters;
import com.cloudhopper.smpp.util.ConcurrentCommandCounter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of a SmppServerCounters interface.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class DefaultSmppSessionCounters implements SmppSessionCounters {
   
    private ConcurrentCommandCounter txSubmitSM;
    private ConcurrentCommandCounter txDeliverSM;
    private ConcurrentCommandCounter txEnquireLink;
    private ConcurrentCommandCounter txDataSM;
    private ConcurrentCommandCounter rxSubmitSM;
    private ConcurrentCommandCounter rxDeliverSM;
    private ConcurrentCommandCounter rxEnquireLink;
    private ConcurrentCommandCounter rxDataSM;
    
    public DefaultSmppSessionCounters() {
        this.txSubmitSM = new ConcurrentCommandCounter();
        this.txDeliverSM = new ConcurrentCommandCounter();
        this.txEnquireLink = new ConcurrentCommandCounter();
        this.txDataSM = new ConcurrentCommandCounter();
        this.rxSubmitSM = new ConcurrentCommandCounter();
        this.rxDeliverSM = new ConcurrentCommandCounter();
        this.rxEnquireLink = new ConcurrentCommandCounter();
        this.rxDataSM = new ConcurrentCommandCounter();
    }
    
    @Override
    public void reset() {
        this.txSubmitSM.reset();
        this.txDeliverSM.reset();
        this.txEnquireLink.reset();
        this.txDataSM.reset();
        this.rxSubmitSM.reset();
        this.rxDeliverSM.reset();
        this.rxEnquireLink.reset();
        this.rxDataSM.reset();
    }

    @Override
    public ConcurrentCommandCounter getRxDataSM() {
        return rxDataSM;
    }

    @Override
    public ConcurrentCommandCounter getRxDeliverSM() {
        return rxDeliverSM;
    }

    @Override
    public ConcurrentCommandCounter getRxEnquireLink() {
        return rxEnquireLink;
    }

    @Override
    public ConcurrentCommandCounter getRxSubmitSM() {
        return rxSubmitSM;
    }

    @Override
    public ConcurrentCommandCounter getTxDataSM() {
        return txDataSM;
    }

    @Override
    public ConcurrentCommandCounter getTxDeliverSM() {
        return txDeliverSM;
    }

    @Override
    public ConcurrentCommandCounter getTxEnquireLink() {
        return txEnquireLink;
    }

    @Override
    public ConcurrentCommandCounter getTxSubmitSM() {
        return txSubmitSM;
    }
}
