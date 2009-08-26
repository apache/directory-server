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
package org.apache.directory.server.schema.bootstrap;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.entry.client.ClientStringValue;
import org.apache.directory.shared.ldap.exception.LdapNamingException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.schema.Normalizer;
import org.apache.directory.shared.ldap.schema.registries.OidRegistry;
import org.apache.directory.shared.ldap.schema.registries.Registries;
import org.apache.directory.shared.ldap.schema.syntaxChecker.NumericOidSyntaxChecker;


/**
 * A name or numeric id normalizer.  Needs an OID registry to operate properly.
 * The OID registry is injected into this class after instantiation if a 
 * setRegistries(Registries) method is exposed.
 * 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NameOrNumericIdNormalizer implements Normalizer
{
    // The serial UID
    private static final long serialVersionUID = 1L;
    
    private NumericOidSyntaxChecker checker = new NumericOidSyntaxChecker();
    
    private transient OidRegistry registry;
    
    
    /** A static instance of this normalizer */
    public static final NameOrNumericIdNormalizer INSTANCE = new NameOrNumericIdNormalizer();
    
    
    public NameOrNumericIdNormalizer( OidRegistry registry )
    {
        this.registry = registry;
    }
    
    
    /**
     * 
     */
    public NameOrNumericIdNormalizer()
    {
        // Nothing to do
    }
    
    
    /**
     * {@inheritDoc} 
     */
    public Value<?> normalize( Value<?> value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }
        
        String strValue = value.getString();

        if ( strValue.length() == 0 )
        {
            return new ClientStringValue( "" );
        }
        
        // if value is a numeric id then return it as is
        if ( checker.isValidSyntax( strValue ) )
        {
            return value;
        }
        
        // if it is a name we need to do a lookup
        if ( registry.hasOid( strValue ) )
        {
            return new ClientStringValue( registry.getOid( strValue ) );
        }
        
        // if all else fails
        throw new LdapNamingException( "Encountered name based id of " + value 
            + " which was not found in the OID registry" , ResultCodeEnum.OTHER );
    }
    
    
    /**
     * {@inheritDoc} 
     */
    public String normalize( String value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }
        
        if ( value.length() == 0 )
        {
            return value;
        }
        
        // if value is a numeric id then return it as is
        if ( checker.isValidSyntax( value ) )
        {
            return value;
        }
        
        // if it is a name we need to do a lookup
        if ( registry.hasOid( value ) )
        {
            return registry.getOid( value );
        }
        
        // if all else fails
        throw new LdapNamingException( "Encountered name based id of " + value 
            + " which was not found in the OID registry" , ResultCodeEnum.OTHER );
    }
    
    
    public void setRegistries( Registries registries )
    {
        this.registry = registries.getOidRegistry();
    }
}
