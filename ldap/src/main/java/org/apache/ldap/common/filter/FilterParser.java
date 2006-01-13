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
package org.apache.ldap.common.filter ;


import java.io.IOException ;
import java.text.ParseException ;


/**
 * Parses an LDAP Filter expression as specified by RFC 2255 into a filter 
 * expression tree.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface FilterParser
{
    /**
     * Parses a search filter string into a filter expression tree. 
     *
     * @param a_filter the filter
     * @return the root node of the filter expression tree
     * @throws IOException if a pipe breaks
     * @throws ParseException if the input string does not conform to syntax
     */
    ExprNode parse( String a_filter ) throws IOException, ParseException ;

    /**
     * Sets the monitor for this filter parser.
     *  
     * @param monitor monitor that recieves parser events
     */
    void setFilterParserMonitor( FilterParserMonitor monitor ) ;
}
