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
package org.apache.eve.listener ;


/**
 * A default server listener.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $LastChangedBy$
 * @version $LastChangedRevision$
 */
public class DefaultServerListener implements ServerListener
{
    /** the port used for the connection */
    private int m_port ;
    /** the connection backlog */
    private int m_backlog ;
    /** the protocol's URL scheme */
    private String m_scheme ;
    /** the interface address or hostname of the server */
    private byte[] m_address ;
    /** whether or not ssl is used to secure connections */
    private boolean m_isSecure ;
    /** whether or not the transport is reliable (TCP or UDP) */
    private boolean m_isReliable ;
    
    
    /**
     * Creates a default listener with all the supplied properties.
     * 
     * @param a_address the interface address or hostname of the server
     * @param a_scheme the URL scheme for the protocol
     * @param a_port the port used for the connection
     * @param a_backlog the connection backlog
     * @param a_isSecure whether or not ssl is used to secure connections
     * @param a_isReliable whether or not the transport is reliable (TCP or UDP)
     */
    public DefaultServerListener( byte[] a_address, String a_scheme, int a_port,
                                  int a_backlog, boolean a_isSecure, 
                                  boolean a_isReliable )
    {
        m_port = a_port ;
        m_scheme = a_scheme ;
        m_backlog = a_backlog ;
        m_address = a_address ;
        m_isSecure = a_isSecure ;
        m_isReliable = a_isReliable ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#getAddress()
     */
    public byte[] getAddress()
    {
        return m_address ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#getBacklog()
     */
    public int getBacklog()
    {
        return m_backlog ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#getPort()
     */
    public int getPort()
    {
        return m_port ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#isSecure()
     */
    public boolean isSecure()
    {
        return m_isSecure ;
    }


    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#getURL()
     */
    public String getURL()
    {
        StringBuffer l_buf = new StringBuffer() ;
        
        l_buf.append( m_scheme ) ;
        l_buf.append( "://" ) ;
        l_buf.append( m_address ) ;
        l_buf.append( ':' ) ;
        l_buf.append( m_port ) ;
        
        return l_buf.toString() ;
    }
    
    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#getProtocolUrlScheme()
     */
    public String getProtocolUrlScheme()
    {
        return m_scheme ;
    }

    
    /* (non-Javadoc)
     * @see org.apache.eve.listener.ServerListener#isReliable()
     */
    public boolean isReliable()
    {
        return m_isReliable ;
    }
    
    
    /**
     * Sets the address for the 
     * 
     * @param a_address The address to set.
     */
    protected void setAddress( byte[] a_address )
    {
        m_address = a_address ;
    }
    

    /**
     * @param a_backlog The backlog to set.
     */
    protected void setBacklog( int a_backlog )
    {
        m_backlog = a_backlog ;
    }

    
    /**
     * @param a_isReliable The isReliable to set.
     */
    protected void setReliable( boolean a_isReliable )
    {
        m_isReliable = a_isReliable ;
    }

    
    /**
     * @param a_isSecure The isSecure to set.
     */
    protected void setSecure( boolean a_isSecure )
    {
        m_isSecure = a_isSecure ;
    }

    
    /**
     * @param a_port The port to set.
     */
    protected void setPort( int a_port )
    {
        m_port = a_port ;
    }

    
    /**
     * @param a_scheme The scheme to set.
     */
    protected void setScheme( String a_scheme )
    {
        m_scheme = a_scheme ;
    }
}
