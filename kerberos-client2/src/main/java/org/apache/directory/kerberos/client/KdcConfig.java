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
package org.apache.directory.kerberos.client;


import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.AES128_CTS_HMAC_SHA1_96;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.AES256_CTS_HMAC_SHA1_96;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES3_CBC_SHA1_KD;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.DES_CBC_MD5;
import static org.apache.directory.shared.kerberos.codec.types.EncryptionType.RC4_HMAC;

import java.util.HashSet;
import java.util.Set;

import org.apache.directory.shared.kerberos.KerberosUtils;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;


/**
 * Configuration class for KDC and changepassword servers.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KdcConfig
{
    /** host name of the Kerberos server */
    private String hostName = "localhost";

    /** port on which the Kerberos server is listening */
    private int kdcPort = 88;

    /** port on which the change password server is listening */
    private int passwdPort = 464;

    /** flag to indicate if the client should use UDP while connecting to Kerberos server */
    private boolean useUdp = true;

    /** flag to indicate if legacy protocol version 1 should be used while sending the change password request. Default is false, we send version 0xFF80 of rfc3244 */
    private boolean useLegacyChngPwdProtocol = false;

    /** the timeout of the connection to the Kerberos server */
    private int timeout = 60000; // default 1 min

    /** the set of encryption types that the client can support */
    private Set<EncryptionType> encryptionTypes;

    /** the default encryption types, this includes <b>many</b> encryption types */
    private static Set<EncryptionType> DEFAULT_ENCRYPTION_TYPES;

    static
    {
        DEFAULT_ENCRYPTION_TYPES = new HashSet<EncryptionType>();

        DEFAULT_ENCRYPTION_TYPES.add( AES128_CTS_HMAC_SHA1_96 );
        DEFAULT_ENCRYPTION_TYPES.add( AES256_CTS_HMAC_SHA1_96 );
        DEFAULT_ENCRYPTION_TYPES.add( DES_CBC_MD5 );
        DEFAULT_ENCRYPTION_TYPES.add( DES3_CBC_SHA1_KD );
        DEFAULT_ENCRYPTION_TYPES.add( RC4_HMAC );
        //DEFAULT_ENCRYPTION_TYPES.add( RC4_HMAC_EXP );

        DEFAULT_ENCRYPTION_TYPES = KerberosUtils.orderEtypesByStrength( DEFAULT_ENCRYPTION_TYPES );
    }


    public KdcConfig()
    {
    }


    public static KdcConfig getDefaultConfig()
    {
        return new KdcConfig();
    }


    public String getHostName()
    {
        return hostName;
    }


    public void setHostName( String hostName )
    {
        this.hostName = hostName;
    }


    public int getKdcPort()
    {
        return kdcPort;
    }


    public void setKdcPort( int kdcPort )
    {
        this.kdcPort = kdcPort;
    }


    public int getPasswdPort()
    {
        return passwdPort;
    }


    public void setPasswdPort( int passwdPort )
    {
        this.passwdPort = passwdPort;
    }


    public boolean isUseUdp()
    {
        return useUdp;
    }


    public void setUseUdp( boolean useUdp )
    {
        this.useUdp = useUdp;
    }


    public boolean isUseLegacyChngPwdProtocol()
    {
        return useLegacyChngPwdProtocol;
    }


    public void setUseLegacyChngPwdProtocol( boolean useLegacyChngPwdProtocol )
    {
        this.useLegacyChngPwdProtocol = useLegacyChngPwdProtocol;
    }


    public int getTimeout()
    {
        return timeout;
    }


    public void setTimeout( int timeout )
    {
        this.timeout = timeout;
    }


    public Set<EncryptionType> getEncryptionTypes()
    {
        return encryptionTypes;
    }


    public void setEncryptionTypes( Set<EncryptionType> encryptionTypes )
    {
        this.encryptionTypes = encryptionTypes;
    }


    @Override
    public String toString()
    {
        return "KdcConfig [hostName=" + hostName + ", kdcPort=" + kdcPort + ", passwdPort=" + passwdPort + ", useUdp="
            + useUdp + ", useLegacyChngPwdProtocol=" + useLegacyChngPwdProtocol + ", timeout=" + timeout
            + ", encryptionTypes=" + encryptionTypes + "]";
    }

}
