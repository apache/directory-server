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
package org.apache.directory.server.ldap.support.bind;


import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.KerberosTime;
import org.apache.directory.server.kerberos.shared.messages.value.SamType;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.message.AttributesImpl;


/**
 * Encapsulates the action of looking up a principal in an embedded ApacheDS DIT.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 494161 $, $Date: 2007-01-08 11:39:36 -0800 (Mon, 08 Jan 2007) $
 */
public class GetPrincipal implements ContextOperation
{
    private static final long serialVersionUID = 4598007518413451945L;

    /** The name of the principal to get. */
    private final KerberosPrincipal principal;

    private String dn;
    private String userPassword;


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
     * Accessor method for retrieving the DN for the username.
     *
     * @return The DN to bind the user as.
     */
    public String getDn()
    {
        return dn;
    }


    /**
     * Accessor method for retrieving the user's password.
     *
     * @return The user's password.
     */
    public String getUserPassword()
    {
        return userPassword;
    }


    /**
     * Note that the base is a relative path from the existing context.
     * It is not a DN.
     */
    public Object execute( DirContext ctx, Name base )
    {
        if ( principal == null )
        {
            return null;
        }

        String[] attrIDs =
            { KerberosAttribute.PRINCIPAL, KerberosAttribute.VERSION, KerberosAttribute.TYPE, KerberosAttribute.KEY,
                KerberosAttribute.SAM_TYPE, KerberosAttribute.ACCOUNT_DISABLED,
                KerberosAttribute.ACCOUNT_EXPIRATION_TIME, KerberosAttribute.ACCOUNT_LOCKEDOUT, "userPassword" };

        Attributes matchAttrs = new AttributesImpl( true );
        matchAttrs.put( new AttributeImpl( KerberosAttribute.PRINCIPAL, principal.getName() ) );

        PrincipalStoreEntry entry = null;

        try
        {
            NamingEnumeration answer = ctx.search( "", matchAttrs, attrIDs );

            if ( answer.hasMore() )
            {
                SearchResult result = ( SearchResult ) answer.next();

                // Changed from original GetPrincipal, along with accessor and member variable.
                dn = result.getName();

                Attributes attrs = result.getAttributes();

                if ( attrs == null )
                {
                    return null;
                }

                entry = getEntry( attrs );
            }
        }
        catch ( NamingException e )
        {
            return null;
        }

        return entry;
    }


    /**
     * Marshals an a PrincipalStoreEntry from an Attributes object.
     *
     * @param attrs the attributes of the Kerberos principal
     * @return the entry for the principal
     * @throws NamingException if there are any access problems
     */
    private PrincipalStoreEntry getEntry( Attributes attrs ) throws NamingException
    {
        if ( attrs.get( "userPassword" ) != null )
        {
            userPassword = ( String ) attrs.get( "userPassword" ).get();
        }

        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();
        String principal = ( String ) attrs.get( KerberosAttribute.PRINCIPAL ).get();
        String encryptionType = ( String ) attrs.get( KerberosAttribute.TYPE ).get();
        String keyVersionNumber = ( String ) attrs.get( KerberosAttribute.VERSION ).get();

        if ( attrs.get( KerberosAttribute.ACCOUNT_DISABLED ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.ACCOUNT_DISABLED ).get();
            modifier.setDisabled( "true".equalsIgnoreCase( val ) );
        }

        if ( attrs.get( KerberosAttribute.ACCOUNT_LOCKEDOUT ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.ACCOUNT_LOCKEDOUT ).get();
            modifier.setLockedOut( "true".equalsIgnoreCase( val ) );
        }

        if ( attrs.get( KerberosAttribute.ACCOUNT_EXPIRATION_TIME ) != null )
        {
            String val = ( String ) attrs.get( KerberosAttribute.ACCOUNT_EXPIRATION_TIME ).get();
            try
            {
                modifier.setExpiration( KerberosTime.getTime( val ) );
            }
            catch ( ParseException e )
            {
                throw new InvalidAttributeValueException( "Account expiration attribute "
                    + KerberosAttribute.ACCOUNT_EXPIRATION_TIME + " contained an invalid value for generalizedTime: "
                    + val );
            }
        }

        if ( attrs.get( KerberosAttribute.SAM_TYPE ) != null )
        {
            String samType = ( String ) attrs.get( KerberosAttribute.SAM_TYPE ).get();
            modifier.setSamType( SamType.getTypeByOrdinal( Integer.parseInt( samType ) ) );
        }

        if ( attrs.get( KerberosAttribute.KEY ) != null )
        {
            Attribute krb5key = attrs.get( KerberosAttribute.KEY );
            try
            {
                Map<EncryptionType, EncryptionKey> keyMap = modifier.reconstituteKeyMap( krb5key );
                modifier.setKeyMap( keyMap );
            }
            catch ( IOException ioe )
            {
                throw new InvalidAttributeValueException( "Account Kerberos key attribute '" + KerberosAttribute.KEY
                    + "' contained an invalid value for krb5key." );
            }
        }

        modifier.setPrincipal( new KerberosPrincipal( principal ) );
        modifier.setEncryptionType( Integer.parseInt( encryptionType ) );
        modifier.setKeyVersionNumber( Integer.parseInt( keyVersionNumber ) );
        return modifier.getEntry();
    }
}
