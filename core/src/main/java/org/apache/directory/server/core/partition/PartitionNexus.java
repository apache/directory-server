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
package org.apache.directory.server.core.partition;


import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.core.configuration.PartitionConfiguration;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.NoOpNormalizer;
import org.apache.directory.shared.ldap.schema.OidNormalizer;


/**
 * A root {@link Partition} that contains all other partitions, and
 * routes all operations to the child partition that matches to its base suffixes.
 * It also provides some extended operations such as accessing rootDSE and
 * listing base suffixes.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public abstract class PartitionNexus implements Partition
{
    /** the default user principal or DN */
    public final static String ADMIN_PRINCIPAL = "uid=admin,ou=system";
    /** the normalized user principal or DN */
    public final static String ADMIN_PRINCIPAL_NORMALIZED = "0.9.2342.19200300.100.1.1=admin,2.5.4.11=system";
    /** the admin super user uid */
    public final static String ADMIN_UID = "admin";
    /** the initial admin passwd set on startup */
    public static final String ADMIN_PASSWORD = "secret";
    /** the base dn under which all users reside */
    public final static String USERS_BASE_NAME = "ou=users,ou=system";
    /** the base dn under which all groups reside */
    public final static String GROUPS_BASE_NAME = "ou=groups,ou=system";

    /** UID attribute name and OID */
    private static final String UID_ATTRIBUTE = "uid";
    private static final String UID_ATTRIBUTE_ALIAS = "userid";
    private static final String UID_ATTRIBUTE_OID = "0.9.2342.19200300.100.1.1";
    
    /** OU attribute names and OID **/
    private static final String OU_ATTRIBUTE = "ou";
    private static final String OU_ATTRIBUTE_ALIAS = "organizationalUnitName";
    private static final String OU_ATTRIBUTE_OID = "2.5.4.11";

    /**
     * System partition suffix constant.  Should be kept down to a single Dn name 
     * component or the default constructor will have to parse it instead of 
     * building the name.  Note that what ever the SUFFIX equals it should be 
     * both the normalized and the user provided form.
     */
    public static final String SYSTEM_PARTITION_SUFFIX = "ou=system";


    /**
     * Gets the DN for the admin user.
     * @return the admin user DN
     */
    public static final LdapDN getAdminName()
    {
        LdapDN adminDn = null;

        try
        {
            adminDn = new LdapDN( ADMIN_PRINCIPAL );
        }
        catch ( NamingException e )
        {
            throw new InternalError();
        }
        
        try
        {
        	Map oidsMap = new HashMap();
        	
        	oidsMap.put( UID_ATTRIBUTE, new OidNormalizer( UID_ATTRIBUTE_OID, new NoOpNormalizer() ) );
        	oidsMap.put( UID_ATTRIBUTE_ALIAS, new OidNormalizer( UID_ATTRIBUTE_OID, new NoOpNormalizer() ) );
        	oidsMap.put( UID_ATTRIBUTE_OID, new OidNormalizer( UID_ATTRIBUTE_OID, new NoOpNormalizer() ) );
        	
        	oidsMap.put( OU_ATTRIBUTE, new OidNormalizer( OU_ATTRIBUTE_OID, new NoOpNormalizer()  ) );
        	oidsMap.put( OU_ATTRIBUTE_ALIAS, new OidNormalizer( OU_ATTRIBUTE_OID, new NoOpNormalizer()  ) );
        	oidsMap.put( OU_ATTRIBUTE_OID, new OidNormalizer( OU_ATTRIBUTE_OID, new NoOpNormalizer()  ) );

            adminDn.normalize( oidsMap );
        }
        catch ( InvalidNameException ine )
        {
            // Nothing we can do ...
        }
        catch ( NamingException ne )
        {
            // Nothing we can do ...
        }

        return adminDn;
    }


    /**
     * Gets the DN for the base entry under which all groups reside.
     * A new Name instance is created and returned every time.
     * @return the groups base DN
     */
    public static final LdapDN getGroupsBaseName()
    {
        LdapDN groupsBaseDn = null;

        try
        {
            groupsBaseDn = new LdapDN( GROUPS_BASE_NAME );
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
    public static final LdapDN getUsersBaseName()
    {
        LdapDN usersBaseDn = null;

        try
        {
            usersBaseDn = new LdapDN( USERS_BASE_NAME );
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
    public abstract boolean compare( LdapDN name, String oid, Object value ) throws NamingException;


    public abstract void addContextPartition( PartitionConfiguration config ) throws NamingException;


    public abstract void removeContextPartition( LdapDN suffix ) throws NamingException;


    public abstract Partition getSystemPartition();


    /**
     * Get's the partition corresponding to a distinguished name.  This 
     * name need not be the name of the partition suffix.  When used in 
     * conjunction with get suffix this can properly find the partition 
     * associated with the DN.  Make sure to use the normalized DN.
     * 
     * @param dn the normalized distinguished name to get a partition for
     * @return the partition containing the entry represented by the dn
     * @throws NamingException if there is no partition for the dn
     */
    public abstract Partition getPartition( LdapDN dn ) throws NamingException;


    /**
     * Gets the most significant Dn that exists within the server for any Dn.
     *
     * @param name the normalized distinguished name to use for matching.
     * @return a distinguished name representing the matching portion of dn,
     * as originally provided by the user on creation of the matched entry or 
     * the empty string distinguished name if no match was found.
     * @throws NamingException if there are any problems
     */
    public abstract LdapDN getMatchedName ( LdapDN name ) throws NamingException;


    /**
     * Gets the distinguished name of the suffix that would hold an entry with
     * the supplied distinguished name parameter.  If the DN argument does not
     * fall under a partition suffix then the empty string Dn is returned.
     *
     * @param name the normalized distinguished name to use for finding a suffix.
     * @return the suffix portion of dn, or the valid empty string Dn if no
     * naming context was found for dn.
     * @throws NamingException if there are any problems
     */
    public abstract LdapDN getSuffix ( LdapDN name ) throws NamingException;


    /**
     * Gets an iteration over the Name suffixes of the partitions managed by this
     * {@link PartitionNexus}.
     *
     * @return Iteration over ContextPartition suffix names as Names.
     * @throws NamingException if there are any problems
     */
    public abstract Iterator listSuffixes () throws NamingException;


    /**
     * Adds a set of supportedExtension (OID Strings) to the RootDSE.
     * 
     * @param extensionOids a set of OID strings to add to the supportedExtension 
     * attribute in the RootDSE
     */
    public abstract void registerSupportedExtensions( Set extensionOids );
}
