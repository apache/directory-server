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


import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.BranchNode;


/**
 * Creates a naming enumeration over the set of candidates accepted by a negated
 * filter expression. 
 * 
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class NegationEnumerator implements Enumerator
{
    /** Database this conjunction is applied upon */
    private Database db = null;
    /** Top level expression evaluator */
    private ExpressionEvaluator evaluator;


    /**
     * Creates a negation branch node enumerator.
     *
     * @param db the database to use for enumerations
     * @param evaluator the top level evaluator
     */
    public NegationEnumerator( Database db, ExpressionEvaluator evaluator )
    {
        this.db = db;
        this.evaluator = evaluator;
    }


    /**
     * @see Enumerator#enumerate(ExprNode)
     */
    public NamingEnumeration enumerate( ExprNode node ) throws NamingException
    {
        Index idx = null;
        final BranchNode bnode = ( BranchNode ) node;
        NamingEnumeration childEnumeration = null;
        NamingEnumeration enumeration = null;

        // Iterates over entire set of index values
        if ( bnode.getChild().isLeaf() )
        {
            LeafNode child = ( LeafNode ) bnode.getChild();
            idx = db.getUserIndex( child.getAttribute() );
            childEnumeration = idx.listIndices();
        } 
        // Iterates over the entire set of entries
        else 
        {
            idx = db.getNdnIndex();
            childEnumeration = idx.listIndices();
        }


        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( IndexRecord rec ) throws NamingException
            {
                // NOTICE THE ! HERE
                // The candidate is valid if it does not pass assertion. A
                // candidate that passes assertion is therefore invalid.
                return ! evaluator.evaluate( bnode.getChild(), rec );
            }
        };

        enumeration = new IndexAssertionEnumeration( childEnumeration, assertion, true );
        return enumeration;
    }
}
