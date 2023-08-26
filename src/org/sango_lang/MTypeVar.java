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
  boolean requiresConcrete;
  MType constraint;  // maybe null
  MFeature.List features;  // maybe null

  private MTypeVar() {}

  static MTypeVar create(int slot, boolean requiresConcrete, MType constraint, MFeature.List features) {
    MTypeVar t = new MTypeVar();
    t.slot = slot;
    t.requiresConcrete = requiresConcrete;
    t.constraint = constraint;
    t.features = features;
    return t;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("<");
    if (this.constraint != null) {
      buf.append(this.constraint.toString());  // HERE: do not embrace by < >
      buf.append(" = ");
    }
    buf.append("_");
    buf.append(this.slot);
    if (this.requiresConcrete) {
      buf.append("!");
    }
    if (this.features != null) {
      buf.append(this.features.toString());
    }
    buf.append(">");
    return buf.toString();
  }

  public Element externalize(Document doc) {
// /* DEBUG */ System.out.println(this);
    Element node = doc.createElement(Module.TAG_TYPE_VAR);
    node.setAttribute(Module.ATTR_SLOT, Integer.toString(this.slot));
    if (this.requiresConcrete) {
      node.setAttribute(Module.ATTR_REQUIRES_CONCRETE, Module.REPR_YES);
    }
    if (this.constraint != null) {
      Element c = doc.createElement(Module.TAG_CONSTRAINT);
      c.appendChild(this.constraint.externalize(doc));
      node.appendChild(c);
    }
    if (this.features != null && this.features.features.length > 0) {
      node.appendChild(this.features.externalize(doc));
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

    Node n = node.getFirstChild();
    MFeature.List features = null;
    MType constraint = null;
    int state = 0;
    while (n != null) {
      if (Module.isIgnorable(n)) {
        ;
      } else if (state < 1 && (constraint = internalizeConstraint(n)) != null) {
        state = 1;
      } else if (state < 2 && (features = MFeature.List.internalize(n)) != null) {
        state = 2;
      } else {
        throw new FormatException("Unknown or extra element : " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return create(slot, requiresConcrete, constraint, features);
  }

  static MType internalizeConstraint(Node node) throws FormatException {
    if (!node.getNodeName().equals(Module.TAG_CONSTRAINT)) { return null; }

    Node n = node.getFirstChild();
    MType constraint = null;
    int state = 0;
    while (n != null) {
      if (Module.isIgnorable(n)) {
        ;
      } else if (state == 0 && (constraint = MType.Envelope.internalizeDesc(n)) != null) {
        state = 1;
      } else {
        throw new FormatException("Unknown or extra element : " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return constraint;
  }

  public boolean isCompatible(Module.ModTab modTab, MType type, Module.ModTab defModTab) {
    boolean b;
    if (!(type instanceof MTypeVar)) {
      b = false;
    } else {
      MTypeVar tv = (MTypeVar)type;
      b = tv.slot == this.slot;
    }
    return b;
  }
}
