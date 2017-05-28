package xyz.kenosee.wikitree.api;

import com.sun.corba.se.spi.monitoring.StatisticsAccumulator;
import org.jetbrains.annotations.NotNull;
import org.json.simple.parser.*;
import org.json.simple.*;
import xyz.kenosee.wikitree.api.exceptions.ReallyBadNewsError;
import xyz.kenosee.wikitree.api.util.MiscUtilities;

import java.io.*;
import java.net.*;
import java.util.Collection;
import java.util.Date;
import java.util.Vector;

/**
 A Java implementation of the WikiTree API.
 <p/>See <a href="https://www.wikitree.com/wiki/Help:API_Documentation">https://www.wikitree.com/wiki/Help:API_Documentation</a>
 for more information about the original WikiTree API (intended for use within web pages).
 */

@SuppressWarnings({ "WeakerAccess", "unused", "SameParameterValue" })
public class WikiTreeApiClient {

    /**
     The default base URL used to access the WikiTree API server.
     <p/>This URL will be augmented with appropriate query parameters for each request.
     For example, a {@code getPerson} request for Winston S. Churchill would use the following URL to make the request to the default WikiTree API server:
     <blockquote>{@code https://apps.wikitree.com/api.php?format=json&action=getPerson&fields=*&key=Churchill-4}</blockquote>
     <p/>See {@link #WikiTreeApiClient(String)} if you want to use a different URL.
     */

    public static final String DEFAULT_BASE_SERVER_URL_STRING = "https://apps.wikitree.com/api.php";

    private static final StatisticsAccumulator s_innerRequestStats = new StatisticsAccumulator( "ms" );

    private static boolean s_usingGet = false;

    private static int _miniServerPort;

    private static String _miniServerUrlString;

    private String _baseServerUrlString;

    private Vector<String> _loginCookies;

    /**
     Create a reusable anonymous WikiTree API client instance which sends its requests to the production WikiTree API server.
     <p/>Requests made via an anonymous client instance are only able to access WikiTree information which is publicly available.
     If you want access to the same information that you are able to access on www.WikiTree.com when you've logged in using your WikiTree account
     then you must convert this anonymous client instance into an authenticated client instance using your WikiTree account (i.e. email address) and associated password.
     <p/>See {@link #login(String, String)} for more information.
     */

    public WikiTreeApiClient() {
        this( null );

    }

    /**
     Create a reusable anonymous WikiTree API client instance which sends its requests to a specified WikiTree API server.
     @param baseServerUrlString the URL of the specified WikiTree API server.
     <p/>See {@link #WikiTreeApiClient()} for more information about anonymous vs authenticated WikiTree API client instances.
     */

    public WikiTreeApiClient( String baseServerUrlString ) {

        _baseServerUrlString = baseServerUrlString == null ? DEFAULT_BASE_SERVER_URL_STRING : baseServerUrlString;

    }

    /**
     Get this instance's server URL.
     @return the base URL that this instance will augment when processing API requests.
     */

    public String getBaseServerUrlString() {

        return _baseServerUrlString;

    }

    /**
     Determine whether this instance will use http GET style queries or http POST style queries to service API requests.
     <p/>The current implementation of this API only supports the use of http GET style queries. Consequently, this method always
     returns {@code true}.
     <p/>Some future version of this software <b><u>MIGHT</u></b> provide the option to use http POST style queries.
     It should be noted that the likelihood that some future version of this software will actually support http POST style queries
     is really quite unlikely.
     It should also be noted that while there is some code within this class which purports to support http POST style queries, it is
     at best very preliminary and very unlikely to actually work properly.
     @return The current implementation of this method always returns {@code true} indicating that requests will be implemented using
     http GET style queries.
     */

    public static boolean usingGet() {

        return s_usingGet;

    }

