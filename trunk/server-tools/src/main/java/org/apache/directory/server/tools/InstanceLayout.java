/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.tools;


import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;


/**
 * TODO InstanceLayout.
 *
 *       ${ads-instance-name}
 *       |-- conf
 *       |-- ldif
 *       |-- log
 *       |-- partitions
 *       |   |-- example
 *       |   |-- schema
 *       |   `-- system
 *       `-- run
 *       
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class InstanceLayout
{
    /** the installation instance's base directory */
    private final File instanceRoot;

    /** a file system directory filter */
    private FileFilter dirFilter = new FileFilter()
    {
        public boolean accept( File pathname )
        {
            return pathname.isDirectory();
        }

    };


    public InstanceLayout( File location )
    {
        this.instanceRoot = location;
    }


    public InstanceLayout( String location )
    {
        this.instanceRoot = new File( location );
    }


    public File getPartitionsDir()
    {
        return new File( instanceRoot, "partitions" );
    }


    public File getLogDir()
    {
        return new File( instanceRoot, "log" );
    }


    public File getConfDir()
    {
        return new File( instanceRoot, "conf" );
    }


    /**
     * returns a list of partition directories
     * @return list of partition directories
     */
    public List<File> getPartitions()
    {
        List<File> partitions = new ArrayList<File>();

        File[] dirs = getPartitionsDir().listFiles( dirFilter );

        for ( File f : dirs )
        {
            File masterFile = new File( f, "master.db" );
            if ( masterFile.isFile() && masterFile.exists() )
            {
                partitions.add( f );
            }
        }

        return partitions;
    }
}
