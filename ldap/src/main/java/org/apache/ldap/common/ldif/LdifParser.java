/*
 * Copyright 2001-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ldap.common.ldif ;


import javax.naming.NamingException ;
import javax.naming.directory.Attributes ;


/**
 * Parses an ldif into a multimap or an JNDI Attributes instance of attribute
 * key/value pairs with potential more than one attribute value per attribute.
 * This parser populates the MultiMap or Attributes instance with all attributes
 * within the LDIF including control attributes like the 'dn' and the
 * 'changeType'.  These attributes are not usually part of the entry proper
 * but are a cue to the processing application.  These control attributes should
 * be accessed and removed from the MultiMap or Attributes instance if need be
 * according to the specific context in which this parser is used.
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface LdifParser
{
    /**
     * Parses an String representing an entry in LDAP Data Interchange Format
     * (LDIF) storing its attributes in the supplied Attributes instance.
     *
     * @param attributes the Attributes instance to populate with LDIF
     * attributes including the DN of the entry represented by the LDIF.
     * @param ldif the entry in LDAP Data Interchange Format
     * @throws NamingException if a naming exception results while the LDIF is
     * being parsed
     */
    void parse( Attributes attributes, String ldif ) throws NamingException ;

    /**
     * Parses an LDIF into a special LdifEntry structure that tracks control
     * attributes within an LDIF.
     *
     * @param ldif the LDIF to parse
     * @return the LdifEntry parsed 
     * @throws NamingException if a naming exception results while the LDIF is
     * being parsed
     */
    LdifEntry parse( String ldif ) throws NamingException ;
}

