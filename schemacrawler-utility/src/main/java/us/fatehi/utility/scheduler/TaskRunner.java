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
package us.fatehi.utility.scheduler;

import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public interface TaskRunner {

  String getId();

  boolean isStopped();

  /**
   * Allows for a deferred conversion to a string. Useful in logging.
   *
   * @return String supplier.
   * @throws Exception
   */
  Supplier<String> report();

  void run(TaskDefinition... taskDefinitions) throws Exception;

  void stop() throws ExecutionException;
}
