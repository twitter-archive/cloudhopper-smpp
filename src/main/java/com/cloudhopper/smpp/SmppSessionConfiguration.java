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

import com.cloudhopper.smpp.ssl.SslConfiguration;
import com.cloudhopper.smpp.type.SmppConnectionConfiguration;
import com.cloudhopper.smpp.type.LoggingOptions;
import com.cloudhopper.smpp.type.Address;

/**
 * Configuration to bind an SmppSession as an ESME to an SMSC.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppSessionConfiguration extends SmppConnectionConfiguration {

    // SSL
    private boolean useSsl = false;
    private SslConfiguration sslConfiguration;
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
    private long windowWaitTimeout;
    // if > 0, then activated
    private long requestExpiryTimeout;
    private long windowMonitorInterval;
    private long writeTimeout;
    private boolean countersEnabled;

    public SmppSessionConfiguration() {
        this(SmppBindType.TRANSCEIVER, null, null, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password) {
        this(type, systemId, password, null);
    }

    public SmppSessionConfiguration(SmppBindType type, String systemId, String password, String systemType) {
        this.windowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        this.type = type;
        this.systemId = systemId;
        this.password = password;
        this.systemType = systemType;
        this.interfaceVersion = SmppConstants.VERSION_3_4;
        this.bindTimeout = SmppConstants.DEFAULT_BIND_TIMEOUT;
        this.loggingOptions = new LoggingOptions();
        this.windowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;
        this.requestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        this.windowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
        this.writeTimeout = SmppConstants.DEFAULT_WRITE_TIMEOUT;
        this.countersEnabled = false;
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

    public long getWindowWaitTimeout() {
        return windowWaitTimeout;
    }

    public void setUseSsl(boolean value) {
	// By default, make an SslConfiguration that will trust everything.
	if (getSslConfiguration() == null) setSslConfiguration(new SslConfiguration());
	this.useSsl = value;
    }

    public boolean isUseSsl() { 
	return this.useSsl;
    }

    public void setSslConfiguration(SslConfiguration value) {
	this.sslConfiguration = value;
	setUseSsl(true);
    }

    public SslConfiguration getSslConfiguration() {
	return this.sslConfiguration;
    }

    /**
     * Set the amount of time to wait until a slot opens up in the sendWindow.
     * Defaults to 60000.
     * @param windowWaitTimeout The amount of time to wait (in ms) until a slot
     *      in the sendWindow becomes available.
     */
    public void setWindowWaitTimeout(long windowWaitTimeout) {
        this.windowWaitTimeout = windowWaitTimeout;
    }

    public long getRequestExpiryTimeout() {
        return requestExpiryTimeout;
    }

    /**
     * Set the amount of time to wait for an endpoint to respond to
     * a request before it expires. Defaults to disabled (-1).
     * @param requestExpiryTimeout  The amount of time to wait (in ms) before
     *      an unacknowledged request expires.  -1 disables.
     */
    public void setRequestExpiryTimeout(long requestExpiryTimeout) {
        this.requestExpiryTimeout = requestExpiryTimeout;
    }

    public long getWindowMonitorInterval() {
        return windowMonitorInterval;
    }

    /**
     * Sets the amount of time between executions of monitoring the window
     * for requests that expire.  It's recommended that this generally either
     * matches or is half the value of requestExpiryTimeout.  Therefore, at worst
     * a request would could take up 1.5X the requestExpiryTimeout to clear out.
     * @param windowMonitorInterval The amount of time to wait (in ms) between
     *      executions of monitoring the window.
     */
    public void setWindowMonitorInterval(long windowMonitorInterval) {
        this.windowMonitorInterval = windowMonitorInterval;
    }

    public long getWriteTimeout() {
        return writeTimeout;
    }

    public void setWriteTimeout(long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    public boolean isCountersEnabled() {
        return countersEnabled;
    }

    public void setCountersEnabled(boolean countersEnabled) {
        this.countersEnabled = countersEnabled;
    }

}
