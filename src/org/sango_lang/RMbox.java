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
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

class RMbox implements RTaskMgr.Blocker {
  RTaskControl owner;
  RStructItem handleItem;  // reverse pointer for cache
  RLock lock;
  Queue<RObjItem> msgQueue;
  List<RTaskControl> blockedTaskList;

  private RMbox() {}

  static RMbox create(RuntimeEngine e, RTaskControl owner) {
    RMbox b = new RMbox();
    b.owner = owner;
    RObjItem p = e.memMgr.getStructItem(e.memMgr.getDataConstr(new Cstr("sango.actor"), "mbox_p_ent_d$"), new RObjItem[] { RMboxPItem.create(e, b) });
    RErefItem pe = e.memMgr.createEntity(p, null);
    b.handleItem = e.memMgr.getStructItem(e.memMgr.getDataConstr(new Cstr("sango.actor"), "mbox_h$"), new RObjItem[] { pe });
    b.lock = RLock.create();
    b.msgQueue = new LinkedList<RObjItem>();
    b.blockedTaskList = new ArrayList<RTaskControl>();
    return b;
  }

  RStructItem getHandleItem() { return this.handleItem; }

  void putMsg(RObjItem msg) {
    RLock.Client L = this.lock.createClient();
    L.require(RLock.EXCLUSIVE);
    RObjItem orgFirst = this.msgQueue.peek();
    this.msgQueue.add(msg);
    if (orgFirst == null) {
      this.owner.theMgr.wakeupMboxListeners(this);
    }
    L.release();
  }

  RObjItem receiveMsg() {
    RLock.Client L = this.lock.createClient();
    L.require(RLock.EXCLUSIVE);
    RObjItem m = this.msgQueue.poll();
// /* DEBUG */ System.out.println((m != null)? "receive " + m.debugRepr().toJavaString(): null);
    L.release();
    return m;
  }

  public void addBlockedTask(RTaskControl t) {
    this.blockedTaskList.add(t);
  }

  public void cancelBlockedTask(RTaskControl t) {
    this.blockedTaskList.remove(t);
  }

  RTaskControl removeFirstBlockedTask() {
    return (this.blockedTaskList.size() > 0)? this.blockedTaskList.remove(0): null;
  }
}
