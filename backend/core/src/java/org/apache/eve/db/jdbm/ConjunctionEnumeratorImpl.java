package org.apache.eve.db.jdbm;


import java.util.ArrayList;
import java.math.BigInteger;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;

import org.apache.eve.db.*;


/**
 * An enumerator implementation which creates a NamingEnumeration over the
 * candidates satisfying a filter expression of AND'ed filter sub-expressions.
 * 
 */
public class ConjunctionEnumeratorImpl implements Enumerator
{
    /** Top level evaluator Avalon dependency */
    private Evaluator evaluator;
    /** Top level expression enumerator - non Avalon dependency avaoids cycle */
    private Enumerator enumerator;


    /**
     * Creates a conjunction expression enumerator.
     *
     * @param enumerator the top level expression enumerator
     * @param evaluator the top level expression evaluator
     */
    public ConjunctionEnumeratorImpl( Enumerator enumerator, Evaluator evaluator )
    {
        this.enumerator = enumerator;
        this.evaluator = evaluator;
    }


    /**
     * @see org.apache.eve.db.Enumerator#enumerate(ExprNode)
     */
    public NamingEnumeration enumerate( final ExprNode node )
        throws NamingException
    {
        int minIndex = 0;
        int minValue = Integer.MAX_VALUE;
        int value = Integer.MAX_VALUE;

        /*
         * We scan the child nodes of a branch node searching for the child
         * expression node with the smallest scan count.  This is the child
         * we will use for iteration by creating a NamingEnumeration over its 
         * expression.
         */
        final ArrayList children = ( ( BranchNode ) node ).getChildren();
        for ( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = ( ExprNode ) children.get( ii );
            value = ( ( BigInteger ) child.get( "count" ) ).intValue();
            minValue = Math.min( minValue, value );

            if ( minValue == value )
            {
                minIndex = ii;
            }
        }

        // Once found we build the child enumeration & the wrapping enum
        final ExprNode minChild = ( ExprNode ) children.get( minIndex );
        IndexAssertion assertion = new IndexAssertion()
        {
            public boolean assertCandidate( IndexRecord rec )
                throws NamingException
            {
                for ( int ii = 0; ii < children.size(); ii++ )
                {
                    ExprNode child = ( ExprNode ) children.get( ii );

                    // Skip the child (with min scan count) chosen for enum
                    if ( child == minChild )
                    {
                        continue;
                    } 
                    else if ( ! evaluator.evaluate( child, rec ) )
                    {
                        return false;
                    }
                }

                return true;
            }
        };

        // Do recursive call to build child enumeration then wrap and return
        NamingEnumeration underlying = enumerator.enumerate( minChild );
        IndexAssertionEnumeration iae;
        iae = new IndexAssertionEnumeration( underlying, assertion );
        return iae;
    }
}
