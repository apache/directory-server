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
package org.apache.ldap.server.referral;


import java.util.HashSet;
import java.util.Set;

import javax.naming.InvalidNameException;
import javax.naming.Name;

import org.apache.ldap.common.name.LdapName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A simple lookup table of normalized referral distinguished names.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ReferralLut
{
    /** the logger for this class */
    private static final Logger log = LoggerFactory.getLogger( ReferralLut.class );
    /** the set of names in the LUT */
    private Set names = new HashSet();

    
    // -----------------------------------------------------------------------
    // Methods to access the LUT: all names are expected to be normalized
    // -----------------------------------------------------------------------
    
    
    /**
     * Checks if a the entry at a name is a referral.
     * 
     * @param dn the normalized name of the referral
     */
    public boolean isReferral( Name dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        return names.contains( dn.toString() );
    }
    
    
    /**
     * Checks if a the entry at a name is a referral.
     * 
     * @param dn the normalized name of the referral
     */
    public boolean isReferral( String dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        return names.contains( dn );
    }
    
    
    /**
     * Gets the normalized name of the farthest ancestor that is a referral. If the argument 
     * is a referral it will not be returned.  Only ancestor's (includes parent) are considered.
     * 
     * @param dn the name to get the farthest ancestor referral name for
     * @return the farthest referral ancestor
     */
    public Name getFarthestReferralAncestor( Name dn ) 
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        Name farthest = new LdapName();
        for ( int ii = 0; ii < dn.size(); ii++ )
        {
            try
            {
                farthest.add( dn.get( ii ) );
            }
            catch ( InvalidNameException e )
            {
                log.error( "Should never get this when moving names from a proper normalized name!", e );
            }
            // do not return dn if it is the farthest referral
            if ( isReferral( farthest ) && farthest.size() != dn.size() )
            {
                return farthest;
            }
        }
        return null;
    }
    
    
    /**
     * Gets the normalized name of the nearest ancestor that is a referral.  If the argument
     * is a referral it will not be returned.  Only ancestor's (includes parent) are considered.
     * 
     * @param dn the name to get the nearest ancestor referral name for
     * @return the nearest referral ancestor or null if one does not exist
     */
    public Name getNearestReferralAncestor( Name dn ) 
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        Name cloned = ( Name ) dn.clone();
        
        // do not return the argument dn if it is a referral (skip it)
        if ( cloned.size() > 0 ) 
        {
            try
            {
                cloned.remove( cloned.size() - 1 );
            }
            catch ( InvalidNameException e )
            {
                log.error( "Should never get this when removing from a cloned normalized name!", e );
            }
        }
        else
        {
            return null;
        }
        
        while ( ! isReferral( cloned ) && cloned.size() > 0 )
        {
            try
            {
                cloned.remove( cloned.size() - 1 );
            }
            catch ( InvalidNameException e )
            {
                log.error( "Should never get this when removing from a cloned normalized name!", e );
            }
        }
        return cloned.isEmpty() ? null : cloned;
    }

    
    // -----------------------------------------------------------------------
    // Methods that notify this lookup table of changes to referrals
    // -----------------------------------------------------------------------
    
    
    /**
     * Called to add an entry to the LUT when a referral is added.
     * 
     * @param dn the normalized name of the added referral
     */
    public void referralAdded( Name dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        if ( ! names.add( dn.toString() ) && log.isWarnEnabled() )
        {
            log.warn( "found " + dn + " in refname lut while adding it" );
        }
    }
    
    
    /**
     * Called to add an entry to the LUT when a referral is added.
     * 
     * @param dn the normalized name of the added referral
     */
    public void referralAdded( String dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        if ( ! names.add( dn ) && log.isWarnEnabled() )
        {
            log.warn( "found " + dn + " in refname lut while adding it" );
        }
    }
    
    
    /**
     * Called delete an entry from the LUT when a referral is deleted.
     * 
     * @param dn the normalized name of the deleted referral
     */
    public void referralDeleted( Name dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        if ( ! names.remove( dn.toString() ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find " + dn + " in refname lut while deleting it" );
        }
    }
    
    
    /**
     * Called delete an entry from the LUT when a referral is deleted.
     * 
     * @param dn the normalized name of the deleted referral
     */
    public void referralDeleted( String dn )
    {
        if ( dn == null ) throw new IllegalArgumentException( "dn cannot be null" );
        if ( ! names.remove( dn ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find " + dn + " in refname lut while deleting it" );
        }
    }
    
    
    /**
     * Called to update the LUT when the name of the referral changes due to 
     * a rename or move in the DIT.
     * 
     * @param oldDn the normalized old name for the referral
     * @param newDn the normalized new name for the referral
     */
    public void referralChanged( Name oldDn, Name newDn )
    {
        if ( oldDn == null || newDn == null ) throw new IllegalArgumentException( "old or new dn cannot be null" );
        if ( ! names.remove( oldDn.toString() ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find old name (" + oldDn + ") in refname lut while moving or renaming it" );
        }
        if ( ! names.add( newDn.toString() ) && log.isWarnEnabled() )
        {
            log.warn( "found new name (" + newDn + ") in refname lut while moving or renaming " + oldDn );
        }
    }
    
    
    /**
     * Called to update the LUT when the name of the referral changes due to 
     * a rename or move in the DIT.
     * 
     * @param oldDn the normalized old name for the referral
     * @param newDn the normalized new name for the referral
     */
    public void referralChanged( String oldDn, String newDn )
    {
        if ( oldDn == null || newDn == null ) throw new IllegalArgumentException( "old or new dn cannot be null" );
        if ( ! names.remove( oldDn ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find old name (" + oldDn + ") in refname lut while moving or renaming it" );
        }
        if ( ! names.add( newDn ) && log.isWarnEnabled() )
        {
            log.warn( "found new name (" + newDn + ") in refname lut while moving or renaming " + oldDn );
        }
    }
    
    
    /**
     * Called to update the LUT when the name of the referral changes due to 
     * a rename or move in the DIT.
     * 
     * @param oldDn the normalized old name for the referral
     * @param newDn the normalized new name for the referral
     */
    public void referralChanged( Name oldDn, String newDn )
    {
        if ( oldDn == null || newDn == null ) throw new IllegalArgumentException( "old or new dn cannot be null" );
        if ( ! names.remove( oldDn.toString() ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find old name (" + oldDn + ") in refname lut while moving or renaming it" );
        }
        if ( ! names.add( newDn ) && log.isWarnEnabled() )
        {
            log.warn( "found new name (" + newDn + ") in refname lut while moving or renaming " + oldDn );
        }
    }
    
    
    /**
     * Called to update the LUT when the name of the referral changes due to 
     * a rename or move in the DIT.
     * 
     * @param oldDn the normalized old name for the referral
     * @param newDn the normalized new name for the referral
     */
    public void referralChanged( String oldDn, Name newDn )
    {
        if ( oldDn == null || newDn == null ) throw new IllegalArgumentException( "old or new dn cannot be null" );
        if ( ! names.remove( oldDn ) && log.isWarnEnabled() )
        {
            log.warn( "cound not find old name (" + oldDn + ") in refname lut while moving or renaming it" );
        }
        if ( ! names.add( newDn ) && log.isWarnEnabled() )
        {
            log.warn( "found new name (" + newDn + ") in refname lut while moving or renaming " + oldDn );
        }
    }
}
