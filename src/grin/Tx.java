package grin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import zede.hash.Blake2b;
import zede.util.Hex;

/**
 * https://tlu.tarilabs.com/protocols/mimblewimble-1/MainReport.html
 *
 *
 * validation logic: https://github.com/mimblewimble/docs/wiki/Validation-logic
 *
 *
 * the transaction in rust code.
 *
 *
 *
 * rust code:
 * https://github.com/mimblewimble/grin/blob/master/core/src/core/transaction.rs
 *
 * generate tx
 * https://github.com/mimblewimble/grin/blob/master/core/src/libtx/build.rs
 *
 *
 * Transaction flow:
 * https://github.com/mimblewimble/grin-wallet/blob/master/doc/transaction/basic-transaction-wf.png
 * *
 *
 */
public class Tx {

    byte[] blindingFactor = new byte[32];
    TransactionBody body = new TransactionBody();

    /*
                0x0688=1672  -11+1400+ 283 =1400+272=1672
613d0f0000000000000688567d30718657d80b29b46c02f9b69f758c87fc61f3755cdcfd5b1cb68b51628f0000000000000002000000000000000200000000000000010009a4f60391e0a2b3e5cfef0af2db2dfbbbadcc74987181b81ba10f9a8b254a94740109ec45b3570ba46a9b8deb67eed2a15f0c40cdf910e85e9caa73f9fc3a831f16df000909a6aee8f9d20f8fc671acd360e67048999706cc15660a375c3d90aa65f3c99100000000000002a3951b61ff3265b50643fa4faaa418a5ff9250a5853d2433b68069a8c12bce693c92e81f7fed0312facb41a17657b53840c60c7f22fe8281bd046fe932347151b90a09da00dbac07688ab0318ee337e23f65719383bc5f0e83e2a49485ad64f10743fdcfc466097d5c245e934ca0f10ba971ca8073c118f0abfa5e55985fa27d72088a4dcd3bd7b1425f497657634c0e6c8b846a3228d6ab4f52b313602180663e9afe38c10c19eb605192c96ffc28761fe4f85997d43c90bb134a0e7d80715fb7a68de326cddd390036f6a5324e2c03d68bb6dc3cbd0f9f76f63df4a4d4029d419f93aa2320abc932fb1bbc37195c7d83cd2fe72c56e7db6ecbd6be88afdfb016188889116e2330f3cf479d847e9971f91dbba55e15aa1c1cc1cfa8c85b67061605ebf30f9884d1b699bcf96c1aad48f0b24332995bf8d1878839a6be3ec78e39019ca99faec44cc7acb4c77994aa9b9523bb4aa15e322915310f9ae2dfdb9727ca5b0197d21e566b6563dcd2347eddc287da848aa23019441855290662a6f5764cfd9d465ed14fd7ca11d3a0856832528e1bc516095aa9ba835a00e76da0abbca282452b9d03ac73f01c98c52db8e939b1d5f25950f3039d13bbb599db4d4127231b85d27fb576f24af6be0d116a5854cdfc0f3ce3d140b8deaf665cf092c6bebe05c7447ba9e0c7daaa6b07ffe5246684ae62942bf10b8481f040c3c791a4697401b0cd968dd2b8fb8454d2b3bbe18678c17eee6b5fde4dd380f4ddb5e0afc9adcdc086cab82c80a6bf5247b1ac2e738503366086fd73959184c204de219e672e3ff9768dac99b75ebc7a1ed19e40b8c5c7dd6322288da84c96ed5bd13cd4503b1bec6e30017488231f810954bddd09de3064b6d83ce25608b4bf9e591bada44c890f3994b3fd7933f111ee922378284bc72eac177aeaa921ac6ee8a552ab88cf760800091b05227d29c36f6c929f64f4bb9132393fdb2ff5c340ef8c702100be677579fa00000000000002a36e10966ddfdca1da84b0e0bbefc20d697e84e2f712037f3179ea268eb3efe7a44c0e27a5645315eea33a636065b7e610fb6a2c8d875fea2b0481a3ea26fbb30604c66259e50608717bc3b09a8744258486a6d9cfcee03b919c22bd4f76f00c08585182aea9b956a4f49ba08cbac716c1c9f3f6a57d12d7aefaf5ac61573e404dd5d1bb50dbe09e02c8c612d60bae964c1b0eaa3b55bbc2c18fdefd190bd3ec40eee0f7e06962122f2863f842d1cbd381579f072a9e591775fe0b47dabb999e3e4d6471322de0f41da6e53e4a9fb0d52771ab62258150a1a08da08ac51387e907c6228305274ec0222307f17afff7af312592c937414848058cb089bbea8fba4b11fcaa379babfb4bcf180b6ff607cfb8aceb2aa6af27b01c74217fb8c81b6ae4cd0147390f6915f5d9efa1f831110dfa3b755fa2872712483d987de51842dfba2119b342e9beb754d4029d440887f379051d91cb3fa02248f43e7222b870c4adbd3002f734d24c31f89882309f10b43d8a2186b8111c689e1c950d235ea44693ac2d94427b6fa128063cfd2e1bbef3ef4f06e64319cddaf0b571b03788b23ca597b9e4aa8073853ec0371ea9d2ffd8514b949067b04cf4c0161d6066e8143baba17035f08c96763d22cddb8fb67a5d13cc7c0c26c9e1a19cc7f3e58ed9e0eefdb5770381754d3abb13c388b410961e2686a38f4986a3559a9644
c4ca214b6998cd159281387681c1801ee832eb87b40c321932bc2689dd42655bbe2a66c8160b478e4121c612a2f41ba62815ab7bdad4b9ef54a19e6d1789f573f1db4fe8070cc704ab0d58a5fb885384482ca3db9514cb00913fa368611c3803cf84939280d5728caf8faf21231f8d36694e7b9edc8bd7d409eb2bd20ee7473b86238dd93cd7dc4316b8121a5b1778f38d2aa85d82225195d4fc5564371b2c8c45d5502856f756ff380000000000006acfc0000000000000000009d5cfea68797091d30b3820f4c1604e77e6078fabf4e764572553013322c9a3ce5281f0b9dae06a163e19c845073b152b90435e83ce14f9d69946991f3bc41f8eb724093ffe0be4d3bc62616d2d1bd5c3022fa8cc437d7ca00666748995946e56
    
    613d0f0000000000000688                                                  magic(2), type(1),length(8)
    567d30718657d80b29b46c02f9b69f758c87fc61f3755cdcfd5b1cb68b51628f        blindingFactor
    000000000000000200000000000000020000000000000001
    0009a4f60391e0a2b3e5cfef0af2db2dfbbbadcc74987181b81ba10f9a8b254a9474    input #1
    0109ec45b3570ba46a9b8deb67eed2a15f0c40cdf910e85e9caa73f9fc3a831f16df    input #2
        2 output: 717 bytes each
    000909a6aee8f9d20f8fc671acd360e67048999706cc15660a375c3d90aa65f3c99100000000000002a3951b61ff3265b50643fa4faaa418a5ff9250a5853d2433b68069a8c12bce693c92e81f7fed0312facb41a17657b53840c60c7f22fe8281bd046fe932347151b90a09da00dbac07688ab0318ee337e23f65719383bc5f0e83e2a49485ad64f10743fdcfc466097d5c245e934ca0f10ba971ca8073c118f0abfa5e55985fa27d72088a4dcd3bd7b1425f497657634c0e6c8b846a3228d6ab4f52b313602180663e9afe38c10c19eb605192c96ffc28761fe4f85997d43c90bb134a0e7d80715fb7a68de326cddd390036f6a5324e2c03d68bb6dc3cbd0f9f76f63df4a4d4029d419f93aa2320abc932fb1bbc37195c7d83cd2fe72c56e7db6ecbd6be88afdfb016188889116e2330f3cf479d847e9971f91dbba55e15aa1c1cc1cfa8c85b67061605ebf30f9884d1b699bcf96c1aad48f0b24332995bf8d1878839a6be3ec78e39019ca99faec44cc7acb4c77994aa9b9523bb4aa15e322915310f9ae2dfdb9727ca5b0197d21e566b6563dcd2347eddc287da848aa23019441855290662a6f5764cfd9d465ed14fd7ca11d3a0856832528e1bc516095aa9ba835a00e76da0abbca282452b9d03ac73f01c98c52db8e939b1d5f25950f3039d13bbb599db4d4127231b85d27fb576f24af6be0d116a5854cdfc0f3ce3d140b8deaf665cf092c6bebe05c7447ba9e0c7daaa6b07ffe5246684ae62942bf10b8481f040c3c791a4697401b0cd968dd2b8fb8454d2b3bbe18678c17eee6b5fde4dd380f4ddb5e0afc9adcdc086cab82c80a6bf5247b1ac2e738503366086fd73959184c204de219e672e3ff9768dac99b75ebc7a1ed19e40b8c5c7dd6322288da84c96ed5bd13cd4503b1bec6e30017488231f810954bddd09de3064b6d83ce25608b4bf9e591bada44c890f3994b3fd7933f111ee922378284bc72eac177aeaa921ac6ee8a552ab88cf7608
    00091b05227d29c36f6c929f64f4bb9132393fdb2ff5c340ef8c702100be677579fa00000000000002a36e10966ddfdca1da84b0e0bbefc20d697e84e2f712037f3179ea268eb3efe7a44c0e27a5645315eea33a636065b7e610fb6a2c8d875fea2b0481a3ea26fbb30604c66259e50608717bc3b09a8744258486a6d9cfcee03b919c22bd4f76f00c08585182aea9b956a4f49ba08cbac716c1c9f3f6a57d12d7aefaf5ac61573e404dd5d1bb50dbe09e02c8c612d60bae964c1b0eaa3b55bbc2c18fdefd190bd3ec40eee0f7e06962122f2863f842d1cbd381579f072a9e591775fe0b47dabb999e3e4d6471322de0f41da6e53e4a9fb0d52771ab62258150a1a08da08ac51387e907c6228305274ec0222307f17afff7af312592c937414848058cb089bbea8fba4b11fcaa379babfb4bcf180b6ff607cfb8aceb2aa6af27b01c74217fb8c81b6ae4cd0147390f6915f5d9efa1f831110dfa3b755fa2872712483d987de51842dfba2119b342e9beb754d4029d440887f379051d91cb3fa02248f43e7222b870c4adbd3002f734d24c31f89882309f10b43d8a2186b8111c689e1c950d235ea44693ac2d94427b6fa128063cfd2e1bbef3ef4f06e64319cddaf0b571b03788b23ca597b9e4aa8073853ec0371ea9d2ffd8514b949067b04cf4c0161d6066e8143baba17035f08c96763d22cddb8fb67a5d13cc7c0c26c9e1a19cc7f3e58ed9e0eefdb5770381754d3abb13c388b410961e2686a38f4986a3559a9644c4ca214b6998cd159281387681c1801ee832eb87b40c321932bc2689dd42655bbe2a66c8160b478e4121c612a2f41ba62815ab7bdad4b9ef54a19e6d1789f573f1db4fe8070cc704ab0d58a5fb885384482ca3db9514cb00913fa368611c3803cf84939280d5728caf8faf21231f8d36694e7b9edc8bd7d409eb2bd20ee7473b86238dd93cd7dc4316b8121a5b1778f38d2aa85d82225195d4fc5564371b2c8c45d5502856f756ff38
        1 kernel 114 bytes
    0000000000006acfc0000000000000000009d5cfea68797091d30b3820f4c1604e77e6078fabf4e764572553013322c9a3ce5281f0b9dae06a163e19c845073b152b90435e83ce14f9d69946991f3bc41f8eb724093ffe0be4d3bc62616d2d1bd5c3022fa8cc437d7ca00666748995946e56
    
     */
    void read(ByteBuffer bb) {
        bb.get(this.blindingFactor);
        body.read(bb);
        if(true){
            //for the above test case, the balance check is done in 
            //Secp256k1Mine.testGrinTxBalance();
        }
    }

