/***************************************************************************
 * MIT License                                                             *
 * Copyright (c) 2024 AKIYAMA Isao                                         *
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

public class Version {
  static final int MAJOR = 1;
  static final int MINOR = 7;
  static final int MICRO = 8;
  static final String LEVEL = null;
  static final int BUILD = 1;

  static final Version instance = new Version(MAJOR, MINOR, MICRO, LEVEL, BUILD);

  public static Version getInstance() { return instance; }

  public int major;
  public int minor;
  public int micro;
  public String level;
  public int build;
  public String full;

  private Version(int major, int minor, int micro, String level, int build) {
    this.major = major;
    this.minor = minor;
    this.micro = micro;
    this.level = (level == null || level.length() == 0)? null: level;
    this.build = build;
    String sMajor = Integer.toString(this.major);
    String sMinor = Integer.toString(this.minor);
    String sMicro = Integer.toString(this.micro);
    String sLevel = (this.level != null)? "-" + this.level: "";
    String sBuild = Integer.toString(this.build);
    this.full = sMajor + "." + sMinor + "." + sMicro + sLevel + "_" + sBuild;
  }

  public String toString() { return this.full; }
}
