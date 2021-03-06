/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.mysql;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;
import org.apache.kafka.common.config.ConfigDef.Width;

import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.config.Field.Recommender;
import io.debezium.config.Field.ValidationOutput;
import io.debezium.jdbc.JdbcConnection;
import io.debezium.jdbc.JdbcValueConverters.DecimalMode;
import io.debezium.relational.TableId;
import io.debezium.relational.history.DatabaseHistory;
import io.debezium.relational.history.KafkaDatabaseHistory;

/**
 * The configuration properties.
 */
public class MySqlConnectorConfig {

    /**
     * The set of predefined TemporalPrecisionMode options or aliases.
     */
    public static enum TemporalPrecisionMode {
        /**
         * Represent time and date values based upon the resolution in the database, using {@link io.debezium.time} semantic
         * types.
         */
        ADAPTIVE("adaptive"),

        /**
         * Represent time and date values using Kafka Connect {@link org.apache.kafka.connect.data} logical types, which always
         * have millisecond precision.
         */
        CONNECT("connect");

        private final String value;

        private TemporalPrecisionMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @return the matching option, or null if no match is found
         */
        public static TemporalPrecisionMode parse(String value) {
            if (value == null) return null;
            value = value.trim();
            for (TemporalPrecisionMode option : TemporalPrecisionMode.values()) {
                if (option.getValue().equalsIgnoreCase(value)) return option;
            }
            return null;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @param defaultValue the default value; may be null
         * @return the matching option, or null if no match is found and the non-null default is invalid
         */
        public static TemporalPrecisionMode parse(String value, String defaultValue) {
            TemporalPrecisionMode mode = parse(value);
            if (mode == null && defaultValue != null) mode = parse(defaultValue);
            return mode;
        }
    }

    /**
     * The set of predefined DecimalHandlingMode options or aliases.
     */
    public static enum DecimalHandlingMode {
        /**
         * Represent {@code DECIMAL} and {@code NUMERIC} values as precise {@link BigDecimal} values, which are
         * represented in change events in a binary form. This is precise but difficult to use.
         */
        PRECISE("precise"),

        /**
         * Represent {@code DECIMAL} and {@code NUMERIC} values as precise {@code double} values. This may be less precise
         * but is far easier to use.
         */
        DOUBLE("double");

        private final String value;

