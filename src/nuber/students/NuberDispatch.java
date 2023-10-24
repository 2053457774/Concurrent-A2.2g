package nuber.students;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.LinkedList;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
/**
 * The core Dispatch class that instantiates and manages everything for Nuber
 * 
 * @author james
 *
 */
public class NuberDispatch {

	/**
	 * The maximum number of idle drivers that can be awaiting a booking 
	 */
	private final int MAX_DRIVERS = 999;
	
	private boolean logEvents = false;

	HashMap<String,Integer> regionInfo;
	HashMap<String,NuberRegion> regionTable;
	private Queue<Driver> idleDrivers;
	private Lock lock;
	private Condition producerCondition;
	private Condition consumerCondition;
	//represent the numbers of bookings waiting for drivers.
	private AtomicLong AwaitingBooks;
	
	/**
	 * Creates a new dispatch objects and instantiates the required regions and any other objects required.
	 * It should be able to handle a variable number of regions based on the HashMap provided.
	 * 
	 * @param regionInfo Map of region names and the max simultaneous bookings they can handle
	 * @param logEvents Whether logEvent should print out events passed to it
	 */
	public NuberDispatch(HashMap<String, Integer> regionInfo, boolean logEvents)
	{
		this.regionInfo = regionInfo;
		regionTable = new HashMap<>();
		for(Map.Entry<String, Integer> entry : regionInfo.entrySet()){
			String regionName = entry.getKey();
			int maxSimultaneousJobs = entry.getValue();
			NuberRegion region = new NuberRegion(this,regionName,maxSimultaneousJobs);
			regionTable.put(regionName,region);
		}
		idleDrivers = new LinkedList<>();// Initialize a LinkedList to store idle drivers

		lock = new ReentrantLock();
		producerCondition = lock.newCondition();// Initialize a Condition object for the producer
		consumerCondition = lock.newCondition();// Initialize a Condition object for the consumer
		AwaitingBooks = new AtomicLong(0);
		this.logEvents = logEvents;
	}
	
	/**
	 * Adds drivers to a queue of idle driver.
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @param The driver to add to the queue.
	 * @return Returns true if driver was added to the queue
	 */
	public boolean addDriver(Driver newDriver)
	{
		lock.lock();
		boolean flag = true;
		flag = idleDrivers.offer(newDriver);// Add the new driver to the idleDrivers queue and update the flag based on the success of the operation
		consumerCondition.signal();// Signal the consumer that an item is available
		lock.unlock();
		return flag;
	}
	
	/**
	 * Gets a driver from the front of the queue
	 *  
	 * Must be able to have drivers added from multiple threads.
	 * 
	 * @return A driver that has been removed from the queue
	 */
	public Driver getDriver()
	{
		Driver driver = null;
		lock.lock();// Acquire the lock to ensure thread safety
		try {
			AwaitingBooks.getAndIncrement();// Increase the count of awaiting books
			while (idleDrivers.isEmpty()) {
				consumerCondition.await(); //Wait until there are available idle drivers
			}
			// Consume an item from the buffer
			AwaitingBooks.getAndDecrement();// Retrieve a driver from the pool of idle drivers
			driver =  idleDrivers.poll();
			// Signal the producer that there is space in the buffer
		}catch(InterruptedException e){}//Handle any interuption that might occur
		 during the awaiting process
		finally {
			lock.unlock();// Release the lock to allow other threads to access the method
		}
		return driver;
	}

	/**
	 * Prints out the string
	 * 	    booking + ": " + message
	 * to the standard output only if the logEvents variable passed into the constructor was true
	 * 
	 * @param booking The booking that's responsible for the event occurring
	 * @param message The message to show
	 */
	public void logEvent(Booking booking, String message) {
		
		if (!logEvents) return;
		
		System.out.println(booking + ": " + message);
		
	}

	/**
	 * Books a given passenger into a given Nuber region.
	 * 
	 * Once a passenger is booked, the getBookingsAwaitingDriver() should be returning one higher.
	 * 
	 * If the region has been asked to shutdown, the booking should be rejected, and null returned.
	 * 
	 * @param passenger The passenger to book
	 * @param region The region to book them into
	 * @return returns a Future<BookingResult> object
	 */
	public Future<BookingResult> bookPassenger(Passenger passenger, String region) {
		//obtain the region and mak booking
		NuberRegion givenRegion = regionTable.get(region);
		return givenRegion.bookPassenger(passenger);
	}

	/**
	 * Gets the number of non-completed bookings that are awaiting a driver from dispatch
	 * 
	 * Once a driver is given to a booking, the value in this counter should be reduced by one
	 * 
	 * @return Number of bookings awaiting driver, across ALL regions
	 */
	public int getBookingsAwaitingDriver()
	{
		return AwaitingBooks.intValue();
	}
	
	/**
	 * Tells all regions to finish existing bookings already allocated, and stop accepting new bookings
	 */
	public void shutdown() {
		//shutdown all threadpools
		for (var region : regionTable.values()) {
			region.shutdown();
		}
	}

}
