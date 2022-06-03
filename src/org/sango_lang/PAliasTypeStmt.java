/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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
import java.util.Map;

class PAliasTypeStmt extends PDefaultProgObj implements PAliasTypeDef {
  PType sig;
  String tcon;
  int availability;
  int acc;
  PTypeVarDef[] tparams;
  PType body;  // is PTypeRef after circular def check
  PTypeRefSkel bodySkel;

  PAliasTypeStmt(Parser.SrcInfo srcInfo, PScope outerScope) {
    super(srcInfo, outerScope.enterInner());
    this.scope.startDef();
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("alias[src=");
    buf.append(this.srcInfo);
    buf.append(",sig=");
    buf.append(this.sig);
    buf.append("],acc=");
    buf.append(this.acc);
    buf.append(",body=");
    buf.append(this.body);
    buf.append("]");
    return buf.toString();
  }

  static class Builder {
    PAliasTypeStmt alias;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.alias = new PAliasTypeStmt(srcInfo, outerScope);
    }

    PScope getScope() { return this.alias.scope; }

    void setSig(PType sig) {
      this.alias.sig = sig;
    }

    void setAvailability(int availability) {
      this.alias.availability = availability;
    }

    void setAcc(int acc) {
      this.alias.acc = acc;
    }

    void setBody(PType body) {
      this.alias.body = body;
    }

