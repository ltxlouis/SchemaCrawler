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


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.logging.Logger;

import schemacrawler.tools.executable.BaseSchemaCrawlerCommand;
import schemacrawler.tools.traversal.SchemaTraversalHandler;
import schemacrawler.tools.traversal.SchemaTraverser;

public class Neo4JCommand
  extends BaseSchemaCrawlerCommand
{

  static final String COMMAND = "neo4j";

  private static final Logger LOGGER = Logger
    .getLogger(Neo4JCommand.class.getName());

  protected Neo4JCommand()
  {
    super(COMMAND);
  }

  @Override
  public void execute()
    throws Exception
  {
    // TODO: Possibly process command-line options, which are available
    // in additionalConfiguration

    final Path outputDirectory = Files.createTempDirectory("sc-neo4j")
      .toAbsolutePath();

    final SchemaTraversalHandler formatter = new SchemaNeo4JHandler(
      outputDirectory);

    final SchemaTraverser traverser = new SchemaTraverser();
    traverser.setCatalog(catalog);
    traverser.setHandler(formatter);

    traverser.traverse();

    // Export to Cypher file
    final Path outputFile = outputOptions.getOutputFile().orElseGet(() -> Paths
      .get(".",
           String.format("schemacrawler-%s.%s",
                         UUID.randomUUID(),
                         outputOptions.getOutputFormatValue()))).normalize()
      .toAbsolutePath();
    final Neo4jExporter neo4jExporter = new Neo4jExporter(outputDirectory);
    neo4jExporter.export(outputFile);
    neo4jExporter.close();

  }

}
