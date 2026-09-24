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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

class PTypeGraph {
  static final int DEBUG = 0;  // 0:off 1:on 2:detail

  Compiler theCompiler;
  PModule theMod;
  List<Node> nodeList;
  boolean proceeded;

  static PTypeGraph create(Compiler comp, PModule mod) {
    PTypeGraph g = new PTypeGraph();
    g.theCompiler = comp;
    g.theMod = mod;
    g.nodeList = new ArrayList<Node>();
    return g;
  }

  private PTypeGraph() {}

  void inferAll() throws CompileException {
    StringBuffer emsg;
    /* DEBUG */ int c = 0;
    do {
      if (DEBUG > 1) {
      /* DEBUG */ System.out.print("inferring ");
      /* DEBUG */ System.out.println(c++);
      }
      this.proceeded = false;
      for (int i = 0; i < this.nodeList.size(); i++) {
        Node n = this.nodeList.get(i);
        if (n.type == null) {
          if (DEBUG > 1) {
          /* DEBUG */ System.out.print("Undetermined ");
          /* DEBUG */ System.out.println(n.exprObj);
          }
          PTypeSkel t = n.infer();
          if (t != null) {
            n.type = t;
            StringBuffer r;
            if (this.theCompiler.reportsType && (r = n.getTypeReportDesc()) != null) {
              System.out.print("# type # ");
              System.out.print(n.exprObj.getSrcInfo());
              System.out.print(" ");
              System.out.print(r);
              System.out.print(" -> ");
              System.out.println(PTypeSkel.Repr.topLevelRepr(n.type));
            }
            if (DEBUG > 0) {
            /* DEBUG */ System.out.print("Inferred ");
            /* DEBUG */ System.out.print(n.exprObj);
            /* DEBUG */ System.out.print(" -> ");
            /* DEBUG */ System.out.println(PTypeSkel.Repr.topLevelRepr(n.type));
            }
            this.proceeded = true;
          }
        }
      }
    } while (this.proceeded);
    for (int i = 0; i < this.nodeList.size(); i++) {
      if (this.nodeList.get(i).type == null) {
        emsg = new StringBuffer();
        emsg.append("Cannot determine type at ");
        emsg.append(this.nodeList.get(i).exprObj.getSrcInfo());
        emsg.append(". - ");
        emsg.append(this.nodeList.get(i).exprObj);
        throw new CompileException(emsg.toString());
      }
    }
    for (int i = 0; i < this.nodeList.size(); i++) {
      this.nodeList.get(i).check();
    }
    // collect tcons including implicitly referred
    Set<PDefDict.IdKey> tconKeys = new HashSet<PDefDict.IdKey>();
    for (int i = 0; i < this.nodeList.size(); i++) {
      this.nodeList.get(i).type.collectTconKeys(tconKeys);
    }
    Iterator<PDefDict.IdKey> iter = tconKeys.iterator();
    while (iter.hasNext()) {
      PDefDict.IdKey tk = iter.next();
      this.theCompiler.defDict.addReferredForeignTcon(this.theMod.actualName, tk);
      this.theMod.addReferredFarMod(tk.modName);
    }
  }

  abstract class Node {
    PExprObj exprObj;
    List<PTypeVarSlot> givenTVarList;
    PTypeSkel type;
    Node[] inNodes;  // main inputs
    boolean dependsOnSelfRet;
    
    Node(int inNodeCount, PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      if (givenTVarList == null) { throw new IllegalArgumentException("Null given tvar list. " + exprObj.toString()); }
      this.inNodes = new Node[inNodeCount];
      for (int i = 0; i < inNodeCount; i++) {
        this.inNodes[i] = null;
      }
      this.exprObj = exprObj;
      this.givenTVarList = givenTVarList;
      PTypeGraph.this.nodeList.add(this);
    }

    void setInNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel getTypeOf(Node node) {
      if (node.dependsOnSelfRet && !this.dependsOnSelfRet) {
        this.dependsOnSelfRet = true;
        PTypeGraph.this.proceeded = true;
      }
      return node.type;
    }

    PTypeSkel getFinalizedType() { return this.type; }

    abstract PTypeSkel infer() throws CompileException;

