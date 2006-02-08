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
package org.apache.directory.server.core.schema;


import java.util.List;

import javax.naming.NamingException;


/**
 * Monitor used to track notable OidRegistry events.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface OidRegistryMonitor
{
    /**
     * Monitors situations where an OID is used to resolve an OID.  The caller
     * does not know that the argument is the same as the return value.
     * 
     * @param oid the OID argument and return value
     */
    void getOidWithOid( String oid );
    
    /**
     * Monitors when an OID is resolved successfully for a name.
     *  
     * @param name the name used to lookup an OID
     * @param oid the OID returned for the name
     */
    void oidResolved( String name, String oid );
    
    /**
     * Monitors when an OID is resolved successfully by using a normalized form
     * of the name.
     *  
     * @param name the name used to lookup an OID
     * @param normalized the normalized name that mapped to the OID
     * @param oid the OID returned for the name
     */
    void oidResolved( String name, String normalized, String oid );
    
    /**
     * Monitors when resolution of an OID by name fails.
     * 
     * @param name the name used to lookup an OID
     * @param fault the exception thrown for the failure after this call
     */
    void oidResolutionFailed( String name, NamingException fault );
    
    /**
     * Monitors when a name lookups fail due to the use of an unknown OID.
     *  
     * @param oid the OID used to lookup object names
     * @param fault the exception thrown for the failure after this call
     */
    void oidDoesNotExist( String oid, NamingException fault );
    
    /**
     * Monitors situations where a primary name is resolved for a OID.
     * 
     * @param oid the OID used for the lookup
     * @param primaryName the primary name found for the OID
     */
    void nameResolved( String oid, String primaryName );

    /**
     * Monitors situations where a names are resolved for a OID.
     * 
     * @param oid the OID used for the lookup
     * @param names the names found for the OID
     */
    void namesResolved( String oid, List names );
    
    /**
     * Monitors the successful registration of a name for an OID.
     * 
     * @param name the one of many names registered with an OID
     * @param oid the OID to be associated with the name
     */
    void registered( String name, String oid );
}
