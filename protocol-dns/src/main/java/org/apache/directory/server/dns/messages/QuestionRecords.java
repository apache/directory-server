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

package org.apache.directory.server.dns.messages;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class QuestionRecords
{
    private List<QuestionRecord> questionRecords;


    public QuestionRecords()
    {
        this.questionRecords = new ArrayList<QuestionRecord>();
    }


    public QuestionRecords( int initialCapacity )
    {
        this.questionRecords = new ArrayList<QuestionRecord>( initialCapacity );
    }


    public void add( QuestionRecord question )
    {
        questionRecords.add( question );
    }


    public int size()
    {
        return questionRecords.size();
    }


    public Iterator<QuestionRecord> iterator()
    {
        return questionRecords.iterator();
    }


    public String toString()
    {
        StringBuffer sb = new StringBuffer();

        for ( QuestionRecord question:questionRecords )
        {
            sb.append( "\n\t" + "dnsName                    " + question.getDomainName() );
            sb.append( "\n\t" + "dnsType                    " + question.getRecordType() );
            sb.append( "\n\t" + "dnsClass                   " + question.getRecordClass() );
        }

        return sb.toString();
    }
}
