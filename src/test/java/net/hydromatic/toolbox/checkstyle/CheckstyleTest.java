/*
 * Licensed to Julian Hyde under one or more contributor license
 * agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership. Julian Hyde
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.hydromatic.toolbox.checkstyle;

import com.puppycrawl.tools.checkstyle.DefaultConfiguration;
import com.puppycrawl.tools.checkstyle.api.CheckstyleException;
import com.puppycrawl.tools.checkstyle.api.LocalizedMessage;
import com.puppycrawl.tools.checkstyle.api.MessageDispatcher;

import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * Checker that applies some custom checks to each file.
 */
public class CheckstyleTest {
  static String times(String s, int n) {
    final StringBuilder b = new StringBuilder();
    for (int i = 0; i < n; i++) {
      b.append(s);
    }
    return b.toString();
  }

  @Test public void testDeString() {
    assertThat(HydromaticFileSetCheck.deString("xx"), is("xx"));
    assertThat(HydromaticFileSetCheck.deString("a \"bc\" d"),
        is("a string d"));
    assertThat(HydromaticFileSetCheck.deString("\"bc\" d"),
        is("string d"));
    assertThat(HydromaticFileSetCheck.deString("a \"bc\""),
        is("a string"));
    assertThat("two strings",
        HydromaticFileSetCheck.deString(" String x = \"a\" + \"bc\";"),
        is(" String x = string + string;"));
    String s = "      + \"a big string " + times("123456", 1000) + "\\n\"";
    assertThat("a big string",
        HydromaticFileSetCheck.deString(s), is("      + string"));
    String s2 = "      + \"a big string " + times("12\\\"3456", 1000) + "\\n\"";
    assertThat("a big string with quotes",
        HydromaticFileSetCheck.deString(s2), is("      + string"));
    assertThat("string that ends in slash",
        HydromaticFileSetCheck.deString(" String x = \"abc\\"),
        is(" String x = string"));
    assertThat("slash just before end of string",
        HydromaticFileSetCheck.deString(" String x = \"abc\\\\\";"),
        is(" String x = string;"));
  }

  @Test public void testCheckstyle() throws CheckstyleException {
    String[] lines = {
      "public class Foo {",
      "  void foo() {",
      "",
      "",
      "  }",
      "  /** See {@link",
      "   * java.lang.String}<p>",
      "   *",
      "   * <p><a href=\"https://issues.apache.org/jira/browse/CALCITE-1234\">[CALCITE-1234]",
      "   * Description</a>",
      "   *",
      "   * <p><a href=\"http://issues.apache.org\">[CALCITE-1234] Description</a>",
      "   *",
      "   * <p><a href=\"https://issues.apache.org/jira/browse\">CALCITE-1234 Description</a>",
      "   *",
      "   * @param descriptionLess",
      "   * @param p A very very very very very very very very very long parameter description",
      "   */",
      "  @Override",
      "  void foo() {",
      "    firstFunction(secondFunction()); // ok",
      "    firstFunction(",
      "      secondFunction(1, 2, 3), thirdFunction(4)); // ok",
      "    firstFunction(secondFunction(1, 2,",
      "      3), thirdFunction(4)); // not ok",
      "    assertFalse(stmt.execute(",
      "        String.format(\"CREATE TABLE %s(id integer)\", productTable)));",
      "",
      "    String x = \"abc\\ndef\";",
      "    String y = \"abc\\n\" + \"def\";",
      "    new StringBuilder()",
      "      .append(\" (\").append(sqlState).append(\"\\n\\n\");",
      "  }",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "",
      "// End Premature.java",
      "}",
      "// End Foo.java"
    };
    String[] errors = {
      "6: Split @link",
      "7: Orphan <p>. Make it the first line of a paragraph",
      "12: Bad JIRA reference",
      "14: Bad JIRA reference",
      "16: Parameter with no description",
      "17: Javadoc line too long (87 chars)",
      "19: @Override should not be on its own line",
      "24: Open parentheses exceed closes by 2 or more",
      "26: Open parentheses exceed closes by 2 or more",
      "30: Newline in string should be at end of line",
      "46: End seen more than once",
      "46: Last line should be '// End Baz.java'",
    };
    final HydromaticFileSetCheck check = new HydromaticFileSetCheck();
    final List<String> actualErrors = new ArrayList<>();
    check.setMessageDispatcher(
        new MessageDispatcher() {
          public void fireFileStarted(String s) {}
          public void fireFileFinished(String s) {}
          public void fireErrors(String s,
              SortedSet<LocalizedMessage> sortedSet) {
            for (LocalizedMessage message : sortedSet) {
              actualErrors.add(message.getLineNo() + ": "
                  + message.getMessage());
            }
          }
        });
    check.configure(new DefaultConfiguration("x"));
    final File file = new File("Baz.java");
    check.processFiltered(file, Arrays.asList(lines));
    check.fireErrors2(file);
    for (String error : actualErrors) {
      assertThat(error, Arrays.asList(errors).contains(error), is(true));
    }
    for (String error : errors) {
      assertThat(error, actualErrors.contains(error), is(true));
    }
  }
}

// End CheckstyleTest.java
