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
package org.apache.directory.server.kerberos.shared.store.operations;


import java.io.IOException;
import java.text.ParseException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.SamType;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.server.schema.registries.AttributeTypeRegistry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeTypeOptions;


/**
 * Encapsulates the action of looking up a principal in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetPrincipal implements ContextOperation
{
    private static final long serialVersionUID = 4598007518413451945L;

    /** The name of the principal to get. */
    private final KerberosPrincipal principal;


    /**
     * Creates the action to be used against the embedded ApacheDS DIT.
     * 
     * @param principal The principal to search for in the directory.
     */
    public GetPrincipal( KerberosPrincipal principal )
    {
        this.principal = principal;
    }


    /**
     * Note that the base is a relative path from the existing context.
     * It is not a DN.
     */
    public Object execute( CoreSession session, LdapDN base )
    {
        if ( principal == null )
        {
            return null;
        }

        String[] attrIDs =
            {   
                KerberosAttribute.KRB5_PRINCIPAL_NAME_AT, 
                KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT, 
                KerberosAttribute.KRB5_KEY_AT,
                KerberosAttribute.APACHE_SAM_TYPE_AT, 
                KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT,
                KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT, 
                KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT 
            };

        Set<AttributeTypeOptions> matchAttrs = new HashSet<AttributeTypeOptions>();
        AttributeTypeRegistry atRegistry = session.getDirectoryService().getRegistries().getAttributeTypeRegistry();
        AttributeTypeOptions krb5PrincipalAT = null;
        
        try
        {
            krb5PrincipalAT = new AttributeTypeOptions( atRegistry.lookup( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ) );
        }
        catch ( NamingException ne )
        {
            return null;
        }
        
        matchAttrs.add( krb5PrincipalAT );

        PrincipalStoreEntry entry = null;

        try
        {
            EntryFilteringCursor cursor = session.list( LdapDN.EMPTY_LDAPDN, AliasDerefMode.DEREF_ALWAYS, matchAttrs );

            cursor.beforeFirst();
            
            if ( cursor.next() )
            {
                ClonedServerEntry result = cursor.get();
                
                if ( !result.containsAttribute( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ) )
                {
                    return null;
                }
                
                entry = getEntry( result );
            }
        }
        catch ( Exception e )
        {
            return null;
        }

        return entry;
    }


    /**
     * Marshals an a PrincipalStoreEntry from an Attributes object.
     *
     * @param dn the distinguished name of the Kerberos principal
     * @param attrs the attributes of the Kerberos principal
     * @return the entry for the principal
     * @throws NamingException if there are any access problems
     */
    private PrincipalStoreEntry getEntry( ServerEntry entry ) throws NamingException
    {
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();

        modifier.setDistinguishedName( entry.getDn().getUpName() );

        String principal = entry.get( KerberosAttribute.KRB5_PRINCIPAL_NAME_AT ).getString();
        modifier.setPrincipal( new KerberosPrincipal( principal ) );

        String keyVersionNumber = entry.get( KerberosAttribute.KRB5_KEY_VERSION_NUMBER_AT ).getString();
        modifier.setKeyVersionNumber( Integer.parseInt( keyVersionNumber ) );

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_DISABLED_AT ).getString();
            modifier.setDisabled( "true".equalsIgnoreCase( val ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_LOCKEDOUT_AT ).getString();
            modifier.setLockedOut( "true".equalsIgnoreCase( val ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ) != null )
        {
            String val = entry.get( KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT ).getString();
            try
            {
                modifier.setExpiration( KerberosTime.getTime( val ) );
            }
            catch ( ParseException e )
            {
                throw new InvalidAttributeValueException( "Account expiration attribute "
                    + KerberosAttribute.KRB5_ACCOUNT_EXPIRATION_TIME_AT + " contained an invalid value for generalizedTime: "
                    + val );
            }
        }

        if ( entry.get( KerberosAttribute.APACHE_SAM_TYPE_AT ) != null )
        {
            String samType = entry.get( KerberosAttribute.APACHE_SAM_TYPE_AT ).getString();
            modifier.setSamType( SamType.getTypeByOrdinal( Integer.parseInt( samType ) ) );
        }

        if ( entry.get( KerberosAttribute.KRB5_KEY_AT ) != null )
        {
            EntryAttribute krb5key = entry.get( KerberosAttribute.KRB5_KEY_AT );
            
            try
            {
                Map<EncryptionType, EncryptionKey> keyMap = modifier.reconstituteKeyMap( krb5key );
                modifier.setKeyMap( keyMap );
            }
            catch ( IOException ioe )
            {
                throw new InvalidAttributeValueException( "Account Kerberos key attribute '" + KerberosAttribute.KRB5_KEY_AT
                    + "' contained an invalid value for krb5key." );
            }
        }

        return modifier.getEntry();
    }
}
