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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

class RMemMgr {
  static final int MAINTENANCE_INTERVAL = 100;  // tentatively
  static final int INT_INT_CACHE_MIN = -10;
  static final int INT_INT_CACHE_MAX = 100;
  static final int BYTE_INT_CACHE_MIN = 0;
  static final int BYTE_INT_CACHE_MAX = 255;
  static final int CHAR_INT_CACHE_MIN = 0;
  static final int CHAR_INT_CACHE_MAX = 127;

  RuntimeEngine theEngine;
  Map<Reference, RClosureItem> entityInvalidatorDict;
  ReferenceQueue<Entity> purgedEntityWrefQueue;
  Map<Reference, WrefListenerInfo> wrefListenerDict;
  ReferenceQueue<Entity> clearedWrefQueue;
  int maintenanceInterval;
  LinkedList<WeakReference<RMemMgr.Entity>> sysMsgReceivers;
  // cache
  RRealItem nanItem;
  RRealItem posInfItem;
  RRealItem negInfItem;
  RStructItem voidItem;
  RStructItem[] boolItems;
  RIntItem[] intIntItems;
  RIntItem[] byteIntItems;
  RIntItem[] charIntItems;
  RListItem.Nil listNilItem;

  RMemMgr(RuntimeEngine e) {
    this.theEngine = e;
    this.entityInvalidatorDict = Collections.synchronizedMap(new HashMap<Reference, RClosureItem>());
    this.purgedEntityWrefQueue = new ReferenceQueue<Entity>();
    this.wrefListenerDict = Collections.synchronizedMap(new HashMap<Reference, WrefListenerInfo>());
    this.clearedWrefQueue = new ReferenceQueue<Entity>();
    this.maintenanceInterval = MAINTENANCE_INTERVAL;
    this.sysMsgReceivers = new LinkedList<WeakReference<RMemMgr.Entity>>();
    this.makeCache();
  }

  void makeCache() {
    this.nanItem = new RRealItem(this.theEngine, Double.NaN);
    this.posInfItem = new RRealItem(this.theEngine, Double.POSITIVE_INFINITY);
    this.negInfItem = new RRealItem(this.theEngine, Double.NEGATIVE_INFINITY);
    this.voidItem = this.getStructItem(
      RDataConstr.create(Module.MOD_LANG, "void$", 0, "void", 0),
      new RObjItem[0]);
    this.boolItems = new RStructItem[] {
      this.getStructItem(RDataConstr.create(Module.MOD_LANG, "false$", 0, "bool", 0), new RObjItem[0]),
      this.getStructItem(RDataConstr.create(Module.MOD_LANG, "true$", 0, "bool", 0), new RObjItem[0])
    };
    this.intIntItems = new RIntItem[INT_INT_CACHE_MAX - INT_INT_CACHE_MIN + 1];
    for (int i = INT_INT_CACHE_MIN; i <= INT_INT_CACHE_MAX; i++) {
      this.intIntItems[i - INT_INT_CACHE_MIN] = RIntItem.create(this.theEngine, MInstruction.INT_OBJ_INT, i);
    }
    this.byteIntItems = new RIntItem[BYTE_INT_CACHE_MAX - BYTE_INT_CACHE_MIN + 1];
    for (int i = BYTE_INT_CACHE_MIN; i <= BYTE_INT_CACHE_MAX; i++) {
      this.byteIntItems[i - BYTE_INT_CACHE_MIN] = RIntItem.create(this.theEngine, MInstruction.INT_OBJ_BYTE, i);
    }
    this.charIntItems = new RIntItem[CHAR_INT_CACHE_MAX - CHAR_INT_CACHE_MIN + 1];
    for (int i = CHAR_INT_CACHE_MIN; i <= CHAR_INT_CACHE_MAX; i++) {
      this.charIntItems[i - CHAR_INT_CACHE_MIN] = RIntItem.create(this.theEngine, MInstruction.INT_OBJ_CHAR, i);
    }
    this.listNilItem = new RListItem.Nil(this.theEngine);
  }

  void maintainQuick() {
    this.maintenanceInterval--;
    if (this.maintenanceInterval <= 0) {
      this.theEngine.scheduleSysTask(new Maintainer(false));
      this.maintenanceInterval = MAINTENANCE_INTERVAL;
    }
  }

  void maintainFull() {
    this.theEngine.scheduleSysTask(new Maintainer(true));
    this.maintenanceInterval = MAINTENANCE_INTERVAL;
  }

  RStructItem getVoidItem() {
    return this.voidItem;
  }

  RStructItem getBoolItem(boolean b) {
    return b? this.boolItems[1]: this.boolItems[0];
  }

