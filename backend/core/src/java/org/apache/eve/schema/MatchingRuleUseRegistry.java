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

import org.apache.ldap.common.schema.MatchingRuleUse;


/**
 * An MatchingRuleUse registry service interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
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
     * Checks to see if an matchingRuleUse exists.
     * 
     * @param name the name of the matchingRuleUse
     * @return true if an matchingRuleUse definition exists for the name, false
     * otherwise
     */
    boolean hasMatchingRuleUse( String name );
}
