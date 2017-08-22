/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.sonar.server.projectbranch.ws;

import java.util.Map;
import javax.annotation.Nullable;
import org.sonar.db.component.BranchDto;
import org.sonar.db.measure.MeasureDto;
import org.sonarqube.ws.Common;
import org.sonarqube.ws.WsBranches;

import static org.sonar.api.measures.CoreMetrics.ALERT_STATUS_KEY;
import static org.sonar.api.measures.CoreMetrics.BUGS_KEY;
import static org.sonar.api.measures.CoreMetrics.CODE_SMELLS_KEY;
import static org.sonar.api.measures.CoreMetrics.VULNERABILITIES_KEY;
import static org.sonar.core.util.Protobuf.setNullable;
import static org.sonar.db.component.BranchType.LONG;
import static org.sonar.db.component.BranchType.SHORT;
import static org.sonar.server.projectbranch.ws.BranchesWs.DEFAULT_MAIN_BRANCH_NAME;

public class BranchDtoToWsBranch {

  private BranchDtoToWsBranch() {
    // static methods only
  }

  public static WsBranches.Branch.Builder toBranchBuilder(BranchDto branch, @Nullable BranchDto mergeBranch, Map<String, MeasureDto> measuresByMetricKey) {
    WsBranches.Branch.Builder builder = WsBranches.Branch.newBuilder();
    String branchKey = branch.getKey();
    String effectiveBranchKey = branchKey == null && branch.isMain() ? DEFAULT_MAIN_BRANCH_NAME : branchKey;
    setNullable(effectiveBranchKey, builder::setName);
    builder.setIsMain(branch.isMain());
    builder.setType(Common.BranchType.valueOf(branch.getBranchType().name()));
    if (mergeBranch != null) {
      String mergeBranchKey = mergeBranch.getKey();
      if (mergeBranchKey == null && branch.getBranchType().equals(SHORT)) {
        builder.setMergeBranch(DEFAULT_MAIN_BRANCH_NAME);
      } else {
        setNullable(mergeBranchKey, builder::setMergeBranch);
      }
    }

    if (branch.getBranchType().equals(LONG)) {
      WsBranches.Branch.Status.Builder statusBuilder = WsBranches.Branch.Status.newBuilder();
      MeasureDto measure = measuresByMetricKey.get(ALERT_STATUS_KEY);
      setNullable(measure, m -> builder.setStatus(statusBuilder.setQualityGateStatus(m.getData())));
    }

    if (branch.getBranchType().equals(SHORT)) {
      WsBranches.Branch.Status.Builder statusBuilder = WsBranches.Branch.Status.newBuilder();
      MeasureDto bugs = measuresByMetricKey.get(BUGS_KEY);
      setNullable(bugs, m -> builder.setStatus(statusBuilder.setBugs(m.getValue().intValue())));

      MeasureDto vulnerabilities = measuresByMetricKey.get(VULNERABILITIES_KEY);
      setNullable(vulnerabilities, m -> builder.setStatus(statusBuilder.setVulnerabilities(m.getValue().intValue())));

      MeasureDto codeSmells = measuresByMetricKey.get(CODE_SMELLS_KEY);
      setNullable(codeSmells, m -> builder.setStatus(statusBuilder.setCodeSmells(m.getValue().intValue())));
      builder.setStatus(statusBuilder);
    }
    return builder;
  }
}