        private DecimalHandlingMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public DecimalMode asDecimalMode() {
            switch (this) {
                case DOUBLE:
                    return DecimalMode.DOUBLE;
                case PRECISE:
                default:
                    return DecimalMode.PRECISE;
            }
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @return the matching option, or null if no match is found
         */
        public static DecimalHandlingMode parse(String value) {
            if (value == null) return null;
            value = value.trim();
            for (DecimalHandlingMode option : DecimalHandlingMode.values()) {
                if (option.getValue().equalsIgnoreCase(value)) return option;
            }
            return null;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @param defaultValue the default value; may be null
         * @return the matching option, or null if no match is found and the non-null default is invalid
         */
        public static DecimalHandlingMode parse(String value, String defaultValue) {
            DecimalHandlingMode mode = parse(value);
            if (mode == null && defaultValue != null) mode = parse(defaultValue);
            return mode;
        }
    }

    /**
     * The set of predefined SnapshotMode options or aliases.
     */
    public static enum SnapshotMode {

        /**
         * Perform a snapshot when it is needed.
         */
        WHEN_NEEDED("when_needed"),

        /**
         * Perform a snapshot only upon initial startup of a connector.
         */
        INITIAL("initial"),

        /**
         * Perform a snapshot of only the database schemas (without data) and then begin reading the binlog.
         * This should be used with care, but it is very useful when the change event consumers need only the changes
         * from the point in time the snapshot is made (and doesn't care about any state or changes prior to this point).
         */
        SCHEMA_ONLY("schema_only"),

        /**
         * Never perform a snapshot and only read the binlog. This assumes the binlog contains all the history of those
         * databases and tables that will be captured.
         */
        NEVER("never"),

        /**
         * Perform a snapshot and then stop before attempting to read the binlog.
         */
        INITIAL_ONLY("initial_only");

        private final String value;

        private SnapshotMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @return the matching option, or null if no match is found
         */
        public static SnapshotMode parse(String value) {
            if (value == null) return null;
            value = value.trim();
            for (SnapshotMode option : SnapshotMode.values()) {
                if (option.getValue().equalsIgnoreCase(value)) return option;
            }
            return null;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @param defaultValue the default value; may be null
         * @return the matching option, or null if no match is found and the non-null default is invalid
         */
        public static SnapshotMode parse(String value, String defaultValue) {
            SnapshotMode mode = parse(value);
            if (mode == null && defaultValue != null) mode = parse(defaultValue);
            return mode;
        }
    }

    /**
     * The set of predefined SecureConnectionMode options or aliases.
     */
    public static enum SecureConnectionMode {
        /**
         * Establish an unencrypted connection.
         */
        DISABLED("disabled"),

        /**
         * Establish a secure (encrypted) connection if the server supports secure connections.
         * Fall back to an unencrypted connection otherwise.
         */
        PREFERRED("preferred"),
        /**
         * Establish a secure connection if the server supports secure connections.
         * The connection attempt fails if a secure connection cannot be established.
         */
        REQUIRED("required"),
        /**
         * Like REQUIRED, but additionally verify the server TLS certificate against the configured Certificate Authority
         * (CA) certificates. The connection attempt fails if no valid matching CA certificates are found.
         */
        VERIFY_CA("verify_ca"),
        /**
         * Like VERIFY_CA, but additionally verify that the server certificate matches the host to which the connection is
         * attempted.
         */
        VERIFY_IDENTITY("verify_identity");

        private final String value;

        private SecureConnectionMode(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @return the matching option, or null if no match is found
         */
        public static SecureConnectionMode parse(String value) {
            if (value == null) return null;
            value = value.trim();
            for (SecureConnectionMode option : SecureConnectionMode.values()) {
                if (option.getValue().equalsIgnoreCase(value)) return option;
            }
            return null;
        }

        /**
         * Determine if the supplied value is one of the predefined options.
         * 
         * @param value the configuration property value; may not be null
         * @param defaultValue the default value; may be null
         * @return the matching option, or null if no match is found and the non-null default is invalid
         */
        public static SecureConnectionMode parse(String value, String defaultValue) {
            SecureConnectionMode mode = parse(value);
            if (mode == null && defaultValue != null) mode = parse(defaultValue);
            return mode;
        }
    }

    private static final String DATABASE_WHITELIST_NAME = "database.whitelist";
    private static final String TABLE_WHITELIST_NAME = "table.whitelist";
    private static final String TABLE_IGNORE_BUILTIN_NAME = "table.ignore.builtin";
    private static final TableRecommender TABLE_LIST_RECOMMENDER = new TableRecommender();
    private static final DatabaseRecommender DATABASE_LIST_RECOMMENDER = new DatabaseRecommender();

    public static final Field HOSTNAME = Field.create("database.hostname")
                                              .withDisplayName("Hostname")
                                              .withType(Type.STRING)
                                              .withWidth(Width.MEDIUM)
                                              .withImportance(Importance.HIGH)
                                              .withValidation(Field::isRequired)
                                              .withDescription("Resolvable hostname or IP address of the MySQL database server.");

    public static final Field PORT = Field.create("database.port")
                                          .withDisplayName("Port")
                                          .withType(Type.INT)
                                          .withWidth(Width.SHORT)
                                          .withDefault(3306)
                                          .withImportance(Importance.HIGH)
                                          .withValidation(Field::isInteger)
                                          .withDescription("Port of the MySQL database server.");

    public static final Field USER = Field.create("database.user")
                                          .withDisplayName("User")
                                          .withType(Type.STRING)
                                          .withWidth(Width.SHORT)
                                          .withImportance(Importance.HIGH)
                                          .withValidation(Field::isRequired)
                                          .withDescription("Name of the MySQL database user to be used when connecting to the database.");

    public static final Field PASSWORD = Field.create("database.password")
                                              .withDisplayName("Password")
                                              .withType(Type.PASSWORD)
                                              .withWidth(Width.SHORT)
                                              .withImportance(Importance.HIGH)
                                              .withValidation(Field::isRequired)
                                              .withDescription("Password of the MySQL database user to be used when connecting to the database.");

    public static final Field SERVER_NAME = Field.create("database.server.name")
                                                 .withDisplayName("Namespace")
                                                 .withType(Type.STRING)
                                                 .withWidth(Width.MEDIUM)
                                                 .withImportance(Importance.HIGH)
                                                 .withValidation(Field::isRequired)
                                                 .withDescription("Unique name that identifies the database server and all recorded offsets, and"
                                                         + "that is used as a prefix for all schemas and topics. "
                                                         + "Each distinct MySQL installation should have a separate namespace and monitored by "
                                                         + "at most one Debezium connector. Defaults to 'host:port'");

    public static final Field SERVER_ID = Field.create("database.server.id")
                                               .withDisplayName("Cluster ID")
                                               .withType(Type.LONG)
                                               .withWidth(Width.LONG)
                                               .withImportance(Importance.HIGH)
                                               .withDefault(MySqlConnectorConfig::randomServerId)
                                               .withValidation(Field::isRequired, Field::isPositiveLong)
                                               .withDescription("A numeric ID of this database client, which must be unique across all "
                                                       + "currently-running database processes in the cluster. This connector joins the "
                                                       + "MySQL database cluster as another server (with this unique ID) so it can read "
                                                       + "the binlog. By default, a random number is generated between 5400 and 6400.");

    public static final Field SSL_MODE = Field.create("database.ssl.mode")
                                              .withDisplayName("SSL mode")
                                              .withEnum(SecureConnectionMode.class, SecureConnectionMode.DISABLED)
                                              .withWidth(Width.MEDIUM)
                                              .withImportance(Importance.MEDIUM)
                                              .withDescription("Whether to use an encrypted connection to MySQL. Options include"
                                                      + "'disabled' (the default) to use an unencrypted connection; "
                                                      + "'preferred' to establish a secure (encrypted) connection if the server supports secure connections, "
                                                      + "but fall back to an unencrypted connection otherwise; "
                                                      + "'required' to use a secure (encrypted) connection, and fail if one cannot be established; "
                                                      + "'verify_ca' like 'required' but additionally verify the server TLS certificate against the configured Certificate Authority "
                                                      + "(CA) certificates, or fail if no valid matching CA certificates are found; or"
                                                      + "'verify_identity' like 'verify_ca' but additionally verify that the server certificate matches the host to which the connection is attempted.");

    public static final Field SSL_KEYSTORE = Field.create("database.ssl.keystore")
                                                  .withDisplayName("SSL Keystore")
                                                  .withType(Type.STRING)
                                                  .withWidth(Width.LONG)
                                                  .withImportance(Importance.MEDIUM)
                                                  .withDescription("Location of the Java keystore file containing an application process's own certificate and private key.");

    public static final Field SSL_KEYSTORE_PASSWORD = Field.create("database.ssl.keystore.password")
                                                           .withDisplayName("SSL Keystore Password")
                                                           .withType(Type.PASSWORD)
                                                           .withWidth(Width.MEDIUM)
                                                           .withImportance(Importance.MEDIUM)
                                                           .withDescription("Password to access the private key from the keystore file specified by 'ssl.keystore' configuration property or the 'javax.net.ssl.keyStore' system or JVM property. "
                                                                   + "This password is used to unlock the keystore file (store password), and to decrypt the private key stored in the keystore (key password).");

    public static final Field SSL_TRUSTSTORE = Field.create("database.ssl.truststore")
                                                    .withDisplayName("SSL Truststore")
                                                    .withType(Type.STRING)
                                                    .withWidth(Width.LONG)
                                                    .withImportance(Importance.MEDIUM)
                                                    .withDescription("Location of the Java truststore file containing the collection of CA certificates trusted by this application process (trust store).");

    public static final Field SSL_TRUSTSTORE_PASSWORD = Field.create("database.ssl.truststore.password")
                                                             .withDisplayName("SSL Truststore Password")
                                                             .withType(Type.PASSWORD)
                                                             .withWidth(Width.MEDIUM)
                                                             .withImportance(Importance.MEDIUM)
                                                             .withDescription("Password to unlock the keystore file (store password) specified by 'ssl.trustore' configuration property or the 'javax.net.ssl.trustStore' system or JVM property.");

    public static final Field TABLES_IGNORE_BUILTIN = Field.create(TABLE_IGNORE_BUILTIN_NAME)
                                                           .withDisplayName("Ignore system databases")
                                                           .withType(Type.BOOLEAN)
                                                           .withWidth(Width.SHORT)
                                                           .withImportance(Importance.LOW)
                                                           .withDefault(true)
                                                           .withValidation(Field::isBoolean)
                                                           .withDependents(DATABASE_WHITELIST_NAME)
                                                           .withDescription("Flag specifying whether built-in tables should be ignored.");

    /**
     * A comma-separated list of regular expressions that match database names to be monitored.
     * May not be used with {@link #DATABASE_BLACKLIST}.
     */
    public static final Field DATABASE_WHITELIST = Field.create(DATABASE_WHITELIST_NAME)
                                                        .withDisplayName("Databases")
                                                        .withType(Type.LIST)
                                                        .withWidth(Width.LONG)
                                                        .withImportance(Importance.HIGH)
                                                        .withRecommender(DATABASE_LIST_RECOMMENDER)
                                                        .withDependents(TABLE_WHITELIST_NAME)
                                                        .withDescription("The databases for which changes are to be captured");

    /**
     * A comma-separated list of regular expressions that match database names to be excluded from monitoring.
     * May not be used with {@link #DATABASE_WHITELIST}.
     */
    public static final Field DATABASE_BLACKLIST = Field.create("database.blacklist")
                                                        .withDisplayName("Exclude Databases")
                                                        .withType(Type.STRING)
                                                        .withWidth(Width.LONG)
                                                        .withImportance(Importance.MEDIUM)
                                                        .withValidation(MySqlConnectorConfig::validateDatabaseBlacklist)
                                                        .withInvisibleRecommender()
                                                        .withDescription("");

    /**
     * A comma-separated list of regular expressions that match the fully-qualified names of tables to be monitored.
     * Fully-qualified names for tables are of the form {@code <databaseName>.<tableName>} or
     * {@code <databaseName>.<schemaName>.<tableName>}. May not be used with {@link #TABLE_BLACKLIST}, and superseded by database
     * inclusions/exclusions.
     */
    public static final Field TABLE_WHITELIST = Field.create(TABLE_WHITELIST_NAME)
                                                     .withDisplayName("Tables")
                                                     .withType(Type.LIST)
                                                     .withWidth(Width.LONG)
                                                     .withImportance(Importance.HIGH)
                                                     .withValidation(Field::isListOfRegex)
                                                     .withRecommender(TABLE_LIST_RECOMMENDER)
                                                     .withDescription("The tables for which changes are to be captured");

    /**
     * A comma-separated list of regular expressions that match the fully-qualified names of tables to be excluded from
     * monitoring. Fully-qualified names for tables are of the form {@code <databaseName>.<tableName>} or
     * {@code <databaseName>.<schemaName>.<tableName>}. May not be used with {@link #TABLE_WHITELIST}.
     */
    public static final Field TABLE_BLACKLIST = Field.create("table.blacklist")
                                                     .withDisplayName("Exclude Tables")
                                                     .withType(Type.STRING)
                                                     .withWidth(Width.LONG)
                                                     .withImportance(Importance.MEDIUM)
                                                     .withValidation(Field::isListOfRegex, MySqlConnectorConfig::validateTableBlacklist)
                                                     .withInvisibleRecommender();

    /**
     * A comma-separated list of regular expressions that match fully-qualified names of columns to be excluded from monitoring
     * and change messages. Fully-qualified names for columns are of the form {@code <databaseName>.<tableName>.<columnName>}
     * or {@code <databaseName>.<schemaName>.<tableName>.<columnName>}.
     */
    public static final Field COLUMN_BLACKLIST = Field.create("column.blacklist")
                                                      .withDisplayName("Exclude Columns")
                                                      .withType(Type.STRING)
                                                      .withWidth(Width.LONG)
                                                      .withImportance(Importance.MEDIUM)
                                                      .withValidation(MySqlConnectorConfig::validateColumnBlacklist)
                                                      .withDescription("");

    /**
     * A comma-separated list of regular expressions that match source UUIDs in the GTID set used to find the binlog
     * position in the MySQL server. Only the GTID ranges that have sources matching one of these include patterns will
     * be used.
     * May not be used with {@link #GTID_SOURCE_EXCLUDES}.
     */
    public static final Field GTID_SOURCE_INCLUDES = Field.create("gtid.source.includes")
                                                          .withDisplayName("Include GTID sources")
                                                          .withType(Type.LIST)
                                                          .withWidth(Width.LONG)
                                                          .withImportance(Importance.HIGH)
                                                          .withRecommender(DATABASE_LIST_RECOMMENDER)
                                                          .withDependents(TABLE_WHITELIST_NAME)
                                                          .withDescription("The source UUIDs used to include GTID ranges when determine the starting position in the MySQL server's binlog.");

    /**
     * A comma-separated list of regular expressions that match source UUIDs in the GTID set used to find the binlog
     * position in the MySQL server. Only the GTID ranges that have sources matching none of these exclude patterns will
     * be used.
     * May not be used with {@link #GTID_SOURCE_INCLUDES}.
     */
    public static final Field GTID_SOURCE_EXCLUDES = Field.create("gtid.source.excludes")
                                                          .withDisplayName("Exclude GTID sources")
                                                          .withType(Type.STRING)
                                                          .withWidth(Width.LONG)
                                                          .withImportance(Importance.MEDIUM)
                                                          .withValidation(MySqlConnectorConfig::validateGtidSetExcludes)
                                                          .withInvisibleRecommender()
                                                          .withDescription("The source UUIDs used to exclude GTID ranges when determine the starting position in the MySQL server's binlog.");

    public static final Field CONNECTION_TIMEOUT_MS = Field.create("connect.timeout.ms")
                                                           .withDisplayName("Connection Timeout (ms)")
                                                           .withType(Type.INT)
                                                           .withWidth(Width.SHORT)
                                                           .withImportance(Importance.MEDIUM)
                                                           .withDescription("Maximum time in milliseconds to wait after trying to connect to the database before timing out.")
                                                           .withDefault(30 * 1000)
                                                           .withValidation(Field::isPositiveInteger);

    public static final Field KEEP_ALIVE = Field.create("connect.keep.alive")
                                                .withDisplayName("Connection Timeout (ms)")
                                                .withType(Type.BOOLEAN)
                                                .withWidth(Width.SHORT)
                                                .withImportance(Importance.LOW)
                                                .withDescription("Whether a separate thread should be used to ensure the connection is kept alive.")
                                                .withDefault(true)
                                                .withValidation(Field::isBoolean);

    public static final Field MAX_QUEUE_SIZE = Field.create("max.queue.size")
                                                    .withDisplayName("Change event buffer size")
                                                    .withType(Type.INT)
                                                    .withWidth(Width.SHORT)
                                                    .withImportance(Importance.MEDIUM)
                                                    .withDescription("Maximum size of the queue for change events read from the database log but not yet recorded or forwarded. Defaults to 2048, and should always be larger than the maximum batch size.")
                                                    .withDefault(2048)
                                                    .withValidation(MySqlConnectorConfig::validateMaxQueueSize);

    public static final Field MAX_BATCH_SIZE = Field.create("max.batch.size")
                                                    .withDisplayName("Change event batch size")
                                                    .withType(Type.INT)
                                                    .withWidth(Width.SHORT)
                                                    .withImportance(Importance.MEDIUM)
                                                    .withDescription("Maximum size of each batch of source records. Defaults to 1024.")
                                                    .withDefault(1024)
                                                    .withValidation(Field::isPositiveInteger);

    public static final Field POLL_INTERVAL_MS = Field.create("poll.interval.ms")
                                                      .withDisplayName("Poll interval (ms)")
                                                      .withType(Type.LONG)
                                                      .withWidth(Width.SHORT)
                                                      .withImportance(Importance.MEDIUM)
                                                      .withDescription("Frequency in milliseconds to wait for new change events to appear after receiving no events. Defaults to 1 second (1000 ms).")
                                                      .withDefault(TimeUnit.SECONDS.toMillis(1))
                                                      .withValidation(Field::isPositiveInteger);

    public static final Field ROW_COUNT_FOR_STREAMING_RESULT_SETS = Field.create("min.row.count.to.stream.results")
                                                                         .withDisplayName("Stream result set of size")
                                                                         .withType(Type.LONG)
                                                                         .withWidth(Width.MEDIUM)
                                                                         .withImportance(Importance.LOW)
                                                                         .withDescription("The number of rows a table must contain to stream results rather than pull "
                                                                                 + "all into memory during snapshots. Defaults to 1,000. Use 0 to stream all results "
                                                                                 + "and completely avoid checking the size of each table.")
                                                                         .withDefault(1_000)
                                                                         .withValidation(Field::isNonNegativeLong);

    /**
     * The database history class is hidden in the {@link #configDef()} since that is designed to work with a user interface,
     * and in these situations using Kafka is the only way to go.
     */
    public static final Field DATABASE_HISTORY = Field.create("database.history")
                                                      .withDisplayName("Database history class")
                                                      .withType(Type.CLASS)
                                                      .withWidth(Width.LONG)
                                                      .withImportance(Importance.LOW)
                                                      .withInvisibleRecommender()
                                                      .withDescription("The name of the DatabaseHistory class that should be used to store and recover database schema changes. "
                                                              + "The configuration properties for the history are prefixed with the '"
                                                              + DatabaseHistory.CONFIGURATION_FIELD_PREFIX_STRING + "' string.")
                                                      .withDefault(KafkaDatabaseHistory.class.getName());

    public static final Field INCLUDE_SCHEMA_CHANGES = Field.create("include.schema.changes")
                                                            .withDisplayName("Include database schema changes")
                                                            .withType(Type.BOOLEAN)
                                                            .withWidth(Width.SHORT)
                                                            .withImportance(Importance.MEDIUM)
                                                            .withDescription("Whether the connector should publish changes in the database schema to a Kafka topic with "
                                                                    + "the same name as the database server ID. Each schema change will be recorded using a key that "
                                                                    + "contains the database name and whose value includes the DDL statement(s)."
                                                                    + "The default is 'true'. This is independent of how the connector internally records database history.")
                                                            .withDefault(true);

    public static final Field SNAPSHOT_MODE = Field.create("snapshot.mode")
                                                   .withDisplayName("Snapshot mode")
                                                   .withEnum(SnapshotMode.class, SnapshotMode.INITIAL)
                                                   .withWidth(Width.SHORT)
                                                   .withImportance(Importance.LOW)
                                                   .withDescription("The criteria for running a snapshot upon startup of the connector. "
                                                           + "Options include: "
                                                           + "'when_needed' to specify that the connector run a snapshot upon startup whenever it deems it necessary; "
                                                           + "'initial' (the default) to specify the connector can run a snapshot only when no offsets are available for the logical server name; "
                                                           + "'initial_only' same as 'initial' except the connector should stop after completing the snapshot and before it would normally read the binlog; and"
                                                           + "'never' to specify the connector should never run a snapshot and that upon first startup the connector should read from the beginning of the binlog. "
                                                           + "The 'never' mode should be used with care, and only when the binlog is known to contain all history.");

    public static final Field SNAPSHOT_MINIMAL_LOCKING = Field.create("snapshot.minimal.locks")
                                                              .withDisplayName("Use shortest database locking for snapshots")
                                                              .withType(Type.BOOLEAN)
                                                              .withWidth(Width.SHORT)
                                                              .withImportance(Importance.LOW)
                                                              .withDescription("Controls how long the connector holds onto the global read lock while it is performing a snapshot. The default is 'true', "
                                                                      + "which means the connector holds the global read lock (and thus prevents any updates) for just the initial portion of the snapshot "
                                                                      + "while the database schemas and other metadata are being read. The remaining work in a snapshot involves selecting all rows from "
                                                                      + "each table, and this can be done using the snapshot process' REPEATABLE READ transaction even when the lock is no longer held and "
                                                                      + "other operations are updating the database. However, in some cases it may be desirable to block all writes for the entire duration "
                                                                      + "of the snapshot; in such cases set this property to 'false'.")
                                                              .withDefault(true);

    public static final Field TIME_PRECISION_MODE = Field.create("time.precision.mode")
                                                         .withDisplayName("Time Precision")
                                                         .withEnum(TemporalPrecisionMode.class, TemporalPrecisionMode.ADAPTIVE)
                                                         .withWidth(Width.SHORT)
                                                         .withImportance(Importance.MEDIUM)
                                                         .withDescription("Time, date, and timestamps can be represented with different kinds of precisions, including:"
                                                                 + "'adaptive' (the default) bases the precision of time, date, and timestamp values on the database column's precision; "
                                                                 + "'connect' always represents time, date, and timestamp values using Kafka Connect's built-in representations for Time, Date, and Timestamp, "
                                                                 + "which uses millisecond precision regardless of the database columns' precision .");

    public static final Field DECIMAL_HANDLING_MODE = Field.create("decimal.handling.mode")
                                                           .withDisplayName("Decimal Handling")
                                                           .withEnum(DecimalHandlingMode.class, DecimalHandlingMode.PRECISE)
                                                           .withWidth(Width.SHORT)
                                                           .withImportance(Importance.MEDIUM)
                                                           .withDescription("Specify how DECIMAL and NUMERIC columns should be represented in change events, including:"
                                                                   + "'precise' (the default) uses java.math.BigDecimal to represent values, which are encoded in the change events using a binary representation and Kafka Connect's 'org.apache.kafka.connect.data.Decimal' type; "
                                                                   + "'double' represents values using Java's 'double', which may not offer the precision but will be far easier to use in consumers.");

    /**
     * Method that generates a Field for specifying that string columns whose names match a set of regular expressions should
     * have their values truncated to be no longer than the specified number of characters.
     * 
     * @param length the maximum length of the column's string values written in source records; must be positive
     * @return the field; never null
     */
    public static final Field TRUNCATE_COLUMN(int length) {
        if (length <= 0) throw new IllegalArgumentException("The truncation length must be positive");
        return Field.create("column.truncate.to." + length + ".chars")
                    .withValidation(Field::isInteger)
                    .withDescription("A comma-separated list of regular expressions matching fully-qualified names of columns that should "
                            + "be truncated to " + length + " characters.");
    }

    /**
     * Method that generates a Field for specifying that string columns whose names match a set of regular expressions should
     * have their values masked by the specified number of asterisk ('*') characters.
     * 
     * @param length the number of asterisks that should appear in place of the column's string values written in source records;
     *            must be positive
     * @return the field; never null
     */
    public static final Field MASK_COLUMN(int length) {
        if (length <= 0) throw new IllegalArgumentException("The mask length must be positive");
        return Field.create("column.mask.with." + length + ".chars")
                    .withValidation(Field::isInteger)
                    .withDescription("A comma-separated list of regular expressions matching fully-qualified names of columns that should "
                            + "be masked with " + length + " asterisk ('*') characters.");
    }

    /**
     * The set of {@link Field}s defined as part of this configuration.
     */
    public static Field.Set ALL_FIELDS = Field.setOf(USER, PASSWORD, HOSTNAME, PORT, SERVER_ID,
                                                     SERVER_NAME,
                                                     CONNECTION_TIMEOUT_MS, KEEP_ALIVE,
                                                     MAX_QUEUE_SIZE, MAX_BATCH_SIZE, POLL_INTERVAL_MS,
                                                     DATABASE_HISTORY, INCLUDE_SCHEMA_CHANGES,
                                                     TABLE_WHITELIST, TABLE_BLACKLIST, TABLES_IGNORE_BUILTIN,
                                                     DATABASE_WHITELIST, DATABASE_BLACKLIST,
                                                     COLUMN_BLACKLIST, SNAPSHOT_MODE, SNAPSHOT_MINIMAL_LOCKING,
                                                     GTID_SOURCE_INCLUDES, GTID_SOURCE_EXCLUDES,
                                                     TIME_PRECISION_MODE, DECIMAL_HANDLING_MODE,
                                                     SSL_MODE, SSL_KEYSTORE, SSL_KEYSTORE_PASSWORD,
                                                     SSL_TRUSTSTORE, SSL_TRUSTSTORE_PASSWORD);

    /**
     * The set of {@link Field}s that are included in the {@link #configDef() configuration definition}. This includes
     * all fields defined in this class (though some are always invisible since they are not to be exposed to the user interface)
     * plus several that are specific to the {@link KafkaDatabaseHistory} class, since history is always stored in Kafka
     * when run via the user interface.
     */
    protected static Field.Set EXPOSED_FIELDS = ALL_FIELDS.with(KafkaDatabaseHistory.BOOTSTRAP_SERVERS,
                                                                KafkaDatabaseHistory.TOPIC,
                                                                KafkaDatabaseHistory.RECOVERY_POLL_ATTEMPTS,
                                                                KafkaDatabaseHistory.RECOVERY_POLL_INTERVAL_MS);

    protected static ConfigDef configDef() {
        ConfigDef config = new ConfigDef();
        Field.group(config, "MySQL", HOSTNAME, PORT, USER, PASSWORD, SERVER_NAME, SERVER_ID,
                    SSL_MODE, SSL_KEYSTORE, SSL_KEYSTORE_PASSWORD, SSL_TRUSTSTORE, SSL_TRUSTSTORE_PASSWORD);
        Field.group(config, "History Storage", KafkaDatabaseHistory.BOOTSTRAP_SERVERS,
                    KafkaDatabaseHistory.TOPIC, KafkaDatabaseHistory.RECOVERY_POLL_ATTEMPTS,
                    KafkaDatabaseHistory.RECOVERY_POLL_INTERVAL_MS, DATABASE_HISTORY);
        Field.group(config, "Events", INCLUDE_SCHEMA_CHANGES, TABLES_IGNORE_BUILTIN, DATABASE_WHITELIST, TABLE_WHITELIST,
                    COLUMN_BLACKLIST, TABLE_BLACKLIST, DATABASE_BLACKLIST,
                    GTID_SOURCE_INCLUDES, GTID_SOURCE_EXCLUDES);
        Field.group(config, "Connector", CONNECTION_TIMEOUT_MS, KEEP_ALIVE, MAX_QUEUE_SIZE, MAX_BATCH_SIZE, POLL_INTERVAL_MS,
                    SNAPSHOT_MODE, SNAPSHOT_MINIMAL_LOCKING, TIME_PRECISION_MODE, DECIMAL_HANDLING_MODE);
        return config;
    }

    protected static class DatabaseRecommender implements Recommender {

        @Override
        public List<Object> validValues(Field field, Configuration config) {
            List<Object> databaseNames = new ArrayList<>();
            try (MySqlJdbcContext jdbcContext = new MySqlJdbcContext(config)) {
                JdbcConnection mysql = jdbcContext.jdbc();
                Set<String> dbNames = mysql.readAllCatalogNames();
                if (config.getBoolean(TABLES_IGNORE_BUILTIN)) {
                    Filters.withoutBuiltInDatabases(dbNames).forEach(databaseNames::add);
                } else {
                    dbNames.forEach(databaseNames::add);
                }
            } catch (SQLException e) {
                // don't do anything ...
            }
            return databaseNames;
        }

        @Override
        public boolean visible(Field field, Configuration config) {
            return true;
        }
    }

    protected static class TableRecommender implements Recommender {

        @Override
        public List<Object> validValues(Field field, Configuration config) {
            // Get the list of allowed databases ...
            Filters dbFilter = new Filters(config);

            List<Object> results = new ArrayList<>();
            try (MySqlJdbcContext jdbcContext = new MySqlJdbcContext(config)) {
                JdbcConnection mysql = jdbcContext.jdbc();
                String[] tableTypes = new String[] { "TABLE" }; // only show MySQL physical tables
                Collection<TableId> tableIds = mysql.readAllTableNames(tableTypes);
                if (config.getBoolean(TABLES_IGNORE_BUILTIN)) {
                    tableIds = Filters.withoutBuiltIns(tableIds);
                }
                tableIds.stream()
                        .filter(dbFilter.tableInDatabaseFilter())
                        .map(TableId::toString).forEach(results::add);
            } catch (SQLException e) {
                // don't do anything ...
            }
            return results;
        }

        @Override
        public boolean visible(Field field, Configuration config) {
            return true;
        }
    }

    private static int validateMaxQueueSize(Configuration config, Field field, ValidationOutput problems) {
        int maxQueueSize = config.getInteger(field);
        int maxBatchSize = config.getInteger(MAX_BATCH_SIZE);
        int count = 0;
        if (maxQueueSize <= 0) {
            maxBatchSize = maxQueueSize / 2;
            problems.accept(field, maxQueueSize, "A positive queue size is required");
            ++count;
        }
        if (maxQueueSize <= maxBatchSize) {
            maxBatchSize = maxQueueSize / 2;
            problems.accept(field, maxQueueSize, "Must be larger than the maximum batch size");
            ++count;
        }
        return count;
    }

    private static int validateDatabaseBlacklist(Configuration config, Field field, ValidationOutput problems) {
        String whitelist = config.getString(DATABASE_WHITELIST);
        String blacklist = config.getString(DATABASE_BLACKLIST);
        if (whitelist != null && blacklist != null) {
            problems.accept(DATABASE_BLACKLIST, blacklist, "Whitelist is already specified");
            return 1;
        }
        return 0;
    }

    private static int validateTableBlacklist(Configuration config, Field field, ValidationOutput problems) {
        String whitelist = config.getString(TABLE_WHITELIST);
        String blacklist = config.getString(TABLE_BLACKLIST);
        if (whitelist != null && blacklist != null) {
            problems.accept(TABLE_BLACKLIST, blacklist, "Whitelist is already specified");
            return 1;
        }
        return 0;
    }

    private static int validateGtidSetExcludes(Configuration config, Field field, ValidationOutput problems) {
        String includes = config.getString(GTID_SOURCE_INCLUDES);
        String excludes = config.getString(GTID_SOURCE_EXCLUDES);
        if (includes != null && excludes != null) {
            problems.accept(GTID_SOURCE_EXCLUDES, excludes, "Included GTID source UUIDs are already specified");
            return 1;
        }
        return 0;
    }

    private static int validateColumnBlacklist(Configuration config, Field field, ValidationOutput problems) {
        // String blacklist = config.getString(COLUMN_BLACKLIST);
        return 0;
    }

    private static int randomServerId() {
        int lowestServerId = 5400;
        int highestServerId = 6400;
        return lowestServerId + new Random().nextInt(highestServerId - lowestServerId);
    }
}
