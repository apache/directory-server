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
package org.apache.directory.server.core.authz;


import static org.apache.directory.server.core.integ.IntegrationUtils.getSystemContext;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.LdapContext;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.DefaultDirectoryService;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ServerEntry;
import org.apache.directory.server.core.integ.DirectoryServiceFactory;
import org.apache.directory.server.core.partition.Partition;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmIndex;
import org.apache.directory.server.core.partition.impl.btree.jdbm.JdbmPartition;
import org.apache.directory.server.core.partition.ldif.LdifPartition;
import org.apache.directory.server.core.schema.SchemaPartition;
import org.apache.directory.server.core.subtree.SubentryInterceptor;
import org.apache.directory.server.xdbm.Index;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.schema.DefaultSchemaManager;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.schema.ldif.extractor.impl.DefaultSchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.ldif.extractor.SchemaLdifExtractor;
import org.apache.directory.shared.ldap.schema.loader.ldif.JarLdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.loader.ldif.LdifSchemaLoader;
import org.apache.directory.shared.ldap.schema.registries.SchemaLoader;
import org.apache.directory.shared.ldap.util.ExceptionUtils;


/**
 * Some extra utility methods added to it which are required by all
 * authorization tests.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AutzIntegUtils
{
    public static DirectoryService service;

    public static class ServiceFactory implements DirectoryServiceFactory
    {
        public DirectoryService newInstance() throws Exception
        {
            String workingDirectory = System.getProperty( "workingDirectory" );

            if ( workingDirectory == null )
            {
                String path = DirectoryServiceFactory.class.getResource( "" ).getPath();
                int targetPos = path.indexOf( "target" );
                workingDirectory = path.substring( 0, targetPos + 6 ) + "/server-work";
            }

            service = new DefaultDirectoryService();
            service.setWorkingDirectory( new File( workingDirectory ) );

            return service;
        }


        public void init() throws Exception
        {
            SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

            // Init the LdifPartition
            LdifPartition ldifPartition = new LdifPartition();

            String workingDirectory = service.getWorkingDirectory().getPath();

            ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

            // Extract the schema on disk (a brand new one) and load the registries
            File schemaRepository = new File( workingDirectory, "schema" );
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );
            extractor.extractOrCopy();

            schemaPartition.setWrappedPartition( ldifPartition );

            SchemaLoader loader = new LdifSchemaLoader( schemaRepository );

            SchemaManager schemaManager = new DefaultSchemaManager( loader );
            service.setSchemaManager( schemaManager );

            schemaManager.loadAllEnabled();

            List<Throwable> errors = schemaManager.getErrors();

            if ( errors.size() != 0 )
            {
                fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
            }

            schemaPartition.setSchemaManager( schemaManager );

            service.getChangeLog().setEnabled( true );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            // Inject the System Partition
            Partition systemPartition = new JdbmPartition();
            systemPartition.setId( "system" );
            ( ( JdbmPartition ) systemPartition ).setCacheSize( 500 );
            systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
            systemPartition.setSchemaManager( schemaManager );
            ( ( JdbmPartition ) systemPartition ).setPartitionDir( new File( workingDirectory, "system" ) );

            // Add objectClass attribute for the system partition
            Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<Object, ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );

            service.setSystemPartition( systemPartition );
            service.setAccessControlEnabled( true );
        }
    }

    public static class DefaultServiceFactory implements DirectoryServiceFactory
    {
        public DirectoryService newInstance() throws Exception
        {
            String workingDirectory = System.getProperty( "workingDirectory" );

            if ( workingDirectory == null )
            {
                String path = DirectoryServiceFactory.class.getResource( "" ).getPath();
                int targetPos = path.indexOf( "target" );
                workingDirectory = path.substring( 0, targetPos + 6 ) + "/server-work";
            }

            DirectoryService service = new DefaultDirectoryService();
            service.setWorkingDirectory( new File( workingDirectory ) );

            return service;
        }


        public void init() throws Exception
        {
            SchemaPartition schemaPartition = service.getSchemaService().getSchemaPartition();

            // Init the LdifPartition
            LdifPartition ldifPartition = new LdifPartition();

            String workingDirectory = service.getWorkingDirectory().getPath();

            ldifPartition.setWorkingDirectory( workingDirectory + "/schema" );

            // Extract the schema on disk (a brand new one) and load the registries
            File schemaRepository = new File( workingDirectory, "schema" );
            SchemaLdifExtractor extractor = new DefaultSchemaLdifExtractor( new File( workingDirectory ) );

            schemaPartition.setWrappedPartition( ldifPartition );

            JarLdifSchemaLoader loader = new JarLdifSchemaLoader();
            SchemaManager schemaManager = new DefaultSchemaManager( loader );
            service.setSchemaManager( schemaManager );

            schemaManager.loadAllEnabled();

            List<Throwable> errors = schemaManager.getErrors();

            if ( errors.size() != 0 )
            {
                fail( "Schema load failed : " + ExceptionUtils.printErrors( errors ) );
            }

            schemaPartition.setSchemaManager( schemaManager );

            extractor.extractOrCopy();

            service.getChangeLog().setEnabled( true );

            // change the working directory to something that is unique
            // on the system and somewhere either under target directory
            // or somewhere in a temp area of the machine.

            // Inject the System Partition
            Partition systemPartition = new JdbmPartition();
            systemPartition.setId( "system" );
            ( ( JdbmPartition ) systemPartition ).setCacheSize( 500 );
            systemPartition.setSuffix( ServerDNConstants.SYSTEM_DN );
            systemPartition.setSchemaManager( schemaManager );
            ( ( JdbmPartition ) systemPartition ).setPartitionDir( new File( workingDirectory, "system" ) );

            // Add objectClass attribute for the system partition
            Set<Index<?, ServerEntry>> indexedAttrs = new HashSet<Index<?, ServerEntry>>();
            indexedAttrs.add( new JdbmIndex<Object, ServerEntry>( SchemaConstants.OBJECT_CLASS_AT ) );
            ( ( JdbmPartition ) systemPartition ).setIndexedAttributes( indexedAttrs );

            service.setSystemPartition( systemPartition );
            service.setAccessControlEnabled( false );
            AutzIntegUtils.service = service;
        }
    }


    // -----------------------------------------------------------------------
    // Utility methods used by subclasses
    // -----------------------------------------------------------------------

    /**
     * Gets a context at ou=system as the admin user.
     *
     * @return the admin context at ou=system
     * @throws NamingException if there are problems creating the context
     */
    public static DirContext getContextAsAdmin() throws Exception
    {
        return getSystemContext( service );
    }


    /**
     * Gets a context at some dn within the directory as the admin user.
     * Should be a dn of an entry under ou=system since no other partitions
     * are enabled.
     *
     * @param dn the DN of the context to get
     * @return the context for the DN as the admin user
     * @throws NamingException if is a problem initializing or getting the context
     */
    @SuppressWarnings("unchecked")
    public static DirContext getContextAsAdmin( String dn ) throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Hashtable<String, Object> env = ( Hashtable<String, Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, "uid=admin, ou=system" );
        env.put( DirContext.SECURITY_CREDENTIALS, "secret" );
        env.put( DirContext.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( DirectoryService.JNDI_KEY, service );
        return new InitialDirContext( env );
    }


    /**
     * Creates a group using the groupOfUniqueNames objectClass under the
     * ou=groups,ou=sytem container with an initial member.
     *
     * @param cn the common name of the group used as the RDN attribute
     * @param firstMemberDn the DN of the first member of this group
     * @return the distinguished name of the group entry
     * @throws NamingException if there are problems creating the new group like
     * it exists already
     */
    public static Name createGroup( String cn, String firstMemberDn ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes group = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );
        group.put( "uniqueMember", firstMemberDn );
        adminCtx.createSubcontext( "cn=" + cn + ",ou=groups", group );
        return new LdapDN( "cn=" + cn + ",ou=groups,ou=system" );
    }


    /**
     * Deletes a user with a specific UID under ou=users,ou=system.
     *
     * @param uid the RDN value for the user to delete
     * @throws NamingException if there are problems removing the user
     * i.e. user does not exist
     */
    public static void deleteUser( String uid ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        adminCtx.destroySubcontext( "uid=" + uid + ",ou=users" );
    }


    /**
     * Creates a simple user as an inetOrgPerson under the ou=users,ou=system
     * container.  The user's RDN attribute is the uid argument.  This argument
     * is also used as the value of the two MUST attributes: sn and cn.
     *
     * @param uid the value of the RDN attriubte (uid), the sn and cn attributes
     * @param password the password to use to create the user
     * @return the dn of the newly created user entry
     * @throws NamingException if there are problems creating the user entry
     */
    public static Name createUser( String uid, String password ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes user = new BasicAttributes( "uid", uid, true );
        user.put( "userPassword", password );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        user.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "person" );
        objectClass.add( "organizationalPerson" );
        objectClass.add( "inetOrgPerson" );
        user.put( "sn", uid );
        user.put( "cn", uid );
        adminCtx.createSubcontext( "uid=" + uid + ",ou=users", user );
        return new LdapDN( "uid=" + uid + ",ou=users,ou=system" );
    }


    /**
     * Creates a simple groupOfUniqueNames under the ou=groups,ou=system
     * container.  The admin user is always a member of this newly created 
     * group.
     *
     * @param groupName the name of the cgroup to create
     * @return the DN of the group as a Name object
     * @throws NamingException if the group cannot be created
     */
    public static Name createGroup( String groupName ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes group = new BasicAttributes( true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        group.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( "groupOfUniqueNames" );

        // TODO might be ServerDNConstants.ADMIN_SYSTEM_DN_NORMALIZED
        group.put( "uniqueMember", "uid=admin, ou=system" );
        adminCtx.createSubcontext( "cn=" + groupName + ",ou=groups", group );
        return new LdapDN( "cn=" + groupName + ",ou=groups,ou=system" );
    }


    /**
     * Adds an existing user under ou=users,ou=system to an existing group under the
     * ou=groups,ou=system container.
     *
     * @param userUid the uid of the user to add to the group
     * @param groupCn the cn of the group to add the user to
     * @throws NamingException if the group does not exist
     */
    public static void addUserToGroup( String userUid, String groupCn ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new BasicAttributes( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn=" + groupCn + ",ou=groups", DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Removes a user from a group.
     *
     * @param userUid the RDN attribute value of the user to remove from the group
     * @param groupCn the RDN attribute value of the group to have user removed from
     * @throws NamingException if there are problems accessing the group
     */
    public static void removeUserFromGroup( String userUid, String groupCn ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new BasicAttributes( "uniqueMember", "uid=" + userUid + ",ou=users,ou=system", true );
        adminCtx.modifyAttributes( "cn=" + groupCn + ",ou=groups", DirContext.REMOVE_ATTRIBUTE, changes );
    }


    /**
     * Gets the context at ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @return the context as the user
     * @throws NamingException if the user does not exist or authx fails
     */
    public static DirContext getContextAs( Name user, String password ) throws Exception
    {
        return getContextAs( user, password, ServerDNConstants.SYSTEM_DN );
    }


    /**
     * Gets the context at any DN under ou=system as a specific user.
     *
     * @param user the DN of the user to get the context as
     * @param password the password of the user
     * @param dn the distinguished name of the entry to get the context for
     * @return the context representing the entry at the dn as a specific user
     * @throws NamingException if the does not exist or authx fails
     */
    @SuppressWarnings("unchecked")
    public static DirContext getContextAs( Name user, String password, String dn ) throws Exception
    {
        LdapContext sysRoot = getSystemContext( service );
        Hashtable<String, Object> env = ( Hashtable<String, Object> ) sysRoot.getEnvironment().clone();
        env.put( DirContext.PROVIDER_URL, dn );
        env.put( DirContext.SECURITY_AUTHENTICATION, "simple" );
        env.put( DirContext.SECURITY_PRINCIPAL, user.toString() );
        env.put( DirContext.SECURITY_CREDENTIALS, password );
        env.put( DirContext.INITIAL_CONTEXT_FACTORY, "org.apache.directory.server.core.jndi.CoreContextFactory" );
        env.put( DirectoryService.JNDI_KEY, service );
        return new InitialDirContext( env );
    }


    public static void deleteAccessControlSubentry( String cn ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        adminCtx.destroySubcontext( "cn=" + cn );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public static void createAccessControlSubentry( String cn, String aciItem ) throws Exception
    {
        createAccessControlSubentry( cn, "{}", aciItem );
    }


    /**
     * Creates an access control subentry under ou=system whose subtree covers
     * the entire naming context.
     *
     * @param cn the common name and rdn for the subentry
     * @param subtree the subtreeSpecification for the subentry
     * @param aciItem the prescriptive ACI attribute value
     * @throws NamingException if there is a problem creating the subentry
     */
    public static void createAccessControlSubentry( String cn, String subtree, String aciItem ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify ou=system to be an AP for an A/C AA if it is not already
        Attributes ap = adminCtx.getAttributes( "", new String[]
            { "administrativeRole" } );
        Attribute administrativeRole = ap.get( "administrativeRole" );
        if ( administrativeRole == null || !administrativeRole.contains( SubentryInterceptor.AC_AREA ) )
        {
            Attributes changes = new BasicAttributes( "administrativeRole", SubentryInterceptor.AC_AREA, true );
            adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
        }

        // now add the A/C subentry below ou=system
        Attributes subentry = new BasicAttributes( "cn", cn, true );
        Attribute objectClass = new BasicAttribute( "objectClass" );
        subentry.put( objectClass );
        objectClass.add( "top" );
        objectClass.add( SchemaConstants.SUBENTRY_OC );
        objectClass.add( "accessControlSubentry" );
        subentry.put( "subtreeSpecification", subtree );
        subentry.put( "prescriptiveACI", aciItem );
        adminCtx.createSubcontext( "cn=" + cn, subentry );
    }


    /**
     * Adds and entryACI attribute to an entry specified by a relative name
     * with respect to ou=system
     *
     * @param rdn a name relative to ou=system
     * @param aciItem the entryACI attribute value
     * @throws NamingException if there is a problem adding the attribute
     */
    public static void addEntryACI( Name rdn, String aciItem ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify the entry relative to ou=system to include the aciItem
        Attributes changes = new BasicAttributes( "entryACI", aciItem, true );
        adminCtx.modifyAttributes( rdn, DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Adds and subentryACI attribute to ou=system
     *
     * @param aciItem the subentryACI attribute value
     * @throws NamingException if there is a problem adding the attribute
     */
    public static void addSubentryACI( String aciItem ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();

        // modify the entry relative to ou=system to include the aciItem
        Attributes changes = new BasicAttributes( "subentryACI", aciItem, true );
        adminCtx.modifyAttributes( "", DirContext.ADD_ATTRIBUTE, changes );
    }


    /**
     * Replaces values of an prescriptiveACI attribute of a subentry subordinate
     * to ou=system.
     *
     * @param cn the common name of the aci subentry
     * @param aciItem the new value for the ACI item
     * @throws NamingException if the modify fails
     */
    public static void changePresciptiveACI( String cn, String aciItem ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new BasicAttributes( "prescriptiveACI", aciItem, true );
        adminCtx.modifyAttributes( "cn=" + cn, DirContext.REPLACE_ATTRIBUTE, changes );
    }


    public static void addPrescriptiveACI( String cn, String aciItem ) throws Exception
    {
        DirContext adminCtx = getContextAsAdmin();
        Attributes changes = new BasicAttributes( "prescriptiveACI", aciItem, true );
        adminCtx.modifyAttributes( "cn=" + cn, DirContext.ADD_ATTRIBUTE, changes );
    }
}
