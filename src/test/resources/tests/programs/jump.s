main:
  li t0, 40
  li t1, 40
  blt t0, t1, mog
  call find
mog:
  done
find:
  jr ra
