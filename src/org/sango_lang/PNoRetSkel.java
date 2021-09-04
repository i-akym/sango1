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

  public PTypeSkel instanciate(PTypeSkel.InstanciationBindings iBindings) {
    return this;
  }

  public PNoRetSkel resolveBindings(PTypeSkelBindings bindings) {
    return this;
  }

  public PTypeSkelBindings applyTo(PTypeSkel type, PTypeSkelBindings trialBindings) {
    return this.applyTo2(type.resolveBindings(trialBindings), trialBindings);
  }

  public PTypeSkelBindings applyTo2(PTypeSkel type, PTypeSkelBindings trialBindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
    PTypeSkelBindings b;
    if (type instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply A "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
    } else if (type instanceof PTypeRefSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      b = trialBindings;
      // PNoRetSkel tr = (PNoRetSkel)type;
      // if (type instanceof PNoRetSkel) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 1-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        // b = trialBindings;
        // for (int i = 0; b != null && i < this.params.length; i++) {
          // b = this.params[i].applyTo(tr.params[i], b);
        // }
      // } else {
    // /* DEBUG */ System.out.print("PNoRetSkel#apply 1-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
        // b = null;
      // }
    } else {  // 'type' is guaranteed to be unbound by applyTo
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
      PTypeVarSkel tv = (PTypeVarSkel)type;
      if (trialBindings.isGivenTVar(tv.varSlot)) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 2-1 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        b = trialBindings;
      } else {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#apply 2-2 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(trialBindings);
}
        trialBindings.bind(tv.varSlot, this);
        b = trialBindings;
      }
    }
    return b;
  }

  public boolean includesVar(PTVarSlot varSlot, PTypeSkelBindings bindings) {
    return false;
  }

  public PTVarSlot getVarSlot() { return null; }

  public PTypeSkel join(PTypeSkel type, PTypeSkelBindings bindings) {
    return this.join2(type.resolveBindings(bindings), bindings);
  }

  public PTypeSkel join2(PTypeSkel type, PTypeSkelBindings bindings) {
if (PTypeGraph.DEBUG > 1) {
    /* DEBUG */ System.out.print("PNoRetSkel#join 0 "); System.out.print(this); System.out.print(" "); System.out.print(type); System.out.print(" "); System.out.println(bindings);
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

  public String repr() {
    return Module.MOD_LANG.repr() + "." + Module.TCON_NORET;
  }
}
