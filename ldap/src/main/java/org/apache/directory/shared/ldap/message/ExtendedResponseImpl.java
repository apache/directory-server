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
package org.apache.directory.shared.ldap.message ;


import org.apache.directory.shared.ldap.util.ArrayUtils;


/**
 * Lockable ExtendedResponse implementation
 * 
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class ExtendedResponseImpl extends AbstractResultResponse implements ExtendedResponse
{
    static final long serialVersionUID = -6646752766410531060L;
    /** Object identifier for the extended response */
    protected String oid ;
    /** Values encoded in the extended response payload */
    protected byte [] value ;


    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------


    /**
     * Creates a Lockable ExtendedResponse as a reply to an ExtendedRequest.
     *
     * @param id the session unique message id
     */
    public ExtendedResponseImpl( final int id )
    {
        super( id, TYPE ) ;
    }


    // ------------------------------------------------------------------------
    // ExtendedResponse Interface Method Implementations
    // ------------------------------------------------------------------------


    /**
     * Gets the reponse OID specific encoded response values.
     *
     * @return the response specific encoded response values.
     */
    public byte [] getResponse()
    {
        return value ;
    }


    /**
     * Sets the reponse OID specific encoded response values.
     *
     * @param value the response specific encoded response values.
     */
    public void setResponse( byte [] value )
    {
        this.value = value ;
    }


    /**
     * Gets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     *
     * @return the OID of the extended response type.
     */
    public String getResponseName()
    {
        return oid ;
    }


    /**
     * Sets the OID uniquely identifying this extended response (a.k.a. its
     * name).
     *
     * @param oid the OID of the extended response type.
     */
    public void setResponseName( String oid )
    {
        this.oid = oid ;
    }


    /**
     * Checks to see if an object equals this ExtendedRequest.
     *
     * @param obj the object to be checked for equality
     * @return true if the obj equals this ExtendedRequest, false otherwise
     */
    public boolean equals( Object obj )
    {
        if ( obj == this )
        {
            return true;
        }

        if ( ! super.equals( obj ) )
        {
            return false;
        }

        ExtendedResponse resp = ( ExtendedResponse ) obj;

        if ( oid != null && resp.getResponseName() == null )
        {
            return false;
        }

        if ( oid == null && resp.getResponseName() != null )
        {
            return false;
        }

        if ( oid != null && resp.getResponseName() != null )
        {
            if ( ! oid.equals( resp.getResponseName() ) )
            {
                return false;
            }
        }

        if ( value != null && resp.getResponse() == null )
        {
            return false;
        }

        if ( value == null && resp.getResponse() != null )
        {
            return false;
        }

        if ( value != null && resp.getResponse() != null )
        {
            if ( ! ArrayUtils.isEquals( value, resp.getResponse() ) )
            {
                return false;
            }
        }

        return true;
    }


    public String getID()
    {
        return getResponseName();
    }


    public byte[] getEncodedValue()
    {
        return getResponse();
    }
}
