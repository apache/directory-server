package org.apache.ldap.server.jndi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.ldap.server.exception.InterceptorException;

/**
 * Manages {@link Interceptor} stack.  The first {@link Interceptor} is
 * invoked and then invocation chain starts.
 * 
 * TODO imeplement me.
 * 
 * @author The Apache Directory Project (dev@directory.apache.org)
 * @author Trustin Lee (trustin@apache.org)
 * @version $Rev$, $Date$
 */
public class InterceptorChain
{
    private static final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        public void init(Properties config) throws NamingException
        {
            // do nothing
        }

        public void destroy()
        {
            // do nothing
        }

        public void invoke(Interceptor nextInterceptor, Invocation invocation)
                throws NamingException
        {
            // do nothing
        }
    };

    private final Map name2entry = new HashMap();
    private final Map interceptor2entry = new IdentityHashMap();
    private Entry head = new Entry( null, null, "", FINAL_INTERCEPTOR );
    private final Entry tail = head;

    /**
     * Create a new interceptor chain.
     */
    public InterceptorChain()
    {
    }

    /**
     * Returns the interceptor with the specified <code>name</code>.
     * 
     * @return <code>null</code> if there is no interceptor with the specified
     *         <code>name</code>.
     */
    public Interceptor getInterceptor( String name )
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
    public synchronized void addInterceptorFirst( String name,
                                                  Interceptor interceptor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( null, head, name, interceptor );
        head.prevEntry = newEntry;
        head = newEntry;
    }

    /**
     * Adds the specified interceptor with the specified name at the end
     * of this chain.
     */
    public synchronized void addInterceptorLast( String name,
                                                 Interceptor interceptor )
    {
        checkNewName( name );
        
        Entry newEntry = new Entry( tail.prevEntry, tail, name, interceptor );
        tail.prevEntry.nextEntry = newEntry;
        tail.prevEntry = newEntry;
    }

    /**
     * Adds the specified interceptor with the specified name just before
     * the interceptor whose name is <code>baseName</code> in this chain.
     */
    public synchronized void addInterceptorBefore( String baseName,
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
    public synchronized void addInterceptorAfter( String baseName,
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
        else
        {
            nextEntry.prevEntry.nextEntry = newEntry;
            nextEntry.prevEntry = newEntry;
        }

        name2entry.put( name, newEntry );
    }
    
    /**
     * Removes the interceptor with the specified name from this chain.
     */
    public synchronized void removeInterceptor( String name )
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
     * Removed all interceptors added to this chain.
     */
    public synchronized void removeAllInterceptors()
    {
        tail.prevEntry = null;
        tail.nextEntry = null;
        head = tail;
    }

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
     * @throws NamingException if invocation failed
     */
    public void invoke( Invocation invocation ) throws NamingException
    {
        Entry head = this.head;
        try
        {
            head.interceptor.invoke(
                    head.nextInterceptor, invocation );
        }
        catch( NamingException ne )
        {
            throw ne;
        }
        catch( Throwable e )
        {
            throw new InterceptorException( head.interceptor, invocation,
                                            "Unexpected exception.", e );
        }
    }

    /**
     * Returns the list of interceptors this chain contains in the order of
     * evaluation.
     */
    public List getAllInterceptors()
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
     * Returns the list of interceptors this chain contains in the reversed
     * order of evaluation.
     */
    public List getAllInterceptorsReversed()
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

    private class Entry
    {
        private Entry prevEntry;
        private Entry nextEntry;
        private final String name;
        private final Interceptor interceptor;
        private final Interceptor nextInterceptor;

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
            this.nextInterceptor = new Interceptor()
            {
                public void init(Properties config) throws NamingException {
                    throw new IllegalStateException();
                }

                public void destroy() {
                    throw new IllegalStateException();
                }
    
                public void invoke(Interceptor nextInterceptor, Invocation invocation)
                        throws NamingException {
                    Interceptor interceptor = Entry.this.nextEntry.interceptor;
                    try
                    {
                        interceptor.invoke(
                                Entry.this.nextEntry.nextInterceptor, invocation );
                    }
                    catch( NamingException ne )
                    {
                        throw ne;
                    }
                    catch( Throwable e )
                    {
                        throw new InterceptorException( interceptor, invocation,
                                                        "Unexpected exception.", e );
                    }
                }
            };
        }
    }
}
