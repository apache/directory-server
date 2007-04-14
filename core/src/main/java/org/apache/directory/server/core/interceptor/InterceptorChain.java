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
package org.apache.directory.server.core.interceptor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;

import org.apache.directory.server.core.DirectoryServiceConfiguration;
import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.ServiceContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Manages the chain of {@link Interceptor}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private static final Logger log = LoggerFactory.getLogger( InterceptorChain.class );

    /** Speedup for logs */
    private static final boolean IS_DEBUG = log.isDebugEnabled();

    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private PartitionNexus nexus;


        public void init( DirectoryServiceConfiguration factoryCfg, InterceptorConfiguration cfg )
        {
            this.nexus = factoryCfg.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        public boolean compare( NextInterceptor next, ServiceContext compareContext ) throws NamingException
        {
            return nexus.compare( compareContext );
        }


        public Attributes getRootDSE( NextInterceptor next, ServiceContext getRootDSEContext ) throws NamingException
        {
            return nexus.getRootDSE( getRootDSEContext );
        }


        public LdapDN getMatchedName( NextInterceptor next, ServiceContext getMatchedNameContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getMatchedName( getMatchedNameContext ).clone();
        }


        public LdapDN getSuffix( NextInterceptor next, ServiceContext getSuffixContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getSuffix( getSuffixContext ).clone();
        }


        public Iterator listSuffixes( NextInterceptor next, ServiceContext emptyContext ) throws NamingException
        {
            return nexus.listSuffixes( emptyContext );
        }


        public void delete( NextInterceptor next, ServiceContext deleteContext ) throws NamingException
        {
            nexus.delete( deleteContext );
        }


        public void add( NextInterceptor next, ServiceContext addContext ) throws NamingException
        {
            nexus.add( addContext );
        }


        public void modify( NextInterceptor next, ServiceContext modifyContext ) throws NamingException
        {
            nexus.modify( modifyContext );
        }


        public void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] mods ) throws NamingException
        {
            nexus.modify( name, mods );
        }


        public NamingEnumeration list( NextInterceptor next, LdapDN base ) throws NamingException
        {
            return nexus.list( base );
        }


        public NamingEnumeration search( NextInterceptor next, LdapDN base, Map env, ExprNode filter,
            SearchControls searchCtls ) throws NamingException
        {
            return nexus.search( base, env, filter, searchCtls );
        }


        public Attributes lookup( NextInterceptor next, ServiceContext lookupContext ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( lookupContext ).clone();
        }


        public boolean hasEntry( NextInterceptor next, ServiceContext entryContext ) throws NamingException
        {
            return nexus.hasEntry( entryContext );
        }


        public void rename( NextInterceptor next, ServiceContext renameContext )
            throws NamingException
        {
            nexus.rename( renameContext );
        }


        public void move( NextInterceptor next, ServiceContext moveContext ) throws NamingException
        {
            nexus.move( moveContext );
        }


        public void moveAndRename( NextInterceptor next, ServiceContext moveAndRenameContext )
            throws NamingException
        {
            nexus.moveAndRename( moveAndRenameContext );
        }


        public void addContextPartition( NextInterceptor next, ServiceContext addContextPartitionContext )
            throws NamingException
        {
            nexus.addContextPartition( addContextPartitionContext );
        }


        public void removeContextPartition( NextInterceptor next, ServiceContext removeContextPartition ) throws NamingException
        {
            nexus.removeContextPartition( removeContextPartition );
        }


        public void bind( NextInterceptor next, ServiceContext bindContext )  throws NamingException
        {
            nexus.bind( bindContext );
        }


        public void unbind( NextInterceptor next, ServiceContext unbindContext ) throws NamingException
        {
            nexus.unbind( unbindContext );
        }
    };

    private final Map<String, Entry> name2entry = new HashMap<String, Entry>();

    private final Entry tail;

    private Entry head;

    private DirectoryServiceConfiguration factoryCfg;


    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
        MutableInterceptorConfiguration tailCfg = new MutableInterceptorConfiguration();
        tailCfg.setName( "tail" );
        tailCfg.setInterceptor( FINAL_INTERCEPTOR );
        tail = new Entry( null, null, tailCfg );
        head = tail;
    }


    /**
     * Initializes and registers all interceptors according to the specified
     * {@link DirectoryServiceConfiguration}.
     */
    public synchronized void init( DirectoryServiceConfiguration factoryCfg ) throws NamingException
    {
        this.factoryCfg = factoryCfg;

        // Initialize tail first.
        FINAL_INTERCEPTOR.init( factoryCfg, null );

        // And register and initialize all interceptors
        ListIterator i = factoryCfg.getStartupConfiguration().getInterceptorConfigurations().listIterator();
        Interceptor interceptor = null;
        try
        {
            while ( i.hasNext() )
            {
                InterceptorConfiguration cfg = ( InterceptorConfiguration ) i.next();

                if ( IS_DEBUG )
                {
                    log.debug( "Adding interceptor " + cfg.getName() );
                }

                register( cfg );
            }
        }
        catch ( Throwable t )
        {
            // destroy if failed to initialize all interceptors.
            destroy();

            if ( t instanceof NamingException )
            {
                throw ( NamingException ) t;
            }
            else
            {
                throw new InterceptorException( interceptor, "Failed to initialize interceptor chain.", t );
            }
        }
    }


    /**
     * Deinitializes and deregisters all interceptors this chain contains.
     */
    public synchronized void destroy()
    {
        List<Entry> entries = new ArrayList<Entry>();
        Entry e = tail;
        
        do
        {
            entries.add( e );
            e = e.prevEntry;
        }
        while ( e != null );

        for ( Entry entry:entries )
        {
            if ( entry != tail )
            {
                try
                {
                    deregister( entry.configuration.getName() );
                }
                catch ( Throwable t )
                {
                    log.warn( "Failed to deregister an interceptor: " + entry.configuration.getName(), t );
                }
            }
        }
    }


    /**
     * Returns the registered interceptor with the specified name.
     * @return <tt>null</tt> if the specified name doesn't exist.
     */
    public Interceptor get( String interceptorName )
    {
        Entry e = name2entry.get( interceptorName );
        if ( e == null )
        {
            return null;
        }

        return e.configuration.getInterceptor();
    }


    /**
     * Returns the list of all registered interceptors.
     */
    public synchronized List getAll()
    {
        List<Interceptor> result = new ArrayList<Interceptor>();
        Entry e = head;
        
        do
        {
            result.add( e.configuration.getInterceptor() );
            e = e.nextEntry;
        }
        while ( e != tail );

        return result;
    }


    public synchronized void addFirst( InterceptorConfiguration cfg ) throws NamingException
    {
        register0( cfg, head );
    }


    public synchronized void addLast( InterceptorConfiguration cfg ) throws NamingException
    {
        register0( cfg, tail );
    }


    public synchronized void addBefore( String nextInterceptorName, InterceptorConfiguration cfg )
        throws NamingException
    {
        Entry e = name2entry.get( nextInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + nextInterceptorName );
        }
        register0( cfg, e );
    }


    public synchronized InterceptorConfiguration remove( String interceptorName ) throws NamingException
    {
        return deregister( interceptorName );
    }


    public synchronized void addAfter( String prevInterceptorName, InterceptorConfiguration cfg )
        throws NamingException
    {
        Entry e = name2entry.get( prevInterceptorName );
        if ( e == null )
        {
            throw new ConfigurationException( "Interceptor not found: " + prevInterceptorName );
        }
        register0( cfg, e.nextEntry );
    }


    /**
     * Adds and initializes an interceptor with the specified configuration.
     */
    private void register( InterceptorConfiguration cfg ) throws NamingException
    {
        checkAddable( cfg );
        register0( cfg, tail );
    }


    /**
     * Removes and deinitializes the interceptor with the specified name.
     */
    private InterceptorConfiguration deregister( String name ) throws ConfigurationException
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;

        if ( nextEntry == null )
        {
            // Don't deregister tail
            return null;
        }

        if ( prevEntry == null )
        {
            nextEntry.prevEntry = null;
            head = nextEntry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;
            nextEntry.prevEntry = prevEntry;
        }

        name2entry.remove( name );
        entry.configuration.getInterceptor().destroy();

        return entry.configuration;
    }


    private void register0( InterceptorConfiguration cfg, Entry nextEntry ) throws NamingException
    {
        String name = cfg.getName();
        Interceptor interceptor = cfg.getInterceptor();
        interceptor.init( factoryCfg, cfg );

        Entry newEntry;
        if ( nextEntry == head )
        {
            newEntry = new Entry( null, head, cfg );
            head.prevEntry = newEntry;
            head = newEntry;
        }
        else if ( head == tail )
        {
            newEntry = new Entry( null, tail, cfg );
            tail.prevEntry = newEntry;
            head = newEntry;
        }
        else
        {
            newEntry = new Entry( nextEntry.prevEntry, nextEntry, cfg );
            nextEntry.prevEntry.nextEntry = newEntry;
            nextEntry.prevEntry = newEntry;
        }

        name2entry.put( name, newEntry );
    }


    /**
     * Throws an exception when the specified interceptor name is not registered in this chain.
     *
     * @return An interceptor entry with the specified name.
     */
    private Entry checkOldName( String baseName ) throws ConfigurationException
    {
        Entry e = name2entry.get( baseName );

        if ( e == null )
        {
            throw new ConfigurationException( "Unknown interceptor name:" + baseName );
        }

        return e;
    }


    /**
     * Checks the specified interceptor name is already taken and throws an exception if already taken.
     */
    private void checkAddable( InterceptorConfiguration cfg ) throws ConfigurationException
    {
        if ( name2entry.containsKey( cfg.getName() ) )
        {
            throw new ConfigurationException( "Other interceptor is using name '" + cfg.getName() + "'" );
        }
    }


    /**
     * Gets the InterceptorEntry to use first with bypass information considered.
     *
     * @return the first entry to use.
     */
    private Entry getStartingEntry()
    {
        if ( InvocationStack.getInstance().isEmpty() )
        {
            return head;
        }

        Invocation invocation = InvocationStack.getInstance().peek();
        if ( !invocation.hasBypass() )
        {
            return head;
        }

        if ( invocation.isBypassed( PartitionNexusProxy.BYPASS_ALL ) )
        {
            return tail;
        }

        Entry next = head;
        while ( next != tail )
        {
            if ( invocation.isBypassed( next.configuration.getName() ) )
            {
                next = next.nextEntry;
            }
            else
            {
                return next;
            }
        }

        return tail;
    }


    public Attributes getRootDSE( ServiceContext getRootDSEContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.getRootDSE( next, getRootDSEContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public LdapDN getMatchedName( ServiceContext getMatchedNameContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.getMatchedName( next, getMatchedNameContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public LdapDN getSuffix( ServiceContext getSuffixContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.getSuffix( next, getSuffixContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public boolean compare( ServiceContext compareContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.compare( next, compareContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public Iterator listSuffixes( ServiceContext emptyContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.listSuffixes( next, emptyContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void addContextPartition( ServiceContext addContextPartitionContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.addContextPartition( next, addContextPartitionContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void removeContextPartition( ServiceContext removeContextPartition ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.removeContextPartition( next, removeContextPartition );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void delete( ServiceContext deleteContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.delete( next, deleteContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void add( ServiceContext addContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        try
        {
            head.add( next, addContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void bind( ServiceContext bindContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        try
        {
            head.bind( next, bindContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void unbind( ServiceContext unbindContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        
        try
        {
            head.unbind( next, unbindContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void modify( ServiceContext modifyContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.modify( next, modifyContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void modify( LdapDN name, ModificationItemImpl[] mods ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.modify( next, name, mods );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public NamingEnumeration list( LdapDN base ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.list( next, base );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
        throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.search( next, base, env, filter, searchCtls );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public Attributes lookup( ServiceContext lookupContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.lookup( next, lookupContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public boolean hasEntry( ServiceContext entryContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            return head.hasEntry( next, entryContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
            throw new InternalError(); // Should be unreachable
        }
    }


    public void rename( ServiceContext renameContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.rename( next, renameContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void move( ServiceContext moveContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.move( next, moveContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }


    public void moveAndRename( ServiceContext moveAndRenameContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        try
        {
            head.moveAndRename( next, moveAndRenameContext );
        }
        catch ( NamingException ne )
        {
            throw ne;
        }
        catch ( Throwable e )
        {
            throwInterceptorException( head, e );
        }
    }

    /**
     * Represents an internal entry of this chain.
     */
    private class Entry
    {
        private Entry prevEntry;

        private Entry nextEntry;

        private final InterceptorConfiguration configuration;

        private final NextInterceptor nextInterceptor;


        private Entry(Entry prevEntry, Entry nextEntry, InterceptorConfiguration configuration)
        {
            if ( configuration == null )
            {
                throw new NullPointerException( "configuration" );
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.configuration = configuration;
            this.nextInterceptor = new NextInterceptor()
            {
                private Entry getNextEntry()
                {
                    if ( InvocationStack.getInstance().isEmpty() )
                    {
                        return Entry.this.nextEntry;
                    }

                    Invocation invocation = InvocationStack.getInstance().peek();
                    if ( !invocation.hasBypass() )
                    {
                        return Entry.this.nextEntry;
                    }

                    //  I don't think we really need this since this check is performed by the chain when
                    //  getting the interceptor head to use.
                    //
                    //                    if ( invocation.isBypassed( DirectoryPartitionNexusProxy.BYPASS_ALL ) )
                    //                    {
                    //                        return tail;
                    //                    }

                    Entry next = Entry.this.nextEntry;
                    while ( next != tail )
                    {
                        if ( invocation.isBypassed( next.configuration.getName() ) )
                        {
                            next = next.nextEntry;
                        }
                        else
                        {
                            return next;
                        }
                    }

                    return next;
                }


                public boolean compare( ServiceContext compareContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.compare( next.nextInterceptor, compareContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Attributes getRootDSE( ServiceContext getRootDSEContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getRootDSE( next.nextInterceptor, getRootDSEContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public LdapDN getMatchedName( ServiceContext getMatchedNameContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getMatchedName( next.nextInterceptor, getMatchedNameContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public LdapDN getSuffix( ServiceContext getSuffixContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getSuffix( next.nextInterceptor, getSuffixContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Iterator listSuffixes( ServiceContext emptyContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.listSuffixes( next.nextInterceptor, emptyContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void delete( ServiceContext deleteContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.delete( next.nextInterceptor, deleteContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void add( ServiceContext addContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.add( next.nextInterceptor, addContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void modify( ServiceContext modifyContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( next.nextInterceptor, modifyContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void modify( LdapDN name, ModificationItemImpl[] mods ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( next.nextInterceptor, name, mods );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public NamingEnumeration list( LdapDN base ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.list( next.nextInterceptor, base );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public NamingEnumeration search( LdapDN base, Map env, ExprNode filter, SearchControls searchCtls )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.search( next.nextInterceptor, base, env, filter, searchCtls );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public Attributes lookup( ServiceContext lookupContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( next.nextInterceptor, lookupContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public boolean hasEntry( ServiceContext entryContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.hasEntry( next.nextInterceptor, entryContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void rename( ServiceContext renameContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.rename( next.nextInterceptor, renameContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void move( ServiceContext moveContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( next.nextInterceptor, moveContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void moveAndRename( ServiceContext moveAndRenameContext )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.moveAndRename( next.nextInterceptor, moveAndRenameContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void bind( ServiceContext bindContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();
    
                    try
                    {
                        interceptor.bind( next.nextInterceptor, bindContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void unbind( ServiceContext unbindContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.unbind( next.nextInterceptor, unbindContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                    }
                }


                public void addContextPartition( ServiceContext addContextPartitionContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.addContextPartition( next.nextInterceptor, addContextPartitionContext );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }


                public void removeContextPartition( ServiceContext removeContextPartition ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.removeContextPartition( next.nextInterceptor, removeContextPartition );
                    }
                    catch ( NamingException ne )
                    {
                        throw ne;
                    }
                    catch ( Throwable e )
                    {
                        throwInterceptorException( interceptor, e );
                        throw new InternalError(); // Should be unreachable
                    }
                }
            };
        }
    }


    private static void throwInterceptorException( Interceptor interceptor, Throwable e ) throws InterceptorException
    {
        throw new InterceptorException( interceptor, "Unexpected exception.", e );
    }
}
