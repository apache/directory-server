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

package org.apache.directory.server.core.api.authn;


import org.apache.directory.shared.ldap.model.constants.LdapSecurityConstants;
import org.apache.directory.shared.util.Strings;


/**
 * A class to store all informations about the existing
 * password found in the cache or get from the backend.
 *
 * This is necessary as we have to compute :
 * - the used algorithm
 * - the salt if any
 * - the password itself.
 *
 * If we have a on-way encrypted password, it is stored using this
 * format :
 * {<algorithm>}<encrypted password>
 * where the encrypted password format can be :
 * - MD5/SHA : base64(<password>)
 * - SMD5/SSH : base64(<salted-password-digest><salt (4 or 8 bytes)>)
 * - crypt : <salt (2 btytes)><password>
 *
 * Algorithm are currently MD5, SMD5, SHA, SSHA, SHA2, SSHA-2 (except SHA-224), CRYPT and empty
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EncryptionMethod
{
    private byte[] salt;
    private LdapSecurityConstants algorithm;


    public EncryptionMethod( LdapSecurityConstants algorithm, byte[] salt )
    {
        this.algorithm = algorithm;
        this.salt = salt;
    }


    public LdapSecurityConstants getAlgorithm()
    {
        return algorithm;
    }


    public byte[] getSalt()
    {
        return salt;
    }


    public void setSalt( byte[] salt )
    {
        // just to make this class immutable, though we have a setter
        if ( this.salt != null )
        {
            throw new IllegalStateException( "salt will only be allowed to set once" );
        }

        this.salt = salt;
    }


    @Override
    public String toString()
    {
        return "EncryptionMethod [algorithm=" + algorithm.getName().toUpperCase() + ", salt="
            + Strings.dumpBytes( salt ) + "]";
    }

}
