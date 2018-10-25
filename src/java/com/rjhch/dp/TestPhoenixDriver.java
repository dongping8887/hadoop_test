package com.rjhch.dp;

import java.sql.*;

public class TestPhoenixDriver {

    public static void main(String[] args) throws Exception {
        Statement stmt = null;
        ResultSet rs = null;

        Class.forName("org.apache.phoenix.jdbc.PhoenixDriver");
        Connection conn = DriverManager.getConnection("jdbc:phoenix:hadoop01,hadoop02,hadoop03:2181");

        /* 在Phoenix中，如果table name/view name、column name等字符串不加上双引号就会被认为是大写。所以，这里的brand_name要加上双引号  */
        PreparedStatement pstmt = conn.prepareStatement("select * from EV_RPY_TRANS_FLOW;");
        rs = pstmt.executeQuery();

        StringBuffer sb = new StringBuffer();

        while (rs.next()) {
            System.err.println(rs.getString("pk")+"---"+rs.getString("BATCH_DATE")+"---"+rs.getString("ACCEPT_ORG_CODE"));
            //System.err.println("\n=========================================================");
        }
        /* 关闭资源*/
        rs.close();
        pstmt.close();
    }
}
