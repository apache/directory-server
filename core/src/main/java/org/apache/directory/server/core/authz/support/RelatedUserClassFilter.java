/*
 *   @(#) $Id$
 *   
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
package org.apache.directory.server.core.authz.support;

import java.util.Collection;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.server.core.subtree.SubtreeEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.UserClass;
import org.apache.directory.shared.ldap.name.LdapName;
import org.apache.directory.shared.ldap.subtree.SubtreeSpecification;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link UserClass}es
 * are not related with the current user. (18.8.3.1, X.501)
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class RelatedUserClassFilter implements ACITupleFilter
{
    private static final LdapName ROOTDSE_NAME = LdapName.EMPTY_LDAP_NAME;

    private final SubtreeEvaluator subtreeEvaluator;

    public RelatedUserClassFilter( SubtreeEvaluator subtreeEvaluator )
    {
        this.subtreeEvaluator = subtreeEvaluator;
    }

    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy, Collection userGroupNames, Name userName, Attributes userEntry, AuthenticationLevel authenticationLevel, Name entryName, String attrId, Object attrValue, Attributes entry, Collection microOperations ) throws NamingException
    {
        if( tuples.size() == 0 )
        {
            return tuples;
        }

        for( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( tuple.isGrant() )
            {
                if( !isRelated( userGroupNames, userName, userEntry, entryName, tuple.getUserClasses() ) ||
                        authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) < 0 )
                {
                    i.remove();
                }
            }
            else // Denials
            {
                if( !isRelated( userGroupNames, userName, userEntry, entryName, tuple.getUserClasses() ) &&
                        authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) >= 0 )
                {
                    i.remove();
                }
            }
        }

        return tuples;
    }

    private boolean isRelated( Collection userGroupNames, Name userName, Attributes userEntry, Name entryName, Collection userClasses ) throws NamingException
    {
        for( Iterator i = userClasses.iterator(); i.hasNext(); )
        {
            UserClass userClass = ( UserClass ) i.next();
            if( userClass == UserClass.ALL_USERS )
            {
                return true;
            }
            else if( userClass == UserClass.THIS_ENTRY )
            {
                if( userName.equals( entryName ) )
                {
                    return true;
                }
            }
            else if( userClass instanceof UserClass.Name )
            {
                UserClass.Name nameUserClass = ( UserClass.Name ) userClass;
                if( nameUserClass.getNames().contains( userName ) )
                {
                    return true;
                }
            }
            else if( userClass instanceof UserClass.UserGroup )
            {
                UserClass.UserGroup userGroupUserClass = ( UserClass.UserGroup ) userClass;
                for( Iterator j = userGroupNames.iterator(); j.hasNext(); )
                {
                    Name userGroupName = ( Name ) j.next();
                    if( userGroupName != null && userGroupUserClass.getNames().contains( userGroupName ) )
                    {
                        return true;
                    }
                }
            }
            else if( userClass instanceof UserClass.Subtree )
            {
                UserClass.Subtree subtree = ( UserClass.Subtree ) userClass;
                if( matchUserClassSubtree( userName, userEntry, subtree ) )
                {
                    return true;
                }
            }
            else
            {
                throw new InternalError( "Unexpected userClass: " + userClass.getClass().getName() );
            }
        }

        return false;
    }

    private boolean matchUserClassSubtree( Name userName, Attributes userEntry, UserClass.Subtree subtree ) throws NamingException
    {
        for( Iterator i = subtree.getSubtreeSpecifications().iterator();
             i.hasNext(); )
        {
            SubtreeSpecification subtreeSpec = ( SubtreeSpecification ) i.next();
            if( subtreeEvaluator.evaluate(
                    subtreeSpec, ROOTDSE_NAME, userName, userEntry.get( "userClass" ) ) )
            {
                return true;
            }
        }

        return false;
    }
}
