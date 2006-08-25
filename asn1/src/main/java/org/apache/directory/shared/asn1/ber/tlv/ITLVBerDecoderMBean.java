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
package org.apache.directory.shared.asn1.ber.tlv;


import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface ITLVBerDecoderMBean
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * Set the number of bytes that can be used to encode the Value length,
     * including the first byte. Max is 127 if the Length use a definite form,
     * default is 1
     * 
     * @param length
     *            The number of byte to use
     */
    void setMaxLengthLength( int length ) throws DecoderException;


    /**
     * Set the maximum number of bytes that should be used to encode a Tag
     * label, including the first byte. Default is 1, no maximum
     * 
     * @param length
     *            The length to use
     */
    void setMaxTagLength( int length );


    /** Allow indefinite length. */
    void allowIndefiniteLength();


    /** Disallow indefinite length. */
    void disallowIndefiniteLength();


    /**
     * Get the actual maximum number of bytes that can be used to encode the
     * Length
     * 
     * @return The maximum bytes of the Length
     */
    int getMaxLengthLength();


    /**
     * Get the actual maximum number of bytes that can be used to encode the Tag
     * 
     * @return The maximum length of the Tag
     */
    int getMaxTagLength();


    /**
     * Tell if indefinite length form could be used for Length
     * 
     * @return <code>true</code> if the Indefinite form is allowed
     */
    boolean isIndefiniteLengthAllowed();
}
