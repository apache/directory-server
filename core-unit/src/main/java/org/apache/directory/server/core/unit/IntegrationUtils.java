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
package org.apache.directory.server.core.unit;


import java.io.File;
import java.io.IOException;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


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


    public static LdifEntry getUserAddLdif() throws InvalidNameException, NamingException
    {
        return getUserAddLdif( "uid=akarasulu,ou=users,ou=system", "test".getBytes(), "Alex Karasulu", "Karasulu" );
    }



    public static void apply( CoreSession root, LdifEntry entry ) throws Exception
    {
        LdapDN dn = new LdapDN( entry.getDn() );

        switch( entry.getChangeType().getChangeType() )
        {
            case( ChangeType.ADD_ORDINAL ):
                root.add( 
                    new DefaultServerEntry( 
                        root.getDirectoryService().getSchemaManager(), entry.getEntry() ) ); 
                break;
                
            case( ChangeType.DELETE_ORDINAL ):
                root.delete( entry.getDn() );
                break;
                
            case( ChangeType.MODDN_ORDINAL ):
            case( ChangeType.MODRDN_ORDINAL ):
                Rdn newRdn = new Rdn( entry.getNewRdn() );
                
                if ( entry.getNewSuperior() != null )
                {
                    // It's a move. The superior have changed
                    // Let's see if it's a rename too
                    Rdn oldRdn = dn.getRdn();
                    LdapDN newSuperior = new LdapDN( entry.getNewSuperior() );
                    
                    if ( dn.size() == 0 )
                    {
                        throw new IllegalStateException( "can't move the root DSE" );
                    }
                    else if ( oldRdn.equals( newRdn ) )
                    {
                        // Same rdn : it's a move
                        root.move( dn, newSuperior );
                    }
                    else
                    {
                        // it's a move and rename 
                        root.moveAndRename( dn, newSuperior, newRdn, entry.isDeleteOldRdn() );
                    }
                }
                else
                {
                    // it's a rename
                    root.rename( dn, newRdn, entry.isDeleteOldRdn() );
                }
                
                break;
                
            case( ChangeType.MODIFY_ORDINAL ):
                root.modify( dn, entry.getModificationItems() );
                break;

            default:
                throw new IllegalStateException( "Unidentified change type value: " + entry.getChangeType() );
        }
    }


    public static LdifEntry getUserAddLdif( String dnstr, byte[] password, String cn, String sn )
            throws InvalidNameException, NamingException
    {
        LdapDN dn = new LdapDN( dnstr );
        LdifEntry ldif = new LdifEntry();
        ldif.setDn( dnstr );
        ldif.setChangeType( ChangeType.Add );

        EntryAttribute attr = new DefaultClientAttribute( "objectClass", 
            "top", "person", "organizationalPerson", "inetOrgPerson" );
        
        ldif.addAttribute( attr );

        attr = new DefaultClientAttribute( "ou", "Engineering", "People" );
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
