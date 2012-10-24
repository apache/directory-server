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


import java.util.List;

import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.password.PasswordUtil;


/**
 * An interceptor to hash plain text password according to the configured
 * hashing algorithm.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class PasswordHashingInterceptor extends BaseInterceptor
{

    /** the hashing algorithm to be used, if null then the password won't be changed */
    private LdapSecurityConstants algorithm;


    /**
     * 
     * Creates a new instance of PasswordHashingInterceptor which hashes the
     * incoming non-hashed password using the given algorithm.
     * If the password is found already hashed then it will skip hashing it.
     * 
     * @param algorithm the name of the algorithm to be used
     */
    protected PasswordHashingInterceptor( String name, LdapSecurityConstants algorithm )
    {
        super( name );
        this.algorithm = algorithm;
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

        Entry entry = addContext.getEntry();

        Attribute pwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );

        includeHashedPassword( pwdAt );

        next( addContext );
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
            String oid = mod.getAttribute().getAttributeType().getOid();

            // check for modification on 'userPassword' AT
            if ( SchemaConstants.USER_PASSWORD_AT_OID.equals( oid ) )
            {
                includeHashedPassword( mod.getAttribute() );
                break;
            }
        }

        next( modifyContext );
    }


    /**
     * hash the password if it was <i>not</i> already hashed
     *
     * @param pwdAt the password attribute
     */
    private void includeHashedPassword( Attribute pwdAt ) throws LdapException
    {
        if ( pwdAt == null )
        {
            return;
        }

        BinaryValue userPassword = ( BinaryValue ) pwdAt.get();

        // check if the given password is already hashed
        LdapSecurityConstants existingAlgo = PasswordUtil.findAlgorithm( userPassword.getValue() );

        // if there exists NO algorithm, then hash the password
        if ( existingAlgo == null )
        {
            byte[] hashedPassword = PasswordUtil.createStoragePassword( userPassword.getValue(), algorithm );

            pwdAt.clear();
            pwdAt.add( hashedPassword );
        }
    }
}
