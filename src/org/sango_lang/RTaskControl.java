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

import java.util.ArrayList;
import java.util.List;

class RTaskControl implements RTaskMgr.Blocker {
  RTaskMgr theMgr;
  RTaskMgr.SchedulingList theList;
  RActorHItem actorH;
  RAsyncResultHItem asyncResultH;
  int prio;
  int type;
  int state;
  boolean terminateOnAbnormalEnd;
  RTaskControl prior;
  RTaskControl next;
  RClosureItem entryClosure;
  RFrame currentFrame;
  RResult result;
  List<RTaskControl> blockedTaskList;
  List<RTaskMgr.Blocker> blockerList;
  RTaskMgr.Wakeup wakeup;
  List<RErefItem> monitorList;
  boolean background;

  private RTaskControl() {}

  static RTaskControl create(RTaskMgr m, int priority, int type, RClosureItem c) {
    return create(m, priority, type, c, new RObjItem[0]);
  }

  static RTaskControl create(RTaskMgr m, int priority, int type, RClosureItem c, RObjItem[] params) {
    if (c.getParamCount() != params.length) { throw new IllegalArgumentException("Param count mismatch."); }
    RTaskControl tc = new RTaskControl();
    tc.theMgr = m;
    tc.actorH = RActorHItem.create(m.theEngine, tc);
    tc.asyncResultH = RAsyncResultHItem.create(m.theEngine, tc);
    tc.prio = priority;
    tc.type = type;
    tc.state = RTaskMgr.TASK_BORN;
    tc.entryClosure = c;
    tc.currentFrame = RFrame.create(tc, c, params);
    tc.blockedTaskList = new ArrayList<RTaskControl>();
    tc.blockerList = new ArrayList<RTaskMgr.Blocker>();
    tc.monitorList = new ArrayList<RErefItem>();
    return tc;
  }

  public void setPriority(int p) {
    this.theMgr.setPriorityOf(this, p);
  }

  public void setBackground(boolean b) {
    this.theMgr.setBackground(this, b);
  }

  public void start() {
    this.theMgr.startTask(this);
  }

  public void startWaitingFor(RTaskControl t) {
    this.theMgr.startTaskWaitingFor(this, t);
  }

  void finish(RObjItem ret) {
    this.theMgr.finishTask(this, ret);
  }

  void abort(RObjItem exc) {
    this.theMgr.abortTask(this, exc);
  }

  void end(RResult res) {
    this.theMgr.endTask(this, res);
  }

  void yield() {
    this.theMgr.yieldTask(this);
  }

  public List<RTaskControl> joinSomeOf(List<RTaskControl> ts, Integer expiration) {
    return this.theMgr.joinSomeOfTasks(this, ts, expiration);
  }

  public boolean join(RTaskControl t, Integer expiration) {
    List<RTaskControl> ts = new ArrayList<RTaskControl>();
    ts.add(t);
    return !this.theMgr.joinSomeOfTasks(this, ts, expiration).isEmpty();
  }

  List<RErefItem> listenMboxes(List<RErefItem> bes, Integer expiration) {
    return this.theMgr.listenMboxes(this, bes, expiration);
  }

  public void addBlockedTask(RTaskControl t) {
    this.blockedTaskList.add(t);
  }

  public void cancelBlockedTask(RTaskControl t) {
    this.blockedTaskList.remove(t);
  }

  void addBlocker(RTaskMgr.Blocker b) {
    this.blockerList.add(b);
  }

  RTaskMgr.Blocker removeFirstBlocker() {
    return (this.blockerList.size() > 0)? this.blockerList.remove(0): null;
  }

  RTaskControl removeFirstBlockedTask() {
    return (this.blockedTaskList.size() > 0)? this.blockedTaskList.remove(0): null;
  }

  void addMonitor(RErefItem mboxpWE) {
    this.monitorList.add(mboxpWE);
  }

  void removeMonitor(RErefItem mboxpWE) {
    this.monitorList.remove(mboxpWE);
  }

  public RResult getResult() {
    if (this.result == null) { throw new RuntimeException("Task has not ended."); }
    return this.result;
  }
}
