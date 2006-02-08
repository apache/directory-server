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
package org.apache.directory.shared.ldap.codec.search.controls;


import org.apache.directory.shared.asn1.ber.AbstractContainer;
import org.apache.directory.shared.asn1.ber.IAsn1Container;
import org.apache.directory.shared.asn1.ber.grammar.IGrammar;


/**
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class PSearchControlContainer extends AbstractContainer implements IAsn1Container
{
    /** PSearchControl */
    private PSearchControl control;


    /**
     * Creates a new PSearchControlContainer object. We will store one grammar,
     * it's enough ...
     */
    public PSearchControlContainer()
    {
        super();
        currentGrammar = 0;
        grammars = new IGrammar[PSearchControlStatesEnum.NB_GRAMMARS];
        grammarStack = new IGrammar[1];
        stateStack = new int[1];
        nbGrammars = 0;

        grammars[PSearchControlStatesEnum.PSEARCH_GRAMMAR] = PSearchControlGrammar.getInstance();
        grammarStack[currentGrammar] = grammars[PSearchControlStatesEnum.PSEARCH_GRAMMAR];
        states = PSearchControlStatesEnum.getInstance();
    }


    /**
     * @return Returns the persistent search control.
     */
    public PSearchControl getPSearchControl()
    {

        return control;
    }


    /**
     * Set a PSearchControl Object into the container. It will be completed by
     * the ldapDecoder.
     * 
     * @param control
     *            the PSearchControl to set.
     */
    public void setPSearchControl( PSearchControl control )
    {
        this.control = control;
    }


    public void clean()
    {
        super.clean();
        control = null;
    }
}
