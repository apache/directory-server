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
package org.apache.eve.schema;


import javax.naming.NamingException;

import org.apache.ldap.common.schema.MatchingRule;


/**
 * A registry used to track system matchingRules.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleRegistry
{
    /**
     * Registers a MatchingRule with this registry.
     * 
     * @param matchingRule the MatchingRule to register
     * @throws NamingException if the matchingRule is already registered or the 
     * registration operation is not supported
     */
    void register( MatchingRule matchingRule ) throws NamingException;
    
    /**
     * Looks up a MatchingRule by its unique Object Identifier or by name.
     * 
     * @param id the object identifier or the name identifier
     * @return the MatchingRule for the id
     * @throws NamingException if there is a backing store failure or the 
     * MatchingRule does not exist.
     */
    MatchingRule lookup( String id ) throws NamingException;

    /**
     * Checks to see if a MatchingRule exists.  Backing store failures simply 
     * return false.
     * 
     * @param oid the object identifier
     * @return true if a MatchingRule definition exists for the oid, false 
     * otherwise
     */
    boolean hasMatchingRule( String oid );
}
