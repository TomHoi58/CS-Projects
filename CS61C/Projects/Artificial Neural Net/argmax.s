.globl argmax

.text
# =================================================================
# FUNCTION: Given a int vector, return the index of the largest
#	element. If there are multiple, return the one
#	with the smallest index.
# Arguments:
# 	a0 is the pointer to the start of the vector
#	a1 is the # of elements in the vector
# Returns:
#	a0 is the first index of the largest element
# =================================================================
argmax:

    # Prologue
	addi a1, a1, -1
	add t0, x0, x0
	add t1, a0, x0
	lw t6, 0(t1)
	add t5, x0, x0


loop_start:
    slt t2, t0, a1
    beq t2, x0, loop_end
    addi t1, t1, 4
    lw t3, 0(t1)
    addi t0, t0 ,1
    slt t4, t6, t3
    beq t4, x0, loop_continue
    lw t6, 0(t1)
    add t5, t0, x0





loop_continue:
    jal x0, loop_start

loop_end:
    add a0, t5, x0

    # Epilogue


    ret
