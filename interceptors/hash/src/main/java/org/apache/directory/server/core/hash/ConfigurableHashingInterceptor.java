/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.hash;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.password.PasswordUtil;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.server.config.beans.HashInterceptorBean;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;


/**
 * An interceptor to hash a configurable set of attributeType(s) using
 * a configurable hashing algorithm.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ConfigurableHashingInterceptor extends BaseInterceptor
{

    /** the hashing algorithm to be used */
    private HashInterceptorBean config;
    private LdapSecurityConstants algorithm;
    private List<AttributeType> attributeTypes;


    /**
     * Creates a new instance of ConfigurableHashingInterceptor which hashes the
     * incoming non-hashed attributeType(s) using the given algorithm.
     * 
     * @param config The configuration bean
     */
    public ConfigurableHashingInterceptor( HashInterceptorBean config )
    {
        this.config = config;
    }


    /**
     * {@inheritDoc}
     */
    public void add( AddOperationContext addContext ) throws LdapException
    {
        if ( algorithm == null )
        {
            next( addContext );
            return;
        }

        for ( Attribute attribute : addContext.getEntry().getAttributes() ) 
        {
            if ( attributeTypes.contains( attribute.getAttributeType() ) ) 
            {
                includeHashed( attribute );    
            }
        }
        
        next( addContext );
    }
    
    
    public LdapSecurityConstants getAlgorithm() 
    {
        return algorithm;
    }
    
    
    public List<AttributeType> getAttributeTypes()
    {
        return Collections.unmodifiableList( attributeTypes );
    }
    
    
    private void includeHashed( Attribute attribute ) throws LdapInvalidAttributeValueException 
    {
        if ( attribute == null ) 
        {
            return;
        }

        // hash any values if necessary
        List<byte[]> values = new ArrayList<>();
        for ( Value<?> value : attribute ) 
        {
            byte[] bytes = value.getBytes();
            if ( bytes == null )
            {
                // value may be empty, dont wanna attempt to hash empty
                continue;
            }

            // check if the given field is already hashed
            LdapSecurityConstants existingAlgo = PasswordUtil.findAlgorithm( bytes );
            if ( existingAlgo == null ) 
            {
                // not already hashed, so hash it
                values.add( PasswordUtil.createStoragePassword( bytes, algorithm ) );
            }
            else 
            {
                // already hashed, just pass through
                values.add( bytes );
            }
        }
        
        // replace the value(s)
        attribute.clear();
        attribute.add( values.toArray( new byte[values.size()][] ) );
    }

    
    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        // allow base initialization
        super.init( directoryService );       

        // initialize from config
        algorithm = LdapSecurityConstants.getAlgorithm( config.getHashAlgorithm() );
        attributeTypes = new ArrayList<>();
        for ( String attributeType : config.getHashAttributes() ) 
        {
            attributeTypes.add( schemaManager.lookupAttributeTypeRegistry( attributeType ) );
        }
    }


    /**
     * {@inheritDoc}
     */
    public void modify( ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( algorithm == null )
        {
            next( modifyContext );
            return;
        }

        List<Modification> mods = modifyContext.getModItems();

        for ( Modification mod : mods ) 
        {
            Attribute attribute = mod.getAttribute();
            if ( attributeTypes.contains( attribute.getAttributeType() ) )
            {
                includeHashed( attribute );
            }
        }

        next( modifyContext );
    }
}
