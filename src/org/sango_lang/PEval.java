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
import java.util.ArrayList;
import java.util.List;

interface PEval extends PExprObj {

  PEval resolve() throws CompileException;

  static final int ACCEPT_NOTHING = 0;
  static final int ACCEPT_BYTE = 1 << 0;
  static final int ACCEPT_INT = 1 << 1;
  static final int ACCEPT_REAL = 1 << 2;
  static final int ACCEPT_CHAR = 1 << 3;
  static final int ACCEPT_LIST = 1 << 4;
  static final int ACCEPT_TUPLE = 1 << 5;
  static final int ACCEPT_STRING = 1 << 6;
  static final int ACCEPT_ID = 1 << 7;
  static final int ACCEPT_FUN_REF = 1 << 8;
  static final int ACCEPT_CLOSURE = 1 << 9;
  static final int ACCEPT_IF_EVAL = 1 << 10;
  static final int ACCEPT_CASE_BLOCK = 1 << 11;
  static final int ACCEPT_DYNAMIC_INV = 1 << 12;
  static final int ACCEPT_SELF_INV = 1 << 13;
  static final int ACCEPT_DATA_CONSTR_USING = 1 << 14;
  static final int ACCEPT_ENCLOSED = 1 << 15;  // includes PEval
  static final int ACCEPT_PIPE = 1 << 16;
  static final int ACCEPT_VAR_REF = 1 << 17;  // occurs when resolved
  static final int ACCEPT_PRIMITIVE = ACCEPT_BYTE + ACCEPT_INT + ACCEPT_REAL + ACCEPT_CHAR;
  static final int ACCEPT_COLLECTION = ACCEPT_LIST + ACCEPT_TUPLE + ACCEPT_STRING;
  static final int ACCEPT_DATA_OBJ = ACCEPT_PRIMITIVE + ACCEPT_COLLECTION;
  static final int ACCEPT_FUN_OBJ = ACCEPT_FUN_REF + ACCEPT_CLOSURE;

  static class Builder {
    private static final int TAG_NOT_DETERMINED = 0;
    private static final int TAG_DATA_CONSTR_USING = 1;
    private static final int TAG_CASE_EXPR = 2;
    private static final int TAG_DYNAMIC_INV = 3;
    private static final int TAG_SELF_INV = 4;

    private static int[] acceptablesTab = {
      // state 0:
      ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_EVAL
      + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_ENCLOSED
      + ACCEPT_VAR_REF,
      // state 1: x
      ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_EVAL
      + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_ENCLOSED + ACCEPT_CASE_BLOCK + ACCEPT_DATA_CONSTR_USING + ACCEPT_PIPE
      + ACCEPT_VAR_REF,
      // state 2: x x..
      ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_EVAL
      + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_ENCLOSED + ACCEPT_DATA_CONSTR_USING + ACCEPT_PIPE
      + ACCEPT_VAR_REF,
      // state 3: x x.. ::
      ACCEPT_ID + ACCEPT_IF_EVAL + ACCEPT_ENCLOSED
      + ACCEPT_VAR_REF,
      // state 4: x x.. :: x
      ACCEPT_ID
      + ACCEPT_VAR_REF,
      // state 5: x x.. &
      ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_EVAL + ACCEPT_ENCLOSED
      + ACCEPT_VAR_REF,
      // state 6: evaluation ended -- ... :: x x  | ... & x  |  ... &&  |  x case { }  |  ... >> x
      ACCEPT_PIPE,
      // state 7: ... >>
      ACCEPT_ID + ACCEPT_CASE_BLOCK + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV,
    };

    int tag;
    int state;
    Parser.SrcInfo srcInfo;
    PScope scope;
    Parser.SrcInfo lastSrcInfo;
    List<PEvalItem.ObjItem> itemList;
    int followingSpace;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.srcInfo = srcInfo;
      this.scope = outerScope;
      this.itemList = new ArrayList<PEvalItem.ObjItem>();
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    int getAcceptables() { return acceptablesTab[this.state]; }

