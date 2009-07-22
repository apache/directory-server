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

package org.apache.directory.shared.dsmlv2.reponse;


import org.apache.directory.shared.dsmlv2.DsmlDecorator;
import org.apache.directory.shared.ldap.codec.LdapResponseCodec;
import org.dom4j.Element;


/**
 * Class representing Error Response. <br>
 * <br>
 * An Error Response has a requestID, a message, and a type which can be :
 * <ul> 
 *     <li>NOT_ATTEMPTED,</li>
 *     <li>COULD_NOT_CONNECT,</li>
 *     <li>CONNECTION_CLOSED,</li>
 *     <li>MALFORMED_REQUEST,</li>
 *     <li>GATEWAY_INTERNAL_ERROR,</li>
 *     <li>AUTHENTICATION_FAILED,</li>
 *     <li>UNRESOLVABLE_URI,</li>
 *     <li>OTHER</li>
 * </ul>
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class ErrorResponse extends LdapResponseCodec implements DsmlDecorator
{
    /**
     * This enum represents the different types of error response
     *
     * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
     * @version $Rev$, $Date$
     */
    public enum ErrorResponseType
    {
        NOT_ATTEMPTED, COULD_NOT_CONNECT, CONNECTION_CLOSED, MALFORMED_REQUEST, GATEWAY_INTERNAL_ERROR, AUTHENTICATION_FAILED, UNRESOLVABLE_URI, OTHER
    };

    /** The type of error response */
    private ErrorResponseType type;

    /** The associated message */
    private String message;

    /** The request ID */
    private int requestID;


    /**
     * Creates a new instance of ErrorResponse.
     */
    public ErrorResponse()
    {
    }


    /**
     * Creates a new instance of ErrorResponse.
     *
     * @param requestID
     *      the requestID of the response
     * @param type 
     *      the type of the response
     * @param message
     *      the associated message
     */
    public ErrorResponse( int requestID, ErrorResponseType type, String message )
    {
        this.requestID = requestID;
        this.type = type;
        this.message = message;
    }


    /* (non-Javadoc)
     * @see org.apache.directory.shared.dsmlv2.reponse.DsmlDecorator#toDsml(org.dom4j.Element)
     */
    public Element toDsml( Element root )
    {
        Element element = root.addElement( "errorResponse" );

        // RequestID
        if ( requestID != 0 )
        {
            element.addAttribute( "requestID", "" + requestID );
        }

        // Type
        element.addAttribute( "type", getTypeDescr( type ) );

        // TODO Add Detail

        if ( ( message != null ) && ( !"".equals( message ) ) )
        {
            Element messageElement = element.addElement( "message" );
            messageElement.addText( message );
        }

        return element;
    }


    /**
     * Returns the String associated to the error response type
     * 
     * @param type 
     *      the error response type
     * @return 
     *      the corresponding String
     */
    public String getTypeDescr( ErrorResponseType type )
    {
        if ( type.equals( ErrorResponseType.NOT_ATTEMPTED ) )
        {
            return "notAttempted";
        }
        else if ( type.equals( ErrorResponseType.COULD_NOT_CONNECT ) )
        {
            return "couldNotConnect";
        }
        else if ( type.equals( ErrorResponseType.CONNECTION_CLOSED ) )
        {
            return "connectionClosed";
        }
        else if ( type.equals( ErrorResponseType.MALFORMED_REQUEST ) )
        {
            return "malformedRequest";
        }
        else if ( type.equals( ErrorResponseType.GATEWAY_INTERNAL_ERROR ) )
        {
            return "gatewayInternalError";
        }
        else if ( type.equals( ErrorResponseType.AUTHENTICATION_FAILED ) )
        {
            return "authenticationFailed";
        }
        else if ( type.equals( ErrorResponseType.UNRESOLVABLE_URI ) )
        {
            return "unresolvableURI";
        }
        else if ( type.equals( ErrorResponseType.OTHER ) )
        {
            return "other";
        }
        else
        {
            return "unknown";
        }
    }


    /**
     * Gets the message
     *
     * @return
     *      the message
     */
    public String getMessage()
    {
        return message;
    }


    /**
     * Sets the message
     *
     * @param message
     *      the message to set
     */
    public void setMessage( String message )
    {
        this.message = message;
    }


    /**
     * Gets the request ID
     *
     * @return
     *      the request ID
     */
    public int getRequestID()
    {
        return requestID;
    }


    /**
     * Sets the request ID
     *
     * @param requestID
     *      the request ID to set
     */
    public void setRequestID( int requestID )
    {
        this.requestID = requestID;
    }


    /**
     * Gets the type of error response
     *
     * @return
     *      the type of error response
     */
    public ErrorResponseType getType()
    {
        return type;
    }


    /**
     * Sets the type of error response
     *
     * @param type
     *      the type of error response to set
     */
    public void setType( ErrorResponseType type )
    {
        this.type = type;
    }
}
