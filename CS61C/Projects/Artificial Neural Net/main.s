.import read_matrix.s
.import write_matrix.s
.import matmul.s
.import dot.s
.import relu.s
.import argmax.s
.import utils.s

.globl main

.text
main:
    # =====================================
    # COMMAND LINE ARGUMENTS
    # =====================================
    # Args:
    #   a0: int argc
    #   a1: char** argv
    #
    # Usage:
    #   main.s <M0_PATH> <M1_PATH> <INPUT_PATH> <OUTPUT_PATH>

    # Exit if incorrect number of command line args

    #set parameters to registers
    mv s0 a1

    #call exit_code_3
    li t0 5
    bne a0 t0 exit_code_3


	# =====================================
    # LOAD MATRICES
    # =====================================






    # Load pretrained m0

    #initialize two pointers to number of rows and columns
    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s2 a0 #set the pointer to number of rows

    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s3 a0 #set the pointer to number of rows

    #call read_matrix
    lw a0 4(s0) #pointer to m0 path filename
    mv a1 s2 #pointer to number of rows
    mv a2 s3 #pointer to number of columns
    jal read_matrix

    mv s1 a0 #pointer to m0 matrix


    # Load pretrained m1

    #initialize two pointers to number of rows and columns
    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s5 a0 #set the pointer to number of rows

    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s6 a0 #set the pointer to number of rows

    #call read_matrix
    lw a0 8(s0) #pointer to m1 path filename
    mv a1 s5 #pointer to number of rows
    mv a2 s6 #pointer to number of columns
    jal read_matrix

    mv s4 a0 #pointer to m1 matrix


    # Load input matrix

    #initialize two pointers to number of rows and columns
    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s8 a0 #set the pointer to number of rows

    li a0 4 #allocate 4 bytes for integer pointer
    jal malloc
    mv s9 a0 #set the pointer to number of rows

    #call read_matrix
    lw a0 12(s0) #pointer to input path filename
    mv a1 s8 #pointer to number of rows
    mv a2 s9 #pointer to number of columns
    jal read_matrix

    mv s7 a0 #pointer to input matrix





    # =====================================
    # RUN LAYERS
    # =====================================
    # 1. LINEAR LAYER:    m0 * input
    # 2. NONLINEAR LAYER: ReLU(m0 * input)
    # 3. LINEAR LAYER:    m1 * ReLU(m0 * input)

    #initialize pointer to matrix m0 * input
    lw t1 0(s2) #number of rows of m0
    lw t2 0(s9) #number of columns of input
    mul t1 t1 t2 #number of elements in m0 * input
    slli a0 t1 2 #number of elements in m0 * input * 4
    jal malloc
    mv s10 a0 #pointer to matrix m0 * input

    #call matmul m0 * input
    mv a0 s1 #pointer to the start of m0
    lw a1 0(s2) #number of rows of m0
    lw a2 0(s3) #number of columns of m0
    mv a3 s7 #pointer to the input
    lw a4 0(s8) #number of rows of input
    lw a5 0(s9) #number of columns of input
    mv a6 s10 #pointer to matrix m0 * input
    jal matmul

    #call relu
    lw t1 0(s2) #number of rows of m0
    lw t2 0(s9) #number of columns of input
    mv a0 s10 #pointer to matrix m0 * input
    mul a1 t1 t2 #number of elements in m0 * input
    jal relu

    #initialize pointer to matrix m1 * ReLU(m0 * input)
    lw t1 0(s5) #number of rows of m1
    lw t2 0(s9) #number of columns of ReLU(m0 * input)
    mul t1 t1 t2 #number of elements in m0 * input
    slli a0 t1 2 #number of elements in m0 * input *4
    jal malloc
    mv s11 a0 #pointer to matrix m1 * ReLU(m0 * input)

    #call matmul m1 * ReLU(m0 * input)
    mv a0 s4 #pointer to the start of m1
    lw a1 0(s5) #number of rows of m1
    lw a2 0(s6) #number of columns of m1
    mv a3 s10 #pointer to the ReLU(m0 * input)
    lw a4 0(s2) #number of rows of ReLU(m0 * input)
    lw a5 0(s9) #number of columns of ReLU(m0 * input)
    mv a6 s11 #pointer to m1 * ReLU(m0 * input)
    jal matmul




    # =====================================
    # WRITE OUTPUT
    # =====================================
    # Write output matrix
    lw a0 16(s0) # Load pointer to output filename
    mv a1 s11 #pointer to m1 * ReLU(m0 * input)
    lw a2 0(s5) #number of rows in the matrix
    lw a3 0(s9) #number of columns in the matrix
    jal write_matrix



    # =====================================
    # CALCULATE CLASSIFICATION/LABEL
    # =====================================
    # Call argmax
    lw t1 0(s5) #number of rows of m1
    lw t2 0(s9) #number of columns of input
    mv a0 s11 #pointer to matrix m1 * ReLU(m0 * input)
    mul a1 t1 t2 #number of elements in matrix m1 * ReLU(m0 * input)
    jal argmax



    # Print classification
    mv a1 a0 #index to the largest value
    jal print_int


    # Print newline afterwards for clarity
    li a1 '\n'
    jal print_char

    jal exit

exit_code_3:
    li a1 3
    jal exit2
