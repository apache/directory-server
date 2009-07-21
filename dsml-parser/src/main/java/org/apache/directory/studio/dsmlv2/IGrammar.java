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

package org.apache.directory.studio.dsmlv2;


import java.io.IOException;

import org.xmlpull.v1.XmlPullParserException;


/**
 * The interface which expose common behavior of a Gramar implementer.
 */
public interface IGrammar
{
    // ~ Methods
    // ------------------------------------------------------------------------------------

    /**
     * This method, when called, execute an action on the current data stored in
     * the container.
     * 
     * @param container
     *            the DSML container
     * @throws XmlPullParserException
     *      Thrown when an unrecoverable error occurs.
     * @throws IOException
     */
    void executeAction( Dsmlv2Container container ) throws XmlPullParserException, IOException;


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