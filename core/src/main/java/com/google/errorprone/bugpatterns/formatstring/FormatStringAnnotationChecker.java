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

import static com.google.errorprone.BugPattern.Category.JDK;
import static com.google.errorprone.BugPattern.MaturityLevel.EXPERIMENTAL;
import static com.google.errorprone.BugPattern.SeverityLevel.ERROR;
import static com.google.errorprone.BugPattern.Suppressibility.UNSUPPRESSIBLE;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.bugpatterns.BugChecker.MethodInvocationTreeMatcher;
import com.google.errorprone.bugpatterns.BugChecker.MethodTreeMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.code.Type;
import java.util.List;

/** {@link BugChecker} to assert validity of methods calls with {@link FormatString} annotations. */
@BugPattern(
  name = "FormatStringAnnotation",
  summary = "Invalid format string passed to formatting method.",
  category = JDK,
  severity = ERROR,
  maturity = EXPERIMENTAL,
  suppressibility = UNSUPPRESSIBLE
)
public final class FormatStringAnnotationChecker extends BugChecker
    implements MethodInvocationTreeMatcher, MethodTreeMatcher {

  @Override
  public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
    if (!ASTHelpers.hasAnnotation(ASTHelpers.getSymbol(tree), FormatMethod.class, state)) {
      return Description.NO_MATCH;
    }

    Type stringType = state.getSymtab().stringType;

    List<VarSymbol> params = ASTHelpers.getSymbol(tree).getParameters();
    int firstStringIndex = -1;
    int formatString = -1;
    for (int i = 0; i < params.size(); i++) {
      VarSymbol param = params.get(i);
      if (ASTHelpers.hasAnnotation(param, FormatString.class, state)) {
        formatString = i;
        break;
      }
      if (firstStringIndex < 0 && ASTHelpers.isSameType(params.get(i).type, stringType, state)) {
        firstStringIndex = i;
      }
    }

    if (formatString < 0) {
      formatString = firstStringIndex;
    }

    List<? extends ExpressionTree> args = tree.getArguments();
    FormatStringValidation.ValidationResult result =
        StrictFormatStringValidation.validate(
            args.get(formatString), args.subList(formatString + 1, args.size()), state);

    if (result != null) {
      return buildDescription(tree).setMessage(result.message()).build();
    } else {
      return Description.NO_MATCH;
    }
  }

  @Override
  public Description matchMethod(MethodTree tree, VisitorState state) {
    Type stringType = state.getSymtab().stringType;

    boolean isFormatMethod =
        ASTHelpers.hasAnnotation(ASTHelpers.getSymbol(tree), FormatMethod.class, state);
    boolean foundFormatString = false;
    boolean foundString = false;
    for (VariableTree param : tree.getParameters()) {
      VarSymbol paramSymbol = ASTHelpers.getSymbol(param);
      boolean isStringParam = ASTHelpers.isSameType(paramSymbol.type, stringType, state);

      if (isStringParam) {
        foundString = true;
      }

      if (ASTHelpers.hasAnnotation(paramSymbol, FormatString.class, state)) {
        if (!isFormatMethod) {
          return buildDescription(tree)
              .setMessage(
                  "A parameter can only be annotated @FormatString in a method annotated "
                      + "@FormatMethod: "
                      + param)
              .build();
        }
        if (!isStringParam) {
          return buildDescription(param)
              .setMessage("Only strings can be annotated @FormatString.")
              .build();
        }
        if (foundFormatString) {
          return buildDescription(tree)
              .setMessage("A method cannot have more than one @FormatString parameter.")
              .build();
        }
        foundFormatString = true;
      }
    }

    if (isFormatMethod && !foundString) {
      return buildDescription(tree)
          .setMessage("An @FormatMethod must contain at least one String parameter.")
          .build();
    }

    return Description.NO_MATCH;
  }
}
