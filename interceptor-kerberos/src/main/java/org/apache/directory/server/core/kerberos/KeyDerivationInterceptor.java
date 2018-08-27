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
package org.apache.directory.server.core.kerberos;


import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.directory.api.asn1.EncoderException;
import org.apache.directory.api.ldap.model.constants.Loggers;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.entry.Value;
import org.apache.directory.api.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.util.Strings;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.shared.kerberos.KerberosAttribute;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that creates symmetric Kerberos keys for users.  When a
 * 'userPassword' is added or modified, the 'userPassword' and 'krb5PrincipalName'
 * are used to derive Kerberos keys.  If the 'userPassword' is the special keyword
 * 'randomKey', a random key is generated and used as the Kerberos key.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class KeyDerivationInterceptor extends BaseInterceptor
{
    /** The log for this class. */
    private static final Logger LOG = LoggerFactory.getLogger( KeyDerivationInterceptor.class );
    private static final Logger LOG_KRB = LoggerFactory.getLogger( Loggers.KERBEROS_LOG.getName() );

    /** The service name. */
    private static final String NAME = "keyDerivationService";

    /** The krb5Key attribute type */
    private AttributeType krb5KeyAT;

    /** The krb5PrincipalName attribute type */
    private AttributeType krb5PrincipalNameAT;

    /** The krb5KeyVersionNumber attribute type */
    private AttributeType krb5KeyVersionNumberAT;

    /** The userPassword attribute tType */
    private AttributeType userPasswordAT;


    /**
     * Creates an instance of a KeyDerivationInterceptor.
     */
    public KeyDerivationInterceptor()
    {
        super( NAME );
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void init( DirectoryService directoryService ) throws LdapException
    {
        super.init( directoryService );

        krb5KeyAT = schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_KEY_AT );
        krb5PrincipalNameAT = schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT );
        krb5KeyVersionNumberAT = schemaManager
            .lookupAttributeTypeRegistry( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT );
        userPasswordAT = schemaManager
            .lookupAttributeTypeRegistry( SchemaConstants.USER_PASSWORD_AT );

        LOG_KRB.info( "KeyDerivation Interceptor initialized" );
    }


    /**
     * Intercepts the addition of the 'userPassword' and 'krb5PrincipalName' attributes.
     * Uses the 'userPassword' and 'krb5PrincipalName' attributes to derive Kerberos keys 
     * for the principal.  If the 'userPassword' is the special keyword 'randomKey', set 
     * random keys for the principal.  Set the key version number (kvno) to '0'.
     */
    @Override
    public void add( AddOperationContext addContext ) throws LdapException
    {
        // Bypass the replication events
        if ( addContext.isReplEvent() )
        {
            next( addContext );

            return;
        }

        Dn normName = addContext.getDn();

        Entry entry = addContext.getEntry();

        if ( ( entry.get( userPasswordAT ) != null ) && ( entry.get( krb5PrincipalNameAT ) != null ) )
        {
            LOG.debug( "Adding the entry '{}' for Dn '{}'.", entry, normName.getName() );

            // Get the entry's password. We will use the first one.
            Value userPassword = entry.get( userPasswordAT ).get();
            String strUserPassword = userPassword.getValue();

            String principalName = entry.get( krb5PrincipalNameAT ).getString();

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "Got principal '{}'.", principalName );
            }
            
            if ( LOG_KRB.isDebugEnabled() )
            {
                LOG_KRB.debug( "Got principal '{}'", principalName );
            }

            Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, strUserPassword );

            // Set the KVNO to 0 as it's a new entry
            entry.put( krb5KeyVersionNumberAT, "0" );

            Attribute keyAttribute = getKeyAttribute( keys );
            entry.put( keyAttribute );

            if ( LOG.isDebugEnabled() )
            {
                LOG.debug( "Adding modified entry '{}' for Dn '{}'.", entry, normName
                    .getName() );
            }
            
            if ( LOG_KRB.isDebugEnabled() )
            {
                LOG_KRB.debug( "Adding modified entry '{}' for Dn '{}'.", entry, normName
                    .getName() );
            }
        }

        next( addContext );
    }


    /**
     * Intercept the modification of the 'userPassword' attribute.  Perform a lookup to check for an
     * existing principal name and key version number (kvno).  If a 'krb5PrincipalName' is not in
     * the modify request, attempt to use an existing 'krb5PrincipalName' attribute.  If a kvno
     * exists, increment the kvno; otherwise, set the kvno to '0'.
     *
     * If both a 'userPassword' and 'krb5PrincipalName' can be found, use the 'userPassword' and
     * 'krb5PrincipalName' attributes to derive Kerberos keys for the principal.
     *
     * If the 'userPassword' is the special keyword 'randomKey', set random keys for the principal.
     */
    @Override
    public void modify( ModifyOperationContext modContext ) throws LdapException
    {
        // bypass replication events
        if ( modContext.isReplEvent() )
        {
            next( modContext );
            return;
        }

        ModifySubContext subContext = new ModifySubContext();

        detectPasswordModification( modContext, subContext );

        if ( subContext.getUserPassword() != null )
        {
            lookupPrincipalAttributes( modContext, subContext );
        }

        if ( subContext.isPrincipal() && subContext.hasValues() )
        {
            deriveKeys( modContext, subContext );
        }

        next( modContext );
    }


    /**
     * Detect password modification by checking the modify request for the 'userPassword'.  Additionally,
     * check to see if a 'krb5PrincipalName' was provided.
     *
     * @param modContext The original ModifyContext
     * @param subContext The modification container
     * @throws LdapException If we get an exception
     */
    private void detectPasswordModification( ModifyOperationContext modContext, ModifySubContext subContext )
        throws LdapException
    {
        List<Modification> mods = modContext.getModItems();

        String operation = null;

        // Loop over attributes being modified to pick out 'userPassword' and 'krb5PrincipalName'.
        for ( Modification mod : mods )
        {
            if ( LOG.isDebugEnabled() )
            {
                switch ( mod.getOperation() )
                {
                    case ADD_ATTRIBUTE:
                        operation = "Adding";
                        break;

                    case REMOVE_ATTRIBUTE:
                        operation = "Removing";
                        break;

                    case REPLACE_ATTRIBUTE:
                        operation = "Replacing";
                        break;

                    default:
                        throw new IllegalArgumentException( "Unexpected modify operation " + mod.getOperation() );
                }
            }

            Attribute attr = mod.getAttribute();

            if ( userPasswordAT.equals( attr.getAttributeType() ) )
            {
                Value firstValue = attr.get();
                String password = null;

                if ( firstValue.isHumanReadable() )
                {
                    password = firstValue.getValue();
                    LOG.debug( "{} Attribute id : 'userPassword',  Values : [ '{}' ]", operation, password );
                    LOG_KRB.debug( "{} Attribute id : 'userPassword',  Values : [ '{}' ]", operation, password );
                }
                else
                {
                    password = Strings.utf8ToString( firstValue.getBytes() );
                }

                subContext.setUserPassword( password );
            }

            if ( krb5PrincipalNameAT.equals( attr.getAttributeType() ) )
            {
                subContext.setPrincipalName( attr.getString() );
                LOG.debug( "Got principal '{}'.", subContext.getPrincipalName() );
                LOG_KRB.debug( "Got principal '{}'.", subContext.getPrincipalName() );
            }
        }
    }


    /**
     * Lookup the principal's attributes that are relevant to executing key derivation.
     *
     * @param modContext The original ModifyContext
     * @param subContext The modification container
     * @throws LdapException If we get an exception
     */
    private void lookupPrincipalAttributes( ModifyOperationContext modContext, ModifySubContext subContext )
        throws LdapException
    {
        Dn principalDn = modContext.getDn();

        LookupOperationContext lookupContext = modContext.newLookupContext( principalDn,
            SchemaConstants.OBJECT_CLASS_AT,
            KerberosAttribute.KRB5_PRINCIPAL_NAME_AT,
            KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT );
        lookupContext.setPartition( modContext.getPartition() );
        lookupContext.setTransaction( modContext.getTransaction() );

        Entry userEntry = directoryService.getPartitionNexus().lookup( lookupContext );

        if ( userEntry == null )
        {
            throw new LdapAuthenticationException( I18n.err( I18n.ERR_512, principalDn ) );
        }

        if ( !( ( ClonedServerEntry ) userEntry ).getOriginalEntry().contains(
            directoryService.getAtProvider().getObjectClass(), SchemaConstants.KRB5_PRINCIPAL_OC ) )
        {
            return;
        }
        else
        {
            subContext.isPrincipal( true );
            LOG.debug( "Dn {} is a Kerberos principal.  Will attempt key derivation.", principalDn.getName() );
            LOG_KRB.debug( "Dn {} is a Kerberos principal.  Will attempt key derivation.", principalDn.getName() );
        }

        if ( subContext.getPrincipalName() == null )
        {
            Attribute principalAttribute = ( ( ClonedServerEntry ) userEntry ).getOriginalEntry().get(
                krb5PrincipalNameAT );
            String principalName = principalAttribute.getString();
            subContext.setPrincipalName( principalName );
            LOG.debug( "Found principal '{}' from lookup.", principalName );
            LOG_KRB.debug( "Found principal '{}' from lookup.", principalName );
        }

        Attribute keyVersionNumberAttr = ( ( ClonedServerEntry ) userEntry ).getOriginalEntry().get(
            krb5KeyVersionNumberAT );

        // Set the KVNO to 0 if it's a password creation,
        // otherwise increment it.
        if ( keyVersionNumberAttr == null )
        {
            subContext.setNewKeyVersionNumber( 0 );
            LOG.debug( "Key version number was null, setting to 0." );
            LOG_KRB.debug( "Key version number was null, setting to 0." );
        }
        else
        {
            int oldKeyVersionNumber = Integer.parseInt( keyVersionNumberAttr.getString() );
            int newKeyVersionNumber = oldKeyVersionNumber + 1;
            subContext.setNewKeyVersionNumber( newKeyVersionNumber );
            LOG.debug( "Found key version number '{}', setting to '{}'.", oldKeyVersionNumber, newKeyVersionNumber );
            LOG_KRB.debug( "Found key version number '{}', setting to '{}'.", oldKeyVersionNumber, newKeyVersionNumber );
        }
    }


    /**
     * Use the 'userPassword' and 'krb5PrincipalName' attributes to derive Kerberos keys for the principal.
     *
     * If the 'userPassword' is the special keyword 'randomKey', set random keys for the principal.
     *
     * @param modContext The original ModifyContext
     * @param subContext The modification container
     */
    void deriveKeys( ModifyOperationContext modContext, ModifySubContext subContext ) throws LdapException
    {
        List<Modification> mods = modContext.getModItems();

        String principalName = subContext.getPrincipalName();
        String userPassword = subContext.getUserPassword();
        int kvno = subContext.getNewKeyVersionNumber();

        LOG.debug( "Got principal '{}' with userPassword '{}'.", principalName, userPassword );
        LOG_KRB.debug( "Got principal '{}' with userPassword '{}'.", principalName, userPassword );

        Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, userPassword );

        List<Modification> newModsList = new ArrayList<>();

        // Make sure we preserve any other modification items.
        for ( Modification mod : mods )
        {
            newModsList.add( mod );
        }

        // Add our modification items.
        Modification krb5PrincipalName =
            new DefaultModification(
                ModificationOperation.REPLACE_ATTRIBUTE,
                new DefaultAttribute(
                    krb5PrincipalNameAT,
                    principalName ) );
        newModsList.add( krb5PrincipalName );

        Modification krb5KeyVersionNumber =
            new DefaultModification(
                ModificationOperation.REPLACE_ATTRIBUTE,
                new DefaultAttribute(
                    krb5KeyVersionNumberAT,
                    Integer.toString( kvno ) ) );

        newModsList.add( krb5KeyVersionNumber );

        Attribute attribute = getKeyAttribute( keys );
        newModsList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute ) );

        LOG.debug( "Added two modifications to the current request : {} and {}", krb5PrincipalName,
            krb5KeyVersionNumber );
        LOG_KRB.debug( "Added two modifications to the current request : {} and {}", krb5PrincipalName,
            krb5KeyVersionNumber );

        modContext.setModItems( newModsList );
    }


    /**
     * Create the KRB5_KEY attribute with all the associated keys.
     *  
     * @param keys The keys to inject in the attribute
     * @return The create attribute
     * @throws LdapException If we had an error while adding a key in the attribute
     */
    private Attribute getKeyAttribute( Map<EncryptionType, EncryptionKey> keys )
        throws LdapException
    {
        Attribute keyAttribute = new DefaultAttribute( krb5KeyAT );

        for ( EncryptionKey encryptionKey : keys.values() )
        {
            try
            {
                ByteBuffer buffer = ByteBuffer.allocate( encryptionKey.computeLength() );
                encryptionKey.encode( buffer );
                keyAttribute.add( buffer.array() );
            }
            catch ( EncoderException ioe )
            {
                LOG.error( I18n.err( I18n.ERR_122 ), ioe );
                LOG_KRB.error( I18n.err( I18n.ERR_122 ), ioe );
            }
        }

        return keyAttribute;
    }


    /**
     * Generate the keys.
     * 
     * @param principalName The Principal
     * @param userPassword Its password
     * @return A Map of keys
     */
    private Map<EncryptionType, EncryptionKey> generateKeys( String principalName, String userPassword )
    {
        if ( userPassword.equalsIgnoreCase( "randomKey" ) )
        {
            // Generate random key.
            try
            {
                return RandomKeyFactory.getRandomKeys();
            }
            catch ( KerberosException ke )
            {
                LOG.debug( ke.getLocalizedMessage(), ke );
                LOG_KRB.debug( ke.getLocalizedMessage(), ke );

                return null;
            }
        }
        else
        {
            // Derive key based on password and principal name.
            return KerberosKeyFactory.getKerberosKeys( principalName, userPassword );
        }
    }

    /**
     * A ModifyContext used to store the changes made to the original context. This
     * is used while processing a ModifyOperation and will be injected in the
     * original ModifyContext.
     */
    static class ModifySubContext
    {
        /** Tells if this is a principal */
        private boolean isPrincipal = false;

        /** The Principal name */
        private String principalName;

        /** The User password */
        private String userPassword;

        /** The Key version */
        private int newKeyVersionNumber = -1;


        boolean isPrincipal()
        {
            return isPrincipal;
        }


        void isPrincipal( boolean isPrincipal )
        {
            this.isPrincipal = isPrincipal;
        }


        String getPrincipalName()
        {
            return principalName;
        }


        void setPrincipalName( String principalName )
        {
            this.principalName = principalName;
        }


        String getUserPassword()
        {
            return userPassword;
        }


        void setUserPassword( String userPassword )
        {
            this.userPassword = userPassword;
        }


        int getNewKeyVersionNumber()
        {
            return newKeyVersionNumber;
        }


        void setNewKeyVersionNumber( int newKeyVersionNumber )
        {
            this.newKeyVersionNumber = newKeyVersionNumber;
        }


        boolean hasValues()
        {
            return ( userPassword != null ) && ( principalName != null ) && ( newKeyVersionNumber > -1 );
        }
    }
}
