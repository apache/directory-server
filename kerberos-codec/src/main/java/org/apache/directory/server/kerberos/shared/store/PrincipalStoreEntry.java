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
package org.apache.directory.server.kerberos.shared.store;


import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.SamType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalStoreEntry
{
    // principal
    private String distinguishedName;
    private KerberosPrincipal principal;

    // KDCEntry
    private int keyVersionNumber;
    private SamType samType;

    private boolean disabled;
    private boolean lockedOut;
    private KerberosTime expiration;

    private Map<EncryptionType, EncryptionKey> keyMap;


    PrincipalStoreEntry( String distinguishedName, KerberosPrincipal principal,
        int keyVersionNumber, 
        Map<EncryptionType, EncryptionKey> keyMap, SamType samType,
        boolean disabled, boolean lockedOut, KerberosTime expiration )
    {
        this.distinguishedName = distinguishedName;
        this.principal = principal;
        this.keyVersionNumber = keyVersionNumber;
        this.disabled = disabled;
        this.lockedOut = lockedOut;
        this.expiration = expiration;
        this.samType = samType;
        this.keyMap = keyMap;
    }


    /**
     * Returns whether this account is disabled.
     *
     * @return Whether this account is disabled.
     */
    public boolean isDisabled()
    {
        return disabled;
    }


    /**
     * Returns whether this account is locked-out.
     *
     * @return Whether this account is locked-out.
     */
    public boolean isLockedOut()
    {
        return lockedOut;
    }


    /**
     * Returns the expiration time.
     *
     * @return The expiration time.
     */
    public KerberosTime getExpiration()
    {
        return expiration;
    }


    /**
     * Returns the distinguished name.
     *
     * @return The distinguished name.
     */
    public String getDistinguishedName()
    {
        return distinguishedName;
    }


    /**
     * Returns the key map.
     *
     * @return The key map.
     */
    public Map<EncryptionType, EncryptionKey> getKeyMap()
    {
        return keyMap;
    }


    /**
     * Returns the key version number (kvno).
     *
     * @return The key version number (kvno).
     */
    public int getKeyVersionNumber()
    {
        return keyVersionNumber;
    }


    /**
     * Returns the principal.
     *
     * @return The principal.
     */
    public KerberosPrincipal getPrincipal()
    {
        return principal;
    }


    /**
     * Returns the single-use authentication (SAM) type.
     *
     * @return The single-use authentication (SAM) type.
     */
    public SamType getSamType()
    {
        return samType;
    }
}
