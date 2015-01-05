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

import com.cloudhopper.smpp.pdu.Pdu;

/**
 * Interface for listening for events on an SmppSessionChannelHandler.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public interface SmppSessionChannelListener {

    /**
     * Fired when a PDU was successfully decoded and received on this Channel.
     * @param pdu The PDU decoded from the Channel
     */
    public void firePduReceived(Pdu pdu);

    /**
     * Fired when an exception was raised by an I/O thread or an upstream handler.
     * @param t The exception thrown
     */
    public void fireExceptionThrown(Throwable t);

    /**
     * Fired when the Channel is closed (reached EOF or timed out)
     */
    public void fireChannelClosed();

}
