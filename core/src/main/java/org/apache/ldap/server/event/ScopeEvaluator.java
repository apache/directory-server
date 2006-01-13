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
package org.apache.ldap.server.event;


import javax.naming.NamingException;
import javax.naming.Name;
import javax.naming.directory.SearchControls;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.filter.ExprNode;
import org.apache.ldap.common.filter.ScopeNode;
import org.apache.ldap.common.name.DnParser;


/**
 * Evaluates ScopeNode assertions on candidates using a database.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ScopeEvaluator implements Evaluator
{
    private DnParser parser = null;


    public ScopeEvaluator() throws NamingException
    {
        parser = new DnParser();
    }


    /**
     * @see Evaluator#evaluate(ExprNode, String, Attributes)
     */
    public boolean evaluate( ExprNode node, String dn, Attributes record )
        throws NamingException
    {
        ScopeNode snode = ( ScopeNode ) node;

        switch( snode.getScope() )
        {
        case( SearchControls.OBJECT_SCOPE ):
            return dn.equals( snode.getBaseDn() );
        case( SearchControls.ONELEVEL_SCOPE ):
            if ( dn.endsWith( snode.getBaseDn() ) )
            {
                Name candidateDn = parser.parse( dn );
                Name scopeDn = parser.parse( snode.getBaseDn() );
                return ( scopeDn.size() + 1 ) == candidateDn.size();
            }
        case( SearchControls.SUBTREE_SCOPE ):
            return dn.endsWith( snode.getBaseDn() );
        default:
            throw new NamingException( "Unrecognized search scope!" );
        }
    }
}
