package org.avasquez.seccloudfs.filesystem.files.impl;

import org.avasquez.seccloudfs.exception.DbException;
import org.avasquez.seccloudfs.filesystem.db.model.DirectoryEntry;
import org.avasquez.seccloudfs.filesystem.db.repos.DirectoryEntryRepository;
import org.avasquez.seccloudfs.utils.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by alfonsovasquez on 01/02/14.
 */
public class DirectoryEntries {

    private DirectoryEntryRepository entryRepository;
    private String directoryId;
    private Map<String, DirectoryEntry> entries;

    public DirectoryEntries(DirectoryEntryRepository entryRepository, String directoryId) throws IOException {
        this.entryRepository = entryRepository;
        this.directoryId = directoryId;
        this.entries = new ConcurrentHashMap<>();

        List<DirectoryEntry> entries = CollectionUtils.asList(entryRepository.findByDirectoryId(directoryId));
        if (entries != null) {
            entries = deleteDuplicateEntries(entries);
            for (DirectoryEntry entry : entries) {
                this.entries.put(entry.getFileName(), entry);
            }
        }
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public boolean hasEntry(String fileName) {
        return entries.containsKey(fileName);
    }

    public DirectoryEntry getEntry(String fileName) {
        return entries.get(fileName);
    }

    public String[] getFileNames() {
        Set<String> fileNames = entries.keySet();

        return fileNames.toArray(new String[fileNames.size()]);
    }

    public DirectoryEntry createEntry(String fileName, String fileId) throws IOException {
        DirectoryEntry entry = new DirectoryEntry(directoryId, fileName, fileId, new Date());

        entryRepository.insert(entry);

        entries.put(fileName, entry);

        return entry;
    }

    public void moveEntryTo(String fileName, DirectoryEntries dst, String newFileName) throws IOException {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            DirectoryEntry replacedEntry = dst.entries.get(fileName);
            DirectoryEntry movedEntry = new DirectoryEntry(entry.getId(),
                    dst.directoryId,
                    newFileName,
                    entry.getFileId(),
                    new Date());

            entryRepository.save(movedEntry);

            entries.remove(fileName);
            dst.entries.put(newFileName, movedEntry);

            if (replacedEntry != null) {
                entryRepository.delete(replacedEntry.getId());
            }
        }
    }

    public void deleteEntry(String fileName) throws IOException {
        DirectoryEntry entry = getEntry(fileName);
        if (entry != null) {
            entryRepository.delete(entry.getId());

            entries.remove(fileName);
        }
    }

    @Override
    public String toString() {
        return "DirectoryEntries{" +
                "directoryId='" + directoryId + '\'' +
                ", entries=" + entries +
                '}';
    }

    private List<DirectoryEntry> deleteDuplicateEntries(List<DirectoryEntry> entries) throws DbException {
        List<DirectoryEntry> nonDupEntries = new ArrayList<>();
        List<DirectoryEntry> dupEntries = new ArrayList<>();

        for (DirectoryEntry entry : entries) {
            if (!hasEntryWithSameName(nonDupEntries, entry)) {
                DirectoryEntry dupEntry = getDuplicateEntry(entries, entry);
                if (dupEntry != null) {
                    if (dupEntry.getAddedDate().after(entry.getAddedDate())) {
                        nonDupEntries.add(dupEntry);
                        dupEntries.add(entry);
                    } else {
                        nonDupEntries.add(entry);
                        dupEntries.add(dupEntry);
                    }
                } else {
                    nonDupEntries.add(entry);
                }
            }
        }

        for (DirectoryEntry dupEntry : dupEntries) {
            entryRepository.delete(dupEntry.getId());
        }

        return nonDupEntries;
    }

    private boolean hasEntryWithSameName(List<DirectoryEntry> entries, DirectoryEntry entry) {
        for (DirectoryEntry ent : entries) {
            if (ent.getFileName().equals(entry.getFileName())) {
                return true;
            }
        }

        return false;
    }

    private DirectoryEntry getDuplicateEntry(List<DirectoryEntry> entries, DirectoryEntry entry) {
        for (DirectoryEntry ent : entries) {
            if (ent.getFileName().equals(entry.getFileName())) {
                return ent;
            }
        }

        return null;
    }

}
