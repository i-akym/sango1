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

class PModule implements PModDecl {
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
  static final Option.Set<Module.Access> ACC_OPTS_FOR_EVAL
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_DATA
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED)
    .add(Module.ACC_OPAQUE).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_EXTEND
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PROTECTED)
    .add(Module.ACC_OPAQUE).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_ALIAS
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Option.Set<Module.Access> ACC_OPTS_FOR_FEATURE
    = (new Option.Set<Module.Access>())
    .add(Module.ACC_PUBLIC).add(Module.ACC_PRIVATE);
  static final Module.Access ACC_DEFAULT_FOR_EVAL = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_DATA = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_EXTEND = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_ALIAS = Module.ACC_PRIVATE;
  static final Module.Access ACC_DEFAULT_FOR_FEATURE = Module.ACC_PRIVATE;

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
  Parser.SrcInfo srcInfo;
  PScope scope;
  Parser.SrcInfo modDefSrcInfo;
  Module.Availability availability;
  Cstr definedName;  // maybe null
  Cstr actualName;
  String myId;
  Map<String, Cstr> modTab;  // mod id -> mod name
  List<String> referredModIds;  // except @LANG, @HERE, my mod id
  List<Cstr> referredFarMods;  // except sango.lang
  // List<Cstr> referredFarMods2;  // refs from foreign definition
  List<PImportStmt> importStmtList;
  List<PDataStmt> dataStmtList;
  List<PExtendStmt> extendStmtList;
  List<PAliasTypeStmt> aliasTypeStmtList;
  List<PFeatureStmt> featureStmtList;
  List<PEvalStmt> evalStmtList;
  int idSuffix;

  private PModule(Compiler theCompiler, Parser.SrcInfo srcInfo) {
    this.theCompiler = theCompiler;
    this.srcInfo = srcInfo;
    this.scope = PScope.create(this);  // hmmm, not elegant
    this.availability = Module.AVAILABILITY_GENERAL;  // default
    this.modTab = new HashMap<String, Cstr>();
    this.referredModIds = new ArrayList<String>();
    this.referredFarMods = new ArrayList<Cstr>();
    // this.referredFarMods2 = new ArrayList<Cstr>();
    this.importStmtList = new ArrayList<PImportStmt>();
    this.dataStmtList = new ArrayList<PDataStmt>();
    this.extendStmtList = new ArrayList<PExtendStmt>();
    this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
    this.featureStmtList = new ArrayList<PFeatureStmt>();
    this.evalStmtList = new ArrayList<PEvalStmt>();
  }

  public String toString() {
    StringBuffer b = new StringBuffer();
    b.append("module[src=");
    b.append(this.modDefSrcInfo);
    b.append(",name=");
    b.append(this.actualName);
    if (this.myId != null) {
      b.append(",id=");
      b.append(this.myId);
    }
    b.append("]");
    return b.toString();
  }

  static PModule accept(Compiler theCompiler, ParserA.TokenReader reader, Cstr modName) throws CompileException, IOException {
    StringBuffer emsg;
    Builder builder = Builder.newInstance(theCompiler, reader.getCurrentSrcInfo(), modName);
    acceptModuleStmt(reader, builder);
    if (modName.equals(Module.MOD_LANG)) {
      Parser.SrcInfo si = new Parser.SrcInfo(Module.MOD_LANG, "builtin");
      builder.addDataStmt(
        PDataStmt.createForVariableParams(si, builder.getScope(), Module.TCON_TUPLE, Module.ACC_OPAQUE));
      builder.addDataStmt(
        PDataStmt.createForVariableParams(si, builder.getScope(), Module.TCON_FUN, Module.ACC_OPAQUE));
    }
    acceptDefStmts(reader, builder);
    PModule mod = builder.create();
    mod.generateNameFun();
    mod.generateInitdFun();
    mod.generateFeatureAliases();
    mod.generateFeatureFuns();
    mod.generateDataFuns();
    return mod;
  }

  static PModule acceptX(Compiler theCompiler, ParserB.Elem elem, Cstr modName) throws CompileException {
    StringBuffer emsg;
    if (elem == null) {
      throw new CompileException("No module definition.");
    }
    if (!elem.getName().equals("module")) {
      throw new CompileException("No module definition.");
    }

    Builder builder = Builder.newInstance(theCompiler, elem.getSrcInfo(), modName);
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
    mod.generateFeatureAliases();
    mod.generateFeatureFuns();
    mod.generateDataFuns();
    return mod;
  }

  static void acceptXDef(ParserB.Elem elem, Builder builder) throws CompileException {
    PImportStmt imp;
    PDataStmt dat;
    PExtendStmt ext;
    PAliasTypeStmt alias;
    PEvalStmt eval;
    if ((imp = PImportStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addImportStmt(imp);
    } else if ((dat = PDataStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addDataStmt(dat);
    } else if ((ext = PExtendStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addExtendStmt(ext);
    } else if ((alias = PAliasTypeStmt.acceptX(elem, builder.getScope())) != null) {
      builder.addAliasTypeStmt(alias);
    } else if ((eval = PEvalStmt.acceptX(elem, builder.getScope())) != null) {
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
    PScope scope = builder.getScope();
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
      PEid id;
      if ((id = PEid.accept(reader, scope, Parser.QUAL_MAYBE, ParserA.SPACE_DO_NOT_CARE)) == null) {
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
    PFeatureStmt feat;
    PEvalStmt eval;
    while (!t.isEOF()) {
      if ((imp = PImportStmt.accept(reader, builder.getScope())) != null) {
        builder.addImportStmt(imp);
      } else if ((dat = PDataStmt.accept(reader, builder.getScope())) != null) {
        builder.addDataStmt(dat);
      } else if ((ext = PExtendStmt.accept(reader, builder.getScope())) != null) {
        builder.addExtendStmt(ext);
      } else if ((alias = PAliasTypeStmt.accept(reader, builder.getScope())) != null) {
        builder.addAliasTypeStmt(alias);
      } else if ((feat = PFeatureStmt.accept(reader, builder.getScope())) != null) {
        builder.addFeatureStmt(feat);
      } else if ((eval = PEvalStmt.accept(reader, builder.getScope())) != null) {
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

  static Module.Access acceptAcc(ParserA.TokenReader reader, Option.Set<Module.Access> options, Module.Access defaultValue) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    if (a == null) {
      return defaultValue;
    } else if (options.contains(Module.ACC_PUBLIC) && a.value.token.equals(ACC_WORD_PUBLIC)) {
      reader.tokenConsumed();
      return Module.ACC_PUBLIC;
    } else if (options.contains(Module.ACC_PROTECTED) && a.value.token.equals(ACC_WORD_PROTECTED)) {
      reader.tokenConsumed();
      return Module.ACC_PROTECTED;
    } else if (options.contains(Module.ACC_OPAQUE) && a.value.token.equals(ACC_WORD_OPAQUE)) {
      reader.tokenConsumed();
      return Module.ACC_OPAQUE;
    } else if (options.contains(Module.ACC_PRIVATE) && a.value.token.equals(ACC_WORD_PRIVATE)) {
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

  static Module.Access acceptXAccAttr(ParserB.Elem elem, Option.Set<Module.Access> options, Module.Access defaultValue) throws CompileException {
    Module.Access a;
    String acc = elem.getAttrValue("acc");
    if (acc == null) {
      a = defaultValue;
    } else if (options.contains(Module.ACC_PUBLIC) && acc.equals(ACC_WORDX_PUBLIC)) {
      a = Module.ACC_PUBLIC;
    } else if (options.contains(Module.ACC_PROTECTED) && acc.equals(ACC_WORDX_PROTECTED)) {
      a = Module.ACC_PROTECTED;
    } else if (options.contains(Module.ACC_OPAQUE) && acc.equals(ACC_WORDX_OPAQUE)) {
      a = Module.ACC_OPAQUE;
    } else if (options.contains(Module.ACC_PRIVATE) && acc.equals(ACC_WORDX_PRIVATE)) {
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

  static Module.Availability acceptAvailability(ParserA.TokenReader reader) throws CompileException, IOException {
    ParserA.Token a = ParserA.acceptSpecialWord(reader, ParserA.SPACE_NEEDED);
    Module.Availability av;
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

  static Module.Availability acceptXAvailabilityAttr(ParserB.Elem elem) throws CompileException {
    Module.Availability av;
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
    if (imp.id.equals(this.myId)) {
      emsg = new StringBuffer();
      emsg.append("Imported module id \"");
      emsg.append(imp.id);
      emsg.append("\" conflicts with my module id at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (this.modTab.containsKey(imp.id)) {
      emsg = new StringBuffer();
      emsg.append("Module id \"");
      emsg.append(imp.id);
      emsg.append("\" already defined at ");
      emsg.append(imp.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    this.importStmtList.add(imp);  // multiple imports allowed
    this.modTab.put(imp.id, imp.modName);
    this.addReferredFarMod(imp.modName);
  }

  void addDataStmt(PDataStmt dat) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.actualName, dat.tcon);
    boolean b = this.theCompiler.defDict.predefineTconData(tconKey, dat.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type constructor \"");
      emsg.append(dat.tcon);
      emsg.append("\" at ");
      emsg.append(dat.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    if (dat.constrs != null) {
      for (int i = 0; i < dat.constrs.length; i++) {
        PDataConstrDef constr = dat.constrs[i];
        b = this.theCompiler.defDict.predefineDcon(
          PDefDict.IdKey.create(this.actualName, constr.dcon), dat.acc);
        if (!b) {
          emsg = new StringBuffer();
          emsg.append("Cannot define data constructor \"");
          emsg.append(constr.dcon);
          emsg.append("\" at ");
          emsg.append(constr.srcInfo);
          emsg.append(".");
          throw new CompileException(emsg.toString());
        }
      }
    }

    dat.collectModRefs();
    this.dataStmtList.add(dat);
    this.theCompiler.defDict.putDataDef(tconKey, dat);
  }

  void addExtendStmt(PExtendStmt ext) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.actualName, ext.tcon);
    Cstr baseModName;
    if (ext.baseModId == null) {
      baseModName = Module.MOD_LANG;
    } else {
      baseModName = this.resolveModId(ext.baseModId);
      if (baseModName == null) {
        emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(ext.baseModId);
        emsg.append("\" not defined at ");
        emsg.append(ext.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }
    ext.baseTconKey = PDefDict.IdKey.create(baseModName, ext.baseTcon);
    boolean b = this.theCompiler.defDict.predefineTconExtend(tconKey, ext.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type constructor \"");
      emsg.append(ext.tcon);
      emsg.append("\" at ");
      emsg.append(ext.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < ext.constrs.length; i++) {
      PDataConstrDef constr = ext.constrs[i];
      b = this.theCompiler.defDict.predefineDcon(
        PDefDict.IdKey.create(this.actualName, constr.dcon), ext.acc);
      if (!b) {
        emsg = new StringBuffer();
        emsg.append("Cannot define data constructor \"");
        emsg.append(constr.dcon);
        emsg.append("\" at ");
        emsg.append(constr.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    ext.collectModRefs();
    this.extendStmtList.add(ext);
    this.theCompiler.defDict.putDataDef(tconKey, ext);
  }

  void addAliasTypeStmt(PAliasTypeStmt alias) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey tconKey = PDefDict.IdKey.create(this.actualName, alias.tcon);
    boolean b = this.theCompiler.defDict.predefineTconAliasType(tconKey, alias.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define type alias \"");
      emsg.append(alias.tcon);
      emsg.append("\" at ");
      emsg.append(alias.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    alias.collectModRefs();
    this.aliasTypeStmtList.add(alias);
    this.theCompiler.defDict.putAliasTypeDef(tconKey, alias);
  }

  void addFeatureStmt(PFeatureStmt feat) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey fnameKey = PDefDict.IdKey.create(this.actualName, feat.fname);
    boolean b = this.theCompiler.defDict.predefineFeature(fnameKey, feat.acc);
    if (!b) {
      emsg = new StringBuffer();
      emsg.append("Cannot define feature \"");
      emsg.append(feat.fname);
      emsg.append("\" at ");
      emsg.append(feat.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }

    feat.collectModRefs();
    this.featureStmtList.add(feat);
    this.theCompiler.defDict.putFeatureDef(fnameKey, feat);
  }

  void addEvalStmt(PEvalStmt eval) throws CompileException {
    StringBuffer emsg;

    PDefDict.IdKey officialKey = PDefDict.IdKey.create(this.actualName, eval.official);
    // boolean b = this.theCompiler.defDict.predefineFunOfficial(officialKey, eval.acc);
    if (!this.predefineFunOfficial(officialKey, eval.acc)) {
      emsg = new StringBuffer();
      emsg.append("Cannot define \"");
      emsg.append(eval.official);
      emsg.append("\" for function's official name at ");
      emsg.append(eval.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    for (int i = 0; i < eval.aliases.length; i++) {
      if (!this.theCompiler.defDict.predefineFunAlias(
            PDefDict.IdKey.create(this.actualName, eval.aliases[i]), eval.acc)) {
        emsg = new StringBuffer();
        emsg.append("Cannot define \"");
        emsg.append(eval.aliases[i]);
        emsg.append("\" for function's alias at ");
        emsg.append(eval.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
    }

    eval.collectModRefs();
    this.evalStmtList.add(eval);
    this.theCompiler.defDict.putFunDef(officialKey, eval);
  }

  boolean isLang() {
    return this.actualName != null && this.actualName.equals(Module.MOD_LANG);
  }

  boolean predefineFunOfficial(String official, Module.Access acc) {
    PDefDict.IdKey k = PDefDict.IdKey.create(this.actualName, official);
    return this.predefineFunOfficial(k, acc);
  }

  boolean predefineFunOfficial(PDefDict.IdKey officialKey, Module.Access acc) {
    return this.theCompiler.defDict.predefineFunOfficial(officialKey, acc);
  }

  Cstr resolveModId(String id) {
    Cstr n = null;
    if (id == null) {
      ;
    } else if (id.equals(this.myId)) {
      n = this.actualName;
    } else if (id.equals(MOD_ID_HERE)) {
      n = this.actualName;
    } else if (id.equals(MOD_ID_LANG)) {
      n = Module.MOD_LANG;
    } else {
      n = this.modTab.get(id);
    }
    return n;
  }

  void addReferredFarMod(Cstr modName) {  // add implicitly referred mod
    if (modName.equals(Module.MOD_LANG) || modName.equals(this.actualName)) { return; }  // skip
    if (this.referredFarMods.contains(modName)) { return; }  // already included
// /* DEBUG */ System.out.println(modName);
    this.referredFarMods.add(modName);
  }

  public Cstr[] getForeignMods() {
    Cstr[] ms;
    if (this.isLang()) {
      ms = new Cstr[0];
    } else {
      ms = new Cstr[1 + this.referredFarMods.size()];  // sango.lang and others
      ms[0] = Module.MOD_LANG;
      for (int i = 1, j = 0; i < ms.length; i++, j++) {
        ms[i] = this.referredFarMods.get(j);
      }
    }
    return ms;
  }

  // public Cstr[] getForeignMods2() {
    // Cstr[] ms;
    // if (this.isLang()) {
      // ms = new Cstr[0];
    // } else {
      // ms = new Cstr[this.referredFarMods2.size()];
      // for (int i = 0; i < ms.length; i++) {
        // ms[i] = this.referredFarMods2.get(i);
      // }
    // }
    // return ms;
  // }

  // int modNameToModRefIndex(boolean inReferredDef, Cstr modName) {
    // int index;
    // if (modName.equals(this.name)) {
      // index = Module.MOD_INDEX_SELF;
    // } else if (modName.equals(Module.MOD_LANG)) {
      // index = Module.MOD_INDEX_LANG;
    // } else {
      // int i = this.referredFarMods.indexOf(modName);
      // if (i >= 0) {
        // ;
      // } else if (inReferredDef) {
        // int j = this.referredFarMods2.indexOf(modName);
        // if (j >= 0) {
          // ;
        // } else {
          // j = this.referredFarMods2.size();  // index at new last
          // this.referredFarMods2.add(modName);
        // }
        // i = this.referredFarMods.size() + j;
      // } else {
        // throw new RuntimeException("Unknown mod name. " + modName.repr() + " " + this.referredFarMods);
      // }
      // index = 2 + i;
    // }
    // return index;
  // }

  PEvalStmt getInitFunDef() {
    PEvalStmt eval = null;
    for (int i = 0; eval == null && i < this.evalStmtList.size(); i++) {
      PEvalStmt e = this.evalStmtList.get(i);
      if (e.official.equals(Module.FUN_INIT) && e.params.length == 0) {
        eval = e;
      }
    }
    // if (this.funOfficialDict.containsKey(Module.FUN_INIT)) {
      // PEvalStmt e = this.evalStmtList.get(this.funOfficialDict.get(Module.FUN_INIT));
      // eval = (e.params.length == 0)? e: null;
    // }
    return eval;
  }

  boolean isInitFunDefined() {
    return this.getInitFunDef() != null;
  }

  void setupAliasBody() throws CompileException {
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.get(i).setupBodySkel();
    }
  }

  PModDecl getModDecl() { return this; }

  PModule resolve() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.set(i, this.dataStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.set(i, this.extendStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.aliasTypeStmtList.size(); i++) {
      this.aliasTypeStmtList.set(i, this.aliasTypeStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.set(i, this.featureStmtList.get(i).resolve());
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.set(i, this.evalStmtList.get(i).resolve());
    }
    return this;
  }

  PDefDict.TidProps resolveTcon(PTid tcon) throws CompileException {
    // variable is already processed in PScope
    if ((tcon.catOpt & PDefDict.TID_CAT_VAR) > 0) {
      throw new IllegalArgumentException("invalid cat of id - " + tcon.toString());
    }
    return this.isLang()?  this.resolveTconInLang(tcon): this.resolveTconInOther(tcon);
  }

  private PDefDict.TidProps resolveTconInLang(PTid tcon) throws CompileException {
    if (tcon.modId == null || tcon.modId.equals(this.myId) || tcon.modId.equals(MOD_ID_HERE) || tcon.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(tcon.modId);
      emsg.append("\" at ");
      emsg.append(tcon.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveTcon(this.actualName, tcon.name, tcon.catOpt);
  }

  private PDefDict.TidProps resolveTconInOther(PTid tcon) throws CompileException {
    PDefDict.TidProps tp;
    if (tcon.modId == null) {
      tp = this.doResolveTcon(this.actualName, tcon.name, tcon.catOpt);
      if (tp == null) {
        tp = this.doResolveTcon(Module.MOD_LANG, tcon.name, tcon.catOpt);
      }
    } else if (tcon.modId.equals(this.myId) || tcon.modId.equals(MOD_ID_HERE)) {
      tp = this.doResolveTcon(this.actualName, tcon.name, tcon.catOpt);
    } else {
      Cstr targetModName = this.modTab.get(tcon.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(tcon.modId);
        emsg.append("is not defined at ");
        emsg.append(tcon.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      tp = this.doResolveTcon(targetModName, tcon.name, tcon.catOpt);
    }
    return tp;
  }

  private PDefDict.TidProps doResolveTcon(Cstr modName, String idName, int catOpt) throws CompileException {
    PDefDict.TidProps tp = this.theCompiler.defDict.resolveTcon(
      this.actualName,
      PDefDict.IdKey.create(modName, idName));
    if (tp == null) {
      ;  // pass
    } else if ((tp.cat & catOpt) == 0) {
      tp = null;  // not hit
    }
    return tp;
  }

  PDefDict.TidProps resolveFeature(PTid fname) throws CompileException {
    return this.isLang()?  this.resolveFeatureInLang(fname): this.resolveFeatureInOther(fname);
  }

  private PDefDict.TidProps resolveFeatureInLang(PTid fname) throws CompileException {
    if (fname.modId == null || fname.modId.equals(this.myId) || fname.modId.equals(MOD_ID_HERE) || fname.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(fname.modId);
      emsg.append("\" at ");
      emsg.append(fname.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveFeature(this.actualName, fname.name);
  }

  private PDefDict.TidProps resolveFeatureInOther(PTid fname) throws CompileException {
    PDefDict.TidProps tp;
    if (fname.modId == null) {
      tp = this.doResolveFeature(this.actualName, fname.name);
      if (tp == null) {
        tp = this.doResolveFeature(Module.MOD_LANG, fname.name);
      }
    } else if (fname.modId.equals(this.myId) || fname.modId.equals(MOD_ID_HERE)) {
      tp = this.doResolveFeature(this.actualName, fname.name);
    } else if (fname.modId.equals(MOD_ID_LANG)) {
      tp = this.doResolveFeature(Module.MOD_LANG, fname.name);
    } else {
      Cstr targetModName = this.modTab.get(fname.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(fname.modId);
        emsg.append("is not defined at ");
        emsg.append(fname.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      tp = this.doResolveFeature(targetModName, fname.name);
    }
    return tp;
  }

  private PDefDict.TidProps doResolveFeature(Cstr modName, String idName) throws CompileException {
    return this.theCompiler.defDict.resolveFeature(
      this.actualName,
      PDefDict.IdKey.create(modName, idName));
  }

  PDefDict.EidProps resolveAnchor(PEid eid) throws CompileException {
    // variable is already processed in PScope
    if ((eid.catOpt & PDefDict.EID_CAT_VAR) > 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.resolveAnchorInLang(eid): this.resolveAnchorInOther(eid);
  }

  PDefDict.EidProps resolveAnchorInLang(PEid eid) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveAnchor(this.actualName, eid.name, eid.catOpt);
  }

  PDefDict.EidProps resolveAnchorInOther(PEid eid) throws CompileException {
    PDefDict.EidProps ep;
    if (eid.modId == null) {
      ep = this.doResolveAnchor(this.actualName, eid.name, eid.catOpt);
      if (ep == null) {
        ep = this.doResolveAnchor(Module.MOD_LANG, eid.name, eid.catOpt);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      ep = this.doResolveAnchor(this.actualName, eid.name, eid.catOpt);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      ep = this.doResolveAnchor(Module.MOD_LANG, eid.name, eid.catOpt);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      ep = this.doResolveAnchor(targetModName, eid.name, eid.catOpt);
    }
    return ep;
  }

  PDefDict.EidProps doResolveAnchor(Cstr modName, String idName, int catOpt) throws CompileException {
    PDefDict.EidProps ep = this.theCompiler.defDict.resolveAnchor(
      this.actualName,
      PDefDict.IdKey.create(modName, idName));
    if (ep == null) {
      ;  // pass
    } else if ((ep.cat & catOpt) == 0) {
      ep = null;  // not hit
    }
    return ep;
  }

  // not used
  boolean isFunDefinedHere(String official) {
    PDefDict.EidProps ep = null;
    try {
      ep = this.doResolveFunOfficial(this.actualName, official);
    } catch (CompileException ex) {
      throw new RuntimeException("Unexpected exception. " + ex.toString());
    }
    return ep != null;
  }

  PDefDict.EidProps resolveFunOfficial(PEid eid) throws CompileException {
    if ((eid.catOpt & PDefDict.EID_CAT_FUN_OFFICIAL) == 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.resolveFunOfficialInLang(eid): this.resolveFunOfficialInOther(eid);
  }

  PDefDict.EidProps resolveFunOfficialInLang(PEid eid) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doResolveFunOfficial(this.actualName, eid.name);
  }

  PDefDict.EidProps resolveFunOfficialInOther(PEid eid) throws CompileException {
    PDefDict.EidProps ep;
    if (eid.modId == null) {
      ep = this.doResolveFunOfficial(this.actualName, eid.name);
      if (ep == null) {
        ep = this.doResolveFunOfficial(Module.MOD_LANG, eid.name);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      ep = this.doResolveFunOfficial(this.actualName, eid.name);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      ep = this.doResolveFunOfficial(Module.MOD_LANG, eid.name);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      ep = this.doResolveFunOfficial(targetModName, eid.name);
    }
    return ep;
  }

  PDefDict.EidProps doResolveFunOfficial(Cstr modName, String idName) throws CompileException {
    PDefDict.EidProps ep = this.theCompiler.defDict.resolveAnchor(
      this.actualName,
      PDefDict.IdKey.create(modName, idName));
    if (ep == null) {
      ;  // pass
    } else if ((ep.cat & PDefDict.EID_CAT_FUN_OFFICIAL) == 0) {
      ep = null;  // not hit
    }
    return ep;
  }

  PDefDict.FunSelRes selectFunDef(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    if ((eid.catOpt & PDefDict.EID_CAT_FUN) == 0) {
      throw new IllegalArgumentException("invalid cat of id - " + eid.toString());
    }
    return this.isLang()? this.selectFunDefInLang(eid, paramTypes, givenTVarList): this.selectFunDefInOther(eid, paramTypes, givenTVarList);
  }

  PDefDict.FunSelRes selectFunDefInLang(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    if (eid.modId == null || eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE) || eid.modId.equals(MOD_ID_LANG)) {
      ;
    } else {
      StringBuffer emsg = new StringBuffer();
      emsg.append("Cannot refer foreign module \"");
      emsg.append(eid.modId);
      emsg.append("\" at ");
      emsg.append(eid.srcInfo);
      emsg.append(".");
      throw new CompileException(emsg.toString());
    }
    return this.doSelectFunDef(this.actualName, eid.name, paramTypes, givenTVarList);
  }

  PDefDict.FunSelRes selectFunDefInOther(PEid eid, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    PDefDict.FunSelRes res = null;
    if (eid.modId == null) {
      res = this.doSelectFunDef(this.actualName, eid.name, paramTypes, givenTVarList);
      if (res == null) {
        res = this.doSelectFunDef(Module.MOD_LANG, eid.name, paramTypes, givenTVarList);
      }
    } else if (eid.modId.equals(this.myId) || eid.modId.equals(MOD_ID_HERE)) {
      res = this.doSelectFunDef(this.actualName, eid.name, paramTypes, givenTVarList);
    } else if (eid.modId.equals(MOD_ID_LANG)) {
      res = this.doSelectFunDef(Module.MOD_LANG, eid.name, paramTypes, givenTVarList);
    }  else {
      Cstr targetModName = this.modTab.get(eid.modId);
      if (targetModName == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Module id \"");
        emsg.append(eid.modId);
        emsg.append("is not defined at ");
        emsg.append(eid.srcInfo);
        emsg.append(".");
        throw new CompileException(emsg.toString());
      }
      res = this.doSelectFunDef(targetModName, eid.name, paramTypes, givenTVarList);
    }
    return res;
  }

  PDefDict.FunSelRes doSelectFunDef(Cstr modName, String idName, PTypeSkel[] paramTypes, List<PTypeVarSlot> givenTVarList) throws CompileException {
    PDefDict.FunSelRes res = this.theCompiler.defDict.selectFunDef(
      this.actualName,
      PDefDict.IdKey.create(modName, idName),
      paramTypes, givenTVarList);
    return res;
  }

  public Module.Availability getAvailability() { return this.availability; }

  public Cstr getName() { return this.actualName; }

  private void generateFeatureAliases() throws CompileException {
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      List<PAliasTypeStmt> as = this.featureStmtList.get(i).generateAliases(this);
      for (int j = 0; j < as.size(); j++) {
        this.addAliasTypeStmt(as.get(j));
      }
    }
  }

  String generateId() {
    return "@@" + this.idSuffix++;
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

  private void generateFeatureFuns() throws CompileException {
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      List<PEvalStmt> es = this.featureStmtList.get(i).generateFuns(this);
      for (int j = 0; j < es.size(); j++) {
        this.addEvalStmt(es.get(j));
      }
    }
  }

  private void generateDataFuns() throws CompileException {
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

  static class Builder {
    Cstr actualName;
    PModule mod;
    List<PImportStmt> importStmtList;
    List<PDataStmt> dataStmtList;
    List<PExtendStmt> extendStmtList;
    List<PAliasTypeStmt> aliasTypeStmtList;
    List<PFeatureStmt> featureStmtList;
    List<PEvalStmt> evalStmtList;

    static Builder newInstance(Compiler theCompiler, Parser.SrcInfo srcInfo, Cstr actualName) {
      return new Builder(theCompiler, srcInfo, actualName);
    }

    Builder(Compiler theCompiler, Parser.SrcInfo srcInfo, Cstr actualName) {
      if (actualName == null) { throw new IllegalArgumentException("Null mod name."); }
      this.actualName = actualName;
      this.mod = new PModule(theCompiler, srcInfo);
      this.importStmtList = new ArrayList<PImportStmt>();
      this.dataStmtList = new ArrayList<PDataStmt>();
      this.extendStmtList = new ArrayList<PExtendStmt>();
      this.aliasTypeStmtList = new ArrayList<PAliasTypeStmt>();
      this.featureStmtList = new ArrayList<PFeatureStmt>();
      this.evalStmtList = new ArrayList<PEvalStmt>();
    }

    PScope getScope() { return this.mod.scope; }

    void setAvailability(Module.Availability availability) {
      this.mod.availability = availability;
    }

    void setDefinedName(Cstr name) throws CompileException {
      StringBuffer emsg;
      if (!Module.isValidModName(name)) {
        emsg = new StringBuffer();
        emsg.append("Invalid module name. ");
        emsg.append(name.repr());
        emsg.append(" (Remark: Non-printable characters may be included.)");
        throw new CompileException(emsg.toString());
      }
      if (this.actualName != null && !name.equals(this.actualName)) {
        emsg = new StringBuffer();
        emsg.append("Module name mismatch.");
        emsg.append("\n  required: ");
        emsg.append(this.actualName.repr());
        emsg.append("\n  defined: ");
        emsg.append(name.repr());
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

    void addAliasTypeStmt(PAliasTypeStmt alias) throws CompileException {
      this.aliasTypeStmtList.add(alias);
    }

    void addFeatureStmt(PFeatureStmt feat) throws CompileException {
      this.featureStmtList.add(feat);
    }

    void addEvalStmt(PEvalStmt eval) throws CompileException {
      this.evalStmtList.add(eval);
    }

    PModule create() throws CompileException {
      if (this.mod.definedName != null) {
        this.mod.actualName = this.mod.definedName;
      } else {
        this.mod.actualName = this.actualName;
      }
      if (!this.mod.actualName.equals(Module.MOD_LANG)) {
        PImportStmt.Builder ib = PImportStmt.Builder.newInstance(
          new Parser.SrcInfo(Module.MOD_LANG,"auto"), this.mod.scope);
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
        this.mod.addAliasTypeStmt(this.aliasTypeStmtList.get(i));
      }
      for (int i = 0; i < this.featureStmtList.size(); i++) {
        this.mod.addFeatureStmt(this.featureStmtList.get(i));
      }
      for (int i = 0; i < this.evalStmtList.size(); i++) {
        this.mod.addEvalStmt(this.evalStmtList.get(i));
      }
      return this.mod;
    }
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

  void checkExtension() throws CompileException {
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkExtension();
    }
  }

  void checkVariance() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).checkVariance();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkVariance();
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.get(i).checkVariance();
    }
  }

  // void checkConcreteness() throws CompileException {
    // for (int i = 0; i < this.dataStmtList.size(); i++) {
      // this.dataStmtList.get(i).checkConcreteness();
    // }
    // for (int i = 0; i < this.extendStmtList.size(); i++) {
      // this.extendStmtList.get(i).checkConcreteness();
    // }
  // }

  void normalizeTypes() throws CompileException {
    for (int i = 0; i < this.dataStmtList.size(); i++) {
      this.dataStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.featureStmtList.size(); i++) {
      this.featureStmtList.get(i).normalizeTypes();
    }
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      this.evalStmtList.get(i).normalizeTypes();
    }
  }

  void checkFeatureImplOnExtension() throws CompileException {
    for (int i = 0; i < this.extendStmtList.size(); i++) {
      this.extendStmtList.get(i).checkFeatureImpl();
    }
  }

  void makeSureTypeConsistency() throws CompileException {
    PTypeGraph g = PTypeGraph.create(this.theCompiler, this);
    for (int i = 0; i < this.evalStmtList.size(); i++) {
      PEvalStmt e = this.evalStmtList.get(i);
      e.setupTypeGraph(g);
    }
    g.inferAll();
  }

  private void generateNameFun() throws CompileException {
    // eval _name_ @public -> <cstr> @native
    Parser.SrcInfo si = new Parser.SrcInfo(this.actualName, ":name");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope);
    evalStmtBuilder.setOfficial(Module.FUN_NAME);
    evalStmtBuilder.setAcc(Module.ACC_PUBLIC);
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(PTid.create(si, retScope, MOD_ID_LANG, "cstr", false));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    this.addEvalStmt(evalStmtBuilder.create());
  }

  private void generateInitdFun() throws CompileException {
    // eval _initd_ -> <_init_'s ret type> @native
    PEvalStmt eval;
    if ((eval = this.getInitFunDef()) == null) { return; }
    Parser.SrcInfo si = eval.srcInfo.appendPostfix("_initd");
    PEvalStmt.Builder evalStmtBuilder = PEvalStmt.Builder.newInstance(si, this.scope);
    evalStmtBuilder.setOfficial(Module.FUN_INITD);
    evalStmtBuilder.setAcc(Module.ACC_PRIVATE);
    PRetDef.Builder retDefBuilder = PRetDef.Builder.newInstance(si, evalStmtBuilder.getDefScope());
    PScope retScope = retDefBuilder.getScope();
    PType.Builder retTypeBuilder = PType.Builder.newInstance(si, retScope);
    retTypeBuilder.addItem(eval.retDef.type.unresolvedCopy(
      si, retScope, PType.COPY_EXT_KEEP, PType.COPY_CONCRETE_OFF));
    retDefBuilder.setType(retTypeBuilder.create());
    evalStmtBuilder.setRetDef(retDefBuilder.create());
    this.addEvalStmt(evalStmtBuilder.create());
  }
}
