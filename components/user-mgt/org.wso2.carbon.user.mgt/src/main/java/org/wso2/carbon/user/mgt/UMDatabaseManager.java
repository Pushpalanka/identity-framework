/*
 * Copyright (c) 2010 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.user.mgt;

import org.wso2.carbon.user.core.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class UMDatabaseManager {

    private DataSource dataSource = null;

    private static final String CREATE_TABLE = "CREATE TABLE USER_MGT_PROPERTIES(ID INTEGER GENERATED BY DEFAULT AS IDENTITY, "
            + "PROPERTY_NAME VARCHAR(255) NOT NULL, "
            + "PROPERTY_VALUE VARCHAR(255), "
            + "PRIMARY KEY (ID))";
    private static final String SET_PROPERTY = "INSERT INTO USER_MGT_PROPERTIES(PROPERTY_NAME, PROPERTY_VALUE) VALUES (?,?)";
    private static final String GET_PROPERTY = "SELECT PROPERTY_VALUE FROM USER_MGT_PROPERTIES WHERE PROPERTY_NAME=?";
    private static final String GET_PROPERTIES = "SELECT PROPERTY_NAME, PROPERTY_VALUE FROM USER_MGT_PROPERTIES";
    private static final String DELETE_PROPERTIES = "DELETE * FROM USER_MGT_PROPERTIES";

    public UMDatabaseManager(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getProperty(String name) throws SQLException {
        String value = null;
        PreparedStatement stmt = null;
        Connection dbConnection = null;
        ResultSet rs = null;
        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(GET_PROPERTY);
            stmt.setString(1, name);
            stmt.executeQuery();
            rs = stmt.executeQuery();
            if (rs.next()) {
                value = rs.getString(1);
            }
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, stmt);
        }
        return value;
    }

    public Map<String, String> getExternalStoreProperties() throws SQLException {
        Connection dbConnection = null;
        Map<String, String> map = new HashMap<String, String>();
        ResultSet rs = null;
        PreparedStatement stmt = null;

        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(GET_PROPERTIES);
            rs = stmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                map.put(name, value);
            }
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, rs, stmt);
        }
        return map;
    }

    public void deleteAllProperties() throws SQLException {
        Connection dbConnection = null;
        PreparedStatement stmt = null;

        try {
            dbConnection = dataSource.getConnection();
            stmt = dbConnection.prepareStatement(DELETE_PROPERTIES);
            stmt.executeUpdate();
        } finally {
            DatabaseUtil.closeAllConnections(dbConnection, stmt);
        }
    }

    public void setProperty(String name, String value) throws SQLException {
        try (Connection dbConnection = dataSource.getConnection()) {
            dbConnection.setAutoCommit(false);
            try (PreparedStatement stmt = dbConnection.prepareStatement(SET_PROPERTY)) {
                stmt.setString(1, name);
                stmt.setString(2, value);
                stmt.executeUpdate();
                stmt.executeUpdate();
            }
        }
    }

    public void createManagementTables() throws SQLException {

        Connection dbConnection = null;
        ResultSet rs = null;
        Statement stmt = null;
        try {

            dbConnection = dataSource.getConnection();
            DatabaseMetaData dbmd = dbConnection.getMetaData();
            rs = dbmd.getTables(null, null, "USER_MGT_PROPERTIES", null);
            stmt =
                    dbConnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            if (!rs.next()) {

                stmt.executeUpdate(CREATE_TABLE);

                dbConnection.commit();
            }

        } finally {

            if (rs != null) {
                rs.close();
            }
            if (stmt != null) {
                stmt.close();
            }
            DatabaseUtil.closeAllConnections(dbConnection);
        }
    }

}
