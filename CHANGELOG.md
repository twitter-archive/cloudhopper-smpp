Cloudhopper by Twitter
============================

cloudhopper-smpp
----------------
## 6.0.1
 - Updated netty to 4.1.36.Final

## 6.0.0-netty4-beta-2
 - ch-commons-util version bumped from 6.0.1 to 6.0.2 to fix race condition bug
   in WindowFuture:
     https://github.com/twitter/cloudhopper-smpp/issues/61

## 6.0.0-netty4-beta-1
 - Pre-release of port to Netty 4. Includes community contributions and changes based
   on Trustin's review.
 - Netty dependency changed to 4.0.25.Final.


NOTE: Project moved to https://github.com/fizzed/cloudhopper-smpp

## 5.0.9 - 2015-11-24
 - Add support for missing pdus and tags
 - Allow unbound channel to respond to enquire_link PDU
 - Corrected name of DataCoding SmppConstant to IA5 (not GSM)
 - Updated value of DATA_CODING_GSM constant and made it deprecated
 - Add ESM_CLASS Message Mode constants
 - DefaultSmppServer should use IO executor passed to its constructor
 - Fix-up comments on the SMPP error code constants

## 5.0.8 - 2015-04-17
 - Fixed issue where rawErrorCode not set on DeliveryReceipt (khaing211)
 - Support for host address in SmppServerConfiguration (pgoergler)
 - Improved demo for echo server (dwilkie)
 - Tlv class supports equals and hashCode (skytreader)

## 5.0.7 - 2015-02-02
 - ch-commons-util version bumped from 6.0.1 to 6.0.2 to fix race condition bug 
   in WindowFuture:
     https://github.com/twitter/cloudhopper-smpp/issues/61
 - Netty dependency bumped from 3.9.0.Final to 3.9.6.Final
 
## 5.0.6 - 2014-04-02
 - Support for low-level PDU listener (supports advanced logging, sniffing, and
   discarding before normal processing). New methods overridable in
   DefaultSmppSessionHandler are firePduRecived()
 - Bug with doneDate null-check fixed in DeliveryReceipt.toShortMessage().
 - Delivery receipt intermediate constant has a flag of bit 4 not 5. Please note
   that esm_class does used bit 5.
 - Bumped pom parent to v1.5
 - Dependencies now have specific version rather than version range.
 - Updated docs with user contributed demos

## 5.0.5 - 2014-01-07
 - Changed bindTimeout and writeTimeout implementations to adhere to documented
   best practices:
     http://netty.io/3.9/api/org/jboss/netty/channel/ChannelFuture.html
	 http://netty.io/3.9/api/org/jboss/netty/handler/timeout/WriteTimeoutHandler.html
   This solves a NullPointerException being thrown in the case where we were
   calling ChannelFuture.getCause() before the read/write was complete (and thus null).
 - Netty dependency changed from 3.7.0.Final to 3.9.0.Final

## 5.0.4 - 2013-10-17
 - Tweaked delivery receipt helper class to be more compliant w/ SMPP 3.4
   specs by parsing err code as String rather than as an int. Previous int
   methods were retained for backwards compatability, but accessing the raw
   error code as a String is now possible.
   Thx to williamd1618 for pull request:
     https://github.com/twitter/cloudhopper-smpp/pull/42

## 5.0.3 - 2013-10-13
 - Netty dependency bumped from 3.5.8.Final to 3.7.0.Final
 - Added support for a "writeTimeout" to channel writes.
 - Added support for PDUEncoder to not include message_id in some edge cases.
   Thx to chadselph for pull request:
     https://github.com/twitter/cloudhopper-smpp/pull/31

## 5.0.2 - 2013-04-10
 - Added SSL support for servers and clients with unit and integration tests.

## 5.0.1 - 2013-03-09
 - Added support for cancel_sm and query_sm SMPP messages. Added unit and 
   integration tests.
 - Cleaned up intermittent unit test failures by added delays before session
   close.