    static class TransactionBody {

        /*
https://github.com/mimblewimble/docs/wiki/P2P-Protocol

says it contains FullOutputs, FullKernels, KernelIds        
        
         */
        Input[] input;
        Output[] output;
        TxKernel[] kernels;
/** 6 bytes each. what does it mean? the offset? the total offset of this tx is the blinding factor.
 * that's confirmed with testGrinTxBalance().
 * 
 * I have not see any example of it. mistake. kernelIds are in CompactBlockBody,
 * not in TxBody. and this variable is never used.
 */
//        long[] kernelIds; 

        void read(ByteBuffer bb) {
            long input_len, output_len, kernel_len;
            input_len = bb.getLong(); //improve: they should just use variantInt
            output_len = bb.getLong();
            kernel_len = bb.getLong();
            if (false) {
                System.out.println("tx data:");
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                System.out.println(Hex.toHexString(bytes));
            } else {
                System.out.println("#input:" + input_len + " #output_len:" + output_len + " #kernel_len:" + kernel_len);
                input = new Input[(int) input_len];
                for (int i = 0; i < input_len; i++) {
                    input[i] = new Input();
                    input[i].read(bb);
                }
                output = new Output[(int) output_len];
                for (int i = 0; i < output_len; i++) {
                    output[i] = new Output();
                    output[i].read(bb);
                }
                kernels = new TxKernel[(int) kernel_len];
                for (int i = 0; i < kernel_len; i++) {
                    kernels[i] = new TxKernel();
                    kernels[i].read(bb);
                }
            }
        }

    }

