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


import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;

import org.apache.directory.server.core.configuration.StartupConfiguration;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.invocation.Invocation;
import org.apache.directory.server.core.invocation.InvocationStack;
import org.apache.directory.server.core.partition.PartitionNexusProxy;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.crypto.encryption.KerberosKeyFactory;
import org.apache.directory.server.kerberos.shared.crypto.encryption.RandomKeyFactory;
import org.apache.directory.server.kerberos.shared.exceptions.KerberosException;
import org.apache.directory.server.kerberos.shared.io.encoder.EncryptionKeyEncoder;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.exception.LdapAuthenticationException;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.ModificationItemImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;
import org.apache.directory.shared.ldap.util.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An {@link Interceptor} that creates symmetric Kerberos keys for users.  When a
 * 'userPassword' is added or modified, the 'userPassword' and 'krb5PrincipalName'
 * are used to derive Kerberos keys.  If the 'userPassword' is the special keyword
 * 'randomKey', a random key is generated and used as the Kerberos key.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class KeyDerivationService extends BaseInterceptor
{
    /** The log for this class. */
    private static final Logger log = LoggerFactory.getLogger( KeyDerivationService.class );

    /** The service name. */
    public static final String NAME = "keyDerivationService";

    /**
     * Define the interceptors to bypass upon user lookup.
     */
    private static final Collection<String> USERLOOKUP_BYPASS;
    static
    {
        Set<String> c = new HashSet<String>();
        c.add( StartupConfiguration.NORMALIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.AUTHENTICATION_SERVICE_NAME );
        c.add( StartupConfiguration.REFERRAL_SERVICE_NAME );
        c.add( StartupConfiguration.AUTHORIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.DEFAULT_AUTHORIZATION_SERVICE_NAME );
        c.add( StartupConfiguration.EXCEPTION_SERVICE_NAME );
        c.add( StartupConfiguration.OPERATIONAL_ATTRIBUTE_SERVICE_NAME );
        c.add( StartupConfiguration.SCHEMA_SERVICE_NAME );
        c.add( StartupConfiguration.SUBENTRY_SERVICE_NAME );
        c.add( StartupConfiguration.COLLECTIVE_ATTRIBUTE_SERVICE_NAME );
        c.add( StartupConfiguration.EVENT_SERVICE_NAME );
        c.add( StartupConfiguration.TRIGGER_SERVICE_NAME );
        USERLOOKUP_BYPASS = Collections.unmodifiableCollection( c );
    }


    /**
     * Intercept the addition of the 'userPassword' and 'krb5PrincipalName' attributes.  Use the 'userPassword'
     * and 'krb5PrincipalName' attributes to derive Kerberos keys for the principal.  If the 'userPassword' is
     * the special keyword 'randomKey', set random keys for the principal.  Set the key version number (kvno)
     * to '0'.
     */
    public void add( NextInterceptor next, OperationContext addContext ) throws NamingException
    {
        LdapDN normName = addContext.getDn();

        Attributes entry = ( ( AddOperationContext ) addContext ).getEntry();

        if ( entry.get( "userPassword" ) != null && entry.get( KerberosAttribute.PRINCIPAL ) != null )
        {
            log.debug( "Adding the entry '{}' for DN '{}'.", AttributeUtils.toString( entry ), normName.getUpName() );

            Object firstValue = entry.get( "userPassword" ).get();

            if ( firstValue instanceof String )
            {
                log.debug( "Adding Attribute id : 'userPassword',  Values : [ '{}' ]", firstValue );
            }
            else if ( firstValue instanceof byte[] )
            {
                String string = StringTools.utf8ToString( ( byte[] ) firstValue );

                if ( log.isDebugEnabled() )
                {
                    StringBuffer sb = new StringBuffer();
                    sb.append( "'" + string + "' ( " );
                    sb.append( StringTools.dumpBytes( ( byte[] ) firstValue ).trim() );
                    sb.append( " )" );
                    log.debug( "Adding Attribute id : 'userPassword',  Values : [ {} ]", sb.toString() );
                }

                firstValue = string;
            }

            String userPassword = ( String ) firstValue;
            String principalName = ( String ) entry.get( KerberosAttribute.PRINCIPAL ).get();

            log.debug( "Got principal '{}' with userPassword '{}'.", principalName, userPassword );

            Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, userPassword );

            entry.put( KerberosAttribute.PRINCIPAL, principalName );
            entry.put( KerberosAttribute.VERSION, Integer.toString( 0 ) );

            entry.put( getKeyAttribute( keys ) );

            log.debug( "Adding modified entry '{}' for DN '{}'.", AttributeUtils.toString( entry ), normName
                .getUpName() );
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
    public void modify( NextInterceptor next, OperationContext opContext ) throws NamingException
    {
        ModifyOperationContext modContext = ( ModifyOperationContext ) opContext;
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
     * @throws NamingException
     */
    void detectPasswordModification( ModifyOperationContext modContext, ModifySubContext subContext )
        throws NamingException
    {
        ModificationItemImpl[] mods = modContext.getModItems();

        String operation = null;

        // Loop over attributes being modified to pick out 'userPassword' and 'krb5PrincipalName'.
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            if ( log.isDebugEnabled() )
            {
                switch ( mods[ii].getModificationOp() )
                {
                    case DirContext.ADD_ATTRIBUTE:
                        operation = "Adding";
                        break;
                    case DirContext.REMOVE_ATTRIBUTE:
                        operation = "Removing";
                        break;
                    case DirContext.REPLACE_ATTRIBUTE:
                        operation = "Replacing";
                        break;
                }
            }

            Attribute attr = mods[ii].getAttribute();
            String attrId = attr.getID();

            if ( attrId.equalsIgnoreCase( "userPassword" ) )
            {
                Object firstValue = attr.get();

                if ( firstValue instanceof String )
                {
                    log.debug( "{} Attribute id : 'userPassword',  Values : [ '{}' ]", operation, firstValue );
                }
                else if ( firstValue instanceof byte[] )
                {
                    String string = StringTools.utf8ToString( ( byte[] ) firstValue );

                    if ( log.isDebugEnabled() )
                    {
                        StringBuffer sb = new StringBuffer();
                        sb.append( "'" + string + "' ( " );
                        sb.append( StringTools.dumpBytes( ( byte[] ) firstValue ).trim() );
                        sb.append( " )" );
                        log.debug( "{} Attribute id : 'userPassword',  Values : [ {} ]", operation, sb.toString() );
                    }

                    firstValue = string;
                }

                subContext.setUserPassword( ( String ) firstValue );
                log.debug( "Got userPassword '{}'.", subContext.getUserPassword() );
            }

            if ( attrId.equalsIgnoreCase( KerberosAttribute.PRINCIPAL ) )
            {
                subContext.setPrincipalName( ( String ) attr.get() );
                log.debug( "Got principal '{}'.", subContext.getPrincipalName() );
            }
        }
    }


    /**
     * Lookup the principal's attributes that are relevant to executing key derivation.
     *
     * @param modContext
     * @param subContext
     * @throws NamingException
     */
    void lookupPrincipalAttributes( ModifyOperationContext modContext, ModifySubContext subContext )
        throws NamingException
    {
        LdapDN principalDn = modContext.getDn();

        Invocation invocation = InvocationStack.getInstance().peek();
        PartitionNexusProxy proxy = invocation.getProxy();
        Attributes userEntry;

        LookupOperationContext lookupContext = new LookupOperationContext( new String[]
            { SchemaConstants.OBJECT_CLASS_AT, KerberosAttribute.PRINCIPAL, KerberosAttribute.VERSION } );
        lookupContext.setDn( principalDn );

        userEntry = proxy.lookup( lookupContext, USERLOOKUP_BYPASS );

        if ( userEntry == null )
        {
            throw new LdapAuthenticationException( "Failed to authenticate user '" + principalDn + "'." );
        }

        Attribute objectClass = userEntry.get( SchemaConstants.OBJECT_CLASS_AT );
        if ( !objectClass.contains( "krb5principal" ) )
        {
            return;
        }
        else
        {
            subContext.isPrincipal( true );
            log.debug( "DN {} is a Kerberos principal.  Will attempt key derivation.", principalDn.getUpName() );
        }

        if ( subContext.getPrincipalName() == null )
        {
            Attribute principalAttribute = userEntry.get( KerberosAttribute.PRINCIPAL );
            String principalName = ( String ) principalAttribute.get();
            subContext.setPrincipalName( principalName );
            log.debug( "Found principal '{}' from lookup.", principalName );
        }

        Attribute keyVersionNumberAttr = userEntry.get( KerberosAttribute.VERSION );

        if ( keyVersionNumberAttr == null )
        {
            subContext.setNewKeyVersionNumber( 0 );
            log.debug( "Key version number was null, setting to 0." );
        }
        else
        {
            int oldKeyVersionNumber = Integer.valueOf( ( String ) keyVersionNumberAttr.get() );
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
    void deriveKeys( ModifyOperationContext modContext, ModifySubContext subContext )
    {
        ModificationItemImpl[] mods = modContext.getModItems();

        String principalName = subContext.getPrincipalName();
        String userPassword = subContext.getUserPassword();
        int kvno = subContext.getNewKeyVersionNumber();

        log.debug( "Got principal '{}' with userPassword '{}'.", principalName, userPassword );

        Map<EncryptionType, EncryptionKey> keys = generateKeys( principalName, userPassword );

        Set<ModificationItemImpl> newModsList = new HashSet<ModificationItemImpl>();

        // Make sure we preserve any other modification items.
        for ( int ii = 0; ii < mods.length; ii++ )
        {
            newModsList.add( mods[ii] );
        }

        // Add our modification items.
        newModsList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl(
            KerberosAttribute.PRINCIPAL, principalName ) ) );
        newModsList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, new AttributeImpl(
            KerberosAttribute.VERSION, Integer.toString( kvno ) ) ) );
        newModsList.add( new ModificationItemImpl( DirContext.REPLACE_ATTRIBUTE, getKeyAttribute( keys ) ) );

        ModificationItemImpl[] newMods = newModsList.toArray( mods );

        modContext.setModItems( newMods );
    }


    private Attribute getKeyAttribute( Map<EncryptionType, EncryptionKey> keys )
    {
        Attribute keyAttribute = new AttributeImpl( KerberosAttribute.KEY );

        Iterator<EncryptionKey> it = keys.values().iterator();

        while ( it.hasNext() )
        {
            try
            {
                keyAttribute.add( EncryptionKeyEncoder.encode( it.next() ) );
            }
            catch ( IOException ioe )
            {
                log.error( "Error encoding EncryptionKey.", ioe );
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
                log.debug( ke.getMessage(), ke );
                return null;
            }
        }
        else
        {
            // Derive key based on password and principal name.
            return KerberosKeyFactory.getKerberosKeys( principalName, userPassword );
        }
    }

    class ModifySubContext
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
