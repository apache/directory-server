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
package org.apache.directory.server.tools.commands.dumpcmd;


import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.server.tools.execution.ToolCommandExecutorStub;
import org.apache.directory.server.tools.request.BaseToolCommandCL;
import org.apache.directory.server.tools.util.Parameter;


/**
 * Simple tool used to dump the contents of a jdbm based partition.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 379013 $
 */
public class DumpCommandCL extends BaseToolCommandCL
{

    public DumpCommandCL()
    {
        super( "dump" );
    }


    public void processOptions( CommandLine cmd )
    {
        // -------------------------------------------------------------------
        // figure out the 'file' to output the dump to
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'f' ) )
        {
            String fileName = cmd.getOptionValue( 'f' );

            parameters.add( new Parameter( DumpCommandExecutor.FILE_PARAMETER, fileName ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'partitions' to dump
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'p' ) )
        {
            String[] partitions = cmd.getOptionValues( 'p' );

            parameters.add( new Parameter( DumpCommandExecutor.PARTITIONS_PARAMETER, partitions ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'attributes' to exclude
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'e' ) )
        {
            String[] excludedAttributes = cmd.getOptionValues( 'e' );

            parameters.add( new Parameter( DumpCommandExecutor.EXCLUDEDATTRIBUTES_PARAMETER, excludedAttributes ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'include-operational' flag
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'o' ) )
        {
            parameters.add( new Parameter( DumpCommandExecutor.INCLUDEOPERATIONAL_PARAMETER, new Boolean( true ) ) );
        }
        else
        {
            // Default value is false
            parameters.add( new Parameter( DumpCommandExecutor.INCLUDEOPERATIONAL_PARAMETER, new Boolean( false ) ) );
        }

        // -------------------------------------------------------------------
        // figure out the 'install-path'
        // and verify if the -i option is present when the -c option is used
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'i' ) )
        {
            parameters.add( new Parameter( DumpCommandExecutor.INSTALLPATH_PARAMETER, cmd.getOptionValue( 'i' ) ) );
        }
        else if ( cmd.hasOption( 'c' ) )
        {
            System.err.println( "forced configuration load (-c) requires the -i option" );
            System.exit( 1 );
        }

        // -------------------------------------------------------------------
        // figure out the 'configuration' flag
        // -------------------------------------------------------------------
        if ( cmd.hasOption( 'c' ) )
        {
            parameters.add( new Parameter( DumpCommandExecutor.CONFIGURATION_PARAMETER, new Boolean( true ) ) );
        }

        // -------------------------------------------------------------------
        // Transform other options into params
        // -------------------------------------------------------------------
        parameters.add( new Parameter( DumpCommandExecutor.DEBUG_PARAMETER, new Boolean( isDebugEnabled() ) ) );
        parameters.add( new Parameter( DumpCommandExecutor.QUIET_PARAMETER, new Boolean( isQuietEnabled() ) ) );
        parameters.add( new Parameter( DumpCommandExecutor.VERBOSE_PARAMETER, new Boolean( isVerboseEnabled() ) ) );
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = new Option( "f", "file", true, "file to output the dump to" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "p", "partitions", true, "the partitions to dump" );
        op.setRequired( true );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "e", "excluded-attributes", true, "the attributes to exclude" );
        op.setRequired( false );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "o", "include-operational", false, "include operational attributes: defaults to false" );
        op.setRequired( false );
        opts.addOption( op );
        op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        op.setRequired( true );
        opts.addOption( op );
        return opts;
    }


    public ToolCommandExecutorStub getStub()
    {
        return new DumpCommandExecutorStub();
    }
}
