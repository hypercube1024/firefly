package com.firefly.example.reactive.coffee.store.utils;

import com.firefly.annotation.Component;
import com.firefly.annotation.Inject;
import com.firefly.utils.io.Resource;
import com.firefly.utils.log.slf4j.ext.LazyLogger;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;
import java.util.stream.Collectors;

import static com.firefly.db.init.ScriptUtils.executeSqlScript;

/**
 * @author Pengtao Qiu
 */
@Component
public class DBUtils {

    private static LazyLogger logger = LazyLogger.create();

    @Inject
    private DataSource dataSource;

    @Inject
    private ResourceUtils resourceUtils;

    public void createTables() {
        executeScript(getSchemaScript());
    }

    public void initializeData() {
        executeScript(getInitDataScript());
    }

    public void executeScript(Resource resource) {
        try (Connection connection = dataSource.getConnection()) {
            executeSqlScript(connection, resource);
        } catch (Exception e) {
            logger.error(() -> "execute SQL exception", e);
        }
    }


    public Resource getSchemaScript() {
        return resourceUtils.resource("/dbScript/coffee_store_schema.sql");
    }

    public Resource getInitDataScript() {
        return resourceUtils.resource("/dbScript/coffee_store_init_data.sql");
    }

    public static String toWildcard(List<?> list) {
        return list.parallelStream().map(o -> "?").collect(Collectors.joining(","));
    }

}
