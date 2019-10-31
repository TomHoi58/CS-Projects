.import ../write_matrix.s
.import ../utils.s

.data
m0: .word 1, 3, 5, 7, 9, 11, 13, 15, 17, 19, 21, 23, 25, 27, 29 # MAKE CHANGES HERE
file_path: .asciiz "test_output.bin"

.text
main:
    # Write the matrix to a file
    #initialize the parameters
    la s0 file_path #load address of filename
    la s1 m0 #load address of the matrix
    li s2 5 #set the number of rows of the matrix
    li s3 3 #set the number of columns of the matrix

    #call write_matrix
    mv a0 s0
    mv a1 s1
    mv a2 s2
    mv a3 s3
    jal write_matrix

    # Exit the program
    addi a0 x0 10
    ecall