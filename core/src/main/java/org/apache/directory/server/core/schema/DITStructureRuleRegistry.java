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

import org.apache.directory.shared.ldap.schema.DITStructureRule;


/**
 * An DITStructureRule registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITStructureRuleRegistry
{
    /**
     * Registers a DITStructureRule with this registry.
     * 
     * @param schema the name of the schema the DITStructureRule is associated with
     * @param dITStructureRule the dITStructureRule to register
     * @throws NamingException if the DITStructureRule is already registered
     * or the registration operation is not supported
     */
    void register( String schema, DITStructureRule dITStructureRule ) throws NamingException;
    
    /**
     * Looks up an dITStructureRule by its unique Object IDentifier or by its
     * name.
     * 
     * @param id the object identifier, or the name
     * @return the DITStructureRule instance for the id
     * @throws NamingException if the DITStructureRule does not exist
     */
    DITStructureRule lookup( String id ) throws NamingException;

    /**
     * Gets the name of the schema this schema object is associated with.
     *
     * @param id the object identifier or the name
     * @return the schema name
     * @throws NamingException if the schema object does not exist
     */
    String getSchemaName( String id ) throws NamingException;

    /**
     * Checks to see if an dITStructureRule exists.
     * 
     * @param id the object identifier, or the name
     * @return true if an dITStructureRule definition exists for the id, false
     * otherwise
     */
    boolean hasDITStructureRule( String id );

    /**
     * Lists all the DITStructureRules within this registry.
     *
     * @return an Iterator over all the DITStructureRules within this registry
     */
    Iterator list();
}
