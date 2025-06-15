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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import sni_sango.SNIlang;

public class RModule {
  RuntimeEngine theEngine;
  Cstr actualName;
  RObjItem[] slots;
  RModule[] modTab;
  RDataConstr[] dataConstrs;
  Map<String, RDataConstr> dataConstrDict;
  Map<String, FeatureImplInfo[]> featureImplDict;
  RClosureConstr[] closureConstrs;
  Map<String, RClosureImpl> closureImplDict;
  RClosureImpl[] closureImpls;
  RObjItem[] consts;
  Class<?> nativeImplClass;
  Object nativeImplInstance;
  RClosureItem initClosure;
  RClosureItem mainClosure;
  RTaskControl initTask;

  private RModule() {}

  public Cstr getName() { return this.actualName; }

  static RModule create(RuntimeEngine eng, Module mod) throws IOException, FormatException {
    RModule m = new RModule();
    m.theEngine = eng;
    m.actualName = mod.actualName;

    m.slots = new RObjItem[mod.getSlotCount()];
    for (int i = 0; i < m.slots.length; i++) {
       m.slots[i] = null;
    }
    m.slots[Module.MSLOT_INDEX_NAME] = eng.memMgr.cstrToArrayItem(m.actualName);

    MDataConstr[] dcs = mod.getDataConstrs();
    m.dataConstrs = new RDataConstr[dcs.length] ;
    m.dataConstrDict = new HashMap<String, RDataConstr>();
    for (int i = 0; i < dcs.length; i++) {
      MDataConstr dc = dcs[i];
      RDataConstr rdc = RDataConstr.create(mod.getModAt(dc.modIndex), dc.name, dc.attrCount, dc.tcon, dc.tparamCount);
      m.dataConstrs[i] = rdc;
      if (dc.modIndex == Module.MOD_INDEX_SELF) {
        m.dataConstrDict.put(dc.name, rdc);
      }
    }

    MClosureConstr[] ccs = mod.getClosureConstrs();
    m.closureConstrs = new RClosureConstr[ccs.length];
    for (int i = 0; i < ccs.length; i++) {
      m.closureConstrs[i] = RClosureConstr.create(ccs[i].modIndex, ccs[i].name, ccs[i].envCount);
    }

    MClosureImpl[] cis = mod.getClosureImpls();
    m.closureImplDict =  new HashMap<String, RClosureImpl>();
    m.closureImpls = new RClosureImpl[cis.length];
    for (int i = 0; i < cis.length; i++) {
      MClosureImpl ci = cis[i];
      RClosureImpl cir = null;
      if (ci.codeBlock != null) {
        cir = RClosureImpl.createVMCode(
          m,
          ci.name,
          ci.paramCount,
          RInstruction.internalize(ci.codeBlock),
          ci.srcInfoTab,
          ci.varCount);
      } else {
        cir = linkNativeMethod(ci, m);
      }
      m.closureImplDict.put(ci.name, cir);
      m.closureImpls[i] = cir;
    }

    Module.ConstElem[] consts = mod.getConsts();
    m.consts = new RObjItem[consts.length];
    for (int i = 0; i < consts.length; i++) {
      m.consts[i] = convertConst(eng, consts[i]);
    }

    m.featureImplDict = new HashMap<String, FeatureImplInfo[]>();
    MDataDef[] dds = mod.getDataDefs();
    for (int i = 0; i < dds.length; i++) {
      MDataDef dd = dds[i];
      if (dd.featureImpls.length > 0) {
        FeatureImplInfo[] fis = new FeatureImplInfo[dd.featureImpls.length];
        for (int j = 0; j < fis.length; j++) {
          MFeatureImplDef fi = dd.featureImpls[j];
          MFeature f = fi.provided;
          fis[j] = new FeatureImplInfo(f.modIndex, f.name, fi.getter);
// /* DEBUG */ System.out.print(dd.tcon); System.out.print(f.modIndex); System.out.print(f.name); System.out.println(fi.getter);
        }
        m.featureImplDict.put(dd.tcon, fis);
      }
    }

    return m;
  }

  private static RClosureImpl linkNativeMethod(MClosureImpl ci, RModule m) throws IOException, FormatException {
    return ci.name.startsWith("_builtin_")?
      linkNativeMethodBuiltin(ci, m):
      linkNativeMethodGeneric(ci, m);
  }