    /**
     Login to the WikiTree API server.
     <p/>This method sends a 'login' request to this instance's WikiTree API server.
     If successful, this instance becomes an authenticated WikiTree API client instance with respect to this instance's WikiTree API server.
     If unsuccessful, this instance either remains or becomes an anonymous WikiTree API client instance with respect to this instance's WikiTree API server.
     <p/>See {@link #WikiTreeApiClient()} for more information about anonymous vs authenticated WikiTree API client instances.
     <p/>See {@link #WikiTreeApiClient(String)} for more info on using alternative WikiTree API servers.
     @param emailAddress the email address associated with a www.wikitree.com account.
     @param password the password associated with the same www.wikitree.com account.
     @return {@code true} if the login request succeeded (valid www.wikitree.com account email address and associated password and no "technical difficulties"
     between here and the WikiTree API server.
     <p/>Note that if this method returns false then this instance is now an anonymous WikiTree API client instance regardless of whether or not it was an
     authenticated WikiTree API client instance before it called this method.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    public boolean login( @NotNull String emailAddress, @NotNull String password )
	    throws IOException, ParseException {

	@SuppressWarnings("UnnecessaryLocalVariable")
	JSONObject rval = login( emailAddress, password, "*" );

	/*
	Note that a successful login request results in the server sending us http cookies which must be provided on all future
	calls to the server via this instance. These cookies must be grabbed by on of the rather low level routines that this method
	calls (quite indirectly). Consequently, the only sure way to determine if this call actually worked is to check if this instance has
	references to these login cookies. This check must be done just before this method returns. Consequently, we do it here.
	 */

