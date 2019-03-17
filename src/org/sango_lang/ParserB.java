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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

class ParserB extends Parser {
  File srcFile;
  Elem rootElem;

  ParserB(Compiler theCompiler, Compiler.CompileEntry ce) throws CompileException, IOException {
    super(theCompiler, ce);
    this.srcFile = ce.srcFile;
  }

  void parse1() throws CompileException, IOException {
    StringBuffer emsg;
    try {
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setIgnoringElementContentWhitespace(true);
      factory.setIgnoringComments(true);
      DocumentBuilder builder = factory.newDocumentBuilder();
      Document doc = builder.parse(this.srcFile);
      this.rootElem = this.rootElem(doc);
    } catch (SAXException ex) {
      emsg = new StringBuffer();
      emsg.append("Source file format error in ");
      emsg.append(modName.repr());
      emsg.append(". - ");
      // emsg.append("Source file format error at line ");
      // emsg.append(Integer.toString(ex.getLineNumber()));
      // emsg.append(" coloumn ");
      // emsg.append(Integer.toString(ex.getColomnNumber()));
      // emsg.append(". - ");
      // emsg.append(modName.repr());
      throw new CompileException(emsg.toString());
    } catch (ParserConfigurationException ex) {
      System.err.println(ex);
      System.exit(1);
    }
    this.mod = PModule.acceptX(this.rootElem, this.modName);
    this.mod.theCompiler = this.theCompiler;
  }

  Elem rootElem(Document doc) {
    Node n = skipIgnorableNodes(doc.getFirstChild());
    Elem root = new Elem();
    root.domNode = n;
    root.loc = rootElemLoc((n != null)? n.getNodeName(): "");
    return root;
  }

  class Elem {
    private Node domNode;
    private ElemLoc loc;

    Parser.SrcInfo getSrcInfo() {
      return new Parser.SrcInfo(ParserB.this.modName, this.loc.toString());
    }

    String getName() { return this.domNode.getNodeName(); }

    String getAttrValue(String attrName) throws CompileException {
      Node a = this.domNode.getAttributes().getNamedItem(attrName);
      if (a == null) {
        return null;
      }
      return a.getNodeValue();
    }

    Cstr getAttrValueAsCstrData(String attrName) throws CompileException {
      Node a = this.domNode.getAttributes().getNamedItem(attrName);
      if (a == null) {
        return null;
      }
      return new Cstr(a.getNodeValue());
    }

    String getAttrValueAsId(String attrName) throws CompileException {
      StringBuffer emsg;
      Node a = this.domNode.getAttributes().getNamedItem(attrName);
      if (a == null) {
        return null;
      }
      String id = a.getNodeValue();
      if (!Parser.isNormalId(id)) {
        emsg = new StringBuffer();
        emsg.append("Invalid identifier at ");
        emsg.append(this.getSrcInfo().toString());
        emsg.append(". - ");
        emsg.append(id);
        throw new CompileException(emsg.toString());
      }
      return id;
    }

    String getAttrValueAsExtendedId(String attrName) throws CompileException {
      StringBuffer emsg;
      Node a = this.domNode.getAttributes().getNamedItem(attrName);
      if (a == null) {
        return null;
      }
      String id = a.getNodeValue();
      if (!Parser.isExtendedId(id)) {
        emsg = new StringBuffer();
        emsg.append("Invalid identifier at ");
        emsg.append(this.getSrcInfo().toString());
        emsg.append(". - ");
        emsg.append(id);
        throw new CompileException(emsg.toString());
      }
      return id;
    }

    boolean getAttrValueAsYesNoSwitch(String attrName, boolean defaultValue) throws CompileException {
      StringBuffer emsg;
      Node a = this.domNode.getAttributes().getNamedItem(attrName);
      if (a == null) {
        return defaultValue;
      }
      String sw = a.getNodeValue();
      boolean b = false;
      if (sw.equals("yes")) {
        b = true;
      } else if (sw.equals("no")) {
        b = false;
      } else {
        emsg = new StringBuffer();
        emsg.append("Invalid switch value at ");
        emsg.append(this.getSrcInfo().toString());
        emsg.append(". - ");
        emsg.append(sw);
        throw new CompileException(emsg.toString());
      }
      return b;
    }

    Elem getNextSibling() {
      Node n = skipIgnorableNodes(this.domNode.getNextSibling());
      Elem e = null;
      if (n != null) {
        e = new Elem();
        e.domNode = n;
        e.loc = this.loc.nextSiblingLoc(n.getNodeName());
      }
      return e;
    }

    Elem getFirstChild() {
      Node n = skipIgnorableNodes(this.domNode.getFirstChild());
      Elem e = null;
      if (n != null) {
        e = new Elem();
        e.domNode = n;
        e.loc = this.loc.firstChildLoc(n.getNodeName());
      }
      return e;
    }
  }

  ElemLoc rootElemLoc(String name) {
    ElemLoc loc = new ElemLoc();
    loc.parent = "";
    loc.pos = 1;
    loc.name = name;
    return loc;
  }

  class ElemLoc {
    String parent;
    int pos;
    String name;

    public String toString() {
      return this.parent + "/(" + Integer.toString(this.pos) + ")" + this.name;
    }

    ElemLoc nextSiblingLoc(String name) {
      ElemLoc loc = new ElemLoc();
      loc.parent = this.parent;
      loc.pos = this.pos + 1;
      loc.name = name;
      return loc;
    }

    ElemLoc firstChildLoc(String name) {
      ElemLoc loc = new ElemLoc();
      loc.parent = this.toString();
      loc.pos = 1;
      loc.name = name;
      return loc;
    }
  }

  static Node skipIgnorableNodes(Node node) {
    Node n = node;
    while ((n != null) && isIgnorable(n)) {
      n = n.getNextSibling();
    }
    return n;
  }

  static boolean isIgnorable(Node node) {
    int type = node.getNodeType();
    return
      (type == Node.COMMENT_NODE) 
      || ((type == Node.TEXT_NODE) && (node.getNodeValue().trim().length() == 0));
  }
}
