#include "userprog/syscall.h"
#include <stdio.h>
#include <string.h>
#include <syscall-nr.h>
#include "threads/interrupt.h"
#include "threads/thread.h"
#include "threads/vaddr.h"
#include "filesys/filesys.h"
#include "filesys/file.h"
#include "threads/palloc.h"

static void syscall_handler (struct intr_frame *);

void
syscall_init (void)
{
  intr_register_int (0x30, 3, INTR_ON, syscall_handler, "syscall");
}

void
syscall_exit (int status)
{
  printf ("%s: exit(%d)\n", thread_current ()->name, status);
  thread_exit ();
}


/* -HW6- Support syscall sbrk to allocate memory dynamically. */
void*
syscall_sbrk (intptr_t increment)
{
  //Case 1: increment >= 0
  if (increment >= 0){
    struct thread * t = thread_current();
    void * old_heap_end = t->heap_end;
    void * cur_heap_end = t->heap_end;
    void * new_heap_end = cur_heap_end + increment;
    void * new_page_top = pg_round_up(new_heap_end);
    int count = 0;
  
    while(pg_round_up(cur_heap_end) < new_page_top){
      uint8_t *kpage;
      kpage = palloc_get_page (PAL_USER | PAL_ZERO);
      if (kpage != NULL){
        if (pagedir_get_page (t->pagedir, pg_round_up(cur_heap_end)) == NULL
              && pagedir_set_page (t->pagedir, pg_round_up(cur_heap_end), kpage, true)){
           count ++;
        }else {

          palloc_free_page (kpage);

          for (int i = 0; i < count ; i++){
            cur_heap_end -= PGSIZE;
            void *kkpage = pagedir_get_page (t->pagedir, pg_round_up(cur_heap_end));
            pagedir_clear_page (t->pagedir, pg_round_up(cur_heap_end));
            palloc_free_page (kkpage);
          }
          return (void*) -1;
        }
      }else {
        for (int i = 0; i < count ; i++){
            cur_heap_end -= PGSIZE;
            void *kkpage = pagedir_get_page (t->pagedir, pg_round_up(cur_heap_end));
            pagedir_clear_page (t->pagedir, pg_round_up(cur_heap_end));
            palloc_free_page (kkpage);
          }
        return (void*) -1;
      }
      cur_heap_end += PGSIZE;
    }
    t->heap_end = new_heap_end;
    return old_heap_end;
  }
  
  //Case 2: increment < 0
  if (increment < 0){
    struct thread * t = thread_current();
    void * old_heap_end = t->heap_end;
    void * cur_heap_end = t->heap_end;
    void * new_heap_end = cur_heap_end + increment;
    void * new_page_top = pg_round_up(new_heap_end);

    if (new_heap_end < t->heap_start){
      return (void*)-1;
    }

    while (pg_round_up(cur_heap_end) > new_page_top){
      void *kpage = pagedir_get_page (t->pagedir, pg_round_down(cur_heap_end));
      pagedir_clear_page (t->pagedir, pg_round_down(cur_heap_end));
      palloc_free_page (kpage);
      if (cur_heap_end - PGSIZE < t->heap_start){
        break;
      }
      cur_heap_end -= PGSIZE;
    }
    t->heap_end = new_heap_end;
    return old_heap_end;
  }
  
}

/*
 * This does not check that the buffer consists of only mapped pages; it merely
 * checks the buffer exists entirely below PHYS_BASE.
 */
static void
validate_buffer_in_user_region (const void* buffer, size_t length)
{
  uintptr_t delta = PHYS_BASE - buffer;
  if (!is_user_vaddr (buffer) || length > delta)
    syscall_exit (-1);
}

/*
 * This does not check that the string consists of only mapped pages; it merely
 * checks the string exists entirely below PHYS_BASE.
 */
static void
validate_string_in_user_region (const char* string)
{
  uintptr_t delta = PHYS_BASE - (const void*) string;
  if (!is_user_vaddr (string) || strnlen (string, delta) == delta)
    syscall_exit (-1);
}


static int
syscall_open (const char* filename)
{
  struct thread* t = thread_current ();
  if (t->open_file != NULL)
    return -1;

  t->open_file = filesys_open (filename);
  if (t->open_file == NULL)
    return -1;

  return 2;
}

static int
syscall_write (int fd, void* buffer, unsigned size)
{
  struct thread* t = thread_current ();
  if (fd == STDOUT_FILENO)
    {
      putbuf (buffer, size);
      return size;
    }
  else if (fd != 2 || t->open_file == NULL)
    return -1;

  return (int) file_write (t->open_file, buffer, size);
}

static int
syscall_read (int fd, void* buffer, unsigned size)
{
  struct thread* t = thread_current ();
  if (fd != 2 || t->open_file == NULL)
    return -1;

  return (int) file_read (t->open_file, buffer, size);
}

static void
syscall_close (int fd)
{
  struct thread* t = thread_current ();
  if (fd == 2 && t->open_file != NULL)
    {
      file_close (t->open_file);
      t->open_file = NULL;
    }
}

static void
syscall_handler (struct intr_frame *f)
{
  uint32_t* args = (uint32_t*) f->esp;
  struct thread* t = thread_current ();
  t->in_syscall = true;
  /* -HW6- Save the user program esp such that the page fault handler can retrieve it rather than f->esp from syscall_handler */
  t->user_esp = f->esp;

  validate_buffer_in_user_region (args, sizeof(uint32_t));
  switch (args[0])
    {
    case SYS_EXIT:
      validate_buffer_in_user_region (&args[1], sizeof(uint32_t));
      syscall_exit ((int) args[1]);
      break;

    case SYS_OPEN:
      validate_buffer_in_user_region (&args[1], sizeof(uint32_t));
      validate_string_in_user_region ((char*) args[1]);
      f->eax = (uint32_t) syscall_open ((char*) args[1]);
      break;

    case SYS_WRITE:
      validate_buffer_in_user_region (&args[1], 3 * sizeof(uint32_t));
      validate_buffer_in_user_region ((void*) args[2], (unsigned) args[3]);
      f->eax = (uint32_t) syscall_write ((int) args[1], (void*) args[2], (unsigned) args[3]);
      break;

    case SYS_READ:
      validate_buffer_in_user_region (&args[1], 3 * sizeof(uint32_t));
      validate_buffer_in_user_region ((void*) args[2], (unsigned) args[3]);
      f->eax = (uint32_t) syscall_read ((int) args[1], (void*) args[2], (unsigned) args[3]);
      break;

    case SYS_CLOSE:
      validate_buffer_in_user_region (&args[1], sizeof(uint32_t));
      syscall_close ((int) args[1]);
      break;

    /* -HW6- Support syscall sbrk to allocate memory dynamically. */
    case SYS_SBRK:
      validate_buffer_in_user_region (&args[1], sizeof(uint32_t));
      f->eax = (uint32_t) syscall_sbrk ((intptr_t) args[1]);
      break;


    default:
      printf ("Unimplemented system call: %d\n", (int) args[0]);
      break;
    }

  t->in_syscall = false;
}
