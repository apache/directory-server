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

/*
 * $Id$
 *
 * -- (c) LDAPd Group
 * -- Please refer to the LICENSE.txt file in the root directory of      --
 * -- any LDAPd project for copyright and distribution information.      --
 *
 */
package org.apache.directory.shared.ldap.filter;


import java.math.BigInteger;


/**
 * Node used for the application of arbitrary predicates on return candidates.
 * Applies dynamic and programatic criteria for the selection of candidates for
 * return. Nodes of this type may be introduced into the filter expression to
 * provided the opportunity to constrain the search further without altering the
 * search algorithm.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision$
 */
public class AssertionNode extends AbstractExprNode
{
    /** Setting scan count to max */
    private static final BigInteger MAX = BigInteger.valueOf( Integer.MAX_VALUE );

    /** Setting display string to [MAX] */
    private static final String MAX_STR = "[" + MAX.toString() + "]";

    /** The assertion or predicate to apply */
    private final Assertion m_assertion;

    /** Description of assertion for polish printouts */
    private final String m_desc;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------

    /**
     * Creates an AssertionNode using an arbitrary candidate assertion.
     * 
     * @param a_assertion
     *            the arbitrary selection logic.
     */
    public AssertionNode(Assertion a_assertion)
    {
        this( a_assertion, "ASSERTION" );
    }


    /**
     * Creates an AssertionNode using an arbitrary candidate assertion with a
     * descriptions used for filter AST walker dumps.
     * 
     * @param a_assertion
     *            the arbitrary selection logic.
     * @param a_desc
     *            the printout representation for filter prints.
     */
    public AssertionNode(Assertion a_assertion, String a_desc)
    {
        super( ASSERTION );
        m_desc = a_desc;
        m_assertion = a_assertion;

        /*
         * We never want this node to ever make it to the point of becoming a
         * candidate for use in an enumeration so we set the scan count to the
         * maximum value.
         */
        set( "count", MAX );
    }


    /**
     * Gets the Assertion used by this assertion node.
     * 
     * @return the assertion used by this node
     */
    public Assertion getAssertion()
    {
        return m_assertion;
    }


    // ------------------------------------------------------------------------
    // A B S T R A C T M E T H O D I M P L E M E N T A T I O N S
    // ------------------------------------------------------------------------

    /**
     * Always returns true since an AssertionNode has no children.
     * 
     * @see org.apache.directory.shared.ldap.filter.ExprNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return true;
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#printToBuffer(java.lang.StringBuffer)
     */
    public StringBuffer printToBuffer( StringBuffer a_buf )
    {
        return a_buf.append( m_desc ).append( MAX_STR );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     */
    public void accept( FilterVisitor a_visitor )
    {
        a_visitor.visit( this );
    }
}
