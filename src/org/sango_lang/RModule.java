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

public class RModule {
  RuntimeEngine theEngine;
  Cstr name;
  RObjItem[] slots;
  RModule[] modTab;
  RDataConstr[] dataConstrs;
  Map<String, RDataConstr> dataConstrDict;
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

  public Cstr getName() { return this.name; }

  static RModule create(RuntimeEngine eng, Module mod) throws IOException, FormatException {
    RModule m = new RModule();
    m.theEngine = eng;
    m.name = mod.name;

    m.slots = new RObjItem[mod.getSlotCount()];
    for (int i = 0; i < m.slots.length; i++) {
       m.slots[i] = null;
    }
    m.slots[Module.MSLOT_INDEX_NAME] = eng.memMgr.cstrToArrayItem(m.name);

    MDataConstr[] dcs = mod.getDataConstrs();
    m.dataConstrDict = new HashMap<String, RDataConstr>();
    m.dataConstrs = new RDataConstr[dcs.length] ;
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
        if (m.nativeImplClass == null) {
          String nativeImplClassName = FileSystem.getInstance().moduleNameToNativeClass(m.name);
          try {
            m.nativeImplClass = Class.forName(nativeImplClassName);
            Method instanceGetter = m.nativeImplClass.getMethod("getInstance", new Class[] { eng.getClass() });
            m.nativeImplInstance = instanceGetter.invoke(null, new Object[] { eng } );
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
        for (int j = 2; j < paramTypes.length; j++) {
          paramTypes[j] = RObjItem.class;
        }
        String nativeMethod = FileSystem.getInstance().funNameToNativeMethod(ci.name);
        try {
          Method nm = m.nativeImplClass.getMethod(nativeMethod, paramTypes);
          //  -- no check for return value type
          // if (nm.getReturnType() != RResult.class) {  // even subclass ok
            // throw new Exception("Invalid return type.");
          // }
          cir = RClosureImpl.createNative(m, ci.name, ci.paramCount, /* nativeMethod, */ m.nativeImplInstance, nm);
        } catch (Exception ex) {
          throw new IOException("Native implementation method link error for " + nativeMethod, ex);
        }
      }
      m.closureImplDict.put(ci.name, cir);
      m.closureImpls[i] = cir;
    }

    Module.ConstElem[] consts = mod.getConsts();
    m.consts = new RObjItem[consts.length];
    for (int i = 0; i < consts.length; i++) {
      m.consts[i] = convertConst(eng, consts[i]);
    }
    return m;
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
    Cstr[] mods = this.theEngine.modMgr.getMod(this.name).getModTab();
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
    // HERE
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
}
