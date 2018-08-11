package com.github.bingoohuang.sqlfilter;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Utils {
    @SneakyThrows
    public static Connection getH2Connection() {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:./src/test/resources/test", "sa", "");
    }

    @SneakyThrows
    public static int executeUpdate(PreparedStatement ps, Object... boundParameters) {
        for (int i = 0; i < boundParameters.length; ++i) {
            ps.setObject(i + 1, boundParameters[i]);
        }

        return ps.executeUpdate();
    }

    public static void executeSql(Connection conn, String sql) throws SQLException {
        @Cleanup val stmt = conn.prepareStatement(sql);
        stmt.executeUpdate();
    }
}
