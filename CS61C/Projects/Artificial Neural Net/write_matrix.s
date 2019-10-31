.globl write_matrix

.text
# ==============================================================================
# FUNCTION: Writes a matrix of integers into a binary file
#   If any file operation fails or doesn't write the proper number of bytes,
#   exit the program with exit code 1.
# FILE FORMAT:
#   The first 8 bytes of the file will be two 4 byte ints representing the
#   numbers of rows and columns respectively. Every 4 bytes thereafter is an
#   element of the matrix in row-major order.
# Arguments:
#   a0 is the pointer to string representing the filename
#   a1 is the pointer to the start of the matrix in memory
#   a2 is the number of rows in the matrix
#   a3 is the number of columns in the matrix
# Returns:
#   None
# ==============================================================================
write_matrix:

    # Prologue
    addi sp sp -28
    sw s1 0(sp)
    sw s2 4(sp)
    sw s3 8(sp)
    sw s4 12(sp)
    sw s5 16(sp)
    sw s6 20(sp)
    sw ra 24(sp)

    mv s1 a2 #number of rows
    mv s2 a3 #number of columns
    mv s6 a1 #pointer to the second buffer which is matrix
    mv a1 a0 #set the filename pointer to a1
    li a2 1 #access code to write only
    jal fopen

    li t1 -1
    beq a0 t1 eof_or_error #check error
    mv s3 a0 #set it to be the descriptor
    li a0 8 #number of bytes to write
    jal malloc

    mv s4 a0 #set it to be the first buffer pointer
    sw s1 0(s4) #initialize the buffer
    sw s2 4(s4)
    mul s5 s1 s2 #set the total elements in the matrix

    #call first fwrite
    mv a1 s3 #set first argument to be descriptor
    mv a2 s4 #set second argument to be first buffer which contains number of rows and columns
    li a3 2 #set third argument to 2
    li a4 4 #size of each element is 4 bytes
    jal fwrite

    bne a0 a3 eof_or_error #check error

    #call second fwrite
    mv a1 s3 #set first argument to be descriptor
    mv a2 s6 #set second argument to be second buffer which is the matrix
    mv a3 s5 #set third argument to be the number of elements in the matrix
    li a4 4 #size of each element is 4 bytes
    jal fwrite

    bne a0 a3 eof_or_error #check error

    #close the file
    mv a1 s3 #set to the file descriptor
    jal fclose
    bne a0 x0 eof_or_error #check error


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
    