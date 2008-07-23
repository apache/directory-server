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
package org.apache.directory.server.core.partition;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.apache.directory.server.core.authn.AuthenticationInterceptor;
import org.apache.directory.server.core.authz.AciAuthorizationInterceptor;
import org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor;
import org.apache.directory.server.core.changelog.ChangeLogInterceptor;
import org.apache.directory.server.core.collective.CollectiveAttributeInterceptor;
import org.apache.directory.server.core.event.EventInterceptor;
import org.apache.directory.server.core.exception.ExceptionInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.normalization.NormalizationInterceptor;
import org.apache.directory.server.core.operational.OperationalAttributeInterceptor;
import org.apache.directory.server.core.schema.SchemaInterceptor;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.core.trigger.TriggerInterceptor;


/**
 * TODO ByPassConstants.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ByPassConstants
{
    /**
     * safe to use set of bypass instructions to lookup raw entries
     */
    public final static Collection<String> LOOKUP_BYPASS;

    /**
     * safe to use set of bypass instructions to lookup raw entries while
     * also avoiding hit on collective attributes {@link Interceptor}: used 
     * by collective attributes interceptor.
     */
    public static final Collection<String> LOOKUP_COLLECTIVE_BYPASS;

    /**
     * bypass instructions used by ExceptionInterceptor
     */
    public final static Collection<String> HAS_ENTRY_BYPASS;

    /**
     * safe to use set of bypass instructions to getMatchedDn
     */
    public static final Collection<String> GETMATCHEDDN_BYPASS;

    /**
     * safe to use set of bypass instructions to lookup raw entries excluding operational attributes
     */
    public static final Collection<String> LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS;
    
    public static final Collection<String> GET_ROOT_DSE_BYPASS;

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
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        LOOKUP_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        HAS_ENTRY_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        LOOKUP_COLLECTIVE_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        GETMATCHEDDN_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( AuthenticationInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS = Collections.unmodifiableCollection( c );
        
        c = new HashSet<String>();
        c.add( NormalizationInterceptor.class.getName() );
        c.add( ChangeLogInterceptor.class.getName() );
        c.add( AciAuthorizationInterceptor.class.getName() );
        c.add( DefaultAuthorizationInterceptor.class.getName() );
        c.add( ExceptionInterceptor.class.getName() );
        c.add( OperationalAttributeInterceptor.class.getName() );
        c.add( SchemaInterceptor.class.getName() );
        c.add( SubentryInterceptor.class.getName() );
        c.add( CollectiveAttributeInterceptor.class.getName() );
        c.add( EventInterceptor.class.getName() );
        c.add( TriggerInterceptor.class.getName() );
        GET_ROOT_DSE_BYPASS = Collections.unmodifiableCollection( c );
    }
}
