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
import java.util.List;

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
          /* DEBUG */ System.out.println(n.typedElem);
          }
          PTypeSkel t = n.infer();
          if (t != null) {
            n.type = t;
            if (DEBUG > 0) {
            /* DEBUG */ System.out.print("Inferred ");
            /* DEBUG */ System.out.print(n.typedElem);
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
        emsg.append(this.nodeList.get(i).typedElem.getSrcInfo());
        emsg.append(". - ");
        emsg.append(this.nodeList.get(i).typedElem);
        throw new CompileException(emsg.toString());
      }
    }
    for (int i = 0; i < this.nodeList.size(); i++) {
      this.nodeList.get(i).check();
    }
  }

  abstract class Node {
    PTypedElem typedElem;
    PTypeSkel type;
    Node inNode;
    boolean dependsOnSelfRet;
    
    Node(PTypedElem typedElem) {
      this.typedElem = typedElem;
      PTypeGraph.this.nodeList.add(this);
    }

    void setInNode(Node node) {
      this.inNode = node;
    }

    PTypeSkel getTypeOf(Node node) {
      if (node.dependsOnSelfRet && !this.dependsOnSelfRet) {
        this.dependsOnSelfRet = true;
        PTypeGraph.this.proceeded = true;
      }
      return node.type;
    }

    PTypeSkel getFixedType() { return this.type; }

    List<PTVarSlot> getGivenTvarList() { return this.typedElem.getScope().getGivenTVarList(); }

    abstract PTypeSkel infer() throws CompileException;

    void check() throws CompileException {}

  }

  DetNode createDetNode(PTypedElem typedElem) {
      DetNode n = new DetNode(typedElem);
      // typedElem.setFixedType(typedElem.getNormalizedType());
      return n;
  }

  class DetNode extends Node {

    DetNode(PTypedElem typedElem) {
      super(typedElem);
      this.type = typedElem.getNormalizedType();
    }

    PTypeSkel infer() throws CompileException {
      throw new RuntimeException("PTypeGraph.DetNode#infer must not be called.");
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.inNode != null) {
        if (DEBUG > 1) {
/* DEBUG */ System.out.println("checking binding...");
        }
        if (PTypeRefSkel.willNotReturn(this.inNode.type)) {
          ;
        } else if (this.type.accept(PTypeSkel.NARROWER, true, this.inNode.type, PTypeSkelBindings.create(this.getGivenTvarList())) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot bind ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNode.type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  RefNode createRefNode(PTypedElem typedElem) {
    RefNode n = new RefNode(typedElem);
    return n;
  }

  class RefNode extends Node {

    RefNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNode);
    }
  }

  VarNode createVarNode(PTypedElem typedElem, String name, int cat) {
    return new VarNode(typedElem, name, cat);
  }

  class VarNode extends Node {
    String name;
    int cat;  // PEVarDef.CAT_xx

    VarNode(PTypedElem typedElem, String name, int cat) {
      super(typedElem);
      this.name = name;
      this.cat = cat;
      this.type = typedElem.getNormalizedType(); 
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNode);  // called only when this.type == null
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.typedElem.getNormalizedType() != null && this.inNode != null) {
// HERE: needed?
        if (DEBUG > 1) {
/* DEBUG */ System.out.println("checking binding...");
        }
        if (PTypeRefSkel.willNotReturn(this.inNode.type)) {
          ;
        } else if (this.type.accept(PTypeSkel.NARROWER, this.cat == PEVarDef.CAT_FUN_PARAM, this.inNode.type, PTypeSkelBindings.create(this.inNode.getGivenTvarList())) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot bind ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNode.type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" *");
          emsg.append(this.name);
          emsg.append(" at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  VarRefNode createVarRefNode(PTypedElem typedElem, String name, Node defNode) {
    return new VarRefNode(typedElem, name, defNode);
  }

  class VarRefNode extends Node {
    String name;
    Node defNode;

    VarRefNode(PTypedElem typedElem, String name, Node defNode) {
      super(typedElem);
      this.name = name;
      /* DEBUG */ if (defNode == null) { throw new IllegalArgumentException("Def node is null."); }
      this.defNode = defNode;
      // this.type = typedElem.getNormalizedType(); 
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.defNode);
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.typedElem.getNormalizedType() != null && this.inNode != null) {
        if (DEBUG > 1) {
/* DEBUG */ System.out.println("checking binding...");
        }
        if (PTypeRefSkel.willNotReturn(this.inNode.type)) {
          ;
        } else if (this.inNode.type.accept(PTypeSkel.NARROWER, true, this.type, PTypeSkelBindings.create(this.getGivenTvarList())) == null) {
          emsg = new StringBuffer();
          emsg.append("Cannot cast ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNode.type));
          emsg.append(" to ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append(" *");
          emsg.append(this.name);
          emsg.append(" at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  RetNode createRetNode(PTypedElem typedElem) {
    return new RetNode(typedElem);
  }

  class RetNode extends Node {

    RetNode(PTypedElem typedElem) {
      super(typedElem);
      this.type = typedElem.getNormalizedType();
    }

    PTypeSkel infer() throws CompileException {
      throw new RuntimeException("PTypeGraph.RetNode#infer must not be called.");
      // return this.getTypeOf(this.inNode);  // called only when this.type == null
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (this.typedElem.getNormalizedType() != null && this.inNode != null) {
        if (DEBUG > 1) {
/* DEBUG */ System.out.println("checking return type...");
        }
        if (PTypeRefSkel.willNotReturn(this.inNode.type)) {
          ;
        } else if (this.type.accept(PTypeSkel.NARROWER, false, this.inNode.type, PTypeSkelBindings.create(this.inNode.getGivenTvarList())) == null) {
          emsg = new StringBuffer();
          emsg.append("Return value type mismatch ");
          emsg.append(" at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          emsg.append("\n  defined: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.type));
          emsg.append("\n  actual: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNode.type));
          throw new CompileException(emsg.toString());
        }
      }
    }
  }

  FunRefNode createFunRefNode(PTypedElem typedElem, PExprId official) {
    FunRefNode n = new FunRefNode(typedElem, official);
    return n;
  }

  class FunRefNode extends Node {
    PExprId official;

    FunRefNode(PTypedElem typedElem, PExprId official) {
      super(typedElem);
      this.official = official;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PFunDef def = this.official.props.defGetter.getFunDef();
      if (def == null) {
        emsg = new StringBuffer();
        emsg.append("Function ");
        emsg.append(this.official);
        emsg.append(" not found at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeSkel[] pts = def.getParamTypes();
      if (DEBUG > 1) {
      /* DEBUG */ System.out.print("param types: ");
      /* DEBUG */ for (int i = 0; i < pts.length; i++) {
        /* DEBUG */ System.out.print(pts[i]);
      /* DEBUG */ }
      /* DEBUG */ System.out.println();
      }
      PTypeSkel rt = def.getRetType();
      PTypeSkel[] ts = new PTypeSkel[pts.length + 1];
      PTypeSkelBindings b = PTypeSkelBindings.create(this.getGivenTvarList());
      PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(b);
      for (int i = 0; i < pts.length; i++) {
        ts[i] = pts[i].instanciate(ib);
      }
      ts[ts.length - 1] = rt.instanciate(ib);
      if (DEBUG > 1) {
      /* DEBUG */ System.out.print("bindings: ");
      /* DEBUG */ System.out.println(b);
      }
      return this.typedElem.getScope().getLangDefinedTypeSkel(this.typedElem.getSrcInfo(), "fun", ts);
    }
  }

  SelfRefNode createSelfRefNode(PTypedElem typedElem, ClosureNode closureNode) {
    SelfRefNode n = new SelfRefNode(typedElem, closureNode);
    return n;
  }

  class SelfRefNode extends Node {

    SelfRefNode(PTypedElem typedElem, ClosureNode closureNode) {
      super(typedElem);
      this.inNode = closureNode;
      this.dependsOnSelfRet = true;
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNode);
    }
  }

  ClosureNode createClosureNode(PTypedElem typedElem, int paramCount) {
    ClosureNode n = new ClosureNode(typedElem, paramCount);
    return n;
  }

  class ClosureNode extends Node {
    Node[] paramNodes;
    Node retNode;

    ClosureNode(PTypedElem typedElem, int paramCount) {
      super(typedElem);
      this.paramNodes = new Node[paramCount];
      for (int i = 0; i < paramCount; i++) {
        this.paramNodes[i] = null;
      }
    }

    void setParamNode(int index, Node node) {
      this.paramNodes[index] = node;
    }

    void setRetNode(Node node) {
      this.retNode = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel[] ts = new PTypeSkel[this.paramNodes.length + 1];
      PTypeSkel t;
      for (int i = 0; i < this.paramNodes.length; i++) {
        t = this.getTypeOf(this.paramNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.willNotReturn(t)) { return t; }
        ts[i] = t;
      }
      t = this.getTypeOf(this.retNode);
      if (t == null) { return null; }
      ts[ts.length - 1] = t;
      return this.typedElem.getScope().getLangDefinedTypeSkel(this.typedElem.getSrcInfo(), "fun", ts);
    }
  }

  StaticInvNode createStaticInvNode(PTypedElem typedElem, PExprId funId, int paramCount) {
    StaticInvNode n = new StaticInvNode(typedElem, funId, paramCount);
    return n;
  }

  class StaticInvNode extends Node {
    PExprId funId;
    Node[] paramNodes;
    PFunDef funDef;
    PTypeSkelBindings bindings;

    StaticInvNode(PTypedElem typedElem, PExprId funId, int paramCount) {
      super(typedElem);
      this.funId = funId;
      this.paramNodes = new Node[paramCount];
      for (int i = 0; i < paramCount; i++) {
        this.paramNodes[i] = null;
      }
    }

    void setParamNode(int index, Node node) {
      this.paramNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel[] pts = new PTypeSkel[this.paramNodes.length];
      for (int i = 0; i < this.paramNodes.length; i++) {
        PTypeSkel t = this.getTypeOf(this.paramNodes[i]);
        if (t == null) { return null; }
        pts[i] = t;
      }
      PDefDict.FunSelRes sel = this.funId.props.defGetter.selectFunDef(pts, this.getGivenTvarList());
      if (sel == null) {
        emsg = new StringBuffer();
        emsg.append("Function ");
        emsg.append(this.funId.toRepr());
        emsg.append(" not found at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        for (int i = 0; i < pts.length; i++) {
          emsg.append("\n  parameter: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(pts[i]));
        }
        throw new CompileException(emsg.toString());
      }
      this.funDef = sel.funDef;
      this.bindings = sel.bindings;
// /* DEBUG */ System.out.println("selected " + this.funDef.getModName().toJavaString() + "." + this.funDef.getOfficialName() + " at " + PTypeGraph.this.theMod.name.toJavaString());
      Cstr modName = this.funDef.getModName();
      if (!modName.equals(PTypeGraph.this.theMod.name)) {
        PTypeGraph.this.theMod.foreignIdResolver.referredFunOfficial(this.funDef);
      }
      PTypeSkel rt = sel.funDef.getRetType();
      if (DEBUG > 1) {
      /* DEBUG */ System.out.print("ret def: ");
      /* DEBUG */ System.out.println(rt);
      /* DEBUG */ System.out.print("bindings: ");
      /* DEBUG */ System.out.println(sel.bindings);
      }
      PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(this.bindings);
      return rt.instanciate(ib);
    }
  }

  DynamicInvNode createDynamicInvNode(PTypedElem typedElem, int paramCount) {
    DynamicInvNode n = new DynamicInvNode(typedElem, paramCount);
    return n;
  }

  class DynamicInvNode extends Node {
    Node[] paramNodes;
    Node closureNode;
    PTypeSkelBindings bindings;

    DynamicInvNode(PTypedElem typedElem, int paramCount) {
      super(typedElem);
      this.paramNodes = new Node[paramCount];
      for (int i = 0; i < paramCount; i++) {
        this.paramNodes[i] = null;
      }
    }

    void setParamNode(int index, Node node) {
      this.paramNodes[index] = node;
    }

    void setClosureNode(Node node) {
      this.closureNode = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      this.bindings = PTypeSkelBindings.create(this.getGivenTvarList());
      PTypeSkel ct = this.getTypeOf(this.closureNode);
      if (ct == null) { return null; }
if (DEBUG > 1) {
      /* DEBUG */ System.out.print("closure type of ");
      /* DEBUG */ System.out.print(this.typedElem.getSrcInfo());
      /* DEBUG */ System.out.print(": ");
      /* DEBUG */ System.out.println(ct);
}
      if (!(ct instanceof PTypeRefSkel)) {
        emsg = new StringBuffer();
        emsg.append("Invalid closure type ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(ct));
        emsg.append(" at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PTypeRefSkel ctr = (PTypeRefSkel)ct;
      if (ctr.tconInfo.key.modName.equals(Module.MOD_LANG) && ctr.tconInfo.key.tcon.equals("fun")) {
        ;
      } else {
        emsg = new StringBuffer();
        emsg.append("Invalid closure object at ");
        emsg.append(this.closureNode.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (this.paramNodes.length != ctr.params.length - 1) {
        emsg = new StringBuffer();
        emsg.append("Invalid argument count at ");
        emsg.append(this.closureNode.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      for (int i = 0; i < this.paramNodes.length; i++) {
/* DEBUG */ if (this.paramNodes[i] == null) { System.out.println(this.typedElem); }
        PTypeSkel t = this.getTypeOf(this.paramNodes[i]);
        if (t == null) { return null; }
        PTypeSkelBindings bb = this.bindings;  // before looks for debug
        this.bindings = ctr.params[i].accept(PTypeSkel.NARROWER, true, t, this.bindings);
        if (this.bindings == null) {
          emsg = new StringBuffer();
          emsg.append("Argument type mismatch at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          emsg.append("\n  parameter pos: ");
          emsg.append(i + 1);
          emsg.append("\n  parameter def: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(ctr.params[i]));
          emsg.append("\n  parameter def in context: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(ctr.params[i].resolveBindings(bb)));
          emsg.append("\n  actual argument: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(t));
          throw new CompileException(emsg.toString());
        }
      }
if (DEBUG > 1) {
      /* DEBUG */ System.out.print("type application: ");
      /* DEBUG */ System.out.println(this.bindings);
}
      PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(this.bindings);
      return ctr.params[ctr.params.length - 1].instanciate(ib);
    }
  }

  SeqNode createSeqNode(PTypedElem typedElem) {
    SeqNode n = new SeqNode(typedElem);
    return n;
  }

  class SeqNode extends Node {
    Node leadingTypeNode;  // inNode is used for following node

    SeqNode(PTypedElem typedElem) {
      super(typedElem);
    }

    void setLeadingTypeNode(Node node) {
      this.leadingTypeNode = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel leading = this.getTypeOf(this.leadingTypeNode);
      PTypeSkel following = this.getTypeOf(this.inNode);
      PTypeSkel t = null;
      if (leading != null) {
        if (PTypeRefSkel.willNotReturn(leading)) {
          t = leading;
        } else if (following != null) {
          t = following;
        } else {
          ;  // wait for following
        }
      } else if (following != null) {
        if (PTypeRefSkel.willNotReturn(following)) {
          t = following;
        } else if (this.leadingTypeNode.dependsOnSelfRet) {
          t = following;
        } else {
          ;  // wait for leading
        }
      } else {
        ;  // wait for leading and following
      }
      return t;
    }
  }

  JoinNode createJoinNode(PTypedElem typedElem, int branchCount) {
    JoinNode n = new JoinNode(typedElem, branchCount);
    return n;
  }

  class JoinNode extends Node {
    Node[] branchNodes;

    JoinNode(PTypedElem typedElem, int branchCount) {
      super(typedElem);
      this.branchNodes = new Node[branchCount];
      for (int i = 0; i < branchCount; i++) {
        this.branchNodes[i] = null;
      }
    }

    void setBranchNode(int index, Node node) {
      this.branchNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t;
      List<PTypeSkel> tt = new ArrayList<PTypeSkel>();
      boolean pending = false;
      for (int i = 0; i < this.branchNodes.length; i++) {
        t = this.getTypeOf(this.branchNodes[i]);
        if (t != null) {
          tt.add(t);
        } else if (this.branchNodes[i].dependsOnSelfRet) {
          pending = true;
        } else {
          return null;
        }
      }
      if (tt.size() > 0) {
        t = tt.get(0);
        for (int i = 1; i < tt.size(); i++) {
          PTypeSkel t1 = t.join(tt.get(i), this.getGivenTvarList());
          if (t1 == null) {
            emsg = new StringBuffer();
            emsg.append("Results of clauses cannot join at - ");
            emsg.append(this.typedElem.getSrcInfo());
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
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return t;
    }
  }

  TupleNode createTupleNode(PTypedElem typedElem, int elemCount) {
    TupleNode n = new TupleNode(typedElem, elemCount);
    return n;
  }

  class TupleNode extends Node {
    Node[] elemNodes;

    TupleNode(PTypedElem typedElem, int elemCount) {
      super(typedElem);
      this.elemNodes = new Node[elemCount];
      for (int i = 0; i < elemCount; i++) {
        this.elemNodes[i] = null;
      }
    }

    void setElemNode(int index, Node node) {
      this.elemNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkel[] ts = new PTypeSkel[this.elemNodes.length];
      PTypeSkel t;
      for (int i = 0; i < this.elemNodes.length; i++) {
        t = this.getTypeOf(this.elemNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.willNotReturn(t)) { return t; }
        ts[i] = t;
      }
      return this.typedElem.getScope().getLangDefinedTypeSkel(this.typedElem.getSrcInfo(), "tuple", ts);
    }
  }

  EmptyListNode createEmptyListNode(PTypedElem typedElem) {
    return new EmptyListNode(typedElem);
  }

  class EmptyListNode extends Node {
    EmptyListNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      return this.typedElem.getScope().getEmptyListType(this.typedElem.getSrcInfo());
    }
  }

  ListNode createListNode(PTypedElem typedElem) {
    ListNode n = new ListNode(typedElem);
    return n;
  }

  class ListNode extends Node {
    Node elemNode;
    Node tailNode;

    ListNode(PTypedElem typedElem) {
      super(typedElem);
    }

    void setElemNode(Node node) {
      this.elemNode = node;
    }

    void setTailNode(Node node) {
      this.tailNode = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel et = this.getTypeOf(this.elemNode);
      if (et == null) { return null; }
      if (PTypeRefSkel.willNotReturn(et)) { return et; }
      PTypeSkel tt = this.getTypeOf(this.tailNode);
      if (tt == null) { return null; }
      if (PTypeRefSkel.willNotReturn(tt)) { return tt; }
      if (!PTypeRefSkel.isList(tt)) {
        emsg = new StringBuffer();
        emsg.append("Type of list tail is invalid at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(". - ");
        emsg.append(tt);  // HERE: convert to readable expression
        throw new CompileException(emsg.toString());
      }
      PTypeSkel t = et.join(((PTypeRefSkel)tt).params[0], this.getGivenTvarList());
      if (t == null) {
// /* DEBUG */ System.out.print("joining "); System.out.print(et); System.out.print(" "); System.out.println(((PTypeRefSkel)tt).params[0]);
        emsg = new StringBuffer();
        emsg.append("Element type is incompatible at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  element: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(et));
        emsg.append("\n  tail: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(((PTypeRefSkel)tt).params[0]));
        throw new CompileException(emsg.toString());
      }
      return this.typedElem.getScope().getLangDefinedTypeSkel(this.typedElem.getSrcInfo(), "list", new PTypeSkel[] { t });
    }
  }

  StringNode createStringNode(PTypedElem typedElem, int elemCount) {
    StringNode n = new StringNode(typedElem, elemCount);
    return n;
  }

  class StringNode extends Node {
    Node[] elemNodes;

    StringNode(PTypedElem typedElem, int elemCount) {
      super(typedElem);
      this.elemNodes = new Node[elemCount];
      for (int i = 0; i < elemCount; i++) {
        this.elemNodes[i] = null;
      }
    }

    void setElemNode(int index, Node node) {
      this.elemNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      if (this.elemNodes.length == 0) {
        return this.typedElem.getScope().getEmptyStringType(this.typedElem.getSrcInfo());
      }
      PTypeSkel t = null;
      for (int i = 0; i < this.elemNodes.length; i++) {
        PTypeSkel t2 = this.getTypeOf(this.elemNodes[i]);
        if (t2 == null) { return null; }
        if (PTypeRefSkel.willNotReturn(t2)) { return t2; }
        if (t == null) {  // [0]
          t = t2;
        } else {
          t = t.join(t2, this.getGivenTvarList());
          if (t == null) {
            emsg = new StringBuffer();
            emsg.append("Element types incompatible at - ");
            emsg.append(this.typedElem.getSrcInfo());
            emsg.append(".");
            throw new CompileException(emsg.toString());
          }
        }
      }
      return this.typedElem.getScope().getLangDefinedTypeSkel(this.typedElem.getSrcInfo(), "string", new PTypeSkel[] { t });
    }
  }

  DataConstrNode createDataConstrNode(PTypedElem typedElem, PExprId dcon, int attrCount) {
    DataConstrNode n = new DataConstrNode(typedElem, dcon, attrCount);
    return n;
  }

  class DataConstrNode extends Node {
    PExprId dcon;
    Node[] attrNodes;

    DataConstrNode(PTypedElem typedElem, PExprId dcon, int attrCount) {
      super(typedElem);
      this.dcon = dcon;
      this.attrNodes = new Node[attrCount];
      for (int i = 0; i < attrCount; i++) {
        this.attrNodes[i] = null;
      }
    }

    void setAttrNode(int index, Node node) {
      this.attrNodes[index] = node;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PDataDef dataDef = this.dcon.props.defGetter.getDataDef();
      PDataDef.Constr constr = dataDef.getConstr(this.dcon.name);
      if (constr.getAttrCount() != this.attrNodes.length) {
        throw new RuntimeException("Attribute count mismatch.");
      }
      PTypeSkelBindings b = PTypeSkelBindings.create(this.getGivenTvarList());
      for (int i = 0; i < constr.getAttrCount(); i++) {
        PTypeSkel t = this.getTypeOf(this.attrNodes[i]);
        if (t == null) { return null; }
        if (PTypeRefSkel.willNotReturn(t)) { return t; }
        PDataDef.Attr a = constr.getAttrAt(i);
        PTypeSkel at = a.getNormalizedType();
        PTypeSkelBindings bb = b;
if (DEBUG > 1) {
          /* DEBUG */ System.out.print("attribute def: ");
          /* DEBUG */ System.out.println(at);
          /* DEBUG */ System.out.print("actual attribute: ");
          /* DEBUG */ System.out.println(t);
          /* DEBUG */ System.out.print("bindings: ");
          /* DEBUG */ System.out.println(b);
}
        if ((b = at.accept(PTypeSkel.NARROWER, true, t, b)) == null) {
          emsg = new StringBuffer();
          emsg.append("Type mismatch at ");
          emsg.append(this.typedElem.getSrcInfo());
          emsg.append(".");
          emsg.append("\n  attribute def: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(at));
          emsg.append("\n  attribute def in context: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(at.resolveBindings(bb)));
          emsg.append("\n  actual attribute: ");
          emsg.append(PTypeSkel.Repr.topLevelRepr(t));
if (DEBUG > 1) {
          /* DEBUG */ emsg.append("\n  bindings: ");
          /* DEBUG */ emsg.append(bb);
}
          throw new CompileException(emsg.toString());
        }
      }
if (DEBUG > 1) {
      /* DEBUG */ System.out.print("type application: ");
      /* DEBUG */ System.out.println(b);
}
      return constr.getType(b);
    }
  }

  TuplePtnNode createTuplePtnNode(PTypedElem typedElem, int elemCount) {
    return new TuplePtnNode(typedElem, elemCount);
  }

  class TuplePtnNode extends Node {
    int elemCount;

    TuplePtnNode(PTypedElem typedElem, int elemCount) {
      super(typedElem);
      this.elemCount = elemCount;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "tuple")) {
        emsg = new StringBuffer();
        emsg.append("Value is not tuple at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        throw new CompileException(emsg.toString());
      }
      PTypeRefSkel tr = (PTypeRefSkel)t;
      if (tr.params.length != this.elemCount) {
        emsg = new StringBuffer();
        emsg.append("Tuple's length mismatch at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return t;
    }
  }

  TuplePtnElemNode createTuplePtnElemNode(PTypedElem typedElem, int index) {
    return new TuplePtnElemNode(typedElem, index);
  }

  class TuplePtnElemNode extends Node {
    int index;

    TuplePtnElemNode(PTypedElem typedElem, int index) {
      super(typedElem);
      this.index = index;
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      return ((PTypeRefSkel)t).params[this.index];  // type is guaranteed in in-node
    }
  }

  ListPtnNode createListPtnNode(PTypedElem typedElem) {
    return new ListPtnNode(typedElem);
  }

  class ListPtnNode extends Node {

    ListPtnNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "list")) {
        emsg = new StringBuffer();
        emsg.append("Not in list at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actually in: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(t));
        throw new CompileException(emsg.toString());
      }
      return t;
    }
  }

  ListPtnElemNode createListPtnElemNode(PTypedElem typedElem) {
    ListPtnElemNode n = new ListPtnElemNode(typedElem);
    return n;
  }

  class ListPtnElemNode extends Node {

    ListPtnElemNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      return ((PTypeRefSkel)t).params[0];  // type is guaranteed in in-node
    }
  }

  StringPtnNode createStringPtnNode(PTypedElem typedElem) {
    return new StringPtnNode(typedElem);
  }

  class StringPtnNode extends Node {

    StringPtnNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      if (!PTypeRefSkel.isLangType(t, "string")) {
        emsg = new StringBuffer();
        emsg.append("Value is not string at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(t);
        throw new CompileException(emsg.toString());
      }
      return t;
    }
  }

  StringPtnElemNode createStringPtnElemNode(PTypedElem typedElem) {
    return new StringPtnElemNode(typedElem);
  }

  class StringPtnElemNode extends Node {

    StringPtnElemNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      StringBuffer emsg;
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }
      if (PTypeRefSkel.willNotReturn(t)) { return t; }
      return ((PTypeRefSkel)t).params[0];  // type is guaranteed in in-node
    }
  }

  DataConstrPtnNode createDataConstrPtnNode(PTypedElem typedElem, int context, PExprId dcon) {
    DataConstrPtnNode n = new DataConstrPtnNode(typedElem, context, dcon);
    return n;
  }

  class DataConstrPtnNode extends RefNode {
    int context;  // PPtnMatch.CONTEXT_*
    PExprId dcon;
    PTypeSkelBindings bindings;

    DataConstrPtnNode(PTypedElem typedElem, int context, PExprId dcon) {
      super(typedElem);
      if (context == PPtnMatch.CONTEXT_FIXED || context == PPtnMatch.CONTEXT_TRIAL) {
      } else {
        throw new IllegalArgumentException("Unexpected context. " + Integer.toString(this.context) + " " + this.typedElem.toString());
      } 
      this.context = context;
      this.dcon = dcon;
    }

    PTypeSkelBindings getBindings() throws CompileException {
      StringBuffer emsg;
      if (this.bindings != null) { return this.bindings; }
      PTypeSkel t = this.getTypeOf(this.inNode);
      if (t == null) { return null; }  // HERE: in case of <_>
// /* DEBUG */ if (!(t instanceof PTypeRefSkel)) { System.out.println("t " + t.toString()); }
      PDataDef dataDef = this.dcon.props.defGetter.getDataDef();
      PTypeRefSkel sig = (PTypeRefSkel)dataDef.getTypeSig();
      PTypeSkelBindings b = PTypeSkelBindings.create(this.getGivenTvarList());
      int width = PTypeSkel.WIDER;  // may strengthen check; warning?
      // int width = (this.context == PPtnMatch.CONTEXT_FIXED)? PTypeSkel.EQUAL: PTypeSkel.WIDER;
      if (sig.accept(width, true, t, b) == null) {
        emsg = new StringBuffer();
        emsg.append("Type mismatch at ");
        emsg.append(this.typedElem.getSrcInfo());
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
        emsg.append(this.typedElem.getSrcInfo());
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
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  value type: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(tr));
        emsg.append("\n  pattern type sig: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(sig));
        throw new CompileException(emsg.toString());
      }
      this.bindings = b;
if (DEBUG > 1) {
/* DEBUG */ System.out.print(this.typedElem);
/* DEBUG */ System.out.print(" >>bindings>> ");
/* DEBUG */ System.out.println(b);
}
      return this.bindings;
    }
  }

  DataConstrPtnAttrNode createDataConstrPtnAttrNode(PTypedElem typedElem, PExprId dcon, int index) {
    DataConstrPtnAttrNode n = new DataConstrPtnAttrNode(typedElem, dcon, index);
    return n;
  }

  class DataConstrPtnAttrNode extends Node {
    PExprId dcon;
    int index;

    DataConstrPtnAttrNode(PTypedElem typedElem, PExprId dcon, int index) {
      super(typedElem);
      this.dcon = dcon;
      this.index = index;
    }

    PTypeSkel infer() throws CompileException {
      PTypeSkelBindings b;
      if ((b = ((DataConstrPtnNode)this.inNode).getBindings()) == null) { return null; }
// /* DEBUG */ System.out.print("Inferring DataConstrPtnAttr ");
// /* DEBUG */ System.out.print(this.typedElem);
// /* DEBUG */ System.out.print(b);
      PDataDef dataDef = this.dcon.props.defGetter.getDataDef();
      PDataDef.Attr attr = dataDef.getConstr(this.dcon.name).getAttrAt(this.index);
// /* DEBUG */ System.out.println(attr.getNormalizedType());
      PTypeSkel.InstanciationBindings ib = PTypeSkel.InstanciationBindings.create(b);
      return attr.getNormalizedType().instanciate(ib);
    }
  }

  CondNode createCondNode(PTypedElem typedElem) {
    return new CondNode(typedElem);
  }

  class CondNode extends Node {

    CondNode(PTypedElem typedElem) {
      super(typedElem);
    }

    PTypeSkel infer() throws CompileException {
      return this.getTypeOf(this.inNode);
    }

    void check() throws CompileException {
      StringBuffer emsg;
      if (!PTypeRefSkel.isLangType(this.inNode.type, "bool")) {
// /* DEBUG */ System.out.println("checking binding...");
        emsg = new StringBuffer();
        emsg.append("Not <bool> at ");
        emsg.append(this.typedElem.getSrcInfo());
        emsg.append(".");
        emsg.append("\n  actual: ");
        emsg.append(PTypeSkel.Repr.topLevelRepr(this.inNode.type));
        throw new CompileException(emsg.toString());
      }
    }
  }
}
