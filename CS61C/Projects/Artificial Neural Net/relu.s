.globl relu

.text
# ==============================================================================
# FUNCTION: Performs an inplace element-wise ReLU on an array of ints
# Arguments:
# 	a0 is the pointer to the array
#	a1 is the # of elements in the array
# Returns:
#	None
# ==============================================================================
relu:
    # Prologue
    add t0, x0, x0
    add t1, a0, x0


loop_start:
    slt t2, t0, a1
    beq t2, x0, loop_end
    lw t3, 0(t1)
    slti t4, t3, 0
    beq t4, x0, loop_continue
    sw x0, 0(t1)

loop_continue:
    addi t1, t1, 4
    addi t0, t0, 1
    jal x0, loop_start


loop_end:

    # Epilogue

    
	ret
