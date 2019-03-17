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
package sni_sango.sni_net;
 
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import org.sango_lang.Cstr;
import org.sango_lang.Module;
import org.sango_lang.RArrayItem;
import org.sango_lang.RClosureItem;
import org.sango_lang.RDataConstr;
import org.sango_lang.RFrame;
import org.sango_lang.RIntItem;
import org.sango_lang.RNativeImplHelper;
import org.sango_lang.RObjItem;
import org.sango_lang.RType;
import org.sango_lang.RuntimeEngine;
import org.sango_lang.RStructItem;

public class SNIinet {
  public static SNIinet getInstance(RuntimeEngine e) {
    return new SNIinet();
  }

  SNIinet() {}

  public void sni_create_connection_socket_impl(RNativeImplHelper helper, RClosureItem self) {
    try {
      helper.setReturnValue(new ConnectionSocketImplPItem(helper.getRuntimeEngine(), new ConnectionSocketImpl(new Socket())));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_csi_connect(RNativeImplHelper helper, RClosureItem self, RObjItem csi, RObjItem addr, RObjItem port, RObjItem timeout) {
    ConnectionSocketImpl impl = ((ConnectionSocketImplPItem)csi).impl;
    int p = ((RIntItem)port).getValue();
    int to = ((RIntItem)timeout).getValue();
    if (0 <= p && p <= 65535) {
      ;
    } else {
      helper.setException(sni_sango.SNIlang.createBadArgException(helper, new Cstr("Invalid port number."), null));
      return;
    }
    try {
      impl.connect(helper, toInetAddress(helper, addr), p, to);
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_csi_close(RNativeImplHelper helper, RClosureItem self, RObjItem csi) {
    ConnectionSocketImpl impl = ((ConnectionSocketImplPItem)csi).impl;
    try {
      impl.close();
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_csi_instream(RNativeImplHelper helper, RClosureItem self, RObjItem csi) {
    ConnectionSocketImpl impl = ((ConnectionSocketImplPItem)csi).impl;
    try {
      helper.setReturnValue(impl.getInstream(helper));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_csi_outstream(RNativeImplHelper helper, RClosureItem self, RObjItem csi) {
    ConnectionSocketImpl impl = ((ConnectionSocketImplPItem)csi).impl;
    try {
      helper.setReturnValue(impl.getOutstream(helper));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_csi_open_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem csi) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_csi_opt(RNativeImplHelper helper, RClosureItem self, RObjItem csi, RObjItem key) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_csi_set_opt(RNativeImplHelper helper, RClosureItem self, RObjItem csi, RObjItem key, RObjItem value) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_csi_opt_keys(RNativeImplHelper helper, RClosureItem self, RObjItem csi) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_create_server_socket_impl(RNativeImplHelper helper, RClosureItem self, RObjItem port) {
    int p = ((RIntItem)port).getValue();
    try {
      helper.setReturnValue(new ServerSocketImplPItem(helper.getRuntimeEngine(), new ServerSocketImpl(new ServerSocket(p))));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_ssi_accept(RNativeImplHelper helper, RClosureItem self, RObjItem ssi) {
    ServerSocketImpl impl = ((ServerSocketImplPItem)ssi).impl;
    try {
      Socket s = impl.accept();
      helper.setReturnValue(new ConnectionSocketImplPItem(helper.getRuntimeEngine(), new ConnectionSocketImpl(s)));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_ssi_close(RNativeImplHelper helper, RClosureItem self, RObjItem ssi) {
    ServerSocketImpl impl = ((ServerSocketImplPItem)ssi).impl;
    try {
      impl.close();
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_ssi_open_Q_(RNativeImplHelper helper, RClosureItem self, RObjItem ssi) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_ssi_opt(RNativeImplHelper helper, RClosureItem self, RObjItem ssi, RObjItem key) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_ssi_set_opt(RNativeImplHelper helper, RClosureItem self, RObjItem ssi, RObjItem key, RObjItem value) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_ssi_opt_keys(RNativeImplHelper helper, RClosureItem self, RObjItem ssi) {
    throw new RuntimeException("Not implemented.");
  }

  public void sni_create_datagram_socket_impl(RNativeImplHelper helper, RClosureItem self, RObjItem port) {
    int p = ((RIntItem)port).getValue();
    try {
      helper.setReturnValue(new DatagramSocketImplPItem(helper.getRuntimeEngine(), new DatagramSocketImpl(new DatagramSocket(p))));
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_dsi_receive(RNativeImplHelper helper, RClosureItem self, RObjItem dsi, RObjItem len) {
    DatagramSocketImpl impl = ((DatagramSocketImplPItem)dsi).impl;
    int L = ((RIntItem)len).getValue();
    try {
      byte[] buf = new byte[L];
      DatagramPacket pkt = new DatagramPacket(buf, L);
      impl.receive(pkt);
      InetAddress addr = pkt.getAddress();
      RObjItem oAddr = helper.cstrToArrayItem(new Cstr(addr.getHostName()));
      RObjItem oPort = helper.getIntItem(pkt.getPort());
      int received = pkt.getLength();
      RArrayItem oData = helper.createArrayItem(received);
      for (int i = 0; i < received; i++) {
        oData.setElemAt(i, helper.getByteItem(sni_sango.SNIio.byteToUnsignedInt(buf[i])));
      }
      RObjItem val = helper.getTupleItem(new RObjItem[] { oAddr, oPort, oData });
      RDataConstr dcValue = helper.getDataConstr(Module.MOD_LANG, "value$");
      RObjItem oValue = helper.getStructItem(dcValue, new RObjItem[] { val });
      helper.setReturnValue(oValue);
    } catch (SocketTimeoutException ex) {
      RDataConstr dcNone = helper.getDataConstr(Module.MOD_LANG, "none$");
      RObjItem oNone = helper.getStructItem(dcNone, new RObjItem[0]);
      helper.setReturnValue(oNone);
    } catch (Exception ex) {
// /* DEBUG */ ex.printStackTrace(System.out);
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_dsi_send(RNativeImplHelper helper, RClosureItem self, RObjItem dsi, RObjItem addr, RObjItem port, RObjItem data) {
    DatagramSocketImpl impl = ((DatagramSocketImplPItem)dsi).impl;
    int p = ((RIntItem)port).getValue();
    RArrayItem aData = (RArrayItem)data;
    int len = aData.getElemCount();
    byte[] bs = new byte[len];
    for (int i = 0; i < len; i++) {
      bs[i] = (byte)((RIntItem)aData.getElemAt(i)).getValue();
    }
    try {
      DatagramPacket pkt = new DatagramPacket(bs, len, toInetAddress(helper, addr), p);
      impl.send(pkt);
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  public void sni_dsi_close(RNativeImplHelper helper, RClosureItem self, RObjItem ssi) {
    DatagramSocketImpl impl = ((DatagramSocketImplPItem)ssi).impl;
    try {
      impl.close();
    } catch (Exception ex) {
      helper.setException(sni_sango.SNIio.createIOErrorIOFailureException(helper, new Cstr(ex.toString()), null));
    }
  }

  static InetAddress toInetAddress(RNativeImplHelper helper, RObjItem addr) throws Exception {  // temporal impl -- simplest
    RStructItem a = (RStructItem)addr;
    String name = helper.arrayItemToCstr((RArrayItem)a.getFieldAt(0)).toJavaString();
    return InetAddress.getByName(name);
  }

  class ConnectionSocketImpl {
    Socket socket;
    RObjItem instream;
    RObjItem outstream;

    ConnectionSocketImpl(Socket s) {
      this.socket = s;
    }

    void connect(RNativeImplHelper helper, InetAddress addr, int port, int timeout) throws Exception {
      this.socket.connect(new InetSocketAddress(addr, port), timeout);
    }

    RObjItem getInstream(RNativeImplHelper helper) throws Exception {
      if (this.instream == null) {
        this.instream = sni_sango.SNIio.openByteInstreamFromJavaInputStream(helper, this.socket.getInputStream());
      }
      return this.instream;
    }
    
    RObjItem getOutstream(RNativeImplHelper helper) throws Exception {
      if (this.outstream == null) {
        this.outstream = sni_sango.SNIio.openByteOutstreamToJavaOutputStream(helper, this.socket.getOutputStream());
      }
      return this.outstream;
    }

    void close() throws Exception {
      this.socket.close();
    }
  }

  class ServerSocketImpl {
    ServerSocket ssocket;

    ServerSocketImpl(ServerSocket s) {
      this.ssocket = s;
    }

    Socket accept() throws Exception {
      return this.ssocket.accept();
    }
    
    void close() throws Exception {
      this.ssocket.close();
    }
  }

  class DatagramSocketImpl {
    DatagramSocket dsocket;

    DatagramSocketImpl(DatagramSocket s) {
      this.dsocket = s;
    }

    void receive(DatagramPacket pkt) throws Exception {
      this.dsocket.receive(pkt);
    }

    void send(DatagramPacket pkt) throws Exception {
      this.dsocket.send(pkt);
    }

    void close() throws Exception {
      this.dsocket.close();
    }
  }

  class ConnectionSocketImplPItem extends RObjItem {
    ConnectionSocketImpl impl;

    ConnectionSocketImplPItem(RuntimeEngine e, ConnectionSocketImpl impl) {
      super(e);
      this.impl = impl; 
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }
  
    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.net.inet"), "connection_socket_impl_p", 0);
    }

    public Cstr debugReprOfContents() {
      return new Cstr(this.toString());
    }
  }

  class ServerSocketImplPItem extends RObjItem {
    ServerSocketImpl impl;

    ServerSocketImplPItem(RuntimeEngine e, ServerSocketImpl impl) {
      super(e);
      this.impl = impl;
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }
  
    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.net.inet"), "server_socket_impl_p", 0);
    }

    public Cstr debugReprOfContents() {
      return new Cstr(this.toString());
    }
  }

  class DatagramSocketImplPItem extends RObjItem {
    DatagramSocketImpl impl;

    DatagramSocketImplPItem(RuntimeEngine e, DatagramSocketImpl impl) {
      super(e);
      this.impl = impl;
    }

    public boolean objEquals(RFrame frame, RObjItem item) {
      return item == this;
    }
  
    public RType.Sig getTsig() {
      return RType.createTsig(new Cstr("sango.net.inet"), "datagram_socket_impl_p", 0);
    }

    public Cstr debugReprOfContents() {
      return new Cstr(this.toString());
    }
  }
}