    /**
     * from
     *
     * https://github.com/mimblewimble/grin/blob/master/core/src/core/transaction.rs
     * 
     * kernel & offset balance check is in grin/core/src/core/committed.rs
     *
     *
     * @author jack
     */
    static class TxKernel {
/** 1 byte for feature, 8 bytes for fee, 8 bytes for lock height, 33 bytes for commitment, 114-1-8-8-33= 64 bytes for sig
 * 
 */
        // Options for a kernel's structure or use.  what did I try to say?

        byte features; //0: plain, 1: coinbase, 2: heightlocked
        /// Fee originally included in the transaction this proof is for.
        long fee;
        /// This kernel is not valid earlier than lock_height blocks
        /// The max lock_height of all *inputs* to this transaction
        long lock_height;
        /// Remainder of the sum of all transaction commitments. If the transaction
        /// is well formed, amounts components should sum to zero and the excess
        /// is hence a valid public key.
        byte[] excess = new byte[33];//33 bytes = new Commitment();
        /**
         * The signature proving the excess is a valid public key, which signs
         * the transaction fee.
         *
         * excess is a commitment with leading byte 08 or 09.
         *
         *
         * it seems that we can get the public key from a signature, so
         * with excess_sig, we can have excess(at most, need one more bit to indicate which one
         * from the possible two). This is not true for Schnorr signature(though it is
         * true for ECDSA).
         *
         */
        byte[] excess_sig = new byte[64];// new Signature(); //64 bytes? or 65?

