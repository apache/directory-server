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
package org.apache.directory.server.core.subtree;


import org.apache.directory.shared.ldap.filter.BranchNode;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.SimpleNode;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import java.util.Iterator;


/**
 * The top level evaluation node for a refinement.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RefinementEvaluator
{
    /** Leaf Evaluator flyweight use for leaf filter assertions */
    private RefinementLeafEvaluator leafEvaluator;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    public RefinementEvaluator( RefinementLeafEvaluator leafEvaluator )
    {
        this.leafEvaluator = leafEvaluator;
    }


    public boolean evaluate( ExprNode node, Attribute objectClasses ) throws NamingException
    {
        if ( node == null )
        {
            throw new IllegalArgumentException( "node cannot be null" );
        }
        if ( objectClasses == null )
        {
            throw new IllegalArgumentException( "objectClasses cannot be null" );
        }
        if ( ! objectClasses.getID().equalsIgnoreCase( "objectClass" ) )
        {
            throw new IllegalArgumentException( "Attribute objectClasses should be of id 'objectClass'" );
        }
        if ( node.isLeaf() )
        {
            return leafEvaluator.evaluate( ( SimpleNode ) node, objectClasses );
        }

        BranchNode bnode = ( BranchNode ) node;

        switch( bnode.getOperator() )
        {
        case( BranchNode.OR ):
            Iterator children = bnode.getChildren().iterator();

            while ( children.hasNext() )
            {
                ExprNode child = ( ExprNode ) children.next();

                if ( evaluate( child, objectClasses ) )
                {
                    return true;
                }
            }

            return false;
        case( BranchNode.AND ):
            children = bnode.getChildren().iterator();
            while ( children.hasNext() )
            {
                ExprNode child = ( ExprNode ) children.next();

                if ( ! evaluate( child, objectClasses ) )
                {
                    return false;
                }
            }

            return true;
        case( BranchNode.NOT ):
            if ( null != bnode.getChild() )
            {
                return ! evaluate( bnode.getChild(), objectClasses );
            }

            throw new IllegalArgumentException( "Negation has no child: " + node );
        default:
            throw new IllegalArgumentException( "Unrecognized branch node operator: "
                + bnode.getOperator() );
        }
    }
}
