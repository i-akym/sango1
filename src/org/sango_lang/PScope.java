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
  PEvalStmt evalStmt;  // set if funLevel == 0
  PClosure closure;  // set if funLevel > 0
  boolean enablesDefineTVar;
  boolean enablesDefineEVar;
  Map<String, PTVarSlot> tvarDict;
  Map<String, PEVarSlot> evarDict;
  Map<String, PTVarSlot> outerTVarDict;
  Map<String, PEVarSlot> outerEVarDict;
  List<PTVarSlot> envTVarList;
  List<PEVarSlot> envEVarList;
  List<PTVarSlot> givenTVarList;

  private PScope(PModule theMod) {
    this.theMod = theMod;
    this.funLevel = -2;
    this.enablesDefineTVar = true;
    this.enablesDefineEVar = true;
    this.tvarDict = new HashMap<String, PTVarSlot>();
    this.evarDict = new HashMap<String, PEVarSlot>();
    this.outerTVarDict = new HashMap<String, PTVarSlot>();
    this.outerEVarDict = new HashMap<String, PEVarSlot>();
  }

  static PScope create(PModule theMod) {
    return new PScope(theMod);
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
    s.envTVarList = new ArrayList<PTVarSlot>();
    s.envEVarList = new ArrayList<PEVarSlot>();
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

  PTVarSlot lookupTVar(String var) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTVarSlot v = this.tvarDict.get(var);
    if (v == null) {
      v = this.outerTVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupTVar(var);
  }

  PEVarSlot lookupEVar(String var) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PEVarSlot v = this.evarDict.get(var);
    if (v == null) {
      v = this.outerEVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupEVar(var);
  }

  void enableDefineTVar(boolean b) {
    this.enablesDefineTVar = b;
  }

  void enableDefineEVar(boolean b) {
    this.enablesDefineEVar = b;
  }

  boolean canDefineTVar(PTVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return this.enablesDefineTVar
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  boolean canDefineEVar(PEVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return this.enablesDefineEVar
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  PTVarSlot defineTVar(PTVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTVarSlot slot = PTVarSlot.create(varDef);
    this.tvarDict.put(varDef.name, slot);
    return slot;
  }

  PEVarSlot defineEVar(PEVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PEVarSlot slot = PEVarSlot.create(varDef);
    this.evarDict.put(varDef.name, slot);
    return slot;
  }

  PTVarSlot referSimpleTid(String id) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTVarSlot v = this.tvarDict.get(id);
    if (v == null) {
      v = this.outerTVarDict.get(id);
      if (v == null && this.parent != null) {
        v = this.parent.referSimpleTid(id);
        if (v != null) {
          this.outerTVarDict.put(id, v);
          if (this.parent.funLevel != this.funLevel) {  // in top scope of closure
            this.envTVarList.add(v);
          }
        }
      }
    }
    return v;
  }

  PEVarSlot referSimpleEid(String id) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PEVarSlot v = this.evarDict.get(id);
    if (v == null) {
      v = this.outerEVarDict.get(id);
      if (v == null && this.parent != null) {
        v = this.parent.referSimpleEid(id);
        if (v != null) {
          this.outerEVarDict.put(id, v);
          if (this.parent.funLevel != this.funLevel) {  // in top scope of closure
            this.envEVarList.add(v);
          }
        }
      }
    }
    return v;
  }

  // boolean isDefinedOuter(String id) {
    // return (this.funLevel >= 0)? this.outerDict.containsKey(id): false;
  // }

  // boolean isDefinedOuter(PVarSlot varSlot) {
    // return (this.funLevel >= 0)? this.outerDict.containsValue(varSlot): false;
  // }

  List<PTVarSlot> getEnvTVarList() { return this.envTVarList; }

  List<PEVarSlot> getEnvEVarList() { return this.envEVarList; }

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

  void addReferredTcons(List<PDefDict.TconInfo> tis) {
    for (int i = 0; i < tis.size(); i++) {
      this.addReferredTcon(tis.get(i));
    }
  }

  void addReferredTcon(PDefDict.TconInfo ti) {
    this.theMod.addReferredTcon(ti);
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

  PTVarDef getNewTVar(Parser.SrcInfo srcInfo, int variance) {
    PTVarDef v = PTVarDef.create(srcInfo, this.generateId(), variance, false);
    try {
      v.setupScope(this);
      v = v.resolveId();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return v;
  }

  PTypeSkel getEmptyListType(Parser.SrcInfo srcInfo) {
    PTVarDef nv = this.getNewTVar(srcInfo, Module.INVARIANT);
    return this.getLangDefinedType(srcInfo, "list", new PTypeDesc[] { nv }).getSkel();
  }

  PTypeSkel getEmptyStringType(Parser.SrcInfo srcInfo) {
    PTVarDef nv = this.getNewTVar(srcInfo, Module.INVARIANT);
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

  List<PTVarSlot> getGivenTVarList() {
    if (this.givenTVarList == null) {
      if (this.parent == null) {
        this.setupGivenTVarListForFun();
      } else if (this.parent.funLevel != this.funLevel) {
        this.setupGivenTVarListForClosure();
      } else {
        this.givenTVarList = this.parent.getGivenTVarList();
      }
    }
    return this.givenTVarList;
  }

  private void setupGivenTVarListForFun() {
    this.givenTVarList = new ArrayList<PTVarSlot>();
    PTypeSkel[] pts = this.evalStmt.getParamTypes();
    for (int i = 0; i < pts.length; i++) {
      List<PTVarSlot> justExtracted = pts[i].extractVars(this.givenTVarList);
      if (justExtracted != null) {
        this.givenTVarList.addAll(justExtracted);
      }
    }
  }

  private void setupGivenTVarListForClosure() {
    this.givenTVarList = new ArrayList<PTVarSlot>(this.parent.getGivenTVarList());
    PTypeSkel[] pts = this.closure.getParamDefinedTypes();
    for (int i = 0; i < pts.length; i++) {
      if (pts[i] != null) {
        List<PTVarSlot> justExtracted = pts[i].extractVars(this.givenTVarList);
        if (justExtracted != null) {
          this.givenTVarList.addAll(justExtracted);
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
