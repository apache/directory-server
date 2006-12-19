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
package org.apache.directory.shared.ldap.schema.syntax;


import javax.naming.NamingException;


/**
 * Used to validate values of a particular syntax. This interface does not
 * correlate to any LDAP or X.500 construct. It has been created as a means to
 * enforce a syntax within the Eve server.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public interface SyntaxChecker
{
    /**
     * Gets the OID of the attribute syntax.
     * 
     * @return the object identifier of the Syntax this SyntaxChecker validates
     */
    String getSyntaxOid();


    /**
     * Determines if the attribute's value conforms to the attribute syntax.
     * 
     * @param value the value of some attribute with the syntax
     * @return true if the value is in the valid syntax, false otherwise
     */
    boolean isValidSyntax( Object value );


    /**
     * Asserts whether or not the attribute's value conforms to the attribute
     * syntax.
     * 
     * @param value the value of some attribute with the syntax
     * @throws NamingException if the value does not conform to the attribute syntax.
     */
    void assertSyntax( Object value ) throws NamingException;
}
