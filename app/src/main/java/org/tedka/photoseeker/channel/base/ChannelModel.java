package org.tedka.photoseeker.channel.base;

/**
 * Base model for a Search provider's results. Extend this to integrate a new provider
 */
abstract public class ChannelModel {

    /**
     * Return the title of the photograph
     *
     * @return
     */
    abstract public String getTitle();
}
