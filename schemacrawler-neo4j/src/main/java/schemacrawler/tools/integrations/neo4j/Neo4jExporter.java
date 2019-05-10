/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2019, Sualeh Fatehi <sualeh@hotmail.com>.
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
package schemacrawler.tools.integrations.neo4j;


import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import apoc.ApocConfiguration;
import apoc.export.cypher.ExportCypher;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import schemacrawler.schemacrawler.SchemaCrawlerException;

public class Neo4jExporter
  implements AutoCloseable
{

  private final GraphDatabaseService dbService;

  public Neo4jExporter(final Path outputDirectory)
  {
    requireNonNull(outputDirectory, "No output directory provided");

    final GraphDatabaseFactory dbFactory = new GraphDatabaseFactory();
    dbService = dbFactory.newEmbeddedDatabase(outputDirectory.toFile());
  }

  public void export(final Path outputFile)
    throws SchemaCrawlerException
  {
    requireNonNull(outputFile, "No output file provided");

    try (final Transaction tx = dbService.beginTx())
    {
      final Map<String, Object> apocConfig = new HashMap<>();
      apocConfig.put("export.file.enabled", "true");
      ApocConfiguration.addToConfig(apocConfig);

      final Map<String, Object> exportConfig = new HashMap<>();
      exportConfig.put("stream", "false");

      final String outputFilename = outputFile.toAbsolutePath().toString();

      final ExportCypher exportCypher = new ExportCypher(dbService);
      exportCypher.all(outputFilename, exportConfig).count();

      tx.success();
    }
    catch (final IOException e)
    {
      throw new SchemaCrawlerException("Could not export Cypher file", e);
    }
  }

  @Override
  public void close()
  {
    if (dbService != null)
    {
      dbService.shutdown();
    }
  }

}
