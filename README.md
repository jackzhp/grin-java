# grin-java
The minimum implementation of mimblewimble grin, so a java application can deal with grin transactions.

To have this mini wallet, is it possible without a grin node？ Should be!
when we want to receive grins, we start to listen to the net, till we found the grin we are receiving is collected into a block,
and for safty, we continue to monitor the net for a few blocks till we feel it is safe not to be reorganized.
when we want to send grins, we do not have to monitor the net.



what code is needed

#1. p2p protocol, so tx can be delivered to the net, and collected into a block

#2. some block samples, parse those blocks

#3. some tx samples, parse those tx
    rust code: https://github.com/mimblewimble/grin/blob/master/core/src/core/transaction.rs
    
#4. generate tx
    https://github.com/mimblewimble/grin/blob/master/core/src/libtx/build.rs
    
#5. range proof:
    rust code: https://github.com/mimblewimble/grin/blob/master/core/src/libtx/proof.rs
    
#6. monitor the chain, notify caller that a specific tx is confirmed.

#7. we do not do validation unless it is very very simple.


what doc is needed?

how double spending is prevented?
    when we are the receiver, we want to make sure the sender did not double spend.
    as long as what we should receive is collected into a block, then we know it did not get double spent.
    

Transaction flow: 
https://github.com/mimblewimble/grin-wallet/blob/master/doc/transaction/basic-transaction-wf.png


I am not able to do much, hope people can contribute.


Progress

#1. connected with peers (working on it)

#2. understand blocks and txs (not started)

#3. tx generation (not started)

#4. 






