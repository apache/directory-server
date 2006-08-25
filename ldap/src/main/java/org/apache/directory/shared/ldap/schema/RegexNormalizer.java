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


import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A Normalizer that uses Perl5 based regular expressions to normalize values.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RegexNormalizer implements Normalizer
{
    /** the perl 5 regex engine */
    private final Pattern[] regexes;

    /** the set of regular expressions used to transform values */
    private final Matcher[] matchers;


    /**
     * Creates a Perl5 regular expression based normalizer.
     * 
     * @param regexes
     *            the set of regular expressions used to transform values
     */
    public RegexNormalizer(Pattern[] regexes)
    {
        this.regexes = regexes;
        matchers = new Matcher[regexes.length];

        for ( int i = 0; i < regexes.length; i++ )
        {
            matchers[i] = regexes[i].matcher( "" );
        }
    }


    /**
     * @see org.apache.directory.shared.ldap.schema.Normalizer#normalize(Object)
     */
    public Object normalize( final Object value )
    {
        if ( value == null )
        {
            return null;
        }

        if ( value instanceof String )
        {
            String str = ( String ) value;

            for ( int i = 0; i < matchers.length; i++ )
            {

                str = matchers[i].replaceAll( str );
            }

            return str;
        }

        return value;
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "RegexNormalizer( " );

        for ( int i = 0; i < regexes.length; i++ )
        {
            buf.append( regexes[i] );

            if ( i < regexes.length - 1 )
            {
                buf.append( ", " );
            }
        }

        buf.append( " )" );
        return buf.toString();
    }
}