    int getFollowingSpace() { return this.followingSpace; }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.srcInfo = si;
    // }

    void addItem(PEvalItem item) throws CompileException {
      if (item.isObjItem()) {
        PExprObj o = item.getObj();
        if (o instanceof PByte) {
          this.addDataObj(ACCEPT_BYTE, (PEvalItem.ObjItem)item);
        } else if (o instanceof PInt) {
          this.addDataObj(ACCEPT_INT, (PEvalItem.ObjItem)item);
        } else if (o instanceof PReal) {
          this.addDataObj(ACCEPT_REAL, (PEvalItem.ObjItem)item);
        } else if (o instanceof PChar) {
          this.addDataObj(ACCEPT_CHAR, (PEvalItem.ObjItem)item);
        } else if (o instanceof PList) {
          this.addDataObj(ACCEPT_LIST, (PEvalItem.ObjItem)item);
        } else if (o instanceof PTuple) {
          this.addDataObj(ACCEPT_TUPLE, (PEvalItem.ObjItem)item);
        } else if (o instanceof PString) {
          this.addDataObj(ACCEPT_STRING, (PEvalItem.ObjItem)item);
        } else if (o instanceof PEid) {
          this.addId(ACCEPT_ID, (PEvalItem.ObjItem)item);
        } else if (o instanceof PFunRef) {
          this.addFunObj(ACCEPT_FUN_REF, (PEvalItem.ObjItem)item);
        } else if (o instanceof PClosure) {
          this.addFunObj(ACCEPT_CLOSURE, (PEvalItem.ObjItem)item);
        } else if (o instanceof PIfEval) {
          this.addIfEval(ACCEPT_IF_EVAL, (PEvalItem.ObjItem)item);
        } else if (o instanceof PCaseBlock) {
          this.addCaseBlock(ACCEPT_CASE_BLOCK, (PEvalItem.ObjItem)item);
        } else if (o instanceof PExpr) {
          this.addEnclosed(ACCEPT_ENCLOSED, (PEvalItem.ObjItem)item);
        } else if (o instanceof PExprVarRef) {
          this.addVarRef(ACCEPT_VAR_REF, (PEvalItem.ObjItem)item);
        } else if (o instanceof PEval) {  // should be at last
          this.addEnclosed(ACCEPT_ENCLOSED, PEvalItem.create(PExpr.create(o.getSrcInfo(), this.scope, (PEval)o, null)));
        } else {
          throw new IllegalArgumentException("Invalid item. " + item.toString());
        }
      } else {
        PProgElem s = item.getSym();
        if (s instanceof PDynamicInv) {
          this.dynamicInv();
        } else if (s instanceof PSelfInv) {
          this.selfInv();
        } else if (s instanceof PDataConstrUsing) {
          this.dataConstrUsing();
        } else if (s instanceof PPipe) {
          this.pipe();
        } else {
          throw new IllegalArgumentException("Invalid item. " + item.toString());
        }
      }
      this.lastSrcInfo = item.srcInfo;
    }

