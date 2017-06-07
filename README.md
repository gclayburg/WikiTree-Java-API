#The Java WikiTree API
by Daniel Boulet (danny@matilda.com)
Copyright © 2017 All rights reserved

This software provides a Java implementation of the client side of the WikiTree API.

See the WikiTree API Help page located at <https://www.wikitree.com/wiki/Help:API_Documentation> for more info on the WikiTree API.
**Essentially all of the documentation in this project assumes that you are familiar with what's on that web page.**

### Project structure
This software consists of two parts:

* the "json-simple-master" directory where you will find version 1.1.1 JSON.simple which is a really quite
impressive implementation of a JSON parser and related support classes.
Please see http://code.google.com/p/json-simple/ for more information.
* the "src" directory where you will find the actual Java implementation of client side of the WikiTree API.

This entire "bundle of stuff" is an IntelliJ IDEA project. If you are not already an IntelliJ IDEA user,
download it from the www.jetbrains.com (they have a payware version and a community edition that is free
and licensed under the Apache 2.0 license).
The Java WikiTree client API was developed using the payware version although you should have no problems
using the community edition with this project).
Just launch IntelliJ, click the "open new project" thingy and point IntelliJ at the base directory of this
"bundle of stuff" and you should be good to go.

### API layers
This API exists in two layers:

* The bottom layer is a fairly simple JSON-based interface to the 'official' WikiTree API.
By "JSON-based", I mean that the methods in this layer return JSON Objects API return JSON objects which the
caller will need to "tear apart" to get what they want out of them).
* The upper layer is a wrapper-based API in the sense that its methods each return Java wrappers which provide
a (hopefully) easier way to access the results of an API call.

### Getting started

The best place to start is almost certainly with the wrapper-based API layer.
The key Java class in this layer is "com.matilda.wikitree.api.wrappers.WikiTreeApiWrappersSession".
There is also a pretty useful demonstration program in "com.matilda.wikitree.api.examples.VerySimpleWrappersApiExample"
and another in "com.matilda.wikitree.api.examples.WrappersApiTestDrive.java".

If you prefer to use the JSON-based layer, the key Java class in that layer is
"com.matilda.wikitree.api.jsonclient.WikiTreeApiJsonSession".
There's a reasonably good demonstration program of the JSON-based layer in
"com.matilda.wikitree.api.examples.JsonApiTestDrive.java".
 
### Javadocs

Most of the classes and methods in this API have reasonably good Javadocs.
They should get better fairly quickly.

Please report any problems to danny@matilda.com

2017/06/06