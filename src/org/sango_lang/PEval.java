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

abstract class PEval {
  private static final int TAG_NOT_DETERMINED = 0;
  private static final int TAG_DATA_CONSTR_USING = 1;
  private static final int TAG_CASE_EXPR = 2;
  private static final int TAG_DYNAMIC_INV = 3;
  private static final int TAG_SELF_INV = 4;

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
  static final int ACCEPT_IF_BLOCK = 1 << 10;
  static final int ACCEPT_CASE_BLOCK = 1 << 11;
  static final int ACCEPT_DYNAMIC_INV = 1 << 12;
  static final int ACCEPT_SELF_INV = 1 << 13;
  static final int ACCEPT_DATA_CONSTR_USING = 1 << 14;
  static final int ACCEPT_EVAL = 1 << 15;
  static final int ACCEPT_PIPE = 1 << 16;
  static final int ACCEPT_PRIMITIVE = ACCEPT_BYTE + ACCEPT_INT + ACCEPT_REAL + ACCEPT_CHAR;
  static final int ACCEPT_COLLECTION = ACCEPT_LIST + ACCEPT_TUPLE + ACCEPT_STRING;
  static final int ACCEPT_DATA_OBJ = ACCEPT_PRIMITIVE + ACCEPT_COLLECTION;
  static final int ACCEPT_FUN_OBJ = ACCEPT_FUN_REF + ACCEPT_CLOSURE;

  private static int[] acceptablesTab = {
    // state 0:
    ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_BLOCK
    + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_EVAL,
    // state 1: x
    ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_BLOCK
    + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_EVAL + ACCEPT_CASE_BLOCK + ACCEPT_DATA_CONSTR_USING + ACCEPT_PIPE,
    // state 2: x x..
    ACCEPT_DATA_OBJ + ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_BLOCK
    + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV + ACCEPT_EVAL + ACCEPT_DATA_CONSTR_USING + ACCEPT_PIPE,
    // state 3: x x.. ::
    ACCEPT_ID + ACCEPT_IF_BLOCK + ACCEPT_EVAL,
    // state 4: x x.. :: x
    ACCEPT_ID,
    // state 5: x x.. &
    ACCEPT_FUN_OBJ + ACCEPT_ID + ACCEPT_IF_BLOCK + ACCEPT_EVAL,
    // state 6: evaluation ended -- ... :: x x  | ... & x  |  ... &&  |  x case { }  |  ... >> x
    ACCEPT_PIPE,
    // state 7: ... >>
    ACCEPT_ID + ACCEPT_CASE_BLOCK + ACCEPT_DYNAMIC_INV + ACCEPT_SELF_INV
  };

  static class Builder {
    int tag;
    int state;
    Parser.SrcInfo srcInfo;
    Parser.SrcInfo lastSrcInfo;
    List<PEvalItem> itemList;
    int followingSpace;

    static Builder newInstance() {
      return new Builder();
    }

