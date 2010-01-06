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
package org.apache.directory.shared.ldap.ldif;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.NamingException;

import org.apache.directory.shared.ldap.entry.Entry;
import org.apache.directory.shared.ldap.entry.EntryAttribute;
import org.apache.directory.shared.ldap.entry.Modification;
import org.apache.directory.shared.ldap.entry.ModificationOperation;
import org.apache.directory.shared.ldap.entry.client.ClientModification;
import org.apache.directory.shared.ldap.entry.client.DefaultClientAttribute;
import org.apache.directory.shared.ldap.name.AVA;
import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.name.Rdn;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * A helper class which provides methods to reverse a LDIF modification operation. 
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class LdifRevertor
{
    /** Two constants for the deleteOldRdn flag */
    public static final boolean DELETE_OLD_RDN = true;
    public static final boolean KEEP_OLD_RDN = false;
    
    /**
     * Compute a reverse LDIF of an AddRequest. It's simply a delete request
     * of the added entry
     *
     * @param dn the dn of the added entry
     * @return a reverse LDIF
     */
    public static LdifEntry reverseAdd( LdapDN dn )
    {
        LdifEntry entry = new LdifEntry();
        entry.setChangeType( ChangeType.Delete );
        entry.setDn( dn );
        return entry;
    }


    /**
     * Compute a reverse LDIF of a DeleteRequest. We have to get the previous
     * entry in order to restore it.
     *
     * @param dn The deleted entry DN
     * @param deletedEntry The entry which has been deleted
     * @return A reverse LDIF
     */
    public static LdifEntry reverseDel( LdapDN dn, Entry deletedEntry ) throws NamingException
    {
        LdifEntry entry = new LdifEntry();

        entry.setDn( dn );
        entry.setChangeType( ChangeType.Add );

        for ( EntryAttribute attribute : deletedEntry )
        {
            entry.addAttribute( attribute );
        }

        return entry;
    }


    /**
    *
    * Compute the reversed LDIF for a modify request. We will deal with the
    * three kind of modifications :
    * - add
    * - remove
    * - replace
    *
    * As the modifications should be issued in a reversed order ( ie, for
    * the initials modifications {A, B, C}, the reversed modifications will
    * be ordered like {C, B, A}), we will change the modifications order.
    *
    * @param dn the dn of the modified entry
    * @param forwardModifications the modification items for the forward change
    * @param modifiedEntry The modified entry. Necessary for the destructive modifications
    * @return A reversed LDIF
    * @throws NamingException If something went wrong
    */
    public static LdifEntry reverseModify( LdapDN dn, List<Modification> forwardModifications, Entry modifiedEntry )
        throws NamingException
    {
        // First, protect the original entry by cloning it : we will modify it
        Entry clonedEntry = ( Entry ) modifiedEntry.clone();

        LdifEntry entry = new LdifEntry();
        entry.setChangeType( ChangeType.Modify );

        entry.setDn( dn );

        // As the reversed modifications should be pushed in reversed order,
        // we create a list to temporarily store the modifications.
        List<Modification> reverseModifications = new ArrayList<Modification>();

        // Loop through all the modifications. For each modification, we will
        // have to apply it to the modified entry in order to be able to generate
        // the reversed modification
        for ( Modification modification : forwardModifications )
        {
            switch ( modification.getOperation() )
            {
                case ADD_ATTRIBUTE:
                    EntryAttribute mod = modification.getAttribute();

                    EntryAttribute previous = clonedEntry.get( mod.getId() );

                    if ( mod.equals( previous ) )
                    {
                        continue;
                    }

                    Modification reverseModification = new ClientModification( ModificationOperation.REMOVE_ATTRIBUTE,
                        mod );
                    reverseModifications.add( 0, reverseModification );
                    break;

                case REMOVE_ATTRIBUTE:
                    mod = modification.getAttribute();

                    previous = clonedEntry.get( mod.getId() );

                    if ( previous == null )
                    {
                        // Nothing to do if the previous attribute didn't exist
                        continue;
                    }

                    if ( mod.get() == null )
                    {
                        reverseModification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, previous );
                        reverseModifications.add( 0, reverseModification );
                        break;
                    }

                    reverseModification = new ClientModification( ModificationOperation.ADD_ATTRIBUTE, mod );
                    reverseModifications.add( 0, reverseModification );
                    break;

                case REPLACE_ATTRIBUTE:
                    mod = modification.getAttribute();

                    previous = clonedEntry.get( mod.getId() );

                    /*
                     * The server accepts without complaint replace 
                     * modifications to non-existing attributes in the 
                     * entry.  When this occurs nothing really happens
                     * but this method freaks out.  To prevent that we
                     * make such no-op modifications produce the same
                     * modification for the reverse direction which should
                     * do nothing as well.  
                     */
                    if ( ( mod.get() == null ) && ( previous == null ) )
                    {
                        reverseModification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE,
                            new DefaultClientAttribute( mod.getId() ) );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }

                    if ( mod.get() == null )
                    {
                        reverseModification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, previous );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }

                    if ( previous == null )
                    {
                        EntryAttribute emptyAttribute = new DefaultClientAttribute( mod.getId() );
                        reverseModification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE,
                            emptyAttribute );
                        reverseModifications.add( 0, reverseModification );
                        continue;
                    }

                    reverseModification = new ClientModification( ModificationOperation.REPLACE_ATTRIBUTE, previous );
                    reverseModifications.add( 0, reverseModification );
                    break;

                default:
                    break; // Do nothing

            }

            AttributeUtils.applyModification( clonedEntry, modification );

        }

        // Special case if we don't have any reverse modifications
        if ( reverseModifications.size() == 0 )
        {
            throw new IllegalArgumentException( "Could not deduce reverse modifications from provided modifications: "
                + forwardModifications );
        }

        // Now, push the reversed list into the entry
        for ( Modification modification : reverseModifications )
        {
            entry.addModificationItem( modification );
        }

        // Return the reverted entry
        return entry;
    }


    /**
     * Compute a reverse LDIF for a forward change which if in LDIF format
     * would represent a Move operation. Hence there is no newRdn in the
     * picture here.
     *
     * @param newSuperiorDn the new parent dn to be (must not be null)
     * @param modifiedDn the dn of the entry being moved (must not be null)
     * @return a reverse LDIF
     * @throws NamingException if something went wrong
     */
    public static LdifEntry reverseMove( LdapDN newSuperiorDn, LdapDN modifiedDn ) throws NamingException
    {
        LdifEntry entry = new LdifEntry();
        LdapDN currentParent = null;
        Rdn currentRdn = null;
        LdapDN newDn = null;

        if ( newSuperiorDn == null )
        {
            throw new NullPointerException( "newSuperiorDn must not be null" );
        }

        if ( modifiedDn == null )
        {
            throw new NullPointerException( "modifiedDn must not be null" );
        }

        if ( modifiedDn.size() == 0 )
        {
            throw new IllegalArgumentException( "Don't think about moving the rootDSE." );
        }

        currentParent = ( LdapDN ) modifiedDn.clone();
        currentRdn = currentParent.getRdn();
        currentParent.remove( currentParent.size() - 1 );

        newDn = ( LdapDN ) newSuperiorDn.clone();
        newDn.add( modifiedDn.getRdn() );

        entry.setChangeType( ChangeType.ModDn );
        entry.setDn( newDn );
        entry.setNewRdn( currentRdn.getUpName() );
        entry.setNewSuperior( currentParent.getName() );
        entry.setDeleteOldRdn( false );
        return entry;
    }

    
    /**
     * A small helper class to compute the simple revert.
     */
    private static LdifEntry revertEntry( List<LdifEntry> entries, Entry entry, LdapDN newDn, 
        LdapDN newSuperior, Rdn oldRdn, Rdn newRdn ) throws InvalidNameException
    {
        LdifEntry reverted = new LdifEntry();
        
        // We have a composite old RDN, something like A=a+B=b
        // It does not matter if the RDNs overlap
        reverted.setChangeType( ChangeType.ModRdn );
        
        if ( newSuperior != null )
        {
            LdapDN restoredDn = (LdapDN)((LdapDN)newSuperior.clone()).add( newRdn ); 
            reverted.setDn( restoredDn );
        }
        else
        {
            reverted.setDn( newDn );
        }
        
        reverted.setNewRdn( oldRdn.getUpName() );

        // Is the newRdn's value present in the entry ?
        // ( case 3, 4 and 5)
        // If keepOldRdn = true, we cover case 4 and 5
        boolean keepOldRdn = entry.contains( newRdn.getNormType(), newRdn.getNormValue() );

        reverted.setDeleteOldRdn( !keepOldRdn );
        
        if ( newSuperior != null )
        {
            LdapDN oldSuperior = ( LdapDN ) entry.getDn().clone();

            oldSuperior.remove( oldSuperior.size() - 1 );
            reverted.setNewSuperior( oldSuperior.getName() );
        }

        return reverted;
    }
    
    
    /**
     * A helper method to generate the modified attribute after a rename.
     */
    private static LdifEntry generateModify( LdapDN parentDn, Entry entry, Rdn oldRdn, Rdn newRdn )
    {
        LdifEntry restored = new LdifEntry();
        restored.setChangeType( ChangeType.Modify );
        
        // We have to use the parent DN, the entry has already
        // been renamed
        restored.setDn( parentDn );

        for ( AVA ava:newRdn )
        {
            // No need to add something which has already been added
            // in the previous modification
            if ( !entry.contains( ava.getNormType(), ava.getNormValue().getString() ) &&
                 !(ava.getNormType().equals( oldRdn.getNormType() ) &&
                   ava.getNormValue().equals( oldRdn.getNormValue() ) ) )
            {
                // Create the modification, which is an Remove
                Modification modification = new ClientModification( 
                    ModificationOperation.REMOVE_ATTRIBUTE, 
                    new DefaultClientAttribute( ava.getUpType(), ava.getUpValue().getString() ) );
                
                restored.addModificationItem( modification );
            }
        }
        
        return restored;
    }
    
    
    /**
     * A helper method which generates a reverted entry
     */
    private static LdifEntry generateReverted( LdapDN newSuperior, Rdn newRdn, LdapDN newDn, 
        Rdn oldRdn, boolean deleteOldRdn ) throws InvalidNameException
    {
        LdifEntry reverted = new LdifEntry();
        reverted.setChangeType( ChangeType.ModRdn );

        if ( newSuperior != null )
        {
            LdapDN restoredDn = (LdapDN)((LdapDN)newSuperior.clone()).add( newRdn ); 
            reverted.setDn( restoredDn );
        }
        else
        {
            reverted.setDn( newDn );
        }
        
        reverted.setNewRdn( oldRdn.getUpName() );
        
        if ( newSuperior != null )
        {
            LdapDN oldSuperior = ( LdapDN ) newDn.clone();

            oldSuperior.remove( oldSuperior.size() - 1 );
            reverted.setNewSuperior( oldSuperior.getName() );
        }
        
        // Delete the newRDN values
        reverted.setDeleteOldRdn( deleteOldRdn );
        
        return reverted;
    }
    
    
    /**
     * Revert a DN to it's previous version by removing the first RDN and adding the given RDN.
     * It's a rename operation. The biggest issue is that we have many corner cases, depending 
     * on the RDNs we are manipulating, and on the content of the initial entry.
     * 
     * @param entry The initial Entry
     * @param newRdn The new RDN
     * @param deleteOldRdn A flag which tells to delete the old RDN AVAs
     * @return A list of LDIF reverted entries 
     * @throws NamingException If the name reverting failed
     */
    public static List<LdifEntry> reverseRename( Entry entry, Rdn newRdn, boolean deleteOldRdn ) throws NamingException
    {
        return reverseMoveAndRename( entry, null, newRdn, deleteOldRdn );
    }
    
    
    /**
     * Revert a DN to it's previous version by removing the first RDN and adding the given RDN.
     * It's a rename operation. The biggest issue is that we have many corner cases, depending 
     * on the RDNs we are manipulating, and on the content of the initial entry.
     * 
     * @param entry The initial Entry
     * @param newSuperior The new superior DN (can be null if it's just a rename)
     * @param newRdn The new RDN
     * @param deleteOldRdn A flag which tells to delete the old RDN AVAs
     * @return A list of LDIF reverted entries 
     * @throws NamingException If the name reverting failed
     */
    public static List<LdifEntry> reverseMoveAndRename( Entry entry, LdapDN newSuperior, Rdn newRdn, boolean deleteOldRdn ) throws NamingException
    {
        LdapDN parentDn = entry.getDn();
        LdapDN newDn = null;

        if ( newRdn == null )
        {
            throw new NullPointerException( "The newRdn must not be null" );
        }

        if ( parentDn == null )
        {
            throw new NullPointerException( "The modified Dn must not be null" );
        }

        if ( parentDn.size() == 0 )
        {
            throw new IllegalArgumentException( "Don't think about renaming the rootDSE." );
        }

        parentDn = ( LdapDN ) entry.getDn().clone();
        Rdn oldRdn = parentDn.getRdn();

        newDn = ( LdapDN ) parentDn.clone();
        newDn.remove( newDn.size() - 1 );
        newDn.add( newRdn );

        List<LdifEntry> entries = new ArrayList<LdifEntry>( 1 );
        LdifEntry reverted = new LdifEntry();

        // Start with the cases here
        if ( newRdn.size() == 1 )
        {
            // We have a simple new RDN, something like A=a
            if ( ( oldRdn.size() == 1 ) && ( oldRdn.equals( newRdn ) ) )
            {
                // We have a simple old RDN, something like A=a
                // If the values overlap, we can't rename the entry, just get out
                // with an error
                throw new NamingException( "Can't rename an entry using the same name ..." ); 
            }

            reverted =
                revertEntry( entries, entry, newDn, newSuperior, oldRdn, newRdn );

            entries.add( reverted );
        }
        else
        {
            // We have a composite new RDN, something like A=a+B=b
            if ( oldRdn.size() == 1 )
            {
                // The old RDN is simple
                boolean overlapping = false;
                boolean existInEntry = false;
                
                // Does it overlap ?
                // Is the new RDN AVAs contained into the entry?
                for ( AVA atav:newRdn )
                {
                    if ( atav.equals( oldRdn.getAtav() ) )
                    {
                        // They overlap
                        overlapping = true;
                    }
                    else
                    {
                        if ( entry.contains( atav.getNormType(), atav.getNormValue().getString() ) )
                        {
                            existInEntry = true;
                        }
                    }
                }
                
                if ( overlapping )
                {
                    // The new RDN includes the old one
                    if ( existInEntry )
                    {
                        // Some of the new RDN AVAs existed in the entry
                        // We have to restore them, but we also have to remove
                        // the new values
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, KEEP_OLD_RDN );
                        
                        entries.add( reverted );
                        
                        // Now, restore the initial values
                        LdifEntry restored = generateModify( parentDn, entry, oldRdn, newRdn );
                        
                        entries.add( restored );
                    }
                    else
                    {
                        // This is the simplest case, we don't have to restore
                        // some existing values (case 8.1 and 9.1)
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, DELETE_OLD_RDN );
                        
                        entries.add( reverted );
                    }
                }
                else
                {
                    if ( existInEntry )
                    {
                        // Some of the new RDN AVAs existed in the entry
                        // We have to restore them, but we also have to remove
                        // the new values
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, KEEP_OLD_RDN );
                        
                        entries.add( reverted );
                        
                        LdifEntry restored = generateModify( parentDn, entry, oldRdn, newRdn );
                        
                        entries.add( restored );
                    }
                    else
                    {
                        // A much simpler case, as we just have to remove the newRDN
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, DELETE_OLD_RDN );

                        entries.add( reverted );
                    }
                }
            }
            else
            {
                // We have a composite new RDN, something like A=a+B=b
                // Does the RDN overlap ?
                boolean overlapping = false;
                boolean existInEntry = false;
                
                Set<AVA> oldAtavs = new HashSet<AVA>();

                // We first build a set with all the oldRDN ATAVs 
                for ( AVA atav:oldRdn )
                {
                    oldAtavs.add( atav );
                }
                
                // Now we loop on the newRDN ATAVs to evaluate if the Rdns are overlaping
                // and if the newRdn ATAVs are present in the entry
                for ( AVA atav:newRdn )
                {
                    if ( oldAtavs.contains( atav ) )
                    {
                        overlapping = true;
                    }
                    else if ( entry.contains( atav.getNormType(), atav.getNormValue().getString() ) )
                    {
                        existInEntry = true;
                    }
                }
                
                if ( overlapping ) 
                {
                    // They overlap
                    if ( existInEntry )
                    {
                        // In this case, we have to reestablish the removed ATAVs
                        // (Cases 12.2 and 13.2)
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, KEEP_OLD_RDN );
    
                        entries.add( reverted );
                    }
                    else
                    {
                        // We can simply remove all the new RDN atavs, as the
                        // overlapping values will be re-created.
                        // (Cases 12.1 and 13.1)
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, DELETE_OLD_RDN );
    
                        entries.add( reverted );
                    }
                }
                else
                {
                    // No overlapping
                    if ( existInEntry )
                    {
                        // In this case, we have to reestablish the removed ATAVs
                        // (Cases 10.2 and 11.2)
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, KEEP_OLD_RDN );
    
                        entries.add( reverted );
                        
                        LdifEntry restored = generateModify( parentDn, entry, oldRdn, newRdn );
                        
                        entries.add( restored );
                    }
                    else
                    {
                        // We are safe ! We can delete all the new Rdn ATAVs
                        // (Cases 10.1 and 11.1)
                        reverted = generateReverted( newSuperior, newRdn, newDn, oldRdn, DELETE_OLD_RDN );
    
                        entries.add( reverted );
                    }
                }
            }
        }

        return entries;
    }
}
