/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ.state;

import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.integ.InheritableSettings;
import org.junit.internal.runners.TestClass;
import org.junit.internal.runners.TestMethod;
import org.junit.runner.notification.RunNotifier;

import javax.naming.NamingException;
import java.io.IOException;


/**
 * The interface representing a state in the lifecycle of a service
 * during integration testing.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public interface TestServiceState
{
    /**
     * Action where an attempt is made to create the service.  Service
     * creation in this system is the combined instantiation and
     * configuration which takes place when the factory is used to get
     * a new instance of the service.
     *
     * @param factory the factory to use for creating a configured service
     */
    void create( DirectoryServiceFactory factory );


    /**
     * Action where an attempt is made to destroy the service.  This
     * entails nulling out reference to it and triggering garbage
     * collection.
     */
    void destroy();


    /**
     * Action where an attempt is made to erase the contents of the
     * working directory used by the service for various files including
     * partition database files.
     *
     * @throws IOException on errors while deleting the working directory
     */
    void cleanup() throws IOException;


    /**
     * Action where an attempt is made to start up the service.
     *
     * @throws NamingException on failures to start the core directory service
     */
    void startup() throws NamingException;


    /**
     * Action where an attempt is made to shutdown the service.
     *
     * @throws NamingException on failures to stop the core directory service
     */
    void shutdown() throws NamingException;


    /**
     * Action where an attempt is made to run a test against the service.
     *
     * All annotations should have already been processed for
     * InheritableSettings yet they and others can be processed since we have
     * access to the method annotations below
     *
     * @param testClass the class whose test method is to be run
     * @param testMethod the test method which is to be run
     * @param notifier a notifier to report failures to
     * @param settings the inherited settings and annotations associated with
     * the test method
     */
    void test( TestClass testClass, TestMethod testMethod, RunNotifier notifier, InheritableSettings settings );


    /**
     * Action where an attempt is made to revert the service to it's
     * initial start up state by using a previous snapshot.
     *
     * @throws NamingException on failures to revert the state of the core
     * directory service
     */
    void revert() throws NamingException;
}