## 5.0.0 - 2012-10-26
 - No major code changes, mostly project layout changes in prep of release to
   Maven Central Repository
 - Maven artifact group changed from cloudhopper to com.cloudhopper
 - Readme and changelog switched to markdown
 - SLF4J, Netty, and Logback dependencies switched to use Maven Central groupIds
 - Updated underlying maven license plugin - every java file had its
   license positioned after package declaration.

## 4.1.2 - 2011-10-26
 - Upgrade parent POM dependency from 1.5 to 1.6
 - Added support for OSGI in final jar

## 4.1.1 - 2011-08-18
 - Fixed bug with DataSM decoding and encoding (previous impl was not per
    SMPP 3.4 specs).

## 4.1.0 - 2011-07-26
 - Upgraded ch-commons-util dependency from 5.0 to 5.1.0
 - Upgraded ch-commons-gsm dependency from 2.0 to 2.1.0
 - Upgrade parent POM dependency from 1.0 to 1.5
 - Switched to new version format.
 - Added Version class to compile build info into jar
 - Added SmppInvalidArgumentException class
 - Fixed issue with short messages > than 255 bytes in length not triggering
    an exception to be thrown when attempting to setShortMessage(). The length
    of the byte array is now checked.

## 4.0 - 2011-05-09
 - Upgraded ch-commons-util dependency from 4.1 to 5.0
 - WARN: There are some minor source and binary backwards compatability issues
    that were unavoidable to add request expiry as a new feature.
 - Automatic request expiration is fully supported.  See the new 
    constructors for both DefaultSmppClient and DefaultSmppServer that include
    a "monitorExecutor" parameter to enable the feature.  Also, there are new
    several new options for an SmppServerConfiguration that will control
    the default settings for each server session (including enable monitoring).
       defaultWindowSize
       defaultWindowMonitorInterval
       defaultWindowWaitTimeout
       defaultRequestExpiryTimeout
       maxConnectionSize
 - Added support for counters in both sessions and server for tracking 
    various key metrics.  Counters are enabled on an SmppServer by default, but
    must be explicitly enabled on sessions.  See countersEnableD() option.
 - Added initial support for JMX management by including an SmppServerMXBean
    and an SmppSessionMXBean.  Servers can automatically have themselves and
    their sessions registered & unregistered, but clients will need to use
    explictly register/unregister.
 - Added 3 new time measurements on asynchronous responses (window wait time,
    response time, and estimated processing time).
 - PduAsyncResponse now includes "getWindowSize" which returns the size of the
    window after adding this response.  Used to estimate processing time.
 - Modified internals of SmppSession to call the "requestWindow" 
    a "sendWindow".  Since either endpoint of a session can technically
    generate requests, this actually represents the window just for sending.
 - Added windowWaitTimeout as a session configuration.
 - Improved demo client and server to show how various executors can be included
    on a constructor for either DefaultSmppClient or DefaultSmppServer to enable
    monitoring as a new feature.
 - Added new demo performance client to load test an SMPP server.
 - Large cleanup of javadocs and comments.
 - Added new feature to allow an SmppServer to stopped and started over again
    by moving some permanent resource cleanup code to new destroy() method.
    Please make a change to calling code to possibly change stop() calls to
    destroy() calls.

