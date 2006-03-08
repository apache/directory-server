/*
 *   Copyright 2006 The Apache Software Foundation
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

package org.apache.directory.shared.ldap.codec.extended.operations;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;


/**
 * A container for the StoredProcedure codec
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class StoredProcedureContainer extends AbstractContainer implements IAsn1Container
{
    // ~ Instance fields
    // ----------------------------------------------------------------------------

    /** StoredProcedure */
    private StoredProcedure storedProcedure;


    // ~ Constructors
    // -------------------------------------------------------------------------------

    public StoredProcedureContainer()
    {
        super();
        currentGrammar = 0;
        grammars = new IGrammar[1];
        grammarStack = new IGrammar[1];
        stateStack = new int[1];
        nbGrammars = 0;

        grammars[StoredProcedureStatesEnum.STORED_PROCEDURE_GRAMMAR] = StoredProcedureGrammar.getInstance();

        grammarStack[currentGrammar] = grammars[StoredProcedureStatesEnum.STORED_PROCEDURE_GRAMMAR];

        states = StoredProcedureStatesEnum.getInstance();
    }


    // ~ Methods
    // ------------------------------------------------------------------------------------
    /**
     * @return Returns the ldapMessage.
     */
    public StoredProcedure getStoredProcedure()
    {
        return storedProcedure;
    }


    /**
     * Set a StoredProcedure object into the container. It will be completed by the
     * ldapDecoder.
     * 
     * @param ldapMessage
     *            The ldapMessage to set.
     */
    public void setStoredProcedure( StoredProcedure storedProcedure )
    {
        this.storedProcedure = storedProcedure;
    }


    public void clean()
    {
        super.clean();

        storedProcedure = null;
    }
}
