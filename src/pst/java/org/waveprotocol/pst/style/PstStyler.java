/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.pst.style;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;

/**
 * A code styler using a Builder approach to configure styles using smaller
 * reformatting components.
 *
 * TODO(kalman): take string literals into account.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstStyler implements Styler {

  private static final String BACKUP_SUFFIX = ".prePstStyler";
  private static final String INDENT = "  ";
  private static final String[] ATOMIC_TOKENS = new String[] {
      "} else {",
      "} else if (",
      "for (",
      "/*-{",
      "}-*/",
  };

  /**
   * Builder for a series of composed style components.
   */
  private static class StyleBuilder {

    /**
     * Styles a single line, outputting generated lines to a generator.
     * The styler may output anywhere between 0 and infinite lines.
     */
    private interface LineStyler {
      void next(String line, LineGenerator generator);
    }

    /**
     * Generates lines to some output sink.
     */
    private interface LineGenerator {
      void yield(CharSequence s);
    }

    /**
     * A pipeline of line stylers as a single line generator.
     */
    private static final class LinePipeline implements LineGenerator {
      private final LineStyler lineStyler;
      private final LineGenerator next;

      private LinePipeline(LineStyler lineStyler, LineGenerator next) {
        this.lineStyler = lineStyler;
        this.next = next;
      }

      /**
       * Constructs a pipeline of line stylers.
       *
       * @param ls the line stylers to place in the pipeline
       * @param sink the line generator at the end of the pipeline
       * @return the head of the pipeline
       */
      public static LinePipeline construct(Iterable<LineStyler> ls, LineGenerator sink) {
        return new LinePipeline(head(ls),
            (Iterables.size(ls) == 1) ? sink : construct(tail(ls), sink));
      }

      private static LineStyler head(Iterable<LineStyler> ls) {
        return ls.iterator().next();
      }

      private static Iterable<LineStyler> tail(final Iterable<LineStyler> ls) {
        return new Iterable<LineStyler>() {
          @Override public Iterator<LineStyler> iterator() {
            Iterator<LineStyler> tail = ls.iterator();
            tail.next();
            return tail;
          }
        };
      }

      @Override
      public void yield(CharSequence s) {
        lineStyler.next(s.toString(), next);
      }
    }

    /**
     * Generates lines into a list.
     */
    private static final class ListGenerator implements LineGenerator {
      private final List<String> list = Lists.newArrayList();

      @Override
      public void yield(CharSequence s) {
        list.add(s.toString());
      }

      public List<String> getList() {
        return list;
      }
    }

    /**
     * Maintains some helpful state across a styling.
     */
    private abstract class StatefulLineStyler implements LineStyler {
      private boolean inShortComment = false;
      private boolean inLongComment = false;
      private boolean inStartOfComment = false;
      private boolean previousWasComment = false;
      private int lineNumber = 0;

      protected boolean inComment() {
        return inShortComment || inLongComment;
      }

      protected boolean inStartOfComment() {
        return inStartOfComment;
      }

      protected boolean inLongComment() {
        return inLongComment;
      }

      protected int getLineNumber() {
        return lineNumber;
      }

      protected boolean previousWasComment() {
        return previousWasComment;
      }

      @Override
      public final void next(String line, LineGenerator generator) {
        lineNumber++;
        // TODO(kalman): JSNI?
        if (line.matches("^[ \t]*/\\*.*")) {
          inLongComment = true;
          inStartOfComment = true;
        }
        if (line.matches("^[ \t]*//.*")) {
          inShortComment = true;
          inStartOfComment = true;
        }
        doNext(line, generator);
        previousWasComment = inShortComment || inLongComment;
        if (line.contains("*/")) {
          inLongComment = false;
        }
        inShortComment = false;
        inStartOfComment = false;
      }

      abstract void doNext(String line, LineGenerator generator);
    }

    private final List<LineStyler> lineStylers = Lists.newArrayList();

    /**
     * Applies the state of the styler to a list of lines.
     *
     * @return the styled lines
     */
    public List<String> apply(List<String> lines) {
      ListGenerator result = new ListGenerator();
      LinePipeline pipeline = LinePipeline.construct(lineStylers, result);
      for (String line : lines) {
        pipeline.yield(line);
      }
      return result.getList();
    }

    public StyleBuilder addNewLineBefore(final char newLineBefore) {
      lineStylers.add(new StatefulLineStyler() {
        @Override public void doNext(String line, LineGenerator generator) {
          // TODO(kalman): this is heavy-handed; be fine-grained and just don't
          // split over tokens (need regexp, presumably).
          if (inComment() || containsAtomicToken(line)) {
            generator.yield(line);
            return;
          }

          StringBuilder s = new StringBuilder();
          for (char c : line.toCharArray()) {
            if (c == newLineBefore) {
              generator.yield(s);
              s = new StringBuilder();
            }
            s.append(c);
          }
          generator.yield(s);
        }
      });
      return this;
    }

    public StyleBuilder addNewLineAfter(final char newLineAfter) {
      lineStylers.add(new StatefulLineStyler() {
        @Override public void doNext(String line, LineGenerator generator) {
          // TODO(kalman): same as above.
          if (inComment() || containsAtomicToken(line)) {
            generator.yield(line);
            return;
          }

          StringBuilder s = new StringBuilder();
          for (char c : line.toCharArray()) {
            s.append(c);
            if (c == newLineAfter) {
              generator.yield(s);
              s = new StringBuilder();
            }
          }
          generator.yield(s);
        }
      });
      return this;
    }

    public StyleBuilder trim() {
      lineStylers.add(new LineStyler() {
        @Override public void next(String line, LineGenerator generator) {
          generator.yield(line.trim());
        }
      });
      return this;
    }

    public StyleBuilder removeRepeatedSpacing() {
      lineStylers.add(new LineStyler() {
        @Override public void next(String line, LineGenerator generator) {
          generator.yield(line.replaceAll("[ \t]+", " "));
        }
      });
      return this;
    }

    public StyleBuilder stripBlankLines() {
      lineStylers.add(new LineStyler() {
        @Override public void next(String line, LineGenerator generator) {
          if (!line.isEmpty()) {
            generator.yield(line);
          }
        }
      });
      return this;
    }

    public StyleBuilder stripInitialBlankLine() {
      lineStylers.add(new LineStyler() {
        boolean firstLine = true;
        @Override public void next(String line, LineGenerator generator) {
          if (!firstLine || !line.isEmpty()) {
            generator.yield(line);
          }
          firstLine = false;
        }
      });
      return this;
    }

    public StyleBuilder stripDuplicateBlankLines() {
      lineStylers.add(new LineStyler() {
        boolean previousWasEmpty = false;
        @Override public void next(String line, LineGenerator generator) {
          if (!previousWasEmpty || !line.isEmpty()) {
            generator.yield(line);
          }
          previousWasEmpty = line.isEmpty();
        }
      });
      return this;
    }

    public StyleBuilder indentBraces() {
      lineStylers.add(new StatefulLineStyler() {
        private int indentLevel = 0;

        @Override public void doNext(String line, LineGenerator generator) {
          if (!ignore(line) && line.contains("}")) {
            indentLevel--;
            Preconditions.checkState(indentLevel >= 0,
                "Indentation level reached < 0 on line " + getLineNumber() + " (" + line + ")");
          }
          String result = "";
          if (!line.isEmpty()) {
            result = Strings.repeat(INDENT, indentLevel) + line;
          }
          if (!ignore(line) && line.contains("{")) {
            indentLevel++;
          }
          generator.yield(result.toString());
        }

        private boolean ignore(String line) {
          // Ignore self-closing braces.
          return line.contains("{")
              && line.contains("}")
              && line.indexOf('{') < line.lastIndexOf('}');
        }
      });
      return this;
    }

    public StyleBuilder indentLongComments() {
      lineStylers.add(new StatefulLineStyler() {
        @Override void doNext(String line, LineGenerator generator) {
          if (inLongComment() && !inStartOfComment()) {
            generator.yield(" " + line);
          } else {
            generator.yield(line);
          }
        }
      });
      return this;
    }

    public StyleBuilder doubleIndentUnfinishedLines() {
      lineStylers.add(new StatefulLineStyler() {
        boolean previousUnfinished = false;

        @Override public void doNext(String line, LineGenerator generator) {
          generator.yield((previousUnfinished ? Strings.repeat(INDENT, 2) : "") + line);
          previousUnfinished =
              !inComment() &&
              !line.matches("^.*[;{},\\-/]$") && // Ends with an expected character.
              !line.contains("@Override") &&    // or an annotation.
              !line.isEmpty() &&
              !line.contains("//"); // Single-line comment.
        }
      });
      return this;
    }

    public StyleBuilder addBlankLineBeforeMatching(final String regex) {
      lineStylers.add(new StatefulLineStyler() {
        @Override public void doNext(String line, LineGenerator generator) {
          if ((!inComment() || inStartOfComment()) && line.matches(regex)) {
            generator.yield("");
          }
          generator.yield(line);
        }
      });
      return this;
    }

    public StyleBuilder addBlankLineBeforeClasslikeWithNoPrecedingComment() {
      lineStylers.add(new StatefulLineStyler() {
        @Override public void doNext(String line, LineGenerator generator) {
          if (!previousWasComment()
              && line.matches(".*\\b(class|interface|enum)\\b.*")) {
            generator.yield("");
          }
          generator.yield(line);
        }
      });
      return this;
    }

    public StyleBuilder addBlankLineAfterBraceUnlessInMethod() {
      lineStylers.add(new StatefulLineStyler() {
        // true for every level of braces that is a class-like construct
        ArrayDeque<Boolean> stack = new ArrayDeque<Boolean>();
        boolean sawClasslike = false;

        @Override public void doNext(String line, LineGenerator generator) {
          if (inComment()) {
            generator.yield(line);
          } else if (line.endsWith("}") && !line.contains("{")) {
            generator.yield(line);
            stack.pop();
            if (!stack.isEmpty() && stack.peek()) {
              generator.yield("");
            }
          } else {
            // Perhaps we could match anonymous classes by adding "new" here,
            // but this is not currently needed.
            if (line.matches(".*\\b(class|interface|enum)\\b.*")) {
              sawClasslike = true;
            }
            if (line.endsWith("{")) {
              if (line.contains("}")) {
                stack.pop();
              }
              stack.push(sawClasslike);
              sawClasslike = false;
            } else if (line.endsWith(";")) {
              sawClasslike = false;
            }
            generator.yield(line);
          }
        }
      });
      return this;
    }

    public StyleBuilder addBlankLineAfterMatching(final String regex) {
      lineStylers.add(new StatefulLineStyler() {
        boolean previousLineMatched = false;

        @Override public void doNext(String line, LineGenerator generator) {
          if (previousLineMatched) {
            generator.yield("");
          }
          generator.yield(line);
          previousLineMatched = line.matches(regex);
        }
      });
      return this;
    }

    private boolean containsAtomicToken(String line) {
      for (String token : ATOMIC_TOKENS) {
        if (line.contains(token)) {
          return true;
        }
      }
      return false;
    }
  }

  @Override
  public void style(File f, boolean saveBackup) {
    List<String> lines = null;
    try {
      lines = CharStreams.readLines(new FileReader(f));
    } catch (IOException e) {
      System.err.println("Couldn't find file " + f.getName() + " to style: " + e.getMessage());
      return;
    }

    Joiner newlineJoiner = Joiner.on('\n');

    if (saveBackup) {
      File backup = new File(f.getAbsolutePath() + BACKUP_SUFFIX);
      try {
        Files.write(newlineJoiner.join(lines), backup, Charset.defaultCharset());
      } catch (IOException e) {
        System.err.println("Couldn't write backup " + backup.getName() + ": " + e.getMessage());
        return;
      }
    }

    try {
      Files.write(newlineJoiner.join(styleLines(lines)), f, Charset.defaultCharset());
    } catch (IOException e) {
      System.err.println("Couldn't write styled file " + f.getName() + ": " + e.getMessage());
      return;
    }
  }

  private List<String> styleLines(List<String> lines) {
    return new StyleBuilder()
        .trim()
        .removeRepeatedSpacing()
        .addNewLineBefore('}')
        .addNewLineAfter('{')
        .addNewLineAfter('}')
        .addNewLineAfter(';')
        .trim()
        .removeRepeatedSpacing()
        .stripBlankLines()
        .trim()
        .indentBraces()
        .indentLongComments()
        .addBlankLineBeforeMatching("[ \t]*@Override.*")
        .addBlankLineBeforeMatching(".*/\\*\\*.*")
        .addBlankLineAfterMatching("package.*")
        .addBlankLineBeforeMatching("package.*")
        .addBlankLineBeforeClasslikeWithNoPrecedingComment()
        .addBlankLineAfterBraceUnlessInMethod()
        .stripDuplicateBlankLines()
        .doubleIndentUnfinishedLines()
        .stripInitialBlankLine()
        // TODO: blank line before first method or constructor if that has no javadoc
        .apply(lines);
  }
}
