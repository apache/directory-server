/*
 * $Id: ClientKey.java,v 1.5 2003/08/22 21:15:55 akarasulu Exp $
 *
 * -- (c) LDAPd Group                                                    --
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */

package org.apache.eve.client ;


import java.net.Socket ;
import java.net.InetAddress ;


/**
 * Every client that successfully binds anonymously or with a valid identity
 * has a unique client key represented by this class.  First and foremost the
 * key is used to uniquely identify the client based on the interface and
 * port used to connection on the server as well as the interface and port used
 * by the client.
 *
 * The ClientKey plays a central role in coordinating activities with the
 * server across various threads.  Threads within the same stage or across
 * stages are synchronized on client resources using lock objects held by a
 * ClientKey instance.  Socket IO is managed using a pair of lock objects
 * specificially for this purpose.  As the need arises more lock objects may be
 * used for specific client resources like a session object.  These extra lock
 * objects are now being discussed.
 *
 * @author <a href="mailto:aok123@bellsouth.net">Alex Karasulu</a>
 * @author $Author: akarasulu $
 * @version $Revision: 1.5 $
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
    /** Server socket interface address */
    private final InetAddress m_serverAddress ;
    /** Client socket interface address */
    private final InetAddress m_clientAddress ;
	/** Whether or not this key has expired: the client has disconnected. */
    private boolean m_hasExpired = false ;

    // ----------------------------------------------
    // pkg-friendly final members: made so the public
    // accessors do not need to be used by classes in
    // this pkg - need access to these members for
    // cleanup operations after expiration.  Do not
    // want to unnecessarily throw an exception.
    // ----------------------------------------------

	/** Unique key or client id */
    final String m_clientId ;
    /** Server socket TCP port on server host */
    final int m_serverPort ;
    /** Client socket TCP port on client host */
    final int m_clientPort ;


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
        // Extract properties needed to formulate the client id ( key ).
        m_serverPort = a_socket.getLocalPort() ;
        m_serverAddress = a_socket.getLocalAddress() ;
        m_clientPort = a_socket.getPort() ;
        m_clientAddress = a_socket.getInetAddress() ;

        // Build the key
        StringBuffer l_buf =
            new StringBuffer(m_serverAddress.getHostAddress()) ;
        l_buf.append(':').append(m_serverPort).append("<-") ;
        l_buf.append(m_clientAddress.getHostAddress()).append(':') ;
        l_buf.append(m_clientPort) ;
        m_clientId = l_buf.toString() ;
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
    public String getClientId()
        throws KeyExpiryException
    {
        checkExpiry() ;
        return m_clientId ;
    }


    /**
     * Gets the client's IP address.
     *
     * @return the client's ip address.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public String getClientAddress()
        throws KeyExpiryException
    {
        checkExpiry() ;
		return m_clientAddress.getHostAddress() ;
    }


    /**
     * Gets the client's hostname.
     *
     * @return the client's hostname.
     * @throws KeyExpiryException to force the handling of expired keys
     */
    public String getClientHost()
        throws KeyExpiryException
    {
        checkExpiry() ;
        return m_clientAddress.getHostName() ;
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
    public Object getOutputLock()
        throws KeyExpiryException
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
    public Object getInputLock()
        throws KeyExpiryException
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
     * only allow access by the ClientModule.
     */
    void expire()
    {
        m_hasExpired = true ;
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
    private void checkExpiry()
        throws KeyExpiryException
    {
        if(m_hasExpired) {
            throw new KeyExpiryException() ;
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
     * collections.
     *
     * @return true if an_obj equals this ClientKey, false otherwise.
     */
    public boolean equals(Object an_obj)
    {
        if(this == an_obj) {
            return true ;
        } else if(an_obj instanceof ClientKey) {
            return ((ClientKey) an_obj).m_clientId.equals(m_clientId) ;
        }

        return false ;
    }
}
