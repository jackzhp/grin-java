package grin;

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.net.ProtocolFamily;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.ByteBuffer;

import java.io.IOException;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
//import static net.tox.ConnectionUDPserver.achannels6;
//import static net.tox.Tox.uitox;
//import zede.messenger.Messenger;
//import static zede.messenger.Messenger.executor;
//import static zede.messenger.Messenger.tox;
//import static net.tox.Constants.;
//import static net.tox.Constants.;
//import static net.tox.Constants.;
//import static net.tox.Constants.;

/**
 *
 *
 *
 *
 */
public class Networking_Core extends Thread {

    /**
     * remote ConnectPoints are in ConnectPoingUDP/TCP.CPs
     *
     * local ConnectionUDPserver are in ConnectionUDPserver.achannels4/6, local
     * TCP_Server are in TCP_Server.achannels4/6.
     *
     * so in fact, they are redundant.
     *
     */
    //static LinkedList<InetSocketAddress> llisa = new LinkedList<>();
//    public static LinkedList<ConnectPoint> cpsLocal = new LinkedList<>();

    ProtocolFamily family; //Family family;
    //short port; //local or remote? must be local, I guess
    /* Our UDP socket. */
    //DatagramChannel channel; //Socket sock;
    static java.nio.channels.MembershipKey mkey; //multicast membership key
    public static Networking_Core one = new Networking_Core();
    static boolean tobesent, quit;
    static byte ipversions; //1 for ipv4, 2: ipv6, 3:both
    static Selector selector;
    static long ltsReqSentLast, ltsRecvLast, //all network receiving
            ltsReqSentLastWAN, ltsRecvLastWAN; //this one is overall all ConnectPoint that has access to WAN.
    static HashSet<InetAddress> wans = new HashSet<>();

    static void sendReqWAN() {
        ltsReqSentLast = ltsReqSentLastWAN = System.currentTimeMillis();
    }

    static void recvFromWAN(InetAddress ia) {
        ltsRecvLast = ltsRecvLastWAN = System.currentTimeMillis();
        wans.add(ia);
    }

    static boolean sameWANIP(InetAddress ia) {
        return wans.contains(ia);
    }

    static boolean isWANdead() {
        if (ltsReqSentLastWAN <= ltsRecvLastWAN) {
            return false;
        }
        //now we have last received is before last sent
        long ltsnow = System.currentTimeMillis();
        /**
         * if last request sent, and did not get response within 5 seconds, we
         * can deem WAN is dead.
         *
         * Be aware that the last sent might not be of request, but of response.
         * In that case, we can not expect response.
         *
         */
        return ltsReqSentLastWAN + 5000 < ltsnow;
    }

    static boolean isNetworkDead() {
        if (ltsReqSentLast <= ltsRecvLast) {
            return false;
        }
        //now we have last received is before last sent
        long ltsnow = System.currentTimeMillis();
        /**
         * if last request sent, and did not get response within 5 seconds, we
         * can deem WAN is dead.
         *
         * Be aware that the last sent might not be of request, but of response.
         * In that case, we can not expect response.
         *
         */
        return ltsReqSentLast + 5000 < ltsnow;
    }

    Networking_Core() {
        super("network");
//        ListenerMSG handle_LANdiscovery = one.getHandlerDiscoveryLan();
//        one.dht_get_net().networking_registerhandler((byte) NET_PACKET_LAN_DISCOVERY, handle_LANdiscovery);//&handle_LANdiscovery, dht);
    }

    /**
     * https://tools.ietf.org/html/rfc6052#section-2.1
     *
     *
     * You can get the MAC address from the IPv6 local address. E.g., the IPv6
     * address "fe80::1034:56ff:fe78:9abc" corresponds to the MAC address
     * "12-34-56-78-9a-bc". 
     *
     *
     * @param a
     * @return
     */
    static boolean IPV6_IPV4_IN_V6(InetAddress a) {
        if (!(a instanceof Inet6Address)) {
            throw new IllegalArgumentException();
        }
        byte[] aip = a.getAddress();
        int i = 0;
        for (i = 0; i < 10; i++) {
            if (aip[i] != 0) {
                return false;
            }
        }
        if (aip[i++] != 0xff || aip[i++] != 0xff) {
            return false;
        }
        return true;
    }

//    static InetAddress getV4fromV6(InetAddress ia6) {
//        if (ia6 instanceof Inet6Address) {
//        } else {
//            throw new IllegalArgumentException();
//        }
//        byte[] aip = ia6.getAddress();
//        int i = 0;
//        for (i = 0; i < 10; i++) {
//            if (aip[i] != 0) {
//                throw new IllegalArgumentException();
//            }
//        }
//        if (aip[i++] != 0xff || aip[i++] != 0xff) {
//            throw new IllegalArgumentException();
//        }
//        byte[] aip4 = new byte[4];
//        System.arraycopy(aip, 12, aip4, 0, 4);
//        InetAddress ia4 = null;
//        try {
//            ia4 = InetAddress.getByAddress(aip4);
//        } catch (Throwable t) {
//        }
//        return ia4;
//    }
    public static final int INADDR_LOOPBACK = 0x7f000001;
    //too broad?

//IP4 get_ip4_loopback()
//{
//    IP4 loopback=new IP4(INADDR_LOOPBACK); //loopback.uint32 = htonl(INADDR_LOOPBACK);
//    return loopback;
//}
//IP6 get_ip6_loopback()
//{
//    IP6 loopback=new IP6(new byte[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,1}); //get_ip6(&loopback, &in6addr_loopback);
//    return loopback;
//}
    static long ltsLast; //this is now. not anything else.

