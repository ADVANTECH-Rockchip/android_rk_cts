.source T_shr_long_2addr_6.java
.class public dot.junit.opcodes.shr_long_2addr.d.T_shr_long_2addr_6
.super java/lang/Object


.method public <init>()V
.limit regs 1

       invoke-direct {v0}, java/lang/Object/<init>()V
       return-void
.end method

.method public run(JI)J
.limit regs 11

       shr-long/2addr v7, v10
       return-wide v7
.end method
