/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.ldap.server.interceptor;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.naming.ConfigurationException;
import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.server.configuration.ContextPartitionConfiguration;
import org.apache.ldap.server.configuration.InterceptorConfiguration;
import org.apache.ldap.server.configuration.MutableInterceptorConfiguration;
import org.apache.ldap.server.jndi.ContextFactoryConfiguration;
import org.apache.ldap.server.partition.ContextPartitionNexus;


/**
 * Manages the chain of {@link Interceptor}s.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private ContextPartitionNexus nexus;

        public void init( ContextFactoryConfiguration factoryCfg, InterceptorConfiguration cfg )
        {
            this.nexus = factoryCfg.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        public Attributes getRootDSE( NextInterceptor next ) throws NamingException
        {
            return nexus.getRootDSE();
        }


        public Name getMatchedName( NextInterceptor next, Name dn, boolean normalized ) throws NamingException
        {
            return ( Name ) nexus.getMatchedName( dn, normalized ).clone();
        }


        public Name getSuffix( NextInterceptor next, Name dn, boolean normalized ) throws NamingException
        {
            return ( Name ) nexus.getSuffix( dn, normalized ).clone();
        }


        public Iterator listSuffixes( NextInterceptor next, boolean normalized ) throws NamingException
        {
            return nexus.listSuffixes( normalized );
        }


        public void delete( NextInterceptor next, Name name ) throws NamingException
        {
            nexus.delete( name );
        }


        public void add( NextInterceptor next, String upName, Name normName, Attributes entry ) throws NamingException
        {
            nexus.add( upName, normName, entry );
        }


        public void modify( NextInterceptor next, Name name, int modOp, Attributes mods ) throws NamingException
        {
            nexus.modify( name, modOp, mods );
        }


        public void modify( NextInterceptor next, Name name, ModificationItem[] mods ) throws NamingException
        {
            nexus.modify( name, mods );
        }


        public NamingEnumeration list( NextInterceptor next, Name base ) throws NamingException
        {
            return nexus.list( base );
        }


        public NamingEnumeration search( NextInterceptor next, Name base, Map env, ExprNode filter, SearchControls searchCtls ) throws NamingException
        {
            return nexus.search( base, env, filter, searchCtls );
        }


        public Attributes lookup( NextInterceptor next, Name name ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( name ).clone();
        }


        public Attributes lookup( NextInterceptor next, Name dn, String[] attrIds ) throws NamingException
        {
            return ( Attributes ) nexus.lookup( dn, attrIds ).clone();
        }


        public boolean hasEntry( NextInterceptor next, Name name ) throws NamingException
        {
            return nexus.hasEntry( name );
        }


        public boolean isSuffix( NextInterceptor next, Name name ) throws NamingException
        {
            return nexus.isSuffix( name );
        }


        public void modifyRn( NextInterceptor next, Name name, String newRn, boolean deleteOldRn ) throws NamingException
        {
            nexus.modifyRn( name, newRn, deleteOldRn );
        }


        public void move( NextInterceptor next, Name oriChildName, Name newParentName ) throws NamingException
        {
            nexus.move( oriChildName, newParentName );
        }


        public void move( NextInterceptor next, Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
        {
            nexus.move( oriChildName, newParentName, newRn, deleteOldRn );
        }


        public void addContextPartition( NextInterceptor next, ContextPartitionConfiguration cfg ) throws NamingException
        {
            nexus.addContextPartition( cfg );
        }


        public void removeContextPartition( NextInterceptor next, Name suffix ) throws NamingException
        {
            nexus.removeContextPartition( suffix );
        }
    };

    private final Map name2entry = new HashMap();

    private final Entry tail;

    private Entry head;

    private ContextFactoryConfiguration factoryCfg;

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
     * {@link ContextFactoryConfiguration}.
     */
    public synchronized void init( ContextFactoryConfiguration factoryCfg ) throws NamingException
    {
        this.factoryCfg = factoryCfg;

        // Initialize tail first.
        FINAL_INTERCEPTOR.init( factoryCfg, null );
        
        // And register and initialize all interceptors
        ListIterator i = factoryCfg.getStartupConfiguration().getInterceptorConfigurations().listIterator();
        Interceptor interceptor = null;
        try
        {
            while( i.hasNext() )
            {
                InterceptorConfiguration cfg = ( InterceptorConfiguration ) i.next();
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
        List entries = new ArrayList();
        Entry e = tail;
        do
        {
            entries.add( e );
            e = e.prevEntry;
        }
        while ( e != null );

        Iterator i = entries.iterator();
        while ( i.hasNext() )
        {
            e = ( Entry ) i.next();
            if( e != tail )
            {
                try
                {
                    deregister( e.configuration );
                }
                catch ( Throwable t )
                {
                    t.printStackTrace();
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
        Entry e = (Entry) name2entry.get( interceptorName );
        if( e == null )
        {
            return null;
        }
        
        return e.configuration.getInterceptor();
    }
    
    /**
     * Returns the list of all registered interceptors.
     */
    public List getAll()
    {
        List result = new ArrayList();
        Entry e = head;
        do
        {
            result.add( e.configuration.getInterceptor() );
            e = e.nextEntry;
        }
        while ( e != tail );
        
        return result;
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
     * Removes and deinitializes the interceptor with the specified configuration.
     */
    private void deregister( InterceptorConfiguration cfg ) throws ConfigurationException
    {
        String name = cfg.getName();
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;

        if( nextEntry == null )
        {
            // Don't deregister tail
            return;
        }

        if ( prevEntry == null )
        {
            nextEntry.prevEntry = null;
            head = entry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;
            nextEntry.prevEntry = prevEntry;
        }

        name2entry.remove( name );
        entry.configuration.getInterceptor().destroy();
    }


    private void register0( InterceptorConfiguration cfg, Entry nextEntry ) throws NamingException
    {
        String name = cfg.getName();
        Interceptor interceptor = cfg.getInterceptor();
        interceptor.init( factoryCfg, cfg );
        
        Entry newEntry;
        if( nextEntry == head )
        {
            newEntry = new Entry( null, head, cfg );
            head.prevEntry = newEntry;
            head = newEntry;
        }
        else if( head == tail )
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
        Entry e = ( Entry ) name2entry.get( baseName );

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


    public Attributes getRootDSE() throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.getRootDSE( next );
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


    public Name getMatchedName( Name name, boolean normalized ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.getMatchedName( next, name, normalized );
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


    public Name getSuffix( Name name, boolean normalized ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.getSuffix( next, name, normalized );
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


    public Iterator listSuffixes( boolean normalized ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.listSuffixes( next, normalized );
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

    public void addContextPartition( ContextPartitionConfiguration cfg ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.addContextPartition( next, cfg );
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

    public void removeContextPartition( Name suffix ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.removeContextPartition( next, suffix );
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

    public void delete( Name name ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.delete( next, name );
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


    public void add( String upName, Name normName, Attributes entry ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.add( next, upName, normName, entry );
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


    public void modify( Name name, int modOp, Attributes mods ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.modify( next, name, modOp, mods );
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


    public void modify( Name name, ModificationItem[] mods ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
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


    public NamingEnumeration list( Name base ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
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


    public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
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


    public Attributes lookup( Name name ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.lookup( next, name );
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


    public Attributes lookup( Name dn, String[] attrIds ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.lookup( next, dn, attrIds );
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


    public boolean hasEntry( Name name ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.hasEntry( next, name );
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


    public boolean isSuffix( Name name ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            return head.isSuffix( next, name );
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


    public void modifyRn( Name name, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.modifyRn( next, name, newRn, deleteOldRn );
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


    public void move( Name oriChildName, Name newParentName ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.move( next, oriChildName, newParentName );
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


    public void move( Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
    {
        Interceptor head = this.head.configuration.getInterceptor();
        NextInterceptor next = this.head.nextInterceptor;
        try
        {
            head.move( next, oriChildName, newParentName, newRn, deleteOldRn );
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


        private Entry( Entry prevEntry, Entry nextEntry,
                       InterceptorConfiguration configuration )
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
                public Attributes getRootDSE() throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getRootDSE( Entry.this.nextEntry.nextInterceptor );
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

                public Name getMatchedName( Name dn, boolean normalized ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getMatchedName( Entry.this.nextEntry.nextInterceptor, dn, normalized );
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

                public Name getSuffix( Name dn, boolean normalized ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.getSuffix( Entry.this.nextEntry.nextInterceptor, dn, normalized );
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

                public Iterator listSuffixes( boolean normalized ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.listSuffixes( Entry.this.nextEntry.nextInterceptor, normalized );
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

                public void delete( Name name ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.delete( Entry.this.nextEntry.nextInterceptor, name );
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

                public void add( String upName, Name normName, Attributes entry ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.add( Entry.this.nextEntry.nextInterceptor, upName, normName, entry );
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

                public void modify( Name name, int modOp, Attributes mods ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( Entry.this.nextEntry.nextInterceptor, name, modOp, mods );
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

                public void modify( Name name, ModificationItem[] mods ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.modify( Entry.this.nextEntry.nextInterceptor, name, mods );
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

                public NamingEnumeration list( Name base ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.list( Entry.this.nextEntry.nextInterceptor, base );
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

                public NamingEnumeration search( Name base, Map env, ExprNode filter, SearchControls searchCtls ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.search( Entry.this.nextEntry.nextInterceptor, base, env, filter, searchCtls );
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

                public Attributes lookup( Name name ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( Entry.this.nextEntry.nextInterceptor, name );
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

                public Attributes lookup( Name dn, String[] attrIds ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.lookup( Entry.this.nextEntry.nextInterceptor, dn, attrIds );
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

                public boolean hasEntry( Name name ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.hasEntry( Entry.this.nextEntry.nextInterceptor, name );
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

                public boolean isSuffix( Name name ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        return interceptor.isSuffix( Entry.this.nextEntry.nextInterceptor, name );
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

                public void modifyRn( Name name, String newRn, boolean deleteOldRn ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.modifyRn( Entry.this.nextEntry.nextInterceptor, name, newRn, deleteOldRn );
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

                public void move( Name oriChildName, Name newParentName ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( Entry.this.nextEntry.nextInterceptor, oriChildName, newParentName );
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

                public void move( Name oriChildName, Name newParentName, String newRn, boolean deleteOldRn ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.move( Entry.this.nextEntry.nextInterceptor, oriChildName, newParentName, newRn, deleteOldRn );
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

                public void addContextPartition( ContextPartitionConfiguration cfg ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.addContextPartition( Entry.this.nextEntry.nextInterceptor, cfg );
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

                public void removeContextPartition( Name suffix ) throws NamingException
                {
                    Interceptor interceptor = Entry.this.nextEntry.configuration.getInterceptor();

                    try
                    {
                        interceptor.removeContextPartition( Entry.this.nextEntry.nextInterceptor, suffix );
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
