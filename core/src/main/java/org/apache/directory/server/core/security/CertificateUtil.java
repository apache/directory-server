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
import java.net.UnknownHostException;
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
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.security.auth.x500.X500Principal;

import org.apache.directory.api.util.Strings;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * Helper class used to generate self-signed certificates, and load a KeyStore
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public final class CertificateUtil
{
    private static final boolean SELF_SIGNED = true;
    private static final boolean CA_SIGNED = false;
    private static final boolean CRITICAL = true;
        
    private CertificateUtil()
    {
        // Nothing to do
    }
    
    public static X509Certificate generateX509Certificate( X500Principal subjectDn, X500Principal issuerDn, KeyPair keyPair,
            long daysValidity, String sigAlgorithm, boolean isCa )
                    throws CertificateException
    {
        Instant from = Instant.now();
        Instant to = from.plus( Duration.ofDays( daysValidity ) );
        BigInteger serialNumber = new BigInteger( 64, new SecureRandom() );
        try
        {
            ContentSigner signer = new JcaContentSignerBuilder( sigAlgorithm ).build( keyPair.getPrivate() );
            InetAddress localHost = InetAddress.getLocalHost();
            GeneralName[] sanLocalHost = new GeneralName[] {
                    new GeneralName( GeneralName.dNSName,
                            localHost.getHostName() ),
                    new GeneralName( GeneralName.iPAddress, localHost.getHostAddress() )
            };
            X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder( issuerDn,
                    serialNumber,
                    Date.from( from ),
                    Date.from( to ),
                    subjectDn,
                    keyPair.getPublic() )
                .addExtension( Extension.basicConstraints, CRITICAL, new BasicConstraints( isCa ) )
                .addExtension( Extension.subjectAlternativeName, false, new GeneralNames( sanLocalHost ) );

            return new JcaX509CertificateConverter().setProvider( new BouncyCastleProvider() )
                    .getCertificate( certificateBuilder.build( signer ) );
        }
        catch ( OperatorCreationException | CertIOException | UnknownHostException e )
        {
            throw new CertificateException( "BouncyCastle failed to generate the X509 certificate.", e );
        }
    }
    
    /**
     * Create a self signed certificate
     * 
     * @param issuer The Issuer (which is the same as the subject
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
    public static X509Certificate generateSelfSignedCertificate( X500Principal issuer, KeyPair keyPair,  int days, String algoStr ) 
        throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        return generateX509Certificate( issuer, issuer, keyPair, days, algoStr, SELF_SIGNED );
    }
    
    /**
     * Generate a Certificate signed by a CA certificate
     * 
     * @param issuer The Issuer (which is the same as the subject
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
    public static X509Certificate generateCertificate( X500Principal subject, X500Principal issuer, KeyPair keyPair,  int days, String algoStr ) 
        throws CertificateException, IOException, NoSuchAlgorithmException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        return generateX509Certificate( subject, issuer, keyPair, days, algoStr, CA_SIGNED );
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
    
    
    public static File createTempKeyStore( String keyStoreName, char[] keyStorePassword ) throws IOException, KeyStoreException,
        NoSuchAlgorithmException, CertificateException, InvalidKeyException, NoSuchProviderException, SignatureException
    {
        // Create a temporary keystore, be sure to remove it when exiting the test
        File keyStoreFile = Files.createTempFile( keyStoreName, "ks" ).toFile();
        keyStoreFile.deleteOnExit();
        
        KeyStore keyStore = KeyStore.getInstance( KeyStore.getDefaultType() );
        
        try ( InputStream keyStoreData = new FileInputStream( keyStoreFile ) )
        {
            keyStore.load( null, keyStorePassword );
        }

        // Generate the asymmetric keys, using EC algorithm
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance( "EC" );
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        
        // Generate the subject's name
        X500Principal owner = new X500Principal( "CN=apacheds,OU=directory,O=apache,C=US" );

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
