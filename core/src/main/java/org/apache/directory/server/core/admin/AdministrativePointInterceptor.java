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
package org.apache.directory.server.core.admin;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.naming.directory.SearchControls;

import org.apache.directory.server.constants.ServerDNConstants;
import org.apache.directory.server.core.CoreSession;
import org.apache.directory.server.core.DefaultCoreSession;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.LdapPrincipal;
import org.apache.directory.server.core.administrative.AdministrativePoint;
import org.apache.directory.server.core.administrative.AutonomousAdministrativePoint;
import org.apache.directory.server.core.administrative.InnerAdministrativePoint;
import org.apache.directory.server.core.administrative.SpecificAdministrativePoint;
import org.apache.directory.server.core.authn.Authenticator;
import org.apache.directory.server.core.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.Interceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.partition.PartitionNexus;
import org.apache.directory.server.i18n.I18n;
import org.apache.directory.shared.ldap.constants.AuthenticationLevel;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.DefaultEntryAttribute;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.Value;
import org.apache.directory.shared.ldap.exception.LdapAttributeInUseException;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapInvalidAttributeValueException;
import org.apache.directory.shared.ldap.exception.LdapNoSuchAttributeException;
import org.apache.directory.shared.ldap.exception.LdapOperationException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.filter.PresenceNode;
import org.apache.directory.shared.ldap.message.AliasDerefMode;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.apache.directory.shared.ldap.schema.SchemaManager;
import org.apache.directory.shared.ldap.subtree.AdministrativeRole;
import org.apache.directory.shared.ldap.util.StringTools;
import org.apache.directory.shared.ldap.util.tree.DnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor to manage the Administrative model
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class AdministrativePointInterceptor extends BaseInterceptor
{
    /** A {@link Logger} for this class */
    private static final Logger LOG = LoggerFactory.getLogger( AdministrativePointInterceptor.class );

    /**
     * Speedup for logs
     */
    private static final boolean IS_DEBUG = LOG.isDebugEnabled();

    /** A reference to the DirectoryService instance */
    private DirectoryService directoryService;

    /** A reference to the SchemaManager instance */
    private SchemaManager schemaManager;

    /** A reference to the nexus for direct backend operations */
    private PartitionNexus nexus;

    /** A reference to the AdministrativeRole AT */
    private static AttributeType ADMINISTRATIVE_ROLE_AT;

    /** A reference to the EntryUUID AT */
    private static AttributeType ENTRY_UUID_AT;

    /** The possible roles */
    private static final Set<String> ROLES = new HashSet<String>();

    // Initialize the ROLES field
    static
    {
        ROLES.add( SchemaConstants.AUTONOMOUS_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.AUTONOMOUS_AREA_OID );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase() );
        ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** A Map to associate a role with it's OID */
    private static final Map<String, String> ROLES_OID = new HashMap<String, String>();

    // Initialize the roles/oid map
    static
    {
        ROLES_OID.put( SchemaConstants.AUTONOMOUS_AREA.toLowerCase(), SchemaConstants.AUTONOMOUS_AREA_OID );
        ROLES_OID.put( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase(),
            SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        ROLES_OID.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase(),
            SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        ROLES_OID.put( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase(),
            SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
        ROLES_OID.put( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase(),
            SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** The possible inner area roles */
    private static final Set<String> INNER_AREA_ROLES = new HashSet<String>();

    static
    {
        INNER_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID );
        INNER_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID );
        INNER_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase() );
        INNER_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID );
    }

    /** The possible specific area roles */
    private static final Set<String> SPECIFIC_AREA_ROLES = new HashSet<String>();

    static
    {
        SPECIFIC_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase() );
        SPECIFIC_AREA_ROLES.add( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID );
    }

    private DnNode<List<AdministrativePoint>> adminPointCache;


    /**
     * Tells if the given role is a InnerArea role
     */
    private boolean isInnerArea( String role )
    {
        return INNER_AREA_ROLES.contains( StringTools.toLowerCase( StringTools.trim( role ) ) );
    }


    /**
     * Tells if the AdministrativeRole attribute contains the same Specific Area role
     * than the given Inner Area role
     */
    private boolean hasSpecificArea( String role, EntryAttribute modifiedAdminRole )
    {
        // Check if the associated specific area role is already present
        if ( role.equals( SchemaConstants.ACCESS_CONTROL_INNER_AREA.toLowerCase() )
            || role.equals( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) )
        {
            if ( modifiedAdminRole.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA.toLowerCase() )
                || modifiedAdminRole.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID ) )
            {
                // Not a valid role : we will throw an exception
                return true;
            }
        }
        else if ( role.equals( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA.toLowerCase() )
            || role.equals( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) )
        {
            if ( modifiedAdminRole.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA )
                || modifiedAdminRole.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID ) )
            {
                // Not a valid role : we will throw an exception
                return true;
            }
        }
        else if ( role.equals( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA.toLowerCase() )
            || role.equals( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) )
        {
            if ( modifiedAdminRole.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA.toLowerCase() )
                || modifiedAdminRole.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID ) )
            {
                // Not a valid role : we will throw an exception
                return true;
            }
        }

        return false;
    }


    /**
     * Create an AP for a given entry, assuming that the AdminsitrativeRoles are correct 
     * (because we pulled the entry from the backend)
     */
    private List<AdministrativePoint> createAdministrativePoints( Entry adminPointEntry, String uuid )
    {
        List<AdministrativePoint> adminPoints = new ArrayList<AdministrativePoint>();
        EntryAttribute roles = adminPointEntry.get( ADMINISTRATIVE_ROLE_AT );
        DN dn = adminPointEntry.getDn();

        // Loop on all the roles
        for ( Value<?> value : roles )
        {
            String role = value.getString();

            // Deal with Autonomous AP
            if ( role.equalsIgnoreCase( SchemaConstants.AUTONOMOUS_AREA )
                || role.equalsIgnoreCase( SchemaConstants.AUTONOMOUS_AREA_OID ) )
            {
                AdministrativePoint aap = new AutonomousAdministrativePoint( dn, uuid );
                adminPoints.add( aap );

                // If it's an AAP, we can get out immediately
                return adminPoints;
            }

            // Deal with AccessControl AP
            if ( role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.AccessControlSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.AccessControlInnerArea );
                adminPoints.add( iap );

                continue;
            }

            // Deal with CollectveAttribute AP
            if ( role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.CollectiveAttributeSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.CollectiveAttributeInnerArea );
                adminPoints.add( iap );

                continue;
            }

            // Deal with SubSchema AP
            if ( role.equalsIgnoreCase( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.SubSchemaSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            // Deal with TriggerExecution AP
            if ( role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.TriggerExecutionSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.TriggerExecutionInnerArea );
                adminPoints.add( iap );

                continue;
            }
        }

        return adminPoints;
    }


    /**
     * Create the list of AP for a given entry
     */
    private List<AdministrativePoint> createAdministrativePoints( EntryAttribute adminPoint, DN dn, String uuid )
    {
        List<AdministrativePoint> adminPoints = new ArrayList<AdministrativePoint>();

        for ( Value<?> value : adminPoint )
        {
            String role = value.getString();

            // Deal with Autonomous AP
            if ( role.equalsIgnoreCase( SchemaConstants.AUTONOMOUS_AREA )
                || role.equalsIgnoreCase( SchemaConstants.AUTONOMOUS_AREA_OID ) )
            {
                AdministrativePoint aap = new AutonomousAdministrativePoint( dn, uuid );
                adminPoints.add( aap );

                // If it's an AAP, we can get out immediately
                return adminPoints;
            }

            // Deal with AccessControl AP
            if ( role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.AccessControlSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.AccessControlInnerArea );
                adminPoints.add( iap );

                continue;
            }

            // Deal with CollectveAttribute AP
            if ( role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.CollectiveAttributeSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.CollectiveAttributeInnerArea );
                adminPoints.add( iap );

                continue;
            }

            // Deal with SubSchema AP
            if ( role.equalsIgnoreCase( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.SUB_SCHEMA_ADMIN_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.SubSchemaSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            // Deal with TriggerExecution AP
            if ( role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA )
                || role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID ) )
            {
                AdministrativePoint sap = new SpecificAdministrativePoint( dn, uuid,
                    AdministrativeRole.TriggerExecutionSpecificArea );
                adminPoints.add( sap );

                continue;
            }

            if ( role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA )
                || role.equalsIgnoreCase( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) )
            {
                AdministrativePoint iap = new InnerAdministrativePoint( dn, uuid,
                    AdministrativeRole.TriggerExecutionInnerArea );
                adminPoints.add( iap );

                continue;
            }
        }

        return adminPoints;
    }


    private AdministrativePoint getParent( AdministrativePoint ap, List<AdministrativePoint> aps,
        AdministrativeRole role, DnNode<List<AdministrativePoint>> currentNode )
    {
        AdministrativePoint parent = null;

        for ( AdministrativePoint adminPoint : aps )
        {
            if ( adminPoint.isAutonomous() || ( adminPoint.getRole() == ap.getRole() ) )
            {
                // Same role or AP : this is the parent
                return adminPoint;
            }
            else if ( adminPoint.getRole() == role )
            {
                parent = adminPoint;
            }
        }

        if ( parent != null )
        {
            return parent;
        }

        // We have to go down one level
        if ( currentNode.hasParent() )
        {
            return findParent( ap, currentNode );
        }
        else
        {
            return null;
        }
    }


    /**
     * Find the parent for the given administrative point. If the AP is an AAP, the parent will be the closest
     * AAP or the closest SAP. If we have a SAP between the added AAP and a AAP, then 
     */
    private AdministrativePoint findParent( AdministrativePoint ap, DnNode<List<AdministrativePoint>> currentNode )
    {
        List<AdministrativePoint> aps = currentNode.getElement();

        if ( aps != null )
        {
            // Check if the current element is a valid parent
            switch ( ap.getRole() )
            {
                case AutonomousArea:
                    AdministrativePoint currentAp = aps.get( 0 );

                    if ( currentAp.isAutonomous() )
                    {
                        return currentAp;
                    }
                    else
                    {
                        // We have to go down one level, as an AAP
                        // must have another AAP as a parent
                        if ( currentNode.hasParent() )
                        {
                            return findParent( ap, currentNode );
                        }
                        else
                        {
                            return null;
                        }
                    }

                case AccessControlInnerArea:
                    return getParent( ap, aps, AdministrativeRole.AccessControlSpecificArea, currentNode );

                case CollectiveAttributeInnerArea:
                    return getParent( ap, aps, AdministrativeRole.CollectiveAttributeSpecificArea, currentNode );

                case TriggerExecutionInnerArea:
                    return getParent( ap, aps, AdministrativeRole.TriggerExecutionSpecificArea, currentNode );

                case AccessControlSpecificArea:
                    return getParent( ap, aps, AdministrativeRole.AccessControlSpecificArea, currentNode );

                case CollectiveAttributeSpecificArea:
                    return getParent( ap, aps, AdministrativeRole.CollectiveAttributeSpecificArea, currentNode );

                case SubSchemaSpecificArea:
                    return getParent( ap, aps, AdministrativeRole.SubSchemaSpecificArea, currentNode );

                case TriggerExecutionSpecificArea:
                    return getParent( ap, aps, AdministrativeRole.TriggerExecutionSpecificArea, currentNode );

                default:
                    return null;
            }
        }
        else
        {
            if ( currentNode.hasParent() )
            {
                return findParent( ap, currentNode.getParent() );
            }
            else
            {
                return null;
            }
        }
    }


    /**
     * Creates an Administrative service interceptor.
     */
    public AdministrativePointInterceptor()
    {
    }


    //-------------------------------------------------------------------------------------------
    // Helper methods
    //-------------------------------------------------------------------------------------------
    private List<Entry> getAdministrativePoints() throws LdapException
    {
        List<Entry> entries = new ArrayList<Entry>();

        DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN, schemaManager );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.ADMINISTRATIVE_ROLE_AT, SchemaConstants.ENTRY_UUID_AT } );

        // Search for all the adminstrativePoints in the base
        ExprNode filter = new PresenceNode( ADMINISTRATIVE_ROLE_AT );

        CoreSession adminSession = new DefaultCoreSession( new LdapPrincipal( adminDn, AuthenticationLevel.STRONG ),
            directoryService );

        SearchOperationContext searchOperationContext = new SearchOperationContext( adminSession, DN.EMPTY_DN, filter,
            controls );

        searchOperationContext.setAliasDerefMode( AliasDerefMode.NEVER_DEREF_ALIASES );

        EntryFilteringCursor results = nexus.search( searchOperationContext );

        try
        {
            while ( results.next() )
            {
                Entry entry = results.get();

                entries.add( entry );
            }
        }
        catch ( Exception e )
        {
            throw new LdapOperationException( e.getMessage() );
        }

        return entries;
    }


    /**
     * Tells if a given role is a valid administrative role. We check the lower cased
     * and trimmed value, and also the OID value.
     */
    private boolean isValidRole( String role )
    {
        return ROLES.contains( StringTools.toLowerCase( StringTools.trim( role ) ) );
    }


    /**
     * Update The Administrative Points cache, adding the given AdminPoint
     */
    private void addAdminPointCache( EntryAttribute adminPoint, AddOperationContext addContext ) throws LdapException
    {
        // Now, update the cache
        String uuid = addContext.getEntry().get( ENTRY_UUID_AT ).getString();

        // Construct the AdministrativePoint objects
        List<AdministrativePoint> administrativePoints = createAdministrativePoints( adminPoint, addContext.getDn(),
            uuid );

        // Store the APs in the AP cache
        adminPointCache.add( addContext.getDn(), administrativePoints );

        System.out.println( "After addition : " + adminPointCache );
    }


    /**
     * Update The Administrative Points cache, adding the given AdminPoints
     */
    private void addAdminPointCache( List<Entry> adminPointEntries ) throws LdapException
    {
        for ( Entry adminPointEntry : adminPointEntries )
        {
            // update the cache
            DN dn = adminPointEntry.getDn();

            String uuid = adminPointEntry.get( ENTRY_UUID_AT ).getString();

            List<AdministrativePoint> currentAdminPoints = adminPointCache.getElement( dn );

            List<AdministrativePoint> administrativePoints = createAdministrativePoints( adminPointEntry, uuid );

            for ( AdministrativePoint administrativePoint : administrativePoints )
            {
                currentAdminPoints.add( administrativePoint );
            }

            // Store the APs in the AP cache
            adminPointCache.add( dn, currentAdminPoints );
        }
    }


    /**
     * Update The Administrative Points cache, removing the given AdminPoint
     */
    private void deleteAdminPointCache( EntryAttribute adminPoint, DeleteOperationContext deleteContext )
        throws LdapException
    {
        // Store the APs in the AP cache
        adminPointCache.remove( deleteContext.getDn() );

        System.out.println( "After deletion : " + adminPointCache );
    }


    //-------------------------------------------------------------------------------------------
    // Interceptor initialization
    //-------------------------------------------------------------------------------------------
    /**
     * Registers and initializes all {@link Authenticator}s to this service.
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        LOG.debug( "Initializing the AdministrativeInterceptor" );

        this.directoryService = directoryService;

        schemaManager = directoryService.getSchemaManager();
        nexus = directoryService.getPartitionNexus();

        // Init the At we use locally
        ADMINISTRATIVE_ROLE_AT = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );

        // Load all the AdministratvePoint :
        // Autonomous Administrative Point first, then Specific
        // administrative point, finally the Inner administrative Point
        DN adminDn = new DN( ServerDNConstants.ADMIN_SYSTEM_DN, schemaManager );

        SearchControls controls = new SearchControls();
        controls.setSearchScope( SearchControls.SUBTREE_SCOPE );
        controls.setReturningAttributes( new String[]
            { SchemaConstants.ADMINISTRATIVE_ROLE_AT } );

        // Create the root AdministrativePoint cache. The first DN
        adminPointCache = new DnNode<List<AdministrativePoint>>();

        // get the list of all the AAPs
        List<Entry> administrativePoints = getAdministrativePoints();
        addAdminPointCache( administrativePoints );
    }


    /**
     * {@inheritDoc}
     */
    public void destroy()
    {
    }


    /**
     * Add an administrative point into the DIT.
     * 
     * We have to deal with some specific cases :
     * <ul>
     * <li>If it's an AA, then the added role should be the only one</li>
     * <li>It's not possible to add IA and SA at the same time</li>
     * 
     * @param next The next {@link Interceptor} in the chain
     * @param addContext The {@link AddOperationContext} instance
     * @throws LdapException If we had some error while processing the Add operation
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        Entry entry = addContext.getEntry();

        // Check if we are adding an Administrative Point
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        if ( adminPoint == null )
        {
            // Nope, go on.
            next.add( addContext );

            LOG.debug( "Exit from Administrative Interceptor" );

            return;
        }

        LOG.debug( "Addition of an administrative point at {} for the role {}", entry.getDn(), adminPoint );

        // Check that the added AdministrativeRoles are valid
        for ( Value<?> role : adminPoint )
        {
            if ( !isValidRole( role.getString() ) )
            {
                String message = "Cannot add the given role, it's not a valid one :" + role;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
        }

        // Now we are trying to add an Administrative point. We have to check that the added
        // AP is correct if it's an AAP : it should not have any other role
        if ( adminPoint.contains( SchemaConstants.AUTONOMOUS_AREA ) )
        {
            if ( adminPoint.size() > 1 )
            {
                String message = "Cannot add an Autonomous Administratve Point when some other" + " roles are added : "
                    + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
            else
            {
                // Ok, we can add the AAP
                LOG.debug( "Adding an Autonomous Administrative Point at {}", entry.getDn() );

                next.add( addContext );

                // Now, update the AdminPoint cache
                addAdminPointCache( adminPoint, addContext );

                LOG.debug( "Added an Autonomous Administrative Point at {}", entry.getDn() );

                return;
            }
        }

        // check that we can't mix Inner and Specific areas
        if ( ( ( adminPoint.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA ) || adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID ) ) && ( adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA ) || adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) ) )
            || ( ( adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA ) || adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID ) ) && ( adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA ) || adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) ) )
            || ( ( adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA ) || adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID ) ) && ( adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA ) || adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) ) ) )
        {
            // This is inconsistent
            String message = "Cannot add a specific Administrative Point and the same"
                + " inner Administrative point at the same time : " + adminPoint;
            LOG.error( message );
            throw new LdapUnwillingToPerformException( message );
        }

        // Check that we don't add the same role twice now
        Set<String> seenRoles = new HashSet<String>();

        for ( Value<?> role : adminPoint )
        {
            String trimmedRole = StringTools.toLowerCase( StringTools.trim( role.getString() ) );

            if ( seenRoles.contains( trimmedRole ) )
            {
                // Already seen : an error
                String message = "The role " + role.getString() + " has already been seen.";
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }

            // Add the role and its OID into the seen roles
            seenRoles.add( trimmedRole );
            seenRoles.add( ROLES_OID.get( trimmedRole ) );
        }

        // Ok, we are golden.
        next.add( addContext );

        // Now, update the AdminPoint cache
        addAdminPointCache( adminPoint, addContext );

        LOG.debug( "Added an Administrative Point at {}", entry.getDn() );

        return;
    }


    /**
     * {@inheritDoc}
     */
    public void delete( NextInterceptor next, DeleteOperationContext deleteContext ) throws LdapException
    {
        Entry entry = deleteContext.getEntry();

        // Check if we are deleting an Administrative Point
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        if ( adminPoint == null )
        {
            // Nope, go on.
            next.delete( deleteContext );

            LOG.debug( "Exit from Administrative Interceptor" );

            return;
        }

        LOG.debug( "Deletion of an administrative point at {} for the role {}", entry.getDn(), adminPoint );

        // Check that the removed AdministrativeRoles are valid
        for ( Value<?> role : adminPoint )
        {
            if ( !isValidRole( role.getString() ) )
            {
                String message = "Cannot remove the given role, it's not a valid one :" + role;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
        }

        // Now we are trying to remove an Administrative point. We have to check that the removed
        // AP is correct if it's an AAP : it should not have any other role
        if ( adminPoint.contains( SchemaConstants.AUTONOMOUS_AREA ) )
        {
            if ( adminPoint.size() > 1 )
            {
                String message = "Cannot remove an Autonomous Administratve Point when some other"
                    + " roles are removed : " + adminPoint;
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }
            else
            {
                // Ok, we can remove the AAP
                LOG.debug( "Deleting an Autonomous Administrative Point at {}", entry.getDn() );

                next.delete( deleteContext );

                // Now, update the AdminPoint cache
                deleteAdminPointCache( adminPoint, deleteContext );

                LOG.debug( "Deleted an Autonomous Administrative Point at {}", entry.getDn() );

                return;
            }
        }

        // check that we can't mix Inner and Specific areas
        if ( ( ( adminPoint.contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA ) || adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_SPECIFIC_AREA_OID ) ) && ( adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA ) || adminPoint
            .contains( SchemaConstants.ACCESS_CONTROL_INNER_AREA_OID ) ) )
            || ( ( adminPoint.contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA ) || adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_SPECIFIC_AREA_OID ) ) && ( adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA ) || adminPoint
                .contains( SchemaConstants.COLLECTIVE_ATTRIBUTE_INNER_AREA_OID ) ) )
            || ( ( adminPoint.contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA ) || adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_SPECIFIC_AREA_OID ) ) && ( adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA ) || adminPoint
                .contains( SchemaConstants.TRIGGER_EXECUTION_INNER_AREA_OID ) ) ) )
        {
            // This is inconsistent
            String message = "Cannot delete a specific Administrative Point and the same"
                + " inner Administrative point at the same time : " + adminPoint;
            LOG.error( message );
            throw new LdapUnwillingToPerformException( message );
        }

        // Check that we don't delete the same role twice now
        Set<String> seenRoles = new HashSet<String>();

        for ( Value<?> role : adminPoint )
        {
            String trimmedRole = StringTools.toLowerCase( StringTools.trim( role.getString() ) );

            if ( seenRoles.contains( trimmedRole ) )
            {
                // Already seen : an error
                String message = "The role " + role.getString() + " has already been seen.";
                LOG.error( message );
                throw new LdapUnwillingToPerformException( message );
            }

            // Add the role and its OID into the seen roles
            seenRoles.add( trimmedRole );
            seenRoles.add( ROLES_OID.get( trimmedRole ) );
        }

        // Ok, we are golden.
        next.delete( deleteContext );

        // Now, update the AdminPoint cache
        deleteAdminPointCache( adminPoint, deleteContext );

        LOG.debug( "Deleted an Administrative Point at {}", entry.getDn() );

        return;
    }


    /**
     * Only the add and remove modifications are fully supported.
     * {@inheritDoc}
     */
    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        // We have to check that the modification is acceptable
        List<Modification> modifications = modifyContext.getModItems();

        EntryAttribute modifiedAdminRole = ( modifyContext.getEntry() ).getOriginalEntry().get( ADMINISTRATIVE_ROLE_AT );

        for ( Modification modification : modifications )
        {
            EntryAttribute attribute = modification.getAttribute();

            if ( attribute.getAttributeType() != ADMINISTRATIVE_ROLE_AT )
            {
                continue;
            }

            // Ok, we have a modification impacting the administrative role
            // Apply it to a virtual AdministrativeRole attribute
            switch ( modification.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    if ( modifiedAdminRole == null )
                    {
                        // Create the attribute
                        modifiedAdminRole = new DefaultEntryAttribute( ADMINISTRATIVE_ROLE_AT, attribute.get() );
                        break;
                    }

                    for ( Value<?> value : attribute )
                    {
                        String role = StringTools.toLowerCase( StringTools.trim( value.getString() ) );

                        if ( !isValidRole( role ) )
                        {
                            // Not a valid role : we will throw an exception
                            String msg = "Invalid role : " + value;
                            LOG.error( msg );
                            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
                        }

                        // At this point, we know that the attribute's syntax is correct
                        // We just have to check that the current attribute does not
                        // contains the value already
                        if ( modifiedAdminRole.contains( value ) )
                        {
                            // This is an error.
                            String msg = I18n.err( I18n.ERR_54, value );
                            LOG.error( msg );
                            throw new LdapAttributeInUseException( msg );
                        }

                        // Forbid the addition of an InnerArea if the same SpecificArea
                        // already exists
                        if ( isInnerArea( role ) && hasSpecificArea( role, modifiedAdminRole ) )
                        {
                            // Not a valid role : we will throw an exception
                            String msg = "Cannot add an Inner Area ole to an AdministrativePoint which already has the same Specific Area role "
                                + value;
                            LOG.error( msg );
                            throw new LdapUnwillingToPerformException( msg );
                        }

                        // Add the role to the modified attribute
                        modifiedAdminRole.add( value );
                    }

                    break;

                case REMOVE_ATTRIBUTE:
                    if ( modifiedAdminRole == null )
                    {
                        // We can't remove a value when the attribute does not exist.
                        String msg = "Cannot remove the administrative role, it does not exist";
                        LOG.error( msg );
                        throw new LdapNoSuchAttributeException( msg );
                    }

                    // It may be a complete removal
                    if ( attribute.size() == 0 )
                    {
                        // Complete removal
                        modifiedAdminRole = null;
                        break;
                    }

                    // Now deal with the values to remove
                    for ( Value<?> value : attribute )
                    {
                        if ( !isValidRole( value.getString() ) )
                        {
                            // Not a valid role : we will throw an exception
                            String msg = "Invalid role : " + value.getString();
                            LOG.error( msg );
                            throw new LdapInvalidAttributeValueException( ResultCodeEnum.INVALID_ATTRIBUTE_SYNTAX, msg );
                        }

                        if ( !modifiedAdminRole.contains( value ) )
                        {
                            // We can't remove a value if it does not exist !
                            String msg = "Cannot remove the administrative role value" + value + ", it does not exist";
                            LOG.error( msg );
                            throw new LdapNoSuchAttributeException( msg );
                        }

                        modifiedAdminRole.remove( value );
                    }

                    break;

                case REPLACE_ATTRIBUTE:
                    // Not supported
                    String msg = "Cannot replace an administrative role, the opertion is not supported";
                    LOG.error( msg );
                    throw new LdapUnwillingToPerformException( msg );
            }
        }

        next.modify( modifyContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        Entry entry = moveContext.getOriginalEntry();

        // Check if we are moving an Administrative Point
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        if ( adminPoint == null )
        {
            // Nope, go on.
            next.move( moveContext );

            LOG.debug( "Exit from Administrative Interceptor" );

            return;
        }

        // Else throw an UnwillingToPerform exception ATM
        String message = "Cannot move an Administrative Point in the current version";
        LOG.error( message );
        throw new LdapUnwillingToPerformException( message );
    }


    /**
     * {@inheritDoc}
     */
    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {
        Entry entry = moveAndRenameContext.getOriginalEntry();

        // Check if we are moving and renaming an Administrative Point
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        if ( adminPoint == null )
        {
            // Nope, go on.
            next.moveAndRename( moveAndRenameContext );

            LOG.debug( "Exit from Administrative Interceptor" );

            return;
        }

        // Else throw an UnwillingToPerform exception ATM
        String message = "Cannot move and rename an Administrative Point in the current version";
        LOG.error( message );
        throw new LdapUnwillingToPerformException( message );
    }


    /**
     * {@inheritDoc}
     */
    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        Entry entry = renameContext.getEntry();

        // Check if we are renaming an Administrative Point
        EntryAttribute adminPoint = entry.get( ADMINISTRATIVE_ROLE_AT );

        if ( adminPoint == null )
        {
            // Nope, go on.
            next.rename( renameContext );

            LOG.debug( "Exit from Administrative Interceptor" );

            return;
        }

        // Else throw an UnwillingToPerform exception ATM
        String message = "Cannot rename an Administrative Point in the current version";
        LOG.error( message );
        throw new LdapUnwillingToPerformException( message );
    }
}
