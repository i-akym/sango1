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

abstract class PPtn {
  static final int ACCEPT_NOTHING = 0;
  static final int ACCEPT_BYTE = 1 << 0;
  static final int ACCEPT_INT = 1 << 1;
  static final int ACCEPT_REAL = 1 << 2;
  static final int ACCEPT_CHAR = 1 << 3;
  static final int ACCEPT_LIST = 1 << 4;
  static final int ACCEPT_TUPLE = 1 << 5;
  static final int ACCEPT_STRING = 1 << 6;
  static final int ACCEPT_ID = 1 << 7;
  static final int ACCEPT_VARDEF_NOT_CASTED = 1 << 8;
  static final int ACCEPT_VARDEF_CASTED = 1 << 9;
  static final int ACCEPT_WILD_CARD = 1 << 10;
  static final int ACCEPT_WILD_CARDS = 1 << 11;
  static final int ACCEPT_PTN_MATCH = 1 << 12;
  static final int ACCEPT_PRIMITIVE = ACCEPT_BYTE + ACCEPT_INT + ACCEPT_REAL + ACCEPT_CHAR;
  static final int ACCEPT_COLLECTION = ACCEPT_LIST + ACCEPT_TUPLE + ACCEPT_STRING;
  static final int ACCEPT_DATA_OBJ = ACCEPT_PRIMITIVE + ACCEPT_COLLECTION;
  static final int ACCEPT_VARDEF = ACCEPT_VARDEF_NOT_CASTED + ACCEPT_VARDEF_CASTED;

  private static int[] acceptablesTab = {
    // state 0:
    ACCEPT_DATA_OBJ + ACCEPT_ID + ACCEPT_VARDEF
    + ACCEPT_WILD_CARD + ACCEPT_WILD_CARDS + ACCEPT_PTN_MATCH,
    // state 1: type
    ACCEPT_VARDEF_NOT_CASTED,
    // state 2: x..
    ACCEPT_DATA_OBJ + ACCEPT_ID + ACCEPT_VARDEF
    + ACCEPT_WILD_CARD + ACCEPT_WILD_CARDS + ACCEPT_PTN_MATCH,
    // state 3: x.. ***
    ACCEPT_ID,
    // state 4: x.. *** x
    ACCEPT_NOTHING
  };

  static class Builder {
    int context;
    Parser.SrcInfo srcInfo;
    PScope scope;
    Parser.SrcInfo lastSrcInfo;
    int state;
    PType leadingCast;
    List<PPtnItem> itemList;
    int followingSpace;

    static Builder newInstance(Parser.SrcInfo srcInfo, PScope outerScope) {
      return new Builder(srcInfo, outerScope);
    }

    Builder(Parser.SrcInfo srcInfo, PScope outerScope) {
      this.srcInfo = srcInfo;
      this.scope = outerScope;
      this.itemList = new ArrayList<PPtnItem>();
      this.followingSpace = ParserA.SPACE_DO_NOT_CARE;
    }

    int getAcceptables() { return acceptablesTab[this.state]; }

    int getFollowingSpace() { return this.followingSpace; }

    // void setSrcInfo(Parser.SrcInfo si) {
      // this.srcInfo = si;
    // }

    void setContext(int context) {
      this.context = context;
    }

    void setLeadingCast(PType cast) {
      StringBuffer emsg;
      if (this.state != 0) {
        throw new IllegalStateException("Cannot set leading cast.");
      }
      this.leadingCast = cast;
      this.state = 1;
    }

