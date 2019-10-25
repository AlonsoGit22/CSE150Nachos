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

  int weLiveInASociety = true;
  int childOahu = children;
  int adultOahu = adults;
  int boatOahu = true;
  int boatMolokai = false;
  int boatEmpty = true;


	// Create threads here. See section 3.4 of the Nachos for Java
	// Walkthrough linked from the projects page.
  for(i=0;i<adults;i++){
  Runnable r = new Runnable() {
	    public void run() {
                AdultItinerary();
            }
        };
        KThread t = new KThread(r);
        t.setName("Adult Oahu Boat Thread");
        t.fork();
  }

  for(i=0;i<children;i++){
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
lck.acquire();
  while(weLiveInASociety){

    if(boatOahu, boat, childOahu <= 1){ // boatOahu is true, boat is empty, and childOahu <= 1.

      Wakeall.Adult();
      adultOahu -= 1;
      bg.AdultRowToMolokai();

    }else{
      sleep.Adults;
    }

  }
lck.release();
  }

    static void ChildItinerary()
    {

      lck.acquire();
      while(weLiveInASociety){
        if(boatOahu,boatEmpty, childOahu > 1){ // boatOahu is true, boat is empty, and childOahu > 1.
          Wakeall.Children();
          bg.ChildRideToMolokai();
          childOahu -= 1;
          bg.ChildRowToMolokai();
          childOahu -=1;
          boatOahu=false;

        }else if(boatOahu,boatEmpty, adultOahu = 0){  // boatOahu is true, boat is empty, and adultOahu is 0.
          bg.ChildRowToMolokai();
          childOahu -= 1;
          sleep.Children();
          begin();
        }else{
          bg.ChildRowToOahu();
          childOahu +=1;
          sleep.childrenMolokai();
        }
      }
      lck.realease();
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


}
