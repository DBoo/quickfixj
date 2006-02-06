/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved. 
 * 
 * This file is part of the QuickFIX FIX Engine 
 * 
 * This file may be distributed under the terms of the quickfixengine.org 
 * license as defined by quickfixengine.org and appearing in the file 
 * LICENSE included in the packaging of this file. 
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 * 
 * See http://www.quickfixengine.org/LICENSE for licensing information. 
 * 
 * Contact ask@quickfixengine.org if any conditions of this licensing 
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import quickfix.field.converter.UtcTimestampConverter;

/**
 * File store implementation. THIS CLASS IS PUBLIC ONLY TO MAINTAIN
 * COMPATIBILITY WITH THE QUICKFIX JNI. IT SHOULD ONLY BE CREATED USING A
 * FACTORY.
 * 
 * @see quickfix.FileStoreFactory
 */
public class FileStore implements RefreshableMessageStore {
    private MemoryStore cache = new MemoryStore();
    private RandomAccessFile msgFile;
    private RandomAccessFile headerFile;
    private RandomAccessFile seqNumFile;
    private RandomAccessFile sessionFile;
    private String msgFileName;
    private String headerFileName;
    private String seqNumFileName;
    private String sessionFileName;

    FileStore(String path, SessionID sessionID) throws IOException {
        if (path == null) {
            path = ".";
        }
        String sessionId = sessionID.getBeginString() + "-" + sessionID.getSenderCompID() + "-"
                + sessionID.getTargetCompID();
        if (sessionID.getSessionQualifier() != null && !sessionID.getSessionQualifier().equals("")) {
            sessionId += "-" + sessionID.getSessionQualifier();
        }
        String prefix = FileUtil.fileAppendPath(path, sessionId + ".");

        msgFileName = prefix + "body";
        headerFileName = prefix + "header";
        seqNumFileName = prefix + "seqnums";
        sessionFileName = prefix + "session";

        File directory = new File(msgFileName).getParentFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }

        initializeFiles(false);
    }

    protected void initializeFiles(boolean delete) throws IOException {
        closeFiles();

        if (delete) {
            deleteFiles();
        }

        msgFile = new RandomAccessFile(msgFileName, "rwd");
        headerFile = new RandomAccessFile(headerFileName, "rwd");
        seqNumFile = new RandomAccessFile(seqNumFileName, "rwd");
        sessionFile = new RandomAccessFile(sessionFileName, "rwd");

        loadCache();
    }

    public void closeFiles() throws IOException {
        closeFile(msgFile);
        closeFile(headerFile);
        closeFile(seqNumFile);
        closeFile(sessionFile);
    }

    public void deleteFiles() throws IOException {
        closeFiles();
        new File(headerFileName).delete();
        new File(msgFileName).delete();
        new File(seqNumFileName).delete();
        new File(sessionFileName).delete();
    }

    private void loadCache() throws IOException {
        loadMessageIndex();
        loadSequenceNumbers();
        loadCreateTime();
    }

    private void loadCreateTime() throws IOException {
        if (sessionFile.length() > 0) {
            byte[] data = new byte[17];
            sessionFile.seek(0);
            sessionFile.read(data);
            try {
                Date date = UtcTimestampConverter.convert(new String(data));
                Calendar c = SystemTime.getUtcCalendar();
                c.setTime(date);
                cache.setCreationTime(c);
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
        }
    }

    private void loadSequenceNumbers() throws IOException {
        seqNumFile.seek(0);
        if (seqNumFile.length() > 0) {
            String s = seqNumFile.readUTF();
            int offset = s.indexOf(" : ");
            cache.setNextSenderMsgSeqNum(Integer.parseInt(s.substring(0, offset)));
            cache.setNextTargetMsgSeqNum(Integer.parseInt(s.substring(offset + 3)));
        }
        //System.err.println("file store: sender=" +
        // cache.getNextSenderMsgSeqNum() + " target="
        //        + cache.getNextTargetMsgSeqNum());
    }

    private HashMap messageIndex = new HashMap();

    private void loadMessageIndex() throws IOException {
        headerFile.seek(0);
        for (int ch = headerFile.read(); ch != -1; ch = headerFile.read()) {
            long sequenceNumber = readLong(headerFile, ch);
            long[] offsetAndSize = new long[] { readLong(headerFile), readLong(headerFile) };
            messageIndex.put(new Long(sequenceNumber), offsetAndSize);
        }
    }

    private long readLong(RandomAccessFile in) throws IOException {
        return in.length() > 0 ? readLong(in, in.read()) : -1;
    }

    private long readLong(RandomAccessFile in, int ch) throws IOException {
        while (in.getFilePointer() < in.length() && !Character.isDigit((char) ch)) {
            ch = in.read();
        }
        long n = 0;
        do {
            n = n * 10 + (ch - '0');
            if (in.getFilePointer() >= in.length()) {
                break;
            }
            ch = in.read();
        } while (Character.isDigit((char) ch));
        return n;
    }

    private void closeFile(RandomAccessFile file) throws IOException {
        if (file != null) {
            file.close();
        }
    }

    public Date getCreationTime() throws IOException {
        return cache.getCreationTime();
    }

    public int getNextSenderMsgSeqNum() throws IOException {
        return cache.getNextSenderMsgSeqNum();
    }

    public int getNextTargetMsgSeqNum() throws IOException {
        return cache.getNextTargetMsgSeqNum();
    }

    public void incrNextSenderMsgSeqNum() throws IOException {
        cache.incrNextSenderMsgSeqNum();
        storeSequenceNumbers();
    }

    public void incrNextTargetMsgSeqNum() throws IOException {
        cache.incrNextTargetMsgSeqNum();
        storeSequenceNumbers();
    }

    public void reset() throws IOException {
        cache.reset();
        storeSequenceNumbers();
        storeSessionTimeStamp();
    }

    public void get(int startSequence, int endSequence, Collection messages) throws IOException {
        for (int i = startSequence; i <= endSequence; i++) {
            String message = getMessage(i);
            if (message != null) {
                messages.add(message);
            }
        }
    }

    /**
     * This method is here for JNI API consistency but it's not 
     * implemented. Use get(int, int, Collection) with the same 
     * start and end sequence.
     * 
     */
    public boolean get(int sequence, String message)throws IOException {
        throw new UnsupportedOperationException("not supported");
    }
    
    private String getMessage(int i) throws IOException {
        long[] offsetAndSize = (long[]) messageIndex.get(new Long(i));
        String message = null;
        if (offsetAndSize != null) {
            msgFile.seek(offsetAndSize[0]);
            byte[] data = new byte[(int) offsetAndSize[1]];
            msgFile.read(data);
            message = new String(data);
        }
        return message;
    }

    public boolean set(int sequence, String message) throws IOException {
        msgFile.seek(msgFile.length());
        headerFile.seek(headerFile.length());

        long offset = msgFile.getFilePointer();
        StringBuffer headerBuffer = new StringBuffer();
        if (offset > 0) {
            headerBuffer.append(' ');
        }
        int size = message.length();
        messageIndex.put(new Long(sequence), new long[] { offset, size });
        headerBuffer.append(sequence);
        headerBuffer.append(",");
        headerBuffer.append(offset);
        headerBuffer.append(",");
        headerBuffer.append(size);
        headerFile.write(headerBuffer.toString().getBytes());
        msgFile.write(message.getBytes());
        return true;
    }

    public void setNextSenderMsgSeqNum(int next) throws IOException {
        cache.setNextSenderMsgSeqNum(next);
        storeSequenceNumbers();
    }

    public void setNextTargetMsgSeqNum(int next) throws IOException {
        cache.setNextTargetMsgSeqNum(next);
        storeSequenceNumbers();
    }

    private void storeSessionTimeStamp() throws IOException {
        sessionFile.seek(0);
        String formattedTime = UtcTimestampConverter.convert(SystemTime.getDate(), false);
        sessionFile.write(formattedTime.getBytes());
    }

    private void storeSequenceNumbers() throws IOException {
        // TODO PERFORMANCE This should use a more efficient byte buffer
        // TODO PERFORMANCE Use Javolution fast object pooling and primitive
        // formatters
        seqNumFile.seek(0);
        StringBuffer sb = new StringBuffer();
        sb.append(Integer.toString(cache.getNextSenderMsgSeqNum())).append(" : ").append(
                Integer.toString(cache.getNextTargetMsgSeqNum()));
        seqNumFile.writeUTF(sb.toString());
    }

    String getHeaderFileName() {
        return headerFileName;
    }

    String getMsgFileName() {
        return msgFileName;
    }

    String getSeqNumFileName() {
        return seqNumFileName;
    }

    public void refresh() throws IOException {
        loadCache();
    }
}