    void addItem(PPtnItem item) {
      PProgElem elem = item.elem;
      if (elem instanceof PByte) {
        this.addDataObj(ACCEPT_BYTE, item);
      } else if (elem instanceof PInt) {
        this.addDataObj(ACCEPT_INT, item);
      } else if (elem instanceof PReal) {
        this.addDataObj(ACCEPT_REAL, item);
      } else if (elem instanceof PChar) {
        this.addDataObj(ACCEPT_CHAR, item);
      } else if (elem instanceof PListPtn) {
        this.addDataObj(ACCEPT_LIST, item);
      } else if (elem instanceof PTuplePtn) {
        this.addDataObj(ACCEPT_TUPLE, item);
      } else if (elem instanceof PStringPtn) {
        this.addDataObj(ACCEPT_STRING, item);
      } else if (elem instanceof PEid) {
        this.addId(item);
      } else if (elem instanceof PExprVarDef) {
        this.addVarDef(item);
      } else if (elem instanceof PWildCard) {
        this.addWildCard(item);
      } else if (elem instanceof PWildCards) {
        this.addWildCards(item);
      } else if (elem instanceof PPtnMatch) {
        this.addPtnMatch(item);
      } else {
        /* DEBUG */ System.out.print("Invalid item = ");
        /* DEBUG */ System.out.println(elem);
        throw new IllegalArgumentException("Invalid item");
      }
      this.lastSrcInfo = item.srcInfo;
    }

