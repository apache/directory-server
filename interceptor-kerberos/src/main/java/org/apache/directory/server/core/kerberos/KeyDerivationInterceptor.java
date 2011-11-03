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

import org.apache.directory.server.core.api.entry.ClonedServerEntry;
import org.apache.directory.server.core.api.interceptor.BaseInterceptor;
import org.apache.directory.server.core.api.interceptor.Interceptor;
import org.apache.directory.server.core.api.interceptor.NextInterceptor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.shared.asn1.EncoderException;
import org.apache.directory.shared.kerberos.codec.types.EncryptionType;
import org.apache.directory.shared.kerberos.components.EncryptionKey;
import org.apache.directory.shared.kerberos.exceptions.KerberosException;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.BinaryValue;
import org.apache.directory.shared.ldap.model.entry.DefaultAttribute;
import org.apache.directory.shared.ldap.model.entry.DefaultModification;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.entry.Attribute;
import org.apache.directory.shared.ldap.model.entry.Modification;
import org.apache.directory.shared.ldap.model.entry.ModificationOperation;
import org.apache.directory.shared.ldap.model.entry.StringValue;
import org.apache.directory.shared.ldap.model.entry.Value;
import org.apache.directory.shared.ldap.model.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.apache.directory.shared.util.Strings;
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
    private static final Logger log = LoggerFactory.getLogger( KeyDerivationInterceptor.class );

    /** The service name. */
    public static final String NAME = "keyDerivationService";

    /**
     * Intercept the addition of the 'userPassword' and 'krb5PrincipalName' attributes.  Use the 'userPassword'
     * and 'krb5PrincipalName' attributes to derive Kerberos keys for the principal.  If the 'userPassword' is
     * the special keyword 'randomKey', set random keys for the principal.  Set the key version number (kvno)
     * to '0'.
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        Dn normName = addContext.getDn();

        Entry entry = addContext.getEntry();

        if ( ( entry.get( SchemaConstants.USER_PASSWORD_AT ) != null ) &&
            ( entry.get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ) != null ) )
        {
            log.debug( "Adding the entry '{}' for Dn '{}'.", entry, normName.getName() );

            BinaryValue userPassword = (BinaryValue)entry.get( SchemaConstants.USER_PASSWORD_AT ).get();
            String strUserPassword = userPassword.getString();

            if ( log.isDebugEnabled() )
            {
                StringBuffer sb = new StringBuffer();
                sb.append( "'" + strUserPassword + "' ( " );
                sb.append( userPassword );
                sb.append( " )" );
                log.debug( "Adding Attribute id : 'userPassword',  Values : [ {} ]", sb.toString() );
            }

            Value<?> principalNameValue = entry.get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ).get();

            String principalName = principalNameValue.getString();

            log.debug( "Got principal '{}' with userPassword '{}'.", principalName, strUserPassword );

            Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, strUserPassword );

            entry.put( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, principalName );
            entry.put( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, "0" );

            entry.put( getKeyAttribute( addContext.getSession().getDirectoryService().getSchemaManager(), keys ) );

            log.debug( "Adding modified entry '{}' for Dn '{}'.", entry, normName
                .getName() );
        }

        next.add( addContext );
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
    public void modify( NextInterceptor next, ModifyOperationContext modContext ) throws LdapException
    {
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

        next.modify( modContext );
    }


    /**
     * Detect password modification by checking the modify request for the 'userPassword'.  Additionally,
     * check to see if a 'krb5PrincipalName' was provided.
     *
     * @param modContext
     * @param subContext
     * @throws LdapException
     */
    void detectPasswordModification( ModifyOperationContext modContext, ModifySubContext subContext )
        throws LdapException
    {
        List<Modification> mods = modContext.getModItems();

        String operation = null;

        // Loop over attributes being modified to pick out 'userPassword' and 'krb5PrincipalName'.
        for ( Modification mod:mods )
        {
            if ( log.isDebugEnabled() )
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
                }
            }

            Attribute attr = mod.getAttribute();

            if ( SchemaConstants.USER_PASSWORD_AT_OID.equals( attr.getAttributeType().getOid() ) )
            {
                Object firstValue = attr.get();
                String password = null;

                if ( firstValue instanceof StringValue )
                {
                    password = ((StringValue)firstValue).getString();
                    log.debug( "{} Attribute id : 'userPassword',  Values : [ '{}' ]", operation, password );
                }
                else if ( firstValue instanceof BinaryValue )
                {
                    password = ((BinaryValue)firstValue).getString();

                    if ( log.isDebugEnabled() )
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append( "'" + password + "' ( " );
                        sb.append( Strings.dumpBytes(((BinaryValue) firstValue).getBytes()).trim() );
                        sb.append( " )" );
                        log.debug( "{} Attribute id : 'userPassword',  Values : [ {} ]", operation, sb.toString() );
                    }
                }

                subContext.setUserPassword( password );
                log.debug( "Got userPassword '{}'.", subContext.getUserPassword() );
            }

            if ( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT_OID.equals( attr.getAttributeType().getOid() ) )
            {
                subContext.setPrincipalName( attr.getString() );
                log.debug( "Got principal '{}'.", subContext.getPrincipalName() );
            }
        }
    }


    /**
     * Lookup the principal's attributes that are relevant to executing key derivation.
     *
     * @param modContext
     * @param subContext
     * @throws LdapException
     */
    void lookupPrincipalAttributes( ModifyOperationContext modContext, ModifySubContext subContext )
        throws LdapException
    {
        Dn principalDn = modContext.getDn();

        LookupOperationContext lookupContext = modContext.newLookupContext( principalDn );
        //lookupContext.setByPassed( USERLOOKUP_BYPASS );
        lookupContext.setAttrsId( new String[]
        {
            SchemaConstants.OBJECT_CLASS_AT,
            KerberosAttribute.KRB5_PRINCIPAL_NAME_AT,
            KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT
        } );

        Entry userEntry = directoryService.getPartitionNexus().lookup( lookupContext );

        if ( userEntry == null )
        {
            throw new LdapAuthenticationException( I18n.err( I18n.ERR_512, principalDn ) );
        }

        Attribute objectClass = ((ClonedServerEntry)userEntry).getOriginalEntry().get( SchemaConstants.OBJECT_CLASS_AT );

        if ( !objectClass.contains( SchemaConstants.KRB5_PRINCIPAL_OC ) )
        {
            return;
        }
        else
        {
            subContext.isPrincipal( true );
            log.debug( "Dn {} is a Kerberos principal.  Will attempt key derivation.", principalDn.getName() );
        }

        if ( subContext.getPrincipalName() == null )
        {
            Attribute principalAttribute = ((ClonedServerEntry)userEntry).getOriginalEntry().get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT );
            String principalName = principalAttribute.getString();
            subContext.setPrincipalName( principalName );
            log.debug( "Found principal '{}' from lookup.", principalName );
        }

        Attribute keyVersionNumberAttr = ((ClonedServerEntry)userEntry).getOriginalEntry().get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT );

        if ( keyVersionNumberAttr == null )
        {
            subContext.setNewKeyVersionNumber( 0 );
            log.debug( "Key version number was null, setting to 0." );
        }
        else
        {
            int oldKeyVersionNumber = Integer.valueOf( keyVersionNumberAttr.getString() );
            int newKeyVersionNumber = oldKeyVersionNumber + 1;
            subContext.setNewKeyVersionNumber( newKeyVersionNumber );
            log.debug( "Found key version number '{}', setting to '{}'.", oldKeyVersionNumber, newKeyVersionNumber );
        }
    }


    /**
     * Use the 'userPassword' and 'krb5PrincipalName' attributes to derive Kerberos keys for the principal.
     *
     * If the 'userPassword' is the special keyword 'randomKey', set random keys for the principal.
     *
     * @param modContext
     * @param subContext
     */
    void deriveKeys( ModifyOperationContext modContext, ModifySubContext subContext ) throws LdapException
    {
        List<Modification> mods = modContext.getModItems();

        String principalName = subContext.getPrincipalName();
        String userPassword = subContext.getUserPassword();
        int kvno = subContext.getNewKeyVersionNumber();

        log.debug( "Got principal '{}' with userPassword '{}'.", principalName, userPassword );

        Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, userPassword );

        List<Modification> newModsList = new ArrayList<Modification>();

        // Make sure we preserve any other modification items.
        for ( Modification mod:mods )
        {
            newModsList.add( mod );
        }

        SchemaManager schemaManager = modContext.getSession()
            .getDirectoryService().getSchemaManager();

        // Add our modification items.
        newModsList.add(
            new DefaultModification(
                ModificationOperation.REPLACE_ATTRIBUTE,
                new DefaultAttribute(
                    KerberosAttribute.KRB5_PRINCIPAL_NAME_AT,
                    schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ),
                    principalName ) ) );
        newModsList.add(
            new DefaultModification(
                ModificationOperation.REPLACE_ATTRIBUTE,
                new DefaultAttribute(
                    KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT,
                    schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ),
                    Integer.toString( kvno ) ) ) );

        Attribute attribute = getKeyAttribute( modContext.getSession()
            .getDirectoryService().getSchemaManager(), keys );
        newModsList.add( new DefaultModification( ModificationOperation.REPLACE_ATTRIBUTE, attribute ) );

        modContext.setModItems( newModsList );
    }


    private Attribute getKeyAttribute( SchemaManager schemaManager, Map<EncryptionType, EncryptionKey> keys ) throws LdapException
    {
        Attribute keyAttribute =
            new DefaultAttribute( KerberosAttribute.KRB5_KEY_AT,
                schemaManager.lookupAttributeTypeRegistry( KerberosAttribute.KRB5_KEY_AT ) );

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
                log.error( I18n.err( I18n.ERR_122 ), ioe );
            }
        }

        return keyAttribute;
    }


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
                log.debug( ke.getLocalizedMessage(), ke );
                return null;
            }
        }
        else
        {
            // Derive key based on password and principal name.
            return KerberosKeyFactory.getKerberosKeys( principalName, userPassword );
        }
    }

    static class ModifySubContext
    {
        private boolean isPrincipal = false;
        private String principalName;
        private String userPassword;
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
            return userPassword != null && principalName != null && newKeyVersionNumber > -1;
        }
    }
}