  private static RClosureImpl linkNativeMethodBuiltin(MClosureImpl ci, RModule m) throws IOException, FormatException {
    // all '_builtin_xxx' will be converted and forwarded to sni__builtin by runtime engine
    if (ci.paramCount != 1) { throw new FormatException("Invalid parameter count for " + ci.name + "."); }
    Class[] paramTypes = new Class[3];
    paramTypes[0] = RNativeImplHelper.class;
    paramTypes[1] = RClosureItem.class;  // self
    paramTypes[2] = RObjItem.class;  // param chunk
    RClosureImpl cir;
    String nativeMethod = FileSystem.getInstance().funNameToNativeMethod("_builtin");
    // String nativeMethod = FileSystem.getInstance().funNameToNativeMethod(ci.name);
    try {
      Method nm = m.getClass().getMethod(nativeMethod, paramTypes);
      //  -- no check for return value type
      // if (nm.getReturnType() != RResult.class) {  // even subclass ok
        // throw new Exception("Invalid return type.");
      // }
      cir = RClosureImpl.createNative(m, ci.name, ci.paramCount, m, nm);
    } catch (Exception ex) {
      throw new IOException("Native implementation method link error for " + nativeMethod, ex);
    }
    return cir;
  }

  private static RClosureImpl linkNativeMethodGeneric(MClosureImpl ci, RModule m) throws IOException, FormatException {
    if (m.nativeImplClass == null) {
      String nativeImplClassName = FileSystem.getInstance().moduleNameToNativeClass(m.actualName);
      try {
        m.nativeImplClass = Class.forName(nativeImplClassName);
        Method instanceGetter = m.nativeImplClass.getMethod("getInstance", new Class[] { m.theEngine.getClass() });
        m.nativeImplInstance = instanceGetter.invoke(null, new Object[] { m.theEngine } );
      } catch (ClassNotFoundException ex) {
        throw new IOException("Native implementation class not found - " + nativeImplClassName);
      } catch (NoSuchMethodException ex) {
        throw new IOException("Native implementation getter \"getInstance(RuntimeEngine)\" not found in " + nativeImplClassName);
      } catch (Exception ex) {
        throw new IOException("Native implementation get error in " + nativeImplClassName + " - " + ex.toString());
      }
    }
    Class[] paramTypes = new Class[ci.paramCount + 2];
    paramTypes[0] = RNativeImplHelper.class;
    paramTypes[1] = RClosureItem.class;  // self
    for (int i = 2; i < paramTypes.length; i++) {
      paramTypes[i] = RObjItem.class;
    }
    RClosureImpl cir;
    String nativeMethod = FileSystem.getInstance().funNameToNativeMethod(ci.name);
    try {
      Method nm = m.nativeImplClass.getMethod(nativeMethod, paramTypes);
      //  -- no check for return value type
      // if (nm.getReturnType() != RResult.class) {  // even subclass ok
        // throw new Exception("Invalid return type.");
      // }
      cir = RClosureImpl.createNative(m, ci.name, ci.paramCount, m.nativeImplInstance, nm);
    } catch (Exception ex) {
      throw new IOException("Native implementation method link error for " + nativeMethod, ex);
    }
    return cir;
  }

  private static RObjItem convertConst(RuntimeEngine eng, Module.ConstElem c) {
    if (c instanceof Module.NilConstElem) {
      return eng.memMgr.getListNilItem();
    } else if (c instanceof Module.IntConstElem) {
      return eng.memMgr.getIntItem(((Module.IntConstElem)c).value);
    } else if (c instanceof Module.RealConstElem) {
      return eng.memMgr.getRealItem(((Module.RealConstElem)c).value);
    } else if (c instanceof Module.CstrConstElem) {
      return eng.memMgr.cstrToArrayItem(((Module.CstrConstElem)c).value);
    } else {  // HERE
      return null;  // dummy
    }
  }

  void resolveRefs() {
    // /* DEBUG */ System.out.println("resolving module references...");
    Cstr[] mods = this.theEngine.modMgr.getMod(this.actualName).getModTab().getAllMods();
    this.modTab = new RModule[mods.length];
    for (int i = 0; i < mods.length; i++) {
      this.modTab[i] = this.theEngine.modMgr.getRMod(mods[i]);
    }
    for (int i = 0; i < this.closureConstrs.length; i++) {
      RClosureConstr cc = this.closureConstrs[i];
      if (cc.impl == null) {
        cc.impl = this.modTab[cc.modIndex].getClosureImpl(cc.name);
      }
    }
    RClosureImpl ici = this.closureImplDict.get(Module.FUN_INIT);
    if (ici != null) {
      this.initClosure = RClosureItem.create(this.theEngine, ici, null);
    }
    RClosureImpl mci = this.closureImplDict.get(Module.FUN_MAIN);
    if (mci != null) {
      this.mainClosure = RClosureItem.create(this.theEngine, mci, null);
    }
    // /* DEV */ if (this.name.toJavaString().equals("f")) {
    // /* DEV */   RClosureItem g = this.getClosure("_bridge_bar_as_a'foo");
    // /* DEV */   System.out.print("FEATURE put getter in table. "); System.out.println(g != null);
    // /* DEV */   this.featureImplDict.put(
    // /* DEV */     "bar",
    // /* DEV */     new FeatureImplInfo[] { new FeatureImplInfo(this.name, "a'foo", g)
    // /* DEV */   });
    // /* DEV */ }
  }

