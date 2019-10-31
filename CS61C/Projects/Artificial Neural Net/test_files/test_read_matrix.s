.import ../read_matrix.s
.import ../utils.s

.data
file_path: .asciiz "./test_input.bin"

.text
main:
    # Read matrix into memory

    #initialize the parameters
    la s0 file_path #load address of filename
    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc

    mv s1 a0 #set the pointer to number of rows to s1
    li a0 4
    jal malloc

    mv s2 a0  #set the pointer to number of columns to s2

    #call read_matrix
    mv a0 s0
    mv a1 s1
    mv a2 s2
    jal read_matrix


    # Print out elements of matrix
    lw a1 0(s1)
    lw a2 0(s2)
    jal print_int_array

    # Terminate the program
    addi a0, x0, 10
    ecall