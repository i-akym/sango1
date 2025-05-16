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

interface MType extends Module.Elem {

  // Element externalize(Document doc);  // inherited

  boolean isCompatible(Module.ModTab modTab, MType type, Module.ModTab defModTab);

  abstract static class Envelope {
    static Element externalize(Document doc, MType type) {
      Element e = doc.createElement(Module.TAG_TYPE);
      e.appendChild(type.externalize(doc));
      return e;
    }

    static MType internalize(Node node) throws FormatException {
      if (!node.getNodeName().equals(Module.TAG_TYPE)) { return null; }
      Node n = node.getFirstChild();
      MType type = null;
      while (n != null) {
        if (Module.isIgnorable(n)) {
          ;
        } else if (type == null) {
          type = internalizeDesc(n);
        } else {
          throw new FormatException("Unknown or extra element under " + Module.TAG_TYPE + " element: " + n.getNodeName());
        }
        n = n.getNextSibling();
      }
      if (type == null) {
        throw new FormatException("Type element not found.");
      }
      return type;
    }

    static MType internalizeDesc(Node node) throws FormatException {
      MType type = null;
      if ((type = MTypeRef.internalize(node)) != null) {
        ;
      } else if ((type = MTypeVar.internalize(node)) != null) {
        ;
      } else {
        throw new FormatException("Unknown element : " + node.getNodeName());
      }
      return type;
    }
  }

  public static class ParamDef {
    Module.Variance variance;
    MTypeVar var;

    static ParamDef create(Module.Variance variance, MTypeVar var) {
      ParamDef p = new ParamDef();
      p.variance = variance;
      p.var = var;
      return p;
    }

    public static ParamDef internalize(Node node) throws FormatException {
      if (!node.getNodeName().equals(Module.TAG_PARAM)) { return null; }
      NamedNodeMap attrs = node.getAttributes();
      Module.Variance variance = Module.NO_VARIANCE;
      Node aVariance = attrs.getNamedItem(Module.ATTR_VARIANCE);
      if (aVariance != null) {
        String sVariance = aVariance.getNodeValue();
        if (sVariance.equals(Module.REPR_INVARIANT)) {
          variance = Module.INVARIANT;
        } else if (sVariance.equals(Module.REPR_COVARIANT)) {
          variance = Module.COVARIANT;
        } else if (sVariance.equals(Module.REPR_CONTRAVARIANT)) {
          variance = Module.CONTRAVARIANT;
        } else {
          throw new FormatException("Invalid 'variance': " + sVariance);
        }
      }
      Node n = node.getFirstChild();
      MTypeVar v = null;
      while (n != null) {
        if (Module.isIgnorable(n)) {
          ;
        } else if (n.getNodeName().equals(Module.TAG_TYPE_VAR)) {
          v = MTypeVar.internalize(n);
        } else {
          throw new FormatException("Unknown element under '" + Module.TAG_CONSTR + "' element: " + n.getNodeName());
        }
        n = n.getNextSibling();
      }
      if (v == null) {
        throw new FormatException("'type_var' not found under data def param");
      }
      return ParamDef.create((variance == Module.NO_VARIANCE)? Module.INVARIANT: variance, v);

      // if (!node.getNodeName().equals(Module.TAG_TYPE_VAR)) { return null; }
      // MTypeVar v = MTypeVar.internalize(node);
      // NamedNodeMap attrs = node.getAttributes();
      // Module.Variance variance = Module.NO_VARIANCE;
      // Node aVariance = attrs.getNamedItem(Module.ATTR_VARIANCE);
      // if (aVariance != null) {
        // String sVariance = aVariance.getNodeValue();
        // if (sVariance.equals(Module.REPR_INVARIANT)) {
          // variance = Module.INVARIANT;
        // } else if (sVariance.equals(Module.REPR_COVARIANT)) {
          // variance = Module.COVARIANT;
        // } else if (sVariance.equals(Module.REPR_CONTRAVARIANT)) {
          // variance = Module.CONTRAVARIANT;
        // } else {
          // throw new FormatException("Invalid 'variance': " + sVariance);
        // }
      // }
      // return Param.create((variance == Module.NO_VARIANCE)? Module.INVARIANT: variance, v);
    }

    public Element externalize(Document doc) {
      Element node = doc.createElement(Module.TAG_PARAM);
      if (this.variance == Module.INVARIANT) {
        ;  // nothing added
      } else if (this.variance == Module.COVARIANT) {
        node.setAttribute(Module.ATTR_VARIANCE, Module.REPR_COVARIANT);
      } else if (this.variance == Module.CONTRAVARIANT) {
        node.setAttribute(Module.ATTR_VARIANCE, Module.REPR_CONTRAVARIANT);
      }
      node.appendChild(this.var.externalize(doc));
      return node;
    }
  }
}
