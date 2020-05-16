Final Report for Project 1: User Programs
=========================================

## Task 1
  Instead of using the string parsing function, we used the `strtok_r()` function which enables us to tokenize a string according to delimiter. By setting the delimiter to be `" "`, we can tokenize the input argument string to get each word and modify the argument string so that each word is seperated by `/0`(null terminator).
  We didn't use a function to push arguments onto stack but use a seqeunce of pushes onto stack inside the `start_process()` function to achieve this goal.
### The general approach
  In `process_execute()`, we create the struct info `info_pt`(explained more in Task 2) which is used for **exec** and **wait** syscalls and store the argument string `fn_copy` into `info_pt`. By passing the `info_pt` into `thread_create()`, we make sure that the `start_process()` will take in the `info_pt` which contains the argument string that we want. The `space_helper()` function is helper function that turns all the double spaces, tripe spaces, or more into a single sapce. Inside of `start_process()` we first create the `args_ptrs[]` which will contain the pointer to each word of the argument on the stack. By tokenizing the argument string, we set the value of each element in `args_ptrs[]` by setting it to proper offset. Each time we obtain a new token, we use `strlen()` to get the length of the new token and add that length to the offset to adjsut the pointer of the next argument on the stack. After this is done, we decrement the stackr pointer by the size of the argument and use `memcpy()` to copy the `fn_copy` stored in `info_pt` onto the stack because we have already modified the content of the argument string by tokenizing it. Next, we decrement the pointer by the size of the argument pointers list and adjust it so that the stack is aligned. After the stack pointer is pointing to the right place, we use `memcpy()` to copy the content in `args_ptrs` which contains the pointer pointing to arguments that are residing on stack onto stack. We finish the stack set up by adding the `Argv` pointer pointing to `argv[0]` and argument counter to the stack. Finally, we decrement the stack by four bytes for the return address. By now, the argument parsing has been done.


## Task 2

Before the design doc review, we only knew that we had to validate the user arguments but had no idea which functions to call. We were also kind of lost on how to exactly implement the syscall, **exec** and **wait** in particular. 

### Validate User Arguments
Everything got clearer and clearer as we worked through the project. First, we used `is_user_vaddr()` and `pagedir_get_page()` to check for invalid pointers byte by byte. While for the pointer to buffers or strings that don’t have a size specified, we iterate through the string bytes and check it one by one until we hit NULL or not. If they are valid, we cleaned up the thread by calling `thread_exit`. For syscalls **halt** and **practice**, we did not use extra data structure since they were straightforward to implement.

### Exec
In order to have the parents and children communicate with each other, we introduce a shared data structure – `struct info` to the `struct thread`, which also benefited the syscall **wait** later. We used it to wrap around the filename that we needed to pass in to `start_process`. It also includes semaphore which we need to hold the parent until the child finishes the loading process. Finally, we also include a boolean `load_success` to let the parent know if the child successfully loads the process. If it fails, it just returns -1. If it successes, it returns the tid, which we use it as pid. 

Add to `struct thread` in thread.h:
```c
struct thread {
  ...
  struct list children;
  ...
};
```

Define in thread.h:
```c
struct info {
  /*for syscall exec*/
  char* fn_copy;
  struct semaphore * sem_pt;
  bool load_success;

  /*for syscall wait*/
  tid_t tid;
  int exit_status;
  int ref_count;
  struct semaphore * being_waited;

  struct list_elem elem;
};
```

### Wait
In order to keep track of the children that belong to the parent processes, we added a list of `children` to the `struct thread`. Because of that, we modified the `process_exec` so that it adds the child process to the list when it loads the process. We also added some elements to the shared `struct info` to support some features. The first one is `tid` which the parent assign after the child loads, so that we could identify which child to wait for. Also, we added an extra semaphore to indicate if the parent is waiting for the child. Just tried not to confuse ourselves, although we knew that we can reuse the semaphore in **exec**. Moreover, a `ref_count` (init:2 and is decremented whenever a connection is killed) is what we needed to know if the process is still needed for others or can be cleaned up. Lastly, `exit_status` is added to the struct so that the `process_wait` can access the exit code which the exit syscall or the kernel assigns. Finally, we make sure that the shared data can be modified at the same time (synchronization) by using a global lock, which is acquired whenever we are fixing the shared `struct info`.

