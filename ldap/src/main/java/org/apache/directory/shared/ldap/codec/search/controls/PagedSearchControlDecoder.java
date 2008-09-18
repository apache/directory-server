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
package org.apache.directory.shared.ldap.codec.search.controls;


import java.nio.ByteBuffer;

import org.apache.directory.shared.asn1.Asn1Object;
import org.apache.directory.shared.asn1.ber.Asn1Decoder;
import org.apache.directory.shared.asn1.codec.DecoderException;
import org.apache.directory.shared.ldap.codec.ControlDecoder;


/**
 * A decoder for PagedSearchControls.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 664290 $, $Date: 2008-06-07 08:28:06 +0200 (Sat, 07 Jun 2008) $, 
 */
public class PagedSearchControlDecoder extends Asn1Decoder implements ControlDecoder
{
    /** The paged search OID */
    private final static String CONTROL_TYPE_OID = "1.2.840.113556.1.4.319";

    /** An instance of this decoder */
    private static final Asn1Decoder decoder = new Asn1Decoder();

    /**
     * Return the paged search OID
     * 
     * @see org.apache.directory.shared.ldap.codec.ControlDecoder#getControlType()
     */
    public String getControlType()
    {
        return CONTROL_TYPE_OID;
    }

    /**
     * Decode the paged search control
     * 
     * @param controlBytes The bytes array which contains the encoded paged search
     * 
     * @return A valid PagedSearch object
     * 
     * @throws DecoderException If the decoding found an error
     * @throws NamingException It will never be throw by this method
     */
    public Asn1Object decode( byte[] controlBytes ) throws DecoderException
    {
        ByteBuffer bb = ByteBuffer.wrap( controlBytes );
        PagedSearchControlContainer container = new PagedSearchControlContainer();
        decoder.decode( bb, container );
        return container.getPagedSearchControl();
    }
}
