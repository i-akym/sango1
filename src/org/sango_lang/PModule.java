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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

class PModule extends PDefaultProgObj implements PDefDict {
  static final String IMPL_WORD_NATIVE = "@native";

  static final String MOD_ID_LANG = "@LANG";
  static final String MOD_ID_HERE = "@HERE";

  static final String ACC_WORD_PUBLIC = "@public";
  static final String ACC_WORD_PROTECTED = "@protected";
  static final String ACC_WORD_OPAQUE = "@opaque";
  static final String ACC_WORD_PRIVATE = "@private";
  static final String ACC_WORDX_PUBLIC = "public";
  static final String ACC_WORDX_PROTECTED = "protected";
  static final String ACC_WORDX_OPAQUE = "opaque";
  static final String ACC_WORDX_PRIVATE = "private";
  static final int ACC_OPTS_FOR_EVAL = Module.ACC_PUBLIC + Module.ACC_PRIVATE;
  static final int ACC_OPTS_FOR_DATA = Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE;
  static final int ACC_OPTS_FOR_EXTEND = Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE;
  static final int ACC_OPTS_FOR_ALIAS = Module.ACC_PUBLIC + Module.ACC_PRIVATE;
  static final int ACC_DEFAULT_FOR_EVAL = Module.ACC_PRIVATE;
  static final int ACC_DEFAULT_FOR_DATA = Module.ACC_PRIVATE;
  static final int ACC_DEFAULT_FOR_EXTEND = Module.ACC_PRIVATE;
  static final int ACC_DEFAULT_FOR_ALIAS = Module.ACC_PRIVATE;

  static final String AVAILABILITY_WORD_GENERAL = "@general";
  static final String AVAILABILITY_WORD_ALPHA = "@alpha";
  static final String AVAILABILITY_WORD_BETA = "@beta";
  static final String AVAILABILITY_WORD_LIMITED = "@limited";
  static final String AVAILABILITY_WORD_DEPRECATED = "@deprecated";
  static final String AVAILABILITY_WORDX_GENERAL = "general";
  static final String AVAILABILITY_WORDX_ALPHA = "alpha";
  static final String AVAILABILITY_WORDX_BETA = "beta";
  static final String AVAILABILITY_WORDX_LIMITED = "limited";
  static final String AVAILABILITY_WORDX_DEPRECATED = "deprecated";

  Compiler theCompiler;
  Parser.SrcInfo modDefSrcInfo;
  int availability;
  Cstr definedName;  // maybe null
  Cstr name;
  String myId;
  List<PImportStmt> importStmtList;
  List<PDataStmt> dataStmtList;
  List<PExtendStmt> extendStmtList;
  List<PAliasTypeStmt> aliasTypeStmtList;
  List<PEvalStmt> evalStmtList;
  List<Cstr> farModList;  // foreign module other than "sango.lang"
  Map<String, Integer> modDict;
  Map<String, PDefDict.TconProps> tconDict;
  Map<String, PDefDict.EidProps> eidDict;
  Map<String, PDataConstrDef> dconDict;
  Map<String, Integer> funOfficialDict;
  Map<String, List<Integer>> funDict;
  ForeignIdResolver foreignIdResolver;
  int idSuffix;

  private PModule() {
    this.importStmtList = new ArrayList<PImportStmt>();
    this.dataStmtList = new ArrayList<PDataStmt>();
    this.extendStmtList = new ArrayList<PExtendStmt>();
    this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
    this.evalStmtList = new ArrayList<PEvalStmt>();
    this.farModList = new ArrayList<Cstr>();
    this.modDict = new HashMap<String, Integer>();
    this.tconDict = new HashMap<String, PDefDict.TconProps>();
    this.eidDict = new HashMap<String, PDefDict.EidProps>();
    this.dconDict = new HashMap<String, PDataConstrDef>();
    this.funOfficialDict = new HashMap<String, Integer>();
    this.funDict = new HashMap<String, List<Integer>>();
    this.foreignIdResolver = new ForeignIdResolver();
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("module[src=");
    b.append(this.modDefSrcInfo);
    b.append(",name=");
    b.append(this.name);
    if (this.myId != null) {
      b.append(",id=");
      b.append(this.myId);
    }
    b.append("]");
    return b.toString();
  }

  boolean isLang() {
    return this.name != null && this.name.equals(Module.MOD_LANG);
  }

  Cstr resolveModId(String id) {
    Cstr n = null;
    if (id == null) {
      ;
    } else if (id.equals(this.myId)) {
      n = this.name;
    } else if (id.equals(MOD_ID_HERE)) {
      n = this.name;
    } else if (id.equals(MOD_ID_LANG)) {
      n = Module.MOD_LANG;
    } else {
      Integer i = this.modDict.get(id);
      if (i != null) {
        n = this.importStmtList.get(i).modName;
        if (!n.equals(this.name)
             && !n.equals(Module.MOD_LANG)
             && !this.farModList.contains(n)) {
          this.farModList.add(n);
        }
      }
    }
    return n;
  }

  String generateId() {
    return "@@" + this.idSuffix++;
  }

  static class Builder {
    Cstr requiredName;
    PModule mod;
    List<PImportStmt> importStmtList;
    List<PDataStmt> dataStmtList;
    List<PExtendStmt> extendStmtList;
    List<PAliasTypeStmt> aliasTypeStmtList;
    List<PEvalStmt> evalStmtList;

    static Builder newInstance(Cstr requiredName) {
      return new Builder(requiredName);
    }

    Builder(Cstr requiredName) {
      this.requiredName = requiredName;
      this.mod = new PModule();
      this.importStmtList = new ArrayList<PImportStmt>();
      this.dataStmtList = new ArrayList<PDataStmt>();
      this.extendStmtList = new ArrayList<PExtendStmt>();
      this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
      this.evalStmtList = new ArrayList<PEvalStmt>();
    }

    void setSrcInfo(Parser.SrcInfo si) {
      this.mod.srcInfo = si;
    }

    void setAvailability(int availability) {
      this.mod.availability = availability;
    }

    void setDefinedName(Cstr name) throws CompileException {
      StringBuffer emsg;
      if (this.requiredName != null && !name.equals(this.requiredName)) {
        emsg = new StringBuffer();
        emsg.append("Module name mismatch.");
        emsg.append("\n  required: ");
        emsg.append(this.requiredName.toJavaString());
        emsg.append("\n  defined: ");
        emsg.append(name.toJavaString());
        throw new CompileException(emsg.toString());
      }
      this.mod.definedName = name;
    }

    void setMyId(String id) {
      this.mod.myId = id;
    }

    void addImportStmt(PImportStmt imp) throws CompileException {
      this.importStmtList.add(imp);
    }

    void addDataStmt(PDataStmt dat) throws CompileException {
      this.dataStmtList.add(dat);
    }

