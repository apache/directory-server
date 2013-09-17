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


import org.apache.directory.api.ldap.model.constants.LdapSecurityConstants;


/**
 * PasswordHashingInterceptor using PBKDF2WithHmacSHA1 encryption algorithm
 * to generate a secret key and use its encoded value as the password hash
 * with {PKCS5S2} prefix.
 * 
 * See <a href="http://en.wikipedia.org/wiki/PBKDF2">PBKDF2 spec</a> for more
 * details.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class Pkcs5s2PasswordHashingInterceptor extends PasswordHashingInterceptor
{
    /**
     * Creates an instance of a Pkcs5s2PasswordHashingInterceptor
     */
    public Pkcs5s2PasswordHashingInterceptor()
    {
        super( "Pkcs5s2PasswordHashingInterceptor", LdapSecurityConstants.HASH_METHOD_PKCS5S2 );
    }

}
