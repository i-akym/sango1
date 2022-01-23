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

import java.util.List;

class PNoRetSkel implements PTypeSkel {
  Parser.SrcInfo srcInfo;

  private PNoRetSkel() {}

  static PNoRetSkel create(Parser.SrcInfo srcInfo) {
    PNoRetSkel n = new PNoRetSkel();
    n.srcInfo = srcInfo;
    return n;
  }

  public boolean equals(Object o) {
    return o == this || o instanceof PNoRetSkel;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("NORET");
    if (this.srcInfo != null) {
      buf.append("[src=");
      buf.append(this.srcInfo);
      buf.append("]");
    }
    return buf.toString();
  }

  public Parser.SrcInfo getSrcInfo() { return this.srcInfo; }

  public boolean isLiteralNaked() { return false; }

  public boolean isConcrete() { return false; }

  public boolean isConcrete(PTypeSkelBindings bindings) { return false; }

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    return this;
  }

  public PNoRetSkel resolveBindings(PTypeSkelBindings bindings) {
    return this;
  }

  public void checkVariance(int width) throws CompileException {}

  public PTypeSkelBindings accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#accept "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkel t = type.resolveBindings(trialBindings);
    PTypeSkelBindings b;
    if (t instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#accept 1 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#accept 2 "); System.out.print(width); System.out.print(" "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = null;
    }
    return b;
  }

  public boolean includesVar(PTVarSlot varSlot, PTypeSkelBindings bindings) {
    return false;
  }

  public PTVarSlot getVarSlot() { return null; }

  public PTypeSkel join(PTypeSkel type, List<PTVarSlot> givenTVarList) {
    return this.join2(type, givenTVarList);
  }

  public PTypeSkel join2(PTypeSkel type, List<PTVarSlot> givenTVarList) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type);
}
    return type;
  }

  public MType toMType(PModule mod, List<PTVarSlot> slotList) {
    MTypeRef.Builder b = MTypeRef.Builder.newInstance();
    b.setModName(mod.isLang()? null: Module.MOD_LANG);
    b.setTcon(Module.TCON_NORET);
    return b.create();
  }

  public List<PTVarSlot> extractVars(List<PTVarSlot> alreadyExtracted) {
    return null;
  }

  public void collectTconInfo(List<PDefDict.TconInfo> list) {}

  public PTypeSkel unalias(PTypeSkelBindings bindings) {
    return this;
  }

  public GFlow.Node setupFlow(GFlow flow, PScope scope, PTypeSkelBindings bindings) {
    GFlow.SeqNode node = flow.createNodeForDataConstrBody(
      this.srcInfo, scope.theMod.modNameToModRefIndex(Module.MOD_LANG), "type$", "type", 0);
    GFlow.Node n = flow.createNodeForEmptyListBody(this.srcInfo);
    node.addChild(n);
    node.addChild(flow.createNodeForCstr(this.srcInfo, Module.MOD_LANG));
    node.addChild(flow.createNodeForCstr(this.srcInfo, Module.TCON_NORET));
    return node;
  }

  public PTypeSkel.Repr repr() {
    PTypeSkel.Repr r = PTypeSkel.Repr.create();
    r.add(Module.MOD_LANG.repr() + "." + Module.TCON_NORET);
    return r;
  }
}
