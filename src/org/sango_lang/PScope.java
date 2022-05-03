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
  List<PScope> parallelScopes;
  boolean inParallel;
  Map<String, PTypeVarDef> tvarDict;
  Map<String, PExprVarDef> evarDict;
  Map<String, PTypeVarDef> outerTVarDict;
  Map<String, PExprVarDef> outerEVarDict;
  List<PTypeVarSlot> envTVarList;
  List<PExprVarSlot> envEVarList;
  List<PTypeVarSlot> givenTVarList;

  private PScope(PModule theMod) {
    this.theMod = theMod;
    this.funLevel = -2;
    this.tvarDict = new HashMap<String, PTypeVarDef>();
    this.evarDict = new HashMap<String, PExprVarDef>();
    this.outerTVarDict = new HashMap<String, PTypeVarDef>();
    this.outerEVarDict = new HashMap<String, PExprVarDef>();
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
    s.envTVarList = new ArrayList<PTypeVarSlot>();
    s.envEVarList = new ArrayList<PExprVarSlot>();
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

  PScope enterInnerWithParallelScopes() {  // for PCaseClause
    PScope s = this.enterInner();
    s.parallelScopes = new ArrayList<PScope>();
    return s;
  }

  boolean isForParallel() {
    return this.parallelScopes != null;
  }

  PScope enterInnterParallel() {  // for PCasePtnMatch
    PScope s = this.enterInner();
    s.inParallel = true;
    this.parallelScopes.add(s);
    return s;
  }

  boolean isActuallyInParallel() {
    return this.inParallel && this.parent.parallelScopes.size() > 1;
  }

  PTypeVarDef lookupTVar(String var) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTypeVarDef v = this.tvarDict.get(var);
    if (v == null) {
      v = this.outerTVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupTVar(var);
  }

  PExprVarDef lookupEVar(String var) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PExprVarDef v = this.evarDict.get(var);
    if (v == null) {
      v = this.outerEVarDict.get(var);
    }
    return (v != null || this.parent == null)? v: this.parent.lookupEVar(var);
  }

  boolean canDefineTVar(PTypeVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return !this.isActuallyInParallel()  // inhibit when actually parallel
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  boolean canDefineEVar(PExprVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    return !this.isActuallyInParallel()  // inhibit when actually parallel
      && !this.tvarDict.containsKey(varDef.name)
      && !this.evarDict.containsKey(varDef.name)
      && !this.outerTVarDict.containsKey(varDef.name)
      && !this.outerEVarDict.containsKey(varDef.name);
  }

  PTypeVarSlot defineTVar(PTypeVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTypeVarSlot slot;
    if (this.inParallel) {
      slot = this.parent.defineTVar(varDef);  // temporal impl - forward simply
    } else {
      slot = PTypeVarSlot.create(varDef);
      varDef.varSlot = slot;
      this.tvarDict.put(varDef.name, varDef);
    }
    return slot;
  }

  PExprVarSlot defineEVar(PExprVarDef varDef) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PExprVarSlot slot;
    if (this.inParallel) {
      slot = this.parent.defineEVar(varDef);  // temporal impl - forward simply
    } else {
      slot = PExprVarSlot.create(varDef);
      varDef.varSlot = slot;
      this.evarDict.put(varDef.name, varDef);
    }
    return slot;
  }

  PTypeVarDef referSimpleTid(String id) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PTypeVarDef v;
    if (this.inParallel) {
      v = this.parent.referSimpleTid(id);  // temporal impl - forward simply
    } else {
      v = this.tvarDict.get(id);
      if (v == null) {
        v = this.outerTVarDict.get(id);
        if (v == null && this.parent != null) {
          v = this.parent.referSimpleTid(id);
          if (v != null) {
            this.outerTVarDict.put(id, v);
            if (this.parent.funLevel != this.funLevel) {  // in top scope of closure
              this.envTVarList.add(v.varSlot);
            }
          }
        }
      }
    }
    return v;
  }

  PExprVarDef referSimpleEid(String id) {
    if (this.funLevel < -1) {
      throw new IllegalStateException("Not active.");
    }
    PExprVarDef v;
    if (this.inParallel) {
      v = this.parent.referSimpleEid(id);  // temporal impl - forward simply
    } else {
      v = this.evarDict.get(id);
      if (v == null) {
        v = this.outerEVarDict.get(id);
        if (v == null && this.parent != null) {
          v = this.parent.referSimpleEid(id);
          if (v != null) {
            this.outerEVarDict.put(id, v);
            if (this.parent.funLevel != this.funLevel) {  // in top scope of closure
              this.envEVarList.add(v.varSlot);
            }
          }
        }
      }
    }
    return v;
  }

  List<PTypeVarSlot> getEnvTVarList() {
    return this.inParallel?
      this.parent.getEnvTVarList():  // temporal impl - forward simply
      this.envTVarList;
  }

  List<PExprVarSlot> getEnvEVarList() {
    return this.inParallel?
      this.parent.getEnvEVarList():  // temporal impl - forward simply
      this.envEVarList;
  }

  Compiler getCompiler() { return this.theMod.theCompiler; }

  Cstr myModName() {
    return this.theMod.name;
  }

  void referredModId(Parser.SrcInfo si, String id) throws CompileException {
    if (id != null) {
      if (this.theMod.resolveModId(id) == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Unknown module id \"");
        emsg.append(id);
        emsg.append("\" at ");
        emsg.append(si);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
  }

  Cstr resolveModId(String id) {
    return this.theMod.resolveModId(id);
  }

  PDefDict.EidProps resolveEid(PExprId id) throws CompileException {
    return this.theMod.resolveEid(id);
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

  PTypeRef getLangDefinedType(Parser.SrcInfo srcInfo, String tcon, PType[] paramTypeDescs) {
    PTypeRef t = null;
    try {
      t = PTypeRef.getLangDefinedType(srcInfo, tcon, paramTypeDescs);
      t.scope = this;  // needed for resolveId only
      // t = t.setupScope(this);
      t = t.resolve();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return t;
  }

  PTypeRef getLangPrimitiveType(Parser.SrcInfo srcInfo, String tcon) {
    return this.getLangDefinedType(srcInfo, tcon, new PType[0]);
  }

  PTypeVarDef getNewTVar(Parser.SrcInfo srcInfo, int variance) {
    PTypeVarDef v = PTypeVarDef.create(srcInfo, this.generateId(), variance, false, null);
    try {
      v.setupScope(this);
      v = v.resolve();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return v;
  }

  PTypeSkel getEmptyListType(Parser.SrcInfo srcInfo) {
    PTypeVarDef nv = this.getNewTVar(srcInfo, Module.INVARIANT);
    return this.getLangDefinedType(srcInfo, "list", new PType[] { nv }).getSkel();
  }

  PTypeSkel getEmptyStringType(Parser.SrcInfo srcInfo) {
    PTypeVarDef nv = this.getNewTVar(srcInfo, Module.INVARIANT);
    return this.getLangDefinedType(srcInfo, "string", new PType[] { nv }).getSkel();
  }

  PTypeRef getCharStringType(Parser.SrcInfo srcInfo) {
    PType.Builder b = PType.Builder.newInstance();
    b.setSrcInfo(srcInfo);
    PType ct;
    try {
      b.addItem(PTypeId.create(srcInfo, PModule.MOD_ID_LANG, "char", false));
      ct = b.create();
      ct.setupScope(this);
      ct = ct.resolve();
    } catch (CompileException ex) {
      throw new RuntimeException("Internal error - " + ex.toString());
    }
    return this.getLangDefinedType(srcInfo, "string", new PType[] { ct });
  }

  PTypeRefSkel getLangDefinedTypeSkel(Parser.SrcInfo srcInfo, String tcon, PTypeSkel[] paramTypeSkels) throws CompileException {
    PDefDict.TconInfo tconInfo = this.theMod.resolveTcon(PModule.MOD_ID_LANG, tcon);
    if (tconInfo == null) {
      throw new RuntimeException("Internal error.");
    }
    return PTypeRefSkel.create(this.theMod.theCompiler, srcInfo, tconInfo, false, paramTypeSkels);
  }

  List<PTypeVarSlot> getGivenTVarList() {
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
    this.givenTVarList = new ArrayList<PTypeVarSlot>();
    PTypeSkel[] pts = this.evalStmt.getParamTypes();
    for (int i = 0; i < pts.length; i++) {
      List<PTypeVarSlot> justExtracted = pts[i].extractVars(this.givenTVarList);
      if (justExtracted != null) {
        this.givenTVarList.addAll(justExtracted);
      }
    }
  }

  private void setupGivenTVarListForClosure() {
    this.givenTVarList = new ArrayList<PTypeVarSlot>(this.parent.getGivenTVarList());
    PTypeSkel[] pts = this.closure.getParamDefinedTypes();
    for (int i = 0; i < pts.length; i++) {
      if (pts[i] != null) {
        List<PTypeVarSlot> justExtracted = pts[i].extractVars(this.givenTVarList);
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
