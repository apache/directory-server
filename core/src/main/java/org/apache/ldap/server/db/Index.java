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
package org.apache.ldap.server.db;


import java.math.BigInteger;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;

import org.apache.regexp.RE;

import org.apache.ldap.common.schema.AttributeType;


/**
 * Required interfaces for an index.
 *
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public interface Index
{
    /**
     * Gets the attribute this Index is built upon.
     *
     * @return the id of the Index's attribute
     */
    AttributeType getAttribute();

    /**
     * Gets the normalized value for an attribute.
     *
     * @param attrVal the user provided value to normalize
     * @return the normalized value.
     * @throws NamingException if something goes wrong.
     */
    Object getNormalized( Object attrVal ) throws NamingException;

    /**
     * Gets the total scan count for this index.
     *
     * @return the number of key/value pairs in this index
     * @throws NamingException if their is a failure accessing the index
     */
    int count() throws NamingException;

    /**
     * Gets the scan count for the occurance of a specific attribute value 
     * within the index.
     *
     * @param attrVal the value of the attribute to get a scan count for
     * @return the number of key/value pairs in this index with the value value
     * @throws NamingException if their is a failure accessing the index
     */
    int count( Object attrVal ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param isGreaterThan TODO
     * @return TODO
     * @throws NamingException TODO
     */
    int count( Object attrVal, boolean isGreaterThan ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @return TODO
     * @throws NamingException TODO
     */
    BigInteger forwardLookup( Object attrVal )  throws NamingException;

    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    Object reverseLookup( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void add( Object attrVal, BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attr TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void add( Attribute attr, BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrs TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void add( Attributes attrs, BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param entryId TODO
     * @throws NamingException TODO
     */
    void drop( BigInteger entryId ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void drop( Object attrVal, BigInteger id ) throws NamingException;
        
    /**
     * If the Attribute does not have any values then this reduces to a 
     * drop(BigInteger) call.
     *
     * @param attr TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void drop( Attribute attr, BigInteger id ) throws NamingException;
        
    /**
     * If the Attribute for this index within the Attributes does not have any 
     * values then this reduces to a drop(BigInteger) call.
     *
     * @param attrs TODO
     * @param id TODO
     * @throws NamingException TODO
     */
    void drop( Attributes attrs, BigInteger id ) throws NamingException;
        
    /**
     * TODO Document me!
     *
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listReverseIndices( BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listIndices() throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listIndices( Object attrVal ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param isGreaterThan TODO
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listIndices( Object attrVal, boolean isGreaterThan )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param regex TODO
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listIndices( RE regex ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @param regex TODO
     * @param prefix TODO
     * @return TODO
     * @throws NamingException TODO
     */
    IndexEnumeration listIndices( RE regex, String prefix )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param id TODO
     * @return  TODO
     * @throws NamingException TODO
     */
    boolean hasValue( Object attrVal, BigInteger id ) 
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param attrVal TODO
     * @param id TODO
     * @param isGreaterThan TODO
     * @return TODO
     * @throws NamingException TODO
     */
    boolean hasValue( Object attrVal, BigInteger id, boolean isGreaterThan )
        throws NamingException;

    /**
     * TODO Document me!
     *
     * @param regex TODO
     * @param id TODO
     * @return TODO
     * @throws NamingException TODO
     */
    boolean hasValue( RE regex, BigInteger id ) throws NamingException;

    /**
     * TODO Document me!
     *
     * @throws NamingException TODO
     */
    void close() throws NamingException;

    /**
     * TODO Document me!
     *
     * @throws NamingException TODO
     */
    void sync() throws NamingException;
}
