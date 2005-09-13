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
package org.apache.ldap.server.normalization;


import org.apache.ldap.common.filter.FilterVisitor;
import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.BranchNode;
import org.apache.ldap.common.filter.SimpleNode;
import org.apache.ldap.common.name.NameComponentNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import java.util.ArrayList;


/**
 * A filter visitor which normalizes leaf node values as it visits them.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ValueNormalizingVisitor implements FilterVisitor
{
    private final static Logger log = LoggerFactory.getLogger( ValueNormalizingVisitor.class );
    private final NameComponentNormalizer ncn;


    public ValueNormalizingVisitor( NameComponentNormalizer ncn )
    {
        this.ncn = ncn;
    }


    public void visit( ExprNode node )
    {
        if ( node instanceof SimpleNode )
        {
            SimpleNode snode = ( SimpleNode ) node;
            String normalized = null;

            try
            {
                if ( Character.isDigit( snode.getAttribute().charAt( 0 ) ) )
                {
                    normalized = ncn.normalizeByOid( snode.getAttribute(), snode.getValue() );
                }
                else
                {
                    normalized = ncn.normalizeByName( snode.getAttribute(), snode.getValue() );
                }
            }
            catch ( NamingException e )
            {
                log.error( "Failed to normalize filter value: " + e.getMessage(), e );
                throw new RuntimeException( e.getMessage() );
            }

            snode.setValue( normalized );
            return;
        }

        BranchNode bnode = ( BranchNode ) node;
        final int size = bnode.getChildren().size();
        for ( int ii = 0; ii < size ; ii++ )
        {
            visit( ( ExprNode ) bnode.getChildren().get( ii ) );
        }
    }


    public boolean canVisit( ExprNode node )
    {
        return node instanceof BranchNode || node instanceof SimpleNode;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public ArrayList getOrder( BranchNode node, ArrayList children )
    {
        return children;
    }
}
