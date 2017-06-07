/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.wrappers;

import com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession;
import com.matilda.wikitree.api.util.WikiTreeApiUtilities;
import com.sun.corba.se.spi.monitoring.StatisticsAccumulator;
import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import com.matilda.wikitree.api.WikiTreeApiClient;

import java.io.IOException;

/**
 A somewhat higher level API that operates as a layer on top of the API provided by {@link WikiTreeApiJsonSession}.

 <p/>Some examples . . .
 <p/>Note that a {@code WikiTreeApiJsonSession} can be wrapped at any time.
 For example, the {@code WikiTreeApiJsonSession} could be wrapped upon creation like this:
 <blockquote>
 {@code WikiTreeApiWrappersSession wrapper = new WikiTreeApiWrappersSession( new WikiTreeApiJsonSession() );}
 </blockquote>
 or, probably more sensibly, like this:
 <blockquote>
 {@code WikiTreeApiWrappersSession wrapper = new WikiTreeApiWrappersSession();}
 </blockquote>
 Alternatively, a {@code WikiTreeApiJsonSession} instance could be used as is for a while and then later wrapped like this:
 <blockquote>
 {@code WikiTreeApiJsonSession jsonClient = new WikiTreeApiJsonSession();}<br>
 .<br>
 . <em>(various things including method calls on the {@code jsonClient} variable happen here)</em><br>
 .<br>
 {@code WikiTreeApiWrappersSession wrapper = new WikiTreeApiWrappersSession( jsonClient );}
 </blockquote>
 Once wrapped, either the {@code WikiTreeApiWrappersSession} wrapper or the wrapped {@code WikiTreeApiJsonSession} can be used to make API calls.
 For example, one could do the following:
 <blockquote>
 {@code WikiTreeApiJsonSession jsonClient = new WikiTreeApiJsonSession();}<br>
 {@code WikiTreeApiWrappersSession wrapper = new WikiTreeApiWrappersSession( jsonClient );}<br>
 JSONObject profileObject = jsonClient.get( "Churchill-4" );<br>
 {@code WikiTreePersonProfile churchillFamilyGroup = wrapper.getPerson( "Churchill-4" );}<br>
 </blockquote>
 One could even mimic a call to {@code getPerson} as follows:
 <blockquote>
 {@code WikiTreeApiJsonSession jsonClient = new WikiTreeApiJsonSession();}<br>
 {@code JSONObject jsonChurchillFamily = jsonClient.getPerson( "Churchill-4" );}<br>
 {@code WikiTreePersonProfile wrappedFamilyGroup = new WikiTreePersonProfile( null, jsonChurchillFamily, "person" );}
 </blockquote>
 */

@SuppressWarnings({ "WeakerAccess", "SameParameterValue" })
public class WikiTreeApiWrappersSession implements WikiTreeApiClient {

    private final WikiTreeApiJsonSession _jsonClient;

    private boolean _authenticated = false;

    /**
     Wrap a new {@link WikiTreeApiJsonSession} instance.
     */

    public WikiTreeApiWrappersSession() {
	this( new WikiTreeApiJsonSession() );

    }

    /**
     Wrap an existing {@code WikiTreeApiJsonSession} instance.

     @param wikiTreeApiJsonSession the {@code WikiTreeApiJsonSession} which this instance is to wrap.
     */

    public WikiTreeApiWrappersSession( @NotNull WikiTreeApiJsonSession wikiTreeApiJsonSession ) {
	super();

	_jsonClient = wikiTreeApiJsonSession;

    }

    /**
     Get the email address of the WikiTree user for whom the wrapped {@link WikiTreeApiJsonSession} is authenticated.
     @return the email address of the user for whom this session is authenticated; {@code null} if this session is not authenticated.
     <p/>See {@link WikiTreeApiJsonSession#getAuthenticatedUserEmailAddress()} for more information.
     */

    @Override
    public String getAuthenticatedUserEmailAddress() {

	return _jsonClient.getAuthenticatedUserEmailAddress();

    }

    /**
     Get the WikiTree ID of the user for whom this session is authenticated.
     @return the WikiTree ID of the user for whom this session is authenticated; {@code null} if this session is not authenticated.
     <p/>See {@link WikiTreeApiJsonSession#getAuthenticatedWikiTreeId()} for more information.
     */

    @Override
    public String getAuthenticatedWikiTreeId() {

	return _jsonClient.getAuthenticatedWikiTreeId();

    }

    /**
     Login to the WikiTree API server.
     <p/>Assuming that {@code jsonClient} refers to a {@link WikiTreeApiJsonSession} instance,
     this method is exactly equivalent to
     <blockquote>
     {@code jsonClient.login( }<em>{@code emailAddress}</em>{@code , }<em>{@code password}</em>{@code );}
     </blockquote>
     See {@link WikiTreeApiJsonSession#login(String, String)} for more information.
     @param emailAddress the email address associated with a www.wikitree.com account.
     @param password the password associated with the same www.wikitree.com account.
     @return {@code true} if the login worked; {@code false otherwise}.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server (definitely see {@link WikiTreeApiJsonSession#login(String, String)} for more information).
     */

