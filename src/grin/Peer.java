package grin;

import grin.Networking_Core.TCPclient;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import zede.util.Hex;

/**
 * protocol: https://github.com/mimblewimble/docs/wiki/P2P-Protocol
 *
 *
 * https://github.com/mimblewimble/grin/blob/master/p2p/src/handshake.rs
 *
 *
 * we need initial connect points to bootstrap.
 *
 *
 * port 3415: mining server connect to wallet port 3416: mining server listens
 *
 * 3413: wallet connect to node.
 *
 * 3414: other peers to connect to us.
 *
 *
 *
 * @author jack
 */
public class Peer implements ListenerSocketData, TCPclient {
//defined in /grin/p2p/src/msg.rs
    private static final int Error = 0;//	Sent when an issue is found during communication with a peer. Usually followed by closing the connection.
    private static final int Hand = 1;//	First part of a handshake, sender advertises its version and characteristics.
    private static final int Shake = 2;//	Second part of a handshake, receiver of the first part replies with its own version and characteristics.
    private static final int Ping = 3;//	Sent to confirm that the connection is still valid, and used to advertise the node's total_difficulty to confirm whether sync is needed.
    private static final int Pong = 4;//	The response to a ping message.
    static final int GetPeerAddrs = 5;//	Used to request addresses of new peers to connect to.

    private static final int PeerAddrs = 6;//	Peer addresses sent in response to a GetPeerAddrs message.
    private static final int GetHeaders = 7;//	Used to request block headers from a peer.
//613d08000000000000019a000200000000000442cf000000005d3eb0c70000024c587b394461b56bc118d5ea996b07d8b65cdb0cd647379680eaa37d2f5638dbe00e06052709ca19fc45552227a255898ff1321412ccb6f45cbf560ff99d7b3cb5fe20d3ae849cfee8f87e7da6ae99ba7d95cc73015b042b4cad4f779c6fa704d57400e8e3883fb53ebd552433870e2947b268399f0ebd6de674a9f4b3d49f8199fca091435b4e89bf3a59b3bdb36960d2b2e5da51be66ef430009daee43bee55c138f12c95e3ae08b86948c325225807d28ae5c3ab83d252342163ed100000000003130ac000000000019c7bd0002e50f74f6be790000029a00a419aea08094601f8524140160ea0682745030019e30ac8028a78e2077597cf43f1159a696d22e75778e98c03fec0cd501a28838d03e258f1dfc4a1ca0916946f1d6544e246ca5ac2bb7ae90341cc894304e9f03a02726ba31ec5b0a2f26a118193d3ffa9501621b4cc7fbed661a5909368c0322abe0aa9f45258de50eeae17c63a24ec0012279e18e9f3571e2d7ecb9cd1e18dd2a3bacfe54a3774fc758cc41cc2de9b874367abb89b93f    
    private static final int Header = 8;//	A single block header received from a peer.
    private static final int Headers = 9;//	Multiple block headers received from a peer in response to a GetHeaders message.
    static final int GetBlock = 10;//	Used to request a block from a peer.
    private static final int Block = 11;//	A single block received from a peer.
    /**
     * what is the difference between Block and Compact Block? see CompactBlock
     * file. CompactBlock without inputs but with extra nonce and shortIDs.
     *
     */
    private static final int GetCompactBlock = 12;//	Used to request a compact block from a peer.
    private static final int CompactBlock = 13;//	A single compact block received from a peer.
    /**
     * what is the difference between Stem tx and tx? see
     * https://github.com/mimblewimble/grin/blob/master/doc/dandelion/dandelion.md
     *
     */
    private static final int StemTransaction = 14;//	A stem transaction received from a peer.
    private static final int Transaction = 15;//	A transaction received from a peer.
    private static final int TxHashSetRequest = 16;// 	Used to request the transaction hashset from a peer.
    private static final int TxHashSetArchive = 17;// 	The transaction hashset in response to the TxHashSetRequest message.
    private static final int BanReason = 18;//	Contains the reason your node was banned by a peer.    
private static final int 		GetTransaction = 19;
private static final int 		TransactionKernel = 20;
private static final int 		KernelDataRequest = 21;
private static final int 		KernelDataResponse = 22;
    

    //capabilites mask. is short(2 bytes) enough?
    private static final short CM_Unknown = 0;
    private static final short CM_FullHistory = 1;
    private static final short CM_TxHashSetHistory = 2;
    private static final short CM_PeerList = 4;
    private static final short CM_FastSyncNode = 2 + 4;
    private static final short CM_FullNode = 1 + 2 + 4;

    long totalDifficulty, height; //of this peer
    String userAgent = "MW/Grin 2.0.1-beta.1";// "do not tell";
    byte[] hash_genesis;
    InetSocketAddress isa;
    int version; //it is name "protocol version" not for each msg.
    /**
     * TODO: is it 4 bytes or 1 byte? the protocol page says 1 byte, but from
     * the msg data, seems to be 4 bytes.
     *
     *
     * what does 0x0f mean? I do not know, i just fake it. I should check code
     * handshake.rs.
     */
    int capabilites;
    PeerSelf self;

    Peer() {
    }

    static ByteBuffer write(ByteBuffer bb, InetSocketAddress isa) {
        byte[] host = isa.getAddress().getAddress();
        byte family = (byte) (host.length == 4 ? 0 : 1);
        bb.put(family);
        bb.put(host);
        bb.putShort((short) isa.getPort());
        return bb;
    }

