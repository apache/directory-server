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
import java.util.EventObject;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.stateful.DecoderCallback;
import org.apache.commons.codec.stateful.DecoderMonitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.decoder.DecodeStageHandler;
import org.apache.eve.decoder.DecoderManager;
import org.apache.eve.decoder.DecoderManagerMonitor;
import org.apache.eve.decoder.DefaultDecoderManager;
import org.apache.eve.event.EventRouter;
import org.apache.eve.event.Subscriber;
import org.apache.eve.listener.ClientKey;
import org.apache.eve.seda.DefaultStageConfig;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.pool.ThreadPool;


/**
 * A Geronimo Decoder Manager service.
 *
 * @version $Revision: $ $Date: $
 */
public class EveDecoderManagerGBean implements DecoderManager, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveDecoderManagerGBean.class);

    /**
     * the name of this Stage
     */
    private String decoderManagerName;

    /**
     * the thread pool used for this Stages workers
     */
    private ThreadPool threadPool;

    /**
     * the event eventRouter we depend on to recieve and publish events
     */
    private EventRouter eventRouter = null;

    /**
     * underlying wrapped DecoderManager implementation
     */
    private DefaultDecoderManager decoderManager = null;


    public String getDecoderManagerName() {
        return decoderManagerName;
    }

    public void setDecoderManagerName(String decoderManagerName) {
        this.decoderManagerName = decoderManagerName;
    }

    public ThreadPool getThreadPool() {
        return threadPool;
    }

    public void setThreadPool(ThreadPool threadPool) {
        this.threadPool = threadPool;
    }

    public EventRouter getEventRouter() {
        return eventRouter;
    }

    public void setEventRouter(EventRouter eventRouter) {
        this.eventRouter = eventRouter;
    }

    public void doStart() throws WaitingException, Exception {
        DefaultStageConfig stageConfig = new DefaultStageConfig(decoderManagerName,
                new org.apache.eve.thread.ThreadPool() {
                    public void execute(Runnable command) {
                        try {
                            threadPool.execute(command);
                        } catch (InterruptedException e) {
                            // DO NOTHING
                        }
                    }
                });
        decoderManager = new DefaultDecoderManager(eventRouter, stageConfig);
        DecodeStageHandler handler = new DecodeStageHandler(decoderManager);
        stageConfig.setHandler(handler);
        decoderManager.setMonitor(new Monitor());
        decoderManager.start();

        log.info("Started " + decoderManagerName);
    }

    public void doStop() throws WaitingException, Exception {
        decoderManager.stop();
        decoderManager = null;

        log.info("Stopped " + decoderManagerName);
    }

    public void doFail() {
        decoderManager = null;
        log.info("Failed " + decoderManagerName);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveDecoderManagerGBean.class);

        infoFactory.addAttribute("decoderManagerName", String.class, true);

        infoFactory.addReference("ThreadPool", ThreadPool.class);
        infoFactory.addReference("EventRouter", EventRouter.class);

        infoFactory.addInterface(DecoderManager.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public void setCallback(ClientKey key, DecoderCallback cb) {
        decoderManager.setCallback(key, cb);
    }

    public void setDecoderMonitor(ClientKey key, DecoderMonitor monitor) {
        decoderManager.setDecoderMonitor(key, monitor);
    }

    public boolean disable(ClientKey key) {
        return decoderManager.disable(key);
    }

    public void decode(ClientKey key, ByteBuffer buffer) throws DecoderException {
        decoderManager.decode(key, buffer);
    }

    public Object decode(ByteBuffer buffer) throws DecoderException {
        return decoderManager.decode(buffer);
    }

    /**
     * StageMonitor that uses this module's logger.
     */
    class Monitor implements DecoderManagerMonitor {

        public void failedOnInform(Subscriber subscriber, EventObject event, Throwable t) {
            log.debug("Failed to invoke the appropriate inform method"
                    + " for event " + event + " on subscriber " + subscriber
                    + ":\n" + t);
        }
    }
}
