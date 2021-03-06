/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.errorprone.bugpatterns.formatstring;

import com.google.errorprone.CompilationTestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** {@link FormatStringAnnotationChecker}Test. */
@RunWith(JUnit4.class)
public class FormatStringAnnotationCheckerTest {

  private CompilationTestHelper compilationHelper;

  @Before
  public void setUp() {
    compilationHelper =
        CompilationTestHelper.newInstance(FormatStringAnnotationChecker.class, getClass());
  }

  @Test
  public void testMatches_failsWithNonMatchingFormatArgs() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog(@FormatString String s, Object arg,",
            "      Object arg2) {",
            "    // BUG: Diagnostic contains: The number of format arguments passed with an",
            "    log(s, \"test\");",
            "    // BUG: Diagnostic contains: The format argument types passed with an",
            "    log(s, \"test1\", \"test2\");",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsWithMatchingFormatStringAndArgs() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  @FormatMethod public static void callLog(@FormatString String s, Object arg) {",
            "    log(s, arg);",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsWithMismatchedFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public static void callLog() {",
            "    // BUG: Diagnostic contains: extra format arguments: used 1, provided 2",
            "    log(\"%s\", new Object(), new Object());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_succeedsForCompileTimeConstantFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  @FormatMethod public static void log(@FormatString String s, Object... args) {}",
            "  public static void callLog() {",
            "    final String formatString = \"%d\";",
            "    log(formatString, new Integer(0));",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsWhenExpressionGivenForFormatString() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "public class FormatStringTestCase {",
            "  @FormatMethod static void log(String s, Object... args) {}",
            "  public static String formatString() { return \"\";}",
            "  public static void callLog() {",
            "    String format = \"log: \";",
            "    // BUG: Diagnostic contains: Format strings must be either a literal or a",
            "    log(format + 3);",
            "    // BUG: Diagnostic contains: Format strings must be either a literal or a",
            "    log(formatString());",
            "  }",
            "}")
        .doTest();
  }

  @Test
  public void testMatches_failsForInvalidMethodHeaders() {
    compilationHelper
        .addSourceLines(
            "test/FormatStringTestCase.java",
            "package test;",
            "import com.google.errorprone.annotations.FormatMethod;",
            "import com.google.errorprone.annotations.FormatString;",
            "public class FormatStringTestCase {",
            "  // BUG: Diagnostic contains: A method cannot have more than one @FormatString",
            "  @FormatMethod void log1(@FormatString String s1, @FormatString String s2) {}",
            "  // BUG: Diagnostic contains: An @FormatMethod must contain at least one String",
            "  @FormatMethod void log2(Object o) {}",
            "  // BUG: Diagnostic contains: Only strings can be annotated @FormatString.",
            "  @FormatMethod void log3(@FormatString Object o) {}",
            "  // BUG: Diagnostic contains: A parameter can only be annotated @FormatString in a",
            "  void log4(@FormatString Object o) {}",
            "}")
        .doTest();
  }
}
