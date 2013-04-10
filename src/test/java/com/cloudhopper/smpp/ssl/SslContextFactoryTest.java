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

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.UnrecoverableKeyException;
import org.junit.Assert;
import org.junit.Test;

public class SslContextFactoryTest {

    @Test
    public void testNoTsFileKs() throws Exception {
	SslConfiguration cf = new SslConfiguration();
        cf.setKeyStorePath("src/test/resources/keystore");
        cf.setKeyStorePassword("changeit");
        cf.setKeyManagerPassword("changeit");
	SslContextFactory factory = new SslContextFactory(cf);
        Assert.assertTrue(factory.getSslContext() != null);
    }
    
    @Test
    public void testNoTsNoKs() throws Exception {
	SslConfiguration cf = new SslConfiguration();
	SslContextFactory factory = new SslContextFactory(cf);
        Assert.assertTrue(factory.getSslContext() != null);
    }
    
    @Test
    public void testFileTsFileKs() throws Exception {
	SslConfiguration cf = new SslConfiguration();
        cf.setKeyStorePath("src/test/resources/keystore");
        cf.setKeyStorePassword("changeit");
        cf.setKeyManagerPassword("changeit");
        cf.setTrustStorePath("src/test/resources/keystore");
        cf.setTrustStorePassword("changeit");
	SslContextFactory factory = new SslContextFactory(cf);
        Assert.assertTrue(factory.getSslContext() != null);
    }

    @Test
    public void testFileTsFileKsWrongPW() throws Exception {
	SslConfiguration cf = new SslConfiguration();
        cf.setKeyStorePath("src/test/resources/keystore");
        cf.setKeyStorePassword("bad_password");
        cf.setKeyManagerPassword("changeit");
        cf.setTrustStorePath("src/test/resources/keystore");
        cf.setTrustStorePassword("changeit");
        try {
	    SslContextFactory factory = new SslContextFactory(cf);
            Assert.fail();
        } catch(IOException e) {
        }
    }

    @Test
    public void testPathTsWrongPWPathKs() throws Exception {
	SslConfiguration cf = new SslConfiguration();
        cf.setKeyStorePath("src/test/resources/keystore");
        cf.setKeyStorePassword("changeit");
        cf.setKeyManagerPassword("changeit");
        cf.setTrustStorePath("src/test/resources/keystore");
        cf.setTrustStorePassword("bad_password");
        try {
	    SslContextFactory factory = new SslContextFactory(cf);
            Assert.fail();
        } catch(IOException e) {
        }
    }
    
    @Test
    public void testNoKeyConfig() throws Exception {
        try {
	    SslConfiguration cf = new SslConfiguration();
            cf.setTrustStorePath("src/test/resources/keystore");
	    SslContextFactory factory = new SslContextFactory(cf);
            Assert.fail();
        } catch (IllegalStateException e) {
	} catch (Exception e) {
            Assert.fail("Unexpected exception");
        }
    }

}
