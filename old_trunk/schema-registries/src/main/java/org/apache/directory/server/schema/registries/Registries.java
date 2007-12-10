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


import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.NamingException;

import org.apache.directory.server.schema.bootstrap.Schema;


/**
 * Document this class.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Registries
{
    String getName();
    
    Map<String,Schema> getLoadedSchemas();
    
    void load( String schemaName ) throws NamingException;
    
    void load( String schemaName, Properties props ) throws NamingException;

    void unload( String schemaName ) throws NamingException;
    
    SchemaLoader setSchemaLoader();
    
    AttributeTypeRegistry getAttributeTypeRegistry();
    
    ComparatorRegistry getComparatorRegistry();

    DITContentRuleRegistry getDitContentRuleRegistry();

    DITStructureRuleRegistry getDitStructureRuleRegistry();

    MatchingRuleRegistry getMatchingRuleRegistry();

    MatchingRuleUseRegistry getMatchingRuleUseRegistry();

    NameFormRegistry getNameFormRegistry();

    NormalizerRegistry getNormalizerRegistry();

    ObjectClassRegistry getObjectClassRegistry();

    OidRegistry getOidRegistry();

    SyntaxCheckerRegistry getSyntaxCheckerRegistry();

    SyntaxRegistry getSyntaxRegistry();

    List<Throwable> checkRefInteg();

    Schema getSchema( String schemaName );

    /**
     * Removes a schema from the loaded set without unloading the schema.
     * This should be used ONLY when an enabled schema is deleted.
     * 
     * @param schemaName the name of the schema to remove
     */
    void removeFromLoadedSet( String schemaName );
    
    /**
     * Adds a schema to the loaded set but does not load the schema in 
     * question.  This may be a temporary fix for new schemas being added
     * which are enabled yet do not have any schema entities associated 
     * with them to load.  In this case all objects added under this 
     * schema will load when added instead of in bulk.
     * 
     * @param schema the schema object to add to the loaded set.
     */
    void addToLoadedSet( Schema schema );
}
