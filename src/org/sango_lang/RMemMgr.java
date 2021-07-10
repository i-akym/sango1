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
import java.util.List;
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
  List<EntityInvalidationInfo> entityInvalidationInfoList;
  List<WrefNotificationInfo> wrefNotificationInfoList;
  List<WeakReference<RErefItem>> sysMsgReceiverList;
  int maintenanceInterval;
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
    this.entityInvalidationInfoList = Collections.synchronizedList(new LinkedList<EntityInvalidationInfo>());
    this.wrefNotificationInfoList = Collections.synchronizedList(new LinkedList<WrefNotificationInfo>());
    this.sysMsgReceiverList = Collections.synchronizedList(new LinkedList<WeakReference<RErefItem>>());
    this.maintenanceInterval = MAINTENANCE_INTERVAL;
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
    RErefItem eref = RErefItem.create(this.theEngine, new Entity(item));
    if (invalidator != null) {
      this.entityInvalidationInfoList.add(new EntityInvalidationInfo(eref, invalidator));
    }
    return eref;
  }

  RWrefItem createWeakHolder(RObjItem entity, RClosureItem listener) {
    RErefItem eref = (RErefItem)entity;
    WeakReference<Entity> wr;
    RWrefItem wref = RWrefItem.create(this.theEngine, new WeakReference<RErefItem>(eref));
    if (listener != null) {
      this.wrefNotificationInfoList.add(new WrefNotificationInfo(wref, listener));
    }
    return wref;
  }

  public RClosureItem createClosureOfNativeImpl(RModule mod, String name, int paramCount, /* String implFor, */ Object nativeImplTargetObject, Method nativeImpl) {
    return RClosureItem.create(this.theEngine, RClosureImpl.createNative(mod, name, paramCount, /* implFor, */ nativeImplTargetObject, nativeImpl), new RObjItem[0]);
  }

  private void doMaintainFull() {
    this.notifyPurgedEntities(this.entityInvalidationInfoList.size());
    this.notifyClearedWrefs(this.wrefNotificationInfoList.size());
    this.maintainSysMsgReceivers(sysMsgReceiverList.size());
  }

  private void doMaintainQuick() {
    int n;
    n = this.entityInvalidationInfoList.size();
    this.notifyPurgedEntities((n > 0)? n / 10 + 1: 0);  // limit by 1/10 of length
    n = this.wrefNotificationInfoList.size();
    this.notifyClearedWrefs((n > 0)? n / 10 + 1: 0);  // limit by 1/10 of length
    this.maintainSysMsgReceivers(1);
  }

  private void notifyPurgedEntities(int count) {
// /* DEBUG */ System.out.println("start notifyPurgedEntities");
    for (int i = 0; i < count; i++) {
      if (this.entityInvalidationInfoList.isEmpty()) { break; }
      EntityInvalidationInfo info = this.entityInvalidationInfoList.remove(0);  // dequeue
      if (info.weref.get() != null) {
        this.entityInvalidationInfoList.add(info);  // requeue
      } else {
// /* DEBUG */ System.out.println("Detected purged entity.");
        this.theEngine.taskMgr.createTask(
          RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, info.invalidator)
        .start();
      }
    }
// /* DEBUG */ System.out.println("end notifyPurgedEntities");
  }

  private void notifyClearedWrefs(int count) {
// /* DEBUG */ System.out.println("start notifyClearedWrefs");
    for (int i = 0; i < count; i++) {
      if (this.wrefNotificationInfoList.isEmpty()) { break; }
      WrefNotificationInfo info = this.wrefNotificationInfoList.remove(0);  // dequeue
      RWrefItem wref;
      if ((wref = info.wwref.get()) != null) {
        if (wref.get() != null) {
          this.wrefNotificationInfoList.add(info);  // requeue
        } else {
// /* DEBUG */ System.out.println("Detected cleared wref.");
          this.theEngine.taskMgr.createTask(
            RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, info.listener, new RObjItem[] { wref })
          .start();
        }
      } else {
        ;  // dispose
      }
    }
// /* DEBUG */ System.out.println("end notifyClearedWrefs");
  }

  private void maintainSysMsgReceivers(int count) {
    for (int i = 0; i < count; i++) {
      if (this.sysMsgReceiverList.isEmpty()) { break; }
      WeakReference<RErefItem> wr = this.sysMsgReceiverList.remove(0);  // dequeue
      if (wr.get() != null) {
        this.sysMsgReceiverList.add(wr);  // requeue
      } else {
        ;  // dispose
      }
    }
  }

  RErefItem createMbox(RTaskControl owner) {
    // create body
    RMbox b = RMbox.create(this.theEngine, owner);
    // wrap to RMBoxPItem(RObjItem)
    RMboxPItem bp = RMboxPItem.create(this.theEngine, b);
    // wrap to entity data
    RObjItem d = this.getStructItem(
      this.getDataConstr(new Cstr("sango.actor"), "mbox_p_ent_d$"),
      new RObjItem[] { bp });
    // create entity
    RErefItem bpe = this.createEntity(d, null);
    return bpe;
  }

  RMbox getMboxBody(RErefItem mboxE) {
    RMboxPItem p = (RMboxPItem)((RStructItem)mboxE.read()).getFieldAt(0);  // mbox_p mbox_p_ent_d$ -> mbox_p
    return p.mbox;
  }

  RMbox tryGetMboxBodyFromSenderEntity(RErefItem senderE) {
    RMbox b = null;
    RWrefItem mboxpEW = (RWrefItem)((RStructItem)senderE.read()).getFieldAt(0);
    RErefItem mboxpE = mboxpEW.get();
    if (mboxpE == null) {
      ;  // mbox is already GC'd.
    } else if (mboxpE instanceof RErefItem) {
      RObjItem mboxp = ((RStructItem)((RErefItem)mboxpE).read()).getFieldAt(0);
      if (mboxp instanceof RMboxPItem) {
        b = ((RMboxPItem)mboxp).mbox;
      } else {
        throw new IllegalArgumentException("Not <post_h>.");
      }
    } else {
      throw new IllegalArgumentException("Not <post_h>.");
    }
    return b;
  }

  public void notifySysMsg(RErefItem be) {
    this.sysMsgReceiverList.add(new WeakReference<RErefItem>(be));  // synchronized
  }

  WeakReference<RErefItem> pollSysMsgReceiver() {
    synchronized (this.sysMsgReceiverList) {  // is synchronization needed here?
      return (!this.sysMsgReceiverList.isEmpty())? this.sysMsgReceiverList.remove(0): null;
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

  private class EntityInvalidationInfo {
    WeakReference<RErefItem> weref;
    RClosureItem invalidator;

    EntityInvalidationInfo(RErefItem eref, RClosureItem invalidator) {
      this.weref = new WeakReference<RErefItem>(eref);
      this.invalidator = invalidator;
    }
  }

  private class WrefNotificationInfo {
    WeakReference<RWrefItem> wwref;
    RClosureItem listener;

    WrefNotificationInfo(RWrefItem wref, RClosureItem listener) {
      this.wwref = new WeakReference<RWrefItem>(wref);
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
