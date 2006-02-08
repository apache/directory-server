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
package org.apache.directory.shared.ldap.ldif ;


/**
 * LDIF Iterator monitor adapter.
 *
 * @author <a href="mailto:dev@directory.apache.org">
 * Apache Directory Project</a>
 * @version $Rev$
 */
public class LdifIteratorMonitorAdapter implements LdifIteratorMonitor
{

    /* (non-Javadoc)
     * @see org.apache.ldap.common.ldif.LdifIteratorMonitor#fatalFailure(
     * java.lang.String, java.lang.Throwable)
     */
    public void fatalFailure( String msg, Throwable cause )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.ldap.common.ldif.LdifIteratorMonitor#failure(
     * java.lang.String, java.lang.Throwable)
     */
    public void failure( String msg, Throwable cause )
    {
    }

    
    /* (non-Javadoc)
     * @see org.apache.ldap.common.ldif.LdifIteratorMonitor#infoAvailable(
     * java.lang.String)
     */
    public void infoAvailable( String msg )
    {
    }
}
