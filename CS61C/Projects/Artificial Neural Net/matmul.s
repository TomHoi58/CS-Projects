.globl matmul

.text
# =======================================================
# FUNCTION: Matrix Multiplication of 2 integer matrices
# 	d = matmul(m0, m1)
#   If the dimensions don't match, exit with exit code 2
# Arguments:
# 	a0 is the pointer to the start of m0
#	a1 is the # of rows (height) of m0
#	a2 is the # of columns (width) of m0
#	a3 is the pointer to the start of m1
# 	a4 is the # of rows (height) of m1
#	a5 is the # of columns (width) of m1
#	a6 is the pointer to the the start of d
# Returns:
#	None, sets d = matmul(m0, m1)
# =======================================================
matmul:

    # Error if mismatched dimensions
    bne a2 a4 mismatched_dimensions

    # Prologue
    addi sp sp -48
    sw s0, 0(sp)
    sw s1, 4(sp)
    sw s2, 8(sp)
    sw s3, 12(sp)
    sw s4, 16(sp)
    sw s5, 20(sp)
    sw s6, 24(sp)
    sw s7, 28(sp)
    sw s8, 32(sp)
    sw s9, 36(sp)
    sw s10, 40(sp)
    sw ra, 44(sp)

    #load values to registers
    mv s0 a0
    mv s1 a1
    mv s2 a2
    mv s3 a3
    mv s4 a4
    mv s5 a5
    mv s6 a6
    add s7 x0 x0
    add s8 x0 x0
    add s9 x0 x0
    mul s10 s1 s5

outer_loop_start:
    li s8 0


inner_loop_start:

    slli t4 s7 2
    slli t5 s8 2
    mul t4 t4 s2
    add a0 s0 t4
    add a1 s3 t5
    add a2 s4 x0
    addi a3 x0 1
    add a4 x0 s5
    jal ra dot

    slli t4 s9 2
    add t4 t4 s6
    sw a0 0(t4)



    addi s8 s8 1
    addi s9 s9 1
    bge s8 s5 inner_loop_end
    jal ra inner_loop_start


inner_loop_end:
    addi s7 s7 1
    bge s7 s1 outer_loop_end
    jal ra outer_loop_start



outer_loop_end:

    add a6 s6 x0

    # Epilogue
    lw s0, 0(sp)
    lw s1, 4(sp)
    lw s2, 8(sp)
    lw s3, 12(sp)
    lw s4, 16(sp)
    lw s5, 20(sp)
    lw s6, 24(sp)
    lw s7, 28(sp)
    lw s8, 32(sp)
    lw s9, 36(sp)
    lw s10, 40(sp)
    lw ra, 44(sp)
    addi sp sp 48

    ret


mismatched_dimensions:
    li a1 2
    jal exit2
