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
package org.apache.directory.shared.ldap.codec.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.AbstractAsn1Object;
import org.apache.directory.shared.asn1.codec.EncoderException;


/**
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class CascadeControl extends AbstractAsn1Object
{
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate( 0 );

    /**
     * Default constructor
     *
     */
    public CascadeControl()
    {
        super();
    }

    /**
     * Returns 0 everytime.
     */
    public int computeLength()
    {
        return 0;
    }


    /**
     * Encodes the control: does nothing but returns an empty buffer.
     */
    public ByteBuffer encode( ByteBuffer buffer ) throws EncoderException
    {
        return EMPTY_BUFFER;
    }
}