    /**
     * should be moved to Util return current monotonic time in milliseconds
     * (ms).
     */
    static long current_time_monotonic() {
        long ltsnow = System.currentTimeMillis();
        if (ltsnow == ltsLast) {
            ltsnow++;
        }
        return ltsLast = ltsnow;//time;
    }

    //ConcurrentLinkedQueue<ByteBuffer> q2send=new ConcurrentLinkedQueue<>();
    public static void quit() {
        quit = true;
        selector.wakeup(); //20180720 null happened.
    }

    /**
     * do not change its parameter to InetAddress
     *
     * @param IA
     * @return
     */
    public static LinkedList<ConnectPoint> getConnectPoint(InterfaceAddress IA) {
        LinkedList<ConnectPoint> llcp = new LinkedList<>();
        InetAddress ia = IA.getAddress();
        InetAddress iaBroadcast = IA.getBroadcast();
//        for (ConnectPoint cp : cpsLocal) {
//            InetAddress iat = cp.isa.getAddress();
//            if (iat.equals(ia) || iat.equals(iaBroadcast)) {
//                llcp.add(cp);
//            }
//        }
        return llcp;
    }

    private static ManagerNetwork manager;

    @Override
    public void run() {
//        String javaHome = System.getProperty("java.home");
//        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
//        String classpath = System.getProperty("java.class.path");
//        String className = "EchoServer.class.getCanonicalName()";
//
//        ProcessBuilder builder = new ProcessBuilder(javaBin, "-cp", classpath, className);
//
//        return builder.start();
        try {
            Networking_Core.networking_poll();
        } catch (IOException e) {
            if (manager != null) {
                try {
                    manager.onExceptionSelect(e);
                } catch (Throwable t) {
                }
            }
        }
        System.out.println("network thread quit");
//        ConnectionUDPserver.clear(); //call this before/after quit the loop?
//        TCP_Server.clear();
    }

    static Networking_Core init() throws IOException {
        selector = Selector.open();
        return one;
    }

