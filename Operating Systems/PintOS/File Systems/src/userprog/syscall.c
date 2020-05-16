#include "userprog/syscall.h"
#include <stdio.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "userprog/pagedir.h"
#include "threads/vaddr.h"


#include "filesys/file.h"
#include "filesys/filesys.h"
#include "lib/kernel/console.h"
#include "devices/input.h"
/* -Proj3- Student testing. */
#include "filesys/cache.h"

// struct lock filesys_lock;


static void syscall_handler (struct intr_frame *);
struct file *get_file(int fd);
void validate_pointer(void* p,int size);
void validate_string(void* p_);
struct list_elem *get_file_elem(int fd);

void
syscall_init (void)
{
  // lock_init(&filesys_lock);
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

static void
syscall_handler (struct intr_frame *f UNUSED)
{

  uint32_t* args = ((uint32_t*) f->esp);


  //validate the stack pointer and the args[0], which is the syscall number
  validate_pointer(f->esp, 4);

  /*
   * The following print statement, if uncommented, will print out the syscall
   * number whenever a process enters a system call. You might find it useful
   * when debugging. It will cause tests to fail, however, so you should not
   * include it in your final submission.
   */
  int fd;
  struct file *file;
  char *buffer;
  char *file_name;
  char *executable;
  size_t size;
  struct fd_ *fd_struct;
  int count;
  struct list_elem *file_elem;

  struct status *status;

  /* -Proj3- Support directories. */
  struct dir *dir;
  struct dir *dir_2;
  char filename[NAME_MAX +1];
  struct inode *inode;

  switch(args[0]) {
    // Task 2: Syscalls
    case SYS_EXIT:
      validate_pointer(f->esp, 8);
      //check if my info_pt is null beceaue the very first process has no info_pt
      if (thread_current()->info_pt != NULL){
        thread_current()->info_pt->exit_status = args[1];
      }
      f->eax = args[1];
      printf ("%s: exit(%d)\n", &thread_current ()->name, args[1]);
      thread_exit ();
      break;

    case SYS_HALT:
      shutdown_power_off();
      break;

    case SYS_EXEC:
      //validate the args first
      validate_pointer(f->esp, 8);
      //validate the *cmd_line
      //validate_pointer(args[1],1);
      //validate the entire cmd_line
      validate_string(args[1]);

      //return the pid (-1 if fails)
      //the args[1] is stored in static, need to copy to heap (remember to free the pointer after using)
      executable = malloc(sizeof(char)*strlen(args[1])+1);
      memcpy(executable,args[1],strlen(args[1])+1);
      f->eax = process_execute(executable);
      break;

    case SYS_WAIT:
      //validate the args first
      validate_pointer(f->esp, 8);
      f->eax = process_wait(args[1]);

      break;

    case SYS_PRACTICE:
      validate_pointer(f->esp, 8);
      f->eax = args[1]+1;
      break;

    // TASK 3: FILE SYSCALLS
    case SYS_CREATE:
      //validate the args fisrt
      validate_pointer(f->esp,12);
      //validate the file char*
      validate_string(args[1]);

      // lock_acquire(&filesys_lock);
      dir = dir_getter(args[1], filename);
      size = args[2];
      f->eax = filesys_create(dir, filename, size, false);
      dir_close(dir);
      // lock_release(&filesys_lock);
      break;

    case SYS_REMOVE:
      validate_pointer(f->esp, 8);
      validate_string(args[1]);

      // lock_acquire(&filesys_lock);
      dir = dir_getter(args[1], filename);
      f->eax = filesys_remove(dir, filename);
      dir_close(dir);
      // lock_release(&filesys_lock);

      break;

    case SYS_OPEN:
      validate_pointer(f->esp, 8);
      validate_string(args[1]);

      // lock_acquire(&filesys_lock);
      dir = dir_getter((char *)args[1],filename);
      if (!dir){
        f->eax = -1;
      }else {
        if (dir_lookup(dir,filename,&inode)){
          if (inode_dir(inode)){
            file = filesys_open(dir, filename,true);
          }else {
            file = filesys_open(dir, filename,false);
          }
          if (file == NULL) {
            // lock_release(&filesys_lock);
            f->eax = -1;
          } else {
            fd_struct = malloc(sizeof(struct fd_));
            fd_struct->fd = thread_current()->counter;
            f->eax = fd_struct->fd;
            thread_current()->counter ++;
            fd_struct->file = file;
            list_push_back(&thread_current()->fd_list, &fd_struct->elem);
            // lock_release(&filesys_lock);
          }
        }else {
          f->eax = -1;
        }
        dir_close(dir);
      }
      break;

    case SYS_FILESIZE:
      validate_pointer(f->esp, 8);
      fd = args[1];

      // lock_acquire(&filesys_lock);
      file = get_file(fd);

      if (file == NULL){
        f->eax = 0;
      } else {
        f->eax = file_length(file);
      }
      // lock_release(&filesys_lock);
      break;

    case SYS_READ:
      // //validate the args first
      validate_pointer(f->esp, 16);
      // //validate the buffer with the size
      validate_pointer(args[2], args[3]);

      fd = args[1];
      buffer = args[2];
      size = args[3];
      //case 1: write into console
      if(fd == 0){
        // lock_acquire(&filesys_lock);
        for (count = 0; count < size; count++) {
          *((uint8_t*) buffer + count) = input_getc();
        }
        f->eax = size;
        // lock_release(&filesys_lock);
      }else {
        //case 2: write into given file
        //get the file pointer
        // lock_acquire(&filesys_lock);

        file = get_file(fd);

        //if no corresponding file found
        if (file == NULL){
          f->eax = -1;
          // lock_release(&filesys_lock);
        }else{
          if (inode_dir (file->inode)){
            f->eax = -1;
          }else {
            f->eax = file_read(file,buffer,size);
          }
          // lock_release(&filesys_lock);
        }
      }
      break;

    case SYS_WRITE:
      // //validate the args fisrt
      validate_pointer(f->esp, 16);
      // //validate the buffer with the size
      validate_pointer(args[2], args[3]);

      fd = args[1];
      buffer = args[2];
      size = args[3];
      //case 1: write into console
      if(fd == 1){
        // lock_acquire(&filesys_lock);
        putbuf(buffer,size);
        f->eax = size;
        // lock_release(&filesys_lock);
      }else {
        //case 2: write into given file
        //get the file pointer
        // lock_acquire(&filesys_lock);

        file = get_file(fd);

        //if no corresponding file found
        if (file == NULL){
          f->eax = -1;
          // lock_release(&filesys_lock);
        }else{
          if (inode_dir (file->inode)){
            f->eax = -1;
          }else {
            f->eax = file_write(file,buffer,size);
          }
          // lock_release(&filesys_lock);
        }
      }
      break;

    case SYS_TELL:
      validate_pointer(f->esp, 8);

      // lock_acquire(&filesys_lock);

      file = get_file(args[1]);
      f->eax = file_tell(file);
      // lock_release(&filesys_lock);
      break;

    case SYS_SEEK:
      validate_pointer(f->esp, 12);
      fd = args[1];
      size = args[2];
      // lock_acquire(&filesys_lock);
      file = get_file(fd);
      file_seek(file, size);
      f->eax = NULL;
      // lock_release(&filesys_lock);
      break;

    case SYS_CLOSE:
      validate_pointer(f->esp, 8);
      // lock_acquire(&filesys_lock);
      fd = args[1];

      file_elem = get_file_elem(fd);
      if (file_elem == NULL) {
        f->eax = -1;
        // lock_release(&filesys_lock);
      } else {
        file  = list_entry(file_elem, struct fd_, elem)->file;
        file_close(file);
        struct fd_ * temp = list_entry(file_elem, struct fd_, elem);
        list_remove(file_elem);
        free(temp);
        // lock_release(&filesys_lock);
      }
      break;

    /* -Proj3- Support directories. */
    case SYS_CHDIR:
      validate_pointer(f->esp, 8);
      dir = dir_getter((char *)args[1],filename);
      if (dir){
        dir_2 = (struct dir *) filesys_open(dir, filename, true);
        if (dir_2){
          dir_close (thread_current()->cwd);
          thread_current()->cwd = dir_reopen(dir_2);
          dir_close(dir_2);
          f->eax = true;
        }else {
          f->eax = false;
        }
      }else {
        f->eax = false;
      }
      dir_close (dir);
      break;

    case SYS_MKDIR:
      validate_pointer(f->esp, 8);
      dir = dir_getter((char *)args[1],filename);
      if (dir){
        if (filesys_create(dir,filename,16, true)){
          dir_2 = (struct dir *)filesys_open(dir, filename, true);
          //Add "." and ".."
          support_myself_and_parent(dir,dir_2);
          dir_close(dir_2);
        }else {
          f->eax = false;
        }
      }else{
        f->eax = false;
      }
      dir_close(dir);

      break;

    case SYS_READDIR:
      validate_pointer(f->esp, 12);
      fd = args[1];
      file = get_file(fd);
      if (file){
        inode = file_get_inode(file);
        if ( inode && inode_dir(inode)){
          dir = (struct dir *) get_file(fd);
          f->eax = dir_readdir(dir,args[2]);
        }else {
          f->eax = false;
        }
      }else {
        f->eax = false;
      }

      break;

    case SYS_ISDIR:
      validate_pointer(f->esp, 8);
      fd = args[1];
      file = get_file(fd);
      if (file){
        f->eax = inode_dir(file_get_inode(file));
      }else {
        f->eax = -1;
      }
      break;

    case SYS_INUMBER:
      validate_pointer(f->esp, 8);
      fd = args[1];
      file = get_file(fd);
      if (file){
        f->eax = file_get_inode(file)->sector;
      }else {
        f->eax = -1;
      }
      break;

    /* -Proj3- Student testing. */
    case SYS_CACHERESET:
      cache_reset();
      break;

    case SYS_HITRESET:
      hit_reset();
      break;

    case SYS_HITRATE:
      validate_pointer(f->esp, 8);
      f->eax = get_hit_rate(args[1]);
      break;

    case SYS_WRITECOUNT:
      validate_pointer(f->esp, 8);
      f->eax = get_write_cnt(fs_device);
      break;

    case SYS_READCOUNT:
      validate_pointer(f->esp, 8);
      f->eax = get_read_cnt(fs_device);
      break;

    default:
      break;

  }

}

/* p is already validated */
void validate_string(void *p_) {
  char *p = p_;
  size_t i = 0;
  uint32_t *pd = thread_current()->pagedir;

  while (is_user_vaddr(p+i) && pagedir_get_page(pd, p+i) != NULL) {
    if (*(p+i) == '\0') {
      // valid
      return;
    }
    i++;
  }
  // invalid
  exit_thread();
}

  /*
   * Validate a pointer with its size(in bytes)
   * p: ptr
   * size: byte length (ie. int == 4)
   */

void
validate_pointer(void* p,int size){
  for (int i = 0; i<size; i++){
    if (!is_user_vaddr(p+i) || pagedir_get_page(thread_current()->pagedir,p+i)== NULL) {
      exit_thread();
    }
  }
}

void exit_thread() {
  printf ("%s: exit(%d)\n", &thread_current ()->name, -1);
    // pagedir_destroy(thread_current()->pagedir);
    //also need to release the lock
  if (thread_current()->info_pt != NULL){
    thread_current()->info_pt->exit_status = -1;
  }
  thread_exit ();
}

struct file *get_file(int fd) {
  struct file * fp = NULL;
  struct fd_ *fd_struct;
  struct list_elem *e;


  for (e = list_begin(&thread_current()->fd_list); e != list_end(&thread_current()->fd_list); e = list_next(e)) {
    fd_struct = list_entry(e,struct fd_, elem);
    if (fd_struct->fd == fd) {
      fp = fd_struct->file;
      break;
    }
  }

  return fp;
}

struct list_elem *get_file_elem(int fd) {
  struct fd_ *fd_struct;
  struct list_elem *e;


  for (e = list_begin(&thread_current()->fd_list); e != list_end(&thread_current()->fd_list); e = list_next(e)) {
    fd_struct = list_entry(e,struct fd_, elem);
    if (fd_struct->fd == fd) {
      return e;
    }
  }

  return NULL;
}
