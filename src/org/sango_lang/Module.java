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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public class Module {
  // module file format version
  // static final String CUR_FORMAT_VERSION = "1.0";
  // initial
  // static final String CUR_FORMAT_VERSION = "1.1";
  // add requires_concrete attribute to type var slot, which must be checked for type consistency
  static final String CUR_FORMAT_VERSION = "1.2";
  // planned: always use module index on type ref

  public static final Cstr MOD_LANG = new Cstr("sango.lang");
  public static final Cstr MOD_ENTITY = new Cstr("sango.entity");
  public static final Cstr MOD_ACTOR = new Cstr("sango.actor");

  // lang-special tcons
  public static final String TCON_BOTTOM = "_";
  public static final String TCON_EXPOSED = "_?_";
  public static final String TCON_VOID = "void";
  public static final String TCON_BOOL = "bool";
  public static final String TCON_INT = "int";
  public static final String TCON_BYTE = "byte";
  public static final String TCON_CHAR = "char";
  public static final String TCON_REAL = "real";
  public static final String TCON_TUPLE = "tuple";
  public static final String TCON_LIST = "list";
  public static final String TCON_STRING = "string";
  public static final String TCON_FUN = "fun";

  // lang-builtin functions
  public static final String FUN_INIT = "_init_";
  public static final String FUN_INITD = "_initd_";
  public static final String FUN_MAIN = "_main_";
  public static final String FUN_NAME = "_name_";

  static final int MOD_INDEX_SELF = 0;
  static final int MOD_INDEX_LANG = 1;
  // static final int MOD_INDEX_IMPORTED_BASE = 2;

  static final String TAG_ALIAS_TYPE_DEF = "alias_type_def";
  static final String TAG_ALIAS_TYPE_DEFS = "alias_type_defs";
  static final String TAG_ALIASES = "aliases";
  static final String TAG_ALIAS = "alias";
  static final String TAG_ATTR = "attr";
  // static final String TAG_CONSTRAINT = "constraint";
  static final String TAG_CLOSURE_CONSTR = "closure_constr";
  static final String TAG_CLOSURE_CONSTRS = "closure_constrs";
  static final String TAG_CLOSURE_IMPL = "closure_impl";
  static final String TAG_CLOSURE_IMPLS = "closure_impls";
  static final String TAG_CONSTR = "constr";
  static final String TAG_CONSTS = "consts";
  static final String TAG_CSTR = "cstr";
  static final String TAG_DATA_CONSTR = "data_constr";
  static final String TAG_DATA_CONSTRS = "data_constrs";
  static final String TAG_DATA_DEF = "data_def";
  static final String TAG_DATA_DEFS = "data_defs";
  static final String TAG_FEATURE = "feature";
  static final String TAG_FEATURE_DEF = "feature_def";
  static final String TAG_FEATURE_DEFS = "feature_defs";
  static final String TAG_FEATURE_IMPL = "feature_impl";
  static final String TAG_FEATURES = "features";
  static final String TAG_FOREIGN = "foreign";
  static final String TAG_FUN_DEF = "fun_def";
  static final String TAG_FUN_DEFS = "fun_defs";
  static final String TAG_I = "i";
  static final String TAG_IMPL = "impl";
  static final String TAG_INST_SEQ = "inst_seq";
  static final String TAG_INT = "int";
  static final String TAG_MOD_REF = "mod_ref";
  static final String TAG_MOD_REFS = "mod_refs";
  static final String TAG_MODULE = "module";
  static final String TAG_NATIVE = "native";
  static final String TAG_NIL = "nil";
  static final String TAG_OBJ = "obj";
  static final String TAG_PARAM = "param";
  static final String TAG_PARAMS = "params";
  static final String TAG_REAL = "real";
  static final String TAG_RET = "ret";
  static final String TAG_S = "s";
  static final String TAG_SRCINFO_TAB = "srcinfo_tab";
  static final String TAG_TYPE = "type";
  static final String TAG_TYPE_REF = "type_ref";
  static final String TAG_TYPE_VAR = "type_var";
  static final String TAG_VMCODE = "vmcode";

  static final String ATTR_ACC = "acc";
  static final String ATTR_ATTR_COUNT = "attr_count";
  static final String ATTR_AVAILABILITY = "availability";
  static final String ATTR_BASE_MOD_INDEX = "base_mod_index";
  static final String ATTR_BASE_TCON = "base_tcon";
  static final String ATTR_DCON = "dcon";
  static final String ATTR_ENV_COUNT = "env_count";
  static final String ATTR_EXT = "ext";
  static final String ATTR_FORMAT_VERSION = "format_version";
  static final String ATTR_GETTER = "getter";
  static final String ATTR_MOD = "mod";
  static final String ATTR_MOD_INDEX = "mod_index";
  static final String ATTR_NAME = "name";
  static final String ATTR_REQUIRES_CONCRETE = "requires_concrete";
  static final String ATTR_O = "o";  // operation
  static final String ATTR_P = "p";  // param #1
  static final String ATTR_PARAM_COUNT = "param_count";
  static final String ATTR_Q = "q";  // param #2
  static final String ATTR_SLOT = "slot";
  static final String ATTR_SLOT_COUNT = "slot_count";
  static final String ATTR_TCON = "tcon";
  static final String ATTR_TPARAM_COUNT = "tparam_count";
  static final String ATTR_U = "u";  // upper code index of src info entry
  static final String ATTR_V = "v";  // src info value
  static final String ATTR_VALUE = "value";
  static final String ATTR_VAR_COUNT = "var_count";
  static final String ATTR_VARIANCE = "variance";

  static final String REPR_YES = "yes";
  static final String REPR_NO = "no";
  static final String REPR_PUBLIC = "public";
  static final String REPR_PROTECTED = "protected";
  static final String REPR_OPAQUE = "opaque";
  static final String REPR_PRIVATE = "private";
  static final String REPR_GENERAL = "general";
  static final String REPR_ALPHA = "alpha";
  static final String REPR_BETA = "beta";
  static final String REPR_LIMITED = "limited";
  static final String REPR_DEPRECATED = "deprecated";
  static final String REPR_INVARIANT = "in";  // CAUTION: symbol differs from constant name
  static final String REPR_COVARIANT = "co";  // CAUTION: symbol differs from constant name
  static final String REPR_CONTRAVARIANT = "cx";  // CAUTION: symbol differs from constant name

  public static class Access extends Option {
    private static int nextSN = 0;
    String repr;
    int openness;
    Access(String repr, int openness) { super(); this.repr = repr; this.openness = openness; }
    int nextSN() { return nextSN++; };
    public String toString() { return this.repr; }
  }
  public static final Access ACC_PUBLIC = new Access("public", 3);
  public static final Access ACC_PROTECTED = new Access("protected", 2);
  public static final Access ACC_OPAQUE = new Access("opaque", 1);
  public static final Access ACC_PRIVATE = new Access("private", 0);

  public static class Availability extends Option {
    private static int nextSN = 0;
    String repr;
    Availability(String repr) { super(); this.repr = repr; }
    int nextSN() { return nextSN++; };
    public String toString() { return this.repr; }
  }
  public static final Availability AVAILABILITY_GENERAL = new Availability("general");
  public static final Availability AVAILABILITY_ALPHA = new Availability("alpha");
  public static final Availability AVAILABILITY_BETA = new Availability("beta");
  public static final Availability AVAILABILITY_LIMITED = new Availability("limited");
  public static final Availability AVAILABILITY_DEPRECATED = new Availability("deprecated");

  public static class Variance extends Option {
    private static int nextSN = 0;
    String repr;
    Variance(String repr) { super(); this.repr = repr; }
    int nextSN() { return nextSN++; };
    public String toString() { return this.repr; }
  }
  public static final Variance NO_VARIANCE = null;
  public static final Variance INVARIANT = new Variance("invariant");
  public static final Variance COVARIANT = new Variance("covariant");
  public static final Variance CONTRAVARIANT = new Variance("contravariant");

  static final int MSLOT_INDEX_NAME = 0;
  static final int MSLOT_INDEX_INITD = 1;

  Cstr name;
  Availability availability;
  int slotCount;
  ModTab modTab;
  Map<Cstr, MDataDef[]> foreignDataDefsDict;
  Map<Cstr, MAliasTypeDef[]> foreignAliasTypeDefsDict;
  Map<Cstr, MFeatureDef[]> foreignFeatureDefsDict;
  Map<Cstr, MFunDef[]> foreignFunDefsDict;
  MDataDef[] dataDefs;
  Map<String, MDataDef> dataDefDict;
  Map<String, MConstrDef> constrDefDict;
  MAliasTypeDef[] aliasTypeDefs;
  MFeatureDef[] featureDefs;
  Map<String, MFeatureDef> featureDefDict;
  MFunDef[] funDefs;
  Map<String, MFunDef> funDefDict;
  MClosureConstr[] closureConstrs;
  MClosureImpl[] closureImpls;
  MDataConstr[] dataConstrs;
  ConstElem[] consts;
  Class<?> nativeImplClass;

  private Module() {}

  public Availability getAvailability() { return this.availability; }

  public int getSlotCount() { return this.slotCount; }

  public MDataDef[] getDataDefs() { return this.dataDefs; }

  public MAliasTypeDef[] getAliasTypeDefs() { return this.aliasTypeDefs; }

  public MFeatureDef[] getFeatureDefs() { return this.featureDefs; }

  public MFunDef[] getFunDefs() { return this.funDefs; }

  public ConstElem[] getConsts() { return this.consts; }

  public MDataConstr[] getDataConstrs() { return this.dataConstrs; }

  public MFunDef getFunDef(String name) { return this.funDefDict.get(name); }

  public MClosureConstr[] getClosureConstrs() { return this.closureConstrs; }

  public MClosureImpl[] getClosureImpls() { return this.closureImpls; }

  public ModTab getModTab() { return this.modTab; }

  public Cstr getModAt(int index) { return this.modTab.get(index); }

  MDataDef[] getForeignDataDefs(Cstr modName) { return this.foreignDataDefsDict.get(modName); }

  MAliasTypeDef[] getForeignAliasTypeDefs(Cstr modName) { return this.foreignAliasTypeDefsDict.get(modName); }

  MFeatureDef[] getForeignFeatureDefs(Cstr modName) { return this.foreignFeatureDefsDict.get(modName); }

  public static Module internalize(Document doc, Cstr name) throws FormatException {
    Builder builder = newBuilder();
    builder.setName(name);

    Node node = doc.getFirstChild();
    if (!internalizeModule(node, name, builder)) {
      throw new FormatException("'" + TAG_MODULE + "' element not found.");
    }
    node = skipIgnorableNodes(node.getFirstChild());

    if (internalizeModRefs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeDataDefs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeAliasTypeDefs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeFeatureDefs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeFunDefs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeClosureImpls(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeDataConstrs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeClosureConstrs(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (internalizeConsts(node, builder)) {
      node = skipIgnorableNodes(node.getNextSibling());
    }
    if (node != null) {
      throw new FormatException("Extra node in module file: " + node.getNodeName());
    }

    return builder.create();
  }

  static boolean internalizeModule(Node node, Cstr name, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_MODULE)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing module node...");
    NamedNodeMap attrs = node.getAttributes();

    Node aFormatVersion = attrs.getNamedItem(ATTR_FORMAT_VERSION);
    if (aFormatVersion == null) {
      throw new FormatException("'" + ATTR_FORMAT_VERSION + "' attribute not found.");
    }
    String formatVersion = aFormatVersion.getNodeValue();
    // /* DEBUG */ System.out.print("format_version = ");
    // /* DEBUG */ System.out.println(formatVersion);
    if (formatVersion.equals(CUR_FORMAT_VERSION)
      || formatVersion.equals("1.0")) {
      ;
    } else {
      throw new FormatException("Non-supported format version: " + formatVersion);
    }

    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName != null) {
      String modName = aName.getNodeValue();
      // /* DEBUG */ System.out.print("name = ");
      // /* DEBUG */ System.out.println(modName);
      if (!name.equalsToString(modName)) {
        throw new FormatException("Invalid module name: " + modName);
      }
    }

    Availability av = AVAILABILITY_GENERAL;
    Node aAvailability = attrs.getNamedItem(ATTR_AVAILABILITY);
    if (aAvailability != null) {
      av = parseAvailabilityAttr(aAvailability.getNodeValue());
    }
    // /* DEBUG */ System.out.print("avilability = ");
    // /* DEBUG */ System.out.println(av);
    builder.setAvailability(av);

    int slotCount = 1;
    Node aSlotCount = attrs.getNamedItem(ATTR_SLOT_COUNT);
    if (aSlotCount != null) {
      slotCount = parseInt(aSlotCount.getNodeValue());
      if (slotCount < 0) {
        throw new FormatException("Invalid slot count: " + aSlotCount.getNodeValue());
      }
    }
    // /* DEBUG */ System.out.print("slot_count = ");
    // /* DEBUG */ System.out.println(slotCount);
    builder.setSlotCount(slotCount);

    return true;
  }

  static boolean internalizeModRefs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_MOD_REFS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing mod_refs node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_MOD_REF)) {
        internalizeModRef(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_MOD_REFS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeModRef(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aMod = attrs.getNamedItem(ATTR_MOD);
    if (aMod == null) {
      throw new FormatException("'" + ATTR_MOD + "' attribute not found.");
    }
    // /* DEBUG */ System.out.print("mod = ");
    // /* DEBUG */ System.out.println(aMod.getNodeValue());

    builder.startForeignMod(new Cstr(aMod.getNodeValue()));

    Node n = node.getFirstChild();
    if (n != null) {
      n = skipIgnorableNodes(n);
      if (internalizeDataDefs(n, builder)) {
        n = skipIgnorableNodes(n.getNextSibling());
      }
      if (internalizeAliasTypeDefs(n, builder)) {
        n = skipIgnorableNodes(n.getNextSibling());
      }
      if (internalizeFeatureDefs(n, builder)) {
        n = skipIgnorableNodes(n.getNextSibling());
      }
      if (internalizeFunDefs(n, builder)) {
        n = skipIgnorableNodes(n.getNextSibling());
      }
      if (n != null) {
        throw new FormatException("Extra node under mod_ref node: " + n.getNodeName());
      }
    }
    builder.endForeignMod();
  }

  static boolean internalizeDataDefs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_DATA_DEFS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing data_defs nodes...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_DATA_DEF)) {
        internalizeDataDef(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_DATA_DEFS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeDataDef(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();

    Node aTcon = attrs.getNamedItem(ATTR_TCON);
    if (aTcon == null) {
      throw new FormatException("'" + ATTR_TCON + "' attribute not found.");
    }
    // /* DEBUG */ System.out.print("tcon = ");
    // /* DEBUG */ System.out.println(aTcon.getNodeValue());

    Availability av = AVAILABILITY_GENERAL;  // default
    Node aAvailability = attrs.getNamedItem(ATTR_AVAILABILITY);
    if (aAvailability != null) {
      av = parseAvailabilityAttr(aAvailability.getNodeValue());
    }
    // /* DEBUG */ System.out.print("availability = ");
    // /* DEBUG */ System.out.println(av);

    Access acc = ACC_PRIVATE;  // default
    Node aAcc = attrs.getNamedItem(ATTR_ACC);
    if (aAcc != null) {
      String sAcc = aAcc.getNodeValue();
      if (sAcc.equals(REPR_PUBLIC)) {
        acc = ACC_PUBLIC;
      } else if (sAcc.equals(REPR_PROTECTED)) {
        acc = ACC_PROTECTED;
      } else if (sAcc.equals(REPR_OPAQUE)) {
        acc = ACC_OPAQUE;
      } else if (sAcc.equals(REPR_PRIVATE)) {
        acc = ACC_PRIVATE;
      } else {
        throw new FormatException("Invalid " + ATTR_ACC + ": " + sAcc);
      }
    }
    // /* DEBUG */ System.out.print("acc = ");
    // /* DEBUG */ System.out.println(acc);

    int paramCount = 0;  // default: 0
    Node aParamCount = attrs.getNamedItem(ATTR_PARAM_COUNT);
    if (aParamCount != null) {
      paramCount = parseInt(aParamCount.getNodeValue());
    }
    if (paramCount < 0) {
      throw new FormatException("Invalid " + ATTR_PARAM_COUNT + ": " + paramCount);
    }
    // /* DEBUG */ System.out.print("param_count = ");
    // /* DEBUG */ System.out.println(paramCount);

    Node aBaseModIndex = attrs.getNamedItem(ATTR_BASE_MOD_INDEX);
    if (aBaseModIndex != null) {
      Node aBaseTcon = attrs.getNamedItem(ATTR_BASE_TCON);
      String baseTcon = (aBaseTcon != null)? aBaseTcon.getNodeValue(): aTcon.getNodeValue();
      builder.startDataDef(aTcon.getNodeValue(), av, acc, parseInt(aBaseModIndex.getNodeValue()), baseTcon);
    } else {
      builder.startDataDef(aTcon.getNodeValue(), av, acc);
    }
    Node n = node.getFirstChild();
    int state = 0;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (state == 0 & n.getNodeName().equals(TAG_PARAMS)) {  // appears first if any
        internalizeDataDefParams(n, builder);
        state = 1;
      } else if (state <= 1 && n.getNodeName().equals(TAG_CONSTR)) {
        internalizeDataDefConstr(n, builder);
        state = 1;
      } else if (state <= 2 && n.getNodeName().equals(TAG_FEATURE_IMPL)) {
        internalizeDataDefFeatureImpl(n, builder);
        state = 2;
      } else {
        throw new FormatException("Unknown element under '" + TAG_DATA_DEF + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    builder.endDataDef();
  }

  static void internalizeDataDefParams(Node node, Builder builder) throws FormatException {
    Node n = node.getFirstChild();
    MTypeVar.DefWithVariance v;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if ((v = MTypeVar.DefWithVariance.internalize(n)) != null) {
        builder.putDataDefParam(v);
      } else {
        throw new FormatException("Unknown element under '" + TAG_PARAMS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
  }

  static void internalizeDataDefConstr(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aDcon = attrs.getNamedItem(ATTR_DCON);
    if (aDcon == null) {
      throw new FormatException("'" + ATTR_DCON + "' attribute not found.");
    }
    String dcon = aDcon.getNodeValue();
    // /* DEBUG */ System.out.print("dcon = ");
    // /* DEBUG */ System.out.println(dcon);

    builder.startConstrDef(dcon);
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_ATTR)) {
        internalizeAttr(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_CONSTR + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    builder.endConstrDef();
  }

  static void internalizeDataDefFeatureImpl(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();

    int modIndex = MOD_INDEX_SELF;  // default: self (= 0)
    Node aModIndex = attrs.getNamedItem(ATTR_MOD_INDEX);
    if (aModIndex != null) {
      modIndex = parseInt(aModIndex.getNodeValue());
    }
    if (modIndex < 0) {
      throw new FormatException("Invalid mod_index: " + modIndex);
    }
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("'" + ATTR_NAME + "' provider name not found.");
    }
    String name = aName.getNodeValue();
    Node aGetter = attrs.getNamedItem(ATTR_GETTER);
    if (aGetter == null) {
      throw new FormatException("'" + ATTR_GETTER + "' getter not found.");
    }
    String getter = aGetter.getNodeValue();

    Node n = node.getFirstChild();
    MFeature f = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (f == null && n.getNodeName().equals(TAG_FEATURE)) {
        f = MFeature.internalize(n);
      } else {
        throw new FormatException("Unknown element under '" + TAG_FEATURE_IMPL + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    builder.addFeatureImplDef(modIndex, name, getter, f);
  }

  static void internalizeAttr(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    String name = null;
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName != null) {
      name = aName.getNodeValue();
    }
    // /* DEBUG */ System.out.print("name = ");
    // /* DEBUG */ System.out.println(name);

    builder.startAttrDef(name);
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Attribute type not found.");
    }
    builder.setAttrType(type);
    builder.endAttrDef();
  }

  static boolean internalizeAliasTypeDefs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_ALIAS_TYPE_DEFS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing alias_type_defs node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_ALIAS_TYPE_DEF)) {
        internalizeAliasTypeDef(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_ALIAS_TYPE_DEFS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeAliasTypeDef(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aTcon = attrs.getNamedItem(ATTR_TCON);
    if (aTcon == null) {
      throw new FormatException("Type constructor not found.");
    }

    Availability av = AVAILABILITY_GENERAL;
    Node aAvailability = attrs.getNamedItem(ATTR_AVAILABILITY);
    if (aAvailability != null) {
      av = parseAvailabilityAttr(aAvailability.getNodeValue());
    }

    int paramCount = 0;  // default: 0
    Node aParamCount = attrs.getNamedItem(ATTR_PARAM_COUNT);
    if (aParamCount != null) {
      paramCount = parseInt(aParamCount.getNodeValue());
    }
    if (paramCount < 0) {
      throw new FormatException("Invalid '" + ATTR_PARAM_COUNT + "' attribute: " + paramCount);
    }

    Access acc = ACC_PRIVATE;  // default
    Node aAcc = attrs.getNamedItem(ATTR_ACC);
    if (aAcc != null) {
      String sAcc = aAcc.getNodeValue();
      if (sAcc.equals(REPR_PUBLIC)) {
        acc = ACC_PUBLIC;
      } else if (sAcc.equals(REPR_PRIVATE)) {
        acc = ACC_PRIVATE;
      } else {
        throw new FormatException("Invalid '" + ATTR_ACC + "' attribute: " + sAcc);
      }
    }

    builder.startAliasTypeDef(aTcon.getNodeValue(), av, acc, paramCount);
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Attribute type not found.");
    }
    builder.setAliasBody(type);
    builder.endAliasTypeDef();
  }

  static boolean internalizeFeatureDefs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_FEATURE_DEFS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing feature_defs node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_FEATURE_DEF)) {
        internalizeFeatureDef(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_FEATURE_DEFS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeFeatureDef(Node node, Builder builder) throws FormatException {
    MFeatureDef.Builder featureDefBuilder = MFeatureDef.Builder.newInstance();

    NamedNodeMap attrs = node.getAttributes();
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("Feature name not found.");
    }
    String featureName = aName.getNodeValue();
    // /* DEBUG */ System.out.print("name = ");
    // /* DEBUG */ System.out.println(featureName);
    featureDefBuilder.setName(featureName);

    Availability av = AVAILABILITY_GENERAL;
    Node aAvailability = attrs.getNamedItem(ATTR_AVAILABILITY);
    if (aAvailability != null) {
      av = parseAvailabilityAttr(aAvailability.getNodeValue());
    }
    // /* DEBUG */ System.out.print("avilability = ");
    // /* DEBUG */ System.out.println(av);
    featureDefBuilder.setAvailability(av);

    Access acc = ACC_PRIVATE;  // default
    Node aAcc = attrs.getNamedItem(ATTR_ACC);
    if (aAcc != null) {
      String sAcc = aAcc.getNodeValue();
      if (sAcc.equals(REPR_PUBLIC)) {
        acc = ACC_PUBLIC;
      } else if (sAcc.equals(REPR_PRIVATE)) {
        acc = ACC_PRIVATE;
      } else {
        throw new FormatException("Invalid '" + ATTR_ACC + "' attribute: " + sAcc);
      }
    }
    // /* DEBUG */ System.out.print("acc = ");
    // /* DEBUG */ System.out.println(acc);
    featureDefBuilder.setAcc(acc);

    Node n = skipIgnorableNodes(node.getFirstChild());

    MTypeVar objType;
    if (n == null || !n.getNodeName().equals(TAG_OBJ)) {
      throw new FormatException("Obj type not found.");
    } else if ((objType = internalizeFeatureObj(n)) == null) {
      throw new FormatException("Obj type not found.");
    }
    featureDefBuilder.setObjType(objType);
    n = skipIgnorableNodes(n.getNextSibling());

    if (internalizeFeatureParams(n, builder, featureDefBuilder)) {
      n = skipIgnorableNodes(n.getNextSibling());
    }

    MTypeRef implType;
    if (n == null || !n.getNodeName().equals(TAG_IMPL)) {
      throw new FormatException("Feature impl not found.");
    } else if ((implType = internalizeFeatureImpl(n)) == null) {
      throw new FormatException("Obj type not found.");
    }
    featureDefBuilder.setImplType(implType);
    n = skipIgnorableNodes(n.getNextSibling());

    if (n != null) {
      throw new FormatException("Unknown element under feature_def: " + n.getNodeName());
    }
    builder.putFeatureDef(featureDefBuilder.create());
  }

  static MTypeVar internalizeFeatureObj(Node node) throws FormatException {
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Obj type not found.");
    } else if (!(type instanceof MTypeVar)) {
      throw new FormatException("Obj type invalid. " + type);
    }
    return (MTypeVar)type;
  }

  static boolean internalizeFeatureParams(Node node, Builder builder, MFeatureDef.Builder featureDefBuilder) throws FormatException {
    if (node == null || !node.getNodeName().equals(TAG_PARAMS)) { return false; }
    Node n = node.getFirstChild();
    MTypeVar.DefWithVariance v;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if ((v = MTypeVar.DefWithVariance.internalize(n)) != null) {
        featureDefBuilder.addParam(v);
      } else {
        throw new FormatException("Unknown element under '" + TAG_PARAMS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static MTypeRef internalizeFeatureImpl(Node node) throws FormatException {
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Impl type not found.");
    } else if (!(type instanceof MTypeRef)) {
      throw new FormatException("Impl type invalid. " + type);
    }
    return (MTypeRef)type;
  }

  static boolean internalizeFunDefs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_FUN_DEFS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing fun_defs node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_FUN_DEF)) {
        internalizeFunDef(n, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_FUN_DEFS + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeFunDef(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("Offical function name not found.");
    }
    String funName = aName.getNodeValue();
    // /* DEBUG */ System.out.print("name = ");
    // /* DEBUG */ System.out.println(funName);

    Availability av = AVAILABILITY_GENERAL;
    Node aAvailability = attrs.getNamedItem(ATTR_AVAILABILITY);
    if (aAvailability != null) {
      av = parseAvailabilityAttr(aAvailability.getNodeValue());
    }
    // /* DEBUG */ System.out.print("avilability = ");
    // /* DEBUG */ System.out.println(av);

    Access acc = ACC_PRIVATE;  // default
    Node aAcc = attrs.getNamedItem(ATTR_ACC);
    if (aAcc != null) {
      String sAcc = aAcc.getNodeValue();
      if (sAcc.equals(REPR_PUBLIC)) {
        acc = ACC_PUBLIC;
      } else if (sAcc.equals(REPR_PRIVATE)) {
        acc = ACC_PRIVATE;
      } else {
        throw new FormatException("Invalid '" + ATTR_ACC + "' attribute: " + sAcc);
      }
    }
    // /* DEBUG */ System.out.print("acc = ");
    // /* DEBUG */ System.out.println(acc);

    MFunDef.Builder funDefBuilder = MFunDef.Builder.newInstance();
    funDefBuilder.setName(funName);
    funDefBuilder.setAvailability(av);
    funDefBuilder.setAcc(acc);
    Node n = skipIgnorableNodes(node.getFirstChild());
    if (internalizeFunAliases(n, builder, funDefBuilder)) {
      n = skipIgnorableNodes(n.getNextSibling());
    }
    if (internalizeFunParams(n, builder, funDefBuilder)) {
      n = skipIgnorableNodes(n.getNextSibling());
    }
    if (n != null && n.getNodeName().equals(TAG_RET)) {
      internalizeFunRet(n, builder, funDefBuilder);
      n = skipIgnorableNodes(n.getNextSibling());
    } else {
      throw new FormatException("ret node not found for " + funName + ".");
    }
    if (n != null) {
      throw new FormatException("Unknown element under fun_def: " + n.getNodeName());
    }
    builder.putFunDef(funDefBuilder.create());
  }

  static boolean internalizeFunAliases(Node node, Builder builder, MFunDef.Builder funDefBuilder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_ALIASES)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing aliases node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_ALIAS)) {
        internalizeFunAlias(n, builder, funDefBuilder);
      } else {
        throw new FormatException("Unknown element under aliases: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeFunAlias(Node node, Builder builder, MFunDef.Builder funDefBuilder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("Alias name not found.");
    }
    funDefBuilder.addAlias(aName.getNodeValue());
  }

  static boolean internalizeFunParams(Node node, Builder builder, MFunDef.Builder funDefBuilder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_PARAMS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing params node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_PARAM)) {
        internalizeFunParam(n, builder, funDefBuilder);
      } else {
        throw new FormatException("Unknown element under params: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeFunParam(Node node, Builder builder, MFunDef.Builder funDefBuilder) throws FormatException {
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Attribute type not found.");
    }
    funDefBuilder.addParamType(type);
  }

  static void internalizeFunRet(Node node, Builder builder, MFunDef.Builder funDefBuilder) throws FormatException {
    Node n = node.getFirstChild();
    MType type = null;
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (type == null && (type = internalizeType(n)) != null) {
        ;
      } else {
        throw new FormatException("Unknown or extra element under " + TAG_ATTR + " element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    if (type == null) {
      throw new FormatException("Attribute type not found.");
    }
    funDefBuilder.setRetType(type);
  }

  static boolean internalizeClosureImpls(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_CLOSURE_IMPLS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing closure_impls node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_CLOSURE_IMPL)) {
        internalizeClosureImpl(n, builder);
      } else {
        throw new FormatException("Unknown element under closure_impls: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeClosureImpl(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("Function name not found.");
    }
    // /* DEBUG */ System.out.print("name = ");
    // /* DEBUG */ System.out.println(aName.getNodeValue());

    int paramCount = 0;  // default: 0
    Node aParamCount = attrs.getNamedItem(ATTR_PARAM_COUNT);
    if (aParamCount != null) {
      paramCount = parseInt(aParamCount.getNodeValue());
    }
    if (paramCount < 0) {
      throw new FormatException("Invalid '" + ATTR_PARAM_COUNT + "' attribute: " + paramCount);
    }
    // /* DEBUG */ System.out.print("param_count = ");
    // /* DEBUG */ System.out.println(paramCount);

    builder.startClosureImpl(aName.getNodeValue(), paramCount);

    Node nImpl = skipIgnorableNodes(node.getFirstChild());
    if (nImpl != null) {
      String nImplTag = nImpl.getNodeName();
      if (nImplTag.equals(TAG_VMCODE)) {
        internalizeVMCode(nImpl, paramCount, builder);
      } else {
        throw new FormatException("Unknown element under '" + TAG_CLOSURE_IMPL + "' element: " + nImplTag);
      }
    }
    builder.endClosureImpl();
  }

  static boolean internalizeDataConstrs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_DATA_CONSTRS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing constr_impls node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_DATA_CONSTR)) {
        internalizeDataConstr(n, builder);
      } else {
        throw new FormatException("Unknown element under data_constrs: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeDataConstr(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    int modIndex = MOD_INDEX_SELF;  // default: self (= 0)
    Node aModIndex = attrs.getNamedItem(ATTR_MOD_INDEX);
    if (aModIndex != null) {
      modIndex = parseInt(aModIndex.getNodeValue());
    }
    if (modIndex < 0) {
      throw new FormatException("Invalid mod_index: " + modIndex);
    }
    // /* DEBUG */ System.out.print("mod_index = ");
    // /* DEBUG */ System.out.println(modIndex);

    Node aDcon = attrs.getNamedItem(ATTR_DCON);
    if (aDcon == null) {
      throw new FormatException("'dcon' attribute not found.");
    }
    // /* DEBUG */ System.out.print("dcon = ");
    // /* DEBUG */ System.out.println(aDcon.getNodeValue());

    int attrCount = 0;  // default: 0
    Node aAttrCount = attrs.getNamedItem(ATTR_ATTR_COUNT);
    if (aAttrCount != null) {
      attrCount = parseInt(aAttrCount.getNodeValue());
    }
    if (attrCount < 0) {
      throw new FormatException("Invalid attr_count: " + attrCount);
    }
    // /* DEBUG */ System.out.print("attr_count = ");
    // /* DEBUG */ System.out.println(attrCount);

    Node aTcon = attrs.getNamedItem(ATTR_TCON);
    String tcon = null;
    int tparamCount = 0;
    if (aTcon != null) {  // no tcon info when ptn matching only
    // /* DEBUG */ System.out.print("tcon = ");
    // /* DEBUG */ System.out.println(aTcon.getNodeValue());

      tcon = aTcon.getNodeValue();
      Node aTparamCount = attrs.getNamedItem(ATTR_TPARAM_COUNT);
      if (aTparamCount == null) {
        throw new FormatException("'tparam_count' attribute not found.");
      }
      tparamCount = parseInt(aTparamCount.getNodeValue());
    // /* DEBUG */ System.out.print("tparam_count = ");
    // /* DEBUG */ System.out.println(tparamCount);
    }

    builder.putDataConstr(modIndex, aDcon.getNodeValue(), attrCount, tcon, tparamCount);
  }

  static boolean internalizeClosureConstrs(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_CLOSURE_CONSTRS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing closure_impls node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_CLOSURE_CONSTR)) {
        internalizeClosureConstr(n, builder);
      } else {
        throw new FormatException("Unknown element under closure_constrs: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeClosureConstr(Node node, Builder builder) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    int modIndex = MOD_INDEX_SELF;  // default: self (= 0)
    Node aModIndex = attrs.getNamedItem(ATTR_MOD_INDEX);
    if (aModIndex != null) {
      modIndex = parseInt(aModIndex.getNodeValue());
    }
    if (modIndex < 0) {
      throw new FormatException("Invalid " + ATTR_MOD_INDEX + ": " + modIndex);
    }
    // /* DEBUG */ System.out.print("mod_index = ");
    // /* DEBUG */ System.out.println(modIndex);

    Node aName = attrs.getNamedItem(ATTR_NAME);
    if (aName == null) {
      throw new FormatException("'" + ATTR_NAME + "' attribute not found.");
    }
    // /* DEBUG */ System.out.print("name = ");
    // /* DEBUG */ System.out.println(aName.getNodeValue());

    int envCount = 0;  // default: 0
    Node aEnvCount = attrs.getNamedItem(ATTR_ENV_COUNT);
    if (aEnvCount != null) {
      envCount = parseInt(aEnvCount.getNodeValue());
    }
    if (envCount < 0) {
      throw new FormatException("Invalid " + ATTR_ENV_COUNT + ": " + envCount);
    }
    // /* DEBUG */ System.out.print("env_count = ");
    // /* DEBUG */ System.out.println(envCount);

    builder.putClosureConstr(modIndex, aName.getNodeValue(), envCount);
  }

  static boolean internalizeConsts(Node node, Builder builder) throws FormatException {
    if ((node != null) && node.getNodeName().equals(TAG_CONSTS)) {
      ;
    } else {
      return false;
    }
    // /* DEBUG */ System.out.println("internalizing consts node...");
    Node n = node.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else {
        internalizeConst(n, builder);
      }
      n = n.getNextSibling();
    }
    return true;
  }

  static void internalizeConst(Node node, Builder builder) throws FormatException {
    ConstElem c = null;
    if ((c = internalizeConstNil(node)) != null) {
      ;
    } else if  ((c = internalizeConstInt(node)) != null) {
      ;
    } else if  ((c = internalizeConstReal(node)) != null) {
      ;
    } else if  ((c = internalizeConstCstr(node)) != null) {
      ;
    } else {
      throw new FormatException("Unknown const: " + node.getNodeName());
    }
    builder.putConstItem(c);
  }

  static NilConstElem internalizeConstNil(Node node) throws FormatException {
    if (!node.getNodeName().equals(TAG_NIL)) { return null; }
    return new NilConstElem();
  }

  static IntConstElem internalizeConstInt(Node node) throws FormatException {
    if (!node.getNodeName().equals(TAG_INT)) { return null; }
    NamedNodeMap attrs = node.getAttributes();
    Node aValue = attrs.getNamedItem(ATTR_VALUE);
    if (aValue == null) {
      throw new FormatException("'" + ATTR_VALUE + "' attribute not found.");
    }
    return createConstForInt(parseInt(aValue.getNodeValue()));
  }

  static RealConstElem internalizeConstReal(Node node) throws FormatException {
    if (!node.getNodeName().equals(TAG_REAL)) { return null; }
    NamedNodeMap attrs = node.getAttributes();
    Node aValue = attrs.getNamedItem(ATTR_VALUE);
    if (aValue == null) {
      throw new FormatException("'" + ATTR_VALUE + "' attribute not found.");
    }
    return createConstForReal(parseReal(aValue.getNodeValue()));
  }

  static CstrConstElem internalizeConstCstr(Node node) throws FormatException {
    if (!node.getNodeName().equals(TAG_CSTR)) { return null; }
    NamedNodeMap attrs = node.getAttributes();
    Node aValue = attrs.getNamedItem(ATTR_VALUE);
    if (aValue == null) {
      throw new FormatException("'" + ATTR_VALUE + "' attribute not found.");
    }
    return parseCstrConst(aValue.getNodeValue());
  }

  static void internalizeVMCode(Node node, int paramCount, Builder builder) throws FormatException {
    // /* DEBUG */ System.out.println("impl method = vmcode");
    NamedNodeMap attrs = node.getAttributes();
    int varCount = paramCount + 1;  // default: self and parameters
    Node aVarCount = attrs.getNamedItem(ATTR_VAR_COUNT);
    if (aVarCount != null) {
      varCount = parseInt(aVarCount.getNodeValue());
    }
    if (varCount < paramCount + 1) {
      throw new FormatException("Invalid " + ATTR_VAR_COUNT + ": " + varCount);
    }

    builder.startClosureImplVMCode(varCount);
    Node nInstSeq = skipIgnorableNodes(node.getFirstChild());
    if ((nInstSeq == null) || !nInstSeq.getNodeName().equals(TAG_INST_SEQ)) {
      throw new FormatException("'" + TAG_INST_SEQ + "' element not found.");
    }
    Node n = nInstSeq.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_I)) {
        builder.putInstruction(internalizeInstruction(n));
      } else {
        throw new FormatException("Unknown element under '" + TAG_INST_SEQ + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }

    Node nSrcInfoTab = skipIgnorableNodes(nInstSeq.getNextSibling());
    if ((nSrcInfoTab == null) || !nSrcInfoTab.getNodeName().equals(TAG_SRCINFO_TAB)) {
      throw new FormatException("'" + TAG_SRCINFO_TAB + "' element not found.");
    }
    n = nSrcInfoTab.getFirstChild();
    while (n != null) {
      if (isIgnorable(n)) {
        ;
      } else if (n.getNodeName().equals(TAG_S)) {
        builder.putSrcInfo(internalizeSrcInfo(n));
      } else {
        throw new FormatException("Unknown element under '" + TAG_SRCINFO_TAB + "' element: " + n.getNodeName());
      }
      n = n.getNextSibling();
    }
    builder.endClosureImplVMCode();
  }

  static void internalizeNativeImpl(Node node, int envCount, int paramCount, Builder builder) throws FormatException {
    // /* DEBUG */ System.out.println("impl method = native");
    if (envCount > 0) {
      throw new FormatException("env_count > 0 for native impl.");
    }

    NamedNodeMap attrs = node.getAttributes();
    Node aImpl = attrs.getNamedItem("impl");
    if (aImpl == null) {
      throw new FormatException("'impl' attribute not found.");
    }
    // builder.putClosureImplNative(/*index, */ aImpl.getNodeValue(), paramCount);
  }

  static void internalizeForeignImpl(Node node, /* int index, */ int envCount, int paramCount, Builder builder) throws FormatException {
    // /* DEBUG */ System.out.println("impl method = foreign");
    if (envCount > 0) {
      throw new FormatException("env_count > 0 for native impl.");
    }

    NamedNodeMap attrs = node.getAttributes();
    Node aModIndex = attrs.getNamedItem(ATTR_MOD_INDEX);
    if (aModIndex == null) {
      throw new FormatException("'mod_index' attribute not found.");
    }
    int modIndex = parseInt(aModIndex.getNodeValue()); 
    if (modIndex < 1) {
      throw new FormatException("Invalid mod_index: " + aModIndex.getNodeValue());
    }

    Node aFun = attrs.getNamedItem("fun");
    if (aFun == null) {
      throw new FormatException("'fun' attribute not found.");
    }
    // builder.putClosureImplForeign(/* index, */ modIndex, aFun.getNodeValue(), paramCount);
  }

  static MInstruction internalizeInstruction(Node node) throws FormatException {
    NamedNodeMap attrs = node.getAttributes();
    Node aOp = attrs.getNamedItem(ATTR_O);
    if (aOp == null) {
      throw new FormatException("'" + ATTR_O + "' attribute not found.");
    }

    String sP0 = null;
    String sP1 = null;
    Node aP0 = attrs.getNamedItem(ATTR_P);
    if (aP0 != null) {
      sP0 = aP0.getNodeValue();
      Node aP1 = attrs.getNamedItem(ATTR_Q);
      if (aP1 != null) {
        sP1 = aP1.getNodeValue();
      }
    }
    return MInstruction.parse(aOp.getNodeValue(), sP0, sP1);
  }

  static MSrcInfo internalizeSrcInfo(Node node) throws FormatException {
    return MSrcInfo.internalize(node);
  }

  static MType internalizeType(Node node) throws FormatException {
    return MType.Envelope.internalize(node);
  }

  static int parseInt(String s) throws FormatException {
    int x = 0;
    try {
      x = Integer.parseInt(s);
    } catch (Exception ex) {
      throw new FormatException("Invalid integer: " + s);
    }
    return x;
  }

  static double parseReal(String s) throws FormatException {
    double x = 0;
    try {
      x = Double.parseDouble(s);
    } catch (Exception ex) {
      throw new FormatException("Invalid integer: " + s);
    }
    return x;
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

  public Document externalize() {
    DocumentBuilder builder = null;
    try {
      builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    } catch (ParserConfigurationException ex) {
      System.err.println(ex);
      System.exit(-1);
    }
    Document doc = builder.getDOMImplementation().createDocument(null, null, null);

    Element moduleNode = doc.createElement(TAG_MODULE);
    doc.appendChild(moduleNode);
    moduleNode.setAttribute(ATTR_FORMAT_VERSION, CUR_FORMAT_VERSION);
    if (this.name != null) {
      moduleNode.setAttribute(ATTR_NAME, this.name.toJavaString());
    }
    if (this.availability != AVAILABILITY_GENERAL) {
      moduleNode.setAttribute(ATTR_AVAILABILITY, reprOfAvailability(this.availability));
    }
    if (this.slotCount != 0) {
      moduleNode.setAttribute(ATTR_SLOT_COUNT, Integer.toString(this.slotCount));
    }

    if (this.modTab.getSize() > 1) {
      moduleNode.appendChild(this.externalizeModRefs(doc));
    }
    if (this.dataDefs.length > 0) {
      moduleNode.appendChild(this.externalizeDataDefs(doc, this.dataDefs));
    }
    if (this.aliasTypeDefs.length > 0) {
      moduleNode.appendChild(this.externalizeAliasTypeDefs(doc, this.aliasTypeDefs));
    }
    if (this.featureDefs.length > 0) {
      moduleNode.appendChild(this.externalizeFeatureDefs(doc, this.featureDefs));
    }
    if (this.funDefs.length > 0) {
      moduleNode.appendChild(this.externalizeFunDefs(doc, this.funDefs));
    }
    if (this.closureImpls.length > 0) {
      moduleNode.appendChild(this.externalizeClosureImpls(doc));
    }
    if (this.dataConstrs.length > 0) {
      moduleNode.appendChild(this.externalizeDataConstrs(doc));
    }
    if (this.closureConstrs.length > 0) {
      moduleNode.appendChild(this.externalizeClosureConstrs(doc));
    }
    if (this.consts.length > 0) {
      moduleNode.appendChild(this.externalizeConsts(doc));
    }
    return doc;
  }

  Element externalizeModRefs(Document doc) {
    Element modRefsNode = doc.createElement(TAG_MOD_REFS);
    Cstr[] foreignMods = this.modTab.getForeignMods();
    for (int i = 0; i < foreignMods.length; i++) {
      modRefsNode.appendChild(this.externalizeModRef(doc, foreignMods[i]));
    }
    return modRefsNode;
  }

  Element externalizeModRef(Document doc, Cstr modName) {
    Element modRefNode = doc.createElement(TAG_MOD_REF);
    modRefNode.setAttribute(ATTR_MOD, modName.toJavaString());
    MDataDef[] dds = this.foreignDataDefsDict.get(modName);
    if (dds.length > 0) {
      modRefNode.appendChild(this.externalizeDataDefs(doc, dds));
    }
    MAliasTypeDef[] ads = this.foreignAliasTypeDefsDict.get(modName);
    if (ads.length > 0) {
      modRefNode.appendChild(this.externalizeAliasTypeDefs(doc, ads));
    }
    MFeatureDef[] ftds = this.foreignFeatureDefsDict.get(modName);
    if (ftds.length > 0) {
      modRefNode.appendChild(this.externalizeFeatureDefs(doc, ftds));
    }
    MFunDef[] fds = this.foreignFunDefsDict.get(modName);
    if (fds.length > 0) {
      modRefNode.appendChild(this.externalizeFunDefs(doc, fds));
    }
    return modRefNode;
  }

  Element externalizeDataDefs(Document doc, MDataDef[] dds) {
    Element dataDefsNode = doc.createElement(TAG_DATA_DEFS);
    for (int i = 0; i < dds.length; i++) {
      if (dds[i].params != null) {  // exclude "fun", "tuple"
        dataDefsNode.appendChild(this.externalizeDataDef(doc, dds[i]));
      }
    }
    return dataDefsNode;
  }

  Element externalizeDataDef(Document doc, MDataDef dataDef) {
    return dataDef.externalize(doc);
  }

  Element externalizeAliasTypeDefs(Document doc, MAliasTypeDef[] ads) {
    Element aliasTypeDefsNode = doc.createElement(TAG_ALIAS_TYPE_DEFS);
    for (int i = 0; i < ads.length; i++) {
      aliasTypeDefsNode.appendChild(this.externalizeAliasTypeDef(doc, ads[i]));
    }
    return aliasTypeDefsNode;
  }

  Element externalizeAliasTypeDef(Document doc, MAliasTypeDef aliasTypeDef) {
    return aliasTypeDef.externalize(doc);
  }

  Element externalizeFeatureDefs(Document doc, MFeatureDef[] fds) {
    Element featureDefsNode = doc.createElement(TAG_FEATURE_DEFS);
    for (int i = 0; i < fds.length; i++) {
      featureDefsNode.appendChild(this.externalizeFeatureDef(doc, fds[i]));
    }
    return featureDefsNode;
  }

  Element externalizeFeatureDef(Document doc, MFeatureDef featureDef) {
    return featureDef.externalize(doc);
  }

  Element externalizeFunDefs(Document doc, MFunDef[] fds) {
    Element funDefsNode = doc.createElement(TAG_FUN_DEFS);
    for (int i = 0; i < fds.length; i++) {
      funDefsNode.appendChild(this.externalizeFunDef(doc, fds[i]));
    }
    return funDefsNode;
  }

  Element externalizeFunDef(Document doc, MFunDef funDef) {
    return funDef.externalize(doc);
  }

  Element externalizeClosureImpls(Document doc) {
    Element closureImplsNode = doc.createElement(TAG_CLOSURE_IMPLS);
    for (int i = 0; i < this.closureImpls.length; i++) {
      closureImplsNode.appendChild(this.externalizeClosureImpl(doc, this.closureImpls[i]));
    }
    return closureImplsNode;
  }

  Element externalizeClosureImpl(Document doc, MClosureImpl closureImpl) {
    return closureImpl.externalize(doc);
  }

  Element externalizeDataConstrs(Document doc) {
    Element dataConstrsNode = doc.createElement(TAG_DATA_CONSTRS);
    for (int i = 0; i < this.dataConstrs.length; i++) {
      dataConstrsNode.appendChild(this.externalizeDataConstr(doc, this.dataConstrs[i]));
    }
    return dataConstrsNode;
  }

  Element externalizeDataConstr(Document doc, MDataConstr dataConstr) {
    return dataConstr.externalize(doc);
  }

  Element externalizeClosureConstrs(Document doc) {
    Element closureConstrsNode = doc.createElement(TAG_CLOSURE_CONSTRS);
    for (int i = 0; i < this.closureConstrs.length; i++) {
      closureConstrsNode.appendChild(this.externalizeClosureConstr(doc, this.closureConstrs[i]));
    }
    return closureConstrsNode;
  }

  Element externalizeClosureConstr(Document doc, MClosureConstr closureConstr) {
    return closureConstr.externalize(doc);
  }

  Element externalizeConsts(Document doc) {
    Element constsNode = doc.createElement(TAG_CONSTS);
    for (int i = 0; i < this.consts.length; i++) {
      constsNode.appendChild(this.externalizeConst(doc, this.consts[i]));
    }
    return constsNode;
  }

  Element externalizeConst(Document doc, ConstElem c) {
    return c.externalize(doc);
  }

  public static Element externalizeType(Document doc, MType type) {
    return MType.Envelope.externalize(doc, type);
  }

  public void writeTo(StreamResult r) throws TransformerException {
    TransformerFactory.newInstance().newTransformer().transform(
      new DOMSource(this.externalize()),
      r);
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {
    Module mod;
    Cstr currentForeignModName;
    List<Cstr> foreignModList;
    Map<Cstr, List<MDataDef>> foreignDataDefListDict;
    Map<Cstr, List<MAliasTypeDef>> foreignAliasTypeDefListDict;
    Map<Cstr, List<MFeatureDef>> foreignFeatureDefListDict;
    Map<Cstr, List<MFunDef>> foreignFunDefListDict;
    MDataDef.Builder dataDefBuilder;
    MConstrDef.Builder constrDefBuilder;
    MAttrDef currentAttrDef;
    List<MDataDef> dataDefList;
    MAliasTypeDef currentAliasTypeDef;
    List<MAliasTypeDef> aliasTypeDefList;
    Map<String, MFeatureDef> featureDefDict;
    List<MFeatureDef> featureDefList;
    Map<String, MFunDef> funDefDict;
    List<MFunDef> funDefList;
    MClosureImpl curClosureImpl;
    List<MClosureImpl> closureImplList;
    List<MInstruction> instSeq;
    List<MSrcInfo> srcInfoTab;
    List<MDataConstr> dataConstrList;
    MDataConstr curDataConstr;
    List<MClosureConstr> closureConstrList;
    MClosureConstr curClosureConstr;
    List<ConstElem> constList;

    Builder() {
      this.mod = new Module();
      this.foreignDataDefListDict = new HashMap<Cstr, List<MDataDef>>();
      this.foreignAliasTypeDefListDict = new HashMap<Cstr, List<MAliasTypeDef>>();
      this.foreignFeatureDefListDict = new HashMap<Cstr, List<MFeatureDef>>();
      this.foreignFunDefListDict = new HashMap<Cstr, List<MFunDef>>();
      this.mod.dataDefDict = new HashMap<String, MDataDef>();
      this.mod.constrDefDict = new HashMap<String, MConstrDef>();
      this.mod.featureDefDict = new HashMap<String, MFeatureDef>();
      this.mod.funDefDict = new HashMap<String, MFunDef>();
      this.foreignModList = new ArrayList<Cstr>();
      this.dataDefList = new ArrayList<MDataDef>();
      this.aliasTypeDefList = new ArrayList<MAliasTypeDef>();
      this.featureDefList = new ArrayList<MFeatureDef>();
      this.funDefList = new ArrayList<MFunDef>();
      this.closureImplList = new ArrayList<MClosureImpl>();
      this.dataConstrList = new ArrayList<MDataConstr>();
      this.closureConstrList = new ArrayList<MClosureConstr>();
      this.constList = new ArrayList<ConstElem>();
    }

    void setName(Cstr name) {
      this.mod.name = name;
    }

    void setAvailability(Availability availability) {
      this.mod.availability = availability;
    }

    void setSlotCount(int n) {
      this.mod.slotCount = n;
    }

    void startForeignMod(Cstr modName) {
      if (this.currentForeignModName != null) { throw new IllegalStateException("Foreign mod open."); }
      this.currentForeignModName = modName;
    }

    void endForeignMod() {
      if (this.currentForeignModName != null && this.currentForeignModName.equals(MOD_LANG)) {
        this.startDataDefSpecial(TCON_TUPLE, AVAILABILITY_GENERAL, ACC_PUBLIC);
        this.endDataDef();
        this.startDataDefSpecial(TCON_FUN, AVAILABILITY_GENERAL, ACC_PUBLIC);
        this.endDataDef();
      }
      this.foreignModList.add(this.currentForeignModName);
// /* DEBUG */ System.out.println("foreign data_def " + this.currentForeignModName.toJavaString() + " " + this.dataDefList);
      this.foreignDataDefListDict.put(this.currentForeignModName, this.dataDefList);
      this.foreignAliasTypeDefListDict.put(this.currentForeignModName, this.aliasTypeDefList);
      this.foreignFeatureDefListDict.put(this.currentForeignModName, this.featureDefList);
      this.foreignFunDefListDict.put(this.currentForeignModName, this.funDefList);
      this.currentForeignModName = null;
      this.dataDefList = new ArrayList<MDataDef>();
      this.aliasTypeDefList = new ArrayList<MAliasTypeDef>();
      this.featureDefList = new ArrayList<MFeatureDef>();
      this.funDefList = new ArrayList<MFunDef>();
    }

    void startDataDefSpecial(String tcon, Availability availability, Access acc) {
      // for tuple, fun
      this.dataDefBuilder = MDataDef.Builder.newInstance();
      this.dataDefBuilder.setTcon(tcon);
      this.dataDefBuilder.setAvailability(availability);
      this.dataDefBuilder.setAcc(acc);
      this.dataDefBuilder.setBaseModIndex(0);
      this.dataDefBuilder.setBaseTcon(null);
    }

    void startDataDef(String tcon, Availability availability, Access acc) {
      this.startDataDef(tcon, availability, acc, 0, null);
    }

    void startDataDef(String tcon, Availability availability, Access acc, int baseModIndex, String baseTcon) {
      this.dataDefBuilder = MDataDef.Builder.newInstance();
      this.dataDefBuilder.prepareForParams();
      this.dataDefBuilder.setTcon(tcon);
      this.dataDefBuilder.setAvailability(availability);
      this.dataDefBuilder.setAcc(acc);
      this.dataDefBuilder.setBaseModIndex(baseModIndex);
      this.dataDefBuilder.setBaseTcon(baseTcon);
    }

    void putDataDefParam(MTypeVar.DefWithVariance p) {
      this.dataDefBuilder.addParam(p);
    }

    void endDataDef() {
      MDataDef dd = this.dataDefBuilder.create();
      this.dataDefList.add(dd);
      this.mod.dataDefDict.put(dd.tcon, dd);
    }

    void startConstrDef(String dcon) {
      /* DEBUG */ if (dcon == null || dcon.length() == 0) throw new IllegalArgumentException("Invalid dcon " + dcon);
      this.constrDefBuilder = MConstrDef.Builder.newInstance();
      this.constrDefBuilder.setDcon(dcon);
    }

    void endConstrDef() {
      MConstrDef cd = this.constrDefBuilder.create();
      this.mod.constrDefDict.put(cd.dcon, cd);
      this.dataDefBuilder.addConstrDef(cd);
      this.constrDefBuilder = null;
    }

    void startAttrDef(String name) {
      this.currentAttrDef = MAttrDef.create(name);
    }

    void setAttrType(MType type) {
      this.currentAttrDef.setType(type);
    }

    void endAttrDef() {
      this.constrDefBuilder.addAttrDef(this.currentAttrDef);
      this.currentAttrDef = null;
    }

    void putDataConstr(int modIndex, String name, int attrCount, String tcon, int tparamCount) {
      this.dataConstrList.add(MDataConstr.create(modIndex, name, attrCount, tcon, tparamCount));
    }

    void addFeatureImplDef(int providerModIndex, String providerFun, String getter, MFeature provided) {
      MFeatureImplDef.Builder featureImplDefBuilder = MFeatureImplDef.Builder.newInstance();
      featureImplDefBuilder.setProvider(providerModIndex, providerFun, getter);
      featureImplDefBuilder.setProvided(provided);
      this.dataDefBuilder.addFeatureImplDef(featureImplDefBuilder.create());
    }

    void startAliasTypeDef(String tcon, Availability availability, Access acc, int paramCount) {
      this.currentAliasTypeDef = MAliasTypeDef.create(tcon, availability, acc, paramCount);
    }

    void setAliasBody(MType type) {
      this.currentAliasTypeDef.setBody(type);
    }

    void endAliasTypeDef() {
      this.putAliasTypeDef(this.currentAliasTypeDef);
      this.currentAliasTypeDef = null;
    }

    void putAliasTypeDef(MAliasTypeDef atd) {
      this.aliasTypeDefList.add(atd);
    }

    void putFeatureDef(MFeatureDef fd) {
      this.mod.featureDefDict.put(fd.fname, fd);
      this.featureDefList.add(fd);
    }

    void putFunDef(MFunDef fd) {
      this.mod.funDefDict.put(fd.name, fd);
      this.funDefList.add(fd);
    }

    void startClosureImpl(String name, int paramCount) {
      if (this.curClosureImpl != null) {
        throw new RuntimeException("Closure-impl is open.");
      }
      this.curClosureImpl = MClosureImpl.create(name, paramCount);
    }

    void endClosureImpl() {
      if (this.curClosureImpl == null) {
        throw new RuntimeException("Closure-impl is not open.");
      }
      this.closureImplList.add(this.curClosureImpl);
      this.curClosureImpl = null;
    }

    void startClosureImplVMCode(int varCount) {
      if (this.curClosureImpl == null) {
        throw new RuntimeException("Closure-impl is not open.");
      }
      this.curClosureImpl.setVarCount(varCount);
      this.instSeq = new ArrayList<MInstruction>();
      this.srcInfoTab = new ArrayList<MSrcInfo>();
    }

    void endClosureImplVMCode() {
      this.curClosureImpl.setCodeBlock(
        this.instSeq.toArray(new MInstruction[this.instSeq.size()]),
        this.srcInfoTab.toArray(new MSrcInfo[this.srcInfoTab.size()]));
      this.instSeq = null;
      this.srcInfoTab = null;
    }

    void putInstruction(MInstruction i) {
      if (this.instSeq == null) {
        throw new RuntimeException("VMcode is not open.");
      }
      this.instSeq.add(i);
    }

    void putInstruction(String op, int[] params) {
      this.putInstruction(MInstruction.create(op, params));
    }

    void putSrcInfo(int upperCodeIndex, String loc) {
      this.putSrcInfo(MSrcInfo.create(upperCodeIndex, loc));
    }

    void putSrcInfo(MSrcInfo si) {
      this.srcInfoTab.add(si);
    }

    int putUniqueDataConstrLocal(String dcon, int attrCount, String tcon, int tparamCount) {
      MDataConstr dc = MDataConstr.create(0, dcon, attrCount, tcon, tparamCount);
      int index = this.dataConstrList.indexOf(dc);
      if (index < 0) {
        index = this.dataConstrList.size();
        this.dataConstrList.add(dc);
      } else if (tcon != null) {
        MDataConstr dc2 = this.dataConstrList.get(index);
        if (dc2.tcon == null) {  // rewrite for adding type sig info
          dc2.tcon = tcon;
          dc2.tparamCount = tparamCount;
        }
      }
      return index;
    }

    int startUniqueDataConstrForeign(int modIndex, String dcon, int attrCount, String tcon, int tparamCount) {
      MDataConstr dc = MDataConstr.create(modIndex, dcon, attrCount, tcon, tparamCount);
      int index = this.dataConstrList.indexOf(dc);
      if (index < 0) {
        this.curDataConstr = dc;
      } else if (tcon != null) {
        MDataConstr dc2 = this.dataConstrList.get(index);
        if (dc2.tcon == null) {  // rewrite for adding type sig info
          dc2.tcon = tcon;
          dc2.tparamCount = tparamCount;
        }
      }
      return index;
    }

    int endUniqueDataConstrForeign() {
      if (this.curDataConstr == null) {
        throw new RuntimeException("data-constr not set up.");
      }
      int index = this.dataConstrList.size();
      this.dataConstrList.add(this.curDataConstr);
      this.curDataConstr = null;
      return index;
    }

    int putUniqueClosureConstrLocal(String name, int envCount) {
      MClosureConstr cc = MClosureConstr.create(MOD_INDEX_SELF, name, envCount);
      int index = this.closureConstrList.indexOf(cc);
      if (index < 0) {
        index = this.closureConstrList.size();
        this.closureConstrList.add(cc);
      }
      return index;
    }

    int startUniqueClosureConstrForeign(int modIndex, String fun) {
      MClosureConstr cc = MClosureConstr.create(modIndex, fun, 0);
      int index = this.closureConstrList.indexOf(cc);
      if (index < 0) {
        this.curClosureConstr = cc;
      }
      return index;
    }

    int endUniqueClosureConstrForeign() {
      if (this.curClosureConstr == null) {
        throw new RuntimeException("clsoure-constr not set up.");
      }
      int index = this.closureConstrList.size();
      this.closureConstrList.add(this.curClosureConstr);
      this.curClosureConstr = null;
      return index;
    }

    void putClosureConstr(int modIndex, String name, int envCount) {
      this.closureConstrList.add(MClosureConstr.create(modIndex, name, envCount));
    }

    int putUniqueConstItem(ConstElem item) {
      int index = this.constList.indexOf(item);
      if (index < 0) {
        index = this.constList.size();
        this.constList.add(item);
      }
      return index;
    }

    void putConstItem(ConstElem item) {
      this.constList.add(item);
    }

    public Module create() {
      this.mod.modTab = ModTab.create(this.mod.name);
      for (int i = 0; i < this.foreignModList.size(); i++) {
        this.mod.modTab.add(this.foreignModList.get(i));
      }
      if (this.mod.name != null && this.mod.name.equals(MOD_LANG)) {
        this.startDataDefSpecial(TCON_TUPLE, AVAILABILITY_GENERAL, ACC_PUBLIC);
        this.endDataDef();
        this.startDataDefSpecial(TCON_FUN, AVAILABILITY_GENERAL, ACC_PUBLIC);
        this.endDataDef();
      }
      this.mod.foreignDataDefsDict = new HashMap<Cstr, MDataDef[]>();
      this.mod.foreignAliasTypeDefsDict = new HashMap<Cstr, MAliasTypeDef[]>();
      this.mod.foreignFeatureDefsDict = new HashMap<Cstr, MFeatureDef[]>();
      this.mod.foreignFunDefsDict = new HashMap<Cstr, MFunDef[]>();
      Cstr[] foreignMods = this.mod.modTab.getForeignMods();
      for (int i = 0; i < foreignMods.length; i++) {
        List<MDataDef> dl = this.foreignDataDefListDict.get(foreignMods[i]);
        this.mod.foreignDataDefsDict.put(foreignMods[i], dl.toArray(new MDataDef[dl.size()]));
        List<MAliasTypeDef> al = this.foreignAliasTypeDefListDict.get(foreignMods[i]);
        this.mod.foreignAliasTypeDefsDict.put(foreignMods[i], al.toArray(new MAliasTypeDef[al.size()]));
        List<MFeatureDef> ftl = this.foreignFeatureDefListDict.get(foreignMods[i]);
        this.mod.foreignFeatureDefsDict.put(foreignMods[i], ftl.toArray(new MFeatureDef[ftl.size()]));
        List<MFunDef> fl = this.foreignFunDefListDict.get(foreignMods[i]);
        this.mod.foreignFunDefsDict.put(foreignMods[i], fl.toArray(new MFunDef[fl.size()]));
      }
      this.mod.dataDefs = this.dataDefList.toArray(new MDataDef[this.dataDefList.size()]);
      this.mod.aliasTypeDefs = this.aliasTypeDefList.toArray(new MAliasTypeDef[this.aliasTypeDefList.size()]);
      this.mod.featureDefs = this.featureDefList.toArray(new MFeatureDef[this.featureDefList.size()]);
      this.mod.funDefs = this.funDefList.toArray(new MFunDef[this.funDefList.size()]);
      this.mod.closureImpls = this.closureImplList.toArray(new MClosureImpl[this.closureImplList.size()]);
      this.mod.dataConstrs = this.dataConstrList.toArray(new MDataConstr[this.dataConstrList.size()]);
      this.mod.closureConstrs = this.closureConstrList.toArray(new MClosureConstr[this.closureConstrList.size()]);
      this.mod.consts = this.constList.toArray(new ConstElem[this.constList.size()]);
      return this.mod;
    }
  }

  static class ModTab {
    List<Cstr> tab; 

    static ModTab create(Cstr ownerModName) {
      return new ModTab(ownerModName);
    }

    private ModTab(Cstr ownerModName) {
      this.tab = new ArrayList<Cstr>();
      if (ownerModName == null || !ownerModName.equals(MOD_LANG)) {
        this.tab.add(ownerModName);
      }
      this.tab.add(MOD_LANG);
    }

    int getSize() { return this.tab.size(); }

    void add(Cstr modName) {
      if (this.tab.indexOf(modName) < 0) {
        this.tab.add(modName);
      }
    }

    Cstr get(int index) {
      return this.tab.get(index);
    }

    Cstr getMyModName() { return this.get(0); }

    int lookup(Cstr modName) {
      int index = this.tab.indexOf(modName);
      if (index < 0) { throw new IllegalArgumentException("Not found. " + modName.toJavaString()); }
      return index;
    }

    Cstr[] getAllMods() {
      Cstr[] ms = new Cstr[this.tab.size()];
      for (int i = 0; i < ms.length; i++) {
        ms[i] = this.tab.get(i);
      }
      return ms;
    }

    Cstr[] getForeignMods() {
      Cstr[] ms = new Cstr[this.tab.size() - 1];
      for (int i = 0, j = 1; i < ms.length; i++, j++) {
        ms[i] = this.tab.get(j);
      }
      return ms;
    }
  }

  static String reprOfAcc(Access acc) {  // repr in xml
    String r;
    if (acc == ACC_PUBLIC) {
      r = REPR_PUBLIC;
    } else if (acc == ACC_PROTECTED) {
      r = REPR_PROTECTED;
    } else if (acc == ACC_OPAQUE) {
      r = REPR_OPAQUE;
    } else if (acc == ACC_PRIVATE) {
      r = REPR_PRIVATE;
    } else {
      throw new IllegalArgumentException();
    }
    return r;
  }

  static String reprOfAvailability(Availability availability) {
    String r;
    if (availability == AVAILABILITY_GENERAL) {
      r = REPR_GENERAL;
    } else if (availability == AVAILABILITY_ALPHA) {
      r = REPR_ALPHA;
    } else if (availability == AVAILABILITY_BETA) {
      r = REPR_BETA;
    } else if (availability == AVAILABILITY_LIMITED) {
      r = REPR_LIMITED;
    } else if (availability == AVAILABILITY_DEPRECATED) {
      r = REPR_DEPRECATED;
    } else {
      throw new IllegalArgumentException();
    }
    return r;
  }

  static Availability parseAvailabilityAttr(String s) {
    Availability av;
    if (s.equals(REPR_GENERAL)) {
      av = AVAILABILITY_GENERAL;
    } else if (s.equals(REPR_ALPHA)) {
      av = AVAILABILITY_ALPHA;
    } else if (s.equals(REPR_BETA)) {
      av = AVAILABILITY_BETA;
    } else if (s.equals(REPR_LIMITED)) {
      av = AVAILABILITY_LIMITED;
    } else if (s.equals(REPR_DEPRECATED)) {
      av = AVAILABILITY_DEPRECATED;
    } else {
      throw new IllegalArgumentException("Invalid " + ATTR_AVAILABILITY + ": " + s);
    }
    return av;
  }

  interface Elem {
    Element externalize(Document doc);
  }

  interface ConstElem extends Elem {
  }

  static IntConstElem createConstForInt(int value) {
    return new IntConstElem(value);
  }

  static class IntConstElem implements ConstElem {
    int value;

    IntConstElem(int value) {
      this.value = value;
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof IntConstElem)) {
        b = false;
      } else {
        IntConstElem c = (IntConstElem)o;
        b = c.value == this.value;
      }
      return b;
    }

    public Element externalize(Document doc) {
      Element n = doc.createElement(TAG_INT);
      n.setAttribute(ATTR_VALUE, Integer.toString(this.value));
      return n;
    }
  }

  static RealConstElem createConstForReal(double value) {
    return new RealConstElem(value);
  }

  static class RealConstElem implements ConstElem {
    double value;

    RealConstElem(double value) {
      this.value = value;
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof RealConstElem)) {
        b = false;
      } else {
        RealConstElem c = (RealConstElem)o;
        b = c.value == this.value;
      }
      return b;
    }

    public Element externalize(Document doc) {
      Element n = doc.createElement(TAG_REAL);
      n.setAttribute(ATTR_VALUE, Double.toString(this.value));
      return n;
    }
  }

  static CstrConstElem createConstForCstr(Cstr cstr) {
    return new CstrConstElem(cstr);
  }

  static CstrConstElem createConstForCstr(String s) {
    return new CstrConstElem(new Cstr(s));
  }

  static CstrConstElem parseCstrConst(String s) throws FormatException {
    return new CstrConstElem(unescapeFromXMLValue(s));
  }

  static class CstrConstElem implements ConstElem {
    Cstr value;

    CstrConstElem(Cstr s) {
      this.value = s;
    }

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (!(o instanceof CstrConstElem)) {
        b = false;
      } else {
        CstrConstElem c = (CstrConstElem)o;
        b = c.value.equals(this.value);
      }
      return b;
    }

    public Element externalize(Document doc) {
      Element n = doc.createElement(TAG_CSTR);
      n.setAttribute(ATTR_VALUE, escapeToXMLValue(this.value));
      return n;
    }
  }

  static NilConstElem createConstForNil() {
    return new NilConstElem();
  }

  static class NilConstElem implements ConstElem {
    NilConstElem() {}

    public boolean equals(Object o) {
      boolean b;
      if (o == this) {
        b = true;
      } else if (o instanceof NilConstElem) {
        b = true;
      } else {
        b = false;
      }
      return b;
    }

    public Element externalize(Document doc) {
      return  doc.createElement(TAG_NIL);
    }
  }

  public static boolean moreOpenAcc(Access a0, Access a1) {
    return a0.openness > a1.openness;
  }

  public static boolean equalOrMoreOpenAcc(Access a0, Access a1) {
    return a0.openness >= a1.openness;
  }

  public void checkDefsCompat(Map<Cstr, Module> modDict) throws FormatException {
    Cstr[] foreignMods = this.modTab.getForeignMods();
    for (int i = 0; i < foreignMods.length; i++) {
      Cstr m = foreignMods[i];
      Module defMod = modDict.get(m);
      if (defMod == null) {
        throw new RuntimeException("Module not found. - " + m.repr());
      }
      MDataDef[] dds = this.foreignDataDefsDict.get(m);
      if (dds != null && dds.length > 0) {
        this.checkDataDefsCompat(dds, defMod);
      }
      MFunDef[] fds = this.foreignFunDefsDict.get(m);
      if (fds != null && fds.length > 0) {
        this.checkFunDefsCompat(fds, defMod);
      }
    }
  }

  void checkDataDefsCompat(MDataDef[] dds, Module defMod) throws FormatException {
    for (int i = 0; i < dds.length; i++) {
      MDataDef dd = dds[i];
      MDataDef ddd = defMod.dataDefDict.get(dd.tcon);
      if (ddd == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Definition missing - type: ");
        emsg.append(dd.tcon);
        emsg.append(", referred in: ");
        emsg.append(this.name.repr());
        emsg.append(" defined in: ");
        emsg.append(defMod.name.repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      dd.checkCompat(this.modTab, ddd, defMod.modTab);
    }
  }

  void checkFunDefsCompat(MFunDef[] fds, Module defMod) throws FormatException {
    for (int i = 0; i < fds.length; i++) {
      MFunDef fd = fds[i];
      MFunDef dfd = defMod.funDefDict.get(fd.name);
      if (dfd == null) {
        StringBuffer emsg = new StringBuffer();
        emsg.append("Definition missing - function: ");
        emsg.append(fd.name);
        emsg.append(", referred in: ");
        emsg.append(this.name.repr());
        emsg.append(" defined in: ");
        emsg.append(defMod.name.repr());
        emsg.append(".");
        throw new FormatException(emsg.toString());
      }
      fd.checkCompat(this.modTab, dfd, defMod.modTab);
    }
  }

  static String escapeToXMLValue(Cstr s) {
    StringBuffer buf = new StringBuffer();
    for (int i = 0; i < s.getLength(); i++) {
      int c = s.getCharAt(i);
      if (c == '`' || c < 0x20) {
        buf.append("`u");
        buf.append(Integer.toHexString(c));
        buf.append(";");
      } else {  // HERE: more escape needed?
        buf.appendCodePoint(c);
      }
    }
    return buf.toString();
  }

  static Cstr unescapeFromXMLValue(String s) throws FormatException {
    Cstr str = new Cstr(s);
    Cstr ret = new Cstr();
    int state = 0;
    int cp = 0;
    for (int i = 0; i < str.getLength(); i++) {
      int c = str.getCharAt(i);
      switch (state) {
      case 0:
        if (c == '`') {
          state = 1;
        } else {
          ret.append(c); state = 0;
        }
        break;
      case 1:
        if (c == 'u') {
          cp = 0; state = 2;
        } else {
          throw new FormatException("Invalid escape character.");
        }
        break;
      case 2:
        if ('0' <= c && c <= '9') {
          cp <<= 4; cp += c - '0'; state = 2;
        } else if ('A' <= c && c <= 'F') {
          cp <<= 4; cp += c - 'A' + 10; state = 2;
        } else if ('a' <= c && c <= 'f') {
          cp <<= 4; cp += c - 'a' + 10; state = 2;
        } else if (c == ';') {
          ret.append(cp); state = 0;
        } else {
          throw new FormatException("Invalid hex character.");
        }
        if (cp > 0x10ffff) {
          throw new FormatException("Too big Unicode.");
        }
        break;
      }
    }
    if (state > 0) {
      throw new FormatException("Incomplete escape sequence.");
    }
    return ret;
  }

  static boolean isValidModName(Cstr modName) {
    boolean b;
    int L = modName.getLength();
    b = L > 0;
    for (int i = 0; b && i < L; i++) {
      switch (modName.getCharAt(i)) {
      case 0: b = false; break;
      default: break;
      }
    }
    return b;
  }
}
