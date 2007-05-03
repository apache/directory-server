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

package org.apache.directory.server.kerberos.shared.crypto.encryption;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;


/**
 * A factory class for producing {@link KerberosKey}'s.  For a list of desired cipher
 * types, Kerberos string-to-key functions are used to derive keys for DES-, DES3-, AES-,
 * and RC4-based encryption types.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KerberosKeyFactory
{
    /** A list of the default cipher types to derive keys for. */
    private static final List<String> DEFAULT_CIPHERS;

    static
    {
        List<String> list = new ArrayList<String>();

        list.add( "DES" );
        list.add( "DESede" );
        list.add( "ArcFourHmac" );
        list.add( "AES128" );
        list.add( "AES256" );

        DEFAULT_CIPHERS = Collections.unmodifiableList( list );
    }


    /**
     * Get a list of KerberosKey's for a given principal name and passphrase.  The default set
     * of cipher types is used.
     *
     * @param principalName The principal name to use for key derivation.
     * @param passPhrase The passphrase to use for key derivation.
     * @return The list of KerberosKey's.
     */
    public static List<KerberosKey> getKerberosKeys( String principalName, String passPhrase )
    {
        return getKerberosKeys( principalName, passPhrase, DEFAULT_CIPHERS );
    }


    /**
     * Get a list of KerberosKey's for a given principal name and passphrase and list of cipher
     * types to derive keys for.
     *
     * @param principalName The principal name to use for key derivation.
     * @param passPhrase The passphrase to use for key derivation.
     * @param ciphers The list of ciphers to derive keys for.
     * @return The list of KerberosKey's.
     */
    public static List<KerberosKey> getKerberosKeys( String principalName, String passPhrase, List<String> ciphers )
    {
        KerberosPrincipal principal = new KerberosPrincipal( principalName );
        List<KerberosKey> kerberosKeys = new ArrayList<KerberosKey>();

        Iterator<String> it = ciphers.iterator();
        while ( it.hasNext() )
        {
            kerberosKeys.add( new KerberosKey( principal, passPhrase.toCharArray(), it.next() ) );
        }

        return kerberosKeys;
    }
}
