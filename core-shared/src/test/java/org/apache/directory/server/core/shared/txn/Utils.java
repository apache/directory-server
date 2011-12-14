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
package org.apache.directory.server.core.shared.txn;

import java.io.File;

public class Utils
{
    public static boolean deleteDirectory( File directory )
    {

        // System.out.println("removeDirectory " + directory);

        if ( directory == null )
        {
            return false;
        }

        if ( !directory.exists() )
        {
            return true;
        }

        if ( !directory.isDirectory() )
        {
            return false;
        }

        String[] list = directory.list();

        // Some JVMs return null for File.list() when the
        // directory is empty.

        if ( list != null )
        {
            for ( int i = 0; i < list.length; i++ )
            {
                File entry = new File( directory, list[i] );

                if ( entry.isDirectory() )
                {
                    if ( !deleteDirectory( entry ) )
                    {
                        return false;
                    }
                }
                else
                {
                    if ( !entry.delete() )
                    {
                        return false;
                    }
                }
            }
        }

        return directory.delete();
    }
}
