/**
 *
 * Copyright 2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.eve.geronimo;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.ResourceException;
import org.apache.eve.buffer.BufferPool;
import org.apache.eve.buffer.BufferPoolConfig;
import org.apache.eve.buffer.DefaultBufferPool;
import org.apache.eve.buffer.DefaultBufferPoolConfig;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;


/**
 * A Geronimo Buffer Pool service.
 *
 * @version $Revision: $ $Date: $
 */
public class EveBufferPoolGBean implements BufferPool, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveBufferPoolGBean.class);

    /**
     * the name
     */
    private String bufferName;

    /**
     * the growth increment
     */
    private int increment = 0;

    /**
     * the maximum pool size
     */
    private int maxSize = 0;

    /**
     * the initial pool size
     */
    private int initialSize = 0;

    /**
     * the size of the buffers pooled
     */
    private int size = 0;

    /**
     * the buffer pool
     */
    private DefaultBufferPool bufferPool = null;


    public String getBufferName() {
        return bufferName;
    }

    public void setBufferName(String bufferName) {
        this.bufferName = bufferName;
    }

    public int getIncrement() {
        return increment;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void doStart() throws WaitingException, Exception {
        BufferPoolConfig config = new DefaultBufferPoolConfig(bufferName, increment, maxSize, initialSize, size);
        bufferPool = new DefaultBufferPool(config);
        log.info("Started " + bufferName);
    }

    public void doStop() throws WaitingException, Exception {
        bufferPool = null;
        log.info("Stopped " + bufferName);
    }

    public void doFail() {
        bufferPool = null;
        log.info("Failed " + bufferName);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveBufferPoolGBean.class);

        infoFactory.addAttribute("bufferName", String.class, true);
        infoFactory.addAttribute("increment", int.class, true);
        infoFactory.addAttribute("maxSize", int.class, true);
        infoFactory.addAttribute("initialSize", int.class, true);
        infoFactory.addAttribute("size", int.class, true);

        infoFactory.addInterface(BufferPool.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public ByteBuffer getBuffer(Object party) throws ResourceException {
        return bufferPool.getBuffer(party);
    }

    public void claimInterest(ByteBuffer buffer, Object party) {
        bufferPool.claimInterest(buffer, party);
    }

    public void releaseClaim(ByteBuffer buffer, Object party) {
        bufferPool.releaseClaim(buffer, party);
    }

    public BufferPoolConfig getConfig() {
        return bufferPool.getConfig();
    }

    public int getInterestedCount(ByteBuffer buffer) {
        return bufferPool.getInterestedCount(buffer);
    }

    public int getFreeCount() {
        return bufferPool.getFreeCount();
    }

    public int getInUseCount() {
        return bufferPool.getInUseCount();
    }

    public int size() {
        return bufferPool.size();
    }

    public String getName() {
        return bufferPool.getName();
    }
}
