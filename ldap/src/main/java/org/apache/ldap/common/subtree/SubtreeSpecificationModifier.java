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
package org.apache.ldap.common.subtree;


import org.apache.ldap.common.name.LdapName;
import org.apache.ldap.common.filter.ExprNode;

import java.util.Set;
import java.util.Collections;
import javax.naming.Name;


/**
 * SubtreeSpecification contains no setters so they must be built by a modifiable
 * object containing all the necessary parameters to build the base object.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SubtreeSpecificationModifier
{
    /** the subtree base relative to the administration point */
    private Name base = new LdapName();

    /** the set of subordinates entries and their subordinates to exclude */
    private Set chopBefore = Collections.EMPTY_SET;

    /** the set of subordinates entries whose subordinates are to be excluded */
    private Set chopAfter = Collections.EMPTY_SET;

    /** the minimum distance below base to start including entries */
    private int minBaseDistance = 0;

    /** the maximum distance from base past which entries are excluded */
    private int maxBaseDistance = SubtreeSpecification.UNBOUNDED_MAX;

    /** a filter using only assertions on objectClass attributes for subtree refinement */
    private ExprNode refinement = null;


    // -----------------------------------------------------------------------
    // F A C T O R Y   M E T H O D
    // -----------------------------------------------------------------------


    /**
     * Creates a SubtreeSpecification using any of the default paramters that may
     * have been modified from their defaults.
     *
     * @return the newly created subtree specification
     */
    public SubtreeSpecification getSubtreeSpecification()
    {

        return new BaseSubtreeSpecification( this.base, this.minBaseDistance,
                                              this.maxBaseDistance, this.chopAfter,
                                              this.chopBefore, this.refinement );
    }


    // -----------------------------------------------------------------------
    // M U T A T O R S
    // -----------------------------------------------------------------------


    /**
     * Sets the subtree base relative to the administration point.
     *
     * @param base subtree base relative to the administration point
     */
    public void setBase( Name base )
    {
        this.base = base;
    }


    /**
     * Sets the set of subordinates entries and their subordinates to exclude.
     *
     * @param chopBefore the set of subordinates entries and their subordinates to exclude
     */
    public void setChopBeforeExclusions( Set chopBefore )
    {
        this.chopBefore = chopBefore;
    }


    /**
     * Sets the set of subordinates entries whose subordinates are to be excluded.
     *
     * @param chopAfter the set of subordinates entries whose subordinates are to be excluded
     */
    public void setChopAfterExclusions( Set chopAfter )
    {
        this.chopAfter = chopAfter;
    }


    /**
     * Sets the minimum distance below base to start including entries.
     *
     * @param minBaseDistance the minimum distance below base to start including entries
     */
    public void setMinBaseDistance( int minBaseDistance )
    {
        if ( minBaseDistance < 0 )
        {
            throw new IllegalArgumentException( "A negative minimum base distance is undefined!" );
        }

        this.minBaseDistance = minBaseDistance;
    }


    /**
     * Sets the maximum distance from base past which entries are excluded.
     *
     * @param maxBaseDistance the maximum distance from base past which entries are excluded
     */
    public void setMaxBaseDistance( int maxBaseDistance )
    {
        if ( maxBaseDistance < 0 )
        {
            this.maxBaseDistance = SubtreeSpecification.UNBOUNDED_MAX;
        }
        else
        {
            this.maxBaseDistance = maxBaseDistance;
        }
    }


    /**
     * Sets a filter using only assertions on objectClass attributes for subtree refinement.
     *
     * @param refinement a filter using only assertions on objectClass attributes for subtree refinement
     */
    public void setRefinement( ExprNode refinement )
    {
        this.refinement = refinement;
    }
}
