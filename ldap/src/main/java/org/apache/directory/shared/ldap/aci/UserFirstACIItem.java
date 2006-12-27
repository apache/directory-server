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
package org.apache.directory.shared.ldap.aci;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;


/**
 * An {@link ACIItem} which specifies {@link UserClass}es first and then
 * {@link ProtectedItem}s each {@link UserClass} will have. (18.4.2.4. X.501)
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class UserFirstACIItem extends ACIItem
{
    private static final long serialVersionUID = 5587483838404246148L;

    private final Collection userClasses;

    private final Collection userPermissions;


    /**
     * Creates a new instance.
     * 
     * @param identificationTag
     *            the id string of this item
     * @param precedence
     *            the precedence of this item
     * @param authenticationLevel
     *            the level of authentication required to this item
     * @param userClasses
     *            the collection of {@link UserClass}es this item protects
     * @param userPermissions
     *            the collection of {@link UserPermission}s each
     *            <tt>protectedItems</tt> will have
     */
    public UserFirstACIItem(String identificationTag, int precedence, AuthenticationLevel authenticationLevel,
        Collection userClasses, Collection userPermissions)
    {
        super( identificationTag, precedence, authenticationLevel );

        for ( Iterator i = userClasses.iterator(); i.hasNext(); )
        {
            if ( !UserClass.class.isAssignableFrom( i.next().getClass() ) )
            {
                throw new IllegalArgumentException( "userClasses contains an element which is not a user class." );
            }
        }

        for ( Iterator i = userPermissions.iterator(); i.hasNext(); )
        {
            if ( !UserPermission.class.isAssignableFrom( i.next().getClass() ) )
            {
                throw new IllegalArgumentException(
                    "userPermissions contains an element which is not a user permission." );
            }
        }

        this.userClasses = Collections.unmodifiableCollection( new ArrayList( userClasses ) );
        this.userPermissions = Collections.unmodifiableCollection( new ArrayList( userPermissions ) );
    }


    /**
     * Returns the set of {@link UserClass}es.
     */
    public Collection getUserClasses()
    {
        return userClasses;
    }


    /**
     * Returns the set of {@link UserPermission}s.
     */
    public Collection getUserPermission()
    {
        return userPermissions;
    }


    public String toString()
    {
        return "userFirstACIItem: " + "identificationTag=" + getIdentificationTag() + ", " + "precedence="
            + getPrecedence() + ", " + "authenticationLevel=" + getAuthenticationLevel() + ", " + "userClasses="
            + userClasses + ", " + "userPermissions=" + userPermissions;
    }


    public Collection toTuples()
    {
        Collection tuples = new ArrayList();
        for ( Iterator i = userPermissions.iterator(); i.hasNext(); )
        {
            UserPermission userPermission = ( UserPermission ) i.next();
            Set grants = userPermission.getGrants();
            Set denials = userPermission.getDenials();
            int precedence = userPermission.getPrecedence() >= 0 ? userPermission.getPrecedence() : this
                .getPrecedence();

            if ( grants.size() > 0 )
            {
                tuples.add( new ACITuple( getUserClasses(), getAuthenticationLevel(), userPermission
                    .getProtectedItems(), toMicroOperations( grants ), true, precedence ) );
            }
            if ( denials.size() > 0 )
            {
                tuples.add( new ACITuple( getUserClasses(), getAuthenticationLevel(), userPermission
                    .getProtectedItems(), toMicroOperations( denials ), false, precedence ) );
            }
        }
        return tuples;
    }
    
    
    /**
     * Converts this item into its string representation as stored
     * in directory.
     *
     * @param buffer the string buffer
     */
    public void printToBuffer( StringBuffer buffer )
    {
        buffer.append( '{' );
        buffer.append( ' ' );
        
        // identificationTag
        buffer.append( "identificationTag" );
        buffer.append( ' ' );
        buffer.append( '"' );
        buffer.append( getIdentificationTag() );
        buffer.append( '"' );
        buffer.append( ',' );
        buffer.append( ' ' );
        
        // precedence
        buffer.append( "precedence" );
        buffer.append( ' ' );
        buffer.append( getPrecedence() );
        buffer.append( ',' );
        buffer.append( ' ' );
        
        // authenticationLevel
        buffer.append( "authenticationLevel" );
        buffer.append( ' ' );
        buffer.append( getAuthenticationLevel().getName() );
        buffer.append( ',' );
        buffer.append( ' ' );
        
        // itemOrUserFirst
        buffer.append( "itemOrUserFirst" );
        buffer.append( ' ' );
        buffer.append( "userFirst" );
        buffer.append( ':' );
        buffer.append( ' ' );
        
        buffer.append( '{' );
        buffer.append( ' ' );
        
        // protectedItems
        buffer.append( "userClasses" );
        buffer.append( ' ' );
        buffer.append( '{' );
        buffer.append( ' ' );
        for ( Iterator it = userClasses.iterator(); it.hasNext(); )
        {
            UserClass userClass =  ( UserClass ) it.next();
            userClass.printToBuffer( buffer );
            
            if(it.hasNext()) {
                buffer.append( ',' );
                buffer.append( ' ' );
            }
        }
        buffer.append( ' ' );
        buffer.append( '}' );
        
        buffer.append( ',' );
        buffer.append( ' ' );
        
        // itemPermissions
        buffer.append( "userPermissions" );
        buffer.append( ' ' );
        buffer.append( '{' );
        buffer.append( ' ' );
        for ( Iterator it = userPermissions.iterator(); it.hasNext(); )
        {
            UserPermission permission = ( UserPermission ) it.next();
            permission.printToBuffer( buffer );
            
            if(it.hasNext()) {
                buffer.append( ',' );
                buffer.append( ' ' );
            }
        }
        buffer.append( ' ' );
        buffer.append( '}' );
        
        buffer.append( ' ' );
        buffer.append( '}' );
        
        buffer.append( ' ' );
        buffer.append( '}' );
    }
}
