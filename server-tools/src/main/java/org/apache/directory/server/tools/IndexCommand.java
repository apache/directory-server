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
import java.math.BigInteger;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import jdbm.helper.MRU;
import jdbm.recman.BaseRecordManager;
import jdbm.recman.CacheRecordManager;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.directory.server.core.partition.impl.btree.Tuple;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmMasterTable;
import org.apache.directory.server.core.schema.bootstrap.BootstrapRegistries;
import org.apache.directory.server.core.schema.bootstrap.BootstrapSchemaLoader;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Simple tool used to dump the contents of a jdbm based partition.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 442600 $
 */
public class IndexCommand extends ToolCommand
{
    private BootstrapRegistries bootstrapRegistries = new BootstrapRegistries();
    private BootstrapSchemaLoader loader = new BootstrapSchemaLoader();


    public IndexCommand()
    {
        super( "index" );
    }


    public void execute( CommandLine cmdline ) throws Exception
    {
        getLayout().verifyInstallation();
        loader.load( getConfiguration().getBootstrapSchemas(), bootstrapRegistries );

        String[] partitions = cmdline.getOptionValues( 'p' );
        String attribute = cmdline.getOptionValue( 'a' );

        for ( int ii = 0; ii < partitions.length; ii++ )
        {
            File partitionDirectory = new File( getLayout().getPartitionsDirectory(), partitions[ii] );
            buildIndex( partitionDirectory, bootstrapRegistries.getAttributeTypeRegistry().lookup( attribute ) );
        }
    }


    private void buildIndex( File partitionDirectory, AttributeType attributeType ) throws Exception
    {
        if ( !partitionDirectory.exists() )
        {
            System.err.println( "Partition directory " + partitionDirectory + " does not exist!" );
            System.exit( 1 );
        }

        String path = partitionDirectory.getPath() + File.separator + "master";
        BaseRecordManager base = new BaseRecordManager( path );
        base.disableTransactions();
        CacheRecordManager recMan = new CacheRecordManager( base, new MRU( 1000 ) );

        JdbmMasterTable master = new JdbmMasterTable( recMan );
        JdbmIndex index = new JdbmIndex( attributeType, partitionDirectory, 1000, 1000 );

        NamingEnumeration list = master.listTuples();
        while ( list.hasMore() )
        {
            Tuple tuple = ( Tuple ) list.next();
            BigInteger id = ( BigInteger ) tuple.getKey();
            Attributes entry = ( Attributes ) tuple.getValue();
            
            Attribute attr = AttributeUtils.getAttribute( entry, attributeType );
            if ( attr == null )
            {
                continue;
            }

            for ( int ii = 0; ii < attr.size(); ii++ )
            {
                index.add( attr.get( ii ), id );
            }
        }
        
        index.sync();
    }


    public Options getOptions()
    {
        Options opts = new Options();
        Option op = null;
        op = new Option( "p", "partitions", true, "the partitions to add the attribute indices to" );
        op.setRequired( true );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "a", "attributes", true, "the attribute to index" );
        op.setRequired( true );
        op.setValueSeparator( File.pathSeparatorChar );
        opts.addOption( op );
        op = new Option( "i", "install-path", true, "path to apacheds installation directory" );
        op.setRequired( true );
        opts.addOption( op );
        return opts;
    }
}
