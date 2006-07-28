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
package org.apache.directory.server.tools.commands.importcmd;


import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.SocketAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;

import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.directory.server.tools.util.ToolCommandException;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.tlv.TLVStateEnum;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.asn1.codec.EncoderException;
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
import org.apache.directory.shared.ldap.ldif.Entry;
import org.apache.directory.shared.ldap.ldif.LdifReader;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.StringTools;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * This is the Executor Class of the Import Command.
 * 
 * The command can be called using the 'execute' method.
 */
public class ImportCommandExecutor extends BaseToolCommandExecutor
{
    // Additional Parameters
    public static final String FILE_PARAMETER = "file";
    public static final String IGNOREERRORS_PARAMETER = "ignore-errors";

    // Additional ListenerParameters
    public static final String ENTRYADDEDLISTENER_PARAMETER = "entryAddedListener";
    public static final String ENTRYADDFAILEDLISTENER_PARAMETER = "entryAddFailedListener";

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

    private IAsn1Container ldapMessageContainer = new LdapMessageContainer();

    private Asn1Decoder ldapDecoder = new LdapDecoder();

    // The listeners
    private ToolCommandListener entryAddedListener;
    private ToolCommandListener entryAddFailedListener;


    public ImportCommandExecutor()
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
                    messageResp = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage();

                    if ( messageResp instanceof BindResponse )
                    {
                        BindResponse resp = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage()
                            .getBindResponse();

                        if ( resp.getLdapResult().getResultCode() != 0 )
                        {
                            notifyOutputListener( "Error : " + resp.getLdapResult().getErrorMessage() );
                        }
                    }
                    else if ( messageResp instanceof ExtendedResponse )
                    {
                        ExtendedResponse resp = ( ( LdapMessageContainer ) ldapMessageContainer ).getLdapMessage()
                            .getExtendedResponse();

                        if ( resp.getLdapResult().getResultCode() != 0 )
                        {
                            notifyOutputListener( "Error : " + resp.getLdapResult().getErrorMessage() );
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
            notifyOutputListener( "Adding entry " + dn );
        }

        Attributes attributes = entry.getAttributes();

        addRequest.setEntry( new LdapDN( dn ) );

        // Copy the attributes
        addRequest.initAttributes();

        for ( NamingEnumeration attrs = attributes.getAll(); attrs.hasMoreElements(); )
        {
            Attribute attribute = ( Attribute ) attrs.nextElement();

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
                notifyOutputListener( "Add of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            notifyErrorListener( "Add of entry " + entry.getDn()
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );

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
            notifyOutputListener( "Deleting entry " + dn );
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
                notifyOutputListener( "Delete of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            notifyErrorListener( "Delete of entry " + entry.getDn()
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );
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
    private int changeModRDNEntry( Entry entry, int messageId ) throws IOException, DecoderException,
        InvalidNameException, NamingException, EncoderException
    {
        ModifyDNRequest modifyDNRequest = new ModifyDNRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            notifyOutputListener( "Modify DN of entry " + dn );
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
                notifyOutputListener( "ModifyDn of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            notifyErrorListener( "ModifyDn of entry " + entry.getDn()
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );
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
    private int changeModifyEntry( Entry entry, int messageId ) throws IOException, DecoderException,
        InvalidNameException, NamingException, EncoderException
    {
        ModifyRequest modifyRequest = new ModifyRequest();

        String dn = entry.getDn();

        if ( isDebugEnabled() )
        {
            notifyOutputListener( "Modify of entry " + dn );
        }

        modifyRequest.setObject( new LdapDN( dn ) );
        modifyRequest.initModifications();

        Iterator modifications = entry.getModificationItems().iterator();

        while ( modifications.hasNext() )
        {
            ModificationItem modification = ( ModificationItem ) modifications.next();

            switch ( modification.getModificationOp() )
            {
                case DirContext.ADD_ATTRIBUTE:
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_ADD );
                    break;

                case DirContext.REMOVE_ATTRIBUTE:
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_DELETE );
                    break;

                case DirContext.REPLACE_ATTRIBUTE:
                    modifyRequest.setCurrentOperation( LdapConstants.OPERATION_REPLACE );
                    break;

                default:
                    notifyErrorListener( "Unknown modify operation for DN " + dn );
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
                notifyOutputListener( "Modify of Entry " + entry.getDn() + " was successful" );
            }

            return IMPORT_SUCCESS;
        }
        else
        {
            notifyErrorListener( "Modify of entry " + entry.getDn()
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );
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
            case Entry.ADD:
                // No difference with the injection of new entries
                return addEntry( entry, messageId );

            case Entry.DELETE:
                return deleteEntry( entry, messageId );

            case Entry.MODIFY:
                return changeModifyEntry( entry, messageId );

            case Entry.MODDN:
            case Entry.MODRDN:
                return changeModRDNEntry( entry, messageId );

            default:
                return IMPORT_ERROR;
        }
    }


    /**
     * Bind to the ldap server
     * 
     * @param messageId The message Id
     * @throws NamingException 
     */
    private void bind( int messageId ) throws EncoderException, DecoderException, IOException,
        ToolCommandException, NamingException
    {
        BindRequest bindRequest = new BindRequest();
        LdapMessage message = new LdapMessage();
        LdapAuthentication authentication = null;

        if ( "simple".equals( auth ) )
        {
            authentication = new SimpleAuthentication();
            ( ( SimpleAuthentication ) authentication ).setSimple( StringTools.getBytesUtf8( password ) );
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
                notifyOutputListener( "Binding of user " + user + " was successful" );
            }
        }
        else
        {
            notifyErrorListener( "Binding of user " + user
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );

            throw new ToolCommandException( "Binding of user " + user
                + " failed for the following reasons provided by the server:\n" + result.getErrorMessage() );
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
            notifyOutputListener( "Unbinding of user " + user + " was successful" );
        }
    }


