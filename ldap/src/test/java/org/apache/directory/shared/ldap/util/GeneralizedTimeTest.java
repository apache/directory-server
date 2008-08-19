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
package org.apache.directory.shared.ldap.util;


import java.text.ParseException;
import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.apache.directory.shared.ldap.schema.syntax.GeneralizedTimeSyntaxCheckerTest;
import org.apache.directory.shared.ldap.util.GeneralizedTime.Format;
import org.apache.directory.shared.ldap.util.GeneralizedTime.TimeZoneFormat;


/**
 * Tests the DateUtils class methods.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 542077 $
 */
public class GeneralizedTimeTest extends TestCase
{

    // Test all valid variants:
    // Time: min + sec / min + no sec / no min + no sec 
    // Fraction: no fraction, dot, comma
    // Timezone: Z / +HH / +HHmm / -HH / -HHmm

    /**
     * Tests yyyyMMddHHmmssZ.
     */
    public void testYearMonthDayHourMinSecZulu() throws ParseException
    {
        String gt = "20080102121314Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss+04.
     */
    public void testYearMonthDayHourMinSecPlusHour() throws ParseException
    {
        String gt = "20080102121314+04";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss-1030.
     */
    public void testYearMonthDayHourMinSecMinusHourMin() throws ParseException
    {
        String gt = "20080102121314-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss.SSSZ.
     */
    public void testYearMonthDayHourMinSecDotFractionZulu() throws ParseException
    {
        String gt = "20080102121314.987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss.SSS+0100.
     */
    public void testYearMonthDayHourMinSecDotFractionPlusHour() throws ParseException
    {
        String gt = "20080102121314.987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss.SSS-1030.
     */
    public void testYearMonthDayHourMinSecDotFractionMinusHourMin() throws ParseException
    {
        String gt = "20080102121314.987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss,SSSZ.
     */
    public void testYearMonthDayHourMinSecCommaFractionZulu() throws ParseException
    {
        String gt = "20080102121314,987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss,SSS+0100.
     */
    public void testYearMonthDayHourMinSecCommaFractionPlusHour() throws ParseException
    {
        String gt = "20080102121314,987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmss,SSS-1030.
     */
    public void testYearMonthDayHourMinSecCommaFractionMinusHourMin() throws ParseException
    {
        String gt = "20080102121314,987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmmZ.
     */
    public void testYearMonthDayHourMinZulu() throws ParseException
    {
        String gt = "200801021213Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm+HH.
     */
    public void testYearMonthDayHourMinPlusHour() throws ParseException
    {
        String gt = "200801021213+04";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm-HHmm.
     */
    public void testYearMonthDayHourMinMinusHourMin() throws ParseException
    {
        String gt = "200801021213-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm.SSSZ.
     */
    public void testYearMonthDayHourMinDotFractionZulu() throws ParseException
    {
        String gt = "200801021213.987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm.SSS+0100.
     */
    public void testYearMonthDayHourMinDotFractionPlusHour() throws ParseException
    {
        String gt = "200801021213.987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm.SSS-1030.
     */
    public void testYearMonthDayHourMinDotFractionMinusHourMin() throws ParseException
    {
        String gt = "200801021213.987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm,SSSZ.
     */
    public void testYearMonthDayHourMinCommaFractionZulu() throws ParseException
    {
        String gt = "200801021213,987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm,SSS+0100.
     */
    public void testYearMonthDayHourMinCommaFractionPlusHour() throws ParseException
    {
        String gt = "200801021213,987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHmm,SSS-1030.
     */
    public void testYearMonthDayHourMinCommaFractionMinusHourMin() throws ParseException
    {
        String gt = "200801021213,987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHHZ.
     */
    public void testYearMonthDayHourZulu() throws ParseException
    {
        String gt = "2008010212Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH+HH.
     */
    public void testYearMonthDayHourPlusHour() throws ParseException
    {
        String gt = "2008010212+04";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH-HHmm.
     */
    public void testYearMonthDayHourMinusHourMin() throws ParseException
    {
        String gt = "2008010212-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH.SSSZ.
     */
    public void testYearMonthDayHourDotFractionZulu() throws ParseException
    {
        String gt = "200801021213.987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH.SSS+0100.
     */
    public void testYearMonthDayHourDotFractionPlusHour() throws ParseException
    {
        String gt = "2008010212.987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH.SSS-1030.
     */
    public void testYearMonthDayHourDotFractionMinusHourMin() throws ParseException
    {
        String gt = "2008010212.987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH,SSSZ.
     */
    public void testYearMonthDayHourCommaFractionZulu() throws ParseException
    {
        String gt = "2008010212,987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH,SSS+0100.
     */
    public void testYearMonthDayHourCommaFractionPlusHour() throws ParseException
    {
        String gt = "2008010212,987+0100";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests yyyyMMddHH,SSS-1030.
     */
    public void testYearMonthDayHourCommaFractionMinusHourMin() throws ParseException
    {
        String gt = "2008010212,987-1030";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests fraction of a second.
     */
    public void testFractionOfSecond() throws ParseException
    {
        String gt = "20080102121314,987Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
        assertEquals( 987, generalizedTime.getCalendar().get( Calendar.MILLISECOND ) );
    }


    /**
     * Tests fraction of a minute.
     */
    public void testFractionOfMinute1() throws ParseException
    {
        String gt = "200801021213,5Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
        assertEquals( 30, generalizedTime.getCalendar().get( Calendar.SECOND ) );
        assertEquals( 0, generalizedTime.getCalendar().get( Calendar.MILLISECOND ) );
    }


    /**
     * Tests fraction of a minute.
     */
    public void testFractionOfMinute2() throws ParseException
    {
        String gt = "200801021213,125Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
        assertEquals( 7, generalizedTime.getCalendar().get( Calendar.SECOND ) );
        assertEquals( 500, generalizedTime.getCalendar().get( Calendar.MILLISECOND ) );
    }


    /**
     * Tests fraction of an hour.
     */
    public void testFractionOfHour1() throws ParseException
    {
        String gt = "2008010212,5Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
        assertEquals( 30, generalizedTime.getCalendar().get( Calendar.MINUTE ) );
        assertEquals( 0, generalizedTime.getCalendar().get( Calendar.SECOND ) );
        assertEquals( 0, generalizedTime.getCalendar().get( Calendar.MILLISECOND ) );
    }


    /**
     * Tests fraction of an hour.
     */
    public void testFractionOfHour2() throws ParseException
    {
        String gt = "2008010212,125Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
        assertEquals( 7, generalizedTime.getCalendar().get( Calendar.MINUTE ) );
        assertEquals( 30, generalizedTime.getCalendar().get( Calendar.SECOND ) );
        assertEquals( 0, generalizedTime.getCalendar().get( Calendar.MILLISECOND ) );
    }


    /**
     * Test formatting
     */
    public void testFormatting() throws ParseException
    {
        String gt = "20080102121314Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );

        result = generalizedTime.toGeneralizedTime( Format.YEAR_MONTH_DAY_HOUR_MIN, null, 0, TimeZoneFormat.Z );
        assertEquals( "200801021213Z", result );

        result = generalizedTime.toGeneralizedTime( Format.YEAR_MONTH_DAY_HOUR, null, 0, TimeZoneFormat.Z );
        assertEquals( "2008010212Z", result );

        result = generalizedTime.toGeneralizedTime( Format.YEAR_MONTH_DAY_HOUR_MIN, null, 0,
            TimeZoneFormat.DIFF_HOUR_MINUTE );
        assertEquals( "200801021213+0000", result );

        result = generalizedTime.toGeneralizedTime( Format.YEAR_MONTH_DAY_HOUR, null, 0,
            TimeZoneFormat.DIFF_HOUR_MINUTE );
        assertEquals( "2008010212+0000", result );
    }


    /**
     * Testcases from {@link GeneralizedTimeSyntaxCheckerTest#testCorrectCase()}.
     */
    public void testGeneralizedTimeSyntaxCheckerTestCorrectCase() throws ParseException
    {
        new GeneralizedTime( "20061205184527Z" );
        new GeneralizedTime( "20061205184527+0500" );
        new GeneralizedTime( "20061205184527-1234" );
        new GeneralizedTime( "20061205184527.123Z" );
        new GeneralizedTime( "20061205184527,123+0100" );
        new GeneralizedTime( "2006120519Z" );
    }


    /**
     * Testcases from {@link GeneralizedTimeSyntaxCheckerTest#testErrorCase()}.
     */
    public void testGeneralizedTimeSyntaxCheckerTestErrorCase()
    {
        try
        {
            new GeneralizedTime( "20060005184527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061305184527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20062205184527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061200184527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061235184527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205604527Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205186027Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184561Z" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184527Z+" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184527+2400" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184527+9900" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184527+1260" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
        try
        {
            new GeneralizedTime( "20061205184527+1299" );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests leap second.
     * The GeneralizedTime class does not support leap seconds!
     */
    public void testLeapSecond() throws ParseException
    {
        String gt = "20051231235960Z";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests Feb 29 in a leap year.
     */
    public void testFebruary29inLeapYear() throws ParseException
    {
        String gt = "20080229000000Z";
        GeneralizedTime generalizedTime = new GeneralizedTime( gt );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( gt, result );
    }


    /**
     * Tests Feb 29 in a non-leap year.
     */
    public void testFebruary29inNonLeapYear() throws ParseException
    {
        String gt = "20070229000000Z";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests null.
     */
    public void testNull() throws ParseException
    {
        try
        {
            String gt = null;
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        try
        {
            Calendar calendar = null;
            new GeneralizedTime( calendar );
            fail( "Expected IllegalArgumentException" );
        }
        catch ( IllegalArgumentException iae )
        {
            // expected
        }

    }


    /**
     * Tests empty string.
     */
    public void testEmpty() throws ParseException
    {
        String gt = "";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests invalid cases.
     */
    public void testInvalid() throws ParseException
    {
        // too short year
        String gt = "200";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in year
        gt = "2XX8";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // too short month
        gt = "20081";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in month
        gt = "20081X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // too short day
        gt = "2008122";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in day
        gt = "2008122X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // too short hour
        gt = "200812211";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in hour
        gt = "20081221X1";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // too short minute
        gt = "20081221121";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in minute
        gt = "20081221121X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // too short second
        gt = "2008122112131";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // non-digits in minute
        gt = "2008122112131X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // missing time zone
        gt = "2008010212";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // missing time zone
        gt = "200801021213";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // missing time zone
        gt = "20080102121314";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no digit
        gt = "2008010212X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no digit
        gt = "200801021213X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no digit
        gt = "20080102121314X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // missing time zone
        gt = "20080102121314,1";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // time zone is not last char
        gt = "20080102121314ZX";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // time zone is not last char
        gt = "20080102121314+0430X";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }

        // no fraction digit
        gt = "20080102121314,Z";
        try
        {
            new GeneralizedTime( gt );
            fail( "Expected ParseException" );
        }
        catch ( ParseException pe )
        {
            // expected
        }
    }


    /**
     * Tests constructor with calendar object.
     */
    public void testCalendar() throws ParseException
    {
        Calendar calendar = Calendar.getInstance();
        calendar.set( Calendar.YEAR, 2008 );
        calendar.set( Calendar.MONTH, 0 );
        calendar.set( Calendar.DAY_OF_MONTH, 2 );
        calendar.set( Calendar.HOUR_OF_DAY, 12 );
        calendar.set( Calendar.MINUTE, 13 );
        calendar.set( Calendar.SECOND, 14 );
        calendar.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        GeneralizedTime generalizedTime = new GeneralizedTime( calendar );
        String result = generalizedTime.toGeneralizedTime();
        assertEquals( "20080102121314Z", result );

    }


    /**
     * Tests the compareTo() method.
     */
    public void testCompareTo() throws ParseException
    {
        String gt1 = "20080102121313,999Z";
        GeneralizedTime generalizedTime1 = new GeneralizedTime( gt1 );

        String gt2 = "20080102121314Z";
        GeneralizedTime generalizedTime2 = new GeneralizedTime( gt2 );

        String gt3 = "20080102121314,001Z";
        GeneralizedTime generalizedTime3 = new GeneralizedTime( gt3 );

        assertTrue( generalizedTime1.compareTo( generalizedTime2 ) < 0 );
        assertTrue( generalizedTime1.compareTo( generalizedTime3 ) < 0 );
        assertTrue( generalizedTime2.compareTo( generalizedTime3 ) < 0 );

        assertTrue( generalizedTime2.compareTo( generalizedTime1 ) > 0 );
        assertTrue( generalizedTime3.compareTo( generalizedTime1 ) > 0 );
        assertTrue( generalizedTime3.compareTo( generalizedTime2 ) > 0 );

        assertTrue( generalizedTime1.compareTo( generalizedTime1 ) == 0 );
        assertTrue( generalizedTime2.compareTo( generalizedTime2 ) == 0 );
        assertTrue( generalizedTime3.compareTo( generalizedTime3 ) == 0 );
    }


    /**
     * Tests the equals() method.
     */
    public void testEquals() throws ParseException
    {
        String gt1 = "20080102121314Z";
        GeneralizedTime generalizedTime1 = new GeneralizedTime( gt1 );

        String gt2 = "20080102121314Z";
        GeneralizedTime generalizedTime2 = new GeneralizedTime( gt2 );

        String gt3 = "20080102121314,001Z";
        GeneralizedTime generalizedTime3 = new GeneralizedTime( gt3 );

        assertTrue( generalizedTime1.equals( generalizedTime2 ) );
        assertFalse( generalizedTime1.equals( generalizedTime3 ) );
        assertFalse( generalizedTime1.equals( null ) );
    }
}