```c
static struct lock info_lock;

```

### Free Resources 
In `process_exit` and `process_wait`, we look at the reference count and see if it is 0. If so, we free the pointers inside shared struct and the struct itself including those file descriptor struct in task 3. 


## Task 3

For each thread to be able to make system calls to the filesystem, we created `struct fd_` which contains information needed to call functions in filesys.h and file.h. Each thread will thus have a `struct list fd_list` within `struct thread` so that we can iterate through the list of file descriptors whenever a new syscall is called by the user program on a file.


Define in thread.h:
```c
struct fd_ {
  int fd;
  struct file* file;
  struct list_elem elem;
};
```

Add to `struct thread` in thread.h:
```c
struct thread {
  ...
  struct list fd_list;
  int counter;
  ...
};
```

The (int) fd member in the struct above corresponds to the file descriptor number(which is allocated according to the `counter`) that the user program will receive from open() such that the user will be able to make subsequent filesystem calls for the same file. However, since the functions in filesys.h and file.h take in pointers to `struct file`, we must have to struct defined above to keep them together.

When a user program makes a filesystem syscall, we are able to get the corresponding `struct file*` by iterating through the list pointer added in to `struct thread`. Using the linked list implementation given by list.h, we file the corresponding `struct fd_struct` by comparing the fd given by the user program to the fd in each of the fd_structs.

In addtion, we have a global filesystem lock defined within syscall.c. This lock is used to put a global lock on the filesystem calls since only one thread should be in a filesystem call at a time. Thus, we acquire and release this lock within every filesystem syscall.

```c
struct lock filesys_lock;

```

### Implementation Details
For the following syscalls, assume pointers have already been validated as in for syscalls in Task 2, and that each call will acquire/release the filesys_lock appropriately.

* create(): Call filesys_create() with args[1] being the file_name and argss[2] being size
* remove(): Call filesys_remove() with args[1] being the file_name. Note that we do not have to mess with fd_list since remove does not    change anything regarding file descriptors
* open(): Call filesys_open() with args[1] being the file_name. If it returns NULL, return -1. Otherwise, allocate memory for a new `fd_struct` and add it to the fd_list of the current thread. Return the assigned fd to the user process for late use in other filesystem syscalls.
* filesize(): Iterate through fd_list of the current thread in order to find the corresponding `struct fd_` which will give us the required `struct file*` to use in file_length() in file.c.
* read(): Given the fd through args[1], iterate through fd_list of the current struct to find the corresponding pointer and call file_read(). If fd is 0, call input_getc().
* write(): Iterate through fd_list to find the corresponding pointer as above. Then call file_write() or putbuf() if fd is 1.
* seek(): Get the corresponding pointer and call file_seek().
* tell(): Get the corresponding pointer and call file_tell().
* close(): Get the corresponding pointer and call file_close(). Them remove the corresponding `struct fd_` in the current thread's `fd_list`. Free the struct.

## Reflection
  To be honest, we work together on all the task so that there is not clear difference on each group member work. The only difference is probably the each group member provides idea respect to different task. For task1, Sanwu proposed that we should use the `if_.esp` and `memcpy` to access and set up the stack. Then we work together to come up with the method that we are going to set up the `argv_ptr` inside the loop of the tokenization of the argument string and figure out how to properly setup different layers of the stack. For task2, we all work together and figure out that we should use some struct to hold the semaphore in order to communicate between the parent and the child. For **wait**, specifically, Houkit incoporates the `ref_ count`, `exit_status`, and `being_waited` semaphore into the `struct info` that we are having to make sure **wait** is working. We work and debug together for **exec** syscall. For task3, Soung Bae comes up with `struct fd_` which is a list that holds the information about file descripter and specific file struct. This is basically like a mapping from `int fd` to `file* file`. Houkit designs the `validate_pointer()` function which can validate the argument that been passed in. Then we all work on each syscall together because it's all about finding the correct function inside filesys.c and file.c and correctly update the `file_list` that we are holding.

  Our group dynamics went pretty good. We can share our idea and work together to solve each task efficiently. However, the thing that we really need to improve on is design doc. Many problems that we are having is because we didn't spend enough time to complete the design doc. Therefore, for the next project, we should spend a lot of time to complete the desgin doc in order to have a clear image of the project.