    abstract StringBuffer getTypeReportDesc();  // maybe null

    void check() throws CompileException {}
  }

  DetNode createDetNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) throws CompileException {
      DetNode n = new DetNode(exprObj, givenTVarList);
      return n;
  }

  class DetNode extends Node {
    // inNodes.length == 1
    // [0] : to be bound (maybe null)

    DetNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) throws CompileException {
      super(1, exprObj, givenTVarList);
      this.type = exprObj.getNormalizedType();
      if (this.type == null) { throw new IllegalArgumentException("Null type. " + exprObj); }
    }

    PTypeSkel infer() throws CompileException {
      throw new RuntimeException("PTypeGraph.DetNode#infer must not be called.");
    }

    StringBuffer getTypeReportDesc() {
      return null;  // not be called, so dummy 
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.inNodes[0] != null) {
        if (PTypeRefSkel.isBottom(this.inNodes[0].type)) {
          ;
        } else if (this.type.accept(this.inNodes[0].type, PTypeSkel.Bindings.create(this.givenTVarList)) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot cast ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNodes[0].type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" at ");
          emsg.append(this.exprObj.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  RefNode createRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    RefNode n = new RefNode(exprObj, givenTVarList);
    return n;
  }

  class RefNode extends Node {
    // inNodes.length == 1
    // [0] : to be received

    RefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNodes[0]);
    }

    StringBuffer getTypeReportDesc() {
      return null;
    }
  }

  VarNode createVarNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, String name, int cat) throws CompileException {
    return new VarNode(exprObj, givenTVarList, name, cat);
  }

  class VarNode extends Node {
    // inNodes.length == 1
    // [0] : to be bound (maybe null)

    String name;
    int cat;  // PExprVarDef.CAT_xx

    VarNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, String name, int cat) throws CompileException {
      super(1, exprObj, givenTVarList);
      this.name = name;
      this.cat = cat;
      this.type = exprObj.getNormalizedType(); 
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNodes[0]);  // called only when this.type == null
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(var def) ");
      buf.append(this.name);
      return buf;
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.exprObj.getNormalizedType() != null && this.inNodes[0] != null) {
// HERE: needed?
        if (DEBUG > 1) {
/* DEBUG */ System.out.println("checking binding...");
        }
        if (PTypeRefSkel.isBottom(this.inNodes[0].type)) {
          ;
        } else if (this.cat == PExprVarDef.CAT_FUN_PARAM
            && this.type.accept(this.inNodes[0].type, PTypeSkel.Bindings.create(this.inNodes[0].givenTVarList)) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot bind ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNodes[0].type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" for *");
          emsg.append(this.name);
          emsg.append(" at ");
          emsg.append(this.exprObj.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else if (this.cat != PExprVarDef.CAT_FUN_PARAM
            && this.type.require(this.inNodes[0].type, PTypeSkel.Bindings.create(this.inNodes[0].givenTVarList)) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot cast ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNodes[0].type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" for *");
          emsg.append(this.name);
          emsg.append(" at ");
          emsg.append(this.exprObj.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  VarRefNode createVarRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, String name, Node defNode) {
    return new VarRefNode(exprObj, givenTVarList, name, defNode);
  }

  class VarRefNode extends Node {
    // inNodes.length == 1
    // [0] : def

    String name;

    VarRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, String name, Node defNode) {
      super(1, exprObj, givenTVarList);
      /* DEBUG */ if (defNode == null) { throw new IllegalArgumentException("Def node is null."); }
      this.name = name;
      this.setInNode(0, defNode);
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNodes[0]);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(var ref) ");
      buf.append(this.name);
      return buf;
    }
  }

  RetNode createRetNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) throws CompileException {
    return new RetNode(exprObj, givenTVarList);
  }

  class RetNode extends Node {
    // inNodes.length == 1
    // [0] : return value

    RetNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) throws CompileException {
      super(1, exprObj, givenTVarList);
      this.type = exprObj.getNormalizedType();
      if (this.type == null) { throw new IllegalArgumentException("Null return type."); }
    }

    PTypeSkel infer() throws CompileException {
      throw new RuntimeException("PTypeGraph.RetNode#infer must not be called.");
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(ret)");
      return buf;
    }

    void check() throws CompileException {
      if (this.inNodes[0] == null) { throw new RuntimeException("Null actual return value."); }

      StringBuffer emsg;
      if (PTypeRefSkel.isBottom(this.inNodes[0].type)) {
        ;
      } else if (this.type.require(this.inNodes[0].type, PTypeSkel.Bindings.create(this.inNodes[0].givenTVarList)) == null) {
        emsg = new StringBuffer();
        emsg.append("Return value type mismatch ");
        emsg.append(" at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  defined: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNodes[0].type));
        throw new CompileException(emsg.toString());
      }
    }
  }

  FunRefNode createFunRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid official) {
    FunRefNode n = new FunRefNode(exprObj, givenTVarList, official);
    return n;
  }

  class FunRefNode extends Node {
    // inNodes.length == 0

    PEid official;

    FunRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid official) {
      super(0, exprObj, givenTVarList);
      this.official = official;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PDefDict.EidProps ep = PTypeGraph.this.theMod.resolveFunOfficial(official);
      if (ep == null) {
        emsg = new StringBuffer();
        emsg.append("Function ");
        emsg.append(this.official);
        emsg.append(" not found at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PFunDef def = PTypeGraph.this.theCompiler.defDict.getFunDefByOfficial(PTypeGraph.this.theMod.actualName, ep.key);
      if (def == null) { throw new RuntimeException("Cannot get def. " + ep.key); }
      PTypeSkel[] pts = def.getParamTypes();
      PTypeSkel rt = def.getRetType();
      PTypeSkel[] ts = new PTypeSkel[pts.length + 1];
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(this.givenTVarList);
      for (int i = 0; i < pts.length; i++) {
        ts[i] = pts[i].instanciate(ic);  // resolving is not needed
      }
      ts[ts.length - 1] = rt.instanciate(ic);  // resolving is not needed
      return this.exprObj.getScope().getLangDefinedTypeSkel(this.exprObj.getSrcInfo(), "fun", ts);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(fun ref) ");
      buf.append(this.official.repr());
      return buf;
    }
  }

  SelfRefNode createSelfRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, ClosureNode closureNode) {
    SelfRefNode n = new SelfRefNode(exprObj, givenTVarList, closureNode);
    return n;
  }

  class SelfRefNode extends Node {
    // inNodes.length == 1
    // [0] : the closure

    SelfRefNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, ClosureNode closureNode) {
      super(1, exprObj, givenTVarList);
      this.inNodes[0] = closureNode;
      this.dependsOnSelfRet = true;
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNodes[0]);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(self ref)");
      return buf;
    }
  }

  ClosureNode createClosureNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int paramCount) {
    ClosureNode n = new ClosureNode(exprObj, givenTVarList, paramCount);
    return n;
  }

  class ClosureNode extends Node {
    // inNodes.length == parameter count
    // [i] : parameter

    Node retNode;

    ClosureNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int paramCount) {
      super(paramCount, exprObj, givenTVarList);
    }

    void setParamNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    void setRetNode(Node node) {
      this.retNode = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel[] ts = new PTypeSkel[this.inNodes.length + 1];
      PTypeSkel t;
      for (int i = 0; i < this.inNodes.length; i++) {
        t = this.getTypeOf(this.inNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.isBottom(t)) { return t; }
        ts[i] = t;
      }
      t = this.getTypeOf(this.retNode);
      if (t == null) { return null; }
      ts[ts.length - 1] = t;
      return this.exprObj.getScope().getLangDefinedTypeSkel(this.exprObj.getSrcInfo(), "fun", ts);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(closure)");
      return buf;
    }
  }

  StaticInvNode createStaticInvNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid funId, int paramCount) {
    StaticInvNode n = new StaticInvNode(exprObj, givenTVarList, funId, paramCount);
    return n;
  }

  class StaticInvNode extends Node {
    // inNodes.length == parameter count
    // [i] : parameter

    PEid funId;
    Node[] paramNodes;
    PFunDef funDef;
    PTypeSkel.Bindings bindings;

    StaticInvNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid funId, int paramCount) {
      super(paramCount, exprObj, givenTVarList);
      this.funId = funId;
    }

    void setParamNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel[] pts = new PTypeSkel[this.inNodes.length];
      for (int i = 0; i < this.inNodes.length; i++) {
        PTypeSkel t = this.getTypeOf(this.inNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.isBottom(t)) { return t; }
        pts[i] = t;
      }
      PDefDict.FunSelRes sel = PTypeGraph.this.theMod.selectFunDef(this.funId, pts, this.givenTVarList);
      if (sel == null) {
        emsg = new StringBuffer();
        emsg.append("Function ");
        emsg.append(this.funId.repr());
        emsg.append(" not found at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        for (int i = 0; i < pts.length; i++) {
          emsg.append("\n  parameter: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(pts[i]));
        }
        throw new CompileException(emsg.toString());
      }
      this.funDef = sel.funDef;
      this.bindings = sel.bindings;
      PTypeSkel rt = sel.funDef.getRetType();
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(this.bindings);
      return rt.resolveBindings(this.bindings).instanciate(ic);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(static invocation) ");
      buf.append(this.funId.repr());
      return buf;
    }
  }

  DynamicInvNode createDynamicInvNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int paramCount) {
    DynamicInvNode n = new DynamicInvNode(exprObj, givenTVarList, paramCount);
    return n;
  }

  class DynamicInvNode extends Node {
    // inNodes.length == parameter count
    // [i] : parameter

    Node closureNode;
    PTypeSkel.Bindings bindings;

    DynamicInvNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int paramCount) {
      super(paramCount, exprObj, givenTVarList);
    }

    void setParamNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    void setClosureNode(Node node) {
      this.closureNode = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      this.bindings = PTypeSkel.Bindings.create(this.givenTVarList);
      PTypeSkel ct = this.getTypeOf(this.closureNode);
      if (ct == null) { return null; }
      if (!(ct instanceof PTypeRefSkel)) {
        emsg = new StringBuffer();
        emsg.append("Invalid closure type ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(ct));
        emsg.append(" at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeRefSkel ctr = (PTypeRefSkel)ct;
      if (PTypeRefSkel.isFun(ctr)) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Invalid closure object at ");
        emsg.append(this.closureNode.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (this.inNodes.length != ctr.params.length - 1) {
        emsg = new StringBuffer();
        emsg.append("Invalid argument count at ");
        emsg.append(this.closureNode.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeSkel.Bindings bx = this.bindings;
      for (int i = 0; i < this.inNodes.length; i++) {
        PTypeSkel t = this.getTypeOf(this.inNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.isBottom(t)) { return t; }
        bx = ctr.params[i].accept(t, bx);
        if (bx == null) {
          emsg = new StringBuffer();
          emsg.append("Argument type mismatch at ");
          emsg.append(this.exprObj.getSrcInfo());
          emsg.append(".");
          emsg.append("\n  parameter pos: ");
          emsg.append(i + 1);
          emsg.append("\n  parameter def: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(ctr.params[i]));
          emsg.append("\n  parameter def in context: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(ctr.params[i].resolveBindings(this.bindings)));
          emsg.append("\n  actual argument: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(t));
          throw new CompileException(emsg.toString());
        }
      }
      this.bindings = bx;
      PTypeVarSkel iv;
      if ((iv = this.bindings.getAnyInconcreteVar()) != null) {
        emsg = new StringBuffer();
        emsg.append("Inconcrete type parameter specified for ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(iv));
        emsg.append(" at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(this.bindings);
      return ctr.params[ctr.params.length - 1].resolveBindings(this.bindings).instanciate(ic);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(dynamic invocation)");
      return buf;
    }
  }

  SeqNode createSeqNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    SeqNode n = new SeqNode(exprObj, givenTVarList);
    return n;
  }

  class SeqNode extends Node {
    // inNodes.length == 2
    // [0] : following
    // [1] : leading

    SeqNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(2, exprObj, givenTVarList);
    }

    void setFollowingTypeNode(Node node) {
      this.inNodes[0] = node;
    }

    void setLeadingTypeNode(Node node) {
      this.inNodes[1] = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel leading = this.getTypeOf(this.inNodes[1]);
      PTypeSkel following = this.getTypeOf(this.inNodes[0]);
      PTypeSkel t = null;
      if (leading != null) {
        if (PTypeRefSkel.isBottom(leading)) {
          t = leading;
        } else if (following != null) {
          t = following;
        } else {
          ;  // wait for following
        }
      } else if (following != null) {
        if (PTypeRefSkel.isBottom(following)) {
          t = following;
        } else if (this.inNodes[1].dependsOnSelfRet) {
          t = following;
        } else {
          ;  // wait for leading
        }
      } else {
        ;  // wait for leading and following
      }
      return t;
    }

    StringBuffer getTypeReportDesc() {
      return null;
    }
  }

  JoinNode createJoinNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int branchCount) {
    JoinNode n = new JoinNode(exprObj, givenTVarList, branchCount);
    return n;
  }

  class JoinNode extends Node {
    // inNodes.length == branch count

    JoinNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int branchCount) {
      super(branchCount, exprObj, givenTVarList);
    }

    void setBranchNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t;
      List<PTypeSkel> tt = new ArrayList<PTypeSkel>();
      boolean pending = false;
      for (int i = 0; i < this.inNodes.length; i++) {
        t = this.getTypeOf(this.inNodes[i]);
        if (t != null) {
          tt.add(t);
        } else if (this.inNodes[i].dependsOnSelfRet) {
          pending = true;
        } else {
          return null;
        }
      }
      if (tt.size() > 0) {
        t = tt.get(0);
        for (int i = 1; i < tt.size(); i++) {
          PTypeSkel t1 = t.join(tt.get(i), this.givenTVarList);
          if (t1 == null) {
            emsg = new StringBuffer();
            emsg.append("Results of clauses cannot join at - ");
            emsg.append(this.exprObj.getSrcInfo());
            emsg.append(".");
            emsg.append("\n  type: ");
            emsg.append(PTypeSkel.Repr.topLevelRepr(t));
            emsg.append("\n  type: ");
            emsg.append(PTypeSkel.Repr.topLevelRepr(tt.get(i)));
            throw new CompileException(emsg.toString());
          }
          t = t1;
        }
      } else if (pending) {
        return null;
      } else {
        emsg = new StringBuffer();
        emsg.append("Cannot determine type at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return t;
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(joined)");
      return buf;
    }
  }

  TupleNode createTupleNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
    TupleNode n = new TupleNode(exprObj, givenTVarList, elemCount);
    return n;
  }

  class TupleNode extends Node {
    // inNodes.length == element count

    TupleNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
      super(elemCount, exprObj, givenTVarList);
    }

    void setElemNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel[] ts = new PTypeSkel[this.inNodes.length];
      PTypeSkel t;
      for (int i = 0; i < this.inNodes.length; i++) {
        t = this.getTypeOf(this.inNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.isBottom(t)) { return t; }
        ts[i] = t;
      }
      return this.exprObj.getScope().getLangDefinedTypeSkel(this.exprObj.getSrcInfo(), "tuple", ts);
    }

    StringBuffer getTypeReportDesc() {
      return null;
    }
  }

  EmptyListNode createEmptyListNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    return new EmptyListNode(exprObj, givenTVarList);
  }

  class EmptyListNode extends Node {
    // inNodes.length == 0

    EmptyListNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(0, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      return this.exprObj.getScope().getEmptyListType(this.exprObj.getSrcInfo());
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(empty list)");
      return buf;
    }
  }

  ListNode createListNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    ListNode n = new ListNode(exprObj, givenTVarList);
    return n;
  }

  class ListNode extends Node {
    // inNodes.length == 2
    // [0] : elem
    // [1] : tail

    ListNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(2, exprObj, givenTVarList);
    }

    void setElemNode(Node node) {
      this.inNodes[0] = node;
    }

    void setTailNode(Node node) {
      this.inNodes[1] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel et = this.getTypeOf(this.inNodes[0]);
      if (et == null) { return null; }
      if (PTypeRefSkel.isBottom(et)) { return et; }
      PTypeSkel tt = this.getTypeOf(this.inNodes[1]);
      if (tt == null) { return null; }
      if (PTypeRefSkel.isBottom(tt)) { return tt; }
      if (!PTypeRefSkel.isList(tt)) {
        emsg = new StringBuffer();
        emsg.append("Type of list tail is invalid at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(". - ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(tt));
        throw new CompileException(emsg.toString());
      }
      PTypeSkel t = et.join(((PTypeRefSkel)tt).params[0], this.givenTVarList);
      if (t == null) {
        emsg = new StringBuffer();
        emsg.append("Element type is incompatible at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  element: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(et));
        emsg.append("\n  tail: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(((PTypeRefSkel)tt).params[0]));
        throw new CompileException(emsg.toString());
      }
      return this.exprObj.getScope().getLangDefinedTypeSkel(this.exprObj.getSrcInfo(), "list", new PTypeSkel[] { t });
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(list)");
      return buf;
    }
  }

  StringNode createStringNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
    StringNode n = new StringNode(exprObj, givenTVarList, elemCount);
    return n;
  }

  class StringNode extends Node {
    // inNodes.length == element count

    StringNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
      super(elemCount, exprObj, givenTVarList);
    }

    void setElemNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      if (this.inNodes.length == 0) {
        return this.exprObj.getScope().getEmptyStringType(this.exprObj.getSrcInfo());
      }
      PTypeSkel t = null;
      for (int i = 0; i < this.inNodes.length; i++) {
        PTypeSkel t2 = this.getTypeOf(this.inNodes[i]);
        if (t2 == null) { return null; }
        if (PTypeRefSkel.isBottom(t2)) { return t2; }
        if (t == null) {  // [0]
          t = t2;
        } else {
          t = t.join(t2, this.givenTVarList);
          if (t == null) {
            emsg = new StringBuffer();
            emsg.append("Element types incompatible at - ");
            emsg.append(this.exprObj.getSrcInfo());
            emsg.append(".");
            throw new CompileException(emsg.toString());
          }
        }
      }
      return this.exprObj.getScope().getLangDefinedTypeSkel(this.exprObj.getSrcInfo(), "string", new PTypeSkel[] { t });
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(string)");
      return buf;
    }
  }

  DataConstrNode createDataConstrNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid dcon, int attrCount) {
    DataConstrNode n = new DataConstrNode(exprObj, givenTVarList, dcon, attrCount);
    return n;
  }

  class DataConstrNode extends Node {
    // inNodes.length == attribute count

    PEid dcon;
    DataConstrNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid dcon, int attrCount) {
      super(attrCount, exprObj, givenTVarList);
      this.dcon = dcon;
    }

    void setAttrNode(int index, Node node) {
      this.inNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PDefDict.EidProps ep = PTypeGraph.this.theMod.resolveAnchor(this.dcon);
      if (ep == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before

      PDataDef dataDef = PTypeGraph.this.theCompiler.defDict.getDataOriginDefFromDcon(PTypeGraph.this.theMod.actualName, ep.key);
      if (dataDef == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before
      PDataDef.Constr constr = PTypeGraph.this.theCompiler.defDict.getConstrDefFromDcon(
        PTypeGraph.this.theMod.actualName, ep.key);
      if (constr == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before
      if (constr.getAttrCount() != this.inNodes.length) {
        emsg = new StringBuffer();
        emsg.append("Attribute count mismatch on ");
        emsg.append(this.dcon.name);
        emsg.append(" at ");
        emsg.append(this.dcon.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeSkel.Bindings bx = PTypeSkel.Bindings.create(this.givenTVarList);
      for (int i = 0; i < constr.getAttrCount(); i++) {
        PTypeSkel t = this.getTypeOf(this.inNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.isBottom(t)) { return t; }
        PDataDef.Attr a = constr.getAttrAt(i);
        PTypeSkel at = a.getNormalizedType();
        PTypeSkel.Bindings b;
        if ((b = at.accept(t, bx)) == null) {
          emsg = new StringBuffer();
          emsg.append("Type mismatch at ");
          emsg.append(this.exprObj.getSrcInfo());
          emsg.append(".");
          emsg.append("\n  attribute pos: ");
          emsg.append(i + 1);
          emsg.append("\n  attribute def: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(at));
          emsg.append("\n  attribute def in context: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(at.resolveBindings(bx)));
          emsg.append("\n  actual attribute: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(t));
          throw new CompileException(emsg.toString());
        }
        bx = b;
      }
      PTypeSkel dt = dataDef.getTypeSig().resolveBindings(bx).instanciate(PTypeSkel.InstanciationContext.create(bx));
      return dt;
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(data constr) ");
      buf.append(this.dcon.repr());
      return buf;
    }
  }

  TuplePtnNode createTuplePtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
    return new TuplePtnNode(exprObj, givenTVarList, elemCount);
  }

  class TuplePtnNode extends Node {
    // inNodes.length == 1
    // [0] : supplied value

    int elemCount;

    TuplePtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int elemCount) {
      super(1, exprObj, givenTVarList);
      this.elemCount = elemCount;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "tuple")) {
        emsg = new StringBuffer();
        emsg.append("Value is not tuple at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        throw new CompileException(emsg.toString());
      }
      PTypeRefSkel tr = (PTypeRefSkel)t;
      if (tr.params.length != this.elemCount) {
        emsg = new StringBuffer();
        emsg.append("Tuple's length mismatch at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return t;
    }

    StringBuffer getTypeReportDesc() {
      return null;
    }
  }

  TuplePtnElemNode createTuplePtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int index) {
    return new TuplePtnElemNode(exprObj, givenTVarList, index);
  }

  class TuplePtnElemNode extends Node {
    // inNodes.length == 1
    // [0] : receiver

    int index;

    TuplePtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int index) {
      super(1, exprObj, givenTVarList);
      this.index = index;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      return ((PTypeRefSkel)t).params[this.index];  // type is guaranteed in in-node
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(tuple pattern elem) ");
      buf.append(Integer.toString(this.index + 1));
      return buf;
    }
  }

  ListPtnNode createListPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    return new ListPtnNode(exprObj, givenTVarList);
  }

  class ListPtnNode extends Node {
    // inNodes.length == 1
    // [0] : supplied value

    ListPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "list")) {
        emsg = new StringBuffer();
        emsg.append("Not in list at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actually in: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        throw new CompileException(emsg.toString());
      }
      return t;
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(list pattern)");
      return buf;
    }
  }

  ListPtnElemNode createListPtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    ListPtnElemNode n = new ListPtnElemNode(exprObj, givenTVarList);
    return n;
  }

  class ListPtnElemNode extends Node {
    // inNodes.length == 1
    // [0] : receiver

    ListPtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      return ((PTypeRefSkel)t).params[0];  // type is guaranteed in in-node
    }
 
    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(list pattern elem)");
      return buf;
   }
 }

  StringPtnNode createStringPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    return new StringPtnNode(exprObj, givenTVarList);
  }

  class StringPtnNode extends Node {
    // inNodes.length == 1
    // [0] : supplied value

    StringPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "string")) {
        emsg = new StringBuffer();
        emsg.append("Value is not string at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        throw new CompileException(emsg.toString());
      }
      return t;
    }
  
    StringBuffer getTypeReportDesc() {
     StringBuffer buf = new StringBuffer();
     buf.append("(string pattern)");
     return buf;
   }
 }

  StringPtnElemNode createStringPtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    return new StringPtnElemNode(exprObj, givenTVarList);
  }

  class StringPtnElemNode extends Node {
    // inNodes.length == 1
    // [0] : receiver

    StringPtnElemNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }
      if (PTypeRefSkel.isBottom(t)) { return t; }
      return ((PTypeRefSkel)t).params[0];  // type is guaranteed in in-node
    }
   
    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(string pattern elem)");
      return buf;
    }
}

  DataConstrPtnNode createDataConstrPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int context, PEid dcon) {
    DataConstrPtnNode n = new DataConstrPtnNode(exprObj, givenTVarList, context, dcon);
    return n;
  }

  class DataConstrPtnNode extends RefNode {
    // inNodes.length == 1
    // [0] : supplied value

    int context;  // PPtnMatch.CONTEXT_*
    PEid dcon;
    PTypeSkel.Bindings bindings;

    DataConstrPtnNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, int context, PEid dcon) {
      super(exprObj, givenTVarList);
      if (context == PPtnMatch.CONTEXT_FIXED || context == PPtnMatch.CONTEXT_TRIAL) {
      } else {
        throw new IllegalArgumentException("Unexpected context. " + Integer.toString(this.context) + " " + this.exprObj.toString());
      } 
      this.context = context;
      this.dcon = dcon;
    }

    PTypeSkel.Bindings getBindings() throws CompileException {
      StringBuffer emsg;
      if (this.bindings != null) { return this.bindings; }
      PTypeSkel t = this.getTypeOf(this.inNodes[0]);
      if (t == null) { return null; }  // HERE: in case of <_>
      PDefDict.EidProps ep = PTypeGraph.this.theMod.resolveAnchor(this.dcon);
      if (ep == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before
      PDataDef dataDef = PTypeGraph.this.theCompiler.defDict.getDataOriginDefFromDcon(PTypeGraph.this.theMod.actualName, ep.key);
      if (dataDef == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before
      PTypeRefSkel sig = (PTypeRefSkel)dataDef.getTypeSig();
      PTypeSkel.Bindings bx = PTypeSkel.Bindings.create(this.givenTVarList);
      if ((bx = sig.accept(t, bx)) == null) {
        emsg = new StringBuffer();
        emsg.append("Type mismatch at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  value type: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        emsg.append("\n  pattern type sig: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(sig));
        throw new CompileException(emsg.toString());
      }
      if (!(t instanceof PTypeRefSkel)) {
        emsg = new StringBuffer();
        emsg.append("Type mismatch at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  value type: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        emsg.append("\n  pattern type sig: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(sig));
        throw new CompileException(emsg.toString());
      }
      PTypeRefSkel tr = (PTypeRefSkel)t;
      if (tr.params.length != sig.params.length) {
        emsg = new StringBuffer();
        emsg.append("Type mismatch at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  value type: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(tr));
        emsg.append("\n  pattern type sig: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(sig));
        throw new CompileException(emsg.toString());
      }
      this.bindings = bx;
      return this.bindings;
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(data pattern) ");
      buf.append(this.dcon.repr());
      return buf;
    }
  }

  DataConstrPtnAttrNode createDataConstrPtnAttrNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid dcon, int index) {
    DataConstrPtnAttrNode n = new DataConstrPtnAttrNode(exprObj, givenTVarList, dcon, index);
    return n;
  }

  class DataConstrPtnAttrNode extends Node {
    // inNodes.length == 1
    // [0] : receiver

    PEid dcon;
    int index;

    DataConstrPtnAttrNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList, PEid dcon, int index) {
      super(1, exprObj, givenTVarList);
      this.dcon = dcon;
      this.index = index;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel.Bindings b;
      if ((b = ((DataConstrPtnNode)this.inNodes[0]).getBindings()) == null) { return null; }
      PDefDict.EidProps ep = PTypeGraph.this.theMod.resolveAnchor(this.dcon);
      if (ep == null) { throw new RuntimeException("Unexpected. " + this.dcon); }  // checked before
      PDataDef.Constr constr =PTypeGraph.this.theCompiler.defDict.getConstrDefFromDcon(PTypeGraph.this.theMod.actualName, ep.key); 
      PDataDef.Attr attr = constr.getAttrAt(this.index);
      PTypeSkel.InstanciationContext ic = PTypeSkel.InstanciationContext.create(b);
      return attr.getNormalizedType().resolveBindings(b).instanciate(ic);
    }

    StringBuffer getTypeReportDesc() {
      StringBuffer buf = new StringBuffer();
      buf.append("(data pattern attr) ");
      buf.append(this.dcon.repr());
      buf.append(" ");
      buf.append(Integer.toString(this.index + 1));
      return buf;
    }
  }

  CondNode createCondNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
    return new CondNode(exprObj, givenTVarList);
  }

  class CondNode extends Node {
    // inNodes.length == 1
    // [0] : supplied value

    CondNode(PExprObj exprObj, List<PTypeVarSlot> givenTVarList) {
      super(1, exprObj, givenTVarList);
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNodes[0]);
    }

    StringBuffer getTypeReportDesc() {
      return null;
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (!PTypeRefSkel.isLangType(this.inNodes[0].type, "bool")) {
        emsg = new StringBuffer();
        emsg.append("Not <bool> at ");
        emsg.append(this.exprObj.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNodes[0].type));
        throw new CompileException(emsg.toString());
      }
    }
  }
}