	return _loginCookies != null;

    }

    /**
     A version of {@link #login(String, String)} which is not intended to be used by those responsible for creating and maintaining this API.
     It is probably best to avoid this method.
     @param emailAddress the email address associated with a www.wikitree.com account.
     @param password the password associated with the same www.wikitree.com account.
     @param fields a list of the fields that should be returned by the server for this request.
     Note that a login request which is successful on the server may fail on the client if the certain fields are not returned by the server
     (did I mention that it is probably best to avoid this method?).
     @return the JSONObject returned by the server in response to this request.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject login( @NotNull String emailAddress, @NotNull String password, @NotNull String fields )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "login" );
	requestParams.put( "email", emailAddress );
	requestParams.put( "password", password );
	requestParams.put( "fields", fields );
	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	/*
	 Figure out what species of fish we got back.
	 Note that unlike most if not all of the other API calls, this call gets a JSONObject back from the server.
	 */

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return (JSONObject)resultObject;

	}

    }

    /**
     Request information about a specified person (someone with a WikiTree profile).
     @param key the specified person's WikiTree id or Person.Id.
     This parameter will be treated as a Person.Id if it can be successfully parsed by {@link Integer#parseInt(String)}.
     For example, specify either {@code "Churchill-4"} (his WikiTree ID) or {@code "5589"} (his Person.Id) to request information about Winston S. Churchill (UK Prime Minister during the Second World War).
     @return A JSONObject containing the profile information for the specified person, their parents, their siblings, and their children.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    public JSONObject getPerson( @NotNull String key )
	    throws IOException, ParseException {

        @SuppressWarnings("UnnecessaryLocalVariable")
	JSONObject rval = getPerson( key, "*" );

	return rval;

    }

    /**
     Request information about a specified person (someone with a WikiTree profile).
     @param key the specified person's WikiTree id or Person.Id.
     This parameter will be treated as a Person.Id if it can be successfully parsed by {@link Integer#parseInt(String)}.
     For example, specify either {@code "Churchill-4"} (his WikiTree ID) or {@code "5589"} (his Person.Id) to request information about Winston S. Churchill (UK Prime Minister during the Second World War).
     @param fields a comma separated list of the fields that you want returned. Specifying {@code "*"} will get you all the available fields.
     @return A JSONObject containing the profile information for the specified person, their parents, their siblings, and their children.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject getPerson( @NotNull String key, String fields )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getPerson" );
	requestParams.put( "key", interpretIdParameter( "getPerson", key ) );
	requestParams.put( "fields", fields );
	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    private Object interpretIdParameter( String who, @NotNull String key ) {

        if ( key.length() == 0 ) {

            throw new IllegalArgumentException( who + ":  key must not be an empty string" );

	} else {

            try {

                @SuppressWarnings("UnnecessaryLocalVariable")
		int personId = Integer.parseInt( key );

                return personId;

	    } catch ( NumberFormatException e ) {

                return key;

	    }

	}

    }

    /**
     Request a person profile or a Free-Space Profile.
     <p/>See the <b>getProfile</b> section
     <a href="https://www.wikitree.com/wiki/Help:API_Documentation">https://www.wikitree.com/wiki/Help:API_Documentation</a>
     for more information about requesting profiles.
     @param key The Person.Name (e.g. {@code "Churchill-4"}), the Space.page_name (e.g. {@code "Space:Allied_POW_camps"), or the PageId
     (e.g. {@code "7933538"}) of the profile of interest.
     @return A JSONObject containing the requested information. %%% Need to document what sort of information is returned and how it is organized.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     @return a JSONObject containing the requested information.
     @throws IOException
     @throws ParseException
     */
    @SuppressWarnings("unchecked")
    public JSONObject getProfile( String key )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getProfile" );
	requestParams.put( "key", key );
	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    /**
     Request the biography for a specified person (someone with a WikiTree profile).
     @param key the specified person's WikiTree id or Person.Id.
     This parameter will be treated as a Person.Id if it can be successfully parsed by {@link Integer#parseInt(String)}.
     For example, specify either {@code "Churchill-4"} (his WikiTree ID) or {@code "5589"} (his Person.Id) to request the biography section of Winston S. Churchill's profile.
     @return A JSONObject containing the specified person's biography.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject getBio( String key )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getBio" );
	requestParams.put( "key", key );
	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    /**
     Request the profile's which are on the logged-in user's watchlist.
     <p/>Note that calls to this method will result in an {@link IllegalArgumentException} being thrown if the anonymous WikiTree API client instance.
     @param getPerson if {@code true} or {@code null} then person profiles on the user's watchlist will be returned;
     otherwise, if {@code false} then person profiles are excluded.
     @param getSpace if {@code true} or {@code null} then space profiles on the user's watchlist will be returned;
     if {@code false} then space profiles are excluded.
     @param onlyLiving if {@code true} then only person profiles of living people on the user's watchlist will be returned (i.e. person profiles for dead people will be excluded);
     otherwise, if {@code false} or {@code null} then person profiles for both living and dead people on the user's watchlist will be returned.
     @param excludeLiving if {@code true} then only person profiles of dead people on the user's watchlist will be returned (i.e. person profiles for living people will be excluded);
     otherwise, if {@code false} or {@code null} then person profiles for both living and dead people on the user's watchlist will be returned.
     This parameter will be treated as a Person.Id if it can be successfully parsed by {@link Integer#parseInt(String)}.
     @param fields a comma separated list of the fields to be returned; if {@code "*"} or {@code null} then all fields are returned.
     @param limit if {@code null} then a maximum of 100 profiles are returned; Otherwise, specifies the maximum number of profiles to be returned.
     @param offset if {@code null} then all profiles (up to the limit specified by the <b>limit</b>} parameter) are returned;
     otherwise, specifies how many profiles should be skipped.
     @param order if {@code null} then the returned profiles are sorted by their <b>user_id</b>; otherwise, the returned profiles are sorted by the field named by this parameter.
     @return A JSONObject containing the requested profiles.
     @throws IllegalArgumentException if this method is called on an anonymous WikiTree API client instance.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject getWatchlist(
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

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getWatchlist" );

	if ( getPerson != null ) {

	    requestParams.put( "getPerson", getPerson.booleanValue() ? 1 : 0 );

	}

	if ( getSpace != null ) {

	    requestParams.put( "getSpace", getSpace.booleanValue() ? 1 : 0 );

	}

	if ( onlyLiving != null && onlyLiving.booleanValue() ) {

	    requestParams.put( "onlyLiving", 1 );

	}

	if ( excludeLiving != null && excludeLiving.booleanValue() ) {

	    requestParams.put( "excludeLiving", 1 );

	}

	if ( fields != null ) {

	    requestParams.put( "fields", fields );

	}

	if ( limit != null ) {

	    requestParams.put( "limit", limit.intValue() );

	}

	if ( offset != null ) {

	    requestParams.put( "offset", offset.intValue() );

	}

	if ( order != null ) {

	    requestParams.put( "order", order );

	}

	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    /**
     Request the ancestors of a specified person (someone with a WikiTree profile).
     @param key the specified person's WikiTree id or Person.Id.
     This parameter will be treated as a Person.Id if it can be successfully parsed by {@link Integer#parseInt(String)}.
     For example, specify either {@code "Churchill-4"} (his WikiTree ID) or {@code "5589"} (his Person.Id) to request the biography section of Winston S. Churchill's profile.
     @param depth how many generations back to retrieve. if {@code null} then a depth of 5 is used. Valid values are 1-10.
     @return A JSONObject containing the specified person's ancestors.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject getAncestors( String key, Integer depth )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getAncestors" );
	requestParams.put( "key", key );

	if ( depth != null ) {

	    requestParams.put( "depth", depth.intValue() );

	}

	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    /**
     Request the immediate relatives of a specified person (someone with a WikiTree profile).
     @param keys a comma separated list of WikiTree id or Person.Id values for the profiles to return relatives for.
     Elements of the list will be treated as a Person.Id if they can be successfully parsed by {@link Integer#parseInt(String)}.
     For example, specifying {@code "5589,Hozier-1"} would return the relatives of Sir Winston S. Churchill (Person.Id 5589) and his wife Baroness Clementine Churchill (Hozier-1).
     @param getParents {@code true} returns parents of the specified profiles.
     @param getChildren {@code true} returns children of the specified profiles.
     @param getSpouses {@code true} returns spouses of the specified profiles.
     @param getSiblings {@code true} returns siblings of the specified profiles.
     @return A JSONObject containing the specified person's ancestors.
     @throws IOException if an IOException is thrown by the networking facilities used to send and receive the login request.
     @throws ParseException if this client is unable to process the response from the WikiTree API server. Seeing this exception should be a rather rare occurrence.
     If you do see one, you have probably encountered a bug in this software. Please notify danny@matilda.com if you get this exception (be prepared to work with Danny
     to reproduce the problem).
     */

    @SuppressWarnings("unchecked")
    public JSONObject getRelatives( String keys, boolean getParents, boolean getChildren, boolean getSpouses, boolean getSiblings )
	    throws IOException, ParseException {

	JSONObject requestParams = new JSONObject();
	requestParams.put( "action", "getRelatives" );
	requestParams.put( "keys", keys );
	requestParams.put( "getParents", getParents ? 1 : 0 );
	requestParams.put( "getChildren", getChildren ? 1 : 0 );
	requestParams.put( "getSpouses", getSpouses ? 1 : 0 );
	requestParams.put( "getSiblings", getSiblings ? 1 : 0 );
	requestParams.put( "format", "json" );

	JSONObject resultObject = makeRequest( requestParams );

	if ( resultObject == null ) {

	    return null;	// No such fish.

	} else {

	    return resultObject;

	}

    }

    public JSONObject makeRequest( JSONObject requestObject )
	    throws IOException, ParseException {

	System.out.println( "... starting @ " + MiscUtilities.formatStandardMs( new Date() ) );
        long startTime = System.currentTimeMillis();
        JSONObject rval = s_usingGet ? requestViaJsonPost( requestObject ) : requestViaHttpGet( requestObject );
        long endTime = System.currentTimeMillis();
	System.out.println( "... done @ " + MiscUtilities.formatStandardMs( new Date() ) );

	synchronized ( s_innerRequestStats ) {

	    double delta = ( endTime - startTime ) / 1000.0;
	    s_innerRequestStats.sample( delta );
	    System.out.println( "delta = " + delta );

	}

        return rval;

    }

    private synchronized JSONObject requestViaHttpGet( JSONObject requestObject )
	    throws IOException, ParseException {

        String who = "requestViaHttpGet";

        String what = "formatting request URL";

	StringBuffer requestSb = new StringBuffer( _baseServerUrlString );
	MiscUtilities.formatRequestAsUrlQueryParameters( who, requestObject, requestSb );

        String actualUrlString = requestSb.toString();

        if ( actualUrlString.contains( "action=login" ) ) {

            System.out.println( "not showing URL for login request (protects the password)" );

	} else {

	    System.out.println( "URL will be " + actualUrlString );

	}

	URL actualUrl = new URL( actualUrlString );

	what = "initializing connection";

	URLConnection urlConnection = actualUrl.openConnection();
	HttpURLConnection connection = (HttpURLConnection)urlConnection;
	connection.setDoOutput( false );
	connection.setDoInput( true );
	connection.setRequestProperty( "Accept", "application/json" );
	if ( !"login".equals( requestObject.get( "action" ) ) && _loginCookies != null ) {

	    System.out.println( "it's not a login attempt and we have cookies" );

	    StringBuilder sb = new StringBuilder();
	    String semiColon = "";
	    for ( String cookie : _loginCookies ) {

	        sb.append( semiColon ).append( cookie.split(";", 2)[0] );
	        semiColon = "; ";

	    }

	    connection.addRequestProperty( "Cookie", sb.toString() );

	    System.out.println( "done adding in cookies" );

	}

	connection.setRequestMethod( "GET" );

	what = "getting response";

	Object rval = MiscUtilities.readResponse( connection, true );

	if ( "login".equals( requestObject.get( "action" ) ) ) {

	    System.out.println( "It's a login attempt" );

	    // Throw away the old cookies regardless of whether or not the login request worked.

	    _loginCookies = null;

	    try {

		JSONObject rvalAsJsonObject = (JSONObject) rval;
		if ( rvalAsJsonObject == null ) {

		    System.err.println( "got null response from login attempt for " + requestObject.get( "email" ) );

		} else {

		    JSONObject loginObject = (JSONObject)rvalAsJsonObject.get( "login" );
		    if ( loginObject == null ) {

			System.err.println( "got response from login attempt for " + requestObject.get( "email" ) + " which doesn't contain a \"login\" response object" );

		    } else {

			String resultString = (String)loginObject.get( "result" );
			if ( "Success".equals( resultString ) ) {

			    System.out.println( "login worked!" );

			    Collection<String> cookies = connection.getHeaderFields().get( "Set-Cookie" );
			    if ( cookies != null ) {

				_loginCookies = new Vector<String>( cookies );

				for ( String cookie : _loginCookies ) {

				    System.out.println( "cookie:  " + MiscUtilities.enquoteForJavaString( cookie ) );

				}

				System.out.println( "done printing cookies" );

			    }

			}

		    }

		}

	    } catch ( RuntimeException e ) {

	        System.err.println( "unable to tear apart login rval:  " + rval );

		e.printStackTrace();

		throw e;

	    }

	}

	if ( rval == null || rval instanceof JSONObject ) {

	    return (JSONObject)rval;

	} else {

	    throw new ReallyBadNewsError( "requestViaJsonGet:  expected a JSONObject, got this instead:  " + rval );

	}
    }

    private JSONObject requestViaJsonPost( JSONObject requestObject )
	    throws IOException, ParseException {

        String requestString = JSONObject.toJSONString( requestObject );

        String what = null;

	what = "initializing connection";

	URL serverUrl = new URL( _baseServerUrlString );

	URLConnection urlConnection = serverUrl.openConnection();
	HttpURLConnection connection = (HttpURLConnection)urlConnection;
	connection.setDoOutput( true );
	connection.setDoInput( true );
	connection.setRequestProperty( "Content-Type", "application/json; charset=UTF-8" );
	connection.setRequestProperty( "Accept", "application/json" );
	connection.setRequestMethod( "POST" );

	what = "posting request";

	OutputStream writer = connection.getOutputStream();
	try {

	    writer.write( ( requestString + '\n' ).getBytes( "UTF-8" ) );
	    writer.flush();

	} finally {

	    writer.close();
	    System.out.println( "writer closed" );

	}

	what = "getting response";

	@SuppressWarnings("UnnecessaryLocalVariable")
	Object rval = MiscUtilities.readResponse( connection, true );

	if ( rval == null || rval instanceof JSONObject ) {

	    return (JSONObject)rval;

	} else {

	    throw new ReallyBadNewsError( "requestViaJsonPost:  expected a JSONObject, got this instead:  " + rval );

	}

    }

    private static void doit( String what, Object result )
	    throws IOException {

        MiscUtilities.prettyPrintJsonThing( what, result );

    }

    public static void main( String[] args ) {

	WikiTreeApiClient request = new WikiTreeApiClient();

	request.maybeLoginToWikiTree( args );

	try {

	    doit( "getPerson for Churchill-4 (WikiTree id for W. S. Churchill)", request.getPerson( "Churchill-4" ) );

	    doit( "getPerson for 5589 (Person.Id for W. S. Churchill)", request.getPerson( "5589" ) );

	    doit( "getPerson for non-existent Churchill-4548988", request.getPerson( "Churchill-4548988" ) );

	    doit( "getProfile for Churchill-4 (WikiTree id for W. S. Churchill)", request.getProfile( "Churchill-4" ) );

	    doit( "getProfile for Space:Allied_POW_camps", request.getProfile( "Space:Allied_POW_camps" ) );

	    doit( "getProfile for Profile Id 7933358 (Space:Allied_POW_camps)", request.getProfile( "7933538" ) );

	    doit( "getPerson for Boulet-169 (author of this API, a living person (private with public bio and family tree))", request.getPerson( "Boulet-169" ) );
	    doit( "getProfile for Boulet-169 (author of this API, a living person (private with public bio and family tree))", request.getProfile( "Boulet-169" ) );
	    doit( "getBio for Boulet-169 (author of this API, a living person (private with public bio and family tree))", request.getBio( "Boulet-169" ) );

	    doit( "getAncestors to depth of 3 for Churchill-4 (WikiTree id for W. S. Churchill)", request.getAncestors( "Churchill-4", 3 ) );

	    doit( "getRelatives children and spouses of Churchill-4 (W. S. Churchill)", request.getRelatives( "Churchill-4", false, true, true, false ) );

	    doit( "getRelatives parents of 5589 (W. S. Churchill) and Roosevelt-1 (F. D. Roosevelt)", request.getRelatives( "5589,Roosevelt-1", true, false, false, false ) );

	    doit(
	    	"watchlist of authenticated user for this session limit of 10",
		request.getWatchlist( true, true, null, null, null, 10, null, null )
	    );

	    return;

	} catch ( IOException e ) {

	    e.printStackTrace();

	} catch ( ParseException e ) {

	    e.printStackTrace();

	}

	return;

    }

    /**
     Try to turn this into an authenticated client instance if the name of a WikiTree user info file was provided to us.
     <p/>This method is intended to only be used to construct test software for this WikiTree Java API.
     It is also used within the fairly primitive sanity test code in our {@link #main} method.
     Production software should invoke {@link #login(String, String)} or {@link #login(String, String, String)} directly.
     <p/>A WikiTree user info file must satisfy all of these requirements:
     <ul>
     <li>the file must be a two line text file.</li>
     <li>the file's name must end with {@code ".wtu"}.</li>
     <li>The first line must contain an email address that is associated with a WikiTree.com account.
     Any leading or trailing whitespace on this line is ignored.</li>
     <li>The second line must contain the password for the WikiTree account associated with the email address on the first line.
     Neither leading nor trailing space on this line is ignored (it isn't our job to impose password rules).</li>
     </ul>
     @param args the args provided when this JVM started up.
     */

    public void maybeLoginToWikiTree( String[] args ) {

	if ( args.length == 0 ) {

	    System.out.println( "no user info file specified on command line, proceeding as an anonymous user" );

	} else if ( args.length == 1 ) {

	    String userInfoFileName = args[0];

	    if ( !userInfoFileName.endsWith( ".wtu" ) ) {

	        System.err.println( "WikiTree user info file specified on the command line does not have a \".wtu\" suffix - bailing out" );

	        System.exit( 1 );
	    }

	    System.out.println( "using WikiTree user info file at " + userInfoFileName );

	    try {

		LineNumberReader lnr = new LineNumberReader( new FileReader( userInfoFileName ) );

		String userName = lnr.readLine();
		if ( userName == null ) {

		    System.out.flush();
		    System.err.println( "user info file \"" + userInfoFileName + "\" is empty" );
		    System.exit( 1 );

		}
		userName = userName.trim();

		String password = lnr.readLine();
		if ( password == null ) {

		    System.out.flush();
		    System.err.println( "user info file \"" + userInfoFileName + "\" only has one line (first line must be an email address; second line must be WikiTree password for that email address)" );
		    System.exit( 1 );

		}

		boolean loginResponse = login( userName, password );
		if ( loginResponse ) {

		    System.out.println( "authenticated session for \"" + userName + " \" created" );

		} else {

		    System.out.flush();
		    System.err.println( "unable to create authenticated session for \"" + userName + "\" (probably incorrect user name or incorrect password; could be network problems or maybe even an invasion of space aliens)" );
		    System.err.flush();
		    System.out.println( "first line of " + userInfoFileName + " must contain the email address that you use to login to WikiTree" );
		    System.out.println( "second line of " + userInfoFileName + " must contain the WikiTree password for that email address" );
		    System.out.println( "leading or trailing whitespace on the email line is ignored" );
		    System.out.println( "IMPORTANT:  leading or trailing whitespace on the password line is NOT ignored" );
		    System.out.flush();
		    System.exit( 1 );

		}

	    } catch ( FileNotFoundException e ) {

	        System.out.flush();
	        System.err.println( "unable to open user info file - " + e.getMessage() );
		System.exit( 1 );

	    } catch ( ParseException e ) {

		System.out.flush();
	        System.err.println( "unable to parse response from server (probably a bug; notify danny@matilda.com)" );
		e.printStackTrace();
		System.exit( 1 );

	    } catch ( IOException e ) {

		System.out.flush();
	        System.err.println( "something went wrong in i/o land" );
		e.printStackTrace();
		System.exit( 1 );

	    }

	} else {

	    System.err.println( "you must specify either no parameter or one parameter" );
	    System.exit( 1 );

	}

    }

}
