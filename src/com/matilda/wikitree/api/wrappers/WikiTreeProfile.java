/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.wrappers;

import com.matilda.wikitree.api.exceptions.ReallyBadNewsError;
import com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONObject;
import com.matilda.wikitree.api.util.WikiTreeApiUtilities;

/**
 Represent some kind of WikiTree profile.
 <p/>Note that this is an abstract class.
 The implementation of this class that is used to represent a particular profile will match the type of said profile.
 For example, person profiles are always represented by {@link WikiTreePersonProfile} instances whereas
 space profiles are always represented by {@link WikiTreeSpaceProfile} instances.
 */

public abstract class WikiTreeProfile extends WikiTreeWrapper {

    private final JSONObject _originalJSONObject;

    protected WikiTreeProfile( @NotNull JSONObject jsonObject, String[] profileLocation ) {
//        super( jsonObject );
	super(
		// Apologies for using an inline-if in a call to {@code super()}.
		// This seems cleaner than the alternative of passing something arbitrary to this {@code super()} call and then
		// fixing things up later if we guessed wrong.

		// Were we handed a naked profile object?
		// I am not sure if profileLocation can be null but let's check it to avoid surprises.
		// After all, the caller's meaning is presumably clear if it is null - that the path is empty.

		profileLocation == null || profileLocation.length == 0

			?

			// Yes - remember it

			jsonObject

			:

			// No - remember the actual profile object

			(JSONObject)WikiTreeApiUtilities.getMandatoryJsonValue( JSONObject.class, jsonObject, profileLocation )

	);

	_originalJSONObject = jsonObject;

    }

    public JSONObject getOriginalJSONObject() {

        return _originalJSONObject;

    }

    /**
     Take a response to a {@link WikiTreeApiJsonSession#getProfile(String)} call and turn it into
     a {@link WikiTreePersonProfile} or a {@link WikiTreeSpaceProfile} as appropriate.
     @param jsonObject the JSON object of the form returned by a call to {@link WikiTreeApiJsonSession#getProfile(String)}.
     @return {@code null} if {@code jsonObject} is null. Otherwise, the provided {@code jsonObject} wrapped in a {@link WikiTreePersonProfile} or a {@link WikiTreeSpaceProfile} as appropriate.
     */

    @Nullable
    public static WikiTreeProfile distinguish( JSONObject jsonObject ) {

//        WikiTreeApiUtilities.prettyPrintJsonThing( "distinguishing ", jsonObject );

	WikiTreeProfile rval;
	Object responseObj = jsonObject.get( "profile" );
	if ( responseObj instanceof JSONObject ) {

	    JSONObject response = (JSONObject) responseObj;
	    JSONObject profile = response;

//	    WikiTreeApiUtilities.prettyPrintJsonThing( "profile", profile );

	    Object isPersonObj = profile.get( "IsPerson" );
	    if ( isPersonObj instanceof Number ? ((Number)isPersonObj).longValue() == 1L : "1".equals( isPersonObj ) ) {

		WikiTreePersonProfile personProfile = new WikiTreePersonProfile( null, jsonObject, "profile" );

		rval = personProfile;

	    } else {

		Object isSpaceObj = profile.get( "IsSpace" );
		if ( isSpaceObj instanceof Number ? ((Number)isSpaceObj).longValue() == 1L : "1".equals( isSpaceObj ) ) {

		    WikiTreeSpaceProfile spaceProfile = new WikiTreeSpaceProfile( jsonObject );

		    rval = spaceProfile;

		} else {

		    throw new ReallyBadNewsError( "WikiTreeProfile.distinguish:  unable to figure out what it is (" + jsonObject + ")" );

		}

	    }

	} else if ( responseObj == null ) {

	    rval = null;

	} else {

	    throw new ReallyBadNewsError( "WikiTreeApiWrappersSession.getProfile:  asked for a profile, got a " + responseObj.getClass().getCanonicalName() );

	}

	return rval;

    }

}
