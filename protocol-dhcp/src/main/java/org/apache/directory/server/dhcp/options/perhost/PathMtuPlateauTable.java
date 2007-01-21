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

/**
 * This option specifies a table of MTU sizes to use when performing
 * Path MTU Discovery as defined in RFC 1191.  The table is formatted as
 * a list of 16-bit unsigned integers, ordered from smallest to largest.
 * The minimum MTU value cannot be smaller than 68.
 * 
 * The code for this option is 25.  Its minimum length is 2, and the
 * length MUST be a multiple of 2.
 */
package org.apache.directory.server.dhcp.options.perhost;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


public class PathMtuPlateauTable extends DhcpOption
{
    private byte[] pathMtuPlateauTable;


    public PathMtuPlateauTable(byte[] pathMtuPlateauTable)
    {
        super( 25, 2 );
        this.pathMtuPlateauTable = pathMtuPlateauTable;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( pathMtuPlateauTable );
    }
}