  RIntItem getIntItem(int value) {
    return (INT_INT_CACHE_MIN <= value && value <= INT_INT_CACHE_MAX)?
      this.intIntItems[value - INT_INT_CACHE_MIN]:
      RIntItem.create(this.theEngine, MInstruction.INT_OBJ_INT, value);
  }

  RIntItem getByteItem(int value) {
    if (value < 0 || value > 255) { throw new IllegalArgumentException("Byte value out of range.") ; }
    return (BYTE_INT_CACHE_MIN <= value && value <= BYTE_INT_CACHE_MAX)?
      this.byteIntItems[value - BYTE_INT_CACHE_MIN]:
      RIntItem.create(this.theEngine, MInstruction.INT_OBJ_BYTE, value);
  }

  RIntItem getCharItem(int value) {
      if (value < 0 || value > 0x10ffff) { throw new IllegalArgumentException("Char value out of range.") ; }
    return (CHAR_INT_CACHE_MIN <= value && value <= CHAR_INT_CACHE_MAX)?
      this.charIntItems[value - CHAR_INT_CACHE_MIN]:
      RIntItem.create(this.theEngine, MInstruction.INT_OBJ_CHAR, value);
  }

  RIntItem getIntItem(int cat, int value) {
    RIntItem I = null;
    switch (cat) {
    case MInstruction.INT_OBJ_INT:
      I = this.getIntItem(value);
      break;
    case MInstruction.INT_OBJ_BYTE:
      I = this.getByteItem(value);
      break;
    case MInstruction.INT_OBJ_CHAR:
      I = this.getCharItem(value);
      break;
    default:
      throw new IllegalArgumentException("Unknown int item category.");
    }
    return I;
  }

  RRealItem getRealItem(double value) {
    return new RRealItem(this.theEngine, value);
  }

  RRealItem getNaNItem() { return this.nanItem; }

  RRealItem getPosInfItem() { return this.posInfItem; }

  RRealItem getNegInfItem() { return this.negInfItem; }

  RDataConstr getDataConstr(Cstr modName, String name) {
    RModule m = this.theEngine.modMgr.getRMod(modName);
    if (m == null) {
      throw new IllegalArgumentException("Unknown module name. - " + modName.toJavaString());
    }
    RDataConstr dc = m.getDataConstr(name);
    if (dc == null) {
      throw new IllegalArgumentException("Unknown data constructor. - " + name);
    }
    return dc;
  }

  RStructItem getStructItem(RDataConstr dataConstr, RObjItem[] attrs) {
      // uniqueness is preferred...
    return RStructItem.create(this.theEngine, dataConstr, attrs);
  }

  RStructItem getTupleItem(RObjItem[] elems) {
    return RStructItem.create(this.theEngine, RDataConstr.pseudoOfTuple, elems);
  }

  RListItem.Nil getListNilItem() { return this.listNilItem; }

  RListItem.Cell createListCellItem() {
    return new RListItem.Cell(this.theEngine);
  }

  RArrayItem createArrayItem(int size) {
    return new RArrayItem(this.theEngine, size);
  }

  RArrayItem cstrToArrayItem(Cstr cstr) {
    int len = cstr.getLength();
    RArrayItem a = RArrayItem.create(this.theEngine, len);
    for (int i = 0; i < len; i++) {
      a.setCharElemAt(i, cstr.getCharAt(i));
    }
    return a;
  }

  static Cstr arrayItemToCstr(RArrayItem array) {
    Cstr s = new Cstr();
    for (int i = 0; i < array.getElemCount(); i++) {
       s.append(((RIntItem)array.getElemAt(i)).getValue());
    }
    return s;
  }

  RResult createResult() {
    return this.createResult(this.getVoidItem());
  }

  RResult createResult(RObjItem ret) {
    return new RResult(this.theEngine, ret);
  }

  RResult retToResult(RObjItem ret) {
    return this.createResult(ret);
  }

  RResult excToResult(RObjItem exc) {
    RResult r = this.createResult();
    r.setException(exc);
    return r;
  }

  RErefItem createEntity(RObjItem item, RClosureItem invalidator) {
    Entity e = new Entity(item);
    if (invalidator != null) {
      WeakReference<Entity> wr = new WeakReference<Entity>(e, this.purgedEntityWrefQueue);
      this.entityInvalidatorDict.put(wr, invalidator);
    }
    return RErefItem.create(this.theEngine, e);
  }

