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

// Usage:
//   java ConfigTool echo "phrase"
//   java ConfigTool echoq "phrase"  -- with double qutations
//   java ConfigTool nl  -- newline
//   java ConfigTool get keyword
//          keyword: byte_order, encoding
//   java ConfigTool replace s s'

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import org.sango_lang.Cstr;

public class ConfigTool {

  public static void main(String[] args) {
    try {
      run(args);
    } catch (Exception ex) {
      System.out.println(ex);
      ex.printStackTrace(System.out);
      System.exit(1);
    }
  }

  static void run(String[] args) throws Exception {
    if (args[0].equals("echo")) {
      System.out.print(args[1]);
    } else if (args[0].equals("echoq")) {
      System.out.print("\""); System.out.print(args[1]); System.out.print("\"");
    } else if (args[0].equals("nl")) {
      System.out.println();
    } else if (args[0].equals("get")) {
      System.out.print(get(args[1]));
    } else if (args[0].equals("replace")) {
      replace(args[1], args[2]);
    } else {
      throw new RuntimeException("Unknown command. " + args[0]);
    }
  }

  static String get(String keyword) {
    String s;
    if (keyword.equals("byte_order")) {
      s = stringRepr(ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN)? "big endian": "little endian");
    } else if (keyword.equals("encoding")) {
      s = stringRepr(System.getProperty("file.encoding"));
    } else {
      throw new RuntimeException("Unknown keyword. " + keyword);
    }
    return s;
  }

  static void replace(String before, String after) throws Exception {
    BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
    String line;
    while ((line = r.readLine()) != null) {
      System.out.println(line.replace(before, after));
    }
  }

  static String stringRepr(String s) {
    return (new Cstr(s)).repr();
  }
}
