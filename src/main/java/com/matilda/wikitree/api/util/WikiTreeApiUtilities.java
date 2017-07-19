/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.util;

import com.matilda.wikitree.api.WikiTreeApiClient;
import com.matilda.wikitree.api.exceptions.ReallyBadNewsError;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 Useful utility methods.
 */

@SuppressWarnings("WeakerAccess")
public class WikiTreeApiUtilities {

    /**
     A sorted set of the fields returned by the WikiTree API's {@code getPerson} request according to
     <a href="https://www.wikitree.com/wiki/Help:API_Documentation#getPerson">https://www.wikitree.com/wiki/Help:API_Documentation#getPerson</a> on 2017/06/14.
     */

    public static final SortedSet<String> S_ALL_GET_PERSON_FIELDS_SET;

    static {

        SortedSet<String> tmpSet = new TreeSet<>();
        Collections.addAll(
                tmpSet,
                "Id",
                "Name",
                "FirstName",
                "MiddleName",
                "LastNameAtBirth",
                "LastNameCurrent",
                "Nicknames",
                "LastNameOther",
                "RealName",
                "Prefix",
                "Suffix",
                "Gender",
                "BirthDate",
                "DeathDate",
                "BirthLocation",
                "DeathLocation",
                "BirthDateDecade",
                "DeathDateDecade",
                "Photo",
                "IsLiving",
                "Privacy",
                "Mother",
                "Father",
                "Parents",
                "Children",
                "Siblings",
                "Spouses",
                "Derived.ShortName",
                "Derived.BirthNamePrivate",
                "Derived.LongNamePrivate",
                "Manager"
        );

        S_ALL_GET_PERSON_FIELDS_SET = Collections.unmodifiableSortedSet( tmpSet );

    }

    private static final SimpleDateFormat STANDARD_MS = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    public static final String INDENT_STRING = "    ";

    public static final String JAVA_NEWLINE = String.format( "%n" );

