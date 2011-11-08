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
package org.apache.directory.server.core.api.interceptor;


import java.util.HashSet;
import java.util.Set;

import org.apache.directory.server.core.api.DirectoryService;
import org.apache.directory.server.core.api.LdapPrincipal;
import org.apache.directory.server.core.api.filtering.EntryFilteringCursor;
import org.apache.directory.server.core.api.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.api.interceptor.context.BindOperationContext;
import org.apache.directory.server.core.api.interceptor.context.CompareOperationContext;
import org.apache.directory.server.core.api.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.api.interceptor.context.EntryOperationContext;
import org.apache.directory.server.core.api.interceptor.context.GetRootDSEOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ListOperationContext;
import org.apache.directory.server.core.api.interceptor.context.LookupOperationContext;
import org.apache.directory.server.core.api.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.api.interceptor.context.OperationContext;
import org.apache.directory.server.core.api.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.api.interceptor.context.SearchOperationContext;
import org.apache.directory.server.core.api.interceptor.context.UnbindOperationContext;
import org.apache.directory.server.core.api.partition.PartitionNexus;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.entry.Entry;
import org.apache.directory.shared.ldap.model.exception.LdapException;
import org.apache.directory.shared.ldap.model.schema.AttributeType;
import org.apache.directory.shared.ldap.model.schema.SchemaManager;


