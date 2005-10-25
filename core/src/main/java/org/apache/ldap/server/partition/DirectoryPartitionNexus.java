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
package org.apache.ldap.server.partition;


import java.util.Iterator;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.server.configuration.DirectoryPartitionConfiguration;


/**
 * A root {@link DirectoryPartition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class DirectoryPartitionNexus implements DirectoryPartition
{
    /** the default user principal or DN */
    public final static String ADMIN_PRINCIPAL = "uid=admin,ou=system";
    /** the admin super user uid */
    public final static String ADMIN_UID = "admin";
    /** the initial admin passwd set on startup */
    public static final String ADMIN_PASSWORD = "secret";
    /** the base dn under which all users reside */
    public final static String USERS_BASE_NAME = "ou=users,ou=system";
    /** the base dn under which all groups reside */
    public final static String GROUPS_BASE_NAME = "ou=groups,ou=system";

    /**
     * System backend suffix constant.  Should be kept down to a single Dn name 
     * component or the default constructor will have to parse it instead of 
     * building the name.  Note that what ever the SUFFIX equals it should be 
     * both the normalized and the user provided form.
     */
    public static final String SYSTEM_PARTITION_SUFFIX = "ou=system" ;

    /**
     * Gets the DN for the admin user.
     * @return the admin user DN
     */
    public static final Name getAdminName()
    {
        Name adminDn = null;
    
        try
        {
            adminDn = new LdapName( ADMIN_PRINCIPAL );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }
    
        return adminDn;
    }

    /**
     * Gets the DN for the base entry under which all groups reside.
     * A new Name instance is created and returned every time.
     * @return the groups base DN
     */
    public static final Name getGroupsBaseName()
    {
        Name groupsBaseDn = null;
    
        try
        {
            groupsBaseDn = new LdapName( GROUPS_BASE_NAME );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }
    
        return groupsBaseDn;
    }

    /**
     * Gets the DN for the base entry under which all non-admin users reside.
     * A new Name instance is created and returned every time.
     * @return the users base DN
     */
    public static final Name getUsersBaseName()
    {
        Name usersBaseDn = null;
    
        try
        {
            usersBaseDn = new LdapName( USERS_BASE_NAME );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }
    
        return usersBaseDn;
    }

    /**
     * Gets the LdapContext associated with the calling thread.
     * 
     * @return The LdapContext associated with the thread of execution or null
     * if no context is associated with the calling thread.
     */
    public abstract LdapContext getLdapContext();

    /**
     * Get's the RootDSE entry for the DSA.
     *
     * @return the attributes of the RootDSE
     */
    public abstract Attributes getRootDSE() throws NamingException;

    /**
     * Performs a comparison check to see if an attribute of an entry has
     * a specified value.
     *
     * @param name the normalized name of the entry
     * @param oid the attribute being compared
     * @param value the value the attribute is compared to
     * @return true if the entry contains an attribute with the value, false otherwise
     * @throws NamingException if there is a problem accessing the entry and its values
     */
    public abstract boolean compare( Name name, String oid, Object value ) throws NamingException;

    public abstract void addContextPartition( DirectoryPartitionConfiguration config ) throws NamingException;
    
    public abstract void removeContextPartition( Name suffix ) throws NamingException;

    public abstract DirectoryPartition getSystemPartition();

    /**
     * Gets the most significant Dn that exists within the server for any Dn.
     *
     * @param name the normalized distinguished name to use for matching.
     * @param normalized boolean if true cause the return of a normalized Dn,
     * if false it returns the original user provided distinguished name for 
     * the matched portion of the Dn as it was provided on entry creation.
     * @return a distinguished name representing the matching portion of dn,
     * as originally provided by the user on creation of the matched entry or 
     * the empty string distinguished name if no match was found.
     * @throws NamingException if there are any problems
     */
    public abstract Name getMatchedName( Name name, boolean normalized ) throws NamingException;

    /**
     * Gets the distinguished name of the suffix that would hold an entry with
     * the supplied distinguished name parameter.  If the DN argument does not
     * fall under a partition suffix then the empty string Dn is returned.
     *
     * @param name the normalized distinguished name to use for finding a suffix.
     * @param normalized if true causes the return of a normalized Dn, but
     * if false it returns the original user provided distinguished name for 
     * the suffix Dn as it was provided on suffix entry creation.
     * @return the suffix portion of dn, or the valid empty string Dn if no
     * naming context was found for dn.
     * @throws NamingException if there are any problems
     */
    public abstract Name getSuffix( Name name, boolean normalized ) throws NamingException;

    /**
     * Gets an iteration over the Name suffixes of the Backends managed by this
     * {@link DirectoryPartitionNexus}.
     *
     * @param normalized if true the returned Iterator contains normalized Dn
     * but, if false, it returns the original user provided distinguished names
     * in the Iterator.
     * @return Iteration over ContextPartition suffix names as Names.
     * @throws NamingException if there are any problems
     */
    public abstract Iterator listSuffixes( boolean normalized ) throws NamingException;
}
