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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.event.EventRouter;
import org.apache.eve.processor.DefaultHandlerRegistry;
import org.apache.eve.processor.DefaultRequestProcessor;
import org.apache.eve.processor.RequestProcessor;
import org.apache.eve.seda.DefaultStageConfig;
import org.apache.eve.seda.LoggingStageMonitor;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;
import org.apache.geronimo.pool.ThreadPool;


/**
 * @version $Revision: $ $Date: $
 */
public class EveRequestProcessorGBean implements RequestProcessor, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveRequestProcessorGBean.class);

    /**
     * the name
     */
    private String stageName;

    /**
     * the thread manager we get thread pools from
     */
    private ThreadPool threadPool = null;

    /**
     * the event eventRouter we depend on to recieve and publish events
     */
    private EventRouter eventRouter = null;

    /**
     * underlying wrapped RequestProcessor implementation
     */
    private DefaultRequestProcessor requestProcessor = null;

    
    public String getStageName() {
        return stageName;
    }

    public void setStageName(String stageName) {
        this.stageName = stageName;
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
        DefaultStageConfig stageConfig = new DefaultStageConfig(stageName,
                new org.apache.eve.thread.ThreadPool() {
                    public void execute(Runnable command) {
                        try {
                            threadPool.execute(command);
                        } catch (InterruptedException e) {
                            // DO NOTHING
                        }
                    }
                });

        DefaultHandlerRegistry hooks = new DefaultHandlerRegistry();
        requestProcessor = new DefaultRequestProcessor(eventRouter, stageConfig, hooks);
        requestProcessor.setMonitor(new LoggingStageMonitor());
        requestProcessor.start();
        log.info("Started " + stageName);
    }

    public void doStop() throws WaitingException, Exception {
        requestProcessor.stop();
        log.info("Stopped " + stageName);
    }

    public void doFail() {
        log.info("Failed " + stageName);
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveRequestProcessorGBean.class);

        infoFactory.addAttribute("stageName", String.class, true);
        infoFactory.addReference("ThreadPool", ThreadPool.class);
        infoFactory.addReference("EventRouter", EventRouter.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }
}
