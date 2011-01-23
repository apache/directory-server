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


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.protocol.KerberosDecoder;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.SamType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalStoreEntryModifier
{
    // principal
    private String distinguishedName;
    private String commonName;
    private KerberosPrincipal principal;
    private String realmName;

    // uidObject
    private String userId;

    // KDCEntry
    // must
    private int keyVersionNumber;
    // may
    private KerberosTime validStart;
    private KerberosTime validEnd;
    private KerberosTime passwordEnd;
    private int maxLife;
    private int maxRenew;
    private int kdcFlags;
    private SamType samType;

    private boolean disabled = false;
    private boolean lockedOut = false;
    private KerberosTime expiration = KerberosTime.INFINITY;

    private Map<EncryptionType, EncryptionKey> keyMap;


    /**
     * Returns the {@link PrincipalStoreEntry}.
     *
     * @return The {@link PrincipalStoreEntry}.
     */
    public PrincipalStoreEntry getEntry()
    {
        return new PrincipalStoreEntry( distinguishedName, commonName, userId, principal, keyVersionNumber, validStart,
            validEnd, passwordEnd, maxLife, maxRenew, kdcFlags, keyMap, realmName, samType, disabled, lockedOut,
            expiration );
    }


    /**
     * Sets whether the account is disabled.
     *
     * @param disabled
     */
    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }


    /**
     * Sets whether the account is locked-out.
     *
     * @param lockedOut
     */
    public void setLockedOut( boolean lockedOut )
    {
        this.lockedOut = lockedOut;
    }


    /**
     * Sets the expiration time.
     *
     * @param expiration
     */
    public void setExpiration( KerberosTime expiration )
    {
        this.expiration = expiration;
    }


    /**
     * Sets the distinguished name (Dn).
     *
     * @param distinguishedName
     */
    public void setDistinguishedName( String distinguishedName )
    {
        this.distinguishedName = distinguishedName;
    }


    /**
     * Sets the common name (cn).
     *
     * @param commonName
     */
    public void setCommonName( String commonName )
    {
        this.commonName = commonName;
    }


    /**
     * Sets the user ID.
     *
     * @param userId
     */
    public void setUserId( String userId )
    {
        this.userId = userId;
    }


    /**
     * Sets the KDC flags.
     *
     * @param kdcFlags
     */
    public void setKDCFlags( int kdcFlags )
    {
        this.kdcFlags = kdcFlags;
    }


    /**
     * Sets the key map.
     *
     * @param keyMap
     */
    public void setKeyMap( Map<EncryptionType, EncryptionKey> keyMap )
    {
        this.keyMap = keyMap;
    }


    /**
     * Sets the key version number.
     *
     * @param keyVersionNumber
     */
    public void setKeyVersionNumber( int keyVersionNumber )
    {
        this.keyVersionNumber = keyVersionNumber;
    }


    /**
     * Sets the ticket maximum life time.
     *
     * @param maxLife
     */
    public void setMaxLife( int maxLife )
    {
        this.maxLife = maxLife;
    }


    /**
     * Sets the ticket maximum renew time.
     *
     * @param maxRenew
     */
    public void setMaxRenew( int maxRenew )
    {
        this.maxRenew = maxRenew;
    }


    /**
     * Sets the end-of-life for the password.
     *
     * @param passwordEnd
     */
    public void setPasswordEnd( KerberosTime passwordEnd )
    {
        this.passwordEnd = passwordEnd;
    }


    /**
     * Sets the principal.
     *
     * @param principal
     */
    public void setPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    /**
     * Sets the realm.
     *
     * @param realmName
     */
    public void setRealmName( String realmName )
    {
        this.realmName = realmName;
    }


    /**
     * Sets the end of validity.
     *
     * @param validEnd
     */
    public void setValidEnd( KerberosTime validEnd )
    {
        this.validEnd = validEnd;
    }


    /**
     * Sets the start of validity.
     *
     * @param validStart
     */
    public void setValidStart( KerberosTime validStart )
    {
        this.validStart = validStart;
    }


    /**
     * Sets the single-use authentication (SAM) type.
     *
     * @param samType
     */
    public void setSamType( SamType samType )
    {
        this.samType = samType;
    }


    /**
     * Converts the ASN.1 encoded key set to a map of encryption types to encryption keys.
     *
     * @param krb5key
     * @return The map of encryption types to encryption keys.
     * @throws LdapException
     * @throws IOException
     */
    public Map<EncryptionType, EncryptionKey> reconstituteKeyMap( EntryAttribute krb5key ) throws KerberosException, LdapException
    {
        Map<EncryptionType, EncryptionKey> map = new HashMap<EncryptionType, EncryptionKey>();

        for ( Value<?> val : krb5key )
        {
            if ( val instanceof StringValue )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_626 ) );
            }

            byte[] encryptionKeyBytes = val.getBytes();
            EncryptionKey encryptionKey = KerberosDecoder.decodeEncryptionKey( encryptionKeyBytes );
            map.put( encryptionKey.getKeyType(), encryptionKey );
        }

        return map;
    }
}
