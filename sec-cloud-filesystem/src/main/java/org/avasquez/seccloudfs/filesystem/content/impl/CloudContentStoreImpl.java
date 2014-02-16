package org.avasquez.seccloudfs.filesystem.content.impl;

import org.avasquez.seccloudfs.cloud.CloudStore;
import org.avasquez.seccloudfs.filesystem.content.Content;
import org.avasquez.seccloudfs.filesystem.db.model.ContentMetadata;
import org.avasquez.seccloudfs.filesystem.db.repos.ContentMetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by alfonsovasquez on 12/01/14.
 */
public class CloudContentStoreImpl extends AbstractCachedContentStore {

    private static final Logger logger = LoggerFactory.getLogger(CloudContentStoreImpl.class);

    private ContentMetadataRepository metadataRepo;
    private CloudStore cloudStore;
    private Path downloadsDir;
    private Path snapshotDir;
    private ScheduledExecutorService executorService;
    private long timeoutForNextUpdateSecs;
    private long retryUploadDelaySecs;

    @Required
    public void setMetadataRepo(ContentMetadataRepository metadataRepo) {
        this.metadataRepo = metadataRepo;
    }

    @Required
    public void setCloudStore(CloudStore cloudStore) {
        this.cloudStore = cloudStore;
    }

    @Required
    public void setDownloadsDir(String downloadsDir) {
        this.downloadsDir = Paths.get(downloadsDir);
    }

    @Required
    public void setSnapshotDir(String snapshotDir) {
        this.snapshotDir = Paths.get(snapshotDir);
    }

    @Required
    public void setExecutorService(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    @Required
    public void setTimeoutForNextUpdateSecs(long timeoutForNextUpdateSecs) {
        this.timeoutForNextUpdateSecs = timeoutForNextUpdateSecs;
    }

    @Required
    public void setRetryUploadDelaySecs(long retryUploadDelaySecs) {
        this.retryUploadDelaySecs = retryUploadDelaySecs;
    }

    @Override
    protected Content doFind(String id) throws IOException {
        ContentMetadata metadata = metadataRepo.find(id);
        if (metadata != null) {
            return createContentObject(metadata);
        } else {
            return null;
        }
    }

    @Override
    protected Content doCreate() throws IOException {
        ContentMetadata metadata = new ContentMetadata();
        metadataRepo.insert(metadata);

        Content content = createContentObject(metadata);

        logger.info("{} created", content);

        return content;
    }

    @Override
    protected void doDelete(Content content) throws IOException {
        ((CloudContentImpl) content).delete();
    }

    private Content createContentObject(ContentMetadata metadata) throws IOException {
        Path downloadPath = downloadsDir.resolve(metadata.getId());
        Lock accessLock = new ReentrantLock();
        Uploader uploader = new Uploader(metadata, metadataRepo, cloudStore, downloadPath, accessLock,
                snapshotDir, executorService, timeoutForNextUpdateSecs, retryUploadDelaySecs);

        return new CloudContentImpl(metadata, metadataRepo, downloadPath, accessLock, cloudStore, uploader);
    }

}