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

package org.waveprotocol.pst;

import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.protobuf.Descriptors.FileDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A loader for a {@link FileDescriptor}, accepting and handling a proto file
 * specified as:
 * <ul>
 * <li>A path to a .proto file.</li>
 * <li>A path to a .java (protoc-)compiled proto spec.</li>
 * <li>A path to a .class (javac-) compiled proto spec.</li>
 * <li>A proto spec that is already on the classpath.</li>
 * </ul>
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
public final class PstFileDescriptor {

  private final FileDescriptor descriptor;

  /**
   * Loads the {@link FileDescriptor} from a path. The path may be a class name
   * (e.g. foo.Bar), path to a class (e.g. bin/foo/Bar.class), or a path to a
   * Java source file (e.g. src/foo/Bar.java).
   *
   * In each case it is the caller's responsibility to ensure that the classpath
   * of the Java runtime is correct.
   *
   * @param path the path to load the proto description from
   * @param saveintermediateJavaDir path to save the intermediate protoc-
   *        generated Java file (if any)
   * @param protoPath any additional path to pass to the protoc compiler
   */
  public static FileDescriptor load(String path, File intermediateJavaDir, File protoPath) {
    return new PstFileDescriptor(path, intermediateJavaDir, protoPath).get();
  }

  private  PstFileDescriptor(String path, File intermediateJavaDir, File protoPath) {
    Class<?> clazz = null;
    if (path.endsWith(".class")) {
      clazz = fromPathToClass(path);
    } else if (path.endsWith(".java")) {
      clazz = fromPathToJava(path);
    } else if (path.endsWith(".proto")) {
      clazz = fromPathToProto(path, intermediateJavaDir, protoPath);
    } else {
      clazz = fromClassName(path);
    }

    if (clazz == null) {
      descriptor = null;
    } else {
      descriptor = asFileDescriptor(clazz);
    }
  }

  private FileDescriptor get() {
    return descriptor;
  }

  private Class<?> fromClassName(String className) {
    try {
      return Class.forName(className);
    } catch (ClassNotFoundException e) {
      return null;
    }
  }

  private Class<?> fromPathToClass(String pathToClass) {
    String currentBaseDir = new File(pathToClass).isAbsolute() ? "" : ".";
    String currentPath = pathToClass;
    Class<?> clazz = null;
    while (clazz == null) {
      clazz = loadClassAtPath(currentBaseDir, currentPath);
      if (clazz == null) {
        int indexOfSep = currentPath.indexOf(File.separatorChar);
        if (indexOfSep == -1) {
          break;
        } else {
          currentBaseDir += File.separator + currentPath.substring(0, indexOfSep);
          currentPath = currentPath.substring(indexOfSep + 1);
        }
      }
    }
    return clazz;
  }

  private Class<?> loadClassAtPath(String baseDir, String path) {
    try {
      ClassLoader classLoader = new URLClassLoader(new URL[] {new File(baseDir).toURI().toURL()});
      return classLoader.loadClass(getBinaryName(path));
    } catch (Throwable t) {
      return null;
    }
  }

  private String getBinaryName(String path) {
    return path.replace(File.separatorChar, '.').substring(0, path.length() - ".class".length());
  }

  private Class<?> fromPathToJava(String pathToJava) {
    try {
      File dir = Files.createTempDir();
      String[] javacCommand = new String[] {
          "javac", pathToJava, "-d", dir.getAbsolutePath(), "-verbose",
          "-cp", determineClasspath(pathToJava) + ":" + determineSystemClasspath()
      };
      Process javac = Runtime.getRuntime().exec(javacCommand);
      consumeStdOut(javac);
      List<String> stdErr = readLines(javac.getErrorStream());
      int exitCode = javac.waitFor();
      if (exitCode != 0) {
        // Couldn't compile the file.
        System.err.printf("ERROR: running \"%s\" failed (%s):",
            Joiner.on(' ').join(javacCommand), exitCode);
        for (String line : stdErr) {
          System.err.println(line);
        }
        return null;
      } else {
        // Compiled the file!  Now to determine where javac put it.
        Pattern pattern = Pattern.compile("\\[wrote ([^\\]]*)\\]");
        String pathToClass = null;
        for (String line : stdErr) {
          Matcher lineMatcher = pattern.matcher(line);
          if (lineMatcher.matches()) {
            pathToClass = lineMatcher.group(1);
            // NOTE: don't break, as the correct path is the last one matched.
          }
        }
        if (pathToClass != null) {
          return fromPathToClass(pathToClass);
        } else {
          System.err.println("WARNING: couldn't find javac output from javac " + pathToJava);
          return null;
        }
      }
    } catch (Exception e) {
      System.err.println("WARNING: exception while processing " + pathToJava + ": "
          + e.getMessage());
      return null;
    }
  }

