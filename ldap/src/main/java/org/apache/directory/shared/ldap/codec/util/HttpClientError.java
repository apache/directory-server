/*
 * $Header: /home/jerenkrantz/tmp/commons/commons-convert/cvs/home/cvs/jakarta-commons//httpclient/src/java/org/apache/commons/httpclient/HttpClientError.java,v 1.4 2004/05/13 04:03:25 mbecke Exp $
 * $Revision: 155418 $
 * $Date: 2005-02-26 08:01:52 -0500 (Sat, 26 Feb 2005) $
 *
 * ====================================================================
 *
 *  Copyright 2002-2004 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.directory.shared.ldap.codec.util;


/**
 * Signals that an error has occurred.
 * 
 * @author Ortwin Gl?ck
 * @version $Revision: 155418 $ $Date: 2005-02-26 08:01:52 -0500 (Sat, 26 Feb
 *          2005) $
 * @since 3.0
 */
public class HttpClientError extends Error
{
    final static long serialVersionUID = 1L;


    /**
     * Creates a new HttpClientError with a <tt>null</tt> detail message.
     */
    public HttpClientError()
    {
        super();
    }


    /**
     * Creates a new HttpClientError with the specified detail message.
     * 
     * @param message
     *            The error message
     */
    public HttpClientError(String message)
    {
        super( message );
    }

}
