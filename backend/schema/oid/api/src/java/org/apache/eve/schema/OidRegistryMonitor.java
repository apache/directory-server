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

import javax.naming.NamingException ;


/**
 * Monitor used to track notable OidRegistry events.
 *
 * @author <a href="mailto:akarasulu@apache.org">Alex Karasulu</a>
 * @author $Author$
 * @version $Rev$
 */
public interface OidRegistryMonitor
{
    /**
     * Monitors situations where an OID is used to resolve an OID.  The caller
     * does not know that the argument is the same as the return value.
     * 
     * @param a_oid the OID argument and return value
     */
    void getOidWithOid( String a_oid ) ;
    
    /**
     * Monitors when an OID is resolved successfully for a name.
     *  
     * @param a_name the name used to lookup an OID
     * @param a_oid the OID returned for the name
     */
    void oidResolved( String a_name, String a_oid ) ;
    
    /**
     * Monitors when an OID is resolved successfully by using a normalized form
     * of the name.
     *  
     * @param a_name the name used to lookup an OID
     * @param a_normalized the normalized name that mapped to the OID
     * @param a_oid the OID returned for the name
     */
    void oidResolved( String a_name, String a_normalized, String a_oid ) ;
    
    /**
     * Monitors when resolution of an OID by name fails.
     * 
     * @param a_name the name used to lookup an OID
     * @param a_fault the exception thrown for the failure after this call
     */
    void oidResolutionFailed( String a_name, NamingException a_fault ) ;
    
    /**
     * Monitors when a name lookups fail due to the use of an unknown OID.
     *  
     * @param a_oid the OID used to lookup object names
     * @param a_fault the exception thrown for the failure after this call
     */
    void oidDoesNotExist( String a_oid, NamingException a_fault ) ;
    
    /**
     * Monitors situations where a primary name is resolved for a OID.
     * 
     * @param a_oid the OID used for the lookup
     * @param a_primaryName the primary name found for the OID
     */
    void nameResolved( String a_oid, String a_primaryName ) ;

    /**
     * Monitors situations where a names are resolved for a OID.
     * 
     * @param a_oid the OID used for the lookup
     * @param a_names the names found for the OID
     */
    void namesResolved( String a_oid, List a_names ) ;
    
    /**
     * Monitors the successful registration of a name for an OID.
     * 
     * @param a_name the one of many names registered with an OID
     * @param a_oid the OID to be associated with the name
     */
    void registered( String a_name, String a_oid ) ;
}
