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
 * RFC 4512 - 4.1.6.  DIT Content Rule Description
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DITContentRuleDescription extends AbstractSchemaDescription
{

    private List<String> auxiliaryObjectClasses;

    private List<String> mustAttributeTypes;

    private List<String> mayAttributeTypes;

    private List<String> notAttributeTypes;


    public DITContentRuleDescription()
    {
        auxiliaryObjectClasses = new ArrayList<String>();
        mustAttributeTypes = new ArrayList<String>();
        mayAttributeTypes = new ArrayList<String>();
        notAttributeTypes = new ArrayList<String>();
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


    public List<String> getAuxiliaryObjectClasses()
    {
        return auxiliaryObjectClasses;
    }


    public void setAuxiliaryObjectClasses( List<String> auxiliaryObjectClasses )
    {
        this.auxiliaryObjectClasses = auxiliaryObjectClasses;
    }


    public List<String> getNotAttributeTypes()
    {
        return notAttributeTypes;
    }


    public void setNotAttributeTypes( List<String> notAttributeTypes )
    {
        this.notAttributeTypes = notAttributeTypes;
    }


    public void addAuxiliaryAttributeType( String oid )
    {
        auxiliaryObjectClasses.add( oid );
    }


    public void addMustAttributeType( String oid )
    {
        mustAttributeTypes.add( oid );
    }


    public void addMayAttributeType( String oid )
    {
        mayAttributeTypes.add( oid );
    }


    public void addNotAttributeType( String oid )
    {
        notAttributeTypes.add( oid );
    }

}
