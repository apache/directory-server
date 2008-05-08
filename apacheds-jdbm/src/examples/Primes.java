
import jdbm.RecordManager;
import jdbm.RecordManagerFactory;
import jdbm.RecordManagerOptions;

import jdbm.btree.BTree;

import jdbm.helper.LongComparator;
import jdbm.helper.Tuple;
import jdbm.helper.TupleBrowser;

import java.util.Properties;
import java.util.Random;
import java.io.IOException;

/**
 * Example program dealing with B+Trees and Prime numbers.
 */
public class Primes
{

    /**
     * Default number of prime number to populate in the database (if not specified on command-line)
     */
    public static int DEFAULT_POPULATE = 100;


    /**
     * Default number of random lookups (if not specified on command-line)
     */
    public static int DEFAULT_LOOKUPS = 100;

    
    /**
     * Record Manager used for persistence.
     */
    private RecordManager _recman;

    
    /**
     * B+Tree holding prime numbers.
     */
    private BTree _primes;
    
    
    /**
     * Random number generator.
     */
    private static Random  _random = new Random();


    /**
     * Main constructor
     */
    public Primes( String[] args ) 
        throws IOException 
    {
        long        recid;
        Properties  props;
        
        // open database and setup an object cache
        props = new Properties();
        props.put( RecordManagerOptions.CACHE_SIZE, "10000" );
        _recman = RecordManagerFactory.createRecordManager( "primes", props );

        recid = _recman.getNamedObject( "primes" );
        if ( recid == 0 ) {
            System.out.println( "Creating a new primes B+Tree." );
            _primes = BTree.createInstance( _recman, new LongComparator() );
            _recman.setNamedObject( "primes", _primes.getRecid() );
        } else {
            _primes = BTree.load( _recman, recid );
            System.out.println( "B+Tree already contains " + _primes.size() + " primes." );
        }
        _recman.commit();
    }

    
    /**
     * Get the largest prime number in the database.
     */
    public Long getLargestPrime()
        throws IOException
     {
        Tuple         tuple;
        TupleBrowser  browser;
        Long          largest = null;

        tuple = new Tuple();
        browser = _primes.browse( null );
        if ( browser.getPrevious( tuple ) ) {
            largest = (Long) tuple.getValue();
            System.out.println( "Largest prime: " + largest );
        } else {
            System.out.println( "No prime number in the database." );
        }
        return largest;
    }

    
    /**
     * Populate the database with more prime numbers.
     *
     * @param count Number of primes to add to database.
     */
    void populate( int count ) 
        throws IOException 
    {
        Long current;
        Long largest;

        System.out.println( "Populating prime B+Tree..." );
        
        // start after the largest known prime
        largest = getLargestPrime();
        if ( largest == null ) {
            largest = new Long( 0 );
        }

        current = new Long( largest.longValue() + 1L ); 
        while ( count > 0 ) {
            if ( isPrime( current ) ) {
                _primes.insert( current, current, false );
                System.out.println( "Found prime #" + _primes.size() + ": " + current );
                count--;
            }
            current = new Long( current.longValue() + 1 );
        }
        _recman.commit();
    }

    
    /**
     * Returns true if a number is prime.
     */
    boolean isPrime( Long number )
        throws IOException
    {
        Tuple         tuple;
        TupleBrowser  browser;
		Long          largest;
        Long          current;

        if ( number.longValue() <= 0L ) {
            throw new IllegalArgumentException( "Number must be greater than zero" );
        }
        if ( number.longValue() == 1 ) {
	    	return true;
		}
        tuple = new Tuple();
        browser = _primes.browse();
        while ( browser.getNext( tuple ) ) {
            current = (Long) tuple.getValue();
            if ( current.longValue() != 1 && ( number.longValue() % current.longValue() ) == 0 ) {
                // not a prime because it is divisibe by a prime
                return false;
            }
        }           
        // this is a prime
        return true;
    }

        
    /**
     * Display a number of random prime numbers.
     */
    void random( int count ) 
        throws IOException
    {
        Tuple         tuple;
        TupleBrowser  browser;
        Long          largest;
        Long          number;

        tuple = new Tuple();
        largest = getLargestPrime();

        System.out.println( "Looking up " + count + " random primes...." );
        long start = System.currentTimeMillis();
        for ( int i=0; i<count; i++ ) {
            number = new Long( random( 0, largest.longValue() ) );
            browser = _primes.browse( number );
            if ( browser.getNext( tuple ) ) {
                number = (Long) tuple.getValue();
                System.out.print( number );
                System.out.print( ", " );
            }
        }
        long stop = System.currentTimeMillis();
        System.out.println();
        System.out.println( "Time: " + (stop-start)/count + " millis/lookup " );
    }


    /**
     * Return true if number is a prime.
     */
    public static boolean isPrimeCompute( long number )
    {
        for ( int i=2; i<number/2; i++ ) {
            if ( ( number % i ) == 0 ) {
                return false;
            }
        }
        return true;
    }


    /**
     * Get random number between "low" and "high" (inclusively)
     */
    public static long random( long low, long high )
    {
        return ( (long) ( _random.nextDouble() * (high-low) ) + low );
    }


    /**
     * Static program entrypoint.
     */
    public static void main( String[] args )
    {
        Primes  primes;
        int     count;
        Long    number;
		Long    largest;
            
        try {
            primes = new Primes( args );
            
            for ( int i=0; i<args.length; i++ ) {
                if ( args[i].equalsIgnoreCase( "-populate" ) ) {
                    if ( ++i < args.length ) {
                        count = Integer.parseInt( args[i] );
                    } else {
                        count = DEFAULT_POPULATE;
                    }
                    primes.populate( count );
                } else if ( args[i].equalsIgnoreCase( "-check" ) ) {
                    if ( ++i < args.length ) {
                        number = new Long( Long.parseLong( args[i] ) );
                    } else {
                        number = new Long( _random.nextLong() );
                    }
				    largest = primes.getLargestPrime();
		    		if ( number.longValue() > primes.getLargestPrime().longValue() ) {
						throw new IllegalArgumentException( "Number is larger than largest known prime in database." );
		    		}
                    if ( primes.isPrime( number ) ) {
                        System.out.println( "The number " + number + " is a prime." );
                    } else {
                        System.out.println( "The number " + number + " is not a prime." );
                    }
                } else if ( args[i].equalsIgnoreCase( "-random" ) ) {
                    if ( ++i < args.length ) {
                        count = Integer.parseInt( args[i] );
                    } else {
                        count = DEFAULT_LOOKUPS;
                    }
                    primes.random( count );
                }
            }
            if ( args.length == 0 ) {
                System.out.println( "Usage:   java Prime [action] [args]" );
                System.out.println( "" );
                System.out.println( "Actions:" );
                System.out.println( "           -populate [number]   Populate database with prime numbers" );
                System.out.println( "           -check [number]      Check if a number is a prime" );
                System.out.println( "           -random [number]     Display random prime numbers" );
                System.out.println( "" );
            }
        } catch ( IOException except ) {
            except.printStackTrace();
        }
    }
}
