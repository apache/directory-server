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
package org.apache.directory.server.core.api.log;


import java.util.Comparator;

import org.apache.directory.server.i18n.I18n;


/**
 * A {@link Comparator} that compares {@link LogAnchor} objects.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class LogAnchorComparator implements Comparator<LogAnchor>
{
    /**
     * Compare two {@link LogAnchor} objects.
     *
     * @param obj1 First object
     * @param obj2 Second object
     * @return a positive integer if obj1 > obj2, 0 if obj1 == obj2,
     *         and a negative integer if obj1 < obj2
     */
     public int compare( LogAnchor obj1, LogAnchor obj2 )
     {
        if ( obj1 == null ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_525 ) );
        }

        if ( obj2 == null ) 
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_526 ) );
        }

        long logFileNumber1 = obj1.getLogFileNumber();
        long logFileOffset1 = obj1.getLogFileOffset();
        long logFileNumber2 = obj2.getLogFileNumber();
        long logFileOffset2 = obj2.getLogFileOffset();
        
        if ( logFileNumber1 > logFileNumber2 )
        {
            return 1;
        }
        else if ( logFileNumber1 == logFileNumber2 )
        {
            if ( logFileOffset1 > logFileOffset2 )
            {
                return 1;
            }
            else if ( logFileOffset1 == logFileOffset2 )
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        else
        {
            return -1;
        }
    }
}
