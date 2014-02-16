package org.avasquez.seccloudfs.cloud;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Handles the storage of data in the cloud.
 *
 * @author avasquez
 */
public interface CloudStore {

    /**
     * Uploads the given data in the cloud.
     *
     * @param dataId    the ID used to identify the data
     * @param src       the source channel from where the data can be retrieved
     * @param length    the length of the data
     */
    void upload(String dataId, ReadableByteChannel src, long length) throws IOException;

    /**
     * Downloads the data from the cloud.
     *
     * @param dataId    the ID used to identify the data
     * @param target    the target channel to where the data will be written
     */
    void download(String dataId, WritableByteChannel target) throws IOException;

    /**
     * Deletes the data.
     *
     * @param dataId    the ID used to identify the data.
     */
    void delete(String dataId) throws IOException;

}