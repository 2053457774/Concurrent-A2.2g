package nuber.students;
import java.util.Random;

public class Driver extends Person {
	Passenger curPassenger;
	public Driver(String driverName, int maxSleep)
	{
		super(driverName,maxSleep);
	}
	
	/**
	 * Stores the provided passenger as the driver's current passenger and then
	 * sleeps the thread for between 0-maxDelay milliseconds.
	 * 
	 * @param newPassenger Passenger to collect
	 * @throws InterruptedException
	 */
	public void pickUpPassenger(Passenger newPassenger) throws InterruptedException
	{
		curPassenger = newPassenger;
		//sleep while picking up passenger
		Thread.sleep(new Random().nextInt(0,this.maxSleep));
	}

	/**
	 * Sleeps the thread for the amount of time returned by the current 
	 * passenger's getTravelTime() function
	 * 
	 * @throws InterruptedException
	 */
	public void driveToDestination() throws InterruptedException{
		int travelTime = curPassenger.getTravelTime();
		Thread.sleep(travelTime);
	}
	
}
