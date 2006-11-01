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
package org.apache.directory.shared.ldap.message;


import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.name.LdapDN;
import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Lockable SearchResponseEntry implementation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class SearchResponseEntryImpl extends AbstractResponse implements SearchResponseEntry
{
    static final long serialVersionUID = -8357316233060886637L;

    /** Distinguished name of the search result entry returned */
    private LdapDN objectName;

    /** Partial set of attributes returned in response to search */
    private Attributes attributes;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a Lockable SearchResponseEntry as a reply to an SearchRequest to
     * indicate the end of a search operation.
     * 
     * @param id
     *            the session unique message id
     */
    public SearchResponseEntryImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // SearchResponseEntry Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the set of attributes and all their values in a Attributes.
     * 
     * @return the set of attributes and all their values
     */
    public Attributes getAttributes()
    {
        return attributes;
    }


    /**
     * Sets the set of attributes and all their values in a Attributes.
     * 
     * @param attributes
     *            the set of attributes and all their values
     */
    public void setAttributes( Attributes attributes )
    {
        this.attributes = attributes;
    }


    /**
     * Gets the distinguished name of the entry object returned.
     * 
     * @return the Dn of the entry returned.
     */
    public LdapDN getObjectName()
    {
        return objectName;
    }


    /**
     * Sets the distinguished name of the entry object returned.
     * 
     * @param objectName
     *            the Dn of the entry returned.
     */
    public void setObjectName( LdapDN objectName )
    {
        this.objectName = objectName;
    }


    /**
     * Checks for equality by comparing the objectName, and attributes
     * properties of this Message after delegating to the super.equals() method.
     * 
     * @param obj
     *            the object to test for equality with this message
     * @return true if the obj is equal false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( this == obj )
        {
            return true;
        }

        if ( !super.equals( obj ) )
        {
            return false;
        }

        SearchResponseEntry resp = ( SearchResponseEntry ) obj;

        if ( !objectName.equals( resp.getObjectName() ) )
        {
            return false;
        }

        if ( attributes == null && resp.getAttributes() != null )
        {
            return false;
        }

        if ( attributes != null && resp.getAttributes() == null )
        {
            return false;
        }

        if ( attributes != null && resp.getAttributes() != null )
        {
            return attributes.equals( resp.getAttributes() );
        }

        return true;
    }


    /**
     * Return a string representation of a SearchResultEntry request
     */
    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Search Result Entry\n" );
        sb.append( "        Object Name : '" ).append( objectName.toString() ).append( "'\n" );
        sb.append( "        Attributes\n" );

        if ( attributes != null )
        {
            sb.append( AttributeUtils.toString( attributes ) );
        }
        else
        {
            sb.append( "            No attributes\n" );
        }

        return sb.toString();
    }
}
