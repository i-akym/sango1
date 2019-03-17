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

public class MClosureImpl implements Module.Elem {
  String name;
  int paramCount;
  MInstruction[] codeBlock;  // null if native impl
  MSrcInfo[] srcInfoTab;  // null if native impl
  int varCount;

  private MClosureImpl() {}

  static MClosureImpl create(String name, int paramCount) {
    MClosureImpl ci = new MClosureImpl();
    ci.name = name;
    ci.paramCount = paramCount;
    return ci;
  }

  void setVarCount(int varCount) {
    this.varCount = varCount;
  }

  void setCodeBlock(MInstruction[] codeBlock, MSrcInfo[] srcInfoTab) {
    this.codeBlock = codeBlock;
    this.srcInfoTab = srcInfoTab;
  }

  public Element externalize(Document doc) {
    Element closureImplNode = doc.createElement(Module.TAG_CLOSURE_IMPL);
    closureImplNode.setAttribute(Module.ATTR_NAME, this.name);
    closureImplNode.setAttribute(Module.ATTR_PARAM_COUNT, Integer.toString(this.paramCount));
    if (this.codeBlock != null) {
      closureImplNode.appendChild(this.externalizeVMCode(doc));
    }
    return closureImplNode;
  }

  Element externalizeVMCode(Document doc) {
    Element vmcodeNode = doc.createElement(Module.TAG_VMCODE);
    vmcodeNode.setAttribute(Module.ATTR_VAR_COUNT, Integer.toString(this.varCount));
    vmcodeNode.appendChild(this.externalizeInstSeq(doc));
    vmcodeNode.appendChild(this.externalizeSrcInfoTab(doc));
    return vmcodeNode;
  }

  Element externalizeInstSeq(Document doc) {
    Element instSeqNode = doc.createElement(Module.TAG_INST_SEQ);
    for (int i = 0; i < this.codeBlock.length; i++) {
      instSeqNode.appendChild(this.externalizeInstruction(doc, this.codeBlock[i]));
    }
    return instSeqNode;
  }

  Element externalizeInstruction(Document doc, MInstruction inst) {
    return inst.externalize(doc);
  }

  Element externalizeSrcInfoTab(Document doc) {
    Element srcInfoTabNode = doc.createElement(Module.TAG_SRCINFO_TAB);
    for (int i = 0; i < this.srcInfoTab.length; i++) {
      srcInfoTabNode.appendChild(this.externalizeSrcInfo(doc, this.srcInfoTab[i]));
    }
    return srcInfoTabNode;
  }

  Element externalizeSrcInfo(Document doc, MSrcInfo srcInfo) {
    return srcInfo.externalize(doc);
  }
}
