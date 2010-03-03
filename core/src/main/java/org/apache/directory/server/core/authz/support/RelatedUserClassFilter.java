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

import javax.naming.NamingException;

import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.subtree.SubtreeEvaluator;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.MicroOperation;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link UserClass}es
 * are not related with the current user. (18.8.3.1, X.501)
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class RelatedUserClassFilter implements ACITupleFilter
{
    private static final DN ROOTDSE_NAME = DN.EMPTY_DN;

    private final SubtreeEvaluator subtreeEvaluator;


    public RelatedUserClassFilter(SubtreeEvaluator subtreeEvaluator)
    {
        this.subtreeEvaluator = subtreeEvaluator;
    }


    public Collection<ACITuple> filter( 
            SchemaManager schemaManager, 
            Collection<ACITuple> tuples, 
            OperationScope scope, 
            OperationContext opContext,
            Collection<DN> userGroupNames, 
            DN userName, 
            ServerEntry userEntry, 
            AuthenticationLevel authenticationLevel,
            DN entryName, 
            String attrId, 
            Value<?> attrValue, 
            ServerEntry entry, 
            Collection<MicroOperation> microOperations,
            ServerEntry entryView )
        throws NamingException
    {
        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        for ( Iterator<ACITuple> ii = tuples.iterator(); ii.hasNext(); )
        {
            ACITuple tuple = ii.next();
            
            if ( tuple.isGrant() )
            {
                if ( !isRelated( userGroupNames, 
                                 userName, 
                                 userEntry, 
                                 entryName, 
                                 tuple.getUserClasses() )
                    || authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) < 0 )
                {
                    ii.remove();
                }
            }
            else
            // Denials
            {
                if ( !isRelated( userGroupNames, 
                                 userName, 
                                 userEntry, 
                                 entryName, 
                                 tuple.getUserClasses() )
                    && authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) >= 0 )
                {
                    ii.remove();
                }
            }
        }

        return tuples;
    }


    private boolean isRelated( Collection<DN> userGroupNames, DN userName, ServerEntry userEntry, 
        DN entryName, Collection<UserClass> userClasses ) throws NamingException
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
                if ( entryName.startsWith( userName ) )
                {
                    return true;
                }
            }
            else if ( userClass instanceof UserClass.Name )
            {
                UserClass.Name nameUserClass = ( UserClass.Name ) userClass;
                if ( nameUserClass.getNames().contains( userName ) )
                {
                    return true;
                }
            }
            else if ( userClass instanceof UserClass.UserGroup )
            {
                UserClass.UserGroup userGroupUserClass = ( UserClass.UserGroup ) userClass;
                
                for ( DN userGroupName : userGroupNames )
                {
                    if ( userGroupName != null && userGroupUserClass.getNames().contains( userGroupName ) )
                    {
                        return true;
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


    private boolean matchUserClassSubtree( DN userName, ServerEntry userEntry, UserClass.Subtree subtree )
        throws NamingException
    {
        for ( SubtreeSpecification subtreeSpec : subtree.getSubtreeSpecifications() )
        {
            if ( subtreeEvaluator.evaluate( subtreeSpec, ROOTDSE_NAME, userName, userEntry ) )
            {
                return true;
            }
        }

        return false;
    }
}
