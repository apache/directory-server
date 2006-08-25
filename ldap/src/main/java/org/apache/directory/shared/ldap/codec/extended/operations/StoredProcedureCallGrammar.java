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

package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.grammar.AbstractGrammar;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;
import org.apache.log4j.Logger;


/**
 * ASN.1 BER Grammar for Stored Procedure Call Extended Operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureCallGrammar extends AbstractGrammar implements IGrammar
{
    // ~ Static fields/initializers -------------------------------------------

    /** The logger */
    private static final Logger log = Logger.getLogger( StoredProcedureCallGrammar.class );

    /** The instance of grammar. StoredProcedureGrammar is a singleton. */
    private static IGrammar instance = new StoredProcedureCallGrammar();


    // ~ Constructors ---------------------------------------------------------

    /**
     * Creates a new StoredProcedureCallGrammar object.
     */
    private StoredProcedureCallGrammar()
    {
        /**
         * TODO: Complete the grammar.
         */
    }


    // ~ Methods --------------------------------------------------------------

    /**
     * Get the instance of this grammar.
     *
     * @return An instance on the StoredProcedureCall Grammar
     */
    public static IGrammar getInstance()
    {
        return instance;
    }
}
