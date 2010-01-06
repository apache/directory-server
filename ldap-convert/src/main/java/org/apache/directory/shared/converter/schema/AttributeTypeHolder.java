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
package org.apache.directory.shared.converter.schema;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.UsageEnum;


/**
 * A bean used to hold the literal values of an AttributeType parsed out of an
 * OpenLDAP schema configuration file.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 476875 $
 */
public class AttributeTypeHolder extends SchemaElementImpl
{
    /** A flag for single valued attributes. Default to false */
    private boolean singleValue = false;

    /** A flag for collective attribute. Default to false */
    private boolean collective = false;

    /** A flaf for immutable attribue. Default to false */
    private boolean noUserModification = false;

    /** The optional superior */
    private String superior;

    /** The equality matching rule */
    private String equality;

    /** The ordering matching rule */
    private String ordering;

    /** The substring matching rule */
    private String substr;

    /** The syntax this attribute respects */
    private String syntax;

    /** The optional length for this attribute */
    private int oidLen = -1;

    /** The attribute uase. Default to userApplication */
    private UsageEnum usage = UsageEnum.USER_APPLICATIONS;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Create an instance of an attributeType
     * 
     * @param oid
     *            The attributeType's OID
     */
    public AttributeTypeHolder( String oid )
    {
        this.oid = oid;
    }


    // ------------------------------------------------------------------------
    // Accessors and mutators
    // ------------------------------------------------------------------------

    /**
     * Tells if the attribute is single-valued
     * 
     * @return true if the attribute is single-valued, false otherwise
     */
    public boolean isSingleValue()
    {
        return singleValue;
    }


    /**
     * Set the attributeType singleValue flag
     * 
     * @param singleValue
     *            The value for this flag
     */
    public void setSingleValue( boolean singleValue )
    {
        this.singleValue = singleValue;
    }


    /**
     * Tells if the attributeType is collectove or not
     * 
     * @return True if the attributeType is collective, false otherwise
     */
    public boolean isCollective()
    {
        return collective;
    }


    /**
     * Set the attributeType collective flag
     * 
     * @param collective
     *            The value for this flag
     */
    public void setCollective( boolean collective )
    {
        this.collective = collective;
    }


    /**
     * Tellse if the attributeType is mutable or not
     * 
     * @return True if the attributeType is immutable, false otherwise
     */
    public boolean isNoUserModification()
    {
        return noUserModification;
    }


    /**
     * Set the attributeType noUserModification flag
     * 
     * @param noUserModification
     *            The value for this flag
     */
    public void setNoUserModification( boolean noUserModification )
    {
        this.noUserModification = noUserModification;
    }


    /**
     * Get the optional attributeType's superior
     * 
     * @return The attributeType's superior, if any
     */
    public String getSuperior()
    {
        return superior;
    }


    /**
     * Set the attributeType's superior
     * 
     * @param superior
     *            The attributeType's superior
     */
    public void setSuperior( String superior )
    {
        this.superior = superior;
    }


    /**
     * Get the equality Matching Rule
     * 
     * @return The equality matchingRule
     */
    public String getEquality()
    {
        return equality;
    }


    /**
     * Set the equality Matching Rule
     * 
     * @param equality
     *            The equality Matching Rule
     */
    public void setEquality( String equality )
    {
        this.equality = equality;
    }


    /**
     * Get the ordering Matching Rule
     * 
     * @return The ordering matchingRule
     */
    public String getOrdering()
    {
        return ordering;
    }


    /**
     * Set the ordering Matching Rule
     * 
     * @param ordering
     *            The ordering Matching Rule
     */
    public void setOrdering( String ordering )
    {
        this.ordering = ordering;
    }


    /**
     * Get the substring Matching Rule
     * 
     * @return The substring matchingRule
     */
    public String getSubstr()
    {
        return substr;
    }


    /**
     * Set the substring Matching Rule
     * 
     * @param substr
     *            The substring Matching Rule
     */
    public void setSubstr( String substr )
    {
        this.substr = substr;
    }


    /**
     * Get the attributeType's syntax
     * 
     * @return The attributeType's syntax
     */
    public String getSyntax()
    {
        return syntax;
    }


    /**
     * Set the attributeType's syntax
     * 
     * @param syntax
     *            The attributeType's syntax
     */
    public void setSyntax( String syntax )
    {
        this.syntax = syntax;
    }


    /**
     * Get the attributeType's usage
     * 
     * @return The attributeType's usage
     */
    public UsageEnum getUsage()
    {
        return usage;
    }


    /**
     * Set the attributeType's usage
     * 
     * @param usage
     *            The attributeType's usage
     */
    public void setUsage( UsageEnum usage )
    {
        this.usage = usage;
    }


    /**
     * Get the attributeType's syntax length
     * 
     * @return The attributeType's syntax length
     */
    public int getOidLen()
    {
        return oidLen;
    }


    /**
     * Set the attributeType's syntax length
     * 
     * @param oidLen
     *            The attributeType's syntax length
     */
    public void setOidLen( int oidLen )
    {
        this.oidLen = oidLen;
    }


    /**
     * Convert this attributeType to a Ldif string
     * 
     * @param schemaName
     *            The name of the schema file containing this attributeType
     * @return A ldif formatted string
     */
    public String toLdif( String schemaName ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();

        sb.append( schemaToLdif( schemaName, "metaAttributeType" ) );

        // The superior
        if ( superior != null )
        {
            sb.append( "m-supAttributeType: " ).append( superior ).append( '\n' );
        }

        // The equality matching rule
        if ( equality != null )
        {
            sb.append( "m-equality: " ).append( equality ).append( '\n' );
        }

        // The ordering matching rule
        if ( ordering != null )
        {
            sb.append( "m-ordering: " ).append( ordering ).append( '\n' );
        }

        // The substrings matching rule
        if ( substr != null )
        {
            sb.append( "m-substr: " ).append( substr ).append( '\n' );
        }

        // The value syntax
        if ( syntax != null )
        {
            sb.append( "m-syntax: " ).append( syntax ).append(  '\n'  );

            if ( oidLen != -1 )
            {
                sb.append( "m-length: " ).append( oidLen ).append( '\n' );
            }
        }

        // The single value flag
        if ( singleValue )
        {
            sb.append( "m-singleValue: TRUE\n" );
        }

        // The collective flag
        if ( collective )
        {
            sb.append( "m-collective: TRUE\n" );
        }

        // The not user modifiable flag
        if ( noUserModification )
        {
            sb.append( "m-noUserModification: TRUE\n" );
        }

        // The usage value
        if ( usage != UsageEnum.USER_APPLICATIONS )
        {
            sb.append( "m-usage: " ).append( usage.render() ).append( '\n' );
        }

        // The extensions
        if ( extensions.size() != 0 )
        {
            extensionsToLdif( "m-extensionAttributeType" );
        }

        return sb.toString();

    }


    /**
     * Return a String representing this AttributeType.
     */
    public String toString()
    {
        return getOid();
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.converter.schema.SchemaElementImpl#dnToLdif(java.lang.String)
     */
    public String dnToLdif( String schemaName ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();

        String dn = "m-oid=" + oid + ", " + SchemaConstants.ATTRIBUTES_TYPE_PATH + ", cn=" + Rdn.escapeValue( schemaName ) + ", ou=schema";

        // First dump the DN only
        Entry entry = new DefaultClientEntry( new LdapDN( dn ) );
        sb.append( LdifUtils.convertEntryToLdif( entry ) );

        return sb.toString();
    }
}
