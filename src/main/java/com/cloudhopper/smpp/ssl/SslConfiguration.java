package com.cloudhopper.smpp.ssl;

/*
 * #%L
 * ch-smpp
 * %%
 * Copyright (C) 2009 - 2013 Cloudhopper by Twitter
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

import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * Configuration for SSL. 
 * 
 * @author garth
 */
public class SslConfiguration
{
    
    public static final String DEFAULT_KEYMANAGERFACTORY_ALGORITHM =
        (Security.getProperty("ssl.KeyManagerFactory.algorithm") == null ?
	 "SunX509" : Security.getProperty("ssl.KeyManagerFactory.algorithm"));
    public static final String DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM =
        (Security.getProperty("ssl.TrustManagerFactory.algorithm") == null ?
	 "SunX509" : Security.getProperty("ssl.TrustManagerFactory.algorithm"));
    
    private final Set<String> excludeProtocols = new HashSet<String>();
    private Set<String> includeProtocols = null;
    
    private final Set<String> excludeCipherSuites = new HashSet<String>();
    private Set<String> includeCipherSuites = null;

    private String keyStorePath;
    private String keyStoreProvider;
    private String keyStoreType = "JKS";
    private String trustStorePath;
    private String trustStoreProvider;
    private String trustStoreType = "JKS";
    private transient String keyStorePassword;
    private transient String trustStorePassword;
    private transient String keyManagerPassword;

    private String certAlias;

    private boolean needClientAuth = false;
    private boolean wantClientAuth = false;
    private boolean allowRenegotiate = true;

    private String sslProvider;
    private String sslProtocol = "TLS";

    private String secureRandomAlgorithm;
    private String keyManagerFactoryAlgorithm = DEFAULT_KEYMANAGERFACTORY_ALGORITHM;
    private String trustManagerFactoryAlgorithm = DEFAULT_TRUSTMANAGERFACTORY_ALGORITHM;

    private boolean validateCerts;
    private boolean validatePeerCerts;
    private int maxCertPathLength = -1;
    private String crlPath;
    private boolean enableCRLDP = false;
    private boolean enableOCSP = false;
    private String ocspResponderURL;

    private boolean sessionCachingEnabled = true;
    private int sslSessionCacheSize;
    private int sslSessionTimeout;

    private boolean trustAll = true;

    /**
     * @return The array of protocol names to exclude from
     * {@link SSLEngine#setEnabledProtocols(String[])}
     */
    public String[] getExcludeProtocols() {
	return this.excludeProtocols == null ? null :
	    this.excludeProtocols.toArray(new String[this.excludeProtocols.size()]);
    }

    /**
     * @param protocols The array of protocol names to exclude from
     * {@link SSLEngine#setEnabledProtocols(String[])}
     */
    public void setExcludeProtocols(String... protocols) {
        this.excludeProtocols.clear();
        this.excludeProtocols.addAll(Arrays.asList(protocols));
    }

    /**
     * @param protocol Protocol names to add to {@link SSLEngine#setEnabledProtocols(String[])}
     */
    public void addExcludeProtocols(String... protocol) {
        this.excludeProtocols.addAll(Arrays.asList(protocol));
    }
    
    /**
     * @return The array of protocol names to include in
     * {@link SSLEngine#setEnabledProtocols(String[])}
     */
    public String[] getIncludeProtocols() {
	return this.includeProtocols == null ? null :
	    this.includeProtocols.toArray(new String[this.includeProtocols.size()]);
    }

    /**
     * @param protocols The array of protocol names to include in
     * {@link SSLEngine#setEnabledProtocols(String[])}
     */
    public void setIncludeProtocols(String... protocols) {
        this.includeProtocols = new HashSet<String>(Arrays.asList(protocols));
    }

    /**
     * @return The array of cipher suite names to exclude from
     * {@link SSLEngine#setEnabledCipherSuites(String[])}
     */
    public String[] getExcludeCipherSuites() {
	return this.excludeCipherSuites == null ? null :
	    this.excludeCipherSuites.toArray(new String[this.excludeCipherSuites.size()]);
    }

