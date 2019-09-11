package grin;

import grin.Tx.Output;
import grin.Tx.TxKernel;
import java.nio.ByteBuffer;
import zede.util.Hex;

/**
 *
 * @author jack
 */
public class BlockCompact extends Block {

    BlockHeader header = new BlockHeader();
    long nonce;
    CompactBlockBody body = new CompactBlockBody();

    /*              0x04f5 = 1269
613d0d00000000000004f5000200000000000442ce000000005d3eb05f000000383f4ae224910ef5376810980e330b1f05f8f5e01ef9d387019f23d111f4e63e0f87e2ce7a320019c3eb1bd3ebceb244d67ffebad097197a40d9438ac543c1afafb939df9f060a70cf298700dfb11161e3a8676231c067efe5868abee366a89a4fb3a89aeff8873d04b933ae90544e286f07c3024dbf8c34e26512d9aa1637125dd5410c5a680fab7656cf0c816ee6c9ec39bdb411c1c62f6dfe809de9cbfecfc8dd3cacda6f4aa1ad0966fa220359d810a6f2f6008d4f79f1b38de12b0000000000313091000000000019c7ad0002e50ea8c4702a0000029b8898adc6002629ff1dbdd33fe0835f2ba4219005a0b9afd1c87d4060d52d4994be8af1ce143335ef18a76fbc07910447a42d072315128ca598944f567ac8ddba598a5d26b0faab208682418df4b0a6a98576d764d9e4d3a89c51a0ae037d2e79df39630fc598f429d845c59a4ca269a5358dab514832653b4a76493ccdee6a7fdac7ed6c4f4b8beeb2eb73ae98b3d32b98a7bcab669f2f2eedf630a360df5db7f803086a7a26cf60634a0000000000000001000000000000000100000000000000010109c5564463b0322c940cc05e9e60c467b7ab3072075e4c7fe2547de3cb1a65b4d500000000000002a3bc9cc094ab9a7364cb381e776f30e03d9d1321d70faed35d3f3fe2f3e07eb4fa2b14cec8019cdf7f48c81691f06eeb2aae7f6d5fe3f9959f0584d0e9f6b1b7af01198814f3873baa1bbfecf9289c8c4133b757cfa8f54ea427ad5ae7c543303dbe0814bc9551877cca1d5b5ec351fe9ba09f504b4a6675ad6311735a4aa9a790e3484d2cf55f16e14825d1eb5029b93b88446ec82bbbd8daf2f416dfdf841fbca397a0071efc652f4f5dd4f662a52daba6729cdef3463f736b5a79a48adf2f515f58392020b52830d2fff5eed0891c72122575bdb948df10d9d5370a97e911f340ba29694971d11c9d5596d080cef4440dc3af763413e28aa229c811a658d0e8620d4963a10d422d694b862cb9494877afcb46f31b20ce7209075f2f4a3457269dc97e29f59945841c532f4105c83fb1b2444de8fc716ca2e1a10775c83a51f4504f019a4bf0c0ec0227666296399cd81777649494a701b4c4ac32141542df187cb7010369265f2ea9aee990d3ec09b229e8eb173733016a759c69312993f067caa79cb1cb2ecf76fe80c9e8b292843ea746809e10d5220b0150ed536adff4839c51b3cc7fe1cf64f3cb73a99912fa638a9e1dedf62e7aa51f630c0ab3385629e8b7e0bedc0e0043bb5914e050d2dd457ac3d4e4aa91a6f23d5bd5c837c07f677cf77be0701c5eafcfce5aead6003359342657414ec1a95ec1551fa61ec763e6f7f1b7d1d31dca1b685de2cd270ef79bc785c23e38c7a159414afc5013bece3b849698f6e7ad897ee0c5b3af47411806c133f3940ee57b5652a7913fc17510fae196aa63f9ae31a67544d801c2c78c6ae9c572e73d344ba52276fce4a0412184bb8fa790f21958f08d05d6e3d1483eb3bf06f93e47e51abb2e4e30c60d53c3f71cb4c6b6e08324222202314c1bd0d5338d9ad92be8dfc0e8448ad1c921c7176d742c0101000000000000000000000000000000000971b900a3203014b57c56eeb199d2b9d59fcb8e3baa373dd1392b59d3f1e69b0220c745c171c1ba52f1f7ee79cb4baef553c12ab4d5084d7755e080f8fc31b6355d1b8faf31bd0d9bc3da530488d8443a412202e69ca82f8d31db58a0e0919747ac99a621625e
     2560/2=1280 -11=1269
    
613d0d00000000000004f5      magic..., the following line is header
000200000000000442ce000000005d3eb05f000000383f4ae224910ef5376810980e330b1f05f8f5e01ef9d387019f23d111f4e63e0f87e2ce7a320019c3eb1bd3ebceb244d67ffebad097197a40d9438ac543c1afafb939df9f060a70cf298700dfb11161e3a8676231c067efe5868abee366a89a4fb3a89aeff8873d04b933ae90544e286f07c3024dbf8c34e26512d9aa1637125dd5410c5a680fab7656cf0c816ee6c9ec39bdb411c1c62f6dfe809de9cbfecfc8dd3cacda6f4aa1ad0966fa220359d810a6f2f6008d4f79f1b38de12b0000000000313091000000000019c7ad0002e50ea8c4702a0000029b8898adc6002629ff1dbdd33fe0835f2ba4219005a0b9afd1c87d4060d52d4994be8af1ce143335ef18a76fbc07910447a42d072315128ca598944f567ac8ddba598a5d26b0faab208682418df4b0a6a98576d764d9e4d3a89c51a0ae037d2e79df39630fc598f429d845c59a4ca269a5358dab514832653b4a76493ccdee6a7fdac7ed6c4f4b8beeb2eb73ae98b3d32b98a7bcab669f2f2eedf630a360df5db7f803
086a7a26cf60634a            nonce
      816/2=403
000000000000000100000000000000010000000000000001
    output 717 bytes/each
0109c5564463b0322c940cc05e9e60c467b7ab3072075e4c7fe2547de3cb1a65b4d500000000000002a3bc9cc094ab9a7364cb381e776f30e03d9d1321d70faed35d3f3fe2f3e07eb4fa2b14cec8019cdf7f48c81691f06eeb2aae7f6d5fe3f9959f0584d0e9f6b1b7af01198814f3873baa1bbfecf9289c8c4133b757cfa8f54ea427ad5ae7c543303dbe0814bc9551877cca1d5b5ec351fe9ba09f504b4a6675ad6311735a4aa9a790e3484d2cf55f16e14825d1eb5029b93b88446ec82bbbd8daf2f416dfdf841fbca397a0071efc652f4f5dd4f662a52daba6729cdef3463f736b5a79a48adf2f515f58392020b52830d2fff5eed0891c72122575bdb948df10d9d5370a97e911f340ba29694971d11c9d5596d080cef4440dc3af763413e28aa229c811a658d0e8620d4963a10d422d694b862cb9494877afcb46f31b20ce7209075f2f4a3457269dc97e29f59945841c532f4105c83fb1b2444de8fc716ca2e1a10775c83a51f4504f019a4bf0c0ec0227666296399cd81777649494a701b4c4ac32141542df187cb7010369265f2ea9aee990d3ec09b229e8eb173733016a759c69312993f067caa79cb1cb2ecf76fe80c9e8b292843ea746809e10d5220b0150ed536adff4839c51b3cc7fe1cf64f3cb73a99912fa638a9e1dedf62e7aa51f630c0ab3385629e8b7e0bedc0e0043bb5914e050d2dd457ac3d4e4aa91a6f23d5bd5c837c07f677cf77be0701c5eafcfce5aead6003359342657414ec1a95ec1551fa61ec763e6f7f1b7d1d31dca1b685de2cd270ef79bc785c23e38c7a159414afc5013bece3b849698f6e7ad897ee0c5b3af47411806c133f3940ee57b5652a7913fc17510fae196aa63f9ae31a67544d801c2c78c6ae9c572e73d344ba52276fce4a0412184bb8fa790f21958f08d05d6e3d1483eb3bf06f93e47e51abb2e4e30c60d53c3f71cb4c6b6e08324222202314c1bd0d5338d9ad92be8dfc0e8448ad1c921c7176d742c01
    kernel 114 bytes/each
01000000000000000000000000000000000971b900a3203014b57c56eeb199d2b9d59fcb8e3baa373dd1392b59d3f1e69b0220c745c171c1ba52f1f7ee79cb4baef553c12ab4d5084d7755e080f8fc31b6355d1b8faf31bd0d9bc3da530488d8443a412202e69ca82f8d31db58a0e0919747
    shortID 6 bytes/each
ac99a621625e
     */
    void read(ByteBuffer bb) {
        header.parse(bb);
        nonce = bb.getLong();
        body.read(bb);
    }

