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
package org.apache.directory.shared.ldap.schema.parsers;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class AbstractSchemaDescription
{
	/** The schema element numeric OID */
    protected String numericOid;
    
    /** The schema element list of short names */
    protected List<String> names;
    
    /** A description for this schema element */
    protected String description;
    
    /** Tells if this schema element is obsolte */
    protected boolean isObsolete;
    
    /** A map containing the list of supported extensions */
    protected Map<String, List<String>> extensions;


    protected AbstractSchemaDescription()
    {
        numericOid = null;
        names = new ArrayList<String>();
        description = null;
        isObsolete = false;
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


    public boolean isObsolete()
    {
        return isObsolete;
    }


    public void setObsolete( boolean isObsolete )
    {
        this.isObsolete = isObsolete;
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


    public void addExtension( String key, List<String> values )
    {
        extensions.put( key, values );
    }


    /**
     * Compute the instance's hash code
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        return numericOid.hashCode();
    }


    public boolean equals( Object obj )
    {
        if ( ! ( obj instanceof AbstractSchemaDescription ) )
        {
            return false;
        }

        return ( ( AbstractSchemaDescription ) obj ).numericOid.equals( numericOid );
    }
}
