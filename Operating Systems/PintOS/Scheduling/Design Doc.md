Design Document for Project 2: Threads
======================================

## Group Members

* Soung Bae Kim <sbkim@berkeley.edu>
* Houkit Hoi <tomhoi@berkeley.edu>
* Sanwu Luo <sanwuluo35@berkeley.edu>

## Task 1: Efficient Timer

### Data Structures & Functions
All of the following will be placed in **thread.h**:

First, we will add a *global* list of all sleeping threads:

```c
static struct list sleeping_list;
static long long min_wake_tick;
```

In addition, we define a new member in `struct thread`. This will allow us to keep record of when the thread should wake up:

```c
struct thread {
  ...
  static long long wake_tick;
  ...
}
```

Next, we define two functions that will be called in **timer.c**:
```c
/* Wakes up threads that are sleeping and have a wake_tick < tick
 * Called by timer_sleep()
 */
void thread_wake(long long tick);

/* Puts the current thread to sleep and sets its wake_tick to given wake_tick
 * Called by timer_interrupt()
 */
void thread_sleep(long long wake_tick);

/*  Returns true if wake_tick of A is less than the wake_tick of B
 *  Used for methods that sort list in list.h
 */
bool wake_tick_less_function (const struct list_elem *a,
  const struct list_elem *b, void *aux)
```

### Algorithms
There are two main parts to this task: **1** Putting a thread to sleep **2** Waking up threads at the right time

#### 1. Putting threads to sleep
When a thread wants to be put to sleep for a certain amount of time, it will get to `timer_sleep()` through the other `timer_xsleep()` calls where the *x* denotes the unit of time.

In order to avoid busy waiting, we will call our function `thread_sleep()` instead of the current implementation that uses `thread_yield()`.

`thread_sleep()` takes in the wake_tick as its first argument which means that we must calculate it by simply adding the **global** `ticks` variable to the `ticks` variable passed into `timer_sleep()` in **timer.c**.

Once in `thread_sleep()`, we will set the `wake_tick` member of the current thread's `struct thread` to the given `wake_tick`. Then, the thread will be added to the **global** `sleeping_list` defined above. Then, the **global** `min_wake_tick` is updated by calling `list_min()` in `list.h` using `wake_tick_less_function()`. We then block the thread by calling `thread_block()` after interrupts have been turned off.

#### 2. Waking up threads
The process of waking up sleeping threads starts every time `timer_interrupt()` is called. In this call, we will call our function `thread_wake()` much like how `thread_tick()` is being called.

`thread_wake()` will be response before looking through the **global** `sleeping_list` and unblock any threads that have `wake_tick` thats less than the `tick` variable passed in.

If the **global** `min_wake_tick` is less than the given `tick`, then we know that there is at least one thread to be awoken. We find these threads by using `wake_tick_less_function()` as we sort the sleeping list to constantly get the thread with the minimum `wake_tick` until it is greater than the current `tick`. All threads with such a `wake_tick` value will be unblocked through `thread_unblock()`.

### Synchronization
When waking up sleeping threads, we do not have to think about any synchronization primitives since `timer_interrupt()`, and consequently `thread_wake()`, is being called with interrupts disabled since its within a syscall handler.

When putting threads to sleep, we must make sure that the **global** `sleeping_list` is not mutated by other threads as we add the current thread to the list. This can be done by disable interrupts. Once we are done unblocking the correct threads (done with `sleeping_list`), we reenable interrupts.

### Rationale
In the spec we were told that `timer_interrupt()` should be kept short since it will be called every time it the clock "ticks". This design saves some time within the function by having `thread_sleep()` do some preprocessing of the minimum `wake_tick`. This saves us from iterating through the entire list of sleeping threads every time `timer_interrupt()` is called.

## Task 2: Priority Scheduler

### 1. Data structures and functions

