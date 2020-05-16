/*********************
**  Color Map generator
** Skeleton by Justin Yokota
**********************/

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <math.h>
#include <string.h>
#include "ColorMapInput.h"


/**************
**This function reads in a file name colorfile.
**It then uses the information in colorfile to create a color array, with each color represented by an int[3].
***************/
uint8_t** FileToColorMap(char* colorfile, int* colorcount)
{
	FILE* fptr = fopen(colorfile,"r");
	if (fptr == NULL){
		 return NULL;
	}
	if (fscanf(fptr,"%d",colorcount) != 1){
		fclose(fptr);
		return NULL;
	}
	uint8_t** p = (uint8_t**) malloc(*colorcount*sizeof(uint8_t*));
	for (int i = 0; i<*colorcount;i++){
		uint8_t* iptr = (uint8_t*) malloc(3*sizeof(uint8_t));
		if (fscanf(fptr,"%hhd %hhd %hhd",iptr,iptr+1,iptr+2) != 3){
			fclose(fptr);
			free(p);
			free(iptr);
			free(colorcount);
			return NULL;
		}
		p[i]= iptr;
		// free(iptr);
	}
	fclose(fptr);
	return p;

}


