
As a client:
  SSLContext.getDefault() seems like the most simple approach. It uses Java's
  default keystore which is preloaded with all current CAs.  Assuming the server
  uses ones of these, you're all set.  If they don't, you could add their CA
  or their cert to the keystore.

  If you want to accept all server certs, SSLContext.getInstance() followed by
  a custom TrustManager does the trick.

  If you want to load a custom keystore and trustmanager, probably should provide
  a factory method for that too.