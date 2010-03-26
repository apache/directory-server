/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.directory.server.core.changelog;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.directory.server.constants.ApacheSchemaConstants;
import org.apache.directory.server.core.DirectoryService;
import org.apache.directory.server.core.entry.ClonedServerEntry;
import org.apache.directory.server.core.entry.ServerEntryUtils;
import org.apache.directory.server.core.interceptor.BaseInterceptor;
import org.apache.directory.server.core.interceptor.NextInterceptor;
import org.apache.directory.server.core.interceptor.context.AddOperationContext;
import org.apache.directory.server.core.interceptor.context.DeleteOperationContext;
import org.apache.directory.server.core.interceptor.context.ModifyOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveAndRenameOperationContext;
import org.apache.directory.server.core.interceptor.context.MoveOperationContext;
import org.apache.directory.server.core.interceptor.context.OperationContext;
import org.apache.directory.server.core.interceptor.context.RenameOperationContext;
import org.apache.directory.server.core.partition.ByPassConstants;
import org.apache.directory.server.core.schema.SchemaService;
import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ServerEntry;
import org.apache.directory.shared.ldap.entry.ServerModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientEntry;
import org.apache.directory.shared.ldap.ldif.ChangeType;
import org.apache.directory.shared.ldap.ldif.LdifEntry;
import org.apache.directory.shared.ldap.ldif.LdifRevertor;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.apache.directory.shared.ldap.schema.AttributeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * An interceptor which intercepts write operations to the directory and
 * logs them with the server's ChangeLog service.
 * Note: Adding/deleting a tag is not recorded as a change
 * 
 * @org.apache.xbean.XBean
 */
public class ChangeLogInterceptor extends BaseInterceptor
{
    /** for debugging */
    private static final Logger LOG = LoggerFactory.getLogger( ChangeLogInterceptor.class );
    
    /** used to ignore modify operations to tombstone entries */
    private AttributeType entryDeleted;
    
    /** the changelog service to log changes to */
    private ChangeLog changeLog;
    
    /** we need the schema service to deal with special conditions */
    private SchemaService schemaService;

    /** OID of the 'rev' attribute used in changeLogEvent and tag objectclasses */
    private static final String REV_AT_OID = "1.3.6.1.4.1.18060.0.4.1.2.47";
    
    // -----------------------------------------------------------------------
    // Overridden init() and destroy() methods
    // -----------------------------------------------------------------------


    /**
     * The init method will initialize the local variables and load the 
     * entryDeleted AttributeType.
     */
    public void init( DirectoryService directoryService ) throws Exception
    {
        super.init( directoryService );

        changeLog = directoryService.getChangeLog();
        schemaService = directoryService.getSchemaService();
        entryDeleted = directoryService.getSchemaManager()
                .lookupAttributeTypeRegistry( ApacheSchemaConstants.ENTRY_DELETED_AT_OID );
    }


    // -----------------------------------------------------------------------
    // Overridden (only change inducing) intercepted methods
    // -----------------------------------------------------------------------
    

    public void add( NextInterceptor next, AddOperationContext opContext ) throws Exception
    {
        next.add( opContext );

        if ( ! changeLog.isEnabled() || ! opContext.isFirstOperation() )
        {
            return;
        }

        ServerEntry addEntry = opContext.getEntry();

        // we don't want to record addition of a tag as a change
        if( addEntry.get( REV_AT_OID ) != null )
        {
           return; 
        }
        
        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Add );
        forward.setDn( opContext.getDn() );

        Set<AttributeType> list = addEntry.getAttributeTypes();
        
        for ( AttributeType attributeType:list )
        {
            forward.addAttribute( addEntry.get( attributeType).toClientAttribute() );
        }
        
