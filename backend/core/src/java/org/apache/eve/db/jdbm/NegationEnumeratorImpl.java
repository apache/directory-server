package org.apache.eve.db.jdbm;


import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.LeafNode;
import org.apache.ldap.common.filter.BranchNode;

import org.apache.eve.db.*;


/**
 * Creates a naming enumeration over the set of candidates accepted by a negated
 * filter expression. 
 * 
 */
public class NegationEnumeratorImpl implements NegationEnumerator
{
    /** Database this conjunction is applied upon */
    private Database db = null;
    /** Top level expression evaluator */
    private Evaluator evaluator;


    /**
     * Creates a negation branch node enumerator.
     *
     * @param db the database to use for enumerations
     * @param evaluator the top level evaluator
     */
    public NegationEnumeratorImpl( Database db, Evaluator evaluator )
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
