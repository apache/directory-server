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

import java.util.EventObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.eve.event.DefaultEventRouter;
import org.apache.eve.event.EventRouter;
import org.apache.eve.event.EventRouterMonitor;
import org.apache.eve.event.Filter;
import org.apache.eve.event.Subscriber;
import org.apache.eve.event.Subscription;

import org.apache.geronimo.gbean.GBeanInfo;
import org.apache.geronimo.gbean.GBeanInfoFactory;
import org.apache.geronimo.gbean.GBeanLifecycle;
import org.apache.geronimo.gbean.WaitingException;


/**
 * @version $Revision: $ $Date: $
 */
public class EveEventRouterGBean implements EventRouter, GBeanLifecycle {

    private final Log log = LogFactory.getLog(EveEventRouterGBean.class);

    /**
     * the default EventRouter implementation we wrap
     */
    private DefaultEventRouter eventRouter;

    public void doStart() throws WaitingException, Exception {
        eventRouter = new DefaultEventRouter();
        eventRouter.setMonitor(new Monitor());
        log.info("Started");
    }

    public void doStop() throws WaitingException, Exception {
        log.info("Stopped");
    }

    public void doFail() {
        log.info("Failed");
    }

    public static final GBeanInfo GBEAN_INFO;

    static {
        GBeanInfoFactory infoFactory = new GBeanInfoFactory(EveEventRouterGBean.class);

        infoFactory.addInterface(EventRouter.class);

        GBEAN_INFO = infoFactory.getBeanInfo();
    }

    public static GBeanInfo getGBeanInfo() {
        return GBEAN_INFO;
    }

    public void subscribe(Class type, Filter filter, Subscriber subscriber) {
        eventRouter.subscribe(type, filter, subscriber);
    }

    public void subscribe(Class type, Subscriber subscriber) {
        eventRouter.subscribe(type, subscriber);
    }

    public void unsubscribe(Subscriber subscriber) {
        eventRouter.unsubscribe(subscriber);
    }

    public void unsubscribe(Class type, Subscriber subscriber) {
        eventRouter.unsubscribe(type, subscriber);
    }

    public void publish(EventObject event) {
        eventRouter.publish(event);
    }

    /**
     * EventRouterMonitor that uses this module's logger.
     */
    class Monitor implements EventRouterMonitor {

        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#eventPublished(
         * java.util.EventObject)
         */
        public void eventPublished(EventObject event) {
            log.debug("published event: " + event);
        }


        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#addedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void addedSubscription(Subscription subscription) {
            log.debug("added subscription: " + subscription);
        }


        /* (non-Javadoc)
         * @see org.apache.eve.event.EventRouterMonitor#removedSubscription(
         * org.apache.eve.event.Subscription)
         */
        public void removedSubscription(Subscription subscription) {
            log.debug("removed subscription: " + subscription);
        }
    }
}