#### Run higher priority thread
In order to run the scheduler based on the higher-priority value that the threads have. We will modify `next_thread_to_run` so that it doesn't pop the first thread but the max priority thread from the `ready_list`. We will write a `less_compare_function` just like we did in homework for wordcount. This function compares the priority of each thread and will be passed in to list_max.

#### Priority donation
We add `int original_priority` to `sturct lock` to record the priority before being donated. We add some lines to `lock_acquire` and `lock_release` functions mainly to set the temporary priority.
For each thread that is trying to acquire the lock. We check if the `lock->holder` has lower priority than the current thread. If so we make `original_priority = holder.get_priority` then set the holder's priority to be current thread priority by using `set_priority()` and `get_priority()`.

For lock releasing, we set the holder's priority back to its `orignal_priority`.

### 2. Algorithms

##### Run higher priority thread
`list_max` works because it gives the scheduler the highest priority value thread instead of random thread that comes off from the list head.

#### Priority donation
The lock can be acquired and released by differnt sources so it makes sence that it could be donated from multiple sources. While releasing the lock would undo the priority since we had a variable `original_priority`. Also, one thread can has multiple layers of locks so it would be donated priorities couple times without errors since `lock_acquire` and `lock_release` are always called in pairs.

### 3. Synchronization

To support sychronization structures. We modify them as follows.

a. `sema_up` : Instead of unblocking the thread (waiter) that `list_pop_front` returns. We unblock the thread that has highest priotity by using `list_max` on waiters.

b. `lock_release`: this is implemented based on sema_up, so we don't have to do extra work.

c. `cond_sina` : We basically use `list_max` instead of `list_pop_front` again to sinal the highest priority one.

### 4. Rationale

First, we thought of sorting the ready_list for scheduler every time we modify the list. But it seems like getting the max out of it would save us some time since we are only going throught the entire list when we need to get some element out of it.

We were also planning to do the priority donation based on each struct thread, but it didn't work well for recursive donation.

## Additional Questions
### Question 1
The program counter, stack pointer, and registers are all stored on the old thread's stack in `switch.S`. Lines 26-29 push register values including the program counter (%eip) and lines 40-41 push the current stack pointer to the stack using the instruction `pushl` and `movl`.

### Question 2
The page containing its stack and TCB is freed inside the `thread_schedule_tail()` at the end of `schedule()` function. We can't free this memory by freeing it inside of `thread_exit()` function because we need the information stored in the thread to do context switch inside of `schedule()` function.

### Question 3
`thread_tick()` function is called by `timer_interrupt()` in **timer.c**. `timer_interrupt()` is called by a syscall handler which means that we are in the *init thread*'s (given by **global** `initial_thread` in **thread.c**) stack when we're in `thread_tick()`.

### Question 4
The following example will be the correct test case. Because the `sema_up()` unblocks the thread with the highest base priority but not effective priority.
```c
Thread A (priority 1) {
   acquire Lock 1
   create Thread B
   create Thread C
   create Thread D
   release Lock 1
}

Thread B (priority 2) {
  acquire Lock 2
  acquire Lock 1
  release Lock 2
  release Lock 1
}

Thread C (priority 3) {
  acquire Lock 1
  release Lock 1
}

Thread D (priority 4) {
  acquire Lock 2
  release Lock 2
}
```
After create Thread D in Thread A, we now have the following priority table:
Thread A {Base Priority: 1; Effective Priority: 4}
Thread B {Base Priority: 2; Effective Priority: 4}
Thread C {Base Priority: 3; Effective Priority: 3}
Thread D {Base Priority: 4; Effective Priority: 4}

If `sema_up()` is working correctly, `Thread B` should be unblocked when the lock in `Thread A` is released. Because in this situation, `Thread B` has a higher effective priority 4 than `Thread C` with priority 3. Therefore, the correct order should be "ABDC".
However, if `sema_up()` is implemented according to base priorities, `Thread C` can be unblocked with its higher base priority 3 than priority 2. Therefore, "ACBD" will be the order.