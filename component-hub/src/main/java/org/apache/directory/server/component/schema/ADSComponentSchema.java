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
package org.apache.directory.server.component.schema;


import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.directory.shared.ldap.model.ldif.LdifEntry;


/**
 * Class that represents an ADSComponent's schema definition.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ADSComponentSchema
{
    /*
     * Schema elements (attribs,ocs) for component.
     */
    private List<LdifEntry> schemaElements = null;

    /*
     * True if the @schemaElements contains complete schema definition with schema entry.
     */
    private boolean completeSchema = false;

    /*
     * Schema name which @schemaElements will go under.
     */
    private String parentSchemaDn = null;

    /*
     * The default attribute/property values for this schemas associated component to be instantiated
     */
    private Dictionary defaultConf = null;


    public ADSComponentSchema( String name, List<LdifEntry> elements )
    {
        this( name, elements, false );
    }


    public ADSComponentSchema( String parentSchema, List<LdifEntry> elements, boolean complete )
    {
        parentSchemaDn = parentSchema;
        schemaElements = elements;
        completeSchema = complete;
    }


    /**
     * Getter for completeSchema
     *
     * @return true if schema is complete with schema entry.
     */
    public boolean ifCompleteSchema()
    {
        return completeSchema;
    }


    /**
     * Returns name of the schema
     *
     * @return name of the schema
     */
    public String getParentSchemaName()
    {
        return parentSchemaDn;
    }


    /**
     * Gets the clone of the schema elements.
     *
     * @return clone of the schema elements.
     */
    public List<LdifEntry> getSchemaElements()
    {
        if ( schemaElements == null )
        {
            return null;
        }

        return new ArrayList<LdifEntry>( schemaElements );
    }


    /**
     * Setter for default configuration
     *
     * @param conf Dictionary containing property name -> value mapping
     */
    public void setDefaultConf( Dictionary conf )
    {
        defaultConf = conf;
    }


    /**
     * Getter for default configuration
     *
     * @param conf Dictionary containing property name -> value mapping
     */
    public Dictionary getDefaultConf()
    {
        return defaultConf;
    }

}
