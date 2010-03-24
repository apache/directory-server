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
import java.util.Map;

import javax.naming.NamingException;
import javax.naming.directory.InvalidAttributeValueException;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.SamType;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.server.protocol.shared.store.DirectoryServiceOperation;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.name.DN;


/**
 * Encapsulates the action of looking up a principal in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetPrincipal implements DirectoryServiceOperation
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
    public Object execute( CoreSession session, DN base ) throws Exception
    {
        if ( principal == null )
        {
            return null;
        }

        return getEntry( StoreUtils.findPrincipalEntry( session, base, principal.getName() ) );
    }


    /**
     * Marshals an a PrincipalStoreEntry from an Attributes object.
     *
     * @param dn the distinguished name of the Kerberos principal
     * @param attrs the attributes of the Kerberos principal
     * @return the entry for the principal
     * @throws NamingException if there are any access problems
     */
    private PrincipalStoreEntry getEntry( ServerEntry entry ) throws Exception
    {
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();

        modifier.setDistinguishedName( entry.getDn().getName() );

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
                throw new InvalidAttributeValueException( I18n.err( I18n.ERR_623, KerberosAttribute.KRB5_KEY_AT ) );
            }
        }

        return modifier.getEntry();
    }
}