    private void addDataObj(int acpt, PPtnItem item) {
      if ((acceptablesTab[this.state] & acpt) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: state = 2; break;
      case 2: state = 2; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addId(PPtnItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_ID) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      PEid id = (PEid)item.elem;
      id.cutOffFun();
      id.cutOffDconEval();
      this.itemList.add(item);
      // this.itemList.add(id);
      switch (this.state) {
      case 0: state = 2; break;
      case 2: state = 2; break;
      case 3: state = 4; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addVarDef(PPtnItem item) {
      PExprVarDef v = (PExprVarDef)item.elem;
      if ((v.type == null && (acceptablesTab[this.state] & ACCEPT_VARDEF_NOT_CASTED) == 0)
          || (v.type != null && (acceptablesTab[this.state] & ACCEPT_VARDEF_CASTED) == 0)) {
        throw new IllegalArgumentException("Invalid item");
      }
      if (this.state == 1) {
        ((PExprVarDef)item.elem).type = this.leadingCast;
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: state = 2; break;
      case 1: state = 2; break;
      case 2: state = 2; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addWildCard(PPtnItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_WILD_CARD) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: state = 2; break;
      case 2: state = 2; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addWildCards(PPtnItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_WILD_CARDS) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: state = 3; break;
      case 2: state = 3; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    private void addPtnMatch(PPtnItem item) {
      if ((acceptablesTab[this.state] & ACCEPT_PTN_MATCH) == 0) {
        throw new IllegalArgumentException("Invalid item");
      }
      this.itemList.add(item);
      switch (this.state) {
      case 0: state = 2; break;
      case 2: state = 2; break;
      }
      this.followingSpace = ParserA.SPACE_NEEDED;
    }

    PExprObj create() throws CompileException {
      StringBuffer emsg;
      switch (this.state) {
      case 0:
        return null;
      case 1:
      case 3:
        emsg = new StringBuffer();
        emsg.append("Incomplete evaluation after ");
        emsg.append(this.lastSrcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return this.createDispatch();
    }

    PExprObj createDispatch() throws CompileException {
      StringBuffer emsg;
      PProgElem anchorElem = this.itemList.get(this.itemList.size() - 1).elem;
      PExprObj p;
      if (this.itemList.size() == 1) {
        if (anchorElem instanceof PEid) {
          p = this.createUndetPtn();
        } else if (anchorElem instanceof PByte
            || anchorElem instanceof PInt
            || anchorElem instanceof PReal
            || anchorElem instanceof PChar
            || anchorElem instanceof PListPtn
            || anchorElem instanceof PTuplePtn
            || anchorElem instanceof PStringPtn
            || anchorElem instanceof PExprVarDef
            || anchorElem instanceof PWildCard
            || anchorElem instanceof PExprObj) {
          p = this.createTermPtn();
        } else {
          emsg = new StringBuffer();
          emsg.append("Invalid pattern at ");
          emsg.append(this.itemList.get(this.itemList.size() - 1).srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      } else if (anchorElem instanceof PEid) {
        p = this.createDataConstrPtn();
      } else {
        emsg = new StringBuffer();
        emsg.append("Data constructor missing at ");
        emsg.append(this.lastSrcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return p;
    }

    private PExprObj createTermPtn() throws CompileException {
      StringBuffer emsg;
      PPtnItem item = this.itemList.get(0);
      if (item.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(item.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (!(item.elem instanceof PExprObj)) {
        emsg = new StringBuffer();
        emsg.append("Not allowed for pattern at ");
        emsg.append(item.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      return (PExprObj)item.elem;
    }

    private PExprObj createUndetPtn() throws CompileException {
      StringBuffer emsg;
      PPtnItem item = this.itemList.get(0);
      if (item.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(item.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      PEid id = (PEid)item.elem;
      id.cutOffFun();
      id.cutOffDconEval();
      return PUndetPtn.create(this.srcInfo, this.scope, this.context, id);
    }

    private PExprObj createDataConstrPtn() throws CompileException {
      StringBuffer emsg;
      PPtnItem anchor = this.itemList.remove(this.itemList.size() - 1);
      if (anchor.name != null) {
        emsg = new StringBuffer();
        emsg.append("Attribute name not allowed at ");
        emsg.append(anchor.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      ((PEid)anchor.elem).cutOffVar();
      PPtnItem lastAttr = this.itemList.get(this.itemList.size() - 1);
      boolean wildCards;
      if (lastAttr.elem instanceof PWildCards) {
        wildCards = true;
        this.itemList.remove(this.itemList.size() - 1);
      } else {
        wildCards = false;
      }
      int namedAttrCount = 0;
      for (int i = 0; i < this.itemList.size(); i++) {
        this.itemList.get(i).fixAsAttr();
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
      PExprObj posdAttrs[] = new PExprObj[this.itemList.size() - namedAttrCount];
      PPtnItem namedAttrs[] = new PPtnItem[namedAttrCount];
      for (int i = 0; i < posdAttrs.length; i++) {
        posdAttrs[i] = (PExprObj)this.itemList.get(i).elem;
      }
      for (int i = posdAttrs.length, j = 0; j < namedAttrCount; i++, j++) {
        namedAttrs[j] = this.itemList.get(i);
      }
      PEid dcon = (PEid)anchor.elem;
      dcon.setCat(PDefDict.EID_CAT_DCON_PTN);
      return PDataConstrPtn.create(this.srcInfo, this.scope, this.context, dcon, posdAttrs, namedAttrs, wildCards);
    }
  }

  static PExprObj accept(ParserA.TokenReader reader, PScope outerScope, int context, PType leadingCast) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder;
    if (leadingCast != null) {
      builder = Builder.newInstance(leadingCast.getSrcInfo(), outerScope);
      builder.setLeadingCast(leadingCast);
    } else {
      builder = Builder.newInstance(reader.getCurrentSrcInfo(), outerScope);
    }
    builder.setContext(context);
    PPtnItem item;
    int acceptables;
    while ((acceptables = builder.getAcceptables()) > 0
        && (item = PPtnItem.accept(reader, outerScope, builder.getFollowingSpace(), acceptables, context)) != null) {
      builder.addItem(item);
    }
    return builder.create();
  }

  static PExprObj acceptX(ParserB.Elem elem , PScope outerScope, int context) throws CompileException {
    PExprObj ptn = null;
    if ((ptn = PByte.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PInt.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PChar.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PTuplePtn.acceptX(elem, outerScope, context)) != null) {
      ;
    } else if ((ptn = PEmptyListPtn.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PListPtn.acceptX(elem, outerScope, context)) != null) {
      ;
    } else if ((ptn = PStringPtn.acceptX(elem, outerScope, context)) != null) {
      ;
    } else if ((ptn = PExprVarDef.acceptX(elem, outerScope, PExprVarDef.CAT_FUN_PARAM, PExprVarDef.TYPE_MAYBE_SPECIFIED)) != null) {
      ;
    } else if ((ptn = PWildCard.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PExprVarRef.acceptX(elem, outerScope)) != null) {
      ;
    } else if ((ptn = PDataConstrPtn.acceptX(elem, outerScope, context)) != null) {
      ;
    }
    return ptn;
  }
}
