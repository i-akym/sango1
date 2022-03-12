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

interface PTypeDesc extends PProgElem {

  void setupScope(PScope scope);  // inherited

  PTypeDesc resolve() throws CompileException;  // inherited

  PTypeDesc deepCopy(Parser.SrcInfo srcInfo, int extOpt, int varianceOpt, int concreteOpt);
  static final int COPY_EXT_KEEP = -1;
  static final int COPY_EXT_OFF = 0;
  static final int COPY_EXT_ON = 1;
  static final int COPY_VARIANCE_KEEP = -1;
  static final int COPY_VARIANCE_INVARIANT = 0;
  static final int COPY_VARIANCE_COVARIANT = 1;
  static final int COPY_VARIANCE_CONTRAVARIANT = 2;
  static final int COPY_CONCRETE_KEEP = -1;
  static final int COPY_CONCRETE_OFF = 0;
  static final int COPY_CONCRETE_ON = 1;

  PDefDict.TconInfo getTconInfo();

  void excludePrivateAcc() throws CompileException;

  PTypeSkel getSkel();

  PTypeSkel normalize();

}