    static InetSocketAddress readInetSocketAddress(ByteBuffer bb) {
        int family = bb.get();
        int len;
        if (family == 0) {
            len = 4;
        } else if (family == 1) {
            len = 8;
        } else {
            throw new IllegalStateException("unnown family:" + family + " " + Integer.toHexString(family) + " pos:" + bb.position());
        }
        byte[] host = new byte[len];
        bb.get(host);
        int port = bb.getShort();
        port &= 0xffff;
        InetAddress ia;
        try {
            ia = InetAddress.getByAddress(host);
        } catch (UnknownHostException ex) {
            //this should never happen
            ex.printStackTrace();
            throw new IllegalStateException();
        }
        InetSocketAddress isa = new InetSocketAddress(ia, port);
        return isa;
    }

    static ByteBuffer writeString(ByteBuffer bb, String s) {
        byte[] bytes = s.getBytes(cutf8);
        if (bytes.length > Integer.MAX_VALUE) {
            throw new IllegalStateException(); //too long to process.
        }
        int len = bytes.length;
        if (len > 255) {
            len = 255;
        }
        bb.put((byte) bytes.length);
        bb.put(bytes, 0, len);
        return bb;
    }

    static String readString(ByteBuffer bb) {
        int len;
        if (false) { //according to the doc page
            long slen = bb.getLong();
            if (slen > Integer.MAX_VALUE) {
                byte[] bytes = new byte[bb.limit()];
                int pos = bb.position();
                bb.position(0);
                bb.get(bytes);
                bb.position(pos);
                throw new IllegalStateException(":" + slen + "pos:" + pos + "\n" + Hex.toHexString(bytes)); //too long to process.
            }
            len = (int) slen;
        } else { //according to the msg data
            len = bb.get();
            len &= 0xff;
        }
        byte[] bytes = new byte[len];
        bb.get(bytes);
        return new String(bytes, cutf8);
    }
//    class MsgError{
//    }

    void writePingPong(ByteBuffer bb) {
        bb.putLong(self.totalDifficulty);
        bb.putLong(self.height);
    }

    void readPingPong(ByteBuffer bb) {
        this.totalDifficulty = bb.getLong();
        this.height = bb.getLong();
    }

    /* total 27 bytes
613D030000000000000010
                      0002E4F42A
7F508B00000000000442AC
    
     */
    void onReceivedPing(ByteBuffer bb) {
        readPingPong(bb);
        CompletableFuture.runAsync(() -> {
            ByteBuffer bbw = ByteBuffer.allocateDirect(1024);
            //TODO: message header is not written yet.
            writePingPong(bbw);
            //int len=bbw.position();  useless.
            bbw.flip(); //now ready to be sent
            //TODO: send the message
        });
    }

    void onReceivedPong(ByteBuffer bb) {
        readPingPong(bb);
        //TODO: schedule for the next one?
    }
    private long nonce_hand2send;
    static Random random = new Random();

    void newNonce_hand2send() {
        nonce_hand2send = random.nextLong();
    }

