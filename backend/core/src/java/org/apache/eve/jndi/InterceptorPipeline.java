package org.apache.eve.jndi;


import java.util.List ;
import java.util.ArrayList ;


/**
 * An Interceptor composed of a pipeline of interceptors that can be called 
 * sequentially with one invokation.
 * 
 */
public abstract class InterceptorPipeline
    implements Interceptor
{
    /** The List of Interceptors */
    private List m_list = new ArrayList() ;
    
    
    /**
     * Adds a Interceptor to this InterceptorPipeline at a specific position.
     *
     * @param a_posn the 0 index based position  
     * @param a_service the Interceptor to add.
     */
    public void add( int a_posn, Interceptor a_service ) 
    {
        m_list.add( a_posn, a_service ) ;
    }
    
    
    /**
     * Removes an Interceptor from this InterceptorPipeline.
     *
     * @param a_posn the 0 index based position  
     * @return the removed Interceptor
     */
    public Interceptor remove( int a_posn ) 
    {
        return ( Interceptor ) m_list.remove( a_posn ) ;
    }
    
    
    /**
     * Gets the Interceptor in this InterceptorPipeline at a specified.
     *
     * @param a_posn the 0 index based position
     * @return the Interceptor at a_posn
     */
    public Interceptor get( int a_posn ) 
    {
        return ( Interceptor ) m_list.get( a_posn ) ;
    }
    
    
    /**
     * Gets the list of interceptors in this pipeline.
     *
     * @return the list of interceptors in this pipeline.
     */
    protected List getList()
    {
        return m_list ;
    }

    
    // ------------------------------------------------------------------------
    // Abstract Methods 
    // ------------------------------------------------------------------------
    
    
    /**
     * Invokes this InterceptorPipeline which sequencially begins the chain
     * of Interceptor invocations on the Interceptors with it. 
     *
     * @see org.apache.ldap.server.jndi.Interceptor#invoke(org.apache.ldap.server.jndi.Invocation)
     */
    public abstract void invoke( Invocation a_invocation )
        throws InterceptorException ;
        
    /**
     * Get whether this pipeline is fail fast or not.
     * 
     * @return whether or not this pipeline is failfast
     */
    public abstract boolean isFailFast() ;
}
