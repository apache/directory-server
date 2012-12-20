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
package org.apache.directory.server.core.api.subtree;


import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultAttribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.schema.AttributeType;
import org.apache.directory.api.ldap.model.schema.SchemaManager;
import org.apache.directory.api.ldap.model.subtree.Subentry;
import org.apache.directory.api.ldap.model.subtree.SubtreeSpecification;
import org.apache.directory.server.core.api.DirectoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SubentryUtils
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( SubentryUtils.class );

    /** A reference to the DirectoryService instance */
    protected DirectoryService directoryService;

    /** A reference to the SchemaManager instance */
    protected SchemaManager schemaManager;

    /** The AccessControlSubentries AttributeType */
    protected static AttributeType ACCESS_CONTROL_SUBENTRIES_AT;

    /** The CollectiveAttributeSubentries AttributeType */
    protected static AttributeType COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT;

    /** A reference to the AccessControlSubentries AT */
    protected static AttributeType SUBSCHEMA_SUBENTRY_AT;

    /** A reference to the TriggerExecutionSubentries AT */
    protected static AttributeType TRIGGER_EXECUTION_SUBENTRIES_AT;


    public SubentryUtils( DirectoryService directoryService )
    {
        this.directoryService = directoryService;
        this.schemaManager = directoryService.getSchemaManager();

        // Init the At we use locally
        ACCESS_CONTROL_SUBENTRIES_AT = schemaManager.getAttributeType( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
        COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT = schemaManager
            .getAttributeType( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        SUBSCHEMA_SUBENTRY_AT = schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        TRIGGER_EXECUTION_SUBENTRIES_AT = schemaManager
            .getAttributeType( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
    }


    //-------------------------------------------------------------------------------------------
    // Shared method
    //-------------------------------------------------------------------------------------------
    /**
     * Evaluates the set of subentry subtrees upon an entry and returns the
     * operational subentry attributes that will be added to the entry if
     * added at the dn specified.
     *
     * @param dn the normalized distinguished name of the entry
     * @param entryAttrs the entry attributes are generated for
     * @return the set of subentry op attrs for an entry
     * @throws Exception if there are problems accessing entry information
     */
    public Entry getSubentryAttributes( Dn dn, Entry entryAttrs ) throws LdapException
    {
        Entry subentryAttrs = new DefaultEntry( schemaManager, dn );

        SubentryCache subentryCache = directoryService.getSubentryCache();
        SubtreeEvaluator evaluator = directoryService.getEvaluator();

        for ( Dn subentryDn : subentryCache )
        {
            Dn apDn = subentryDn.getParent();
            Subentry subentry = subentryCache.getSubentry( subentryDn );
            SubtreeSpecification ss = subentry.getSubtreeSpecification();

            if ( evaluator.evaluate( ss, apDn, dn, entryAttrs ) )
            {
                Attribute operational;

                if ( subentry.isAccessControlAdminRole() )
                {
                    operational = subentryAttrs.get( ACCESS_CONTROL_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultAttribute( ACCESS_CONTROL_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isSchemaAdminRole() )
                {
                    operational = subentryAttrs.get( SUBSCHEMA_SUBENTRY_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultAttribute( SUBSCHEMA_SUBENTRY_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isCollectiveAdminRole() )
                {
                    operational = subentryAttrs.get( COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultAttribute( COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }

                if ( subentry.isTriggersAdminRole() )
                {
                    operational = subentryAttrs.get( TRIGGER_EXECUTION_SUBENTRIES_AT );

                    if ( operational == null )
                    {
                        operational = new DefaultAttribute( TRIGGER_EXECUTION_SUBENTRIES_AT );
                        subentryAttrs.put( operational );
                    }

                    operational.add( subentryDn.getNormName() );
                }
            }
        }

        return subentryAttrs;
    }
}
