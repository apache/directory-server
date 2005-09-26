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
package org.apache.ldap.server.authz;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.ldap.common.aci.ACIItem;
import org.apache.ldap.common.aci.ACITuple;
import org.apache.ldap.common.aci.AuthenticationLevel;
import org.apache.ldap.common.aci.MicroOperation;
import org.apache.ldap.common.aci.ProtectedItem;
import org.apache.ldap.common.aci.UserClass;
import org.apache.ldap.common.aci.ProtectedItem.MaxValueCountItem;
import org.apache.ldap.common.aci.ProtectedItem.RestrictedByItem;
import org.apache.ldap.common.exception.LdapNoPermissionException;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.subtree.SubtreeSpecification;
import org.apache.ldap.server.event.Evaluator;
import org.apache.ldap.server.event.ExpressionEvaluator;
import org.apache.ldap.server.interceptor.NextInterceptor;
import org.apache.ldap.server.schema.AttributeTypeRegistry;
import org.apache.ldap.server.schema.OidRegistry;
import org.apache.ldap.server.subtree.RefinementEvaluator;
import org.apache.ldap.server.subtree.RefinementLeafEvaluator;
import org.apache.ldap.server.subtree.SubtreeEvaluator;

public class ACDFEngine
{
    private static final LdapName ROOTDSE_NAME = new LdapName();

    private final Evaluator entryEvaluator;
    private final SubtreeEvaluator subtreeEvaluator;
    private final RefinementEvaluator refinementEvaluator;
    
    public ACDFEngine( OidRegistry oidRegistry, AttributeTypeRegistry attrTypeRegistry ) throws NamingException
    {
        entryEvaluator = new ExpressionEvaluator( oidRegistry, attrTypeRegistry );
        subtreeEvaluator = new SubtreeEvaluator( oidRegistry );
        refinementEvaluator = new RefinementEvaluator(
                new RefinementLeafEvaluator( oidRegistry ) );
    }
    
    /**
     * Checks the user with the specified name can access the specified resource
     * (entry, attribute type, or attribute value) and throws {@link LdapNoPermissionException}
     * if the user doesn't have any permission to perform the specified grants.
     * 
     * @param next the next interceptor to the current interceptor
     * @param userGroupName the DN of the group of the user who is trying to access the resource
     * @param username the DN of the user who is trying to access the resource
     * @param entryName the DN of the entry the user is trying to access 
     * @param attrId the attribute type of the attribute the user is trying to access.
     *               <tt>null</tt> if the user is not accessing a specific attribute type.
     * @param attrValue the attribute value of the attribute the user is trying to access.
     *                  <tt>null</tt> if the user is not accessing a specific attribute value.
     * @param microOperations the {@link MicroOperation}s to perform
     * @param aciTuples {@link ACITuple}s translated from {@link ACIItem}s in the subtree entries
     * @throws NamingException if failed to evaluate ACI items
     */
    public void checkPermission(
            NextInterceptor next,
            Name userGroupName, Name username, AuthenticationLevel authenticationLevel,
            Name entryName, String attrId, Object attrValue,
            Collection microOperations, Collection aciTuples ) throws NamingException 
    {
        if( !hasPermission(
                next,
                userGroupName, username, authenticationLevel,
                entryName, attrId, attrValue,
                microOperations, aciTuples ) )
        {
            throw new LdapNoPermissionException();
        }
    }
    
