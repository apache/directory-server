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
package org.apache.ldap.server.authz.acdf.userclass;

import org.apache.ldap.common.name.LdapName;

public class UserGroupUserClass extends UserClass
{
    private static final long serialVersionUID = 8887107815072965807L;

    private final LdapName groupName;
    
    public UserGroupUserClass( LdapName username )
    {
        this.groupName = ( LdapName ) username.clone();
    }
    
    public LdapName getGroupName()
    {
        return ( LdapName ) groupName.clone();
    }

    public boolean equals( Object o )
    {
        if( this == o )
        {
            return true;
        }
        
        if( o instanceof UserGroupUserClass )
        {
            UserGroupUserClass that = ( UserGroupUserClass ) o;
            return this.groupName.equals( that.groupName );
        }
        
        return false;
    }
    
    public String toString()
    {
        return "userGroup: " + groupName;
    }
}