  /**
   * Fires off a background thread to consume anything written to a process'
   * standard output. Without running this, a process that outputs too much data
   * will block.
   */
  private void consumeStdOut(Process p) {
    final InputStream o = p.getInputStream();
    Thread t = new Thread() {
      @Override
      public void run() {
        try {
          while (o.read() != -1) {}
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };
    t.setDaemon(true);
    t.start();
  }

  private String determineClasspath(String pathToJava) {
    // Try to determine the classpath component of a path by looking at the
    // path components.
    StringBuilder classpath = new StringBuilder();
    if (new File(pathToJava).isAbsolute()) {
      classpath.append(File.separator);
    }

    // This is just silly, but it will get by for now.
    for (String component : pathToJava.split(File.separator)) {
      if (component.equals("org")
          || component.equals("com")
          || component.equals("au")) {
        return classpath.toString();
      } else {
        classpath.append(component + File.separator);
      }
    }

    System.err.println("WARNING: couldn't determine classpath for " + pathToJava);
    return ".";
  }

  private String determineSystemClasspath() {
    StringBuilder s = new StringBuilder();
    boolean needsColon = false;
    for (URL url : ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()) {
      if (needsColon) {
        s.append(':');
      }
      s.append(url.getPath());
      needsColon = true;
    }
    return s.toString();
  }

  private Class<?> fromPathToProto(String pathToProto, File intermediateJavaDir, File protoPath) {
    try {
      intermediateJavaDir.mkdirs();
      File proto = new File(pathToProto);
      String[] protocCommand = new String[] {
          "protoc", tryGetRelativePath(proto),
          "-I" + protoPath.getPath(),
          "--java_out", intermediateJavaDir.getAbsolutePath()
      };
      Process protoc = Runtime.getRuntime().exec(protocCommand);
      // TODO(ben): configure timeout?
      killProcessAfter(10, TimeUnit.SECONDS, protoc);
      int exitCode = protoc.waitFor();
      if (exitCode != 0) {
        // Couldn't compile the file.
        System.err.printf("ERROR: running \"%s\" failed (%s):",
            Joiner.on(' ').join(protocCommand), exitCode);
        for (String line : readLines(protoc.getErrorStream())) {
          System.err.println(line);
        }
        return null;
      } else {
        final String javaFileName = capitalize(stripSuffix(".proto", proto.getName())) + ".java";
        String maybeJavaFilePath = find(intermediateJavaDir, new Predicate<File>() {
          @Override public boolean apply(File f) {
            return f.getName().equals(javaFileName);
          }
        });
        if (maybeJavaFilePath == null) {
          System.err.println("ERROR: couldn't find result of protoc in " + intermediateJavaDir);
          return null;
        }
        return fromPathToJava(maybeJavaFilePath);
      }
    } catch (Exception e) {
      System.err.println("WARNING: exception while processing " + pathToProto + ": "
          + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  private String find(File dir, Predicate<File> predicate) {
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        String path = find(file, predicate);
        if (path != null) {
          return path;
        }
      }
      if (predicate.apply(file)) {
        return file.getAbsolutePath();
      }
    }
    return null;
  }

  private String tryGetRelativePath(File file) {
    String pwd = System.getProperty("user.dir");
    return stripPrefix(pwd + File.separator, file.getAbsolutePath());
  }

  private String stripPrefix(String prefix, String s) {
    return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
  }

  private String stripSuffix(String suffix, String s) {
    return s.endsWith(suffix) ? s.substring(0, s.length() - suffix.length()) : s;
  }

  private String capitalize(String s) {
    return Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  private List<String> readLines(InputStream is) {
    try {
      return CharStreams.readLines(new InputStreamReader(is));
    } catch (IOException e) {
     e.printStackTrace();
      // TODO(kalman): this is a bit hacky, deal with it properly.
      return Collections.singletonList("(Error, couldn't read lines from the input stream. " +
          "Try running the command external to PST to view the output.)");
    }
  }

  private FileDescriptor asFileDescriptor(Class<?> clazz) {
    try {
      Method method = clazz.getMethod("getDescriptor");
      return (FileDescriptor) method.invoke(null);
    } catch (Exception e) {
      return null;
    }
  }

  private void killProcessAfter(final long delay, final TimeUnit unit, final Process process) {
    Thread processKiller = new Thread() {
      @Override public void run() {
        try {
          Thread.sleep(unit.toMillis(delay));
          process.destroy();
        } catch (InterruptedException e) {
        }
      }
    };
    processKiller.setDaemon(true);
    processKiller.start();
  }
}
