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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class RNativeImplHelper {
  RuntimeEngine theEngine;
  RFrame frame;
  RResult result;
  Object resumeInfo;
  RFrame frameToExecNext;
  Core core;

  RNativeImplHelper(RuntimeEngine eng, RFrame frame) {
    this.theEngine = eng;
    this.frame = frame;
    this.core = new Core();
  }

  public RuntimeEngine getRuntimeEngine() { return this.theEngine; }

  public Core getCore() { return this.core; }

  // environment

  public Locale getLocale() { return this.theEngine.getLocale(); }

  public Cstr getProgName() { return this.theEngine.getProgName(); }

  public List<Cstr> getArgs() { return this.theEngine.getArgs(); }

  public List<File> getSysLibPaths() { return this.theEngine.getSysLibPaths(); }

  // handling object

  public RStructItem getBoolItem(boolean b) {
    return this.theEngine.getClientHelper().getBoolItem(b);
  }

  public boolean boolItemToBoolean(RStructItem b) {
    return this.theEngine.getClientHelper().boolItemToBoolean(b);
  }

  public RIntItem getByteItem(int b) {
    return this.theEngine.getClientHelper().getByteItem(b);
  }

  public RIntItem getCharItem(int c) {
    return this.theEngine.getClientHelper().getCharItem(c);
  }

  public RIntItem getIntItem(int i) {
    return this.theEngine.getClientHelper().getIntItem(i);
  }

  public RRealItem getRealItem(double d) {
    return this.theEngine.getClientHelper().getRealItem(d);
  }

  public RRealItem getNaNItem() {
    return this.theEngine.getClientHelper().getNaNItem();
  }

  public RRealItem getPosInfItem() {
    return this.theEngine.getClientHelper().getPosInfItem();
  }

  public RRealItem getNegInfItem() {
    return this.theEngine.getClientHelper().getNegInfItem();
  }

  public RStructItem getVoidItem() {
    return this.theEngine.getClientHelper().getVoidItem();
  }

  public RListItem.Nil getListNilItem() {
    return this.theEngine.getClientHelper().getListNilItem();
  }

  public RListItem.Cell createListCellItem() {
    return this.theEngine.getClientHelper().createListCellItem();
  }

  public RDataConstr getDataConstr(Cstr modName, String name) {
    return this.theEngine.getClientHelper().getDataConstr(modName, name);
  }

  public RStructItem getStructItem(RDataConstr dataConstr, RObjItem[] attrs) {
    return this.theEngine.getClientHelper().getStructItem(dataConstr, attrs);
  }

  public RStructItem getTupleItem(RObjItem[] elems) {
    return this.theEngine.getClientHelper().getTupleItem(elems);
  }

  public RArrayItem createArrayItem(int size) {
    return this.theEngine.getClientHelper().createArrayItem(size);
  }

  public RArrayItem cstrToArrayItem(Cstr cstr) {
    return this.theEngine.getClientHelper().cstrToArrayItem(cstr);
  }

  public Cstr arrayItemToCstr(RArrayItem array) {
    return this.theEngine.getClientHelper().arrayItemToCstr(array);
  }

  public RListItem listToListItem(List<? extends RObjItem> os) {
    return this.theEngine.getClientHelper().listToListItem(os);
  }

  public RClosureItem createClosureOfNativeImplHere(String name, int paramCount, Object nativeImplTargetObject, Method nativeImpl) {
    return this.theEngine.memMgr.createClosureOfNativeImpl(
      this.frame.closure.impl.mod,
      name, paramCount, nativeImplTargetObject, nativeImpl);
  }

  public boolean objEquals(RObjItem item0, RObjItem item1) {
    return item0.objEquals(this.frame, item1);
  }

  public RVMItem popOstack() { return this.frame.os.pop(); }

  public void pushOstack(RVMItem item) {
    this.frame.os.push(item);
  }

  public RObjItem[] popOStackMultipleObjItemsPushOrder(int count) {
    return this.frame.os.popMultipleObjItemsPushOrder(count);
  }

  // handling result

  public void setCatchException(boolean b) {
    this.frame.resultHandling = b? RFrame.ACCEPT_ALL_RESULT: RFrame.ACCEPT_RET_RESULT;
  }

  public RResult getResult() {
    if (this.result == null /* && !this.toReexec */ ) {
      this.result = this.theEngine.memMgr.createResult();
    }
    return this.result;
  }

  public void setReturnValue(RObjItem v) {
    if (this.result != null) {
      this.result.setReturnValue(v);
    } else {
      this.result = this.theEngine.memMgr.createResult(v);
    }
  }

  public RExcInfoItem getExcInfo() {
    return this.frame.getExcInfo();
  }

  public RStructItem createException(RObjItem excDesc, Cstr msg, RObjItem org) {
    RObjItem orgExc;
    if (org != null) {
      RDataConstr dcValue = this.getDataConstr(Module.MOD_LANG, "value$");
      orgExc = this.getStructItem(dcValue, new RObjItem[] { org });
    } else {
      RDataConstr dcNone = this.getDataConstr(Module.MOD_LANG, "none$");
      orgExc = this.getStructItem(dcNone, new RObjItem[0]);
    }
    RDataConstr dce = this.getDataConstr(Module.MOD_LANG, "exception$");
    return this.getStructItem(
      dce,
      new RObjItem[] { excDesc, this.cstrToArrayItem(msg), this.getExcInfo(), orgExc });
  }

  public void setException(RObjItem e) {
    if (this.result == null) {
      this.result = this.theEngine.memMgr.createResult();
    }
    this.result.setException(e);
  }

  // for further invocation

  Object getResumeInfo() { return this.resumeInfo; }

  public Object getAndClearResumeInfo() {
    Object o = this.resumeInfo;
    this.resumeInfo = null;
    return o;
  }

  public void scheduleInvocation(RClosureItem c, RObjItem[] params, Object resumeInfo) {
    if (resumeInfo == null) {
      throw new IllegalArgumentException("Resume info missing.");
    }
    this.frameToExecNext = this.frame.createChild(c, params);
    this.resumeInfo = resumeInfo;
  }

  RFrame getFrameToExecNext() {
    return (this.frameToExecNext != null)? this.frameToExecNext: this.frame;
  }

  public RResult getInvocationResult() {
    return this.frame.result;
  }

  // misc

  public Version getVersion() { return RuntimeEngine.getVersion(); }

  public void mayRunLong() {
    this.theEngine.taskMgr.taskMayRunLong(this.frame.theTaskControl);
  }

  public class Core {  // for core features
    boolean toReleaseTask;

    Core() {}

    // entity

    public RErefItem createEntity(RObjItem item, RClosureItem invalidator) {
      return RNativeImplHelper.this.theEngine.memMgr.createEntity(item, invalidator);
    }

    public RWrefItem createWeakHolder(RErefItem eref, RClosureItem listener) {
      return RNativeImplHelper.this.theEngine.memMgr.createWeakHolder(eref, listener);
    }

    // scheduling control

    public void setResumeInfo(Object o) {
      RNativeImplHelper.this.resumeInfo = o;
    }

    public void releaseTask() {
      this.toReleaseTask = true;
    }

    boolean mustReleaseTask() { return this.toReleaseTask; }

    void restart() {
      this.toReleaseTask = false;
    }

    public void yield() {
      RNativeImplHelper.this.frame.theTaskControl.yield();
    }

    // multi-actor

    public RActorHItem myActorH() {
      return RNativeImplHelper.this.frame.theTaskControl.actorH;
    }

    public RObjItem getActorState(RActorHItem actorH) {
      return RNativeImplHelper.this.theEngine.taskMgr.getActorState(actorH.taskControl);
    }

    public RObjItem spawnActor(RClosureItem c) {  // returns async_h
      RTaskControl tc = RNativeImplHelper.this.theEngine.taskMgr.createTask(
        RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_APPL,  c);
      RDataConstr asyncH = RNativeImplHelper.this.getDataConstr(new Cstr("sango.actor"), "async_h$");
      return RNativeImplHelper.this.getStructItem(asyncH, new RObjItem[] { tc.actorH, tc.asyncResultH });
    }

    public void startActor(RActorHItem a) {
      a.taskControl.start();
    }

    public void setPriority(RActorHItem a, int p) {
      a.taskControl.setPriority(p);
    }

    public void setBackground(RActorHItem a, boolean b) {
      a.taskControl.setBackground(b);
    }

    public boolean waitFor(RActorHItem a, Integer expiration) {
      return RNativeImplHelper.this.frame.theTaskControl.join(a.taskControl, expiration);
    }

    public List<RActorHItem> waitFor(List<RActorHItem> as, Integer expiration) {
      List<RTaskControl> ts = new ArrayList<RTaskControl>();
      for (int i = 0; i < as.size(); i++) {
        ts.add(as.get(i).taskControl);
      }
      List<RTaskControl> ets = RNativeImplHelper.this.frame.theTaskControl.joinSomeOf(ts, expiration);
      List<RActorHItem> eas = new ArrayList<RActorHItem>();
      for (int i = 0; i < ets.size(); i++) {
        eas.add(ets.get(i).actorH);
      }
      return eas;
    }

    public RResult peekAsyncResult(RAsyncResultHItem a) {
      return RNativeImplHelper.this.theEngine.taskMgr.peekTaskResult(a.taskControl);
    }

    public void addActorMonitor(RObjItem actorH, RObjItem postH) {
      RStructItem p = (RStructItem)postH;
      RErefItem mboxpEWE = (RErefItem)p.getFieldAt(0);
      RWrefItem mboxpEW = (RWrefItem)((RStructItem)mboxpEWE.read()).getFieldAt(0);
      RErefItem mboxpE = mboxpEW.get();
      if (mboxpE == null) {
        ;  // mbox is already GC'd.
      } else if (mboxpE instanceof RErefItem) {
        RObjItem mboxp = ((RStructItem)((RErefItem)mboxpE).read()).getFieldAt(0);
        if (mboxp instanceof RMboxPItem) {
          RNativeImplHelper.this.theEngine.taskMgr.addActorMonitor((RActorHItem)actorH, mboxpEWE);
        } else {
          throw new IllegalArgumentException("Not <post_h>.");
        }
      } else {
        throw new IllegalArgumentException("Not <post_h>.");
      }
    }

    public void removeActorMonitor(RObjItem actorH, RObjItem postH) {
      RStructItem p = (RStructItem)postH;
      RErefItem mboxpEWE = (RErefItem)p.getFieldAt(0);
      RNativeImplHelper.this.theEngine.taskMgr.removeActorMonitor((RActorHItem)actorH, mboxpEWE);
    }

    // messaging

    public RObjItem createMbox() {
      return RNativeImplHelper.this.theEngine.taskMgr.createMbox(RNativeImplHelper.this.frame.theTaskControl);
    }

    public void notifySysMsg(RObjItem bpew) {
      RNativeImplHelper.this.theEngine.memMgr.notifySysMsg(bpew);
    }

    public RActorHItem getOwnerOfMbox(RObjItem b) {
      return this.getBoxBody(b).owner.actorH;
    }

    public void putMsg(RObjItem bp, RObjItem m) {
      ((RMboxPItem)bp).mbox.putMsg(m);
    }

    public List<RObjItem> listenMboxes(List<RObjItem> bs, Integer expiration) {
      List<RMbox> boxes = new ArrayList<RMbox>();
      for (int i = 0; i < bs.size(); i++) {
        boxes.add(this.getBoxBody(bs.get(i)));
      }
      List<RMbox> rboxes = RNativeImplHelper.this.frame.theTaskControl.listenMboxes(boxes, expiration);
      List<RObjItem> receivables = new ArrayList<RObjItem>();
      for (int i = 0; i < rboxes.size(); i++) {
        receivables.add(rboxes.get(i).getHandleItem());
      }
      return receivables;
    }

    public RObjItem receiveMsg(RObjItem b) {
      return this.getBoxBody(b).receiveMsg();
    }

    RMbox getBoxBody(RObjItem mboxH) {
      RStructItem h = (RStructItem)mboxH;
      RErefItem e =(RErefItem)h.getFieldAt(0);
      RMboxPItem p = (RMboxPItem)((RStructItem)e.read()).getFieldAt(0);  // mbox_p mbox_p_ent_d$ -> mbox_p
      return p.mbox;
    }

    // runtime features

    public RClosureItem getClosureItem(Cstr modName, String official) {
      RClosureItem c = null;
      RModule m = RNativeImplHelper.this.theEngine.modMgr.getRMod(modName);
      if (m != null) {
        RClosureImpl ci = m.getClosureImpl(official);
        if (ci != null) {
          c = RClosureItem.create(RNativeImplHelper.this.theEngine, ci, new RObjItem[0]);
        }
      }
      return c;
    }

    public PDataDef getDataDef(Cstr modName, String dcon) {
      PDataDef dd = null;
      try {
        PDefDict d = this.getDefDictGetter().getReferredDefDict(modName);
        if (d == null) { return null; }
        PDefDict.EidProps ep = d.resolveEid(
          dcon, PExprId.CAT_DCON,
          Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
        if (ep == null) { return null; }
        dd = ep.defGetter.getDataDef();
      } catch (CompileException ex) {}
      return dd;
    }

    public PTypeRefSkel getFunType(Cstr modName, String official) {
      PTypeRefSkel t = null;
      try {
        PDefDict d = this.getDefDictGetter().getReferredDefDict(modName);
        if (d == null) { return null; }
        PDefDict.EidProps ep = d.resolveEid(
          official, PExprId.CAT_FUN_OFFICIAL, Module.ACC_PUBLIC + Module.ACC_PRIVATE);
        if (ep == null) { return null; }
        PFunDef fd = ep.defGetter.getFunDef();
        PTypeSkel.InstanciationBindings ibs = PTypeSkel.InstanciationBindings.create(PTypeSkelBindings.create());
        PTypeSkel[] pts = fd.getParamTypes();
        PTypeSkel[] tis = new PTypeSkel[pts.length + 1];
        for (int i = 0; i < pts.length; i++) {
          tis[i] = pts[i].instanciate(ibs);
        }
        tis[pts.length] = fd.getRetType().instanciate(ibs);
        PDefDict.TconKey tk = PDefDict.TconKey.create(Module.MOD_LANG, "fun");
        PDefDict.TconProps tp = PDefDict.TconProps.create(
          PTypeId.SUBCAT_NOT_FOUND, pts.length, Module.ACC_OPAQUE, null);
        PDefDict.TconInfo ti = PDefDict.TconInfo.create(tk, tp);
        t = PTypeRefSkel.create(this.getDefDictGetter(), null, ti, false, tis);
      } catch (CompileException ex) {}
      return t;
    }

    public void loadModuleOnDemand(Cstr modName) throws Exception {
      RNativeImplHelper.this.theEngine.modMgr.loadModuleOnDemand(modName);
    }

    public RActorHItem getModuleInitActorH(Cstr modName) {
      return RNativeImplHelper.this.theEngine.modMgr.getModuleInitActorH(modName);
    }

    public PDefDict.DefDictGetter getDefDictGetter() {
      return RNativeImplHelper.this.theEngine.modMgr;
    }

    public void requestShutdown(int exitCode, int timeout) {
      RNativeImplHelper.this.theEngine.requestShutdown(exitCode, timeout);
    }

    public void terminateOnAbnormalEnd(RActorHItem actorH) {
      RNativeImplHelper.this.theEngine.taskMgr.terminateOnAbnormalEnd(actorH.taskControl);
    }

    public void requestGC() {
      RNativeImplHelper.this.theEngine.memMgr.maintainFull();
    }
  }
}