    /**
     * @param cipherSuites The array of cipher suite names to exclude from
     * {@link SSLEngine#setEnabledCipherSuites(String[])}
     */
    public void setExcludeCipherSuites(String... cipherSuites) {
        this.excludeCipherSuites.clear();
        this.excludeCipherSuites.addAll(Arrays.asList(cipherSuites));
    }
    
    /**
     * @param cipher Cipher names to add to {@link SSLEngine#setEnabledCipherSuites(String[])}
     */
    public void addExcludeCipherSuites(String... cipher) {
        this.excludeCipherSuites.addAll(Arrays.asList(cipher));
    }

    /**
     * @return The array of cipher suite names to include in
     * {@link SSLEngine#setEnabledCipherSuites(String[])}
     */
    public String[] getIncludeCipherSuites() {
	return this.includeCipherSuites == null ? null:
	    this.includeCipherSuites.toArray(new String[this.includeCipherSuites.size()]);
    }

    /**
     * @param cipherSuites The array of cipher suite names to include in
     * {@link SSLEngine#setEnabledCipherSuites(String[])}
     */
    public void setIncludeCipherSuites(String... cipherSuites) {
        this.includeCipherSuites = new HashSet<String>(Arrays.asList(cipherSuites));
    }

    /**
     * @return The file or URL of the SSL Key store.
     */
    public String getKeyStorePath() {
        return this.keyStorePath;
    }

    /**
     * @param keyStorePath The file or URL of the SSL Key store.
     */
    public void setKeyStorePath(String keyStorePath) {
        this.keyStorePath = keyStorePath;
    }

    /**
     * @return The provider of the key store
     */
    public String getKeyStoreProvider() {
        return this.keyStoreProvider;
    }

    /**
     * @param keyStoreProvider The provider of the key store
     */
    public void setKeyStoreProvider(String keyStoreProvider) {
        this.keyStoreProvider = keyStoreProvider;
    }

    /**
     * @return The type of the key store (default "JKS")
     */
    public String getKeyStoreType() {
        return this.keyStoreType;
    }

    /**
     * @param keyStoreType The type of the key store (default "JKS")
     */
    public void setKeyStoreType(String keyStoreType) {
        this.keyStoreType = keyStoreType;
    }

    /**
     * @return Alias of SSL certificate for the connector
     */
    public String getCertAlias() {
        return this.certAlias;
    }

    /**
     * @param certAlias Alias of SSL certificate for the connector
     */
    public void setCertAlias(String certAlias) {
        this.certAlias = certAlias;
    }

    /**
     * @return The file name or URL of the trust store location
     */
    public String getTrustStorePath() {
        return this.trustStorePath;
    }

    /**
     * @param trustStorePath The file name or URL of the trust store location
     */
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    /**
     * @return The provider of the trust store
     */
    public String getTrustStoreProvider() {
        return this.trustStoreProvider;
    }

    /**
     * @param trustStoreProvider The provider of the trust store
     */
    public void setTrustStoreProvider(String trustStoreProvider) {
        this.trustStoreProvider = trustStoreProvider;
    }

    /**
     * @return The type of the trust store (default "JKS")
     */
    public String getTrustStoreType() {
        return this.trustStoreType;
    }

    /**
     * @param trustStoreType The type of the trust store (default "JKS")
     */
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    /**
     * @return True if SSL needs client authentication.
     * @see SSLEngine#getNeedClientAuth()
     */
    public boolean getNeedClientAuth() {
        return this.needClientAuth;
    }

    /**
     * @param needClientAuth True if SSL needs client authentication.
     */
    public void setNeedClientAuth(boolean needClientAuth) {
        this.needClientAuth = needClientAuth;
    }

    /**
     * @return True if SSL wants client authentication.
     * @see SSLEngine#getWantClientAuth()
     */
    public boolean getWantClientAuth() {
        return this.wantClientAuth;
    }

    /**
     * @param wantClientAuth True if SSL wants client authentication.
     */
    public void setWantClientAuth(boolean wantClientAuth) {
        this.wantClientAuth = wantClientAuth;
    }

    /**
     * @return true if SSL certificate has to be validated
     */
    public boolean isValidateCerts() {
        return this.validateCerts;
    }

