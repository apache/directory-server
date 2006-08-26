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

package org.apache.directory.server.dhcp.options.perinterface;


import java.nio.ByteBuffer;

import org.apache.directory.server.dhcp.options.DhcpOption;


/**
 * This option specifies whether or not the client should respond to
 * subnet mask requests using ICMP.  A value of 0 indicates that the
 * client should not respond.  A value of 1 means that the client should
 * respond.
 * 
 * The code for this option is 30, and its length is 1.
 */
public class MaskSupplier extends DhcpOption
{
    private byte[] maskSupplier;


    public MaskSupplier(byte[] maskSupplier)
    {
        super( 30, 1 );
        this.maskSupplier = maskSupplier;
    }


    protected void valueToByteBuffer( ByteBuffer out )
    {
        out.put( maskSupplier );
    }
}