    /*
0000   50 bd 5f 81 69 aa 00 e0 4c 37 b2 a2 08 00 45 00   P½_.iª.àL7²¢..E.
0010   00 a1 ab a0 40 00 40 06 44 a5 c0 a8 00 09 2f 60   .¡« @.@.D¥À¨../`
0020   5a 00 c3 74 0d 56 31 7e 87 3e 8d b2 74 4d 80 18   Z.Ãt.V1~.>.²tM..
0030   00 e5 67 b0 00 00 01 01 08 0a a2 e0 91 a2 ab 5b   .åg°......¢à.¢«[
0040   46 1b 
             61 3d 01 00 00 00 00 00 00 00 62 00 00 00   F.a=........b...
0050   01 00 00 00 0f df e9 38 07 40 14 ff 33 00 02 e4   .....ßé8.@.ÿ3..ä
0060   59 ca f5 b6 12 00 00 00 00 00 0d 56 00 2f 60 5a   YÊõ¶.......V./`Z
0070   00 0d 56 00 00 00 00 00 00 00 14 4d 57 2f 47 72   ..V........MW/Gr
0080   69 6e 20 32 2e 30 2e 31 2d 62 65 74 61 2e 31 40   in 2.0.1-beta.1@
0090   ad ad 0a ec 27 79 7b 48 84 0a a9 e0 04 72 01 5c   ...ì'y{H..©à.r.\
00a0   21 ba ea 11 8c e7 a2 ff 1a 82 c0 f8 f5 bf 82      !ºê..ç¢ÿ..Àøõ¿.

magic: 613d
 tyep: 1    
    length: 0x62
    version: 1
    capabilities: 0x0000_000f
    nonce: 8 bytes     df e9 38 07 40 14 ff 33
    dificulty: 8 bytes 00 02 e4 59 ca f5 b6 12
    family: 00
    ip: 0
    port: 0x0d56=3414
    family: 00
    ip: 2f 60 5a 00 is ip 47.96.90.0
    port:     0x0d56 =3414 
    7 0x00: why?
    0x14: length of user agent
    user agent: MW/Grin 2.0.1-beta.1
    genesis_hash: 0x40ad...
    
    
    response:    
0000   00 e0 4c 37 b2 a2 50 bd 5f 81 69 aa 08 00 45 00   .àL7²¢P½_.iª..E.
0010   00 84 38 99 40 00 72 06 85 c9 2f 60 5a 00 c0 a8   ..8.@.r..É/`Z.À¨
0020   00 09 0d 56 c3 74 8d b2 74 4d 31 7e 87 ab 80 18   ...VÃt.²tM1~.«..
0030   02 02 e5 03 00 00 01 01 08 0a ab 5b 46 1e a2 e0   ..å.......«[F.¢à
0040   91 a2 
             61 3d 02 00 00 00 00 00 00 00 45 00 00 00   .¢a=........E...
0050   01 00 00 00 0f 00 02 e4 5c 49 2a e3 7c 00 00 00   .......ä\I*ã|...
0060   00 00 00 00 0d 4d 57 2f 47 72 69 6e 20 32 2e 30   .....MW/Grin 2.0
0070   2e 30 40 ad ad 0a ec 27 79 7b 48 84 0a a9 e0 04   .0@...ì'y{H..©à.
0080   72 01 5c 21 ba ea 11 8c e7 a2 ff 1a 82 c0 f8 f5   r.\!ºê..ç¢ÿ..Àøõ
0090   bf 82 
    
    magic: 0x613d
    type: 02
    length: 0x45
    version: 1
    capabilities: 0x0f
    difficulty:   00 02 e4 5c 49 2a e3 7c
    7 0x00: why？
    length of user agent: 0x0d
    user agent: MW/Grin 2.0.0
    genesis_hash: 0x40adad....

shake received from my local node.    
613D02000000000000004C
                      00000001      version
                              00    capabilities
00000F
      0002E4841EFCD7C9              difficulty
                      0000000000    7 0x00
0000
    144D572F4772696E20322E302E31    user agent
2D626574612E31
              40ADAD0AEC27797B48    genesis hash
840AA9E00472015C21BAEA118CE7A2FF
1A82C0F8F5BF82
    
    
     */
    /**
     * nonce_hand should be prepared before calling this.
     *
     * prepare msg Hand(1)
     *
     * @param bb
     * @param self
     */
    /*
     * 
     * the hand sent out by my local node when the height is at about 326607, tip hash is 000000a37fce..., cumulative
     * difficulty is 98802716...
     * 
     * 50bd5f8169aa00e04c37b2a20800450000a13de240004006d51fc0a80009276c3f38b0620d56c9a9dae621efb4d9801801f619d500000101080a7b676863a4a87096613d010000000000000062000000010000000f33ce3e739e2c1e22000382937ed69b4b00000000000d5600276c3f380d5600000000000000144d572f4772696e20322e302e312d626574612e3140adad0aec27797b48840aa9e00472015c21baea118ce7a2ff1a82c0f8f5bf82
     *  358 - 109*2=358-218=140
     * 
613d    magic
    01  HAND
      0000000000000062  8 bytes length
                      00000001  version
                              0000000f  capabilities
33ce3e739e2c1e22                        nonce
                000382937ed69b4b        cumulative difficulty
                                00000000000d56  family ip port sender  while local port in fact is 45145.
                                00276c3f380d56  receiver    0x0d56 =3414 
00000000000000          7 bytes 0
144d572f4772696e20322e302e312d626574612e31  length(1) and user agent "MW/Grin 2.0.1-beta.1"
40adad0aec27797b48840aa9e00472015c21baea118ce7a2ff1a82c0f8f5bf82    genesis hash
     * 
     */
    void writeHand(ByteBuffer bb) throws IOException {
        bb.putInt(1); //version
        bb.putInt(self.capabilites);
        bb.putLong(nonce_hand2send);
        bb.putLong(self.totalDifficulty);
        if (false) {
            bb.put(new byte[5]);
            bb.putShort((short) 0x0d56);
//        } else if(true){
        } else {
            write(bb, (InetSocketAddress) sc.getLocalAddress()); //the sender
        }
        write(bb, isa); //the receiver
        byte[] seven0 = new byte[7];
        bb.put(seven0);
        writeString(bb, self.userAgent);
        bb.put(self.genesis.genesis_hash); //it is 0x40adad...
        //bb.put(self.genesis.genesis_full_hash);
        System.out.println("what is prepared\n" + Hex.toHexString(bb.array(), 0, bb.position()));
    }
//    long nonce_shake; //according to msg data, there is no such thing.

    /*
shake received from my local node.    
613D02000000000000004C
                      00000001      version
                              00    capabilities
00000F
      0002E4841EFCD7C9              difficulty
                      0000000000    7 0x00
0000
    144D572F4772696E20322E302E31    user agent
2D626574612E31
              40ADAD0AEC27797B48    genesis hash
840AA9E00472015C21BAEA118CE7A2FF
1A82C0F8F5BF82
    
     */
    /**
     * nonce_shake should be prepared before calling this.
     *
     * @param bb
     * @param self
     */
    void writeShake(ByteBuffer bb) {
        bb.putInt(1);
        bb.putInt((byte) self.capabilites);
        //bb.putLong(nonce_shake);
        bb.putLong(self.totalDifficulty);
        byte[] seven0 = new byte[0];
        bb.put(seven0);
        writeString(bb, self.userAgent);
        bb.put(self.hash_genesis);
    }

