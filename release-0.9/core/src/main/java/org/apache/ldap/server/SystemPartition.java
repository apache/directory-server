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
package org.apache.ldap.server;

import org.apache.ldap.common.message.LockableAttributesImpl;
import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.schema.AttributeType;
import org.apache.ldap.common.util.DateUtils;
import org.apache.ldap.common.util.NamespaceTools;
import org.apache.ldap.server.db.Database;
import org.apache.ldap.server.db.SearchEngine;

import javax.naming.InvalidNameException;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;


/**
 * A very special ContextPartition used to store system information such as
 * users, the system catalog and other administrative information.  This
 * partition is fixed at the ou=system context.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public final class SystemPartition extends AbstractContextPartition
{
    /** the default user principal or DN */
    public final static String ADMIN_PRINCIPAL = "uid=admin,ou=system";
    /** the admin super user uid */
    public final static String ADMIN_UID = "admin";
    /** the initial admin passwd set on startup */
    public static final byte[] ADMIN_PW = "secret".getBytes();
    /** the base dn under which all users reside */
    public final static String USERS_BASE_DN = "ou=users,ou=system";
    /** the base dn under which all groups reside */
    public final static String GROUPS_BASE_DN = "ou=groups,ou=system";

    /**
     * System backend suffix constant.  Should be kept down to a single Dn name 
     * component or the default constructor will have to parse it instead of 
     * building the name.  Note that what ever the SUFFIX equals it should be 
     * both the normalized and the user provided form.
     */
    public static final String SUFFIX = "ou=system" ;
    
    /** The suffix as a name. */
    private final Name suffix ;


    // ------------------------------------------------------------------------
    // S T A T I C   M E T H O D S
    // ------------------------------------------------------------------------


    /**
     * Gets the DN for the base entry under which all non-admin users reside.
     * A new Name instance is created and returned every time.
     *
     * @see #USERS_BASE_DN
     * @return the users base DN
     */
    public static final Name getUsersBaseDn()
    {
        Name usersBaseDn = null;

        try
        {
            usersBaseDn = new LdapName( USERS_BASE_DN );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            // should never really happen since names are correct
        }

        return usersBaseDn;
    }


    /**
     * Gets the DN for the base entry under which all groups reside.
     * A new Name instance is created and returned every time.
     *
     * @see #GROUPS_BASE_DN
     * @return the groups base DN
     */
    public static final Name getGroupsBaseDn()
    {
        Name groupsBaseDn = null;

        try
        {
            groupsBaseDn = new LdapName( GROUPS_BASE_DN );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            // should never really happen since names are correct
        }

        return groupsBaseDn;
    }


    /**
     * Gets the DN for the admin user.
     *
     * @see #ADMIN_PRINCIPAL
     * @return the admin user DN
     */
    public static final Name getAdminDn()
    {
        Name adminDn = null;

        try
        {
            adminDn = new LdapName( ADMIN_PRINCIPAL );
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
            // should never really happen since names are correct
        }

        return adminDn;
    }


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S 
    // ------------------------------------------------------------------------
    
    
    /**
     * Creates the system partition which is used to store various peices of
     * information critical for server operation.  Things like the system
     * catalog and other operational information like system users are
     * maintained within the context of this partition.  Unlike other
     * ContextBackends which must have their suffix specified this one does
     * not since it will stay fixed at the following namingContext: ou=system.
     *
     * @param db the database used for this partition
     * @param searchEngine the search engine to conduct searches with
     * @param indexAttributes the attributeTypes of indicies to build which must
     * also contain all system index attribute types - if not the system will
     * not operate correctly.
     */
    public SystemPartition( Database db, SearchEngine searchEngine,
                            AttributeType[] indexAttributes )
        throws NamingException
    {
        super( db, searchEngine, indexAttributes );
        suffix = new LdapName() ;
        
        try
        {
            suffix.add( SUFFIX ) ;
        }
        catch ( InvalidNameException e ) 
        {
            // Never thrown - name will always be valid!
        }

        // add the root entry for the system root context if it does not exist
        Attributes attributes = db.getSuffixEntry() ;
        if ( null == attributes )
        {
            attributes = new LockableAttributesImpl() ;
            attributes.put( "objectClass", "top" ) ;
            attributes.put( "objectClass", "organizationalUnit" ) ;
            attributes.put( "creatorsName", ADMIN_PRINCIPAL ) ;
            attributes.put( "createTimestamp", DateUtils.getGeneralizedTime() ) ;
            attributes.put( NamespaceTools.getRdnAttribute( SUFFIX ),
                NamespaceTools.getRdnValue( SUFFIX ) ) ;

            getDb().add( SUFFIX, suffix, attributes ) ;
        }
    }


    // ------------------------------------------------------------------------
    // B A C K E N D   M E T H O D S 
    // ------------------------------------------------------------------------


    /**
     * @see org.apache.ldap.server.ContextPartition#getSuffix(boolean)
     */
    public final Name getSuffix( boolean normalized )
    {
        /*
         * The suffix is presummed to be both the normalized and the user
         * provided form so we do not need to take a_normalized into account.
         */
        return ( Name ) suffix.clone() ;
    }


    /**
     * @see org.apache.ldap.server.BackingStore#isSuffix(javax.naming.Name)
     */
    public final boolean isSuffix( Name dn )
    {
        return SUFFIX.equals( dn.toString() ) ;
    }
}
