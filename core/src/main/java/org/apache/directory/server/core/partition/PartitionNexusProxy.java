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
package org.apache.directory.server.core.partition;


import java.util.*; 

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchResult;
import javax.naming.event.EventContext;
import javax.naming.event.NamingListener;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.enumeration.SearchResultFilter;
import org.apache.directory.server.core.enumeration.SearchResultFilteringEnumeration;
import org.apache.directory.server.core.event.EventService;
import org.apache.directory.server.core.interceptor.InterceptorChain;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.shared.ldap.exception.LdapSizeLimitExceededException;
import org.apache.directory.shared.ldap.exception.LdapTimeLimitExceededException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.LdapDN;


/**
 * A decorator that wraps other {@link PartitionNexus} to enable
 * {@link InterceptorChain} and {@link InvocationStack} support.
 * All {@link Invocation}s made to this nexus is automatically pushed to
 * {@link InvocationStack} of the current thread, and popped when
 * the operation ends.  All invocations are filtered by {@link InterceptorChain}.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PartitionNexusProxy extends PartitionNexus
{
    /** safe to use set of bypass instructions to lookup raw entries */
    public static final Collection LOOKUP_BYPASS;
    /** safe to use set of bypass instructions to getMatchedDn */
    public static final Collection GETMATCHEDDN_BYPASS;
    /** safe to use set of bypass instructions to lookup raw entries excluding operational attributes */
    public static final Collection LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS;
    /** Bypass String to use when ALL interceptors should be skipped */
    public static final String BYPASS_ALL = "*";
    /** Bypass String to use when ALL interceptors should be skipped */
    public static final Collection BYPASS_ALL_COLLECTION = Collections.singleton( BYPASS_ALL );
    /** Integer const for DirContext.ADD_ATTRIBUTE */
    private static final Integer ADD_MODOP = new Integer( DirContext.ADD_ATTRIBUTE );
    /** Integer const for DirContext.REMOVE_ATTRIBUTE */
    private static final Integer REMOVE_MODOP = new Integer( DirContext.REMOVE_ATTRIBUTE );
    /** Integer const for DirContext.REPLACE_ATTRIBUTE */
    private static final Integer REPLACE_MODOP = new Integer( DirContext.REPLACE_ATTRIBUTE );

    private final Context caller;
    private final DirectoryService service;
    private final DirectoryServiceConfiguration configuration;

    static
    {
        Collection c = new HashSet();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
        LOOKUP_BYPASS = Collections.unmodifiableCollection( c );

        c = new HashSet();
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "operationalAttributeService" );
        c.add( "referralService" );
        c.add( "eventService" );
        GETMATCHEDDN_BYPASS = Collections.unmodifiableCollection( c );
        
        c = new HashSet();
        c.add( "normalizationService" );
        c.add( "authenticationService" );
        c.add( "authorizationService" );
        c.add( "defaultAuthorizationService" );
        c.add( "schemaService" );
        c.add( "subentryService" );
        c.add( "referralService" );
        c.add( "eventService" );
        LOOKUP_EXCLUDING_OPR_ATTRS_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Creates a new instance.
     * 
     * @param caller a JNDI {@link Context} object that will call this proxy
     * @param service a JNDI service
     */
    public PartitionNexusProxy(Context caller, DirectoryService service)
    {
        this.caller = caller;
        this.service = service;
        this.configuration = service.getConfiguration();
    }


    public LdapContext getLdapContext()
    {
        return this.configuration.getPartitionNexus().getLdapContext();
    }


    public void init( DirectoryServiceConfiguration factoryCfg, PartitionConfiguration cfg )
    {
    }


    public void destroy()
    {
    }


    public Partition getSystemPartition()
    {
        return this.configuration.getPartitionNexus().getSystemPartition();
    }


    public Partition getPartition( LdapDN dn ) throws NamingException
    {
        return this.configuration.getPartitionNexus().getPartition( dn );
    }


    public LdapDN getSuffix() throws NamingException
    {
        return this.configuration.getPartitionNexus().getSuffix();
    }

    public LdapDN getUpSuffix() throws NamingException
    {
        return this.configuration.getPartitionNexus().getUpSuffix();
    }


    public void sync() throws NamingException
    {
        this.service.sync();
    }


    public void close() throws NamingException
    {
        this.service.shutdown();
    }


    public boolean isInitialized()
    {
        return this.service.isStarted();
    }


    public LdapDN getMatchedName ( LdapDN dn ) throws NamingException
    {
        return getMatchedName( dn, null );
    }


    public LdapDN getMatchedName( LdapDN dn, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { dn };
        stack.push( new Invocation( this, caller, "getMatchedDn", args, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().getMatchedName( dn );
        }
        finally
        {
            stack.pop();
        }
    }


    public LdapDN getSuffix ( LdapDN dn ) throws NamingException
    {
        return getSuffix( dn, null );
    }


    public LdapDN getSuffix( LdapDN dn, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { dn };
        stack.push( new Invocation( this, caller, "getSuffix", args, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().getSuffix( dn );
        }
        finally
        {
            stack.pop();
        }
    }


    public Iterator listSuffixes () throws NamingException
    {
        return listSuffixes( null );
    }


    public Iterator listSuffixes( Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[] { };
        stack.push( new Invocation( this, caller, "listSuffixes", args, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().listSuffixes();
        }
        finally
        {
            stack.pop();
        }
    }


    public boolean compare( LdapDN name, String oid, Object value ) throws NamingException
    {
        return compare( name, oid, value, null );
    }


    public boolean compare( LdapDN name, String oid, Object value, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "compare", new Object[]
            { name, oid, value }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().compare( name, oid, value );
        }
        finally
        {
            stack.pop();
        }
    }


    public void delete( LdapDN name ) throws NamingException
    {
        delete( name, null );
    }


    public void delete( LdapDN name, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "delete", new Object[]
            { name }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().delete( name );
        }
        finally
        {
            stack.pop();
        }
    }


    public void add( LdapDN normName, Attributes entry ) throws NamingException
    {
        add( normName, entry, null );
    }


    public void add( LdapDN normName, Attributes entry, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "add", new Object[]
            { normName, entry }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().add( normName, entry );
        }
        finally
        {
            stack.pop();
        }
    }


    public void modify( LdapDN name, int modOp, Attributes mods ) throws NamingException
    {
        modify( name, modOp, mods, null );
    }


    public void modify( LdapDN name, int modOp, Attributes mods, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Integer modOpObj;

        switch ( modOp )
        {
            case ( DirContext.ADD_ATTRIBUTE  ):
                modOpObj = ADD_MODOP;
                break;
            case ( DirContext.REMOVE_ATTRIBUTE  ):
                modOpObj = REMOVE_MODOP;
                break;
            case ( DirContext.REPLACE_ATTRIBUTE  ):
                modOpObj = REPLACE_MODOP;
                break;
            default:
                throw new IllegalArgumentException( "bad modification operation value: " + modOp );
        }

        stack.push( new Invocation( this, caller, "modify", new Object[]
            { name, modOpObj, mods }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().modify( name, modOp, mods );
        }
        finally
        {
            stack.pop();
        }
    }


    public void modify( LdapDN name, ModificationItem[] mods ) throws NamingException
    {
        modify( name, mods, null );
    }


    public void modify( LdapDN name, ModificationItem[] mods, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "modify", new Object[]
            { name, mods }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().modify( name, mods );
        }
        finally
        {
            stack.pop();
        }
    }


    public NamingEnumeration list( LdapDN base ) throws NamingException
    {
        return list( base, null );
    }


    public NamingEnumeration list( LdapDN base, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "list", new Object[]
            { base }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().list( base );
        }
        finally
        {
            stack.pop();
        }
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        NamingEnumeration ne = search( base, env, filter, searchCtls, null );

        if ( ne instanceof SearchResultFilteringEnumeration )
        {
            SearchResultFilteringEnumeration results = ( SearchResultFilteringEnumeration ) ne;
            if ( searchCtls.getTimeLimit() + searchCtls.getCountLimit() > 0 )
            {
                // this will be he last filter added so other filters before it must 
                // have passed/approved of the entry to be returned back to the client
                // so the candidate we have is going to be returned for sure
                results.addResultFilter( new SearchResultFilter()
                {
                    final long startTime = System.currentTimeMillis();
                    int count = 1; // with prefetch we've missed one which is ok since 1 is the minimum


                    public boolean accept( Invocation invocation, SearchResult result, SearchControls controls )
                        throws NamingException
                    {
                        if ( controls.getTimeLimit() > 0 )
                        {
                            long runtime = System.currentTimeMillis() - startTime;
                            if ( runtime > controls.getTimeLimit() )
                            {
                                throw new LdapTimeLimitExceededException();
                            }
                        }

                        if ( controls.getCountLimit() > 0 )
                        {
                            if ( count > controls.getCountLimit() )
                            {
                                throw new LdapSizeLimitExceededException();
                            }
                        }

                        count++;
                        return true;
                    }
                } );
            }
        }
        return ne;
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "search", new Object[]
            { base, env, filter, searchCtls }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().search( base, env, filter, searchCtls );
        }
        finally
        {
            stack.pop();
        }
    }


    public Attributes lookup( LdapDN name ) throws NamingException
    {
        return lookup( name, ( Collection ) null );
    }


    public Attributes lookup( LdapDN name, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "lookup", new Object[]
            { name }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().lookup( name );
        }
        finally
        {
            stack.pop();
        }
    }


    public Attributes lookup( LdapDN dn, String[] attrIds ) throws NamingException
    {
        return lookup( dn, attrIds, null );
    }


    public Attributes lookup( LdapDN dn, String[] attrIds, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "lookup", new Object[]
            { dn, attrIds }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().lookup( dn, attrIds );
        }
        finally
        {
            stack.pop();
        }
    }


    public boolean hasEntry( LdapDN name ) throws NamingException
    {
        return hasEntry( name, null );
    }


    public boolean hasEntry( LdapDN name, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "hasEntry", new Object[]
            { name }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().hasEntry( name );
        }
        finally
        {
            stack.pop();
        }
    }


    public boolean isSuffix( LdapDN name ) throws NamingException
    {
        return isSuffix( name, null );
    }


    public boolean isSuffix( LdapDN name, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "isSuffix", new Object[]
            { name }, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().isSuffix( name );
        }
        finally
        {
            stack.pop();
        }
    }


    public void modifyRn( LdapDN name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        modifyRn( name, newRn, deleteOldRn, null );
    }


    public void modifyRn( LdapDN name, String newRn, boolean deleteOldRn, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { name, newRn, deleteOldRn ? Boolean.TRUE : Boolean.FALSE };
        stack.push( new Invocation( this, caller, "modifyRn", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().modifyRn( name, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName ) throws NamingException
    {
        move( oriChildName, newParentName, null );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "move", new Object[]
            { oriChildName, newParentName }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().move( oriChildName, newParentName );
        }
        finally
        {
            stack.pop();
        }
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        move( oriChildName, newParentName, newRn, deleteOldRn, null );
    }


    public void move( LdapDN oriChildName, LdapDN newParentName, String newRn, boolean deleteOldRn, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { oriChildName, newParentName, newRn, deleteOldRn ? Boolean.TRUE : Boolean.FALSE };
        stack.push( new Invocation( this, caller, "move", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().move( oriChildName, newParentName, newRn, deleteOldRn );
        }
        finally
        {
            stack.pop();
        }
    }

    /**
     * TODO : check if we can find another way to procect ourselves from recursion.
     * 
     * @param bindDn
     * @param credentials
     * @param mechanisms
     * @param saslAuthId
     * @param bypass
     * @throws NamingException
     */
    public void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId, Collection bypass )
        throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { bindDn, credentials, mechanisms, saslAuthId };
        stack.push( new Invocation( this, caller, "bind", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().bind( bindDn, credentials, mechanisms, saslAuthId );
        }
        finally
        {
            stack.pop();
        }
    }


    public void unbind( LdapDN bindDn, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        Object[] args = new Object[]
            { bindDn };
        stack.push( new Invocation( this, caller, "unbind", args, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().unbind( bindDn );
        }
        finally
        {
            stack.pop();
        }
    }


    public void bind( LdapDN bindDn, byte[] credentials, List mechanisms, String saslAuthId ) throws NamingException
    {
        bind( bindDn, credentials, mechanisms, saslAuthId, null );
    }


    public void unbind( LdapDN bindDn ) throws NamingException
    {
        unbind( bindDn, null );
    }


    public Attributes getRootDSE() throws NamingException
    {
        return getRootDSE( null );
    }


    public Attributes getRootDSE( Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "getRootDSE", null, bypass ) );
        try
        {
            return this.configuration.getInterceptorChain().getRootDSE();
        }
        finally
        {
            stack.pop();
        }
    }


    public void addContextPartition( PartitionConfiguration config ) throws NamingException
    {
        addContextPartition( config, null );
    }


    public void addContextPartition( PartitionConfiguration config, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "addContextPartition", new Object[]
            { config }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().addContextPartition( config );
        }
        finally
        {
            stack.pop();
        }
    }


    public void removeContextPartition( LdapDN suffix ) throws NamingException
    {
        removeContextPartition( suffix, null );
    }


    public void removeContextPartition( LdapDN suffix, Collection bypass ) throws NamingException
    {
        ensureStarted();
        InvocationStack stack = InvocationStack.getInstance();
        stack.push( new Invocation( this, caller, "removeContextPartition", new Object[]
            { suffix }, bypass ) );
        try
        {
            this.configuration.getInterceptorChain().removeContextPartition( suffix );
        }
        finally
        {
            stack.pop();
        }
    }


    private void ensureStarted() throws ServiceUnavailableException
    {
        if ( !service.isStarted() )
        {
            throw new ServiceUnavailableException( "Directory service is not started." );
        }
    }


    public void registerSupportedExtensions( Set extensionOids )
    {
        configuration.getPartitionNexus().registerSupportedExtensions( extensionOids );
    }


    // -----------------------------------------------------------------------
    // EventContext and EventDirContext notification methods
    // -----------------------------------------------------------------------

    /*
     * All listener registration/deregistration methods can be reduced down to
     * the following methods.  Rather then make these actual intercepted methods
     * we use them as out of band methods to interface with the notification
     * interceptor.
     */

    public void addNamingListener( EventContext ctx, Name name, ExprNode filter, SearchControls searchControls,
                                   NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.addNamingListener( ctx, name, filter, searchControls, namingListener );
    }


    public void removeNamingListener( EventContext ctx, NamingListener namingListener ) throws NamingException
    {
        InterceptorChain chain = this.configuration.getInterceptorChain();
        if ( chain == null )
        {
            return;
        }
        EventService interceptor = ( EventService ) chain.get( "eventService" );
        interceptor.removeNamingListener( ctx, namingListener );
    }
}