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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JdbcLog implements quickfix.Log {
    private Logger log = LoggerFactory.getLogger(getClass());
    private static final String MESSAGES_LOG_TABLE = "messages_log";
    private static final String EVENT_LOG_TABLE = "event_log";
    private Connection connection;
    private SessionID sessionID;

    public JdbcLog(SessionSettings settings, SessionID sessionID) throws SQLException,
            ClassNotFoundException, ConfigError, FieldConvertError {
        this.sessionID = sessionID;
        connection = connect(settings, sessionID);
    }

    protected Connection connect(SessionSettings settings, SessionID sessionID2)
            throws SQLException, ClassNotFoundException, ConfigError, FieldConvertError {
        return JdbcUtil.openConnection(settings, sessionID);
    }

    public void onEvent(String value) {
        insert(EVENT_LOG_TABLE, value);
    }

    public void onIncoming(String value) {
        insert(MESSAGES_LOG_TABLE, value);
    }

    public void onOutgoing(String value) {
        insert(MESSAGES_LOG_TABLE, value);
    }

    private void insert(String tableName, String value) {
        PreparedStatement insert = null;
        try {
            insert = connection.prepareStatement("INSERT INTO " + tableName
                    + " (time, beginstring, sendercompid, targetcompid, session_qualifier, text) "
                    + "VALUES (?,?,?,?,?,?)");
            insert.setTimestamp(1, new Timestamp(SystemTime.getUtcCalendar().getTimeInMillis()));
            insert.setString(2, sessionID.getBeginString());
            insert.setString(3, sessionID.getSenderCompID());
            insert.setString(4, sessionID.getTargetCompID());
            insert.setString(5, sessionID.getSessionQualifier());
            insert.setString(6, value);
            insert.execute();
        } catch (SQLException e) {
            throw new RuntimeError(e);
        } finally {
            if (insert != null) {
                try {
                    insert.close();
                } catch (SQLException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * Deletes all rows from the log tables.
     */
    public void clear() {
        clearTable(EVENT_LOG_TABLE);
        clearTable(MESSAGES_LOG_TABLE);
    }

    private void clearTable(String tableName) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement("DELETE FROM " + tableName
                    + " WHERE beginString=? AND senderCompID=? "
                    + "AND targetCompID=? AND session_qualifier=?");
            statement.setString(1, sessionID.getBeginString());
            statement.setString(2, sessionID.getSenderCompID());
            statement.setString(3, sessionID.getTargetCompID());
            statement.setString(4, sessionID.getSessionQualifier());
            statement.execute();
        } catch (SQLException e) {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e1) {
                    log.error(e1.getMessage(), e1);
                }
            }
        }
    }

}