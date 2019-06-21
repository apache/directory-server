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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.Security;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;

import org.apache.directory.api.util.Strings;

import sun.security.x509.AlgorithmId;
import sun.security.x509.BasicConstraintsExtension;
import sun.security.x509.CertificateAlgorithmId;
import sun.security.x509.CertificateExtensions;
import sun.security.x509.CertificateSerialNumber;
import sun.security.x509.CertificateValidity;
import sun.security.x509.CertificateVersion;
import sun.security.x509.CertificateX509Key;
import sun.security.x509.DNSName;
import sun.security.x509.GeneralName;
import sun.security.x509.GeneralNames;
import sun.security.x509.IPAddressName;
import sun.security.x509.SubjectAlternativeNameExtension;
import sun.security.x509.X500Name;
import sun.security.x509.X509CertImpl;
import sun.security.x509.X509CertInfo;

/**
 * Helper class used to generate self-signed certificates, and load a KeyStore
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
@SuppressWarnings("restriction")
public final class CertificateUtil
{
    private static final boolean SELF_SIGNED = true;
    private static final boolean CA_SIGNED = false;
    private static final boolean CRITICAL = true;
        
    private CertificateUtil()
    {
        // Nothing to do
    }
    
    
    private static void setInfo( X509CertInfo info, X500Name subject, X500Name issuer, KeyPair keyPair, int days, 
        String algoStr, boolean isCA ) 
        throws CertificateException, IOException, NoSuchAlgorithmException
    {
        Date from = new Date();
        Date to = new Date( from.getTime() + days * 86_400_000L );
        CertificateValidity interval = new CertificateValidity( from, to );

        // Feed the certificate info structure
        // version         [0]  EXPLICIT Version DEFAULT v1
        // Version  ::=  INTEGER  {  v1(0), v2(1), v3(2)  }
        info.set( X509CertInfo.VERSION, new CertificateVersion( CertificateVersion.V3 ) );
        
        // serialNumber         CertificateSerialNumber
        // CertificateSerialNumber  ::=  INTEGER
        BigInteger serialNumber = new BigInteger( 64, new SecureRandom() );
        info.set( X509CertInfo.SERIAL_NUMBER, new CertificateSerialNumber( serialNumber ) );

        // signature            AlgorithmIdentifier
        AlgorithmId algo = AlgorithmId.get( algoStr );
        info.set( X509CertInfo.ALGORITHM_ID, new CertificateAlgorithmId( algo ) );

        // issuer               Name
        // Name ::= CHOICE {
        //          RDNSequence }
        // RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
        // RelativeDistinguishedName ::=
        //          SET OF AttributeTypeAndValue
        // AttributeTypeAndValue ::= SEQUENCE {
        //          type     AttributeType,
        //          value    AttributeValue }
        // AttributeType ::= OBJECT IDENTIFIER
        // AttributeValue ::= ANY DEFINED BY AttributeType
        info.set( X509CertInfo.ISSUER, issuer );
        
        // validity             Validity,
        // Validity ::= SEQUENCE {
        //          notBefore      Time,
        //          notAfter       Time }
        info.set( X509CertInfo.VALIDITY, interval );
        
        // subject              Name
        // Name ::= CHOICE {
        //          RDNSequence }
        // RDNSequence ::= SEQUENCE OF RelativeDistinguishedName
        // RelativeDistinguishedName ::=
        //          SET OF AttributeTypeAndValue
        // AttributeTypeAndValue ::= SEQUENCE {
        //          type     AttributeType,
        //          value    AttributeValue }
        // AttributeType ::= OBJECT IDENTIFIER
        // AttributeValue ::= ANY DEFINED BY AttributeType
        info.set( X509CertInfo.SUBJECT, subject );
        
        // subjectPublicKeyInfo SubjectPublicKeyInfo,
        // SubjectPublicKeyInfo  ::=  SEQUENCE  {
        //          algorithm            AlgorithmIdentifier,
        //          subjectPublicKey     BIT STRING  }
        info.set( X509CertInfo.KEY, new CertificateX509Key( keyPair.getPublic() ) );

        // Extensions. Basically, a subjectAltName and a Basic-Constraint 
        CertificateExtensions extensions = new CertificateExtensions();

        // SubjectAltName
        GeneralNames names = new GeneralNames();
        names.add( new GeneralName( new DNSName( InetAddress.getLocalHost().getHostName() ) ) );
        String ipAddress = InetAddress.getLocalHost().getHostAddress();
        names.add( new GeneralName( new IPAddressName( ipAddress ) ) );
        
        // A wildcard
        //names.add( new GeneralName( 
        //    new DNSName( 
        //        new DerValue( 
        //            DerValue.tag_IA5String, "*.apache.org" ) ) ) );
        SubjectAlternativeNameExtension subjectAltName = new SubjectAlternativeNameExtension( names );
        
        extensions.set( subjectAltName.getExtensionId().toString(), subjectAltName );

        // The Basic-Constraint,
        BasicConstraintsExtension basicConstraint = new BasicConstraintsExtension( CRITICAL, isCA, -1 );
        extensions.set( basicConstraint.getExtensionId().toString(), basicConstraint );

        // Inject the extensions into the cert
        info.set( X509CertInfo.EXTENSIONS, extensions );
    }
    
    
    /**
     * Create a self signed certificate
     * 
     * @param issuer The Issuer (§which is the same as the subject
     * @param keyPair The asymmetric keyPair
     * @param days Validity number of days
     * @param algoStr Algorithm
     * @return A self signed CA certificate
     * @throws CertificateException If the info store din the certificate is invalid
     * @throws IOException If we can't store some info in the certificate
     * @throws NoSuchAlgorithmException If the algorithm does not exist
     * @throws SignatureException If the certificate cannot be signed
     * @throws NoSuchProviderException  If we don't have a security provider
     * @throws InvalidKeyException  If the KeyPair is invalid
     */
    public static X509Certificate generateSelfSignedCertificate( X500Name issuer, KeyPair keyPair,  int days, String algoStr ) 
        throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        // Create the certificate info
        X509CertInfo info = new X509CertInfo();
        
        // Set the common certificate info
        setInfo( info, issuer, issuer, keyPair, days, algoStr, SELF_SIGNED );
        
        // Sign the cert to identify the algorithm that's used.
        X509CertImpl certificate = new X509CertImpl( info );
        certificate.sign( keyPair.getPrivate(), algoStr );

        return certificate;
    }
    
    
    /**
     * Generate a Certificate signed by a CA certificate
     * 
     * @param issuer The Issuer (§which is the same as the subject
     * @param keyPair The asymmetric keyPair
     * @param days Validity number of days
     * @param algoStr Algorithm
     * @return A self signed CA certificate
     * @throws CertificateException If the info store din the certificate is invalid
     * @throws IOException If we can't store some info in the certificate
     * @throws NoSuchAlgorithmException If the algorithm does not exist
     * @throws SignatureException If the certificate cannot be signed
     * @throws NoSuchProviderException  If we don't have a security provider
     * @throws InvalidKeyException  If the KeyPair is invalid
     */
    public static X509Certificate generateCertificate( X500Name subject, X500Name issuer, KeyPair keyPair,  int days, String algoStr ) 
        throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        // Create the certificate info
        X509CertInfo info = new X509CertInfo();
        
        // Set the common certificate info
        setInfo( info, subject, issuer, keyPair, days, algoStr, CA_SIGNED );
         
        // Sign the cert to identify the algorithm that's used.
        X509CertImpl certificate = new X509CertImpl( info );
        certificate.sign( keyPair.getPrivate(), algoStr );

        return certificate;
    }
    
    
    /**
     * Loads the digital certificate from a keystore file
     *
     * @param keyStoreFile The KeyStore file to load
     * @param keyStorePasswordStr The KeyStore password
     * @return The KeyManager factory it created 
     * @throws Exception If the KeyStore can't be loaded
     */
    public static KeyManagerFactory loadKeyStore( String keyStoreFile, String keyStorePasswordStr ) throws Exception
    {
        char[] keyStorePassword = Strings.isEmpty( keyStorePasswordStr ) ? null : keyStorePasswordStr.toCharArray();

        if ( !Strings.isEmpty( keyStoreFile ) )
        {
            // We have a provided KeyStore file: read it
            KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );

            try ( InputStream is = Files.newInputStream( Paths.get( keyStoreFile ) ) )
            {
                keyStore.load( is, keyStorePassword );
            }
    
            /*
             * Verify key store:
             * * Must only contain one entry which must be a key entry
             * * Must contain a certificate chain
             * * The private key must be recoverable by the key store password
             */
            Enumeration<String> aliases = keyStore.aliases();
            
            if ( !aliases.hasMoreElements() )
            {
                throw new KeyStoreException( "Key store is empty" );
            }
            
            String alias = aliases.nextElement();
            
            if ( aliases.hasMoreElements() )
            {
                throw new KeyStoreException( "Key store contains more than one entry" );
            }
            
            if ( !keyStore.isKeyEntry( alias ) )
            {
                throw new KeyStoreException( "Key store must contain a key entry" );
            }
            
            if ( keyStore.getCertificateChain( alias ) == null )
            {
                throw new KeyStoreException( "Key store must contain a certificate chain" );
            }
            
            if ( keyStore.getKey( alias, keyStorePassword ) == null )
            {
                throw new KeyStoreException( "Private key must be recoverable by the key store password" );
            }
    
            // Set up key manager factory to use our key store
            String algorithm = Security.getProperty( "ssl.KeyManagerFactory.algorithm" );
    
            if ( algorithm == null )
            {
                algorithm = KeyManagerFactory.getDefaultAlgorithm();
            }
    
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance( algorithm );
    
            keyManagerFactory.init( keyStore, keyStorePassword );
            
            return keyManagerFactory;
        }
        else
        {
            return null;
        }
    }
    
    
    public static File createTempKeyStore( String keyStoreName ) throws IOException, KeyStoreException, 
        NoSuchAlgorithmException, CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        // Create a temporary keystore, be sure to remove it when exiting the test
        File keyStoreFile = File.createTempFile( keyStoreName, "ks" );
        keyStoreFile.deleteOnExit();
        
        KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        char[] keyStorePassword = "secret".toCharArray();
        
        try ( InputStream keyStoreData = new FileInputStream( keyStoreFile ) )
        {
            keyStore.load( null, keyStorePassword );
        }

        // Generate the asymmetric keys, using EC algorithm
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // Generate the subject's name
        @SuppressWarnings("restriction")
        X500Name owner = new X500Name( "apacheds", "directory", "apache", "US" );

        // Create the self-signed certificate
        X509Certificate certificate = CertificateUtil.generateSelfSignedCertificate( owner, keyPair, 365, "SHA256WithECDSA" );
        
        keyStore.setKeyEntry( "apachedsKey", keyPair.getPrivate(), keyStorePassword, new X509Certificate[] { certificate } );
        
        try ( FileOutputStream out = new FileOutputStream( keyStoreFile ) )
        {
            keyStore.store( out, keyStorePassword );
        }
        
        return keyStoreFile;
    }
}