        void write(ByteBuffer bb) {
            bb.put(features);
            bb.putLong(fee);
            bb.putLong(lock_height);
            if (excess.length != 33) {
                throw new IllegalStateException(":" + excess.length);
            }
            bb.put(excess);
            if (excess_sig.length != 33) {
                throw new IllegalStateException(":" + excess_sig.length);
            }
            bb.put(excess_sig);
        }

        void read(ByteBuffer bb) {
            features = bb.get();
            fee = bb.getLong();
            lock_height = bb.getLong();
            bb.get(excess);//excess.read(bb);
            bb.get(excess_sig); //excess_sig.read(bb);
        }

/// Construct msg from tx fee, lock_height and kernel features.
///
/// msg = hash(features)                       for coinbase kernels
///       hash(features || fee)                for plain kernels
///       hash(features || fee || lock_height) for height locked kernels
///  what type of hash? blake2b-256?
        /**
         * is this method right?
         *
         * @return
         */
        byte[] kernel_sig_msg() {
            byte[] bytes = new byte[17];
            ByteBuffer bb = ByteBuffer.wrap(bytes);
            bb.put(features);
            if (features == 1) { //coinbase
            } else if (features == 0) { //plain
                bb.putLong(fee);
            } else if (features == 2) { //height locked
                bb.putLong(fee);
                bb.putLong(this.lock_height);
            } else {
                throw new IllegalStateException();
            }
//TODO: what kind of hasher? sha256? blake2b-256? or what？            
            Blake2b md = Blake2b.Digest.newInstance(32);  //default params, but change length
            md.update(bytes, 0, bb.position());
            return md.digest();
        }
    }