    class CompactBlockBody {

        private Output[] outputsFull;//   FullOutputs 	TxOutput[] 	List of full outputs - specifically the coinbase output(s).
        private TxKernel[] kernelsFull;//? 	FullKernels 	TxKernel[] 	List of full kernels - specifically the coinbase kernel(s).
        /**
         * List of transaction kernels, excluding those in the full list. Each
         * ShortId is 6 bytes.
         *
         * what does it exactly mean?          *
         */
        private long[] IdsKernel;//? 	KernelIds 	ShortId[] 	

        void read(ByteBuffer bb) {
            long id_len, output_len, kernel_len;
            output_len = bb.getLong();
            kernel_len = bb.getLong();
            id_len = bb.getLong();
            if (false) {
                System.out.println("tx data:");
                byte[] bytes = new byte[bb.remaining()];
                bb.get(bytes);
                System.out.println(Hex.toHexString(bytes));
            } else {
                System.out.println("#output_len:" + output_len + " #kernel_len:" + kernel_len + " #ids:" + id_len);
                outputsFull = new Output[(int) output_len];
                for (int i = 0; i < output_len; i++) {
                    outputsFull[i] = new Output();
                    outputsFull[i].read(bb);
                }
                kernelsFull = new TxKernel[(int) kernel_len];
                for (int i = 0; i < kernel_len; i++) {
                    kernelsFull[i] = new TxKernel();
                    kernelsFull[i].read(bb);
                }
                IdsKernel = new long[(int) id_len];
                for (int i = 0; i < id_len; i++) {
                    bb.position(bb.position() - 2);
                    IdsKernel[i] = bb.getLong();
                    IdsKernel[i] &= 0xffff_ffffffffl;
                }
            }
        }
    }

