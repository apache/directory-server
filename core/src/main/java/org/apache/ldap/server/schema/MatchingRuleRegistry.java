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
package org.apache.ldap.server.schema;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.ldap.common.schema.MatchingRule;


/**
 * A registry used to track system matchingRules.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleRegistry
{
    /**
     * Registers a MatchingRule with this registry.
     * 
     * @param schema the name of the schema the MatchingRule is associated with
     * @param matchingRule the MatchingRule to register
     * @throws NamingException if the matchingRule is already registered or the 
     * registration operation is not supported
     */
    void register( String schema, MatchingRule matchingRule ) throws NamingException;
    
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
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String id ) throws NamingException;

    /**
     * Checks to see if a MatchingRule exists.  Backing store failures simply 
     * return false.
     * 
     * @param oid the object identifier
     * @return true if a MatchingRule definition exists for the oid, false 
     * otherwise
     */
    boolean hasMatchingRule( String oid );

    /**
     * Gets an Iterator over the MatchingRules within this registry.
     *
     * @return an iterator over all MatchingRules in registry
     */
    Iterator list();
}
