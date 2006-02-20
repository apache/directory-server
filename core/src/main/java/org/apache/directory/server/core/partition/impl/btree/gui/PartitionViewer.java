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
package org.apache.directory.server.core.partition.impl.btree.gui;


import java.awt.Dimension;
import java.awt.Toolkit;

import javax.naming.NamingException;

import org.apache.directory.server.core.partition.impl.btree.BTreeDirectoryPartition;
import org.apache.directory.server.core.partition.impl.btree.SearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A partition database viewer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class PartitionViewer
{
    private static final Logger log = LoggerFactory.getLogger( PartitionViewer.class );

    /** A handle on the atomic partition */
    private BTreeDirectoryPartition partition;
    private SearchEngine eng;


    public PartitionViewer(BTreeDirectoryPartition db, SearchEngine eng)
    {
        this.partition = db;
        this.eng = eng;
    }


    //    /**
    //     * Viewer main is not really used.
    //     *
    //     * @param argv the var args
    //     */
    //    public static void main( String [] argv )
    //    {
    //        // set up system Look&Feel
    //        try
    //        {
    //            UIManager.setLookAndFeel( UIManager.getSystemLookAndFeelClassName() ) ;
    //        }
    //        catch ( Exception e )
    //        {
    //            System.out.println( "Could not set look and feel to use " +
    //                UIManager.getSystemLookAndFeelClassName() + "." ) ;
    //            e.printStackTrace() ;
    //        }
    //
    //        PartitionViewer viewer = new PartitionViewer(  ) ;
    //
    //        try
    //        {
    //            viewer.execute() ;
    //        }
    //        catch ( Exception e )
    //        {
    //            e.printStackTrace() ;
    //            System.exit( -1 ) ;
    //        }
    //    }

    public void execute() throws NamingException
    {
        PartitionFrame frame = new PartitionFrame( partition, eng );

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getSize();
        frameSize.height = ( ( frameSize.height > screenSize.height ) ? screenSize.height : frameSize.height );
        frameSize.width = ( ( frameSize.width > screenSize.width ) ? screenSize.width : frameSize.width );
        frame.setLocation( ( screenSize.width - frameSize.width ) / 2, ( screenSize.height - frameSize.height ) / 2 );

        frame.setVisible( true );
        log.debug( frameSize + "" );
    }
}
