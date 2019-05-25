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
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.List;
import java.util.Locale;

public class RuntimeEngine {
  static final int RUNNING = 0;
  static final int SHUTDOWN = 1;

  PrintStream msgOut;
  Locale locale;
  Cstr prog;
  List<Cstr> argList;
  List<File> sysLibPathList;
  List<File> userModPathList;
  List<File> modPathList;
  RMemMgr memMgr;
  RModMgr modMgr;
  RTaskMgr taskMgr;
  List<SysTask> sysTaskList;
  Timer timer;
  boolean verboseModule;
  End end;
  boolean sysWorkerStop;
  RClientHelper clientHelper;

  public static void main(String[] args) {
    LauncherControl lc = new LauncherControl();
    lc.msgOut = System.err;
    lc.printVersion = true;
    int exitCode = 0;
    try {
      RuntimeEngine e = newInstance();
      setup(lc, e, args);
      if (lc.printVersion) { printVersion(lc.msgOut); }
      if (lc.printHelp) { printHelp(lc.msgOut); }
      exitCode = e.run();
    } catch (AbortException ex) {
      lc.msgOut.println(ex.getMessage());
      exitCode = 1;
    } catch (Throwable th) {
      th.printStackTrace(lc.msgOut);
      exitCode = 1;
    }
    System.exit(exitCode);
  }

  private static class LauncherControl {
    PrintStream msgOut;
    boolean printVersion;
    boolean printHelp;
  }

  public static RuntimeEngine newInstance() {
    return new RuntimeEngine();
  }

  public static Version getVersion() { return Version.getInstance(); }

  public PrintStream getMessageOut() {  return this.msgOut; }

  public void setMessageOut(PrintStream o) {
    this.msgOut = o;
  }

  public Locale getLocale() { return this.locale; }

