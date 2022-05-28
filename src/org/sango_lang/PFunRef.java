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
import java.util.List;

class PFunRef extends PDefaultExprObj {
  PExprId official;  // null means self

  private PFunRef(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("funref[src=");
    buf.append(this.srcInfo);
    if (this.official != null) {
      buf.append(",official=");
      buf.append(this.official);
    } else {
      buf.append(",SELF");
    }
    buf.append("]");
    return buf.toString();
  }

  static PFunRef create(Parser.SrcInfo srcInfo, PScope outerScope, PExprId official) {
    PFunRef fr = new PFunRef(srcInfo, outerScope);
    fr.official = official;
    return fr;
  }

  static PFunRef createSelf(Parser.SrcInfo srcInfo, PScope outerScope) {
    return create(srcInfo, outerScope, null);
  }

  boolean isSelfFormally() {
    return this.official == null;
  }

  static PFunRef accept(ParserA.TokenReader reader, PScope outerScope, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    PExprId id;
    if ((t = ParserA.acceptToken(reader, LToken.CARET_CARET, spc)) != null) {
      return createSelf(t.getSrcInfo(), outerScope);
    } else if ((t = ParserA.acceptToken(reader, LToken.CARET, spc)) == null) {
      return null;
    }
    if ((id = PExprId.accept(reader, outerScope, PExprId.ID_MAYBE_QUAL, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    id.setCat(PExprId.CAT_FUN_OFFICIAL);
    return create(t.getSrcInfo(), outerScope, id);
  }

  static PFunRef acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("fun")) { return null; }
    String id = elem.getAttrValueAsExtendedId("id");
    if (id == null) {
      emsg = new StringBuffer();
      emsg.append("Id missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    String mid = elem.getAttrValueAsId("mid");
    PFunRef f = null;
    if (id.equals("@SELF")) {
      if (mid != null) {
        emsg = new StringBuffer();
        emsg.append("Module id is not allowed for self reference at ");
        emsg.append(elem.getSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      f = createSelf(elem.getSrcInfo(), outerScope);
    } else if (Parser.isNormalId(id)) {
      PExprId official = PExprId.create(elem.getSrcInfo(), outerScope, mid, id);
      official.setCat(PExprId.CAT_FUN_OFFICIAL);
      f = create(elem.getSrcInfo(), outerScope, official);
    } else {
      emsg = new StringBuffer();
      emsg.append("Invalid id at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(". - ");
      emsg.append(id);
      throw new CompileException(emsg.toString());
    }
    return f;
  }

  // public void setupScope(PScope scope) {
    // if (scope == this.scope) { return; }
    // this.scope = scope;
    // this.idResolved = false;
    // if (this.official != null) {
      // this.official.setupScope(scope);
    // }
  // }

  public void collectModRefs() throws CompileException {
    if (this.official != null) {
      this.official.collectModRefs();
    }
  }

  public PFunRef resolve() throws CompileException {
    // if (this.idResolved) { return this; }
    if (this.official != null) {
      this.official = (PExprId)this.official.resolve();
    }
    // this.idResolved = true;
    return this;
  }

  public void normalizeTypes() {}

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    if (this.official != null) {
      this.typeGraphNode = graph.createFunRefNode(this, this.official);
    } else if (this.scope.funLevel == 0) {
      PExprId callee = PExprId.create(
          this.scope.evalStmt.srcInfo, this.scope,
          PModule.MOD_ID_HERE,
          this.scope.evalStmt.official);
      try {
        // callee.setupScope(this.scope);
        callee = (PExprId)callee.resolve();
      } catch (CompileException ex) {
        throw new RuntimeException("Internal error.");
      }
      this.typeGraphNode = graph.createFunRefNode(this, callee);
    } else {
      this.typeGraphNode = graph.createSelfRefNode(
        this,
        (PTypeGraph.ClosureNode)this.scope.closure.typeGraphNode);
    }
    return this.typeGraphNode;
  }

  public GFlow.Node setupFlow(GFlow flow) {
    GFlow.SeqNode node = flow.createNodeForFunRef(this.srcInfo);
    // List<PVarSlot> nvList = this.typeGraphNode.getNewvarList();
    // for (int i = 0; i < nvList.size(); i++) {
      // node.addChild(flow.createNodeForNewTvar(this.srcInfo, nvList.get(i), this.scope));
    // }
    if (this.official != null) {
      node.addChild(flow.createNodeForFunRefBody(
        this.srcInfo, this.scope.theMod.modNameToModRefIndex(this.official.props.modName), this.official.name));
    } else {
      node.addChild(flow.createNodeForSelfRef(this.srcInfo));
    }
    return node;
  }
}
