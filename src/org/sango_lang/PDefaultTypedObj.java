/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2022 AKIYAMA Isao                                         *
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

abstract class PDefaultTypedObj extends PDefaultProgObj implements PTypedObj {
  PType type;
  PTypeSkel nTypeSkel;
  PTypeGraph.Node typeGraphNode;

  PDefaultTypedObj(Parser.SrcInfo srcInfo, PScope scope) {
    super(srcInfo, scope);
  }

  public PType getType() { return this.type; }

  public PTypeSkel getNormalizedType() throws CompileException {
    if (this.nTypeSkel == null && this.type != null) {
      this.nTypeSkel = this.type.getNormalizedSkel();
    }
    return this.nTypeSkel;
  }

  public PTypeGraph.Node setupTypeGraph(PTypeGraph graph) throws CompileException {
    throw new RuntimeException("PTypedObj#setupTypeGraph called. - " + this.toString());
  }

  public PTypeGraph.Node getTypeGraphNode() { return this.typeGraphNode; }

  public void setTypeGraphNode(PTypeGraph.Node node) {
    this.typeGraphNode = node;
  }

  public PTypeSkel getFixedType() {
// /* DEBUG */ System.out.print(this); System.out.print(this.nTypeSkel); System.out.println(this.typeGraphNode);
    return (this.nTypeSkel != null)? this.nTypeSkel: this.typeGraphNode.getFixedType();
  }
}
