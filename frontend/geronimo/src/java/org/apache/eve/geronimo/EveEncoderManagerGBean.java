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

import org.apache.commons.codec.EncoderException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.encoder.EncoderManager;
import org.apache.eve.encoder.DefaultEncoderManager;
import org.apache.eve.event.EventRouter;
import org.apache.eve.seda.DefaultStageConfig;
import org.apache.eve.decoder.DefaultDecoderManager;
import org.apache.eve.decoder.DecodeStageHandler;
import org.apache.ldap.common.message.Response;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.pool.ThreadPool;


/**
 * @version $Revision: $ $Date: $
 */
public class EveEncoderManagerGBean implements EncoderManager, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveEncoderManagerGBean.class);

    /**
     * the name of this Stage
     */
    private String encoderManagerName;

    /**
     * the thread pool used for this Stages workers
     */
    private ThreadPool threadPool;

    /**
     * the event eventRouter we depend on to recieve and publish events
     */
    private EventRouter eventRouter = null;

    /**
     * underlying wrapped EncoderManager implementation
     */
    private DefaultEncoderManager encoderManager = null;


    public String getEncoderManagerName() {
        return encoderManagerName;
    }

    public void setEncoderManagerName(String encoderManagerName) {
        this.encoderManagerName = encoderManagerName;
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
        DefaultStageConfig stageConfig = new DefaultStageConfig(encoderManagerName,
                new org.apache.eve.thread.ThreadPool() {
                    public void execute(Runnable command) {
                        try {
                            threadPool.execute(command);
                        } catch (InterruptedException e) {
                            // DO NOTHING
                        }
                    }
                });
        encoderManager = new DefaultEncoderManager(eventRouter, stageConfig);
        encoderManager.start();

        log.info("Started " + encoderManagerName);
    }

    public void doStop() throws WaitingException, Exception {
        encoderManager.stop();
        encoderManager = null;

        log.info("Stopped " + encoderManagerName);
    }

    public void doFail() {
        encoderManager = null;
        log.info("Failed " + encoderManagerName);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveEncoderManagerGBean.class);

        infoFactory.addAttribute("encoderManagerName", String.class, true);

        infoFactory.addReference("ThreadPool", ThreadPool.class);
        infoFactory.addReference("EventRouter", EventRouter.class);

        infoFactory.addInterface(EncoderManager.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public byte[] encode(Response response) throws EncoderException {
        return encoderManager.encode(response);
    }
}