    public boolean login( @NotNull String emailAddress, @NotNull String password )
	    throws IOException, ParseException {

	if ( _jsonClient.login( emailAddress, password ) ) {

	    _authenticated = true;

	} else {

	    _authenticated = false;
	    System.err.println( "authentication failed for \"" + emailAddress + "\"" );

	}

	return isAuthenticated();

    }

    @Override
    public String getBaseServerUrlString() {

	return getWrappedJsonWikiTreeApiClient().getBaseServerUrlString();

    }

    @Override
    public String getLoginResultStatus() {

	return _jsonClient.getLoginResultStatus();

    }

    /**
     Get the {@link WikiTreeApiJsonSession} instance that this instance wraps.
     @return the {@link WikiTreeApiJsonSession} instance that this instance wraps.
     */

    public WikiTreeApiJsonSession getWrappedJsonWikiTreeApiClient() {

        return _jsonClient;

    }

    @Override
    public boolean isAuthenticated() {

        return _authenticated;

    }

    public WikiTreePersonProfile getPerson( @NotNull String key )
	    throws IOException, ParseException {

	@SuppressWarnings("UnnecessaryLocalVariable")
	WikiTreePersonProfile rval = getPerson( key, "*" );

	return rval;

//	if ( rval == null ) {
//
//	    return null;
//
//	} else {
//
//	    return new WikiTreePersonFamilyGroup( rval );
//
//	}

    }

    public WikiTreePersonProfile getPerson( @NotNull String key, String fields )
	    throws IOException, ParseException {

	JSONObject resultObject = _jsonClient.getPerson( key, fields );
	if ( resultObject == null ) {

	    return null;

	} else {

	    return new WikiTreePersonProfile( null, resultObject, "person" );

	}

    }

    public WikiTreeProfile getProfile( String key )
	    throws IOException, ParseException {

	JSONObject resultObject = _jsonClient.getProfile( key );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    WikiTreeProfile rval;

	    rval = WikiTreeProfile.distinguish( resultObject );

	    return rval;

	}

    }

    public WikiTreePersonProfile getPersonProfile( String key )
	    throws IOException, ParseException {

	WikiTreeProfile profile = getProfile( key );
	if ( profile == null || profile instanceof WikiTreePersonProfile ) {

	    return (WikiTreePersonProfile) profile;

	} else {

	    throw new IllegalArgumentException( "WikiTreeApiWrappersSession.getPersonProfile:  key \"" + key + "\" refers to a Space, not a Person" );

	}

    }

    public WikiTreeBiography getBio( String key )
	throws IOException, ParseException {

        JSONObject resultObject = _jsonClient.getBio( key );

        if ( resultObject == null ) {

            return null;

	} else {

            return new WikiTreeBiography( key, resultObject );

	}

    }

    @SuppressWarnings("unchecked")
    public WikiTreeWatchlist getWatchlist(
	    Boolean getPerson,
	    Boolean getSpace,
	    Boolean onlyLiving,
	    Boolean excludeLiving,
	    String fields,
	    Integer limit,
	    Integer offset,
	    String order
    )
	    throws IOException, ParseException {

	JSONObject resultObject = _jsonClient.getWatchlist( getPerson, getSpace, onlyLiving, excludeLiving, fields, limit, offset, order );

	if ( resultObject == null ) {

	    return null;

	} else {

//	    WikiTreeApiUtilities.prettyPrintJsonThing( "got the watchlist", resultObject );

	    return new WikiTreeWatchlist( getPerson, getSpace, onlyLiving, excludeLiving, fields, limit, offset, order, resultObject );

//	    return "got it";

	}

    }

    public WikiTreeAncestors getAncestors( String key, Integer depth )
	    throws IOException, ParseException {

	JSONObject requestObject = _jsonClient.getAncestors( key, depth );

	if ( requestObject == null ) {

	    return null;

	} else {

	    return new WikiTreeAncestors( key, depth, requestObject );

	}
    }

    public WikiTreeRelatives getRelatives( String keys, boolean getParents, boolean getChildren, boolean getSpouses, boolean getSiblings )
	    throws IOException, ParseException {

	JSONObject requestObject = _jsonClient.getRelatives( keys, getParents, getChildren, getSpouses, getSiblings );

	if ( requestObject == null ) {

	    return null;

	} else {

	    @SuppressWarnings("UnnecessaryLocalVariable")
	    WikiTreeRelatives rval = new WikiTreeRelatives( keys, getParents, getChildren, getSpouses, getSiblings, requestObject );
//	    System.out.println( "relatives are " + rval );

	    return rval;

	}

    }

    public static StatisticsAccumulator getTimingStats() {

        return WikiTreeApiJsonSession.getTimingStats();

    }

    public String toString() {

        return "WikiTreeApiWrappersSession( url=" + getBaseServerUrlString() + " )";

    }

}
