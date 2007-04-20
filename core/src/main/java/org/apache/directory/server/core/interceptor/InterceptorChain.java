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
import org.apache.directory.server.core.configuration.InterceptorConfiguration;
import org.apache.directory.server.core.configuration.MutableInterceptorConfiguration;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.shared.ldap.filter.ExprNode;
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


        public boolean compare( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return nexus.compare( opContext );
        }


        public Attributes getRootDSE( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return nexus.getRootDSE( opContext );
        }


        public LdapDN getMatchedName( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getMatchedName( opContext ).clone();
        }


        public LdapDN getSuffix( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return ( LdapDN ) nexus.getSuffix( opContext ).clone();
        }


        public Iterator listSuffixes( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return nexus.listSuffixes( opContext );
        }


        public void delete( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.delete( opContext );
        }


        public void add( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.add( opContext );
        }


        public void modify( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.modify( opContext );
        }


        /*public void modify( NextInterceptor next, LdapDN name, ModificationItemImpl[] mods ) throws NamingException
        {
            nexus.modify( name, mods );
        }*/


        public NamingEnumeration list( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return nexus.list( opContext );
        }


        public NamingEnumeration search( NextInterceptor next, LdapDN base, Map env, ExprNode filter,
            SearchControls searchCtls ) throws NamingException
        {
            return nexus.search( base, env, filter, searchCtls );
        }


        public Attributes lookup( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( opContext ).clone();
        }


        public boolean hasEntry( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            return nexus.hasEntry( opContext );
        }


        public void rename( NextInterceptor next, OperationContext opContext )
            throws NamingException
        {
            nexus.rename( opContext );
        }


        public void move( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.move( opContext );
        }


        public void moveAndRename( NextInterceptor next, OperationContext opContext )
            throws NamingException
        {
            nexus.moveAndRename( opContext );
        }


        public void addContextPartition( NextInterceptor next, OperationContext opContext )
            throws NamingException
        {
            nexus.addContextPartition( opContext );
        }


        public void removeContextPartition( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.removeContextPartition( opContext );
        }


        public void bind( NextInterceptor next, OperationContext opContext )  throws NamingException
        {
            nexus.bind( opContext );
        }


        public void unbind( NextInterceptor next, OperationContext opContext ) throws NamingException
        {
            nexus.unbind( opContext );
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


    public Attributes getRootDSE( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.getRootDSE( next, opContext );
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


    public LdapDN getMatchedName( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;

        try
        {
            return head.getMatchedName( next, opContext );
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


    public LdapDN getSuffix( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.getSuffix( next, opContext );
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


    public boolean compare( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.compare( next, opContext );
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


    public Iterator listSuffixes( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.listSuffixes( next, opContext );
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


    public void addContextPartition( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.addContextPartition( next, opContext );
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


    public void removeContextPartition( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.removeContextPartition( next, opContext );
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


    public void delete( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.delete( next, opContext );
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


    public void add( OperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        
        try
        {
            head.add( next, opContext );
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


    public void bind( OperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        
        try
        {
            head.bind( next, opContext );
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


    public void unbind( OperationContext opContext ) throws NamingException
    {
        Entry node = getStartingEntry();
        Interceptor head = node.configuration.getInterceptor();
        NextInterceptor next = node.nextInterceptor;
        
        try
        {
            head.unbind( next, opContext );
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


    public void modify( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.modify( next, opContext );
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


    /*public void modify( LdapDN name, ModificationItemImpl[] mods ) throws NamingException
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
    }*/


    public NamingEnumeration list( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.list( next, opContext );
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


    public Attributes lookup( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.lookup( next, opContext );
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


    public boolean hasEntry( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            return head.hasEntry( next, opContext );
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


    public void rename( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.rename( next, opContext );
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


    public void move( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.move( next, opContext );
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


    public void moveAndRename( OperationContext opContext ) throws NamingException
    {
        Entry entry = getStartingEntry();
        Interceptor head = entry.configuration.getInterceptor();
        NextInterceptor next = entry.nextInterceptor;
        
        try
        {
            head.moveAndRename( next, opContext );
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


                public boolean compare( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.compare( next.nextInterceptor, opContext );
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


                public Attributes getRootDSE( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getRootDSE( next.nextInterceptor, opContext );
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


                public LdapDN getMatchedName( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getMatchedName( next.nextInterceptor, opContext );
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


                public LdapDN getSuffix( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getSuffix( next.nextInterceptor, opContext );
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


                public Iterator listSuffixes( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.listSuffixes( next.nextInterceptor, opContext );
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


                public void delete( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.delete( next.nextInterceptor, opContext );
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


                public void add( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.add( next.nextInterceptor, opContext );
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


                public void modify( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( next.nextInterceptor, opContext );
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

                
                public NamingEnumeration list( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.list( next.nextInterceptor, opContext );
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


                public Attributes lookup( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( next.nextInterceptor, opContext );
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


                public boolean hasEntry( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        return interceptor.hasEntry( next.nextInterceptor, opContext );
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


                public void rename( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.rename( next.nextInterceptor, opContext );
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


                public void move( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( next.nextInterceptor, opContext );
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


                public void moveAndRename( OperationContext opContext )
                    throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.moveAndRename( next.nextInterceptor, opContext );
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


                public void bind( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();
    
                    try
                    {
                        interceptor.bind( next.nextInterceptor, opContext );
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


                public void unbind( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.unbind( next.nextInterceptor, opContext );
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


                public void addContextPartition( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.addContextPartition( next.nextInterceptor, opContext );
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


                public void removeContextPartition( OperationContext opContext ) throws NamingException
                {
                    Entry next = getNextEntry();
                    Interceptor interceptor = next.configuration.getInterceptor();

                    try
                    {
                        interceptor.removeContextPartition( next.nextInterceptor, opContext );
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
