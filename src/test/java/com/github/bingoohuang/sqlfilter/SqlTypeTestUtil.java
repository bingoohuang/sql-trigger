package com.github.bingoohuang.sqlfilter;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.val;
import org.n3r.eql.util.Rs;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

public class SqlTypeTestUtil {
    @SneakyThrows
    public static Connection getH2Connection() {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection("jdbc:h2:./src/test/resources/test", "sa", "");
    }


    @SneakyThrows
    private static void bindArgs(PreparedStatement ps, Object[] args) {
        for (int i = 0; i < args.length; ++i) {
            ps.setObject(i + 1, args[i]);
        }
    }

    @SneakyThrows
    public static int executeUpdate(PreparedStatement ps, Object... args) {
        bindArgs(ps, args);
        return ps.executeUpdate();
    }


    @SneakyThrows
    public static int executeUpdate(Connection conn, String sql, Object... args) {
        @Cleanup val ps = conn.prepareStatement(sql);
        return executeUpdate(ps, args);
    }


    @SneakyThrows
    public static List<Map<String, Object>> executeSelect(Connection conn, String sql, Object... args) {
        @Cleanup val ps = conn.prepareStatement(sql);
        bindArgs(ps, args);

        @Cleanup val rs = ps.executeQuery();

        int columnCount = rs.getMetaData().getColumnCount();

        List<Map<String, Object>> list = Lists.newArrayList();
        while (rs.next()) {
            Map<String, Object> row = Maps.newHashMap();
            list.add(row);

            for (int i = 1; i <= columnCount; ++i) {
                row.put(Rs.lookupColumnName(rs.getMetaData(), i), rs.getObject(i));
            }
        }

        return list;
    }
}
