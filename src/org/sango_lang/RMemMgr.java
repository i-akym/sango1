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

public class RMemMgr {
  static final int MAINTENANCE_INTERVAL = 100;  // tentatively
  static final int INT_INT_CACHE_MIN = -10;
  static final int INT_INT_CACHE_MAX = 100;
  static final int BYTE_INT_CACHE_MIN = 0;
  static final int BYTE_INT_CACHE_MAX = 255;
  static final int CHAR_INT_CACHE_MIN = 0;
  static final int CHAR_INT_CACHE_MAX = 127;

  RuntimeEngine theEngine;
  List<ExistenceInvalidationInfo> entityInvalidationInfoList;
  List<WeakRefNotificationInfo> weakRefNotificationInfoList;
  List<WeakReference<RObjItem>> sysMsgReceiverList;
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
    this.entityInvalidationInfoList = Collections.synchronizedList(new LinkedList<ExistenceInvalidationInfo>());
    this.weakRefNotificationInfoList = Collections.synchronizedList(new LinkedList<WeakRefNotificationInfo>());
    this.sysMsgReceiverList = Collections.synchronizedList(new LinkedList<WeakReference<RObjItem>>());
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

  UniqueItem createUnique() {
    return new UniqueItem(this.theEngine);
  }

  RObjItem[] createImmutableExistence(RObjItem assoc, RClosureItem invalidator) {
    ExistenceItem e = new ExistenceItem(this.theEngine);
    SlotItem s = (assoc != null)? new SlotItem(this.theEngine, e, false, assoc): null;
    if (invalidator != null) {
      this.entityInvalidationInfoList.add(new ExistenceInvalidationInfo(e, invalidator));
    }
    return new RObjItem[] { e, s };
  }

  RObjItem[] createMutableExistence(RObjItem assoc, RClosureItem invalidator) {
    ExistenceItem e = new ExistenceItem(this.theEngine);
    SlotItem s = new SlotItem(this.theEngine, e, true, assoc);  // assoc != null
    if (invalidator != null) {
      this.entityInvalidationInfoList.add(new ExistenceInvalidationInfo(e, invalidator));
    }
    return new RObjItem[] { e, s };
  }

  WeakRefItem createWeakRef(ExistenceItem existence, RClosureItem listener) {
    WeakRefItem wr = new WeakRefItem(this.theEngine, existence);
    if (listener != null) {
      this.weakRefNotificationInfoList.add(new WeakRefNotificationInfo(wr, listener));
    }
    return wr;
  }

  public RClosureItem createClosureOfNativeImpl(RModule mod, String name, int paramCount, /* String implFor, */ Object nativeImplTargetObject, Method nativeImpl) {
    return RClosureItem.create(this.theEngine, RClosureImpl.createNative(mod, name, paramCount, /* implFor, */ nativeImplTargetObject, nativeImpl), new RObjItem[0]);
  }

  private void doMaintainFull() {
    this.notifyPurgedEntities(this.entityInvalidationInfoList.size());
    this.notifyClearedWeakRefs(this.weakRefNotificationInfoList.size());
    this.maintainSysMsgReceivers(sysMsgReceiverList.size());
  }

  private void doMaintainQuick() {
    int n;
    n = this.entityInvalidationInfoList.size();
    this.notifyPurgedEntities((n > 0)? n / 10 + 1: 0);  // limit by 1/10 of length
    n = this.weakRefNotificationInfoList.size();
    this.notifyClearedWeakRefs((n > 0)? n / 10 + 1: 0);  // limit by 1/10 of length
    this.maintainSysMsgReceivers(1);
  }

