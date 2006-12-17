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

package org.apache.directory.shared.ldap.schema.syntax;


import java.util.ArrayList;
import java.util.List;


/**
 * RFC 4512 - 4.1.7.2.  Name Form Description
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class NameFormDescription extends AbstractSchemaDescription
{

    private String structuralObjectClass;
    
    private List<String> mustAttributeTypes;

    private List<String> mayAttributeTypes;


    public NameFormDescription()
    {
        structuralObjectClass = null;
        mustAttributeTypes = new ArrayList<String>();
        mayAttributeTypes = new ArrayList<String>();
    }


    public List<String> getMayAttributeTypes()
    {
        return mayAttributeTypes;
    }


    public void setMayAttributeTypes( List<String> mayAttributeTypes )
    {
        this.mayAttributeTypes = mayAttributeTypes;
    }


    public List<String> getMustAttributeTypes()
    {
        return mustAttributeTypes;
    }


    public void setMustAttributeTypes( List<String> mustAttributeTypes )
    {
        this.mustAttributeTypes = mustAttributeTypes;
    }


    public void addMustAttributeType( String oid )
    {
        mustAttributeTypes.add( oid );
    }


    public void addMayAttributeType( String oid )
    {
        mayAttributeTypes.add( oid );
    }


    public String getStructuralObjectClass()
    {
        return structuralObjectClass;
    }


    public void setStructuralObjectClass( String structuralObjectClass )
    {
        this.structuralObjectClass = structuralObjectClass;
    }

}
