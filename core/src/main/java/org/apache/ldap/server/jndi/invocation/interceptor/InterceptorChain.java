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
package org.apache.ldap.server.jndi.invocation.interceptor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.server.BackingStore;
import org.apache.ldap.server.jndi.invocation.Invocation;

/**
 * Manages the chain of {@link Interceptor}s.
 * <p>
 * {@link org.apache.ldap.server.jndi.JndiProvider#invoke(Invocation)}
 * redirects {@link Invocation}s to {@link #process(Invocation)} and
 * the chain starts.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        public void init(Properties config) throws NamingException
        {
            // unused
        }

        public void destroy()
        {
            // unused
        }

        public void process(NextInterceptor nextInterceptor, Invocation call)
                throws NamingException
        {
            call.execute( store );
        }
    };

    private final BackingStore store;
    private final Map name2entry = new HashMap();
    private Entry head = new Entry( null, null, "end", FINAL_INTERCEPTOR );
    private final Entry tail = head;

    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain( BackingStore store )
    {
        if( store == null )
        {
            throw new NullPointerException( "store" ) ;
        }
        
        this.store = store;
    }

    /**
     * Returns the interceptor with the specified <code>name</code>.
     * 
     * @return <code>null</code> if there is no interceptor with the specified
     *         <code>name</code>.
     */
    public Interceptor get( String name )
    {
        Entry e = ( Entry ) name2entry.get( name );
        if( e == null )
        {
            return null;
        }
        return e.interceptor;
    }

    /**
     * Adds the specified interceptor with the specified name at the beginning
     * of this chain.
     */
    public synchronized void addFirst( String name,
                                       Interceptor interceptor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( null, head, name, interceptor );
        head.prevEntry = newEntry;
        head = newEntry;

        name2entry.put( name, newEntry );
    }

    /**
     * Adds the specified interceptor with the specified name at the end
     * of this chain.
     */
    public synchronized void addLast( String name,
                                      Interceptor interceptor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( tail.prevEntry, tail, name, interceptor );
        if( tail.prevEntry != null )
        {
            tail.prevEntry.nextEntry = newEntry;
        }
        else
        {
            head = newEntry;
        }
        tail.prevEntry = newEntry;
        
        name2entry.put( name, newEntry );
    }

    /**
     * Adds the specified interceptor with the specified name just before
     * the interceptor whose name is <code>baseName</code> in this chain.
     */
    public synchronized void addBefore( String baseName,
                                        String name,
                                        Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkNewName( name );

        Entry prevEntry = baseEntry.prevEntry;
        Entry newEntry = new Entry( prevEntry, baseEntry, name, interceptor );
        if( prevEntry == null )
        {
            head = newEntry;
        }
        else
        {
            prevEntry.nextEntry.prevEntry = newEntry;
            prevEntry.nextEntry = newEntry;
        }
        
        name2entry.put( name, newEntry );
    }
    
    /**
     * Adds the specified interceptor with the specified name just after
     * the interceptor whose name is <code>baseName</code> in this chain.
     */
    public synchronized void addAfter( String baseName,
                                       String name,
                                       Interceptor interceptor )
    {
        Entry baseEntry = checkOldName( baseName );
        checkNewName(name);

        Entry nextEntry = baseEntry.nextEntry;
        Entry newEntry = new Entry( baseEntry, nextEntry, name, interceptor );
        if( nextEntry == null )
        {
            throw new IllegalStateException();
        }

        nextEntry.prevEntry.nextEntry = newEntry;
        nextEntry.prevEntry = newEntry;
        name2entry.put( name, newEntry );
    }
    
    /**
     * Removes the interceptor with the specified name from this chain.
     */
    public synchronized void remove( String name )
    {
        Entry entry = checkOldName( name );
        Entry prevEntry = entry.prevEntry;
        Entry nextEntry = entry.nextEntry;
        if( prevEntry == null )
        {
            nextEntry.prevEntry = null;
            head = entry;
        }
        else
        {
            prevEntry.nextEntry = nextEntry;
            nextEntry.prevEntry = prevEntry;
        }
    }

    /**
     * Removes all interceptors added to this chain.
     */
    public synchronized void clear()
    {
        tail.prevEntry = null;
        tail.nextEntry = null;
        head = tail;
    }

	/**
	 * Throws an exception when the specified interceptor name is not registered
	 * in this chain.
	 * 
	 * @return An interceptor entry with the specified name.
	 */
    private Entry checkOldName( String baseName )
    {
        Entry e = ( Entry ) name2entry.get( baseName );
        if( e == null )
        {
            throw new IllegalArgumentException( "Unknown interceptor name:" +
                                                baseName );
        }
        return e;
    }

	/**
	 * Checks the specified interceptor name is already taken and throws
	 * an exception if already taken.
	 */
    private void checkNewName( String name )
    {
        if( name2entry.containsKey( name ) )
        {
            throw new IllegalArgumentException(
                    "Other interceptor is using name '" + name + "'" );
        }
    }
    
    /**
     * Start invocation chain with the specified invocation.
     * 
     * @throws NamingException if invocation failed
     */
    public void process( Invocation call ) throws NamingException
    {
        Entry head = this.head;
        try
        {
            head.interceptor.process(
                    head.nextInterceptor, call );
        }
        catch( NamingException ne )
        {
            throw ne;
        }
        catch( Throwable e )
        {
            throw new InterceptorException( head.interceptor, call,
                                            "Unexpected exception.", e );
        }
    }

    /**
     * Returns the list of interceptors this chain in the order of
     * evaluation.
     */
    public List getAll()
    {
        List list = new ArrayList();
        Entry e = head;
        do
        {
            list.add( e.interceptor );
            e = e.nextEntry;
        }
        while( e != null );

        return list;
    }

    /**
     * Returns the list of interceptors this chain in the reversed
     * order of evaluation.
     */
    public List getAllReversed()
    {
        List list = new ArrayList();
        Entry e = tail;
        do
        {
            list.add( e.interceptor );
            e = e.prevEntry;
        }
        while( e != null );

        return list;
    }

	/**
	 * Represents an internal entry of this chain.
	 */
    private class Entry
    {
        private Entry prevEntry;
        private Entry nextEntry;
        private final String name;
        private final Interceptor interceptor;
        private final NextInterceptor nextInterceptor;

        private Entry( Entry prevEntry, Entry nextEntry,
                       String name, Interceptor interceptor )
        {
            if( interceptor == null )
            {
                throw new NullPointerException( "interceptor" );
            }
            if( name == null )
            {
                throw new NullPointerException( "name" );
            }

            this.prevEntry = prevEntry;
            this.nextEntry = nextEntry;
            this.name = name;
            this.interceptor = interceptor;
            this.nextInterceptor = new NextInterceptor()
            {
                public void process(Invocation call)
                        throws NamingException {
                    Interceptor interceptor = Entry.this.nextEntry.interceptor;
                    try
                    {
                        interceptor.process(
                                Entry.this.nextEntry.nextInterceptor, call );
                    }
                    catch( NamingException ne )
                    {
                        throw ne;
                    }
                    catch( Throwable e )
                    {
                        throw new InterceptorException( interceptor, call,
                                                             "Unexpected exception.", e );
                    }
                }
            };
        }
    }
}
