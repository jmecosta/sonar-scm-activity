/*
 * Sonar SCM Activity Plugin
 * Copyright (C) 2010 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.scmactivity;

import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.command.blame.BlameLine;
import org.apache.maven.scm.command.blame.BlameScmResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.api.measures.Measure;
import org.sonar.api.measures.Metric;
import org.sonar.api.measures.PropertiesBuilder;
import org.sonar.api.resources.Resource;
import org.sonar.api.utils.DateUtils;

import java.io.File;

public class Blame implements BatchExtension {
  private static final Logger LOG = LoggerFactory.getLogger(Blame.class);

  private final ScmFacade scmFacade;

  public Blame(ScmFacade scmFacade) {
    this.scmFacade = scmFacade;
  }

  public MeasureUpdate save(File file, Resource resource, String sha1) {
    BlameScmResult result = retrieveBlame(file);
    if (result == null) {
      return new CopyPreviousMeasures(resource);
    }

    PropertiesBuilder<Integer, String> authors = propertiesBuilder(CoreMetrics.SCM_AUTHORS_BY_LINE);
    PropertiesBuilder<Integer, String> dates = propertiesBuilder(CoreMetrics.SCM_LAST_COMMIT_DATETIMES_BY_LINE);
    PropertiesBuilder<Integer, String> revisions = propertiesBuilder(CoreMetrics.SCM_REVISIONS_BY_LINE);

    int lineNumber = 1;
    for (BlameLine line : result.getLines()) {
      authors.add(lineNumber, line.getAuthor());
      dates.add(lineNumber, DateUtils.formatDateTime(line.getDate()));
      revisions.add(lineNumber, line.getRevision());

      lineNumber++;
    }

    return new SaveNewMeasures(resource, authors.build(), dates.build(), revisions.build(), new Measure(ScmActivityMetrics.SCM_HASH, sha1));
  }

  private BlameScmResult retrieveBlame(File file) {
    LOG.info("Retrieve SCM info for {}", file);

    try {
      LOG.info("Lets See {}", file);      
      BlameScmResult result = scmFacade.blame(file);
      if (result.isSuccess()) {
        return result;
      }

      LOG.warn(String.format("Fail to retrieve SCM info of: %s. Reason: %s%n%s", file, result.getProviderMessage(), result.getCommandOutput()));
    } catch (ScmException e) {
      LOG.warn(String.format("Fail to retrieve SCM info of: %s", file), e); // See SONARPLUGINS-368. Can occur on generated source
    }

    return null;
  }

  private static PropertiesBuilder<Integer, String> propertiesBuilder(Metric metric) {
    return new PropertiesBuilder<Integer, String>(metric);
  }
}
