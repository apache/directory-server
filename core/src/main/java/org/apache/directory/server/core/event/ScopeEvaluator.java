/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.server.core.event;


import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.ScopeNode;
import org.apache.directory.shared.ldap.name.Dn;


/**
 * Evaluates ScopeNode assertions on candidates.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class ScopeEvaluator implements Evaluator
{
    public ScopeEvaluator()
    {
    }


    /**
     * {@inheritDoc}
     */
    public boolean evaluate( ExprNode node, Dn dn, Entry record ) throws LdapException
    {
        ScopeNode snode = ( ScopeNode ) node;

        switch ( snode.getScope() )
        {
            case OBJECT:
                return dn.equals( snode.getBaseDn() );
            
            case ONELEVEL:
                if ( dn.isChildOf( snode.getBaseDn() ) )
                {
                    return ( snode.getBaseDn().size() + 1 ) == dn.size();
                }
            
            case SUBTREE:
                return dn.isChildOf( snode.getBaseDn() );
            
            default:
                throw new LdapInvalidSearchFilterException( I18n.err( I18n.ERR_247 ) );
        }
    }
}