    static class Input {
        //1 byte for feature, 33 bytes for commitment
/// The features of the output being spent.
        /// We will check maturity for coinbase output.

        byte features; //0: plain, 1: coinbase
        /// The commit referencing the output being spent.
        byte[] commit = new byte[33]; //Commitment  //this refer to an Output in the history? Yes!

        void write(ByteBuffer bb) {
            bb.put(features);
            if (commit.length != 33) {
                throw new IllegalStateException(":" + commit.length);
            }
            bb.put(commit);
        }

        void read(ByteBuffer bb) {
            features = bb.get();
            bb.get(commit);
        }
    }

    static class Output {

        /* equal length 1439-5=1434 /2=717 bytes.
            1 byte for feature, 33 bytes for comittment, 8 bytes for the length of proof, 
        proof(289,353,418,482,546,610,675,739,803, 867, 932,996,1060,1124,1189,...).
        
    000909a6aee8f9d20f8fc671acd360e67048999706cc15660a375c3d90aa65f3c99100000000000002a3951b61ff3265b50643fa4faaa418a5ff9250a5853d2433b68069a8c12bce693c92e81f7fed0312facb41a17657b53840c60c7f22fe8281bd046fe932347151b90a09da00dbac07688ab0318ee337e23f65719383bc5f0e83e2a49485ad64f10743fdcfc466097d5c245e934ca0f10ba971ca8073c118f0abfa5e55985fa27d72088a4dcd3bd7b1425f497657634c0e6c8b846a3228d6ab4f52b313602180663e9afe38c10c19eb605192c96ffc28761fe4f85997d43c90bb134a0e7d80715fb7a68de326cddd390036f6a5324e2c03d68bb6dc3cbd0f9f76f63df4a4d4029d419f93aa2320abc932fb1bbc37195c7d83cd2fe72c56e7db6ecbd6be88afdfb016188889116e2330f3cf479d847e9971f91dbba55e15aa1c1cc1cfa8c85b67061605ebf30f9884d1b699bcf96c1aad48f0b24332995bf8d1878839a6be3ec78e39019ca99faec44cc7acb4c77994aa9b9523bb4aa15e322915310f9ae2dfdb9727ca5b0197d21e566b6563dcd2347eddc287da848aa23019441855290662a6f5764cfd9d465ed14fd7ca11d3a0856832528e1bc516095aa9ba835a00e76da0abbca282452b9d03ac73f01c98c52db8e939b1d5f25950f3039d13bbb599db4d4127231b85d27fb576f24af6be0d116a5854cdfc0f3ce3d140b8deaf665cf092c6bebe05c7447ba9e0c7daaa6b07ffe5246684ae62942bf10b8481f040c3c791a4697401b0cd968dd2b8fb8454d2b3bbe18678c17eee6b5fde4dd380f4ddb5e0afc9adcdc086cab82c80a6bf5247b1ac2e738503366086fd73959184c204de219e672e3ff9768dac99b75ebc7a1ed19e40b8c5c7dd6322288da84c96ed5bd13cd4503b1bec6e30017488231f810954bddd09de3064b6d83ce25608b4bf9e591bada44c890f3994b3fd7933f111ee922378284bc72eac177aeaa921ac6ee8a552ab88cf7608
    00091b05227d29c36f6c929f64f4bb9132393fdb2ff5c340ef8c702100be677579fa00000000000002a36e10966ddfdca1da84b0e0bbefc20d697e84e2f712037f3179ea268eb3efe7a44c0e27a5645315eea33a636065b7e610fb6a2c8d875fea2b0481a3ea26fbb30604c66259e50608717bc3b09a8744258486a6d9cfcee03b919c22bd4f76f00c08585182aea9b956a4f49ba08cbac716c1c9f3f6a57d12d7aefaf5ac61573e404dd5d1bb50dbe09e02c8c612d60bae964c1b0eaa3b55bbc2c18fdefd190bd3ec40eee0f7e06962122f2863f842d1cbd381579f072a9e591775fe0b47dabb999e3e4d6471322de0f41da6e53e4a9fb0d52771ab62258150a1a08da08ac51387e907c6228305274ec0222307f17afff7af312592c937414848058cb089bbea8fba4b11fcaa379babfb4bcf180b6ff607cfb8aceb2aa6af27b01c74217fb8c81b6ae4cd0147390f6915f5d9efa1f831110dfa3b755fa2872712483d987de51842dfba2119b342e9beb754d4029d440887f379051d91cb3fa02248f43e7222b870c4adbd3002f734d24c31f89882309f10b43d8a2186b8111c689e1c950d235ea44693ac2d94427b6fa128063cfd2e1bbef3ef4f06e64319cddaf0b571b03788b23ca597b9e4aa8073853ec0371ea9d2ffd8514b949067b04cf4c0161d6066e8143baba17035f08c96763d22cddb8fb67a5d13cc7c0c26c9e1a19cc7f3e58ed9e0eefdb5770381754d3abb13c388b410961e2686a38f4986a3559a9644c4ca214b6998cd159281387681c1801ee832eb87b40c321932bc2689dd42655bbe2a66c8160b478e4121c612a2f41ba62815ab7bdad4b9ef54a19e6d1789f573f1db4fe8070cc704ab0d58a5fb885384482ca3db9514cb00913fa368611c3803cf84939280d5728caf8faf21231f8d36694e7b9edc8bd7d409eb2bd20ee7473b86238dd93cd7dc4316b8121a5b1778f38d2aa85d82225195d4fc5564371b2c8c45d5502856f756ff38
         */
        byte features; //0: plain, 1:coinbase
        /// The homomorphic commitment representing the output amount
        byte[] commit = new byte[33]; //Commitment
        /// A proof that the commitment is in the right range
        byte[] proof = new byte[683]; ////RangeProof proof;

