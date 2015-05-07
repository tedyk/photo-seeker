package org.tedka.photoseeker.channel.base;

import java.util.ArrayList;

/**
 * Base model for a Search provider. Extend this to integrate a new provider
 */
abstract public class ChannelController {
    /**
     * Return the search results data
     *
     * @return
     */
    abstract public ArrayList<ChannelModel> getImageFeed();

    /**
     * Construct and return the URL for the full-size photo at a given position in the results
     *
     * @param photoPosition
     * @return
     */
    abstract public String getPhotoUrl(int photoPosition);

}
