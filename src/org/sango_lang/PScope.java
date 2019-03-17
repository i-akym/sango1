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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class PScope {
  PModule theMod;
  PScope parent;
  int funLevel;  // -2:inactive  -1:in data def  0,1,..:in fun def
  boolean enablesDefineVar;
  Map<String, PVarSlot> varDict;
  Map<String, PVarSlot> outerDict;
  List<PVarSlot> envList;
  PEvalStmt evalStmt;  // set if funLevel == 0
  PClosure closure;  // set if funLevel > 0
  List<PVarSlot> givenTvarList;

  private PScope(PModule theMod) {
    this.theMod = theMod;
    this.funLevel = -2;
    this.enablesDefineVar = true;
    this.varDict = new HashMap<String, PVarSlot>();
    this.outerDict = new HashMap<String, PVarSlot>();
  }

  static PScope create(PModule theMod) {
    return new PScope(theMod);
  }

  PScope copy() {
    PScope s = new PScope(this.theMod);
    s.theMod = this.theMod;
    s.parent = this.parent;
    s.funLevel = this.funLevel;
    s.varDict.putAll(this.varDict);
    s.outerDict.putAll(this.outerDict);
    s.envList = this.envList;
    s.evalStmt = this.evalStmt;
    s.closure = this.closure;
    return s;
  }

  PScope start() {
    if (this.funLevel != -2) {
      throw new IllegalStateException("Cannot start.");
    }
    this.funLevel = -1;
    return this;
  }

  PScope defineFun(PEvalStmt evalStmt) {
    if (this.funLevel != -2) {
      throw new IllegalStateException("Cannot define function.");
    }
    this.funLevel = 0;
    this.evalStmt = evalStmt;
    return this;
  }

  PScope enterClosure(PClosure closure) {
    if (this.funLevel < 0) {
      throw new IllegalStateException("Cannot enter function.");
    }
    PScope s = new PScope(this.theMod);
    s.parent = this;
    s.funLevel = this.funLevel + 1;
    s.envList = new ArrayList<PVarSlot>();
    s.closure = closure;
    return s;
  }

  PScope enterInner() {
    if (this.funLevel < -1) {
    // if (this.funLevel < 0) {
      throw new IllegalStateException("Cannot enter inner scope.");
    }
    PScope s = new PScope(this.theMod);
    s.parent = this;
    s.funLevel = this.funLevel;
    s.evalStmt = this.evalStmt;
    s.closure = this.closure;
    return s;
  }

  // PScope exit() {  // common for defineFun(), enterFun(), enterInner()
    // return this.parent;
  // }

  PVarSlot lookupVar(String var) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PVarSlot v = this.varDict.get(var);
    if (v == null) {
      v = this.outerDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupVar(var);
  }

  void enableDefineVar(boolean b) {
    this.enablesDefineVar = b;
  }

  boolean canDefineVar(PVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return this.enablesDefineVar
      && !this.varDict.containsKey(varDef.name)
      && !this.outerDict.containsKey(varDef.name);
  }

  PVarSlot defineVar(PVarDef varDef) {
  // PVarSlot defineVar(int defineAs, String var, PTypeDesc type) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PVarSlot slot = PVarSlot.create(varDef);
    this.varDict.put(varDef.name, slot);
    // PVarSlot slot = PVarSlot.create(this, defineAs, type);
    // this.varDict.put(var, slot);
    return slot;
  }

  PVarSlot referSimpleId(String id) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PVarSlot v = this.varDict.get(id);
    if (v == null) {
      v = this.outerDict.get(id);
      if (v == null && this.parent != null) {
        v = this.parent.referSimpleId(id);
        if (v != null) {
          this.outerDict.put(id, v);
          if (this.parent.funLevel != this.funLevel) {  // in top scope of closure
            this.envList.add(v);
          }
        }
      }
    }
    return v;
  }

  boolean isDefinedOuter(String id) {
    return (this.funLevel >= 0)? this.outerDict.containsKey(id): false;
  }

  boolean isDefinedOuter(PVarSlot varSlot) {
    return (this.funLevel >= 0)? this.outerDict.containsValue(varSlot): false;
  }

  boolean areSameVarNamesDefined(PScope s) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return this.varDict.keySet().equals(s.varDict.keySet());
  }

  void mergeOuterDict(PScope s) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    this.outerDict.putAll(s.outerDict);
  }

  List<PVarSlot> getEnvList() { return this.envList; }

  Compiler getCompiler() { return this.theMod.theCompiler; }

  Cstr myModName() {
    return this.theMod.name;
  }

  Cstr resolveModId(String id) {
    return this.theMod.resolveModId(id);
  }

  PDefDict.EidProps resolveEid(PExprId id) throws CompileException {
    return this.theMod.resolveEid(id);
  }

  PDefDict.TconInfo resolveTconDirect(PDefDict.TconKey key) throws CompileException {
    return this.theMod.resolveTconDirect(key);
  }

  PDefDict.TconInfo resolveTcon(String mod, String name) throws CompileException {
    return this.theMod.resolveTcon(mod, name);
  }

  PTypeRef getLangDefinedType(Parser.SrcInfo srcInfo, String tcon, PTypeDesc[] paramTypeDescs) {
    PTypeRef t = null;
    try {
      t = PTypeRef.getLangDefinedType(srcInfo, tcon, paramTypeDescs);
      t.scope = this;  // needed for resolveId only
      // t = t.setupScope(this);
      t = t.resolveId();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return t;
  }

  PTypeRef getLangPrimitiveType(Parser.SrcInfo srcInfo, String tcon) {
    return this.getLangDefinedType(srcInfo, tcon, new PTypeDesc[0]);
  }

  PVarDef getNewTvar(Parser.SrcInfo srcInfo) {
    PVarDef v = PVarDef.create(srcInfo, PVarDef.CAT_TYPE_PARAM, null, this.generateId());
    try {
      // v.setPolymorphic(true);
      v.setupScope(this);
      v = v.resolveId();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return v;
  }

  PTypeSkel getEmptyListType(Parser.SrcInfo srcInfo) {
    PVarDef nv = this.getNewTvar(srcInfo);
    return this.getLangDefinedType(srcInfo, "list", new PTypeDesc[] { nv }).getSkel();
  }

  PTypeSkel getEmptyStringType(Parser.SrcInfo srcInfo) {
    PVarDef nv = this.getNewTvar(srcInfo);
    return this.getLangDefinedType(srcInfo, "string", new PTypeDesc[] { nv }).getSkel();
  }

  PTypeRef getCharStringType(Parser.SrcInfo srcInfo) {
    PTypeId c = PTypeId.create(srcInfo, /* PModule.MOD_ID_LANG, */ PModule.MOD_ID_LANG, "char", false);
    PTypeDesc ct;
    try {
      ct = c.setupScope(this);
      ct = ct.resolveId();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return this.getLangDefinedType(srcInfo, "string", new PTypeDesc[] { ct });
  }

  PTypeRefSkel getLangDefinedTypeSkel(Parser.SrcInfo srcInfo, String tcon, PTypeSkel[] paramTypeSkels) throws CompileException {
    PDefDict.TconInfo tconInfo = this.theMod.resolveTcon(/* PModule.MOD_ID_LANG, */ PModule.MOD_ID_LANG, tcon);
    if (tconInfo == null) {
      throw new RuntimeException("Internal error.");
    }
    return PTypeRefSkel.create(this.theMod.theCompiler, srcInfo, tconInfo, false, paramTypeSkels);
  }

  List<PVarSlot> getGivenTvarList() {
    if (this.givenTvarList == null) {
      if (this.parent == null) {
        this.setupGivenTvarListForFun();
      } else if (this.parent.funLevel != this.funLevel) {
        this.setupGivenTvarListForClosure();
      } else {
        this.givenTvarList = this.parent.getGivenTvarList();
      }
    }
    return this.givenTvarList;
  }

  private void setupGivenTvarListForFun() {
    this.givenTvarList = new ArrayList<PVarSlot>();
    PTypeSkel[] pts = this.evalStmt.getParamTypes();
    for (int i = 0; i < pts.length; i++) {
      List<PVarSlot> justExtracted = pts[i].extractVars(this.givenTvarList);
      if (justExtracted != null) {
        this.givenTvarList.addAll(justExtracted);
      }
    }
  }

  private void setupGivenTvarListForClosure() {
    this.givenTvarList = new ArrayList<PVarSlot>(this.parent.getGivenTvarList());
    PTypeSkel[] pts = this.closure.getParamDefinedTypes();
    for (int i = 0; i < pts.length; i++) {
      if (pts[i] != null) {
        List<PVarSlot> justExtracted = pts[i].extractVars(this.givenTvarList);
        if (justExtracted != null) {
          this.givenTvarList.addAll(justExtracted);
        }
      }
    }
  }

  String getFunOfficial() {
    if (this.funLevel < 0) { throw new IllegalStateException("Not in fun."); }
    return (this.funLevel == 0)? this.evalStmt.official: this.parent.getFunOfficial();
  }

  String generateId() {
    return this.theMod.generateId();
  }
}
