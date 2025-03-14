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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class PDataConstrEval extends PDefaultExprObj implements PEval {
  PEid dcon;
  PEvalItem.ObjItem posdAttrs[];
  PEvalItem.ObjItem namedAttrs[];
  Map<String, PEvalItem> namedAttrDict;
  PEvalItem.ObjItem using;
  PDefDict.EidProps _resolved_dconProps;
  PDataDef _resolved_dataDef;
  PExpr[] bdPosd;
  PExpr[] bdNamed;
  PExpr bdUsing;
  PExprObj[] bdAttrs;

  private PDataConstrEval(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("data_constr[src=");
    buf.append(this.srcInfo);
    buf.append(",dcon=");
    buf.append(this.dcon);
    buf.append(",posd_attrs=[");
    for (int i = 0; i < this.posdAttrs.length; i++) {
      buf.append(this.posdAttrs[i]);
      buf.append(",");
    }
    buf.append("],named_attrs=[");
    for (int i = 0; i < this.namedAttrs.length; i++) {
      buf.append(this.namedAttrs[i]);
      buf.append(",");
    }
    buf.append("],using=");
    buf.append(this.using);
    buf.append("]");
    return buf.toString();
  }

  static PDataConstrEval create(Parser.SrcInfo srcInfo, PScope outerScope,
      PEid dcon, PEvalItem.ObjItem[] posdAttrs, PEvalItem.ObjItem[] namedAttrs,
      PEvalItem.ObjItem using) throws CompileException {
    StringBuffer emsg;
    PDataConstrEval e = new PDataConstrEval(srcInfo, outerScope);
    e.dcon = dcon;
    e.posdAttrs = posdAttrs;
    e.namedAttrs = namedAttrs;
    e.using = using;
    e.namedAttrDict = new HashMap<String, PEvalItem>();
    for (int i = 0; i < namedAttrs.length; i++) {
      PEvalItem.ObjItem a = namedAttrs[i];
      if (a.name == null) {
        throw new IllegalArgumentException("No name.");
      }
      if (e.namedAttrDict.containsKey(a.name)) {
        emsg = new StringBuffer();
        emsg.append("Attribute name duplicated at ");
        emsg.append(a.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      e.namedAttrDict.put(a.name, a);
    }
    return e;
  }

  static PDataConstrEval convertFromResolvedUndet(Parser.SrcInfo srcInfo, PScope outerScope,
      PEid dcon, PEvalItem.ObjItem[] attrs) throws CompileException {
    StringBuffer emsg;
    PDataConstrEval e = new PDataConstrEval(srcInfo, outerScope);
    e.dcon = dcon;
    e.posdAttrs = attrs;
    e.namedAttrs = new PEvalItem.ObjItem[0];
    e.bdAttrs = attrs;
    return e;
  }

  static PDataConstrEval acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("constr")) { return null; }
    String dcon = elem.getAttrValueAsId("dcon");
    if (dcon == null) {
      emsg = new StringBuffer();
      emsg.append("Data constructor missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    String mid = elem.getAttrValueAsId("mid");

    ArrayList<PEvalItem> pas = new ArrayList<PEvalItem>();
    ArrayList<PEvalItem> nas = new ArrayList<PEvalItem>();
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("attrs")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        if (!ee.getName().equals("attr")) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        ParserB.Elem eee = ee.getFirstChild();
        if (eee == null) {
          emsg = new StringBuffer();
          emsg.append("Attribute missing at ");
          emsg.append(ee.getSrcInfo().toString());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        PExpr ex = PExpr.acceptX(eee, outerScope);
        if (ex == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(eee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        String name = ee.getAttrValueAsId("name");
        if (name != null) {
          nas.add(PEvalItem.ObjItem.create(ee.getSrcInfo(), outerScope, name, ex));
        } else if (!nas.isEmpty()) {
          emsg = new StringBuffer();
          emsg.append("Attribute name missing at ");
          emsg.append(ee.getSrcInfo().toString());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else {
          pas.add(PEvalItem.ObjItem.create(ee.getSrcInfo(), outerScope, null, ex));
        }
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    PEval using = null;
    if (e != null && e.getName().equals("other-attrs")) {
      ParserB.Elem ee = e.getFirstChild();
      if (ee == null) {
        emsg = new StringBuffer();
        emsg.append("Evaluation missing at ");
        emsg.append(e.getSrcInfo().toString());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      using = PEval.acceptX(ee, outerScope);
      if (using == null) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(ee.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      e = e.getNextSibling();
    }

    Parser.SrcInfo si = elem.getSrcInfo();
    return create(
      si, outerScope,
      PEid.create(si, outerScope, mid, dcon),
      pas.toArray(new PEvalItem.ObjItem[pas.size()]),
      nas.toArray(new PEvalItem.ObjItem[nas.size()]),
      PEvalItem.ObjItem.create(using.getSrcInfo(), outerScope, null, using));
  }

  public void collectModRefs() throws CompileException {
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.posdAttrs[i].collectModRefs();
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      this.namedAttrs[i].collectModRefs();
    }
    if (this.using != null) {
      this.using.collectModRefs();
    }
    this.dcon.collectModRefs();
  }

  public PDataConstrEval resolve() throws CompileException {
    StringBuffer emsg;
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.posdAttrs[i] = this.posdAttrs[i].resolve();
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      this.namedAttrs[i] = this.namedAttrs[i].resolve();
    }
    if (this.using != null) {
      this.using = this.using.resolve();
    }
    this.dcon.setDconEval();
    this._resolved_dconProps = this.scope.theMod.resolveAnchor(this.dcon);
    if (this._resolved_dconProps == null) {
      emsg = new StringBuffer();
      emsg.append("Data constructor \"");
      emsg.append(this.dcon.repr());
      emsg.append("\" is not defined at ");
      emsg.append(this.dcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PDefDict.IdKey tconKey = this.scope.getCompiler().defDict.getTconFromDconForEval(this.scope.theMod.name, this._resolved_dconProps.key);
    this._resolved_dataDef = this.scope.getCompiler().defDict.getDataDef(this.scope.theMod.name, tconKey);
    this.breakDown();
    return this;
  }
  
  // attribute source
  //  >=0 means to retrieve from named attribute and its position
  private static final int ATTR_FROM_NOT_DETERMINED = -1;
  private static final int ATTR_FROM_POSD = -2;
  private static final int ATTR_FROM_USING = -3;

  private void breakDown() throws CompileException {
    int[] attrSrcs = this.analyzeAttrSrc();
    this.bdAttrs = new PExprObj[attrSrcs.length];
    boolean u = false;
    boolean n = false;
    int ni = -1;
    for (int i = 0; i < attrSrcs.length; i++) {
      u |= attrSrcs[i] == ATTR_FROM_USING;
      n |= attrSrcs[i] >= 0 && attrSrcs[i] < ni;
      ni = (attrSrcs[i] >= 0)? attrSrcs[i]: ni;
    }
    if (u || n) {
      this.breakDown1(attrSrcs);
    } else {
      this.breakDown2(attrSrcs);  // straight!
    }
  }

  private int[] analyzeAttrSrc() throws CompileException {
    StringBuffer emsg;
    //PDataDef dataDef = this.dcon.props.defGetter.getDataDef();
    PDataDef.Constr constr = this._resolved_dataDef.getConstr(this.dcon.name);
    int[] attrSrcs = new int[constr.getAttrCount()];
    if (this.posdAttrs.length + this.namedAttrs.length > attrSrcs.length) {
      emsg = new StringBuffer();
      emsg.append("Too many attributes at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < this.posdAttrs.length; i++) {
      attrSrcs[i] = ATTR_FROM_POSD;
    }
    for (int i = this.posdAttrs.length; i < attrSrcs.length; i++) {
      attrSrcs[i] = ATTR_FROM_NOT_DETERMINED;
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      int index = constr.getAttrIndex(this.namedAttrs[i].name);
      if (index < 0) {
        emsg = new StringBuffer();
        emsg.append("Invalid attribute name \"");
        emsg.append(this.namedAttrs[i].name);
        emsg.append("\" at ");
        emsg.append(this.namedAttrs[i].srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (attrSrcs[index] != ATTR_FROM_NOT_DETERMINED) {
        emsg = new StringBuffer();
        emsg.append("Attribute \"");
        emsg.append(this.namedAttrs[i].name);
        emsg.append("\" is already specified at ");
        emsg.append(this.namedAttrs[i].srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      attrSrcs[index] = i;
    }
    for (int i = this.posdAttrs.length; i < attrSrcs.length; i++) {
      if (attrSrcs[i] == ATTR_FROM_NOT_DETERMINED) {
        if (this.using == null) {
          emsg = new StringBuffer();
          emsg.append("Insufficient attributes at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        attrSrcs[i] = ATTR_FROM_USING;
      }
    }
    return attrSrcs;
  }

  private void breakDown1(int[] attrSrcs) {
    // p0 = *VP0,
    // p1 = *VP1,
    // ...
    // n1 = *VN1,
    // n2 = *VN2,
    // ...
    // u = ** ... *VUp ... ** ... *VUq ... ** ... dcon$,
    // VP0 VP1 ... Vxn ... dcon$
    // /* DEBUG */ System.out.print("break down 1 - ");
    // /* DEBUG */ System.out.println(this);
    try {
      PExprVarDef[] posdVarDefs = new PExprVarDef[this.posdAttrs.length];
      PEval[] posdVarRefs = new PEval[this.posdAttrs.length];
      for (int i = 0; i < this.posdAttrs.length; i++) {
        String varName = this.scope.generateId();
        posdVarDefs[i] = PExprVarDef.create(this.srcInfo, this.scope, PExprVarDef.CAT_LOCAL_VAR, null, varName);
        PEid v = PEid.create(this.srcInfo, this.scope, null, varName);
        v.setVar();
        posdVarRefs[i] = PUndetEval.create(this.srcInfo, this.scope, v, new PEvalItem.ObjItem[0]);
        // posdVarRefs[i] = PExprVarRef.create(this.srcInfo, this.scope, varName, posdVarDefs[i].varSlot);
        // posdVarRefs[i] = PEid.create(this.srcInfo, this.scope, null, varName);
      }
      PExprVarDef[] namedVarDefs = new PExprVarDef[this.namedAttrs.length];
      PEval[] namedVarRefs = new PEval[this.namedAttrs.length];
      for (int i = 0; i < this.namedAttrs.length; i++) {
        String varName = this.scope.generateId();
        namedVarDefs[i] = PExprVarDef.create(this.srcInfo, this.scope, PExprVarDef.CAT_LOCAL_VAR, null, varName);
        PEid v = PEid.create(this.srcInfo, this.scope, null, varName);
        v.setVar();
        namedVarRefs[i] = PUndetEval.create(this.srcInfo, this.scope, v, new PEvalItem.ObjItem[0]);
        // namedVarRefs[i] = PExprVarRef.create(this.srcInfo, this.scope, varName, namedVarDefs[i].varSlot);
        // namedVarRefs[i] = PEid.create(this.srcInfo, this.scope, null, varName);
      }
      PExprVarDef[] usingVarDefs = new PExprVarDef[attrSrcs.length];
      PEval[] usingVarRefs = new PEval[attrSrcs.length];
      for (int i = 0; i < attrSrcs.length; i++) {
        if (attrSrcs[i] == ATTR_FROM_USING) {
          String varName = this.scope.generateId();
          usingVarDefs[i] = PExprVarDef.create(this.srcInfo, this.scope, PExprVarDef.CAT_LOCAL_VAR, null, varName);
          PEid v = PEid.create(this.srcInfo, this.scope, null, varName);
          v.setVar();
          usingVarRefs[i] = PUndetEval.create(this.srcInfo, this.scope, v, new PEvalItem.ObjItem[0]);
          // usingVarRefs[i] = PExprVarRef.create(this.srcInfo, this.scope, varName, usingVarDefs[i].varSlot);
          // usingVarRefs[i] = PEid.create(this.srcInfo, this.scope, null, varName);
        } else {
          usingVarDefs[i] = null;
          usingVarRefs[i] = null;
        }
      }
      this.bdPosd = new PExpr[this.posdAttrs.length];
      for (int i = 0; i < this.posdAttrs.length; i++) {
        PEval.Builder evalBuilder = PEval.Builder.newInstance(this.srcInfo, this.scope);
        evalBuilder.addItem(this.posdAttrs[i].shallowCopyNoName());
        PPtn.Builder ptnBuilder = PPtn.Builder.newInstance(this.srcInfo, this.scope);
        ptnBuilder.setContext(PPtnMatch.CONTEXT_FIXED);
        ptnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, posdVarDefs[i]));
        this.bdPosd[i] = PExpr.create(this.srcInfo, this.scope, evalBuilder.create(),
            PPtnMatch.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, ptnBuilder.create()));
        this.bdPosd[i] = this.bdPosd[i].resolve();  // *
        // /* DEBUG */ System.out.print("  >> ");
        // /* DEBUG */ System.out.println(this.bdPosd[i]);
      }
      this.bdNamed = new PExpr[this.namedAttrs.length];
      for (int i = 0; i < this.namedAttrs.length; i++) {
        PEval.Builder evalBuilder = PEval.Builder.newInstance(this.srcInfo, this.scope);
        evalBuilder.addItem(this.namedAttrs[i].shallowCopyNoName());
        PPtn.Builder ptnBuilder = PPtn.Builder.newInstance(this.srcInfo, this.scope);
        ptnBuilder.setContext(PPtnMatch.CONTEXT_FIXED);
        ptnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, namedVarDefs[i]));
        this.bdNamed[i] = PExpr.create(this.srcInfo, this.scope, evalBuilder.create(),
            PPtnMatch.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, ptnBuilder.create()));
        this.bdNamed[i] = this.bdNamed[i].resolve();  // *
        // /* DEBUG */ System.out.print("  >> ");
        // /* DEBUG */ System.out.println(this.bdNamed[i]);
      }
      if (this.using != null) {
        PEval.Builder usingEvalBuilder = PEval.Builder.newInstance(this.srcInfo, this.scope);
        usingEvalBuilder.addItem(this.using);
        PPtn.Builder usingPtnBuilder = PPtn.Builder.newInstance(this.srcInfo, this.scope);
        usingPtnBuilder.setContext(PPtnMatch.CONTEXT_FIXED);
        for (int i = 0; i < attrSrcs.length; i++) {
          usingPtnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, 
            (usingVarDefs[i] != null)? usingVarDefs[i]: PWildCard.create(this.srcInfo, this.scope)
          ));
        }
        usingPtnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, this.dcon));
        this.bdUsing = PExpr.create(this.srcInfo, this.scope, usingEvalBuilder.create(),
            PPtnMatch.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, usingPtnBuilder.create()));
        this.bdUsing = this.bdUsing.resolve();  // *
        // /* DEBUG */ System.out.print("  >> ");
        // /* DEBUG */ System.out.println(this.bdUsing);
        // /* DEBUG */ System.out.print("  >>> ");
      }
      for (int i = 0; i < attrSrcs.length; i++) {
        int as = attrSrcs[i];
        PExprObj e;
        switch (as) {
        case ATTR_FROM_POSD:
          e = posdVarRefs[i];
          break;
        case ATTR_FROM_USING:
          e = usingVarRefs[i];
          break;
        default:  //  from named attribute
          e = namedVarRefs[as];
          break;
        }
        this.bdAttrs[i] = e;
        this.bdAttrs[i] = e.resolve();  // *
        // /* DEBUG */ System.out.print(i);
        // /* DEBUG */ System.out.print(":");
        // /* DEBUG */ System.out.print(this.bdAttrs[i]);
        // /* DEBUG */ System.out.print(" ");
      }
      // /* DEBUG */ System.out.println();
    } catch (CompileException ex) {
/* DEBUG */ ex.printStackTrace(System.out);
      throw new RuntimeException("Internal error: " + ex.toString());
    }
  }

  private void breakDown2(int[] attrSrcs) {
    // /* DEBUG */ System.out.print("break down 2 - ");
    // /* DEBUG */ System.out.println(this);
    if (this.using != null) {
      try {
        PEval.Builder usingEvalBuilder = PEval.Builder.newInstance(this.srcInfo, this.scope);
        usingEvalBuilder.addItem(this.using);
        PPtn.Builder usingPtnBuilder = PPtn.Builder.newInstance(this.srcInfo, this.scope);
        usingPtnBuilder.setContext(PPtnMatch.CONTEXT_FIXED);
        for (int i = 0; i < attrSrcs.length; i++) {
          usingPtnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, PWildCard.create(this.srcInfo, this.scope)));
        }
        usingPtnBuilder.addItem(PPtnItem.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, this.dcon));
        this.bdUsing = PExpr.create(this.srcInfo, this.scope, usingEvalBuilder.create(),
            PPtnMatch.create(this.srcInfo, this.scope, PPtnMatch.CONTEXT_FIXED, null, usingPtnBuilder.create()));
        this.bdUsing = this.bdUsing.resolve();  // *
      } catch (CompileException ex) {
        throw new RuntimeException("Internal error: " + ex.toString());
      }
    }
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.bdAttrs[i] = this.posdAttrs[i].shallowCopyNoName();
    }
    for (int i = this.posdAttrs.length, j = 0 ; j < this.namedAttrs.length; i++, j++) {
      this.bdAttrs[i] = (PExprObj)this.namedAttrs[j].obj;
    }
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.posdAttrs[i].normalizeTypes();
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      this.namedAttrs[i].normalizeTypes();
    }
    if (this.using != null) {
      this.using.normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    if (this.bdPosd != null) {
      for (int i = 0; i < this.bdPosd.length; i++) {
        this.bdPosd[i].setupTypeGraph(graph);
      }
    }
    if (this.bdNamed != null) {
      for (int i = 0; i < this.bdNamed.length; i++) {
        this.bdNamed[i].setupTypeGraph(graph);
      }
    }
    if (this.bdUsing != null) {
      this.bdUsing.setupTypeGraph(graph);
    }
    this.typeGraphNode = graph.createDataConstrNode(this, this.dcon, this.bdAttrs.length);
    for (int i = 0; i < this.bdAttrs.length; i++) {
      PTypeGraph.Node an = this.bdAttrs[i].setupTypeGraph(graph);
      // HERE: add constraint to attr
      ((PTypeGraph.DataConstrNode)this.typeGraphNode).setAttrNode(i, an);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForDataConstr(this.srcInfo);
    if (this.bdPosd != null) {
      for (int i = 0; i < this.bdPosd.length; i++) {
        node.addChild(this.bdPosd[i].setupFlow(flow));
        node.addChild(flow.createSinkNode(this.srcInfo));
      }
    }
    if (this.bdNamed != null) {
      for (int i = 0; i < this.bdNamed.length; i++) {
        node.addChild(this.bdNamed[i].setupFlow(flow));
        node.addChild(flow.createSinkNode(this.srcInfo));
      }
    }
    if (this.bdUsing != null) {
      node.addChild(this.bdUsing.setupFlow(flow));
      node.addChild(flow.createSinkNode(this.srcInfo));
    }
    PTypeRefSkel type = (PTypeRefSkel)this.typeGraphNode.getFinalizedType();
    // /* DEBUG */ System.out.println(type.tconProps);
    GFlow.DataConstrNode n = flow.createNodeForDataConstrBody(
      this.srcInfo, this.scope.theMod.modNameToModRefIndex(this._resolved_dconProps.key.modName), this.dcon.name,
      type.tconKey.idName, type.params.length);
    for (int i = 0; i < this.bdAttrs.length; i++) {
      n.addChild(this.bdAttrs[i].setupFlow(flow));
    }
    node.addChild(n);
    return node;
  }
}
