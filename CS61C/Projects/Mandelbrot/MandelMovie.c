/*********************
**  Mandelbrot fractal movie generator
** clang -Xpreprocessor -fopenmp -lomp -o Mandelbrot Mandelbrot.c
** by Dan Garcia <ddgarcia@cs.berkeley.edu>
** Modified for this class by Justin Yokota and Chenyu Shi
**********************/

#include <stdio.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <math.h>
#include "ComplexNumber.h"
#include "Mandelbrot.h"
#include "ColorMapInput.h"
#include <sys/types.h>

void printUsage(char* argv[])
{
  printf("Usage: %s <threshold> <maxiterations> <center_real> <center_imaginary> <initialscale> <finalscale> <framecount> <resolution> <output_folder> <colorfile>\n", argv[0]);
  printf("    This program simulates the Mandelbrot Fractal, and creates an iteration map of the given center, scale, and resolution, then saves it in output_file\n");
}


/*
This function calculates the threshold values of every spot on a sequence of frames. The center stays the same throughout the zoom. First frame is at initialscale, and last frame is at finalscale scale.
The remaining frames form a geometric sequence of scales, so 
if initialscale=1024, finalscale=1, framecount=11, then your frames will have scales of 1024, 512, 256, 128, 64, 32, 16, 8, 4, 2, 1.
As another example, if initialscale=10, finalscale=0.01, framecount=5, then your frames will have scale 10, 10 * (0.01/10)^(1/4), 10 * (0.01/10)^(2/4), 10 * (0.01/10)^(3/4), 0.01 .
*/
void MandelMovie(double threshold, u_int64_t max_iterations, ComplexNumber* center, double initialscale, double finalscale, int framecount, u_int64_t resolution, u_int64_t ** output){
    //YOUR CODE HERE
	for (int i = 0; i<framecount; i++){
		double scale = initialscale* pow(finalscale/initialscale,(1.0 * i)/(framecount-1));
		Mandelbrot(threshold,max_iterations,center,scale,resolution,output[i]);
	}
}
void helper(u_int64_t* lol, char* colorfile,u_int64_t size,char* outputfile){
		int* colorcount = (int*) malloc(sizeof(int));
		uint8_t** p = FileToColorMap(colorfile,colorcount);
		if (p == NULL){
		free(colorcount);
		free(p);
		return;
		}
		FILE* fptr = fopen(outputfile,"w+");
		fprintf(fptr,"%c%d %ld %ld %d\n",'P',6,size,size,255);
		for (int i =0; i<size*size;i++){
			if (lol[i] == 0){
				uint8_t* iptr = (uint8_t*) malloc(3*sizeof(uint8_t));
				iptr[0] = (uint8_t) 0;
				iptr[1] = (uint8_t) 0;
				iptr[2] = (uint8_t) 0;
				fwrite(iptr,1,3,fptr);
				free(iptr);
			}else {
				fwrite(p[(lol[i] -1) % (*colorcount)],1,3,fptr);
			}
		}
		for (int j = 0;j<*colorcount; j++){
			free(p[j]);
		}
		fclose(fptr);
		free(colorcount);
		free(p);
	}

/**************
**This main function converts command line inputs into the format needed to run MandelMovie.
**It then uses the color array from FileToColorMap to create PPM images for each frame, and stores it in output_folder
***************/
int main(int argc, char* argv[])
{
	//Tips on how to get started on main function: 
	//MandelFrame also follows a similar sequence of steps; it may be useful to reference that.
	//Mayke you complete the steps below in order. 

	//STEP 1: Convert command line inputs to local variables, and ensure that inputs are valid.
	/*
	Check the spec for examples of invalid inputs.
	Remember to use your solution to B.1.1 to process colorfile.
	*/
	if (argc != 11){
		printf("%s: Wrong number of arguments, expecting 10\n", argv[0]);
		printUsage(argv);
		return 1;
	}

	double threshold, initialscale,finalscale;
	u_int64_t maxiterations,resolution;
	ComplexNumber* center;
	int framecount;

	threshold = atof(argv[1]);
	initialscale = atof(argv[5]);
	finalscale = atof(argv[6]);
	maxiterations = (u_int64_t) atoi(argv[2]);
	resolution = (u_int64_t) atoi(argv[8]);
	center = newComplexNumber(atof(argv[3]), atof(argv[4]));
	framecount = atoi(argv[7]);

	if (threshold <= 0 || finalscale <= 0 || maxiterations <= 0 || initialscale <= 0) {
		freeComplexNumber(center);
		printf("The threshold, scale, and max_iterations must be > 0");
		printUsage(argv);
		return 1;
	}
	if (framecount > 10000 || framecount <= 0) {
		freeComplexNumber(center);
		printf("The number of frames must be greater than 0 and less than 10000.");
		printUsage(argv);
		return 1;
	}
	if (resolution <0) {
		freeComplexNumber(center);
		printf("Resolution must be >= 0");
		printUsage(argv);
		return 1;
	}
	if (framecount == 1 && initialscale!= finalscale){
		freeComplexNumber(center);
		printf("The number of frames is 1 AND initialscale != finalscale");
		printUsage(argv);
		return 1;
	}

	u_int64_t size = 2 * resolution + 1;
	 

	//STEP 2: Run MandelMovie on the correct arguments.
	/*
	MandelMovie requires an output array, so make sure you allocate the proper amount of space. 
	If allocation fails, free all the space you have already allocated (including colormap), then return with exit code 1.
	*/

	u_int64_t ** output = (u_int64_t **) malloc(framecount*sizeof(u_int64_t *));
	if (output == NULL){
		freeComplexNumber(center);
		printf("allocation memory for pointers fails");
		printUsage(argv);
		return 1;
	}

	for (int i = 0; i<framecount; i++){
		output[i] = (u_int64_t *) malloc(size*size*sizeof(u_int64_t));
		if (output[i] == NULL){
		freeComplexNumber(center);
		printf("allocation memory for integers fails");
		printUsage(argv);
		return 1;
	}
	}

	MandelMovie(threshold,maxiterations,center,initialscale,finalscale,framecount,resolution,output);



	//STEP 3: Output the results of MandelMovie to .ppm files.
	/*  
	Convert from iteration count to colors, and output the results into output files.
	Use what we showed you in Part B.1.2, create a seqeunce of ppm files in the output folder.
	Feel free to create your own helper function to complete this step.
	As a reminder, we are using P6 format, not P3.
	*/

	for (int j = 0 ; j<framecount; j++){
		char str[19];
		sprintf(str,"/frame%05d.ppm",j);
		char buf[strlen(argv[9])+20];
		strcpy(buf, argv[9]);
		strcat(buf,str);
		helper(output[j],argv[10],size,buf);
	}

	




	//STEP 4: Free all allocated memory
	/*
	Make sure there's no memory leak.
	*/
	//YOUR CODE HERE 





	freeComplexNumber(center);
	for (int i = 0; i<framecount; i++){
		free(output[i]);
	}
	free(output);
	return 0;
}