    Builder() {
      this.itemList = new ArrayList<PEvalItem>();
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    int getAcceptables() { return acceptablesTab[this.state]; }

    int getFollowingSpace() { return this.followingSpace; }

    void setSrcInfo(Parser.SrcInfo si) {
      this.srcInfo = si;
    }

    void addItem(PEvalItem item) throws CompileException {
      PProgElem elem = item.elem;
      if (elem instanceof PByte) {
        this.addDataObj(ACCEPT_BYTE, item);
      } else if (elem instanceof PInt) {
        this.addDataObj(ACCEPT_INT, item);
      } else if (elem instanceof PReal) {
        this.addDataObj(ACCEPT_REAL, item);
      } else if (elem instanceof PChar) {
        this.addDataObj(ACCEPT_CHAR, item);
      } else if (elem instanceof PList) {
        this.addDataObj(ACCEPT_LIST, item);
      } else if (elem instanceof PTuple) {
        this.addDataObj(ACCEPT_TUPLE, item);
      } else if (elem instanceof PString) {
        this.addDataObj(ACCEPT_STRING, item);
      } else if (elem instanceof PExprId) {
        this.addId(item);
      } else if (elem instanceof PFunRef) {
        this.addFunObj(ACCEPT_FUN_REF, item);
      } else if (elem instanceof PClosure) {
        this.addFunObj(ACCEPT_CLOSURE, item);
      } else if (elem instanceof PIfBlock) {
        this.addIfBlock(item);
      } else if (elem instanceof PCaseBlock) {
        this.addCaseBlock(item);
      } else if (elem instanceof PDynamicInv) {
        this.dynamicInv();
      } else if (elem instanceof PSelfInv) {
        this.selfInv();
      } else if (elem instanceof PDataConstrUsing) {
        this.dataConstrUsing();
      } else if (elem instanceof PEvalElem) {
        this.addEval(item);
      } else if (elem instanceof PPipe) {
        this.pipe();
      } else {
        /* DEBUG */ System.out.print("Invalid item = ");
        /* DEBUG */ System.out.println(elem);
        throw new IllegalArgumentException("Invalid item");
      }
      this.lastSrcInfo = item.srcInfo;
    }

    private void addDataObj(int acpt, PEvalItem item) {
      if ((acceptablesTab[this.state] & acpt) == 0) {
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

    private void addId(PEvalItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_ID) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      ((PExprId)item.elem).cutOffCat(PExprId.CAT_DCON_PTN);
      this.itemList.add(item);
      switch (this.state) {
      case 0: this.state = 1; break;
      case 1: this.state = 2; break;
      case 2: this.state = 2; break;
      case 3:
        ((PExprId)item.elem).cutOffDcon();
        this.state = 4;
        break;
      case 4:
        ((PExprId)item.elem).setCat(PExprId.CAT_DCON_EVAL);
        this.state = 6;
        break;
      case 5:
        ((PExprId)item.elem).cutOffDcon();
        this.state = 6;
        break;
      case 7:
        ((PExprId)item.elem).setFun();
        this.state = 6;
        break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addFunObj(int acpt, PEvalItem item) throws CompileException {
      StringBuffer emsg;
      if ((acceptablesTab[this.state] & acpt) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      if (this.state != 5 && item.elem instanceof PClosure) {
        PClosure closure = (PClosure)item.elem;
        for (int i = 0; i < closure.params.length; i++) {
          PEVarDef param = closure.params[i];
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

    private void addIfBlock(PEvalItem item) {
      StringBuffer emsg;
      if ((acceptablesTab[this.state] & ACCEPT_IF_BLOCK) == 0) {
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

    private void addCaseBlock(PEvalItem item) throws CompileException {
      if ((acceptablesTab[this.state] & ACCEPT_CASE_BLOCK) == 0) {
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

    private void addEval(PEvalItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_EVAL) == 0) {
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
      PEvalElem e = this.create();
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

    PEvalElem create() throws CompileException {
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

    private PEvalElem createDispatch() throws CompileException {
      StringBuffer emsg;
      PEvalElem e;
      if (this.tag == TAG_DATA_CONSTR_USING) {
        e = this.createDataConstrUsingEval();
      } else if (this.tag == TAG_CASE_EXPR) {
        e = this.createCaseEval();
      } else if (this.tag == TAG_DYNAMIC_INV) {
        e = this.createDynInvEval();
      } else if (this.tag == TAG_SELF_INV) {
        e = this.createSelfInvEval();
      } else if (this.itemList.size() == 1) {
        PProgElem elem = this.itemList.get(0).elem;
        if (elem instanceof PExprId) {
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
          || elem instanceof PIfBlock
          || elem instanceof PEvalElem
          ) {
          e = this.createTermEval();
        } else {
          e = this.createDispatch2();
        }
      } else {
        e = this.createDispatch2();
      }
      return e;
    }

    private PEvalElem createDispatch2() throws CompileException {
      StringBuffer emsg;
      PEvalItem anchor = this.itemList.remove(this.itemList.size() - 1);
      if (anchor.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (!(anchor.elem instanceof PExprId)) {
        emsg = new StringBuffer();
        emsg.append("Either function or data constructor missing at ");
        emsg.append(anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      int namedAttrCount = 0;
      for (int i = 0; i < this.itemList.size(); i++) {
        this.itemList.get(i).fixAsParam();
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
      PEvalElem posdParams[] = new PEvalElem[this.itemList.size() - namedAttrCount];
      PEvalItem namedAttrs[] = new PEvalItem[namedAttrCount];
      for (int i = 0; i < posdParams.length; i++) {
        posdParams[i] = (PEvalElem)this.itemList.get(i).elem;
      }
      for (int i = posdParams.length, j = 0; j < namedAttrCount; i++, j++) {
        namedAttrs[j] = this.itemList.get(i);
      }
      PEvalElem e;
      if (namedAttrCount > 0) {
        PExprId dcon = (PExprId)anchor.elem;
	dcon.setCat(PExprId.CAT_DCON_EVAL);
        e = PDataConstrEval.create(this.srcInfo, dcon, posdParams, namedAttrs, null);
      } else {
        PExprId id = (PExprId)anchor.elem;
        id.cutOffCat(PExprId.CAT_DCON_PTN);
        e = PUndetEval.create(this.srcInfo, id, posdParams);
      }
      return e;
      // return (namedAttrCount > 0)?
        // PDataConstrEval.create(this.srcInfo, (PExprId)anchor.elem, posdParams, namedAttrs, null):
        // PUndetEval.create(this.srcInfo, (PExprId)anchor.elem, posdParams);
    }

    private PEvalElem createDataConstrUsingEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem dcon = this.itemList.remove(this.itemList.size() - 1);
      PEvalItem using = this.itemList.remove(this.itemList.size() - 1);
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
      if (!(dcon.elem instanceof PExprId)) {
        emsg = new StringBuffer();
        emsg.append("Either function or data constructor missing at ");
        emsg.append(dcon.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      int namedAttrCount = 0;
      for (int i = 0; i < this.itemList.size(); i++) {
        this.itemList.get(i).fixAsParam();
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
      PEvalElem posdAttrs[] = new PEvalElem[this.itemList.size() - namedAttrCount];
      PEvalItem namedAttrs[] = new PEvalItem[namedAttrCount];
      for (int i = 0; i < posdAttrs.length; i++) {
        posdAttrs[i] = (PEvalElem)this.itemList.get(i).elem;
      }
      for (int i = posdAttrs.length, j = 0; j < namedAttrCount; i++, j++) {
        namedAttrs[j] = this.itemList.get(i);
      }
      return PDataConstrEval.create(this.srcInfo, (PExprId)dcon.elem, posdAttrs, namedAttrs, (PEvalElem)using.elem);
    }

    private PEvalElem createCaseEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem obj = this.itemList.get(0);
      if (obj.name != null) {
        emsg = new StringBuffer();
        emsg.append("Named attribute not allowed for case block at ");
        emsg.append(obj.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PEvalElem v = (obj.elem instanceof PExprId)? 
        PUndetEval.create(this.srcInfo, (PExprId)obj.elem, new PEvalElem[0]):
        (PEvalElem)obj.elem;
      return PCaseEval.create(this.srcInfo, v, (PCaseBlock)this.itemList.get(1).elem);
      // return PCaseEval.create(this.srcInfo, (PEvalElem)obj.elem, (PCaseBlock)this.itemList.get(1).elem);
    }

    private PEvalElem createDynInvEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem funObj = this.itemList.remove(this.itemList.size() - 1);
      if (funObj.name != null) {
        emsg = new StringBuffer();
        emsg.append("Name not allowed to function object at ");
        emsg.append(funObj.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if ((funObj.elem instanceof PClosure) && this.itemList.size() != ((PClosure)funObj.elem).params.length) {
        emsg = new StringBuffer();
        emsg.append("Argument count mismatch at ");
        emsg.append(funObj.srcInfo);
        emsg.append(".");
        emsg.append("\n  closure param count = ");
        emsg.append(((PClosure)funObj.elem).params.length);
        emsg.append("\n  actual argument count = ");
        emsg.append(this.itemList.size());
        throw new CompileException(emsg.toString());
      }
      PEvalElem[] params = new PEvalElem[this.itemList.size()];
      for (int i = 0; i < params.length; i++) {
        PEvalItem p = this.itemList.get(i);
        if (p.name != null) {
          emsg = new StringBuffer();
          emsg.append("Name not allowed to function object at ");
          emsg.append(p.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        p.fixAsParam();
        params[i] = (PEvalElem)p.elem;
      }
      return PDynamicInvEval.create(this.srcInfo, (PEvalElem)funObj.elem, params);
    }

    private PEvalElem createSelfInvEval() throws CompileException {
      StringBuffer emsg;
      PEvalElem[] params = new PEvalElem[this.itemList.size()];
      for (int i = 0; i < params.length; i++) {
        PEvalItem p = this.itemList.get(i);
        if (p.name != null) {
          emsg = new StringBuffer();
          emsg.append("Name not allowed to function object at ");
          emsg.append(p.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        p.fixAsParam();
        params[i] = (PEvalElem)p.elem;
      }
      return PDynamicInvEval.create(this.srcInfo, PFunRef.createSelf(this.srcInfo), params);
    }

    private PEvalElem createTermEval() throws CompileException {
      StringBuffer emsg;
      PEvalItem item = this.itemList.get(0);  // element count == 1
      if (item.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(item.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return (PEvalElem)item.elem;
    }
  }

  static PEvalElem accept(ParserA.TokenReader reader) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance();
    builder.setSrcInfo(reader.getCurrentSrcInfo());
    int acceptables;
    PEvalItem item;
    while ((acceptables = builder.getAcceptables()) > 0
        && (item = PEvalItem.accept(reader, builder.getFollowingSpace(), acceptables)) != null) {
      builder.addItem(item);
    }
    return builder.create();
  }

  static PEvalElem acceptX(ParserB.Elem elem) throws CompileException {
    PEvalElem eval = null;
    if ((eval = PByte.acceptX(elem)) != null) {
      ;
    } else if ((eval = PInt.acceptX(elem)) != null) {
      ;
    } else if ((eval = PReal.acceptX(elem)) != null) {
      ;
    } else if ((eval = PChar.acceptX(elem)) != null) {
      ;
    } else if ((eval = PTuple.acceptX(elem)) != null) {
      ;
    } else if ((eval = PEmptyList.acceptX(elem)) != null) {
      ;
    } else if ((eval = PList.acceptX(elem)) != null) {
      ;
    } else if ((eval = PString.acceptX(elem)) != null) {
      ;
    } else if ((eval = PDataConstrEval.acceptX(elem)) != null) {
      ;
    } else if ((eval = PClosure.acceptX(elem)) != null) {
      ;
    } else if ((eval = PFunRef.acceptX(elem)) != null) {
      ;
    } else if ((eval = PEVarRef.acceptX(elem)) != null) {
      ;
    } else if ((eval = PStaticInvEval.acceptX(elem)) != null) {
      ;
    } else if ((eval = PDynamicInvEval.acceptX(elem)) != null) {
      ;
    } else if ((eval = PIfBlock.acceptX(elem)) != null) {
      ;
    } else if ((eval = PCaseEval.acceptX(elem)) != null) {
      ;
    }
    return eval;
  }

  static PEvalElem acceptEnclosed(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token lpar;
    if ((lpar = ParserA.acceptToken(reader, LToken.LPAR, spc)) == null) { return null; }
    PEvalElem eval;
    if ((eval = PExpr.accept(reader)) == null) {
      emsg = new StringBuffer();
      emsg.append("Expression missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if ((ParserA.acceptToken(reader, LToken.RPAR, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\")\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    eval.setSrcInfo(lpar.getSrcInfo());  // set source info to lpar's
    return eval;
  }
}