        ByteBuffer write(ByteBuffer bb) {
            bb.put(features);
            if (commit.length != 33) {
                throw new IllegalStateException(":" + commit.length);
            }
            bb.put(commit);
            if (proof.length != 683) {
                throw new IllegalStateException(":" + proof.length);
            }
            bb.put(proof);
            return bb;
        }

        void read(ByteBuffer bb) {
            features = bb.get();
            bb.get(commit);
            bb.get(proof);
        }
    }
    
    /**
     * the extended Tx for internal use.
     * for simple case, sender & receiver. 
     * 
     */
    static class TxEx extends Tx {
        
        void buildAsSender(Input[] inputs,long change){
            
        }
        
        void buildAsReceiver(long value,byte[] abGamma, byte[] nonce,byte[] nonce_private){
        byte[] extra_data_in = null;
        String message = "some chars as msg";

//        ByteBufferWrapper bbw = ByteBufferWrapper.wrap2write(1024);
//        write(bbw, r2p);
//        byte[] proof = bbw.useByteArray();
//        int len = (int) bbw.position();
//        dumpOutput(proof, len);
            
        }
    }
    

public static void receive()    {
        byte[] abGamma= new byte[32];
        abGamma[31] = 1;
        byte[] nonce = abGamma;
        byte[] private_nonce = nonce;
        byte[] extra_data_in = null;
        String message = "some chars as msg";

//        Range2prove r2p = bullet_proof(value, abGamma, nonce, //also called rewind nonce
//                private_nonce,
//                extra_data_in, message);
//
//        ByteBufferWrapper bbw = ByteBufferWrapper.wrap2write(1024);
//        write(bbw, r2p);
//        byte[] proof = bbw.useByteArray();
//        int len = (int) bbw.position();
//        dumpOutput(proof, len);
    
}
    
    

//    static class Signature {
//
//        private void read(ByteBuffer bb) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//
//    }
//    static class Commitment { //TODO: remove this class.
//        //33 bytes
////
//
//        private void read(ByteBuffer bb) {
//            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
//        }
//        //just 32 bytes?
//    }



}