/**
 * A easy-to-use implementation of {@link Interceptor}.  All methods are
 * implemented to pass the flow of control to next interceptor by defaults.
 * Please override the methods you have concern in.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public abstract class BaseInterceptor implements Interceptor
{
    /** A reference to the DirectoryService instance */
    protected DirectoryService directoryService;

    /** A reference to the SchemaManager instance */
    protected SchemaManager schemaManager;

    /** set of operational attribute types used for representing the password policy state of a user entry */
    protected static final Set<AttributeType> PWD_POLICY_STATE_ATTRIBUTE_TYPES = new HashSet<AttributeType>();

    /** The AccessControlSubentries AttributeType */
    protected static AttributeType ACCESS_CONTROL_SUBENTRIES_AT;

    /** A reference to the AdministrativeRole AT */
    protected static AttributeType ADMINISTRATIVE_ROLE_AT;

    /** The CollectiveAttributeSubentries AttributeType */
    protected static AttributeType COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT;

    /** The CollectiveExclusions AttributeType */
    protected static AttributeType COLLECTIVE_EXCLUSIONS_AT;

    /** A storage for the entryACI attributeType */
    protected static AttributeType ENTRY_ACI_AT;

    /** A reference to the EntryCSN AT */
    protected static AttributeType ENTRY_CSN_AT;

    /** A reference to the EntryUUID AT */
    protected static AttributeType ENTRY_UUID_AT;

    /** A reference to the ModifiersName AT */
    protected static AttributeType MODIFIERS_NAME_AT;

    /** A reference to the ModifyTimestamp AT */
    protected static AttributeType MODIFY_TIMESTAMP_AT;

    /** The ObjectClass AttributeType */
    protected static AttributeType OBJECT_CLASS_AT;

    /** the subentry ACI attribute type */
    protected static AttributeType SUBENTRY_ACI_AT;

    /** A reference to the AccessControlSubentries AT */
    protected static AttributeType SUBSCHEMA_SUBENTRY_AT;

    /** A reference to the SubtreeSpecification AT */
    protected static AttributeType SUBTREE_SPECIFICATION_AT;

    /** A reference to the TriggerExecutionSubentries AT */
    protected static AttributeType TRIGGER_EXECUTION_SUBENTRIES_AT;

    /** A starage for the uniqueMember attributeType */
    protected static AttributeType UNIQUE_MEMBER_AT;


    /**
     * The final interceptor which acts as a proxy in charge to dialog with the nexus partition.
     */
    private final Interceptor FINAL_INTERCEPTOR = new Interceptor()
    {
        private PartitionNexus nexus;


        public String getName()
        {
            return "FINAL";
        }


        public void init( DirectoryService directoryService )
        {
            this.nexus = directoryService.getPartitionNexus();
        }


        public void destroy()
        {
            // unused
        }


        public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
        {
            return nexus.compare( compareContext );
        }


        /**
         * {@inheritDoc}
         */
        public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext ) throws LdapException
        {
            return nexus.getRootDSE( getRootDseContext );
        }


        /**
         * {@inheritDoc}
         */
        public void delete( DeleteOperationContext deleteContext ) throws LdapException
        {
            nexus.delete( deleteContext );
        }


        public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
        {
            nexus.add( addContext );
        }


        public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
        {
            nexus.modify( modifyContext );
        }


        public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
        {
            return nexus.list( listContext );
        }


        public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext )
            throws LdapException
        {
            return nexus.search( searchContext );
        }


        public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext )
            throws LdapException
        {
            return nexus.lookup( lookupContext );
        }


        public boolean hasEntry( NextInterceptor next, EntryOperationContext hasEntryContext ) throws LdapException
        {
            return nexus.hasEntry( hasEntryContext );
        }


        public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
        {
            nexus.rename( renameContext );
        }


        public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
        {
            nexus.move( moveContext );
        }


        public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
            throws LdapException
        {
            nexus.moveAndRename( moveAndRenameContext );
        }


        public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
        {
            nexus.bind( bindContext );
        }


        /**
         * {@inheritDoc}
         */
        public void unbind( UnbindOperationContext unbindContext ) throws LdapException
        {
            nexus.unbind( unbindContext );
        }
    };


    /**
     * default interceptor name is its class, preventing accidental duplication of interceptors by naming
     * instances differently
     * @return (default, class name) interceptor name
     */
    public String getName()
    {
        return getClass().getSimpleName();
    }


    /**
     * Returns {@link LdapPrincipal} of current context.
     * 
     * @param opContext TODO
     * @return the authenticated principal
     */
    public static LdapPrincipal getPrincipal( OperationContext opContext )
    {
        return opContext.getSession().getEffectivePrincipal();
    }


    /**
     * Creates a new instance.
     */
    protected BaseInterceptor()
    {
    }


    /**
     * This method does nothing by default.
     * @throws Exception 
     */
    public void init( DirectoryService directoryService ) throws LdapException
    {
        this.directoryService = directoryService;
        this.schemaManager = directoryService.getSchemaManager();

        // Init the At we use locally
        ACCESS_CONTROL_SUBENTRIES_AT = schemaManager.getAttributeType( SchemaConstants.ACCESS_CONTROL_SUBENTRIES_AT );
        ADMINISTRATIVE_ROLE_AT = schemaManager.getAttributeType( SchemaConstants.ADMINISTRATIVE_ROLE_AT );
        COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT = schemaManager
            .getAttributeType( SchemaConstants.COLLECTIVE_ATTRIBUTE_SUBENTRIES_AT );
        COLLECTIVE_EXCLUSIONS_AT = schemaManager.getAttributeType( SchemaConstants.COLLECTIVE_EXCLUSIONS_AT );
        ENTRY_ACI_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_ACI_AT_OID );
        ENTRY_CSN_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_CSN_AT );
        ENTRY_UUID_AT = schemaManager.getAttributeType( SchemaConstants.ENTRY_UUID_AT );
        MODIFIERS_NAME_AT = schemaManager.getAttributeType( SchemaConstants.MODIFIERS_NAME_AT );
        MODIFY_TIMESTAMP_AT = schemaManager.getAttributeType( SchemaConstants.MODIFY_TIMESTAMP_AT );
        OBJECT_CLASS_AT = schemaManager.getAttributeType( SchemaConstants.OBJECT_CLASS_AT );
        SUBENTRY_ACI_AT = schemaManager.getAttributeType( SchemaConstants.SUBENTRY_ACI_AT_OID );
        SUBSCHEMA_SUBENTRY_AT = schemaManager.getAttributeType( SchemaConstants.SUBSCHEMA_SUBENTRY_AT );
        SUBTREE_SPECIFICATION_AT = schemaManager.getAttributeType( SchemaConstants.SUBTREE_SPECIFICATION_AT );
        TRIGGER_EXECUTION_SUBENTRIES_AT = schemaManager
            .getAttributeType( SchemaConstants.TRIGGER_EXECUTION_SUBENTRIES_AT );
        UNIQUE_MEMBER_AT = schemaManager.getAttributeType( SchemaConstants.UNIQUE_MEMBER_AT_OID );
        
        FINAL_INTERCEPTOR.init( directoryService );
    }


    /**
     * This method does nothing by default.
     */
    public void destroy()
    {
    }


    /**
     * Computes the next interceptor to call for a given operation. If we find none, 
     * we return the proxy to the nexus.
     * 
     * @param operationContext The operation context
     * @return The next interceptor in the list for this operation
     */
    private Interceptor getNextInterceptor( OperationContext operationContext )
    {
    	String currentInterceptor = operationContext.getNextInterceptor();
    	
    	if ( currentInterceptor.equals( "FINAL" ) )
    	{
    		return FINAL_INTERCEPTOR;
    	}

    	Interceptor interceptor = directoryService.getInterceptor( currentInterceptor );
    	
    	return interceptor;
    }
    

    // ------------------------------------------------------------------------
    // Interceptor's Invoke Method
    // ------------------------------------------------------------------------
    /**
     * {@inheritDoc}
     */
    public void add( NextInterceptor next, AddOperationContext addContext ) throws LdapException
    {
        next.add( addContext );
    }


    /**
     * {@inheritDoc}
     */
    public void delete( DeleteOperationContext deleteContext ) throws LdapException
    {
    	// Do nothing
    }
    

    /**
     * Calls the next interceptor for the delete operation.
     * 
     * @param deleteContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( DeleteOperationContext deleteContext ) throws LdapException
    {
    	Interceptor interceptor = getNextInterceptor( deleteContext );
    	
    	interceptor.delete( deleteContext );
    }

    
    /**
     * {@inheritDoc}
     */
    public Entry getRootDSE( GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
    	// Nothing to do
        return null;
    }
    
    
    /**
     * Calls the next interceptor for the getRootDse operation.
     * 
     * @param deleteContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final Entry next( GetRootDSEOperationContext getRootDseContext ) throws LdapException
    {
    	Interceptor interceptor = getNextInterceptor( getRootDseContext );

        return interceptor.getRootDSE( getRootDseContext );
    }

    
    public boolean hasEntry( NextInterceptor next, EntryOperationContext hasEntryContext ) throws LdapException
    {
        return next.hasEntry( hasEntryContext );
    }


    public EntryFilteringCursor list( NextInterceptor next, ListOperationContext listContext ) throws LdapException
    {
        return next.list( listContext );
    }


    public Entry lookup( NextInterceptor next, LookupOperationContext lookupContext ) throws LdapException
    {
        return next.lookup( lookupContext );
    }


    public void modify( NextInterceptor next, ModifyOperationContext modifyContext ) throws LdapException
    {
        next.modify( modifyContext );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext moveAndRenameContext )
        throws LdapException
    {
        next.moveAndRename( moveAndRenameContext );
    }


    public void rename( NextInterceptor next, RenameOperationContext renameContext ) throws LdapException
    {
        next.rename( renameContext );
    }


    /**
     * {@inheritDoc}
     */
    public void move( NextInterceptor next, MoveOperationContext moveContext ) throws LdapException
    {
        next.move( moveContext );
    }


    public EntryFilteringCursor search( NextInterceptor next, SearchOperationContext searchContext )
        throws LdapException
    {
        return next.search( searchContext );
    }


    public boolean compare( NextInterceptor next, CompareOperationContext compareContext ) throws LdapException
    {
        return next.compare( compareContext );
    }


    public void bind( NextInterceptor next, BindOperationContext bindContext ) throws LdapException
    {
        next.bind( bindContext );
    }


    /**
     * {@inheritDoc}
     */
    public void unbind( UnbindOperationContext unbindContext ) throws LdapException
    {
        // Nothing to do
    }
    
    
    /**
     * Compute the next interceptor for the unbind operation.
     * 
     * @param deleteContext The context in which we are executing this operation
     * @throws LdapException If something went wrong
     */
    protected final void next( UnbindOperationContext unbindContext ) throws LdapException
    {
    	Interceptor interceptor = getNextInterceptor( unbindContext );

        interceptor.unbind( unbindContext );
    }
}
