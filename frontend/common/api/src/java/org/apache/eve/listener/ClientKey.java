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


import java.net.Socket ;

import java.io.IOException ;


/**
 * Every client that successfully binds anonymously or with a valid identity
 * has a unique client key represented by this class.  The key uniquely 
 * identifies the client based on the connection parameters: interface and port 
 * used on the server as well as the interface and port used by the client.
 * <p>
 * The ClientKey plays a central role in coordinating activities with the
 * server across various threads.  Threads within the same stage or across
 * stages are synchronized on client resources using lock objects held by a
 * ClientKey instance.  Socket IO is managed using a pair of lock objects
 * specificially for this purpose.
 * </p>
 * 
 * @todo do we really need these lock objects?
 * @todo why are we carrying around the damn socket?
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public final class ClientKey
{
    // ----------------------------------------------
    // Private members.
    // ----------------------------------------------

    /** Input channel synchronization object */
    private final Object m_inputLock = new Object() ;
    /** Output channel synchronization object */
    private final Object m_outputLock = new Object() ;
    /** Unique key or client id */
    private final String m_clientId ;
    /** Socket connection to client */
    private final Socket m_socket ;
    
    /** Whether or not this key has expired: the client has disconnected. */
    private boolean m_hasExpired = false ;


    // ----------------------------------------------
    // Constructors
    // ----------------------------------------------


    /**
      * Generates a unique connection/client identifier String for a client
      * socket connection.  The key is composed of the local server address
      * and port attached to the remote client address and port.  If the
      * server ip and port are 192.168.1.1:1389 and the client's ip and port are
      * 34.23.12.1:5678 then the key string would be:
      *
      * 192.168.1.1:1389<-34.23.12.1:5678
      *
      * This makes the key unique at any single point in time.
      *
      * @param a_socket newly established client socket connection to the
      * server.
      */
    ClientKey( final Socket a_socket )
    {
        // build the key ...
        StringBuffer l_buf = new StringBuffer() ;
        l_buf.append( a_socket.getLocalAddress().getHostAddress() ) ;
        l_buf.append( ':' ) ;
        l_buf.append( a_socket.getLocalPort() ).append( "<-" ) ;
        l_buf.append( a_socket.getInetAddress().getHostAddress() ) ;
        l_buf.append( ':' ) ;
        l_buf.append( a_socket.getPort() ) ;
        
        // set finals ...
        m_clientId = l_buf.toString() ;
        m_socket = a_socket ;
    }


    // ----------------------------------------------
    // Accessors of conn. parameters to client id
    // ----------------------------------------------

    
    /**
     * Get the unique client id for a connected client based on connection
     * parameters.
     *
     * @return the unique id of the client connection
     * @throws KeyExpiryException to force the handling of expired keys rather
     * than depending on developers to maintain a convention of checking for
     * key expiration before use in other modules.
     */
    public String getClientId() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_clientId ;
    }
    
    
    /**
     * Gets the clients socket connection.
     * 
     * @return the client's socket connection
     */
    public Socket getSocket() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_socket ; 
    }


    /**
     * Gets the client's IP address.
     *
     * @return the client's ip address.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public String getClientAddress() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_socket.getInetAddress().getHostAddress() ; 
    }


    /**
     * Gets the client's hostname.
     *
     * @return the client's hostname.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public String getClientHost() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_socket.getInetAddress().getHostName() ;
    }


    // ----------------------------------------------
    // ClientKey lock object accessors.
    // ----------------------------------------------


    /**
     * Gets the client's output stream lock object.
     *
     * @return ouput lock object.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public Object getOutputLock() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_outputLock ;
    }


    /**
     * Gets the client's input stream lock object.
     *
     * @return input lock object.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public Object getInputLock() throws KeyExpiryException
    {
        checkExpiry() ;
        return m_inputLock ;
    }


    // ----------------------------------------------
    // Key expiration methods.
    // ----------------------------------------------


    /**
     * Determines if the client represented by this ClientKey is still
     * connected to the server.  Once disconnected the ClientKey is expired
     * by the server so processing on behalf of the client does not continue.
     *
     * @return true if the client is no longer connected to the server, false
     * if the client is connected.
     */
    public boolean hasExpired()
    {
        return m_hasExpired ;
    }


    /**
     * Expires this key to indicate the disconnection of the client represented
     * by this key from the server.  It is intentionally package friendly to
     * only allow access by the ClientModule.  Tries to close socket if it is
     * still open.
     */
    void expire() throws IOException
    {
        m_hasExpired = true ;
        
        if ( null != m_socket )
        {
            m_socket.close() ;
        }
    }


    /**
     * Utility method to throw key expiration exception if this ClientKey has
     * expired.  This method is called by most accessor methods within this
     * class with <code>hasExpired()</code> being the only exception.  The
     * purpose for this is to force ClientKey using modules to check for
     * expiration rather rely upon them to check to see if the key is valid
     * before use everytime.
     * 
     * @throws KeyExpiryException to force the handling of expired keys rather
     * than depending on developers to maintain a convention of checking for
     * key expiration before use in other modules.
     */
    void checkExpiry() throws KeyExpiryException
    {
        if( m_hasExpired ) 
        {
            throw new KeyExpiryException( this ) ;
        }
    }


    // ----------------------------------------------
    // Class java.lang.Object method overrides.
    // ----------------------------------------------


    /**
     * For debugging returns the clientId string.
     *
     * @return the client id string.
     */
    public String toString()
    {
        return m_clientId ;
    }


    /**
     * Gets the hashCode of the unique clientId String.  Overriden to correctly
     * manage ClientKey's within Map based collections.
     *
     * @return the clientId hashCode value.
     */
    public int hashCode()
    {
        return m_clientId.hashCode() ;
    }


    /**
     * Determines whether this ClientKey is equivalent to another.  If argument
     * object is not the same reference the clientId String's are compared using
     * the <code>String.equal()</code> method.  Required for containment within
     * collections.  Also note that if Strings are supplied or other classes are
     * supplied as arguments String equality checks are conducted on the client
     * id String.
     *
     * @return true if an_obj equals this ClientKey, false otherwise.
     */
    public boolean equals( Object an_obj )
    {
        if( this == an_obj ) 
        {
            return true ;
        } 
        else if( an_obj instanceof String )
        {
            return m_clientId.equals( an_obj ) ;
        }
        else if( an_obj instanceof ClientKey ) 
        {
            return ( ( ClientKey ) an_obj ).m_clientId.equals( m_clientId ) ;
        }
        else
        {
            return m_clientId.equals( an_obj.toString() ) ;
        }
    }
}
