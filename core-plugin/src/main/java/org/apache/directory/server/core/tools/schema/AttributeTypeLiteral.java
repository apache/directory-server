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
package org.apache.directory.server.core.tools.schema;


import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.util.ArrayUtils;


/**
 * A bean used to hold the literal values of an AttributeType parsed out of an
 * OpenLDAP schema configuration file.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AttributeTypeLiteral
{
    private boolean obsolete = false;
    private boolean singleValue = false;
    private boolean collective = false;
    private boolean noUserModification = false;

    private final String oid;
    private String description;
    private String superior;
    private String equality;
    private String ordering;
    private String substr;
    private String syntax;

    private UsageEnum usage = UsageEnum.USER_APPLICATIONS;

    private String[] names = ArrayUtils.EMPTY_STRING_ARRAY;

    private int length = -1;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    public AttributeTypeLiteral(String oid)
    {
        this.oid = oid;
    }


    // ------------------------------------------------------------------------
    // Accessors and mutators
    // ------------------------------------------------------------------------

    public boolean isObsolete()
    {
        return obsolete;
    }


    public void setObsolete( boolean obsolete )
    {
        this.obsolete = obsolete;
    }


    public boolean isSingleValue()
    {
        return singleValue;
    }


    public void setSingleValue( boolean singleValue )
    {
        this.singleValue = singleValue;
    }


    public boolean isCollective()
    {
        return collective;
    }


    public void setCollective( boolean collective )
    {
        this.collective = collective;
    }


    public boolean isNoUserModification()
    {
        return noUserModification;
    }


    public void setNoUserModification( boolean noUserModification )
    {
        this.noUserModification = noUserModification;
    }


    public String getOid()
    {
        return oid;
    }


    public String getDescription()
    {
        return description;
    }


    public void setDescription( String description )
    {
        this.description = description;
    }


    public String getSuperior()
    {
        return superior;
    }


    public void setSuperior( String superior )
    {
        this.superior = superior;
    }


    public String getEquality()
    {
        return equality;
    }


    public void setEquality( String equality )
    {
        this.equality = equality;
    }


    public String getOrdering()
    {
        return ordering;
    }


    public void setOrdering( String ordering )
    {
        this.ordering = ordering;
    }


    public String getSubstr()
    {
        return substr;
    }


    public void setSubstr( String substr )
    {
        this.substr = substr;
    }


    public String getSyntax()
    {
        return syntax;
    }


    public void setSyntax( String syntax )
    {
        this.syntax = syntax;
    }


    public UsageEnum getUsage()
    {
        return usage;
    }


    public void setUsage( UsageEnum usage )
    {
        this.usage = usage;
    }


    public String[] getNames()
    {
        return names;
    }


    public void setNames( String[] names )
    {
        this.names = names;
    }


    public int getLength()
    {
        return length;
    }


    public void setLength( int length )
    {
        this.length = length;
    }


    // ------------------------------------------------------------------------
    // Object overrides
    // ------------------------------------------------------------------------

    public int hashCode()
    {
        return getOid().hashCode();
    }


    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !( obj instanceof AttributeTypeLiteral ) )
        {
            return false;
        }

        return getOid().equals( ( ( AttributeTypeLiteral ) obj ).getOid() );
    }


    public String toString()
    {
        return getOid();
    }
}
