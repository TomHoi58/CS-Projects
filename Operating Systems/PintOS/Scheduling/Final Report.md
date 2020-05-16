Final Report for Project 2: Threads
===================================

## Task 1

We no longer kept track of the `min_wake_tick` like we specified in the design doc, since we might possibly wake up more than one thread. The final data structures we used are:

Defined in `timer.c` and initialzed in `timer_init()`:
 ```c
static struct list sleeping_threads_list; /*Contains struct sleeping_thread*/
```

Defined in `thread.h`:
```c
struct sleeping_thread
{
  struct thread *t;
  int64_t wakeup_time;
  struct list_elem elem;
};
```

### Implementation Details
`timer_sleep (int64_t ticks)`: Instead of using `thread_yield()`, we make a `struct sleeping_thread` with the `wakeup_time` being the current ticks plus the argument ticks, then `list_insert_ordered` it to `sleeping_threads_list` with our own compare functions `less_list` and `less_wake`. Finally we block the thread by calling `thread_block()`. To ensure the concurrecny of the list, we disable the interrupt while inserting. 

`timer_interrupt()`: We iterate through the ordered `sleeping_threads_list` and check if the `sleeping_thread`'s `wakeup_time` is equal to the current timer ticks. If so, we unblock the thread by calling `thread_unblock()`. Else, we break the iteration since the list is sorted based on `wakeup_time`. We also ensure the concurrency of the list by disabling the interrupt.

## Task 2.1

We pretty much followed what we wrote on design doc for 2.1. We wrote some compare functions:

 `less_priority_thread` and `less_list_thread`: used in `next_thread_to_run()`.

 `less_priority_synch` and `less_list_sema`: same as the ones above but with different names in `sema_up()`.
 
 `less_list_cond`: different than above since the waiters list in `cond_signal` contains semaphores instead of threads.

We then use `list_max` to run the highest-priority thread in `next_thread_to_run()`. Also, we wake up the highest-priority thread in `sema_up()` and `cond_singal()` by using `list_max` again. Finally, in order to make sure the highest-priority always get to run whenever they are set, we add `thread_yield()` in both `thread_create()` and `thread_set_priority()`.

## Task 2.2

Originally in the design doc, we were thinking to keep all the information like original priority, donors and receivers in a lock struct. It turned out we won't be able to know what priority we should set the thread to after releasing a lock when the thread has been donated by multiple donors. Therefore, we decied to keep track of `struct lock* wait_on_lock` and a list of `locks_held` in each thread. When we release a lock, we iterate through the `locks_held`list and set the thread priority to the largest donated priority out of all the locks. Unfortunately, there were some weird bugs when we add the lock to `locks_held` after acquiring it. After hours of debugging, we found out that somehow adding to the list that is owned by `thread_current()` didn't behavior properly. As a result, we scratched that approach and decided we only add to other threads' list. That is, `donors_list` when donating to others. The final data structures we used are:

Added to `struct thread` in **thread.h**:
```c
struct thread {
  ...
  struct list donors_list;            /* List of donors which have donated to this thread. */
  struct lock *wait_on_lock;          /* The lock that the thread is waiting for to acquire. */
  bool been_donated;                  /* If the thread has been priority-donated. */
  int original_priority;              /* The original priority before donations. */
  struct list_elem elem_donors_list;  /* List element for donors_list. */
  ...
};
```

New function defined in **synch.c**:
```c
void donate_priority (struct thread *from, struct thread *to)
/* Donate priority from a lock waitier to a lock holder.
   If the waiter's priority is not higher than the holder's priority, do nothing.
   If the holder has never been donated, record its original_priority.
   Update the holder's priority and add the waiter to the holder's doners_list.
   Recursively call donate_priority() on the holder's wait_on_lock if exists*/
```

### Implementation Details
`init_thread()`: We initizalie the new members of the `struct thread` in here.
`lock_acquire()`: Before `sema_down`, if the lock has a holder already, we set the thread's `wait_on_lock` to be the lock, donate its priority to the holder and update its `donors_list` by calling `donate_priority`. After `sema_down`, we clear the `wait_on_lock`. To ensure the synchronization, we disable the interrupt while donating. 
`lock_release()`: Get all the waiters for the lock, which is being released,remove them from the holder's `donors_list` if they are donors. Then update the lock holder's priority by looking at the max(original_priority,highest remaining donor's priority). After all, we run `thread_yield()` to make sure the highest-priority thread gets to run. We also disable interrupt to make sure the concurrecy of the lists. 

## Reflection
This project is relatively harder than the last one to collaborate with due to the coronavirus. We are all in different areas and even time zones, so we did not meet up and work together, which honestly make it difficult to make sure everyone is in the same page. However, we still decided that we all should work on all of the tasks instead of splitting the tasks. Mainly Soung Bae and Sanwu worked on Task 1 and passed the tests for the check point, while Houkit did Task 1 on the plane and checked with the team afterwards. We later decided to use Houkit's implementation for Task 1 becasue it's more line up to the Task 2 implementation, which the main ideas are provided by Houkit and the team all code together. For Task 3, Soung Bae and Sanwu are more good at math so they focued more on that. Overall, we still got the project done despite all these things happening. We learnt that we could do better by saying a specific meetup(Zoom) time insteading of waiting for each other to wake up and talk, but hopefully we don't have to do these any longer in the future classes. 

