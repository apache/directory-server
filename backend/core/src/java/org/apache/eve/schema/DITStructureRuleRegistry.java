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

import org.apache.ldap.common.schema.DITStructureRule;


/**
 * An DITStructureRule registry's service interface.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITStructureRuleRegistry
{
    /**
     * Registers a Comparator with this registry.
     * 
     * @param dITStructureRule the dITStructureRule to register
     * @throws NamingException if the DITStructureRule is already
     * registered or the registration operation is not supported
     */
    void register( DITStructureRule dITStructureRule ) throws NamingException;
    
    /**
     * Looks up an dITStructureRule by its unique Object Identifier.
     * 
     * @param oid the object identifier
     * @return the DITStructureRule instance for the oid
     * @throws NamingException if the DITStructureRule does not exist
     */
    DITStructureRule lookup( String oid ) throws NamingException;

    /**
     * Checks to see if an dITStructureRule exists.
     * 
     * @param oid the object identifier
     * @return true if an dITStructureRule definition exists for the oid, false
     * otherwise
     */
    boolean hasDITStructureRule( String oid );
}