  void spawnInitTask() {
    if (this.initClosure != null) {
      this.initTask = this.theEngine.taskMgr.createTask(RTaskMgr.PRIO_DEFAULT, RTaskMgr.TASK_TYPE_INIT, this.initClosure);
      // this.initTask.start();
    }
  }

  RTaskControl getInitTask() {
    synchronized (this) {
      return this.initTask;
    }
  }

  void initialized(RObjItem i) {
    synchronized (this) {
      if (this.initTask == null) {
        throw new IllegalStateException("Not in initializing state.");
      }
      if (this.slots[Module.MSLOT_INDEX_INITD] != null) {
        throw new IllegalStateException("Already initialized.");
      }
      this.slots[Module.MSLOT_INDEX_INITD] = i;
      this.initTask = null;
    }
  }

  RObjItem getConstItem(int index) {
    return this.consts[index];
  }

  RDataConstr getDataConstr(int index) {
    return this.dataConstrs[index];
  }

  RDataConstr getDataConstr(String name) {
    return this.dataConstrDict.get(name);
  }

  RClosureConstr getClosureConstrAt(int index) {
    return this.closureConstrs[index];
  }

  RClosureImpl getClosureImplAt(int index) {
    return this.closureImpls[index];
  }

  RClosureImpl getClosureImpl(String name) {
    return this.closureImplDict.get(name);
  }

  RClosureItem getClosure(String name) {
    RClosureImpl i = this.getClosureImpl(name);
    return (i == null)?  null: RClosureItem.create(this.theEngine, i, null);
  }

  RClosureItem getInitClosure() { return this.initClosure; }

  RClosureItem getMainClosure() { return this.mainClosure; }


// builtin-function implementations

  public void sni__builtin(RNativeImplHelper helper, RClosureItem self, RObjItem generic) {
    if (self.impl.name.startsWith("_builtin_feature_get_")) {
      this.feature_get(helper, self, generic);
    } else {
      throw new RuntimeException("Invalid native implementation: " + self.impl.name);
    }
  }

  // eval <*T> *X _builtin_feature_get_xxx -> <IMPL>
  void feature_get(RNativeImplHelper helper, RClosureItem self, RObjItem generic) {
    if (helper.getAndClearResumeInfo() == null) {
      RObjItem obj = generic;
      RClosureItem g = helper.getCore().getFeatureImplGetter(
        obj,
        this.actualName,
        self.impl.name.substring(21));  // feature name = after "_builtin_feature_get_"
      if (g == null) {
        throw new RuntimeException("Feature impl not found. " + obj.dump().toJavaString());
        // helper.setReturnValue(SNIlang.getMaybeItem(helper, null));
      } else {
        helper.scheduleInvocation(g, new RObjItem[0], self);  // ignore arg
      }
    } else {
      helper.setReturnValue(helper.getInvocationResult().getReturnValue());
    }
  }

  RClosureItem getFeatureImplGetter(String tcon, Cstr featureModName, String featureName) {
// /* DEBUG */ System.out.print(tcon); System.out.print(featureModName.repr()); System.out.println(featureName);
    RClosureItem g = null;
    FeatureImplInfo[] fis = this.featureImplDict.get(tcon);
    if (fis != null) {
      for (int i = 0; g == null && i < fis.length; i++) {  // sequential search
        FeatureImplInfo fi = fis[i];
// /* DEBUG */ System.out.println(fi.featureModIndex); System.out.println(this.modTab[fi.featureModIndex].name.repr());
        if (this.modTab[fi.featureModIndex].actualName.equals(featureModName) && fi.featureName.equals(featureName)) {
          g = RClosureItem.create(this.theEngine, this.closureImplDict.get(fi.getter), null);
        }
      }
    }
    return g;
  }

  static class FeatureImplInfo {
    int featureModIndex;
    String featureName;
    String getter;

    FeatureImplInfo(int featureModIndex, String featureName, String getter) {
      this.featureModIndex = featureModIndex;
      this.featureName = featureName;
      this.getter = getter;
    }
  }
}
