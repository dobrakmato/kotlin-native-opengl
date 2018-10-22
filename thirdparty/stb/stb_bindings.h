//
// Created by Matej on 22.10.2018.
//

#ifndef STB_STB_BINDINGS_H
#define STB_STB_BINDINGS_H

#include <stdio.h>

unsigned char *loadImageFromFile(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels);

unsigned char *loadImage(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels);

unsigned short *loadImageFromFile16(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels);

unsigned short *loadImage16(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels);

float *loadImageFromFileFloat(FILE *f, int *x, int *y, int *channels_in_file, int desired_channels);

float *loadImageFloat(const char *filename, int *x, int *y, int *channels_in_file, int desired_channels);

void freeImage(void *retval_from_stbi_load);

void setFlipVerticallyOnLoad(int flag_true_if_should_flip);

#endif //STB_STB_BINDINGS_H
