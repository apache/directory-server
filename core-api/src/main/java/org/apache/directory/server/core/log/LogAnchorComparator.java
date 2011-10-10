
package org.apache.directory.server.core.log;

import java.util.Comparator;

import org.apache.directory.server.i18n.I18n;

public class LogAnchorComparator implements Comparator<LogAnchor>
{
    /**
     * Compare two log anchors.
     *
     * @param obj1 First object
     * @param obj2 Second object
     * @return a positive integer if obj1 > obj2, 0 if obj1 == obj2,
     *         and a negative integer if obj1 < obj2
     */
     public int compare( LogAnchor obj1, LogAnchor obj2 )
     {
        if ( obj1 == null ) {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_525 ) );
        }

        if ( obj2 == null ) {
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
