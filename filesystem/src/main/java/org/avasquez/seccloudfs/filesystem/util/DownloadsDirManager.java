package org.avasquez.seccloudfs.filesystem.util;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.content.CloudContent;
import org.avasquez.seccloudfs.filesystem.content.ContentStore;
import org.avasquez.seccloudfs.filesystem.db.dao.FileMetadataDao;
import org.avasquez.seccloudfs.filesystem.db.model.FileMetadata;
import org.avasquez.seccloudfs.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

/**
 * Created by alfonsovasquez on 09/02/14.
 */
public class DownloadsDirManager {

    private static final Logger logger = LoggerFactory.getLogger(DownloadsDirManager.class);

    private Path downloadsDir;
    private long maxDirSize;
    private FileMetadataDao fileMetadataDao;
    private ContentStore contentStore;

    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = Paths.get(downloadsDir);
    }

    public void setMaxDirSize(long maxDirSize) {
        this.maxDirSize = maxDirSize;
    }

    public void setFileMetadataDao(FileMetadataDao fileMetadataDao) {
        this.fileMetadataDao = fileMetadataDao;
    }

    public void setContentStore(ContentStore contentStore) {
        this.contentStore = contentStore;
    }

    public void deleteDownloadsOnMaxDirSize() {
        long dirSize;
        try {
            dirSize = FileUtils.sizeOfDirectory(downloadsDir);
        } catch (IOException e) {
            logger.error("Unable to calculate size of downloads dir " + downloadsDir, e);

            return;
        }

        if (dirSize > maxDirSize) {
            logger.info("Downloads dir {} max size {} reached (current size {})", downloadsDir, maxDirSize, dirSize);

            Iterable<FileMetadata> lruMetadata;
            try {
                lruMetadata = fileMetadataDao.findAllSortedByDescLastAccessTime();
            } catch (DbException e) {
                logger.error("Error while retrieving file metadata sorted by desc last access time", e);

                return;
            }

            Iterator<FileMetadata> lruIter = lruMetadata.iterator();
            while (dirSize > maxDirSize && lruIter.hasNext()){
                FileMetadata fileMetadata = lruIter.next();
                String contentId = fileMetadata.getContentId();

                try {
                    CloudContent content = (CloudContent) contentStore.find(contentId);
                    long size = content.getSize();
                    boolean deleted = content.deleteDownload();

                    if (deleted) {
                        logger.info("Download for content '{}' deleted", fileMetadata);

                        dirSize -= size;
                    } else {
                        logger.info("Download for content {} couldn't be deleted (probably being used)");
                    }
                } catch (IOException e) {
                    logger.error("Unable to delete download of content '" + contentId + "'", e);
                }
            }

            if (dirSize <= maxDirSize) {
                logger.info("Downloads dir {} was shrunk successfully to size {}", downloadsDir, dirSize);
            } else {
                logger.warn("Downloads dir {} couldn't be shrunk successfully to less than max size {}. " +
                        "Current size is {}", downloadsDir, maxDirSize, dirSize);
            }
        }
    }

}
