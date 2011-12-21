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
import java.util.List;
import java.util.Properties;

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
     * Schema name which @schemaElements will go under.
     */
    private String parentSchemaDn = null;

    /*
     * The object class which will be used to represent instances of the component.
     */
    private String objectClassForComponent = null;


    public ADSComponentSchema( String parentSchema, List<LdifEntry> elements, String oc )
    {
        parentSchemaDn = parentSchema;
        schemaElements = elements;
        objectClassForComponent = oc;
    }


    /**
     * Returns name of the schema
     *
     * @return name of the schema
     */
    public String getParentSchemaDn()
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
     * Getter for OC to represent instances.
     *
     * @return
     */
    public String getOCName()
    {
        return objectClassForComponent;
    }

}
