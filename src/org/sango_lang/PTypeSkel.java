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

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface PTypeSkel {

  Parser.SrcInfo getSrcInfo();

  int getCat();
  static final int CAT_BOTTOM = 1;
  static final int CAT_SOME = 2;
  static final int CAT_VAR = 3;

  boolean isLiteralNaked();

  boolean isConcrete();

  boolean isConcrete(PTypeSkelBindings bindings);

  PTypeSkel instanciate(InstanciationBindings iBindings);

  PTypeSkel resolveBindings(PTypeSkelBindings bindings);

  boolean accept(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings);
  // where, width is
  static final int EQUAL = 0;
  static final int NARROWER = 1;
  static final int WIDER = - NARROWER;

  boolean includesVar(PTypeVarSlot varSlot, PTypeSkelBindings bindings);

  PTypeVarSlot getVarSlot();

  PTypeSkel join(PTypeSkel type, List<PTypeVarSlot> givenTVarList);
    // foward to following method internally
  JoinResult join2(int width, boolean bindsRef, PTypeSkel type, PTypeSkelBindings bindings);

  MType toMType(PModule mod, List<PTypeVarSlot> slotList);

  void extractVars(List<PTypeVarSlot> extracted);

  void collectTconProps(List<PDefDict.TconProps> list);

  PTypeSkel unalias(PTypeSkelBindings bindings);

  Repr repr();

  public static class JoinResult {
    PTypeSkel joined;
    PTypeSkelBindings bindings;

    public static JoinResult create(PTypeSkel joined, PTypeSkelBindings bindings) {
      JoinResult r = new JoinResult();
      r.joined = joined;
      r.bindings = bindings;
      return r;
    }

    private JoinResult() {}
  }

  public static class InstanciationBindings {
    PTypeSkelBindings applBindings;
    Map<PTypeVarSlot, PTypeVarSkel> bindingDict;

    public static InstanciationBindings create(PTypeSkelBindings applBindings) {
      InstanciationBindings ib = new InstanciationBindings();
      ib.applBindings = applBindings;
      ib.bindingDict = new HashMap<PTypeVarSlot, PTypeVarSkel>();
      return ib;
    }

    private InstanciationBindings() {}

    boolean isGivenTVar(PTypeVarSlot var) { return this.applBindings.givenTVarList.contains(var); }

    boolean isInFeatureImpl(PTypeVarSlot var) { return this.applBindings.isInFeatureImpl(var); }

    boolean isBound(PTypeVarSlot var) {
      return this.bindingDict.containsKey(var);
    }

    void bind(PTypeVarSlot var, PTypeVarSkel vs) {
      PTypeVarSlot s = vs.getVarSlot();
      if (this.bindingDict.containsKey(var)) {
        throw new IllegalArgumentException("Already added. " + var);
      }
      this.bindingDict.put(var, vs);
    }

    PTypeVarSkel lookup(PTypeVarSlot var) {
      return this.bindingDict.get(var);
    }

    boolean isBoundAppl(PTypeVarSlot var) {
      return this.applBindings.isBound(var);
    }

    PTypeSkel lookupAppl(PTypeVarSlot var) {
      return this.applBindings.lookup(var);
    }
  }

  public static class Repr {
    public static String topLevelRepr(PTypeSkel t) {
      return "<" + t.repr().toString() + ">";
    }

    static Repr create() {
      return new Repr();
    }

    List<String> items;

    private Repr() {
      this.items = new ArrayList<String>();
    }

    void add(String s) {
      if (s.length() > 0) {
        this.items.add(s);
      }
    }

    void add(Repr r) {
      if (r.items.isEmpty()) {
        ;
      } else if (r.items.size() == 1) {
        this.items.add(r.toString());
      } else {
        this.items.add("<" + r.toString() + ">");
      }
    }

    void append(Repr r) {
      this.items.addAll(r.items);
    }

    public String toString() {  // without top-level < >
      StringBuffer buf = new StringBuffer();
      String sep = "";
      for (int i = 0; i < this.items.size(); i++) {
        buf.append(sep);
        buf.append(this.items.get(i));
        sep = " ";
      }
      return buf.toString();
    }
  }
}
