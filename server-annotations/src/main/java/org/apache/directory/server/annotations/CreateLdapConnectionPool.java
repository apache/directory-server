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

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionFactory;
import org.apache.directory.ldap.client.api.DefaultLdapConnectionValidator;
import org.apache.directory.ldap.client.api.DefaultPoolableLdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionFactory;
import org.apache.directory.ldap.client.api.LdapConnectionValidator;


/**
 * A annotation used to define a LdapConnection configuration. 
 * Many elements can be configured :
 * 
 * <ul>
 * <li> The connection timeout</li>
 * <li> A list of attributes to be interpreted as binary (in addition to the defaults)</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(
    { ElementType.METHOD, ElementType.TYPE })
public @interface CreateLdapConnectionPool
{
    /** Attributes names to be added to the list of default binary attributes */
    String[] additionalBinaryAttributes() default {};
    
    
    /** LdapConnection factory implementation class */
    Class<? extends LdapConnectionFactory> connectionFactoryClass() default 
            DefaultLdapConnectionFactory.class;
    
    
    /** LdapConnection pool factory implementation class */
    Class<? extends PoolableObjectFactory<LdapConnection>> factoryClass() default 
            DefaultPoolableLdapConnectionFactory.class;
    
    
    /** Connections borrowed in LIFO order, default true */
    boolean lifo() default true;
    
    
    /** The maximum number of active connections, default 8 */
    int maxActive() default 8;
    
    
    /** The maximum number of idle connections, default 8 */
    int maxIdle() default 8;
    
    
    /** The maximum amount of time to wait for a connection to be returned in millis, default -1 */
    long maxWait() default -1L;
    
    
    /** The minimum idle time before evicting a connection in millis, default 1000*60*30 */
    long minEvictableIdleTimeMillis() default 1000L * 60L * 30L;
    
    
    /** The minumum number of idle instances before evictor spawns new object, default 0 */
    int minIdle() default 0;
    
    
    /** The number of objects to test per eviction run, default 3 */
    int numTestsPerEvictionRun() default 3;

    
    /** Same as minEvictableIdleTimeMillis with extra condition that minIdle objects remain in pool, default -1 */
    long softMinEvictableIdleTimeMillis() default -1L;
    
    
    /** If true, connection will be tested on borrow, default false */
    boolean testOnBorrow() default false;
    
    
    /** If true, connection will be tested on return, default false */
    boolean testOnReturn() default false;
    
    
    /** If true, connection will be tested on while idle, default false */
    boolean testWhileIdle() default false;
    
    
    /** The time, in millis, between eviction runs, default -1 (forever) */
    long timeBetweenEvictionRunsMillis() default -1L;
    
    
    /** The connection timeout in millis, default 30000 */
    long timeout() default 30000L;
    
    
    /** The class to use for validation */
    Class<? extends LdapConnectionValidator> validatorClass() default 
        DefaultLdapConnectionValidator.class;


    /** The default action when connections are exhausted, default 1 (block) */
    byte whenExhaustedAction() default 1;
}