package com.github.bingoohuang.sqlfilter;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;

public class Utils {
    @SneakyThrows
    public static Connection getH2Connection() {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:./src/test/resources/test", "sa", "");
    }

    @SneakyThrows
    public static int executeUpdate(PreparedStatement ps, Object... args) {
        for (int i = 0; i < args.length; ++i) {
            ps.setObject(i + 1, args[i]);
        }

        return ps.executeUpdate();
    }

    @SneakyThrows
    public static int executeUpdate(Connection conn, String sql, Object... args) {
        @Cleanup val ps = conn.prepareStatement(sql);
        return executeUpdate(ps, args);
    }
}
