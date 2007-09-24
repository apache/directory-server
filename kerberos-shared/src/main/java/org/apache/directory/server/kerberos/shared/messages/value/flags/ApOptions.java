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
package org.apache.directory.server.kerberos.shared.messages.value.flags;


/**
 * An abstract class where are implemented the common methods described in
 * the interface
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 540371 $, $Date: 2007-05-22 02:00:43 +0200 (Tue, 22 May 2007) $
 */
public class ApOptions extends AbstractKerberosFlags
{
    public static final long serialVersionUID = 1L;

    /**
      * Basic constructor of a ApOptions BitString
      */
     public ApOptions()
     {
         super();
     }
     
     /**
      * Constructor of a ApOptions BitString with an int value
      */
     public ApOptions( int flags )
     {
         super( getBytes( flags ) );
     }
     
     /**
      * Basic constructor of a ApOptions BitString with a byte array
      */
     public ApOptions( byte[] flags )
     {
         super( flags );
     }
     
     /**
      * AP Option flag - the client requires mutual authentication
      */
     public boolean isMutualRequired()
     {
        return isFlagSet( ApOption.MUTUAL_REQUIRED );
     }

     /**
      * AP Option flag - Reserved for future use
      */
     public boolean isReserved()
     {
        return isFlagSet( ApOption.RESERVED );
     }

     /**
      * AP Option flag - the ticket the client is presenting to a 
      * server is encrypted in the session key from the server's TGT
      */
     public boolean isUseSessionKey()
     {
        return isFlagSet( ApOption.USE_SESSION_KEY );
     }

     /**
     * Converts the object to a printable string.
     */
    public String toString()
    {
        StringBuilder result = new StringBuilder();

        if ( isFlagSet( ApOption.RESERVED ) )
        {
            result.append( "RESERVED(0) " );
        }

        if ( isFlagSet( ApOption.USE_SESSION_KEY ) )
        {
            result.append( "USE_SESSION_KEY(1) " );
        }

        if ( isFlagSet( ApOption.MUTUAL_REQUIRED ) )
        {
            result.append( "MUTUAL_REQUIRED(2) " );
        }

        return result.toString().trim();
    }
}
