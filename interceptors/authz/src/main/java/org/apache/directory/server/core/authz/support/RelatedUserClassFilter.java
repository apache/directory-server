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
package org.apache.directory.server.core.authz.support;


import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.directory.api.ldap.aci.ACITuple;
import org.apache.directory.api.ldap.aci.UserClass;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.server.core.api.subtree.SubtreeEvaluator;
import org.apache.directory.server.i18n.I18n;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link UserClass}es
 * are not related with the current user. (18.8.3.1, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class RelatedUserClassFilter implements ACITupleFilter
{
    private final SubtreeEvaluator subtreeEvaluator;


    public RelatedUserClassFilter( SubtreeEvaluator subtreeEvaluator )
    {
        this.subtreeEvaluator = subtreeEvaluator;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<ACITuple> filter( AciContext aciContext, OperationScope scope, Entry userEntry )
        throws LdapException
    {
        if ( aciContext.getAciTuples().isEmpty() )
        {
            return aciContext.getAciTuples();
        }

        for ( Iterator<ACITuple> ii = aciContext.getAciTuples().iterator(); ii.hasNext(); )
        {
            ACITuple tuple = ii.next();

            if ( tuple.isGrant() )
            {
                if ( !isRelated( aciContext.getUserGroupNames(),
                    aciContext.getUserDn(),
                    userEntry,
                    aciContext.getEntryDn(),
                    tuple.getUserClasses() )
                    || aciContext.getAuthenticationLevel().compareTo( tuple.getAuthenticationLevel() ) < 0 )
                {
                    ii.remove();
                }
            }
            else
            // Denials
            {
                if ( !isRelated( aciContext.getUserGroupNames(),
                    aciContext.getUserDn(),
                    userEntry,
                    aciContext.getEntryDn(),
                    tuple.getUserClasses() )
                    && aciContext.getAuthenticationLevel().compareTo( tuple.getAuthenticationLevel() ) >= 0 )
                {
                    ii.remove();
                }
            }
        }

        return aciContext.getAciTuples();
    }


    private boolean isRelated( Collection<String> userGroupNames, Dn userName, Entry userEntry,
        Dn entryName, Collection<UserClass> userClasses ) throws LdapException
    {
        for ( UserClass userClass : userClasses )
        {
            if ( userClass == UserClass.ALL_USERS )
            {
                return true;
            }
            else if ( userClass == UserClass.THIS_ENTRY )
            {
                if ( userName.equals( entryName ) )
                {
                    return true;
                }
            }
            else if ( userClass == UserClass.PARENT_OF_ENTRY )
            {
                if ( entryName.isDescendantOf( userName ) )
                {
                    return true;
                }
            }
            else if ( userClass instanceof UserClass.Name )
            {
                UserClass.Name nameUserClass = ( UserClass.Name ) userClass;
                
                if ( ( userName != null ) && nameUserClass.getNames().contains( userName.getNormName() ) )
                {
                    return true;
                }
            }
            else if ( userClass instanceof UserClass.UserGroup )
            {
                UserClass.UserGroup userGroupUserClass = ( UserClass.UserGroup ) userClass;

                for ( String userGroupName : userGroupNames )
                {
                    Set<String> dns = userGroupUserClass.getNames();

                    if ( userGroupName != null )
                    {
                        for ( String dn : dns )
                        {
                            if ( userGroupName.equals( dn ) )
                            {
                                return true;
                            }
                        }
                    }
                }
            }
            else if ( userClass instanceof UserClass.Subtree )
            {
                UserClass.Subtree subtree = ( UserClass.Subtree ) userClass;
                if ( matchUserClassSubtree( userName, userEntry, subtree ) )
                {
                    return true;
                }
            }
            else
            {
                throw new InternalError( I18n.err( I18n.ERR_233, userClass.getClass().getName() ) );
            }
        }

        return false;
    }


    private boolean matchUserClassSubtree( Dn userName, Entry userEntry, UserClass.Subtree subtree )
        throws LdapException
    {
        for ( SubtreeSpecification subtreeSpec : subtree.getSubtreeSpecifications() )
        {
            if ( subtreeEvaluator.evaluate( subtreeSpec, Dn.ROOT_DSE, userName, userEntry ) )
            {
                return true;
            }
        }

        return false;
    }
}
