/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.directory.shared.asn1.ber.grammar;


import javax.naming.NamingException;

import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.codec.DecoderException;


/**
 * The interface which expose common behavior of a Gramar implementer.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface IGrammar
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * This method, when called, execute an action on the current data stored in
     * the container.
     * 
     * @param asn1Container
     *            Store the data being processed.
     * @throws DecoderException
     *             Thrown when an unrecoverable error occurs.
     */
    void executeAction( IAsn1Container asn1Container ) throws DecoderException, NamingException;


    /**
     * Get the grammar name
     * 
     * @return Return the grammar's name
     */
    String getName();


    /**
     * Get the statesEnum for the current grammar
     * 
     * @return The specific States Enum for the current grammar
     */
    IStates getStatesEnum();


    /**
     * Set the grammar's name
     * 
     * @param name
     *            The grammar name
     */
    void setName( String name );
}
