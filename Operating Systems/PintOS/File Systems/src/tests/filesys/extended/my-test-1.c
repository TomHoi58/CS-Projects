/* -Proj3- Student testing. */
/* Test for improved hit rate using buffer cache.
   Randomly generialize some contents for a file, compute the cache hit rate first time we read it.
   Close the file and reopen it, read it sequentially again. Compute the hit rate and expect it to be higher. */

#include <random.h>
#include <syscall.h>
#include "tests/lib.h"
#include "tests/main.h"

#define FILE_SIZE (512*32)
static char buf_tom[FILE_SIZE];

static void
write_some_bytes (const char *file_name, int fd, const char *buf, size_t *ofs)
{
  if (*ofs < FILE_SIZE)
    {
      size_t block_size = random_ulong () % (FILE_SIZE / 8) + 1;
      size_t ret_val;
      if (block_size > FILE_SIZE - *ofs)
        block_size = FILE_SIZE - *ofs;

      ret_val = write (fd, buf + *ofs, block_size);
      if (ret_val != block_size)
        fail ("write %zu bytes at offset %zu in \"%s\" returned %zu",
              block_size, *ofs, file_name, ret_val);
      *ofs += block_size;
    }
}

void
test_main(void){
	int fd_tom;
	size_t ofs_tom = 0;

	random_init (0);
	random_bytes (buf_tom, sizeof buf_tom);

	CHECK (create ("tom", 0), "create \"tom\"");

	CHECK ((fd_tom = open ("tom")) > 1, "open \"tom\"");

	msg ("write \"tom\"");
  	while (ofs_tom < FILE_SIZE)
  	  {
  	    write_some_bytes ("tom", fd_tom, buf_tom, &ofs_tom);
  	  }

  	msg ("close \"tom\"");
  	close (fd_tom);

  	//Test really starts here
  	msg("reset cache");
  	cachereset();
  	hitreset();

  	CHECK ((fd_tom = open ("tom")) > 1, "open \"tom\"");

  	//Read the file with cold cache
  	CHECK (read (fd_tom, buf_tom, sizeof buf_tom) == sizeof buf_tom, "read \"tom\" with cold cache");

  	int cold_hit_rate = hitrate(0);

  	msg ("close \"tom\"");
  	close (fd_tom);

  	//Reopen the file and read it again with hot cache
  	hitreset();
  	CHECK ((fd_tom = open ("tom")) > 1, "open \"tom\"");

  	//Read the file with cold cache
  	CHECK (read (fd_tom, buf_tom, sizeof buf_tom) == sizeof buf_tom, "read \"tom\" with hot cache");
  	int hot_hit_rate = hitrate(0);

  	msg ("close \"tom\"");
  	close (fd_tom);

  	//Compare two hit rates
  	if (hot_hit_rate > cold_hit_rate){
  		msg ("The hit rate improves !");
  	}else{
  		msg ("The hit rate doesn't improve !");
  	}
 
}