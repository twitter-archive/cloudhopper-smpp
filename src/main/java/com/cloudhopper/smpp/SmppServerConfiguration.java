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

/**
 * Configuration of an SMPP server.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppServerConfiguration extends SmppConnectionConfiguration {

    private String name;
    // SSL
    private boolean useSsl = false;
    private SslConfiguration sslConfiguration;
    // length of time to wait for a bind request
    private long bindTimeout;       
    private String systemId;
    // if true, <= 3.3 for interface version normalizes to version 3.3
    // if true, >= 3.4 for interface version normalizes to version 3.4 and
    // optional sc_interface_version is set to 3.4
    private boolean autoNegotiateInterfaceVersion;
    // smpp version the server supports
    private byte interfaceVersion;
    // max number of connections/sessions this server will expect to handle
    // this number corrosponds to the number of worker threads handling reading
    // data from sockets and the thread things will be processed under
    private int maxConnectionSize;
    private boolean nonBlockingSocketsEnabled;
    private boolean reuseAddress;
    // jmx options
    private boolean jmxEnabled;
    private String jmxDomain;
    // default request expiry timeout on server sessions
    private int defaultWindowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
    private long defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;
    private long defaultRequestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
    private long defaultWindowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
    private boolean defaultSessionCountersEnabled = false;

    public SmppServerConfiguration() {
        super("0.0.0.0", 2775, 5000l);
        this.name = "SmppServer";
        this.bindTimeout = 5000;
        this.systemId = "cloudhopper";
        this.autoNegotiateInterfaceVersion = true;
        this.interfaceVersion = SmppConstants.VERSION_3_4;
        this.maxConnectionSize = SmppConstants.DEFAULT_SERVER_MAX_CONNECTION_SIZE;
        this.nonBlockingSocketsEnabled = SmppConstants.DEFAULT_SERVER_NON_BLOCKING_SOCKETS_ENABLED;
        this.reuseAddress = SmppConstants.DEFAULT_SERVER_REUSE_ADDRESS;
        this.jmxEnabled = false;
        this.jmxDomain = "com.cloudhopper.smpp";
        this.defaultWindowSize = SmppConstants.DEFAULT_WINDOW_SIZE;
        this.defaultWindowWaitTimeout = SmppConstants.DEFAULT_WINDOW_WAIT_TIMEOUT;
        this.defaultRequestExpiryTimeout = SmppConstants.DEFAULT_REQUEST_EXPIRY_TIMEOUT;
        this.defaultWindowMonitorInterval = SmppConstants.DEFAULT_WINDOW_MONITOR_INTERVAL;
    }

    public String getJmxDomain() {
        return jmxDomain;
    }

    public void setJmxDomain(String jmxDomain) {
        this.jmxDomain = jmxDomain;
    }

    public boolean isJmxEnabled() {
        return jmxEnabled;
    }

    public void setJmxEnabled(boolean jmxEnabled) {
        this.jmxEnabled = jmxEnabled;
    }
    
    public boolean isReuseAddress() {
        return reuseAddress;
    }

    public void setReuseAddress(boolean reuseAddress) {
        this.reuseAddress = reuseAddress;
    }

    public boolean isNonBlockingSocketsEnabled() {
        return nonBlockingSocketsEnabled;
    }

    /**
     * Sets if non-blocking (NIO) is used for this server.  If true, then the
     * NIO server socket is used for Netty, otherwise the normal blocking
     * server socket will be used.
     * @param nonBlockingSocketsEnabled True if enabled, otherwise false 
     */
    public void setNonBlockingSocketsEnabled(boolean nonBlockingSocketsEnabled) {
        this.nonBlockingSocketsEnabled = nonBlockingSocketsEnabled;
    }

    public int getMaxConnectionSize() {
        return maxConnectionSize;
    }

    /**
     * Set the maximum number of connections this server is configured to
     * handle.
     * @param maxConnectionSize Max number of connections. Must be >= 1
     */
    public void setMaxConnectionSize(int maxConnectionSize) {
        if (this.maxConnectionSize < 1) {
            throw new IllegalArgumentException("Max connection size must be >= 1");
        }
        this.maxConnectionSize = maxConnectionSize;
    }

    /**
     * Set the name of the server that is used for some logging and naming of
     * threads.  The default is "SmppServer".
     * @param value The name of this server
     */
    public void setName(String value) {
        this.name = value;
    }

    public String getName() {
        return this.name;
    }

    public void setUseSsl(boolean value) {
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
     * Set the amount of time to allow a connection to finish binding into the
     * server before the server automatically closes the connection.
     * @param value The number of milliseconds to wait for a bind to be established
     *      before a socket will be closed.
     */
    public void setBindTimeout(long value) {
        this.bindTimeout = value;
    }

    public long getBindTimeout() {
        return this.bindTimeout;
    }

    /**
     * Set the system id that will be returned in a bind response.
     * @param value The system id to return in a bind response
     */
    public void setSystemId(String value) {
        this.systemId = value;
    }

    public String getSystemId() {
        return this.systemId;
    }

    public boolean isAutoNegotiateInterfaceVersion() {
        return autoNegotiateInterfaceVersion;
    }

    /**
     * Enables or disables auto sc_interface_version negotiation.  If the
     * version from the client <= 3.3 then the client version is 3.3.  If the
     * version from the client >= 3.4 then the client version will be 3.4 and
     * the prepared bind response will include the optional parameter sc_interface_version.
     * @param autoNegotiateInterfaceVersion
     */
    public void setAutoNegotiateInterfaceVersion(boolean autoNegotiateInterfaceVersion) {
        this.autoNegotiateInterfaceVersion = autoNegotiateInterfaceVersion;
    }

    public byte getInterfaceVersion() {
        return interfaceVersion;
    }

    public void setInterfaceVersion(byte interfaceVersion) {
        this.interfaceVersion = interfaceVersion;
    }

    public long getDefaultRequestExpiryTimeout() {
        return defaultRequestExpiryTimeout;
    }

    public void setDefaultRequestExpiryTimeout(long defaultRequestExpiryTimeout) {
        this.defaultRequestExpiryTimeout = defaultRequestExpiryTimeout;
    }

    public long getDefaultWindowMonitorInterval() {
        return defaultWindowMonitorInterval;
    }

    public void setDefaultWindowMonitorInterval(long defaultWindowMonitorInterval) {
        this.defaultWindowMonitorInterval = defaultWindowMonitorInterval;
    }

    public int getDefaultWindowSize() {
        return defaultWindowSize;
    }

    public void setDefaultWindowSize(int defaultWindowSize) {
        this.defaultWindowSize = defaultWindowSize;
    }

    public long getDefaultWindowWaitTimeout() {
        return defaultWindowWaitTimeout;
    }

    public void setDefaultWindowWaitTimeout(long defaultWindowWaitTimeout) {
        this.defaultWindowWaitTimeout = defaultWindowWaitTimeout;
    }

    public boolean isDefaultSessionCountersEnabled() {
        return defaultSessionCountersEnabled;
    }

    public void setDefaultSessionCountersEnabled(boolean defaultSessionCountersEnabled) {
        this.defaultSessionCountersEnabled = defaultSessionCountersEnabled;
    }

}
