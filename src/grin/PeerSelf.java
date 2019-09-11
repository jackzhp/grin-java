package grin;

import static grin.Peer.writeString;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import zede.util.Hex;

/**
 *
 * @author jack
 */
public class PeerSelf extends Peer {

    //as a mini wallet, we choose not to participate into this.
    //we just ignore stem tx.
    Dandelion dandelion = new Dandelion();

    /**
     * this method is only defined for self.
     *
     * @param isas
     */
    ConcurrentHashMap<InetSocketAddress, Peer> peers = new ConcurrentHashMap<>();

    /**
     * I want to use 2 files to store peers list
     *
     *
     * @return
     */
    int idxFilePeers;
    AtomicBoolean savingPeers = new AtomicBoolean();
    boolean savePeersAgain;

    CompletableFuture<Void> savePeers() {
        return CompletableFuture.runAsync(() -> {
            if (savingPeers.compareAndSet(false, true)) {
                try {
                    do {
                        savePeersAgain = false;
                        String fn = "peers." + idxFilePeers;
                        BufferedOutputStream bos = new java.io.BufferedOutputStream(new FileOutputStream(fn));
                        Map.Entry<InetSocketAddress, Peer>[] ame = (Map.Entry<InetSocketAddress, Peer>[]) peers.entrySet().toArray(new Map.Entry[0]);
                        byte[] bytes = new byte[32];
                        ByteBuffer bb = ByteBuffer.wrap(bytes);
                        for (Map.Entry<InetSocketAddress, Peer> me : ame) {
                            Peer peer = me.getValue();
                            if (peer.tNetwrok == null) {
                                InetSocketAddress isa = me.getKey();
                                bb.position(0);
                                write(bb, isa);
                                bos.write(bytes, 0, bb.position());
                            }
                        }
                        bos.flush();
                        bos.close();
                        idxFilePeers ^= 1;
                    } while (savePeersAgain);
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    savingPeers.set(false);
                }
            } else {
                savePeersAgain = true;
            }
        });
    }

