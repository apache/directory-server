/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.directory.server.annotations;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * A annotation used to specify an sequence of LDIF's to be applied to
 * the instance for integration testing.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateTransport
{
    /** @return The name for this protocol*/
    String protocol();


    /** @return The transport type (TCP or UDP) Default to TCP */
    TransportType type() default TransportType.TCP;


    /** @return The port to use, default to a bad value so that we know 
     * we have to pick one random available port */
    int port() default -1;


    /** @return The InetAddress for this transport. */
    String address() default "";


    /** @return The backlog. Default to 50 */
    int backlog() default 50;


    /** @return A flag to tell if the transport is SSL based. Default to false */
    boolean ssl() default false;


    /** @return The number of threads to use. Default to 3*/
    int nbThreads() default 3;

    /** @return A flag to tell if the transport should ask for client certificate. Default to false */
    boolean clientAuth() default false;
}