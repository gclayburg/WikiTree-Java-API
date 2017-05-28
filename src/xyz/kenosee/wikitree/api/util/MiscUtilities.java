package xyz.kenosee.wikitree.api.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import xyz.kenosee.wikitree.api.exceptions.ReallyBadNewsError;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 Useful utility methods.
 */

@SuppressWarnings("WeakerAccess")
public class MiscUtilities {

    private static final SimpleDateFormat STANDARD_MS = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );

    public static final String INDENT_STRING = "    ";

    public static final String JAVA_NEWLINE = String.format( "%n" );

    @Nullable
    public static Object readResponse( HttpURLConnection connection, boolean expectSingleResult )
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
	    System.out.println( "got response:  " + sb );

	    if ( responseString.isEmpty() ) {

		return null;

	    } else if ( responseString.startsWith( "[" ) ) {

		JSONArray arrayResponse = parseJsonArray( responseString );

		if ( expectSingleResult ) {

		    if ( arrayResponse.size() == 1 ) {

			System.out.println( "one entity in response" );
			Object singleResult = arrayResponse.get( 0 );
			if ( singleResult == null ) {

			    return null;

			} else if ( singleResult instanceof JSONObject ) {

			    return singleResult;

			} else {

			    throw new ReallyBadNewsError( "caller expected a single JSONObject result; got a single " + singleResult.getClass().getCanonicalName() + " instead" );

			}

		    } else {

			System.err.println( "caller expected a single JSONObject result; got " + arrayResponse.size() + " things instead; here they are . . ." );
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

    /**
     Manage pretty printing with a particular emphasis on making it easy to emit commas in all the right places.
     */

    public static class PrettyLineManager {

	private final Writer _ps;

	private StringBuilder _lastOutputLine;
	private StringBuilder _currentOutputLine;

	private static String s_newline = String.format( "%n" );

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

		_lastOutputLine.append( "," );

	    }

	}

    }

    /**
     Mark this as a utilities class.
     */

    private MiscUtilities() {
        super();
    }

    /**
     * Format a date string in a 'standard' format which includes milliseconds.
     * <p/>The 'standard' format is
     * <blockquote><tt>yyyy-MM-dd'T'HH:mm:ss.SSSZ</tt></blockquote>
     * or
     * <blockquote><tt>2001-07-04T12:08:56.235-0700</tt></blockquote>
     */

    public static String formatStandardMs( Date dateTime ) {

	synchronized ( MiscUtilities.STANDARD_MS ) {

	    MiscUtilities.STANDARD_MS.setTimeZone( TimeZone.getDefault() );
	    @SuppressWarnings("UnnecessaryLocalVariable")
	    String rval = MiscUtilities.STANDARD_MS.format( dateTime );
	    return rval;

	}

    }

    /**
     Format a {@link JSONObject} describing a request into the form of a set of URL query parameters.
     @param who who is making the request (used for tracing and throwing exceptions).
     @param parametersObject the request as a {@link JSONObject}.
     @param requestSb a {@link StringBuffer} to append the resulting URL query parameters into.
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
			    append( URLEncoder.encode( (String) paramValue, "UTF-8" ) );

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
     @param s the string to be replicated.
     @param copies how many copies of the string should appear in the result.
     @return a string consisting of the specified number of copies of the specified string (an empty string if {@code copies} is {@code 0}).
     @throws IllegalArgumentException if {@code copies} is negative.
     */

    public static String repl( String s, int copies ) {

        if ( copies < 0 ) {

            throw new IllegalArgumentException( "MiscUtilities.repl:  invalid copies value (" + copies + ")" );

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
     <p/>This is intended to be an easy-to-use pretty printer. See {@link #prettyPrintJsonThing(int, String, Object, PrettyLineManager)}
     for the more flexible and elaborate version (which is what does the actually pretty printing that this method is claiming credit for).
     @param name the optional name of the thing.
     @param thing the thing being pretty-printed.
     @return a {@link StringBuffer} containing the entire pretty-printed result (we return a {@link StringBuffer} instead of something
     more modern because that's what we can get out of the {@link StringWriter} that we buffer the pretty-printing into.
     <p/>If the provided Json thing is valid then the contents of the returned {@link StringBuffer} are intended
     to be parseable by {@link #parseJsonArray(String)} or {@link #parseJsonObject(String)}.
     Roughly speaking, the Json thing is valid if:
     <ul>
     <li>it is a {@link JSONArray} or a {@link JSONObject} which only contains valid things.</li>
     <li>it is a something (like a {@link JSONArray}, a {@link JSONObject}, a string or a number)
     which can legitimately appear within a {@link JSONArray} or a {@link JSONObject}.</li>
     <li>if it has a name then the name is a {@link String} instance.</li>
     </ul>
     @throws IOException if something goes wrong writing the pretty-printed lines.
     */

    public static StringBuffer prettyPrintJsonThing( @SuppressWarnings("SameParameterValue") String name, Object thing )
	    throws IOException {

        StringWriter sw = new StringWriter();
	PrettyLineManager plm = new PrettyLineManager( sw );
	try {

	    prettyPrintJsonThing( 0, name, thing, plm );

	} finally {

	    plm.flush();

	}

	System.out.print( sw.getBuffer() );

	return sw.getBuffer();

    }

    /**
     Pretty-print onto a {@link PrettyLineManager} anything which might appear in a {@link JSONArray} or {@link JSONObject}.
     <p/>{@link JSONArray} and {@link JSONObject} instances get the full treatment (indented lines, curly or square brackets, etc).
     {@link String} instances get encoded using {@link #enquoteForJavaString(String)}.
     Everything else gets the {@code String.valueOf( thing )} treatment.
     <p/>See {@link #prettyPrintJsonThing(String, Object)} for an easier to use but less flexible pretty-printer.
     @param indent how deeply to indent the current thing. This is used when this method calls itself recursively to print the contents
     of {@link JSONArray} and {@link JSONObject} instances.
     Just passing {@code 0} for this parameter is almost always the right thing to do when calling it from other places.
     @param name the optional name of the thing.
     @param thing the thing being pretty-printed.
     @param plm the {@link PrettyLineManager} that is to do the actual printing.
     @throws IOException if something goes wrong writing the pretty-printed lines.
     */

    public static void prettyPrintJsonThing( int indent, String name, Object thing, PrettyLineManager plm )
	    throws IOException {

	plm.append( repl( INDENT_STRING, indent ) );
	if ( name != null ) {

	    plm.append( enquoteForJavaString( name ) ).append( " : " );

	}

	if ( thing == null ) {

	    plm.append( "null" ).rotate();

	} else if ( thing instanceof JSONObject ) {

	    JSONObject jObject = (JSONObject) thing;

	    plm.append( "{" ).rotate();

	    boolean doComma = false;
	    for ( Object paramName : jObject.keySet() ) {

	        if ( doComma ) {

	            plm.doComma();

		}
		doComma = true;

		if ( paramName instanceof String ) {

		    prettyPrintJsonThing( indent + 1, (String) paramName, jObject.get( paramName ), plm );

		} else {

		    plm.
			    append( repl( INDENT_STRING, indent + 1 ) ).
			    append( "*** parameter name is not a string:  " ).
			    append( paramName );

		}

	    }

	    plm.append( repl( INDENT_STRING, indent ) ).append( "}" ).rotate();

	} else if ( thing instanceof JSONArray ) {

	    JSONArray jArray = (JSONArray) thing;
	    plm.append( "[" ).rotate();

	    boolean doComma = false;
	    for ( Object value : jArray ) {

		if ( doComma ) {

		    plm.doComma();

		}
		doComma = true;

		prettyPrintJsonThing( indent + 1, null, value, plm );

	    }

	    plm.append( repl( INDENT_STRING, indent ) ).append( "]" ).rotate();

	} else if ( thing instanceof String ) {

	    plm.append( enquoteForJavaString( (String) thing ) ).rotate();

	} else {

	    plm.append( thing ).rotate();

	}

    }

    /**
     Read the content returned via a {@link java.net.URLConnection}'s connection.
     @param server are we acting as a server (used for some debugging; please set to {@link false}).
     @param sb the {@link StringBuilder} to send the content to.
     @param reader the {@link Reader} to get the content from.
     @throws IOException if something goes wrong while reading the content from the {@link Reader}.
     */

    public static void readFromConnection( boolean server, StringBuilder sb, Reader reader )
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
	final JSONArray parsedArray = (JSONArray) parsedObject;

	System.out.println( "parse of array worked:  " + parsedArray );

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
	final JSONObject jsonObject = (JSONObject) parsedObject;

	System.out.println( "parse of object worked:  " + jsonObject );

	return jsonObject;

    }

}
