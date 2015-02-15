Cloudhopper SMPP by Twitter [![Build Status](https://secure.travis-ci.org/twitter/cloudhopper-smpp.png?branch=master)](http://travis-ci.org/twitter/cloudhopper-smpp)
============================

News
------------------------
 * Netty 4 beta is available at Maven Central. Please test and submit issues this month (1/2015).

        <dependency>
          <groupId>com.cloudhopper</groupId>
          <artifactId>ch-smpp</artifactId>
          <version>6.0.0-netty4-beta-2</version>
        </dependency>

Overview
------------------------

Efficient, scalable, rock-solid, and flexible Java implementation of the Short
Messaging Peer to Peer Protocol (SMPP).

 * Real-world used library by Twitter across nearly every SMSC vendor and
   mobile operator around the world.  We've seen almost every variance in the
   SMPP protocol and this library handles it flawlessly.
 * Rigorous unit testing
 * Support for SMPP protocol:
    * Version 3.3
    * Version 3.4
    * Most of version 5.0
 * Uses non-blocking (NIO) sockets (via underlying Netty dependency, one thread
   can support 1 or more SMPP sessions)
 * Can support thousands of binds/connections using minimal resources and threads
 * Supports both client and server modes of the SMPP protocol (yes you can
   write your own SMPP server using this library as well as be a client to one)
 * Supports synchronous request mode (send request and block until response
   received)
 * Supports asynchronous request mode (send request, get a future response,
   and then decide when you'd like to wait/get a response)
 * Advanced support for SMPP "windowing":
    * Configurable window size per session
    * Waiting for a window slot to open up
    * Get a list of unacknowledged/in-flight PDUs if session disconnects
 * SSL/TLS support for clients and servers
 * Configurable support for expiry of unacknowledged PDUs
 * Configurable counter metrics per client-session, server-session, or server.
 * Support for sniffing/logging/discarding of PDUs before normal processing

The library has been tested and certified with hundreds of mobile operators
and suppliers around the world.  It's effective at being flexible with SMPP
specifications that aren't truly compliant.


Background and Contributors
---------------------------

This library was originally developed by Cloudhopper, Inc. in 2008. Cloudhopper
was acquired by Twitter in April 2010. The main author of this library,
Joe Lauer, left Twitter in April 2013. While folks at Twitter still contribute
and maintain this library, Joe is now with [Mfizz, Inc](http://mfizz.com).
Mfizz actively sponsors this project in conjunction with Twitter. If you have
any commercial questions/ideas pertaining to this library, feel free to reach
out to [Mfizz](http://mfizz.com).

- Joe Lauer (Twitter: [@jjlauer](http://twitter.com/jjlauer))
- Garth (Twitter: [@trg](http://twitter.com/trg))

Installation
------------

Library versions >= 5.0.0 are now published to the Maven Central Repository.
Just add the following dependency to your project maven pom.xml:

    <dependency>
      <groupId>com.cloudhopper</groupId>
      <artifactId>ch-smpp</artifactId>
      <version>[5.0.0,)</version>
    </dependency>

Demo Code / Tutorials
---------------------

There are numerous examples of how to use various parts of this library:

    src/test/java/com/cloudhopper/smpp/demo/

To run some of the samples, there is a Makefile to simplify the syntax required
by Maven:

    make client
    make server
    make performance-client
    make simulator
    make rebind
    make parser
    make dlr
    make ssl-client
    make ssl-server
    make persist-client

On Windows, the examples can run with `nmake` instead of `make`.

The easiest way to get started is to try out our `server` and `client` or `ssl-server`
and `ssl-client` examples. Open up two shells.  In the first shell, run:

    make server

In the second shell, run:

    make client

You'll see the client bind to the server and a few different type of requests
exchanged back and forth.

You can also try `make persist-client` instead of `make client` which demonstrates a persistent SMPP connection.

User Contributed Demos
----------------------

A more complete persistent client demo:

(https://github.com/krasa/cloudhopper-smpp/tree/persistent-connection/src/test/java/com/cloudhopper/smpp/demo/persist)

A tutorial in Russian:

https://github.com/wizardjedi/my-spring-learning/wiki/Twitter-cloudhopper

Please let us know if you have other tutorials worth mentioning!


License
-------

Copyright (C) 2009-2014 Twitter, Inc.

This work is licensed under the Apache License, Version 2.0. See LICENSE for details.
