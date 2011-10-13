/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.core.authn;


import org.apache.directory.shared.ldap.model.constants.SchemaConstants;
import org.apache.directory.shared.util.Base64;
import org.apache.directory.shared.util.Strings;


/**
 * A class to hold the data of historical passwords of a entry.
 * Note: This class's natural ordering is inconsistent with the equals() method
 *       hence it is advised not to use this in any implementations of sorted sets
 *       Instead use Collections.sort() to sort the collection of PasswordHistory objects.
 *       
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$, $Date$
 */
public class PasswordHistory implements Comparable<PasswordHistory>
{
    /** time when password was last changed */
    private String time;

    /** the syntax OID that is to be used on the password data */
    private String syntaxOID = SchemaConstants.OCTET_STRING_SYNTAX;

    /** the length of the password data */
    private int length;

    /** password octet string */
    private String data;

    private static final char DELIMITER = '#';


    public PasswordHistory( String pwdHistoryVal )
    {
        int pos = pwdHistoryVal.indexOf( DELIMITER );
        time = pwdHistoryVal.substring( 0, pos );

        pos++;
        int nextPos = pwdHistoryVal.indexOf( DELIMITER, pos );
        syntaxOID = pwdHistoryVal.substring( pos, nextPos );

        nextPos++;
        pos = pwdHistoryVal.indexOf( DELIMITER, nextPos );
        length = Integer.parseInt( pwdHistoryVal.substring( nextPos, pos ) );

        data = pwdHistoryVal.substring( pos + 1 );
    }


    public PasswordHistory( String time, byte[] password )
    {
        this.time = time;
        this.data = String.valueOf( Base64.encode( password ) );
        this.length = data.length();
    }


    public byte[] getHistoryValue()
    {
        StringBuilder sb = new StringBuilder();

        sb.append( time ).append( DELIMITER );

        sb.append( syntaxOID ).append( DELIMITER );

        sb.append( length ).append( DELIMITER );

        sb.append( data );

        return Strings.getBytesUtf8(sb.toString());
    }


    public String getTime()
    {
        return time;
    }


    public String getSyntaxOID()
    {
        return syntaxOID;
    }


    public int getLength()
    {
        return length;
    }


    public byte[] getPassword()
    {
        return Base64.decode( data.toCharArray() );
    }


    public int compareTo( PasswordHistory o )
    {
        return o.getTime().compareTo( time );
    }


    @Override
    public boolean equals( Object o )
    {
        if ( !( o instanceof PasswordHistory ) )
        {
            return false;
        }

        PasswordHistory other = ( PasswordHistory ) o;

        return this.getTime().equals( other.getTime() ) &&
               this.data.equals( other.data );
    }


    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( data == null ) ? 0 : data.hashCode() );
        result = prime * result + length;
        result = prime * result + ( ( syntaxOID == null ) ? 0 : syntaxOID.hashCode() );
        result = prime * result + ( ( time == null ) ? 0 : time.hashCode() );
        return result;
    }


    @Override
    public String toString()
    {
        return "PasswordHistory [time=" + time + ", syntaxOID=" + syntaxOID + ", length=" + length + ", data=" + data
            + "]";
    }
}
