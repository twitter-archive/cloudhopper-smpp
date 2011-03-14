/**
 * Copyright (C) 2011 Twitter, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.cloudhopper.smpp;

import com.cloudhopper.smpp.type.SmppConnectionConfiguration;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.Address;

/**
 * Configuration to bind an SmppSession as an ESME to an SMSC.
 * 
 * @author joelauer
 */
public class SmppSessionConfiguration extends SmppConnectionConfiguration {

    // other behavioral settings
    private String name;
    private int windowSize;
    // configuration settings
    private SmppBindType type;
    private String systemId;
    private String password;
    private String systemType;
    private byte interfaceVersion;  // interface version requested by us or them
    private Address addressRange;
    private long bindTimeout;       // length of time to wait for a bind response
    // logging settings
    private LoggingOptions loggingOptions;
    // if > 0, then activated
    private long requestExpiryTimeout;

    public SmppSessionConfiguration() {
        this(SmppBindType.TRANSCEIVER, null, null, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password) {
        this(type, systemId, password, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password, String systemType) {
        this.windowSize = 1;
        this.type = type;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.interfaceVersion = SmppConstants.VERSION_3_4;
        this.bindTimeout = SmppConstants.DEFAULT_BIND_TIMEOUT;
        this.loggingOptions = new LoggingOptions();
        this.requestExpiryTimeout = 0;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getName() {
        return this.name;
    }

    public void setWindowSize(int value) {
        this.windowSize = value;
    }

    public int getWindowSize() {
        return this.windowSize;
    }

    public void setBindTimeout(long value) {
        this.bindTimeout = value;
    }

    public long getBindTimeout() {
        return this.bindTimeout;
    }

    public void setType(SmppBindType bindType) {
        this.type = bindType;
    }

    public SmppBindType getType() {
        return this.type;
    }

    public void setSystemId(String value) {
        this.systemId = value;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public String getPassword() {
        return this.password;
    }

    public void setSystemType(String value) {
        this.systemType = value;
    }

    public String getSystemType() {
        return this.systemType;
    }

    public void setInterfaceVersion(byte value) {
        this.interfaceVersion = value;
    }

    public byte getInterfaceVersion() {
        return this.interfaceVersion;
    }

    public Address getAddressRange() {
        return this.addressRange;
    }

    public void setAddressRange(Address value) {
        this.addressRange = value;
    }

    public LoggingOptions getLoggingOptions() {
        return this.loggingOptions;
    }

    public void setLoggingOptions(LoggingOptions loggingOptions) {
        this.loggingOptions = loggingOptions;
    }

    public long getRequestExpiryTimeout() {
        return requestExpiryTimeout;
    }

    public void setRequestExpiryTimeout(long requestExpiryTimeout) {
        this.requestExpiryTimeout = requestExpiryTimeout;
    }
    
}
