package com.rjhch.dp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HRegionInfo;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.security.UserGroupInformation;

import java.util.List;

/**
 * 测试hbase表自动处理，包含合并和拆分
 */
public class TestHbaseTableAutoProcess {
    private static final Log log = LogFactory.getLog(TestHbaseTableAutoProcess.class);

    private int table_split_times = 5;
    private int size_least = 1073741824;
    private String[] no_check_tables = {"SYSTEM.CATALOG", "SYSTEM.FUNCTION", "SYSTEM.MUTEX", "SYSTEM.SEQUENCE", "SYSTEM.STATS"};

    private static FileSystem hdfs;
    private static Admin admin;

    /**
     * 是否需要检测的表
     *
     * @param tableName
     * @return
     */
    private boolean isNeedCheckTable(TableName tableName) throws Exception {
        if (tableName.getNamespaceAsString() != "default") {        //default命名空间的才需要检测
            return false;
        }

        for (String no_check_table : no_check_tables) {             //一些表不需要检测，节约时间
            if (tableName.getNameAsString().equals(no_check_table)) {
                return false;
            }
        }

        Path path_table = new Path("/hbase/data/default/" + tableName.getNameAsString());
        long length_path_table = hdfs.getContentSummary(path_table).getLength();
        if (length_path_table < size_least) {           //大小超过1G的表才需要检测
            return false;
        }

        return true;
    }

    /**
     * 是否需要检测的region
     *
     * @param regionInfo
     * @return
     */
    private boolean isNeedCheckRegion(TableName tableName, HRegionInfo regionInfo) throws Exception {

        Path path_region = new Path("/hbase/data/default/" + tableName.getNameAsString() + "/" + regionInfo.getShortNameToLog());
        long length_path_table = hdfs.getContentSummary(path_region).getLength();
        if (length_path_table < size_least) {           //大小超过1G的表才需要检测
            return false;
        }
        return true;
    }

    private void hbaseTableAutoProcess() throws Exception {
        if (UserGroupInformation.isSecurityEnabled() && UserGroupInformation.isLoginKeytabBased()) {
            String keytabFile;
            if (System.getenv("os.name").startsWith("Windows")) {
                keytabFile = "D:\\keytab\\hadoop.keytab";
            } else {
                keytabFile = "/etc/security/keytab/hadoop.keytab";
            }
            UserGroupInformation.loginUserFromKeytab("hadoop/hadoop01@HADOOP.COM", keytabFile);
        }

        Configuration config = HBaseConfiguration.create();
        Connection con = ConnectionFactory.createConnection(config);

        admin = con.getAdmin();
        hdfs = FileSystem.get(config);

        TableName[] tableNames = admin.listTableNames();
        for (TableName tableName : tableNames) {

            log.info("tableName: " + tableName);
            List<HRegionInfo> hRegionInfos = admin.getTableRegions(tableName);
            for (HRegionInfo hRegionInfo : hRegionInfos) {
                log.info("hRegionInfo: " + hRegionInfo);
                log.info("region name: " + hRegionInfo.getRegionNameAsString());
                log.info("region short name: " + hRegionInfo.getShortNameToLog());

                Path path = new Path("/hbase/data/default/" + tableName.getNameAsString() + "/" + hRegionInfo.getShortNameToLog());
                long len = hdfs.getContentSummary(path).getLength();
                log.info("region length: " + len);
                //hRegionInfo.get
                admin.splitRegion(hRegionInfo.getRegionName());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        new TestHbaseTableAutoProcess().hbaseTableAutoProcess();
    }
}
