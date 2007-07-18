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
package org.apache.directory.server.kerberos.protocol;


import java.util.HashMap;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.PrincipalStore;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;


/**
 * An implementation of {@link PrincipalStore} that is backed by a {@link Map}.  This
 * store implements only getPrincipal, as required by the Kerberos service.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class MapPrincipalStoreImpl implements PrincipalStore
{
    private static Map<KerberosPrincipal, PrincipalStoreEntry> store = new HashMap<KerberosPrincipal, PrincipalStoreEntry>();

    static
    {
        String principalName = "hnelson@EXAMPLE.COM";
        String passPhrase = "secret";

        PrincipalStoreEntry entry = getEntry( principalName, passPhrase );

        store.put( entry.getPrincipal(), entry );

        principalName = "tquist@EXAMPLE.COM";
        passPhrase = "secret";

        entry = getNullKeyEntry( principalName );

        store.put( entry.getPrincipal(), entry );
    }


    public PrincipalStoreEntry getPrincipal( KerberosPrincipal principal ) throws Exception
    {
        PrincipalStoreEntry entry = store.get( principal );

        return entry;
    }


    public String addPrincipal( PrincipalStoreEntry entry ) throws Exception
    {
        return null;
    }


    public String changePassword( KerberosPrincipal principal, String newPassword ) throws Exception
    {
        return null;
    }


    public String deletePrincipal( KerberosPrincipal principal ) throws Exception
    {
        return null;
    }


    public PrincipalStoreEntry[] getAllPrincipals( String realm ) throws Exception
    {
        return null;
    }


    private static PrincipalStoreEntry getEntry( String principalName, String passPhrase )
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( principalName );

        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();
        modifier.setPrincipal( clientPrincipal );

        Map<EncryptionType, EncryptionKey> keyMap = KerberosKeyFactory.getKerberosKeys( principalName, passPhrase );

        modifier.setKeyMap( keyMap );

        return modifier.getEntry();
    }


    private static PrincipalStoreEntry getNullKeyEntry( String principalName )
    {
        KerberosPrincipal clientPrincipal = new KerberosPrincipal( principalName );

        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();
        modifier.setPrincipal( clientPrincipal );

        return modifier.getEntry();
    }
}
