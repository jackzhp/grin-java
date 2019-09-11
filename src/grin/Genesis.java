package grin;

import zede.util.Hex;

/**
 * https://github.com/mimblewimble/grin/blob/master/core/src/genesis.rs
 * 
 * 
 * @author jack
 */
public class Genesis {
    
    /**
     * what is the difference between the two?
     * 
     * by gen_hash and by  gen_bin
     * 
     */
    byte[] genesis_hash,genesis_full_hash;
    int MAGIC =0x613d; // 0x1EC5;// is not response  0xC51E; //
    
    
    static Genesis main=new Genesis();
    
    static {
        main.genesis_hash=Hex.toByteArray("40adad0aec27797b48840aa9e00472015c21baea118ce7a2ff1a82c0f8f5bf82");
        main.genesis_full_hash=Hex.toByteArray("6be6f34b657b785e558e85cc3b8bdb5bcbe8c10e7e58524c8027da7727e189ef");
    }
    
    
    
}
