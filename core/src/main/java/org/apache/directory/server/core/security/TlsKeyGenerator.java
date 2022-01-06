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
package org.apache.directory.server.core.security;


import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import javax.security.auth.x500.X500Principal;

import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.server.i18n.I18n;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Generates the default RSA key pair for the server.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class TlsKeyGenerator
{
    private TlsKeyGenerator()
    {
    }

    private static final Logger LOG = LoggerFactory.getLogger( TlsKeyGenerator.class );

    public static final String TLS_KEY_INFO_OC = "tlsKeyInfo";
    public static final String PRIVATE_KEY_AT = "privateKey";
    public static final String PUBLIC_KEY_AT = "publicKey";
    public static final String KEY_ALGORITHM_AT = "keyAlgorithm";
    public static final String PRIVATE_KEY_FORMAT_AT = "privateKeyFormat";
    public static final String PUBLIC_KEY_FORMAT_AT = "publicKeyFormat";
    public static final String USER_CERTIFICATE_AT = "userCertificate";

    private static final String BASE_DN = "OU=Directory, O=ASF, C=US";

    public static final String CERTIFICATE_PRINCIPAL_DN = "CN=ApacheDS," + BASE_DN;

    private static final String ALGORITHM = "RSA";

    /* 
     * Eventually we have to make several of these parameters configurable,
     * however note to pass export restrictions we must use a key size of
     * 512 or less here as the default.  Users can configure this setting
     * later based on their own legal situations.  This is required to 
     * classify ApacheDS in the ECCN 5D002 category.  Please see the following
     * page for more information:
     * 
     *    http://www.apache.org/dev/crypto.html
     * 
     * Also ApacheDS must be classified on the following page:
     * 
     *    http://www.apache.org/licenses/exports
     */
    private static final int KEY_SIZE = 2048;
    public static final long YEAR_MILLIS = 365L * 24L * 3600L * 1000L;

    static
    {
        Security.addProvider( new BouncyCastleProvider() );
    }


    /**
     * Gets the certificate associated with the self signed TLS private/public 
     * key pair.
     *
     * @param entry the TLS key/cert entry
     * @return the X509 certificate associated with that entry
     * @throws org.apache.directory.api.ldap.model.exception.LdapException if there are problems accessing or decoding
     */
    public static X509Certificate getCertificate( Entry entry ) throws LdapException
    {
        X509Certificate cert = null;
        CertificateFactory certFactory = null;

        try
        {
            certFactory = CertificateFactory.getInstance( "X.509", "BC" );
        }
        catch ( Exception e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_286 ) );
            ne.initCause( e );
            throw ne;
        }

        byte[] certBytes = entry.get( USER_CERTIFICATE_AT ).getBytes();
        InputStream in = new ByteArrayInputStream( certBytes );

        try
        {
            cert = ( X509Certificate ) certFactory.generateCertificate( in );
        }
        catch ( CertificateException e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_287 ) );
            ne.initCause( e );
            throw ne;
        }

        return cert;
    }


    /**
     * Extracts the public private key pair from the tlsKeyInfo entry.
     *
     * @param entry an entry of the tlsKeyInfo objectClass
     * @return the private and public key pair
     * @throws LdapException if there are format or access issues
     */
    public static KeyPair getKeyPair( Entry entry ) throws LdapException
    {
        PublicKey publicKey = null;
        PrivateKey privateKey = null;

        KeyFactory keyFactory = null;
        try
        {
            keyFactory = KeyFactory.getInstance( ALGORITHM );
        }
        catch ( Exception e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_288, ALGORITHM ) );
            ne.initCause( e );
            throw ne;
        }

        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec( entry.get( PRIVATE_KEY_AT ).getBytes() );
        try
        {
            privateKey = keyFactory.generatePrivate( privateKeySpec );
        }
        catch ( Exception e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_289 ) );
            ne.initCause( e );
            throw ne;
        }

        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec( entry.get( PUBLIC_KEY_AT ).getBytes() );
        try
        {
            publicKey = keyFactory.generatePublic( publicKeySpec );
        }
        catch ( InvalidKeySpecException e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_290 ) );
            ne.initCause( e );
            throw ne;
        }

        return new KeyPair( publicKey, privateKey );
    }


    /**
     * Adds a private key pair along with a self signed certificate to an 
     * entry making sure it contains the objectClasses and attributes needed
     * to support the additions.  This function is intended for creating a TLS
     * key value pair and self signed certificate for use by the server to 
     * authenticate itself during SSL handshakes in the course of establishing
     * an LDAPS connection or a secure LDAP connection using StartTLS. Usually
     * this information is added to the administrator user's entry so the 
     * administrator (effectively the server) can manage these security 
     * concerns.
     * 
     * @param entry the entry to add security attributes to
     * @throws LdapException on problems generating the content in the entry
     */
    public static void addKeyPair( Entry entry ) throws LdapException
    {
        String subjectDn = null;
        try
        {
            String hostName = InetAddress.getLocalHost().getHostName();
            subjectDn = "CN=" + hostName + "," + BASE_DN;
        }
        catch ( Exception e )
        {
            LOG.warn( "failed to create certificate subject name from host name", e );
            subjectDn = CERTIFICATE_PRINCIPAL_DN;
        }
        addKeyPair( entry, CERTIFICATE_PRINCIPAL_DN, subjectDn, ALGORITHM, KEY_SIZE );
    }


    public static void addKeyPair( Entry entry, String issuerDN, String subjectDN, String keyAlgo ) throws LdapException
    {
        addKeyPair( entry, issuerDN, subjectDN, keyAlgo, KEY_SIZE );
    }


    /**
     * @see #addKeyPair(org.apache.directory.api.ldap.model.entry.Entry)
     * 
     * @param entry The Entry to update
     * @param issuerDN The issuer
     * @param subjectDN The subject
     * @param keyAlgo The algorithm
     * @param keySize The key size
     * @throws LdapException If the addition failed 
     */
    public static void addKeyPair( Entry entry, String issuerDN, String subjectDN, String keyAlgo, int keySize )
        throws LdapException
    {
        Date startDate = new Date();
        Date expiryDate = new Date( System.currentTimeMillis() + YEAR_MILLIS );
        addKeyPair( entry, issuerDN, subjectDN, startDate, expiryDate, keyAlgo, keySize, null, false );
    }


    public static void addKeyPair( Entry entry, String issuerDN, String subjectDN, Date startDate, Date expiryDate,
        String keyAlgo, int keySize, PrivateKey optionalSigningKey, boolean isCA ) throws LdapException
    {
        Attribute objectClass = entry.get( SchemaConstants.OBJECT_CLASS_AT );

        if ( objectClass == null )
        {
            entry.put( SchemaConstants.OBJECT_CLASS_AT, TLS_KEY_INFO_OC, SchemaConstants.INET_ORG_PERSON_OC );
        }
        else
        {
            objectClass.add( TLS_KEY_INFO_OC, SchemaConstants.INET_ORG_PERSON_OC );
        }

        KeyPairGenerator generator = null;
        try
        {
            generator = KeyPairGenerator.getInstance( keyAlgo );
        }
        catch ( NoSuchAlgorithmException e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_291 ) );
            ne.initCause( e );
            throw ne;
        }

        generator.initialize( keySize );
        KeyPair keypair = generator.genKeyPair();
        entry.put( KEY_ALGORITHM_AT, keyAlgo );

        // Generate the private key attributes 
        PrivateKey privateKey = keypair.getPrivate();
        entry.put( PRIVATE_KEY_AT, privateKey.getEncoded() );
        entry.put( PRIVATE_KEY_FORMAT_AT, privateKey.getFormat() );
        LOG.debug( "PrivateKey: {}", privateKey );

        PublicKey publicKey = keypair.getPublic();
        entry.put( PUBLIC_KEY_AT, publicKey.getEncoded() );
        entry.put( PUBLIC_KEY_FORMAT_AT, publicKey.getFormat() );
        LOG.debug( "PublicKey: {}", publicKey );

        // Generate the self-signed certificate
        BigInteger serialNumber = BigInteger.valueOf( System.currentTimeMillis() );

        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
        X500Principal issuerName = new X500Principal( issuerDN );
        X500Principal subjectName = new X500Principal( subjectDN );

        certGen.setSerialNumber( serialNumber );
        certGen.setIssuerDN( issuerName );
        certGen.setNotBefore( startDate );
        certGen.setNotAfter( expiryDate );
        certGen.setSubjectDN( subjectName );
        certGen.setPublicKey( publicKey );
        certGen.setSignatureAlgorithm( "SHA256With" + keyAlgo );
        certGen.addExtension( Extension.basicConstraints, false, new BasicConstraints( isCA ) );
        certGen.addExtension( Extension.extendedKeyUsage, true, new ExtendedKeyUsage( 
            new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth } ) );

        try
        {
            PrivateKey signingKey = optionalSigningKey != null ? optionalSigningKey : privateKey;
            X509Certificate cert = certGen.generate( signingKey, "BC" );
            entry.put( USER_CERTIFICATE_AT, cert.getEncoded() );
            LOG.debug( "X509 Certificate: {}", cert );
        }
        catch ( Exception e )
        {
            LdapException ne = new LdapException( I18n.err( I18n.ERR_292 ) );
            ne.initCause( e );
            throw ne;
        }

        LOG.info( "Keys and self signed certificate successfully generated." );
    }

}
