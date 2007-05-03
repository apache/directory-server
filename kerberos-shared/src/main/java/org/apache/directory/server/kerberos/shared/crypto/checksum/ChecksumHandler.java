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
import java.util.HashMap;
import java.util.Map;

import org.apache.directory.server.kerberos.shared.crypto.encryption.Aes128CtsSha1Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.Aes256CtsSha1Encryption;
import org.apache.directory.server.kerberos.shared.crypto.encryption.Des3CbcSha1KdEncryption;
import org.apache.directory.server.kerberos.shared.exceptions.ErrorType;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.messages.value.Checksum;


/**
 * A Hashed Adapter encapsulating checksum engines for performing integrity checks.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ChecksumHandler
{
    /** A map of the default encodable class names to the encoder class names. */
    private static final Map DEFAULT_CHECKSUMS;

    static
    {
        Map<ChecksumType, Class> map = new HashMap<ChecksumType, Class>();

        map.put( ChecksumType.HMAC_MD5, HmacMd5Checksum.class );
        map.put( ChecksumType.HMAC_SHA1_96_AES128, Aes128CtsSha1Encryption.class );
        map.put( ChecksumType.HMAC_SHA1_96_AES256, Aes256CtsSha1Encryption.class );
        map.put( ChecksumType.HMAC_SHA1_DES3_KD, Des3CbcSha1KdEncryption.class );
        map.put( ChecksumType.RSA_MD5, RsaMd5Checksum.class );

        DEFAULT_CHECKSUMS = Collections.unmodifiableMap( map );
    }


    /**
     * Verify a checksum by providing the raw bytes and an (optional) key for keyed checksums.
     *
     * @param checksum
     * @param bytes
     * @param key
     * @throws KerberosException
     */
    public void verifyChecksum( Checksum checksum, byte[] bytes, byte[] key ) throws KerberosException
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
        Checksum newChecksum = new Checksum( checksumType, digester.calculateChecksum( bytes, key ) );

        if ( !newChecksum.equals( checksum ) )
        {
            throw new KerberosException( ErrorType.KRB_AP_ERR_MODIFIED );
        }
    }


    private ChecksumEngine getEngine( ChecksumType checksumType ) throws KerberosException
    {
        Class clazz = ( Class ) DEFAULT_CHECKSUMS.get( checksumType );

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
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }
        catch ( InstantiationException ie )
        {
            throw new KerberosException( ErrorType.KDC_ERR_SUMTYPE_NOSUPP );
        }
    }
}
