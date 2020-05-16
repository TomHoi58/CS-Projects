/*********************
**  Mandelbrot fractal
** clang -Xpreprocessor -fopenmp -lomp -o Mandelbrot Mandelbrot.c
** by Dan Garcia <ddgarcia@cs.berkeley.edu>
** Modified for this class by Justin Yokota and Chenyu Shi
**********************/

#include <stdio.h>
#include <stdlib.h>
#include "ComplexNumber.h"
#include "Mandelbrot.h"
#include <sys/types.h>

/*
This function returns the number of iterations before the initial point >= the threshold.
If the threshold is not exceeded after maxiters, the function returns 0.
*/
u_int64_t MandelbrotIterations(u_int64_t maxiters, ComplexNumber * point, double threshold)
{
    //YOUR CODE HERE
  ComplexNumber* z = newComplexNumber(0,0);
  for (u_int64_t i = 0;i<maxiters;i++){
    ComplexNumber* product = ComplexProduct(z,z);
    freeComplexNumber(z);
  	z = ComplexSum(product,point);
    freeComplexNumber(product);
  	if (ComplexAbs(z) >= 2){
      freeComplexNumber(z);
  		return i+1;
  	}
  }
  freeComplexNumber(z);
  
  return 0;


}

/*
This function calculates the Mandelbrot plot and stores the result in output.
The number of pixels in the image is resolution * 2 + 1 in one row/column. It's a square image.
Scale is the the distance between center and the top pixel in one dimension.
*/
void Mandelbrot(double threshold, u_int64_t max_iterations, ComplexNumber* center, double scale, u_int64_t resolution, u_int64_t * output){
    //YOUR CODE HERE
    double left_x = Re(center)- scale;
    double bot_y = Im(center) - scale;

    for (double j =resolution*2 ; j>=0 ;j--){
    	for (double i=0; i<resolution*2 +1;i++){
        ComplexNumber* new = newComplexNumber(left_x+(scale/resolution)*i,bot_y+(scale/resolution)*j);
        		*output++ = MandelbrotIterations(max_iterations,new,threshold);
            freeComplexNumber(new);
    	}
    }
}

// int main(){
// 	printf("%lld\n", MandelbrotIterations(10,newComplexNumber(-1,0),2));
// 	return 0;
// }


