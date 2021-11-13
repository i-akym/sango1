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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class MTypeVar implements MType {
  int slot;
  int variance;
  boolean requiresConcrete;

  private MTypeVar() {}

  static MTypeVar create(int slot, int variance, boolean requiresConcrete) {
    MTypeVar t = new MTypeVar();
    t.slot = slot;
    t.variance = variance;
    t.requiresConcrete = requiresConcrete;
    return t;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<");
    switch (this.variance) {
    case Module.COVARIANT:
      buf.append("+");
      break;
    case Module.CONTRAVARIANT:
      buf.append("-");
      break;
    }
    buf.append("_");
    buf.append(this.slot);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    buf.append(">");
    return buf.toString();
  }

  public Element externalize(Document doc) {
    Element node = doc.createElement(Module.TAG_TYPE_VAR);
    node.setAttribute(Module.ATTR_SLOT, Integer.toString(this.slot));
    switch (this.variance) {
    case Module.COVARIANT:
      node.setAttribute(Module.ATTR_VARIANCE, Module.REPR_COVARIANT);
      break;
    case Module.CONTRAVARIANT:
      node.setAttribute(Module.ATTR_VARIANCE, Module.REPR_CONTRAVARIANT);
      break;
    }
    if (this.requiresConcrete) {
      node.setAttribute(Module.ATTR_REQUIRES_CONCRETE, Module.REPR_YES);
    }
    return node;
  }

  public static MTypeVar internalize(Node node) throws FormatException {
    if (!node.getNodeName().equals(Module.TAG_TYPE_VAR)) { return null; }

    NamedNodeMap attrs = node.getAttributes();
    Node aSlot = attrs.getNamedItem(Module.ATTR_SLOT);
    if (aSlot == null) {
      throw new FormatException("'slot' attribute not found.");
    }
    int slot = Module.parseInt(aSlot.getNodeValue()); 
    // if (slot < 0 || slot > builder.typeVarCount) {
      // throw new FormatException("Invalid slot: " + aSlot.getNodeValue());
    // }
    int variance = Module.INVARIANT;
    Node aVariance = attrs.getNamedItem(Module.ATTR_VARIANCE);
    if (aVariance != null) {
      String sVariance = aVariance.getNodeValue();
      if (sVariance.equals(Module.REPR_INVARIANT)) {
        ;
      } else if (sVariance.equals(Module.REPR_COVARIANT)) {
        variance = Module.COVARIANT;
      } else if (sVariance.equals(Module.REPR_CONTRAVARIANT)) {
        variance = Module.COVARIANT;
      } else {
        throw new FormatException("Invalid 'variance': " + sVariance);
      }
    }
    boolean requiresConcrete = false;
    Node aRequiresConcrete = attrs.getNamedItem(Module.ATTR_REQUIRES_CONCRETE);
    if (aRequiresConcrete != null) {
      String sRequiresConcrete = aRequiresConcrete.getNodeValue();
      if (sRequiresConcrete.equals(Module.REPR_YES)) {
        requiresConcrete = true;
      } else if (sRequiresConcrete.equals(Module.REPR_NO)) {
        ;
      } else {
        throw new FormatException("Invalid 'requires_concrete': " + sRequiresConcrete);
      }
    }
    return create(slot, variance, requiresConcrete);
  }

  public boolean isCompatible(Cstr defModName, MType type) {
    boolean b;
    if (!(type instanceof MTypeVar)) {
      b = false;
    } else {
      MTypeVar v = (MTypeVar)type;
      b = v.slot == this.slot;
    }
    return b;
  }
}
