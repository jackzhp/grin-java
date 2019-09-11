package grin;

/**
 *
 *please use the one in road.id.RangeProof
 *
 * rust code:
 * https://github.com/mimblewimble/grin/blob/master/core/src/libtx/proof.rs
 *
 *
 * bullet_proof & verify_bullet_proof are defined in
 * https://github.com/mimblewimble/rust-secp256k1-zkp/src/pedersen.rs
 *
 *
 * which in turn calls secp256k1_bulletproof_rangeproof_prove &
 * secp256k1_bulletproof_rangeproof_verify in
 * https://github.com/mimblewimble/rust-secp256k1-zkp/src/ffi.rs
 *
 * which says the two functions are defined in
 * https://github.com/mimblewimble/secp256k1-zkp/src/modules/bulletproofs/main_impl.h
 *
 *  run 
 * cargo test test_bullet_proof_single
 *         and modify some codes to print info.
 *
 *
 *
 *
 *
 *
 */
public class RangeProof {

}
