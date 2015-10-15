package com.cloudhopper.smpp.channel;

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

/**
 * Constants used for SMPP channels and pipelines.
 * 
 * @author joelauer (twitter: @jjlauer or <a href="http://twitter.com/jjlauer" target=window>http://twitter.com/jjlauer</a>)
 */
public class SmppChannelConstants {

    public static final String PIPELINE_SERVER_CONNECTOR_NAME = "smppServerConnector";

    // default channel handler used only during connects
    public static final String PIPELINE_CLIENT_CONNECTOR_NAME = "smppClientConnector";

    // channel handlers used for once a session is connected
    public static final String PIPELINE_SESSION_THREAD_RENAMER_NAME = "smppSessionThreadRenamer";
    public static final String PIPELINE_SESSION_LOGGER_NAME = "smppSessionLogger";
    public static final String PIPELINE_SESSION_PDU_DECODER_NAME = "smppSessionPduDecoder";
    public static final String PIPELINE_SESSION_WRAPPER_NAME = "smppSessionWrapper";
    public static final String PIPELINE_SESSION_SSL_NAME = "smppSessionSSL"; 
    public static final String PIPELINE_SESSION_WRITE_TIMEOUT_NAME = "smppSessionWriteTimeout";

}
