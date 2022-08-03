/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2022, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/
package schemacrawler.tools.sqlite;

import static java.util.Objects.requireNonNull;
import static us.fatehi.utility.IOUtility.createTempFilePath;
import static us.fatehi.utility.IOUtility.isFileReadable;
import static us.fatehi.utility.database.DatabaseUtility.checkConnection;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.function.Predicate;

import schemacrawler.inclusionrule.InclusionRule;
import schemacrawler.inclusionrule.ListExclusionRule;
import schemacrawler.inclusionrule.RegularExpressionExclusionRule;
import schemacrawler.schemacrawler.LimitOptions;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.exceptions.DatabaseAccessException;
import schemacrawler.schemacrawler.exceptions.ExecutionRuntimeException;
import schemacrawler.schemacrawler.exceptions.IORuntimeException;
import schemacrawler.tools.databaseconnector.DatabaseConnectionSource;
import schemacrawler.tools.databaseconnector.DatabaseUrlConnectionOptions;
import schemacrawler.tools.databaseconnector.SingleUseUserCredentials;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.options.OutputFormat;
import schemacrawler.tools.options.OutputOptions;
import schemacrawler.tools.options.OutputOptionsBuilder;

public class EmbeddedSQLiteWrapper {

  private static InclusionRule sqliteTableExclusionRule =
      new InclusionRule() {

        private static final long serialVersionUID = -7643052797359767051L;

        private final Predicate<String> exclusionRule =
            new ListExclusionRule(
                    Arrays.asList(
                        // Django tables
                        "auth_group",
                        "auth_group_permissions",
                        "auth_permission",
                        "auth_user",
                        "auth_user_groups",
                        "auth_user_user_permissions",
                        "otp_totp_totpdevice",
                        // Liquibase
                        "DATABASECHANGELOG",
                        // Flyway
                        "SCHEMA_VERSION",
                        // Entity Framework Core https://github.com/dotnet/efcore
                        "_EFMigrationsHistory",
                        // Android
                        "android_metadata"))
                .and(new RegularExpressionExclusionRule("django_.*"));

        /** {@inheritDoc} */
        @Override
        public boolean test(final String text) {
          return exclusionRule.test(text);
        }
      };

  private Path databaseFile;

  public DatabaseConnectionSource createDatabaseConnectionSource() {
    requireNonNull(databaseFile, "Database file not loaded");

    final DatabaseUrlConnectionOptions urlConnectionOptions =
        new DatabaseUrlConnectionOptions(getConnectionUrl());
    final DatabaseConnectionSource connectionOptions =
        new SQLiteDatabaseConnector()
            .newDatabaseConnectionSource(urlConnectionOptions, new SingleUseUserCredentials());
    return connectionOptions;
  }

  public Path executeForOutput(final String title, final OutputFormat extension) {
    try (final Connection connection = createDatabaseConnectionSource().get()) {
      return executeForOutput(connection, title, extension);
    } catch (final SQLException e) {
      throw new DatabaseAccessException("Could not create database connection", e);
    }
  }

  public String getConnectionUrl() {
    requireNonNull(databaseFile, "Database file not loaded");
    return "jdbc:sqlite:" + databaseFile.toString();
  }

  public Path getDatabasePath() {
    if (databaseFile == null) {
      return null;
    } else {
      return databaseFile;
    }
  }

  public void setDatabasePath(final Path dbFile) {
    databaseFile = checkDatabaseFile(dbFile);
  }

  protected final Path checkDatabaseFile(final Path dbFile) {
    final Path databaseFile =
        requireNonNull(dbFile, "No database file path provided").normalize().toAbsolutePath();
    if (!isFileReadable(databaseFile)) {
      throw new IORuntimeException(String.format("Could not read database file <%s>", dbFile));
    }
    return databaseFile;
  }

  private Path executeForOutput(
      final Connection connection, final String title, final OutputFormat extension) {
    try {
      checkConnection(connection);

      final LimitOptions limitOptions =
          LimitOptionsBuilder.builder().includeTables(sqliteTableExclusionRule).toOptions();
      final SchemaCrawlerOptions schemaCrawlerOptions =
          SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions().withLimitOptions(limitOptions);

      final Path diagramFile = createTempFilePath("schemacrawler", extension.getFormat());
      final OutputOptions outputOptions =
          OutputOptionsBuilder.builder()
              .title(title)
              .withOutputFormat(extension)
              .withOutputFile(diagramFile)
              .toOptions();

      final SchemaCrawlerExecutable executable = new SchemaCrawlerExecutable("schema");
      executable.setSchemaCrawlerOptions(schemaCrawlerOptions);
      executable.setOutputOptions(outputOptions);
      executable.setConnection(connection);
      executable.execute();

      return diagramFile;
    } catch (final Exception e) {
      throw new ExecutionRuntimeException(
          String.format("Could not create database schema diagram <%s>", title), e);
    }
  }
}
