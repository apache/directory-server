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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.io.decoder.EncryptionKeyDecoder;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.SamType;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PrincipalStoreEntryModifier
{
    // principal
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
    private int encryptionType;
    private SamType samType;

    private boolean disabled = false;
    private boolean lockedOut = false;
    private KerberosTime expiration = KerberosTime.INFINITY;

    private Map<EncryptionType, EncryptionKey> keyMap;


    public PrincipalStoreEntry getEntry()
    {
        return new PrincipalStoreEntry( commonName, userId, principal, keyVersionNumber, validStart, validEnd,
            passwordEnd, maxLife, maxRenew, kdcFlags, encryptionType, keyMap, realmName, samType, disabled, lockedOut,
            expiration );
    }


    public void setDisabled( boolean disabled )
    {
        this.disabled = disabled;
    }


    public void setLockedOut( boolean lockedOut )
    {
        this.lockedOut = lockedOut;
    }


    public void setExpiration( KerberosTime expiration )
    {
        this.expiration = expiration;
    }


    public void setCommonName( String commonName )
    {
        this.commonName = commonName;
    }


    public void setUserId( String userId )
    {
        this.userId = userId;
    }


    public void setEncryptionType( int encryptionType )
    {
        this.encryptionType = encryptionType;
    }


    public void setKDCFlags( int kdcFlags )
    {
        this.kdcFlags = kdcFlags;
    }


    public void setKeyMap( Map<EncryptionType, EncryptionKey> keyMap )
    {
        this.keyMap = keyMap;
    }


    public void setKeyVersionNumber( int keyVersionNumber )
    {
        this.keyVersionNumber = keyVersionNumber;
    }


    public void setMaxLife( int maxLife )
    {
        this.maxLife = maxLife;
    }


    public void setMaxRenew( int maxRenew )
    {
        this.maxRenew = maxRenew;
    }


    public void setPasswordEnd( KerberosTime passwordEnd )
    {
        this.passwordEnd = passwordEnd;
    }


    public void setPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    public void setRealmName( String realmName )
    {
        this.realmName = realmName;
    }


    public void setValidEnd( KerberosTime validEnd )
    {
        this.validEnd = validEnd;
    }


    public void setValidStart( KerberosTime validStart )
    {
        this.validStart = validStart;
    }


    public void setSamType( SamType samType )
    {
        this.samType = samType;
    }


    public Map<EncryptionType, EncryptionKey> reconstituteKeyMap( Attribute krb5key ) throws NamingException,
        IOException
    {
        Map<EncryptionType, EncryptionKey> map = new HashMap<EncryptionType, EncryptionKey>();

        for ( int ii = 0; ii < krb5key.size(); ii++ )
        {
            Object key = krb5key.get( ii );

            if ( key instanceof String )
            {
                throw new NamingException(
                    "JNDI should not return a string for the Kerberos key: JNDI property java.naming.ldap.attributes.binary must include the krb5key attribute." );
            }

            byte[] encryptionKeyBytes = ( byte[] ) key;
            EncryptionKey encryptionKey = EncryptionKeyDecoder.decode( encryptionKeyBytes );
            map.put( encryptionKey.getKeyType(), encryptionKey );
        }

        return map;
    }
}
