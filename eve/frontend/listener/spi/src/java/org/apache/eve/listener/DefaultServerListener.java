/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2002 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Eve Directory Server", "Apache Directory Project", "Apache Eve" 
    and "Apache Software Foundation"  must not be used to endorse or promote
    products derived  from this  software without  prior written
    permission. For written permission, please contact apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

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
