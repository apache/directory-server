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
package org.apache.directory.server.core.api.interceptor.context;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.directory.server.core.api.CoreSession;
import org.apache.directory.server.core.api.OperationEnum;
import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.ldap.model.name.Dn;
import org.apache.directory.shared.util.Strings;


/**
 * A context for tracking lookup operations. Lookup operations will return a
 * cloned server entry.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LookupOperationContext extends AbstractOperationContext
{
    private static final String[] EMPTY = new String[]
        {};

    /** The list of attributes id to return */
    private List<String> attrsId = new ArrayList<String>();

    /** A flag set to true if the user has requested all the operational attributes ( "+" )*/
    private Boolean allOperational;

    /** A flag set to true if the user has requested all the user attributes ( "*" ) */
    private Boolean allUser;

    /** A flag set to true if the user has requested no attribute to be returned */
    private Boolean noAttribute;

    /** flag to indicate if this search is done for replication */
    private boolean syncreplLookup;

    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( CoreSession session )
    {
        super( session );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( CoreSession session, Dn dn )
    {
        super( session, dn );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( CoreSession session, String attrsId[] )
    {
        super( session );
        setAttrsId( attrsId );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * 
     * Creates a new instance of LookupOperationContext.
     *
     */
    public LookupOperationContext( CoreSession session, Dn dn, String attrsId[] )
    {
        super( session, dn );
        setAttrsId( attrsId );

        if ( session != null )
        {
            setInterceptors( session.getDirectoryService().getInterceptors( OperationEnum.LOOKUP ) );
        }
    }


    /**
     * @return Get the attribute ids as a String array
     */
    public String[] getAttrsIdArray()
    {
        if ( ( attrsId == null ) || ( attrsId.size() == 0 ) )
        {
            return EMPTY;
        }
        else
        {
            String[] attrs = new String[attrsId.size()];
            return attrsId.toArray( attrs );
        }
    }


    /**
     * Set the attribute Ids
     *
     * @param attrsId The String array containing all the attribute IDs
     */
    public void setAttrsId( String[] attrsId )
    {
        if ( ( attrsId != null ) && ( attrsId.length > 0 ) )
        {
            this.attrsId = new ArrayList<String>( Arrays.asList( attrsId ) );
            int nbNoAttribute = 0;

            // filter out the '+' and '*' and set boolean parameters
            for ( String id : this.attrsId )
            {
                if ( id.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
                {
                    allOperational = true;
                }
                else if ( id.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
                {
                    allUser = true;
                }
                else if ( id.equals( SchemaConstants.NO_ATTRIBUTE ) )
                {
                    noAttribute = true;
                    nbNoAttribute++;
                }
            }

            if ( ( allOperational != null ) && allOperational )
            {
                this.attrsId.remove( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES );
            }

            if ( ( allUser != null ) && allUser )
            {
                this.attrsId.remove( SchemaConstants.ALL_USER_ATTRIBUTES );
            }

            if ( noAttribute != null )
            {
                if ( attrsId.length == nbNoAttribute )
                {
                    this.attrsId.clear();
                }
                else
                {
                    // We have to ignore the 1.1
                    this.attrsId.remove( SchemaConstants.NO_ATTRIBUTE );
                    noAttribute = false;
                }
            }
        }
    }


    /**
     * Add an attribute ID to the current list, creating the list if necessary
     *
     * @param attrId the Id to add
     */
    public void addAttrsId( String attrId )
    {
        if ( noAttribute == null )
        {
            if ( attrId.equals( SchemaConstants.NO_ATTRIBUTE ) )
            {
                noAttribute = true;

                if ( attrsId != null )
                {
                    attrsId.clear();
                }

                return;
            }

            if ( attrId.equals( SchemaConstants.ALL_USER_ATTRIBUTES ) )
            {
                allUser = true;

                return;
            }

            if ( attrId.equals( SchemaConstants.ALL_OPERATIONAL_ATTRIBUTES ) )
            {
                allOperational = true;

                return;
            }

            if ( attrsId == null )
            {
                attrsId = new ArrayList<String>();
            }

            attrsId.add( attrId );
        }
    }


    /**
     * @return The attribute IDs list
     */
    public List<String> getAttrsId()
    {
        return attrsId;
    }


    /**
     * @return The flag telling if the "*" attribute has been used
     */
    public boolean hasAllUser()
    {
        return ( allUser != null ) && allUser;
    }


    /**
     * @return The flag telling if the "+" attribute has been used
     */
    public boolean hasAllOperational()
    {
        return ( allOperational != null ) && allOperational;
    }


    /**
     * @return The flag telling if the "1.1" attribute has been used
     */
    public boolean hasNoAttribute()
    {
        return ( noAttribute != null );
    }


    /**
     * @return the operation name
     */
    public String getName()
    {
        return "Lookup";
    }

    
    /**
     * @return true if this is a syncrepl specific search
     */
    public boolean isSyncreplLookup()
    {
        return syncreplLookup;
    }


    /**
     * sets the flag to indicate if this is a synrepl specific search or not
     * 
     * @param syncreplLookup
     */
    public void setSyncreplLookup( boolean syncreplLookup )
    {
        this.syncreplLookup = syncreplLookup;
    }
    
    
    /**
     * @see Object#toString()
     */
    public String toString()
    {
        return "LookupContext for Dn '" + getDn().getName() + "'"
            + ( ( attrsId != null ) ? ", attributes : <" + Strings.listToString( attrsId ) + ">" : "" );
    }
}
