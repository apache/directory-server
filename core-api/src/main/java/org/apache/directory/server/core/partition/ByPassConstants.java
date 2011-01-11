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

import org.apache.directory.server.core.interceptor.Interceptor;


/**
 * Constants used to determine what kinds of {@link Interceptor}s need to be 
 * bypassed while performing operations within other Interceptors.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
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

    /** Bypass for when we modify schema attributes */
    public static final Collection<String> SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS;
    
    static
    {
        Collection<String> c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.AdministrativePointInterceptor" );
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.authn.AuthenticationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.collective.CollectiveAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.referral.ReferralInterceptor" );
        c.add( "org.apache.directory.server.core.changelog.ChangeLogInterceptor" );
        c.add( "org.apache.directory.server.core.operational.OperationalAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.trigger.TriggerInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        LOOKUP_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.AdministrativePointInterceptor" );
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.authn.AuthenticationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.exception.ExceptionInterceptor" );
        c.add( "org.apache.directory.server.core.operational.OperationalAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        HAS_ENTRY_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.authn.AuthenticationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.collective.CollectiveAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.operational.OperationalAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        LOOKUP_COLLECTIVE_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.authn.AuthenticationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.operational.OperationalAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        GETMATCHEDDN_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.authn.AuthenticationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.trigger.TriggerInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS = Collections.unmodifiableCollection( c );
                
        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.changelog.ChangeLogInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.exception.ExceptionInterceptor" );
        c.add( "org.apache.directory.server.core.operational.OperationalAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.subtree.SubentryInterceptor" );
        c.add( "org.apache.directory.server.core.collective.CollectiveAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.event.EventInterceptor" );
        c.add( "org.apache.directory.server.core.trigger.TriggerInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        GET_ROOT_DSE_BYPASS = Collections.unmodifiableCollection( c );
        
        
        c = new HashSet<String>();
        c.add( "org.apache.directory.server.core.normalization.NormalizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.AciAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.authz.DefaultAuthorizationInterceptor" );
        c.add( "org.apache.directory.server.core.exception.ExceptionInterceptor" );
        c.add( "org.apache.directory.server.core.schema.SchemaInterceptor" );
        c.add( "org.apache.directory.server.core.collective.CollectiveAttributeInterceptor" );
        c.add( "org.apache.directory.server.core.journal.JournalInterceptor" );
        SCHEMA_MODIFICATION_ATTRIBUTES_UPDATE_BYPASS = Collections.unmodifiableCollection( c );
    }
}