    CompletableFuture<Void> loadPeers() {
        return CompletableFuture.runAsync(() -> {
            nPeersReceiving = Integer.MAX_VALUE;
            for (int idx = 0; idx < 2; idx++) {
                String fn = "peers." + idx;
                File f = new File(fn);
                if (f.exists() == false) {
                    continue;
                }
                try {
                    BufferedInputStream bis = new java.io.BufferedInputStream(new FileInputStream(fn));
                    byte[] bytes = new byte[4096];
                    int istart = 0;
                    while (true) {
                        int len = bis.read(bytes, istart, bytes.length - istart);
                        if (len == -1) {
                            break;
                        }
                        ByteBuffer bb = ByteBuffer.wrap(bytes, 0, istart + len);
                        this.onReceivedPeerAddrs(bb);
                        bb.compact();
                        istart = bb.position();
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            nPeersReceiving = 0;
        });
    }

    void onReceivedPeerAddrs(ArrayList<InetSocketAddress> isas) {
        CompletableFuture.runAsync(() -> {
            try {
                for (InetSocketAddress isa : isas) {
                    Peer peer = peers.get(isa);
                    if (peer == null) {
                        peers.put(isa, new Peer().set(isa, this));
                        //TODO: we might not want to enable it
                    }
                }
                ;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).thenCompose(tf -> {
            return savePeers();
        }).exceptionally(t -> {
            t.printStackTrace();
            return (Void) null;
        });

    }

    /**
     * this method is only defined for self.
     *
     */
    void onReceivedHand(MsgHand hand) {

        System.out.println("to send shake back, not implemented yet");
    }

    void onReceivedHand(ByteBuffer bb) {
        MsgHand hand = new MsgHand(); //seems not needed.
        hand.read(bb);
        System.out.println(hand.dump(new StringBuilder()));
        onReceivedHand(hand);
    }

    /**
     * TODO: a lot of work to be done for this.
     *
     * we are receive tx or just tx hash?
     *
     * @param bb
     */
    void onReceivedTxHashSetArchive(ByteBuffer bb) {
        byte[] hash = new byte[32];
        bb.get(hash);
        long height = bb.getLong();
        bytesTxHashSetArchive = bb.getLong();
        blkTxHashSetArchive = null;
        onTxHashSetArchive = true;
        //TODO: now as stream to read bytes.
        readTxHashSetArchive(bb);
    }

    void readTxHashSetArchive(ByteBuffer bb) {
        ArrayList<byte[]> al = new ArrayList<>();
        while (bytesTxHashSetArchive > 0) {
            int len = bb.remaining();
            if (len > 32) {
                byte[] hash = new byte[32];
                bb.get(hash);
                al.add(hash);
                bytesTxHashSetArchive -= 32;
            } else {
                break;
            }
        }
        onTxHashSetArchive = bytesTxHashSetArchive > 0;
        blkTxHashSetArchive.onTxHashSetArchive(al, onTxHashSetArchive);
    }
    boolean onTxHashSetArchive;
    long bytesTxHashSetArchive; //the remaining
    Block blkTxHashSetArchive;

    void onBanReason(ByteBuffer bb) {
        int reason = bb.getInt();
        onBanReason(reason);
    }

    void onBanReason(int reason) {
        System.out.println("ban reason:" + reason);
    }

    void onReceivedShake(Peer peer) {
        System.out.println("on received shake");
        CompletableFuture.runAsync(() -> {
            if (true) { //request peers
                ByteBuffer bb = ByteBuffer.allocate(1024);
                peer.writeGetPeerAddrs(bb, 0x0f);
                peer.writeMsg(Peer.GetPeerAddrs, bb);
            }
            if (false) { //request the genesis block
                ByteBuffer bb = ByteBuffer.allocate(1024);
                peer.writeGetBlock(bb, genesis.genesis_hash);
                peer.writeMsg(Peer.GetBlock, bb);
            }
        }).exceptionally(t -> {
            t.printStackTrace();
            return (Void) null;
        });
    }

    void connectToPeers() {
        //pickOneToConnect();
        connectToAll();
    }
boolean first=true;
    void pickOneToConnect() {
        if (true) {
            try {
                InetSocketAddress isa;
                if (true) {
                    String sHost;
                    sHost="192.168.0.9";
                    //sHost="120.24.200.27";
/**
 * use command to find a new node: ./grin client listconnectedpeers
 * 
 * 
 * 
 */                    
                    InetAddress ia = InetAddress.getByName(sHost);
                    isa = new InetSocketAddress(ia, 3414);
                } else {
                    isa = null;
                }
                Peer peer = peers.get(isa);
                if (peer == null) {
                    peer = new Peer().set(isa, this);
                    peers.put(isa, peer);
                }else
                    System.out.println("not null");
                if(peer.state==0){
                    System.out.println("will connect to " + isa);
                    peer.connect();
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
            return;
        }
        /**
         * set a timer or a timeout callback to ensure the peer is connected. if
         * not, then pick another one.
         *
         *
         */
        int size = peers.size();
        if (size == 0) {
            throw new IllegalStateException("no peer");
        }
        int idx = Peer.random.nextInt(size);
        Iterator<Map.Entry<InetSocketAddress, Peer>> I = peers.entrySet().iterator();
        int i = 0;
        Peer peer = null;
        while (I.hasNext()) {
            peer = I.next().getValue();
            i++;
            if (i == idx) {
                break;
            }
        }
        peer.connect();
    }

    void connectToAll() {
        for (Map.Entry<InetSocketAddress, Peer> me : peers.entrySet()) {
            Peer peer = me.getValue();
            if (peer.state == 0) {
                peer.connect();
            }
        }
    }

    /*
0000   61 3d 06 00 00 00 00 00 00 07 04 
                                        00 00 01 00 
                                                    00   a=..............
0010   65 5d f6 a3 0d 56 00 95 ca 35 25 0d 56 00 53 17   e]ö£.V..Ê5%.V.S.
0020   5d eb 0d 56 00 56 81 2a 8d 0d 56 00 23 e3 9b 55   ]ë.V.V.*..V.#ã.U
0030   0d 56 00 34 35 dd 0f 0d 56 00 d3 38 42 24 0d 56   .V.45Ý..V.Ó8B$.V
0040   00 34 3b ee b2 0d 56 00 23 b8 54 0d 0d 56 00 7e   .4;î².V.#¸T..V.~
0050   6c f3 e3 0d 56 00 05 09 57 07 0d 56 00 23 ee 76   lóã.V...W..V.#îv
0060   88 0d 56 00 2d 20 ec a6 0d 56 00 58 ca f6 4a 0d   ..V.- ì¦.V.XÊöJ.
0070   56 00 d8 dd 57 be 0d 56 00 79 4b 94 ff 0d 56 00   V.ØÝW¾.V.yK.ÿ.V.
0080   53 f0 52 9d 0d 56 00 6f 47 39 ca 0d 56 00 79 1f   SðR..V.oG9Ê.V.y.
0090   29 5b 0d 56 00 5f d8 0b bb 0d 56 00 92 73 ac fa   )[.V._Ø.».V..s¬ú
00a0   0d 56 00 23 b5 4c db 0d 56 00 54 c1 69 fd 0d 56   .V.#µLÛ.V.TÁiý.V
00b0   00 bf f3 c7 2a 0d 56 00 1b df 55 43 0d 56 00 12   .¿óÇ*.V..ßUC.V..
00c0   da 13 b6 0d 56 00 0e 76 81 7b 0d 56 00 57 64 a2   Ú.¶.V..v.{.V.Wd¢
00d0   6e 0d 56 00 6c c4 c8 e9 0d 56 00 58 63 fa 18 0d   n.V.lÄÈé.V.Xcú..
00e0   56 00 50 66 25 1c 0d 56 00 51 1a 98 e2 0d 56 00   V.Pf%..V.Q..â.V.
00f0   4e 2d 41 1a 0d 56 00 46 70 c8 5a 0d 56 00 53 82   N-A..V.FpÈZ.V.S.
0100   e0 fd 0d 56 00 93 87 0b 90 35 2e 00 17 61 f6 da   àý.V.....5...aöÚ
0110   0d 56 00 58 82 9d 06 0d 56 00 4b 28 cf 6e 0d 56   .V.X....V.K(Ïn.V
0120   00 88 18 4e 19 0d 56 00 23 b6 88 f1 0d 56 00 55   ...N..V.#¶.ñ.V.U
0130   0a ca a7 0d 56 00 9f 41 5a f2 0d 56 00 2a 74 76   .Ê§.V..AZò.V.*tv
0140   d3 0d 56 00 53 87 cc 93 0d 56 00 18 3e f7 97 0d   Ó.V.S.Ì..V..>÷..
0150   56 00 dd 01 45 6e 0d 56 00 44 b7 a7 b7 0d 56 00   V.Ý.En.V.D·§·.V.
0160   3f 22 23 7c 0d 56 00 8e 5d 8d bd 0d 56 00 59 a9   ?"#|.V..].½.V.Y©
0170   38 94 0d 56 00 23 b5 23 5d 0d 56 00 23 c4 73 fd   8..V.#µ#].V.#Äsý
0180   0d 56 00 54 c0 d3 6f 0d 56 00 7d 7d 2a 94 0d 56   .V.TÀÓo.V.}}*..V
0190   00 2f 6f 5e 57 0d 56 00 2f 61 c6 15 0d 56 00 b9   ./o^W.V./aÆ..V.¹
01a0   24 bf f4 0d 56 00 23 f3 c4 e0 0d 56 00 58 62 c6   $¿ô.V.#óÄà.V.XbÆ
01b0   c4 0d 56 00 74 3e d6 80 0d 56 00 3f 56 bc 1c 0d   Ä.V.t>Ö..V.?V¼..
01c0   56 00 77 f6 02 b2 0d 56 00 dd 7c c3 f3 0d 56 00   V.wö.².V.Ý|Ãó.V.
01d0   9b 8a de f2 0d 56 00 4f 89 29 0e 0d 56 00 5e 82   ..Þò.V.O.)..V.^.
01e0   cf ec 0d 56 00 b2 3f 10 34 0d 74 00 36 f5 1d fc   Ïì.V.²?.4.t.6õ.ü
01f0   0d 56 00 49 03 4c 21 0d 56 00 2f 4b 96 ac 0d 56   .V.I.L!.V./K.¬.V
0200   00 ac 3a dc 23 0d 56 00 c0 00 86 cb 0d 56 00 be   .¬:Ü#.V.À..Ë.V.¾
0210   71 66 23 0d 56 00 b4 a8 26 be 0d 56 00 c3 c9 11   qf#.V.´¨&¾.V.ÃÉ.
0220   77 0d 56 00 05 c4 45 6b 0d 56 00 b2 43 2f 1c 0d   w.V..ÄEk.V.²C/..
0230   56 00 32 e0 28 e1 0d 56 00 88 3d 03 59 0d 56 00   V.2à(á.V..=.Y.V.
0240   0d e6 5e 30 0d 56 00 18 55 ad 32 0d 56 00 3b 95   .æ^0.V..U.2.V.;.
0250   88 d2 0d 56 00 6f 25 e4 0b 0d 56 00 04 1c 86 ca   .Ò.V.o%ä..V....Ê
0260   0d 56 00 23 9d f7 d1 5b 76 00 23 be 80 ee 0d 56   .V.#.÷Ñ[v.#¾.î.V
0270   00 57 6e a1 ea 0d 56 00 2f 65 b8 52 0d 56 00 56   .Wn¡ê.V./e¸R.V.V
0280   7b 39 56 0d 56 00 27 9b c0 e7 0d 56 00 5b b9 27   {9V.V.'.Àç.V.[¹'
0290   24 34 66 00 48 c6 c7 9e 0d 56 00 86 d1 fb d6 0d   $4f.HÆÇ..V..ÑûÖ.
02a0   56 00 58 42 dc f9 0d 56 00 56 bb ae 11 0d 56 00   V.XBÜù.V.V»®..V.
02b0   a7 56 5e d3 0d 56 00 5e ef 09 a9 0d 56 00 34 c0   §V^Ó.V.^ï.©.V.4À
02c0   7e b4 0d 56 00 34 39 0e e1 0d 56 00 d0 5d e7 f0   ~´.V.49.á.V.Ð]çð
02d0   0d 56 00 b2 21 88 a5 0d 56 00 4d 67 c2 81 0d 56   .V.²!.¥.V.MgÂ..V
02e0   00 a3 ac d2 88 0d 56 00 c3 c9 a8 11 0d 56 00 22   .£¬Ò..V.ÃÉ¨..V."
02f0   49 fc 96 0d 56 00 55 19 82 2f 0d 56 00 64 08 b5   Iü..V.U../.V.d.µ
0300   24 0d 56 00 93 4b 5e 73 0d 56 00 6d be 81 95 0d   $.V..K^s.V.m¾...
0310   56 00 74 3e c8 89 0d 56 00 34 1f d2 bf 0d 56 00   V.t>È..V.4.Ò¿.V.
0320   a7 63 03 17 0d 56 00 50 8a fb 3b 0d 56 00 22 4a   §c...V.P.û;.V."J
0330   91 91 0d 56 00 40 4f 6e c2 0d 56 00 46 31 90 9b   ...V.@OnÂ.V.F1..
0340   0d 56 00 b9 f5 55 4e 0d 56 00 12 b3 2a 68 0d 56   .V.¹õUN.V..³*h.V
0350   00 2f 63 5a db 0d 56 00 0d 71 52 c1 0d 56 00 48   ./cZÛ.V..qRÁ.V.H
0360   02 ef 4b 0d 56 00 5f d3 30 b2 0d 56 00 b9 42 5f   .ïK.V._Ó0².V.¹B_
0370   0c 0d 56 00 3b 6e 0c b8 0d 56 00 42 2a 2a c3 0d   ..V.;n.¸.V.B**Ã.
0380   56 00 5b f1 8c e6 0d 56 00 68 c8 99 57 0d 56 00   V.[ñ.æ.V.hÈ.W.V.
0390   68 e5 0c 05 0d 56 00 78 4d 96 35 0d 56 00 2e 15   hå...V.xM.5.V...
03a0   93 7b 11 3e 00 b7 24 50 0b 0d 56 00 2f 34 ef ad   .{.>.·$P..V./4ï.
03b0   0d 56 00 23 d3 88 a0 0d 56 00 64 12 30 21 0d 56   .V.#Ó. .V.d.0!.V
03c0   00 72 56 6f 2c 0d 56 00 4a 43 9e 78 0d 56 00 c7   .rVo,.V.JC.x.V.Ç
03d0   f7 02 d8 0d 56 00 b4 a4 3d 62 0d 56 00 55 7e c5   ÷.Ø.V.´¤=b.V.U~Å
03e0   ec 0d 56 00 27 6a c3 1f 0d 56 00 72 27 06 95 0d   ì.V.'jÃ..V.r'...
03f0   56 00 ad 4f 77 f4 0d 56 00 51 ab 3a a7 0d 56 00   V..Owô.V.Q«:§.V.
0400   0d 71 9d ad 0d 56 00 6b b5 b1 07 0d 56 00 23 f1   .q...V.kµ±..V.#ñ
0410   8c 65 0d 56 00 6d ae 39 3b 0d 56 00 d9 85 6e ce   .e.V.m®9;.V.Ù.nÎ
0420   0d 56 00 22 49 eb 2c 0d 56 00 46 32 21 7e 0d 56   .V."Ië,.V.F2!~.V
0430   00 23 ee 4e 47 34 66 00 2f 6f bf 14 34 66 00 6a   .#îNG4f./o¿.4f.j
0440   26 62 ae 0d 56 00 27 68 31 01 34 66 00 44 b7 a3   &b®.V.'h1.4f.D·£
0450   d0 34 66 00 92 94 09 1f 0d 56 00 2f 4b 46 fc 0d   Ð4f......V./KFü.
0460   56 00 12 e0 3b 30 0d 56 00 cf f6 51 3f 0d 56 00   V..à;0.V.ÏöQ?.V.
0470   23 ea 6e 44 0d 56 00 d9 c5 79 fa 0d 56 00 de 5e   #ênD.V.ÙÅyú.V.Þ^
0480   49 7c 0d 56 00 5a 9a d7 fc 0d 56 00 8b 63 c0 b0   I|.V.Z.×ü.V..cÀ°
0490   0d 56 00 34 4e 5a 9a 0d 56 00 ad ea 9e 73 0d 56   .V.4NZ..V..ê.s.V
04a0   00 cf 9a f5 ee 0d 56 00 23 f3 8a ad 0d 56 00 1f   .Ï.õî.V.#ó...V..
04b0   03 99 8b 0d 56 00 8b 63 44 42 35 2e 00 47 be b7   ....V..cDB5..G¾·
04c0   9e 0d 56 00 b2 b0 af 2c 0d 56 00 2e 65 65 79 0d   ..V.²°¯,.V..eey.
04d0   56 00 5b 40 d6 5b 0d 56 00 51 ab 3a 7c 0d 56 00   V.[@Ö[.V.Q«:|.V.
04e0   b2 3e e8 31 0d 56 00 79 ce 99 34 0d 56 00 2f 4a   ²>è1.V.yÎ.4.V./J
04f0   f3 d2 0d 56 00 dd a3 89 54 0d 56 00 d4 5c 7c b5   óÒ.V.Ý£.T.V.Ô\|µ
0500   0d 56 00 5e 82 b7 0d 0d 56 00 2e 05 9d 75 0d 56   .V.^.·..V....u.V
0510   00 46 34 8f 10 0d 56 00 2f 6e 55 17 0d 56 00 2d   .F4...V./nU..V.-
0520   12 97 4d 0d 56 00 8b b4 87 a2 0d 56 00 2e 65 5d   ..M.V..´.¢.V..e]
0530   b1 0d 56 00 6c d0 49 9b 0d 56 00 2f 4a 3a 23 0d   ±.V.lÐI..V./J:#.
0540   56 00 45 a6 68 26 0d 56 00 23 9c da b4 0d 56 00   V.E¦h&.V.#.Ú´.V.
0550   43 fd 6b aa 0d 56 00 47 e5 b0 10 0d 56 00 d0 60   Cýkª.V.Gå°..V.Ð`
0560   67 a6 0d 56 00 23 c5 43 73 0d 56 00 ae 18 a3 c6   g¦.V.#ÅCs.V.®.£Æ
0570   0d 56 00 6d 7c 12 78 0d                           .V.m|.x.

my own code received:
613D06000000000000006D0000000F00276C3F380D560055BBFD3BC0A4002768176A3466002F641B410D560034390EE10D5600B70F7B130D56004E24CB7F0D5600792BB7B40D56007818C81B0D56002F5ABCBF34660023C584610D560045C398720D5600334B37610D560068C7CD150D560044B7217D0D56
    
     */
    int nPeersReceiving;

    void onReceivedPeerAddrs(ByteBuffer bb) {
        if (nPeersReceiving == 0) {
            nPeersReceiving = bb.getInt();
        }
        if (nPeersReceiving < 0) {
            //we should terminate the connection and the peer.
            throw new IllegalStateException();
        }
        ArrayList<InetSocketAddress> isas = new ArrayList<>();
        for (; nPeersReceiving > 0; nPeersReceiving--) {
            int pos = bb.position();
            try {
                isas.add(readInetSocketAddress(bb));
            } catch (Throwable t) {
//                t.printStackTrace();
                if (t instanceof java.nio.BufferUnderflowException) {
                    bb.position(pos);
                    break;
                }
            }
        }
        self.onReceivedPeerAddrs(isas);
    }

    void onNetworkFailed(Peer peer, Throwable t) {
        CompletableFuture.runAsync(() -> {
            try {
                String msg = t.getMessage();
                if (msg != null) {
                    if (msg.contains("timed out")
                            || msg.contains("No route to host")
                            || msg.contains("Connection reset by peer")
                            || msg.contains("Connection refused")) {
                        peers.remove(peer.isa);  //for temp do not remove them.
                        System.out.println("peers remaining:" + peers.size());
                    }
                }
                pickOneToConnect(); //should not be peer
            } catch (Throwable t2) {
                t2.printStackTrace(System.out);
            }
        });
    }

    class Dandelion {
        //https://github.com/mimblewimble/grin/blob/master/doc/dandelion/dandelion.md
        //as a mini wallet, we just do not participate in this.
    }

    static class MsgHand {

        /**
         * directly read into Peer or use this class?
         *
         */
        int version;
        int capabilites;
        long nonce;
        long totalDifficulty;
        InetSocketAddress addressSender, addressReceiver;
        byte[] seven0 = new byte[7];
        String userAgent;
        private byte[] hash_genesis;

        //for temp to use this class
        void read(ByteBuffer bb) {
            version = bb.getInt();
            this.capabilites = bb.getInt();
            //this.capabilites &= 0xff;
            nonce = bb.getLong();
            this.totalDifficulty = bb.getLong();
            this.addressSender = readInetSocketAddress(bb);
            this.addressReceiver = readInetSocketAddress(bb);
            bb.get(seven0);
            //the following is what I sent out.
//613D0100000000000000590000000100000000A2B35B2B7DA84CB1000000000000000000C0A80009E66600C0A800090D56000000000000000B646F206E6F742074656C6C40ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82
//613D
//    01
//      0000000000000059
//00000001
//        00000000
//                A2B35B2B7DA84CB1  nonce
//0000000000000000                  difficulty is 0?
//                00C0A80009E666    address
//                              00  address
//C0A800090D56
//            00000000000000
//                          0B646F  user agent
//206E6F742074656C6C
//                  40ADAD0AEC2779  main genesis
//7B48840AA9E00472015C21BAEA118CE7
//A2FF1A82C0F8F5BF82

            userAgent = readString(bb);
            hash_genesis = new byte[32];
            bb.get(hash_genesis);
        }

        StringBuilder dump(StringBuilder sb) {
            sb.append("version:" + version + " " + Integer.toHexString(version) + "\n");
            sb.append("capabilites:" + capabilites + " " + Long.toHexString(capabilites) + "\n");
            sb.append("nonce:" + nonce + " " + Long.toHexString(nonce) + "\n");
            sb.append("totalDifficulty:" + totalDifficulty + " " + Long.toHexString(this.totalDifficulty) + "\n");
            sb.append("address   sender:" + addressSender);
            sb.append("address receiver:" + addressReceiver);
            sb.append("seven0:" + seven0 + " " + Hex.toHexString(seven0) + "\n");
            sb.append("user agent:" + userAgent + "\n");
            sb.append("hash_genesis:" + hash_genesis + " " + Hex.toHexString(hash_genesis) + "\n");
            return sb;
        }
    }
    Genesis genesis;
}
