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

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.directory.server.core.ServerUtils;
import org.apache.directory.server.core.event.Evaluator;
import org.apache.directory.server.core.partition.DirectoryPartitionNexusProxy;
import org.apache.directory.server.core.schema.AttributeTypeRegistry;
import org.apache.directory.server.core.schema.OidRegistry;
import org.apache.directory.server.core.subtree.RefinementEvaluator;
import org.apache.directory.shared.ldap.aci.ACITuple;
import org.apache.directory.shared.ldap.aci.AuthenticationLevel;
import org.apache.directory.shared.ldap.aci.ProtectedItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.MaxValueCountItem;
import org.apache.directory.shared.ldap.aci.ProtectedItem.RestrictedByItem;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.util.AttributeUtils;


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
    private final OidRegistry oidRegistry;
    private final AttributeTypeRegistry attrRegistry;


    public RelatedProtectedItemFilter( RefinementEvaluator refinementEvaluator, Evaluator entryEvaluator, 
        OidRegistry oidRegistry, AttributeTypeRegistry attrRegistry )
    {
        this.refinementEvaluator = refinementEvaluator;
        this.entryEvaluator = entryEvaluator;
        this.oidRegistry = oidRegistry;
        this.attrRegistry = attrRegistry;
    }


    public Collection filter( Collection tuples, OperationScope scope, DirectoryPartitionNexusProxy proxy,
                              Collection userGroupNames, LdapDN userName, Attributes userEntry,
                              AuthenticationLevel authenticationLevel, LdapDN entryName, String attrId,
                              Object attrValue, Attributes entry, Collection microOperations )
        throws NamingException
    {
        if ( tuples.size() == 0 )
        {
            return tuples;
        }

        for ( Iterator i = tuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if ( !isRelated( tuple, scope, userName, entryName, attrId, attrValue, entry ) )
            {
                i.remove();
            }
        }

        return tuples;
    }


    private boolean isRelated( ACITuple tuple, OperationScope scope, LdapDN userName, LdapDN entryName, String attrId,
                               Object attrValue, Attributes entry ) throws NamingException, InternalError
    {
        String oid = null;
        if ( attrId != null )
        {
            oid = oidRegistry.getOid( attrId );
        }
        
        for ( Iterator i = tuple.getProtectedItems().iterator(); i.hasNext(); )
        {
            ProtectedItem item = ( ProtectedItem ) i.next();
            if ( item == ProtectedItem.ENTRY )
            {
                if ( scope == OperationScope.ENTRY )
                {
                    return true;
                }
            }
            else if ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                return true;
            }
            else if ( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                return true;
            }
            else if ( item instanceof ProtectedItem.AllAttributeValues )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE && scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AllAttributeValues aav = ( ProtectedItem.AllAttributeValues ) item;
                for ( Iterator j = aav.iterator(); j.hasNext(); )
                {
                    if ( oid.equals( oidRegistry.getOid( ( String ) j.next() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.AttributeType )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.AttributeType at = ( ProtectedItem.AttributeType ) item;
                for ( Iterator j = at.iterator(); j.hasNext(); )
                {
                    if ( oid.equals( oidRegistry.getOid( ( String ) j.next() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.AttributeValue )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.AttributeValue av = ( ProtectedItem.AttributeValue ) item;
                for ( Iterator j = av.iterator(); j.hasNext(); )
                {
                    Attribute attr = ( Attribute ) j.next();
                    String attrOid = oidRegistry.getOid( attr.getID() );
                    AttributeType attrType = attrRegistry.lookup( attrOid );
                    
                    if ( oid.equals( attrOid ) && AttributeUtils.containsValue( attr, attrValue, attrType ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.Classes )
            {
                ProtectedItem.Classes c = ( ProtectedItem.Classes ) item;
                if ( refinementEvaluator.evaluate( c.getClasses(), entry.get( "objectClass" ) ) )
                {
                    return true;
                }
            }
            else if ( item instanceof ProtectedItem.MaxImmSub )
            {
                return true;
            }
            else if ( item instanceof ProtectedItem.MaxValueCount )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.MaxValueCount mvc = ( ProtectedItem.MaxValueCount ) item;
                for ( Iterator j = mvc.iterator(); j.hasNext(); )
                {
                    MaxValueCountItem mvcItem = ( MaxValueCountItem ) j.next();
                    if ( oid.equals( oidRegistry.getOid( mvcItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.RangeOfValues )
            {
                ProtectedItem.RangeOfValues rov = ( ProtectedItem.RangeOfValues ) item;
                if ( entryEvaluator.evaluate( rov.getFilter(), entryName.toString(), entry ) )
                {
                    return true;
                }
            }
            else if ( item instanceof ProtectedItem.RestrictedBy )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE )
                {
                    continue;
                }

                ProtectedItem.RestrictedBy rb = ( ProtectedItem.RestrictedBy ) item;
                for ( Iterator j = rb.iterator(); j.hasNext(); )
                {
                    RestrictedByItem rbItem = ( RestrictedByItem ) j.next();
                    if ( oid.equals( oidRegistry.getOid( rbItem.getAttributeType() ) ) )
                    {
                        return true;
                    }
                }
            }
            else if ( item instanceof ProtectedItem.SelfValue )
            {
                if ( scope != OperationScope.ATTRIBUTE_TYPE_AND_VALUE && scope != OperationScope.ATTRIBUTE_TYPE )
                {
                    continue;
                }

                ProtectedItem.SelfValue sv = ( ProtectedItem.SelfValue ) item;
                for ( Iterator j = sv.iterator(); j.hasNext(); )
                {
                    String svItem = String.valueOf( j.next() );
                    if ( oid.equals( oidRegistry.getOid( svItem ) ) )
                    {
                        AttributeType attrType = attrRegistry.lookup( oid );
                        Attribute attr = ServerUtils.getAttribute( attrType, entry );
                        if ( attr != null && ( ( attr.contains( userName.toNormName() ) || attr.contains( userName.toUpName() ) ) ) )
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
}
