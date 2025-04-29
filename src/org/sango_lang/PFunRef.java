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
  PEid official;  // null means self
  PDefDict.EidProps _resolved_officialProps;

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

  static PFunRef create(Parser.SrcInfo srcInfo, PScope outerScope, PEid official) {
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
    PEid id;
    if ((t = ParserA.acceptToken(reader, LToken.CARET_CARET, spc)) != null) {
      return createSelf(t.getSrcInfo(), outerScope);
    } else if ((t = ParserA.acceptToken(reader, LToken.CARET, spc)) == null) {
      return null;
    }
    if ((id = PEid.accept(reader, outerScope, Parser.QUAL_MAYBE, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("Function name missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    id.setCat(PDefDict.EID_CAT_FUN_OFFICIAL);
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
      PEid official = PEid.create(elem.getSrcInfo(), outerScope, mid, id);
      official.setCat(PDefDict.EID_CAT_FUN_OFFICIAL);
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

  public void collectModRefs() throws CompileException {
    if (this.official != null) {
      this.official.collectModRefs();
    }
  }

  public PFunRef resolve() throws CompileException {
    if (this.official != null) {
      this.official.setCat(PDefDict.EID_CAT_FUN_OFFICIAL);
      this._resolved_officialProps = this.scope.resolveEid(this.official);
      if (this._resolved_officialProps == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Function \"");
        emsg.append(this.official.repr());
        emsg.append("\" not defined at ");
        emsg.append(this.official.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    return this;
  }

  public void normalizeTypes() throws CompileException {}

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) {
    if (this.official != null) {
      this.typeGraphNode = graph.createFunRefNode(this, this.official);
    } else if (this.scope.pos == 1) {
      PEid callee = PEid.create(
          this.scope.evalStmt.srcInfo, this.scope,
          PModule.MOD_ID_HERE,
          this.scope.evalStmt.official);
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
    if (this.official != null) {
      node.addChild(flow.createNodeForFunRefBody(
        this.srcInfo, this.scope.theMod.modNameToModRefIndex(false, this._resolved_officialProps.key.modName), this.official.name));
    } else {
      node.addChild(flow.createNodeForSelfRef(this.srcInfo));
    }
    return node;
  }
}
