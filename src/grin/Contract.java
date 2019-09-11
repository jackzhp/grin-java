package grin;

/*
 *
 * https://github.com/mimblewimble/grin/blob/master/doc/contracts.md
 * 
 * The trustless tx. is it the normal grin tx?
 * https://github.com/mimblewimble/grin-wallet/blob/master/doc/transaction/basic-transaction-wf.png
 * 
 * sender's output  
 *        ks*G, rs*G       ->   ks*G, rs*G
 * receiver's output
 *    kr*G, rr*G, sig_r    <-   kr*G, rr*G,  sig_r=kr + e * rr while e is the 
 *             e = SHA256(R|P|blake2b(fee|lock_height))
                   R=kr*G + ks*G, P=rr*G + rs*G
 * 
 * now sender can compute 
 *         sig_s=ks + e*rs
 *
 *   the tx includes output r, output s, 
 *            signature (sig_r, sig_s)
 * The above process is good for the receiver to decide how much fee to pay.
 * 
 * If sender to pay the fee, the above process can also be used. 
 * but the receiver can initiate the process by sending kr*G & rr*G (while output r is optional)
 *  to the sender.  
 *   when sender receives it, the sender can calculate the hash used in the signature.
 * 
 * 
 *       could ks & kr be same as the gamma in the value committed?
 *                      V=h^gamma * g^value
 *       I do not see any problem. since V is public. g^value is known to both parties to the tx.
 *               so h^gamma can be derived. h in the bullet proof paper is indeed G.
 * 
 * end of trustless tx. it is just a simple transfer.
 * 
 * 
 *
 */
public class Contract {

    /** this part is well analyzed, so send & receive can be implemented.
     * 
     * For a simple value transfer, at most 2 outputs: 1 for the receiver, 1 for
     * the sender as the change.
     *
     * #1. be noted that the sender can sign the tx.
     *
     * #2. the sender need some inputs, these inputs gives total gamma(gamma_i)
     * and the total value(v_i). Be noted that in order to spend these
     * inputs(they are the committed values of outputs on the chain), what is
     * needed is the value and its gamma, nothing else.
     *
     *
     * #3. 2 equations must be observed(suffix _i means input, _or means output
     * for receiver, _os means output for sender):
     *
     * v_i = fee + v_or + v_os and gamma_i = offset + excess + gamma_or+gamma_os
     *
     * #4. either the sender pays for the fee, or the receiver pays for the fee.
     * fee, v_or, and v_os are determined.
     *
     * #4.1 the sender pays the fee: the sender knows v_or, so given the v_i,
     * the fee & v_os can be determined. in this case, the receiver does not
     * have to check how much the fee is. the receiver cares only v_or(the
     * amount).
     *
     * #4.2 the receiver pays the fee: the sender & the receiver agree on the
     * amount much the sender pays. in this case, can the sender stole some
     * money from the fee? TODO: I have to think this through.
     *
     * #4.3 or the sender and the receiver agree on the fee.
     *
     * #5. the sender sends G^rs, G^（gamma_i-offset-gamma_os)=G^(excess +
     * gamma_or) and H^(v_i -v_os)=H^(fee + v_or) or H^(v_i - fee - v_os)=H^v_or
     * to the receiver, while rs is a random chosen by the sender privately.
     *
     *
     * #6. upon received, the receiver check value and fee consistency. If
     * consistency check fails, then this tx fails.
     *
     * #7. the receiver generates an output(including G^gamma_or, H^v_or, and
     * the range proof for v_or). The receiver have to record gamma_or & v_or,
     * so the receiver can spend the output in the future.
     *
     * #8. the receiver generates a Schnorr half signature(G^rr,sr), while rr is
     * a random chosen by the receiver privately, sr=rr + e*gamma_or and
     * e=Hash(x(R)|pk33(G^excess)|blake2b-256(features|fee|lock_height)), R=G^rr
     * * G^sr;
     *
     * why the term half?since G^rr & G^gamma_or are from the the receiver;
     * while G^rs & G^(excess + gamma_or) are from the sender.
     *
     * #9. the receiver sends the half signature (G^rr,sr) and G^gamma_or to the
     * sender.
     *
     * #10. The sender generates Schnorr another half signature(G^rs,ss), while
     * ss=rs + e*(gamma_i-offset-gamma_os) and e is calculated in the same way
     * as the receiver. The sender can calculate G^excess only after received
     * G^gamma_or from the receiver. The sender can calculate e only after
     * received the Schnorr half signature from the receiver.
     *
     * #11. The sender combines two Schnorr half signatures get the Schnorr
     * singature for the tx: (x(G^rr * G^rs), sr+ss).
     *
     * #12. either the sender or the receiver can build the tx and push it to
     * the network(#input, #output, #kernel=1,offset, inputs, outputs,
     * kernel(feature,fee, lock_height, G^excess, Schnorr sig)), but generally
     * speaking, the sender would like to do this since the sender does not want
     * the receiver to know the inputs.
     *
     * #12.1 The sender build the tx and push it to the network.
     *
     * #12.2 the receiver build the tx and push it to the network upon received
     * the inputs and Schnorr half signature (G^rs, ss) from the sender.
     *
     *
     * On the case when there is no change
     *
     * either v_os==0 and everything is same, or the Schnorr half signature from
     * the sender can be omitted.
     *
     *
     * On the case when there are more than 2 outputs
     *
     * each output corresponding to a receiver, nothing special. Just be noted
     * that R is from all output belonging parties.
     *
     *
     * On output generations
     *
     * to generate an output, related data is: value, gamma,nonce(private
     * nonce), extra_data,message. but only value and gamma must be recorded,
     * all other can be random. though if any info to be encoded into the proof,
     * then those nonce should be kept.
     *
     *
     *
     *
     *
     */



    
    
    
    
}
