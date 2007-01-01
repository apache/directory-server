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
package org.apache.directory.shared.ldap.schema;


import javax.naming.NamingException;

import org.apache.directory.shared.ldap.util.StringTools;


/**
 * Normalizer which trims down whitespace replacing multiple whitespace
 * characters on the edges and within the string with a single space character
 * thereby preserving tokenization order - while doing all this in the same pass
 * it lower cases all characters.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class DeepTrimToLowerNormalizer implements Normalizer
{
    public static final long serialVersionUID = 1L;

    public Object normalize( Object value ) throws NamingException
    {
        if ( value == null )
        {
            return null;
        }

        if ( value instanceof byte[] )
        {
            return StringTools.deepTrimToLower( StringTools.utf8ToString( ( byte[] ) value ) );
        }
        else
        {
            return StringTools.deepTrimToLower( ( String ) value );
        }
    }
}
