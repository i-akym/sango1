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
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class Compiler implements PDefDict.DefDictGetter {
  static final int ACTION_IGNORE = 0;
  static final int ACTION_WARN = 1;
  static final int ACTION_ERROR = 2;

  PrintStream msgOut;
  List<String> srcList;
  List<Cstr> importQueue;
  List<ReferEntry> referQueue;
  List<CompileEntry> parse1Queue;
  List<CompileEntry> parse2Queue;
  List<CompileEntry> parse3Queue;
  List<CompileEntry> parse4Queue;
  List<CompileEntry> parse5Queue;
  List<CompileEntry> generateQueue;
  Map<Cstr, Parser> parserDict;
  Map<Cstr, PDefDict> defDictDict;
  List<File> sysLibPathList;
  List<File> userModPathList;
  List<File> modPathList;
  File modOutPath;
  boolean verboseModule;
  boolean compileError;
  int onAvGeneral;
  int onAvAlpha;
  int onAvBeta;
  int onAvLimited;
  int onAvDeprecated;

  public static void main(String[] args) {
    LauncherControl lc = new LauncherControl();
    lc.msgOut = System.out;
    lc.printVersion = true;
    int exitCode = 0;
    try {
      Compiler c = newInstance();
      setup(lc, c, args);
      if (lc.printVersion) { printVersion(lc.msgOut); }
      if (lc.printHelp) { printHelp(lc.msgOut); }
      c.run();
    } catch (AbortException ex) {
      lc.msgOut.println(ex.getMessage());
      exitCode = 1;
    } catch (Exception ex) {
      ex.printStackTrace(lc.msgOut);
      exitCode = 1;
    }
    System.exit(exitCode);
  }

  private static class LauncherControl {
    PrintStream msgOut;
    boolean printVersion;
    boolean printHelp;
  }

  public static Compiler newInstance() {
    return new Compiler();
  }

  public static Version getVersion() { return Version.getInstance(); }

  public PrintStream getMessageOut() {  return this.msgOut; }

  public void setMessageOut(PrintStream o) {
    this.msgOut = o;
  }

  public boolean isVerboseModule() { return this.verboseModule; }

  public void setVerboseModule(boolean sw) {
    this.verboseModule = sw;
  }

  public List<File> getSysLibPaths() { return this.sysLibPathList; }

  public void setSysLibPaths(List<File> paths) {
    this.sysLibPathList = paths;
  }

  public List<File> getUserModPaths() { return this.userModPathList; }

  public void setUserModPaths(List<File> paths) {
    this.userModPathList = paths;
  }

  public File getModOutPath() { return this.modOutPath; }

  public void setModOutPath(File path) {
    this.modOutPath = path;
  }

  public List<String> getSourceFileNames() { return this.srcList; }

  public void setSourceFileNames(List<String> fileNames) {
    this.srcList = fileNames;
  }

  Compiler() {
    this.msgOut = System.out;
    this.importQueue = new ArrayList<Cstr>();
    this.referQueue = new ArrayList<ReferEntry>();
    this.parse1Queue = new ArrayList<CompileEntry>();
    this.parse2Queue = new ArrayList<CompileEntry>();
    this.parse3Queue = new ArrayList<CompileEntry>();
    this.parse4Queue = new ArrayList<CompileEntry>();
    this.parse5Queue = new ArrayList<CompileEntry>();
    this.generateQueue = new ArrayList<CompileEntry>();
    this.parserDict = new HashMap<Cstr, Parser>();
    this.defDictDict = new HashMap<Cstr, PDefDict>();
    this.sysLibPathList = new ArrayList<File>();
    this.sysLibPathList.add(new File("lib"));
    this.userModPathList = new ArrayList<File>();
    this.userModPathList.add(new File("."));
    this.modPathList = new ArrayList<File>();
    this.verboseModule = true;
    this.onAvGeneral = ACTION_IGNORE;
    this.onAvAlpha = ACTION_WARN;
    this.onAvBeta = ACTION_IGNORE;
    this.onAvLimited = ACTION_WARN;
    this.onAvDeprecated = ACTION_WARN;
    this.srcList = new ArrayList<String>();
  }

  static void setup(LauncherControl lc, Compiler c, String[] args) {
    if (args.length <= 2) {  // at least L option is specified
      lc.printHelp = true;
    }
    List<String[]> optList = new ArrayList<String[]>();
    List<String> srcList = new ArrayList<String>();
    String[] opt = null;
    for (int i = 0; i < args.length; i++) {
      String s = args[i];
      if (srcList.size() > 0) {
        srcList.add(s);
      } else if (opt != null) {
        opt[1] = s;
        optList.add(opt);
        opt = null;
      } else if (  // no param needed
          s.equals("-help") || s.equals("-h") || s.equals("-?") ||
          s.equals("-version")) {
        opt = new String[] { s, null };
        optList.add(opt);
        opt = null;
      } else if (  // one param needed
          s.equals("-L") ||
          s.equals("-modules") || s.equals("-m") ||
          s.equals("-out") ||
          s.equals("-verbose") ||
          s.equals("-quiet") ||
          s.equals("-ignore") ||
          s.equals("-warn") ||
          s.equals("-error")) {
        opt = new String[] { s, null };
      } else if (s.startsWith("-")) {
        lc.msgOut.print("Unknown option. ");
        lc.msgOut.println(s);
        System.exit(1);
      } else {
        srcList.add(s);
      }
    }
    if (opt != null) {
      lc.msgOut.print("Parameter missing for option ");
      lc.msgOut.print(opt[0]);
      lc.msgOut.println(".");
      System.exit(1);
    }
    c.setSourceFileNames(srcList);
    processOpts(lc, c, optList);
  }

  static void processOpts(LauncherControl lc, Compiler c, List<String[]> optList) {
    for (int i = 0; i < optList.size(); i++) {
      String[] opt = optList.get(i);
      processOpt(lc, c, opt[0], opt[1]);
    }
  }

  static void processOpt(LauncherControl lc, Compiler c, String optName, String optParam) {
    if (optName.equals("-help") || optName.equals("-h") || optName.equals("-?")) {
      processHelpOpt(lc);
    } else if (optName.equals("-version")) {
      processVersionOpt(lc);
    } else if (optName.equals("-modules") || optName.equals("-m")) {
      processModulesOpt(lc, c, optParam);
    } else if (optName.equals("-out")) {
      processOutOpt(lc, c, optParam);
    } else if (optName.equals("-verbose")) {
      processVerboseOpt(lc, c, optParam);
    } else if (optName.equals("-quiet")) {
      processQuietOpt(lc, c, optParam);
    } else if (optName.equals("-ignore")) {
      processIgnoreOpt(lc, c, optParam);
    } else if (optName.equals("-warn")) {
      processWarnOpt(lc, c, optParam);
    } else if (optName.equals("-error")) {
      processErrorOpt(lc, c, optParam);
    } else if (optName.equals("-L")) {  // system lib path, which is used internally
      processLOpt(lc, c, optParam);
    } else {
      throw new IllegalArgumentException("Unknown option");
    }
  }

  static void processHelpOpt(LauncherControl lc) {
    lc.printHelp = true;
  }

  static void processVersionOpt(LauncherControl lc) {
    lc.printVersion = true;
  }

  static void processLOpt(LauncherControl lc, Compiler c, String optParam) {
    c.setSysLibPaths(parseModulePathList(lc, c, optParam));
  }

  static void processModulesOpt(LauncherControl lc, Compiler c, String optParam) {
    c.setUserModPaths(parseModulePathList(lc, c, optParam));
  }

  static List<File> parseModulePathList(LauncherControl lc, Compiler c, String optParam) {
    List<File> pathList = new ArrayList<File>();
    String[] ps = optParam.split(File.pathSeparator);
    for (int i = 0; i < ps.length; i++) {
      String p = ps[i];
      if (!p.equals("")) {
        File path = new File(p);
        if (!path.isDirectory()) {
          lc.msgOut.print("Path ");
          lc.msgOut.print(p);
          lc.msgOut.println(" is not a directory.");
          System.exit(1);
        }
        pathList.add(path);
      }
    }
    return pathList;
  }

  static void processOutOpt(LauncherControl lc, Compiler c, String optParam) {
    File path = new File(optParam);
    if (!path.isDirectory()) {
      lc.msgOut.print("Path ");
      lc.msgOut.print(optParam);
      lc.msgOut.println(" is not a directory.");
      System.exit(1);
    }
    c.setModOutPath(path);
  }

  static void processVerboseOpt(LauncherControl lc, Compiler c, String optParam) {
    processMessageOptWithParams(lc, c, optParam, true);
  }

  static void processQuietOpt(LauncherControl lc, Compiler c, String optParam) {
    processMessageOptWithParams(lc, c, optParam, false);
  }

  static void processMessageOptWithParams(LauncherControl lc, Compiler c, String optParam, boolean sw) {
    String[] ps = optParam.split("\\+");
    for (int i = 0; i < ps.length; i++) {
      if (ps[i] != null) {
        processMessageOpt(lc, c, ps[i], sw);
      }
    }
  }

  static void processMessageOpt(LauncherControl lc, Compiler c, String param, boolean sw) {
    if (param.equals("help")) {
      lc.printHelp = sw;
    } else if (param.equals("version")) {
      lc.printVersion = sw;
    } else if (param.equals("module")) {
      c.setVerboseModule(sw);
    } else if (param.equals("all")) {
      lc.printHelp = sw;
      lc.printVersion = sw;
      c.setVerboseModule(sw);
    } else {
      lc.msgOut.println("Unknown parameter to -verbose/-quiet option.");
      System.exit(1);
    }
  }

  static void processIgnoreOpt(LauncherControl lc, Compiler c, String optParam) {
    processActionOptWithParams(lc, c, optParam, ACTION_IGNORE);
  }

  static void processWarnOpt(LauncherControl lc, Compiler c, String optParam) {
    processActionOptWithParams(lc, c, optParam, ACTION_WARN);
  }

  static void processErrorOpt(LauncherControl lc, Compiler c, String optParam) {
    processActionOptWithParams(lc, c, optParam, ACTION_ERROR);
  }

  static void processActionOptWithParams(LauncherControl lc, Compiler c, String optParam, int action) {
    String[] ps = optParam.split("\\+");
    for (int i = 0; i < ps.length; i++) {
      if (ps[i] != null) {
        processActionOpt(lc, c, ps[i], action);
      }
    }
  }

  static void processActionOpt(LauncherControl lc, Compiler c, String param, int action) {
    if (param.equals("av-all")) {
      c.onAvGeneral = action;
      c.onAvAlpha = action;
      c.onAvBeta = action;
      c.onAvLimited = action;
      c.onAvDeprecated = action;
    } else if (param.equals("av-general")) {
      c.onAvGeneral = action;
    } else if (param.equals("av-alpha")) {
      c.onAvAlpha = action;
    } else if (param.equals("av-beta")) {
      c.onAvBeta = action;
    } else if (param.equals("av-limited")) {
      c.onAvLimited = action;
    } else if (param.equals("av-deprecated")) {
      c.onAvDeprecated = action;
    } else {
      lc.msgOut.println("Unknown parameter to -ignore/-warn/-error option.");
      System.exit(1);
    }
  }

  void run() throws AbortException {
    for (int i = 0; i < this.userModPathList.size(); i++) {
      this.modPathList.add(this.userModPathList.get(i));
    }
    for (int i = 0; i < this.sysLibPathList.size(); i++) {
      this.modPathList.add(this.sysLibPathList.get(i));
    }

    try {
      for (int i = 0; i < this.srcList.size(); i++) {
        this.preprocessSourceFiles(this.srcList.get(i));
      }
    } catch (IOException ex) {
      throw new AbortException(ex.getMessage());
    }

    this.compile();
    if (this.compileError) {
      throw new AbortException("** One or more errors occurred during compilation.");
    }
  }

  void preprocessSourceFiles(String target) throws IOException {
    if (target.endsWith(FileSystem.SOURCEX_FILE_SUFFIX)) {
      ;
    } else if (target.endsWith(FileSystem.SOURCE_FILE_SUFFIX)) {
      ;
    } else {
      throw new IOException("File " + target + " is not a source.");
    }
    File targetFile = new File(target).getCanonicalFile();
    if (!targetFile.exists()) {
      throw new IOException("Source file " + target + " not found.");
    }
    Cstr targetModName = FileSystem.getInstance().sourceFileObjToModuleName(targetFile, this.modPathList);
    if (targetModName == null) {
      throw new IOException("Cannot identify module name of source file " + targetFile.getCanonicalPath() + ".");
    }
// /* DEBUG */ System.out.println("module name " + targetModName.repr());
    File[] sm = FileSystem.getInstance().findSourceAndModuleFilesForModuleName(targetModName, this.modPathList);
// /* DEBUG */ System.out.println("0 " + sm);
    if (sm == null) {
      throw new IOException("Cannot identify module name of source file " + targetFile.getCanonicalPath() + ".");
    }
// /* DEBUG */ System.out.println("1 " + targetFile.getCanonicalPath());
// /* DEBUG */ System.out.println("2 " + sm[0].getCanonicalPath());
    if (!targetFile.getCanonicalPath().equals(sm[0].getCanonicalPath())) {
      throw new IOException("Source file " + sm[0].getCanonicalPath() + " is shadowed.");
    }
    CompileEntry tce = new CompileEntry(targetModName, targetFile, null);
    tce.complete();
    this.parse1Queue.add(tce);
  }

  void compile() throws AbortException {
    // phase1
    boolean cont = true;
    while (cont) {
      if (!this.parse1Queue.isEmpty()) {
        CompileEntry ce = this.parse1Queue.remove(0);
        if (!this.parserDict.containsKey(ce.modName)) {
          this.parse1(ce);
        }
      } else if (!this.referQueue.isEmpty()) {
        ReferEntry re = this.referQueue.remove(0);
        if (!this.defDictDict.containsKey(re.modName)) {
          this.referMod(re);
        }
      } else if (!this.importQueue.isEmpty()) {
        Cstr modName = this.importQueue.remove(0);
        if (!this.defDictDict.containsKey(modName)
            // && !this.parserDict.containsKey(modName)  // if contained in parserDict, also contained in defDictDict
            && !this.referQueueContains(modName)
            && !this.parse1QueueContains(modName)) {
          try {
            this.determineActionToImport(modName);
          } catch (IOException ex) {
            throw new AbortException(ex.getMessage());
          }
        }
      } else {
        cont = false;
      }
    }

    // phase 2
    while (!this.parse2Queue.isEmpty()) {
      this.parse2(this.parse2Queue.remove(0));
    }

    // phase 3
    while (!this.parse3Queue.isEmpty()) {
      this.parse3(this.parse3Queue.remove(0));
    }

    // phase 4
    while (!this.parse4Queue.isEmpty()) {
      this.parse4(this.parse4Queue.remove(0));
    }

    // phase 5
    while (!this.parse5Queue.isEmpty()) {
      this.parse5(this.parse5Queue.remove(0));
    }

    // generate
    while (!this.generateQueue.isEmpty()) {
      this.generate(this.generateQueue.remove(0));
    }
  }

  void parse1(CompileEntry ce) throws AbortException {
    try {
      StringBuffer emsg;
      if (this.verboseModule) {
        this.msgOut.print("Compiling ");
        this.msgOut.print(ce.modName.repr());
        this.msgOut.print(" = ");
      }
      ce.complete();
      if (this.verboseModule) {
        this.msgOut.print(ce.srcFile.getCanonicalPath());
        this.msgOut.println(" ...");
      }
      Parser p = ce.createParser();
      p.parse1();
      this.parse2Queue.add(ce);
      PDefDict d = p.getDefDict();
      this.parserDict.put(ce.modName, p);
      this.defDictDict.put(ce.modName, d);
      Cstr[] ms = d.getForeignMods();
      for (int i = 0; i < ms.length; i++) {
        this.importQueue.add(ms[i]);
      }
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void parse2(CompileEntry ce) throws AbortException {
    try {
      this.parserDict.get(ce.modName).parse2();
      this.parse3Queue.add(ce);
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void parse3(CompileEntry ce) throws AbortException {
    try {
      this.parserDict.get(ce.modName).parse3();
      this.parse4Queue.add(ce);
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void parse4(CompileEntry ce) throws AbortException {
    try {
      this.parserDict.get(ce.modName).parse4();
      this.parse5Queue.add(ce);
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void parse5(CompileEntry ce) throws AbortException {
    try {
      this.parserDict.get(ce.modName).parse5();
      this.generateQueue.add(ce);
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void generate(CompileEntry ce) throws AbortException {
    try {
      Generator g = new Generator(this, ce);
      g.generate();
    } catch (Exception ex) {
      this.compileErrorDetected(ce.modName, ex);
    }
  }

  void referMod(ReferEntry re) throws AbortException {
    try {
      this.performReferMod(re);
    } catch (Exception ex) {
      this.importErrorDetected(re.modName, ex);
    }
  }

  void performReferMod(ReferEntry re) throws IOException, FormatException {
    StringBuffer emsg;
    if (this.verboseModule) {
      this.msgOut.print("Importing ");
      this.msgOut.print(re.modName.repr());
      this.msgOut.print(" = ");
      this.msgOut.print(re.file.getCanonicalPath());
      this.msgOut.println(" ...");
    }
    PDefDict d = null;
    ZipInputStream zis = null;
    try {
      zis = new ZipInputStream(new FileInputStream(re.file));
      ZipEntry ze = zis.getNextEntry();
      if (ze == null || !ze.getName().equals(FileSystem.MODULE_ZIP_ENTRY)) {
        emsg = new StringBuffer();
        emsg.append("Module file format error. - ");
        emsg.append(re.file.getCanonicalPath());
        throw new FormatException(emsg.toString());
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(zis);
      d = PCompiledModule.create(this, Module.internalize(doc, re.modName));
    } catch (SAXException ex) {
      emsg = new StringBuffer();
      emsg.append("Module file format error. - ");
      emsg.append(re.file.getCanonicalPath());
      throw new FormatException(emsg.toString());
    } catch (ParserConfigurationException ex) {
      this.msgOut.println("Parser configuration error.");
      this.msgOut.println(ex);
      System.exit(1);
    } finally {
      if (zis != null) {
        try {
          zis.closeEntry();
        } catch (Exception ex) {}
        try {
          zis.close();
        } catch (Exception ex) {}
      }
    }
    this.defDictDict.put(re.modName, d);
  }

  void determineActionToImport(Cstr modName) throws IOException {
    File[] sm = FileSystem.getInstance().findSourceAndModuleFilesForModuleName(modName, this.modPathList);
    if (sm == null) {
      throw new IOException("Neither source file nor module found: " + modName.repr());
    }
    if (!sm[0].exists()) {  // only module file exists
      this.referQueue.add(new ReferEntry(modName, sm[1]));
    } else if (!sm[1].exists()) {  // only source file exists
      if (!this.parse1Queue.contains(sm[0])) {
        CompileEntry ce = new CompileEntry(modName, sm[0],null);
        ce.complete();
        this.parse1Queue.add(ce);
      }
    } else {
      long srcFileTime = sm[0].lastModified();
      long modFileTime = sm[1].lastModified();
      if (modFileTime > srcFileTime && !this.referQueueContains(modName)) {
        this.referQueue.add(new ReferEntry(modName, sm[1]));
      } else if (modFileTime <= srcFileTime && !this.parse1QueueContains(sm[0])) {
        CompileEntry ce = new CompileEntry(modName, sm[0], null);
        ce.complete();
        this.parse1Queue.add(ce);
      }
    }
  }

  public PDefDict getReferredDefDict(Cstr modName) throws CompileException {
    PDefDict d =  this.defDictDict.get(modName);
    if (d == null) {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Unavailable module. - ");
      emsg.append(modName.repr());
      throw new CompileException(emsg.toString());
    }
    return d;
  }

  void compileErrorDetected(Cstr modName, Exception ex) throws AbortException {
    if (ex instanceof RuntimeException) {
      throw (RuntimeException)ex;  // maybe internal bug
    } else {
      this.msgOut.println("** Compile error in " + modName.repr() + ".");
      this.msgOut.println(ex.getMessage());
      this.parserDict.remove(modName);
      this.defDictDict.remove(modName);
      this.compileError = true;
    }
  }

  void importErrorDetected(Cstr modName, Exception ex) throws AbortException {
    if (ex instanceof RuntimeException) {
      throw (RuntimeException)ex;  // maybe internal bug
    } else {
      this.msgOut.println("** Import error in " + modName.repr() + ".");
      this.msgOut.println(ex.getMessage());
      this.defDictDict.remove(modName);
      this.compileError = true;
    }
  }

  void handleFunAvailability(Cstr referrerModName, Cstr referredModName, String id, int featureAv)
      throws CompileException {
    Parser p = this.parserDict.get(referredModName);
    if (p != null) {  // should be not null...
      switch (this.decideAvailabilityAction(p.mod.availability, featureAv)) {
      case ACTION_IGNORE: break;
      case ACTION_WARN:
        this.msgOut.print("Warning: ");
        this.msgOut.println(funAvailabilityMsg(
            referrerModName,
            referredModName,p. mod.availability,
            id, featureAv));
        break;
      case ACTION_ERROR:
        this.msgOut.print("Error: ");
        this.msgOut.println(funAvailabilityMsg(
            referrerModName,
            referredModName,p. mod.availability,
            id, featureAv));
        this.compileError = true;
        throw new CompileException("Availability error.");
      }
    }
  }

  static String funAvailabilityMsg(Cstr referrerModName, Cstr referredModName, int modAv, String id, int featureAv) {
    String ma = (modAv == Module.AVAILABILITY_GENERAL)? "": "[" + availabilityRepr(modAv) + "]";
    String fa = (featureAv == Module.AVAILABILITY_GENERAL)? "": "[" + availabilityRepr(featureAv) + "]";
    return
      referrerModName.repr() + " refers function "
      + "\"" + id + "\"" + fa
      + " in " + referredModName.repr() + ma + ".";
  }

  void handleTypeAvailability(Cstr referrerModName, Cstr referredModName, String id, int featureAv)
      throws CompileException {
    Parser p = this.parserDict.get(referredModName);
    if (p != null) {  // should be not null...
      switch (this.decideAvailabilityAction(p.mod.availability, featureAv)) {
      case ACTION_IGNORE: break;
      case ACTION_WARN:
        this.msgOut.print("Warning: ");
        this.msgOut.println(typeAvailabilityMsg(
            referrerModName,
            referredModName,p. mod.availability,
            id, featureAv));
        break;
      case ACTION_ERROR:
        this.msgOut.print("Error: ");
        this.msgOut.println(typeAvailabilityMsg(
            referrerModName,
            referredModName,p. mod.availability,
            id, featureAv));
        this.compileError = true;
        throw new CompileException("Availability error.");
      }
    }
  }

  static String typeAvailabilityMsg(Cstr referrerModName, Cstr referredModName, int modAv, String id, int featureAv) {
    String ma = (modAv == Module.AVAILABILITY_GENERAL)? "": "[" + availabilityRepr(modAv) + "]";
    String fa = (featureAv == Module.AVAILABILITY_GENERAL)? "": "[" + availabilityRepr(featureAv) + "]";
    return
      referrerModName.repr() + " refers type "
      + "\"" + id + "\"" + fa
      + " in " + referredModName.repr() + ma + ".";
  }

  int decideAvailabilityAction(int modAv, int featureAv) {
    int ma = this.availabilityAction(modAv);
    int fa = this.availabilityAction(featureAv);
    return (ma > fa)? ma: fa;
  }

  int availabilityAction(int availability) {
    int a;
    switch (availability) {
    case Module.AVAILABILITY_GENERAL: a = this.onAvGeneral; break;
    case Module.AVAILABILITY_ALPHA: a = this.onAvAlpha; break;
    case Module.AVAILABILITY_BETA: a = this.onAvBeta; break;
    case Module.AVAILABILITY_LIMITED: a = this.onAvLimited; break;
    case Module.AVAILABILITY_DEPRECATED: a = this.onAvDeprecated; break;
    default: throw new IllegalArgumentException("Invalid availability.");
    }
    return a;
  }

  static String availabilityRepr(int availability) {
    String s;
    switch (availability) {
    case Module.AVAILABILITY_GENERAL: s = "General"; break;
    case Module.AVAILABILITY_ALPHA: s = "Alpha"; break;
    case Module.AVAILABILITY_BETA: s = "Beta"; break;
    case Module.AVAILABILITY_LIMITED: s = "Limited"; break;
    case Module.AVAILABILITY_DEPRECATED: s = "Deprecated"; break;
    default: throw new IllegalArgumentException("Invalid availability.");
    }
    return s;
  }

  class CompileEntry {
    Cstr modName;
    File srcFile;
    File modFile;

    CompileEntry(Cstr modName, File srcFile, File modFile) {
      this.modName = modName;
      this.srcFile = srcFile;
      this.modFile = modFile;
    }

    Parser createParser() throws CompileException, IOException {
      return srcFile.getCanonicalPath().endsWith(FileSystem.SOURCE_FILE_SUFFIX)? 
        new ParserA(Compiler.this, this):
        new ParserB(Compiler.this, this);
    }

    void complete() throws IOException {
      StringBuffer emsg;
      if (this.srcFile == null) {
        this.srcFile = FileSystem.getInstance().findSourceFileForModuleName(this.modName, Compiler.this.modPathList);
        if (this.srcFile == null) {
          emsg = new StringBuffer();
          emsg.append("Source file not found. - ");
          emsg.append(modName.repr());
          throw new IOException(emsg.toString());
        }
      }
      if (this.modFile == null) {
        this.modFile = (Compiler.this.modOutPath != null)?
          FileSystem.getInstance().moduleNameToModuleFileObj(Compiler.this.modOutPath, this.modName):
          FileSystem.getInstance().moduleFileInSameDirectoryWithSourceFile(this.srcFile);
      }
    }
  }

  private class ReferEntry {
    Cstr modName;
    File file;

    ReferEntry(Cstr modName, File modFile) {
      this.modName = modName;
      this.file = modFile;
    }
  }

  boolean parse1QueueContains(Cstr modName) {
    boolean b = false;
    for (int i = 0; !b && i < this.parse1Queue.size(); i++) {
      b = modName.equals(parse1Queue.get(i).modName);
    }
    return b;
  }

  boolean parse1QueueContains(File file) {  // file must be a canonical file
    boolean b = false;
    for (int i = 0; !b && i < this.parse1Queue.size(); i++) {
      b = file.getPath().equals(parse1Queue.get(i).srcFile.getPath());
    }
    return b;
  }

  boolean referQueueContains(Cstr modName) {
    boolean b = false;
    for (int i = 0; !b && i < this.referQueue.size(); i++) {
      b = modName.equals(referQueue.get(i).modName);
    }
    return b;
  }

  static void printVersion(PrintStream out) {
    out.println("Sango " + Version.getInstance().full);
  }

  static void printHelp(PrintStream out) {
    out.println();
    out.println("Usage");
    out.println("  sangoc [option...] source-file...");
    out.println();
    out.println("Option");
    out.println("  -h, -help, -? : Print this message. Same as '-verbose help'.");
    out.println("  -version : Print version. Same as '-verbose version'.");
    out.println("  -modules <paths> : Use specified module root paths.");
    out.println("  -m <paths> : Same as above.");
    out.println("  -out <path> : Generate module files to <path> directory.");
    out.println("  -verbose <switches1> : Show detail messages.");
    out.println("  -quiet <switches1> : Suppress messages.");
    out.println("  -warn <switches2> : Display if detected.");
    out.println("  -error <switches2> : Handle as an error if detected.");
    out.println("  -ignore <switches2> : Clear -warn/-error switches.");
    out.println();
    out.println("  <switches1> -- one or more switches concatenated with '+'. Switches are...");
    out.println("    help : help message");
    out.println("    version : version information");
    out.println("    module : actions to module");
    out.println("    all : all switches above");
    out.println("  <switches2> -- one or more switches concatenated with '+'. Switches are...");
    out.println("    av-general : availability - general");
    out.println("    av-alpha : availability - alpha");
    out.println("    av-beta : availability - beta");
    out.println("    av-limited : availability - limited");
    out.println("    av-deprecated : availability - deprecated");
    out.println("    av-all : availability - all states");
    out.println();
    out.println("Default options");
    out.println("  -m . -verbose version+module -warn av-alpha+av-limited+av-deprecated");
    out.println();
  }
}
