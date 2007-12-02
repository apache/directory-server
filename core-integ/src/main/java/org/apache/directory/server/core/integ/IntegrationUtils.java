/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.integ;


import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.authn.LdapPrincipal;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.message.AttributeImpl;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import java.io.File;
import java.io.IOException;


/**
 * Integration test utility methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class IntegrationUtils
{
    private static final Logger LOG = LoggerFactory.getLogger( IntegrationUtils.class );


    /**
     * Deletes the working directory.
     *
     * @param wkdir the working directory to delete
     * @throws IOException if the working directory cannot be deleted
     */
    public static void doDelete( File wkdir ) throws IOException
    {
        if ( wkdir.exists() )
        {
            try
            {
                FileUtils.deleteDirectory( wkdir );
            }
            catch ( IOException e )
            {
                LOG.error( "Failed to delete the working directory.", e );
            }
        }
        if ( wkdir.exists() )
        {
            throw new IOException( "Failed to delete: " + wkdir );
        }
    }


    public static Entry getUserAddLdif() throws InvalidNameException
    {
        return getUserAddLdif( "uid=akarasulu,ou=users,ou=system", "test".getBytes(), "Alex Karasulu", "Karasulu" );
    }


    public static LdapContext getContext( String principalDn, DirectoryService service, String dn )
            throws NamingException
    {
        if ( principalDn == null )
        {
            principalDn = "";
        }

        LdapDN userDn = new LdapDN( principalDn );
        userDn.normalize( service.getRegistries().getAttributeTypeRegistry().getNormalizerMapping() );
        LdapPrincipal principal = new LdapPrincipal( userDn, AuthenticationLevel.SIMPLE );

        if ( dn == null )
        {
            dn = "";
        }

        return service.getJndiContext( principal, dn );
    }


    public static LdapContext getSystemContext( DirectoryService service ) throws NamingException
    {
        return getContext( "uid=admin,ou=system", service, "ou=system" );
    }


    public static LdapContext getRootContext( DirectoryService service ) throws NamingException
    {
        return getContext( "uid=admin,ou=system", service, "" );
    }


    public static void apply( LdapContext root, Entry entry ) throws NamingException
    {
        LdapDN dn = new LdapDN( entry.getDn() );

        switch( entry.getChangeType().getChangeType() )
        {
            case( ChangeType.ADD_ORDINAL ):
                root.createSubcontext( dn, entry.getAttributes() );
                break;
            case( ChangeType.DELETE_ORDINAL ):
                root.destroySubcontext( entry.getDn() );
                break;
            case( ChangeType.MODDN_ORDINAL ):
                LdapDN target = new LdapDN( entry.getNewSuperior() );
                if ( entry.getNewRdn() != null )
                {
                    target.add( entry.getNewRdn() );
                }
                else
                {
                    target.add( dn.getRdn().toString() );
                }

                if ( entry.isDeleteOldRdn() )
                {
                    root.addToEnvironment( "java.naming.ldap.deleteRDN", "true" );
                }
                else
                {
                    root.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
                }

                root.rename( dn, target );
                break;
            case( ChangeType.MODRDN_ORDINAL ):
                target = ( LdapDN ) dn.clone();
                target.remove( dn.size() - 1 );
                target.add( entry.getNewRdn() );

                if ( entry.isDeleteOldRdn() )
                {
                    root.addToEnvironment( "java.naming.ldap.deleteRDN", "true" );
                }
                else
                {
                    root.addToEnvironment( "java.naming.ldap.deleteRDN", "false" );
                }

                root.rename( dn, target );
                break;
            case( ChangeType.MODIFY_ORDINAL ):
                root.modifyAttributes( dn, entry.getModificationItemsArray() );
                break;

            default:
                throw new IllegalStateException( "Unidentified change type value: " + entry.getChangeType() );
        }
    }


    public static Entry getUserAddLdif( String dnstr, byte[] password, String cn, String sn )
            throws InvalidNameException
    {
        LdapDN dn = new LdapDN( dnstr );
        Entry ldif = new Entry();
        ldif.setDn( dnstr );
        ldif.setChangeType( ChangeType.Add );

        AttributeImpl attr = new AttributeImpl( "objectClass", "top" );
        attr.add( "person" );
        attr.add( "organizationalPerson" );
        attr.add( "inetOrgPerson" );
        ldif.addAttribute( attr );

        attr = new AttributeImpl( "ou", "Engineering" );
        attr.add( "People" );
        ldif.addAttribute( attr );

        String uid = ( String ) dn.getRdn().getValue();
        ldif.putAttribute( "uid", uid );

        ldif.putAttribute( "l", "Bogusville" );
        ldif.putAttribute( "cn", cn );
        ldif.putAttribute( "sn", sn );
        ldif.putAttribute( "mail", uid + "@apache.org" );
        ldif.putAttribute( "telephoneNumber", "+1 408 555 4798" );
        ldif.putAttribute( "facsimileTelephoneNumber", "+1 408 555 9751" );
        ldif.putAttribute( "roomnumber", "4612" );
        ldif.putAttribute( "userPassword", password );

        String givenName = cn.split( " " )[0];
        ldif.putAttribute( "givenName", givenName );
        return ldif;
    }
}