    //BeNoted that, this is a dead loop if using .select
    //if use .selectNow it will return
    //now I let it be a dead loop
    public static synchronized void networking_poll() throws IOException {
        //unix_time_update();
        //InetSocketAddress ip_port;
        //byte[]  data=new byte[MAX_UDP_PACKET_SIZE];
        //int  length;
        java.nio.channels.SelectableChannel sc = null;
        Object att = null;
        int readies = 0;
        while (quit == false) {
            selector.select(); //.selectNow();//
            try {
                checkPendingEvents();
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
            java.util.Set<SelectionKey> selectedKeys = selector.selectedKeys();
            java.util.Iterator<SelectionKey> iter = selectedKeys.iterator();
            try {
                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    sc = key.channel();
                    readies = key.readyOps();
                    att = key.attachment();
                    //System.out.println("\nreadies:"+Integer.toHexString(readies));
                    //TODO: I can go to con directly.
                    //might be TCP connection or UDP connection
//                if (key.isAcceptable()) {
//                    register(selector, serverSocket);
//                }
                    if (att instanceof ListenerSocketData) {
                        ListenerSocketData ln = (ListenerSocketData) att;
                        if ((readies & SelectionKey.OP_WRITE) != 0) { //4
                            ln.doWrite(sc);
                            continue;
                        }
                        if ((readies & SelectionKey.OP_READ) != 0) { //1
                            ln.doRead(sc);
                        }
                        if ((readies & SelectionKey.OP_CONNECT) != 0) { //8
                            if (att instanceof TCPclient) {
                                TCPclient con = (TCPclient) att;
                                con.onConnectable();
                            } else {
                                System.out.println("key attachment is not ConnectionTCPclient, but " + att.getClass().getName());
                            }
                        }
                    } else if (att instanceof ListenerSocketServer) {
                        if ((readies & SelectionKey.OP_ACCEPT) != 0) {
                            ServerSocketChannel channelServer = (ServerSocketChannel) sc;
                            SocketChannel client = channelServer.accept();
                            if (client == null) {
                                //TODO: how to avoid this unnecessary notification?
                            } else {
                                ListenerSocketServer lss = (ListenerSocketServer) att;
                                lss.onConnectIncoming(client);
                            }
                        }
                    } else {
                        System.out.println("key attachment is unrecognized:" + att.getClass().getName());
                    }
                    iter.remove(); //we do not have to remove it, we just go to the next item
                }
            } catch (Throwable t) {
                try {
                    throw new Exception("readies:" + Integer.toHexString(readies) + "att:" + att + " channel:" + sc, t);
                } catch (Throwable t2) {
                    t2.printStackTrace(System.out);
                }
            }
            try {
                checkPendingEvents();
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }
        System.out.println("network thread quit already!");
        System.out.flush();
    }

    private static void checkPendingEvents() {
//        LinkedList<ListenerSocketData> llo = new LinkedList<>();
//        nextEvent:
//        while (true) {
//            ListenerSocketData lsd = qEwrite.poll();
//            if (lsd == null) {
//                break;
//            }
//            for (ListenerSocketData lsdt : llo) {
//                if (lsdt == lsd) {
//                    continue nextEvent;
//                }
//            }
//            llo.add(lsd);
//        }
//        //whether interestOps will block, is implementation dependent
//        //so we had better treat it as blocking.
//        for (ListenerSocketData lsd : llo) {
//            int target, bits = -1;
//            SelectionKey key = lsd.key();
//            if (lsd.waiting2write() == false) {
//                bits ^= SelectionKey.OP_WRITE;
//                target = key.interestOps() & bits;
//            } else {
//                target = key.interestOps() | SelectionKey.OP_WRITE;
//            }
//            key.interestOps(target);
//        }
        while (true) {
            Operation o = qO.poll();
            if (o == null) {
                break;
            }
            try {
                o.run();
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }
    }

    static boolean ipport_equal(final InetSocketAddress a, final InetSocketAddress b) {
        return a.equals(b);
    }

    static boolean ipport_isset(final InetSocketAddress ipport) {
        if (ipport == null) {
            return false;
        }
//
//    if (ipport.port==0) {
//        return false;
//    }
//
//    return ip_isset(ipport.ip);
        return true;
    }
//the following, learned from http://www.baeldung.com/java-nio-selector
    private static final String POISON_PILL = "POISON_PILL";

//    public static void main(String[] args) throws IOException {
//    }
//    static void listen(SocketChannel sc, ListenerSocketData lsd) throws IOException {
//        sc.configureBlocking(false);
//        //why two OP_READ? and no OP_WRITE?
//        sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, lsd);
//    }
//    static void listenREAD(SelectableChannel sc, ListenerSocketData lsd) throws IOException {
//        qO.add(new OperationRegister(SelectionKey.OP_READ,sc,lsd));
//    }
    static final ConcurrentLinkedQueue<Operation> qO = new ConcurrentLinkedQueue<>();

    static java.util.Enumeration<NetworkInterface> e;

    public static java.util.Enumeration<NetworkInterface> NICs() {
        if (e == null) {
            /**
             * TODO: for desktop, this does not change but for cellphone, this
             * might change over time.
             */
            try {
                e = NetworkInterface.getNetworkInterfaces();
            } catch (Throwable t) {
                t.printStackTrace(System.out);
            }
        }
        return e;
    }

    //not used at present
    //static void listenWRITE(SocketChannel sc, ListenerSocketData lsd) throws IOException {
    static void listenWRITE(SelectableChannel sc, ListenerSocketData lsd) throws IOException {
        //sc.configureBlocking(false);
        SelectionKey k = sc.register(selector, SelectionKey.OP_WRITE, lsd);
    }

    public static void change(int interests, SelectionKey key, ListenerSocketData lsd) {
        qO.add(new OperationChange(interests, key, lsd));
        selector.wakeup();
    }

    public static void listen(int interests, SelectableChannel sc, ListenerSocketData lsd) {
        if (sc == null) {
            throw new IllegalArgumentException();
        }
        qO.add(new OperationRegister(interests, sc, lsd));
        selector.wakeup();
    }

    static void register(RegisterInterests ri) {
        qO.add(ri);
        selector.wakeup();
    }

    public static void connect(InetSocketAddress isa, SocketChannel sc, ListenerSocketData lsd) {
        qO.add(new OperationConnect(isa, sc, lsd));
        selector.wakeup();
    }

    public static void listen(ServerSocketChannel serverSocket, ListenerSocketServer listener) throws IOException {
        qO.add(new OperationRegisterServer(serverSocket, listener));
        selector.wakeup();
    }
//    static ConcurrentLinkedQueue<ListenerSocketData> qEwrite = new ConcurrentLinkedQueue<>();

//    static void nothing2write(ListenerSocketData lsd) {
//        qEwrite.offer(lsd);
//        selector.wakeup();
//    }
//    static void wants2write(ListenerSocketData lsd) {
//        qEwrite.offer(lsd);
//        selector.wakeup();
//    }
    private static void answerWithEcho(ByteBuffer buffer, SelectionKey key)
            throws IOException {

        SocketChannel client = (SocketChannel) key.channel();
        client.read(buffer);
        if (new String(buffer.array()).trim().equals(POISON_PILL)) {
            client.close();
            System.out.println("Not accepting client messages anymore");
        }

        buffer.flip();
        client.write(buffer);
        buffer.clear();
    }

    public static ProtocolFamily address2family(InetAddress ia) {
        if (ia instanceof java.net.Inet4Address) {
            return StandardProtocolFamily.INET;
        } else if (ia instanceof java.net.Inet6Address) {
            return StandardProtocolFamily.INET6;
        }
        throw new IllegalArgumentException("unknown address:" + ia.getClass().getName());
    }

    public static boolean isIPv4(InetSocketAddress isa) {
        InetAddress ia = isa.getAddress();
        return ia instanceof java.net.Inet4Address;
    }

    public static boolean isIPv6(InetSocketAddress isa) {
        InetAddress ia = isa.getAddress();
        return ia instanceof java.net.Inet6Address;
    }

    public static boolean isIPv4(InetAddress ia) {
        return ia instanceof java.net.Inet4Address;
    }

    public static boolean isIPv6(InetAddress ia) {
        return ia instanceof java.net.Inet6Address;
    }

    public static boolean ip_isset(InetSocketAddress isa) {
        //return isa!=null;
        return isa != null;
    }

    static boolean isThis(InetSocketAddress isa) {
//        for (ConnectPoint ip_port : cpsLocal) {
//            if (isa.equals(ip_port.isa)) {
//                return true;
//            }
//        }
        return false;
    }

    static boolean isLocal(InetAddress ia) {
//        for (ConnectPoint ip_port : cpsLocal) {
//            if (ia.equals(ip_port.isa.getAddress())) {
//                return true;
//            } else {
//                //System.out.println(ia+" : "+ip_port.isa);
//            }
//        }
        return false;
    }

    /**
     * 20190219 added, not used yet.
     *
     * my purpose is to disconnect.
     */
    public static int nBound() {
        int nbroadcast = 0, n = 0;
//        Iterator<ConnectPoint> I = cpsLocal.iterator();
//        while (I.hasNext()) {
//            ConnectPoint cp = I.next();
//            if (cp.isBroadcastListening) {
//                nbroadcast++;
//                continue;
//            }
//            n++;
//        }
        System.out.println("broadcasting:" + nbroadcast + " ordinary:" + n);
        return n;
    }

    /**
     * broadcast listening is not cleared. No, all cleared.
     *
     */
    public static void clear() {
//        ConnectionUDPserver.clear();
//        TCP_Server.clear();
//
////        Iterator<ConnectPoint> I=llisa.iterator();
////        while(I.hasNext()){
////            ConnectPoint cp=I.next();
////            if(cp.isBroadcastListening){
////                continue;
////            }
////        }
//        if (cpsLocal.isEmpty()) {
//        } else {
//            throw new IllegalStateException("not cleared:" + cpsLocal.size());
//        }
////        cpsLocal.clear();
//        LAN.clear();
    }

//    public static ConnectPoint newConnectPoint(InterfaceAddress IA, boolean tcp) {
//        return newConnectPoint(IA, tcp ? Messenger.portTCP() : Messenger.portUDP(), tcp);
//    }
//
//    public static ConnectPoint newConnectPoint(InterfaceAddress IA, int port, boolean tcp) {
//        ConnectPoint cp = null;
//        InetAddress ia = IA.getAddress();
//        if (Networking_Core.isIPv6(ia) && Tox.mtox.supportIP6()
//                || Networking_Core.isIPv4(ia) && Tox.mtox.supportIP4()) {
//        } else {
//            throw new IllegalStateException();
//        }
//
//        if (tcp) {
//            cp = TCP_Server.init(IA, port); //, llt
//        } else {
//            if (Tox.mtox.udp_enabled()) {   //TODO: do I really need this control here? I feel not.
//                cp = ConnectionUDPserver.init(IA, port); //, llt
//            } else {
//                throw new IllegalStateException("UDP is disabled");
//            }
//        }
////        LinkedList<Throwable> llt = new LinkedList<Throwable>();
////        try {
////        } catch (Throwable t) {
////            llt.add(t);
////        }
////        if (llt.isEmpty() == false) {
////            throw new IllegalStateException(llt.getFirst());//uitox.onThrowable(llt);
////        }
//        return cp;
//    }

    static interface Operation extends Runnable {
    }

    static interface RegisterInterests extends Operation {
    }

    static class OperationRegister implements Operation {

        int interests;
        SelectableChannel sc;
        ListenerSocketData lsd;

        private OperationRegister(int interests, SelectableChannel sc, ListenerSocketData lsd) {
            this.interests = interests;
            this.sc = sc;
            this.lsd = lsd;
        }

        @Override
        public void run() {
            try {
                sc.configureBlocking(false);
//                SelectionKey key = sc.register(selector, SelectionKey.OP_READ, lsd);
//20181112 I found the above line does not use interests
                SelectionKey key = sc.register(selector, interests, lsd);
                lsd.on(key);
            } catch (IOException e) {
                lsd.on(e);
            }
        }
    }

    static class OperationChange implements Operation {

        int interests;
        SelectionKey key;
        ListenerSocketData lsd;

        private OperationChange(int interests, SelectionKey key, ListenerSocketData lsd) {
            if (key == null) {
                throw new IllegalArgumentException();
            }
            this.interests = interests;
            this.key = key;
            this.lsd = lsd;
        }

        @Override
        public void run() {
            try {
                key.interestOps(interests);
            } catch (Throwable e) {
                lsd.on(e);
            }
        }
    }

    static class OperationConnect implements Operation {

        private InetSocketAddress isa;
        SocketChannel sc;
        ListenerSocketData lsd;

        private OperationConnect(InetSocketAddress isa, SocketChannel sc, ListenerSocketData lsd) {
            this.isa = isa;
            this.sc = sc;
            this.lsd = lsd;
        }

        @Override
        public void run() {
            try {
                sc.configureBlocking(false);
                SelectionKey key = sc.register(selector, SelectionKey.OP_CONNECT, lsd);
                key.attach(lsd);
                lsd.on(key);
//                String msg = TCP_Connections.tcp_connections_length + " tcp connect to " + isa;
//                System.out.println(msg);
                if (sc.connect(isa)) {
                    System.out.println("connect return true");
                    //in this case, do we still get onConnectable notification? 
                    //I guess so. hence I do not have to do anything here.
                }
            } catch (IOException e) {
                lsd.on(e);
            }
        }
    }

    static class OperationRegisterServer implements Operation {

        //int interests;
        ServerSocketChannel ss;
        ListenerSocketServer lss;

        private OperationRegisterServer(ServerSocketChannel ss, ListenerSocketServer lss) {
            this.ss = ss;
            this.lss = lss;
        }

        @Override
        public void run() {
            try {
                ss.configureBlocking(false);
                SelectionKey key = ss.register(selector, SelectionKey.OP_ACCEPT, lss);
                //lss.on(key);
            } catch (IOException e) {
                lss.on(e);
            }
        }
    }

    public static interface TCPclient {

        void onConnectable();
    }
}

interface ManagerNetwork {

    void onExceptionSelect(Exception e);
}

interface ListenerSocketData {

    /**
     *
     * @param channel
     * @throws IOException the exception occurred during write on the channel
     * then close the channel.
     *
     * @param channel
     * @return true if has more data waiting to be written
     */
    public void doWrite(java.nio.channels.SelectableChannel channel);

    /**
     *
     * @param channel
     * @throws IOException the exception occurred during reading on the channel.
     * then close the channel.
     */
    public void doRead(java.nio.channels.SelectableChannel channel);
//    abstract SelectableChannel channel();

    //boolean waiting2write();
    boolean waiting2write();

    //SelectionKey key();//{ return key;}
    void on(Throwable e);

    void on(SelectionKey key);
}

interface ListenerSocketServer {

    void onConnectIncoming(SocketChannel client);

    void on(IOException e);

    //void on(SelectionKey key);
}
class ConnectPoint {
    
}
class LAN{
    
}