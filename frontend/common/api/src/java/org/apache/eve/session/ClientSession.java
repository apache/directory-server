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
package org.apache.eve.session ;


import java.util.Locale ;
import java.util.Iterator ;

import java.security.Principal ;

import org.apache.eve.listener.ClientKey ;


/**
 * Session interface for a client.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public interface ClientSession
{
    /**
     * Gets the unique client key associated with this session.
     * 
     * @return the unique client key
     */
    ClientKey getClientKey() ;

    /**
     * Gets whether or not this session is valid.  A valid session is one that
     * corresponds to a connected client.  Sessions are invalidated once the
     * client connection is lost or dropped or when sessions are destroyed to
     * bind as a different user.
     * 
     * @return true if this session is still valid, false if it is not 
     */
    boolean isValid() ;

    /**
     * Gets an iterator over all the attribute names stored in this session.
     * 
     * @return an iterator of key names
     */
    Iterator getAttributeNames() ;

    /**
     * Gets the value for a session attribute name.
     * 
     * @param an_attrName the name of the session attribute key
     * @return the value of the session attribute
     */
    Object getAttribute( String an_attrName ) ;

    /**
     * Removes a session attribute key and value from this session.
     * 
     * @param an_attrName the name of the session attribute key
     */
    void removeAttribute( String an_attrName ) ;

    /**
     * Sets the value of a session attribute.
     * 
     * @param an_attrName the name of the session attribute key
     * @param a_attrValue the value to set for the session attribute
     */
    void setAttribute( String an_attrName, Object a_attrValue ) ;

    /**
     * Determines if this session is newly created before an appropriate 
     * authenticated principal could be set.
     * 
     * @return true if the principal has yet to be authenticated
     */
    boolean isNew() ;

    /**
     * Gets the creation time of this session in milliseconds.
     * 
     * @return the creation time in milliseconds
     */
    long getCreationTime() ;

    /**
     * Gets the time this session was accessed in milliseconds.
     * 
     * @return the last access time in milliseconds
     */
    long getLastAccessedTime() ;

    /**
     * Gets the maximum inactivity time in seconds before timing out this 
     * session.
     * 
     * @return the timeout period in seconds
     */
    int getMaxInactiveInterval() ;

    /**
     * Gets the locale of the session's client.
     * 
     * @return the locale of the client
     */
    Locale getLocale() ;

    /**
     * Gets the principal for the client this session belongs to.  If the 
     * session is new or annonymous authentication is allowed then this 
     * principal will return the empty string for getName().
     * 
     * @return the principal owning this session
     */
    Principal getPrincipal() ;
}