    /*
I did receive this response 87 bytes=0x0B+0x4C:
613D02000000000000004C000000010000000F000382FCF5BE079D00000000000000144D572F4772696E20322E302E312D626574612E3140ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82

    
000000010000000F
000382FCF5BE079D
00000000000000
144D572F4772696E20322E302E312D626574612E3140ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82    
 * 
 * 
another instance
613D02000000000000004C000000010000000F000383EF84E74BC000000000000000144D572F4772696E20322E302E312D626574612E3140ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82    
    
     */
    void onReceivedShake(ByteBuffer bb) {
        state |= StateNetworkShakeReceived;
        System.out.println("on receiving shake");
        version = bb.getInt();
        if (version != 1) {
            throw new IllegalStateException(); //do not know how to parse it.
        }
        this.capabilites = bb.getInt();
        //this.capabilites &= 0xff;
        //TODO: what does it mean exactly？ according to the msg data, no such thing.
        //long nonce = bb.getLong();
        this.totalDifficulty = bb.getLong();
        byte[] seven0 = new byte[7];
        bb.get(seven0);
//            this.addressSender=parseSocketAddress(bb);
//            this.addressReceiver=parseSocketAddress(bb);
        userAgent = readString(bb);
        this.hash_genesis = new byte[32];
        bb.get(this.hash_genesis);
//613D02000000000000004C000000010000000F0002E497811C3BD900000000000000144D572F4772696E20322E302E312D626574612E3140ADAD0AEC
//27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82613D1400000000000000208BEFC63442859C4E24E75ECB6BFD8BF5361BA981788FAC74D255A963C850A94A613D140000000000000020427E884168C39E9497CF81D75B109E6CB8633905C6D9C94724738BC16164E284        
        self.onReceivedShake(this);
    }

