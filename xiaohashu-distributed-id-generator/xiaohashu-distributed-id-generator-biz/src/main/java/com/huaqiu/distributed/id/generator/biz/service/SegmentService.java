package com.huaqiu.distributed.id.generator.biz.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.huaqiu.distributed.id.generator.biz.constant.Constants;
import com.huaqiu.distributed.id.generator.biz.core.IDGen;
import com.huaqiu.distributed.id.generator.biz.core.common.PropertyFactory;
import com.huaqiu.distributed.id.generator.biz.core.common.Result;
import com.huaqiu.distributed.id.generator.biz.core.segment.SegmentIDGenImpl;
import com.huaqiu.distributed.id.generator.biz.core.segment.dao.IDAllocDao;
import com.huaqiu.distributed.id.generator.biz.core.segment.dao.impl.IDAllocDaoImpl;
import com.huaqiu.distributed.id.generator.biz.exception.InitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Properties;

@Service("SegmentService")
public class SegmentService {
    private final Logger logger = LoggerFactory.getLogger(SegmentService.class);

    private final IDGen idGen;

    /**
     * 生产环境通过 LEAF_JDBC_URL / LEAF_JDBC_USERNAME / LEAF_JDBC_PASSWORD 环境变量注入；
     * 为空时回退到 classpath 下的 leaf.properties，保证本地 dev 无需改动。
     */
    public SegmentService(@Value("${leaf.jdbc.url:}") String url,
                          @Value("${leaf.jdbc.username:}") String username,
                          @Value("${leaf.jdbc.password:}") String password) throws SQLException, InitException {
        // 逐项回退：环境变量为空时才用 leaf.properties，避免某个字段被整体覆盖
        Properties properties = PropertyFactory.getProperties();
        if (url == null || url.isEmpty()) {
            url = properties.getProperty(Constants.LEAF_JDBC_URL);
        }
        if (username == null || username.isEmpty()) {
            username = properties.getProperty(Constants.LEAF_JDBC_USERNAME);
        }
        if (password == null || password.isEmpty()) {
            password = properties.getProperty(Constants.LEAF_JDBC_PASSWORD);
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.init();

        IDAllocDao dao = new IDAllocDaoImpl(dataSource);
        SegmentIDGenImpl segmentIDGen = new SegmentIDGenImpl();
        segmentIDGen.setDao(dao);
        if (!segmentIDGen.init()) {
            throw new InitException("Segment Service Init Fail");
        }

        this.idGen = segmentIDGen;
        logger.info("Segment Service Init Successfully");
    }

    public Result getId(String key) {
        return idGen.get(key);
    }

    public SegmentIDGenImpl getIdGen() {
        if (idGen instanceof SegmentIDGenImpl) {
            return (SegmentIDGenImpl) idGen;
        }
        return null;
    }
}
