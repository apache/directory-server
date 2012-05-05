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
package org.apache.directory.server.tools;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.Asn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.message.LdapEncoder;
import org.apache.directory.shared.ldap.message.UnbindRequest;
import org.apache.directory.shared.ldap.message.UnbindRequestImpl;


/**
 * A command to import data into a server. The data to be imported must be
 * stored in a Ldif File, and they could be added entries or modified entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ImportCommand extends ToolCommand
{
    public static final String PORT_RANGE = "(" + AvailablePortFinder.MIN_PORT_NUMBER + ", "
        + AvailablePortFinder.MAX_PORT_NUMBER + ")";

    private int port = 10389;

    private String host = "localhost";

    private String password = "secret";

    private String user = "uid=admin,ou=system";

    private String auth = "simple";

    private File ldifFile;

    private String logs;

    private boolean ignoreErrors = false;

    private static final int IMPORT_ERROR = -1;
    private static final int IMPORT_SUCCESS = 0;

    /**
     * Socket used to connect to the server
     */
    private SocketChannel channel;

    private SocketAddress serverAddress;

    private Asn1Container ldapMessageContainer = new LdapMessageContainer();

    private Asn1Decoder ldapDecoder = new Asn1Decoder();


    /**
     * The constructor save the command's name into it's super class
     * 
     */
    protected ImportCommand()
    {
        super( "import" );
    }


    /**
     * Connect to the LDAP server through a socket and establish the Input and
     * Output Streams. All the required information for the connection should be
     * in the options from the command line, or the default values.
     * 
     * @throws UnknownHostException
     *             The hostname or the Address of server could not be found
     * @throws IOException
     *             There was a error opening or establishing the socket
     */
    private void connect() throws UnknownHostException, IOException
    {
        serverAddress = new InetSocketAddress( host, port );
        channel = SocketChannel.open( serverAddress );
        channel.configureBlocking( true );
    }


    private void sendMessage( ByteBuffer bb ) throws IOException
    {
        channel.write( bb );
        bb.clear();
    }


    /*
    private LdapMessageCodec readResponse( ByteBuffer bb ) throws IOException, DecoderException
    {
        LdapMessageCodec messageResp = null;

        while ( true )
        {
            int nbRead = channel.read( bb );

            if ( nbRead == -1 )
            {
                break;
            }
            else
            {
                bb.flip();

                // Decode the PDU
                ldapDecoder.decode( bb, ldapMessageContainer );

                if ( ldapMessageContainer.getState() == TLVStateEnum.PDU_DECODED )
                {
                    Message message = ( ( LdapMessageContainer ) ldapMessageContainer ).getInternalMessage();

                    if ( message instanceof BindResponse )
                    {
                        BindResponse resp = ( BindResponse ) message;

                        if ( resp.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
                        {
                            System.out.println( "Error : " + resp.getLdapResult().getErrorMessage() );
                        }
                    }
                    else if ( message instanceof ExtendedResponse )
                    {
                        ExtendedResponse response = ( ( LdapMessageContainer ) ldapMessageContainer )
                            .getExtendedResponse();

                        if ( response.getLdapResult().getResultCode() != ResultCodeEnum.SUCCESS )
                        {
                            System.out.println( "Error : " + response.getLdapResult().getErrorMessage() );
                        }
                    }

                    ( ( LdapMessageContainer ) ldapMessageContainer ).clean();

                    break;
                }
                else
                {
                    bb.flip();
                }
            }
        }

        return messageResp;
    }


    /**
     * Send the entry to the encoder, then wait for a
     * reponse from the LDAP server on the results of the operation.
     * 
     * @param ldifEntry The entry to add
     * @param msgId message id number
     *
    private int addEntry( LdifEntry ldifEntry, int messageId ) throws IOException, DecoderException, LdapException,
        EncoderException
    {
        AddRequest addRequest = new AddRequestImpl();

        String dn = ldifEntry.getDn().getName();

        if ( isDebugEnabled() )
        {
            System.out.println( "Adding entry " + dn );
        }

        Entry entry = ldifEntry.getEntry();

        addRequest.setEntryDn( new DN( dn ) );

        // Copy the attributes
        for ( EntryAttribute attribute : entry )
        {
            addRequest.addAttributeType( attribute.getId() );

            for ( Value<?> value : attribute )
            {
                addRequest.addAttributeValue( value );
            }
        }

        addRequest.setMessageId( messageId );

        // Encode and send the addRequest message
        LdapEncoder encoder = new LdapEncoder();
        ByteBuffer bb = encoder.encodeMessage( addRequest );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessageCodec response = readResponse( bb );

        LdapResultCodec result = ( ( LdapResponseCodec ) response ).getLdapResult();

        if ( result.getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Add of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_203, entry.getDn(), result.getErrorMessage() ) );

            return IMPORT_ERROR;
        }
    }


    /**
     * Send the entry to the encoder, then wait for a
     * reponse from the LDAP server on the results of the operation.
     * 
     * @param entry
     *            The entry to delete
     * @param msgId
     *            message id number
     *
    private int deleteEntry( LdifEntry entry, int messageId ) throws IOException, DecoderException,
        LdapInvalidDnException, EncoderException
    {
        LdapEncoder encoder = new LdapEncoder();
        DeleteRequest delRequest = new DeleteRequestImpl( messageId );

        String dn = entry.getDn().getName();

        if ( isDebugEnabled() )
        {
            System.out.println( "Deleting entry " + dn );
        }

        delRequest.setName( new DN( dn ) );

        // Encode and send the delete request
        ByteBuffer bb = encoder.encodeMessage( delRequest );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessageCodec response = readResponse( bb );

        LdapResultCodec result = ( ( LdapResponseCodec ) response ).getLdapResult();

        if ( result.getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Delete of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_204, entry.getDn(), result.getErrorMessage() ) );
            return IMPORT_ERROR;
        }
    }


    /**
     * Send the entry to the encoder, then wait for a
     * reponse from the LDAP server on the results of the operation.
     * 
     * @param entry
     *            The entry to modify
     * @param msgId
     *            message id number
     *
    private int changeModRDNEntry( LdifEntry entry, int messageId ) throws IOException, DecoderException,
        LdapInvalidDnException, EncoderException
    {
        ModifyDnRequest modifyDNRequest = new ModifyDnRequestImpl();

        String dn = entry.getDn().getName();

        if ( isDebugEnabled() )
        {
            System.out.println( "Modify DN of entry " + dn );
        }

        modifyDNRequest.setName( new DN( dn ) );
        modifyDNRequest.setDeleteOldRdn( entry.isDeleteOldRdn() );
        modifyDNRequest.setNewRdn( new RDN( entry.getNewRdn() ) );

        if ( StringTools.isEmpty( entry.getNewSuperior() ) == false )
        {
            modifyDNRequest.setNewSuperior( new DN( entry.getNewSuperior() ) );
        }

        modifyDNRequest.setMessageId( messageId );

        // Encode and send the modifyDn request
        LdapEncoder encoder = new LdapEncoder();

        ByteBuffer bb = encoder.encodeMessage( modifyDNRequest );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessageCodec response = readResponse( bb );

        LdapResultCodec result = ( ( LdapResponseCodec ) response ).getLdapResult();

        if ( result.getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "ModifyDn of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_205, entry.getDn(), result.getErrorMessage() ) );
            return IMPORT_ERROR;
        }
    }


    /**
     * Send the entry to the encoder, then wait for a
     * reponse from the LDAP server on the results of the operation.
     * 
     * @param entry The entry to modify
     * @param msgId message id number
     *
    private int changeModifyEntry( LdifEntry entry, int messageId ) throws IOException, DecoderException,
        LdapInvalidDnException, EncoderException
    {
        ModifyRequest modifyRequest = new ModifyRequestImpl();

        String dn = entry.getDn().getName();

        if ( isDebugEnabled() )
        {
            System.out.println( "Modify of entry " + dn );
        }

        modifyRequest.setName( new DN( dn ) );

        for ( Modification modification : entry.getModificationItems() )
        {
            modifyRequest.addModification( modification );
        }

        modifyRequest.setMessageId( messageId );

        // Encode and send the delete request
        LdapEncoder encoder = new LdapEncoder();

        ByteBuffer bb = encoder.encodeMessage( modifyRequest );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessageCodec response = readResponse( bb );

        LdapResultCodec result = ( ( LdapResponseCodec ) response ).getLdapResult();

        if ( result.getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Modify of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_206, entry.getDn(), result.getErrorMessage() ) );
            return IMPORT_ERROR;
        }
    }


    /**
     * Send the change operation to the encoder, then wait for a
     * reponse from the LDAP server on the results of the operation.
     * 
     * @param entry
     *            The entry to add
     * @param msgId
     *            message id number
     *
    private int changeEntry( LdifEntry entry, int messageId ) throws IOException, DecoderException, LdapException,
        EncoderException
    {
        switch ( entry.getChangeType().getChangeType() )
        {
            case ChangeType.ADD_ORDINAL:
                // No difference with the injection of new entries
                return addEntry( entry, messageId );

            case ChangeType.DELETE_ORDINAL:
                return deleteEntry( entry, messageId );

            case ChangeType.MODIFY_ORDINAL:
                return changeModifyEntry( entry, messageId );

            case ChangeType.MODDN_ORDINAL:
            case ChangeType.MODRDN_ORDINAL:
                return changeModRDNEntry( entry, messageId );

            default:
                return IMPORT_ERROR;
        }
    }


    /**
     * Bind to the ldap server
     * 
     * @param messageId The message Id
     *
    private void bind( int messageId ) throws LdapInvalidDnException, EncoderException, DecoderException, IOException
    {
        BindRequest bindRequest = new BindRequestImpl( messageId );

        if ( "simple".equals( auth ) )
        {
            bindRequest.setCredentials( StringTools.getBytesUtf8( password ) );
            bindRequest.setSimple( true );
        }

        bindRequest.setName( new DN( user ) );

        // Encode and send the bind request
        LdapEncoder encoder = new LdapEncoder();

        ByteBuffer bb = encoder.encodeMessage( bindRequest );
        bb.flip();

        connect();
        sendMessage( bb );

        bb.clear();

        // Get the bind response
        LdapMessageCodec response = readResponse( bb );

        LdapResultCodec result = ( ( LdapResponseCodec ) response ).getLdapResult();

        if ( result.getResultCode() == ResultCodeEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Binding of user " + user + " was successful" );
            }
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_207, user, result.getErrorMessage() ) );
            System.exit( 1 );
        }
    }


    /**
     * Unbind from the server
     * 
     * @param messageId
     *            The message Id
     * @throws EncoderException
     * @throws DecoderException
     * @throws IOException
     */
    private void unbind( int messageId ) throws EncoderException, DecoderException, IOException
    {
        UnbindRequest unbindRequest = new UnbindRequestImpl( messageId );
        LdapEncoder encoder = new LdapEncoder();

        ByteBuffer bb = encoder.encodeMessage( unbindRequest );
        bb.flip();

        sendMessage( bb );

        if ( isDebugEnabled() )
        {
            System.out.println( "Unbinding of user " + user + " was successful" );
        }
    }


    /**
     * Execute the command
     * 
     * @param cmd
     *            The command to be executed
     */
    public void execute( CommandLine cmd ) throws Exception
    {
        return;
    }


    /*
        processOptions( cmd );

        if ( isDebugEnabled() )
        {
            System.out.println( "Parameters for Ldif import request:" );
            System.out.println( "port = " + port );
            System.out.println( "host = " + host );
            System.out.println( "user = " + user );
            System.out.println( "auth type = " + auth );
            System.out.println( "file = " + ldifFile );
            System.out.println( "logs = " + logs );
        }

        int messageId = 0;

        // Login to the server
        bind( messageId++ );

        if ( isDebugEnabled() )
        {
            System.out.println( "Connection to the server established.\n" + "Importing data ... " );
        }

        LdifReader ldifReader = new LdifReader( ldifFile );

        if ( ldifReader.containsEntries() )
        {
            // Parse the file and inject every entry
            long t0 = System.currentTimeMillis();
            int nbAdd = 0;

            for ( LdifEntry entry : ldifReader )
            {
                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    System.err.println( "Found an error while persing an entry : "
                        + ldifReader.getError().getLocalizedMessage() );

                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        System.err.println( I18n.err( I18n.ERR_208 ) );
                        System.exit( 1 );
                    }
                }

                if ( ( addEntry( entry, messageId++ ) == IMPORT_ERROR ) && ( ignoreErrors == false ) )
                {
                    unbind( messageId );

                    System.err.println( I18n.err( I18n.ERR_208 ) );
                    System.exit( 1 );
                }

                nbAdd++;

                if ( nbAdd % 10 == 0 )
                {
                    System.out.print( '.' );
                }

                if ( nbAdd % 500 == 0 )
                {
                    System.out.println( nbAdd );
                }
            }

            long t1 = System.currentTimeMillis();

            System.out.println( "Done!" );
            System.out.println( nbAdd + " entries added in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );
        }
        else
        {
            // Parse the file and inject every modification
            long t0 = System.currentTimeMillis();
            int nbMod = 0;

            for ( LdifEntry entry : ldifReader )
            {
                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    System.err.println( "Found an error while persing an entry : "
                        + ldifReader.getError().getLocalizedMessage() );

                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        System.err.println( I18n.err( I18n.ERR_208 ) );
                        System.exit( 1 );
                    }
                }

                if ( ( changeEntry( entry, messageId++ ) == IMPORT_ERROR ) && ( ignoreErrors == false ) )
                {
                    unbind( messageId );

                    System.err.println( I18n.err( I18n.ERR_208 ) );
                    System.exit( 1 );
                }

                nbMod++;

                if ( nbMod % 10 == 0 )
                {
                    System.out.print( '.' );
                }

                if ( nbMod % 500 == 0 )
                {
                    System.out.println( nbMod );
                }
            }

            long t1 = System.currentTimeMillis();

            System.out.println( "Done!" );
            System.out.println( nbMod + " entries changed in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );
        }

        ldifReader.close();

        // Logout to the server
        unbind( messageId++ );

    }


    /**
     * Read the command line and get the options : 'h' : host 'p' : port 'u' :
     * user 'w' : password 'a' : authentication type 'i' : ignore errors 'f' :
     * ldif file to import
     * 
     * @param cmd
     *            The command line
     */
    private void processOptions( CommandLine cmd )
    {
        if ( isDebugEnabled() )
        {
            System.out.println( "Processing options for launching diagnostic UI ..." );
        }

        // -------------------------------------------------------------------
        // figure out the host value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'h' ) )
        {
            host = cmd.getOptionValue( 'h' );

            if ( isDebugEnabled() )
            {
                System.out.println( "ignore-errors overriden by -i option: true" );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "ignore-errors set to default: false" );
        }

        // -------------------------------------------------------------------
        // figure out and error check the port value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'p' ) ) // - user provided port w/ -p takes
        // precedence
        {
            String val = cmd.getOptionValue( 'p' );

            try
            {
                port = Integer.parseInt( val );
            }
            catch ( NumberFormatException e )
            {
                System.err.println( I18n.err( I18n.ERR_193, val ) );
                System.exit( 1 );
            }

            if ( port > AvailablePortFinder.MAX_PORT_NUMBER )
            {
                System.err.println( I18n.err( I18n.ERR_194, val, AvailablePortFinder.MAX_PORT_NUMBER ) );
                System.exit( 1 );
            }
            else if ( port < AvailablePortFinder.MIN_PORT_NUMBER )
            {
                System.err.println( I18n.err( I18n.ERR_195, val, AvailablePortFinder.MIN_PORT_NUMBER ) );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                System.out.println( "port overriden by -p option: " + port );
            }
        }
        else if ( getApacheDS() != null )
        {
            port = getApacheDS().getLdapServer().getPort();

            if ( isDebugEnabled() )
            {
                System.out.println( "port overriden by server.xml configuration: " + port );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "port set to default: " + port );
        }

        // -------------------------------------------------------------------
        // figure out the user value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'u' ) )
        {
            user = cmd.getOptionValue( 'u' );

            if ( isDebugEnabled() )
            {
                System.out.println( "user overriden by -u option: " + user );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "user set to default: " + user );
        }

        // -------------------------------------------------------------------
        // figure out the password value
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'w' ) )
        {
            password = cmd.getOptionValue( 'w' );

            if ( isDebugEnabled() )
            {
                System.out.println( "password overriden by -w option: " + password );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "password set to default: " + password );
        }

        // -------------------------------------------------------------------
        // figure out the authentication type
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'a' ) )
        {
            auth = cmd.getOptionValue( 'a' );

            if ( isDebugEnabled() )
            {
                System.out.println( "authentication type overriden by -a option: " + auth );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "authentication type set to default: " + auth );
        }

        // -------------------------------------------------------------------
        // figure out the 'ignore-errors' flag
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'e' ) )
        {
            ignoreErrors = true;

            if ( isDebugEnabled() )
            {
                System.out.println( "authentication type overriden by -a option: " + auth );
            }
        }
        else if ( isDebugEnabled() )
        {
            System.out.println( "authentication type set to default: " + auth );
        }

        // -------------------------------------------------------------------
        // figure out the ldif file to import
        // -------------------------------------------------------------------

        if ( cmd.hasOption( 'f' ) )
        {
            String ldifFileName = cmd.getOptionValue( 'f' );

            ldifFile = new File( ldifFileName );

            if ( ldifFile.exists() == false )
            {
                System.err.println( I18n.err( I18n.ERR_209, ldifFileName ) );
                System.exit( 1 );
            }

            if ( ldifFile.canRead() == false )
            {
                System.err.println( I18n.err( I18n.ERR_210, ldifFileName ) );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                try
                {
                    System.out.println( "ldif file to import: " + ldifFile.getCanonicalPath() );
                }
                catch ( IOException ioe )
                {
                    System.out.println( "ldif file to import: " + ldifFileName );
                }
            }
        }
        else
        {
            System.err.println( I18n.err( I18n.ERR_211 ) );
            System.exit( 1 );
        }
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = new Option( "h", "host", true, "server host: defaults to localhost" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "p", "port", true, "server port: defaults to 10389 or server.xml specified port" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "u", "user", true, "the user: default to uid=admin, ou=system" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "w", "password", true, "the apacheds administrator's password: defaults to secret" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "a", "auth", true, "the authentication mode: defaults to 'simple'" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "f", "file", true, "the ldif file to import" );
        op.setRequired( true );
        opts.addOption( op );
        op = new Option( "e", "ignore", false, "continue to process the file even if errors are encountered " );
        op.setRequired( false );
        opts.addOption( op );

        return opts;
    }
}
