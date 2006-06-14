/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
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
import java.util.Iterator;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.daemon.AvailablePortFinder;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.codec.LdapConstants;
import org.apache.directory.shared.ldap.codec.LdapDecoder;
import org.apache.directory.shared.ldap.codec.LdapMessage;
import org.apache.directory.shared.ldap.codec.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.LdapResult;
import org.apache.directory.shared.ldap.codec.add.AddRequest;
import org.apache.directory.shared.ldap.codec.bind.BindRequest;
import org.apache.directory.shared.ldap.codec.bind.BindResponse;
import org.apache.directory.shared.ldap.codec.bind.LdapAuthentication;
import org.apache.directory.shared.ldap.codec.bind.SimpleAuthentication;
import org.apache.directory.shared.ldap.codec.del.DelRequest;
import org.apache.directory.shared.ldap.codec.extended.ExtendedResponse;
import org.apache.directory.shared.ldap.codec.modify.ModifyRequest;
import org.apache.directory.shared.ldap.codec.modifyDn.ModifyDNRequest;
import org.apache.directory.shared.ldap.codec.unbind.UnBindRequest;
import org.apache.directory.shared.ldap.codec.util.LdapResultEnum;

/**
 * A command to import data into a server. The data to be imported must be
 * stored in a Ldif File, and they could be added entries or modified entries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 406112 $
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
    private static final int IMPORT_SUCCESS= 0;

    /**
     * Socket used to connect to the server
     */
    private SocketChannel channel;

    private SocketAddress serverAddress;

    private IAsn1Container ldapMessageContainer = new LdapMessageContainer();

    private Asn1Decoder ldapDecoder = new LdapDecoder();

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

    private LdapMessage readResponse( ByteBuffer bb ) throws IOException, DecoderException, NamingException
    {

        LdapMessage messageResp = null;

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
                    messageResp = ( (LdapMessageContainer) ldapMessageContainer ).getLdapMessage();

                    if ( messageResp instanceof BindResponse )
                    {
                        BindResponse resp = ( (LdapMessageContainer) ldapMessageContainer ).getLdapMessage().getBindResponse();

                        if ( resp.getLdapResult().getResultCode() != 0 )
                        {
                            System.out.println( "Error : " + resp.getLdapResult().getErrorMessage() );
                        }
                    }
                    else if ( messageResp instanceof ExtendedResponse )
                    {
                        ExtendedResponse resp = ( (LdapMessageContainer) ldapMessageContainer ).getLdapMessage()
                                .getExtendedResponse();

                        if ( resp.getLdapResult().getResultCode() != 0 )
                        {
                            System.out.println( "Error : " + resp.getLdapResult().getErrorMessage() );
                        }
                    }

                    ( (LdapMessageContainer) ldapMessageContainer ).clean();
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
     * @param entry
     *            The entry to add
     * @param msgId
     *            message id number
     */
    private int addEntry( Entry entry, int messageId ) throws IOException, DecoderException, InvalidNameException,
            NamingException, EncoderException
    {
        AddRequest addRequest = new AddRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            System.out.println( "Adding entry " + dn );
        }

        Attributes attributes = entry.getAttributes();

        addRequest.setEntry( new LdapDN( dn ) );

        // Copy the attributes
        addRequest.initAttributes();

        for ( NamingEnumeration attrs = attributes.getAll(); attrs.hasMoreElements(); )
        {
            Attribute attribute = (Attribute) attrs.nextElement();

            addRequest.addAttributeType( attribute.getID() );

            for ( NamingEnumeration values = attribute.getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();
                addRequest.addAttributeValue( value );
            }
        }

        LdapMessage message = new LdapMessage();

        message.setProtocolOP( addRequest );
        message.setMessageId( messageId );
        
        // Encode and send the addRequest message
        ByteBuffer bb = message.encode( null );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessage response = readResponse( bb );

        LdapResult result = response.getAddResponse().getLdapResult();

        if ( result.getResultCode() == LdapResultEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Add of Entry " + entry.getDn() + " was successful" );
            }
            
            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( "Add of entry " + entry.getDn() + " failed for the following reasons provided by the server:\n"
                    + result.getErrorMessage() );
            
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
     */
    private int deleteEntry( Entry entry, int messageId ) throws IOException, DecoderException, InvalidNameException,
            NamingException, EncoderException
    {
        DelRequest delRequest = new DelRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            System.out.println( "Deleting entry " + dn );
        }
        
        delRequest.setEntry( new LdapDN( dn ) );
        
        LdapMessage message = new LdapMessage();

        message.setProtocolOP( delRequest );
        message.setMessageId( messageId );
        
        // Encode and send the delete request
        ByteBuffer bb = message.encode( null );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessage response = readResponse( bb );

        LdapResult result = response.getDelResponse().getLdapResult();

        if ( result.getResultCode() == LdapResultEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Delete of Entry " + entry.getDn() + " was successful" );
            }
            
            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( "Delete of entry " + entry.getDn() + " failed for the following reasons provided by the server:\n"
                    + result.getErrorMessage() );
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
     */
    private int changeModRDNEntry( Entry entry, int messageId ) throws IOException, DecoderException, InvalidNameException,
            NamingException, EncoderException
    {
        ModifyDNRequest modifyDNRequest = new ModifyDNRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            System.out.println( "Modify DN of entry " + dn );
        }
        
        modifyDNRequest.setEntry( new LdapDN( dn ) );
        modifyDNRequest.setDeleteOldRDN( entry.isDeleteOldRdn() );
        modifyDNRequest.setNewRDN( new Rdn( entry.getNewRdn() ) );
        
        if ( StringTools.isEmpty( entry.getNewSuperior() ) == false )
        {
            modifyDNRequest.setNewSuperior( new LdapDN( entry.getNewSuperior() ) );
        }
        
        LdapMessage message = new LdapMessage();

        message.setProtocolOP( modifyDNRequest );
        message.setMessageId( messageId );
        
        // Encode and send the delete request
        ByteBuffer bb = message.encode( null );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessage response = readResponse( bb );

        LdapResult result = response.getModifyDNResponse().getLdapResult();

        if ( result.getResultCode() == LdapResultEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "ModifyDn of Entry " + entry.getDn() + " was successful" );
            }
            
            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( "ModifyDn of entry " + entry.getDn() + " failed for the following reasons provided by the server:\n"
                    + result.getErrorMessage() );
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
     */
    private int changeModifyEntry( Entry entry, int messageId ) throws IOException, DecoderException, InvalidNameException,
            NamingException, EncoderException
    {
        ModifyRequest modifyRequest = new ModifyRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            System.out.println( "Modify of entry " + dn );
        }
        
        modifyRequest.setObject( new LdapDN( dn ) );
        modifyRequest.initModifications();
        
        Iterator modifications = entry.getModificationItems().iterator();
        
        while ( modifications.hasNext() )
        {
            ModificationItem modification = (ModificationItem)modifications.next();
            
            switch ( modification.getModificationOp() )
            {
                case DirContext.ADD_ATTRIBUTE :
                    modifyRequest.setCurrentOperation(  LdapConstants.OPERATION_ADD );
                    break;
                    
                case DirContext.REMOVE_ATTRIBUTE :
                    modifyRequest.setCurrentOperation(  LdapConstants.OPERATION_DELETE );
                    break;

                case DirContext.REPLACE_ATTRIBUTE :
                    modifyRequest.setCurrentOperation(  LdapConstants.OPERATION_REPLACE );
                    break;
                    
                default :
                    System.err.println( "Unknown modify operation for DN " + dn );
            }
            
            modifyRequest.addAttributeTypeAndValues( modification.getAttribute().getID() );
            
            for ( NamingEnumeration values = modification.getAttribute().getAll(); values.hasMoreElements(); )
            {
                Object value = values.nextElement();
                modifyRequest.addAttributeValue( value );
            }
        }

        LdapMessage message = new LdapMessage();

        message.setProtocolOP( modifyRequest );
        message.setMessageId( messageId );
        
        // Encode and send the delete request
        ByteBuffer bb = message.encode( null );
        bb.flip();

        sendMessage( bb );

        bb.clear();

        // Get the response
        LdapMessage response = readResponse( bb );

        LdapResult result = response.getModifyResponse().getLdapResult();

        if ( result.getResultCode() == LdapResultEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Modify of Entry " + entry.getDn() + " was successful" );
            }
            
            return IMPORT_SUCCESS;
        }
        else
        {
            System.err.println( "Modify of entry " + entry.getDn() + " failed for the following reasons provided by the server:\n"
                    + result.getErrorMessage() );
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
     */
    private int changeEntry( Entry entry, int messageId ) throws IOException, DecoderException, InvalidNameException,
            NamingException, EncoderException
    {
        switch ( entry.getChangeType() )
        {
            case Entry.ADD :
                // No difference with the injection of new entries
                return addEntry( entry, messageId );
                
            case Entry.DELETE :
                return deleteEntry( entry, messageId );
                
            case Entry.MODIFY :
                return changeModifyEntry( entry, messageId );
                
            case Entry.MODDN :
            case Entry.MODRDN :
                return changeModRDNEntry( entry, messageId );
                
            default :
                return IMPORT_ERROR;
        }
    }

    /**
     * Bind to the ldap server
     * 
     * @param messageId The message Id
     */
    private void bind( int messageId ) throws NamingException, EncoderException, DecoderException, IOException
    {
        BindRequest bindRequest = new BindRequest();
        LdapMessage message = new LdapMessage();
        LdapAuthentication authentication = null;

        if ( "simple".equals( auth ) )
        {
            authentication = new SimpleAuthentication();
            ( (SimpleAuthentication) authentication ).setSimple( StringTools.getBytesUtf8( password ) );
        }

        bindRequest.setAuthentication( authentication );
        bindRequest.setName( new LdapDN( user ) );
        bindRequest.setVersion( 3 );

        message.setProtocolOP( bindRequest );
        message.setMessageId( messageId );
        
        // Encode and send the bind request
        ByteBuffer bb = message.encode( null );
        bb.flip();

        connect();
        sendMessage( bb );

        bb.clear();

        // Get the bind response
        LdapMessage response = readResponse( bb );

        LdapResult result = response.getBindResponse().getLdapResult();

        if ( result.getResultCode() == LdapResultEnum.SUCCESS )
        {
            if ( isDebugEnabled() )
            {
                System.out.println( "Binding of user " + user + " was successful" );
            }
        }
        else
        {
            System.err.println( "Binding of user " + user + " failed for the following reasons provided by the server:\n"
                    + result.getErrorMessage() );
            System.exit( 1 );
        }
    }

    /**
     * Unbind from the server
     * 
     * @param messageId
     *            The message Id
     * @throws InvalidNameException
     * @throws EncoderException
     * @throws DecoderException
     * @throws IOException
     */
    private void unbind( int messageId ) throws InvalidNameException, EncoderException, DecoderException, IOException
    {
        UnBindRequest unbindRequest = new UnBindRequest();
        LdapMessage message = new LdapMessage();

        message.setProtocolOP( unbindRequest );
        message.setMessageId( messageId );
        ByteBuffer bb = message.encode( null );
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
            Iterator entries = ldifReader.iterator();
            long t0 = System.currentTimeMillis();
            int nbAdd = 0;

            while ( entries.hasNext() )
            {
                Entry entry = (Entry) entries.next();

                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    System.err.println( "Found an error while persing an entry : " + ldifReader.getError().getMessage() );
                    
                    if ( ignoreErrors == false )
                    {
                        unbind(  messageId );
                        
                        System.err.println( "Import failed..." );
                        System.exit( 1 );
                    }
                }
                
                if ( ( addEntry( entry, messageId++ ) == IMPORT_ERROR ) && 
                        ( ignoreErrors == false ) )
                {
                    unbind(  messageId );
                    
                    System.err.println( "Import failed..." );
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
            System.out.println( nbAdd + " users added in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );
        }
        else
        {
            // Parse the file and inject every modification
            Iterator entries = ldifReader.iterator();
            long t0 = System.currentTimeMillis();
            int nbMod = 0;

            while ( entries.hasNext() )
            {
                Entry entry = (Entry) entries.next();
                
                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    System.err.println( "Found an error while persing an entry : " + ldifReader.getError().getMessage() );
                    
                    if ( ignoreErrors == false )
                    {
                        unbind(  messageId );
                        
                        System.err.println( "Import failed..." );
                        System.exit( 1 );
                    }
                }

                if ( ( changeEntry( entry, messageId++ ) == IMPORT_ERROR ) && 
                        ( ignoreErrors == false ) )
                {
                    unbind(  messageId );
                    
                    System.err.println( "Import failed..." );
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
            System.out.println( nbMod + " users changed in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );
        }

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
            catch (NumberFormatException e)
            {
                System.err.println( "port value of '" + val + "' is not a number" );
                System.exit( 1 );
            }

            if ( port > AvailablePortFinder.MAX_PORT_NUMBER )
            {
                System.err.println( "port value of '" + val + "' is larger than max port number: "
                        + AvailablePortFinder.MAX_PORT_NUMBER );
                System.exit( 1 );
            }
            else if ( port < AvailablePortFinder.MIN_PORT_NUMBER )
            {
                System.err.println( "port value of '" + val + "' is smaller than the minimum port number: "
                        + AvailablePortFinder.MIN_PORT_NUMBER );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                System.out.println( "port overriden by -p option: " + port );
            }
        }
        else if ( getConfiguration() != null )
        {
            port = getConfiguration().getLdapPort();

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
                System.err.println( "ldif file '" + ldifFileName + "' does not exist" );
                System.exit( 1 );
            }

            if ( ldifFile.canRead() == false )
            {
                System.err.println( "ldif file '" + ldifFileName + "' can't be read" );
                System.exit( 1 );
            }

            if ( isDebugEnabled() )
            {
                try
                {
                    System.out.println( "ldif file to import: " + ldifFile.getCanonicalPath() );
                }
                catch (IOException ioe)
                {
                    System.out.println( "ldif file to import: " + ldifFileName );
                }
            }
        }
        else
        {
            System.err.println( "ldif file name must be provided" );
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
