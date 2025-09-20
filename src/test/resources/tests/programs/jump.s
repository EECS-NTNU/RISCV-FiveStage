main:
  addi	sp,sp,-16
	sw	ra,12(sp)
	lw	a0,0(zero)
	call	find
	lw	ra,12(sp)
	addi	sp,sp,16
	jr	ra
find:
  ret