  RWrefItem createWeakHolder(RObjItem entity, RClosureItem listener) {
    RErefItem eref = (RErefItem)entity;
    WeakReference<Entity> wr;
    RWrefItem wref;
    if (listener != null) {
      wr = new WeakReference<Entity>(eref.entity, this.clearedWrefQueue);
      wref = RWrefItem.create(this.theEngine, wr);
      this.wrefListenerDict.put(wr, new WrefListenerInfo(wref, listener));
    } else {
      wr = new WeakReference<Entity>(eref.entity);
      wref = RWrefItem.create(this.theEngine, wr);
    }
    return wref;
  }

  public RClosureItem createClosureOfNativeImpl(RModule mod, String name, int paramCount, /* String implFor, */ Object nativeImplTargetObject, Method nativeImpl) {
    return RClosureItem.create(this.theEngine, RClosureImpl.createNative(mod, name, paramCount, /* implFor, */ nativeImplTargetObject, nativeImpl), new RObjItem[0]);
  }

  private void doMaintainFull() {
    this.checkSysMsgReceiver();
    this.notifyPurgedEntities(Integer.MAX_VALUE);
    this.notifyClearedWrefs(Integer.MAX_VALUE);
  }

  private void doMaintainQuick() {
    this.checkSysMsgReceiver();
    this.notifyPurgedEntities(1);
    this.notifyClearedWrefs(1);
  }

  private void checkSysMsgReceiver() {
    synchronized (this.sysMsgReceivers) {
      if (this.sysMsgReceivers.size() > 0) {
        WeakReference<RMemMgr.Entity> w = this.sysMsgReceivers.poll();
        if (w.get() != null) {
          this.sysMsgReceivers.add(w);  // requeue if alive
        }
      }
    }
  }

  private void notifyPurgedEntities(int countMax) {
    Reference<? extends Entity> r;
    int countDown = countMax;
    while (countDown >= 0 && (r = this.purgedEntityWrefQueue.poll()) != null) {
      RClosureItem invalidator = this.entityInvalidatorDict.remove(r);
      if (invalidator != null) {  // supposed to be always not null
        countDown--;
// System.out.println("Detected purged entity.");
        this.theEngine.taskMgr.createTask(
          RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, invalidator)
        .start();
      }
    }
  }

  private void notifyClearedWrefs(int countMax) {
    Reference<? extends Entity> r;
    int countDown = countMax;
    while (countDown >= 0 && (r = this.clearedWrefQueue.poll()) != null) {
      WrefListenerInfo listenerInfo = this.wrefListenerDict.remove(r);
      if (listenerInfo != null) {  // supposed to be always not null
        countDown--;
// System.out.println("Detected cleared weak ref.");
        this.theEngine.taskMgr.createTask(
          RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, listenerInfo.listener, new RObjItem[] { listenerInfo.wref })
        .start();
      }
    }
  }

  RObjItem createMbox(RTaskControl owner) {
    return RMbox.create(this.theEngine, owner).handleItem;
  }

  public void notifySysMsg(RObjItem bpew) {
    RWrefItem wref = (RWrefItem)bpew;
    RMemMgr.Entity ent = wref.entityWref.get();
    if (ent != null) {
      RStructItem bp = (RStructItem)ent.read();
      if (bp.getFieldAt(0) instanceof RMboxPItem) {
        synchronized (this.sysMsgReceivers) {
          this.sysMsgReceivers.add(wref.entityWref);
        }
      } else {
        throw new IllegalArgumentException("Not wref of mbox entity.");
      }
    }
  }

  WeakReference<RMemMgr.Entity> pollSysMsgReceiver() {
    synchronized (this.sysMsgReceivers) {
      return this.sysMsgReceivers.poll();
    }
  }

  public class Entity {
    RObjItem item;

    Entity(RObjItem item) {
      this.item = item;
    }

    public RObjItem read() {
      synchronized (this) {
        return this.item;
      }
    }

    public RObjItem write(RObjItem newItem) {
      synchronized (this) {
        RObjItem oldItem = this.item;
        this.item = newItem;
        return oldItem;
      }
    }
  }

  private class WrefListenerInfo {
    RWrefItem wref;
    RClosureItem listener;

    WrefListenerInfo(RWrefItem wref, RClosureItem listener) {
      this.wref = wref;
      this.listener = listener;
    }
  }

  private class Maintainer implements RuntimeEngine.SysTask {
    boolean full;

    Maintainer(boolean full) {
      this.full = full;
    }

    public void run(int runState) {
      if (this.full || runState == RuntimeEngine.SHUTDOWN) {
        RMemMgr.this.doMaintainFull();
      } else {
        RMemMgr.this.doMaintainQuick();
      }
    }
  }
}
