/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2018 Isao Akiyama                                         *
 *                                                                         *
 * Permission is hereby granted, free of charge, to any person obtaining   *
 * a copy of this software and associated documentation files (the         *
 * "Software"), to deal in the Software without restriction, including     *
 * without limitation the rights to use, copy, modify, merge, publish,     *
 * distribute, sublicense, and/or sell copies of the Software, and to      *
 * permit persons to whom the Software is furnished to do so, subject to   *
 * the following conditions:                                               *
 *                                                                         *
 * The above copyright notice and this permission notice shall be          *
 * included in all copies or substantial portions of the Software.         *
 *                                                                         *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,         *
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF      *
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  *
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY    *
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,    *
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE       *
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.                  *
 ***************************************************************************/
package org.sango_lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class FileSystem {
  static final String SOURCEX_FILE_EXT = "sgx";
  static final String SOURCEX_FILE_SUFFIX = "." + SOURCEX_FILE_EXT;
  static final String SOURCE_FILE_EXT = "sg";
  static final String SOURCE_FILE_SUFFIX = "." + SOURCE_FILE_EXT;
  static final String MODULE_FILE_EXT = "sgm";
  static final String MODULE_FILE_SUFFIX = "." + MODULE_FILE_EXT;
  static final String MODULE_ZIP_ENTRY = "module";
  static final String NATIVE_IMPL_PACKAGE_PREFIX = "sni_";
  static final String NATIVE_IMPL_CLASS_PREFIX = "SNI";
  static final String NATIVE_IMPL_METHOD_PREFIX = "sni_";

  private static FileSystem fileSys;

  private FileSystem() {}

  static FileSystem getInstance() {
    if (fileSys == null) {
      fileSys = new FileSystem();
    }
    return fileSys;
  }

  String[] splitModuleNameToDirectoryPathAndFileNameBase(Cstr modName) {
    int sep = modName.lastIndexOf('.');
    String dir;
    String file;
    if (sep >= 0) {
      dir = moduleNameToPath(modName.substring(0, sep));
      file = moduleNameToPath(modName.substring(sep + 1, modName.getLength()));
    } else {
      dir = ".";
      file = moduleNameToPath(modName);
    }
    return new String[] { dir, file };
  }

  private String moduleNameToPath(Cstr modName) {
    return modName.toJavaString().replace('.', File.separatorChar);
  }

  File moduleNameToSourceXFileObj(Cstr modName) throws IOException {
    return new File(moduleNameToPath(modName) + SOURCEX_FILE_SUFFIX).getCanonicalFile();
  }

  File moduleNameToSourceFileObj(Cstr modName) throws IOException {
    return new File(moduleNameToPath(modName) + SOURCE_FILE_SUFFIX).getCanonicalFile();
  }

  File moduleNameToModuleFileObj(Cstr modName) throws IOException {
    return new File(moduleNameToPath(modName) + MODULE_FILE_SUFFIX).getCanonicalFile();
  }

  File moduleNameToModuleFileObj(File root, Cstr modName) throws IOException {
    return new File(root, moduleNameToPath(modName) + MODULE_FILE_SUFFIX).getCanonicalFile();
  }

  Cstr sourceFileObjToModuleName(File src, List<File> pathList) throws IOException {
    String srcPath = src.getCanonicalPath();
    String s = null;
    if (srcPath.endsWith(SOURCEX_FILE_SUFFIX)) {
      s = srcPath.substring(0, srcPath.length() - SOURCEX_FILE_SUFFIX.length());
    } else if (srcPath.endsWith(SOURCE_FILE_SUFFIX)) {
      s = srcPath.substring(0, srcPath.length() - SOURCE_FILE_SUFFIX.length());
    } else {
      throw new IllegalArgumentException("Invalid source file path.");
    }
    Cstr modName = null;
    for (int i = 0; modName == null && i < pathList.size(); i++) {
      String p = pathList.get(i).getCanonicalPath();
      if (s.startsWith(p)) {
        modName = new Cstr(s.substring(p.length() + 1).replace(File.separatorChar, '.'));
      }
    }
    return modName;
  }

  File findSourceFileForModuleName(Cstr modName, List<File> pathList) throws IOException {
    File f = null;
    for (int i = 0; f == null && i < pathList.size(); i++) {
      File ff = new File(pathList.get(i), moduleNameToPath(modName) + SOURCE_FILE_SUFFIX).getCanonicalFile();
      f = ff.exists()? ff: null;
    }
    return f;
  }

  File findModuleFileForModuleName(Cstr modName, List<File> pathList) throws IOException {
    File f = null;
    for (int i = 0; f == null && i < pathList.size(); i++) {
      File ff = new File(pathList.get(i), moduleNameToPath(modName) + MODULE_FILE_SUFFIX).getCanonicalFile();
      f = ff.exists()? ff: null;
    }
    return f;
  }

  File[] findSourceAndModuleFilesForModuleName(Cstr modName, List<File> pathList) throws IOException {
  // [0] source file  [1] module file
    String p = moduleNameToPath(modName);
    File[] sm = null;
    for (int i = 0; sm == null && i < pathList.size(); i++) {
      File d = pathList.get(i);
      File sxf = new File(d, p + SOURCEX_FILE_SUFFIX).getCanonicalFile();
      File sf = new File(d, p + SOURCE_FILE_SUFFIX).getCanonicalFile();
      File mf = new File(d, p + MODULE_FILE_SUFFIX).getCanonicalFile();
      if (sxf.exists()) {
        sm = new File[] { sxf, mf };
      } else if (sf.exists()) {
        sm = new File[] { sf, mf };
      } else if (mf.exists()) {
        sm = new File[] { sxf, mf };
      }
    }
    return sm;
  }

  File moduleFileInSameDirectoryWithSourceFile(File src) throws IOException {
    String sp = src.getPath();
    String mp = null;
    if (sp.endsWith(SOURCEX_FILE_SUFFIX)) {
      mp = sp.substring(0, sp.length() - SOURCEX_FILE_SUFFIX.length()) + MODULE_FILE_SUFFIX;
    } else if (sp.endsWith(SOURCE_FILE_SUFFIX)) {
      mp = sp.substring(0, sp.length() - SOURCE_FILE_SUFFIX.length()) + MODULE_FILE_SUFFIX;
    } else {
      throw new IllegalArgumentException("Not source file.");
    }
    return new File(mp).getCanonicalFile();
  }

  void prepareForFileCreation(File f) throws IOException {
    if (f.isFile()) { return; }
    File p = f.getParentFile();
    p.mkdirs();
  }

  String moduleNameToNativeClass(Cstr modName) {
    int lastDotPos = modName.lastIndexOf('.');
    String s;
    if (lastDotPos >= 0) {
      Cstr p = modName.substring(0, lastDotPos);
      StringBuffer b = new StringBuffer();
      b.append(NATIVE_IMPL_PACKAGE_PREFIX);
      for (int i = 0; i < p.getLength(); i++) {
        int c = p.getCharAt(i);
        b.appendCodePoint(c);
        if (c == '.') {
          b.append(NATIVE_IMPL_PACKAGE_PREFIX);
	}
      }
      b.append('.');
      b.append(NATIVE_IMPL_CLASS_PREFIX);
      b.append(modName.substring(lastDotPos + 1, modName.getLength()).toJavaString());
      s = b.toString();
    } else {
      s = NATIVE_IMPL_CLASS_PREFIX + modName.toJavaString();
    }
    return s;
  }

  String funNameToNativeMethod(String funName) {
    return NATIVE_IMPL_METHOD_PREFIX + funName.replaceAll("\\?", "_Q_").replaceAll("@", "_A_").replaceAll("\\$", "_D_").replaceAll("\'", "_P_");
  }
}
