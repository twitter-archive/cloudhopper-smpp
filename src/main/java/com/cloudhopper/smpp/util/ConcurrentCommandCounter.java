package com.cloudhopper.smpp.util;

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

import com.cloudhopper.commons.util.DecimalUtil;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author joelauer
 */
public class ConcurrentCommandCounter {
    
    private AtomicInteger request;
    private AtomicInteger requestExpired;
    private AtomicLong requestWaitTime;
    private AtomicLong requestResponseTime;
    private AtomicLong requestEstimatedProcessingTime;
    private AtomicInteger response;
    private ConcurrentCommandStatusCounter responseCommandStatusCounter;
    
    public ConcurrentCommandCounter() {
        this.request = new AtomicInteger(0);
        this.requestExpired = new AtomicInteger(0);
        this.requestWaitTime = new AtomicLong(0);
        this.requestResponseTime = new AtomicLong(0);
        this.requestEstimatedProcessingTime = new AtomicLong(0);
        this.response = new AtomicInteger(0);
        this.responseCommandStatusCounter = new ConcurrentCommandStatusCounter();
    }

    public ConcurrentCommandCounter(int request, int requestExpired, long requestWaitTime, long requestResponseTime, long requestEstimatedProcessingTime, int response, final ConcurrentCommandStatusCounter responseCommandStatusCounter) {
        this.request = new AtomicInteger(request);
        this.requestExpired = new AtomicInteger(requestExpired);
        this.requestWaitTime = new AtomicLong(requestWaitTime);
        this.requestResponseTime = new AtomicLong(requestResponseTime);
        this.requestEstimatedProcessingTime = new AtomicLong(requestEstimatedProcessingTime);
        this.response = new AtomicInteger(response);
        this.responseCommandStatusCounter = responseCommandStatusCounter.copy();
    }
    
    public void reset() {
        this.request.set(0);
        this.requestExpired.set(0);
        this.requestWaitTime.set(0);
        this.requestResponseTime.set(0);
        this.requestEstimatedProcessingTime.set(0);
        this.response.set(0);
        this.responseCommandStatusCounter.reset();
    }
    
    public ConcurrentCommandCounter createSnapshot() {
        return new ConcurrentCommandCounter(request.get(), requestExpired.get(), requestWaitTime.get(), requestResponseTime.get(), requestEstimatedProcessingTime.get(), response.get(), responseCommandStatusCounter);
    }

    public int getRequest() {
        return this.request.get();
    }
    
    public int incrementRequestAndGet() {
        return this.request.incrementAndGet();
    }

    public int getRequestExpired() {
        return this.requestExpired.get();
    }
    
    public int incrementRequestExpiredAndGet() {
        return this.requestExpired.incrementAndGet();
    }

    public long getRequestWaitTime() {
        return this.requestWaitTime.get();
    }
    
    public long addRequestWaitTimeAndGet(long waitTime) {
        return this.requestWaitTime.addAndGet(waitTime);
    }

    public long getRequestResponseTime() {
        return this.requestResponseTime.get();
    }
    
    public long addRequestResponseTimeAndGet(long responseTime) {
        return this.requestResponseTime.addAndGet(responseTime);
    }
    
    public long getRequestEstimatedProcessingTime() {
        return this.requestEstimatedProcessingTime.get();
    }
    
    public long addRequestEstimatedProcessingTimeAndGet(long estimatedProcessingTime) {
        return this.requestEstimatedProcessingTime.addAndGet(estimatedProcessingTime);
    }

    public int getResponse() {
        return this.response.get();
    }
    
    public int incrementResponseAndGet() {
        return this.response.incrementAndGet();
    }

    public ConcurrentCommandStatusCounter getResponseCommandStatusCounter() {
        return this.responseCommandStatusCounter;
    }

    @Override
    public String toString() {
        StringBuilder to = new StringBuilder();
        to.append("[request=");
        to.append(getRequest());
        to.append(" expired=");
        to.append(getRequestExpired());
        to.append(" response=");
        to.append(getResponse());
        
        to.append(" avgWaitTime=");
        double avgWaitTime = 0;
        if (getResponse() > 0) {
            avgWaitTime = (double)getRequestWaitTime()/(double)getResponse();
        }
        to.append(DecimalUtil.toString(avgWaitTime, 1));
        
        to.append("ms avgResponseTime=");
        double avgResponseTime = 0;
        if (getResponse() > 0) {
            avgResponseTime = (double)getRequestResponseTime()/(double)getResponse();
        }
        to.append(DecimalUtil.toString(avgResponseTime, 1));
        
        to.append("ms avgEstimatedProcessingTime=");
        double avgEstimatedProcessingTime = 0;
        if (getResponse() > 0) {
            avgEstimatedProcessingTime = (double)getRequestEstimatedProcessingTime()/(double)getResponse();
        }
        to.append(DecimalUtil.toString(avgEstimatedProcessingTime, 1));
        
        to.append("ms cmdStatus=[");
        to.append(this.responseCommandStatusCounter.toString());
        to.append("]]");
        return to.toString();
    }
}
