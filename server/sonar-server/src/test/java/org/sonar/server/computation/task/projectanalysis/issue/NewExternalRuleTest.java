/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
package org.sonar.server.computation.task.projectanalysis.issue;

import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.RuleType;

import static org.assertj.core.api.Assertions.assertThat;

public class NewExternalRuleTest {
  @org.junit.Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void should_build_new_external_rule() {
    NewExternalRule.Builder builder = new NewExternalRule.Builder()
      .setDescriptionUrl("url")
      .setKey(RuleKey.of("repo", "rule"))
      .setName("name")
      .setSeverity("MAJOR")
      .setType(RuleType.BUG);

    assertThat(builder.descriptionUrl()).isEqualTo("url");
    assertThat(builder.name()).isEqualTo("name");
    assertThat(builder.severity()).isEqualTo("MAJOR");
    assertThat(builder.type()).isEqualTo(RuleType.BUG);
    assertThat(builder.descriptionUrl()).isEqualTo("url");

    NewExternalRule rule = builder.build();

    assertThat(rule.getDescriptionUrl()).isEqualTo("url");
    assertThat(rule.getName()).isEqualTo("name");
    assertThat(rule.getPluginKey()).isNull();
    assertThat(rule.getSeverity()).isEqualTo("MAJOR");
    assertThat(rule.getType()).isEqualTo(RuleType.BUG);
    assertThat(rule.getDescriptionUrl()).isEqualTo("url");
  }

  @Test
  public void fail_if_name_is_not_set() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("'name' not expected to be empty for an external rule");

    new NewExternalRule.Builder()
      .setDescriptionUrl("url")
      .setKey(RuleKey.of("repo", "rule"))
      .setSeverity("MAJOR")
      .setType(RuleType.BUG)
      .build();
  }

  @Test
  public void fail_if_rule_key_is_not_set() {
    exception.expect(IllegalStateException.class);
    exception.expectMessage("'key' not expected to be null for an external rule");

    new NewExternalRule.Builder()
      .setDescriptionUrl("url")
      .setName("name")
      .setSeverity("MAJOR")
      .setType(RuleType.BUG)
      .build();
  }
}
