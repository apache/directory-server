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

import org.apache.directory.server.core.authn.PasswordUtil;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.EntryAttribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * An interceptor to hash plain text password according to the configured
 * hashing algorithm.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PasswordHashingInterceptor extends BaseInterceptor
{

    /** the hashing algorithm to be used, if null then the password won't be changed */
    private LdapSecurityConstants algorithm;


    /**
     * Creates a new instance of PasswordHashingInterceptor which does not hash the passwords.
     */
    public PasswordHashingInterceptor()
    {
        this( null );
    }


    /**
     * 
     * Creates a new instance of PasswordHashingInterceptor which hashes the
     * incoming non-hashed password using the given algorithm.
     * If the password is found already hashed then it will skip hashing it.
     *  
     * @param algorithm the name of the algorithm to be used
     */
    public PasswordHashingInterceptor( LdapSecurityConstants algorithm )
    {
        this.algorithm = algorithm;
    }


    @Override
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        if ( algorithm == null )
        {
            next.add( addContext );
            return;
        }

        Entry entry = addContext.getEntry();

        EntryAttribute pwdAt = entry.get( SchemaConstants.USER_PASSWORD_AT );

        includeHashedPassword( pwdAt );

        next.add( addContext );
    }


    @Override
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        if ( algorithm == null )
        {
            next.modify( modifyContext );
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

        next.modify( modifyContext );
    }


    /**
     * hash the password if it was <i>not</i> already hashed
     *
     * @param pwdAt the password attribute
     */
    private void includeHashedPassword( EntryAttribute pwdAt ) throws LdapException
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