    void addExtendStmt(PExtendStmt ext) throws CompileException {
      this.extendStmtList.add(ext);
    }

    void addAliasStmt(PAliasTypeStmt alias) throws CompileException {
      this.aliasTypeStmtList.add(alias);
    }

    void addEvalStmt(PEvalStmt eval) throws CompileException {
      this.evalStmtList.add(eval);
    }

    PModule create() throws CompileException {
      if (this.mod.definedName != null) {
        this.mod.name = this.mod.definedName;
      } else {
        this.mod.name = this.requiredName;
      }
      if (!this.mod.name.equals(Module.MOD_LANG)) {
        PImportStmt.Builder ib = PImportStmt.Builder.newInstance();
        ib.setModName(Module.MOD_LANG);
        ib.setId(MOD_ID_LANG);
        this.mod.addImportStmt(ib.create());
      }
      for (int i = 0; i < this.importStmtList.size(); i++) {
        this.mod.addImportStmt(this.importStmtList.get(i));
      }
      for (int i = 0; i < this.dataStmtList.size(); i++) {
        this.mod.addDataStmt(this.dataStmtList.get(i));
      }
      for (int i = 0; i < this.extendStmtList.size(); i++) {
        this.mod.addExtendStmt(this.extendStmtList.get(i));
      }
      for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
        this.mod.addAliasStmt(this.aliasTypeStmtList.get(i));
      }
      for (int i = 0; i < this.evalStmtList.size(); i++) {
        this.mod.addEvalStmt(this.evalStmtList.get(i));
      }
      return this.mod;
    }
  }
  static PModule accept(ParserA.TokenReader reader, Cstr modName) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(modName);
    acceptModuleStmt(reader, builder);
    if (modName.equals(Module.MOD_LANG)) {
      Parser.SrcInfo si = new Parser.SrcInfo(Module.MOD_LANG, ":builtin");
      builder.addDataStmt(PDataStmt.createForVariableParams(si, Module.TCON_TUPLE, Module.ACC_OPAQUE));
      builder.addDataStmt(PDataStmt.createForVariableParams(si, Module.TCON_FUN, Module.ACC_OPAQUE));
    }
    acceptDefStmts(reader, builder);
    PModule mod = builder.create();
    mod.generateNameFun();
    mod.generateInitdFun();
    mod.generateDataFuns();
    return mod;
  }

  static PModule acceptX(ParserB.Elem elem, Cstr modName) throws CompileException {
    StringBuffer emsg;
    if (elem == null) {
      throw new CompileException("No module definition.");
    }
    if (!elem.getName().equals("module")) {
      throw new CompileException("No module definition.");
    }

    Builder builder = Builder.newInstance(modName);
    Cstr name = elem.getAttrValueAsCstrData("name");
    if (name != null) {
      builder.setDefinedName(name);
    }
    builder.setAvailability(acceptXAvailabilityAttr(elem));
    String id = elem.getAttrValueAsId("id");
    if (id != null) {
      builder.setMyId(id);
    }

    ParserB.Elem e = elem.getFirstChild();
    while (e != null) {
      acceptXDef(e, builder);
      e = e.getNextSibling();
    }
    PModule mod = builder.create();
    mod.generateNameFun();
    mod.generateInitdFun();
    mod.generateDataFuns();
    return mod;
  }

  static void acceptXDef(ParserB.Elem elem, Builder builder) throws CompileException {
    PImportStmt imp;
    PDataStmt dat;
    PExtendStmt ext;
    PAliasTypeStmt alias;
    PEvalStmt eval;
    if ((imp = PImportStmt.acceptX(elem)) != null) {
      builder.addImportStmt(imp);
    } else if ((dat = PDataStmt.acceptX(elem)) != null) {
      builder.addDataStmt(dat);
    } else if ((ext = PExtendStmt.acceptX(elem)) != null) {
      builder.addExtendStmt(ext);
    } else if ((alias = PAliasTypeStmt.acceptX(elem)) != null) {
      builder.addAliasStmt(alias);
    } else if ((eval = PEvalStmt.acceptX(elem)) != null) {
      builder.addEvalStmt(eval);
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Unexpected XML node. - ");
      emsg.append(elem.getSrcInfo().toString());
      throw new CompileException(emsg.toString());
    }
  }

  private static void acceptModuleStmt(ParserA.TokenReader reader, Builder builder) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t;
    if ((t = ParserA.acceptSpecifiedWord(reader, "module", ParserA.SPACE_DO_NOT_CARE)) == null) {
      return;
    }
    builder.setSrcInfo(t.getSrcInfo());
    builder.setAvailability(acceptAvailability(reader));
    if ((t = ParserA.acceptCstr(reader, ParserA.SPACE_NEEDED)) == null) {
      emsg = new StringBuffer();
      emsg.append("No module name at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    builder.setDefinedName(t.value.cstrValue);
    if ((t = ParserA.acceptToken(reader, LToken.HYPH_GT, ParserA.SPACE_DO_NOT_CARE)) != null) {
      PExprId id;
      if ((id = PExprId.accept(reader, PExprId.ID_NO_QUAL, ParserA.SPACE_DO_NOT_CARE)) == null) {
        emsg = new StringBuffer();
        emsg.append("My id missing at ");
        emsg.append(reader.getCurrentSrcInfo());
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      builder.setMyId(id.name);
    }
    if ((t = ParserA.acceptToken(reader, LToken.SEM_SEM, ParserA.SPACE_DO_NOT_CARE)) == null) {
      emsg = new StringBuffer();
      emsg.append("\";;\" missing at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
  }

  private static void acceptDefStmts(ParserA.TokenReader reader, Builder builder) throws CompileException, IOException {
    StringBuffer emsg;
    ParserA.Token t = reader.getToken();
    PImportStmt imp;
    PDataStmt dat;
    PExtendStmt ext;
    PAliasTypeStmt alias;
    PEvalStmt eval;
    while (!t.isEOF()) {
      if ((imp = PImportStmt.accept(reader)) != null) {
        builder.addImportStmt(imp);
      } else if ((dat = PDataStmt.accept(reader)) != null) {
        builder.addDataStmt(dat);
      } else if ((ext = PExtendStmt.accept(reader)) != null) {
        builder.addExtendStmt(ext);
      } else if ((alias = PAliasTypeStmt.accept(reader)) != null) {
        builder.addAliasStmt(alias);
      } else if ((eval = PEvalStmt.accept(reader)) != null) {
        builder.addEvalStmt(eval);
      } else {
        emsg = new StringBuffer();
        emsg.append("Syntax error at ");
        emsg.append(t.getSrcInfo());
        emsg.append(". - ");
        emsg.append(t.value.token);
        throw new CompileException(emsg.toString());
      }
      t = reader.getToken();
    }
  }

  static int acceptAcc(ParserA.TokenReader reader, int options, int defaultValue) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    if (a == null) {
      return defaultValue;
    } else if (((options & Module.ACC_PUBLIC) > 0) && a.value.token.equals(ACC_WORD_PUBLIC)) {
      reader.tokenConsumed();
      return Module.ACC_PUBLIC;
    } else if (((options & Module.ACC_PROTECTED) > 0) && a.value.token.equals(ACC_WORD_PROTECTED)) {
      reader.tokenConsumed();
      return Module.ACC_PROTECTED;
    } else if (((options & Module.ACC_OPAQUE) > 0) && a.value.token.equals(ACC_WORD_OPAQUE)) {
      reader.tokenConsumed();
      return Module.ACC_OPAQUE;
    } else if (((options & Module.ACC_PRIVATE) > 0) && a.value.token.equals(ACC_WORD_PRIVATE)) {
      reader.tokenConsumed();
      return Module.ACC_PRIVATE;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid access descriptor at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(a.value.token);
      throw new CompileException(emsg.toString());
    }
  }

  static int acceptXAccAttr(ParserB.Elem elem, int options, int defaultValue) throws CompileException {
    int a;
    String acc = elem.getAttrValue("acc");
    if (acc == null) {
      a = defaultValue;
    } else if (((options & Module.ACC_PUBLIC) > 0) && acc.equals(ACC_WORDX_PUBLIC)) {
      a = Module.ACC_PUBLIC;
    } else if (((options & Module.ACC_PROTECTED) > 0) && acc.equals(ACC_WORDX_PROTECTED)) {
      a = Module.ACC_PROTECTED;
    } else if (((options & Module.ACC_OPAQUE) > 0) && acc.equals(ACC_WORDX_OPAQUE)) {
      a = Module.ACC_OPAQUE;
    } else if (((options & Module.ACC_PRIVATE) > 0) && acc.equals(ACC_WORDX_PRIVATE)) {
      a = Module.ACC_PRIVATE;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid access descriptor at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(". - ");
      emsg.append(acc);
      throw new CompileException(emsg.toString());
    }
    return a;
  }

  static int acceptAvailability(ParserA.TokenReader reader) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    int av;
    if (a == null) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (a.value.token.equals(AVAILABILITY_WORD_GENERAL)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_GENERAL;
    } else if (a.value.token.equals(AVAILABILITY_WORD_ALPHA)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_ALPHA;
    } else if (a.value.token.equals(AVAILABILITY_WORD_BETA)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_BETA;
    } else if (a.value.token.equals(AVAILABILITY_WORD_LIMITED)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_LIMITED;
    } else if (a.value.token.equals(AVAILABILITY_WORD_DEPRECATED)) {
      reader.tokenConsumed();
      av = Module.AVAILABILITY_DEPRECATED;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid availability descriptor at ");
      emsg.append(reader.getCurrentSrcInfo());
      emsg.append(". - ");
      emsg.append(a.value.token);
      throw new CompileException(emsg.toString());
    }
    return av;
  }

  static int acceptXAvailabilityAttr(ParserB.Elem elem) throws CompileException {
    int av;
    String avail = elem.getAttrValue("availability");
    if (avail == null) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (avail.equals(AVAILABILITY_WORDX_GENERAL)) {
      av = Module.AVAILABILITY_GENERAL;
    } else if (avail.equals(AVAILABILITY_WORDX_ALPHA)) {
      av = Module.AVAILABILITY_ALPHA;
    } else if (avail.equals(AVAILABILITY_WORDX_BETA)) {
      av = Module.AVAILABILITY_BETA;
    } else if (avail.equals(AVAILABILITY_WORDX_LIMITED)) {
      av = Module.AVAILABILITY_LIMITED;
    } else if (avail.equals(AVAILABILITY_WORDX_DEPRECATED)) {
      av = Module.AVAILABILITY_DEPRECATED;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Invalid availability descriptor at ");
      emsg.append(elem.getSrcInfo().toString());
      emsg.append(". - ");
      emsg.append(avail);
      throw new CompileException(emsg.toString());
    }
    return av;
  }

  static ParserA.Token acceptWildCard(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return ParserA.acceptToken(reader, LToken.AST_AST, spc);
  }

  static ParserA.Token acceptWildCards(ParserA.TokenReader reader, int spc) throws CompileException, IOException {
    return ParserA.acceptToken(reader, LToken.AST_AST_AST, spc);
  }

  void addImportStmt(PImportStmt imp) throws CompileException {
    StringBuffer emsg;
    if (imp.modName.equals(this.name)) {
      emsg = new StringBuffer();
      emsg.append("Cannot import myself at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (imp.id.equals(this.myId)) {
      emsg = new StringBuffer();
      emsg.append("Imported module id \"");
      emsg.append(imp.id);
      emsg.append("\" conflicts with my module id at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (this.modDict.containsKey(imp.id)) {
      emsg = new StringBuffer();
      emsg.append("Id \"");
      emsg.append(imp.id);
      emsg.append("\" for imported module duplicate at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int importIndex = this.importStmtList.size();
    this.importStmtList.add(imp);  // multiple imports allowed
    this.modDict.put(imp.id, importIndex);
  }

  void addDataStmt(PDataStmt dat) throws CompileException {
    StringBuffer emsg;
    dat.setupScope(PScope.create(this)); dat.collectModRefs();
    if (this.tconDict.containsKey(dat.tcon)) {
      emsg = new StringBuffer();
      emsg.append("Type constructor \"");
      emsg.append(dat.tcon);
      emsg.append("\" duplicate at ");
      emsg.append(dat.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int datIndex = this.dataStmtList.size();
    this.dataStmtList.add(dat);
    PDefDict.TparamProps[] paramPropss;
    if (dat.tparams != null) {
      paramPropss = new PDefDict.TparamProps[dat.tparams.length];
      for (int i = 0; i < dat.tparams.length; i++) {
        paramPropss[i] = new PDefDict.TparamProps(dat.tparams[i].variance, dat.tparams[i].requiresConcrete);
      }
    } else {
      paramPropss = null;
    }
    this.tconDict.put(
      dat.tcon,
      PDefDict.TconProps.create(
        PTypeId.SUBCAT_DATA,
        paramPropss,
        dat.acc,
        DataDefGetter.createForDataDef(dat)));
    if (dat.constrs != null) {
      for (int i = 0; i < dat.constrs.length; i++) {
        PDataConstrDef constr = dat.constrs[i];
        if (this.eidDict.containsKey(constr.dcon)) {
          emsg = new StringBuffer();
          emsg.append("\"");
          emsg.append(constr.dcon);
          emsg.append("\" already defined at ");
          emsg.append(constr.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
        this.dconDict.put(constr.dcon, constr);
        this.eidDict.put(constr.dcon, PDefDict.EidProps.create(this.name, PExprId.CAT_DCON, dat.acc, this.createExprDefGetter(dat)));
      }
    }
    // /* DEBUG */ System.out.print("data stmt added: ");
    // /* DEBUG */ System.out.println(dat);
  }

  void addExtendStmt(PExtendStmt ext) throws CompileException {
    StringBuffer emsg;
    ext.setupScope(PScope.create(this)); ext.collectModRefs();
    if (this.tconDict.containsKey(ext.tcon)) {
      emsg = new StringBuffer();
      emsg.append("Type constructor \"");
      if (ext.baseMod != null) {
        emsg.append(ext.baseMod);
        emsg.append(".");
      }
      emsg.append(ext.tcon);
      emsg.append("\" duplicate at ");
      emsg.append(ext.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int extIndex = this.extendStmtList.size();
    this.extendStmtList.add(ext);
    PDefDict.TparamProps[] paramPropss;
    if (ext.tparams != null) {
      paramPropss = new PDefDict.TparamProps[ext.tparams.length];
      for (int i = 0; i < ext.tparams.length; i++) {
        paramPropss[i] = new PDefDict.TparamProps(ext.tparams[i].variance, ext.tparams[i].requiresConcrete);
      }
    } else {
      paramPropss = null;
    }
    this.tconDict.put(
      ext.tcon,
      PDefDict.TconProps.create(
        PTypeId.SUBCAT_EXTEND,
        paramPropss,
        ext.acc,
        DataDefGetter.createForDataDef(ext)));
    // /* DEBUG */ System.out.print("added to tcondict  "); System.out.println(ext.tcon);
    for (int i = 0; i < ext.constrs.length; i++) {
      PDataConstrDef constr = ext.constrs[i];
      if (this.eidDict.containsKey(constr.dcon)) {
        emsg = new StringBuffer();
        emsg.append("\"");
        emsg.append(constr.dcon);
        emsg.append("\" already defined at ");
        emsg.append(constr.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      this.dconDict.put(constr.dcon, constr);
      this.eidDict.put(constr.dcon, PDefDict.EidProps.create(this.name, PExprId.CAT_DCON, ext.acc, this.createExprDefGetter(ext)));
    }
    // /* DEBUG */ System.out.print("extend stmt added: ");
    // /* DEBUG */ System.out.println(ext);
  }

  void addAliasStmt(PAliasTypeStmt alias) throws CompileException {
    StringBuffer emsg;
    alias.setupScope(PScope.create(this)); alias.collectModRefs();
    if (this.tconDict.containsKey(alias.tcon)) {
      emsg = new StringBuffer();
      emsg.append("Type constructor \"");
      emsg.append(alias.tcon);
      emsg.append("\" duplicate at ");
      emsg.append(alias.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    int aliasIndex = this.aliasTypeStmtList.size();
    this.aliasTypeStmtList.add(alias);
    PDefDict.TparamProps[] paramPropss;
    if (alias.tparams != null) {
      paramPropss = new PDefDict.TparamProps[alias.tparams.length];
      for (int i = 0; i < alias.tparams.length; i++) {
        paramPropss[i] = new PDefDict.TparamProps(alias.tparams[i].variance, alias.tparams[i].requiresConcrete);
          // actually (invariant, false)
      }
    } else {
      paramPropss = null;
    }
    this.tconDict.put(
      alias.tcon,
      PDefDict.TconProps.create(
        PTypeId.SUBCAT_ALIAS,
        paramPropss,
        alias.acc,
        DataDefGetter.createForAliasTypeDef(alias)));
    // /* DEBUG */ System.out.print("alias stmt added: ");
    // /* DEBUG */ System.out.println(alias);
  }

  void addEvalStmt(PEvalStmt eval) throws CompileException {
    StringBuffer emsg;
    eval.setupScope(PScope.create(this)); eval.collectModRefs();
    String official = eval.official;
    String[] aliases = eval.aliases;
    if (this.funOfficialDict.containsKey(official)) {
      emsg = new StringBuffer();
      emsg.append("\"");
      emsg.append(official);
      emsg.append("\" already defined at ");
      emsg.append(eval.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (this.eidDict.containsKey(official)) {
      this.mergeFunToEidDict(official, PExprId.CAT_FUN_OFFICIAL, eval.acc);
    } else {
      this.eidDict.put(official, PDefDict.EidProps.create(this.name, PExprId.CAT_FUN_OFFICIAL, eval.acc, this.createExprDefGetter(official)));
    }
    Integer evalIndex = this.evalStmtList.size();
    this.funOfficialDict.put(official, evalIndex);
    if (!this.funDict.containsKey(official)) {
      this.funDict.put(official, new ArrayList<Integer>());
    }
    this.funDict.get(official).add(evalIndex);
    Set<String> names = new HashSet<String>();
    names.add(official);
    for (int i = 0; i < aliases.length; i++) {
      String alias = aliases[i];
      if (names.contains(alias)) {
        emsg = new StringBuffer();
        emsg.append("Function alias \"");
        emsg.append(alias);
        emsg.append("\" duplicate at ");
        emsg.append(eval.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (this.dconDict.containsKey(alias)) {
        emsg = new StringBuffer();
        emsg.append("Function name \"");
        emsg.append(official);
        emsg.append("\" conflicts with data constructor at ");
        emsg.append(eval.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      if (!this.funDict.containsKey(alias)) {
        this.funDict.put(alias, new ArrayList<Integer>());
      }
      this.funDict.get(alias).add(evalIndex);
      if (this.eidDict.containsKey(alias)) {
        this.mergeFunToEidDict(official, PExprId.CAT_FUN_ALIAS, eval.acc);
      } else {
        this.eidDict.put(alias, PDefDict.EidProps.create(this.name, PExprId.CAT_FUN_ALIAS, eval.acc, this.createExprDefGetter(alias)));
      }
      names.add(alias);
    }
    this.evalStmtList.add(eval);
    // /* DEBUG */ System.out.print("eval stmt added: ");
    // /* DEBUG */ System.out.print(eval.official);
    // /* DEBUG */ System.out.print(" - ");
    // /* DEBUG */ System.out.println(eval);
  }

  private void mergeFunToEidDict(String name, int cat, int acc) {
    PDefDict.EidProps p = this.eidDict.get(name);
    p.cat |= cat;
    p.acc |= acc;
  }

  private void generateNameFun() throws CompileException {
    // eval _name_ @public -> <cstr> @native
    Parser.SrcInfo si = new Parser.SrcInfo(this.name, ":name");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance();
    evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setOfficial(Module.FUN_NAME);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    retTypeBuilder.addItem(PTypeId.create(si, MOD_ID_LANG, "cstr", false));
    evalStmtBuilder.setRetDef(PRetDef.create(retTypeBuilder.create()));
    this.addEvalStmt(evalStmtBuilder.create());
  }

  private void generateInitdFun() throws CompileException {
    // eval _initd_ -> <_init_'s ret type> @native
    PEvalStmt eval;
    if ((eval = this.getInitFunDef()) == null) { return; }
    Parser.SrcInfo si = eval.srcInfo.appendPostfix("_initd");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance();
    evalStmtBuilder.setSrcInfo(si);
    evalStmtBuilder.setOfficial(Module.FUN_INITD);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PType.Builder retTypeBuilder = PType.Builder.newInstance();
    evalStmtBuilder.setRetDef(PRetDef.create(eval.retDef.type.deepCopy(
      si, PTypeDesc.COPY_EXT_KEEP, PTypeDesc.COPY_VARIANCE_INVARIANT, PTypeDesc.COPY_CONCRETE_OFF)));
    this.addEvalStmt(evalStmtBuilder.create());
  }

  PEvalStmt getInitFunDef() {
    PEvalStmt eval = null;
    if (this.funOfficialDict.containsKey(Module.FUN_INIT)) {
      PEvalStmt e = this.evalStmtList.get(this.funOfficialDict.get(Module.FUN_INIT));
      eval = (e.params.length == 0)? e: null;
    }
    return eval;
  }

  boolean isInitFunDefined() {
    return this.getInitFunDef() != null;
  }

  private void generateDataFuns() throws CompileException {
    StringBuffer emsg;
    List<PEvalStmt> es;
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      es = this.dataStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      es = this.extendStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
  }

  static String[] generateInFunNames(String tcon) {
    return new String[] { "_in_" + tcon + "?", tcon + "?" };
  }

  static String[] generateNarrowFunNames(String tcon) {
    return new String[] { "_narrow_" + tcon, "narrow" };
  }

  static String[] generateAttrFunNames(String tcon, String attrName) {
    return new String[] { "_attr_" + tcon + "_" + attrName, attrName };
  }

  static String[] generateMaybeAttrFunNames(String tcon, String attrName) {
    return new String[] { "_maybe_attr_" + tcon + "_" + attrName, "maybe_" + attrName };
  }

  static String[] generateIds(String prefix, int count) {
    String[] ids = new String[count];
    for (int i = 0; i < count; i++) {
      ids[i] = prefix + i;
    }
    return ids;
  }

  public void setupScope(PScope scope) {
    throw new RuntimeException("PModule#setupScope() called. - " + this.toString());
  }

  public void collectModRefs() throws CompileException {
    throw new RuntimeException("PModule#collectModRefs() called. - " + this.toString());
  }

  public PModule resolve() throws CompileException {
    if (this.idResolved) { return this; }
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.set(i, this.dataStmtList.get(i).resolve());
      // /* DEBUG */ System.out.print("id category resolved - ");
      // /* DEBUG */ System.out.print(this.dataStmtList.get(i).tcon);
      // /* DEBUG */ System.out.print(" ");
      // /* DEBUG */ System.out.println(this.dataStmtList.get(i));
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.set(i, this.extendStmtList.get(i).resolve());
      // /* DEBUG */ System.out.print("id category resolved - ");
      // /* DEBUG */ System.out.print(this.extendStmtList.get(i).baseOmod);
      // /* DEBUG */ System.out.print("/");
      // /* DEBUG */ System.out.print(this.extendStmtList.get(i).baseMod);
      // /* DEBUG */ System.out.print(".");
      // /* DEBUG */ System.out.print(this.extendStmtList.get(i).tcon);
      // /* DEBUG */ System.out.print(" ");
      // /* DEBUG */ System.out.println(this.extendStmtList.get(i));
    }
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.set(i, this.aliasTypeStmtList.get(i).resolve());
      // /* DEBUG */ System.out.print("id category resolved - ");
      // /* DEBUG */ System.out.print(this.aliasTypeStmtList.get(i).tcon);
      // /* DEBUG */ System.out.print(" ");
      // /* DEBUG */ System.out.println(this.aliasTypeStmtList.get(i));
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.set(i, this.evalStmtList.get(i).resolve());
      // /* DEBUG */ System.out.print("id category resolved - ");
      // /* DEBUG */ System.out.print(this.evalStmtList.get(i).official);
      // /* DEBUG */ System.out.print(" ");
      // /* DEBUG */ System.out.println(this.evalStmtList.get(i));
    }
    this.idResolved = true;
    return this;
  }

  PDefDict.EidProps resolveEid(PExprId id) throws CompileException {
    // variable is already converted to PVarRef
    if ((id.catOpt & PExprId.CAT_VAR) > 0) {
      throw new IllegalArgumentException("invalid cat of id - " + id.toString());
    }
    return this.isLang()? this.resolveEidInLang(id): this.resolveEidInOther(id);
  }

  PDefDict.EidProps resolveEidInLang(PExprId id) {
    if (id.mod == null || id.mod.equals(this.myId) || id.mod.equals(MOD_ID_HERE) || id.mod.equals(MOD_ID_LANG)) {
      ;
    } else {
      throw new IllegalArgumentException("mod invalid.");
    }
    return this.resolveEidLocal(id.name, id.catOpt);
  }

  PDefDict.EidProps resolveEidInOther(PExprId id) throws CompileException {
    PDefDict.EidProps props;
    if (id.mod == null) {
      if ((props = this.resolveEidLocal(id.name, id.catOpt)) == null) {
        props = this.foreignIdResolver.resolveEid(MOD_ID_LANG, id.name, id.catOpt);
      } else if (id.mod == null) {
        props.defGetter.setSearchInLang();
      }
    } else if (id.mod.equals(this.myId) || id.mod.equals(MOD_ID_HERE)) {
      props = this.resolveEidLocal(id.name, id.catOpt);
    } else {
      props = this.foreignIdResolver.resolveEid(id.mod, id.name, id.catOpt);
    }
    return props;
  }

  PDefDict.EidProps resolveEidLocal(String name, int catOpts) {
    return this.resolveEid(
      name,
      catOpts,
      Module.ACC_PUBLIC + Module.ACC_PRIVATE
        + (((catOpts & PExprId.CAT_DCON) > 0)? Module.ACC_PROTECTED + Module.ACC_OPAQUE: 0));
  }

  PDefDict.TconInfo resolveTcon(String mod, String tcon) throws CompileException {
    return this.isLang()?  this.resolveTconInLang(mod, tcon): this.resolveTconInOther(mod, tcon);
  }

  private PDefDict.TconInfo resolveTconInLang(String mod, String tcon) {
    if (mod == null || mod.equals(this.myId) || mod.equals(MOD_ID_HERE) || mod.equals(MOD_ID_LANG)) {
      ;
    } else {
      throw new IllegalArgumentException("mod invalid.");
    }
    return this.resolveTcon(
      tcon,
      PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
      Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
  }

  private PDefDict.TconInfo resolveTconInOther(String mod, String tcon) throws CompileException {
    PDefDict.TconInfo ti = null;
    if (mod == null) {
      if ((ti = this.resolveTconLocal(tcon)) != null) {
        ;
      } else if ((ti = this.foreignIdResolver.resolveTcon(MOD_ID_LANG, tcon)) != null) {
        ;
      }
    } else if (mod.equals(this.myId) || mod.equals(MOD_ID_HERE)) {
      ti = this.resolveTconLocal(tcon);
    } else {
      ti = this.foreignIdResolver.resolveTcon(mod, tcon);
    }
    return ti;
  }

  private PDefDict.TconInfo resolveTconLocal(String tcon) {
    return this.resolveTcon(
      tcon,
      PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
      Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE + Module.ACC_PRIVATE);
  }

  void addReferredTcon(PDefDict.TconInfo ti) {
    this.foreignIdResolver.referredTcon(ti.key.modName, ti.key.tcon, ti.props);
  }

  public int getModAvailability() { return this.availability; }

  public Cstr[] getForeignMods() {
    Cstr[] ms;
    if (this.isLang()) {
      ms = new Cstr[0];
    } else {
      ms = new Cstr[1 + this.farModList.size()];
      ms[0] = Module.MOD_LANG;
      for (int i = 1, j = 0; j < this.farModList.size(); i++, j++) {
        ms[i] = this.farModList.get(j);
      }
    }
    return ms;
  }

  public PDefDict.EidProps resolveEid(String id, int catOpts, int accOpts) {
    PDefDict.EidProps props = this.eidDict.get(id);
    return (props != null && (props.cat & catOpts) > 0 && (props.acc & accOpts) > 0)? props: null;
  }

  public PDefDict.TconInfo resolveTcon(String tcon, int subcatOpts, int accOpts) {
    PDefDict.TconProps tp;
    return ((tp = this.tconDict.get(tcon)) != null && (tp.subcat & subcatOpts) > 0 && (tp.acc & accOpts) > 0)?
      PDefDict.TconInfo.create(PDefDict.TconKey.create(this.name, tcon), tp): null;
  }

  ExprDefGetter createExprDefGetter(PDataDef dataDef) {
    return new ExprDefGetter(dataDef, null);
  }

  ExprDefGetter createExprDefGetter(String funName) {
    return new ExprDefGetter(null, funName);
  }

  class ExprDefGetter implements PDefDict.ExprDefGetter {
    PDataDef dataDef;
    String funName;
    boolean searchInLang;

    private ExprDefGetter(PDataDef dataDef, String funName) {
      this.dataDef = dataDef;
      this.funName = funName;
    }

    public void setSearchInLang() {
      this.searchInLang = true;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PDefDict.FunSelRes selectFunDef(PTypeSkel[] paramTypes, List<PTVarSlot> givenTVarList) throws CompileException {
      PDefDict.FunSelRes r = null;
      if (this.funName == null) {
        ;
      } else if ((r = PModule.this.selectFun(this.funName, paramTypes, givenTVarList)) != null) {
        ;
      } else if (this.searchInLang) {
        PDefDict.EidProps p = PModule.this.theCompiler.getReferredDefDict(Module.MOD_LANG).resolveEid(
          this.funName, PExprId.CAT_FUN, Module.ACC_PUBLIC);
        if (p != null) {
          r = p.defGetter.selectFunDef(paramTypes, givenTVarList);
        }
      }
      if (r != null) {
// /* DEBUG */ System.out.println("selected " + r.funDef.getModName().toJavaString() + "." + r.funDef.getOfficialName() + " at " + PModule.this.name.toJavaString());
        Cstr modName = r.funDef.getModName();
        if (!modName.equals(PModule.this.name)) {
          PModule.this.foreignIdResolver.referredFunOfficial(r.funDef);
        }
      }
      return r;
    }

    public PFunDef getFunDef() throws CompileException { // get by official name
      PFunDef d = null;
      if (this.funName == null) {
        ;
      } else if ((d = PModule.this.getFun(this.funName)) != null) {
        ;
      } else if (this.searchInLang) {
        PDefDict.EidProps p = PModule.this.theCompiler.getReferredDefDict(Module.MOD_LANG).resolveEid(
          this.funName, PExprId.CAT_FUN_OFFICIAL, Module.ACC_PUBLIC);
        if (p != null) {
          d = p.defGetter.getFunDef();
        }
      }
      if (d != null) {
// /* DEBUG */ System.out.println("get by official " + d.getModName().toJavaString() + "." + d.getOfficialName() + " at " + PModule.this.name.toJavaString());
        Cstr modName = d.getModName();
        if (!modName.equals(PModule.this.name)) {
          PModule.this.foreignIdResolver.referredFunOfficial(d);
        }
      }
      return d;
    }
  }

  static class DataDefGetter implements PDefDict.DataDefGetter {
    PDataDef dataDef;
    PAliasTypeDef aliasTypeDef;

    static DataDefGetter createForDataDef(PDataDef dataDef) {
      return new DataDefGetter(dataDef, null);
    }

    static DataDefGetter createForAliasTypeDef(PAliasTypeDef aliasTypeDef) {
      return new DataDefGetter(null, aliasTypeDef);
    }

    private DataDefGetter(PDataDef dataDef, PAliasTypeDef aliasTypeDef) {
      this.dataDef = dataDef;
      this.aliasTypeDef = aliasTypeDef;
    }

    public PDataDef getDataDef() { return this.dataDef; }

    public PAliasTypeDef getAliasTypeDef() { return this.aliasTypeDef; }
  }

  PDefDict.FunSelRes selectFun(String name, PTypeSkel[] paramTypes, List<PTVarSlot> givenTVarList) throws CompileException {
    List<Integer> indices = this.funDict.get(name);
    PDefDict.FunSelRes sel = null;
    if (indices == null) { return null; }
    for (int i = 0; sel == null && i < indices.size(); i++) {
      PFunDef fd = this.evalStmtList.get(indices.get(i));
      PTypeSkel[] pts = fd.getParamTypes();
      if (pts.length != paramTypes.length) { continue; }
      PTypeSkelBindings b = PTypeSkelBindings.create(givenTVarList);
      for (int j = 0; b != null && j < pts.length; j++) {
        b = pts[j].accept(PTypeSkel.NARROWER, true, paramTypes[j], b);
      }
      if (b != null) {
        sel = PDefDict.FunSelRes.create(fd, b);
      }
    }
    return sel;
  }

  PFunDef getFun(String official) {
    Integer index = this.funOfficialDict.get(official);
    if (index == null) { return null; }
    return this.evalStmtList.get(index);
  }

  void checkAccInDefs() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.get(i).checkAcc();
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).checkAcc();
    }
  }

  void  checkCyclicAlias() throws CompileException {
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.get(i).checkCyclicAlias();
    }
  }

  public void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).normalizeTypes();
    }
  }

  void setupExtensionGraph(PDefDict.ExtGraph g) throws CompileException {
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).setupExtensionGraph(g);
    }
  }

  void checkConcreteness() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkConcreteness();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkConcreteness();
    }
  }

  void makeSureTypeConsistency() throws CompileException {
    PTypeGraph g = PTypeGraph.create(this.theCompiler, this);
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).setupTypeGraph(g);
    }
    g.inferAll();
  }

  int modNameToModRefIndex(Cstr modName) {
    int index;
    if (modName.equals(this.name)) {
      index = Module.MOD_INDEX_SELF;
    } else if (modName.equals(Module.MOD_LANG)) {
      index = Module.MOD_INDEX_LANG;
    } else {
      int i = this.farModList.indexOf(modName);
      if (i < 0) {
        throw new IllegalArgumentException("Unknown module name: " + modName);
      }
      index = 2 + i;
    }
    return index;
  }

  class ForeignIdResolver {
    Map<Cstr, Map<String, ForeignDataDef>> dataDefDictDict;
    Map<Cstr, Map<String, PAliasTypeDef>> aliasDefDictDict;
    Map<Cstr, Map<String, PFunDef>> funDefDictDict;

    ForeignIdResolver() {
      this.dataDefDictDict = new HashMap<Cstr, Map<String, ForeignDataDef>>();
      this.aliasDefDictDict = new HashMap<Cstr, Map<String, PAliasTypeDef>>();
      this.funDefDictDict = new HashMap<Cstr, Map<String, PFunDef>>();
    }

    PDefDict.EidProps resolveEid(String mod, String name, int catOpts) throws CompileException {
      PDefDict.EidProps ep = PModule.this.theCompiler.getReferredDefDict(PModule.this.resolveModId(mod)).resolveEid(
        name,
        catOpts,
        Module.ACC_PUBLIC
          + (((catOpts & PExprId.CAT_DCON_PTN) > 0)? Module.ACC_PROTECTED: 0));
      if (ep != null) {
        this.referredEid(ep.modName, name, catOpts, ep);
      }
      return ep;
    }

    PDefDict.TconInfo resolveTcon(String mod, String tcon) throws CompileException {
      Cstr modName = PModule.this.resolveModId(mod);
      PDefDict.TconInfo ti = PModule.this.theCompiler.getReferredDefDict(modName).resolveTcon(
        tcon,
        PTypeId.SUBCAT_DATA + PTypeId.SUBCAT_EXTEND + PTypeId.SUBCAT_ALIAS,
        Module.ACC_PUBLIC + Module.ACC_PROTECTED + Module.ACC_OPAQUE);
      if (ti != null) {
        this.referredTcon(modName, tcon, ti.props);
      }
      return ti;
    }

    void referredTcon(Cstr modName, String tcon, PDefDict.TconProps tp) {
// /* DEBUG */ System.out.print("FIR tcon "); 
// /* DEBUG */ System.out.print(modName.toJavaString()); 
// /* DEBUG */ System.out.print(" "); 
// /* DEBUG */ System.out.print(tcon); 
// /* DEBUG */ System.out.print(" "); 
// /* DEBUG */ System.out.println(tp); 
      switch (tp.subcat) {
      case PTypeId.SUBCAT_ALIAS:
      // /* DEBUG */ System.out.println(" >> ALIAS");
        PAliasTypeDef ad = tp.defGetter.getAliasTypeDef();
        if (this.aliasDefDictDict.containsKey(modName)) {
          Map<String, PAliasTypeDef> m = this.aliasDefDictDict.get(modName);
          if (m.containsKey(ad.getTcon())) {
      // /* DEBUG */ System.out.println(" >> ALIAS >> already registered " + ad.getTcon());
          } else {
      // /* DEBUG */ System.out.println(" >> ALIAS >> new alias_def " + ad.getTcon());
            m.put(ad.getTcon(), ad);
          }
        } else {
      // /* DEBUG */ System.out.println(" >> ALIAS >> new module " + ad.getTcon());
          Map<String, PAliasTypeDef> m = new HashMap<String, PAliasTypeDef>();
          m.put(ad.getTcon(), ad);
          this.aliasDefDictDict.put(modName, m);
        }
        break;
      default:
      // /* DEBUG */ System.out.println(" >> DATA");
        PDataDef dd = tp.defGetter.getDataDef();
        if (this.dataDefDictDict.containsKey(modName)) {
          Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          if (m.containsKey(dd.getFormalTcon())) {
      // /* DEBUG */ System.out.println(" >> DATA >> ++ " + dd.getFormalTcon());
            ForeignDataDef fdd = m.get(dd.getFormalTcon());
            fdd.requireAcc(Module.ACC_OPAQUE);
          } else {
      // /* DEBUG */ System.out.println(" >> DATA >> new data_def " + dd.getFormalTcon());
            ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_OPAQUE);
            m.put(dd.getFormalTcon(), fdd);
          }
        } else {
      // /* DEBUG */ System.out.println(" >> DATA >> new module " + dd.getFormalTcon());
          Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_OPAQUE);
          m.put(dd.getFormalTcon(), fdd);
          this.dataDefDictDict.put(modName, m);
        }
        break;
      }
    }

    void referredEid(Cstr modName, String id, int catOpts, PDefDict.EidProps ep) throws CompileException {
// /* DEBUG */ System.out.print("FIR eid "); 
// /* DEBUG */ System.out.print(modName.toJavaString()); 
// /* DEBUG */ System.out.print(" "); 
// /* DEBUG */ System.out.print(id); 
// /* DEBUG */ System.out.print(" "); 
// /* DEBUG */ System.out.print(catOpts); 
// /* DEBUG */ System.out.print(" "); 
// /* DEBUG */ System.out.print(ep); 
      PDataDef dd;
      PFunDef fd;
      switch (catOpts & ep.cat) {
      case PExprId.CAT_DCON_EVAL:
      // /* DEBUG */ System.out.println(" >> DCON_EVAL");
        dd = ep.defGetter.getDataDef();
        if (this.dataDefDictDict.containsKey(modName)) {
          Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          if (m.containsKey(dd.getFormalTcon())) {
      // /* DEBUG */ System.out.println(" >> DCON_EVAL >> ++ " + dd.getFormalTcon());
            ForeignDataDef fdd = m.get(dd.getFormalTcon());
            fdd.referredDcon(id);
            fdd.requireAcc(Module.ACC_PUBLIC);
          } else {
      // /* DEBUG */ System.out.println(" >> DCON_EVAL >> new data_def " + dd.getFormalTcon());
            ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PUBLIC);
            fdd.referredDcon(id);
            m.put(dd.getFormalTcon(), fdd);
          }
        } else {
      // /* DEBUG */ System.out.println(" >> DCON_EVAL >> new module " + dd.getFormalTcon());
          Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PUBLIC);
          fdd.referredDcon(id);
          m.put(dd.getFormalTcon(), fdd);
          this.dataDefDictDict.put(modName, m);
        }
        break;
      case PExprId.CAT_DCON_PTN:
      // /* DEBUG */ System.out.println(" >> DCON_PTN");
        dd = ep.defGetter.getDataDef();
        if (this.dataDefDictDict.containsKey(modName)) {
          Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
          if (m.containsKey(dd.getFormalTcon())) {
      // /* DEBUG */ System.out.println(" >> DCON_PTN >> ++ " + dd.getFormalTcon());
            ForeignDataDef fdd = m.get(dd.getFormalTcon());
            fdd.referredDcon(id);
            fdd.requireAcc(Module.ACC_PROTECTED);
          } else {
      // /* DEBUG */ System.out.println(" >> DCON_PTN >> new data_def " + dd.getFormalTcon());
            ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PROTECTED);
            fdd.referredDcon(id);
            m.put(dd.getFormalTcon(), fdd);
          }
        } else {
      // /* DEBUG */ System.out.println(" >> DCON_PTN >> new module " + dd.getFormalTcon());
          Map<String, ForeignDataDef> m = new HashMap<String, ForeignDataDef>();
          ForeignDataDef fdd = new ForeignDataDef(dd, Module.ACC_PROTECTED);
          fdd.referredDcon(id);
          m.put(dd.getFormalTcon(), fdd);
          this.dataDefDictDict.put(modName, m);
        }
        break;
      case PExprId.CAT_FUN_OFFICIAL:
      // /* DEBUG */ System.out.println(" >> FUN official " + id);
        this.referredFunOfficial(ep.defGetter.getFunDef());
        break;
      // case PExprId.CAT_FUN_ALIAS:
      // /* DEBUG */ System.out.println(" >> FUN alias " + id);
        // break;
      default:
      // /* DEBUG */ System.out.println(" >> EID_OTHER " + id + Integer.toString(catOpts) + " " + Integer.toString(ep.cat));
        break;
      }
    }

    void referredFunOfficial(PFunDef fd) {
      Cstr modName = fd.getModName();
      String official = fd.getOfficialName();
// /* DEBUG */ System.out.println("official " + official);
      Map<String, PFunDef> m;
      if ((m = this.funDefDictDict.get(modName)) == null) {
        m = new HashMap<String, PFunDef>();
        m.put(official, fd);
        this.funDefDictDict.put(modName, m);
      } else if (!m.containsKey(official)) {
      m.put(official, fd);
      }
    }

    PDataDef[] getReferredDataDefsIn(Cstr modName) {
      PDataDef[] dds;
      if (this.dataDefDictDict.containsKey(modName)) {
        Map<String, ForeignDataDef> m = this.dataDefDictDict.get(modName);
        Set<String> s = m.keySet();
        dds = new PDataDef[s.size()];
        Iterator<String> iter = s.iterator();
        for (int i = 0; iter.hasNext(); i++) {
          dds[i] = m.get(iter.next());
        }
      } else {
        dds = new PDataDef[0];
      }
      return dds;
    }

    PAliasTypeDef[] getReferredAliasTypeDefsIn(Cstr modName) {
      PAliasTypeDef[] ads;
      if (this.aliasDefDictDict.containsKey(modName)) {
        Map<String, PAliasTypeDef> m = this.aliasDefDictDict.get(modName);
        Set<String> s = m.keySet();
        ads = new PAliasTypeDef[s.size()];
        Iterator<String> iter = s.iterator();
        for (int i = 0; iter.hasNext(); i++) {
          ads[i] = m.get(iter.next());
        }
      } else {
        ads = new PAliasTypeDef[0];
      }
      return ads;
    }

    PFunDef[] getReferredFunDefsIn(Cstr modName) {
// /* DEBUG */ System.out.println("getReferredFunDefsIn");
      PFunDef[] fds;
      if (this.funDefDictDict.containsKey(modName)) {
        Map<String, PFunDef> m = this.funDefDictDict.get(modName);
        Set<String> s = m.keySet();
        fds = new PFunDef[s.size()];
        Iterator<String> iter = s.iterator();
        for (int i = 0; iter.hasNext(); i++) {
          fds[i] = m.get(iter.next());
        }
      } else {
        fds = new PFunDef[0];
      }
      return fds;
    }
  }

  class ForeignTconRef {
    int paramCount;
    int acc;
    PDataDef dataDef;


    ForeignTconRef(PDefDict.TconProps props, int acc) {
      this.paramCount = props.paramCount();
      this.acc = acc;
      this.dataDef = props.defGetter.getDataDef();
    }

    void mergeAcc(int acc) {
      this.acc |= acc;
    }

    int getRequiredAcc() {
      int a;
      if ((this.acc & Module.ACC_PUBLIC) > 0) {
        a = Module.ACC_PUBLIC;
      } else if ((this.acc & Module.ACC_PROTECTED) > 0) {
        a = Module.ACC_PROTECTED;
      } else if ((this.acc & Module.ACC_OPAQUE) > 0) {
        a = Module.ACC_OPAQUE;
      } else {
        throw new IllegalStateException("Invalid acc. - " + this.acc);
      }
      return a;
    }
  }

  class ForeignDataDef implements PDataDef {
    PDataDef referredDataDef;
    int requiredAcc;
    List<String> referredDconList;

    ForeignDataDef(PDataDef dd, int acc) {
      this.referredDataDef = dd;
      this.requiredAcc = acc;
      this.referredDconList = new ArrayList<String>();
    }

    void requireAcc(int acc) {
      this.requiredAcc |= acc;
    }

    void referredDcon(String dcon) {
      if (!this.referredDconList.contains(dcon)) {
        this.referredDconList.add(dcon);
      }
    }

    public String getFormalTcon() { return this.referredDataDef.getFormalTcon(); }

    public int getParamCount() { return this.referredDataDef.getParamCount(); }

    // public PTVarSlot[] getParamVarSlots() { return this.referredDataDef.getParamVarSlots(); }

    public PTypeSkel getTypeSig() { return this.referredDataDef.getTypeSig(); }

    public int getAvailability() { return this.referredDataDef.getAvailability(); }

    public int getAcc() {
      int acc;
      if ((this.requiredAcc & Module.ACC_PUBLIC) > 0) {
        acc = Module.ACC_PUBLIC;
      } else if ((this.requiredAcc & Module.ACC_PROTECTED) > 0) {
        acc = Module.ACC_PROTECTED;
      } else {
        acc = Module.ACC_OPAQUE;
      }
      return acc;
    }

    public int getConstrCount() { return this.referredDconList.size(); }

    public PDataDef.Constr getConstr(String dcon) {
      return this.referredDconList.contains(dcon)? this.referredDataDef.getConstr(dcon): null;
    }

    public PDataDef.Constr getConstrAt(int index) {
      return this.referredDataDef.getConstr(this.referredDconList.get(index));
    }

    public PDefDict.TconKey getBaseTconKey() { return this.referredDataDef.getBaseTconKey(); }
  }
}
