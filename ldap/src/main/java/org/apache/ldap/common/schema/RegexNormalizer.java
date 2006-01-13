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
package org.apache.ldap.common.schema;


import org.apache.oro.text.perl.Perl5Util ;


/**
 * A Normalizer that uses Perl5 based regular expressions to
 * normalize values.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class RegexNormalizer implements Normalizer
{
    /** the perl 5 regex engine */
    private final Perl5Util perl = new Perl5Util() ;
    
    /** the set of regular expressions used to transform values*/
    private String [] regexes ;


    /**
     * Creates a Perl5 regular expression based normalizer.
     *
     * @param regexes the set of regular expressions used to transform values
     */
    public RegexNormalizer( String [] regexes )
    {
        this.regexes = regexes ;
    }


    /**
     * @see org.apache.ldap.common.schema.Normalizer#normalize(Object)
     */
    public Object normalize( final Object value )
    {
        if ( value == null )
        {
            return null;
        }
        
        if ( value instanceof String )
        {
            String str = ( String ) value ;
    
            for ( int ii = 0; ii < regexes.length; ii++ )
            {
                str = perl.substitute( regexes[ii], str ) ;
            }
    
            return str ;
        }
        
        return value ;
    }


    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer() ;
        buf.append( "RegexNormalizer( " ) ;

        for ( int ii = 0; ii < regexes.length; ii++ )
        {
            buf.append( regexes[ii] );

            if ( ii < regexes.length - 1 )
            {
                buf.append( ", " );
            }
        }

        buf.append( " )" ) ;
        return buf.toString() ;
    }
}
