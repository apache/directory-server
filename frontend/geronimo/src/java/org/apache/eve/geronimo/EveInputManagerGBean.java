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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.ResourceException;
import org.apache.eve.buffer.BufferPool;
import org.apache.eve.event.EventRouter;
import org.apache.eve.input.DefaultInputManager;
import org.apache.eve.input.InputManager;
import org.apache.eve.input.InputManagerMonitor;
import org.apache.eve.listener.ClientKey;
import org.apache.eve.listener.KeyExpiryException;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.pool.ThreadPool;


/**
 * @version $Revision: $ $Date: $
 */
public class EveInputManagerGBean implements InputManager, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveInputManagerGBean.class);

    /**
     * the thread manager we get thread pools from
     */
    private ThreadPool threadPool = null;

    /**
     * the buffer pool to get buffers from
     */
    private BufferPool bufferPool = null;

    /**
     * event router used to decouple source to sink relationships
     */
    private EventRouter eventRouter = null;

    /**
     * selector used to select a ready socket channel
     */
    private Selector selector = null;

    /**
     * the wrapped input manager implementation
     */
    private DefaultInputManager inputManager = null;

    
    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public BufferPool getBufferPool() {
        return bufferPool;
    }

    public void setBufferPool(BufferPool bufferPool) {
        this.bufferPool = bufferPool;
    }

    public EventRouter getEventRouter() {
        return eventRouter;
    }

    public void setEventRouter(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    public Selector getSelector() {
        return selector;
    }

    public void setSelector(Selector selector) {
        this.selector = selector;
    }

    public void doStart() throws WaitingException, Exception {
        inputManager = new DefaultInputManager(eventRouter, bufferPool);
        inputManager.setMonitor(new Monitor());
        log.info("Started");
    }

    public void doStop() throws WaitingException, Exception {
        inputManager.stop();
        log.info("Stopped");
    }

    public void doFail() {
        log.info("Failed");
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveInputManagerGBean.class);

        infoFactory.addReference("ThreadPool", ThreadPool.class);
        infoFactory.addReference("BufferPool", BufferPool.class);
        infoFactory.addReference("EventRouter", EventRouter.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    private class Monitor implements InputManagerMonitor {

        public void bufferUnavailable(BufferPool bp, ResourceException fault) {
            if (log.isErrorEnabled()) {
                log.error("Failed to acquire buffer resource from buffer pool " + bp, fault);
            }
        }

        public void channelCloseFailure(SocketChannel channel, IOException fault) {
            if (log.isErrorEnabled()) {
                log.error("Could not properly close socket channel " + channel, fault);
            }
        }

        public void channelRegistrationFailure(Selector selector, SocketChannel channel, int key, IOException fault) {
            if (log.isErrorEnabled()) {
                log.error("Could not register socket channel " + channel + " for selector " + selector + " using selection key mode " + key, fault);
            }
        }

        public void disconnectedClient(ClientKey key) {
            if (log.isInfoEnabled()) {
                log.info("Disconnected client with key: " + key);
            }
        }

        public void enteringSelect(Selector selector) {
            if (log.isDebugEnabled()) {
                log.debug("About to enter select() on selector " + selector);
            }
        }

        public void inputRecieved(ClientKey key) {
            if (log.isDebugEnabled()) {
                log.debug("Got some input from " + key);
            }
        }

        public void keyExpiryFailure(ClientKey key, KeyExpiryException fault) {
            if (log.isInfoEnabled()) {
                log.info("While working with client key " + key + " it was prematurely expired!", fault);
            }
        }

        public void readFailed(ClientKey key, IOException fault) {
            if (log.isErrorEnabled()) {
                log.error("Encountered failure while reading from " + key, fault);
            }
        }

        public void registeredChannel(ClientKey key, Selector selector) {
            if (log.isDebugEnabled()) {
                log.debug("Succeeded in registering " + key + " with selector " + selector);
            }
        }

        public void selectFailure(Selector selector, IOException fault) {
            if (log.isErrorEnabled()) {
                log.error("Failed on select() of selector " + selector, fault);
            }
        }

        public void selectorReturned(Selector selector) {
            if (log.isDebugEnabled()) {
                log.debug("Select on " + selector + " returned");
            }
        }

        public void selectTimedOut(Selector selector) {
            if (log.isWarnEnabled()) {
                log.warn("Select on " + selector + " timed out");
            }
        }

        public void inputRecieved(ByteBuffer buffer, ClientKey key) {
            if (log.isDebugEnabled()) {
                log.debug("Recieved input [" + toHexString(buffer) + "] from client " + key);
            }
        }

        public void cleanedStaleKey(SelectionKey key) {
            if (log.isWarnEnabled()) {
                log.warn("Cleaning up stale connection key for client: " + key.attachment());
            }
        }

        public String toHexString(ByteBuffer buf) {
            byte[] l_bites = new byte[buf.remaining()];
            buf.get(l_bites);
            return new String(l_bites);
        }

    }
}
