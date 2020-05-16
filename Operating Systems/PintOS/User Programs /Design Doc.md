Design Document for Project 1: User Programs
============================================

## Group Members

* Houkit Hoi <tomhoi@berkeley.edu>
* Soung Bae Lim  <sbkim@berkeley.edu>
* SanWu Luo  <sanwuluo35@berkeley.edu>
* Yi Tian <lorna0728@berkeley.edu>

Replace this text with your design document.

## Task 1: Passing Arguments
### Data Structures & Functions:
char *parse_args(char *filename): Parses “filename” which contains arguments in addition to the filename. Returns an array of strings of the arguments.

void push_arg(char *arg): Pushes and argument pointer onto the stack for the user program.

### Algorithm:
First, the string passed into process_execute contains not just the filename but the arguments for the process. Therefore, we must parse through “filename” and separate the arguments according to spaces.

In order for the user program to access the arguments, they must be pushed onto the stack according to the 80x86 Calling Convention. Thus, we push the arguments up to the stack during start_process since we have access to the intr_frame “if_” which allows us to access the stack pointer.

We will handle potential null pointers and invalid addresses by checking the arguments passed through the variable “filename” before we push them on the stack.

### Synchronization:
Since this task is mainly focused on manipulation of the stack to pass arguments through to the kernel, it does not require any design in synchronization.

### Rationale: 
Our method of parsing through the “filename” variable and push the arguments onto the stack is the simplest way of going about this task. The simple approach allows for a clean and methodical error handling.

## Task 2: Process Control Syscalls
### Data Structures & Functions:
int get_syscall(uint_t *args): Returns the correct syscall for syscall_handler

### Algorithm:
In order to safely read from the user stack, we must first check to see that arugments passed for the syscalls are valid and not null pointers or invalid pointers. We will do this by comparing the vituraly address of the arugment with PHYS_BASE. If it is in the kernel space, we will stop the thread and exit. 
However, if it is within the user space, we will pass it along to the the kernel syscall lib. There, if the pointer was invalid in the user space, then a page fault will occur which will be handled accordingly.
### Synchronization:
Calling syscalls will require the system to go into kernel mode which implies a lot of resources being moved around. This requires implementation of control blocks that allow us to keep track of where whe are in each of the individual threads while moving back and forth.
### Rationale:
 This method is a much cleaner approach compared to the complex error checking needed to be done for the alternative method. In addition, it is the way chosen by the professionals in the industry.

## Task 3: File Operation Syscalls
### Data Structure & Function:
struct file
  {
    struct inode *inode;        /* File's inode. */
    off_t pos;                  /* Current position. */
    bool deny_write;            /* Has file_deny_write() been called? */
  };

file_handler(struct file *f, function)
access file system functions to read, open, remove, etc.

file_management(struct file *f, struct intr_frame *frame)
store the file data in the static segment for user program

bool global_lock(struct file *f)
make sure that file that's being access is in a critical section and all the other process will be wait until this operation is finished. Return true if the lock is sueccessful, return false if there is an error.
### Algorithm:
In syscall_handler, we can access the requested of a program by looking into the data stored in the intr_frame *f passed in and create a file struct to call the function in filesys.c and file.c. 
We use the data we get from calling functino in filesys.c and file.c and store it in the memory by accessing the frame.

### Synchronization:
Every time when we try to access the file system, we use the global_lock to make sure that the each file operation is a single critical section and no other file system call can be made.

### Rationale:
This is the simplest way to communicate data between a single process and file system. By passing file struct around, we can easily store the data and call the neccessary function that we need to use to achieve the file system call.

## Additional Questions

1. sc-bad-sp 

on line 18, it trys to assign the address of esp to be below the code segment by movl $.-(64*1024*1024), %esp, and then it invokes a system call by using int $030. The expected result should be a terminated process with -1 exit code. 

2.sc-boundary

on line 14, it makes the address of p = the boundary address then it decrements it to be a bad address. on line 16 and 17, it stores two arguments on the bad adress. Finally, on line 20, it makes the stack pointer to be at the boundary address and have the system call with the passing arguements. the expected result is a termineated program with exit code 42.

3.remove files

We don't see the test case for remove files systems. In order to test this, we have to create a file. e.g. hey.dat. Then we can add a test_main including CHECK(remove("hey.dat"),"remove hey.dat")
