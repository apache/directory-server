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


import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.ldap.LdapContext;

import org.apache.commons.io.FileUtils;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.entry.DefaultServerEntry;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.schema.registries.Schema;
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
    /** The class logger */
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


    /**
     * Inject an ldif String into the server. DN must be relative to the
     * root.
     *
     * @param service the directory service to use 
     * @param ldif the ldif containing entries to add to the server.
     * @throws NamingException if there is a problem adding the entries from the LDIF
     */
    public static void injectEntries( DirectoryService service, String ldif ) throws Exception
    {
        LdifReader reader = new LdifReader();
        List<LdifEntry> entries = reader.parseLdif( ldif );

        for ( LdifEntry entry : entries )
        {
            if ( entry.isChangeAdd() )
            {
                service.getAdminSession().add( 
                    new DefaultServerEntry( service.getRegistries(), entry.getEntry() ) );
            }
            else if ( entry.isChangeModify() )
            {
                service.getAdminSession().modify( 
                    entry.getDn(), entry.getModificationItems() );
            }
            else
            {
                String message = "Unsupported changetype found in LDIF: " + 
                    entry.getChangeType();
                LOG.error( message );
                throw new NamingException( message );
            }
        }
    }


    public static LdifEntry getUserAddLdif() throws InvalidNameException, NamingException
    {
        return getUserAddLdif( "uid=akarasulu,ou=users,ou=system", "test".getBytes(), "Alex Karasulu", "Karasulu" );
    }


    public static LdapContext getContext( String principalDn, DirectoryService service, String dn )
            throws Exception
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

        CoreSession session = service.getSession( principal );
        LdapContext ctx = new ServerLdapContext( service, session, new LdapDN( dn ) );
        return ctx;
    }


    public static CoreSession getCoreSession( String principalDn, DirectoryService service, String dn )
        throws Exception
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
        
        CoreSession session = service.getSession( principal );
        return session;
    }


    public static LdapContext getSystemContext( DirectoryService service ) throws Exception
    {
        return getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, ServerDNConstants.SYSTEM_DN );
    }


    public static LdapContext getSchemaContext( DirectoryService service ) throws Exception
    {
        return getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, ServerDNConstants.OU_SCHEMA_DN );
    }


    public static LdapContext getRootContext( DirectoryService service ) throws Exception
    {
        return getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "" );
    }


    public static void apply( DirectoryService service, LdifEntry entry ) throws Exception
    {
        LdapDN dn = new LdapDN( entry.getDn() );
        CoreSession session = service.getAdminSession();

        switch( entry.getChangeType().getChangeType() )
        {
            case( ChangeType.ADD_ORDINAL ):
                session.add( 
                    new DefaultServerEntry( service.getRegistries(), entry.getEntry() ) ); 
                break;
                
            case( ChangeType.DELETE_ORDINAL ):
                session.delete( dn );
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
                        session.move( dn, newSuperior );
                    }
                    else
                    {
                        // it's a move and rename 
                        session.moveAndRename( dn, newSuperior, newRdn, entry.isDeleteOldRdn() );
                    }
                }
                else
                {
                    // it's a rename
                    session.rename( dn, newRdn, entry.isDeleteOldRdn() );
                }
                
                break;

            case( ChangeType.MODIFY_ORDINAL ):
                session.modify( dn, entry.getModificationItems() );
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

    // -----------------------------------------------------------------------
    // Enable/Disable Schema Tests
    // -----------------------------------------------------------------------


    public static void enableSchema( DirectoryService service, String schemaName ) throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // now enable the test schema
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-disabled", "FALSE" );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    public static void disableSchema( DirectoryService service, String schemaName ) throws Exception
    {
        LdapContext schemaRoot = getSchemaContext( service );

        // now enable the test schema
        ModificationItem[] mods = new ModificationItem[1];
        Attribute attr = new BasicAttribute( "m-disabled", "TRUE" );
        mods[0] = new ModificationItem( DirContext.REPLACE_ATTRIBUTE, attr );
        schemaRoot.modifyAttributes( "cn=" + schemaName, mods );
    }
    
    
    /**
     * A helper method which tells if a schema is disabled.
     */
    public static boolean isDisabled( DirectoryService service, String schemaName )
    {
        Schema schema = service.getRegistries().getLoadedSchema( schemaName );
        
        if ( schema != null )
        {
            return schema.isDisabled();
        }
        else
        {
            // If the schema is not loaded, it's disabled
            return false;
        }
    }
    
    
    /**
     * A helper method which tells if a schema is enabled
     */
    public static boolean isEnabled( DirectoryService service, String schemaName )
    {
    	return service.getRegistries().isSchemaLoaded( schemaName );
    }
}
