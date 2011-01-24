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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.awt.Dimension;
import java.awt.Toolkit;

import org.apache.directory.server.core.partition.impl.btree.BTreePartition;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A partition database viewer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PartitionViewer
{
    private static final Logger LOG = LoggerFactory.getLogger( PartitionViewer.class );

    /** A handle on the atomic partition */
    private BTreePartition partition;
    
    /** A handle on the global schemaManager */
    private SchemaManager schemaManager;


    public PartitionViewer( BTreePartition db, SchemaManager schemaManager )
    {
        this.partition = db;
        this.schemaManager = schemaManager;
    }


    // added return value so expressions in debugger does not freak with void
    public int execute() throws Exception
    {
        Thread t = new Thread( new Runnable() {
            public void run()
            {
                PartitionFrame frame = null;
                try
                {
                    frame = new PartitionFrame( PartitionViewer.this.partition, schemaManager );
                }
                catch ( Exception e )
                {
                    e.printStackTrace();
                    return;
                }
                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                Dimension frameSize = frame.getSize();
                frameSize.height = ( ( frameSize.height > screenSize.height ) ? screenSize.height : frameSize.height );
                frameSize.width = ( ( frameSize.width > screenSize.width ) ? screenSize.width : frameSize.width );
                frame.setLocation( ( screenSize.width - frameSize.width ) / 2, ( screenSize.height - frameSize.height ) / 2 );

                frame.setVisible( true );
                LOG.debug( frameSize + "" );
            }
        });

        t.run();
        return 0;
    }
}
