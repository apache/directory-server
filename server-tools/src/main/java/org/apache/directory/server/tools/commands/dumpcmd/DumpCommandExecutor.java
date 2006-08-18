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
package org.apache.directory.server.tools.commands.dumpcmd;


import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.directory.server.configuration.ServerStartupConfiguration;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmMasterTable;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.server.tools.ToolCommandListener;
import org.apache.directory.server.tools.execution.BaseToolCommandExecutor;
import org.apache.directory.server.tools.util.ListenerParameter;
import org.apache.directory.server.tools.util.Parameter;
import org.apache.directory.server.tools.util.ToolCommandException;
import org.apache.directory.shared.ldap.ldif.LdifUtils;
import org.apache.directory.shared.ldap.message.LockableAttributeImpl;
import org.apache.directory.shared.ldap.message.LockableAttributesImpl;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.UsageEnum;
import org.apache.directory.shared.ldap.util.Base64;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;


/**
 * This is the Executor Class of the Dump Command.
 * 
 * The command can be called using the 'execute' method.
 */
public class DumpCommandExecutor extends BaseToolCommandExecutor
{
    // Additional Parameters
    public static final String FILE_PARAMETER = "file";
    public static final String PARTITIONS_PARAMETER = "partitions";
    public static final String EXCLUDEDATTRIBUTES_PARAMETER = "excluded-attributes";
    public static final String INCLUDEOPERATIONAL_PARAMETER = "include-operational";

    private BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
    private BootstrapSchemaLoader loader = new BootstrapSchemaLoader();
    private Set exclusions = new HashSet();
    private boolean includeOperational = false;
    private String outputFile;
    private String[] partitions;
    private String[] excludedAttributes;


    public DumpCommandExecutor()
    {
        super( "dump" );
    }


