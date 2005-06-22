package org.apache.ldap.server.invocation;


import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public final class InvocationStack
{
    // I didn't use ThreadLocal to release contexts explicitly.
    // It seems like JDK 1.5 supports explicit release by introducing
    // <tt>ThreadLocal.remove()</tt>, but we're still targetting 1.4.
    private static final Map stacks = new IdentityHashMap();
    
    public static InvocationStack getInstance()
    {
        Thread currentThread = Thread.currentThread();
        InvocationStack ctx;
        synchronized( stacks )
        {
            ctx = ( InvocationStack ) stacks.get( currentThread );
            if( ctx == null )
            {
                ctx = new InvocationStack();
            }
        }
        return ctx;
    }

    private final Thread thread;
    private final List stack = new ArrayList();

    private InvocationStack()
    {
        Thread currentThread = Thread.currentThread();
        this.thread = currentThread;
        // This operation is already synchronized from getInstance()
        stacks.put( currentThread, this );
    }
    
    public Invocation[] toArray()
    {
        Invocation[] result = new Invocation[ stack.size() ];
        result = ( Invocation[] ) stack.toArray( result );
        return result;
    }
    
    public Invocation peek()
    {
        return ( Invocation ) this.stack.get( 0 );
    }
    
    public void push( Invocation invocation )
    {
        this.stack.add( 0, invocation );
    }
    
    public Invocation pop()
    {
        Invocation invocation = ( Invocation ) this.stack.remove( 0 );
        if( this.stack.size() == 0 )
        {
            synchronized( stacks )
            {
                stacks.remove( thread );
            }
        }

        return invocation;
    }
}
