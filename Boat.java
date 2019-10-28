package nachos.threads;
import nachos.ag.BoatGrader;

public class Boat
{
    static BoatGrader bg;

    public static void selfTest()
    {
	BoatGrader b = new BoatGrader();

	System.out.println("\n ***Testing Boats with only 2 children***");
	begin(0, 2, b);

//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
//  	begin(1, 2, b);

//  	System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
//  	begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
	// Store the externally generated autograder in a class
	// variable to be accessible by children.
	bg = b;

	// Instantiate global variables here

  weLiveInASociety = true;
  childOahu = children; // Only accessed by Oahu
  adultOahu = adults; // Only accessed by Oahu
  childMolokai = 0; // Only accessed by Molokai
  adultMolokai = 0; // Only accesses by Molokai
  lastSeenOahuAdults = 0; // Not accurate, but used in Finish to check if the simulation is over
	lastSeenOahuChildren = 0; // Not accurate, but used in Finish to check if the simulation is over
  boatOahu = true; // The boat is on Oahu at the start
  boatMolokai = false; // The boat is not on Oahu at the start
  boatEmpty = true; // check if the boat is empty before getting on
  onlyLock = new Lock(); // protect code
  private static Condition adultOnOahu = new Condtion(onlyLock); // Condition for adults on Oahu
  private static Condition adultOnMolokai = new Condtion(onlyLock); // Condition for adults on Molokai
  private static Condition childOnOahu = new Condtion(onlyLock); // Condition for children on Oahu
  private static Condition childOnMolokai = new Condtion(onlyLock); // Condition for children on Molokai
  // Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
  for(int i=0;i<adults;i++){
  Runnable r = new Runnable() {
	    public void run() {
                AdultItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Adult Oahu Boat Thread");
        t.fork();
  }

  for(int i=0;i<children;i++){
	Runnable r = new Runnable() {
	    public void run() {
                ChildItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Child Oahu Boat Thread");
        t.fork();

    }
  }

    static void AdultItinerary()
    {
	/* This is where you should put your solutions. Make calls
	   to the BoatGrader to show that it is synchronized. For
	   example:
	       bg.AdultRowToMolokai();
	   indicates that an adult has rowed the boat across to Molokai
	*/
   onlyLock.acquire();
      while(weLiveInASociety){
		//if the Boat is  on Oahu , it's empty, and there is 1 child
    if(boatOahu){
      if(boatEmpty){
    if(childOahu == 1){ // boatOahu is true, boat is empty, and childOahu == 1.
      adultOnOahu.wakeAll();
      adultOahu -= 1;
      boatEmpty = false;
      boatOahu = false;
      bg.AdultRowToMolokai();
      }
    }
  }else{
      adultMolokai += 1;
      boatEmpty = true;
      childOnMolokai.wakeAll();
      adultOnOahu.sleep();
    }

  }
onlyLock.release();
  }

    static void ChildItinerary()
    {
      onlyLock.acquire();
      while(weLiveInASociety){
        // A child will get on the boat if it is on the island Oahu, if it is empty, and there is more than one child on the island
        if(boatOahu){
        if(boatEmpty){
        if(childOahu > 1){ // boatOahu is true, boat is empty, and childOahu > 1.
          childOnOahu.wakeAll();
          lastSeenOahuAdults = adultOahu;
          lastSeenOahuChildren = childOahu;
          bg.ChildRideToMolokai();
          childOahu -= 1;
          bg.ChildRowToMolokai();
          childOahu -=1;
          boatOahu=false;
          boatEmpty = false;

        // A child will get on the boat if it is on the island Oahu, if it is empty, and there are no longer adults on the island.
      }else if(adultOahu == 0){  // boatOahu is true, boat is empty, and adultOahu is 0.
          bg.ChildRowToMolokai();
          childOahu -= 1;
          boatOahu = false;
          boatEmpty = false;
          childMolokai += 1;
          weLiveInASociety = false;
          childOnMolokai.sleep();

        // if the boat is not on Oahu, then it is on Molokai, and it is empty because
          }
        }
      }else{
          if(lastSeenOahuAdults > 0){
          childMolokai +=1;
          childMolokai +=1;
          }
          boatEmpty = true;
          bg.ChildRowToOahu();
          childOahu +=1;
          boatOahu = true;
          childOnMolokai.sleep();
        }
      }
      onlyLock.release();
    }


    static void SampleItinerary()
    {
	// Please note that this isn't a valid solution (you can't fit
	// all of them on the boat). Please also note that you may not
	// have a single thread calculate a solution and then just play
	// it back at the autograder -- you will be caught.
	System.out.println("\n ***Everyone piles on the boat and goes to Molokai***");
	bg.AdultRowToMolokai();
	bg.ChildRideToMolokai();
	bg.AdultRideToMolokai();
	bg.ChildRideToMolokai();
    }

  private static Lock onlyLock;
  private static boolean weLiveInASociety;
  private static boolean boatOahu;
  private static boolean boatMolokai;
  private static boolean boatEmpty;
  private static int childOahu;
  private static int adultOahu;
  private static int childMolokai = 0;
  private static int adultMolokai = 0;
  private static int lastSeenOahuAdults = 0;
	private static int lastSeenOahuChildren = 0;

}
