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

import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.util.StringTools;

/**
 * An abstract SchemaElement implementation. It contains shared
 * elements from AttributeType and ObjectClass, like obsolete, oid, 
 * description, names and extensions (not implemented)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class SchemaElementImpl implements SchemaElement
{
    /** The schema element oid */
    protected String oid;
    
    /** The schema element description */
    protected String description;
    
    /** The list of names for this schemaElements */
    protected List<String> names = new ArrayList<String>();

    /** The obsolete flag */
    protected boolean obsolete = false;
    
    /** The optional list of extensions */
    protected List<String> extensions = new ArrayList<String>();
    
    /**
     * @see SchemaElement#isObsolete()
     */
    public boolean isObsolete()
    {
        return obsolete;
    }

    /**
     * @see SchemaElement#setObsolete(boolean)
     */
    public void setObsolete( boolean obsolete )
    {
        this.obsolete = obsolete;
    }

    /**
     * @see SchemaElement#getOid()
     */
    public String getOid()
    {
        return oid;
    }
    
    /**
     * @see SchemaElement#getDescription()
     */
    public String getDescription()
    {
        return description;
    }
    
    /**
     * @see SchemaElement#setDescription(String)
     */
    public void setDescription( String description )
    {
        this.description = description;
    }
    
    /**
     * @see SchemaElement#getNames()
     */
    public List<String> getNames()
    {
        return names;
    }
    
    /**
     * @see SchemaElement#setNames(List)
     */
    public void setNames( List<String> names )
    {
        this.names = names;
    }

    /**
     * @see SchemaElement#getExtensions()
     */
    public List<String> getExtensions()
    {
        return extensions;
    }

    /**
     * @see SchemaElement#setExtensions(List)
     */
    public void setExtensions( List<String> extensions )
    {
        this.extensions = extensions;
    }
    
    /**
     * @return The OID as a Ldif line 
     */
    private String oidToLdif()
    {
        return "m-oid: " + oid + '\n';
    }
    
    /**
     * @return the Names as Ldif lines
     */
    private String nameToLdif() throws NamingException
    {
        if ( names.size() == 0 )
        {
            return "";
        }
        else
        {
            Entry entry = new DefaultClientEntry();
            EntryAttribute attribute = new DefaultClientAttribute( "m-name" );
            
            for ( String name:names )
            {
                attribute.add( name );
            }
            
            entry.put( attribute );
            
            return LdifUtils.convertToLdif( entry );
        }
    }
    
    /**
     * @return The description as a ldif line
     */
    private String descToLdif() throws NamingException
    {
        if ( StringTools.isEmpty( description ) )
        {
            return "";
        }
        else
        {
            Entry entry = new DefaultClientEntry();
            EntryAttribute attribute = new DefaultClientAttribute( "m-description", description );

            entry.put( attribute );
            
            return LdifUtils.convertToLdif( entry );
        }
    }
    
    /**
     * @return The dn as a ldif line
     */
    public abstract String dnToLdif( String schemaName ) throws NamingException;
    
    /**
     * Return the extensions formated as Ldif lines
     * @param ID The attributeId : can be m-objectClassExtension or
     * m-attributeTypeExtension
     * 
     * @return The extensions formated as ldif lines
     * @throws NamingException If the conversion goes wrong
     */
    protected String extensionsToLdif( String ID ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();
        
        Entry entry = new DefaultClientEntry();
        EntryAttribute attribute = new DefaultClientAttribute( ID ); 

        for ( String extension:extensions )
        {
            attribute.add( extension );
        }

        sb.append( LdifUtils.convertToLdif( entry ) );
        
        return sb.toString();
    }

    protected String schemaToLdif( String schemaName, String type ) throws NamingException
    {
        StringBuilder sb = new StringBuilder();
        
        // The DN
        sb.append( dnToLdif( schemaName ) );

        // ObjectClasses
        sb.append( "objectclass: " ).append( type ).append( '\n' );
        sb.append( "objectclass: metaTop\n" );
        sb.append( "objectclass: top\n" );

        // The oid
        sb.append( oidToLdif() );

        // The name
        sb.append( nameToLdif() );
        
        // The desc
        sb.append( descToLdif() );
        
        // The obsolete flag, only if "true"
        if ( obsolete )
        {
            sb.append( "m-obsolete: TRUE\n" );
        }
        
        return sb.toString();
    }
}