    /**
     * @param validateCerts true if SSL certificates have to be validated
     */
    public void setValidateCerts(boolean validateCerts) {
        this.validateCerts = validateCerts;
    }

    /**
     * @return true if SSL certificates of the peer have to be validated
     */
    public boolean isValidatePeerCerts() {
        return this.validatePeerCerts;
    }

    /**
     * @param validatePeerCerts true if SSL certificates of the peer have to be validated
     */
    public void setValidatePeerCerts(boolean validatePeerCerts) {
        this.validatePeerCerts = validatePeerCerts;
    }

    /**
     * @return True if SSL re-negotiation is allowed (default false)
     */
    public boolean isAllowRenegotiate() {
        return this.allowRenegotiate;
    }

    /**
     * Set if SSL re-negotiation is allowed. CVE-2009-3555 discovered
     * a vulnerability in SSL/TLS with re-negotiation.  If your JVM
     * does not have CVE-2009-3555 fixed, then re-negotiation should
     * not be allowed.  CVE-2009-3555 was fixed in Sun java 1.6 with a ban
     * of renegotiates in u19 and with RFC5746 in u22.
     *
     * @param allowRenegotiate
     *            true if re-negotiation is allowed (default false)
     */
    public void setAllowRenegotiate(boolean allowRenegotiate) {
        this.allowRenegotiate = allowRenegotiate;
    }

    /**
     * @param password The password for the key store
     */
    public void setKeyStorePassword(String password) {
        this.keyStorePassword = password;
    }

    /**
     * @return The password for the key store
     */
    public String getKeyStorePassword() {
	return this.keyStorePassword;
    }

    /**
     * @param password The password (if any) for the specific key within the key store
     */
    public void setKeyManagerPassword(String password) {
        this.keyManagerPassword = password;
    }

    /**
     * @return The password (if any) for the specific key within the key store
     */
    public String getKeyManagerPassword() {
	return this.keyManagerPassword;
    }

    /**
     * @param password The password for the trust store
     */
    public void setTrustStorePassword(String password) {
        this.trustStorePassword = password;
    }

    /**
     * @return The password for the trust store
     */
    public String getTrustStorePassword() {
	return this.trustStorePassword;
    }

    /**
     * @return The SSL provider name, which if set is passed to
     * {@link SSLContext#getInstance(String, String)}
     */
    public String getProvider() {
        return this.sslProvider;
    }

    /**
     * @param provider The SSL provider name, which if set is passed to
     * {@link SSLContext#getInstance(String, String)}
     */
    public void setProvider(String provider) {
        this.sslProvider = provider;
    }

    /**
     * @return The SSL protocol (default "TLS") passed to
     * {@link SSLContext#getInstance(String, String)}
     */
    public String getProtocol() {
        return this.sslProtocol;
    }

    /**
     * @param protocol The SSL protocol (default "TLS") passed to
     * {@link SSLContext#getInstance(String, String)}
     */
    public void setProtocol(String protocol) {
        this.sslProtocol = protocol;
    }

    /**
     * @return The algorithm name, which if set is passed to
     * {@link SecureRandom#getInstance(String)} to obtain the {@link SecureRandom} instance passed to
     * {@link SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], SecureRandom)}
     */
    public String getSecureRandomAlgorithm() {
        return this.secureRandomAlgorithm;
    }

    /**
     * @param algorithm The algorithm name, which if set is passed to
     * {@link SecureRandom#getInstance(String)} to obtain the {@link SecureRandom} instance passed to
     * {@link SSLContext#init(javax.net.ssl.KeyManager[], javax.net.ssl.TrustManager[], SecureRandom)}
     */
    public void setSecureRandomAlgorithm(String algorithm) {
        this.secureRandomAlgorithm = algorithm;
    }

    /**
     * @return The algorithm name (default "SunX509") used by the {@link KeyManagerFactory}
     */
    public String getKeyManagerFactoryAlgorithm() {
        return this.keyManagerFactoryAlgorithm;
    }
    
    /**
     * @param algorithm The algorithm name (default "SunX509") used by the {@link KeyManagerFactory}
     */
    public void setKeyManagerFactoryAlgorithm(String algorithm) {
        this.keyManagerFactoryAlgorithm = algorithm;
    }