    private void processParameters( Parameter[] params )
    {
        Map parameters = new HashMap();
        for ( int i = 0; i < params.length; i++ )
        {
            Parameter parameter = params[i];
            parameters.put( parameter.getName(), parameter.getValue() );
        }

        // Quiet param
        Boolean quietParam = ( Boolean ) parameters.get( QUIET_PARAMETER );
        if ( quietParam != null )
        {
            setQuietEnabled( quietParam.booleanValue() );
        }

        // Debug param
        Boolean debugParam = ( Boolean ) parameters.get( DEBUG_PARAMETER );
        if ( debugParam != null )
        {
            setDebugEnabled( debugParam.booleanValue() );
        }

        // Verbose param
        Boolean verboseParam = ( Boolean ) parameters.get( VERBOSE_PARAMETER );
        if ( verboseParam != null )
        {
            setVerboseEnabled( verboseParam.booleanValue() );
        }

        // Install-path param
        String installPathParam = ( String ) parameters.get( INSTALLPATH_PARAMETER );
        if ( installPathParam != null )
        {
            try
            {
                setLayout( installPathParam );
                if ( !isQuietEnabled() )
                {
                    notifyOutputListener( "loading settings from: " + getLayout().getConfigurationFile() );
                }
                ApplicationContext factory = null;
                URL configUrl;

                configUrl = getLayout().getConfigurationFile().toURL();
                factory = new FileSystemXmlApplicationContext( configUrl.toString() );
                setConfiguration( ( ServerStartupConfiguration ) factory.getBean( "configuration" ) );
            }
            catch ( MalformedURLException e )
            {
                notifyErrorListener( e.getMessage() );
                notifyExceptionListener( e );
            }
        }

        // Host param
        String hostParam = ( String ) parameters.get( HOST_PARAMETER );
        if ( hostParam != null )
        {
            host = hostParam;
        }
        else
        {
            host = DEFAULT_HOST;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "host set to default: " + host );
            }
        }

        // Port param
        Integer portParam = ( Integer ) parameters.get( PORT_PARAMETER );
        if ( portParam != null )
        {
            port = portParam.intValue();
        }
        else if ( getConfiguration() != null )
        {
            port = getConfiguration().getLdapPort();

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "port overriden by server.xml configuration: " + port );
            }
        }
        else
        {
            port = DEFAULT_PORT;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "port set to default: " + port );
            }
        }

        // User param
        String userParam = ( String ) parameters.get( USER_PARAMETER );
        if ( userParam != null )
        {
            user = userParam;
        }
        else
        {
            user = DEFAULT_USER;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "user set to default: " + user );
            }
        }

        // Password param
        String passwordParam = ( String ) parameters.get( PASSWORD_PARAMETER );
        if ( passwordParam != null )
        {
            password = passwordParam;
        }
        else
        {
            password = DEFAULT_PASSWORD;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "password set to default: " + password );
            }
        }

        // Auth param
        String authParam = ( String ) parameters.get( AUTH_PARAMETER );
        if ( authParam != null )
        {
            auth = authParam;
        }
        else
        {
            auth = DEFAULT_AUTH;

            if ( isDebugEnabled() )
            {
                notifyOutputListener( "authentication type set to default: " + auth );
            }
        }

        // LdifFile param
        File ldifFileParam = ( File ) parameters.get( FILE_PARAMETER );
        if ( ldifFileParam != null )
        {
            ldifFile = ldifFileParam;
        }

        // Ignore-Errors param
        Boolean ignoreErrorsParam = ( Boolean ) parameters.get( IGNOREERRORS_PARAMETER );
        if ( ignoreErrorsParam != null )
        {
            ignoreErrors = ignoreErrorsParam.booleanValue();
        }
        else if ( isDebugEnabled() )
        {
            notifyOutputListener( "ignore-errors set to default: false" );
        }

    }


    private void execute() throws Exception
    {
        if ( isDebugEnabled() )
        {
            notifyOutputListener( "Parameters for Ldif import request:" );
            notifyOutputListener( "port = " + port );
            notifyOutputListener( "host = " + host );
            notifyOutputListener( "user = " + user );
            notifyOutputListener( "auth type = " + auth );
            notifyOutputListener( "file = " + ldifFile );
            notifyOutputListener( "logs = " + logs );
        }

        int messageId = 0;

        // Login to the server
        bind( messageId++ );

        if ( isDebugEnabled() )
        {
            notifyOutputListener( "Connection to the server established.\n" + "Importing data ... " );
        }

        LdifReader ldifReader = null;

        try
        {
            ldifReader = new LdifReader( ldifFile );

        }
        catch ( NamingException ne )
        {
            notifyErrorListener( "Could not parse the LDIF file:" + ne.getMessage() );
            throw new ToolCommandException( "Naming Exception :" + ne.getMessage() );
        }

        if ( ldifReader.containsEntries() )
        {
            // Parse the file and inject every entry
            Iterator entries = ldifReader.iterator();
            long t0 = System.currentTimeMillis();
            int nbAdd = 0;

            while ( entries.hasNext() )
            {
                Entry entry = ( Entry ) entries.next();

                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    notifyErrorListener( "Found an error while persing an entry : "
                        + ldifReader.getError().getMessage() );

                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        notifyErrorListener( "Import failed..." );
                        throw new ToolCommandException( "Import failed..." );
                    }

                    notifyEntryAddFailedListener( entry.getDn() );
                }

                if ( addEntry( entry, messageId++ ) == IMPORT_ERROR )
                {
                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        notifyErrorListener( "Import failed..." );
                        throw new ToolCommandException( "Import failed..." );
                    }

                    notifyEntryAddFailedListener( entry.getDn() );
                }
                else
                {
                    nbAdd++;

                    notifyEntryAddedListener( entry.getDn() ); // The Entry class is not serializable, so we have to notify the listener using the toString method
                }

                if ( nbAdd % 10 == 0 )
                {
                    notifyOutputListener( new Character( '.' ) );
                }

                if ( nbAdd % 500 == 0 )
                {
                    notifyOutputListener( "" + nbAdd );
                }

            }

            long t1 = System.currentTimeMillis();

            notifyOutputListener( "Done!" );
            notifyOutputListener( nbAdd + " users added in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );

        }
        else
        {
            // Parse the file and inject every modification
            Iterator entries = ldifReader.iterator();
            long t0 = System.currentTimeMillis();
            int nbMod = 0;

            while ( entries.hasNext() )
            {
                Entry entry = ( Entry ) entries.next();

                // Check if we have had some error, has next() does not throw any exception
                if ( ldifReader.hasError() )
                {
                    notifyErrorListener( "Found an error while persing an entry : "
                        + ldifReader.getError().getMessage() );

                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        notifyErrorListener( "Import failed..." );
                        //System.exit( 1 );
                        throw new ToolCommandException( "Import failed..." );
                    }

                    notifyEntryAddFailedListener( entry.getDn() );
                }

                if ( changeEntry( entry, messageId++ ) == IMPORT_ERROR )
                {
                    if ( ignoreErrors == false )
                    {
                        unbind( messageId );

                        notifyErrorListener( "Import failed..." );
                        //System.exit( 1 );
                        throw new ToolCommandException( "Import failed..." );
                    }

                    notifyEntryAddFailedListener( entry.getDn() );
                }
                else
                {
                    nbMod++;

                    notifyEntryAddedListener( entry.getDn() ); // The Entry class is not serializable, so we have to notify the listener using the toString method
                }

                if ( nbMod % 10 == 0 )
                {
                    notifyOutputListener( new Character( '.' ) );
                }

                if ( nbMod % 500 == 0 )
                {
                    notifyOutputListener( "" + nbMod );
                }

            }

            long t1 = System.currentTimeMillis();

            notifyOutputListener( "Done!" );
            notifyOutputListener( nbMod + " users changed in " + ( ( t1 - t0 ) / 1000 ) + " seconds" );

        }

        // Logout to the server
        unbind( messageId++ );
    }


    private void notifyEntryAddedListener( Serializable o )
    {
        if ( this.entryAddedListener != null )
        {
            this.entryAddedListener.notify( o );
        }
    }


    private void notifyEntryAddFailedListener( Serializable o )
    {
        if ( this.entryAddFailedListener != null )
        {
            this.entryAddFailedListener.notify( o );
        }
    }


    /**
     * Executes the command.
     * <p>
     * Use the following Parameters and ListenerParameters to call the command.
     * <p>
     * Parameters : <ul>
     *      <li>"HOST_PARAMETER" with a value of type 'String', representing server host</li>
     *      <li>"PORT_PARAMETER" with a value of type 'Integer', representing server port</li>
     *      <li>"USER_PARAMETER" with a value of type 'String', representing user DN</li>
     *      <li>"PASSWORD_PARAMETER" with a value of type 'String', representing user password</li>
     *      <li>"AUTH_PARAMETER" with a value of type 'String', representing the type of authentication</li>
     *      <li>"FILE_PARAMETER" with a value of type 'String', representing the path to the ldif file to import</li>
     *      <li>"IGNOREERRORS_PARAMETER" with a value of type 'Boolean', true to continue to process the file even if errors are encountered</li>
     *      <li>"DEBUG_PARAMETER" with a value of type 'Boolean', true to enable debug</li>
     *      <li>"QUIET_PARAMETER" with a value of type 'Boolean', true to enable quiet</li>
     *      <li>"VERBOSE_PARAMETER" with a value of type 'Boolean', true to enable verbose</li>
     *      <li>"INSTALLPATH_PARAMETER" with a value of type 'String', representing the path to installation
     *          directory</li>
     *      <li>"CONFIGURATION_PARAMETER" with a value of type "Boolean", true to force loading the server.xml
     *          (requires "install-path")</li>
     * </ul>
     * <br />
     * ListenersParameters : <ul>
     *      <li>"OUTPUTLISTENER_PARAMETER", a listener that will receive all output messages. It returns
     *          messages as a String.</li>
     *      <li>"ERRORLISTENER_PARAMETER", a listener that will receive all error messages. It returns messages
     *          as a String.</li>
     *      <li>"EXCEPTIONLISTENER_PARAMETER", a listener that will receive all exception(s) raised. It returns
     *          Exceptions.</li>
     *      <li>"ENTRYADDEDLISTENER_PARAMETER", a listener that will receive a message each time an entry is
     *          added. It returns the DN of the added entry as a String.</li>
     *      <li>"ENTRYADDFAILEDLISTENER_PARAMETER", a listener that will receive a message each time an entry 
     *          has failed to import. It returns the DN og the entry on error.</li>
     * </ul>
     * <b>Note:</b> "HOST_PARAMETER", "PORT_PARAMETER", "USER_PARAMETER", "PASSWORD_PARAMETER", "AUTH_PARAMETER" and "IGNOREERRORS_PARAMETER" are required.
     */
    public void execute( Parameter[] params, ListenerParameter[] listeners )
    {
        processParameters( params );
        processListeners( listeners );
        try
        {
            execute();
        }
        catch ( Exception e )
        {
            notifyExceptionListener( e );
        }
    }


    /**
     * Initializes Listeners properties with the Listeners provided
     * 
     * @param listeners
     * 				The provided Listeners
     */
    private void processListeners( ListenerParameter[] listeners )
    {
        Map parameters = new HashMap();
        for ( int i = 0; i < listeners.length; i++ )
        {
            ListenerParameter parameter = listeners[i];
            parameters.put( parameter.getName(), parameter.getListener() );
        }

        // OutputListener param
        ToolCommandListener outputListener = ( ToolCommandListener ) parameters.get( OUTPUTLISTENER_PARAMETER );
        if ( outputListener != null )
        {
            this.outputListener = outputListener;
        }

        // ErrorListener param
        ToolCommandListener errorListener = ( ToolCommandListener ) parameters.get( ERRORLISTENER_PARAMETER );
        if ( errorListener != null )
        {
            this.errorListener = errorListener;
        }

        // ExceptionListener param
        ToolCommandListener exceptionListener = ( ToolCommandListener ) parameters.get( EXCEPTIONLISTENER_PARAMETER );
        if ( exceptionListener != null )
        {
            this.exceptionListener = exceptionListener;
        }

        // EntryAddedListener param
        ToolCommandListener entryAddedListener = ( ToolCommandListener ) parameters.get( ENTRYADDEDLISTENER_PARAMETER );
        if ( entryAddedListener != null )
        {
            this.entryAddedListener = entryAddedListener;
        }

        // EntryAddFailedListener param
        ToolCommandListener entryAddFailedListener = ( ToolCommandListener ) parameters
            .get( ENTRYADDFAILEDLISTENER_PARAMETER );
        if ( entryAddFailedListener != null )
        {
            this.entryAddFailedListener = entryAddFailedListener;
        }
    }

}