  private void notifyPurgedEntities(int count) {
// /* DEBUG */ System.out.println("start notifyPurgedEntities");
    for (int i = 0; i < count; i++) {
      if (this.entityInvalidationInfoList.isEmpty()) { break; }
      ExistenceInvalidationInfo info = this.entityInvalidationInfoList.remove(0);  // dequeue
      if (info.weakRef.get() != null) {
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

  private void notifyClearedWeakRefs(int count) {
// /* DEBUG */ System.out.println("start notifyClearedWeakRefs");
    for (int i = 0; i < count; i++) {
      if (this.weakRefNotificationInfoList.isEmpty()) { break; }
      WeakRefNotificationInfo info = this.weakRefNotificationInfoList.remove(0);  // dequeue
      WeakRefItem weakRef;
      if ((weakRef = info.wweakRef.get()) != null) {
        if (weakRef.get() != null) {
          this.weakRefNotificationInfoList.add(info);  // requeue
        } else {
// /* DEBUG */ System.out.println("Detected cleared wref.");
          this.theEngine.taskMgr.createTask(
            RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL, info.listener, new RObjItem[] { weakRef })
          .start();
        }
      } else {
        ;  // dispose
      }
    }
// /* DEBUG */ System.out.println("end notifyClearedWeakRefs");
  }

  private void maintainSysMsgReceivers(int count) {
    for (int i = 0; i < count; i++) {
      if (this.sysMsgReceiverList.isEmpty()) { break; }
      WeakReference<RObjItem> wr = this.sysMsgReceiverList.remove(0);  // dequeue
      if (wr.get() != null) {
        this.sysMsgReceiverList.add(wr);  // requeue
      } else {
        ;  // dispose
      }
    }
  }

  RStructItem createMbox(RTaskControl owner) {
    // create body
    RMbox b = RMbox.create(this.theEngine, owner);
    // wrap to RMBoxPItem(RObjItem)
    RMboxPItem bp = RMboxPItem.create(this.theEngine, b);
    // create box
    RObjItem[] es = this.createMutableExistence(bp, null);  // no invalidator
    RStructItem bpe = this.getStructItem(
      RDataConstr.create(new Cstr("sango.entity.box"), "box_h$", 2, "box_h", 1),
      new RObjItem[] { es[0], es[1] });
    return bpe;
  }

  RMbox getMboxBodyFromEntity(RObjItem mboxE) {  // <mboxp box_h>
    RMboxPItem mboxp = (RMboxPItem)this.readBox((RStructItem)mboxE);
    return mboxp.mbox;
  }

  RMbox tryGetMboxBodyFromSenderEntity(RObjItem senderE) {  // <<mboxp wbox_h> box_h>
    RStructItem mboxpEW = (RStructItem)this.readBox((RStructItem)senderE);  // <mboxp wbox_h>
    RStructItem mboxpE = (RStructItem)this.getBoxFromWeakBox(mboxpEW);  // <mboxp box_h> or null
    return (mboxpE != null)?  this.getMboxBodyFromEntity(mboxpE): null;
  }

  RObjItem readBox(RStructItem box) {
    ExistenceItem e = (ExistenceItem)box.getFieldAt(0);
    SlotItem s = (SlotItem)box.getFieldAt(1);
    return s.peekAssoc(e);
  }

  RStructItem getBoxFromWeakBox(RStructItem wbox) {  // wbox -> box
    WeakRefItem w = (WeakRefItem)wbox.getFieldAt(0);
    SlotItem s = (SlotItem)wbox.getFieldAt(1);
    ExistenceItem e = w.get();
    RStructItem box;
    if (e != null) {
      box = this.getStructItem(
        RDataConstr.create(new Cstr("sango.entity.box"), "box_h$", 2, "box_h", 1),
        new RObjItem[] { e, s });
    } else {
      box = null;
    }
    return box;
  }

  public void notifySysMsg(RObjItem be) {
    this.sysMsgReceiverList.add(new WeakReference<RObjItem>(be));  // synchronized
  }

  WeakReference<RObjItem> pollSysMsgReceiver() {
    synchronized (this.sysMsgReceiverList) {  // is synchronization needed here?
      return (!this.sysMsgReceiverList.isEmpty())? this.sysMsgReceiverList.remove(0): null;
    }
  }

  public class UniqueItem extends RObjItem {

    UniqueItem(RuntimeEngine e) {
      super(e);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.unique"), "u", 0);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }
  }

  public class ExistenceItem extends RObjItem {
    public UniqueItem uniqueness;  // ExistenceItem and SlotItem share this to avoid mutual strong reference

    ExistenceItem(RuntimeEngine e) {
      super(e);
      this.uniqueness = RMemMgr.this.createUnique();
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.entity.existence"), "existence", 0);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }
  }

  public class SlotItem extends RObjItem {
    Object uniqueness;
    boolean updatable;
    RObjItem assoc;

    SlotItem(RuntimeEngine e, ExistenceItem ex, boolean updatable, RObjItem assoc) {
      super(e);
      this.uniqueness = ex.uniqueness;
      this.updatable = updatable;
      this.assoc = assoc;
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.entity.existence"), this.updatable? "rw_slot": "ro_slot", 1);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }

    public RObjItem peekAssoc(ExistenceItem ex) {
      if (ex.uniqueness != this.uniqueness) {
        throw new IllegalArgumentException("Invalid slot.");
      }
      synchronized (this) {
        return this.assoc;
      }
    }

    public RObjItem replaceAssoc(ExistenceItem ex, RObjItem item) {
      if (ex.uniqueness != this.uniqueness) {
        throw new IllegalArgumentException("Invalid slot.");
      }
      synchronized (this) {
        RObjItem old = this.assoc;
        this.assoc = item;
        return old;
      }
    }
  }

  public class WeakRefItem extends RObjItem {
    WeakReference<ExistenceItem> weakRef;

    WeakRefItem(RuntimeEngine e, ExistenceItem existence) {
      super(e);
      this.weakRef = new WeakReference<ExistenceItem>(existence);
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }

    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.entity.existence"), "weak_ref", 0);
    }

    public void doHash(RNativeImplHelper helper, RClosureItem self) {
      helper.setReturnValue(helper.getIntItem(this.hashCode()));
    }

    public Cstr dumpInside() {
      return new Cstr(this.toString());
    }

    public ExistenceItem get() {
      return this.weakRef.get();
    }

    public void clear() {
      this.weakRef.clear();
    }
  }

  private class ExistenceInvalidationInfo {
    WeakReference<ExistenceItem> weakRef;
    RClosureItem invalidator;

    ExistenceInvalidationInfo(ExistenceItem existence, RClosureItem invalidator) {
      this.weakRef = new WeakReference<ExistenceItem>(existence);
      this.invalidator = invalidator;
    }
  }

  private class WeakRefNotificationInfo {
    WeakReference<WeakRefItem> wweakRef;
    RClosureItem listener;

    WeakRefNotificationInfo(WeakRefItem weakRef, RClosureItem listener) {
      this.wweakRef = new WeakReference<WeakRefItem>(weakRef);
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
