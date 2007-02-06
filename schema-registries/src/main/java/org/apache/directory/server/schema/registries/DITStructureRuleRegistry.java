/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.schema.registries;


import java.util.Iterator;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.schema.DITStructureRule;


/**
 * An DITStructureRule registry service interface.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface DITStructureRuleRegistry extends SchemaObjectRegistry
{
    /**
     * Registers a DITStructureRule with this registry.
     * 
     * @param dITStructureRule the dITStructureRule to register
     * @throws NamingException if the DITStructureRule is already registered
     * or the registration operation is not supported
     */
    void register( DITStructureRule dITStructureRule ) throws NamingException;


    /**
     * Looks up an dITStructureRule using a composite key composed of the
     * nameForm object identifier with a DOT and the rule id of the 
     * DITStructureRule appended to it.  If the name form object identifier
     * is 1.2.3.4 and the rule identifier is 5 then the OID used for the 
     * lookup is 1.2.3.4.5.
     * 
     * @param id the nameForm object identifier with rule identifier appended
     * @return the DITStructureRule instance for the id
     * @throws NamingException if the DITStructureRule does not exist
     */
    DITStructureRule lookup( String id ) throws NamingException;


    /**
     * Looks up an dITStructureRule by its unique Object IDentifier or by its
     * name.
     * 
     * @param ruleId the rule identifier for the DITStructureRule
     * @return the DITStructureRule instance for rule identifier
     * @throws NamingException if the DITStructureRule does not exist
     */
    DITStructureRule lookup( Integer ruleId ) throws NamingException;


    /**
     * Checks to see if an dITStructureRule exists using the object identifier
     * of the nameForm appended with the rule identifier of the DITStructureRule.
     * 
     * @param id the object identifier of the nameForm with the rule Id appended
     * @return true if an dITStructureRule definition exists for the id, false
     * otherwise
     */
    boolean hasDITStructureRule( String id );


    /**
     * Checks to see if an dITStructureRule exists using the rule identifier.
     * 
     * @param ruleId the rule identifier for the DITStructureRule.
     * @return true if an dITStructureRule definition exists for the id, false
     * otherwise
     */
    boolean hasDITStructureRule( Integer ruleId );

    
    /**
     * Unregisters a DITStructureRule using it's rule identifier. 
     * 
     * @param ruleId the rule identifier for the DITStructureRule to unregister
     * @throws NamingException if no such DITStructureRule exists
     */
    void unregister( Integer ruleId ) throws NamingException;
    
    
    /**
     * Gets the schema name for a DITStructureRule using the rule identifier. 
     * 
     * @param ruleId the rule identifier for the DITStructureRule
     * @return the schema name for the DITStructureRule
     * @throws NamingException if no such rule could be found
     */
    String getSchemaName( Integer ruleId ) throws NamingException;


    /**
     * Lists all the DITStructureRules within this registry.
     *
     * @return an Iterator over all the DITStructureRules within this registry
     */
    Iterator<DITStructureRule> iterator();
}
