/*
 *   Copyright 2004 The Apache Software Foundation
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
package org.apache.eve.db;


import java.util.ArrayList;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;


/**
 * Creates a naming enumeration over the set of candidates accepted by a set
 * of filter expressions joined together using the OR ('|') operator. 
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DisjunctionEnumerator implements Enumerator
{
    /** Top level expression enumerator - non Avalon dependency avaoids cycle */
    private Enumerator enumerator;


    /**
     * Creates a disjunction enumerator using a top level enumerator.
     *
     * @param enumerator the top level enumerator
     */
    public DisjunctionEnumerator( Enumerator enumerator )
    {
        this.enumerator = enumerator;
    }


    /**
     * @see Enumerator#enumerate(ExprNode)
     */
    public NamingEnumeration enumerate( ExprNode node ) throws NamingException
    {
        ArrayList children = ( ( BranchNode ) node ).getChildren();
        NamingEnumeration [] childEnumerations = new NamingEnumeration [children.size()];

        // Recursively create NamingEnumerations for each child expression node
        for ( int ii = 0; ii < childEnumerations.length; ii++ ) 
        {
            childEnumerations[ii] = enumerator.enumerate( ( ExprNode ) children.get( ii ) );
        }

        return new DisjunctionEnumeration( childEnumerations );
    }
}
