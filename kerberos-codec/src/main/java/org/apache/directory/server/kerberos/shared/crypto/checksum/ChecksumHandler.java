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
package org.apache.directory.server.kerberos.shared.crypto.checksum;


import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.encryption.KeyUsage;
import org.apache.directory.shared.kerberos.components.Checksum;
import org.apache.directory.shared.kerberos.crypto.checksum.ChecksumType;
import org.apache.directory.shared.kerberos.exceptions.ErrorType;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;


/**
 * A Hashed Adapter encapsulating checksum engines for performing integrity checks.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ChecksumHandler
{
    /** A map of the default encodable class names to the encoder class names. */
    private static final Map<ChecksumType, Class<?>> DEFAULT_CHECKSUMS;

    static
    {
        EnumMap<ChecksumType, Class<?>> map = new EnumMap<>( ChecksumType.class );

        map.put( ChecksumType.HMAC_MD5, HmacMd5Checksum.class );
        map.put( ChecksumType.RSA_MD5, RsaMd5Checksum.class );

        DEFAULT_CHECKSUMS = Collections.unmodifiableMap( map );
    }


    /**
     * Calculate a checksum based on raw bytes and an (optional) key for keyed checksums.
     *
     * @param checksumType The type of checksum to use
     * @param bytes The data
     * @param key The key
     * @param usage The key usage 
     * @return The computed {@link Checksum}.
     * @throws KerberosException If the checksum can't be cmputed
     */
    public Checksum calculateChecksum( ChecksumType checksumType, byte[] bytes, byte[] key, KeyUsage usage )
        throws KerberosException
    {
        if ( !DEFAULT_CHECKSUMS.containsKey( checksumType ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }

        ChecksumEngine digester = getEngine( checksumType );
        return new Checksum( checksumType, digester.calculateChecksum( bytes, key, usage ) );
    }


    /**
     * Verify a checksum by providing the raw bytes and an (optional) key for keyed checksums.
     *
     * @param checksum The checksum to verify
     * @param bytes The data
     * @param key The key
     * @param usage The key usage 
     * @throws KerberosException If the verification failed
     */
    public void verifyChecksum( Checksum checksum, byte[] bytes, byte[] key, KeyUsage usage ) throws KerberosException
    {
        if ( checksum == null )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_INAPP_CKSUM );
        }

        if ( !DEFAULT_CHECKSUMS.containsKey( checksum.getChecksumType() ) )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }

        ChecksumType checksumType = checksum.getChecksumType();
        ChecksumEngine digester = getEngine( checksumType );
        Checksum newChecksum = new Checksum( checksumType, digester.calculateChecksum( bytes, key, usage ) );

        if ( !newChecksum.equals( checksum ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_MODIFIED );
        }
    }


    private ChecksumEngine getEngine( ChecksumType checksumType ) throws KerberosException
    {
        Class<?> clazz = DEFAULT_CHECKSUMS.get( checksumType );

        if ( clazz == null )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }

        try
        {
            return ( ChecksumEngine ) clazz.newInstance();
        }
        catch ( IllegalAccessException iae )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP, iae );
        }
        catch ( InstantiationException ie )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP, ie );
        }
    }
}
