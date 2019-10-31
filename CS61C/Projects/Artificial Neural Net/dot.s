.globl dot

.text
# =======================================================
# FUNCTION: Dot product of 2 int vectors
# Arguments:
#   a0 is the pointer to the start of v0
#   a1 is the pointer to the start of v1
#   a2 is the length of the vectors
#   a3 is the stride of v0
#   a4 is the stride of v1
# Returns:
#   a0 is the dot product of v0 and v1
# =======================================================
dot:

    # Prologue
    addi sp, sp, -12
    sw s0, 0(sp)
    sw s1, 4(sp)
    sw ra, 8(sp)

    add s0, a0, x0
    add s1, a1, x0
    add t0, x0, x0
    add t1, x0, x0

loop_start:
    slt t2, t0, a2
    beq t2, x0, loop_end
    slli t3, t0, 2
    mul t4, t3, a3
    mul t3, t3, a4
    add t4, t4, s0
    add t3, t3, s1
    lw t5, 0(t4)
    lw t6, 0(t3)
    mul t5, t5, t6
    add t1, t1, t5
    addi t0, t0, 1
    jal x0, loop_start

loop_end:
    add a0, t1, x0

    # Epilogue
    lw s0, 0(sp)
    lw s1, 4(sp)
    lw ra, 8(sp)
    addi sp, sp, 12
    ret
