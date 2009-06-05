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
package org.apache.directory.shared.ldap.schema.syntaxes;

import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.schema.AbstractSyntaxChecker;


/**
 * A SyntaxChecker which verifies that a value is a Jpeg according to RFC 4517.
 * 
 * The JFIF (Jpeg File Interchange Format) specify that a jpeg image starts with
 * the following bytes :
 * 0xFF 0xD8 (SOI, Start Of Image)
 * 0xFF 0xE0 (App0)
 * 0xNN 0xNN (Header length)
 * "JFIF\0" (JFIF string with an ending \0)
 * some other bytes which are related to the image.
 * 
 * We will check for those 11 bytes, except the length.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 437007 $
 */
public class JpegSyntaxChecker extends AbstractSyntaxChecker
{
    /**
     * 
     * Creates a new instance of JpegSyntaxChecker.
     *
     */
    public JpegSyntaxChecker()
    {
        super( SchemaConstants.JPEG_SYNTAX );
    }
    
    
    /**
     * 
     * Creates a new instance of JpegSyntaxChecker.
     * 
     * @param oid the oid to associate with this new SyntaxChecker
     *
     */
    protected JpegSyntaxChecker( String oid )
    {
        super( oid );
    }
    
    /* (non-Javadoc)
     * @see org.apache.directory.shared.ldap.schema.SyntaxChecker#isValidSyntax(java.lang.Object)
     */
    public boolean isValidSyntax( Object value )
    {
        if ( value == null )
        {
            return false;
        }
        
        // The value must be a byte array
        if ( ! ( value instanceof byte[] ) )
        {
            return false;
        }
        
        byte[] bytes = (byte[])value;

        // The header must be at least 11 bytes long
        if ( bytes.length < 11 )
        {
            return false;
        }

        if ( ( bytes[0] == (byte)0x00FF ) && // SOI
             ( bytes[1] == (byte)0x00D8 ) &&
             ( bytes[2] == (byte)0x00FF ) && // APP0
             ( bytes[3] == (byte)0x00E0 ) &&
             ( bytes[6] == 'J' ) && // JFIF
             ( bytes[7] == 'F' ) && // JFIF
             ( bytes[8] == 'I' ) && // JFIF
             ( bytes[9] == 'F' ) &&
             ( bytes[10] == 0x00 ) ) // \0
        {
            // Note : this is not because the header is correct
            // that the file is a jpeg file. There are much more
            // elements to check, but we are not writing a jpeg
            // file checker...
            return true;
        }

        return false;
    }
}
