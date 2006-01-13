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
package org.apache.ldap.common.codec.search.controls;


import org.apache.asn1.ber.AbstractContainer;
import org.apache.asn1.ber.IAsn1Container;
import org.apache.asn1.ber.grammar.IGrammar;


/**
 *  
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class EntryChangeControlContainer extends AbstractContainer implements IAsn1Container 
{
    /** EntryChangeControl */
    private EntryChangeControl control;

    /**
     * Creates a new EntryChangeControlContainer object.
     * We will store one grammar, it's enough ...
     */
    public EntryChangeControlContainer()
    {
        super( );
        currentGrammar = 0;
        grammars = new IGrammar[EntryChangeControlStatesEnum.NB_GRAMMARS];
        grammarStack = new IGrammar[1];
        stateStack = new int[1];
        nbGrammars = 0;

        grammars[EntryChangeControlStatesEnum.EC_GRAMMAR] = EntryChangeControlGrammar.getInstance();
        grammarStack[currentGrammar] = grammars[EntryChangeControlStatesEnum.EC_GRAMMAR];
        states = EntryChangeControlStatesEnum.getInstance();
    }


    /**
     * @return Returns the EntryChangeControl.
     */
    public EntryChangeControl getEntryChangeControl()
    {
        return control;
    }

    
    /**
     * Set a EntryChangeControl Object into the container. It will be completed
     * by the ldapDecoder.
     *
     * @param control the EntryChangeControl to set.
     */
    public void setEntryChangeControl( EntryChangeControl control )
    {
        this.control = control;
    }

    
    public void clean()
    {
        super.clean();
        control = null;
    }
}
