/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */
package org.apache.directory.server.core.api.partition;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.directory.server.core.api.interceptor.Interceptor;


/**
 * Constants used to determine what kinds of {@link Interceptor}s need to be 
 * bypassed while performing operations within other Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ByPassConstants
{
    /**
     * safe to use set of bypass instructions to lookup raw entries while
     * also avoiding hit on collective attributes {@link Interceptor}: used 
     * by collective attributes interceptor.
     */
    public static final Collection<String> LOOKUP_COLLECTIVE_BYPASS;

    /**
     * Bypass String to use when ALL interceptors should be skipped
     */
    public static final String BYPASS_ALL = "*";

    /**
     * Bypass String to use when ALL interceptors should be skipped
     */
    public static final Collection<String> BYPASS_ALL_COLLECTION = Collections.singleton( BYPASS_ALL );

    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( "NormalizationInterceptor" );
        c.add( "AuthenticationInterceptor" );
        c.add( "AciAuthorizationInterceptor" );
        c.add( "DefaultAuthorizationInterceptor" );
        c.add( "CollectiveAttributeInterceptor" );
        c.add( "OperationalAttributeInterceptor" );
        c.add( "SchemaInterceptor" );
        c.add( "SubentryInterceptor" );
        c.add( "EventInterceptor" );
        c.add( "JournalInterceptor" );
        LOOKUP_COLLECTIVE_BYPASS = Collections.unmodifiableCollection( c );
    }
}
