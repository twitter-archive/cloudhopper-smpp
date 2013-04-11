# How to use SSL with cloudhopper-smpp

The purpose of this document is to provide a summary of how to configuration SSL for ch-smpp servers and clients. The internal implementation uses Java Secure Sockets Extension (JSSE).

## Configuring a SMPP server with SSL transport

### Example:

    // Configure the server as you normally would:
    SmppServerConfiguration configuration = new SmppServerConfiguration();
    configuration.setPort(2776);
    ...

    // Then create a SSL configuration:
    SslConfiguration sslConfig = new SslConfiguration();
    sslConfig.setKeyStorePath("path/to/keystore");
    sslConfig.setKeyStorePassword("changeit");
    sslConfig.setKeyManagerPassword("changeit");
    sslConfig.setTrustStorePath("path/to/keystore");
    sslConfig.setTrustStorePassword("changeit");
    ...

    // And add it to the server configuration:
    configuration.setUseSsl(true);
    configuration.setSslConfiguration(sslConfig);


### Require client auth

    sslConfig.setNeedClientAuth(true);


## Configuring a SMPP client with SSL transport

### Example:

    // Configure the server as you normally would:
    SmppSessionConfiguration configuration = new SmppSessionConfiguration();
    configuration.setType(SmppBindType.TRANSCEIVER);
    configuration.setHost("127.0.0.1");
    configuration.setPort(2776);
    ...

    // Then create a SSL configuration:
    SslConfiguration sslConfig = new SslConfiguration();
    // Which trusts all certs by default. You can turn this off with
    // sslConfig.setTrustAll(false);
    ...

    // And add it to the server configuration:
    configuration.setSslConfiguration(sslConfig);
    configuration.setUseSsl(true);

### Validate certificates

    sslConfig.setValidateCerts(true);
    sslConfig.setValidatePeerCerts(true);


## Generating key pairs and certificates

Generating Keys and Certificates with the JDK's keytool

    keytool -keystore keystore -alias smpp -genkey -keyalg RSA

Generating Keys and Certificates with OpenSSL

    openssl genrsa -des3 -out smpp.key
    openssl req -new -x509 -key smpp.key -out smpp.crt

## Requesting a trusted certificate

Generating a CSR from keytool

    keytool -certreq -alias smpp -keystore keystore -file smpp.csr

Generating a CSR from OpenSSL

    openssl req -new -key smpp.key -out smpp.csr

## Loading keys and certificates

Loading Certificates with keytool

The following command loads a PEM encoded certificate in the smpp.crt file into a JSSE keystore:

    keytool -keystore keystore -import -alias smpp -file smpp.crt -trustcacerts

Loading Keys and Certificates via PKCS12

If you have a key and certificate in separate files, you need to combine them into a PKCS12 format file to load into a new keystore. The certificate can be one you generated yourself or one returned from a CA in response to your CSR. 

The following OpenSSL command combines the keys in smpp.key and the certificate in the smpp.crt file into the smpp.pkcs12 file.

    openssl pkcs12 -inkey smpp.key -in smpp.crt -export -out smpp.pkcs12
    keytool -importkeystore -srckeystore smpp.pkcs12 -srcstoretype PKCS12 -destkeystore keystore


## Appendix

### Interop with stunnel

This library has been tested with stunnel4 wrapping both client and servers. There is a sample stunnel.conf in src/test/resources that works with `make server` and `make ssl-client`. The SSL implementation should be compatible with other TLS/SSL encryption wrappers, assuming the JDK you are using supports the same cryptographic algorithms as the encryption wrapper.

### Known issues



