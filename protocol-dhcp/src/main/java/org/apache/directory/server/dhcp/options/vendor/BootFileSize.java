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
 * This option specifies the length in 512-octet blocks of the default
 * boot image for the client.  The file length is specified as an
 * unsigned 16-bit integer.
 * 
 * The code for this option is 13, and its length is 2.
 */
package org.apache.directory.server.dhcp.options.vendor;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


public class BootFileSize extends DhcpOption
{
    private byte[] bootFileSize;


    public BootFileSize(byte[] bootFileSize)
    {
        super( 13, 2 );
        this.bootFileSize = bootFileSize;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( bootFileSize );
    }
}
