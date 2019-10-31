.globl read_matrix

.text
# ==============================================================================
# FUNCTION: Allocates memory and reads in a binary file as a matrix of integers
#   If any file operation fails or doesn't read the proper number of bytes,
#   exit the program with exit code 1.
# FILE FORMAT:
#   The first 8 bytes are two 4 byte ints representing the # of rows and columns
#   in the matrix. Every 4 bytes afterwards is an element of the matrix in
#   row-major order.
# Arguments:
#   a0 is the pointer to string representing the filename
#   a1 is a pointer to an integer, we will set it to the number of rows
#   a2 is a pointer to an integer, we will set it to the number of columns
# Returns:
#   a0 is the pointer to the matrix in memory
# ==============================================================================
read_matrix:

    # Prologue
	addi sp sp -28
	sw s1 0(sp)
    sw s2 4(sp)
    sw s3 8(sp)
    sw s4 12(sp)
    sw s5 16(sp)
    sw s6 20(sp)
    sw ra 24(sp)


    mv s1 a1 #a pointer to the number of rows
    mv s2 a2 #a pointer to the number of columns
    mv a1 a0 #set the filename pointer to a1
    li a2 0 #access code to read only
    jal fopen

    li t1 -1
    beq a0 t1 eof_or_error #check error
    mv s3 a0 #set it to be the descriptor
    li a0 8 #number of bytes to be read
    jal malloc

    mv s4 a0 #set it to be the first buffer pointer
    mv a1 s3 #set first argument to be descriptor
    mv a2 s4 #set second argument to be first buffer pointer
    li a3 8 #set third argument to be number of bytes read
    jal fread

    bne a0 a3 eof_or_error #check error
    lw t1 0(s4) #get the number of rows
    lw t2 4(s4) #get the number of columns
    sw t1 0(s1) #save it to the row pointer
    sw t2 0(s2) #save it to the column pointer
    mul t3 t1 t2
    slli s5 t3 2 #calculate the bytes to read by multiplying the rows and columns *4
    mv a0 s5 #set first argument to be the bytes to read
    jal malloc

    mv s6 a0 #set it to be the second buffer pointer
    mv a1 s3 #set first argument to be descriptor
    mv a2 s6 #set second argument to be second buffer pointer
    mv a3 s5 #set third argument to be the bytes to read
    jal fread

    bne a0 a3 eof_or_error #check error


    mv a0 s6 #is the pointer to the matrix in memory

    # Epilogue
    lw s1, 0(sp)
    lw s2, 4(sp)
    lw s3, 8(sp)
    lw s4, 12(sp)
    lw s5, 16(sp)
    lw s6, 20(sp)
    lw ra 24(sp)
    addi sp sp 28

    ret

eof_or_error:
    li a1 1
    jal exit2
    