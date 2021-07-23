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

import java.lang.ref.WeakReference;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.List;

class RTaskMgr {
  static final int TASK_TYPE_SYS = 0;
  static final int TASK_TYPE_INIT = 1;
  static final int TASK_TYPE_APPL = 2;

  static final int TASK_BORN = 0;
  static final int TASK_READY = 1;
  static final int TASK_RUNNING = 2;
  static final int TASK_BLOCKED = 3;
  static final int TASK_DEAD = 9;

  static final int QUOTA = 1000;  // temporal impl
  private static final int WORKER_COUNT_MAINTENANCE_INTERVAL = 100;  // temporal impl
  private static final int WORKER_COUNT_MAX = 100;  // temporal impl
  private static final int WORKER_IDLE_KEEP = 1;  // temporal impl
  static final int PRIO_MAX = 9;  // min = 0
  static final int PRIO_DEFAULT = 4;

  RuntimeEngine theEngine;
  RLock lock;
  SchedulingList[] readyTasksTab;
  SchedulingList runningTasks;
  SchedulingList blockedTasks;
  RLock.SignalReceiver workerWait;
  int runningWorkerCount;
  int idleWorkerCount;
  boolean justAfterFullMaintenance;
  int workerMaintenaceCountDown;
  List<MsgReq> msgReqQueue;
  boolean dumpedForegroundActors;


  RTaskMgr(RuntimeEngine e) {
    this.theEngine = e;
    this.lock = RLock.create();
    this.workerWait = this.lock.createSignalReceiver();
    this.readyTasksTab = new SchedulingList[PRIO_MAX + 1];
    for (int i = 0; i <= PRIO_MAX; i++) {
      this.readyTasksTab[i] = new SchedulingList();
    }
    this.runningTasks = new SchedulingList();
    this.blockedTasks = new SchedulingList();
    this.workerMaintenaceCountDown = WORKER_COUNT_MAINTENANCE_INTERVAL;
    this.msgReqQueue = new ArrayList<MsgReq>();
  }

  void start() {
    this.scheduleMaintenance();
  }

  RTaskControl createTask(int priority, int type, RClosureItem c) {
    return this.createTask(priority, type, c, new RObjItem[0]);
  }

  RTaskControl createTask(int priority, int type, RClosureItem c, RObjItem[] params) {
    return RTaskControl.create(this, priority, type, c, params);
  }

