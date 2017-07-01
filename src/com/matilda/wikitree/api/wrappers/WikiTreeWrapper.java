/*
 * Copyright © 2017 Daniel Boulet
 */

package com.matilda.wikitree.api.wrappers;

import org.jetbrains.annotations.NotNull;
import org.json.simple.JSONObject;

/**
 %%% Something clever goes here.
 */

public class WikiTreeWrapper extends JSONObject {

    private WikiTreeRequestType _requestType = WikiTreeRequestType.UNSPECIFIED;

    protected WikiTreeWrapper( @NotNull JSONObject jsonObject ) {

        super( jsonObject );

    }

    protected void setRequestType( @NotNull WikiTreeRequestType requestType ) {

        if ( requestType == WikiTreeRequestType.UNSPECIFIED ) {

            throw new IllegalArgumentException(
                    "WikiTreeWrapper.setRequestType:  cannot set request type to " + WikiTreeRequestType.UNSPECIFIED
            );

        } else if ( _requestType == WikiTreeRequestType.UNSPECIFIED ) {

            _requestType = requestType;

        } else {

            throw new IllegalArgumentException(
                    "WikiTreeWrapper.setRequestType:  request type can only be set once (is " + _requestType +
                    ", asked to set to " + requestType + ")"
            );

        }

    }

    public WikiTreeRequestType getRequestType() {

        return _requestType;

    }

//    @NotNull
//    public String getRequestKey() {
//
//	return _requestedKey;
//
//    }
//
//    @NotNull
//    public String getRequestedFields() {
//
//	return _requestedFields;
//
//    }

}