    @Nullable
    public static Object readResponse( HttpURLConnection connection, @SuppressWarnings("SameParameterValue") boolean expectSingleResult )
            throws IOException, ParseException {

        StringBuilder sb = new StringBuilder();
        int httpResponseCode = connection.getResponseCode();
        if (
                httpResponseCode / 100 == 2
                ||
                httpResponseCode / 100 == 3

                ) {

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader( connection.getInputStream(), "utf-8" )
            );

            readFromConnection( false, sb, reader );

            String responseString = sb.toString();
//	    System.out.println( "got response:  " + sb );

            if ( responseString.isEmpty() ) {

                return null;

            } else if ( responseString.startsWith( "[" ) ) {

                JSONArray arrayResponse = parseJsonArray( responseString );

                if ( expectSingleResult ) {

                    if ( arrayResponse.size() == 1 ) {

//			System.out.println( "one entity in response" );
                        Object singleResult = arrayResponse.get( 0 );
                        if ( singleResult == null ) {

                            return null;

                        } else if ( singleResult instanceof JSONObject ) {

                            return singleResult;

                        } else {

                            throw new ReallyBadNewsError( "caller expected a single JSONObject result; got a single " +
                                                          singleResult.getClass().getCanonicalName() +
                                                          " instead" );

                        }

                    } else {

                        System.err.println( "caller expected a single JSONObject result; got " +
                                            arrayResponse.size() +
                                            " things instead; here they are . . ." );
                        int ix = 0;
                        for ( Object obj : arrayResponse ) {

                            System.out.println( "result[" + ix + "] = " + obj );
                            ix += 1;

                        }

                        throw new ReallyBadNewsError( "caller expected a single JSONObject result; got " + arrayResponse.size() + " things instead" );

                    }

                } else {

                    return arrayResponse;

                }

            } else if ( responseString.startsWith( "{" ) ) {

                @SuppressWarnings("UnnecessaryLocalVariable")
                JSONObject objectResponse = parseJsonObject( responseString );

                return objectResponse;

            } else {

                return responseString;

            }

        } else {

            System.err.println( "request failed:  " + httpResponseCode );

            return httpResponseCode;

        }
    }

    public static String cleanupStringDate( Object stringDateObj ) {

        if ( stringDateObj instanceof String ) {

            return cleanupStringDate( (String)stringDateObj );

        } else if ( stringDateObj == null ) {

            return null;

        } else {

            throw new IllegalArgumentException( "string date is not a string (it is a " + stringDateObj.getClass().getCanonicalName() + ")" );

        }

    }

    public static String cleanupStringDate( String stringDate ) {

        if ( stringDate == null ) {

            return "<<unknown>>";

        } else {

            String rval = stringDate;
            while ( rval.endsWith( "-00" ) ) {

                rval = rval.substring( 0, rval.length() - 3 );

            }

            return rval;

        }

    }

    public static SortedSet<String> constructExcludedGetPersonFieldsSet( String... excludedFields ) {

        SortedSet<String> tmpFields = new TreeSet<>();
        Collections.addAll( tmpFields, excludedFields );

        return constructExcludedGetPersonFieldsSet( tmpFields );

    }

    public static SortedSet<String> constructExcludedGetPersonFieldsSet( @NotNull SortedSet<String> excludedFields ) {

        SortedSet<String> includedFields = new TreeSet<>( S_ALL_GET_PERSON_FIELDS_SET );
        includedFields.removeAll( excludedFields );

        return includedFields;

    }

    public static String constructGetPersonFieldsString( @NotNull SortedSet<String> includedFields ) {

        return constructGetPersonFieldsString( includedFields.toArray( new String[includedFields.size()] ) );

    }

    public static String constructGetPersonFieldsString( String... includedFields ) {

        StringBuilder sb = new StringBuilder();
        String comma = "";
        for ( String includedField : includedFields ) {

            sb.append( comma ).append( includedField );
            comma = ",";

        }

        return sb.toString();

    }

    /**
     Manage pretty printing with a particular emphasis on making it easy to emit commas in all the right places.
     */

    public static class PrettyLineManager {

        private final Writer _ps;

        private StringBuilder _lastOutputLine;
        private StringBuilder _currentOutputLine;

        /**
         Create a pretty line manager instance.

         @param ps where the pretty-fied output should go.
         */

        public PrettyLineManager( @NotNull Writer ps ) {

            super();

            _ps = ps;

        }

        /**
         Actually write an output line.

         @param sb the line to be written.
         @throws IOException if something goes wrong when writing the line.
         */

        private void println( StringBuilder sb )
                throws IOException {

            _ps.write( sb.toString() );
            _ps.write( JAVA_NEWLINE );

        }

        /**
         Append some text to the current output line.

         @param value what is to be appended.
         What actually happens is that a current output line is created if it doesn't already exist and then
         the output of {@code String.valueOf( value )} is appended to the current output line.
         This is conceptually equivalent to {@code value.toString()} except that it yields {@code "null"} if {@code value} happens to be {@code null}'.
         @return this instance (allows chained calls to methods in this class).
         */

        public PrettyLineManager append( @NotNull Object value ) {

            if ( _currentOutputLine == null ) {

                _currentOutputLine = new StringBuilder();

            }

            _currentOutputLine.append( String.valueOf( value ) );

            return this;

        }

        /**
         Rotate the output lines.
         <p/>If there is a last output line then it is printed. The current output line then becomes the last line and the current output line becomes null.

         @return this instance (allows chained calls to methods in this class).
         @throws IOException if something goes wrong writing the last line.
         */

        @SuppressWarnings("UnusedReturnValue")
        public PrettyLineManager rotate()
                throws IOException {

            if ( _lastOutputLine != null ) {

                println( _lastOutputLine );

            }

            _lastOutputLine = _currentOutputLine;
            _currentOutputLine = null;

            return this;

        }

        /**
         Ensure that the last output line, if it exists, and then the current output line, if it exists, are written.
         When this method is done, there will be no last output line or current output line.

         @throws IOException if something goes wrong writing either line.
         */

        public void flush()
                throws IOException {

            if ( _lastOutputLine != null ) {

                println( _lastOutputLine );

            }

            if ( _currentOutputLine != null ) {

                println( _lastOutputLine );

            }

        }

        /**
         Append a comma to the last output line.

         @throws IllegalArgumentException if there is no last output line.
         */

        public void doComma() {

            if ( _lastOutputLine == null ) {

                throw new IllegalArgumentException( "PLM:  cannot do a comma until after first rotate call" );

            } else {

                _lastOutputLine.append( ',' );

            }

        }

    }

    /**
     Mark this as a utilities class.
     */

    private WikiTreeApiUtilities() {

        super();
    }

    /**
     Format a date string in a 'standard' format which includes milliseconds.
     <p/>The 'standard' format is
     <blockquote><tt>yyyy-MM-dd'T'HH:mm:ss.SSSZ</tt></blockquote>
     or
     <blockquote><tt>2001-07-04T12:08:56.235-0700</tt></blockquote>
     */

    public static String formatStandardMs( Date dateTime ) {

        synchronized ( WikiTreeApiUtilities.STANDARD_MS ) {

            WikiTreeApiUtilities.STANDARD_MS.setTimeZone( TimeZone.getDefault() );
            @SuppressWarnings("UnnecessaryLocalVariable")
            String rval = WikiTreeApiUtilities.STANDARD_MS.format( dateTime );
            return rval;

        }

    }

    /**
     Format a {@link JSONObject} describing a request into the form of a set of URL query parameters.

     @param who              who is making the request (used for tracing and throwing exceptions).
     @param parametersObject the request as a {@link JSONObject}.
     @param requestSb        a {@link StringBuffer} to append the resulting URL query parameters into.
     This buffer is not changed if there happen to be no parameters.
     @throws UnsupportedEncodingException if one of the parameter values cannot be encoded by {@link URLEncoder#encode(String)}.
     */

    public static void formatRequestAsUrlQueryParameters( String who, JSONObject parametersObject, StringBuffer requestSb )
            throws UnsupportedEncodingException {

        boolean first = true;
        for ( Object paramName : parametersObject.keySet() ) {

            if ( paramName == null ) {

                throw new IllegalArgumentException( who + "we found a null parameter name in " + parametersObject );

            } else if ( paramName instanceof String ) {

                Object paramValue = parametersObject.get( paramName );
                if ( paramValue == null ) {

                    System.out.println( who + ":  parameter \"" + paramName + "\" has no value - ignored" );

                } else if ( paramValue instanceof String ) {

                    requestSb.
                                     append( first ? "?" : "&" ).
                                     append( paramName ).
                                     append( '=' ).
                                     append( URLEncoder.encode( (String)paramValue, "UTF-8" ) );

                    first = false;

                } else if ( paramValue instanceof Number ) {

                    requestSb.
                                     append( first ? "?" : "&" ).
                                     append( paramName ).
                                     append( '=' ).
                                     append( paramValue );

                    first = false;

                } else if ( paramValue instanceof Boolean ) {

                    requestSb.
                                     append( first ? "?" : "&" ).
                                     append( paramName ).
                                     append( '=' ).
                                     append( paramValue );

                    first = false;

                } else {

                    throw new IllegalArgumentException(
                            who + ":  unexpected parameter value type - \"" + paramName + "\" is a " +
                            paramValue.getClass().getCanonicalName()
                    );

                }

            } else {

                throw new IllegalArgumentException(
                        who + ":  unexpected parameter name type - \"" + paramName + "\" is a " +
                        paramName.getClass().getCanonicalName()
                );

            }

        }

    }

    /**
     Generate a Java-style representation of a string.
     <p/>This method wraps the string in double quotes and replaces backspace, newline, carriage return, tab, backslash and double quote characters
     with their backslash equivalents (\b, \n, \r, \t, \\, and \").

     @param string the string to be enquoted.
     @return the enquoted string or the four character string {@code "null"} if the supplied parameter is null.
     */

    public static String enquoteForJavaString( String string ) {

        if ( string == null ) {

            return "null";

        }

        StringBuilder rval = new StringBuilder( "\"" );
        for ( char c : string.toCharArray() ) {

            switch ( c ) {

                case '\b':
                    rval.append( "\\b" );
                    break;

                case '\n':
                    rval.append( "\\n" );
                    break;

                case '\r':
                    rval.append( "\\r" );
                    break;

                case '\t':
                    rval.append( "\\t" );
                    break;

                case '\\':
                    rval.append( "\\\\" );
                    break;

                case '"':
                    rval.append( "\\\"" );
                    break;

                default:
                    rval.append( c );

            }

        }

        rval.append( '"' );
        return rval.toString();

    }

    /**
     Create a string which contains the specified number of copies of a specifed string.
     <p/>For example, {@code repl( "hello", 3 )} would yield {@code "hellohellohello"}.

     @param s      the string to be replicated.
     @param copies how many copies of the string should appear in the result.
     @return a string consisting of the specified number of copies of the specified string (an empty string if {@code copies} is {@code 0}).
     @throws IllegalArgumentException if {@code copies} is negative.
     */

    public static String repl( String s, int copies ) {

        if ( copies < 0 ) {

            throw new IllegalArgumentException( "WikiTreeApiUtilities.repl:  invalid copies value (" + copies + ")" );

        }

        StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < copies; i += 1 ) {

            sb.append( s );

        }

        return sb.toString();

    }

    /**
     Pretty-print onto {@link System#out} anything which might appear in a {@link JSONArray} or {@link JSONObject}.
     <p/>{@link JSONArray} and {@link JSONObject} instances get the full treatment (indented lines, curly or square brackets, etc).
     {@link String} instances get encoded using {@link #enquoteForJavaString(String)}.
     Everything else gets the {@code String.valueOf( thing )} treatment.
     <p/>This is intended to be an easy-to-use pretty printer. See {@link #prettyFormatJsonThing(int, String, Object, PrettyLineManager)}
     for the more flexible and elaborate version (which is what does the actually pretty printing that this method is claiming credit for).

     @param name  the optional name of the thing.
     @param thing the thing being pretty-printed.
     <p/>If the provided Json thing is valid then the contents of the returned {@link StringBuffer} are intended
     to be parseable by {@link #parseJsonArray(String)} or {@link #parseJsonObject(String)}.
     Roughly speaking, the Json thing is valid if:
     <ul>
     <li>it is a {@link JSONArray} or a {@link JSONObject} which only contains valid things.</li>
     <li>it is a something (like a {@link JSONArray}, a {@link JSONObject}, a string or a number)
     which can legitimately appear within a {@link JSONArray} or a {@link JSONObject}.</li>
     <li>if it has a name then the name is a {@link String} instance.</li>
     </ul>
     @throws ReallyBadNewsError if an IOException gets thrown while generating the pretty-printed lines
     (this strikes me as impossible which is why this method doesn't throw the IOException).
     */

    public static void prettyPrintJsonThing( String name, @Nullable Object thing )
        /*throws IOException*/ {

        StringWriter sw = prettyFormatJsonThing( name, thing );

        System.out.print( sw.getBuffer() );
        System.out.flush();

    }

    /**
     Pretty-format into a {@link StringWriter} anything which might appear in a {@link JSONArray} or {@link JSONObject}.
     <p/>{@link JSONArray} and {@link JSONObject} instances get the full treatment (indented lines, curly or square brackets, etc).
     {@link String} instances get encoded using {@link #enquoteForJavaString(String)}.
     Everything else gets the {@code String.valueOf( thing )} treatment.
     <p/>This is intended to be an easy-to-use pretty printer. See {@link #prettyFormatJsonThing(int, String, Object, PrettyLineManager)}
     for the more flexible and elaborate version (which is what does the actually pretty printing that this method is claiming credit for).

     @param name  the optional name of the thing.
     @param thing the thing being pretty-printed.
     @return a {@link StringWriter} containing the entire pretty-printed result.
     <p/>If the provided Json thing is valid then the contents of the returned {@link StringBuffer} are intended
     to be parseable by {@link #parseJsonArray(String)} or {@link #parseJsonObject(String)}.
     Roughly speaking, the Json thing is valid if:
     <ul>
     <li>it is a {@link JSONArray} or a {@link JSONObject} which only contains valid things.</li>
     <li>it is a something (like a {@link JSONArray}, a {@link JSONObject}, a string or a number)
     which can legitimately appear within a {@link JSONArray} or a {@link JSONObject}.</li>
     <li>if it has a name then the name is a {@link String} instance.</li>
     </ul>
     @throws ReallyBadNewsError if an IOException gets thrown while generating the pretty-printed lines
     (this strikes me as impossible which is why this method doesn't throw the IOException).
     */

    public static StringWriter prettyFormatJsonThing( String name, @Nullable Object thing ) {

        StringWriter sw = new StringWriter();
        try {

            PrettyLineManager plm = new PrettyLineManager( sw );

            try {

                prettyFormatJsonThing( 0, name, thing, plm );

            } finally {

                plm.flush();

            }

            return sw;

        } catch ( IOException e ) {

            throw new ReallyBadNewsError( "WikiTreeApiUtilities.prettyPrintJsonThing:  caught an IOException writing to a StringWriter(!)", e );

        }

    }

    /**
     Pretty-format onto a {@link PrettyLineManager} anything which might appear in a {@link JSONArray} or {@link JSONObject}.
     <p/>{@link JSONArray} and {@link JSONObject} instances get the full treatment (indented lines, curly or square brackets, etc).
     {@link String} instances get encoded using {@link #enquoteForJavaString(String)}.
     Everything else gets the {@code String.valueOf( thing )} treatment.
     <p/>See {@link #prettyPrintJsonThing(String, Object)} or {@link #prettyFormatJsonThing(String, Object)} for an easier to use but somewhat less flexible pretty-printer or pretty-formatter.

     @param indent how deeply to indent the current thing. This is used when this method calls itself recursively to print the contents
     of {@link JSONArray} and {@link JSONObject} instances.
     Just passing {@code 0} for this parameter is almost always the right thing to do when calling it from other places.
     @param name   the optional name of the thing. If {@code thing} is an {@link Optional} instance then it and any {@code Optional} instances
     which it wraps are peeled off before anything gets printed. This may seem like a strange service to offer but it does seem to simplify calling
     this set of printing and formatting methods and we don't really provide any sensible printed representation of an {@code Optional} instance.
     @param thing  the thing being pretty-printed.
     @param plm    the {@link PrettyLineManager} that is to do the actual printing.
     @throws IOException if something goes wrong writing the pretty-printed lines.
     */

    public static void prettyFormatJsonThing( int indent, String name, @Nullable Object thing, PrettyLineManager plm )
            throws IOException {

        // Unwrap any {@link Optional} instances which happen to wrap what we're supposed to be formatting.
        Object iThing = thing;
        while ( iThing instanceof Optional ) {

            Optional optThing = (Optional)iThing;
            iThing = optThing.isPresent() ? optThing.get() : null;

        }

        plm.append( repl( INDENT_STRING, indent ) );
        if ( name != null ) {

            plm.append( enquoteForJavaString( name ) ).append( " : " );

        }

//        Object iThing;
//        if ( thing instanceof Optional ) {
//
//            Optional optThing = (Optional)thing;
//            iThing = optThing.isPresent() ? optThing.get() : null;
//
//        } else {
//
//            iThing = thing;
//
//        }

        if ( iThing == null ) {

            plm.append( "null" ).rotate();

        } else if ( iThing instanceof Map ) {

            // Covers JSONObject instances and other kinds of Java Maps.
            // This makes it possible to use the pretty printer to print out collections of things.
            // The map's keys are assumed to be something that {@link String.valueOf(Object)} is able to provide a reasonable result for.

            Map map = (Map)iThing;

            plm.append( '{' ).rotate();

            boolean doComma = false;
            for ( Object paramName : map.keySet() ) {

                if ( doComma ) {

                    plm.doComma();

                }
                doComma = true;

                if ( paramName instanceof String ) {

                    prettyFormatJsonThing( indent + 1, String.valueOf( paramName ), map.get( paramName ), plm );

//		} else {
//
//		    plm.
//			    append( repl( INDENT_STRING, indent + 1 ) ).
//			    append( "*** parameter name is not a string:  " ).
//			    append( paramName );

                }

            }

            plm.append( repl( INDENT_STRING, indent ) ).append( '}' ).rotate();

        } else if ( iThing instanceof Collection ) {

            // Covers JSONArray instances and other kinds of Java Collections.
            // This makes it possible to use the pretty printer to print out collections of things.

            Collection collection = (Collection)iThing;
            plm.append( '[' ).rotate();

            boolean doComma = false;
            for ( Object value : collection ) {

                if ( doComma ) {

                    plm.doComma();

                }
                doComma = true;

                prettyFormatJsonThing( indent + 1, null, value, plm );

            }

            plm.append( repl( INDENT_STRING, indent ) ).append( ']' ).rotate();

        } else if ( iThing instanceof String ) {

            plm.append( enquoteForJavaString( (String)iThing ) ).rotate();

        } else {

            plm.append( iThing ).rotate();

        }

    }

    /**
     Read the content returned via a {@link java.net.URLConnection}'s connection.

     @param server are we acting as a server (used for some debugging; please set to {@link false}).
     @param sb     the {@link StringBuilder} to send the content to.
     @param reader the {@link Reader} to get the content from.
     @throws IOException if something goes wrong while reading the content from the {@link Reader}.
     */

    public static void readFromConnection( @SuppressWarnings("SameParameterValue") boolean server, StringBuilder sb, Reader reader )
            throws IOException {

        try {

            while ( true ) {

                int ch = reader.read();
                if ( ch == -1 ) {

                    break;

                }

                sb.append( (char)ch );
                if ( server ) {

                    System.out.println( "server so far:  " + sb );

                }

            }

        } finally {

            reader.close();

        }

    }

    /**
     Parse a string representing a Json array.
     <p/>The string <b><u>must</u></b> start with an opening square bracket ('['). No leading white space is allowed.

     @param jsonArrayString the string representing the Json array.
     @return the resulting {@link JSONArray} instance.
     @throws ParseException if something goes wrong parsing the string.
     */

    public static JSONArray parseJsonArray( String jsonArrayString )
            throws ParseException {

        JSONParser jp = new JSONParser();

        Object parsedObject = jp.parse( jsonArrayString.trim() );
        final JSONArray parsedArray = (JSONArray)parsedObject;

//	System.out.println( "parse of array worked:  " + parsedArray );

        return parsedArray;

    }

    /**
     Parse a string representing a Json object.
     <p/>The string <b><u>must</u></b> start with an opening curly brace ('{'). No leading white space is allowed.

     @param jsonObjectString the string representing the Json object.
     @return the resulting {@link JSONObject} instance.
     @throws ParseException if something goes wrong parsing the string.
     */

    public static JSONObject parseJsonObject( String jsonObjectString )
            throws ParseException {

        JSONParser jp = new JSONParser();

        Object parsedObject = jp.parse( jsonObjectString );
        final JSONObject jsonObject = (JSONObject)parsedObject;

//	System.out.println( "parse of object worked:  " + jsonObject );

        return jsonObject;

    }

    /**
     Try to turn this into an authenticated client instance if the name of a WikiTree user info file was provided to us.
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
     Put another way, a {@code String} array with one element containing the name of the {@code .wtu} file
     that you'd like to use to login to the API. The specified name of the {@code .wtu} will be interpreted relative to your
     home directory. In other words, {@code .myWikiTreeAPIInfo.wtu} would be interpreted as {@code ~/.myWikiTreeAPIInfo.wtu}
     if you're on a Unix or Mac OS X system and as {@code C:\Users\YourWindowsName} if you're on a Windows 10 system.
     */

    public static void maybeLoginToWikiTree( WikiTreeApiClient apiClient, String[] args ) {

        if ( args.length == 0 ) {

            System.out.println( "no user info file specified on command line, proceeding as an anonymous user" );

        } else if ( args.length == 1 ) {

            String userHome = System.getProperty( "user.home" );
            String userInfoFileName;
            if ( userHome == null ) {

                userInfoFileName = args[0];

            } else {

                userInfoFileName = userHome + File.separator + args[0];

            }

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
                    System.err.println( "user info file \"" +
                                        userInfoFileName +
                                        "\" only has one line (first line must be an email address; second line must be WikiTree password for that email address)" );
                    System.exit( 1 );

                }

                boolean loginResponse = apiClient.login( userName, password );
                if ( !loginResponse ) {

                    System.out.flush();
                    System.err.println( "unable to create authenticated session for \"" +
                                        userName +
                                        "\" (probably incorrect user name or incorrect password; could be network problems or maybe even an invasion of space aliens)" );
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

    public static void printAuthenticatedSessionUserInfo( WikiTreeApiClient apiClient ) {

        if ( apiClient.isAuthenticated() ) {

            System.out.println( "authenticated WikiTree API session for " + apiClient.getAuthenticatedUserEmailAddress() + " (" +
                                apiClient.getAuthenticatedWikiTreeId() + ")" );

        } else {

            System.out.println( "WikiTree API session is not authenticated" );

        }

    }

    @Nullable
    public static String getOptionalJsonString( JSONObject jsonObject, String... keys ) {

        return (String)getOptionalJsonValue( String.class, jsonObject, keys );

    }

    @NotNull
    public static String getMandatoryJsonString( JSONObject jsonObject, String... keys ) {

        return (String)getMandatoryJsonValue( String.class, jsonObject, keys );

    }

    @Nullable
    public static Object getOptionalJsonValue( @Nullable Class requiredClass, JSONObject jsonObject, String... keys ) {

        Object rval = getJsonValue( false, jsonObject, keys );

        if ( rval == null ) {

            return null;

        }

        if ( requiredClass != null ) {

            //noinspection unchecked
            if ( requiredClass.isAssignableFrom( rval.getClass() ) ) {

                return rval;

            } else {

                throw new IllegalArgumentException(
                        "WikiTreeApiUtilities.getOptionalJsonValue:  value at " + formatPath( keys ) +
                        " should be a " + requiredClass.getCanonicalName() + " but it is a " + rval.getClass().getCanonicalName()
                );

            }

        }

        return rval;

    }

    /**
     Get a value which must exist.

     @param requiredClass if specified, the class that the requested value must be assignable to; otherwise, the value may be of any class.
     @param jsonObject    where to look for the value.
     @param keys          the path to the value.
     @return the value.
     @throws IllegalArgumentException if the value does not exist or if a required class was specified and the value is not assignable to.
     */

    @NotNull
    public static Object getMandatoryJsonValue( @Nullable Class requiredClass, @NotNull JSONObject jsonObject, String... keys ) {

        Object rval = getJsonValue( true, jsonObject, keys );
        if ( rval == null ) {

            throw new IllegalArgumentException(
                    "WikiTreeApiUtilities.getMandatoryJsonValue:  required value at " + formatPath( keys ) + " is null"
            );

        }

        if ( requiredClass != null ) {

            //noinspection unchecked
            if ( requiredClass.isAssignableFrom( rval.getClass() ) ) {

                return rval;

            } else {

                throw new IllegalArgumentException(
                        "WikiTreeApiUtilities.getMandatoryJsonValue:  value at " + formatPath( keys ) +
                        " should be a " + requiredClass.getCanonicalName() + " but it is a " + rval.getClass().getCanonicalName()
                );

            }

        }

        return rval;

    }

    public static Object getMandatoryJsonValue( JSONObject jsonObject, String... keys ) {

        Object rval = getJsonValue( true, jsonObject, keys );

        return rval;

    }

    public static String formatPath( String[] keys ) {

        StringBuilder sb = new StringBuilder();
        String pointer = "";
        for ( String key : keys ) {

            sb.append( pointer ).append( '"' ).append( key ).append( '"' );
            pointer = " -> ";

        }

        return sb.toString();

    }

    public static String formatResultObject( JSONObject resultObject ) {

        StringBuilder sb = new StringBuilder();

        String comma = "";
        int consumedFieldCount = 0;
        if ( resultObject.containsKey( "status" ) ) {

            sb.append( "status=\"" ).append( resultObject.get( "status" ) ).append( '"' );
            comma = ", ";
            consumedFieldCount += 1;

        }

        for ( Object keyObject : resultObject.keySet() ) {

            String key = (String)keyObject;
            if ( !"status".equals( key ) ) {

                sb.append( comma ).append( key ).append( '=' ).append( resultObject.get( key ) );
                comma = ", ";

            }

        }

        return sb.toString();

    }

    public static Object getJsonValue( boolean verifyStructure, JSONObject xJsonObject, String... keys ) {

        int depth = 1;
        JSONObject jsonObject = xJsonObject;
        Object value = null;
        StringBuffer sb = new StringBuffer();
        String pointer = "";
        for ( String key : keys ) {

            sb.append( pointer ).append( '"' ).append( key ).append( '"' );
            pointer = " -> ";

            value = jsonObject.get( key );
            if ( depth == keys.length ) {

                break;

            }

            if ( value instanceof JSONObject ) {

                jsonObject = (JSONObject)value;

            } else if ( value == null ) {

                if ( verifyStructure ) {

                    throw new IllegalArgumentException( "WikiTreeApiUtilities.getOptionalJsonValue:  found null at " + sb );

                } else {

                    break;

                }

            } else {

                if ( verifyStructure ) {

                    throw new IllegalArgumentException(
                            "WikiTreeApiUtilities.getJsonValue:  expected JSONObject but found something else at " + sb +
                            ":  (" + value.getClass().getCanonicalName() + ") " + value
                    );

                } else {

                    break;

                }

            }

            depth += 1;

        }

        return value;

    }

    /**
     Intended to be used to provide a place to put a breakpoint.
     */

    public static void doNothing() {

    }

}
