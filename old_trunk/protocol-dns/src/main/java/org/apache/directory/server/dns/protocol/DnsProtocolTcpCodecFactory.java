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

package org.apache.directory.server.dns.protocol;


import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 545041 $, $Date: 2007-06-06 20:31:34 -0700 (Wed, 06 Jun 2007) $
 */
public class DnsProtocolTcpCodecFactory implements ProtocolCodecFactory
{
    private static final DnsProtocolTcpCodecFactory INSTANCE = new DnsProtocolTcpCodecFactory();


    /**
     * Returns the singleton instance of {@link DnsProtocolTcpCodecFactory}.
     *
     * @return The singleton instance of {@link DnsProtocolTcpCodecFactory}.
     */
    public static DnsProtocolTcpCodecFactory getInstance()
    {
        return INSTANCE;
    }


    private DnsProtocolTcpCodecFactory()
    {
        // Private constructor prevents instantiation outside this class.
    }


    public ProtocolEncoder getEncoder()
    {
        // Create a new encoder.
        return new DnsTcpEncoder();
    }


    public ProtocolDecoder getDecoder()
    {
        // Create a new decoder.
        return new DnsTcpDecoder();
    }
}
