#include <stdlib.h>
#include <stdio.h>
#include <string.h>

/* -HW6 Imported from */
typedef struct metadata Metadata;

struct metadata
{
	size_t size;
	bool free;
	Metadata *next;
	Metadata *prev;
	char block[0];
};

//Address of the start of the heap
Metadata * base;


void*
malloc (size_t size)
{
  //TODO: Implement malloc
  
  //Requested size is 0
  if (size == 0){
  	return NULL;
  }

  if (base == NULL){
  	//First time calling malloc
  	Metadata *m = sbrk(sizeof(Metadata)+size);
  	//Error Checking
  	if ((void *)m == (void *)-1){
  		return NULL;
  	}
  	base = m;

  	m->size = size;
  	m->free = false;
  	m->next = NULL;
  	m->prev = NULL;
  	//Zero out the allocated memory
  	memset(m->block,0,size);
  	
  	return (void*)m->block;
  }else {
  	//We have linked list to walk through
  	Metadata *m = base;

  	while (m != NULL){
  		if (m->free && m->size > size + sizeof(Metadata)){
  			//Large enough for a new block

  			//New block
  			Metadata *n = (Metadata*) (m->block +size);
  			n->size = m->size - size - sizeof(Metadata);
  			n->free = true;
  			n->prev = m;
  			n->next = m->next;
  			if (n->next != NULL){
  				n->next->prev= n;
  			}

  			//Modify the old block
  			m->free = false;
  			m->size = size;
  			m->next = n;

  			//Zero out the allocated memory
  			memset(m->block,0,size);
  			
  			return (void*)m->block;

  		}else if (m->free && m->size >= size){
  			//A bit larger 
  			m->free = false;
  			//Zero out the allocated memory
  			memset(m->block,0,size);
  			
  			return (void*)m->block;

  		}else{
  			if (m-> next == NULL){
  				break;
  			}
  			m = m->next;
  		}
  	}
  	//Create new space on the heap
  	Metadata *n = sbrk(sizeof(Metadata)+size);
  	//Error Checking
  	if ((void *)n == (void *)-1){
  		return NULL;
  	}
  	n->size = size;
  	n->free = false;
  	n->next = NULL;
  	n->prev = m;

  	m->next = n;
  	//Zero out the allocated memory
  	memset(n->block,0,size);
  	
  	return (void*)n->block;
  }
}

void free (void* ptr)
{
  //TODO: Implement free
  //NULL pointer
  if (ptr == NULL){
  	return;
  }

  //Get the Medatata of ptr
  Metadata *m = (Metadata *)(ptr - sizeof(Metadata));

  Metadata *p = m->prev;
  Metadata *n = m->next;

  m->free = true;

  //Merge with the prev block if any
  if (p!= NULL && p->free){
  	p->size += sizeof(Metadata) + m->size;
  	p->next = n;

  	if (n !=NULL){
  		n->prev = p;
  	}
  	m = p;
  }

  //Merge with the next block if any 
  if (n!= NULL && n->free){
  	m->size += sizeof(Metadata) + n->size;
  	m->next = n->next;

  	if (m->next !=NULL){
  		m->next->prev = m;
  	}
  }

  return NULL;
}

void* calloc (size_t nmemb, size_t size)
{
  /* Homework 5, Part B: YOUR CODE HERE */
  size_t total = nmemb* size;
  return malloc(total);
}

void* realloc (void* ptr, size_t size)
{
  //TODO: Implement realloc

  if (size == 0){
  	free(ptr);
  	return NULL;
  }
  
  //Allocate the new space
  void* new = malloc(size);
  //Error checking
  if (new == NULL){
  	return NULL;}

  if (ptr == NULL){
  	return new;
  }

  Metadata *p = (Metadata *)(ptr - sizeof(Metadata));

  if (p->size <= size){
  	//Allocating bigger chunk
  	memcpy(new,ptr,p->size);
  }else {
  	//Allocating smaller chunk
  	memcpy(new,ptr,size);
  }

  //Free old chunk
  free(ptr);

  return new;
}
