/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.wrappers;

import com.matilda.wikitree.api.exceptions.ReallyBadNewsError;
import com.matilda.wikitree.api.util.WikiTreeApiUtilities;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Represent a WikiTree ID as a class rather than as a string to make it harder to pass junk as WikiTree ID parameters.
 <p/>A WikiTree ID is defined as either:
 <ul>
 <li>a WikiTree ID Name consisting of sequence of one or more characters followed by a minus sign followed by one or more digits (for example, {@code "Churchill-4"})</li>
 <li>a WikiTree Space Name which starts with {@code Space:} and has at least one character after the colon (for example, {@code "Space:Allied_POW_camps"}</li>
 </ul>
 For example, each of the following are, according to the above definition, valid WikiTree IDs:
 <ul>
 <li>{@code "Churchill-4"} (Sir Winston S. Churchill)</li>
 <li>{@code "Baden-Powell-19"} (Son of Lord Baden-Powell who founded the Scout Movement)</li>
 <li>{@code "宋-1"} ( 靄齡 (Ai-ling) "Eling" Soong Kung formerly 宋 aka Soong )</li>
 <li>{@code "Space:Allied_POW_camps"} (the name of a WikiTree Space)</li>
 <li>{@code "tre98743,n54jlsdgo7dsi23hljkfdioufsdew-123"} (mostly random junk that satisfies the definition - see below for more info)</li>
 </ul>
 Alternatively, none of the following are, according to the above definition, valid WikiTree IDs:
 <ul>
 <li>{@code "Churchill"} (no minus sign followed by one or more digits)</li>
 <li>{@code "Churchill-"} (no digits after the minus sign)</li>
 <li>{@code "5589"} (numeric {@code Person.Id}s or {@code Space.Id}s are not WikiTree IDs)</li>
 <li>{@code "Space:"} (nothing after the colon)</li>
 </ul>
 Note that the goal of this exercise is to eliminate total junk and to classify every possible string into exactly one of
 <ul>
 <li>a WikiTree ID Name</li>
 <li>a WikiTree Space Name</li>
 <li>none of the above</li>
 </ul>
 We err on the side of accepting an invalid value subject to the constraint
 that we don't accept a value that one of our sibling methods also accepts.
 This allows us to let the WikiTree API server make the final determination of validity.
 */

@SuppressWarnings("WeakerAccess")
public class WikiTreeId implements Comparable<WikiTreeId> {

//    public enum Kind {
//        PERSON_NAME,
//        NUMERIC,
//        SPACE_NAME
//    }

//    public static final Pattern WIKITREE_ID_NAME_PATTERN = Pattern.compile( "([wW][iI][dD]=)?(.*-\\d\\d*)" );
//    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile( "([pPsS][iI][dD]=)?(\\d\\d*)" );
//    private static final Pattern SPACE_NAME_PATTERN = Pattern.compile( "([sS][nN][aA][mM][eE]=)?(Space:..*)" );

    public static final Pattern WIKITREE_ID_NAME_PATTERN = Pattern.compile( ".*-\\d\\d*" );
//    public static final Pattern NUMERIC_ID_PATTERN = Pattern.compile( "\\d\\d*" );
    public static final Pattern SPACE_NAME_PATTERN = Pattern.compile( "Space:..*" );

    @NotNull
    private final String _wikiTreeIdString;
    private final boolean _isIdName;

//    private final Kind _kind;

    public WikiTreeId( @NotNull String wikiTreeIdString )
            throws IllegalArgumentException, ReallyBadNewsError {

        super();

        if ( wikiTreeIdString.isEmpty() ) {

            throw new IllegalArgumentException( "WikiTreeId:  invalid id \"" + wikiTreeIdString + "\" (must not be empty)" );

        }

        boolean isName = isValidWikiTreeIdPersonName( wikiTreeIdString );
//        boolean isNumericId = isValidNumericIdString( wikiTreeIdString );
        boolean isSpaceName = isValidWikiTreeSpaceName( wikiTreeIdString );

//        Matcher mNumber = NUMERIC_ID_PATTERN.matcher( wikiTreeIdString );
//        Matcher mWikiTreeName = WIKITREE_ID_NAME_PATTERN.matcher( wikiTreeIdString );
//        Matcher mSpaceName = SPACE_NAME_PATTERN.matcher( wikiTreeIdString );

        if ( isName && isSpaceName ) {

            throw new ReallyBadNewsError(
                    "WikiTreeId:  " + WikiTreeApiUtilities.enquoteForJavaString( wikiTreeIdString ) +
                    " looks like both a WikiTreeId Name and a Space name " +
                    "(this is a bug; please email this entire message to danny@matilda.com)"
            );

        }

        if ( !isName && !isSpaceName ) {

            throw new IllegalArgumentException(
                    "WikiTreeId:  " + WikiTreeApiUtilities.enquoteForJavaString( wikiTreeIdString ) +
                    " is neither a WikiTreeId Name or a WikiTree Space Name"
            );

        }

        _isIdName = isName;

//        _isSpaceName = wikiTreeIdString.startsWith( "Space:" );
//        if ( _isSpaceName ) {
//
//            _wikiTreeIdString = wikiTreeIdString;
//
//            _isWikiTreeStyleName = true;
//
//            return;
//
//        }return

        _wikiTreeIdString = wikiTreeIdString;

//        if (isName ) {
//
////            _wikiTreeIdString = mWikiTreeName.group( 2 );
//            _kind = Kind.PERSON_NAME;
//
//        } else if ( isNumericId ) {
//
////            _wikiTreeIdString = mNumber.group( 2 );
//            _kind = Kind.NUMERIC;
//
//        } else if ( isSpaceName ) {
//
////            _wikiTreeIdString = mSpaceName.group( 2 );
//            _kind = Kind.SPACE_NAME;
//
//        } else {
//
//            throw new IllegalArgumentException(
//                    "WikiTreeId:  " + WikiTreeApiUtilities.enquoteForJavaString( wikiTreeIdString ) +
//                    " does not look like either a WikiTreeId-style person name, space name or numeric id"
//            );
//
//        }

    }

    @NotNull
    public String getName() {

        return _wikiTreeIdString;
//        if ( isPersonName() || isSpaceName() ) {
//
//            return _wikiTreeIdString;
//
//        } else {
//
//            throw new IllegalArgumentException(
//                    "WikiTreeId.getWikiTreeStyleName:  " + WikiTreeApiUtilities.enquoteForJavaString( _wikiTreeIdString ) +
//                    " is not a WikiTreeId-style person or space name"
//            );
//
//        }

    }

    /**
     Determine if a string represents a value which can be used as a Space.Id or a Person.Id.
     <p/>Note that this method doesn't really have anything to do with WikITree Ids. It should probably find a better home.
     @param numericIdString the id string.
     @return {@code true} if the specified id string yields a positive value when parsed using {@link Long#parseLong(String)};
     {@code false} otherwise.
     */

    public static boolean isValidNumericIdString( String numericIdString ) {

        try {

            long id = Long.parseLong( numericIdString );

            return id > 0;

        } catch ( NumberFormatException e ) {

            return false;

        }

    }

    /**
     Determine if a string contains a valid WikiTree ID Name.
     A valid WikiTree ID Name is defined as a string which is not a valid WikiTree Space name
     (does not consist of {@code "Space:"} followed by at least one additional character)
     and does contain a sequence of one or more characters followed by a minus sign followed by one or more digits.
     This is a rather loose definition but it does eliminate anything which is not a valid WikiTree ID Name.
     @param wikiTreeIdName the proposed WikiTree ID Name.
     @return {@code true} if the string satisfies the above definition of a valid WikiTree ID Name; {@code false} otherwise.
     */

    public static boolean isValidWikiTreeIdPersonName( String wikiTreeIdName ) {

        if ( isValidWikiTreeSpaceName( wikiTreeIdName ) ) {

            return false;

        } else {

            Matcher mWikiTreeName = WIKITREE_ID_NAME_PATTERN.matcher( wikiTreeIdName );
            return mWikiTreeName.matches();

        }

    }

    /**
     Determine if a string contains a valid WikiTree Space Name.
     An string which starts with {@code "Space:"} and contains at least one additional character is considered to be
     a WikiTree Space Name.
     @param spaceWikiTreeId the proposed WikiTree Space Name.
     @return {@code true} if the string starts with {@code "Space:"} and contains at least one additional character; {@code false} otherwise.
     */

    public static boolean isValidWikiTreeSpaceName( String spaceWikiTreeId ) {

        Matcher mSpaceName = SPACE_NAME_PATTERN.matcher( spaceWikiTreeId );
        return mSpaceName.matches();

    }

    /**
     Determine if this instance encapsulates a WikiTree ID name.
     <p/>A WikiTree ID Name consists of a sequence of one or more characters followed by a minus sign followed by one or more digits (for example, {@code "Churchill-4"}).
     <p/>ALWAYS yields the same result as a call to {@link #isPersonName()}.
     <p/>Since there are only two kinds of ids managed by this class, a call to this method always yields the opposite result as a call to
     {@link #isSpaceName()}.
     @return {@code true} if this instance encapsulates a WikiTreeId Name; {@code false} otherwise.
     */

    public boolean isIdName() {

        return _isIdName;

    }

    /**
     Determine if this instance encapsulates a WikiTree Person Name (another way of asking if this instance encapsulates a WikiTree ID name).
     <p/>ALWAYS yields the same result as a call to {@link #isIdName()}.
     <p/>Since there are only two kinds of ids managed by this class, a call to this method always yields the opposite result as a call to
     {@link #isSpaceName()}.
     @return {@code true} if this instance encapsulates a WikiTreeId Name; {@code false} otherwise.
     */

    public boolean isPersonName() {

        return isIdName();

    }

    /**
     Determine if this instance encapsulates a WikiTree Space Name.
     <p/>A WikiTree Space Name starts with {@code Space:} and has at least one character after the colon (for example, {@code "Space:Allied_POW_camps"}).
     <p/>Since there are only two kinds of ids managed by this class, a call to this method always yields the opposite result as a call to
     either {@link #isIdName()} or {@link #isPersonName()}.
     @return {@code true} if this instance encapsulates a WikiTree Space Name; {@code false} otherwise.
     */

    public boolean isSpaceName() {

        return !isIdName();

    }

//    public Kind getKind() {
//
//        return _kind;
//
//    }
//
//    public boolean isPersonName() {
//
//        return _kind == Kind.PERSON_NAME;
//
//    }
//
//    public boolean isNumericId() {
//
//        return _kind == Kind.NUMERIC;
//
//    }

//    /**
//     Determine if this instance encapsulate the name of a WikiTree space.
//
//     @return {@code true} if this instance encapsulates the name of a WikiTree space (i.e. it is a string that starts with {@code "Space:"}).
//     {@code false} otherwise.
//     */
//
//    public boolean isSpaceName() {
//
//        return _kind == Kind.SPACE_NAME;
//
//    }
//
//    /**
//     Get this instance's value as an integer.
//
//     @return this instance's value as an integer.
//     @throws NumberFormatException if this instance's value string cannot be parsed as a positive integer.
//     */
//
//    public long getNumericId() {
//
//        return Long.parseLong( _wikiTreeIdString );
//
//    }

    /**
     Get this instance's value string.

     @return this instance's value string (the string that was provided when this instance was created).
     */

    @NotNull
    public String getValueString() {

        return _wikiTreeIdString;

        //        if ( _wikiTreeIdString.startsWith( "Id=" ) ) {
//
//            return _wikiTreeIdString.substring( 3 );
//
//        } else {
//
//            return _wikiTreeIdString;
//
//        }

    }

    public boolean equals( Object rhs ) {

        return rhs instanceof WikiTreeId && compareTo( (WikiTreeId)rhs ) == 0;

    }

    public int hashCode() {

        return _wikiTreeIdString.hashCode();

    }

    public String toString() {

        return _wikiTreeIdString;

    }

    @Override
    public int compareTo( @NotNull final WikiTreeId o ) {

        return getValueString().compareTo( o.getValueString() );

    }

}