    /**
     * Executes the command.
     * <p>
     * Use the following Parameters and ListenerParameters to call the command.
     * <p>
     * Parameters : <ul>
     *      <li>"FILE_PARAMETER" with a value of type 'String', representing the file to output the dump to</li>
     *      <li>"PARTITIONS_PARAMETER" with a value of type 'String[]', representing the partitions to dump</li>
     *      <li>"EXCLUDEDATTRIBUTES_PARAMETER" with a value of type 'String[]', representing the attributes 
     *          to exclude</li>
     *      <li>"INCLUDEOPERATIONAL_PARAMETER" with a value of type 'Boolean', to include operational
     *          attributes</li>
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
     * </ul>
     * <b>Note:</b> "FILE_PARAMETER", "PARTITIONS_PARAMETER" and "INSTALLPATH_PARAMETER" are required.
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


    private void execute() throws Exception
    {
        getLayout().verifyInstallation();
        loader.load( getConfiguration().getBootstrapSchemas(), bootstrapRegistries );

        PrintWriter out = null;
        if ( excludedAttributes != null )
        {
            AttributeTypeRegistry registry = bootstrapRegistries.getAttributeTypeRegistry();
            for ( int ii = 0; ii < excludedAttributes.length; ii++ )
            {
                AttributeType type = registry.lookup( excludedAttributes[ii] );
                exclusions.add( type.getName() );
            }
        }

        if ( outputFile == null )
        {
            out = new PrintWriter( System.out );
        }
        else
        {
            out = new PrintWriter( new FileWriter( outputFile ) );
        }

        for ( int ii = 0; ii < partitions.length; ii++ )
        {
            File partitionDirectory = new File( getLayout().getPartitionsDirectory(), partitions[ii] );
            out.println( "\n\n" );
            dump( partitionDirectory, out );
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

        // File param
        String fileParam = ( String ) parameters.get( FILE_PARAMETER );
        if ( fileParam != null )
        {
            outputFile = fileParam;
        }

        // Partitions param
        String[] partitionsParam = ( String[] ) parameters.get( PARTITIONS_PARAMETER );
        if ( partitionsParam != null )
        {
            partitions = partitionsParam;
        }

        // Excluded-Attributes param
        String[] excludedAttributesParam = ( String[] ) parameters.get( EXCLUDEDATTRIBUTES_PARAMETER );
        if ( excludedAttributesParam != null )
        {
            excludedAttributes = excludedAttributesParam;
        }

        // Include-Operationnal param
        Boolean includeOperationalParam = ( Boolean ) parameters.get( INCLUDEOPERATIONAL_PARAMETER );
        if ( includeOperationalParam != null )
        {
            includeOperational = includeOperationalParam.booleanValue();
        }
    }


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
    }


    private void dump( File partitionDirectory, PrintWriter out ) throws Exception
    {
        if ( !partitionDirectory.exists() )
        {
            notifyErrorListener( "Partition directory " + partitionDirectory + " does not exist!" );
            throw new ToolCommandException( "Partition directory " + partitionDirectory + " does not exist!" );
        }

        out.println( "# ========================================================================" );
        out.println( "# ApacheDS Tools Version: " + getVersion() );
        out.println( "# Partition Directory: " + partitionDirectory );
        out.println( "# ========================================================================\n\n" );

        String path = partitionDirectory.getPath() + File.separator + "master";
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();
        CacheRecordManager recMan = new CacheRecordManager( base, new MRU( 1000 ) );

        JdbmMasterTable master = new JdbmMasterTable( recMan );
        AttributeType attributeType = bootstrapRegistries.getAttributeTypeRegistry().lookup( "apacheUpdn" );
        JdbmIndex idIndex = new JdbmIndex( attributeType, partitionDirectory, 1000 );

        out.println( "#---------------------" );
        NamingEnumeration list = master.listTuples();
        StringBuffer buf = new StringBuffer();
        while ( list.hasMore() )
        {
            Tuple tuple = ( Tuple ) list.next();
            BigInteger id = ( BigInteger ) tuple.getKey();
            String dn = ( String ) idIndex.reverseLookup( id );
            Attributes entry = ( Attributes ) tuple.getValue();

            filterAttributes( dn, entry );

            if ( !( entry instanceof LockableAttributesImpl ) )
            {
                Attributes tmp = entry;
                entry = new LockableAttributesImpl();
                NamingEnumeration attrs = tmp.getAll();
                while ( attrs.hasMore() )
                {
                    Attribute attr = ( Attribute ) attrs.next();
                    LockableAttributeImpl myattr = new LockableAttributeImpl( attr.getID() );
                    entry.put( myattr );
                    for ( int ii = 0; ii < attr.size(); ii++ )
                    {
                        Object value = attr.get(ii);
                        
                        // Checking if the value is binary
                        if ( value instanceof byte[] )
                        {
                        	// It is binary, so we have to encode it using Base64 before adding it
                        	char[] encoded = Base64.encode( ( byte[] ) value );
                        	
                        	myattr.add( new String( encoded ) );                        	
                        }
                        else if ( value instanceof String )
                        {
                        	// It's a String but, we have to check if encoding isn't required
                        	String str = (String) value;
                        	if ( !LdifUtils.isLDIFSafe( str ) )
                        	{
                        		char[] encoded = Base64.encode( ( ( String ) value ).getBytes() );
                        		
                        		myattr.add( new String( encoded ) );
                        	}
                        	else
                        	{
                        		myattr.add( value );
                        	}
                        }
                    }
                }
            }

            buf.append( "# Entry: " ).append( id ).append( "\n#---------------------\n\n" );
            buf.append( "dn: " ).append( dn ).append( "\n" ).append( entry );
            if ( list.hasMore() )
            {
                buf.append( "\n\n#---------------------\n" );
            }
            out.print( buf.toString() );
            out.flush();
            buf.setLength( 0 );
        }
    }

    private void filterAttributes( String dn, Attributes entry ) throws NamingException
    {
        List toRemove = new ArrayList();
        AttributeTypeRegistry registry = bootstrapRegistries.getAttributeTypeRegistry();
        NamingEnumeration attrs = entry.getAll();
        while ( attrs.hasMore() )
        {
            Attribute attr = ( Attribute ) attrs.next();
            if ( !registry.hasAttributeType( attr.getID() ) )
            {
                if ( !isQuietEnabled() )
                {
                    notifyOutputListener( "# Cannot properly filter unrecognized attribute " + attr.getID() + " in "
                        + dn );
                }
                continue;
            }

            AttributeType type = registry.lookup( attr.getID() );
            boolean isOperational = type.getUsage() != UsageEnum.USERAPPLICATIONS;
            if ( exclusions.contains( attr.getID() ) || ( isOperational && ( !includeOperational ) ) )
            {
                toRemove.add( attr.getID() );
            }
        }
        for ( int ii = 0; ii < toRemove.size(); ii++ )
        {
            String id = ( String ) toRemove.get( ii );
            entry.remove( id );
            if ( isDebugEnabled() )
            {
                notifyOutputListener( "# Excluding attribute " + id + " in " + dn );
            }
        }
    }
}
