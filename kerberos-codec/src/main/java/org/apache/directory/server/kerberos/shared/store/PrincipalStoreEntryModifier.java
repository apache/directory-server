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


import java.util.HashMap;
import java.util.Map;

import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.kerberos.KerberosTime;
import org.apache.directory.shared.kerberos.codec.KerberosDecoder;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.codec.types.SamType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PrincipalStoreEntryModifier
{
    // principal
    private String distinguishedName;
    private KerberosPrincipal principal;

    // KDCEntry
    // must
    private int keyVersionNumber;
    // may
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
        return new PrincipalStoreEntry( distinguishedName, principal, keyVersionNumber,
            keyMap, samType, disabled, lockedOut, expiration );
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
     * Sets the principal.
     *
     * @param principal
     */
    public void setPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
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
     * @throws KerberosException If the key cannot be converted to a map
     */
    public Map<EncryptionType, EncryptionKey> reconstituteKeyMap( Attribute krb5key ) 
            throws KerberosException
    {
        Map<EncryptionType, EncryptionKey> map = new HashMap<>();

        for ( Value val : krb5key )
        {
            if ( val.isHumanReadable() )
            {
                throw new IllegalStateException( I18n.err( I18n.ERR_32005_KERBEROS_KEY_CANNOT_BE_A_STRING ) );
            }

            byte[] encryptionKeyBytes = val.getBytes();
            EncryptionKey encryptionKey = KerberosDecoder.decodeEncryptionKey( encryptionKeyBytes );
            map.put( encryptionKey.getKeyType(), encryptionKey );
        }

        return map;
    }
}
