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
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.naming.NamingException;
import javax.naming.ldap.LdapContext;
import javax.naming.ldap.LdapName;

import org.apache.commons.io.FileUtils;
import org.apache.directory.api.ldap.model.constants.AuthenticationLevel;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.ldif.ChangeType;
import org.apache.directory.api.ldap.model.ldif.LdifEntry;
import org.apache.directory.api.ldap.model.ldif.LdifReader;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.api.ldap.model.schema.registries.Schema;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapCoreSessionConnection;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.jndi.ServerLdapContext;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.server.ldap.LdapServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Integration test utility methods.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IntegrationUtils
{
    /** The class logger */
    private static final Logger LOG = LoggerFactory.getLogger( IntegrationUtils.class );

    private static final List<LdapConnection> OPEN_CONNECTIONS = new ArrayList<LdapConnection>();


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
                LOG.error( I18n.err( I18n.ERR_115 ), e );
            }
        }
        if ( wkdir.exists() )
        {
            throw new IOException( I18n.err( I18n.ERR_116, wkdir ) );
        }
    }


    /**
     * Inject an ldif String into the server. Dn must be relative to the
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
            if ( entry.isEntry() )
            {
                service.getAdminSession().add(
                    new DefaultEntry( service.getSchemaManager(), entry.getEntry() ) );
            }
            else if ( entry.isChangeModify() )
            {
                service.getAdminSession().modify(
                    entry.getDn(), entry.getModifications() );
            }
            else
            {
                String message = I18n.err( I18n.ERR_117, entry.getChangeType() );
                LOG.error( message );
                reader.close();
                throw new NamingException( message );
            }
        }

        // And close the reader
        reader.close();
    }


    public static LdifEntry getUserAddLdif() throws LdapException
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

        Dn userDn = new Dn( service.getSchemaManager(), principalDn );
        LdapPrincipal principal = new LdapPrincipal( service.getSchemaManager(), userDn, AuthenticationLevel.SIMPLE );

        if ( dn == null )
        {
            dn = "";
        }

        CoreSession session = service.getSession( principal );
        LdapContext ctx = new ServerLdapContext( service, session, new LdapName( dn ) );
        return ctx;
    }


    public static CoreSession getCoreSession( String principalDn, DirectoryService service, String dn )
        throws Exception
    {
        if ( principalDn == null )
        {
            principalDn = "";
        }

        Dn userDn = new Dn( service.getSchemaManager(), principalDn );
        LdapPrincipal principal = new LdapPrincipal( service.getSchemaManager(), userDn, AuthenticationLevel.SIMPLE );

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
        return getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, SchemaConstants.OU_SCHEMA );
    }


    public static LdapContext getRootContext( DirectoryService service ) throws Exception
    {
        return getContext( ServerDNConstants.ADMIN_SYSTEM_DN, service, "" );
    }


    public static void apply( DirectoryService service, LdifEntry entry ) throws Exception
    {
        Dn dn = entry.getDn();
        CoreSession session = service.getAdminSession();

        switch ( entry.getChangeType().getChangeType() )
        {
            case ( ChangeType.ADD_ORDINAL ):
                session.add(
                    new DefaultEntry( service.getSchemaManager(), entry.getEntry() ) );
                break;

            case ( ChangeType.DELETE_ORDINAL ):
                session.delete( dn );
                break;

            case ( ChangeType.MODDN_ORDINAL ):
            case ( ChangeType.MODRDN_ORDINAL ):
                Rdn newRdn = new Rdn( entry.getNewRdn() );

                if ( entry.getNewSuperior() != null )
                {
                    // It's a move. The superior have changed
                    // Let's see if it's a rename too
                    Rdn oldRdn = dn.getRdn();
                    Dn newSuperior = new Dn( entry.getNewSuperior() );

                    if ( dn.size() == 0 )
                    {
                        throw new IllegalStateException( I18n.err( I18n.ERR_475 ) );
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

            case ( ChangeType.MODIFY_ORDINAL ):
                session.modify( dn, entry.getModifications() );
                break;

            default:
                throw new IllegalStateException( I18n.err( I18n.ERR_476, entry.getChangeType() ) );
        }
    }


    public static LdifEntry getUserAddLdif( String dnstr, byte[] password, String cn, String sn )
        throws LdapException
    {
        Dn dn = new Dn( dnstr );
        LdifEntry ldif = new LdifEntry();
        ldif.setDn( dnstr );
        ldif.setChangeType( ChangeType.Add );

        Attribute attr = new DefaultAttribute( "objectClass",
            "top", "person", "organizationalPerson", "inetOrgPerson" );
        ldif.addAttribute( attr );

        attr = new DefaultAttribute( "ou", "Engineering", "People" );
        ldif.addAttribute( attr );

        String uid = dn.getRdn().getNormValue();
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
        LdapConnection connection = getAdminConnection( service );

        // now enable the test schema
        connection.modify( "cn=" + schemaName + ",ou=schema",
            new DefaultModification(
                ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "FALSE" ) );
    }


    public static void disableSchema( DirectoryService service, String schemaName ) throws Exception
    {
        LdapConnection connection = getAdminConnection( service );

        // now enable the test schema
        Modification mod = new DefaultModification(
            ModificationOperation.REPLACE_ATTRIBUTE, "m-disabled", "TRUE" );

        connection.modify( "cn=" + schemaName + ",ou=schema", mod );
    }


    /**
     * A helper method which tells if a schema is disabled.
     */
    public static boolean isDisabled( DirectoryService service, String schemaName )
    {
        Schema schema = service.getSchemaManager().getLoadedSchema( schemaName );

        return ( schema == null ) || schema.isDisabled();
    }


    /**
     * A helper method which tells if a schema is loaded.
     */
    public static boolean isLoaded( DirectoryService service, String schemaName )
    {
        Schema schema = service.getSchemaManager().getLoadedSchema( schemaName );

        return ( schema != null );
    }


    /**
     * A helper method which tells if a schema is enabled. A shema must be
     * loaded and enabled.
     */
    public static boolean isEnabled( DirectoryService service, String schemaName )
    {
        Schema schema = service.getSchemaManager().getLoadedSchema( schemaName );

        return ( schema != null ) && schema.isEnabled();
    }


    /**
     * Gets a LdapCoreSessionConnection bound using the default admin Dn uid=admin,ou=system and password "secret"
     * 
     * @param dirService The Directory Service to be connected to
     * @return A LdapCoreSessionConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getAdminConnection( DirectoryService dirService ) throws Exception
    {
        return getConnectionAs( dirService, ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );
    }


    /**
     * Gets a LdapCoreSessionConnection bound using a user's DN and a password. We will bind using those
     * credentials.
     * 
     * @param dirService The Directory Service to be connected to
     * @param dn The User's DN as a String
     * @param password The User's password as a String
     * @return A LdapCoreSessionConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getConnectionAs( DirectoryService dirService, String dn, String password )
        throws Exception
    {
        return getConnectionAs( dirService, new Dn( dn ), password );
    }


    /**
     * Gets a LdapCoreSessionConnection bound using a user's DN and a password. We will bind using those
     * credentials.
     * 
     * @param dirService The Directory Service to be connected to
     * @param dn The User's DN
     * @param password The User's password as a String
     * @return A LdapCoreSessionConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getConnectionAs( DirectoryService dirService, Dn dn, String password )
        throws Exception
    {
        LdapCoreSessionConnection connection = new LdapCoreSessionConnection();

        connection.setDirectoryService( dirService );

        connection.bind( dn, password );

        return connection;
    }


    /**
     * Gets a LdapNetworkConnection bound using a user's DN and a password. We will bind using those
     * credentials.
     * 
     * @param dirService The Directory Service to be connected to
     * @param dn The User's DN as a String
     * @param password The User's password as a String
     * @return A LdapNetworkConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getNetworkConnectionAs( String host, int port, String dn, String password )
        throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( host, port );

        connection.bind( dn, password );
        OPEN_CONNECTIONS.add( connection );
        return connection;
    }


    /**
     * Gets an anonymous LdapNetworkConnection
     * 
     * @param dirService The Directory Service to be connected to
     * @return A LdapNetworkConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getAnonymousNetworkConnection( LdapServer ldapServer ) throws Exception
    {
        LdapConnection connection = getAnonymousNetworkConnection( InetAddress.getLocalHost().getHostName(), ldapServer.getPort() );

        return connection;
    }


    /**
     * Gets an anonymous LdapNetworkConnection
     * 
     * @param dirService The Directory Service to be connected to
     * @return A LdapNetworkConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getAnonymousNetworkConnection( String host, int port ) throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( host, port );
        connection.bind();

        OPEN_CONNECTIONS.add( connection );

        return connection;
    }


    /**
     * Gets a LdapNetworkConnection bound to the Admin user (uid=admin,ou=system).
     * 
     * @param dirService The Directory Service to be connected to
     * @return A LdapNetworkConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getAdminNetworkConnection( LdapServer ldapServer ) throws Exception
    {
        LdapConnection connection = new LdapNetworkConnection( InetAddress.getLocalHost().getHostName(), ldapServer.getPort() );

        connection.setTimeOut( 0 );
        connection.bind( ServerDNConstants.ADMIN_SYSTEM_DN, "secret" );

        OPEN_CONNECTIONS.add( connection );

        return connection;
    }


    /**
     * Gets a LdapNetworkConnection bound using a user's DN and a password. We will bind using those
     * credentials. We specify a LdapServer instance too.
     * 
     * @param ldapServer The LdapServer to be connected to
     * @param dn The User's DN as a String
     * @param password The User's password as a String
     * @return A LdapNetworkConnection instance
     * @exception If the connection could not be established.
     */
    public static LdapConnection getNetworkConnectionAs( LdapServer ldapServer, String userDn, String password )
        throws Exception
    {
        return getNetworkConnectionAs( InetAddress.getLocalHost().getHostName(), ldapServer.getPort(), userDn, password );
    }


    public static void closeConnections()
    {

        for ( LdapConnection con : OPEN_CONNECTIONS )
        {
            if ( con == null )
            {
                continue;
            }

            try
            {
                if ( con.isConnected() )
                {
                    con.close();
                }
            }
            catch ( Exception e )
            {
                // shouldn't happen, but print the stacktrace so that less pain during development to find the cause
                e.printStackTrace();
            }
        }

        OPEN_CONNECTIONS.clear();
    }
}
