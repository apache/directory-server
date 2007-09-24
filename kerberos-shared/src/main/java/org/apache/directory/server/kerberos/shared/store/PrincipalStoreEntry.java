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

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.types.SamType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrincipalStoreEntry
{
    // principal
    private String distinguishedName;
    private String commonName;
    private KerberosPrincipal principal;
    private String realmName;

    // uidObject
    private String userId;

    // KDCEntry
    private KerberosTime validStart;
    private KerberosTime validEnd;
    private KerberosTime passwordEnd;
    private int keyVersionNumber;
    private int maxLife;
    private int maxRenew;
    private int kdcFlags;
    private SamType samType;

    private boolean disabled;
    private boolean lockedOut;
    private KerberosTime expiration;

    private Map<EncryptionType, EncryptionKey> keyMap;


    PrincipalStoreEntry( String distinguishedName, String commonName, String userId, KerberosPrincipal principal,
        int keyVersionNumber, KerberosTime validStart, KerberosTime validEnd, KerberosTime passwordEnd, int maxLife,
        int maxRenew, int kdcFlags, Map<EncryptionType, EncryptionKey> keyMap, String realmName, SamType samType,
        boolean disabled, boolean lockedOut, KerberosTime expiration )
    {
        this.distinguishedName = distinguishedName;
        this.commonName = commonName;
        this.userId = userId;
        this.principal = principal;
        this.validStart = validStart;
        this.validEnd = validEnd;
        this.passwordEnd = passwordEnd;
        this.keyVersionNumber = keyVersionNumber;
        this.maxLife = maxLife;
        this.maxRenew = maxRenew;
        this.kdcFlags = kdcFlags;
        this.realmName = realmName;
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
     * Returns the common name.
     *
     * @return The common name.
     */
    public String getCommonName()
    {
        return commonName;
    }


    /**
     * Returns the user ID.
     *
     * @return The user ID.
     */
    public String getUserId()
    {
        return userId;
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
     * Returns the KDC flags.
     *
     * @return The KDC flags.
     */
    public int getKDCFlags()
    {
        return kdcFlags;
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
     * Returns the max life.
     *
     * @return The max life.
     */
    public int getMaxLife()
    {
        return maxLife;
    }


    /**
     * Returns the maximum renew time.
     *
     * @return The maximum renew time.
     */
    public int getMaxRenew()
    {
        return maxRenew;
    }


    /**
     * Returns the expiration time for the password.
     *
     * @return The expiration time for the password.
     */
    public KerberosTime getPasswordEnd()
    {
        return passwordEnd;
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
     * Returns the realm name.
     *
     * @return The realm name.
     */
    public String getRealmName()
    {
        return realmName;
    }


    /**
     * Returns the end of validity.
     *
     * @return The end of validity.
     */
    public KerberosTime getValidEnd()
    {
        return validEnd;
    }


    /**
     * Returns the start of validity.
     *
     * @return The start of validity.
     */
    public KerberosTime getValidStart()
    {
        return validStart;
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
