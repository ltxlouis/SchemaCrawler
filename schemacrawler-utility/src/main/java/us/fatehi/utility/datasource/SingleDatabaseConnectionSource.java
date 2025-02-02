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

package us.fatehi.utility.datasource;

import static java.util.Objects.requireNonNull;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Properties;

import us.fatehi.utility.database.DatabaseUtility;

final class SingleDatabaseConnectionSource extends AbstractDatabaseConnectionSource {

  private final Connection connection;

  public SingleDatabaseConnectionSource(final String connectionUrl, final Connection connection) {
    super(connectionUrl);
    this.connection = requireNonNull(connection, "No database connection provided");
    try {
      DatabaseUtility.checkConnection(connection);
    } catch (final SQLException e) {
      throw new RuntimeException("Could not use provided connection", e);
    }
  }

  SingleDatabaseConnectionSource(
      final String connectionUrl,
      final Map<String, String> connectionProperties,
      final UserCredentials userCredentials) {
    super(connectionUrl);
    // Ensure that user credentials are not null
    requireNonNull(userCredentials, "No user credentials provided");

    final String user = userCredentials.getUser();
    final String password = userCredentials.getPassword();
    final Properties jdbcConnectionProperties =
        createConnectionProperties(connectionUrl, connectionProperties, user, password);
    connection = getConnection(connectionUrl, jdbcConnectionProperties);
  }

  @Override
  public void close() throws Exception {
    connection.close();
  }

  @Override
  public Connection get() {
    return PooledConnectionUtility.newPooledConnection(connection, this);
  }

  @Override
  public boolean releaseConnection(final Connection connection) {
    // No-op
    return true;
  }

  @Override
  protected void finalize() throws Throwable {
    // Assert that all connections are closed
    if (!connection.isClosed()) {
      throw new RuntimeException("Connection pool is not closed");
    }
    super.finalize();
  }
}
