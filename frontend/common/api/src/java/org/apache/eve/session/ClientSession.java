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
package org.apache.eve.session ;


import java.util.Locale ;
import java.util.Iterator ;

import java.security.Principal ;

import org.apache.eve.listener.ClientKey ;


/**
 * Session interface for a client.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
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