    public static void main(String[] args) {
        String s = "613d0d00000000000004f5000200000000000442ce000000005d3eb05f000000383f4ae224910ef5376810980e330b1f05f8f5e01ef9d387019f23d111f4e63e0f87e2ce7a320019c3eb1bd3ebceb244d67ffebad097197a40d9438ac543c1afafb939df9f060a70cf298700dfb11161e3a8676231c067efe5868abee366a89a4fb3a89aeff8873d04b933ae90544e286f07c3024dbf8c34e26512d9aa1637125dd5410c5a680fab7656cf0c816ee6c9ec39bdb411c1c62f6dfe809de9cbfecfc8dd3cacda6f4aa1ad0966fa220359d810a6f2f6008d4f79f1b38de12b0000000000313091000000000019c7ad0002e50ea8c4702a0000029b8898adc6002629ff1dbdd33fe0835f2ba4219005a0b9afd1c87d4060d52d4994be8af1ce143335ef18a76fbc07910447a42d072315128ca598944f567ac8ddba598a5d26b0faab208682418df4b0a6a98576d764d9e4d3a89c51a0ae037d2e79df39630fc598f429d845c59a4ca269a5358dab514832653b4a76493ccdee6a7fdac7ed6c4f4b8beeb2eb73ae98b3d32b98a7bcab669f2f2eedf630a360df5db7f803086a7a26cf60634a0000000000000001000000000000000100000000000000010109c5564463b0322c940cc05e9e60c467b7ab3072075e4c7fe2547de3cb1a65b4d500000000000002a3bc9cc094ab9a7364cb381e776f30e03d9d1321d70faed35d3f3fe2f3e07eb4fa2b14cec8019cdf7f48c81691f06eeb2aae7f6d5fe3f9959f0584d0e9f6b1b7af01198814f3873baa1bbfecf9289c8c4133b757cfa8f54ea427ad5ae7c543303dbe0814bc9551877cca1d5b5ec351fe9ba09f504b4a6675ad6311735a4aa9a790e3484d2cf55f16e14825d1eb5029b93b88446ec82bbbd8daf2f416dfdf841fbca397a0071efc652f4f5dd4f662a52daba6729cdef3463f736b5a79a48adf2f515f58392020b52830d2fff5eed0891c72122575bdb948df10d9d5370a97e911f340ba29694971d11c9d5596d080cef4440dc3af763413e28aa229c811a658d0e8620d4963a10d422d694b862cb9494877afcb46f31b20ce7209075f2f4a3457269dc97e29f59945841c532f4105c83fb1b2444de8fc716ca2e1a10775c83a51f4504f019a4bf0c0ec0227666296399cd81777649494a701b4c4ac32141542df187cb7010369265f2ea9aee990d3ec09b229e8eb173733016a759c69312993f067caa79cb1cb2ecf76fe80c9e8b292843ea746809e10d5220b0150ed536adff4839c51b3cc7fe1cf64f3cb73a99912fa638a9e1dedf62e7aa51f630c0ab3385629e8b7e0bedc0e0043bb5914e050d2dd457ac3d4e4aa91a6f23d5bd5c837c07f677cf77be0701c5eafcfce5aead6003359342657414ec1a95ec1551fa61ec763e6f7f1b7d1d31dca1b685de2cd270ef79bc785c23e38c7a159414afc5013bece3b849698f6e7ad897ee0c5b3af47411806c133f3940ee57b5652a7913fc17510fae196aa63f9ae31a67544d801c2c78c6ae9c572e73d344ba52276fce4a0412184bb8fa790f21958f08d05d6e3d1483eb3bf06f93e47e51abb2e4e30c60d53c3f71cb4c6b6e08324222202314c1bd0d5338d9ad92be8dfc0e8448ad1c921c7176d742c0101000000000000000000000000000000000971b900a3203014b57c56eeb199d2b9d59fcb8e3baa373dd1392b59d3f1e69b0220c745c171c1ba52f1f7ee79cb4baef553c12ab4d5084d7755e080f8fc31b6355d1b8faf31bd0d9bc3da530488d8443a412202e69ca82f8d31db58a0e0919747ac99a621625e";
        byte[] bytes = Hex.toByteArray(s);
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        int istart = 11;
        bb.position(istart);
        BlockCompact bc = new BlockCompact();
        bc.read(bb);
        System.out.println(bb.position() + "/" + bb.limit());

    }
}
