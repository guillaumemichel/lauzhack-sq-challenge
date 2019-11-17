package com.swissquote.lauzhack.evolution.sq.team;

import com.swissquote.lauzhack.evolution.SwissquoteEvolutionBuilder;
import com.swissquote.lauzhack.evolution.api.MarketProfile;
import com.swissquote.lauzhack.evolution.api.SwissquoteEvolution;

public class App {

	/**
	 * This is the starter for the application.
	 * You can keep this one, or create your own (using any Framework)
	 * As long as you run a SwissquoteEvolution
	 */
	public static void main(String[] args) {
		// Instantiate our BBook
		OurBBook ourBBook = new OurBBook();
		//ourBBook.onInit();

		MarketProfile[] profiles = {MarketProfile.SOMETHING, MarketProfile.POC, MarketProfile.IT_WORKS,
		                            MarketProfile.STARTUP, MarketProfile.UNICORN};
		//int[] seeds = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		//int[] seeds = {11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
		//int[] seeds = {21};
		
		
		// Create the application runner
		SwissquoteEvolution app = new SwissquoteEvolutionBuilder().
				profile(MarketProfile.SOMETHING).
				seed(1060).
				team("SteakHashés").
				bBook(ourBBook).
				filePath("/home/goodwine/Documents/Laushack2019/new_strat/").
				interval(1).
				steps(5000).
				build();

		// Let's go !
		app.run();
		app.logBook();
		System.out.println("Yolo");

		// Display the result as JSON in console (also available in the file at "Path")
		//System.out.println(app.logBook());
	}

}
