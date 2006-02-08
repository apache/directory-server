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
package org.apache.directory.shared.ldap.message;


import javax.naming.directory.Attributes;

import org.apache.directory.shared.ldap.util.AttributeUtils;


/**
 * Lockable add request implemenation.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AddRequestImpl extends AbstractAbandonableRequest implements AddRequest
{
    static final long serialVersionUID = 7534132448349520346L;

    /** Distinguished name of the new entry. */
    private String entry;

    /** A MultiMap of the new entry's attributes and their values */
    private Attributes attributes;

    private AddResponse response;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates an AddRequest implementation to create a new entry.
     * 
     * @param id
     *            the sequence identifier of the AddRequest message.
     */
    public AddRequestImpl(final int id)
    {
        super( id, TYPE );
    }


    // ------------------------------------------------------------------------
    // AddRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the distinguished name of the entry to add.
     * 
     * @return the Dn of the added entry.
     */
    public String getEntry()
    {
        return entry;
    }


    /**
     * Sets the distinguished name of the entry to add.
     * 
     * @param entry
     *            the Dn of the added entry.
     */
    public void setEntry( String entry )
    {
        this.entry = entry;
    }


    /**
     * Gets the attribute value pairs of the entry to add as a MultiMap.
     * 
     * @return the Attribute with attribute value pairs.
     */
    public Attributes getAttributes()
    {
        return attributes;
    }


    /**
     * Sets the attribute value pairs of the entry to add as a MultiMap.
     * 
     * @param entry
     *            the Attributes with attribute value pairs for the added entry.
     */
    public void setAttributes( Attributes attributes )
    {
        this.attributes = attributes;
    }


    // ------------------------------------------------------------------------
    // SingleReplyRequest Interface Method Implementations
    // ------------------------------------------------------------------------

    /**
     * Gets the protocol response message type for this request which produces
     * at least one response.
     * 
     * @return the message type of the response.
     */
    public MessageTypeEnum getResponseType()
    {
        return RESP_TYPE;
    }


    /**
     * The result containing response for this request.
     * 
     * @return the result containing response for this request
     */
    public ResultResponse getResultResponse()
    {
        if ( response == null )
        {
            response = new AddResponseImpl( getMessageId() );
        }

        return response;
    }


    /**
     * Checks to see if an object is equivalent to this AddRequest. First
     * there's a quick test to see if the obj is the same object as this one -
     * if so true is returned. Next if the super method fails false is returned.
     * Then the name of the entry is compared - if not the same false is
     * returned. Lastly the attributes of the entry are compared. If they are
     * not the same false is returned otherwise the method exists returning
     * true.
     * 
     * @param obj
     *            the object to test for equality to this
     * @return true if the obj is equal to this AddRequest, false otherwise
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

        AddRequest req = ( AddRequest ) obj;

        if ( entry != null && req.getEntry() == null )
        {
            return false;
        }

        if ( entry == null && req.getEntry() != null )
        {
            return false;
        }

        if ( entry != null && req.getEntry() != null )
        {
            if ( !entry.equals( req.getEntry() ) )
            {
                return false;
            }
        }

        if ( attributes != null && req.getAttributes() == null )
        {
            return false;
        }

        if ( attributes == null && req.getAttributes() != null )
        {
            return false;
        }

        if ( attributes != null && req.getAttributes() != null )
        {
            if ( !attributes.equals( req.getAttributes() ) )
            {
                return false;
            }
        }

        return true;
    }


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        sb.append( "    Add Request\n" );
        sb.append( "        Entry : '" ).append( entry.toString() ).append( "'\n" );

        if ( ( attributes == null ) || ( attributes.size() == 0 ) )
        {
            sb.append( "            No attributes\n" );
        }
        else
        {
            AttributeUtils.toString( "            ", attributes );
        }

        return sb.toString();
    }
}
