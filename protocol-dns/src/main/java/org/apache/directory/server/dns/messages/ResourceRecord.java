/*
 *   Copyright 2005 The Apache Software Foundation
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

package org.apache.directory.server.dns.messages;


public interface ResourceRecord
{
    /**
     * @return Returns the domainName.
     */
    public String getDomainName();


    /**
     * @return Returns the recordType.
     */
    public RecordType getRecordType();


    /**
     * @return Returns the recordClass.
     */
    public RecordClass getRecordClass();


    /**
     * @return Returns the timeToLive.
     */
    public int getTimeToLive();


    /**
     * @return Returns the value for an id.
     */
    public String get( String id );
}
