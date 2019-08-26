/**
 * Copyright (C) 2018 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.build;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenerateRpmScripts {
  private static Pattern includeDirectivePattern = Pattern.compile("^#include\\s+<(.*?)>", Pattern.MULTILINE);

  public static void main(String[] args) throws IOException {
    if (args.length < 3)
      throw new IllegalArgumentException(
          "Need at least three arguments: <includesDir> <destDir> <src>...");
    Path includeFilesDirectory = Paths.get(args[0]);
    Path destinationDirectory = Paths.get(args[1]);
    List<Path> sourceFiles = new ArrayList<>();
    for (int i = 2; i < args.length; ++i) {
      sourceFiles.add(Paths.get(args[i]));
    }
    execute(sourceFiles, includeFilesDirectory, destinationDirectory);
  }

  public static void execute(List<Path> sourceFiles, Path includeFilesDirectory, Path destinationDirectory) throws IOException {
    for (Path sourceFile: sourceFiles) {
      execute(sourceFile, includeFilesDirectory, destinationDirectory);
    }
  }

  public static void execute(Path sourceFile, Path includeFilesDirectory, Path destinationDirectory) throws IOException  {

    String scriptText = "";
    if (!Files.exists(destinationDirectory)) {
      System.out.println("Creating destination dir: " + destinationDirectory.toAbsolutePath());
      Files.createDirectories(destinationDirectory);
    }

    if (Files.exists(sourceFile)) {
      scriptText = new String(Files.readAllBytes(sourceFile), StandardCharsets.UTF_8);
      Matcher m = includeDirectivePattern.matcher(scriptText);
      List<String> includeDirectives = new ArrayList<>();
      List<String> includeFileNames = new ArrayList<>();

      // Find all the include directives.
      while (m.find()) {
        includeDirectives.add(m.group(0));
        includeFileNames.add(m.group(1));
      }

      // Map include directive to replacement text.
      Map<String, String> includeDirectiveToReplacementText = new HashMap<>();
      for (int i = 0; i < includeDirectives.size(); ++i) {
        Path includeSource = includeFilesDirectory.resolve(includeFileNames.get(i));
        String includeText = new String(Files.readAllBytes(includeSource), StandardCharsets.UTF_8);
        includeDirectiveToReplacementText.put(includeDirectives.get(i), includeText);
      }

      // Do the replacements.
      for (String includeDirective: includeDirectiveToReplacementText.keySet()) {
        scriptText = scriptText.replace(includeDirective, includeDirectiveToReplacementText.get(includeDirective));
      }
    } else {
      System.out.println("Source file " + sourceFile.toAbsolutePath() + " not found; creating empty file in destination.");
    }

    // Write the final source text to the destination directory.
    Files.write(destinationDirectory.resolve(sourceFile.getFileName()), scriptText.getBytes(StandardCharsets.UTF_8));
  }
}
