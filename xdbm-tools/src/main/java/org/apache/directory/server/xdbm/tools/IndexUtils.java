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
package org.apache.directory.server.xdbm.tools;


import java.io.OutputStream;
import java.io.PrintStream;

import org.apache.directory.server.xdbm.Index;
import org.apache.directory.server.xdbm.IndexCursor;
import org.apache.directory.shared.ldap.model.entry.Entry;


/**
 * Utility methods for Index objects.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class IndexUtils
{
    public static void printContents( Index<?, Entry, Long> idx ) throws Exception
    {
        printContents( idx, System.out );
    }


    public static void printContents( Index<?, Entry, Long> idx, OutputStream outputStream ) throws Exception
    {
        PrintStream out;

        if ( outputStream == null )
        {
            out = System.out;
        }
        else if ( outputStream instanceof PrintStream )
        {
            out = ( PrintStream ) outputStream;
        }
        else
        {
            out = new PrintStream( outputStream );
        }

        IndexCursor<?, Long> cursor = idx.forwardCursor();
        cursor.first();
        for ( Object entry : cursor )
        {
            out.println( entry );
        }
    }
}
