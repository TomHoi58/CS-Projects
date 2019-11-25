addi t1, s1, 2047
addi t2, t1, -700
add t2, t1, t0
add t1, x0, t2
mul t1 ,t0, t1
mul x0, t1, t2
mul a0 t1 x0
mulh t1 t2 t0
mulh x0 t1 t2
mulhu t2 t0 t1
xor t1 t0 t0
xor t1 t0 t2
slt t2 t1 t0
slt t1 x0 t2
sub t1 t1 t1
sub t2 t1 t0
srl t1 t2 x0
srl t2 x0 x0
srl t2 t1 t0
sll t2 t1 t0
sll x0 x0 x0
sll t2 t1 x0


or t1 t2 t0
or t1 x0 t2
or t1 t1 t1
add a0 t1 t2
add s1 a0 t1
addi sp sp -4
addi sp sp 4
add t2 s0 a0
and t1 t2 t0
and t1 x0 t2
and t1 t1 t1
slli t1 t0 2
slli a0 t1 3
slli t2 t1 0
slli t2 t1 29
slti a0 x0 1
slti a0 t1 2
slti x0 x0 0
xori a0 t1 2
xori a0 x0 10
srli t1 t2 4
srli t0 t2 0
srli a0 a0 28
srli x0 t1 2
srai t1 t2 4
srai t0 t2 0
srai a0 a0 20
srai x0 t1 2
ori t1 t2 4
ori t0 t2 0
ori a0 a0 100
ori x0 t1 2
andi t1 t2 4
andi t0 t2 0
andi a0 a0 100
andi x0 t1 2
lui t0 123235
lui t1 12325
addi t0 t2 0
addi x0 x0 3
addi t1 x0 -2048

divu t1 t2 t0
divu t1 t2 x0
remu t1 t2 x0
remu t1 t2 t0
addi t1 x0 2000


sw t2 0(t1)
sw a0 16(t1)
addi t1 x0 4

lw s0 4(t1)
lh s0 8(t1)
lh s0 2(t1)
lh s0 4(t1)
lb s0 2(t1)
lb s0 0(t1)
lb s0 1(t1)
lb s0 3(t1)
addi t0 x0 1
addi t1 x0 1
beq t0 t1 tom

tom:

jal ra end

addi t0 x0 -1
addi t1 x0 1
beq t0 t1 tom

addi t0 x0 1
addi t1 x0 2
beq t0 t1 tom

addi t0 x0 1
addi t1 x0 2
blt t0 t1 tom

add t0 t0 x0
add t1 t1 x0
blt t0 t1 tom

addi t0 x0 2
addi t1 x0 1
blt t0 t1 tom

add t0 t0 x0
add t1 t1 x0
blt t0 t1 tom

addi t0 x0 -2
addi t1 x0 1
blt t0 t1 tom

add t0 t0 x0
add t1 t1 x0
blt t0 t1 tom

addi t0 x0 1
addi t1 x0 1
blt t0 t1 tom

add t0 t0 x0
add t1 t1 x0
blt t0 t1 tom

addi t0 x0 1
addi t1 x0 2
blt t0 t1 tom

add t0 t0 x0
add t1 t1 x0
blt t0 t1 tom

addi t0 x0 2
addi t1 x0 1
bltu t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bltu t0 t1 tom

addi t0 x0 1
addi t1 x0 1
bltu t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bltu t0 t1 tom

addi t0 x0 1
addi t1 x0 2
bltu t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bltu t0 t1 tom

addi t0 x0 2
addi t1 x0 1
bne t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bne t0 t1 tom

addi t0 x0 1
addi t1 x0 1
bne t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bne t0 t1 tom

addi t0 x0 -1
addi t1 x0 -1
bne t0 t1 tom

add t0 t0 x0
add t1 t1 x0
bne t0 t1 tom

end:
jal ra endgame
endgame:
jalr x0 ra 4









