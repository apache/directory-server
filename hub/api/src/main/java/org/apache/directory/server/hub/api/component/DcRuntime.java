/*
 *   Licensed to the Apache Software Foundation (ASF) under one
 *   or more contributor license agreements.  See the NOTICE file
 *   distributed with this work for additional information
 *   regarding copyright ownership.  The ASF licenses this file
 *   to you under the Apache License, Version 2.0 (the
 *   "License"); you may not use this file except in compliance
 *   with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 *
 */

package org.apache.directory.server.hub.api.component;


public class DcRuntime
{
    private Object pojo;
    private Object specialObject;


    public DcRuntime( Object specialObject, Object pojo )
    {
        this.specialObject = specialObject;
        this.pojo = pojo;
    }


    public void setPojo( Object pojo )
    {
        this.pojo = pojo;
    }


    public Object getPojo()
    {
        return pojo;
    }


    public void setSpecialObject( Object specialObject )
    {
        this.specialObject = specialObject;
    }


    public Object getSpecialObject()
    {
        return specialObject;
    }
}
