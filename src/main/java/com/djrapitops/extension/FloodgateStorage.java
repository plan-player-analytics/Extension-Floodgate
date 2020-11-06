/*
 * Copyright(c) 2020 Risto Lahtela (Rsl1122)
 *
 * The MIT License(MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files(the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions :
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.djrapitops.extension;

import com.djrapitops.plan.query.QueryService;
import org.geysermc.floodgate.util.DeviceOS;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class FloodgateStorage {

    private final QueryService queryService;

    public FloodgateStorage() {
        queryService = QueryService.getInstance();
        createTable();
        queryService.subscribeDataClearEvent(this::recreateTable);
        queryService.subscribeToPlayerRemoveEvent(this::removePlayer);
    }

    private void createTable() {
        String dbType = queryService.getDBType();
        boolean sqlite = dbType.equalsIgnoreCase("SQLITE");

        String sql = "CREATE TABLE IF NOT EXISTS plan_platforms (" +
                "id int " + (sqlite ? "PRIMARY KEY" : "NOT NULL AUTO_INCREMENT") + ',' +
                "uuid varchar(36) NOT NULL UNIQUE," +
                "platform int NOT NULL," +
                "bedrockUsername VARCHAR(32) NOT NULL UNIQUE," +
                "javaUsername VARCHAR(16) NOT NULL," +
                "linkedPlayer VARCHAR(16) NULL," +
                "languageCode VARCHAR(8) NOT NULL," +
                "version VARCHAR(16) NOT NULL" +
                (sqlite ? "" : ",PRIMARY KEY (id)") +
                ")";

        queryService.execute(sql, PreparedStatement::execute);
    }

    private void dropTable() {
        queryService.execute("DROP TABLE IF EXISTS plan_platforms", PreparedStatement::execute);
    }

    private void recreateTable() {
        createTable();
        dropTable();
    }

    public void storePlayer(UUID playerUUID, DeviceOS platform, String bedrockUsername, String javaUsername,
                            String linkedJavaPlayer, String languageCode, String version) throws ExecutionException {
        String update = "UPDATE plan_platforms SET " +
                "platform = ?, " +
                "bedrockUsername = ?, " +
                "javaUsername = ?, " +
                "linkedPlayer = ?, " +
                "languageCode = ?, " +
                "version = ? " +
                "WHERE uuid = ?";
        String insert = "INSERT INTO plan_platforms (" +
                "platform, bedrockUsername, javaUsername, " +
                "linkedPlayer, languageCode, version, uuid" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?)";

        QueryService.ThrowingConsumer<PreparedStatement> dataSetter = preparedStatement -> {
            preparedStatement.setInt(1, platform.ordinal());
            preparedStatement.setString(2, bedrockUsername);
            preparedStatement.setString(3, javaUsername);
            preparedStatement.setString(4, linkedJavaPlayer);
            preparedStatement.setString(5, languageCode);
            preparedStatement.setString(6, version);
            preparedStatement.setString(7, playerUUID.toString());
        };

        AtomicBoolean updated = new AtomicBoolean(false);
        try {
            queryService.execute(update, statement -> {
                dataSetter.accept(statement);
                updated.set(statement.executeUpdate() > 0);
            }).get(); // Wait
            if (!updated.get()) {
                queryService.execute(insert, statement -> {
                    dataSetter.accept(statement);
                    statement.execute();
                });
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void removePlayer(UUID playerUUID) {
        queryService.execute(
                "DELETE FROM plan_platforms WHERE uuid=?",
                statement -> {
                    statement.setString(1, playerUUID.toString());
                    statement.execute();
                }
        );
    }

    public DeviceOS getPlatform(UUID playerUUID) {
        String sql = "SELECT platform FROM plan_platforms WHERE uuid=?";

        return queryService.query(sql, statement -> {
            statement.setString(1, playerUUID.toString());
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? DeviceOS.getById(set.getInt("platform")) : null;
            }
        });
    }

    private String getStringFromTable(UUID playerUUID, String data) {
        String sql = "SELECT " + data + " FROM plan_platforms WHERE uuid=?";

        return queryService.query(sql, statement -> {
            statement.setString(1, playerUUID.toString());
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getString(data) : null;
            }
        });
    }

    public String getBedrockUsername(UUID playerUUID) {
        return getStringFromTable(playerUUID, "bedrockUsername");
    }

    public String getJavaUsername(UUID playerUUID) {
        return getStringFromTable(playerUUID, "javaUsername");
    }

    public String getLinkedPlayer(UUID playerUUID) {
        return getStringFromTable(playerUUID, "linkedPlayer");
    }

    public String getLanguageCode(UUID playerUUID) {
        return getStringFromTable(playerUUID, "languageCode");
    }

    public String getVersion(UUID playerUUID) {
        return getStringFromTable(playerUUID, "version");
    }
}
