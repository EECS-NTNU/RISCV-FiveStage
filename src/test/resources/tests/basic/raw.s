main:
  add x1, x1, x2
  add x1, x1, x1
  add x1, x1, x2
  li  x2, 0x42
  lw  x3, 0(x2)
  add x4, x3, x2
  done
#memset 0x42,  8
