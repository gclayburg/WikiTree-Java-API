/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.examples;

import com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession;
import com.matilda.wikitree.api.util.WikiTreeApiUtilities;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;

/**
 Take the JSON-based WikiTree Java API out for a test drive.
 */

public class JsonApiTestDrive {

    public static void main( String[] args ) {

        // Create a JSON-based API session.

	WikiTreeApiJsonSession request = new WikiTreeApiJsonSession();

	// Login using the info in the authentication file specified on the command line.
	// First line contains the email address associated with your WikiTree account
	// and the second line contains that account's WikiTree password.
	// If there is no file specified on the command line then

	WikiTreeApiUtilities.maybeLoginToWikiTree( request, args );

	try {

	    JSONObject churchillPersonByWikiTreeID = request.getPerson( "Churchill-4" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getPerson for Churchill-4 (WikiTree ID for W. S. Churchill)",
		    churchillPersonByWikiTreeID
	    );

	    JSONObject churchillPersonByPersonId = request.getPerson( "5589" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
	    	"getPerson for 5589 (Person.Id for W. S. Churchill)",
		churchillPersonByPersonId
	    );

	    JSONObject nonExistentPerson = request.getPerson( "Churchill-4548988" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
	    	"getPerson for non-existent Churchill-4548988",
		nonExistentPerson
	    );

	    JSONObject churchillProfileByWikiTreeID = request.getProfile( "Churchill-4" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getProfile for Churchill-4 (WikiTree ID for W. S. Churchill)",
		    churchillProfileByWikiTreeID
	    );

	    JSONObject alliedPowCampsSpaceProfileByPageName = request.getProfile( "Space:Allied_POW_camps" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getProfile for Space:Allied_POW_camps",
		    alliedPowCampsSpaceProfileByPageName
	    );

	    JSONObject alliedPowCampsSpaceProfileByPageId = request.getProfile( "7933538" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getProfile for Profile Id 7933358 (Space:Allied_POW_camps)",
		    alliedPowCampsSpaceProfileByPageId
	    );

	    JSONObject churchillBioByWikiTreeID = request.getBio( "Churchill-4" );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getBio for Churchill-4",
		    churchillBioByWikiTreeID
	    );

	    JSONObject churchillsAncestors = request.getAncestors( "Churchill-4", 3 );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getAncestors to depth of 3 for Churchill-4 (WikiTree ID for W. S. Churchill)",
		    churchillsAncestors
	    );

	    JSONObject churchillsRelatives = request.getRelatives( "Churchill-4", true, true, true, true );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getRelatives (parents, children, spouses, siblings) of Churchill-4",
		    churchillsRelatives
	    );

	    JSONObject churchillsChildrenAndSpouses = request.getRelatives( "Churchill-4", false, true, true, false );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getRelatives children and spouses of Churchill-4",
		    churchillsChildrenAndSpouses
	    );

	    JSONObject parentsOfChurchillAndRoosevelt = request.getRelatives( "5589,Roosevelt-1", true, false, false, false );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "getRelatives parents of 5589 (W. S. Churchill) and Roosevelt-1 (F. D. Roosevelt)",
		    parentsOfChurchillAndRoosevelt
	    );

	    JSONObject watchlist = request.getWatchlist( true, true, null, null, null, 10, null, null );
	    WikiTreeApiUtilities.prettyPrintJsonThing(
		    "watchlist of authenticated user for this session (space and person profiles, limit of 10)",
		    watchlist
	    );

	    return;

	} catch ( IOException | ParseException e ) {

	    e.printStackTrace();

	}

	// Add a final word (mostly to provide a place for a final breakpoint).

	System.out.println( "done" );

    }
}
