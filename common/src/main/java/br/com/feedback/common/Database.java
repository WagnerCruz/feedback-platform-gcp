package br.com.feedback.common;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Pool de conexões com o PostgreSQL (Cloud SQL).
 * <p>
 * Em produção usa o Cloud SQL Java Connector (TLS + IAM, sem expor IP): basta definir
 * INSTANCE_CONNECTION_NAME, DB_NAME, DB_USER e DB_PASSWORD. Para desenvolvimento local,
 * defina DB_URL com uma URL JDBC completa.
 */
public final class Database {
    private static HikariDataSource dataSource;

    private Database() {
    }

    public static synchronized DataSource get() {
        if (dataSource == null) {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(urlJdbc());
            cfg.setUsername(Config.obrigatoria("DB_USER"));
            cfg.setPassword(Config.obrigatoria("DB_PASSWORD"));
            // Funções serverless: pool minúsculo para não esgotar conexões do Cloud SQL.
            cfg.setMaximumPoolSize(2);
            cfg.setMinimumIdle(0);
            cfg.setConnectionTimeout(10_000);
            cfg.setIdleTimeout(60_000);
            dataSource = new HikariDataSource(cfg);
        }
        return dataSource;
    }

    private static String urlJdbc() {
        String url = Config.opcional("DB_URL");
        if (url != null) {
            return url;
        }
        return "jdbc:postgresql:///" + Config.obrigatoria("DB_NAME")
                + "?cloudSqlInstance=" + Config.obrigatoria("INSTANCE_CONNECTION_NAME")
                + "&socketFactory=com.google.cloud.sql.postgres.SocketFactory";
    }
}
