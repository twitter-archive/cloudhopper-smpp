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

/**
 * Configuration of an SMPP server.
 * 
 * @author joelauer
 */
public class SmppServerConfiguration {

    private String name;
    private int port;
    // length of time to wait for a bind request
    private long bindTimeout;       
    private String systemId;
    // if true, <= 3.3 for interface version normalizes to version 3.3
    // if true, >= 3.4 for interface version normalizes to version 3.4 and
    // optional sc_interface_version is set to 3.4
    private boolean autoNegotiateInterfaceVersion;
    // smpp version the server supports
    private byte interfaceVersion;

    public SmppServerConfiguration() {
        this.name = "SmppServer";
        this.port = 2775;
        this.bindTimeout = 5000;
        this.systemId = "cloudhopper";
        this.autoNegotiateInterfaceVersion = true;
        this.interfaceVersion = SmppConstants.VERSION_3_4;
    }

    public void setName(String value) {
        this.name = value;
    }

    public String getName() {
        return this.name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setBindTimeout(long value) {
        this.bindTimeout = value;
    }

    public long getBindTimeout() {
        return this.bindTimeout;
    }

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
   
}
