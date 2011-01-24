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


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.SingletonEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A read only key store facility designed specifically for TLS/CA operations.
 * It is only intended for accessing the 'apacheds' private/public key pairs
 * as well as the self signed certificate.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class CoreKeyStoreSpi extends KeyStoreSpi
{
    private static final String APACHEDS_ALIAS = "apacheds";

    private static final Logger LOG = LoggerFactory.getLogger( CoreKeyStoreSpi.class );
    
    private DirectoryService directoryService;
    

    /**
     * Creates a new instance of LocalKeyStore.
     */
    public CoreKeyStoreSpi( DirectoryService directoryService )
    {
        LOG.debug( "Constructor called." );
        this.directoryService = directoryService;
    }


    private Entry getTlsEntry() throws Exception
    {
        Dn adminDn = directoryService.getDNFactory().create( ServerDNConstants.ADMIN_SYSTEM_DN );
        LdapPrincipal principal = new LdapPrincipal( adminDn, AuthenticationLevel.SIMPLE );
        CoreSession session = directoryService.getSession( principal );
        return session.lookup( adminDn );
    }
    
    
    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineAliases()
     */
    @Override
    public Enumeration<String> engineAliases()
    {
        LOG.debug( "engineAliases() called." );
        return new SingletonEnumeration<String>( APACHEDS_ALIAS );
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineContainsAlias(java.lang.String)
     */
    @Override
    public boolean engineContainsAlias( String alias )
    {
        LOG.debug( "engineContainsAlias({}) called.", alias );
        
        if ( alias.equalsIgnoreCase( APACHEDS_ALIAS ) )
        {
            return true;
        }
        
        return false;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineDeleteEntry(java.lang.String)
     */
    @Override
    public void engineDeleteEntry( String alias ) throws KeyStoreException
    {
        LOG.debug( "engineDeleteEntry({}) called.", alias );
        throw new UnsupportedOperationException();
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineGetCertificate(java.lang.String)
     */
    @Override
    public Certificate engineGetCertificate( String alias )
    {
        LOG.debug( "engineGetCertificate({}) called.", alias );
        if ( alias.equalsIgnoreCase( APACHEDS_ALIAS ) )
        {
            try
            {
                Entry entry = getTlsEntry();
                return TlsKeyGenerator.getCertificate( entry );
            }
            catch ( Exception e )
            {
                LOG.error( I18n.err( I18n.ERR_65 ), e );
            }
        }
        
        return null;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineGetCertificateAlias(java.security.cert.Certificate)
     */
    @Override
    public String engineGetCertificateAlias( Certificate cert )
    {
        LOG.debug( "engineGetCertificateAlias({}) called.", cert );
        
        if ( cert instanceof X509Certificate )
        {
            LOG.debug( "Certificate in alias request is X.509 based." );
            X509Certificate xcert = ( X509Certificate ) cert;
            if ( xcert.getIssuerDN().toString().equals( TlsKeyGenerator.CERTIFICATE_PRINCIPAL_DN ) )
            {
                return APACHEDS_ALIAS;
            }
        }
        
        try
        {
            Entry entry = getTlsEntry();
            if ( ArrayUtils.isEquals( cert.getEncoded(), entry.get( TlsKeyGenerator.USER_CERTIFICATE_AT ).getBytes() ) )
            {
                return APACHEDS_ALIAS;
            }
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_66 ), e );
        }
        
        return null;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineGetCertificateChain(java.lang.String)
     */
    @Override
    public Certificate[] engineGetCertificateChain( String alias )
    {
        LOG.debug( "engineGetCertificateChain({}) called.", alias );
        try
        {
            Entry entry = getTlsEntry();
            LOG.debug( "Entry:\n{}", entry );
            return new Certificate[] { TlsKeyGenerator.getCertificate( entry ) };
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_66 ), e );
        }
        
        return new Certificate[0];
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineGetCreationDate(java.lang.String)
     */
    @Override
    public Date engineGetCreationDate( String alias )
    {
        LOG.debug( "engineGetCreationDate({}) called.", alias );
        return new Date();
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineGetKey(java.lang.String, char[])
     */
    @Override
    public Key engineGetKey( String alias, char[] password ) throws NoSuchAlgorithmException, UnrecoverableKeyException
    {
        LOG.debug( "engineGetKey({}, {}) called.", alias, password );
        
        try
        {
            Entry entry = getTlsEntry();
            KeyPair keyPair = TlsKeyGenerator.getKeyPair( entry );
            return keyPair.getPrivate();
        }
        catch ( Exception e )
        {
            LOG.error( I18n.err( I18n.ERR_68 ), e );
        }
        
        return null;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineIsCertificateEntry(java.lang.String)
     */
    @Override
    public boolean engineIsCertificateEntry( String alias )
    {
        LOG.debug( "engineIsCertificateEntry({}) called.", alias );
        return false;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineIsKeyEntry(java.lang.String)
     */
    @Override
    public boolean engineIsKeyEntry( String alias )
    {
        LOG.debug( "engineIsKeyEntry({}) called.", alias );
        return true;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineLoad(java.io.InputStream, char[])
     */
    @Override
    public void engineLoad( InputStream stream, char[] password ) throws IOException, NoSuchAlgorithmException,
        CertificateException
    {
        LOG.debug( "engineLoad({}, {}) called.", stream, password );
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineSetCertificateEntry(java.lang.String, java.security.cert.Certificate)
     */
    @Override
    public void engineSetCertificateEntry( String alias, Certificate cert ) throws KeyStoreException
    {
        LOG.debug( "engineSetCertificateEntry({}, {}) called.", alias, cert );
        throw new NotImplementedException();
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineSetKeyEntry(java.lang.String, byte[], java.security.cert.Certificate[])
     */
    @Override
    public void engineSetKeyEntry( String alias, byte[] key, Certificate[] chain ) throws KeyStoreException
    {
        LOG.debug( "engineSetKeyEntry({}, key, {}) called.", alias, chain );
        throw new NotImplementedException();
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineSetKeyEntry(java.lang.String, java.security.Key, char[], java.security.cert.Certificate[])
     */
    @Override
    public void engineSetKeyEntry( String alias, Key key, char[] password, Certificate[] chain )
        throws KeyStoreException
    {
        LOG.debug( "engineSetKeyEntry({}, key, {}, chain) called.", alias, new String( password ) );
        throw new NotImplementedException();
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineSize()
     */
    @Override
    public int engineSize()
    {
        LOG.debug( "engineSize() called." );
        return 1;
    }


    /* (non-Javadoc)
     * @see java.security.KeyStoreSpi#engineStore(java.io.OutputStream, char[])
     */
    @Override
    public void engineStore( OutputStream stream, char[] password ) throws IOException, NoSuchAlgorithmException,
        CertificateException
    {
        LOG.debug( "engineStore(stream, {}) called.", new String( password ) );
    }
}