## 3.2 - 2011-04-28
 - Upgraded netty dependency from 3.1.5 to 3.2.4 (to keep it current, not to
    fix any specific problem)
 - Upgraded joda-time dependency from 1.6 to 1.6.2 (to keep it current)
 - Upgraded ch-commons-util dependency from 4.0 to 4.1
 - Fixed a significant performance issue with the SmppServer having a total
    session count greater than the number of processors on the server.  Main
    issue was with Netty's default "workerCount" value in its constructor.
    The maximum number of expected concurrent sessions is recommended to be set
    in the SmppServerConfiguration object.  The new default is 100 max sessions.
 - Added several other configuration options for an SmppServer:
       maxConnections - max number of concurrent connections/sessions (default 100)
       reuseAddress - whether to reuse the server socket (default is true)
       nonBlockingSocketsEnabled - whether to use NIO (non blocking) or OIO
            (old blocking) type server sockets. (default is true)
 - Added an example of a "SlowSmppServer" to test what happens if an SMSC is
    slow acknowledging submits.
 - Added support for passing up a SmppChannelException on blocking calls to
    bind, submit, etc. if the underlying channel is closed during a block.
    The implementation requires checking the RequestWindow if any callers are
    waiting and immediately cancelling those requests and setting the "cause" to
    a ClosedChannelException throwable.
 - Added a warning to be output to the logger if the number of SMPP server
    sessions exceeds the maxConnections set in the configuration object.

## 3.1 - 2011-03-14
 - Removed debug logging from DeliveryReceipt parser.

## 3.0 - 2011-03-14
 - Migrated build system from Ant to Maven
 - Deleted all legacy Ant build files (build.xml, Dependency.txt)
 - Added Apache license to top of all source files
 - Moved demo source code from src/demo/java into src/test/java directory and
    placed in separate "demo" package.
 - Added "Makefile" with targets that match previous demo ant tasks.
 - Added support for parsing delivery receipts with timestamps that contain
    seconds and a 4 digit year.
 - Added loose parsing of the text of a delivery receipt where the position of
    a particular field no longer matters.  Made missing field error checking
    optional.
 - Added DeliveryReceipt demo.

## 2.6 - 2011-02-14
 - Added support for parsing delivery receipts with timestamps that contain
    seconds.

## 2.5 - 2010-10-12
 - Allow sequence number of all 32-bits to be used. Previously, we validated that
    a sequence number was in the range of 0x00000000 to 0x7FFFFFFF. NOTE: Our
    outgoing implementation still only uses sequence numbers in the range of
    0x00000001 to 0x7FFFFFFF.

## 2.4 - 2010-08-23
 - Allow sequence number of zero to be used. Previously, we validated that
    a sequence number was in the range of 0x00000001 to 0x7FFFFFFF.
 - EXPERIMENTAL: Added ability to expire request PDUs that are not acknowledged.

## 2.3 - 2010-06-09
 - Refactored SmppSessionBootstrap to DefaultSmppClient
 - Added SmppServer and SmppServerSession: support for running an SMPP server
    and handling new incoming connections and creating the same smpp session
    object that a client bootstrap can create.
 - Added SmppUtil methods to work with short messages, esm class, and
    registered delivery flags.
 - Added "getInterfaceVersion" to a session that represents the actual protocol
    version in use between local and remote endpoints.  This is based on the
    negotiation of protocol versions during the bind process.  Works for both
    server and client side sessions.
 - Added processing of "sc_interface_version" optional parameter during client
    ESME binds for protocol negotiation.
 - Added "areOptionalParametersSupported" for a session to determine if
    optional parameters are supported.
 - Added utility method to convert interface byte values to human readable
    version strings such as 0x34 to "3.4"
 - Added DeliveryReceipt utility class for writing and reading delivery receipts
 - Added additional methods of converting TLVs into bytes, shorts, ints, and longs.
 - Fixed STDERR logging of exceptions during connects.

## 2.2 - 2010-03-14
 - Added TlvUtil class to create various types of TLVs.

## 2.1 - 2010-02-21
 - Added a PDU "referenceObject" property to be attached.

## 2.0 - 2010-02-19
 - Entirely refactored version

## 1.1 - 2009-11-03
 - Fixed bug with optional TLV parsing which assumed an optional TLV had to
    be 8 or more bytes in order to be correct.  The correct value is actually
    just 4 bytes or more (2 for header and 2 for length)

## 1.0 - 2009-11-01
 - Initial release - used today in all backend systems.