  void setPriorityOf(RTaskControl t, int p) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    if (t.prio != p) {
      t.prio = p;
      if (t.state == TASK_READY && t.theList != this.readyTasksTab[p]) {
        this.removeFromReadyQueue(lc, t);
        this.readyTasksTab[p].enqueue(t);
      }
      this.scheduleNotifyActorState(lc, t);
    }
    lc.release();  // EXIT CRITICAL SECTION
  }

  void setBackground(RTaskControl t, boolean b) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    if (t.background == b) {
      ;
    } else {
      t.background = b;
      if (b) {
        switch (t.state) {
        case TASK_READY:
        case TASK_RUNNING:
        case TASK_BLOCKED:
          this.scheduleMaintenance();
          break;
        default:
          break;
        }
      }
      this.scheduleNotifyActorState(lc, t);
    }
    lc.release();  // EXIT CRITICAL SECTION
  }

  void terminateOnAbnormalEnd(RTaskControl t) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    switch (t.state) {
    case TASK_BORN:
    case TASK_READY:
    case TASK_RUNNING:
    case TASK_BLOCKED:
      t.terminateOnAbnormalEnd = true;
      break;
    case TASK_DEAD:
      RStructItem exc = t.result.getException();
      if (exc != null) {
        this.theEngine.monitoredActorAborted(exc);
      }
      break;
    default:
      break;
    }
    lc.release();  // EXIT CRITICAL SECTION
  }

  void startTask(RTaskControl t) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    switch (t.state) {
    case TASK_BORN:
      this.enqueueToReadyQueue(lc, t);
      this.scheduleNotifyActorState(lc, t);
      break;
    case TASK_READY:
    case TASK_RUNNING:
    case TASK_BLOCKED:
    case TASK_DEAD:
      break;
    default:
      break;
    }
    lc.release();  // EXIT CRITICAL SECTION
  }

  void startTaskWaitingFor(RTaskControl w, RTaskControl t) {
    if (w.theMgr != this || t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    switch (t.state) {
    case TASK_BORN:
    case TASK_READY:
    case TASK_RUNNING:
    case TASK_BLOCKED:
      this.addToBlockedList(lc, w);
      t.addBlockedTask(w);
      w.addBlocker(t);
      break;
    case TASK_DEAD:
      this.enqueueToReadyQueue(lc, w);
      break;
    default:
      break;
    }
    lc.release();  // EXIT CRITICAL SECTION
  }

  void finishTask(RTaskControl t, RObjItem ret) {
    this.endTask(t, this.theEngine.memMgr.retToResult(ret));
  }

  void abortTask(RTaskControl t, RObjItem exc) {
    this.endTask(t, this.theEngine.memMgr.excToResult(exc));
  }

  void endTask(RTaskControl t, RResult res) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    if (t.type == TASK_TYPE_INIT) {
      RModule mod = t.entryClosure.impl.mod;
      if (res.endCondition() == RResult.NORMAL_END) {
        mod.initialized(res.getReturnValue());
      } else {
        this.initTaskAborted(mod, res.getException());
      }
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    t.result = res;
    switch (t.state) {
    case TASK_RUNNING:
      this.removeFromRunningList(lc, t);
      t.state = TASK_DEAD;
      break;
    case TASK_BORN:
    case TASK_READY:
    case TASK_BLOCKED:
    case TASK_DEAD:
      throw new IllegalStateException("Not active.");  // logical error
    default:
      break;
    }
    if (t.type == TASK_TYPE_INIT && res.endCondition() != RResult.NORMAL_END) {  // exit$ or exception
      ;  // keep blocked tasks
    } else {
      this.scheduleNotifyActorState(lc, t);
      RTaskControl w;
      while ((w = t.removeFirstBlockedTask()) != null) {
        this.wakeupTask(lc, w);
      }
    }
    if (t.terminateOnAbnormalEnd && res.endCondition() == RResult.ABNORMAL_END) {
      this.theEngine.monitoredActorAborted(res.getException());
    }
    this.scheduleMaintenance();
    lc.release();  // EXIT CRITICAL SECTION
  }

  void taskMayRunLong(RTaskControl t) {
    this.scheduleMaintenance();  // spawn worker if needed
  }

  RObjItem getActorState(RTaskControl t) {
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.SHARED);  // ENTER CRITCAL SECTION
    RObjItem s = this.createActorState(lc, t);
    lc.release();  // EXIT CRITICAL SECTION
    return s;
  }

  void addActorMonitor(RActorHItem actorH, RErefItem senderE) {
    RTaskControl t = actorH.taskControl;
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    t.addMonitor(senderE);
    this.scheduleNotifyActorState(lc, t);
    this.scheduleMaintenance();
    lc.release();  // EXIT CRITICAL SECTION
  }

  void removeActorMonitor(RActorHItem actorH, RErefItem senderE) {
    RTaskControl t = actorH.taskControl;
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    t.removeMonitor(senderE);
    lc.release();  // EXIT CRITICAL SECTION
  }

  void initTaskAborted(RModule mod, RObjItem exc) {
    this.theEngine.scheduleSysTask(new ActionOnInitTaskAborted(mod, exc));
  }

  RResult peekTaskResult(RTaskControl t) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.SHARED);  // ENTER CRITCAL SECTION
    RResult r = t.result;
    lc.release();  // EXIT CRITICAL SECTION
    return r;
  }

  void scheduleNotifyActorState(RLock.Client lc, RTaskControl t) {
    if (t.monitorList.isEmpty()) { return; }
    RDataConstr dcActorStateChanged = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "actor_state_changed$");
    RObjItem objActorStateChanged = this.theEngine.memMgr.getStructItem(
      dcActorStateChanged, new RObjItem[] { t.actorH, this.createActorState(lc, t) });
    for (int i = 0; i < t.monitorList.size(); i++) {
      this.msgReqQueue.add(new MsgReq(t.monitorList.get(i), objActorStateChanged));
    }
  }

  RObjItem createActorState(RLock.Client lc, RTaskControl t) {
    RDataConstr dcActorState = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "actor_state$");
    return this.theEngine.memMgr.getStructItem(
      dcActorState,
      new RObjItem[] {
        this.createActorRunState(t),
        this.theEngine.memMgr.getIntItem(t.prio),
        this.theEngine.memMgr.getBoolItem(t.background) }
    );
  }

  RObjItem createActorRunState(RTaskControl t) {
    RObjItem s = null;
    switch (t.state) {
    case TASK_BORN:
      RDataConstr dcActorBorn = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "actor_born$");
      s = this.theEngine.memMgr.getStructItem(dcActorBorn, new RObjItem[0]);
      break;
    case TASK_RUNNING:
    case TASK_READY:
    case TASK_BLOCKED:
      RDataConstr dcActorStarted = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "actor_started$");
      s = this.theEngine.memMgr.getStructItem(dcActorStarted, new RObjItem[0]);
      break;
    case TASK_DEAD:
      RObjItem exc = t.getResult().getException();
      RObjItem maybeExc;
      if (exc != null) {
        RDataConstr dcValue = this.theEngine.memMgr.getDataConstr(Module.MOD_LANG, "value$");
        maybeExc = this.theEngine.memMgr.getStructItem(dcValue, new RObjItem[] { exc });
      } else {
        RDataConstr dcNone = this.theEngine.memMgr.getDataConstr(Module.MOD_LANG, "none$");
        maybeExc = this.theEngine.memMgr.getStructItem(dcNone, new RObjItem[0]);
      }
      RDataConstr dcActorEnded = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "actor_ended$");
      s = this.theEngine.memMgr.getStructItem(dcActorEnded, new RObjItem[] { maybeExc });
      break;
    default:
      break;
    }
    return s;
  }

  void wakeupTask(RLock.Client lc, RTaskControl t) {
    switch (t.state) {
    case TASK_BORN:
    case TASK_RUNNING:
    case TASK_READY:
    case TASK_DEAD:
      break;
    case TASK_BLOCKED:
      this.removeFromBlockedList(lc, t);
      this.enqueueToReadyQueue(lc, t);
      if (this.runningTasks.count < 2) {  // if insufficient available workers...  (tentative impl)
        this.scheduleMaintenance();
      }
      break;
    default:
      break;
    }
  }

  List<RTaskControl> joinSomeOfTasks(RTaskControl w, List<RTaskControl> ts, Integer expiration) {
    // expiration == null means wait forever
    if (w.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    List<RTaskControl> endedList = new ArrayList<RTaskControl>();
    List<RTaskControl> activeList = new ArrayList<RTaskControl>();
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
    for (int i = 0; i < ts.size(); i++) {
      this.scanForJoining(lc, ts.get(i), endedList, activeList);
    }
    if (endedList.isEmpty()) {
      switch (w.state) {
      case TASK_BORN:
      case TASK_DEAD:
      case TASK_BLOCKED:
        break;
      case TASK_READY:
        if (expiration == null || expiration > 0) {
          this.removeFromReadyQueue(lc, w);
          this.addToBlockedList(lc, w);
        }
        break;
      case TASK_RUNNING:
        if (expiration == null || expiration > 0) {
          this.removeFromRunningList(lc, w);
          this.addToBlockedList(lc, w);
        }
        break;
      default:
        break;
      }
      switch (w.state) {
      case TASK_BLOCKED:
        for (int i = 0; i < activeList.size(); i++) {
          RTaskControl b = activeList.get(i);
          b.addBlockedTask(w);
          w.addBlocker(b);
        }
        if (expiration != null) {
          this.startWakeupTimer(lc, w, expiration);
        }
        break;
      default:
        break;
      }
    }
    lc.release();  // EXIT CRITICAL SECTION
    return endedList;
  }

  private void scanForJoining(RLock.Client lc, RTaskControl t, List<RTaskControl> endedList, List<RTaskControl> activeList) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    switch (t.state) {
    case TASK_BORN:
    case TASK_READY:
    case TASK_RUNNING:
    case TASK_BLOCKED:
      activeList.add(t);
      break;
    case TASK_DEAD:
      endedList.add(t);
      break;
    default:
      break;
    }
  }

  List<RErefItem> listenMboxes(RTaskControl t, List<RErefItem> bes, Integer expiration) {
    List<RErefItem> receivables = new ArrayList<RErefItem>();
    List<RLock.Client> lockClients = new ArrayList<RLock.Client>();
    for (int i = 0; i < bes.size(); i++) {
      RErefItem be = bes.get(i);
      RMbox b = this.theEngine.memMgr.getMboxBody(be);
      RLock.Client blc = b.lock.createClient();
      blc.require(RLock.EXCLUSIVE);  // LOCK
      lockClients.add(blc);
      if (!b.msgQueue.isEmpty()) {
        receivables.add(be);
      }
    }
    if (receivables.isEmpty() && (expiration == null || expiration > 0)) {
      RLock.Client lc = this.lock.createClient();
      lc.require(RLock.EXCLUSIVE);  // ENTER CRITCAL SECTION
      switch (t.state) {
      case TASK_RUNNING:
        this.removeFromRunningList(lc, t);
        this.addToBlockedList(lc, t);
        for (int i = 0; i < bes.size(); i++) {
          RMbox b = this.theEngine.memMgr.getMboxBody(bes.get(i));
          b.addBlockedTask(t);
          t.addBlocker(b);
        }
        if (expiration != null) {
          this.startWakeupTimer(lc, t, expiration);
        }
        break;
      case TASK_BORN:
      case TASK_READY:
      case TASK_BLOCKED:
      case TASK_DEAD:
        throw new IllegalArgumentException("Task not running.");  // logical error
      default:
        break;
      }
      lc.release();  // EXIT CRITICAL SECTION
    }
    for (int i = 0; i < lockClients.size(); i++) {
      lockClients.get(i).release();  // UNLOCK
    }
    return receivables;
  }

  void yieldTask(RTaskControl t) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
    switch (t.state) {
    case TASK_RUNNING:
      this.removeFromRunningList(lc, t);
      this.enqueueToReadyQueue(lc, t);
      break;
    case TASK_BORN:
    case TASK_READY:
    case TASK_BLOCKED:
    case TASK_DEAD:
      break;
    default:
      break;
    }
    lc.release();  // EXIT CRITCAL SECTION
  }

  void workEnded(RTaskControl t) {
    if (t.theMgr != this) {
      throw new IllegalArgumentException("Not my task.");
    }
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
    switch (t.state) {
    case TASK_RUNNING:
      this.removeFromRunningList(lc, t);
      this.enqueueToReadyQueue(lc, t);
      break;
    case TASK_BORN:
    case TASK_READY:
    case TASK_BLOCKED:
    case TASK_DEAD:
      break;
    default:
      break;
    }
    lc.release();  // EXIT CRITCAL SECTION
  }

  private void startWakeupTimer(RLock.Client lc, RTaskControl target, int wait) {
    target.wakeup = new Wakeup(target);
    this.theEngine.timer.schedule(target.wakeup, wait);
  }

  private void enqueueToReadyQueue(RLock.Client lc, RTaskControl t) {
    t.state = TASK_READY;
    this.readyTasksTab[t.prio].enqueue(t);
    Blocker b;
    while ((b = t.removeFirstBlocker()) != null) {
      b.cancelBlockedTask(t);
    }
    if (t.wakeup != null) {
      t.wakeup.cancel();
      t.wakeup = null;
    }
    lc.sendSignal(this.workerWait);  // WAKE UP WORKER
  }

  private RTaskControl dequeueFromReadyQueue(RLock.Client lc) {
    RTaskControl t = null;
    for (int i = PRIO_MAX; t == null && i >= 0; i--) {
      t = this.readyTasksTab[i].dequeue();
    }
    return t;
  }

  private void removeFromReadyQueue(RLock.Client lc, RTaskControl t) {
    t.theList.remove(t);
  }

  private boolean anyReadyTasks(RLock.Client lc) {
    boolean b = false;
    for (int i = 0; !b && i <= PRIO_MAX; i++) {
      b = this.readyTasksTab[i].first != null;
    }
    return b;
  }

  private void addToRunningList(RLock.Client lc, RTaskControl t) {
    t.state = TASK_RUNNING;
    this.runningTasks.add(t);
  }

  private void removeFromRunningList(RLock.Client lc, RTaskControl t) {
    /* DEBUG */ if (t.state != TASK_RUNNING) {
    /* DEBUG */   throw new RuntimeException("Attempt to remove non-running task from running list.");
    /* DEBUG */  }
    this.runningTasks.remove(t);
  }

  private void addToBlockedList(RLock.Client lc, RTaskControl t) {
    t.state = TASK_BLOCKED;
    this.blockedTasks.add(t);
  }

  private void removeFromBlockedList(RLock.Client lc, RTaskControl t) {
    /* DEBUG */ if (t.state != TASK_BLOCKED) {
    /* DEBUG */   throw new RuntimeException("Attempt to remove non-blocked task from blocked list.");
    /* DEBUG */  }
    this.blockedTasks.remove(t);
  }

  Work getWork() {
    Work w = null;
    boolean cont = true;
    RLock.Client lc = this.lock.createClient();
    while (cont) {
      lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
      RTaskControl t = this.dequeueFromReadyQueue(lc);
      if (t != null) {
// /* DEBUG */ System.out.println("ready " + t.toString());
        w = new Work(t);
        this.addToRunningList(lc, t);
        this.theEngine.memMgr.maintainQuick();
        this.justAfterFullMaintenance = false;
        if (this.anyReadyTasks(lc)) {
          if (this.workerMaintenaceCountDown-- <= 0) {
            this.workerMaintenaceCountDown = WORKER_COUNT_MAINTENANCE_INTERVAL;
            this.scheduleMaintenance();
          }
        }
        cont = false;
      } else {
        this.scheduleMaintenance();
        if (this.idleWorkerCount < WORKER_IDLE_KEEP) {
// /* DEBUG */ System.out.println("ready none wait");
          this.idleWorkerCount++;
          lc.waitSignal(this.workerWait);  // WAIT+UNLOCK and LOCK AGAIN
          this.idleWorkerCount--;
          cont = true;
        } else {
// /* DEBUG */ System.out.println("ready none exit");
          this.runningWorkerCount--;
          cont = false;
        }
      }
      lc.release();  // EXIT CRITCAL SECTION
    }
    return w;
  }

  RObjItem createMbox(RTaskControl owner) {
    return this.theEngine.memMgr.createMbox(owner);
  }

  void wakeupMboxListeners(RMbox b) {
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
    RTaskControl t;
    while ((t = b.removeFirstBlockedTask()) != null) {
      this.wakeupTask(lc, t);
    }
    lc.release();  // EXIT CRITCAL SECTION
  }

  void scheduleMaintenance() {
    this.theEngine.scheduleSysTask(new Maintainer());
  }

  void maintain(int runState) {
    switch (runState) {
    case RuntimeEngine.RUNNING:
      this.maintainRunning();
      break;
    case RuntimeEngine.SHUTDOWN:
      this.maintainShutdown();
      break;
    default:
      throw new IllegalArgumentException("Invalid run state.");
    }
  }

  void maintainRunning() {
    boolean cont = true;
    while (cont) {
      MsgReq m = null;
      RLock.Client lc = this.lock.createClient();
      lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
      if (this.msgReqQueue.size() > 0) {
        m = this.msgReqQueue.remove(0);  // pickup first
      } else if (!this.anyForegroundActors()) {
        this.theEngine.requestShutdown(0, 10000);
        this.scheduleMaintenance();
      } else if (this.anyReadyTasks(lc)) {
        this.spawnWorkerIfNeeded(lc);
      } else if (this.runningTasks.first != null) {
        ;
      } else {
        boolean timer = false;
        RTaskControl tc = this.blockedTasks.first;
        while (!timer && tc != null) {
          timer = tc.wakeup != null;
          tc = tc.next;
        }
        if (timer) {
          ;
        } else if (!this.dumpedForegroundActors) {
          this.theEngine.msgOut.println("No runnable actors.");
          this.dumpForegroundActors();
          this.theEngine.requestShutdown(1, 10000);
          this.scheduleMaintenance();
        }
      }
      lc.release();  // EXIT CRITCAL SECTION
      if (m != null) {
        m.doSend();
      } else {
        cont = false;
      }
    }
  }

  void maintainShutdown() {
// /* DEBUG */ System.out.println("Maintenance in shutdown process ...");
    this.notifyShutdown();
    boolean cont = true;
    while (cont) {
      MsgReq m = null;
      RLock.Client lc = this.lock.createClient();
      lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
      if (this.msgReqQueue.size() > 0) {
        m = this.msgReqQueue.remove(0);  // pickup first
      } else if (this.anyReadyTasks(lc)) {
        this.spawnWorkerIfNeeded(lc);
      } else if (this.runningTasks.first != null) {
        ;
      } else {
        boolean timer = false;
        RTaskControl tc = this.blockedTasks.first;
        while (!timer && tc != null) {
          timer = tc.wakeup != null;
          tc = tc.next;
        }
        if (timer) {
          ;
        } else {
          this.theEngine.requestExit();
        }
      }
      lc.release();  // EXIT CRITCAL SECTION
      if (m != null) {
        m.doSend();
      } else {
        cont = false;
      }
    }
  }

  void spawnWorkerIfNeeded(RLock.Client lc) {
    if (this.runningWorkerCount < WORKER_COUNT_MAX
        && this.idleWorkerCount < WORKER_IDLE_KEEP) {
      RWorker w = new RWorker(this);
      w.setPriority(3);  // lower than monitor
      w.start();
      this.runningWorkerCount++;
    }
  }

  void notifyShutdown() {
    RDataConstr dcShutdown = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "shutdown$");
    RObjItem oShutdown = this.theEngine.memMgr.getStructItem(dcShutdown, new RObjItem[0]);
    RDataConstr dcMsg = this.theEngine.memMgr.getDataConstr(new Cstr("sango.actor"), "sys_msg$");
    RObjItem oMsg = this.theEngine.memMgr.getStructItem(dcMsg, new RObjItem[] { oShutdown });
    WeakReference<RErefItem> pew;
    while ((pew = this.theEngine.memMgr.pollSysMsgReceiver()) != null) {
      RErefItem pe = pew.get();
      if (pe != null) {
        ((RMboxPItem)((RStructItem)pe.read()).getFieldAt(0)).mbox.putMsg(oMsg);
      }
    }
  }

  private boolean anyForegroundActors() {
    boolean fg = false;
    RTaskControl tc = this.runningTasks.first;
    while (!fg && (tc != null)) {
      fg = !tc.background;
      tc = tc.next;
    }
    for (int i = 0; !fg && i <= PRIO_MAX; i++) {
      tc = this.readyTasksTab[i].first;
      while (!fg && (tc != null)) {
        fg = !tc.background;
        tc = tc.next;
      }
    }
    tc = this.blockedTasks.first;
    while (!fg && (tc != null)) {
      fg = !tc.background;
      tc = tc.next;
    }
    return fg;
  }

  void dumpForegroundActors() {
    this.theEngine.msgOut.println("-- Foreground actor list --");
    RTaskControl t = this.blockedTasks.first;
    while (t != null) {
      if (!t.background) {
        this.dumpActor(t);
      }
      t = t.next;
    }
    this.dumpedForegroundActors = true;
  }

  void dumpActor(RTaskControl t) {
    this.theEngine.msgOut.println("<Actor>");
    RuntimeEngine.printExcInfo(this.theEngine.msgOut, t.currentFrame.getExcInfo());
  }

  void moduleInitializationFailed(RModule mod, RObjItem exc) {
    this.theEngine.msgOut.print("Module ");
    this.theEngine.msgOut.print(mod.getName().repr());
    this.theEngine.msgOut.println(" initialization failed.");
    RuntimeEngine.printException(this.theEngine.msgOut, (RStructItem)exc);
    this.theEngine.requestHalt("Aborted due to failure of module initialization.");
  }

  void abort() {
    RLock.Client lc = this.lock.createClient();
    lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
    for (int i = 0; i < this.readyTasksTab.length; i++) {
      this.readyTasksTab[i].clear();
    }
    lc.sendSignal(this.workerWait);
    lc.release();  // EXIT CRITCAL SECTION
  }

  class SchedulingList {  // light weight bi-direction list of task controls
    RTaskControl first;
    RTaskControl last;
    int count;

    SchedulingList() {}

    private void clear() {
      this.first = null;
      this.last = null;
      this.count = 0;
    }

    private void add(RTaskControl t) {
      if (t.theList != null) { throw new IllegalArgumentException("Not free."); }
      if (this.last == null) {  // empty
        this.first = t;
        this.last = t;
        t.prior = null;
        t.next = null;
      } else {
        this.last.next = t;
        t.prior = this.last;
        this.last = t;
        t.next = null;
      }
      this.count++;
      t.theList = this;
    }

    private void remove(RTaskControl t) {
      if (t.theList != this) { throw new IllegalArgumentException("Not my task."); }
      if (t == this.first) {
        this.first = t.next;
        if (this.first == null) {
          this.last = null;
        } else {
          this.first.prior = null;
          t.next = null;
        }
      } else if (t == this.last) {  // t != this.first
          this.last = t.prior;
          this.last.next = null;
          t.prior = null;
      } else {
          RTaskControl p = t.prior;
          RTaskControl n = t.next;
          p.next = t.next;
          n.prior = t.prior;
          t.prior = null;
          t.next = null;
      }
      this.count--;
      t.theList = null;
    }

    private void enqueue(RTaskControl t) {
      this.add(t);
    }

    private RTaskControl dequeue() {
      RTaskControl t = null;
      if (this.first != null) {
        t = this.first;
        this.first = t.next;
        if (this.first == null) {
          this.last = null;
        } else {
          this.first.prior = null;
          t.next = null;
        }
        this.count--;
        t.theList = null;
      }
      return t;
    }
  }

  class Work implements Runnable {
    RTaskControl theTaskControl;
    boolean toQuit;

    Work(RTaskControl c) {
      this.theTaskControl = c;
    }

    void quit() {
      this.toQuit = true;
    }

    public void run() {
      RFrame frame = this.theTaskControl.currentFrame;
      int toExec = QUOTA;
      while (!this.toQuit && frame != null && toExec > 0) {
        frame = RInstruction.exec(RTaskMgr.this.theEngine, this, frame);
        if (!this.toQuit) {
          this.theTaskControl.currentFrame = frame;
        }
        toExec--;
      }
      if (!this.toQuit) {
        RTaskMgr.this.workEnded(this.theTaskControl);
      }
    }
  }

  class Wakeup extends TimerTask {
    RTaskControl target;

    Wakeup(RTaskControl target) {
      this.target = target;
    }

    public void run() {
      RLock.Client lc = RTaskMgr.this.lock.createClient();
      lc.require(RLock.EXCLUSIVE);  // ENTER CRITICAL SECTION
      if (this.target.wakeup == this) {
        RTaskMgr.this.wakeupTask(lc, this.target);
      }
      lc.release();  // EXIT CRITCAL SECTION
    }
  }

  class Maintainer implements RuntimeEngine.SysTask {
    public void run(int runState) {
      RTaskMgr.this.maintain(runState);
    }
  }

  class ActionOnInitTaskAborted implements RuntimeEngine.SysTask {
    RModule mod;
    RObjItem exc;

    ActionOnInitTaskAborted(RModule mod, RObjItem exc) {
      this.mod = mod;
      this.exc = exc;
    }

    public void run(int runState) {
      RTaskMgr.this.moduleInitializationFailed(this.mod, this.exc);
    }
  }

  class MsgReq {
    RErefItem senderE;
    RObjItem msg;

    MsgReq(RErefItem senderE, RObjItem msg) {
      this.senderE = senderE;
      this.msg = msg;
    }

    void doSend() {
      RMbox b = RTaskMgr.this.theEngine.memMgr.tryGetMboxBodyFromSenderEntity(this.senderE);
      if (b != null) {
        b.putMsg(this.msg);
      }
    }
  }

  interface Blocker {
    void addBlockedTask(RTaskControl t);
    void cancelBlockedTask(RTaskControl t);
  }
}
