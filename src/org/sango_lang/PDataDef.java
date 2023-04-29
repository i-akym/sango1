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

public interface PDataDef {

  String getFormalTcon();

  PDefDict.IdKey getBaseTconKey();

  int getParamCount();  // -1 if variable count  // needed?

  PTypeSkel getTypeSig();

  Module.Variance getParamVarianceAt(int pos);

  Module.Availability getAvailability();

  Module.Access getAcc();

  int getConstrCount();  // 0 if native impl

  Constr getConstr(String dcon);

  Constr getConstrAt(int index);

  int getFeatureImplCount();

  FeatureImpl getFeatureImplAt(int index);

  interface Constr {

    String getDcon();

    int getAttrCount();

    Attr getAttrAt(int i);

    int getAttrIndex(String name);  // -1 if not found

    PTypeSkel getType(PTypeSkelBindings bindings);

  }

  interface Attr {
  
    String getName();

    PTypeSkel getNormalizedType() throws CompileException;

    PTypeSkel getFixedType();
  
  }

  interface FeatureImpl {
    // HERE
  }
}
