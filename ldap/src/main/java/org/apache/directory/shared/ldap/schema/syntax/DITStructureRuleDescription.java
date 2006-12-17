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
 * RFC 4512 - 4.1.7.  DIT Structure Rule Description
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DITStructureRuleDescription extends AbstractSchemaDescription
{

    private Integer ruleId;
    
    private String form;

    private List<Integer> superRules;


    public DITStructureRuleDescription()
    {
        ruleId = 0;
        form = null;
        superRules = new ArrayList<Integer>();
    }


    public String getForm()
    {
        return form;
    }


    public void setForm( String form )
    {
        this.form = form;
    }



    public Integer getRuleId()
    {
        return ruleId;
    }


    public void setRuleId( Integer ruleId )
    {
        this.ruleId = ruleId;
    }


    public List<Integer> getSuperRules()
    {
        return superRules;
    }


    public void setSuperRules( List<Integer> superRules )
    {
        this.superRules = superRules;
    }

    
    public void addSuperRule( Integer superRule )
    {
        superRules.add( superRule );
    }

}
