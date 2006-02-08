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
package org.apache.directory.shared.ldap.filter;


import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.io.IOException;
import java.text.ParseException;


/**
 * Visitor which traverses a filter tree while normalizing the branch node
 * order.  Filter expressions can change the order of expressions in branch
 * nodes without effecting the logical meaning of the expression.  This visitor
 * orders the children of expression tree branch nodes consistantly.  It is
 * really useful for comparing expression trees which may be altered for
 * performance or altered because of codec idiosyncracies: for example the
 * SNACC4J codec uses a hashmap to store expressions in a sequence which
 * rearranges the order of children based on object hashcodes.  We need this
 * visitor to remove such inconsitancies in order hence normalizing the branch
 * node's child order.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory
 *         Project</a>
 * @version $Rev$
 */
public class BranchNormalizedVisitor implements FilterVisitor
{
    public void visit( ExprNode node )
    {
        if ( ! ( node instanceof BranchNode ) )
        {
            return;
        }

        BranchNode branch = ( BranchNode ) node;

        if ( branch.getOperator() == AbstractExprNode.NOT )
        {
            return;
        }

        Comparator nodeComparator = new NodeComparator();

        TreeSet set = new TreeSet( nodeComparator ) ;

        ArrayList children = branch.getChildren();

        for( int ii = 0; ii < children.size(); ii++ )
        {
            ExprNode child = ( ExprNode ) children.get( ii );

            if ( ! child.isLeaf() )
            {
                visit( child );
            }

            set.add( child ) ;
        }

        children.clear();

        children.addAll( set ) ;
    }


    public boolean canVisit( ExprNode node )
    {
        if ( node instanceof BranchNode )
        {
            return true;
        }

        return false;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public ArrayList getOrder( BranchNode node, ArrayList children )
    {
        return children;
    }


    /**
     * Normalizes a filter expression to a canonical representation while
     * retaining logical meaning of the expression.
     *
     * @param filter the filter to normalize
     * @return the normalized version of the filter
     * @throws java.io.IOException if filter parser cannot be created
     * @throws java.text.ParseException if the filter is malformed
     */
    public static String getNormalizedFilter( String filter ) throws IOException, ParseException
    {
        FilterParserImpl parser = new FilterParserImpl();

        ExprNode originalNode = parser.parse( filter );

        return getNormalizedFilter( originalNode );
    }


    /**
     * Normalizes a filter expression to a canonical representation while
     * retaining logical meaning of the expression.
     *
     * @param filter the filter to normalize
     * @return the normalized String version of the filter
     */
    public static String getNormalizedFilter( ExprNode filter )
    {
        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        visitor.visit( filter );

        StringBuffer normalized = new StringBuffer();

        filter.printToBuffer( normalized );

        return normalized.toString().trim();
    }


    class NodeComparator implements Comparator
    {
        public int compare( Object o1, Object o2 )
        {
            StringBuffer buf = new StringBuffer();

            ExprNode n1 = ( ExprNode ) o1;

            ExprNode n2 = ( ExprNode ) o2;

            buf.setLength( 0 );

            String s1 = null;

            n1.printToBuffer( buf );

            s1 = buf.toString();

            buf.setLength( 0 );

            String s2 = null;

            n2.printToBuffer( buf );

            s2 = buf.toString();

            return s1.compareTo( s2 );
        }
    }
}
