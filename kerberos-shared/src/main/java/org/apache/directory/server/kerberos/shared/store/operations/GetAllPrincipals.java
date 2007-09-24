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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InvalidAttributeValueException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.security.auth.kerberos.KerberosPrincipal;

import org.apache.directory.server.kerberos.shared.crypto.encryption.EncryptionType;
import org.apache.directory.server.kerberos.shared.messages.value.EncryptionKey;
import org.apache.directory.server.kerberos.shared.messages.value.types.SamType;
import org.apache.directory.server.kerberos.shared.store.KerberosAttribute;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntry;
import org.apache.directory.server.kerberos.shared.store.PrincipalStoreEntryModifier;
import org.apache.directory.server.protocol.shared.store.ContextOperation;
import org.apache.directory.shared.ldap.constants.SchemaConstants;


/**
 * Command for getting all principals in a JNDI context.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class GetAllPrincipals implements ContextOperation
{
    private static final long serialVersionUID = -1214321426487445132L;

    private static final String filter = "(objectClass=krb5Principal)";


    public Object execute( DirContext ctx, Name searchBaseDn )
    {
        SearchControls controls = new SearchControls();

        List<PrincipalStoreEntry> answers = new ArrayList<PrincipalStoreEntry>();

        try
        {
            Attributes attrs = null;

            NamingEnumeration answer = ctx.search( searchBaseDn, filter, controls );

            while ( answer.hasMore() )
            {
                SearchResult result = ( SearchResult ) answer.next();
                attrs = result.getAttributes();
                PrincipalStoreEntry entry = getEntry( attrs );
                answers.add( entry );
            }

            answer.close();

            PrincipalStoreEntry[] entries = new PrincipalStoreEntry[answers.size()];

            return answers.toArray( entries );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();

            return null;
        }
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
        PrincipalStoreEntryModifier modifier = new PrincipalStoreEntryModifier();

        String principal = ( String ) attrs.get( KerberosAttribute.PRINCIPAL ).get();
        String keyVersionNumber = ( String ) attrs.get( KerberosAttribute.VERSION ).get();

        String commonName = ( String ) attrs.get( SchemaConstants.CN_AT ).get();

        if ( attrs.get( "apacheSamType" ) != null )
        {
            String samType = ( String ) attrs.get( "apacheSamType" ).get();

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

        modifier.setCommonName( commonName );
        modifier.setPrincipal( new KerberosPrincipal( principal ) );
        modifier.setKeyVersionNumber( Integer.parseInt( keyVersionNumber ) );

        return modifier.getEntry();
    }
}
