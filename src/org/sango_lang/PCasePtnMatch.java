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
import java.util.ArrayList;
import java.util.List;

class PCasePtnMatch extends PDefaultProgElem {
  PPtnMatch ptnMatch;
  PPtnDetail[] details;

  private PCasePtnMatch() {}

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("case_ptnmatch[src=");
    buf.append(this.srcInfo);
    buf.append(",ptnmatch=");
    buf.append(this.ptnMatch);
    buf.append(",details=[");
    for (int i = 0; i < this.details.length; i++) {
      buf.append(this.details[i]);
      buf.append(",");
    }
    buf.append("]]");
    return buf.toString();
  }

  static class Builder {
    PCasePtnMatch casePtnMatch;
    List<PPtnDetail> detailList;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.casePtnMatch = new PCasePtnMatch();
      this.detailList = new ArrayList<PPtnDetail>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.casePtnMatch.srcInfo = si;
    }

    void setPtnMatch(PPtnMatch ptnMatch) {
      this.casePtnMatch.ptnMatch = ptnMatch;
    }

    void addDetail(PPtnDetail detail) {
      this.detailList.add(detail);
    }

    void addDetailList(List<PPtnDetail> detailList) {
      for (int i = 0; i < detailList.size(); i++) {
        this.addDetail(detailList.get(i));
      }
    }

    PCasePtnMatch create() throws CompileException {
      StringBuffer emsg;
      this.casePtnMatch.details = this.detailList.toArray(new PPtnDetail[this.detailList.size()]);
      return this.casePtnMatch;
    }
  }

  static PCasePtnMatch accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    PPtnMatch ptnMatch;
    Parser.SrcInfo si = reader.getCurrentSrcInfo();
    if ((ptnMatch = PPtnMatch.accept(reader, PPtnMatch.CONTEXT_TRIAL)) == null) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(si);
    builder.setPtnMatch(ptnMatch);
    if (ParserA.acceptToken(reader, LToken.EQ_GT, ParserA.SPACE_DO_NOT_CARE) != null) {
      builder.addDetailList(PPtnDetail.acceptSeq(reader));
    }
    return builder.create();
  }

  static List<PCasePtnMatch> acceptSeq(ParserA.TokenReader reader) throws CompileException, IOException {
    List<PCasePtnMatch> casePtnMatchList = new ArrayList<PCasePtnMatch>();
    PCasePtnMatch casePtnMatch;
    int state = 0;
    while (state >= 0) {
      switch (state) {
      case 0:
        if ((casePtnMatch = accept(reader)) != null) {
          casePtnMatchList.add(casePtnMatch);
          state = 1;
        } else if (ParserA.acceptToken(reader, LToken.VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
          state = 0;
        } else {
          state = -1;
        }
        break;
      case 1:
        if (ParserA.acceptToken(reader, LToken.VBAR, ParserA.SPACE_DO_NOT_CARE) != null) {
          state = 0;
        } else {
          state = -1;
        }
      }
    }
    return casePtnMatchList;
  }

  static PCasePtnMatch acceptX(ParserB.Elem elem) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("case-ptn-match")) { return null; }
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(elem.getSrcInfo());
    ParserB.Elem e = elem.getFirstChild();
    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Pattern match missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PPtnMatch pm = PPtnMatch.acceptX(e, PPtnMatch.CONTEXT_TRIAL);
    if (pm == null) {
      emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(e.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
    builder.setPtnMatch(pm);
    e = e.getNextSibling();
    if (e != null) {
      if (!e.getName().equals("ptn-details")) {
        emsg = new StringBuffer();
        emsg.append("Unexpected XML node. - ");
        emsg.append(e.getSrcInfo().toString());
        throw new CompileException(emsg.toString());
      }
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PPtnDetail pd = PPtnDetail.acceptX(ee);
        if (pd == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        builder.addDetail(pd);
        ee = ee.getNextSibling();
      }
    }
    return builder.create();
  }

  public void setupScope(PScope scope) {
    if (scope == this.scope) { return; }
    this.scope = scope;
    this.idResolved = false;
    this.ptnMatch.setupScope(scope);
    for (int i = 0; i < this.details.length; i++) {
      this.details[i].setupScope(scope);
    }
  }

  public void collectModRefs() throws CompileException {
    this.ptnMatch.collectModRefs();
    for (int i = 0; i < this.details.length; i++) {
      this.details[i].collectModRefs();
    }
  }

  public PCasePtnMatch resolve() throws CompileException {
    if (this.idResolved) { return this; }
    this.ptnMatch = (PPtnMatch)this.ptnMatch.resolve();
    for (int i = 0; i < this.details.length; i++) {
      this.details[i] = (PPtnDetail)this.details[i].resolve();
    }
    this.idResolved = true;
    return this;
  }

  public void normalizeTypes() throws CompileException {
    this.ptnMatch.normalizeTypes();
    for (int i = 0; i < this.details.length; i++) {
      this.details[i].normalizeTypes();
    }
  }

  PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    this.ptnMatch.setupTypeGraph(graph);
    for (int i = 0; i < this.details.length; i++) {
      this.details[i].setupTypeGraph(graph);
    }
    return null;
  }

  void setTypeGraphInNode(PTypeGraph.Node node) {
      this.ptnMatch.setTypeGraphInNode(node);
  }

  GFlow.Node setupFlow(GFlow flow, GFlow.BranchNode b) {
    GFlow.SeqNode node = flow.createNodeForCasePtnMatch(this.srcInfo);
    node.addChild(flow.createCopyNode(this.srcInfo));
    node.addChild(flow.createMatchingRootNode(this.srcInfo, (GFlow.PtnMatchNode)this.ptnMatch.setupFlow(flow)));
    if (this.details != null) {
      for (int i = 0; i < this.details.length; i++) {
        node.addChild(this.details[i].setupFlow(flow, b));
      }
    }
    return node;
  }
}
