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
package org.apache.eve.schema ;


import java.util.List ;
import java.util.Iterator ;

import javax.naming.NamingException ;


/**
 * Object identifier registry.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface OidRegistry
{
    /** Avalon service interface ROLE */
    String ROLE = OidRegistry.class.toString() ;
    
    /**
     * Gets the object identifier using a common name
     * 
     * @param a_name the name to lookup an OID for
     * @return the OID string associated with a name
     * @throws NamingException if a_name does not map to an OID
     */
    String getOid( String a_name ) throws NamingException ;
    
    /**
     * Gets the primary name associated with an OID.  The primary name is the
     * first name specified for the OID.
     * 
     * @param a_oid the object identifier
     * @return the primary name
     * @throws NamingException if a_oid does not exist
     */
    String getPrimaryName( String a_oid ) throws NamingException ;
    
    /**
     * Gets the names associated with an OID.  An OID is unique however it may 
     * have many names used to refer to it.  A good example is the cn and
     * commonName attribute names for OID 2.5.4.3.  Within a server one name 
     * within the set must be chosen as the primary name.  This is used to
     * name certain things within the server internally.  If there is more than
     * one name then the first name is taken to be the primary.
     * 
     * @param a_oid the OID for which we return the set of common names
     * @return a sorted set of names
     * @throws NamingException if a_oid does not exist
     */
    List getNameSet( String a_oid ) throws NamingException ;
    
    /**
     * Lists all the OIDs within the registry.  This may be a really big list.
     * 
     * @return all the OIDs registered
     */
    Iterator list() ;
    
    /**
     * Adds an OID name pair to the registry.
     * 
     * @param a_name the name to associate with the OID
     * @param a_oid the OID to add or associate a new name with 
     */
    void register( String a_name, String a_oid ) ;
}
