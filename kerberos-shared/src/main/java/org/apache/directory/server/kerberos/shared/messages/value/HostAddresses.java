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
package org.apache.directory.server.kerberos.shared.messages.value;


public class HostAddresses
{
    private HostAddress[] addresses;


    /**
     * Class constructors
     */
    public HostAddresses(HostAddress[] addresses)
    {
        this.addresses = addresses;
    }


    public boolean contains( HostAddress address )
    {
        if ( addresses != null )
        {
            for ( int ii = 0; ii < addresses.length; ii++ )
            {
                if ( addresses[ii].equals( address ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    public boolean equals( HostAddresses that )
    {
        if ( ( this.addresses == null && that.addresses != null )
            || ( this.addresses != null && that.addresses == null ) )
        {
            return false;
        }

        if ( this.addresses != null && that.addresses != null )
        {
            if ( this.addresses.length != that.addresses.length )
            {
                return false;
            }

            for ( int ii = 0; ii < this.addresses.length; ii++ )
            {
                if ( !this.addresses[ii].equals( that.addresses[ii] ) )
                {
                    return false;
                }
            }
        }

        return true;
    }


    public HostAddress[] getAddresses()
    {
        return addresses;
    }


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for ( int ii = 0; ii < this.addresses.length; ii++ )
        {
            sb.append( addresses[ii].toString() );

            if ( ii < addresses.length - 1 )
            {
                sb.append( ", " );
            }
        }

        return sb.toString();
    }
}
