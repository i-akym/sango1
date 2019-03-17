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

import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

class MInstruction implements Module.Elem {
  static final String OP_NOP = "NO";
  static final String OP_RETURN = "RE";
  static final String OP_LOAD_INT = "LDI";
  static final String OP_LOAD_NIL = "LDN";
  static final String OP_LOAD_CONST = "LDC";
  static final String OP_LOAD_LOCAL = "LDL";
  static final String OP_LOAD_AND_CLEAR_LOCAL = "LCL";
  static final String OP_LOAD_ENV = "LDE";
  static final String OP_LOAD_FIELD = "LDF";
  static final String OP_LOAD_ARRAY_ELEM = "LDAE";
  static final String OP_LOAD_ARRAY_LEN = "LDAL";
  static final String OP_LOAD_HEAD = "LDH";
  static final String OP_LOAD_TAIL = "LDT";
  static final String OP_LOAD_MSLOT = "LDM";
  static final String OP_LOAD_SP = "LDZ";
  static final String OP_STORE_LOCAL = "STL";
  static final String OP_CLEAR_LOCAL = "CLL";
  static final String OP_STORE_ARRAY_ELEM = "STAE";
  static final String OP_NEW_CLOSURE = "NWC";
  static final String OP_NEW_DATA = "NWD";
  static final String OP_NEW_TUPLE = "NWT";
  static final String OP_NEW_LIST = "NWL";
  static final String OP_NEW_ARRAY = "NWA";
  static final String OP_JUMP = "JU";
  static final String OP_INVOKE = "IV";
  static final String OP_INVOKE_GOTO = "IVG";
  static final String OP_POP = "PO";
  static final String OP_DUP = "DU";
  static final String OP_REWIND = "RW";
  static final String OP_BRANCH_TRUE = "BRBT";  // temporary?
  static final String OP_BRANCH_FALSE = "BRBF";  // temporary?
  static final String OP_BRANCH_OBJ_EQ = "BROEQ";
  static final String OP_BRANCH_OBJ_NE = "BRONE";
  static final String OP_BRANCH_DCON_EQ = "BRDEQ";
  static final String OP_BRANCH_DCON_NE = "BRDNE";
  static final String OP_BRANCH_LIST_CELL = "BRLC";
  static final String OP_BRANCH_LIST_NIL = "BRLN";
  static final String OP_BRANCH_TSIG_COMPAT = "BRTSCO";
  static final String OP_BRANCH_TSIG_INCOMPAT = "BRTSIC";
  static final String OP_EXCEPTION = "EX";

  // EXCEPTION's parameter
  // non-negative value -> runtime_failure$
  // negative value -> sys_err$
  static final int EXCEPTION_INCOMPAT = 0;  // incompat$
  static final int EXCEPTION_NO_CASE = 1;  // no_case$
  static final int EXCEPTION_NO_ELEM = 2;  // no_elem$
  static final int EXCEPTION_TRAP = -1;
  static final int EXCEPTION_INVALID_OPERATION = -2;
  static final int EXCEPTION_RUNTIME_FAILURE = -3;

  static final int INT_OBJ_INT = 0;
  static final int INT_OBJ_BYTE = 1;
  static final int INT_OBJ_CHAR = 2;

  static Set<String> opSet;

  static {
    opSet = new HashSet<String>();
    opSet.add(OP_NOP);
    opSet.add(OP_RETURN);
    opSet.add(OP_LOAD_INT);
    opSet.add(OP_LOAD_NIL);
    opSet.add(OP_LOAD_CONST);
    opSet.add(OP_LOAD_LOCAL);
    opSet.add(OP_LOAD_AND_CLEAR_LOCAL);
    opSet.add(OP_LOAD_ENV);
    opSet.add(OP_LOAD_FIELD);
    opSet.add(OP_LOAD_ARRAY_ELEM);
    opSet.add(OP_LOAD_ARRAY_LEN);
    opSet.add(OP_LOAD_HEAD);
    opSet.add(OP_LOAD_TAIL);
    opSet.add(OP_LOAD_MSLOT);
    opSet.add(OP_LOAD_SP);
    opSet.add(OP_STORE_LOCAL);
    opSet.add(OP_CLEAR_LOCAL);
    opSet.add(OP_STORE_ARRAY_ELEM);
    opSet.add(OP_REWIND);
    opSet.add(OP_NEW_CLOSURE);
    opSet.add(OP_NEW_DATA);
    opSet.add(OP_NEW_TUPLE);
    opSet.add(OP_NEW_LIST);
    opSet.add(OP_NEW_ARRAY);
    opSet.add(OP_JUMP);
    opSet.add(OP_INVOKE);
    opSet.add(OP_INVOKE_GOTO);
    opSet.add(OP_POP);
    opSet.add(OP_DUP);
    opSet.add(OP_BRANCH_TRUE);
    opSet.add(OP_BRANCH_FALSE);
    opSet.add(OP_BRANCH_OBJ_EQ);
    opSet.add(OP_BRANCH_OBJ_NE);
    opSet.add(OP_BRANCH_DCON_EQ);
    opSet.add(OP_BRANCH_DCON_NE);
    opSet.add(OP_BRANCH_LIST_CELL);
    opSet.add(OP_BRANCH_LIST_NIL);
    opSet.add(OP_BRANCH_TSIG_COMPAT);
    opSet.add(OP_BRANCH_TSIG_INCOMPAT);
    opSet.add(OP_EXCEPTION);
  }

  String operation;
  int[] params;

  MInstruction() {}

  static MInstruction create(String operation) {
    return create(operation, new int[0]);
  }

  static MInstruction create(String operation, int param0) {
    return create(operation, new int[] { param0 });
  }

  static MInstruction create(String operation, int param0, int param1) {
    return create(operation, new int[] { param0, param1 });
  }

  static MInstruction create(String operation, int[] params) {
    MInstruction i = new MInstruction();
    i.operation = operation;
    i.params = params;
    return i;
  }

  static MInstruction parse(String op, String p0, String p1) throws FormatException {
    if (!opSet.contains(op)) {
      throw new FormatException("Unknown opration: " + op);
    }
    MInstruction inst;
    if (p0 == null) {
      inst = create(op);
    } else if (p1 == null) {
      inst = create(op, Module.parseInt(p0));
    } else {
      inst = create(op, Module.parseInt(p0), Module.parseInt(p1));
    }
    return inst;
  }

  public Element externalize(Document doc) {
    Element instNode = doc.createElement(Module.TAG_I);
    instNode.setAttribute(Module.ATTR_O, this.operation);
    switch (this.params.length) {
      case 2:
        instNode.setAttribute(Module.ATTR_Q, Integer.toString(this.params[1]));
      case 1:
        instNode.setAttribute(Module.ATTR_P, Integer.toString(this.params[0]));
      case 0:
      break;
    }
    return instNode;
  }
}