    void onReceivedGetPeerAddrs(int mask) {
        CompletableFuture.runAsync(() -> {
            try {
                //TODO: send back a list of Peer Addresses.
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }).exceptionally(t -> {
            //TODO: UI element or whatever.
            t.printStackTrace();
            return (Void) null;
        });
    }

    /*
0000   61 3d 05 00 00 00 00 00 00 00 04 00 00 00 0f      a=.............
    
     */
    void writeGetPeerAddrs(ByteBuffer bb, int mask) {
        bb.putInt(mask);
    }

    void writePeerAddrs(ByteBuffer bb, ArrayList<Peer> peers) {
        int size = peers.size();
        bb.putInt(size);
        for (Peer peer : peers) {
            write(bb, peer.isa);
        }
    }

    Peer set(InetSocketAddress isa, PeerSelf self) {
        if (this.isa != null) {
            throw new IllegalStateException();
        }
        this.isa = isa;
        this.self = self;

        return this;
    }

    void writeGetHeaders(ByteBuffer bb, ArrayList<byte[]> al_hash_header) {
        int size = al_hash_header.size();
        if (size > Byte.MAX_VALUE) {
            throw new IllegalStateException("too many:" + size);
        }
        bb.put((byte) size);
        for (byte[] hash : al_hash_header) {
            if (hash.length != 32) {
                throw new IllegalStateException("not 32 bytes, but " + hash.length);
            }
            bb.put(hash);
        }
    }

    void onReceivedGetHeaders(ByteBuffer bb) {
        int size = bb.get();
        size &= 0xff;
        //TODO: we are not serving the peer.
        byte[] hash = new byte[32];
        for (int i = 0; i < size; i++) {
            bb.get(hash);
        }
    }

    //in Block.BlockHeader
//    ByteBuffer writeHeader(ByteBuffer bb, Block.BlockHeader blk){
//        //TODO: ...
//        return null;
//    }
    void onReceivedHeader(ByteBuffer bb) {
        Block.BlockHeader bh = new Block.BlockHeader();
        bh.parse(bb);
        //TODO: deal with bh
    }

    void onReceivedHeaders(ByteBuffer bb) {
        int size = bb.getShort();
        size &= 0xffff;
        for (int i = 0; i < size; i++) {
            onReceivedHeader(bb);
        }
    }

    ByteBuffer writeGetBlock(ByteBuffer bb, byte[] hash) {
        if (hash.length != 32) {
            throw new IllegalStateException(":" + hash.length);
        }
        bb.put(hash);
        return bb;
    }

    void onReceivedGetBlock(ByteBuffer bb) {
        byte[] hash = new byte[32];
        bb.get(hash);
        CompletableFuture.runAsync(() -> {
            Block blk = null;
            //TODO: serve the peer with the block data.
            ByteBuffer bbw = null;
            writeBlock(bbw, blk);
            //TODO: send data
        }).exceptionally(t -> {
            //TODO: UI element or whatever.
            t.printStackTrace();
            return (Void) null;
        });
    }

    //TODO: remove this method.
    ByteBuffer writeBlock(ByteBuffer bb, Block blk) {
        blk.write(bb);
        return bb;
    }

    /*
    my code received:
613D0B00000000000004E700010000000000000000000000005C3E03D600000000000000000000000000000000000000000000000000000000000000000000000000000000002A8BC32F43277FE9C063B9C99EA252B483941DCD06E217FA7566D275006C6C467876758F2BC87E4CEBD2020AE9CF9F294C6217828D68721B7FFF259AEE3EDFB5867C4775E4E1717826B843CDA6685E5140442ECE7BFC2EE8BB096A73CBE6E099968965F5342FC1702EE2802802902286DCF0F279E326BF00000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000100000004000000000000074000000000000000291D1B0243A0E60E4600721B09FCFF27E171E339DE0AB14852AA1641EB7F342ABF6528088B15D93B9923465E789431429698932A53B14D6DDADCA9551C2C938BB8B0B121090F37BAF582476C7303372F20617DE54B04F8688A2CAC85F19957463AE9C0CCC1988BCA7FF9534FC990EC410FB0EDE29EB9DDF10638BA000EFBE123621F8DDEDC7480A597FD297E14C5C6E73668F95F524A3F415FF3030000000000000000000000000000000100000000000000010108B7E57C448DB5EF25AA119DDE2312C64D7FF1B890C416C6DDA5EC73CBFED2EDEA00000000000002A39330AD8CDE205F317C6537ECA96B866293A0489615A9A277B4D3A597C873544C82474932B641E06AC8719604EE52E895E8CD4621B6BFB85780CD9BECCE14D0700B83A664DB2F52A26C425FD777AD88944CDFFF38043A2793ED4D9AA67E36CBFD5585579FC69DDA930418AF5EAF603654F6F751258D2DFC8C2113C171E130F31EC1E6CCE2A718E435298FCE5D64FFE1BD3464FD7C87CFA92093855BE034BFE4439E928BD92AD77FD0A0E00355EE1D1A9CEB1ED0C408DCFDBA8C583E7598DC700AAA9F91432097259A405F5B7315A2F7658861E3349BB0DC8BF883726A215F0149DED6613E5AC0670C0C5202247D7C27C8A7D03BDB03C9CF5455463F9B42CF87403E31F8383CC4F49A34C62AE459F5801A9EED4F0EE3DFD5F55B7011C0CAE393C474ABD6F8C7965B9B5FFF3104DD4E39542077C0C8DD2F8FFCEB6BB598512D90506D0A7184F20F1498CF458787F23284B54888C9BE416D103F760406357A16B6D841A303D5C95B6B474D2D7F0FEA0A2A76C897DD2110E9303F54684169421147684C6F1819C33CEF3F38EC995A508450C02CD1872F8065FDEE723109C18B1DD2DDDE75825546ECF0DF0793C353B20C946CD64122CEA8C116F432336899A16AD24A2AAFCB8F900E09A1147135FCF2A54CBF81DB308A47A08A49C77C130E5DC5E661CD55A5CC69E607055A5B08111BF61A62EA5778F85119043633F1CAB8C756D756C5A34851024AC311A596B1CD919BBCA43226F0BA057F6B57DE2F6955B0823C3826DE7F6096C1C1B6B9B8E4063E1645C0BFF32F80561AAA959D97120FBC2ECD9D2BE28BD0C17811DC59A88049F6D8952EE9A0A0207693C89CA3AD1197E9BFDFC03BE9D845AEA8D663969217E3B494CEE9E652BC9F8713E2FD5CB1843848F46C3A6AB024D0E3D57CA45454CDBDA414ADAA835FA147DEB4FFB7129CF3A8D86726A01447940100000000000000000000000000000000096385D86C5CFDA718AA0B7295BE0ADF7E5AC051EDFE130593A2A257F09F78A3B150D029AB1CE0FA793CC0D5E86FC76F69121636A56B21BA71BA640C2A486A2A1443FDBCB2E4F615A8FD1216B3293FFADA50844B43F40B6C1BBCFBD4A6E96775ED
    2026/2=1013   04e7=1255 bytes
00010000000000000000000000005C3E03D600000000000000000000000000000000000000000000000000000000000000000000000000000000002A8BC32F43277FE9C063B9C99EA252B483941DCD06E217FA7566D275006C6C467876758F2BC87E4CEBD2020AE9CF9F294C6217828D68721B7FFF259AEE3EDFB5867C4775E4E1717826B843CDA6685E5140442ECE7BFC2EE8BB096A73CBE6E099968965F5342FC1702EE2802802902286DCF0F279E326BF00000000000000000000000000000000000000000000000000000000000000000000000000000001000000000000000100000004000000000000074000000000000000291D1B0243A0E60E4600721B09FCFF27E171E339DE0AB14852AA1641EB7F342ABF6528088B15D93B9923465E789431429698932A53B14D6DDADCA9551C2C938BB8B0B121090F37BAF582476C7303372F20617DE54B04F8688A2CAC85F19957463AE9C0CCC1988BCA7FF9534FC990EC410FB0EDE29EB9DDF10638BA000EFBE123621F8DDEDC7480A597FD297E14C5C6E73668F95F524A3F415FF3030000000000000000000000000000000100000000000000010108B7E57C448DB5EF25AA119DDE2312C64D7FF1B890C416C6DDA5EC73CBFED2EDEA00000000000002A39330AD8CDE205F317C6537ECA96B866293A0489615A9A277B4D3A597C873544C82474932B641E06AC8719604EE52E895E8CD4621B6BFB85780CD9BECCE14D0700B83A664DB2F52A26C425FD777AD88944CDFFF38043A2793ED4D9AA67E36CBFD5585579FC69DDA930418AF5EAF603654F6F751258D2DFC8C2113C171E130F31EC1E6CCE2A718E435298FCE5D64FFE1BD3464FD7C87CFA92093855BE034BFE4439E928BD92AD77FD0A0E00355EE1D1A9CEB1ED0C408DCFDBA8C583E7598DC700AAA9F91432097259A405F5B7315A2F7658861E3349BB0DC8BF883726A215F0149DED6613E5AC0670C0C5202247D7C27C8A7D03BDB03C9CF5455463F9B42CF87403E31F8383CC4F49A34C62AE459F5801A9EED4F0EE3DFD5F55B7011C0CAE393C474ABD6F8C7965B9B5FFF3104DD4E39542077C0C8DD2F8FFCEB6BB598512D90506D0A7184F20F1498CF458787F23284B54888C9BE416D103F760406357A16B6D841A303D5C95B6B474D2D7F0FEA0A2A76C897DD2110E9303F54684169421147684C6F1819C33CEF3F38EC995A508450C02CD1872F8065FDEE723109C18B1DD2DDDE75825546ECF0DF0793C353B20C946CD64122CEA8C116F432336899A16AD24A2AAFCB8F900E09A1147135FCF2A54CBF81DB308A47A08A49C77C130E5DC5E661CD55A5CC69E607055A5B08111BF61A62EA5778F85119043633F1CAB8C756D756C5A34851024AC311A596B1CD919BBCA43226F0BA057F6B57DE2F6955B0823C3826DE7F6096C1C1B6B9B8E4063E1645C0BFF32F80561AAA959D97120FBC2ECD9D2BE28BD0C17811DC59A88049F6D8952EE9A0A0207693C89CA3AD1197E9BFDFC03BE9D845AEA8D663969217E3B494CEE9E652BC9F8713E2FD5CB1843848F46C3A6AB024D0E3D57CA45454CDBDA414ADAA835FA147DEB4FFB7129CF3A8D86726A01447940100000000000000000000000000000000096385D86C5CFDA718AA0B7295BE0ADF7E5AC051EDFE130593A2A257F09F78A3B150D029AB1CE0FA793CC0D5E86FC76F69121636A56B21BA71BA640C2A486A2A1443FDBCB2E4F615A8FD1216B3293FFADA50844B43F40B6C1BBCFBD4A6E96775ED
    
after the block I request, I received ping:
613D030000000000000010000382FCF5BE079D000000000004FC3C
and then I received 411 bytes(a header)
613D0800000000000001900002000000000004FC3D000000005D6A4DB9000018E85B4D5730D71C13280FC51DA2DBEEE3124961BB74F91369915171A5672ADD57BBBD9922C10EE6C8DD105108D33EF61757CE3589A70CACF7014A5E78BAA4FF28B691C1BA533E2854F13BA6763AF5E886045F5F779836AF94CD1FC9D9ABF46986A1848CDF25ED1D84787606CC803FB644E697FF936A2162691112DFFC16E83AFEDE175E01840DA9C14EF6DC3466591046F16E3F54A03C25091358C66E602CDA82B9141E8B808089D56A10D487D653A95C734265288177AA313B32F3AE70000000000038183B00000000001DEA02000382FDD421D2F200000240A0018F4E142AF4781D991E9420462D142034CA83C3ED12C125623AC0D3DB48C68D29E1BD5F31A3D244066F1DCF443E249B5B721E952A72AB26EC30D8AACD0973BB79753823D42E2AF0FBC9255B443CD58C88A1EF1FE91D1F25E632B5EC60059B127E649327F07606560DCFDF4119CA0E8552E52A712A9A6C85E567B8B4158751F918713F3731D0EAAF18DF4D7523D85540F83B7D4B96DF7E8AF597D625BF8A06F703    
    
    
    
     */
    void onReceivedBlock(ByteBuffer bb) {
        System.out.println("block data:");
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        System.out.println(Hex.toHexString(bytes));
    }

    void onReceivedGetCompactBlock(ByteBuffer bb) {
        byte[] hash = new byte[32];
        bb.get(hash);
        CompletableFuture.runAsync(() -> {
            Block blk = null;
            //TODO: serve the peer with the block data.
            ByteBuffer bbw = null;
            writeBlock(bbw, blk);
            //TODO: send data
        }).exceptionally(t -> {
            //TODO: UI element or whatever.
            t.printStackTrace();
            return (Void) null;
        });
    }

    void onReceivedCompactBlock(ByteBuffer bb) {
        System.out.println("compact block data:");
        byte[] bytes = new byte[bb.remaining()];
        bb.get(bytes);
        System.out.println(Hex.toHexString(bytes));
    }

    /**
     *
     *
     *
     * @param bb
     * @param blk
     * @return
     */
    ByteBuffer writeCompactBlock(ByteBuffer bb, Block blk) {
        blk.writeCompact(bb);
        return bb;
    }

//    ByteBuffer write(ByteBuffer bb, StemTx stemtx) {
//        return bb;
//    }
    ByteBuffer write(ByteBuffer bb, Tx tx) {
        return bb;
    }

    /**
     *
     *
     *
     * @param bb
     * @param stem what does it mean?
     *
     * @return
     */
    Tx read(ByteBuffer bb, boolean stem) {
        byte[] blindingFactor = new byte[32];
        bb.get(blindingFactor);
        //TODO:     
        Tx tx = new Tx();
        tx.read(bb);
        return tx;
    }

    ByteBuffer writeTxHashSetRequest(ByteBuffer bb, Block blk) {
        if (blk.hash.length != 32) {
            throw new IllegalStateException(":" + blk.hash.length);
        }
        bb.put(blk.hash);
        bb.putLong(blk.height);
        return bb;
    }

    void onReceivedTxHashSetRequest(ByteBuffer bb) {
        byte[] hash = new byte[32];
        bb.get(hash);
        long height = bb.getLong();
        //TODO: server tx
        CompletableFuture.runAsync(() -> {
            //TODO: write 
        }).exceptionally(t -> {
            t.printStackTrace();
            return (Void) null;
        });
    }

    void onReceivedTxStem(ByteBuffer bb) {

    }

    void onReceivedTx(ByteBuffer bb) {

    }
    SocketChannel sc;
    int state; //1: connect, 2: hand sent, shake received
    private final static int StateNetworkConnect = 1;
    private final static int StateNetworkHandSent = 2;
    private final static int StateNetworkShakeReceived = 4;
    private final static int StateNetworkFailed = 0x80_0000;

    CompletableFuture<Void> connect() {
        return CompletableFuture.runAsync(() -> {
            try {
                if (sc != null) {
                    throw new IllegalStateException();
                }
                sc = SocketChannel.open();
                sc.configureBlocking(false);
                Networking_Core.connect(isa, sc, this);
                state |= StateNetworkConnect;
            } catch (Throwable ex) {
                throw new IllegalStateException(ex);
            }
        });
    }

    @Override
    public void doWrite(SelectableChannel channel) {
        System.out.println("do write is called");
        while (true) {
            ByteBuffer bb = q2send.peek();
            if (bb == null) {
                Networking_Core.change(SelectionKey.OP_READ, key, this);
                break;
            }
            try {
                int n = sc.write(bb);
                System.out.println(n + " bytes written"); //handshake sent, but no response at all.
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            if (bb.hasRemaining()) {
                break;
            }
            q2send.poll();
        }

    }

    /**
     *
     *
     * @param type
     * @param bb in writing mode
     */
    void writeMsg(int type, ByteBuffer bb) {
        //bb.flip(); //turn it to reading mode.
        int pos = bb.position();
        ByteBuffer bbFinal = ByteBuffer.allocate(1024);
        ByteBuffer.allocateDirect(11 + pos);
        bbFinal.putShort((short) self.genesis.MAGIC);
        bbFinal.put((byte) type);
        bbFinal.putLong(pos);
        //bbFinal.put(bb);
        bbFinal.put(bb.array(), 0, pos);
        bbFinal.flip();
        if (false) { //as test
            read(bbFinal); //as test
            bbFinal.position(0);
        }
        q2send.add(bbFinal);
        this.waiting2write();
    }

    ByteBuffer bbRead = ByteBuffer.allocateDirect(1024);

    @Override
    public void doRead(SelectableChannel c) {
        System.out.println("before read" + bbRead.position() + "/" + bbRead.limit());
        int len;
        try {
            len = sc.read(bbRead);
            if (len == -1) {
                sc.close();
            }
        } catch (Throwable t) {
            this.onNetworkFailed(t);
            return;
        }
        //if(len!=-1)
        System.out.println("after  read" + bbRead.position() + "/" + bbRead.limit() + " len:" + len);
        bbRead.flip();
        if (bbRead.hasRemaining()) {
//613D02000000000000004C000000010000000F0002E4841EFCD7C900000000000000144D572F4772696E20322E302E312D626574612E3140ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82
//000000010000000F0002E4841EFCD7C900000000000000144D572F4772696E20322E302E312D626574612E3140ADAD0AEC27797B48840AA9E00472015C21BAEA118CE7A2FF1A82C0F8F5BF82
//613D140000000000000020BB2F666934DACC5AB963995BDA08A5EE3B4A0E00C594AC60E10F5DC8F089068C
            read(bbRead);
        }
        bbRead.compact();
    }

    /**
     * 20190729 what does it mean? I have already forgot it.
     *
     * @return
     */
    @Override
    public boolean waiting2write() {
        //return q2send.peek() != null;
        Networking_Core.change(SelectionKey.OP_WRITE | SelectionKey.OP_READ, key, this);
        return true;
    }

    @Override
    public void on(Throwable e) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    SelectionKey key;

    @Override
    public void on(SelectionKey key) {
        this.key = key;//I can just ignore
    }
    ConcurrentLinkedQueue<ByteBuffer> q2send = new ConcurrentLinkedQueue<>();

    @Override
    public void onConnectable() {
        try {
            boolean bret = sc.finishConnect(); //if succeeded, do I get any notification?
            System.out.println("connected: " + bret + " to " + sc.getRemoteAddress());
            newNonce_hand2send();
            ByteBuffer bb = ByteBuffer.allocate(1024);
            this.writeHand(bb); //prepare data in bb
            writeMsg(Hand, bb);
            state |= StateNetworkHandSent; //fake, in fact, not sent yet by now.
        } catch (Throwable t) {
            try {
                System.out.println(this.isa + " failed");
                sc.close();
            } catch (Throwable t2) {
            }
            //t.printStackTrace(System.out);
            onNetworkFailed(t);
        }
    }
    Throwable tNetwrok;

    void onNetworkFailed(Throwable t) {
        t.printStackTrace();
        this.tNetwrok = t;
        state |= StateNetworkFailed;
        self.onNetworkFailed(this, t);
    }

    private class Msg {

        int type;
        byte[] data; //I had better turn it into a blocked stream, especially when the length is long.
        int received;
        long length;

        boolean waiting() {
            return received < length;
        }
    }

    /**
     * do I need this? for a specific message. but for which one?
     *
     *
     */
    class MsgWhat {

        long total_difficulty;
        int secondary_scaling;
        long nonce;
        /**
         * cuckoo_size: 1 byte, for c29: 42*29bits=153 bytes, for c31:
         * 42*31bits=163 bytes
         *
         * for p2p message, it is 42*8bytes=336 bytes.
         */
//        byte[] proof; 
        private short edgeBits;
        private byte[] proofNonces;

        void parseProofOfWork(ByteBuffer bbw) {

            this.total_difficulty = bbw.getLong();
            this.secondary_scaling = bbw.getInt();
            this.nonce = bbw.getLong();
            edgeBits = bbw.get();
            System.out.println("cuckoo_size:" + this.edgeBits);
            int len;
            if (edgeBits == 29) {
                len = 153;
            } else if (edgeBits == 31) {
                len = 163;
            } else {
                throw new IllegalStateException("cuckoo_size:" + edgeBits);
            }
            proofNonces = new byte[42];
            bbw.get(proofNonces);
        }
    }
    static Charset cutf8 = Charset.forName("utf8");

    void onReceivedError(int code, String message) {
        try {
            //TODO: to be override or with a callback?
            System.out.println("error:" + code + " " + message);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    private final static boolean doTest = true;
    Msg msgCurrent = new Msg();

    void read(ByteBuffer bb) {
        int pos = bb.position(), limit = bb.limit();
        if (doTest) {
            byte[] bytes = new byte[limit];
            bb.position(0);
            bb.get(bytes);
            bb.position(pos);
            System.out.println("data pos:" + pos + "\n" + Hex.toHexString(bytes)); //too long to process.
        }
        while (true) {
            try {
                if (msgCurrent.waiting()) {
                    int bytes = bb.remaining();
                    if (bytes > msgCurrent.length) {
                        bytes = (int) msgCurrent.length;
                    }
                    bb.get(msgCurrent.data, msgCurrent.received, bytes);
                    msgCurrent.received += bytes;
                    if (msgCurrent.waiting() == false) {
                        ByteBuffer bbReceived = ByteBuffer.wrap(msgCurrent.data, 0, msgCurrent.received);
                        CompletableFuture.runAsync(() -> {
                            try {
                                processMsg(msgCurrent.type, bbReceived);
                            } catch (Throwable t) {
                                t.printStackTrace(System.out);
                            }
                        });
                        msgCurrent.data = null;
                    }
                } else {
//we are not waiting for a message to be completed, so we are waiting for a new msg
                    int magic = bb.getShort();
                    magic &= 0xffff;
                    if (magic != self.genesis.MAGIC) {
                        //TODO: if we are not in debug, I should discard this peer.
                        throw new IllegalStateException(Integer.toHexString(magic));
                    }
                    msgCurrent.type = bb.get();
                    long length = bb.getLong();
                    msgCurrent.type &= 0xff;
                    if (bb.remaining() >= length) {
                        int limitnew=(int) (bb.position() + length);
                        bb.limit(limitnew);
                        try {
                            processMsg(msgCurrent.type, bb);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        bb.position(limitnew).limit(limit);
                    } else {
                        msgCurrent.length = length;
                        if (msgCurrent.data == null || msgCurrent.data.length < length) {
                            msgCurrent.data = new byte[(int) length];
                        }
                    }
                }
                if (bb.remaining() <= 11) { //magic(2)+type(1)+length(8)
                    break;
                }
            } catch (Throwable t) {
                t.printStackTrace();
//                bb.position(pos);
                break;
            }
        }
    }

    void processMsg(int type, ByteBuffer bb) {
        switch (type) {
            case Error: {
                int code = bb.getInt();
                String message = readString(bb);
                onReceivedError(code, message);
                break;
            }
            case Hand: { //1
                self.onReceivedHand(bb); //Tothink: should be "self."?
                break;
            }
            case Shake: { //2
                onReceivedShake(bb);
                break;
            }
            case Ping: { //3
                onReceivedPing(bb);
                break;
            }
            case Pong: { //4
                onReceivedPong(bb);
                break;
            }
            case GetPeerAddrs: { //5
                int mask = bb.get();
                onReceivedGetPeerAddrs(mask);
                break;
            }
            case PeerAddrs: {  //6
                //TODO: bb might not have all the data available
                //    some more are being received, but not in the bb yet.
                self.onReceivedPeerAddrs(bb);
                break;
            }
            case GetHeaders: { //7
                onReceivedGetHeaders(bb);
                break;
            }
            case Header: { //8
                onReceivedHeader(bb);
                break;
            }
            case Headers: { //9
                onReceivedHeaders(bb);
                break;
            }
            case GetBlock: { //10
                this.onReceivedGetBlock(bb);
                break;
            }
            case Block: { //11
                this.onReceivedBlock(bb);
                break;
            }
            case GetCompactBlock: { //12
                this.onReceivedGetCompactBlock(bb);
                break;
            }
            case CompactBlock: { //13
                this.onReceivedCompactBlock(bb);
                break;
            }
            case StemTransaction: { //14
                this.onReceivedTxStem(bb);
                break;
            }
            case Transaction: { //15 = 0x0F 
                /**
                 * confirmed or not? if not, then we just ignore it.
                 *
                 * does the sender send the tx to us directly? if yes, then we
                 * can ignore this. if not, then we will have to monitor them.
                 */
                this.onReceivedTx(bb);
                break;
            }
            case TxHashSetRequest: { //16 Used to request the transaction hashset from a peer.
                onReceivedTxHashSetRequest(bb);
                break;
            }
            case TxHashSetArchive: { //17 The transaction hashset in response to the TxHashSetRequest message.
                self.onReceivedTxHashSetArchive(bb);
                break;
            }
            case BanReason: {
                break;
            }
            case GetTransaction:{ //19= 0x13
                break;
            }
            case TransactionKernel:{  //20= 0x14
//613D140000000000000020DD5D0F68A6789C58A9D8804A40356D302F926B6C1C6F257837CA495A0CD2180F
//  what does it mean?
//TODO:     type 0x14  what does it mean? tx hash or blk hash?
// I can see that after 0x14 is received, I sent 0x13 out.    
//    and then 0x0f is followed so this is tx hash?    pretty sure it is not of block!
//613d1400000000000000208acb345cad2d1886b39cf0f7dda00f877db73a091edcf098f3bca2e88df6a2f4
//613d1300000000000000208acb345cad2d1886b39cf0f7dda00f877db73a091edcf098f3bca2e88df6a2f4    
//		TransactionKernel = 20,
//		KernelDataRequest = 21,
//		KernelDataResponse = 22,

System.out.println(String.format("time: %1$tM%1$tS", System.currentTimeMillis()));
                break;
            }
            case KernelDataRequest:{  //21= 0x15
                break;
            }
            case KernelDataResponse:{  //22= 0x16
                break;
            }
            default:
                throw new IllegalStateException("unknown type:" + type);
        }
    }
}