    /**
     * @return The algorithm name (default "SunX509") used by the {@link TrustManagerFactory}
     */
    public String getTrustManagerFactoryAlgorithm() {
        return this.trustManagerFactoryAlgorithm;
    }

    /**
     * @return True if all certificates should be trusted if there is no KeyStore or TrustStore
     */
    public boolean isTrustAll() {
        return this.trustAll;
    }

    /**
     * @param trustAll True if all certificates should be trusted if there is no KeyStore or TrustStore
     */
    public void setTrustAll(boolean trustAll) {
        this.trustAll = trustAll;
    }

    /**
     * @param algorithm The algorithm name (default "SunX509") used by the {@link TrustManagerFactory}
     * Use the string "TrustAll" to install a trust manager that trusts all.
     */
    public void setTrustManagerFactoryAlgorithm(String algorithm) {
        this.trustManagerFactoryAlgorithm = algorithm;
    }

    /**
     * @return Path to file that contains Certificate Revocation List
     */
    public String getCrlPath() {
        return this.crlPath;
    }

    /**
     * @param crlPath Path to file that contains Certificate Revocation List
     */
    public void setCrlPath(String crlPath) {
        this.crlPath = crlPath;
    }

    /**
     * @return Maximum number of intermediate certificates in
     * the certification path (-1 for unlimited)
     */
    public int getMaxCertPathLength() {
        return this.maxCertPathLength;
    }

    /**
     * @param maxCertPathLength maximum number of intermediate certificates in
     * the certification path (-1 for unlimited)
     */
    public void setMaxCertPathLength(int maxCertPathLength) {
        this.maxCertPathLength = maxCertPathLength;
    }

    /**
     * @return true if CRL Distribution Points support is enabled
     */
    public boolean isEnableCRLDP() {
        return this.enableCRLDP;
    }

    /**
     * Enables CRL Distribution Points Support
     * @param enableCRLDP true - turn on, false - turns off
     */
    public void setEnableCRLDP(boolean enableCRLDP) {
        this.enableCRLDP = enableCRLDP;
    }

    /**
     * @return true if On-Line Certificate Status Protocol support is enabled
     */
    public boolean isEnableOCSP() {
        return this.enableOCSP;
    }

    /**
     * Enables On-Line Certificate Status Protocol support
     * @param enableOCSP true - turn on, false - turn off
     */
    public void setEnableOCSP(boolean enableOCSP) {
        this.enableOCSP = enableOCSP;
    }

    /**
     * @return Location of the OCSP Responder
     */
    public String getOcspResponderURL() {
        return this.ocspResponderURL;
    }

    /**
     * Set the location of the OCSP Responder.
     * @param ocspResponderURL location of the OCSP Responder
     */
    public void setOcspResponderURL(String ocspResponderURL) {
        this.ocspResponderURL = ocspResponderURL;
    }
    
    /**
     * @return true if SSL Session caching is enabled
     */
    public boolean isSessionCachingEnabled() {
        return this.sessionCachingEnabled;
    }
    
    /**
     * Set the flag to enable SSL Session caching.
     * @param enableSessionCaching the value of the flag
     */
    public void setSessionCachingEnabled(boolean enableSessionCaching) {
        this.sessionCachingEnabled = enableSessionCaching;
    }

    /**
     * Get SSL session cache size.
     * @return SSL session cache size
     */
    public int getSslSessionCacheSize() {
        return this.sslSessionCacheSize;
    }

    /**
     * Set SSL session cache size.
     * @param sslSessionCacheSize SSL session cache size to set
     */
    public void setSslSessionCacheSize(int sslSessionCacheSize) {
        this.sslSessionCacheSize = sslSessionCacheSize;
    }

    /**
     * Get SSL session timeout.
     * @return SSL session timeout
     */
    public int getSslSessionTimeout() {
        return this.sslSessionTimeout;
    }

    /**
     * Set SSL session timeout.
     * @param sslSessionTimeout SSL session timeout to set
     */
    public void setSslSessionTimeout(int sslSessionTimeout) {
        this.sslSessionTimeout = sslSessionTimeout;
    }
    
}
