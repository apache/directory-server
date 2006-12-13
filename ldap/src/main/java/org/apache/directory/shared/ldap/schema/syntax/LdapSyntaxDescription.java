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

 
 
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
 
 
/**
 * RFC 4512 - 4.1.5. LDAP Syntaxes
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LdapSyntaxDescription
{
 
    private String numericOid;
 
    private String description;
 
    private Map<String, List<String>> extensions;
 
 
    public LdapSyntaxDescription( )
    {
        this.numericOid = "";
        description = "";
        extensions = new LinkedHashMap<String, List<String>>();
    }
 
 
    public String getDescription()
    {
        return description;
    }
 
 
    public void setDescription( String description )
    {
        this.description = description;
    }
 
 
    public Map<String, List<String>> getExtensions()
    {
        return extensions;
    }
 
 
    public void setExtensions( Map<String, List<String>> extensions )
    {
        this.extensions = extensions;
    }
 
 
    public String getNumericOid()
    {
        return numericOid;
    }
 
 
    public void setNumericOid( String oid )
    {
        numericOid = oid;
    }
 
    
    public void addExtension( String key, List<String> values )
    {
        extensions.put( key, values );
    }
 
}