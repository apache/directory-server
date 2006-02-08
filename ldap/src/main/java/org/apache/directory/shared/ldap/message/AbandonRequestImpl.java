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


/**
 * Lockable implementation of an AbandonRequest.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class AbandonRequestImpl extends AbstractRequest implements AbandonRequest
{
    static final long serialVersionUID = -4688193359792740969L;

    /** Sequence identifier of the outstanding request message to abandon */
    private int abandonId;


    /**
     * Creates an AbandonRequest implementation for an outstanding request.
     * 
     * @param id
     *            the sequence identifier of the AbandonRequest message.
     */
    public AbandonRequestImpl(final int id)
    {
        super( id, TYPE, false );
    }


    /**
     * Gets the id of the request operation to terminate.
     * 
     * @return the id of the request message to abandon
     */
    public int getAbandoned()
    {
        return abandonId;
    }


    /**
     * Sets the id of the request operation to terminate.
     * 
     * @param abandonId
     *            the sequence id of the request message to abandon
     */
    public void setAbandoned( int abandonId )
    {
        this.abandonId = abandonId;
    }


    /**
     * Checks for equality first by asking the super method which should compare
     * all but the Abandoned request's Id. It then compares this to determine
     * equality.
     * 
     * @param obj
     *            the object to test for equality to this AbandonRequest
     * @return true if the obj equals this request false otherwise
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

        AbandonRequest req = ( AbandonRequest ) obj;
        if ( req.getAbandoned() != abandonId )
        {
            return false;
        }

        return true;
    }


    /**
     * RFC 2251 [Section 4.11]: Abandon, Bind, Unbind, and StartTLS operations
     * cannot be abandoned.
     */
    public void abandon()
    {
        throw new UnsupportedOperationException(
            "RFC 2251 [Section 4.11]: Abandon, Bind, Unbind, and StartTLS operations cannot be abandoned. " );
    }
}
