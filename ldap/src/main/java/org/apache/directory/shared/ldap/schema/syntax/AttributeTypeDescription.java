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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.ldap.schema.UsageEnum;
 
 
/**
 * RFC 4512 - 4.1.2. Attribute Types
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AttributeTypeDescription
{
    private String numericOid;
 
    private List<String> names;
 
    private String description;
 
    private boolean isObsolete;
 
    private String superType;
 
    private String equalityMatchingRule;
 
    private String orderingMatchingRule;
 
    private String substringsMatchingRule;
 
    private String syntax;
 
    private int syntaxLength;
 
    private boolean isSingleValued;
 
    private boolean isCollective;
 
    private boolean isUserModifiable;
 
    private UsageEnum usage;
 
    private Map<String, List<String>> extensions;
 
 
    public AttributeTypeDescription( )
    {
        this.numericOid = "";
        names = new ArrayList<String>();
        description = "";
        isObsolete = false;
        superType = null;
        equalityMatchingRule = null;
        orderingMatchingRule = null;
        substringsMatchingRule = null;
        syntax = null;
        syntaxLength = 0;
        isSingleValued = false;
        isCollective = false;
        isUserModifiable = true;
        usage = UsageEnum.USER_APPLICATIONS;
        extensions = new LinkedHashMap<String, List<String>>();;
    }
 
 
    public String getDescription()
    {
        return description;
    }
 
 
    public void setDescription( String description )
    {
        this.description = description;
    }
 
 
    public String getEqualityMatchingRule()
    {
        return equalityMatchingRule;
    }
 
 
    public void setEqualityMatchingRule( String equalityMatchingRule )
    {
        this.equalityMatchingRule = equalityMatchingRule;
    }
 
 
    public Map<String, List<String>> getExtensions()
    {
        return extensions;
    }
 
 
    public void setExtensions( Map<String, List<String>> extensions )
    {
        this.extensions = extensions;
    }
 
 
    public boolean isCollective()
    {
        return isCollective;
    }
 
 
    public void setCollective( boolean isCollective )
    {
        this.isCollective = isCollective;
    }
 
 
    public boolean isObsolete()
    {
        return isObsolete;
    }
 
 
    public void setObsolete( boolean isObsolete )
    {
        this.isObsolete = isObsolete;
    }
 
 
    public boolean isUserModifiable()
    {
        return isUserModifiable;
    }
 
 
    public void setUserModifiable( boolean isUserModifiable )
    {
        this.isUserModifiable = isUserModifiable;
    }
 
 
    public List<String> getNames()
    {
        return names;
    }
 
 
    public void setNames( List<String> names )
    {
        this.names = names;
    }
 
 
    public String getNumericOid()
    {
        return numericOid;
    }
 
 
    public void setNumericOid( String oid )
    {
        this.numericOid = oid;
    }
 
 
    public String getOrderingMatchingRule()
    {
        return orderingMatchingRule;
    }
 
 
    public void setOrderingMatchingRule( String orderingMatchingRule )
    {
        this.orderingMatchingRule = orderingMatchingRule;
    }
 
 
    public boolean isSingleValued()
    {
        return isSingleValued;
    }
 
 
    public void setSingleValued( boolean singleValued )
    {
        this.isSingleValued = singleValued;
    }
 
 
    public String getSubstringsMatchingRule()
    {
        return substringsMatchingRule;
    }
 
 
    public void setSubstringsMatchingRule( String substringsMatchingRule )
    {
        this.substringsMatchingRule = substringsMatchingRule;
    }
 
 
    public String getSuperType()
    {
        return superType;
    }
 
 
    public void setSuperType( String superType )
    {
        this.superType = superType;
    }
 
 
    public String getSyntax()
    {
        return syntax;
    }
 
 
    public void setSyntax( String syntax )
    {
        this.syntax = syntax;
    }
 
 
    public int getSyntaxLength()
    {
        return syntaxLength;
    }
 
 
    public void setSyntaxLength( int syntaxLenght )
    {
        this.syntaxLength = syntaxLenght;
    }
 
 
    public UsageEnum getUsage()
    {
        return usage;
    }
 
 
    public void setUsage( UsageEnum usage )
    {
        this.usage = usage;
    }
    
    public void addExtension( String key, List<String> values )
    {
        this.extensions.put( key, values );
    }
 
}