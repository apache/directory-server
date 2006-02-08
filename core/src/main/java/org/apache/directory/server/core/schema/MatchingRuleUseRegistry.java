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


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.MatchingRuleUse;


/**
 * A MatchingRuleUse registry service interface.  MatchingRuleUse objects are
 * special in that they do not have unique OID's specifically assigned to them.
 * Their OID is really the OID of the MatchingRule they refer to.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface MatchingRuleUseRegistry
{
    /**
     * Registers a MatchingRuleUse with this registry.
     *
     * @param schema the name of the schema the MatchingRuleUse is associated with
     * @param matchingRuleUse the matchingRuleUse to register
     * @throws NamingException if the MatchingRuleUse is already registered or
     * the registration operation is not supported
     */
    void register( String schema, MatchingRuleUse matchingRuleUse ) throws NamingException;
    
    /**
     * Looks up an matchingRuleUse by its name.
     * 
     * @param name the name of the matchingRuleUse
     * @return the MatchingRuleUse instance for the name
     * @throws NamingException if the MatchingRuleUse does not exist
     */
    MatchingRuleUse lookup( String name ) throws NamingException;

    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param name the name String
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String name ) throws NamingException;

    /**
     * Checks to see if an matchingRuleUse exists.
     * 
     * @param name the name of the matchingRuleUse
     * @return true if an matchingRuleUse definition exists for the name, false
     * otherwise
     */
    boolean hasMatchingRuleUse( String name );

    /**
     * Lists all the MatchingRuleUses within this registry.
     *
     * @return an Iterator over all the MatchingRuleUses within this registry
     */
    Iterator list();
}
