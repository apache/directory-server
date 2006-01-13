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
package org.apache.ldap.server.authz.support;

import java.util.Collection;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.aci.ProtectedItem.MaxValueCountItem;
import org.apache.ldap.common.aci.ProtectedItem.RestrictedByItem;
import org.apache.ldap.server.event.Evaluator;
import org.apache.ldap.server.subtree.RefinementEvaluator;
import org.apache.ldap.server.partition.DirectoryPartitionNexusProxy;


/**
 * An {@link ACITupleFilter} that discards all tuples whose {@link ProtectedItem}s
 * are not related with the operation. (18.8.3.2, X.501)
 *
 * @author The Apache Directory Project
 * @version $Rev$, $Date$
 */
public class RelatedProtectedItemFilter implements ACITupleFilter
{
    private final RefinementEvaluator refinementEvaluator;
    private final Evaluator entryEvaluator;

    public RelatedProtectedItemFilter(
            RefinementEvaluator refinementEvaluator, Evaluator entryEvaluator )
    {
        this.refinementEvaluator = refinementEvaluator;
        this.entryEvaluator = entryEvaluator;
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
            if( !isRelated( tuple, scope, userName, entryName, attrId, attrValue, entry ) )
            {
                i.remove();
            }
        }

        return tuples;
    }

    private boolean isRelated( ACITuple tuple, OperationScope scope, Name userName, Name entryName, String attrId, Object attrValue, Attributes entry ) throws NamingException, InternalError
    {
        for( Iterator i = tuple.getProtectedItems().iterator(); i.hasNext(); )
        {
            ProtectedItem item = ( ProtectedItem ) i.next();
            if( item == ProtectedItem.ENTRY )
            {
                if( scope == OperationScope.ENTRY )
                {
                    return true;
                }
            }
            else if( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE &&
                    scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                if( isUserAttribute( attrId ) )
                {
                    return true;
                }
            }
            else if( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE &&
                    scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                if( isUserAttribute( attrId ) )
                {
                    return true;
                }
            }
            else if( item instanceof ProtectedItem.AllAttributeValues )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE &&
                    scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AllAttributeValues aav = ( ProtectedItem.AllAttributeValues ) item;
                for( Iterator j = aav.iterator(); j.hasNext(); )
                {
                    if( attrId.equalsIgnoreCase( ( String ) j.next() ) )
                    {
                        return true;
                    }
                }
            }
            else if( item instanceof ProtectedItem.AttributeType )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.AttributeType at = ( ProtectedItem.AttributeType ) item;
                for( Iterator j = at.iterator(); j.hasNext(); )
                {
                    if( attrId.equalsIgnoreCase( ( String ) j.next() ) )
                    {
                        return true;
                    }
                }
            }
            else if( item instanceof ProtectedItem.AttributeValue )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AttributeValue av = ( ProtectedItem.AttributeValue ) item;
                for( Iterator j = av.iterator(); j.hasNext(); )
                {
                    Attribute attr = ( Attribute ) j.next();
                    if( attrId.equalsIgnoreCase( attr.getID() ) &&
                            attr.contains( attrValue ) )
                    {
                        return true;
                    }
                }
            }
            else if( item instanceof ProtectedItem.Classes )
            {
                ProtectedItem.Classes c = ( ProtectedItem.Classes ) item;
                if( refinementEvaluator.evaluate(
                        c.getClasses(), entry.get( "objectClass" ) ) )
                {
                    return true;
                }
            }
            else if( item instanceof ProtectedItem.MaxImmSub )
            {
                return true;
            }
            else if( item instanceof ProtectedItem.MaxValueCount )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.MaxValueCount mvc = ( ProtectedItem.MaxValueCount ) item;
                for( Iterator j = mvc.iterator(); j.hasNext(); )
                {
                    MaxValueCountItem mvcItem = ( MaxValueCountItem ) j.next();
                    if( attrId.equalsIgnoreCase( mvcItem.getAttributeType() ) )
                    {
                        return true;
                    }
                }
            }
            else if( item instanceof ProtectedItem.RangeOfValues )
            {
                ProtectedItem.RangeOfValues rov = ( ProtectedItem.RangeOfValues ) item;
                if( entryEvaluator.evaluate( rov.getFilter(), entryName.toString(), entry ) )
                {
                    return true;
                }
            }
            else if( item instanceof ProtectedItem.RestrictedBy )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.RestrictedBy rb = ( ProtectedItem.RestrictedBy ) item;
                for( Iterator j = rb.iterator(); j.hasNext(); )
                {
                    RestrictedByItem rbItem = ( RestrictedByItem ) j.next();
                    if( attrId.equalsIgnoreCase( rbItem.getAttributeType() ) )
                    {
                        return true;
                    }
                }
            }
            else if( item instanceof ProtectedItem.SelfValue )
            {
                if( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE &&
                    scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.SelfValue sv = ( ProtectedItem.SelfValue ) item;
                for( Iterator j = sv.iterator(); j.hasNext(); )
                {
                    String svItem = String.valueOf( j.next() );
                    if( svItem.equalsIgnoreCase( attrId ) )
                    {
                        Attribute attr = entry.get( attrId );
                        if( attr != null && ( attr.contains( userName ) || attr.contains( userName.toString() ) ) )
                        {
                            return true;
                        }
                    }
                }
            }
            else
            {
                throw new InternalError( "Unexpected protectedItem: " + item.getClass().getName() );
            }
        }

        return false;
    }

    private final boolean isUserAttribute( String attrId )
    {
        /* Not used anymore.  Just retaining in case of resurrection. */
        return true;

        /*
        try
        {
            AttributeType type = attrTypeRegistry.lookup( attrId );
            if( type != null && type.isCanUserModify() )
            {
                return true;
            }
        }
        catch( NamingException e )
        {
            // Ignore
        }

        return false;
        */
    }
}