    private void addDataObj(int accept, PEvalItem.ObjItem item) {
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addId(int accept, PEvalItem.ObjItem item) {
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      ((PEid)item.obj).cutOffDconPtn();
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 3:
        ((PEid)item.obj).cutOffDcon();
        this.state = 4;
        break;
      case 4:
        ((PEid)item.obj).setCat(PDefDict.EID_CAT_DCON_EVAL);
        this.state = 6;
        break;
      case 5:
        ((PEid)item.obj).cutOffDcon();
        this.state = 6;
        break;
      case 7:
        ((PEid)item.obj).setFun();
        this.state = 6;
        break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addFunObj(int accept, PEvalItem.ObjItem item) throws CompileException {
      StringBuffer emsg;
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      if (this.state != 5 && item.obj instanceof PClosure) {
        PClosure closure = (PClosure)item.obj;
        for (int i = 0; i < closure.params.length; i++) {
          PExprVarDef param = closure.params[i];
          if (param.type == null) {
            emsg = new StringBuffer();
            emsg.append("Parameter type missing at ");
            emsg.append(param.srcInfo);
            emsg.append(".");
          }
        }
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 5: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addIfEval(int accept, PEvalItem.ObjItem item) {
      StringBuffer emsg;
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 3: this.state = 4; break;
      case 5: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addCaseBlock(int accept, PEvalItem.ObjItem item) throws CompileException {
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      this.tag = TAG_CASE_EXPR;
      switch (this.state) {
      case 1: this.state = 6; break;
      case 7: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void dynamicInv() throws CompileException {
      if ((acceptablesTab[this.state] & ACCEPT_DYNAMIC_INV) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.tag = TAG_DYNAMIC_INV;
      switch (this.state) {
      case 0: this.state = 5; break;
      case 1: this.state = 5; break;
      case 2: this.state = 5; break;
      case 7: this.state = 5; break;
      }
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    private void selfInv() throws CompileException {
      if ((acceptablesTab[this.state] & ACCEPT_SELF_INV) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.tag = TAG_SELF_INV;
      switch (this.state) {
      case 0: this.state = 6; break;
      case 1: this.state = 6; break;
      case 2: this.state = 6; break;
      case 7: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    private void dataConstrUsing() {
      if ((acceptablesTab[this.state] & ACCEPT_DATA_CONSTR_USING) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.tag = TAG_DATA_CONSTR_USING;
      switch (this.state) {
      case 1: this.state = 3; break;
      case 2: this.state = 3; break;
      }
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    private void addEnclosed(int accept, PEvalItem.ObjItem item) {
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 3: this.state = 4; break;
      case 5: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void pipe() throws CompileException {
      if ((acceptablesTab[this.state] & ACCEPT_PIPE) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      PEval e = this.create();
      this.itemList.clear();
      this.tag = TAG_NOT_DETERMINED;
      this.itemList.add(PEvalItem.create(e));
      switch (this.state) {
      case 1: this.state = 7; break;
      case 2: this.state = 7; break;
      case 6: this.state = 7; break;
      }
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    private void addVarRef(int accept, PEvalItem.ObjItem item) {
      if ((acceptablesTab[this.state] & accept) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 3: this.state = 4; break;
      case 4: this.state = 6; break;
      case 5: this.state = 6; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    PEval create() throws CompileException {
      StringBuffer emsg;
      switch (this.state) {
      case 0:
        return null;
      case 3:
      case 4:
      case 5:
      case 7:
        emsg = new StringBuffer();
        emsg.append("Incomplete evaluation after ");
        emsg.append(this.lastSrcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return this.createDispatch();
    }

    private PEval createDispatch() throws CompileException {
      StringBuffer emsg;
      PEval e;
// /* DEBUG */ System.out.print(this.tag);
      if (this.tag == TAG_DATA_CONSTR_USING) {
        e = this.createDataConstrUsingEval();
      } else if (this.tag == TAG_CASE_EXPR) {
        e = this.createCaseEval();
      } else if (this.tag == TAG_DYNAMIC_INV) {
        e = this.createDynInvEval();
      } else if (this.tag == TAG_SELF_INV) {
        e = this.createSelfInvEval();
      } else if (this.itemList.size() == 1) {
        PExprObj elem = this.itemList.get(0).obj;
        if (elem instanceof PEval) {
          e = (PEval)elem;
        } else if (elem instanceof PEid) {
          e = this.createDispatch2();
        } else if (elem instanceof PByte
          || elem instanceof PInt
          || elem instanceof PReal
          || elem instanceof PChar
          || elem instanceof PList
          || elem instanceof PTuple
          || elem instanceof PString
          || elem instanceof PFunRef
          || elem instanceof PClosure
          ) {
          e = this.createTermEval();
        } else {
          e = this.createDispatch2();
        }
      } else {
        e = this.createDispatch2();
      }
// /* DEBUG */ System.out.print("CREATED EVAL "); System.out.println(e);
      return e;
    }

    private PEval createDispatch2() throws CompileException {
      StringBuffer emsg;
      PEvalItem.ObjItem anchor = this.itemList.remove(this.itemList.size() - 1);
      if (anchor.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (!(anchor.obj instanceof PEid)) {
// /* DEBUG */ System.out.print("anchor "); System.out.println(anchor);
        emsg = new StringBuffer();
        emsg.append("Either function or data constructor missing at ");
        emsg.append(anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      int namedAttrCount = 0;
      for (int i = 0; i < this.itemList.size(); i++) {
        if (this.itemList.get(i).name != null) {
          namedAttrCount++;
        } else if (namedAttrCount > 0) {
          emsg = new StringBuffer();
          emsg.append("Attribute name missing at ");
          emsg.append(this.itemList.get(i).srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else {
          ;
        }
      }
      PEvalItem.ObjItem posdParams[] = new PEvalItem.ObjItem[this.itemList.size() - namedAttrCount];
      PEvalItem.ObjItem namedAttrs[] = new PEvalItem.ObjItem[namedAttrCount];
      for (int i = 0; i < posdParams.length; i++) {
        posdParams[i] = this.itemList.get(i);
        posdParams[i].fixAsParam();
      }
      for (int i = posdParams.length, j = 0; j < namedAttrCount; i++, j++) {
        namedAttrs[j] = this.itemList.get(i);
        namedAttrs[j].fixAsParam();
      }
      PEval e;
      if (namedAttrCount > 0) {
        PEid dcon = (PEid)anchor.obj;
	dcon.setCat(PDefDict.EID_CAT_DCON_EVAL);
        e = PDataConstrEval.create(this.srcInfo, this.scope, dcon, posdParams, namedAttrs, null);
      } else {
        PEid id = (PEid)anchor.obj;
        id.cutOffDconPtn();
        e = PUndetEval.create(this.srcInfo, this.scope, id, posdParams);
      }
      return e;
    }

    private PEval createDataConstrUsingEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem.ObjItem dcon = this.itemList.remove(this.itemList.size() - 1);
      PEvalItem.ObjItem using = this.itemList.remove(this.itemList.size() - 1);
      if (using.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribuite name \"");
        emsg.append(using.name);
        emsg.append("\" not allowed at ");
        emsg.append(using.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (dcon.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribuite name not allowed at ");
        emsg.append(dcon.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (!(dcon.obj instanceof PEid)) {
        emsg = new StringBuffer();
        emsg.append("Either function or data constructor missing at ");
        emsg.append(dcon.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      int namedAttrCount = 0;
      for (int i = 0; i < this.itemList.size(); i++) {
        if (this.itemList.get(i).name != null) {
          namedAttrCount++;
        } else if (namedAttrCount > 0) {
          emsg = new StringBuffer();
          emsg.append("Attribute name missing at ");
          emsg.append(this.itemList.get(i).srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        } else {
          ;
        }
      }
      PEvalItem.ObjItem[] posdAttrs = new PEvalItem.ObjItem[this.itemList.size() - namedAttrCount];
      PEvalItem.ObjItem[] namedAttrs = new PEvalItem.ObjItem[namedAttrCount];
      for (int i = 0; i < posdAttrs.length; i++) {
        posdAttrs[i] = this.itemList.get(i);
        posdAttrs[i].fixAsParam();
      }
      for (int i = posdAttrs.length, j = 0; j < namedAttrCount; i++, j++) {
        namedAttrs[j] = this.itemList.get(i);
        namedAttrs[j].fixAsParam();
      }
      if (using != null) {
        using.fixAsParam();
      }
      return PDataConstrEval.create(this.srcInfo, this.scope, (PEid)dcon.obj, posdAttrs, namedAttrs, using);
    }

    private PEval createCaseEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem.ObjItem obj = this.itemList.get(0);
      if (obj.name != null) {
        emsg = new StringBuffer();
        emsg.append("Named attribute not allowed for case block at ");
        emsg.append(obj.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PExprObj v = (obj.obj instanceof PEid)? 
        PUndetEval.create(this.srcInfo, this.scope, (PEid)obj.obj, new PEvalItem.ObjItem[0]):
        (PExprObj)obj.obj;
      return PCaseEval.create(this.srcInfo, this.scope, v, (PCaseBlock)this.itemList.get(1).obj);
    }

    private PEval createDynInvEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem.ObjItem funObj = this.itemList.remove(this.itemList.size() - 1);
      if (funObj.name != null) {
        emsg = new StringBuffer();
        emsg.append("Name not allowed to function object at ");
        emsg.append(funObj.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (funObj.obj instanceof PEid) {
        funObj.obj = PUndetEval.create(funObj.srcInfo, this.scope, (PEid)funObj.obj, new PEvalItem.ObjItem[0]);
      } else if ((funObj.obj instanceof PClosure) && this.itemList.size() != ((PClosure)funObj.obj).params.length) {
        emsg = new StringBuffer();
        emsg.append("Argument count mismatch at ");
        emsg.append(funObj.srcInfo);
        emsg.append(".");
        emsg.append("\n  closure param count = ");
        emsg.append(((PClosure)funObj.obj).params.length);
        emsg.append("\n  actual argument count = ");
        emsg.append(this.itemList.size());
        throw new CompileException(emsg.toString());
      }
      PExprObj[] params = new PExprObj[this.itemList.size()];
      for (int i = 0; i < params.length; i++) {
        PEvalItem.ObjItem p = this.itemList.get(i);
        if (p.name != null) {
          emsg = new StringBuffer();
          emsg.append("Name not allowed to function object at ");
          emsg.append(p.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        p.fixAsParam();
        params[i] = (PExprObj)p.obj;
      }
      return PDynamicInvEval.create(this.srcInfo, this.scope, (PExprObj)funObj.obj, params);
    }

    private PEval createSelfInvEval() throws CompileException {
      StringBuffer emsg;
      PExprObj[] params = new PExprObj[this.itemList.size()];
      for (int i = 0; i < params.length; i++) {
        PEvalItem.ObjItem p = this.itemList.get(i);
        if (p.name != null) {
          emsg = new StringBuffer();
          emsg.append("Name not allowed to function object at ");
          emsg.append(p.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        p.fixAsParam();
        params[i] = (PExprObj)p.obj;
      }
      return PDynamicInvEval.create(this.srcInfo, this.scope, PFunRef.createSelf(this.srcInfo, this.scope), params);
    }

    private PEval createTermEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem.ObjItem item = this.itemList.get(0);  // element count == 1
      if (item.name != null) {
// /* DEBUG */ System.out.println(item);
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(item.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return PObjEval.create(item.getSrcInfo(), this.scope, item.obj);
    }
  }

  static PEval accept(ParserA.TokenReader reader, PScope outerScope) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(reader.getCurrentSrcInfo(), outerScope);
    int acceptables;
    PEvalItem item;
    while ((acceptables = builder.getAcceptables()) > 0
        && (item = PEvalItem.accept(reader, outerScope, builder.getFollowingSpace(), acceptables)) != null) {
      builder.addItem(item);
    }
    return builder.create();
  }

  static PEval acceptX(ParserB.Elem elem, PScope outerScope) throws CompileException {
    PEval eval = null;
    PExprObj o = null;
    if ((o = PByte.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PInt.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PReal.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PChar.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PTuple.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PEmptyList.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PList.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PString.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PDataConstrEval.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PClosure.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PFunRef.acceptX(elem, outerScope)) != null) {
      eval = PObjEval.create(o.getSrcInfo(), outerScope, o);
    } else if ((o = PExprVarRef.acceptX(elem, outerScope)) != null) {
      eval = PUndetEval.create(o.getSrcInfo(), outerScope, (PEid)o, new PEvalItem.ObjItem[0]);
    } else if ((eval = PStaticInvEval.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((eval = PDynamicInvEval.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((eval = PIfEval.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((eval = PCaseEval.acceptX(elem, outerScope)) != null) {
      ;
    }
    return eval;
  }
}