    PAliasTypeStmt create() throws CompileException {
      if (this.alias.sig instanceof PTypeId) {
        PTypeId ti = (PTypeId)this.alias.sig;
        this.alias.tcon = ti.name;
        this.alias.tparams = new PTypeVarDef[0];
      } else if (this.alias.sig instanceof PTypeRef) {
        PTypeRef tr = (PTypeRef)this.alias.sig;
        this.alias.tcon = tr.tcon;
        this.alias.tparams = new PTypeVarDef[tr.params.length];
        for (int i = 0; i < tr.params.length; i++) {
          this.alias.tparams[i] = (PTypeVarDef)tr.params[i];
        }
      } else {
        throw new RuntimeException("Unexpected type. " + this.alias.sig.toString());
      }
      return this.alias;
    }
  }

  static PAliasTypeStmt accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token next = reader.getNextToken();
    ParserA.Token t;
    if (next.wordEquals("type") &&
        (t = ParserA.acceptSpecifiedWord(reader, "alias", ParserA.SPACE_DO_NOT_CARE)) != null) {
      ;
    } else {
      return null;
    }
    ParserA.acceptSpecifiedWord(reader, "type", ParserA.SPACE_NEEDED);
    Builder builder = Builder.newInstance(t.getSrcInfo(), outerScope);
    PScope scope = builder.getScope();
    builder.setAvailability(PModule.acceptAvailability(reader));
    PType tsig;
    if ((tsig = PType.acceptSig2(reader, scope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Type description missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setSig(tsig);
    builder.setAcc(PModule.acceptAcc(reader, PModule.ACC_OPTS_FOR_ALIAS, PModule.ACC_DEFAULT_FOR_ALIAS));
    if (ParserA.acceptToken(reader, LToken.COL_EQ, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\":=\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType body;
    if ((body = acceptAliasBodyDef(reader, scope)) == null) {
      emsg = new StringBuffer();
      emsg.append("Alias definition missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setBody(body);
    if (ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return builder.create();
  }

  static PAliasTypeStmt acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    StringBuffer emsg;
    if (!elem.getName().equals("alias-type-def")) { return null; }
    Builder builder = Builder.newInstance(elem.getSrcInfo(), outerScope);
    PScope scope = builder.getScope();

    String tcon = elem.getAttrValueAsId("tcon");
    if (tcon == null) {
      emsg = new StringBuffer();
      emsg.append("Type constructor missing at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PTypeId tconItem = PTypeId.create(elem.getSrcInfo(), scope, null, tcon, false);
    tconItem.setTcon();

    builder.setAvailability(PModule.acceptXAvailabilityAttr(elem));
    int acc = PModule.acceptXAccAttr(elem, PModule.ACC_OPTS_FOR_ALIAS, PModule.ACC_DEFAULT_FOR_ALIAS);
    builder.setAcc(acc);

    PType.Builder tb = PType.Builder.newInstance(elem.getSrcInfo(), scope);
    ParserB.Elem e = elem.getFirstChild();
    if (e != null && e.getName().equals("params")) {
      ParserB.Elem ee = e.getFirstChild();
      while (ee != null) {
        PTypeVarDef var = PTypeVarDef.acceptX(ee, scope);
        if (var == null) {
          emsg = new StringBuffer();
          emsg.append("Unexpected XML node. - ");
          emsg.append(ee.getSrcInfo().toString());
          throw new CompileException(emsg.toString());
        }
        tb.addItem(var);
        ee = ee.getNextSibling();
      }
      e = e.getNextSibling();
    }
    tb.addItem(tconItem);
    builder.setSig(tb.create());

    if (e == null) {
      emsg = new StringBuffer();
      emsg.append("Alias definition missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (!e.getName().equals("is")) {
      emsg = new StringBuffer();
      emsg.append("Alias definition missing at ");
      emsg.append(elem.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    ParserB.Elem d = e.getFirstChild();
    if (d == null) {
      emsg = new StringBuffer();
      emsg.append("Alias definition missing at ");
      emsg.append(e.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    PType body = PType.acceptXRO(d, scope);
    if (body == null) {
      emsg = new StringBuffer();
      emsg.append("Alias definition missing at ");
      emsg.append(e.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setBody(body);
    return builder.create();
  }

  private static PType acceptAliasBodyDef(ParserA.TokenReader reader, PScope scope) throws CompileException, IOException {
    return PType.acceptRO(reader, scope, ParserA.SPACE_DO_NOT_CARE);
  }

  // public void setupScope(PScope scope) {
    // StringBuffer emsg;
    // if (this.scope != null) { throw new RuntimeException("Scope is already set.");}
    // // if (s == this.scope) { return; }
    // this.scope = scope.start();
    // this.idResolved = false;
    // this.sig.setupScope(this.scope);
    // this.body.setupScope(this.scope);
  // }

  public void collectModRefs() throws CompileException {
    // sig has no mod refs
    this.body.collectModRefs();
  }

  public PAliasTypeStmt resolve() throws CompileException {
    StringBuffer emsg;
    // if (this.idResolved) { return this; }
    this.sig = this.sig.resolve();
    // for (int i = 0; i < this.tparams.length; i++) {
      // this.tparams[i] = this.tparams[i].resolve();
    // }
    this.body = this.body.resolve();
    if (!(this.body instanceof PTypeRef)) {
      emsg = new StringBuffer();
      emsg.append("Alias of non-concrete type at ");
      emsg.append(this.body.getSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    // this.idResolved = true;
    return this;
  }

  PAliasTypeDef getAliasTypeDef() { return this; }

  void checkCyclicAlias() throws CompileException {
// /* DEBUG */ System.out.println("unalias " + this.tcon);
// /* DEBUG */ System.out.println(((PTypeRef)this.sig).tconInfo.toString());
    AliasGraphNode n = AliasGraphNode.createRoot(((PTypeRef)this.sig).tconInfo);
    this.checkUnalias(n);
  }

  void checkUnalias(AliasGraphNode a) throws CompileException {
    StringBuffer emsg;
// /* DEBUG */ System.out.println(this);
    if (a.ti.props.subcat != PTypeId.SUBCAT_ALIAS) { return; }
    List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    a.ti.props.defGetter.getAliasTypeDef().collectUnaliasTconInfo(tis);
// /* DEBUG */ System.out.println("unalias to " + tis.toString());
    for (int i = 0; i < tis.size(); i++) {
      AliasGraphNode c = a.addChild(tis.get(i));
      if (c == null) {
        emsg = new StringBuffer();
        emsg.append("Circular definition detected for \"");
        emsg.append(this.tcon);
        emsg.append("\" at ");
        emsg.append(this.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.checkUnalias(c);
    }
  }
 
  void checkAcc() throws CompileException {
    if (this.acc == Module.ACC_PRIVATE) { return; }
    this.body.excludePrivateAcc();
  }

  public void normalizeTypes() {
    throw new RuntimeException("PAliasTypeStmt#normalizeTypes() should not be called. - " + this.toString());
  }

  public String getTcon() { return this.tcon; }

  public PTypeVarSlot[] getParamVarSlots() {
    PTypeVarSlot[] vs = new PTypeVarSlot[this.tparams.length] ;
    for (int i = 0; i < this.tparams.length; i++) {
      vs[i] = this.tparams[i].varSlot;
    }
    return vs;
  }

  public int getAvailability() { return this.availability; }

  public int getAcc() { return this.acc; }

  public void collectUnaliasTconInfo(List<PDefDict.TconInfo> list) { this.body.getSkel().collectTconInfo(list); }

  public PTypeRefSkel getBody() {
    if (this.bodySkel == null) {
      this.setupBodySkel();
    }
    return this.bodySkel;
  }

  public PTypeRefSkel unalias(PTypeSkel[] params) {
    if (params.length != this.tparams.length) {
      throw new IllegalArgumentException("Length of unaliasing params mismatch.");
    }
    if (this.bodySkel == null) {
      this.setupBodySkel();
    }
    // /* DEBUG */ System.out.print("unalias");
    PTypeSkelBindings bindings = PTypeSkelBindings.create();
    for (int i = 0; i < this.tparams.length; i++) {
    // /* DEBUG */ System.out.print(" ");
    // /* DEBUG */ System.out.print(this.tparams[i]);
    /* DEBUG */ if (this.tparams[i].varSlot == params[i].getVarSlot()) { throw new RuntimeException("Attempt to bind itself."); }
      bindings.bind(this.tparams[i].varSlot, params[i]);  // param is normalized in advance
    }
    // /* DEBUG */ System.out.print(" ");
    // /* DEBUG */ System.out.print(this.tcon);
    // /* DEBUG */ System.out.print(this.bodySkel.toString());
    // /* DEBUG */ System.out.print(" ...start instanciation... ");
    // /* DEBUG */ System.out.print(bindings.toString());
    PTypeRefSkel tr = (PTypeRefSkel)this.bodySkel.instanciate(PTypeSkel.InstanciationBindings.create(bindings));
    return tr;
  }

  void setupBodySkel() {
    this.bodySkel = (PTypeRefSkel)((PTypeRefSkel)this.body.getSkel()).unalias(PTypeSkelBindings.create());
    List<PDefDict.TconInfo> tis = new ArrayList<PDefDict.TconInfo>();
    this.bodySkel.collectTconInfo(tis);
    this.scope.addReferredTcons(tis);
  }

  private static class AliasGraphNode {
   PDefDict.TconInfo ti;
    AliasGraphNode parent;
    List<AliasGraphNode> children;

    static AliasGraphNode createRoot(PDefDict.TconInfo ti) {
      return new AliasGraphNode(ti);
    }

    private AliasGraphNode(PDefDict.TconInfo ti) {
// /* DEBUG */ System.out.println(ti);
      this.ti = ti;
      this.children = new ArrayList<AliasGraphNode>();
    }

    AliasGraphNode addChild(PDefDict.TconInfo ti) {  // null if loop detected
      AliasGraphNode p = this;
      boolean loopDetected = false;
      while (!loopDetected && (p != null)) {
        loopDetected = ti.key.equals(p.ti.key);
        p = p.parent;
      }
      AliasGraphNode n = null;
      if (!loopDetected) {
        n = new AliasGraphNode(ti);
	this.children.add(n);
	n.parent = this;
// /* DEBUG */ System.out.println("new alias graph node " + ti.toString());
      }
      return n;
    }

  }
}
