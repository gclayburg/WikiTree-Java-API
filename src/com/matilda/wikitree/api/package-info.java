
/*
 * Copyright © 2017 Daniel Boulet
 */

/**
 A Java implementation of the WikiTree API.
 <p/> This API supports the same requests that the 'official' WikiTree API supports.
 See <a href="https://www.wikitree.com/wiki/Help:API_Documentation">https://www.wikitree.com/wiki/Help:API_Documentation</a> for more
 information on said 'official' WikiTree API.

 <h3>Organization</h3>
 This API is implemented in two layers:
 <ul>
 <li>The bottom layer very closely mimics the 'official' WikiTree API. Most importantly, this bottom layer API essentially just
 returns whatever the 'official' WikiTree API returns.
 This layer is almost entirely implemented by {@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession}.</li>
 <li>The upper layer which provides the same functionality as the lower layer but returns results in the form of
 (hopefully) easier to use Java objects representing the various kinds of things that the 'official' WikiTree API returns.
 This layer involves a rather larger collection of classes although the key class is almost certainly
 {@link com.matilda.wikitree.api.wrappers.WikiTreeApiWrappersSession}</li>
 </ul>

 <h3>Using the upper layer API</h3>

 <h3>Using the bottom layer API</h3>
 The bottom layer API provides a JSON-based session-oriented interface to the 'official' WikiTree API.
 While you are free to use this layer, you will almost certainly find the upper layer to be easier to use.
 <p/>
 A session is represented by a {@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession} instance. The default constructor
 is almost always the correct way to create a {@code }WikiTreeApiJsonSession} instance:
 <blockquote>
 <pre>WikiTreeApiClient session = new WikiTreeApiJsonSession();</pre>
 </blockquote>
 Once the session instance exists, it is usually necessary to authenticate the session using
 {@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#login(String,String)}.
 This method returns {@code true} if the login worked and {@code false} if the login failed due to an authentication-related issue (for example, incorrect password):
 <blockquote>
 <pre>
 if ( session.login( "fred@example.com", "fred's secret password" ) ) {

     // the real action happens here.

 } else {

     // Oops!

 }
 </pre>
 </blockquote>
 It is possible to proceed without authenticating the session although the result is generally that each API call provides considerably less information
 than the user of the API desires.
 <blockquote>
 <h4>Sidebar:</h4>
 Like many of this API's methods, the {@code WikiTreeApiJsonSession.login(String,String)} method throws exceptions when something bad happens
 (in the case of this API, "something bad" is almost always a synonym for "a network communication error").
 In the interests of cleanliness over rigorous completeness, these examples are going to pretend that exceptions are not declared to be thrown by the various API methods.
 It should be noted that many of this API's methods throw unchecked {@code IllegalArgumentException} and {@link com.matilda.wikitree.api.exceptions.ReallyBadNewsError} exceptions.
 An {@code IllegalArgumentException} is usually thrown when the caller provided bogus information or failing to provide required information.
 In contrast, a {@code ReallyBadNewsError} is usually thrown when the API software discovers that it is 'suddenly' unable to continue (this is almost always a
 bug in the API implementation; if you get one, email danny@matilda.com with an explanation of what you were trying to do and the {@code ReallyBadNewsError}'s
 stack traceback).
 </blockquote>
 <p/>Once the session has been authenticated (or not as appropriate), the rest of this API comes into play as the user issues calls to the following methods
 (each of which corresponds to a request supported by the 'official' WikiTree API (see above link for more information)):
 <ul>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getPerson(String)}</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getPerson(String,String)} (less useful variant)</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getProfile(String)}</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getBio(String)}</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getWatchlist(Boolean,Boolean,Boolean,Boolean,String,Integer,Integer,String)}</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getAncestors(String,Integer)}</li>
 <li>{@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession#getRelatives(String,boolean,boolean,boolean,boolean)}</li>
 </ul>
 Successful calls to each of the above methods return a JSON object containing the results.
 Unsuccessful calls either return {@code null} or a JSON object which describes why the request failed.
 <p/>Determining if a call that returned a JSON object failed can be a bit tricky.
 This is an area where this API is likely to improve over the next while although most of the improvement is likely to be in the upper layer of this API.
 <p/>
 It should be noted that<ul>
 <li>the cost of creating a {@link com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession} session instance is essentially zero because it does not involve any network traffic</li>
 <li>the cost of authenticating a session instance or requesting information from the WikiTree API server is not zero as doing so involves a round-trip to said server</li>
 <li>This API does absolutely no caching of results obtained from the WikiTree API server (this ensures that the requested data is always 'fresh')</li>
 <li>Most applications which use this API will only need one {@code WikiTreeApiJsonSession} session instance (don't use more than one session instance
 unless you are <u>certain</u> that you need more than one; also, try to avoid having multiple threads making simultaneous requests to the WikiTree API server
 as that server is most definitely a 'limited resource')</li>
 </ul>

 */

package com.matilda.wikitree.api;