    /**
     * Returns <tt>true</tt> if the user with the specified name can access the specified resource
     * (entry, attribute type, or attribute value) and throws {@link LdapNoPermissionException}
     * if the user doesn't have any permission to perform the specified grants.
     * 
     * @param next the next interceptor to the current interceptor 
     * @param userGroupName the DN of the group of the user who is trying to access the resource
     * @param userName the DN of the user who is trying to access the resource
     * @param entryName the DN of the entry the user is trying to access 
     * @param attrId the attribute type of the attribute the user is trying to access.
     *               <tt>null</tt> if the user is not accessing a specific attribute type.
     * @param attrValue the attribute value of the attribute the user is trying to access.
     *                  <tt>null</tt> if the user is not accessing a specific attribute value.
     * @param microOperations the {@link MicroOperation}s to perform
     * @param aciTuples {@link ACITuple}s translated from {@link ACIItem}s in the subtree entries
     */
    public boolean hasPermission(
            NextInterceptor next, 
            Name userGroupName, Name userName, AuthenticationLevel authenticationLevel,
            Name entryName, String attrId, Object attrValue,
            Collection microOperations, Collection aciTuples ) throws NamingException
    {
        Attributes userEntry = next.lookup( userName );
        Attributes entry = next.lookup( entryName );

        aciTuples = removeTuplesWithoutRelatedUserClasses(
                userGroupName, userName, userEntry, authenticationLevel, entryName, aciTuples );
        aciTuples = removeTuplesWithoutRelatedProtectedItems( userName, entryName, attrId, attrValue, entry, aciTuples );
        
        // TODO Discard all tuples that include the maxValueCount, maxImmSub, restrictedBy which
        // grant access and which don't satisfy any of these constraints
        // We have to access the DIT here, but no way so far.  We need discussion here.
        
        aciTuples = removeTuplesWithoutRelatedMicroOperation( microOperations, aciTuples );
        aciTuples = getTuplesWithHighestPrecedence( aciTuples );
        
        aciTuples = getTuplesWithMostSpecificUserClasses( userName, userEntry, aciTuples );
        aciTuples = getTuplesWithMostSpecificProtectedItems( entryName, attrId, attrValue, entry, aciTuples );
        
        // Grant access if and only if one or more tuples remain and
        // all grant access. Otherwise deny access.
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( !tuple.isGrant() )
            {
                return false;
            }
        }
        return true;
    }
    
    private Collection removeTuplesWithoutRelatedUserClasses(
            Name userGroupName, Name userName, Attributes userEntry,
            AuthenticationLevel authenticationLevel,
            Name entryName, Collection aciTuples ) throws NamingException
    {
        Collection filteredTuples = new ArrayList( aciTuples );
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( tuple.isGrant() )
            {
                if( !matchUserClass( userGroupName, userName, userEntry, entryName, tuple.getUserClasses() ) ||
                        authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) < 0 )
                {
                    i.remove();
                }
            }
            else // Denials
            {
                if( !matchUserClass( userGroupName, userName, userEntry, entryName, tuple.getUserClasses() ) &&
                        authenticationLevel.compareTo( tuple.getAuthenticationLevel() ) >= 0 )
                {
                    i.remove();
                }
            }
        }
        
        return filteredTuples;
    }
    
    private Collection removeTuplesWithoutRelatedProtectedItems(
            Name userName,
            Name entryName, String attrId, Object attrValue, Attributes entry,
            Collection aciTuples ) throws NamingException
    {
        Collection filteredTuples = new ArrayList();
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( matchProtectedItem(
                    userName, entryName, attrId, attrValue, entry, tuple.getProtectedItems() ) )
            {
                filteredTuples.add( tuple );
            }
        }
        
        return filteredTuples;
    }
    
    protected Collection removeTuplesWithoutRelatedMicroOperation(
            Collection microOperations, Collection aciTuples )
    {
        Collection filteredTuples = new ArrayList();

        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            boolean retain = false;
            for( Iterator j = microOperations.iterator(); j.hasNext(); )
            {
                MicroOperation microOp = ( MicroOperation ) j.next();
                if( tuple.getMicroOperations().contains( microOp ) )
                {
                    retain = true;
                    break;
                }
            }
            
            if( retain )
            {
                filteredTuples.add( tuple );
            }
        }
        
        return filteredTuples;
    }
    
    private Collection getTuplesWithHighestPrecedence( Collection aciTuple )
    {
        Collection filteredTuples = new ArrayList();
        
        int maxPrecedence = -1;
        for( Iterator i = aciTuple.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( tuple.getPrecedence() > maxPrecedence ) 
            {
                maxPrecedence = tuple.getPrecedence();
            }
        }
        
        for( Iterator i = aciTuple.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            if( tuple.getPrecedence() == maxPrecedence ) 
            {
                filteredTuples.add( tuple );
            }            
        }
        
        return filteredTuples;
    }
    
    private Collection getTuplesWithMostSpecificUserClasses( Name userName, Attributes userEntry, Collection aciTuples ) throws NamingException
    {
        if( aciTuples.size() <= 1 )
        {
            return aciTuples;
        }

        Collection filteredTuples = new ArrayList();
        
        // If there are any tuples matching the requestor with UserClasses
        // element name or thisEntry, discard all other tuples.
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
                if( userClass instanceof UserClass.Name ||
                        userClass instanceof UserClass.ThisEntry )
                {
                    filteredTuples.add( tuple );
                    break;
                }
            }
        }
        
        if( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }
        
        // Otherwise if there are any tuples matching UserGroup,
        // discard all other tuples.
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
                if( userClass instanceof UserClass.UserGroup )
                {
                    filteredTuples.add( tuple );
                    break;
                }
            }
        }
        
        if( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }

        // Otherwise if there are any tuples matching subtree,
        // discard all other tuples.
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for( Iterator j = tuple.getUserClasses().iterator(); j.hasNext(); )
            {
                UserClass userClass = ( UserClass ) j.next();
                if( userClass instanceof UserClass.Subtree )
                {
                    UserClass.Subtree subtree = ( UserClass.Subtree ) userClass;
                    if( matchUserClassSubtree( userName, userEntry, subtree ) )
                    {
                        filteredTuples.add( tuple );
                        break;
                    }
                }
            }
        }
        
        if( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }
        
        return aciTuples;
    }

    private boolean matchUserClassSubtree( Name userName, Attributes userEntry, UserClass.Subtree subtree ) throws NamingException
    {
        for( Iterator k = subtree.getSubtreeSpecifications().iterator();
             k.hasNext(); )
        {
            SubtreeSpecification subtreeSpec = ( SubtreeSpecification ) k.next();
            if( subtreeEvaluator.evaluate(
                    subtreeSpec, ROOTDSE_NAME, userName, userEntry.get( "userClass" ) ) )
            {
                return true;
            }
        }
        
        return false;
    }
    
    private Collection getTuplesWithMostSpecificProtectedItems( Name entryName, String attrId, Object attrValue, Attributes entry, Collection aciTuples ) throws NamingException
    {
        if( aciTuples.size() <= 1 )
        {
            return aciTuples;
        }

        Collection filteredTuples = new ArrayList();
        
        // If the protected item is an attribute and there are tuples that
        // specify the attribute type explicitly, discard all other tuples.
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            itemLoop: for( Iterator j = tuple.getProtectedItems().iterator(); j.hasNext(); )
            {
                ProtectedItem item = ( ProtectedItem ) j.next();
                if( item instanceof ProtectedItem.AttributeType )
                {
                    if( contains( attrId, ( ( ProtectedItem.AttributeType ) item ).iterator() ) )
                    {
                        filteredTuples.add( tuple );
                        break;
                    }
                }
                else if( item instanceof ProtectedItem.AllAttributeValues )
                {
                    if( contains( attrId, ( ( ProtectedItem.AllAttributeValues ) item ).iterator() ) )
                    {
                        filteredTuples.add( tuple );
                        break;
                    }
                }
                else if( item instanceof ProtectedItem.SelfValue )
                {
                    if( contains( attrId, ( ( ProtectedItem.SelfValue ) item ).iterator() ) )
                    {
                        filteredTuples.add( tuple );
                        break;
                    }
                }
                else if( item instanceof ProtectedItem.AttributeValue )
                {
                    if( attrId == null || attrValue == null )
                    {
                        continue;
                    }

                    ProtectedItem.AttributeValue av = ( ProtectedItem.AttributeValue ) item;
                    for( Iterator k = av.iterator(); k.hasNext(); )
                    {
                        Attribute attr = ( Attribute ) k.next();
                        if( attr.getID().equalsIgnoreCase( attrId ) &&
                                attr.contains( attrValue ) )
                        {
                            filteredTuples.add( tuple );
                            break itemLoop;
                        }
                    }
                }
            }
        }
        
        if( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }

        // If the protected item is an attribute value, and there are tuples
        // that specify the attribute value explicitly, discard all other tuples.
        // A protected item which is a rangeOfValues is to be treated as
        // specifying an attribute value explicitly. 
        for( Iterator i = aciTuples.iterator(); i.hasNext(); )
        {
            ACITuple tuple = ( ACITuple ) i.next();
            for( Iterator j = tuple.getProtectedItems().iterator(); j.hasNext(); )
            {
                ProtectedItem item = ( ProtectedItem ) j.next();
                if( item instanceof ProtectedItem.RangeOfValues )
                {
                    ProtectedItem.RangeOfValues rov = ( ProtectedItem.RangeOfValues ) item;
                    if( entryEvaluator.evaluate( rov.getFilter(), entryName.toString(), entry ) )
                    {
                        filteredTuples.add( tuple );
                        break;
                    }
                }
            }
        }

        if( filteredTuples.size() > 0 )
        {
            return filteredTuples;
        }

        return aciTuples;
    }
    

    private boolean matchUserClass( Name userGroupName, Name userName, Attributes userEntry, Name entryName, Collection userClasses ) throws NamingException
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
                if( userGroupName != null && userGroupUserClass.getNames().contains( userGroupName ) )
                {
                    return true;
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
    
    private boolean matchProtectedItem(
            Name userName,
            Name entryName, String attrId, Object attrValue, Attributes entry,
            Collection protectedItems ) throws NamingException
    {
        for( Iterator i = protectedItems.iterator(); i.hasNext(); )
        {
            ProtectedItem item = ( ProtectedItem ) i.next();
            if( item == ProtectedItem.ENTRY )
            {
                if( attrId == null )
                {
                    return true;
                }
            }
            else if( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES )
            {
                if( attrId != null )
                {
                    return true;
                }
            }
            else if( item == ProtectedItem.ALL_USER_ATTRIBUTE_TYPES_AND_VALUES )
            {
                if( attrId != null && attrValue != null )
                {
                    return true;
                }
            }
            else if( item instanceof ProtectedItem.AllAttributeValues )
            {
                if( attrId == null )
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
                if( attrId == null )
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
                if( attrId == null || attrValue == null )
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
                ProtectedItem.MaxImmSub mis = ( ProtectedItem.MaxImmSub ) item;
                if( attrId == null )
                {
                    return true;
                }
            }
            else if( item instanceof ProtectedItem.MaxValueCount )
            {
                if( attrId == null )
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
                if( attrId == null )
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
                if( attrId == null || attrValue == null )
                {
                    continue;
                }
                
                ProtectedItem.SelfValue sv = ( ProtectedItem.SelfValue ) item;
                for( Iterator j = sv.iterator(); j.hasNext(); )
                {
                    Attribute attr = entry.get( String.valueOf( j.next() ) );
                    if( attr.contains( userName ) || attr.contains( userName.toString() ) )
                    {
                        return true;
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
    
    private static boolean contains( Object needle, Iterator haystack )
    {
        if( needle == null )
        {
            return false;
        }

        while( haystack.hasNext() )
        {
            if( haystack.next().equals( needle ) )
            {
                return true;
            }
        }
        
        return false;
    }
}