  public void setLocale(Locale lcl) {
    this.locale = lcl;
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

  public Cstr getProgName() { return this.prog; }

  public void setProgName(Cstr prog) {
    this.prog = prog;
  }

  public List<Cstr> getArgs() { return this.argList; }

  public void setArgs(List<Cstr> args) {
    this.argList = args;
  }

  public RClientHelper getClientHelper() { return this.clientHelper; }

  RuntimeEngine() {
    this.msgOut = System.err;
    this.locale = Locale.getDefault();
    this.memMgr = new RMemMgr(this);
    this.modMgr = new RModMgr(this);
    this.taskMgr = new RTaskMgr(this);
    this.sysTaskList = new ArrayList<SysTask>();
    this.timer = new Timer();
    this.sysLibPathList = new ArrayList<File>();
    this.sysLibPathList.add(new File("lib"));
    this.userModPathList = new ArrayList<File>();
    this.userModPathList.add(new File("."));
    this.argList = new ArrayList<Cstr>();
    this.verboseModule = true;
  }

  public int run() throws AbortException {
    if (this.prog == null) { return 0; }

    this.modPathList = new ArrayList<File>();
    for (int i = 0; i < this.userModPathList.size(); i++) {
      this.modPathList.add(this.userModPathList.get(i));
    }
    for (int i = 0; i < this.sysLibPathList.size(); i++) {
      this.modPathList.add(this.sysLibPathList.get(i));
    }
    try {
      this.modMgr.loadMain(this.prog);
      RModule main = this.modMgr.getMainRMod();
      RClosureItem c = main.getMainClosure();
      if (c == null) {
        throw new AbortException("_main_ not found.");
      }
      RTaskControl i = main.getInitTask();
      RTaskControl m = this.taskMgr.createTask(RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, c);
      this.taskMgr.terminateOnAbnormalEnd(m);
      if (i != null) {
        m.startWaitingFor(i);
      } else {
        m.start();
      }
      this.clientHelper = new RClientHelper(this);
      this.taskMgr.start();
      this.sysWork();
    } catch (IOException ex) {
      throw new AbortException(ex.getMessage());
    } catch (FormatException ex) {
      throw new AbortException(ex.getMessage());
    }
    return this.end.exit();
  }

  static void setup(LauncherControl lc, RuntimeEngine e, String[] args) {
    if (args.length <= 2) {  // at least L option is specified
      lc.printHelp = true;
    }
    List<String[]> optList = new ArrayList<String[]>();
    String prog = null;
    List<Cstr> argList = new ArrayList<Cstr>();
    String[] opt = null;
    for (int i = 0; i < args.length; i++) {
      String s = args[i];
      if (prog != null) {  // after prog
        argList.add(new Cstr(s));
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
          s.equals("-verbose") ||
          s.equals("-quiet")) {
        opt = new String[] { s, null };
      } else if (s.startsWith("-")) {
        lc.msgOut.print("Unknown option. ");
        lc.msgOut.println(s);
        System.exit(1);
      } else {
        prog = s;
      }
    }
    if (opt != null) {
      lc.msgOut.print("Parameter missing for option ");
      lc.msgOut.print(opt[0]);
      lc.msgOut.println(".");
      System.exit(1);
    }
    e.setProgName((prog != null)? new Cstr(prog): null);
    e.setArgs(argList);
    processOpts(lc, e, optList);
  }

  static void processOpts(LauncherControl lc, RuntimeEngine e, List<String[]> optList) {
    for (int i = 0; i < optList.size(); i++) {
      String[] opt = optList.get(i);
      processOpt(lc, e, opt[0], opt[1]);
    }
  }

  static void processOpt(LauncherControl lc, RuntimeEngine e, String optName, String optParam) {
    if (optName.equals("-help") || optName.equals("-h") || optName.equals("-?")) {
      processHelpOpt(lc);
    } else if (optName.equals("-version")) {
      processVersionOpt(lc);
    } else if (optName.equals("-modules") || optName.equals("-m")) {
      processModulesOpt(lc, e, optParam);
    } else if (optName.equals("-verbose")) {
      processVerboseOpt(lc, e, optParam);
    } else if (optName.equals("-quiet")) {
      processQuietOpt(lc, e, optParam);
    } else if (optName.equals("-L")) {  // system lib path, which is used internally
      processLOpt(lc, e, optParam);
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

  static void processLOpt(LauncherControl lc, RuntimeEngine e, String optParam) {
    e.setSysLibPaths(parseModulePathList(lc, optParam));
  }

  static void processModulesOpt(LauncherControl lc, RuntimeEngine e, String optParam) {
    e.setUserModPaths(parseModulePathList(lc, optParam));
  }

  static List<File> parseModulePathList(LauncherControl lc, String optParam) {
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

  static void processVerboseOpt(LauncherControl lc, RuntimeEngine e, String optParam) {
    processMessageOptWithParams(lc, e, optParam, true);
  }

  static void processQuietOpt(LauncherControl lc, RuntimeEngine e, String optParam) {
    processMessageOptWithParams(lc, e, optParam, false);
  }

  static void processMessageOptWithParams(LauncherControl lc, RuntimeEngine e, String optParam, boolean sw) {
    String[] ps = optParam.split("\\+");
    for (int i = 0; i < ps.length; i++) {
      if (ps[i] != null) {
        processMessageOpt(lc, e, ps[i], sw);
      }
    }
  }

  static void processMessageOpt(LauncherControl lc, RuntimeEngine e, String param, boolean sw) {
    if (param.equals("help")) {
      lc.printHelp = sw;
    } else if (param.equals("version")) {
      lc.printVersion = sw;
    } else if (param.equals("module")) {
      e.setVerboseModule(sw);
    } else if (param.equals("all")) {
      lc.printHelp = sw;
      lc.printVersion = sw;
      e.setVerboseModule(sw);
    } else {
      lc.msgOut.println("Unknown parameter to -verbose/-quiet option.");
      System.exit(1);
    }
  }

  private void sysWork() {
    while (!this.sysWorkerStop) {
      SysTask t = this.getSysTask();
      t.run((this.end == null)? RUNNING: SHUTDOWN);
    }
  }

  void scheduleSysTask(SysTask t) {
    synchronized (this.sysTaskList) {
      this.sysTaskList.add(t);
      this.sysTaskList.notify();
    }
  }

  private SysTask getSysTask() {
    SysTask t = null;
    while (t == null) {
      synchronized (this.sysTaskList) {
        if (this.sysTaskList.size() > 0) {
          t = this.sysTaskList.remove(0);
        } else {
          try {
            this.sysTaskList.wait();
          } catch (InterruptedException ex) {}
        }
      }
    }
    return t;
  }

  abstract class End {
    abstract int exit() throws AbortException;
  }

  class NormalEnd extends End {
    int exitCode;

    NormalEnd(int exitCode) {
      this.exitCode = exitCode;
    }

    int exit() { return this.exitCode; }
  }

  class Abort extends End {
    String message;

    Abort(String message) {
      this.message = message;
    }

    int exit() throws AbortException {
      throw new AbortException(this.message);
    }
  }

  void monitoredActorAborted(RStructItem exc) {
    this.scheduleSysTask(new TerminateTask(exc));
    return;
  }

  void requestShutdown(int exitCode, int timeout) {
    this.scheduleSysTask(new ShutdownTask(exitCode, timeout));
    return;
  }

  void requestExit() {
    this.scheduleSysTask(new ExitTask());
    return;
  }

  void requestHalt(String message) {
    this.scheduleSysTask(new HaltTask(message));
    return;
  }

  public static void printException(PrintStream ps, RStructItem exc) {
    RStructItem e = exc;
    String causedBy = null;
    while (e != null) {
      if (causedBy != null) {
        ps.println(causedBy);
      }
      ps.print("Exception  ");
      RStructItem desc = (RStructItem)e.getFieldAt(0);
      ps.print(desc.debugRepr().toJavaString());
      ps.print("  ");
      RArrayItem msg = (RArrayItem)e.getFieldAt(1);
      ps.println(RMemMgr.arrayItemToCstr(msg).toJavaString());
      printExcInfo(ps, (RExcInfoItem)e.getFieldAt(2));

      RStructItem maybeOrg = (RStructItem)e.getFieldAt(3);
      e = (maybeOrg.getFieldCount() > 0)? (RStructItem)maybeOrg.getFieldAt(0): null;  // if e value$, print org exception
      causedBy = "Caused by...";
    }
  }

  public static void printExcInfo(PrintStream ps, RExcInfoItem excInfo) {
    List<RExcInfoItem.FrameSnapshot> frameList = excInfo.getCallStack();
    ps.println("Call stack:");
    for (int i = 0; i < frameList.size(); i++) {
      printFrame(ps, frameList.get(i));
    }
  }

  public static void printFrame(PrintStream ps, RExcInfoItem.FrameSnapshot frame) {
    if (frame.transferred) {
      ps.print("  * ");
    } else {
      ps.print("    ");
    }
    ps.print(frame.impl.getModule().getName().repr());
    ps.print(".");
    ps.print(frame.impl.getName());
    String loc = frame.impl.getSrcLoc(frame.codeIndex);
    if (loc != null) {
      ps.print(" at ");
      ps.print(loc);
    }
    ps.println();
  }

  interface SysTask {
    void run(int runState) ;
  }

  private class ShutdownTask implements SysTask {
    int code;
    int timeout;

    ShutdownTask(int code, int timeout) {
      this.code = code;
      this.timeout = timeout;
    }

    public void run(int runState) {
// /* DEBUG */ System.out.println("ShutdownTask started.");
      if (RuntimeEngine.this.end == null) {
        RuntimeEngine.this.end = new NormalEnd(this.code);
        RuntimeEngine.this.timer.schedule(new ExitTask(), this.timeout);
      }
// /* DEBUG */ System.out.println("ShutdownTask ended.");
    }
  }

  private class TerminateTask extends ShutdownTask {
    RStructItem exc;

    TerminateTask(RStructItem exc) {
      super(/* dummy */ 0, 1000);
      this.exc = exc;
    }
  
    public void run(int runState) {
      RuntimeEngine.this.msgOut.println("Detected actor's abnormal end.");
      printException(RuntimeEngine.this.msgOut, this.exc);
      if (RuntimeEngine.this.end == null) {
        RuntimeEngine.this.end = new Abort("Aborted due to actor's abnormal end.");
      }
      super.run(runState);
    }
  }

  private class ExitTask extends TimerTask implements SysTask {
    public void run(int runState) {
      RuntimeEngine.this.sysWorkerStop = true;
      RuntimeEngine.this.taskMgr.abort();
    }

    public void run() {  // on shutdown timer expired
      RuntimeEngine.this.sysWorkerStop = true;
      RuntimeEngine.this.taskMgr.abort();
    }
  }

  private class HaltTask implements SysTask {
    String message;

    HaltTask(String message) {
      this.message = message;
    }

    public void run(int runState) {
      if (RuntimeEngine.this.end == null) {
        RuntimeEngine.this.end = new Abort(this.message);
      }
      RuntimeEngine.this.sysWorkerStop = true;
      RuntimeEngine.this.taskMgr.abort();
    }
  }

  static void printVersion(PrintStream out) {
    out.println("Sango " + getVersion().full);
  }

  static void printHelp(PrintStream out) {
    out.println();
    out.println("Usage");
    out.println("  sango [option...] program-module-name [arg...]");
    out.println();
    out.println("Option");
    out.println("  -h, -help, -? : Print this message. Same as '-verbose help'.");
    out.println("  -version : Print version. Same as '-verbose version'.");
    out.println("  -modules <paths> : Use specified module root paths.");
    out.println("  -m <paths> : Same as above.");
    out.println("  -verbose <switches1> : Show detail messages.");
    out.println("  -quiet <switches1> : Suppress messages.");
    out.println();
    out.println("  <switches1> -- one or more switches concatenated with '+'. Switch are as follows");
    out.println("    help : help message");
    out.println("    version : version information");
    out.println("    module : module loading");
    out.println("    all : all switches above");
    out.println();
    out.println("Default options");
    out.println("  -m . -verbose version+module");
    out.println();
  }
}