        LdifEntry reverse = LdifRevertor.reverseAdd( opContext.getDn() );
        opContext.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverse ) );
    }


    /**
     * The delete operation has to be stored with a way to restore the deleted element.
     * There is no way to do that but reading the entry and dump it into the LOG.
     */
    public void delete( NextInterceptor next, DeleteOperationContext opContext ) throws Exception
    {
        // @todo make sure we're not putting in operational attributes that cannot be user modified
        // must save the entry if change log is enabled
        ServerEntry serverEntry = null;

        if ( changeLog.isEnabled() && opContext.isFirstOperation() )
        {
            serverEntry = getAttributes( opContext );
        }

        next.delete( opContext );

        if ( ! changeLog.isEnabled() || ! opContext.isFirstOperation() )
        {
            return;
        }

        // we don't want to record deleting a tag as a change
        if( serverEntry.get( REV_AT_OID ) != null )
        {
           return; 
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Delete );
        forward.setDn( opContext.getDn() );
        
        Entry reverseEntry = new DefaultClientEntry( serverEntry.getDn() );

        for ( EntryAttribute attribute : serverEntry )
        {
            // filter collective attributes, they can't be added by the revert operation
            AttributeType at = schemaService.getSchemaManager().getAttributeTypeRegistry().lookup( attribute.getId() );
            if ( !at.isCollective() )
            {
                reverseEntry.add( attribute.toClientAttribute() );
            }
        }

        LdifEntry reverse = LdifRevertor.reverseDel( opContext.getDn(), reverseEntry );
        opContext.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverse ) );
    }


    /**
     * Gets attributes required for modifications.
     *
     * @param dn the dn of the entry to get
     * @return the entry's attributes (may be immutable if the schema subentry)
     * @throws Exception on error accessing the entry's attributes
     */
    private ServerEntry getAttributes( OperationContext opContext ) throws Exception
    {
        DN dn = opContext.getDn();
        ClonedServerEntry serverEntry;

        // @todo make sure we're not putting in operational attributes that cannot be user modified
        if ( schemaService.isSchemaSubentry( dn.getNormName() ) )
        {
            return schemaService.getSubschemaEntryCloned();
        }
        else
        {
            serverEntry = opContext.lookup( dn, ByPassConstants.LOOKUP_BYPASS );
        }

        return serverEntry;
    }


    /**
     * 
     */
    public void modify( NextInterceptor next, ModifyOperationContext opContext ) throws Exception
    {
        ServerEntry serverEntry = null;
        Modification modification = ServerEntryUtils.getModificationItem( opContext.getModItems(), entryDeleted );
        boolean isDelete = ( modification != null );

        if ( ! isDelete && ( changeLog.isEnabled() && opContext.isFirstOperation() ) )
        {
            // @todo make sure we're not putting in operational attributes that cannot be user modified
            serverEntry = getAttributes( opContext );
        }
        
        // Duplicate modifications so that the reverse does not contain the operational attributes
        List<Modification> clonedMods = new ArrayList<Modification>(); 

        for ( Modification mod : opContext.getModItems() )
        {
            clonedMods.add( mod.clone() );
        }

        // Call the next interceptor
        next.modify( opContext );

        // @TODO: needs big consideration!!!
        // NOTE: perhaps we need to log this as a system operation that cannot and should not be reapplied?
        if ( 
            isDelete ||   
            ! changeLog.isEnabled() || 
            ! opContext.isFirstOperation() ||
            
         // if there are no modifications due to stripping out bogus non-
         // existing attributes then we will have no modification items and
         // should ignore not this without registering it with the changelog
         
            opContext.getModItems().size() == 0 )  
        {
            if ( isDelete )
            {
                LOG.debug( "Bypassing changelog on modify of entryDeleted attribute." );
            }
            
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.Modify );
        forward.setDn( opContext.getDn() );
        
        List<Modification> mods = new ArrayList<Modification>( clonedMods.size() );
        
        for ( Modification modItem : clonedMods )
        {
            Modification mod = ((ServerModification)modItem).toClientModification();
            
            // TODO: handle correctly http://issues.apache.org/jira/browse/DIRSERVER-1198
            mod.getAttribute().setId( modItem.getAttribute().getId() );
            mods.add( mod );
            
            forward.addModificationItem( mod );
        }
        
        Entry clientEntry = new DefaultClientEntry( serverEntry.getDn() );
        
        for ( EntryAttribute attribute:serverEntry )
        {
            clientEntry.add( attribute.toClientAttribute() );
        }

        LdifEntry reverse = LdifRevertor.reverseModify( 
            opContext.getDn(), 
            mods, 
            clientEntry );
        
        opContext.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverse ) );
    }


    // -----------------------------------------------------------------------
    // Though part left as an exercise (Not Any More!)
    // -----------------------------------------------------------------------


    public void rename ( NextInterceptor next, RenameOperationContext renameContext ) throws Exception
    {
        ServerEntry serverEntry = null;
        
        if ( renameContext.getEntry() != null )
        {
            serverEntry = renameContext.getEntry().getOriginalEntry();
        }
        
        next.rename( renameContext );
        
        // After this point, the entry has been modified. The cloned entry contains
        // the modified entry, the originalEntry has changed

        if ( ! changeLog.isEnabled() || ! renameContext.isFirstOperation() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModRdn );
        forward.setDn( renameContext.getDn() );
        forward.setNewRdn( renameContext.getNewRdn().getName() );
        forward.setDeleteOldRdn( renameContext.getDelOldDn() );

        List<LdifEntry> reverses = LdifRevertor.reverseRename( 
            serverEntry, renameContext.getNewRdn(), renameContext.getDelOldDn() );
        
        renameContext.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverses ) );
    }


    public void moveAndRename( NextInterceptor next, MoveAndRenameOperationContext opCtx )
        throws Exception
    {
        ClonedServerEntry serverEntry = null;
        
        if ( changeLog.isEnabled() && opCtx.isFirstOperation() )
        {
            // @todo make sure we're not putting in operational attributes that cannot be user modified
            serverEntry = opCtx.lookup( opCtx.getDn(), ByPassConstants.LOOKUP_BYPASS );
        }

        next.moveAndRename( opCtx );

        if ( ! changeLog.isEnabled() || ! opCtx.isFirstOperation() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( opCtx.getDn() );
        forward.setDeleteOldRdn( opCtx.getDelOldDn() );
        forward.setNewRdn( opCtx.getNewRdn().getName() );
        forward.setNewSuperior( opCtx.getParent().getName() );

        List<LdifEntry> reverses = LdifRevertor.reverseMoveAndRename(  
            serverEntry, opCtx.getParent(), new RDN( opCtx.getNewRdn() ), false );
        opCtx.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverses ) );
    }


    public void move ( NextInterceptor next, MoveOperationContext opCtx ) throws Exception
    {
        next.move( opCtx );

        if ( ! changeLog.isEnabled() || ! opCtx.isFirstOperation() )
        {
            return;
        }

        LdifEntry forward = new LdifEntry();
        forward.setChangeType( ChangeType.ModDn );
        forward.setDn( opCtx.getDn() );
        forward.setNewSuperior( opCtx.getParent().getName() );

        LdifEntry reverse = LdifRevertor.reverseMove( opCtx.getParent(), opCtx.getDn() );
        opCtx.setChangeLogEvent( changeLog.log( getPrincipal(), forward, reverse ) );
    }
}
