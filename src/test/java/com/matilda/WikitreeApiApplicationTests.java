package com.matilda;

import com.matilda.wikitree.api.util.WikiTreeApiUtilities;
import com.matilda.wikitree.api.wrappers.WikiTreeApiWrappersSession;
import com.matilda.wikitree.api.wrappers.WikiTreeId;
import com.matilda.wikitree.api.wrappers.WikiTreePersonProfile;
import org.junit.Test;

public class WikitreeApiApplicationTests {

    @Test
    public void junitok() {
    }

    @Test
    public void anonymouslookup() throws Exception {
        WikiTreeApiWrappersSession request = new WikiTreeApiWrappersSession();

        WikiTreeApiUtilities.maybeLoginToWikiTree(request, new String[]{});

        WikiTreePersonProfile churchillPersonByWikiTreeID = request.getPerson(new WikiTreeId("Churchill-4"));
        WikiTreeApiUtilities.prettyPrintJsonThing(
                "Winston S. Churchill's person instance",
                churchillPersonByWikiTreeID
        );
    }
}
