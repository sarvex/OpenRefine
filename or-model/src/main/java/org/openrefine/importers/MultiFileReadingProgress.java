package org.openrefine.importers;

/**
 * Interface for reporting progress of reading a file,
 * by repeatedly updating how many bytes have been read in the file.
 */
public interface MultiFileReadingProgress {
    /**
     * Called when starting to read a file
     * @param fileSource
     */
    public void startFile(String fileSource);
    
    /**
     * Called while the file is being read, with the total number of bytes
     * read since the beginning of the file
     * 
     * @param fileSource
     * @param bytesRead
     */
    public void readingFile(String fileSource, long bytesRead);
    
    /**
     * Called at the end of reading of a file
     * @param fileSource
     * @param bytesRead
     */
    public void endFile(String fileSource, long bytesRead);
}