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
import java.util.Map;

class PDataConstrPtn extends PDefaultPtnElem {
  PExprId dcon;
  PPtnElem posdAttrs[];
  PPtnItem namedAttrs[];
  Map<String, PPtnItem> namedAttrDict;
  boolean wildCards;
  PPtnElem[] sortedAttrs;

  private PDataConstrPtn() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("data_constr_ptn[src=");
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
    buf.append("],wildcards=");
    buf.append(this.wildCards);
    buf.append("]");
    return buf.toString();
  }

  static PDataConstrPtn create(Parser.SrcInfo srcInfo, PExprId dcon,
      PPtnElem[] posdAttrs, PPtnItem[] namedAttrs,
      boolean wildCards) throws CompileException {
    StringBuffer emsg;
    PDataConstrPtn p = new PDataConstrPtn();
    p.srcInfo = srcInfo;
    p.dcon = dcon;
    p.posdAttrs = posdAttrs;
    p.namedAttrs = namedAttrs;
    p.wildCards = wildCards;
    p.namedAttrDict = new HashMap<String, PPtnItem>();
    for (int i = 0; i < namedAttrs.length; i++) {
      PPtnItem a = namedAttrs[i];
      if (a.name == null) {
        throw new IllegalArgumentException("No name.");
      }
      if (p.namedAttrDict.containsKey(a.name)) {
        emsg = new StringBuffer();
        emsg.append("Attribute name duplicated at ");
        emsg.append(a.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      p.namedAttrDict.put(a.name, a);
    }
    return p;
  }

  static PDataConstrPtn acceptX(ParserB.Elem elem) throws CompileException {
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

    ArrayList<PPtnElem> pas = new ArrayList<PPtnElem>();
    ArrayList<PPtnItem> nas = new ArrayList<PPtnItem>();
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
        PPtnMatch pm = PPtnMatch.acceptX(eee);
        if (pm == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(eee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        String name = ee.getAttrValueAsId("name");
        if (name != null) {
          nas.add(PPtnItem.create(ee.getSrcInfo(), name, pm));
        } else if (!nas.isEmpty()) {
          emsg = new StringBuffer();
          emsg.append("Attribute name missing at ");
          emsg.append(ee.getSrcInfo().toString());
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else {
          pas.add(pm);
        }
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    boolean wildCards = false;
    if (e != null && e.getName().equals("other-attrs")) {
      wildCards = true;
      e = e.getNextSibling();
    }

    Parser.SrcInfo si = elem.getSrcInfo();
    return create(
      si,
      PExprId.create(si, mid, dcon),
      pas.toArray(new PPtnElem[pas.size()]),
      nas.toArray(new PPtnItem[nas.size()]),
      wildCards);
  }

  public PDataConstrPtn setupScope(PScope scope) throws CompileException {
    if (scope == this.scope) { return this; }
    this.scope = scope;
    this.idResolved = false;
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.posdAttrs[i] = this.posdAttrs[i].setupScope(scope);
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      this.namedAttrs[i] = this.namedAttrs[i].setupScope(scope);
    }
    this.dcon = (PExprId)this.dcon.setupScope(scope);
    return this;
  }

  public PDataConstrPtn resolveId() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.posdAttrs.length; i++) {
      this.posdAttrs[i] = this.posdAttrs[i].resolveId();
    }
    for (int i = 0; i < this.namedAttrs.length; i++) {
      this.namedAttrs[i] = this.namedAttrs[i].resolveId();
    }
    this.dcon = this.dcon.resolveId();
    this.idResolved = true;
    this.sortAttrs();
    return this;
  }

  // attribute source
  //  >=0 means to bind to named attribute and its position
  private static final int ATTR_TO_NOT_DETERMINED = -1;
  private static final int ATTR_TO_POSD = -2;
  private static final int ATTR_TO_NONE = -3;

  private void sortAttrs() throws CompileException {
    int[] attrDsts = this.analyzeAttrDst();
    this.sortedAttrs = new PPtnElem[attrDsts.length];
    for (int i = 0; i < attrDsts.length; i++) {
      int ad = attrDsts[i];
      PPtnElem p;
      switch (ad) {
      case ATTR_TO_POSD:
        p = this.posdAttrs[i];
        break;
      case ATTR_TO_NONE:
        p = PWildCard.create(this.srcInfo);
        break;
      default:  //  to named attribute
        p = (PPtnElem)this.namedAttrs[ad].elem;
        break;
      }
      this.sortedAttrs[i] = p;
    }
  }

  private int[] analyzeAttrDst() throws CompileException {
    StringBuffer emsg;
    PDataDef dataDef = this.dcon.props.defGetter.getDataDef();
    PDataDef.Constr constr = dataDef.getConstr(this.dcon.name);
    int[] attrDsts = new int[constr.getAttrCount()];
    if (this.posdAttrs.length + this.namedAttrs.length > attrDsts.length) {
      emsg = new StringBuffer();
      emsg.append("Too many attributes at ");
      emsg.append(this.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < this.posdAttrs.length; i++) {
      attrDsts[i] = ATTR_TO_POSD;
    }
    for (int i = this.posdAttrs.length; i < attrDsts.length; i++) {
      attrDsts[i] = ATTR_TO_NOT_DETERMINED;
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
      if (attrDsts[index] != ATTR_TO_NOT_DETERMINED) {
        emsg = new StringBuffer();
        emsg.append("Attribute \"");
        emsg.append(this.namedAttrs[i].name);
        emsg.append("\" is already specified at ");
        emsg.append(this.namedAttrs[i].srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      attrDsts[index] = i;
    }
    for (int i = this.posdAttrs.length; i < attrDsts.length; i++) {
      if (attrDsts[i] == ATTR_TO_NOT_DETERMINED) {
        if (!this.wildCards) {
          emsg = new StringBuffer();
          emsg.append("Insufficient attributes at ");
          emsg.append(this.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        attrDsts[i] = ATTR_TO_NONE;
      }
    }
    return attrDsts;
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.sortedAttrs.length; i++) {
      this.sortedAttrs[i].normalizeTypes();
    }
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.typeGraphNode = graph.createDataConstrPtnNode(this, this.dcon);
    for (int i = 0; i < this.sortedAttrs.length; i++) {
      PTypeGraph.Node an = this.sortedAttrs[i].setupTypeGraph(graph);
      PTypeGraph.DataConstrPtnAttrNode an2 = graph.createDataConstrPtnAttrNode(this, this.dcon, i);
      an2.setInNode(this.typeGraphNode);
      an.setInNode(an2);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForDataConstrPtn(
      this.srcInfo, this.scope.theMod.modNameToModRefIndex(this.dcon.props.modName), this.dcon.name,
      null, 0);
    for (int i = 0; i < this.sortedAttrs.length; i++) {
      node.addChild(this.sortedAttrs[i].setupFlow(flow));
    }
    return node;
  }
}
